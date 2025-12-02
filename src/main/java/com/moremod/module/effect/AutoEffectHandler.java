package com.moremod.module.effect;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.module.ModuleAutoRegistry;
import com.moremod.module.ModuleDefinition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自动效果处理器 - 处理所有通过 ModuleDefinition 定义的效果
 *
 * 自动处理：
 * - 属性修改器的应用和移除
 * - 药水效果的持续应用
 * - 周期性恢复效果
 * - 伤害加成/减免/反弹
 * - 自定义回调
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class AutoEffectHandler {

    // 玩家效果状态追踪
    private static final Map<UUID, PlayerEffectState> playerStates = new ConcurrentHashMap<>();

    // NBT键前缀
    private static final String NBT_PREFIX = "AutoEffect_";

    /**
     * 玩家效果状态
     */
    private static class PlayerEffectState {
        Map<String, Long> lastTickTimes = new HashMap<>();
        Map<String, Set<UUID>> activeModifiers = new HashMap<>();
    }

    /**
     * 获取或创建玩家状态
     */
    private static PlayerEffectState getState(EntityPlayer player) {
        return playerStates.computeIfAbsent(player.getUniqueID(), k -> new PlayerEffectState());
    }

    // ========== Tick 事件处理 ==========

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        ItemStack coreStack = ItemMechanicalCore.findEquippedCore(player);
        if (coreStack.isEmpty()) {
            // 清理玩家的所有效果
            cleanupAllEffects(player);
            return;
        }

        long currentTime = player.world.getTotalWorldTime();
        PlayerEffectState state = getState(player);

        // 遍历所有自动注册的模块
        for (ModuleDefinition def : ModuleAutoRegistry.getAllDefinitions()) {
            if (def.effects == null || def.effects.isEmpty()) continue;

            // 检查模块是否激活
            boolean isActive = ItemMechanicalCore.isUpgradeActive(coreStack, def.id);
            int level = 0;
            try {
                level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, def.id);
            } catch (Throwable ignored) {}

            if (!isActive || level <= 0) {
                // 模块未激活，清理该模块的效果
                cleanupModuleEffects(player, def.id, state);
                continue;
            }

            // 处理该模块的所有效果
            for (ModuleEffect effect : def.effects) {
                processTickEffect(player, coreStack, def.id, level, effect, currentTime, state);
            }
        }
    }

    /**
     * 处理单个tick效果
     */
    private static void processTickEffect(EntityPlayer player, ItemStack coreStack, String moduleId,
                                          int level, ModuleEffect effect, long currentTime,
                                          PlayerEffectState state) {
        String effectKey = moduleId + "_" + effect.effectId;

        switch (effect.type) {
            case ATTRIBUTE_MODIFIER:
                applyAttributeModifier(player, moduleId, level, effect, state);
                break;

            case POTION_EFFECT:
                applyPotionEffect(player, level, effect);
                break;

            case HEALING:
                if (shouldTrigger(effectKey, currentTime, effect.tickInterval, state)) {
                    if (checkEnergy(player, coreStack, moduleId, effect)) {
                        float amount = effect.getHealAmountForLevel(level);
                        player.heal(amount);
                    }
                }
                break;

            case FOOD_RESTORE:
                if (shouldTrigger(effectKey, currentTime, effect.tickInterval, state)) {
                    if (checkEnergy(player, coreStack, moduleId, effect)) {
                        int amount = effect.foodAmount + (effect.foodPerLevel * (level - 1));
                        player.getFoodStats().addStats(amount, effect.saturation);
                    }
                }
                break;

            case TICK_CALLBACK:
                if (effect.tickCallback != null) {
                    if (shouldTrigger(effectKey, currentTime, effect.tickInterval, state)) {
                        if (checkEnergy(player, coreStack, moduleId, effect)) {
                            effect.tickCallback.execute(player, coreStack, moduleId, level, null);
                        }
                    }
                }
                break;

            default:
                // 其他类型在事件处理器中处理
                break;
        }
    }

    /**
     * 检查是否应该触发（基于间隔）
     */
    private static boolean shouldTrigger(String key, long currentTime, int interval, PlayerEffectState state) {
        Long lastTime = state.lastTickTimes.get(key);
        if (lastTime == null || currentTime - lastTime >= interval) {
            state.lastTickTimes.put(key, currentTime);
            return true;
        }
        return false;
    }

    /**
     * 检查并消耗能量
     */
    private static boolean checkEnergy(EntityPlayer player, ItemStack coreStack, String moduleId, ModuleEffect effect) {
        if (!effect.requiresEnergy) return true;

        int cost = effect.energyCost > 0 ? effect.energyCost : effect.energyPerTick;
        if (cost <= 0) return true;

        return ItemMechanicalCore.consumeEnergy(coreStack, cost);
    }

    /**
     * 应用属性修改器
     */
    private static void applyAttributeModifier(EntityPlayer player, String moduleId, int level,
                                               ModuleEffect effect, PlayerEffectState state) {
        if (effect.attribute == null) return;

        IAttributeInstance attr = player.getAttributeMap().getAttributeInstance(effect.attribute);
        if (attr == null) return;

        String modifierKey = moduleId + "_" + effect.effectId;
        UUID modUUID = effect.modifierUUID;

        // 移除旧的修改器
        AttributeModifier existing = attr.getModifier(modUUID);
        if (existing != null) {
            attr.removeModifier(existing);
        }

        // 计算新值并应用
        double value = effect.getValueForLevel(level);
        AttributeModifier modifier = new AttributeModifier(
                modUUID,
                "MechanicalCore_" + moduleId,
                value,
                effect.operation.mcValue
        );
        attr.applyModifier(modifier);

        // 记录活动的修改器
        state.activeModifiers
                .computeIfAbsent(moduleId, k -> new HashSet<>())
                .add(modUUID);
    }

    /**
     * 应用药水效果
     */
    private static void applyPotionEffect(EntityPlayer player, int level, ModuleEffect effect) {
        if (effect.potion == null) return;

        int amplifier = effect.getAmplifierForLevel(level);
        PotionEffect potionEffect = new PotionEffect(
                effect.potion,
                effect.baseDuration,
                amplifier,
                effect.ambient,
                effect.showParticles
        );
        player.addPotionEffect(potionEffect);
    }

    // ========== 伤害事件处理 ==========

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 处理伤害加成（玩家攻击）
        Entity source = event.getSource().getTrueSource();
        if (source instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source;
            ItemStack coreStack = ItemMechanicalCore.findEquippedCore(player);
            if (!coreStack.isEmpty()) {
                float damage = event.getAmount();
                damage = applyDamageBoost(player, coreStack, damage, event);
                event.setAmount(damage);
            }
        }

        // 处理伤害减免和反弹（玩家受伤）
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            ItemStack coreStack = ItemMechanicalCore.findEquippedCore(player);
            if (!coreStack.isEmpty()) {
                float damage = event.getAmount();
                damage = applyDamageReduction(player, coreStack, damage, event);
                event.setAmount(damage);

                // 处理伤害反弹
                applyDamageReflection(player, coreStack, event);
            }
        }
    }

    /**
     * 应用伤害加成
     */
    private static float applyDamageBoost(EntityPlayer player, ItemStack coreStack, float damage, LivingHurtEvent event) {
        float totalMultiplier = 1.0f;

        for (ModuleDefinition def : ModuleAutoRegistry.getAllDefinitions()) {
            if (def.effects == null) continue;
            if (!ItemMechanicalCore.isUpgradeActive(coreStack, def.id)) continue;

            int level = 0;
            try {
                level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, def.id);
            } catch (Throwable ignored) {}
            if (level <= 0) continue;

            for (ModuleEffect effect : def.effects) {
                if (effect.type == ModuleEffect.EffectType.DAMAGE_BOOST) {
                    totalMultiplier += effect.getDamageMultiplierForLevel(level) - 1.0f;
                }

                // 处理自定义攻击回调
                if (effect.type == ModuleEffect.EffectType.ON_HIT && effect.hitCallback != null) {
                    if (checkEnergy(player, coreStack, def.id, effect)) {
                        effect.hitCallback.execute(player, coreStack, def.id, level, event);
                    }
                }
            }
        }

        return damage * totalMultiplier;
    }

    /**
     * 应用伤害减免
     */
    private static float applyDamageReduction(EntityPlayer player, ItemStack coreStack, float damage, LivingHurtEvent event) {
        float totalReduction = 0.0f;
        String damageType = event.getSource().getDamageType();

        for (ModuleDefinition def : ModuleAutoRegistry.getAllDefinitions()) {
            if (def.effects == null) continue;
            if (!ItemMechanicalCore.isUpgradeActive(coreStack, def.id)) continue;

            int level = 0;
            try {
                level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, def.id);
            } catch (Throwable ignored) {}
            if (level <= 0) continue;

            for (ModuleEffect effect : def.effects) {
                if (effect.type == ModuleEffect.EffectType.DAMAGE_REDUCTION) {
                    // 检查伤害类型过滤
                    if (!effect.damageTypes.isEmpty() && !effect.damageTypes.contains(damageType)) {
                        continue;
                    }
                    totalReduction += effect.getReductionForLevel(level);
                }

                // 处理自定义受伤回调
                if (effect.type == ModuleEffect.EffectType.ON_HURT && effect.hurtCallback != null) {
                    if (checkEnergy(player, coreStack, def.id, effect)) {
                        effect.hurtCallback.execute(player, coreStack, def.id, level, event);
                    }
                }
            }
        }

        // 限制最大减免为 90%
        totalReduction = Math.min(totalReduction, 0.9f);
        return damage * (1.0f - totalReduction);
    }

    /**
     * 应用伤害反弹
     */
    private static void applyDamageReflection(EntityPlayer player, ItemStack coreStack, LivingHurtEvent event) {
        Entity attacker = event.getSource().getTrueSource();
        if (!(attacker instanceof EntityLivingBase)) return;

        float totalReflection = 0.0f;

        for (ModuleDefinition def : ModuleAutoRegistry.getAllDefinitions()) {
            if (def.effects == null) continue;
            if (!ItemMechanicalCore.isUpgradeActive(coreStack, def.id)) continue;

            int level = 0;
            try {
                level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, def.id);
            } catch (Throwable ignored) {}
            if (level <= 0) continue;

            for (ModuleEffect effect : def.effects) {
                if (effect.type == ModuleEffect.EffectType.DAMAGE_REFLECTION) {
                    totalReflection += effect.getReflectionForLevel(level);
                }
            }
        }

        if (totalReflection > 0) {
            float reflectedDamage = event.getAmount() * totalReflection;
            ((EntityLivingBase) attacker).attackEntityFrom(
                    net.minecraft.util.DamageSource.causeThornsDamage(player),
                    reflectedDamage
            );
        }
    }

    // ========== 清理方法 ==========

    /**
     * 清理玩家的所有自动效果
     */
    private static void cleanupAllEffects(EntityPlayer player) {
        PlayerEffectState state = playerStates.get(player.getUniqueID());
        if (state == null) return;

        // 移除所有属性修改器
        for (Map.Entry<String, Set<UUID>> entry : state.activeModifiers.entrySet()) {
            for (UUID modUUID : entry.getValue()) {
                removeModifierByUUID(player, modUUID);
            }
        }

        state.activeModifiers.clear();
        state.lastTickTimes.clear();
    }

    /**
     * 清理指定模块的效果
     */
    private static void cleanupModuleEffects(EntityPlayer player, String moduleId, PlayerEffectState state) {
        Set<UUID> modifiers = state.activeModifiers.get(moduleId);
        if (modifiers != null) {
            for (UUID modUUID : modifiers) {
                removeModifierByUUID(player, modUUID);
            }
            modifiers.clear();
        }

        // 清理该模块的tick计时
        state.lastTickTimes.entrySet().removeIf(e -> e.getKey().startsWith(moduleId + "_"));
    }

    /**
     * 通过UUID移除属性修改器
     */
    private static void removeModifierByUUID(EntityPlayer player, UUID modUUID) {
        // 尝试从所有属性中移除
        for (IAttributeInstance attr : player.getAttributeMap().getAllAttributes()) {
            AttributeModifier modifier = attr.getModifier(modUUID);
            if (modifier != null) {
                attr.removeModifier(modifier);
            }
        }
    }

    /**
     * 玩家离开时清理
     */
    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        playerStates.remove(event.player.getUniqueID());
    }
}
