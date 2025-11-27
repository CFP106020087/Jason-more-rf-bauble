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
 * 正确实现：通过 attackEntityFrom + 无敌帧重置 + 绕过护甲的 DamageSource
 * 确保完整的伤害流程：LivingHurtEvent → LivingDamageEvent → 死亡 → 掉落
 */
public class TrueDamageHelper {

    /** 正在处理真伤的实体（防止递归） */
    private static final Set<UUID> processingEntities = new HashSet<>();

    /** 标记当前是否在真伤处理中（供外部事件检查） */
    private static final ThreadLocal<Boolean> IN_TRUE_DAMAGE = ThreadLocal.withInitial(() -> false);

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
     * 创建绕过护甲的真伤伤害源
     */
    public static DamageSource createTrueDamageSource(@Nullable Entity source) {
        if (source instanceof EntityPlayer) {
            return new EntityDamageSource("moremod.true_damage", source)
                    .setDamageBypassesArmor()
                    .setDamageIsAbsolute();
        }
        return new DamageSource("moremod.true_damage")
                .setDamageBypassesArmor()
                .setDamageIsAbsolute();
    }

    /**
     * 应用包装真伤
     *
     * 通过重置无敌帧 + 使用绕过护甲的伤害源，走完整伤害流程
     * 这样 LivingHurtEvent, LivingDamageEvent, 死亡, 掉落都会正常触发
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
            IN_TRUE_DAMAGE.set(true);

            // 重置无敌帧，确保伤害能打进去
            target.hurtResistantTime = 0;

            // 使用绕过护甲的伤害源，走正常伤害流程
            DamageSource dmgSource = createTrueDamageSource(source);

            // 这会触发完整的伤害链：
            // attackEntityFrom → applyPotionDamageCalculations →
            // LivingHurtEvent → damageEntity → LivingDamageEvent →
            // 如果死亡 → onDeath → LivingDeathEvent → LivingDropsEvent
            boolean result = target.attackEntityFrom(dmgSource, trueDamage);

            return result;

        } finally {
            processingEntities.remove(targetId);
            IN_TRUE_DAMAGE.set(false);
        }
    }

    /**
     * 应用斩杀真伤（造成目标当前血量的伤害）
     */
    public static boolean applyExecuteDamage(EntityLivingBase target,
                                              @Nullable Entity source) {
        if (target == null || target.world.isRemote) return false;

        // 造成目标当前血量 + 10 的伤害，确保击杀
        float executeDamage = target.getHealth() + 10f;
        return applyWrappedTrueDamage(target, source, executeDamage, TrueDamageFlag.EXECUTE);
    }

    /**
     * 检查是否正在处理真伤（用于外部事件判断，防止递归）
     */
    public static boolean isProcessingTrueDamage(EntityLivingBase entity) {
        return processingEntities.contains(entity.getUniqueID());
    }

    /**
     * 检查当前线程是否在真伤处理中
     */
    public static boolean isInTrueDamageContext() {
        return IN_TRUE_DAMAGE.get();
    }

    /**
     * 检查伤害源是否是我们的真伤
     */
    public static boolean isTrueDamageSource(DamageSource source) {
        return source != null && "moremod.true_damage".equals(source.getDamageType());
    }

    /**
     * 计算无视护甲后的等效伤害
     * 用于模拟"忽略50%护甲"等效果（非真伤场景）
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
