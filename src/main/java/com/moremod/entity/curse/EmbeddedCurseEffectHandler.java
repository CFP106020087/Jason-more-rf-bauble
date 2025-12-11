package com.moremod.entity.curse;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.moremod.core.CurseDeathHook;
import com.moremod.entity.curse.EmbeddedCurseManager.EmbeddedRelicType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 嵌入遗物效果处理器
 *
 * 七咒之戒的七项诅咒与对应的七圣遗物：
 *
 * 1. 受到伤害加倍 → 圣光之心 (SACRED_HEART) 抵消
 * 2. 中立生物主动攻击 → 和平徽章 (PEACE_EMBLEM) 抵消
 * 3. 护甲效力降低30% → 守护鳞片 (GUARDIAN_SCALE) 抵消
 * 4. 对怪物伤害降低50% → 勇气之刃 (COURAGE_BLADE) 抵消
 * 5. 着火永燃 → 霜华之露 (FROST_DEW) 抵消
 * 6. 死亡灵魂破碎 → 灵魂锚点 (SOUL_ANCHOR) 抵消
 * 7. 失眠症 → 安眠香囊 (SLUMBER_SACHET) 抵消
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class EmbeddedCurseEffectHandler {

    // ========== 反射字段：访问 Entity.fire ==========
    private static final Field FIRE_FIELD;
    // ========== 反射字段：访问 EntityPlayer.sleepTimer ==========
    private static final Field SLEEP_TIMER_FIELD;

    static {
        // "field_190534_ay" 是 Entity.fire 的 SRG 混淆名
        FIRE_FIELD = ObfuscationReflectionHelper.findField(Entity.class, "field_190534_ay");
        // "field_71076_b" 是 EntityPlayer.sleepTimer 的 SRG 混淆名
        SLEEP_TIMER_FIELD = ObfuscationReflectionHelper.findField(EntityPlayer.class, "field_71076_b");
    }

    /**
     * 获取实体的 fire ticks
     */
    private static int getFireTicks(Entity entity) {
        try {
            return FIRE_FIELD.getInt(entity);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 设置实体的 fire ticks
     */
    private static void setFireTicks(Entity entity, int ticks) {
        try {
            FIRE_FIELD.setInt(entity, ticks);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取玩家的 sleepTimer
     */
    private static int getSleepTimer(EntityPlayer player) {
        try {
            return SLEEP_TIMER_FIELD.getInt(player);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 设置玩家的 sleepTimer
     */
    private static void setSleepTimer(EntityPlayer player, int ticks) {
        try {
            SLEEP_TIMER_FIELD.setInt(player, ticks);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    // ========== 1. 受到伤害加倍 → 圣光之心祝福 ==========
    // 原诅咒：受到伤害加倍
    // 祝福效果：受到伤害减少25%（抵消诅咒后额外减伤）

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查是否嵌入了圣光之心
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SACRED_HEART)) {
            // 七咒会让伤害翻倍（×2），嵌入后转化为祝福
            // 祝福：不仅抵消翻倍，还额外减伤25%
            // 原伤害 × 2（诅咒）× 0.5（抵消）× 0.75（祝福）= 原伤害 × 0.375
            // 相当于 62.5% 减伤
            float currentDamage = event.getAmount();
            event.setAmount(currentDamage * 0.375f);
        }
    }

    // ========== 2. 中立生物主动攻击 → 和平徽章抵消 ==========

    // 用于追踪需要清除攻击目标的生物
    private static final Map<Integer, UUID> pendingTargetClear = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSetAttackTarget(LivingSetAttackTargetEvent event) {
        if (!(event.getTarget() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getTarget();

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查是否嵌入了和平徽章
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.PEACE_EMBLEM)) {
            // 如果是中立生物（非敌对怪物），立即清除攻击目标
            if (!(event.getEntityLiving() instanceof EntityMob)) {
                // 立即清除攻击目标
                if (event.getEntityLiving() instanceof EntityCreature) {
                    ((EntityCreature) event.getEntityLiving()).setAttackTarget(null);
                } else if (event.getEntityLiving() instanceof EntityLiving) {
                    ((EntityLiving) event.getEntityLiving()).setAttackTarget(null);
                }
                // 也标记以便下一 tick 再次清除，防止诅咒在同一帧内重新设置目标
                pendingTargetClear.put(event.getEntityLiving().getEntityId(), player.getUniqueID());
            }
        }
    }

    /**
     * 每 tick 清除中立生物的攻击目标（和平徽章效果）
     */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world.isRemote) return;

        // 每 tick 处理，确保及时清除攻击目标
        if (pendingTargetClear.isEmpty()) return;

        // 清除标记的攻击目标
        pendingTargetClear.entrySet().removeIf(entry -> {
            net.minecraft.entity.Entity entity = event.world.getEntityByID(entry.getKey());
            if (entity instanceof EntityCreature) {
                EntityCreature creature = (EntityCreature) entity;
                if (creature.getAttackTarget() != null &&
                        creature.getAttackTarget().getUniqueID().equals(entry.getValue())) {
                    creature.setAttackTarget(null);
                }
            } else if (entity instanceof EntityLiving) {
                EntityLiving living = (EntityLiving) entity;
                if (living.getAttackTarget() != null &&
                        living.getAttackTarget().getUniqueID().equals(entry.getValue())) {
                    living.setAttackTarget(null);
                }
            }
            return true;
        });
    }

    // ========== 3. 护甲效力降低30% → 守护鳞片祝福 ==========
    // 原诅咒：护甲效力降低30%
    // 祝福效果：护甲效力提升30%（抵消诅咒后额外加成）
    // 护甲处理在 onPlayerTick 中的 handleGuardianScaleArmor 方法

    // ========== 4. 对怪物伤害降低50% → 勇气之刃祝福 ==========
    // 原诅咒：对怪物伤害降低50%
    // 祝福效果：对怪物伤害提升25%（抵消诅咒后额外增伤）

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查目标是否是怪物
        if (!(event.getEntityLiving() instanceof EntityMob)) return;

        // 检查是否嵌入了勇气之刃
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.COURAGE_BLADE)) {
            // 七咒会让对怪物伤害减半（×0.5），嵌入后转化为祝福
            // 祝福：不仅抵消减半，还额外增伤25%
            // 原伤害 × 0.5（诅咒）× 2（抵消）× 1.25（祝福）= 原伤害 × 1.25
            // 相当于 25% 增伤
            float currentDamage = event.getAmount();
            event.setAmount(currentDamage * 2.5f);
        }
    }

    // ========== 5. 着火永燃 → 霜华之露祝福 ==========
    // 原诅咒：着火永燃
    // 祝福效果：火焰抗性（立即灭火 + 火焰抗性 buff）

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查是否嵌入了霜华之露
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.FROST_DEW)) {
            // 霜华之露祝福效果：立即灭火 + 火焰抗性
            if (player.isBurning()) {
                // 立即灭火（强制设置 fire ticks 为 0）
                player.extinguish();
                setFireTicks(player, 0);

                // 祝福：给予火焰抗性
                if (!player.isPotionActive(net.minecraft.init.MobEffects.FIRE_RESISTANCE)) {
                    player.addPotionEffect(new net.minecraft.potion.PotionEffect(
                            net.minecraft.init.MobEffects.FIRE_RESISTANCE, 100, 0, false, true));
                }
            }
        }

        // 检查是否嵌入了安眠香囊 - 睡眠祝福
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SLUMBER_SACHET)) {
            // 安眠香囊祝福效果：睡眠时获得再生 + 强制突破睡眠诅咒
            if (player.isPlayerSleeping()) {
                // 给予再生效果
                if (!player.isPotionActive(net.minecraft.init.MobEffects.REGENERATION)) {
                    player.addPotionEffect(new net.minecraft.potion.PotionEffect(
                            net.minecraft.init.MobEffects.REGENERATION, 100, 1, false, false));
                }

                // 强制突破睡眠诅咒：七咒会把 sleepTimer 卡在 90，我们强制推进它
                // 睡眠需要 sleepTimer >= 100 才能完成，七咒通过 sleepTimer = 90 来阻止
                int currentSleepTimer = getSleepTimer(player);
                // 如果 sleepTimer 被卡在 90-91（诅咒会把它设回 90），强制推进
                if (currentSleepTimer >= 89 && currentSleepTimer <= 91) {
                    // 每 tick 强制 +2，这样即使诅咒设回 90，下一 tick 也能推进
                    setSleepTimer(player, currentSleepTimer + 2);
                }
            }
        }

        // 处理守护鳞片的护甲恢复
        handleGuardianScaleArmor(player);
    }

    // ========== 守护鳞片护甲祝福处理 ==========

    private static final UUID GUARDIAN_SCALE_ARMOR_UUID = UUID.fromString("a8b3c4d5-e6f7-4a8b-9c0d-1e2f3a4b5c6d");
    private static final Map<UUID, Boolean> armorModifierApplied = new HashMap<>();

    private static void handleGuardianScaleArmor(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        boolean hasGuardianScale = EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.GUARDIAN_SCALE);

        // 直接检查实体上是否有修改器，而不是依赖 HashMap（解决死亡后重置问题）
        boolean actuallyApplied = player.getEntityAttribute(SharedMonsterAttributes.ARMOR) != null &&
                player.getEntityAttribute(SharedMonsterAttributes.ARMOR).getModifier(GUARDIAN_SCALE_ARMOR_UUID) != null;

        if (hasGuardianScale && !actuallyApplied) {
            // 嵌入了守护鳞片，添加护甲祝福修正
            // 七咒降低30%护甲（×0.7），祝福效果：不仅抵消，还额外+30%
            // 目标：原护甲 × 0.7（诅咒）× 1.857（修正）≈ 原护甲 × 1.3（祝福）
            // 1.857 = 1.3 / 0.7 = 13/7
            AttributeModifier armorBoost = new AttributeModifier(
                    GUARDIAN_SCALE_ARMOR_UUID,
                    "Guardian Scale Armor Blessing",
                    0.857D,  // +85.7% 来实现 70% × 185.7% = 130%（+30%祝福）
                    2  // Operation: Multiply
            );

            if (player.getEntityAttribute(SharedMonsterAttributes.ARMOR) != null) {
                player.getEntityAttribute(SharedMonsterAttributes.ARMOR).applyModifier(armorBoost);
                armorModifierApplied.put(playerId, true);
            }
        } else if (!hasGuardianScale && actuallyApplied) {
            // 移除了守护鳞片，移除护甲修正
            if (player.getEntityAttribute(SharedMonsterAttributes.ARMOR) != null) {
                player.getEntityAttribute(SharedMonsterAttributes.ARMOR).removeModifier(GUARDIAN_SCALE_ARMOR_UUID);
                armorModifierApplied.put(playerId, false);
            }
        }
    }

    // ========== 6. 死亡灵魂破碎 → 灵魂锚点抵消 ==========
    // 这个效果通过 MixinSuperpositionHandler 实现
    // 拦截 keletu.enigmaticlegacy.event.SuperpositionHandler.loseSoul 方法
    // 灵魂锚点嵌入后，死亡不会导致灵魂破碎

    // ========== 7. 失眠症 → 安眠香囊抵消 ==========

    /**
     * 在睡眠事件的最低优先级处理（最后执行）
     * 如果嵌入了安眠香囊，并且之前被诅咒阻止，强制允许睡觉
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerSleepLowest(PlayerSleepInBedEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查是否嵌入了安眠香囊
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SLUMBER_SACHET)) {
            // 如果结果是 NOT_POSSIBLE_HERE（七咒造成的失眠），强制覆盖为 null 让原版逻辑处理
            // 或者如果其他条件都满足，设置为 OK
            EntityPlayer.SleepResult currentResult = event.getResultStatus();
            if (currentResult == EntityPlayer.SleepResult.NOT_POSSIBLE_HERE ||
                currentResult == EntityPlayer.SleepResult.OTHER_PROBLEM) {
                // 强制设置结果为 OK，抵消诅咒的失眠效果
                event.setResult(EntityPlayer.SleepResult.OK);
            }
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 获取玩家当前抵消的诅咒数量
     */
    public static int getCounteredCurseCount(EntityPlayer player) {
        int count = 0;

        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SACRED_HEART)) count++;
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.PEACE_EMBLEM)) count++;
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.GUARDIAN_SCALE)) count++;
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.COURAGE_BLADE)) count++;
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.FROST_DEW)) count++;
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SOUL_ANCHOR)) count++;
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SLUMBER_SACHET)) count++;

        return count;
    }

    /**
     * 获取诅咒状态描述（祝福版）
     */
    public static String[] getCurseStatus(EntityPlayer player) {
        return new String[] {
                "1.伤害加倍: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SACRED_HEART) ? "§6祝福§r (减伤62.5%)" : "§c生效中"),
                "2.中立生物攻击: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.PEACE_EMBLEM) ? "§6祝福§r (和平光环)" : "§c生效中"),
                "3.护甲降低30%: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.GUARDIAN_SCALE) ? "§6祝福§r (护甲+30%)" : "§c生效中"),
                "4.伤害降低50%: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.COURAGE_BLADE) ? "§6祝福§r (增伤25%)" : "§c生效中"),
                "5.永燃: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.FROST_DEW) ? "§6祝福§r (火焰抗性)" : "§c生效中"),
                "6.灵魂破碎: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SOUL_ANCHOR) ? "§6祝福§r (灵魂护佑)" : "§c生效中"),
                "7.失眠症: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SLUMBER_SACHET) ? "§6祝福§r (安眠回血)" : "§c生效中")
        };
    }
}