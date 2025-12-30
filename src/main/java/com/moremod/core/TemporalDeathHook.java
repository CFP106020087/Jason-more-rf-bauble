package com.moremod.core;

import com.moremod.fabric.handler.FabricEventHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

/**
 * 时序织印死亡钩子
 * Temporal Fabric Death Hook
 *
 * 此类由 ASM Transformer 调用，提供死亡保护：
 * 1. damageEntity - 检测致命伤害并触发时序回溯
 * 2. onDeath - 最终防线
 *
 * 必须在游戏早期加载，不依赖任何延迟初始化的类
 */
public class TemporalDeathHook {

    // ========== Hook 1: damageEntity ==========

    /**
     * 检查并触发时序回溯
     * Called by ASM at HEAD of EntityLivingBase.damageEntity
     *
     * @param entity 受伤的实体
     * @param source 伤害来源
     * @param damage 最终伤害量（护甲后）
     * @return true = 取消伤害并触发回溯
     */
    public static boolean checkAndTriggerRewind(EntityLivingBase entity, DamageSource source, float damage) {
        try {
            // 快速路径：非玩家直接放行
            if (!(entity instanceof EntityPlayer)) {
                return false;
            }

            EntityPlayer player = (EntityPlayer) entity;

            // 检查是否穿戴时序织印
            if (!FabricEventHandler.hasTemporalFabric(player)) {
                return false;
            }

            // 已在回溯保护中：取消伤害
            if (FabricEventHandler.isInTemporalRewind(player)) {
                return true;
            }

            // 检测致命伤害：当前血量 - 伤害 <= 0
            float currentHealth = player.getHealth();
            if (currentHealth - damage <= 0) {
                // 致命伤害！尝试触发时序回溯
                boolean success = FabricEventHandler.triggerTemporalRewind(player);
                if (success) {
                    System.out.println("[TemporalDeathHook] Fatal damage detected, temporal rewind triggered: " + player.getName());
                    return true;
                }
            }

            return false;

        } catch (Throwable t) {
            System.err.println("[TemporalDeathHook] Error in checkAndTriggerRewind: " + t.getMessage());
            return false;
        }
    }

    // ========== Hook 2: onDeath（最终防线） ==========

    /**
     * 检查是否应该拦截死亡
     * Called by ASM at HEAD of EntityLivingBase.onDeath
     *
     * @param entity 将要死亡的实体
     * @param source 伤害来源
     * @return true = 阻止死亡
     */
    public static boolean shouldPreventDeath(EntityLivingBase entity, DamageSource source) {
        try {
            if (!(entity instanceof EntityPlayer)) {
                return false;
            }

            EntityPlayer player = (EntityPlayer) entity;

            // 检查是否穿戴时序织印
            if (!FabricEventHandler.hasTemporalFabric(player)) {
                return false;
            }

            // 如果已经在回溯保护中，阻止死亡
            if (FabricEventHandler.isInTemporalRewind(player)) {
                if (player.getHealth() < 0.5f) {
                    player.setHealth(0.5f);
                }
                return true;
            }

            // 最终防线：尝试触发回溯
            boolean success = FabricEventHandler.triggerTemporalRewind(player);
            if (success) {
                System.out.println("[TemporalDeathHook] Final defense: Temporal rewind triggered for " + player.getName());
                return true;
            }

            return false;

        } catch (Throwable t) {
            System.err.println("[TemporalDeathHook] Error in shouldPreventDeath: " + t.getMessage());
            return false;
        }
    }

    // ========== Hook 3: setDead（终极防线） ==========

    /**
     * 检查是否应该阻止 setDead() 调用
     * Called by ASM at HEAD of Entity.setDead
     *
     * @param entity 将要被标记为死亡的实体
     * @return true = 阻止 setDead
     */
    public static boolean shouldPreventSetDead(net.minecraft.entity.Entity entity) {
        try {
            if (!(entity instanceof EntityPlayer)) {
                return false;
            }

            EntityPlayer player = (EntityPlayer) entity;

            // 检查是否穿戴时序织印
            if (!FabricEventHandler.hasTemporalFabric(player)) {
                return false;
            }

            // 如果已经在回溯保护中，阻止 setDead
            if (FabricEventHandler.isInTemporalRewind(player)) {
                if (player.getHealth() < 0.5f) {
                    player.setHealth(0.5f);
                }
                return true;
            }

            // 最终防线：尝试触发回溯
            boolean success = FabricEventHandler.triggerTemporalRewind(player);
            if (success) {
                System.out.println("[TemporalDeathHook] setDead intercepted, temporal rewind triggered: " + player.getName());
                return true;
            }

            return false;

        } catch (Throwable t) {
            System.err.println("[TemporalDeathHook] Error in shouldPreventSetDead: " + t.getMessage());
            return false;
        }
    }
}
