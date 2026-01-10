package com.moremod.util.combat;

import com.moremod.core.ScriptDeathHook;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.curse.ItemScriptOfFifthAct;
import com.moremod.util.BaublesCompatibility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 高级真伤系统
 */
public class TrueDamageHelper {

    private static final Set<UUID> processingEntities = new HashSet<>();
    private static final ThreadLocal<Boolean> IN_TRUE_DAMAGE = ThreadLocal.withInitial(() -> false);

    // ========== 反射字段 ==========
    private static final Field recentlyHitField;
    private static final Field attackingPlayerField;
    private static final Field idleTimeField;
    private static final Field lastDamageField;
    private static final Field lastDamageSourceField;
    private static final Field lastDamageStampField;

    static {
        try {
            recentlyHitField = ReflectionHelper.findField(EntityLivingBase.class, "recentlyHit", "field_70718_bc");
            attackingPlayerField = ReflectionHelper.findField(EntityLivingBase.class, "attackingPlayer", "field_70717_bb");
            idleTimeField = ReflectionHelper.findField(EntityLivingBase.class, "idleTime", "field_70708_bq");
            lastDamageField = ReflectionHelper.findField(EntityLivingBase.class, "lastDamage", "field_110153_bc");
            lastDamageSourceField = ReflectionHelper.findField(EntityLivingBase.class, "lastDamageSource", "field_189750_bF");
            lastDamageStampField = ReflectionHelper.findField(EntityLivingBase.class, "lastDamageStamp", "field_189751_bG");
        } catch (Exception e) {
            throw new RuntimeException("TrueDamageHelper: Failed to initialize reflection fields", e);
        }
    }

    // ========== 反射工具方法 ==========
    private static void setRecentlyHit(EntityLivingBase entity, int value) {
        try { recentlyHitField.setInt(entity, value); } catch (Exception ignored) {}
    }

    private static void setAttackingPlayer(EntityLivingBase entity, EntityPlayer player) {
        try { attackingPlayerField.set(entity, player); } catch (Exception ignored) {}
    }

    private static void setIdleTime(EntityLivingBase entity, int value) {
        try { idleTimeField.setInt(entity, value); } catch (Exception ignored) {}
    }

    private static void setLastDamage(EntityLivingBase entity, float value) {
        try { lastDamageField.setFloat(entity, value); } catch (Exception ignored) {}
    }

    private static void setLastDamageSource(EntityLivingBase entity, DamageSource source) {
        try { lastDamageSourceField.set(entity, source); } catch (Exception ignored) {}
    }

    private static void setLastDamageStamp(EntityLivingBase entity, long stamp) {
        try { lastDamageStampField.setLong(entity, stamp); } catch (Exception ignored) {}
    }

    // ========== 原有逻辑 ==========

    public enum TrueDamageFlag {
        PHANTOM_TWIN,
        TERMINUS_BONUS,
        PHANTOM_STRIKE,
        EXECUTE,
        REFLECT,           // 反伤/反弹
        THORN_BURST,       // 荆棘爆发（七咒）
        SCRIPT_SETTLE      // 剧本结算（七咒）
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

