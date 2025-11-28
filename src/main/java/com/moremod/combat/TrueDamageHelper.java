package com.moremod.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.CombatTracker;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 高级真伤系统
 * Advanced True Damage System
 *
 * 参考 Avaritia 的实现，使用 setHealth 但完整包装战斗流程
 * 确保：受击动画、音效、战斗记录、死亡、掉落物 都正常触发
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
     * 创建真伤伤害源
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
     * 应用包装真伤 - 完整战斗流程
     *
     * 参考 Avaritia 的实现，手动设置所有战斗相关字段
     * 确保受击动画、音效、战斗记录、死亡、掉落物都正常
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
        if (target.isDead) return false;

        UUID targetId = target.getUniqueID();

        // 防止递归
        if (processingEntities.contains(targetId)) {
            return false;
        }

        try {
            processingEntities.add(targetId);
            IN_TRUE_DAMAGE.set(true);

            DamageSource dmgSource = createTrueDamageSource(source);

            // 执行包装的伤害
            return doWrappedHurt(target, source, dmgSource, trueDamage);

        } finally {
            processingEntities.remove(targetId);
            IN_TRUE_DAMAGE.set(false);
        }
    }

    /**
     * 包装的伤害处理 - 模拟完整战斗流程
     *
     * 严格遵守 Minecraft 1.12.2 死亡流程：
     * - 不调用 attackEntityFrom()
     * - 不调用两次 onDeath()
     * - 不设置 victim.dead = true（由 vanilla 自己设）
     * - 使用 setHealth(0F) + deathTime = 19 + onDeath() 触发完整死亡链
     */
    private static boolean doWrappedHurt(EntityLivingBase victim, @Nullable Entity attacker,
                                          DamageSource source, float amount) {
        if (victim.world.isRemote) return false;
        if (victim.isDead) return false;

        // ========== 1. 唤醒睡眠中的玩家 ==========
        if (victim.isPlayerSleeping() && victim instanceof EntityPlayer) {
            ((EntityPlayer) victim).wakeUpPlayer(true, true, false);
        }

        // ========== 2. 设置攻击者归属（关键：确保掉落物、经验、统计正常） ==========
        if (attacker != null) {
            if (attacker instanceof EntityLivingBase) {
                victim.setRevengeTarget((EntityLivingBase) attacker);
            }
            if (attacker instanceof EntityPlayer) {
                victim.recentlyHit = 100;  // 必须设置，否则没有玩家击杀掉落和经验
                victim.attackingPlayer = (EntityPlayer) attacker;
            }
        }

        // ========== 3. 设置战斗相关字段 ==========
        victim.idleTime = 0;
        victim.lastDamage = amount;
        victim.hurtResistantTime = victim.maxHurtResistantTime;

        // 设置受击动画
        victim.maxHurtTime = 10;
        victim.hurtTime = victim.maxHurtTime;

        // ========== 4. 记录到战斗追踪器（用于死亡信息） ==========
        float healthBefore = victim.getHealth();
        victim.getCombatTracker().trackDamage(source, healthBefore, amount);

        // ========== 5. 计算新血量 ==========
        float newHealth = healthBefore - amount;

        // ========== 6. 击退效果 ==========
        if (attacker != null) {
            double dx = attacker.posX - victim.posX;
            double dz = attacker.posZ - victim.posZ;
            while (dx * dx + dz * dz < 1.0E-4D) {
                dx = (Math.random() - Math.random()) * 0.01D;
                dz = (Math.random() - Math.random()) * 0.01D;
            }
            victim.knockBack(attacker, 0.4F, dx, dz);
        }

        // ========== 7. 播放受击音效 ==========
        victim.playSound(SoundEvents.ENTITY_GENERIC_HURT, 1.0F,
                (victim.world.rand.nextFloat() - victim.world.rand.nextFloat()) * 0.2F + 1.0F);

        // ========== 8. 记录最后伤害源 ==========
        victim.lastDamageSource = source;
        victim.lastDamageStamp = victim.world.getTotalWorldTime();

        // ========== 9. 应用伤害并处理死亡 ==========
        if (newHealth <= 0) {
            // 触发完整的 vanilla 死亡链
            triggerVanillaDeathChain(victim, source);
        } else {
            // 普通伤害，只设置血量
            victim.setHealth(newHealth);
        }

        return true;
    }

    /**
     * 触发完整的 vanilla 死亡链
     *
     * 严格遵守要求：
     * ✔ setHealth(0F)
     * ✔ 设置 deathTime = 19（下一 tick 自动进入死亡完成阶段）
     * ✔ 调用 onDeath() 触发 LivingDeathEvent / LivingDropsEvent
     * ✔ 不调用 attackEntityFrom()
     * ✔ 不调用两次 onDeath()
     * ✔ 不设置 victim.dead = true（vanilla 在 onDeath 末尾自己设）
     * ✔ CombatTracker 已在调用方设置
     * ✔ 攻击者归属已在调用方设置
     */
    private static void triggerVanillaDeathChain(EntityLivingBase victim, DamageSource source) {
        if (victim.isDead) return;

        // ========== 1. 设置生命值为 0 ==========
        victim.setHealth(0F);

        // ========== 2. 设置 deathTime = 19 ==========
        // 下一 tick deathTime 会变成 20，触发 onDeathUpdate 中的实体移除逻辑
        victim.deathTime = 19;

        // ========== 3. 重新检查战斗追踪器状态 ==========
        victim.getCombatTracker().recheckStatus();

        // ========== 4. 调用 onDeath() 触发完整死亡事件链 ==========
        // onDeath() 内部会：
        //   - 触发 ForgeHooks.onLivingDeath() -> LivingDeathEvent
        //   - 调用 dropLoot() -> LivingDropsEvent
        //   - 调用 dropEquipment()
        //   - 在末尾设置 dead = true
        // 不要手动设置 dead = true，让 vanilla 自己处理
        victim.onDeath(source);
    }

    /**
     * 应用斩杀真伤（直接将目标生命归零）
     */
    public static boolean applyExecuteDamage(EntityLivingBase target,
                                              @Nullable Entity source) {
        if (target == null || target.world.isRemote) return false;

        // 造成目标当前血量 + 100 的伤害，确保击杀
        float executeDamage = target.getHealth() + 100f;
        return applyWrappedTrueDamage(target, source, executeDamage, TrueDamageFlag.EXECUTE);
    }

    /**
     * 检查是否正在处理真伤
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
     */
    public static float calculateArmorBypassDamage(float baseDamage, float armorIgnorePercent) {
        float bonusMultiplier = 1.0f + (armorIgnorePercent * 0.4f);
        return baseDamage * bonusMultiplier;
    }
}
