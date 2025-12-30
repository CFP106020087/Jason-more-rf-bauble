package com.moremod.enchantment;

import com.moremod.init.ModEnchantments;
import com.moremod.util.combat.TrueDamageHelper;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

/**
 * 真伤附魔事件处理器
 * 在攻击前清空目标护甲值，然后在多个伤害事件节点监听，取最大伤害值转换为真伤
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class TrueDamageEnchantHandler {

    // 追踪每次攻击的伤害信息
    private static final Map<UUID, DamageTracker> damageTrackers = new WeakHashMap<>();

    // 追踪被清空护甲的实体及其原始护甲值
    private static final Map<UUID, ArmorCache> armorCaches = new WeakHashMap<>();

    // 标记正在处理真伤，防止递归
    private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);

    // 护甲清空修改器的UUID
    private static final UUID ARMOR_CLEAR_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    /**
     * 护甲缓存 - 记录被清空护甲的实体
     */
    private static class ArmorCache {
        final double originalArmor;
        final double originalToughness;
        final long timestamp;
        boolean restored = false;

        ArmorCache(double armor, double toughness) {
            this.originalArmor = armor;
            this.originalToughness = toughness;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 1000; // 1秒超时
        }
    }

    /**
     * 伤害追踪器 - 记录一次攻击过程中各阶段的伤害值
     */
    private static class DamageTracker {
        final EntityLivingBase attacker;
        final int enchantLevel;
        float attackDamage = 0;
        float hurtDamage = 0;
        float finalDamage = 0;
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
            return System.currentTimeMillis() - timestamp > 500;
        }
    }

    /**
     * 获取攻击者手持物品的真伤附魔等级
     */
    private static int getTrueDamageLevel(Entity attacker) {
        if (!(attacker instanceof EntityLivingBase)) return 0;
        if (ModEnchantments.TRUE_DAMAGE == null) return 0;

        EntityLivingBase living = (EntityLivingBase) attacker;

        // 检查主手
        ItemStack mainHand = living.getHeldItemMainhand();
        int mainLevel = mainHand.isEmpty() ? 0 : EnchantmentHelper.getEnchantmentLevel(ModEnchantments.TRUE_DAMAGE, mainHand);

        // 检查副手
        ItemStack offHand = living.getHeldItemOffhand();
        int offLevel = offHand.isEmpty() ? 0 : EnchantmentHelper.getEnchantmentLevel(ModEnchantments.TRUE_DAMAGE, offHand);

        return Math.max(mainLevel, offLevel);
    }

    /**
     * 清空目标护甲值
     */
    private static void clearTargetArmor(EntityLivingBase target) {
        UUID targetId = target.getUniqueID();

        // 已经清空过了
        if (armorCaches.containsKey(targetId) && !armorCaches.get(targetId).restored) {
            return;
        }

        IAttributeInstance armorAttr = target.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        IAttributeInstance toughnessAttr = target.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);

        double currentArmor = armorAttr != null ? armorAttr.getAttributeValue() : 0;
        double currentToughness = toughnessAttr != null ? toughnessAttr.getAttributeValue() : 0;

        // 缓存原始护甲值
        armorCaches.put(targetId, new ArmorCache(currentArmor, currentToughness));

        // 移除旧的修改器（如果存在）
        if (armorAttr != null) {
            armorAttr.removeModifier(ARMOR_CLEAR_UUID);
            // 添加负护甲修改器，清空护甲
            if (currentArmor > 0) {
                armorAttr.applyModifier(new AttributeModifier(
                    ARMOR_CLEAR_UUID, "TrueDamage Armor Clear", -currentArmor, 0
                ));
            }
        }

        if (toughnessAttr != null) {
            toughnessAttr.removeModifier(ARMOR_CLEAR_UUID);
            // 清空护甲韧性
            if (currentToughness > 0) {
                toughnessAttr.applyModifier(new AttributeModifier(
                    ARMOR_CLEAR_UUID, "TrueDamage Toughness Clear", -currentToughness, 0
                ));
            }
        }
    }

    /**
     * 恢复目标护甲值
     */
    private static void restoreTargetArmor(EntityLivingBase target) {
        UUID targetId = target.getUniqueID();
        ArmorCache cache = armorCaches.get(targetId);

        if (cache == null || cache.restored) return;
        cache.restored = true;

        IAttributeInstance armorAttr = target.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        IAttributeInstance toughnessAttr = target.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);

        // 移除清空修改器，护甲自然恢复
        if (armorAttr != null) {
            armorAttr.removeModifier(ARMOR_CLEAR_UUID);
        }
        if (toughnessAttr != null) {
            toughnessAttr.removeModifier(ARMOR_CLEAR_UUID);
        }

        armorCaches.remove(targetId);
    }

    /**
     * 阶段1: LivingAttackEvent - 最早的伤害事件
     * 在这里清空目标护甲并记录原始伤害
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

        // 清理过期的追踪器和护甲缓存
        damageTrackers.entrySet().removeIf(e -> e.getValue().isExpired());
        armorCaches.entrySet().removeIf(e -> e.getValue().isExpired());

        // ★ 关键：在伤害计算前清空目标护甲
        clearTargetArmor(target);

        // 创建新的追踪器
        DamageTracker tracker = new DamageTracker((EntityLivingBase) source, level);
        tracker.attackDamage = event.getAmount();
        damageTrackers.put(targetId, tracker);
    }

    /**
     * 阶段2: LivingHurtEvent - 护甲计算阶段
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
     * 阶段3: LivingDamageEvent - 最终伤害阶段，执行真伤转换并恢复护甲
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().world.isRemote) return;
        if (PROCESSING.get()) return;
        if (TrueDamageHelper.isInTrueDamageContext()) return;
        if (TrueDamageHelper.isTrueDamageSource(event.getSource())) return;

        Entity source = event.getSource().getTrueSource();
        int level = getTrueDamageLevel(source);

        EntityLivingBase target = event.getEntityLiving();
        UUID targetId = target.getUniqueID();

        // ★ 无论如何都要恢复护甲
        restoreTargetArmor(target);

        if (level <= 0) return;

        DamageTracker tracker = damageTrackers.get(targetId);
        if (tracker == null || tracker.isExpired() || tracker.processed) {
            tracker = new DamageTracker((EntityLivingBase) source, level);
            tracker.finalDamage = event.getAmount();
        } else {
            tracker.finalDamage = event.getAmount();
        }

        tracker.processed = true;

        // 计算最大伤害值
        float maxDamage = tracker.getMaxDamage();
        if (maxDamage <= 0) {
            damageTrackers.remove(targetId);
            return;
        }

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

        damageTrackers.remove(targetId);
    }

    /**
     * 备用监听：AttackEntityEvent 预创建追踪器
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttackEntity(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity().world.isRemote) return;
        if (event.isCanceled()) return;

        EntityPlayer player = event.getEntityPlayer();
        int level = getTrueDamageLevel(player);
        if (level <= 0) return;

        Entity target = event.getTarget();
        if (target instanceof EntityLivingBase) {
            UUID targetId = target.getUniqueID();
            if (!damageTrackers.containsKey(targetId)) {
                DamageTracker tracker = new DamageTracker(player, level);
                damageTrackers.put(targetId, tracker);
            }
        }
    }
}
