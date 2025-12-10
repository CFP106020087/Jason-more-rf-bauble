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

    static {
        // "field_190534_ay" 是 Entity.fire 的 SRG 混淆名
        FIRE_FIELD = ObfuscationReflectionHelper.findField(Entity.class, "field_190534_ay");
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

    // ========== 1. 受到伤害加倍 → 圣光之心抵消 ==========

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查是否嵌入了圣光之心
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SACRED_HEART)) {
            // 七咒会让伤害翻倍，嵌入后抵消这个效果
            // 如果伤害被翻倍了，我们减半恢复原值
            float currentDamage = event.getAmount();
            event.setAmount(currentDamage * 0.5f);
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
            // 如果是中立生物（非敌对怪物），取消攻击目标
            if (!(event.getEntityLiving() instanceof EntityMob)) {
                if (event.getEntityLiving() instanceof EntityAnimal ||
                        (event.getEntityLiving() instanceof EntityLiving && !(event.getEntityLiving() instanceof EntityMob))) {
                    // 标记需要在下一tick清除攻击目标
                    pendingTargetClear.put(event.getEntityLiving().getEntityId(), player.getUniqueID());
                }
            }
        }
    }

    /**
     * 定期清除中立生物的攻击目标（和平徽章效果）
     */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world.isRemote) return;

        // 每 5 tick 处理一次
        if (event.world.getTotalWorldTime() % 5 != 0) return;

        // 清除标记的攻击目标
        pendingTargetClear.entrySet().removeIf(entry -> {
            net.minecraft.entity.Entity entity = event.world.getEntityByID(entry.getKey());
            if (entity instanceof EntityCreature) {
                EntityCreature creature = (EntityCreature) entity;
                if (creature.getAttackTarget() != null &&
                        creature.getAttackTarget().getUniqueID().equals(entry.getValue())) {
                    creature.setAttackTarget(null);
                }
            }
            return true;
        });
    }

    // ========== 3. 护甲效力降低30% → 守护鳞片抵消 ==========
    // 护甲恢复处理在 onPlayerTick 中的 handleGuardianScaleArmor 方法

    // ========== 4. 对怪物伤害降低50% → 勇气之刃抵消 ==========

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
            // 七咒会让对怪物伤害减半，嵌入后抵消这个效果
            // 将伤害恢复（乘以2）
            float currentDamage = event.getAmount();
            event.setAmount(currentDamage * 2.0f);
        }
    }

    // ========== 5. 着火永燃 → 霜华之露抵消 ==========

    // 追踪玩家的火焰状态，用于判断火焰是否应该熄灭
    private static final Map<UUID, Integer> playerFireTicks = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查是否嵌入了霜华之露
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.FROST_DEW)) {
            // 霜华之露效果：如果玩家着火，允许火焰正常减少
            // 七咒会让火焰永不熄灭（fire ticks 不减少或持续增加）
            // 嵌入后，我们主动减少 fire ticks 来抵消这个效果
            if (player.isBurning()) {
                UUID playerId = player.getUniqueID();
                int lastFireTicks = playerFireTicks.getOrDefault(playerId, 0);
                int currentFireTicks = getFireTicks(player);

                // 如果火焰没有自然减少（被七咒阻止了），我们手动减少
                if (currentFireTicks >= lastFireTicks && lastFireTicks > 0) {
                    // 每 tick 减少 2 点火焰时间，正常熄灭
                    setFireTicks(player, Math.max(0, currentFireTicks - 2));
                }

                playerFireTicks.put(playerId, getFireTicks(player));
            } else {
                // 不着火时清除记录
                playerFireTicks.remove(player.getUniqueID());
            }
        }

        // 处理守护鳞片的护甲恢复
        handleGuardianScaleArmor(player);
    }

    // ========== 守护鳞片护甲恢复处理 ==========

    private static final UUID GUARDIAN_SCALE_ARMOR_UUID = UUID.fromString("a8b3c4d5-e6f7-4a8b-9c0d-1e2f3a4b5c6d");
    private static final Map<UUID, Boolean> armorModifierApplied = new HashMap<>();

    private static void handleGuardianScaleArmor(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        boolean hasGuardianScale = EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.GUARDIAN_SCALE);
        boolean wasApplied = armorModifierApplied.getOrDefault(playerId, false);

        if (hasGuardianScale && !wasApplied) {
            // 嵌入了守护鳞片，添加护甲恢复修正
            // 七咒降低30%护甲，我们添加一个正向修正来抵消
            // 注意：这里使用Operation 2 (乘法)，值为 0.428 表示 +42.8%
            // 这样 70% * 1.428 ≈ 100%，恢复原始护甲值
            AttributeModifier armorBoost = new AttributeModifier(
                    GUARDIAN_SCALE_ARMOR_UUID,
                    "Guardian Scale Armor Restoration",
                    0.428D,  // +42.8% 来抵消 -30%
                    2  // Operation: Multiply
            );

            if (player.getEntityAttribute(SharedMonsterAttributes.ARMOR) != null) {
                if (player.getEntityAttribute(SharedMonsterAttributes.ARMOR).getModifier(GUARDIAN_SCALE_ARMOR_UUID) == null) {
                    player.getEntityAttribute(SharedMonsterAttributes.ARMOR).applyModifier(armorBoost);
                    armorModifierApplied.put(playerId, true);
                }
            }
        } else if (!hasGuardianScale && wasApplied) {
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
     * 在睡眠事件的最高优先级处理
     * 如果嵌入了安眠香囊，强制设置结果为 OK
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerSleep(PlayerSleepInBedEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查是否嵌入了安眠香囊
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SLUMBER_SACHET)) {
            // 安眠香囊抵消失眠症
            // 如果当前结果是 NOT_POSSIBLE_HERE（七咒造成的失眠），我们尝试允许睡觉
            // 注意：这里不能直接设置 setResult(OK)，因为还有其他条件需要检查
            // 但我们可以在 enigmaticlegacy 的事件之后重置结果

            // 保存原始状态，在后续处理中使用
            player.getEntityData().setBoolean("moremod_slumber_sachet_active", true);
        }
    }

    /**
     * 在较低优先级检查是否需要覆盖失眠结果
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerSleepLow(PlayerSleepInBedEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        // 检查是否激活了安眠香囊
        if (player.getEntityData().getBoolean("moremod_slumber_sachet_active")) {
            player.getEntityData().removeTag("moremod_slumber_sachet_active");

            // 如果结果是 NOT_POSSIBLE_HERE（可能是七咒造成的），尝试重置
            // 注意：由于 Forge 事件系统的限制，我们不能直接覆盖结果
            // 但我们可以通过 Mixin 来实现这个功能
            if (event.getResultStatus() != null &&
                    event.getResultStatus() == EntityPlayer.SleepResult.NOT_POSSIBLE_HERE) {
                // 这里需要 Mixin 来完全覆盖
                // 目前只是标记，让 Mixin 来处理
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
     * 获取诅咒抵消状态描述
     */
    public static String[] getCurseStatus(EntityPlayer player) {
        return new String[] {
                "1.伤害加倍: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SACRED_HEART) ? "§a已抵消" : "§c生效中"),
                "2.中立生物攻击: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.PEACE_EMBLEM) ? "§a已抵消" : "§c生效中"),
                "3.护甲降低30%: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.GUARDIAN_SCALE) ? "§a已抵消" : "§c生效中"),
                "4.伤害降低50%: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.COURAGE_BLADE) ? "§a已抵消" : "§c生效中"),
                "5.永燃: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.FROST_DEW) ? "§a已抵消" : "§c生效中"),
                "6.灵魂破碎: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SOUL_ANCHOR) ? "§a已抵消" : "§c生效中"),
                "7.失眠症: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SLUMBER_SACHET) ? "§a已抵消" : "§c生效中")
        };
    }
}