        // ★ 机械核心佩戴者免疫真实伤害 ★
        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) target;
            if (hasMechanicalCoreEquipped(player)) {
                // 机械核心免疫真实伤害，不消耗能量，完全免疫
                return false;
            }
        }

        // ★ 第五幕剧本：真伤缓存到剧本 ★
        // 注意：SCRIPT_SETTLE 和 PHANTOM_STRIKE 是剧本自己的结算伤害，不应被缓存
        if (target instanceof EntityPlayer && flag != TrueDamageFlag.SCRIPT_SETTLE && flag != TrueDamageFlag.PHANTOM_STRIKE) {
            EntityPlayer player = (EntityPlayer) target;
            if (tryBufferToScript(player, trueDamage)) {
                // 伤害已被剧本缓存，不实际造成
                return true;
            }
        }

        UUID targetId = target.getUniqueID();
        if (processingEntities.contains(targetId)) return false;

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
     * 尝试将伤害缓存到第五幕剧本
     * @return true 如果伤害被缓存，false 如果玩家没有剧本或不应缓存
     */
    private static boolean tryBufferToScript(EntityPlayer player, float damage) {
        try {
            // 检查是否佩戴剧本
            if (!ItemScriptOfFifthAct.hasScript(player)) {
                return false;
            }

            // 检查是否在落幕状态（落幕状态不缓存，直接受伤）
            if (ItemScriptOfFifthAct.isInCurtainFall(player)) {
                return false;
            }

            // 获取剧本数据
            ItemScriptOfFifthAct.ScriptData data = ItemScriptOfFifthAct.getScriptData(player);
            if (data == null || data.isSettling) {
                return false;
            }

            // 缓存伤害
            data.addDamage(damage);
            data.recordCombat(player.world.getTotalWorldTime());

            return true;
        } catch (Exception e) {
            // 如果出现任何错误，回退到正常伤害处理
            return false;
        }
    }

    /**
     * 检查玩家是否佩戴了机械核心
     */
    private static boolean hasMechanicalCoreEquipped(EntityPlayer player) {
        try {
            return BaublesCompatibility.hasEquippedBauble(player, ItemMechanicalCore.class);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean doWrappedHurt(EntityLivingBase victim, @Nullable Entity attacker,
                                         DamageSource source, float amount) {
        if (victim.world.isRemote || victim.isDead) return false;

        // 1. 唤醒睡眠玩家
        if (victim.isPlayerSleeping() && victim instanceof EntityPlayer) {
            ((EntityPlayer) victim).wakeUpPlayer(true, true, false);
        }

        // 2. 设置攻击者归属
        if (attacker != null) {
            if (attacker instanceof EntityLivingBase) {
                victim.setRevengeTarget((EntityLivingBase) attacker);
            }
            if (attacker instanceof EntityPlayer) {
                setRecentlyHit(victim, 100);
                setAttackingPlayer(victim, (EntityPlayer) attacker);
            }
        }

        // 3. 设置战斗相关字段
        setIdleTime(victim, 0);
        setLastDamage(victim, amount);
        victim.hurtResistantTime = victim.maxHurtResistantTime;

        // 受击动画
        victim.maxHurtTime = 10;
        victim.hurtTime = victim.maxHurtTime;

        // 4. 记录到战斗追踪器
        float healthBefore = victim.getHealth();
        victim.getCombatTracker().trackDamage(source, healthBefore, amount);

        // 5. 计算新血量
        float newHealth = healthBefore - amount;

        // 6. 击退效果
        if (attacker != null) {
            double dx = attacker.posX - victim.posX;
            double dz = attacker.posZ - victim.posZ;
            while (dx * dx + dz * dz < 1.0E-4D) {
                dx = (Math.random() - Math.random()) * 0.01D;
                dz = (Math.random() - Math.random()) * 0.01D;
            }
            victim.knockBack(attacker, 0.4F, dx, dz);
        }

        // 7. 播放受击音效
        victim.playSound(SoundEvents.ENTITY_GENERIC_HURT, 1.0F,
                (victim.world.rand.nextFloat() - victim.world.rand.nextFloat()) * 0.2F + 1.0F);

        // 8. 记录最后伤害源
        setLastDamageSource(victim, source);
        setLastDamageStamp(victim, victim.world.getTotalWorldTime());

        // 9. 应用伤害并处理死亡
        if (newHealth <= 0) {
            // ★ 第五幕剧本：落幕机制（真伤绕过ASM，需要手动检查）
            if (victim instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) victim;
                // 调用 ScriptDeathHook 检查是否应该拦截死亡
                if (ScriptDeathHook.shouldPreventDeath(player, source)) {
                    // 死亡被拦截，已进入落幕状态，不执行死亡链
                    return true;
                }
            }

            // ★ 使用带攻击者信息的伤害源，确保掠夺/抢夺等附魔正常工作
            triggerVanillaDeathChain(victim, source);
        } else {
            victim.setHealth(newHealth);
        }

        return true;
    }

    /**
     * 触发原版死亡链：直接杀死目标
     * 用于斩杀/处决类技能，绕过所有伤害计算
     */
    public static void triggerVanillaDeathChain(EntityLivingBase victim) {
        triggerVanillaDeathChain(victim, DamageSource.OUT_OF_WORLD);
    }

    /**
     * 触发原版死亡链：使用指定的伤害源
     * 可保留自定义死亡消息，同时正确触发掉落物和死亡动画
     *
     * 注意：不要手动设置 isDead = true！
     * 这会阻止实体继续更新，导致 deathTime 无法递增，死亡动画无法播放。
     * 让实体自然完成死亡过程（约20 tick后自动设置isDead）
     */
    public static void triggerVanillaDeathChain(EntityLivingBase victim, DamageSource source) {
        if (victim == null || victim.isDead || victim.world.isRemote) return;

        // ★ 机械核心佩戴者免疫斩杀/处决 ★
        if (victim instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) victim;
            if (hasMechanicalCoreEquipped(player)) {
                // 机械核心免疫真实伤害的斩杀/处决
                return;
            }

            // ★ 第五幕剧本：落幕机制 ★
            if (ScriptDeathHook.shouldPreventDeath(player, source)) {
                // 死亡被拦截，已进入落幕状态
                return;
            }
        }

        // 设置血量为0
        victim.setHealth(0F);

        // 调用原版死亡流程（会触发掉落、经验、死亡消息、setEntityState(3)等）
        victim.onDeath(source);

        // 不要设置 isDead = true！让实体自然完成死亡动画
        // 实体的 onLivingUpdate 会递增 deathTime，约20 tick后自动标记为 isDead
        // 如果 onDeath 没有正确设置死亡状态，额外触发死亡动画
        if (!victim.isDead && victim.deathTime == 0) {
            // 设置 deathTime 启动死亡动画计时
            victim.deathTime = 1;
        }
    }

    public static boolean applyExecuteDamage(EntityLivingBase target, @Nullable Entity source) {
        if (target == null || target.world.isRemote) return false;
        return applyWrappedTrueDamage(target, source, target.getHealth() + 100f, TrueDamageFlag.EXECUTE);
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
        return baseDamage * (1.0f + (armorIgnorePercent * 0.4f));
    }
}