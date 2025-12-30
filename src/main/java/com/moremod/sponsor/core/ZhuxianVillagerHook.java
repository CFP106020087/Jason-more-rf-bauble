package com.moremod.sponsor.core;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.DamageSource;

/**
 * 诛仙剑村民保护钩子
 * Zhuxian Villager Protection Hook
 *
 * 此类由 ASM Transformer 调用，提供"为生民立命"技能的村民无敌保护
 * 比事件方式更底层，能拦截所有伤害类型
 *
 * 开关: moremodTransformer.ENABLE_ZHUXIAN_VILLAGER
 */
public class ZhuxianVillagerHook {

    /** NBT标记键 - 与 ZhuxianEventHandler 中保持一致 */
    private static final String NBT_PROTECTED = "ZhuxianProtected";

    /**
     * 检查是否应该保护村民免受伤害
     * Called by ASM at HEAD of EntityLivingBase.damageEntity
     *
     * @param entity 受伤的实体
     * @param source 伤害来源
     * @param damage 伤害量
     * @return true = 取消伤害（保护村民）
     */
    public static boolean shouldProtectVillager(EntityLivingBase entity, DamageSource source, float damage) {
        try {
            // 只处理村民
            if (!(entity instanceof EntityVillager)) {
                return false;
            }

            EntityVillager villager = (EntityVillager) entity;

            // 检查是否被诛仙剑保护
            if (villager.getEntityData().getBoolean(NBT_PROTECTED)) {
                // 调试日志（可选）
                // System.out.println("[ZhuxianVillagerHook] Protected villager from " + damage + " damage");
                return true;
            }

            return false;

        } catch (Throwable t) {
            // 任何错误都不应该阻止正常伤害流程
            return false;
        }
    }

    /**
     * 检查是否应该阻止村民死亡
     * Called by ASM at HEAD of EntityLivingBase.onDeath
     *
     * @param entity 将要死亡的实体
     * @param source 伤害来源
     * @return true = 阻止死亡
     */
    public static boolean shouldPreventVillagerDeath(EntityLivingBase entity, DamageSource source) {
        try {
            if (!(entity instanceof EntityVillager)) {
                return false;
            }

            EntityVillager villager = (EntityVillager) entity;

            if (villager.getEntityData().getBoolean(NBT_PROTECTED)) {
                // 恢复村民生命值
                villager.setHealth(villager.getMaxHealth());
                return true;
            }

            return false;

        } catch (Throwable t) {
            return false;
        }
    }
}
