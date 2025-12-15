package com.moremod.sponsor.util;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 真实伤害助手
 *
 * 在 LivingAttackEvent, LivingHurtEvent, LivingDamageEvent 三个阶段
 * 监听伤害值，取最大值后作为真实伤害应用
 *
 * 真实伤害特性：
 * - 无视护甲
 * - 无视附魔保护
 * - 无视药水效果减伤
 * - 无法被格挡
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class TrueDamageHelper {

    // 标记正在处理真伤的实体，防止递归
    private static final Map<Integer, Boolean> PROCESSING_TRUE_DAMAGE = new ConcurrentHashMap<>();

    // 待处理的真伤数据：entityId -> {attackerId, maxDamage, timestamp}
    private static final Map<Integer, TrueDamageData> PENDING_TRUE_DAMAGE = new ConcurrentHashMap<>();

    // 真伤标记，用于识别我们发出的伤害
    private static final String TRUE_DAMAGE_TAG = "zhuxian_true_damage";

    // 自定义真伤类型
    public static final DamageSource TRUE_DAMAGE = new DamageSource(TRUE_DAMAGE_TAG)
            .setDamageBypassesArmor()
            .setDamageIsAbsolute();

    /**
     * 真伤数据类
     */
    private static class TrueDamageData {
        int attackerId;
        float maxDamage;
        long timestamp;
        float attackDamage;
        float hurtDamage;
        float finalDamage;

        TrueDamageData(int attackerId, float initialDamage) {
            this.attackerId = attackerId;
            this.maxDamage = initialDamage;
            this.timestamp = System.currentTimeMillis();
            this.attackDamage = initialDamage;
            this.hurtDamage = 0;
            this.finalDamage = 0;
        }

        void updateMax(float damage) {
            if (damage > maxDamage) {
                maxDamage = damage;
            }
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 100; // 100ms超时
        }
    }

    /**
     * 对目标造成真实伤害
     *
     * @param attacker 攻击者
     * @param target 目标
     * @param damage 伤害值
     */
    public static void dealTrueDamage(EntityLivingBase attacker, EntityLivingBase target, float damage) {
        if (target == null || target.isDead || damage <= 0) return;
        if (target.world.isRemote) return;

        int targetId = target.getEntityId();

        // 防止递归
        if (PROCESSING_TRUE_DAMAGE.getOrDefault(targetId, false)) {
            return;
        }

        try {
            PROCESSING_TRUE_DAMAGE.put(targetId, true);

            // 直接设置生命值，绕过所有伤害计算
            float newHealth = target.getHealth() - damage;

            if (newHealth <= 0) {
                // 使用伤害源杀死目标（正确计算击杀归属）
                DamageSource source = attacker != null ?
                    DamageSource.causePlayerDamage((EntityPlayer) attacker) : TRUE_DAMAGE;
                target.attackEntityFrom(source, Float.MAX_VALUE);
            } else {
                target.setHealth(newHealth);
                // 触发受伤效果
                target.hurtTime = 10;
                target.hurtResistantTime = 0; // 无视无敌帧
            }

        } finally {
            PROCESSING_TRUE_DAMAGE.remove(targetId);
        }
    }

    /**
     * 开始追踪一次攻击的伤害（用于三阶段监听）
     *
     * @param attacker 攻击者
     * @param target 目标
     * @param baseDamage 基础伤害
     * @return 追踪ID
     */
    public static int startDamageTracking(EntityLivingBase attacker, EntityLivingBase target, float baseDamage) {
        int targetId = target.getEntityId();
        int attackerId = attacker != null ? attacker.getEntityId() : -1;
        PENDING_TRUE_DAMAGE.put(targetId, new TrueDamageData(attackerId, baseDamage));
        return targetId;
    }

    /**
     * 完成追踪并应用真伤（取三阶段最大值）
     *
     * @param target 目标
     * @param attacker 攻击者
     */
    public static void finalizeTrueDamage(EntityLivingBase target, EntityLivingBase attacker) {
        int targetId = target.getEntityId();
        TrueDamageData data = PENDING_TRUE_DAMAGE.remove(targetId);

        if (data != null && !data.isExpired()) {
            dealTrueDamage(attacker, target, data.maxDamage);
        }
    }

    /**
     * 更新追踪的伤害值
     */
    public static void updateTrackedDamage(int targetId, float damage, String phase) {
        TrueDamageData data = PENDING_TRUE_DAMAGE.get(targetId);
        if (data != null) {
            switch (phase) {
                case "attack":
                    data.attackDamage = damage;
                    break;
                case "hurt":
                    data.hurtDamage = damage;
                    break;
                case "damage":
                    data.finalDamage = damage;
                    break;
            }
            data.updateMax(damage);
        }
    }

    /**
     * 检查伤害源是否是真伤
     */
    public static boolean isTrueDamage(DamageSource source) {
        return source != null && TRUE_DAMAGE_TAG.equals(source.getDamageType());
    }

    /**
     * 对目标造成百分比最大生命真伤
     *
     * @param attacker 攻击者
     * @param target 目标
     * @param percent 百分比 (0.0-1.0)
     */
    public static void dealPercentTrueDamage(EntityLivingBase attacker, EntityLivingBase target, float percent) {
        if (target == null) return;
        float damage = target.getMaxHealth() * percent;
        dealTrueDamage(attacker, target, damage);
    }

    /**
     * 对范围内的敌对生物造成真伤
     *
     * @param attacker 攻击者
     * @param center 中心位置
     * @param radius 半径
     * @param damage 伤害值
     * @param excludeTarget 排除的目标（可为null）
     */
    public static void dealAoeTrueDamage(EntityLivingBase attacker, EntityLivingBase center,
                                          double radius, float damage, EntityLivingBase excludeTarget) {
        if (center == null || center.world.isRemote) return;

        center.world.getEntitiesWithinAABB(EntityLivingBase.class,
            center.getEntityBoundingBox().grow(radius),
            entity -> entity != attacker && entity != excludeTarget && !entity.isDead
        ).forEach(entity -> {
            double dist = entity.getDistance(center);
            if (dist <= radius) {
                dealTrueDamage(attacker, entity, damage);
            }
        });
    }

    /**
     * 清理过期数据
     */
    public static void cleanup() {
        PENDING_TRUE_DAMAGE.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
