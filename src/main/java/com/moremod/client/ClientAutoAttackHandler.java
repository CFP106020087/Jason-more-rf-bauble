package com.moremod.client;

import com.moremod.compat.crafttweaker.GemAffix;
import com.moremod.compat.crafttweaker.GemNBTHelper;
import com.moremod.compat.crafttweaker.GemSocketHelper;
import com.moremod.compat.crafttweaker.IdentifiedAffix;
import com.moremod.network.MessageAutoAttackTrigger;
import com.moremod.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 平衡版自动攻击处理器
 * 
 * 考虑6槽叠加：
 * - 12-24倍：每5tick（4次/秒）
 * - 24-48倍：每3tick（6.7次/秒）
 * - 48-90倍：每2tick（10次/秒）
 * - 90倍+：每1tick（20次/秒）
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
            performAttack(mc);
            mc.player.swingArm(EnumHand.MAIN_HAND);
            mc.player.resetCooldown();
            wasAttacking = true;
            tickCounter = 0;
        } else {
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
            return;
        }
        
        ItemStack weapon = player.getHeldItemMainhand();
        if (weapon.isEmpty() || !(weapon.getItem() instanceof ItemSword)) {
            if (wasAttacking) {
                PacketHandler.INSTANCE.sendToServer(new MessageAutoAttackTrigger(false));
                wasAttacking = false;
            }
            return;
        }
        
        boolean hasAutoAttack = hasAutoAttackAffix(weapon);
        
        if (!hasAutoAttack) {
            if (wasAttacking) {
                PacketHandler.INSTANCE.sendToServer(new MessageAutoAttackTrigger(false));
                wasAttacking = false;
            }
            return;
        }
        
        if (mouseHeld && wasAttacking) {
            tickCounter++;
            
            float speedMultiplier = getAttackSpeedMultiplier(weapon);
            
            // 平衡版攻击间隔（考虑6槽叠加后的总倍率）:
            // speedMultiplier >= 90   → 每1tick（20次/秒）极限频率
            // speedMultiplier >= 48   → 每2tick（10次/秒）
            // speedMultiplier >= 24   → 每3tick（6.7次/秒）
            // speedMultiplier >= 12   → 每5tick（4次/秒）
            // speedMultiplier < 12    → 动态计算
            
            int interval;
            if (speedMultiplier >= 90.0f) {
                interval = 1; // 每tick都攻击
            } else if (speedMultiplier >= 48.0f) {
                interval = 2;
            } else if (speedMultiplier >= 24.0f) {
                interval = 3;
            } else if (speedMultiplier >= 12.0f) {
                interval = 5;
            } else {
                // 低于12倍时，使用传统计算
                interval = Math.max(2, (int)(10 / speedMultiplier));
            }
            
            if (tickCounter >= interval) {
                performAttack(mc);
                mc.player.swingArm(EnumHand.MAIN_HAND);
                mc.player.resetCooldown();
                tickCounter = 0;
            }
            
            if (!wasAttacking) {
                PacketHandler.INSTANCE.sendToServer(new MessageAutoAttackTrigger(true));
                wasAttacking = true;
            }
        } else if (!mouseHeld && wasAttacking) {
            PacketHandler.INSTANCE.sendToServer(new MessageAutoAttackTrigger(false));
            wasAttacking = false;
            tickCounter = 0;
        }
    }
    
    @SideOnly(Side.CLIENT)
    private static void performAttack(Minecraft mc) {
        Entity target = getMouseOverEntity(mc, 6.0);
        if (target instanceof EntityLivingBase) {
            mc.player.connection.sendPacket(new net.minecraft.network.play.client.CPacketUseEntity(target));
            mc.player.attackTargetEntityWithCurrentItem(target);
        }
    }
    
    @SideOnly(Side.CLIENT)
    private static Entity getMouseOverEntity(Minecraft mc, double distance) {
        if (mc.getRenderViewEntity() == null) return null;
        
        Vec3d eyes = mc.getRenderViewEntity().getPositionEyes(1.0F);
        Vec3d look = mc.getRenderViewEntity().getLook(1.0F);
        Vec3d reach = eyes.add(look.x * distance, look.y * distance, look.z * distance);
        
        Entity pointed = null;
        double minDist = distance;
        
        List<Entity> list = mc.world.getEntitiesWithinAABBExcludingEntity(
            mc.getRenderViewEntity(),
            mc.getRenderViewEntity().getEntityBoundingBox()
                .expand(look.x * distance, look.y * distance, look.z * distance)
                .grow(1.0)
        );
        
        for (Entity e : list) {
            if (!e.canBeCollidedWith()) continue;
            if (!(e instanceof EntityLivingBase)) continue;
            
            AxisAlignedBB aabb = e.getEntityBoundingBox().grow(e.getCollisionBorderSize());
            RayTraceResult hit = aabb.calculateIntercept(eyes, reach);
            
            if (hit != null) {
                double dist = eyes.distanceTo(hit.hitVec);
                if (dist < minDist) {
                    minDist = dist;
                    pointed = e;
                }
            }
        }
        
        return pointed;
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