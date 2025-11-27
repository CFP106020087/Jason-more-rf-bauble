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
import net.minecraft.util.SoundEvent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    // 反射字段缓存
    private static Field idleTimeField;
    private static Field lastDamageField;
    private static Field recentlyHitField;
    private static Field attackingPlayerField;
    private static Field lastDamageSourceField;
    private static Field lastDamageStampField;
    private static Field scoreValueField;
    private static Field deadField;
    private static Method getDeathSoundMethod;

    static {
        try {
            // EntityLivingBase 字段 - 使用 SRG 名称
            idleTimeField = ObfuscationReflectionHelper.findField(EntityLivingBase.class, "field_70721_aZ"); // idleTime
            lastDamageField = ObfuscationReflectionHelper.findField(EntityLivingBase.class, "field_110153_bc"); // lastDamage
            recentlyHitField = ObfuscationReflectionHelper.findField(EntityLivingBase.class, "field_70718_bc"); // recentlyHit
            attackingPlayerField = ObfuscationReflectionHelper.findField(EntityLivingBase.class, "field_70717_bb"); // attackingPlayer
            lastDamageSourceField = ObfuscationReflectionHelper.findField(EntityLivingBase.class, "field_189750_bF"); // lastDamageSource
            lastDamageStampField = ObfuscationReflectionHelper.findField(EntityLivingBase.class, "field_189751_bG"); // lastDamageStamp
            scoreValueField = ObfuscationReflectionHelper.findField(EntityLivingBase.class, "field_70744_aE"); // scoreValue
            deadField = ObfuscationReflectionHelper.findField(EntityLivingBase.class, "field_70729_aU"); // dead

            // getDeathSound 方法
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    private static boolean doWrappedHurt(EntityLivingBase victim, @Nullable Entity attacker,
                                         DamageSource source, float amount) {
        if (victim.world.isRemote) return false;
        if (victim.isDead) return false;

        if (victim.isPlayerSleeping() && victim instanceof EntityPlayer) {
            ((EntityPlayer) victim).wakeUpPlayer(true, true, false);
        }

        try {
            // 设置 idleTime
            if (idleTimeField != null) {
                idleTimeField.setInt(victim, 0);
            }

            // 设置 lastDamage
            if (lastDamageField != null) {
                lastDamageField.setFloat(victim, amount);
            }

            // 设置无敌帧
            victim.hurtResistantTime = victim.maxHurtResistantTime;

            // 记录到战斗追踪器
            CombatTracker tracker = victim.getCombatTracker();
            tracker.trackDamage(source, victim.getHealth(), amount);

            // 设置受击时间
            victim.maxHurtTime = 10;
            victim.hurtTime = victim.maxHurtTime;

            // 应用伤害
            float newHealth = victim.getHealth() - amount;
            victim.setHealth(newHealth);

            // 设置攻击者相关
            if (attacker != null) {
                if (attacker instanceof EntityLivingBase) {
                    victim.setRevengeTarget((EntityLivingBase) attacker);
                }

                if (attacker instanceof EntityPlayer) {
                    if (recentlyHitField != null) {
                        recentlyHitField.setInt(victim, 100);
                    }
                    if (attackingPlayerField != null) {
                        attackingPlayerField.set(victim, attacker);
                    }
                }
            }

            // 击退效果
            if (attacker != null) {
                double dx = attacker.posX - victim.posX;
                double dz = attacker.posZ - victim.posZ;

                while (dx * dx + dz * dz < 1.0E-4D) {
                    dx = (Math.random() - Math.random()) * 0.01D;
                    dz = (Math.random() - Math.random()) * 0.01D;
                }

                victim.knockBack(attacker, 0.4F, dx, dz);
            }

            // 播放受击音效
            victim.playSound(SoundEvents.ENTITY_GENERIC_HURT, 1.0F,
                    (victim.world.rand.nextFloat() - victim.world.rand.nextFloat()) * 0.2F + 1.0F);

            // 检查死亡
            if (victim.getHealth() <= 0) {
                doWrappedDeath(victim, source);
            }

            // 记录最后伤害源
            if (lastDamageSourceField != null) {
                lastDamageSourceField.set(victim, source);
            }
            if (lastDamageStampField != null) {
                lastDamageStampField.setLong(victim, victim.world.getTotalWorldTime());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static void doWrappedDeath(EntityLivingBase victim, DamageSource source) {
        if (victim.isDead) return;

        try {
            EntityLivingBase killer = victim.getCombatTracker().getBestAttacker();

            // 授予击杀分数
            if (scoreValueField != null && killer != null) {
                int scoreValue = scoreValueField.getInt(victim);
                if (scoreValue >= 0) {
                    killer.awardKillScore(victim, scoreValue, source);
                }
            }

            if (victim.isPlayerSleeping() && victim instanceof EntityPlayer) {
                ((EntityPlayer) victim).wakeUpPlayer(true, true, false);
            }

            // 标记为死亡
            if (deadField != null) {
                deadField.setBoolean(victim, true);
            }

            // CombatTracker.recheckStatus() 在 1.12.2 中不存在
            // 战斗追踪器会在 onDeath 中自动处理

            // 触发 onDeath 确保掉落物
            victim.onDeath(source);

            // 播放死亡音效
            if (victim.world instanceof WorldServer && getDeathSoundMethod != null) {
                SoundEvent deathSound = (SoundEvent) getDeathSoundMethod.invoke(victim);
                if (deathSound != null) {
                    ((WorldServer) victim.world).playSound(null, victim.posX, victim.posY, victim.posZ,
                            deathSound, SoundCategory.HOSTILE, 1.0F, 1.0F);
                }
            }
// 标准化进入死亡流程
            victim.setHealth(0F);
            victim.deathTime = 19;

// 触发完整掉落链
            victim.onKillCommand();

        } catch (Exception e) {
            e.printStackTrace();
        }
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