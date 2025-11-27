package com.moremod.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 高级真伤系统
 * Advanced True Damage System
 *
 * 使用 setHealth 包装的真伤逻辑，绕过无敌帧和部分防御机制
 * 但保留正常的受击、死亡流程
 */
public class TrueDamageHelper {

    /** 正在处理真伤的实体（防止递归） */
    private static final Set<UUID> processingEntities = new HashSet<>();

    /** 自定义真伤伤害源 */
    public static final DamageSource TRUE_DAMAGE = new DamageSource("moremod_true_damage")
            .setDamageBypassesArmor()
            .setDamageIsAbsolute();

    /**
     * 真伤类型标记
     */
    public enum TrueDamageFlag {
        PHANTOM_TWIN,       // 幻象分身（破碎_投影）
        TERMINUS_BONUS,     // 终末追加（破碎_终结）
        PHANTOM_STRIKE,     // 幻象打击（破碎_手）
        EXECUTE             // 斩杀执行
    }

    /**
     * 应用包装真伤
     *
     * @param target 目标实体
     * @param source 伤害来源（可为null）
     * @param trueDamage 真伤数值
     * @param flag 真伤类型标记
     * @return 是否成功应用
     */
    public static boolean applyWrappedTrueDamage(EntityLivingBase target,
                                                  @Nullable Entity source,
                                                  float trueDamage,
                                                  TrueDamageFlag flag) {
        if (target == null || target.world.isRemote) return false;
        if (trueDamage <= 0) return false;

        UUID targetId = target.getUniqueID();

        // 防止递归
        if (processingEntities.contains(targetId)) {
            return false;
        }

        try {
            processingEntities.add(targetId);

            float before = target.getHealth();
            if (before <= 0) return false;

            float after = Math.max(0, before - trueDamage);

            // 触发受击动画和音效（使用极小伤害）
            // 这样不会造成实际伤害但能触发视觉效果
            DamageSource dmgSource = createDamageSource(source, flag);

            // 先尝试触发受击效果（0.01伤害不会改变血量太多）
            target.hurtResistantTime = 0; // 重置无敌帧
            target.attackEntityFrom(dmgSource, 0.01f);

            // 使用 setHealth 应用真正的伤害
            target.setHealth(after);

            // 如果目标死亡，确保死亡流程正常触发
            if (after <= 0 && !target.isDead) {
                target.onDeath(dmgSource);
            }

            return true;

        } finally {
            processingEntities.remove(targetId);
        }
    }

    /**
     * 应用斩杀真伤（直接将目标生命归零）
     */
    public static boolean applyExecuteDamage(EntityLivingBase target,
                                              @Nullable Entity source) {
        if (target == null || target.world.isRemote) return false;

        UUID targetId = target.getUniqueID();
        if (processingEntities.contains(targetId)) {
            return false;
        }

        try {
            processingEntities.add(targetId);

            float before = target.getHealth();
            if (before <= 0) return false;

            DamageSource dmgSource = createDamageSource(source, TrueDamageFlag.EXECUTE);

            // 触发受击
            target.hurtResistantTime = 0;
            target.attackEntityFrom(dmgSource, 0.01f);

            // 直接归零
            target.setHealth(0);

            // 触发死亡
            if (!target.isDead) {
                target.onDeath(dmgSource);
            }

            return true;

        } finally {
            processingEntities.remove(targetId);
        }
    }

    /**
     * 检查是否正在处理真伤（用于外部判断防止递归）
     */
    public static boolean isProcessingTrueDamage(EntityLivingBase entity) {
        return processingEntities.contains(entity.getUniqueID());
    }

    /**
     * 创建伤害源
     */
    private static DamageSource createDamageSource(@Nullable Entity source, TrueDamageFlag flag) {
        if (source instanceof EntityPlayer) {
            return new EntityDamageSource("moremod_true_damage", source)
                    .setDamageBypassesArmor()
                    .setDamageIsAbsolute();
        }
        return TRUE_DAMAGE;
    }

    /**
     * 计算无视护甲后的等效伤害
     * 用于模拟"忽略50%护甲"等效果
     *
     * @param baseDamage 基础伤害
     * @param armorIgnorePercent 护甲忽略百分比 (0.5 = 50%)
     * @return 调整后的伤害
     */
    public static float calculateArmorBypassDamage(float baseDamage, float armorIgnorePercent) {
        // 简单实现：增加伤害来模拟护甲穿透
        // 假设护甲平均减免40%，忽略50%护甲 = 恢复20%伤害
        float bonusMultiplier = 1.0f + (armorIgnorePercent * 0.4f);
        return baseDamage * bonusMultiplier;
    }
}
