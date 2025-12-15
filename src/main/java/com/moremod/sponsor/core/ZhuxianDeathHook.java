package com.moremod.sponsor.core;

import com.moremod.sponsor.item.ZhuxianSword;
import com.moremod.util.combat.TrueDamageHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

/**
 * 诛仙剑死亡钩子
 * Zhuxian Death Hook
 *
 * 此类由 ASM Transformer 调用，提供"为生民立命"技能的血量保护：
 * - 血量不会低于最大生命的20%
 *
 * 集成方式：在 moremodTransformer 中添加对此类的调用
 */
public class ZhuxianDeathHook {

    /** 血量保护阈值：20% */
    private static final float HEALTH_THRESHOLD = 0.2f;

    /**
     * 检查并限制伤害，确保血量不低于20%
     * Called by ASM at HEAD of EntityLivingBase.damageEntity
     *
     * @param entity 受伤的实体
     * @param source 伤害来源
     * @param damage 伤害量
     * @return true = 取消伤害
     */
    public static boolean checkAndLimitDamage(EntityLivingBase entity, DamageSource source, float damage) {
        try {
            if (!(entity instanceof EntityPlayer)) {
                return false;
            }

            EntityPlayer player = (EntityPlayer) entity;

            // 检查是否激活"为生民立命"技能
            if (!ZhuxianSword.isPlayerSkillActive(player, ZhuxianSword.NBT_SKILL_LIMING)) {
                return false;
            }

            // 跳过真伤（避免递归）
            if (TrueDamageHelper.isInTrueDamageContext()) {
                return false;
            }
            if (TrueDamageHelper.isTrueDamageSource(source)) {
                return false;
            }

            float currentHealth = player.getHealth();
            float minHealth = player.getMaxHealth() * HEALTH_THRESHOLD;

            // 如果伤害会使血量低于阈值
            if (currentHealth - damage < minHealth) {
                if (currentHealth <= minHealth) {
                    // 已经在阈值以下，完全取消伤害
                    return true;
                }
                // 否则让 LivingHurtEvent 处理伤害限制
            }

            return false;

        } catch (Throwable t) {
            System.err.println("[ZhuxianDeathHook] Error in checkAndLimitDamage: " + t.getMessage());
            return false;
        }
    }

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

            // 检查是否激活"为生民立命"技能
            if (!ZhuxianSword.isPlayerSkillActive(player, ZhuxianSword.NBT_SKILL_LIMING)) {
                return false;
            }

            // 恢复到最低血量
            float minHealth = player.getMaxHealth() * HEALTH_THRESHOLD;
            player.setHealth(minHealth);

            return true;

        } catch (Throwable t) {
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
