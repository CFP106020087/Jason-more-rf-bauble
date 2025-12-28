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
import java.util.Optional;

/**
 * 自动攻击客户端处理器
 *
 * 职责：
 * 1. 检测鼠标按住状态
 * 2. 获取目标实体（支持触及距离加成）
 * 3. 发送目标ID到服务器（服务器负责所有攻击逻辑）
 * 4. 播放挥舞动画
 */
@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
public class ClientAutoAttackHandler {

    private static boolean wasAttacking = false;
    private static int tickCounter = 0;
    private static boolean mouseHeld = false;

    // 扩展追踪缓存（性能优化）
    private static Entity cachedExtendedTarget = null;
    private static int extendedTraceTimer = 0;
    private static double cachedReachDistance = 3.0;
    private static int reachCacheTimer = 0;
    
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
     * 获取目标实体ID - 支持触及距离加成
     *
     * 优化策略：
     * 1. 优先使用原版 objectMouseOver（零开销）
     * 2. 只有原版找不到时，才做扩展追踪
     * 3. 扩展追踪每3tick更新一次（降低开销）
     * 4. 触及距离每20tick缓存一次
     */
    @SideOnly(Side.CLIENT)
    private static int getTargetEntityId(Minecraft mc) {
        EntityPlayer player = mc.player;
        if (player == null) return -1;

        // 1. 优先使用原版结果（零开销）
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.ENTITY) {
            Entity target = mc.objectMouseOver.entityHit;
            if (target != null && !target.isDead && target.canBeAttackedWithItem()) {
                return target.getEntityId();
            }
        }

        // 2. 更新触及距离缓存（每20tick = 1秒）
        reachCacheTimer++;
        if (reachCacheTimer >= 20) {
            reachCacheTimer = 0;
            cachedReachDistance = getPlayerReachDistance(player);
        }

        // 3. 如果触及距离没有加成，直接返回
        if (cachedReachDistance <= 3.5) {
            return -1;
        }

        // 4. 扩展追踪（每3tick更新一次）
        extendedTraceTimer++;
        if (extendedTraceTimer >= 3) {
            extendedTraceTimer = 0;
            cachedExtendedTarget = extendedRayTraceEntity(player, cachedReachDistance);
        }

        // 5. 返回缓存的扩展目标
        if (cachedExtendedTarget != null && !cachedExtendedTarget.isDead && cachedExtendedTarget.canBeAttackedWithItem()) {
            return cachedExtendedTarget.getEntityId();
        }

        return -1;
    }

    /**
     * 获取玩家的触及距离（包括加成）
     */
    @SideOnly(Side.CLIENT)
    private static double getPlayerReachDistance(EntityPlayer player) {
        // 尝试获取 Forge 的触及距离属性
        try {
            net.minecraft.entity.ai.attributes.IAttributeInstance reachAttr =
                    player.getEntityAttribute(EntityPlayer.REACH_DISTANCE);
            if (reachAttr != null) {
                return reachAttr.getAttributeValue();
            }
        } catch (Exception e) {
            // 属性不存在，使用默认值
        }

        // 默认触及距离：生存模式3格，创造模式4.5格
        return player.isCreative() ? 4.5 : 3.0;
    }

    /**
     * 扩展范围的实体光线追踪
     */
    @SideOnly(Side.CLIENT)
    private static Entity extendedRayTraceEntity(EntityPlayer player, double reach) {
        Vec3d eyePos = player.getPositionEyes(1.0F);
        Vec3d lookVec = player.getLook(1.0F);
        Vec3d reachVec = eyePos.add(lookVec.x * reach, lookVec.y * reach, lookVec.z * reach);

        // 先做方块追踪，确定最远可达距离
        RayTraceResult blockResult = player.world.rayTraceBlocks(eyePos, reachVec, false, false, true);
        double maxDistance = reach;
        if (blockResult != null && blockResult.typeOfHit == RayTraceResult.Type.BLOCK) {
            maxDistance = eyePos.distanceTo(blockResult.hitVec);
        }

        // 实体追踪
        Entity closestEntity = null;
        double closestDistance = maxDistance;

        Vec3d maxReachVec = eyePos.add(lookVec.x * maxDistance, lookVec.y * maxDistance, lookVec.z * maxDistance);
        AxisAlignedBB searchBox = player.getEntityBoundingBox()
                .expand(lookVec.x * maxDistance, lookVec.y * maxDistance, lookVec.z * maxDistance)
                .grow(1.0, 1.0, 1.0);

        List<Entity> entities = player.world.getEntitiesWithinAABBExcludingEntity(player, searchBox);

        for (Entity entity : entities) {
            if (!entity.canBeCollidedWith() || !entity.canBeAttackedWithItem()) continue;
            if (entity.isDead) continue;

            AxisAlignedBB entityBox = entity.getEntityBoundingBox().grow(entity.getCollisionBorderSize());
            RayTraceResult intercept = entityBox.calculateIntercept(eyePos, maxReachVec);

            if (intercept != null) {
                double dist = eyePos.distanceTo(intercept.hitVec);
                if (dist < closestDistance) {
                    closestDistance = dist;
                    closestEntity = entity;
                }
            }
        }

        return closestEntity;
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