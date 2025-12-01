package com.moremod.core;

import com.moremod.combat.TrueDamageHelper;
import com.moremod.config.ShambhalaConfig;
import com.moremod.system.ascension.ShambhalaEventHandler;
import com.moremod.system.ascension.ShambhalaHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.WorldServer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 香巴拉死亡钩子
 * Shambhala Death Hook
 *
 * 此类由 ASM Transformer 调用，提供能量护盾保护和真伤反伤：
 * 1. damageEntity HEAD - 消耗能量吸收 + 触发真伤反伤（使用原始伤害）
 * 2. onDeath HEAD - 最终防线，消耗能量阻止死亡
 *
 * 原始伤害捕获：由 ShambhalaEventHandler.onLivingAttack (Forge事件) 处理
 * - LivingAttackEvent 在护甲计算前触发，比 ASM 更简单
 *
 * 与破碎之神的区别：
 * - 破碎之神：停机模式，完全免疫
 * - 香巴拉：消耗能量抵挡，没能量则死亡，同时反伤攻击者
 */
public class ShambhalaDeathHook {

    /** 正在进行反伤的玩家（防止循环） */
    private static final Set<UUID> reflectingPlayers = new HashSet<>();

    // 原始伤害捕获已移至 ShambhalaEventHandler.onLivingAttack (Forge事件)
    // LivingAttackEvent 在护甲计算前触发，能拿到原始伤害

    // ========== Hook 2: damageEntity ==========

