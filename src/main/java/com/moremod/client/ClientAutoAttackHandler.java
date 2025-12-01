package com.moremod.client;

import com.moremod.compat.crafttweaker.GemAffix;
import com.moremod.compat.crafttweaker.GemNBTHelper;
import com.moremod.compat.crafttweaker.GemSocketHelper;
import com.moremod.compat.crafttweaker.IdentifiedAffix;
import com.moremod.network.MessageAutoAttackTrigger;
import com.moremod.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 自动攻击客户端处理器
 * 
 * 职责：
 * 1. 检测鼠标按住状态
 * 2. 获取mc.objectMouseOver的目标实体
 * 3. 发送目标ID到服务器（服务器负责所有攻击逻辑）
 * 4. 播放挥舞动画
 */
@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
public class ClientAutoAttackHandler {
    
    private static boolean wasAttacking = false;
    private static int tickCounter = 0;
    private static boolean mouseHeld = false;
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @SideOnly(Side.CLIENT)
    public static void onMouseClick(MouseEvent event) {
        if (event.getButton() != 0) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;
        
        ItemStack weapon = mc.player.getHeldItemMainhand();
        if (weapon.isEmpty() || !(weapon.getItem() instanceof ItemSword)) return;
        
        if (!hasAutoAttackAffix(weapon)) return;
        
        mouseHeld = event.isButtonstate();
        
        if (mouseHeld) {
            event.setCanceled(true);
            
            // 获取目标实体ID
            int targetId = getTargetEntityId(mc);
            
            // 发送到服务器处理
            PacketHandler.INSTANCE.sendToServer(new MessageAutoAttackTrigger(true, targetId));
            
            // 客户端播放动画
            mc.player.swingArm(EnumHand.MAIN_HAND);
            mc.player.resetCooldown();
            
            wasAttacking = true;
            tickCounter = 0;
        } else {
            if (wasAttacking) {
                PacketHandler.INSTANCE.sendToServer(new MessageAutoAttackTrigger(false, -1));
            }
            wasAttacking = false;
            tickCounter = 0;
        }
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        
        if (player == null || mc.world == null) {
            wasAttacking = false;
            mouseHeld = false;
            return;
        }
        
        ItemStack weapon = player.getHeldItemMainhand();
        if (weapon.isEmpty() || !(weapon.getItem() instanceof ItemSword)) {
            if (wasAttacking) {
                PacketHandler.INSTANCE.sendToServer(new MessageAutoAttackTrigger(false, -1));
                wasAttacking = false;
            }
            mouseHeld = false;
            return;
        }
        
        boolean hasAutoAttack = hasAutoAttackAffix(weapon);
        
        if (!hasAutoAttack) {
            if (wasAttacking) {
                PacketHandler.INSTANCE.sendToServer(new MessageAutoAttackTrigger(false, -1));
                wasAttacking = false;
            }
            mouseHeld = false;
            return;
        }
        
        if (mouseHeld && wasAttacking) {
            tickCounter++;
            
            float speedMultiplier = getAttackSpeedMultiplier(weapon);
            
            int interval;
            if (speedMultiplier >= 90.0f) {
                interval = 1;
            } else if (speedMultiplier >= 48.0f) {
                interval = 2;
            } else if (speedMultiplier >= 24.0f) {
                interval = 3;
            } else if (speedMultiplier >= 12.0f) {
                interval = 5;
            } else {
                interval = Math.max(2, (int)(10 / speedMultiplier));
            }
            
            if (tickCounter >= interval) {
                // 获取目标实体ID
                int targetId = getTargetEntityId(mc);
                
                // 发送到服务器处理
                PacketHandler.INSTANCE.sendToServer(new MessageAutoAttackTrigger(true, targetId));
                
                // 客户端播放动画
                mc.player.swingArm(EnumHand.MAIN_HAND);
                mc.player.resetCooldown();
                
                tickCounter = 0;
            }
        } else if (!mouseHeld && wasAttacking) {
            PacketHandler.INSTANCE.sendToServer(new MessageAutoAttackTrigger(false, -1));
            wasAttacking = false;
            tickCounter = 0;
        }
    }
    
    /**
     * 获取目标实体ID - 使用Minecraft原版的objectMouseOver
     */
    @SideOnly(Side.CLIENT)
    private static int getTargetEntityId(Minecraft mc) {
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.ENTITY) {
            Entity target = mc.objectMouseOver.entityHit;
            if (target != null && !target.isDead) {
                return target.getEntityId();
            }
        }
        return -1;
    }
    
    private static boolean hasAutoAttackAffix(ItemStack weapon) {
        if (!GemSocketHelper.hasSocketedGems(weapon)) {
            return false;
        }
        
        ItemStack[] gems = GemSocketHelper.getAllSocketedGems(weapon);
        
        for (ItemStack gem : gems) {
            if (gem.isEmpty() || !GemNBTHelper.isIdentified(gem)) {
                continue;
            }
            
            List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
            
            for (IdentifiedAffix affix : affixes) {
                if (affix.getAffix().getType() != GemAffix.AffixType.SPECIAL_EFFECT) {
                    continue;
                }
                
                String effectType = (String) affix.getAffix().getParameter("effectType");
                if ("auto_attack".equals(effectType)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private static float getAttackSpeedMultiplier(ItemStack weapon) {
        if (!GemSocketHelper.hasSocketedGems(weapon)) {
            return 1.0f;
        }
        
        ItemStack[] gems = GemSocketHelper.getAllSocketedGems(weapon);
        float totalMultiplier = 0.0f;
        boolean hasAutoAttack = false;
        
        for (ItemStack gem : gems) {
            if (gem.isEmpty() || !GemNBTHelper.isIdentified(gem)) {
                continue;
            }
            
            List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
            
            for (IdentifiedAffix affix : affixes) {
                if (affix.getAffix().getType() != GemAffix.AffixType.SPECIAL_EFFECT) {
                    continue;
                }
                
                String effectType = (String) affix.getAffix().getParameter("effectType");
                if ("auto_attack".equals(effectType)) {
                    hasAutoAttack = true;
                    totalMultiplier += affix.getValue();
                }
            }
        }
        
        return hasAutoAttack ? Math.max(totalMultiplier, 0.1f) : 1.0f;
    }
}