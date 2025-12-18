package com.moremod.enchantment;

import com.moremod.init.ModEnchantments;
import com.moremod.util.combat.TrueDamageHelper;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EntityDamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 真伤附魔事件处理器
 * 在多个伤害事件节点监听，取最大伤害值转换为真伤
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class TrueDamageEnchantHandler {

    // 追踪每次攻击的伤害信息
    private static final Map<UUID, DamageTracker> damageTrackers = new WeakHashMap<>();

    // 标记正在处理真伤，防止递归
    private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);

    /**
     * 伤害追踪器 - 记录一次攻击过程中各阶段的伤害值
     */
    private static class DamageTracker {
        final EntityLivingBase attacker;
        final int enchantLevel;
        float attackDamage = 0;   // LivingAttackEvent 阶段
        float hurtDamage = 0;     // LivingHurtEvent 阶段
        float finalDamage = 0;    // LivingDamageEvent 阶段
        long timestamp;
        boolean processed = false;

        DamageTracker(EntityLivingBase attacker, int level) {
            this.attacker = attacker;
            this.enchantLevel = level;
            this.timestamp = System.currentTimeMillis();
        }

        float getMaxDamage() {
            return Math.max(attackDamage, Math.max(hurtDamage, finalDamage));
        }

        boolean isExpired() {
            // 500ms 超时
            return System.currentTimeMillis() - timestamp > 500;
        }
    }

    /**
     * 获取攻击者手持武器的真伤附魔等级
     */
    private static int getTrueDamageLevel(Entity attacker) {
        if (!(attacker instanceof EntityLivingBase)) return 0;
        if (ModEnchantments.TRUE_DAMAGE == null) return 0;

        ItemStack held = ((EntityLivingBase) attacker).getHeldItemMainhand();
        if (held.isEmpty()) return 0;

        return EnchantmentHelper.getEnchantmentLevel(ModEnchantments.TRUE_DAMAGE, held);
    }

    /**
     * 阶段1: LivingAttackEvent - 最早的伤害事件，记录原始伤害
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().world.isRemote) return;
        if (PROCESSING.get()) return;
        if (TrueDamageHelper.isInTrueDamageContext()) return;

        Entity source = event.getSource().getTrueSource();
        int level = getTrueDamageLevel(source);
        if (level <= 0) return;

        EntityLivingBase target = event.getEntityLiving();
        UUID targetId = target.getUniqueID();

        // 清理过期的追踪器
        damageTrackers.entrySet().removeIf(e -> e.getValue().isExpired());

        // 创建新的追踪器
        DamageTracker tracker = new DamageTracker((EntityLivingBase) source, level);
        tracker.attackDamage = event.getAmount();
        damageTrackers.put(targetId, tracker);
    }

    /**
     * 阶段2: LivingHurtEvent - 护甲计算前，记录伤害
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().world.isRemote) return;
        if (PROCESSING.get()) return;
        if (TrueDamageHelper.isInTrueDamageContext()) return;

        Entity source = event.getSource().getTrueSource();
        int level = getTrueDamageLevel(source);
        if (level <= 0) return;

        EntityLivingBase target = event.getEntityLiving();
        UUID targetId = target.getUniqueID();

        DamageTracker tracker = damageTrackers.get(targetId);
        if (tracker != null && !tracker.isExpired() && !tracker.processed) {
            tracker.hurtDamage = event.getAmount();
        }
    }

    /**
     * 阶段3: LivingDamageEvent - 最终伤害阶段，执行真伤转换
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().world.isRemote) return;
        if (PROCESSING.get()) return;
        if (TrueDamageHelper.isInTrueDamageContext()) return;

        // 已经是真伤，跳过
        if (TrueDamageHelper.isTrueDamageSource(event.getSource())) return;

        Entity source = event.getSource().getTrueSource();
        int level = getTrueDamageLevel(source);
        if (level <= 0) return;

        EntityLivingBase target = event.getEntityLiving();
        UUID targetId = target.getUniqueID();

        DamageTracker tracker = damageTrackers.get(targetId);
        if (tracker == null || tracker.isExpired() || tracker.processed) {
            // 没有追踪器，创建临时的
            tracker = new DamageTracker((EntityLivingBase) source, level);
            tracker.finalDamage = event.getAmount();
        } else {
            tracker.finalDamage = event.getAmount();
        }

        // 标记已处理
        tracker.processed = true;

        // 计算最大伤害值
        float maxDamage = tracker.getMaxDamage();
        if (maxDamage <= 0) return;

        // 获取真伤转换比例
        float ratio = EnchantmentTrueDamage.getTrueDamageRatio(tracker.enchantLevel);
        float trueDamageAmount = maxDamage * ratio;
        float normalDamageAmount = maxDamage * (1 - ratio);

        // 修改原伤害为剩余的普通伤害部分
        event.setAmount(normalDamageAmount);

        // 应用真伤部分
        if (trueDamageAmount > 0) {
            try {
                PROCESSING.set(true);
                TrueDamageHelper.applyWrappedTrueDamage(
                    target,
                    tracker.attacker,
                    trueDamageAmount,
                    TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE
                );
            } finally {
                PROCESSING.set(false);
            }
        }

        // 清理追踪器
        damageTrackers.remove(targetId);
    }

    /**
     * 备用监听：直接在 AttackEntityEvent 也做一次检测
     * 用于捕获一些特殊情况
     */
    @SubscribeEvent(priority = EventPriority.MONITOR)
    public static void onAttackEntity(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity().world.isRemote) return;
        if (event.isCanceled()) return;

        EntityPlayer player = event.getEntityPlayer();
        int level = getTrueDamageLevel(player);
        if (level <= 0) return;

        Entity target = event.getTarget();
        if (target instanceof EntityLivingBase) {
            UUID targetId = target.getUniqueID();
            // 预创建追踪器，确保后续事件能捕获
            if (!damageTrackers.containsKey(targetId)) {
                DamageTracker tracker = new DamageTracker(player, level);
                damageTrackers.put(targetId, tracker);
            }
        }
    }
}
