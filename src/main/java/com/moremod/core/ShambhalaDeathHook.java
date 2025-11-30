package com.moremod.core;

import com.moremod.config.ShambhalaConfig;
import com.moremod.system.ascension.ShambhalaHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

/**
 * 香巴拉死亡钩子
 * Shambhala Death Hook
 *
 * 此类由 ASM Transformer 调用，提供能量护盾保护：
 * 1. attackEntityFrom - 无条件放行（香巴拉不免疫攻击）
 * 2. damageEntity - 检测致命伤害，消耗能量阻止
 * 3. onDeath - 最终防线，消耗能量阻止死亡
 *
 * 与破碎之神的区别：
 * - 破碎之神：停机模式，完全免疫
 * - 香巴拉：消耗能量抵挡，没能量则死亡
 */
public class ShambhalaDeathHook {

    // ========== Hook 1: attackEntityFrom ==========
    // 香巴拉不在这里拦截，让伤害正常计算

    /**
     * 检查是否应该取消攻击
     * 香巴拉：始终返回false，不免疫攻击
     */
    public static boolean shouldCancelAttack(EntityLivingBase entity, DamageSource source, float amount) {
        // 香巴拉不在攻击阶段拦截
        return false;
    }

    // ========== Hook 2: damageEntity ==========

    /**
     * 检查并尝试用能量阻止致命伤害
     * Called by ASM at HEAD of EntityLivingBase.damageEntity
     *
     * @param entity 受伤的实体
     * @param source 伤害来源
     * @param damage 最终伤害量（护甲后）
     * @return true = 取消伤害（用能量吸收了）
     */
    public static boolean checkAndAbsorbDamage(EntityLivingBase entity, DamageSource source, float damage) {
        try {
            if (!(entity instanceof EntityPlayer)) {
                return false;
            }

            EntityPlayer player = (EntityPlayer) entity;

            if (!ShambhalaHandler.isShambhala(player)) {
                return false;
            }

            // 检测致命伤害：当前血量 - 伤害 <= 0
            float currentHealth = player.getHealth();
            if (currentHealth - damage <= 0) {
                // 致命伤害！尝试用能量阻止
                int energyCost = (int) (damage * ShambhalaConfig.energyPerDamage);

                if (ShambhalaHandler.consumeEnergy(player, energyCost)) {
                    // 成功用能量抵消
                    player.setHealth((float) ShambhalaConfig.coreHealthLock);
                    System.out.println("[ShambhalaDeathHook] Absorbed fatal damage with energy for: " + player.getName());
                    return true;
                }

                // 能量不足，尝试用剩余能量
                int remaining = ShambhalaHandler.getCurrentEnergy(player);
                if (remaining > 0) {
                    float absorbable = (float) remaining / ShambhalaConfig.energyPerDamage;
                    if (absorbable > 0) {
                        ShambhalaHandler.consumeEnergy(player, remaining);
                        // 减少伤害但不完全取消
                        // 让剩余伤害继续，可能导致死亡
                    }
                }
                // 没有足够能量 = 让死亡发生
            }

            return false;

        } catch (Throwable t) {
            System.err.println("[ShambhalaDeathHook] Error in checkAndAbsorbDamage: " + t.getMessage());
            return false;
        }
    }

    // ========== Hook 3: onDeath（最终防线） ==========

    /**
     * 检查是否应该拦截死亡
     * Called by ASM at HEAD of EntityLivingBase.onDeath
     *
     * @param entity 将要死亡的实体
     * @param source 伤害来源
     * @return true = 阻止死亡（有能量）
     */
    public static boolean shouldPreventDeath(EntityLivingBase entity, DamageSource source) {
        try {
            if (!(entity instanceof EntityPlayer)) {
                return false;
            }

            EntityPlayer player = (EntityPlayer) entity;

            if (!ShambhalaHandler.isShambhala(player)) {
                return false;
            }

            // 最终防线：检查是否有能量
            int currentEnergy = ShambhalaHandler.getCurrentEnergy(player);
            if (currentEnergy > 0) {
                // 消耗大量能量作为死亡拦截代价
                int deathCost = ShambhalaConfig.energyPerDamage * 20; // 20点伤害的能量
                ShambhalaHandler.consumeEnergy(player, Math.min(deathCost, currentEnergy));

                // 恢复到最低血量
                player.setHealth((float) ShambhalaConfig.coreHealthLock);

                System.out.println("[ShambhalaDeathHook] Final defense: Prevented death for " + player.getName());
                return true;
            }

            // 没有能量 = 真正死亡（香巴拉的核心代价）
            System.out.println("[ShambhalaDeathHook] No energy remaining, death allowed for: " + player.getName());
            return false;

        } catch (Throwable t) {
            System.err.println("[ShambhalaDeathHook] Error in shouldPreventDeath: " + t.getMessage());
            return false;
        }
    }

    /**
     * 兼容方法
     */
    public static boolean onPreLivingDeath(EntityLivingBase entity, DamageSource source) {
        return shouldPreventDeath(entity, source);
    }
}