    /**
     * 检查并尝试用能量阻止致命伤害，同时触发真伤反伤
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

            // 跳过真伤（反伤使用真伤，不应被再次处理）
            if (TrueDamageHelper.isInTrueDamageContext()) {
                return false;
            }
            if (TrueDamageHelper.isTrueDamageSource(source)) {
                return false;
            }

            // 跳过香巴拉反伤伤害（虽然现在用真伤，但保留检查）
            if (ShambhalaHandler.isShambhalaReflectDamage(source)) {
                return false;
            }

            // ========== 触发反伤（使用原始伤害计算，护甲前） ==========
            // 原始伤害由 ShambhalaEventHandler.onLivingAttack 捕获
            float rawDamage = ShambhalaEventHandler.getAndClearCapturedRawDamage();
            Entity capturedAtt = ShambhalaEventHandler.getAndClearCapturedAttacker();

            // 使用原始伤害进行反伤计算
            // 如果没有捕获到原始伤害（可能是真伤等直接调用damageEntity的情况），回退到最终伤害
            float reflectBaseDamage = rawDamage > 0 ? rawDamage : damage;
            Entity attacker = capturedAtt != null ? capturedAtt : source.getTrueSource();

            if (attacker != null && attacker instanceof EntityLivingBase && reflectBaseDamage > 0) {
                triggerTrueDamageReflect(player, (EntityLivingBase) attacker, reflectBaseDamage);
            }

            // ========== 能量护盾吸收 ==========
            // 尝试用能量吸收伤害
            float remainingDamage = ShambhalaHandler.tryAbsorbDamage(player, damage);

            if (remainingDamage <= 0) {
                // 完全吸收
                return true;
            }

            // 检测致命伤害：当前血量 - 剩余伤害 <= 0
            float currentHealth = player.getHealth();
            if (currentHealth - remainingDamage <= 0) {
                // 致命伤害！尝试用能量阻止
                int energyCost = (int) (remainingDamage * ShambhalaConfig.energyPerDamage);

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

    // ========== 真伤反伤系统 ==========

    /**
     * 触发真伤反伤
     * 使用 TrueDamageHelper 造成真实伤害，绕过护甲和ASM吸收
     *
     * 新公式（比例反伤）：
     * 反伤 = (受到的伤害 / 玩家最大血量) * 攻击者最大血量
     * 例如：玩家有20血，受到10伤害（50%血量），攻击者有100血
     *       反伤 = (10/20) * 100 = 50 真实伤害
     *
     * @param player   香巴拉玩家
     * @param attacker 攻击者
     * @param damage   受到的原始伤害
     */
    public static void triggerTrueDamageReflect(EntityPlayer player, EntityLivingBase attacker, float damage) {
        UUID playerId = player.getUniqueID();

        System.out.println("[Shambhala TriggerReflect] Called with damage=" + damage + ", attacker=" + attacker.getName());

        // 循环防护
        if (reflectingPlayers.contains(playerId)) {
            System.out.println("[Shambhala TriggerReflect] Skipped: already reflecting");
            return;
        }

        // 不反伤自己
        if (attacker == player) {
            System.out.println("[Shambhala TriggerReflect] Skipped: self-damage");
            return;
        }

        reflectingPlayers.add(playerId);

        try {
            // ========== 新公式：比例反伤 ==========
            // (受到的伤害 / 玩家最大血量) * 攻击者最大血量
            float playerMaxHealth = player.getMaxHealth();
            if (playerMaxHealth <= 0) playerMaxHealth = 20.0F; // 防止除零

            float damageRatio = damage / playerMaxHealth;
            float reflectDamage = damageRatio * attacker.getMaxHealth();

            // 应用配置的倍率修正（可选，默认1.0则无效果）
            reflectDamage *= (float) ShambhalaConfig.thornsReflectMultiplier;

            double aoeRadius = ShambhalaConfig.thornsAoeRadius;

            // 计算能量消耗（基于受到的伤害，而非造成的反伤），应用能量上限
            // 这样面对高血量敌人时不会瞬间耗尽能量
            int baseCost = (int) (damage * ShambhalaConfig.energyPerReflect);
            baseCost = Math.min(baseCost, ShambhalaConfig.reflectEnergyCap);

            System.out.println("[Shambhala TriggerReflect] receivedDamage=" + damage + ", reflectDamage=" + reflectDamage + ", baseCost=" + baseCost + ", currentEnergy=" + ShambhalaHandler.getCurrentEnergy(player));

            // ========== 主目标反伤 ==========
            if (ShambhalaHandler.consumeEnergy(player, baseCost)) {
                System.out.println("[Shambhala TriggerReflect] Applying reflect damage to " + attacker.getName());
                applyTrueDamageReflect(player, attacker, reflectDamage);
            } else {
                // 没能量就不反伤
                System.out.println("[Shambhala TriggerReflect] Failed: not enough energy");
                return;
            }

            // ========== AoE 反伤 ==========
            if (aoeRadius > 0) {
                AxisAlignedBB aabb = new AxisAlignedBB(
                        attacker.posX - aoeRadius, attacker.posY - aoeRadius, attacker.posZ - aoeRadius,
                        attacker.posX + aoeRadius, attacker.posY + aoeRadius, attacker.posZ + aoeRadius
                );

                List<EntityLivingBase> nearbyMobs = player.world.getEntitiesWithinAABB(
                        EntityLivingBase.class, aabb,
                        e -> e != null && e != player && e != attacker && !(e instanceof EntityPlayer) && e.isEntityAlive()
                );

                // AoE也使用比例反伤，基于每个mob自己的血量
                // AoE能量消耗也基于受到的伤害（减半）
                int aoeCost = (int) (damage * ShambhalaConfig.energyPerReflect * 0.5f);
                aoeCost = Math.min(aoeCost, ShambhalaConfig.reflectEnergyCap);

                for (EntityLivingBase mob : nearbyMobs) {
                    float aoeReflectDamage = damageRatio * mob.getMaxHealth() * 0.5f; // AoE伤害减半
                    aoeReflectDamage *= (float) ShambhalaConfig.thornsReflectMultiplier;

                    if (ShambhalaHandler.consumeEnergy(player, aoeCost)) {
                        applyTrueDamageReflect(player, mob, aoeReflectDamage);
                    } else {
                        break; // 能量不足停止AoE
                    }
                }
            }

        } finally {
            reflectingPlayers.remove(playerId);
        }
    }

    /**
     * 对单个目标施加真伤反伤
     */
    private static void applyTrueDamageReflect(EntityPlayer player, EntityLivingBase target, float damage) {
        if (target == null || target.isDead) return;

        // 使用 TrueDamageHelper 造成真实伤害
        // 这会绕过护甲、绕过ASM钩子的吸收
        TrueDamageHelper.applyWrappedTrueDamage(
                target, player, damage,
                TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE
        );

        // 反伤粒子效果
        if (player.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) player.world;
            ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                    target.posX, target.posY + target.height / 2, target.posZ,
                    15, 0.5, 0.5, 0.5, 0.1);
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

    /**
     * 清理玩家状态（玩家退出时调用）
     */
    public static void cleanupPlayer(UUID playerId) {
        reflectingPlayers.remove(playerId);
    }

    /**
     * 清空所有状态（世界卸载时调用）
     */
    public static void clearAllState() {
        reflectingPlayers.clear();
    }
}
