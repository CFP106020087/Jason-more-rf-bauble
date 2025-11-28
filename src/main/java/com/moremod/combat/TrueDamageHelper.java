package com.moremod.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 高级真伤系统
 * Advanced True Damage System
 */
public class TrueDamageHelper {

    private static final Set<UUID> processingEntities = new HashSet<>();
    private static final ThreadLocal<Boolean> IN_TRUE_DAMAGE = ThreadLocal.withInitial(() -> false);

    public enum TrueDamageFlag {
        PHANTOM_TWIN,
        TERMINUS_BONUS,
        PHANTOM_STRIKE,
        EXECUTE
    }

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

    public static boolean applyWrappedTrueDamage(EntityLivingBase target,
                                                 @Nullable Entity source,
                                                 float trueDamage,
                                                 TrueDamageFlag flag) {
        if (target == null || target.world.isRemote) return false;
        if (trueDamage <= 0) return false;
        if (target.isDead) return false;

        UUID targetId = target.getUniqueID();

        if (processingEntities.contains(targetId)) {
            return false;
        }

        try {
            processingEntities.add(targetId);
            IN_TRUE_DAMAGE.set(true);

            DamageSource dmgSource = createTrueDamageSource(source);
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
     * - 使用 setHealth(0F) + onKillCommand() 触发完整死亡链
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
     * 攻击者归属（recentlyHit, attackingPlayer）已在调用方设置
     */
    private static void triggerVanillaDeathChain(EntityLivingBase victim, DamageSource source) {
        if (victim.isDead) return;

        // 设置血量为 0，然后让 onKillCommand 触发完整的 vanilla 死亡链
        victim.setHealth(0F);
        victim.onKillCommand();
    }

    public static boolean applyExecuteDamage(EntityLivingBase target, @Nullable Entity source) {
        if (target == null || target.world.isRemote) return false;
        float executeDamage = target.getHealth() + 100f;
        return applyWrappedTrueDamage(target, source, executeDamage, TrueDamageFlag.EXECUTE);
    }

    public static boolean isProcessingTrueDamage(EntityLivingBase entity) {
        return processingEntities.contains(entity.getUniqueID());
    }

    public static boolean isInTrueDamageContext() {
        return IN_TRUE_DAMAGE.get();
    }

    public static boolean isTrueDamageSource(DamageSource source) {
        return source != null && "moremod.true_damage".equals(source.getDamageType());
    }

    public static float calculateArmorBypassDamage(float baseDamage, float armorIgnorePercent) {
        float bonusMultiplier = 1.0f + (armorIgnorePercent * 0.4f);
        return baseDamage * bonusMultiplier;
    }
}
