package com.moremod.event;

import com.moremod.compat.crafttweaker.GemSocketHelper;
import com.moremod.network.MessageAutoAttackTrigger;
import com.moremod.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
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
 * 客户端自动攻击 - 配合GemAttackSpeedSystem使用
 * 
 * 重要：攻击间隔计算必须与GemAttackSpeedSystem完全一致！
 */
@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
public class  ClientAutoAttackHandler {
    
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
            
            // ==================== 关键：必须与GemAttackSpeedSystem的计算一致 ====================
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
            // ==================================================================================
            
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
            if (gem.isEmpty()) continue;
            
            NBTTagCompound nbt = gem.getTagCompound();
            if (nbt == null) continue;
            
            if (!nbt.hasKey("GemData")) continue;
            NBTTagCompound gemData = nbt.getCompoundTag("GemData");
            
            if (!gemData.hasKey("identified") || gemData.getByte("identified") != 1) {
                continue;
            }
            
            if (!gemData.hasKey("affixes")) continue;
            
            try {
                NBTTagList affixList = gemData.getTagList("affixes", 10);
                
                for (int i = 0; i < affixList.tagCount(); i++) {
                    NBTTagCompound affixTag = affixList.getCompoundTagAt(i);
                    String id = affixTag.getString("id");
                    
                    if (id.contains("auto_attack")) {
                        return true;
                    }
                }
            } catch (Exception e) {
                // 忽略
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
        
        for (ItemStack gem : gems) {
            if (gem.isEmpty()) continue;
            
            NBTTagCompound nbt = gem.getTagCompound();
            if (nbt == null) continue;
            
            if (!nbt.hasKey("GemData")) continue;
            NBTTagCompound gemData = nbt.getCompoundTag("GemData");
            
            if (!gemData.hasKey("identified") || gemData.getByte("identified") != 1) {
                continue;
            }
            
            if (!gemData.hasKey("affixes")) continue;
            
            try {
                NBTTagList affixList = gemData.getTagList("affixes", 10);
                
                for (int i = 0; i < affixList.tagCount(); i++) {
                    NBTTagCompound affixTag = affixList.getCompoundTagAt(i);
                    String id = affixTag.getString("id");
                    
                    if (id.contains("auto_attack")) {
                        if (affixTag.hasKey("value")) {
                            float value = affixTag.getFloat("value");
                            totalMultiplier += value;
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略
            }
        }
        
        return totalMultiplier > 0 ? totalMultiplier : 1.0f;
    }
}