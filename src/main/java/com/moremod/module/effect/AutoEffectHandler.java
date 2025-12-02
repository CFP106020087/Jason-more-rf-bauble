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
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                      自动效果处理器 (AutoEffectHandler)                        ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  处理所有通过 ModuleDefinition 定义的效果:                                      ║
 * ║  1. 简单效果 (.effects) - 属性修改器、药水、恢复、伤害等                          ║
 * ║  2. 事件处理器 (.handler) - 完整自定义逻辑                                      ║
 * ║                                                                              ║
 * ║  自动触发的事件:                                                               ║
 * ║  - PlayerTickEvent      → onTick / onSecondTick                              ║
 * ║  - LivingHurtEvent      → onPlayerHurt / onPlayerAttack                      ║
 * ║  - LivingDeathEvent     → onPlayerKillEntity                                 ║
 * ║  - LivingAttackEvent    → onPlayerAttacked                                   ║
 * ║  - PlayerInteractEvent  → onRightClick / onLeftClick                         ║
 * ║  - BlockEvent.BreakEvent→ onBlockBreak                                       ║
 * ║                                                                              ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class AutoEffectHandler {

    // 玩家效果状态追踪
    private static final Map<UUID, PlayerEffectState> playerStates = new ConcurrentHashMap<>();

    // 模块激活状态追踪 (用于触发 onModuleActivated/Deactivated)
    private static final Map<UUID, Set<String>> activeModules = new ConcurrentHashMap<>();

    // 【优化】玩家活跃模块缓存 (moduleId -> level)
    // 在 tick 中计算一次，其他事件复用，避免重复 isUpgradeActive/getUpgradeLevel 调用
    private static final Map<UUID, Map<String, Integer>> activeModulesCache = new ConcurrentHashMap<>();

    // 【优化】玩家装备的核心缓存 (在同一 tick 周期内复用)
    private static final Map<UUID, ItemStack> coreStackCache = new ConcurrentHashMap<>();

    // 缓存过期时间 (tick)
    private static final Map<UUID, Long> cacheExpireTime = new ConcurrentHashMap<>();

    /**
     * 玩家效果状态
     */
    private static class PlayerEffectState {
        Map<String, Long> lastTickTimes = new HashMap<>();
        Map<String, Long> lastSecondTicks = new HashMap<>();
        Map<String, Set<UUID>> activeModifiers = new HashMap<>();
    }

    private static PlayerEffectState getState(EntityPlayer player) {
        return playerStates.computeIfAbsent(player.getUniqueID(), k -> new PlayerEffectState());
    }

    private static Set<String> getActiveModules(EntityPlayer player) {
        return activeModules.computeIfAbsent(player.getUniqueID(), k -> new HashSet<>());
    }

    /**
     * 【优化】获取缓存的活跃模块 (moduleId -> level)
     * 如果缓存过期或不存在，返回 null
     */
    private static Map<String, Integer> getCachedActiveModules(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        Long expireTime = cacheExpireTime.get(uuid);
        if (expireTime != null && player.world.getTotalWorldTime() <= expireTime) {
            return activeModulesCache.get(uuid);
        }
        return null;
    }

    /**
     * 【优化】获取缓存的核心物品
     */
    private static ItemStack getCachedCoreStack(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        Long expireTime = cacheExpireTime.get(uuid);
        if (expireTime != null && player.world.getTotalWorldTime() <= expireTime) {
            ItemStack cached = coreStackCache.get(uuid);
            if (cached != null) return cached;
        }
        // 缓存过期或不存在，重新获取
        ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(player);
        coreStackCache.put(uuid, coreStack);
        cacheExpireTime.put(uuid, player.world.getTotalWorldTime());
        return coreStack;
    }

    /**
     * 【优化】更新玩家活跃模块缓存
     */
    private static void updateActiveModulesCache(EntityPlayer player, ItemStack coreStack, Map<String, Integer> modules) {
        UUID uuid = player.getUniqueID();
        activeModulesCache.put(uuid, modules);
        coreStackCache.put(uuid, coreStack);
        // 缓存有效期1 tick
        cacheExpireTime.put(uuid, player.world.getTotalWorldTime());
    }

    // ==================== Tick 事件处理 ====================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(player);

        if (coreStack.isEmpty()) {
            handleCoreRemoved(player);
            // 清除缓存
            activeModulesCache.remove(player.getUniqueID());
            coreStackCache.remove(player.getUniqueID());
            return;
        }

        long currentTime = player.world.getTotalWorldTime();
        PlayerEffectState state = getState(player);
        Set<String> currentActive = getActiveModules(player);
        Set<String> nowActive = new HashSet<>();

        // 【优化】构建活跃模块缓存 (供其他事件使用)
        Map<String, Integer> activeLevelCache = new HashMap<>();

        // 遍历所有自动注册的模块
        for (ModuleDefinition def : ModuleAutoRegistry.getAllDefinitions()) {
            boolean isActive = ItemMechanicalCore.isUpgradeActive(coreStack, def.id);
            int level = 0;
            try {
                level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, def.id);
            } catch (Throwable ignored) {}

            if (!isActive || level <= 0) {
                // 模块未激活
                if (currentActive.contains(def.id)) {
                    // 触发 onModuleDeactivated
                    handleModuleDeactivated(player, coreStack, def, state);
                }
                cleanupModuleEffects(player, def.id, state);
                continue;
            }

            nowActive.add(def.id);
            // 【优化】记录活跃模块和等级
            activeLevelCache.put(def.id, level);

            // 检查是否新激活
            if (!currentActive.contains(def.id)) {
                handleModuleActivated(player, coreStack, def, level);
            }

            // 创建上下文
            EventContext ctx = new EventContext(player, coreStack, def.id, level);

            // 处理被动能耗
            if (def.hasHandler()) {
                int energyCost = def.handler.getPassiveEnergyCost();
                if (energyCost > 0) {
                    if (!ItemMechanicalCore.consumeEnergy(coreStack, energyCost)) {
                        // 能量不足，跳过此模块
                        continue;
                    }
                }
            }

            // 处理事件处理器的 tick
            if (def.hasHandler()) {
                processHandlerTick(def, ctx, currentTime, state);
            }

            // 处理简单效果
            if (def.hasEffects()) {
                for (ModuleEffect effect : def.effects) {
                    processSimpleEffect(player, coreStack, def.id, level, effect, currentTime, state);
                }
            }
        }

        // 【优化】更新活跃模块缓存
        updateActiveModulesCache(player, coreStack, activeLevelCache);

        // 更新激活状态
        currentActive.clear();
        currentActive.addAll(nowActive);
    }

    /**
     * 处理 Handler 的 tick 事件
     */
    private static void processHandlerTick(ModuleDefinition def, EventContext ctx,
                                           long currentTime, PlayerEffectState state) {
        IModuleEventHandler handler = def.handler;
        String tickKey = def.id + "_tick";
        String secondKey = def.id + "_second";

        // 自定义间隔 tick
        int interval = handler.getTickInterval();
        if (interval <= 0) {
            // 每tick调用
            handler.onTick(ctx);
        } else {
            Long lastTick = state.lastTickTimes.get(tickKey);
            if (lastTick == null || currentTime - lastTick >= interval) {
                state.lastTickTimes.put(tickKey, currentTime);
                handler.onTick(ctx);
            }
        }

        // 每秒调用
        Long lastSecond = state.lastSecondTicks.get(secondKey);
        if (lastSecond == null || currentTime - lastSecond >= 20) {
            state.lastSecondTicks.put(secondKey, currentTime);
            handler.onSecondTick(ctx);
        }
    }

    /**
     * 处理简单效果
     */
    private static void processSimpleEffect(EntityPlayer player, ItemStack coreStack, String moduleId,
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
                    if (checkEnergy(coreStack, effect)) {
                        float amount = effect.getHealAmountForLevel(level);
                        player.heal(amount);
                    }
                }
                break;

            case FOOD_RESTORE:
                if (shouldTrigger(effectKey, currentTime, effect.tickInterval, state)) {
                    if (checkEnergy(coreStack, effect)) {
                        int amount = effect.foodAmount + (effect.foodPerLevel * (level - 1));
                        player.getFoodStats().addStats(amount, effect.saturation);
                    }
                }
                break;

            case TICK_CALLBACK:
                if (effect.tickCallback != null) {
                    if (shouldTrigger(effectKey, currentTime, effect.tickInterval, state)) {
                        if (checkEnergy(coreStack, effect)) {
                            effect.tickCallback.execute(player, coreStack, moduleId, level, null);
                        }
                    }
                }
                break;

            default:
                break;
        }
    }

    // ==================== 伤害事件处理 ====================

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity sourceEntity = event.getSource().getTrueSource();

        // 玩家攻击
        if (sourceEntity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sourceEntity;
            // 【优化】尝试使用缓存
            ItemStack coreStack = getCachedCoreStack(player);
            if (!coreStack.isEmpty() && event.getEntityLiving() != null) {
                float damage = event.getAmount();
                Map<String, Integer> cachedModules = getCachedActiveModules(player);
                damage = processPlayerAttack(player, coreStack, event.getEntityLiving(), damage, event, cachedModules);
                event.setAmount(damage);
            }
        }

        // 玩家受伤
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            // 【优化】尝试使用缓存
            ItemStack coreStack = getCachedCoreStack(player);
            if (!coreStack.isEmpty()) {
                float damage = event.getAmount();
                Map<String, Integer> cachedModules = getCachedActiveModules(player);
                damage = processPlayerHurt(player, coreStack, damage, event, cachedModules);
                event.setAmount(damage);
            }
        }
    }

    /**
     * 处理玩家攻击
     * @param cachedModules 【优化】可选的已缓存活跃模块，null 则回退到直接查询
     */
    private static float processPlayerAttack(EntityPlayer player, ItemStack coreStack,
                                             EntityLivingBase target, float damage, LivingHurtEvent event,
                                             Map<String, Integer> cachedModules) {
        float totalMultiplier = 1.0f;

        for (ModuleDefinition def : ModuleAutoRegistry.getAllDefinitions()) {
            int level;
            if (cachedModules != null) {
                Integer cached = cachedModules.get(def.id);
                if (cached == null) continue;
                level = cached;
            } else {
                if (!ItemMechanicalCore.isUpgradeActive(coreStack, def.id)) continue;
                level = 0;
                try { level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, def.id); } catch (Throwable ignored) {}
                if (level <= 0) continue;
            }

            // 处理 Handler
            if (def.hasHandler()) {
                EventContext ctx = new EventContext(player, coreStack, def.id, level);
                float newDamage = def.handler.onPlayerAttack(ctx, target, damage * totalMultiplier);
                totalMultiplier = newDamage / damage;
                def.handler.onPlayerHitEntity(ctx, target, event);
            }

            // 处理简单效果
            if (def.hasEffects()) {
                for (ModuleEffect effect : def.effects) {
                    if (effect.type == ModuleEffect.EffectType.DAMAGE_BOOST) {
                        totalMultiplier += effect.getDamageMultiplierForLevel(level) - 1.0f;
                    }
                    if (effect.type == ModuleEffect.EffectType.ON_HIT && effect.hitCallback != null) {
                        if (checkEnergy(coreStack, effect)) {
                            effect.hitCallback.execute(player, coreStack, def.id, level, event);
                        }
                    }
                }
            }
        }

        return damage * totalMultiplier;
    }

    /**
     * 处理玩家受伤
     * @param cachedModules 【优化】可选的已缓存活跃模块，null 则回退到直接查询
     */
    private static float processPlayerHurt(EntityPlayer player, ItemStack coreStack,
                                           float damage, LivingHurtEvent event,
                                           Map<String, Integer> cachedModules) {
        float totalReduction = 0.0f;
        float totalReflection = 0.0f;

        for (ModuleDefinition def : ModuleAutoRegistry.getAllDefinitions()) {
            int level;
            if (cachedModules != null) {
                Integer cached = cachedModules.get(def.id);
                if (cached == null) continue;
                level = cached;
            } else {
                if (!ItemMechanicalCore.isUpgradeActive(coreStack, def.id)) continue;
                level = 0;
                try { level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, def.id); } catch (Throwable ignored) {}
                if (level <= 0) continue;
            }

            // 处理 Handler
            if (def.hasHandler()) {
                EventContext ctx = new EventContext(player, coreStack, def.id, level);
                damage = def.handler.onPlayerHurt(ctx, event.getSource(), damage);
            }

            // 处理简单效果
            if (def.hasEffects()) {
                String damageType = event.getSource().getDamageType();
                for (ModuleEffect effect : def.effects) {
                    if (effect.type == ModuleEffect.EffectType.DAMAGE_REDUCTION) {
                        if (effect.damageTypes.isEmpty() || effect.damageTypes.contains(damageType)) {
                            totalReduction += effect.getReductionForLevel(level);
                        }
                    }
                    if (effect.type == ModuleEffect.EffectType.DAMAGE_REFLECTION) {
                        totalReflection += effect.getReflectionForLevel(level);
                    }
                    if (effect.type == ModuleEffect.EffectType.ON_HURT && effect.hurtCallback != null) {
                        if (checkEnergy(coreStack, effect)) {
                            effect.hurtCallback.execute(player, coreStack, def.id, level, event);
                        }
                    }
                }
            }
        }

        // 应用减伤
        totalReduction = Math.min(totalReduction, 0.9f);
        damage = damage * (1.0f - totalReduction);

        // 应用反伤
        if (totalReflection > 0 && event.getSource().getTrueSource() instanceof EntityLivingBase) {
            EntityLivingBase attacker = (EntityLivingBase) event.getSource().getTrueSource();
            float reflectedDamage = event.getAmount() * totalReflection;
            attacker.attackEntityFrom(net.minecraft.util.DamageSource.causeThornsDamage(player), reflectedDamage);
        }

        return damage;
    }

    // ==================== 攻击事件 (伤害计算前) ====================

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 【优化】尝试使用缓存
        ItemStack coreStack = getCachedCoreStack(player);
        if (coreStack.isEmpty()) return;

        Map<String, Integer> cachedModules = getCachedActiveModules(player);

        // 【优化】只遍历有 handler 的模块
        for (ModuleDefinition def : ModuleAutoRegistry.getHandlerModules()) {
            Integer level;
            if (cachedModules != null) {
                level = cachedModules.get(def.id);
                if (level == null) continue;
            } else {
                if (!ItemMechanicalCore.isUpgradeActive(coreStack, def.id)) continue;
                level = 0;
                try { level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, def.id); } catch (Throwable ignored) {}
                if (level <= 0) continue;
            }

            EventContext ctx = new EventContext(player, coreStack, def.id, level);
            if (def.handler.onPlayerAttacked(ctx, event.getSource(), event.getAmount())) {
                event.setCanceled(true);
                return;
            }
        }
    }

    // ==================== 击杀事件 ====================

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity killer = event.getSource().getTrueSource();
        if (!(killer instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) killer;

        // 【优化】尝试使用缓存
        ItemStack coreStack = getCachedCoreStack(player);
        if (coreStack.isEmpty()) return;

        EntityLivingBase target = event.getEntityLiving();
        Map<String, Integer> cachedModules = getCachedActiveModules(player);

        // 【优化】只遍历有 handler 的模块
        for (ModuleDefinition def : ModuleAutoRegistry.getHandlerModules()) {
            Integer level;
            if (cachedModules != null) {
                level = cachedModules.get(def.id);
                if (level == null) continue;
            } else {
                if (!ItemMechanicalCore.isUpgradeActive(coreStack, def.id)) continue;
                level = 0;
                try { level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, def.id); } catch (Throwable ignored) {}
                if (level <= 0) continue;
            }

            EventContext ctx = new EventContext(player, coreStack, def.id, level);
            def.handler.onPlayerKillEntity(ctx, target, event);
        }
    }

    // ==================== 交互事件 ====================

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote) return;
        processInteractEvent(event.getEntityPlayer(), (player, coreStack, def, ctx) ->
                def.handler.onRightClickBlock(ctx, event));
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote) return;
        processInteractEvent(event.getEntityPlayer(), (player, coreStack, def, ctx) ->
                def.handler.onRightClickItem(ctx, event));
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getWorld().isRemote) return;
        processInteractEvent(event.getEntityPlayer(), (player, coreStack, def, ctx) ->
                def.handler.onLeftClickBlock(ctx, event));
    }

    // ==================== 方块事件 ====================

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        if (player == null || player.world.isRemote) return;

        // 【优化】尝试使用缓存
        ItemStack coreStack = getCachedCoreStack(player);
        if (coreStack.isEmpty()) return;

        Map<String, Integer> cachedModules = getCachedActiveModules(player);

        // 【优化】只遍历有 handler 的模块
        for (ModuleDefinition def : ModuleAutoRegistry.getHandlerModules()) {
            Integer level;
            if (cachedModules != null) {
                // 使用缓存
                level = cachedModules.get(def.id);
                if (level == null) continue;
            } else {
                // 回退到直接查询
                if (!ItemMechanicalCore.isUpgradeActive(coreStack, def.id)) continue;
                level = 0;
                try { level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, def.id); } catch (Throwable ignored) {}
                if (level <= 0) continue;
            }

            EventContext ctx = new EventContext(player, coreStack, def.id, level);
            def.handler.onBlockBreak(ctx, event);
        }
    }

    private interface InteractCallback {
        void handle(EntityPlayer player, ItemStack coreStack, ModuleDefinition def, EventContext ctx);
    }

    private static void processInteractEvent(EntityPlayer player, InteractCallback callback) {
        // 【优化】尝试使用缓存
        ItemStack coreStack = getCachedCoreStack(player);
        if (coreStack.isEmpty()) return;

        Map<String, Integer> cachedModules = getCachedActiveModules(player);

        // 【优化】只遍历有 handler 的模块
        for (ModuleDefinition def : ModuleAutoRegistry.getHandlerModules()) {
            Integer level;
            if (cachedModules != null) {
                level = cachedModules.get(def.id);
                if (level == null) continue;
            } else {
                if (!ItemMechanicalCore.isUpgradeActive(coreStack, def.id)) continue;
                level = 0;
                try { level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, def.id); } catch (Throwable ignored) {}
                if (level <= 0) continue;
            }

            EventContext ctx = new EventContext(player, coreStack, def.id, level);
            callback.handle(player, coreStack, def, ctx);
        }
    }

    // ==================== 状态事件 ====================

    private static void handleModuleActivated(EntityPlayer player, ItemStack coreStack,
                                              ModuleDefinition def, int level) {
        if (def.hasHandler()) {
            EventContext ctx = new EventContext(player, coreStack, def.id, level);
            def.handler.onModuleActivated(ctx);
        }
    }

    private static void handleModuleDeactivated(EntityPlayer player, ItemStack coreStack,
                                                ModuleDefinition def, PlayerEffectState state) {
        if (def.hasHandler()) {
            EventContext ctx = new EventContext(player, coreStack, def.id, 0);
            def.handler.onModuleDeactivated(ctx);
        }
    }

    private static void handleCoreRemoved(EntityPlayer player) {
        Set<String> active = getActiveModules(player);
        ItemStack emptyStack = ItemStack.EMPTY;

        for (String moduleId : active) {
            ModuleDefinition def = ModuleAutoRegistry.getDefinition(moduleId);
            if (def != null && def.hasHandler()) {
                EventContext ctx = new EventContext(player, emptyStack, moduleId, 0);
                def.handler.onModuleDeactivated(ctx);
            }
        }

        active.clear();
        cleanupAllEffects(player);
    }

    // ==================== 辅助方法 ====================

    private static boolean shouldTrigger(String key, long currentTime, int interval, PlayerEffectState state) {
        Long lastTime = state.lastTickTimes.get(key);
        if (lastTime == null || currentTime - lastTime >= interval) {
            state.lastTickTimes.put(key, currentTime);
            return true;
        }
        return false;
    }

    private static boolean checkEnergy(ItemStack coreStack, ModuleEffect effect) {
        if (!effect.requiresEnergy) return true;
        int cost = effect.energyCost > 0 ? effect.energyCost : effect.energyPerTick;
        if (cost <= 0) return true;
        return ItemMechanicalCore.consumeEnergy(coreStack, cost);
    }

    private static void applyAttributeModifier(EntityPlayer player, String moduleId, int level,
                                               ModuleEffect effect, PlayerEffectState state) {
        if (effect.attribute == null) return;

        IAttributeInstance attr = player.getAttributeMap().getAttributeInstance(effect.attribute);
        if (attr == null) return;

        UUID modUUID = effect.modifierUUID;

        AttributeModifier existing = attr.getModifier(modUUID);
        if (existing != null) {
            attr.removeModifier(existing);
        }

        double value = effect.getValueForLevel(level);
        AttributeModifier modifier = new AttributeModifier(
                modUUID, "MechanicalCore_" + moduleId, value, effect.operation.mcValue
        );
        attr.applyModifier(modifier);

        state.activeModifiers.computeIfAbsent(moduleId, k -> new HashSet<>()).add(modUUID);
    }

    private static void applyPotionEffect(EntityPlayer player, int level, ModuleEffect effect) {
        if (effect.potion == null) return;
        int amplifier = effect.getAmplifierForLevel(level);
        PotionEffect potionEffect = new PotionEffect(
                effect.potion, effect.baseDuration, amplifier, effect.ambient, effect.showParticles
        );
        player.addPotionEffect(potionEffect);
    }

    // ==================== 清理方法 ====================

    private static void cleanupAllEffects(EntityPlayer player) {
        PlayerEffectState state = playerStates.get(player.getUniqueID());
        if (state == null) return;

        for (Map.Entry<String, Set<UUID>> entry : state.activeModifiers.entrySet()) {
            for (UUID modUUID : entry.getValue()) {
                removeModifierByUUID(player, modUUID);
            }
        }

        state.activeModifiers.clear();
        state.lastTickTimes.clear();
        state.lastSecondTicks.clear();
    }

    private static void cleanupModuleEffects(EntityPlayer player, String moduleId, PlayerEffectState state) {
        Set<UUID> modifiers = state.activeModifiers.get(moduleId);
        if (modifiers != null) {
            for (UUID modUUID : modifiers) {
                removeModifierByUUID(player, modUUID);
            }
            modifiers.clear();
        }

        state.lastTickTimes.entrySet().removeIf(e -> e.getKey().startsWith(moduleId + "_"));
        state.lastSecondTicks.entrySet().removeIf(e -> e.getKey().startsWith(moduleId + "_"));
    }

    private static void removeModifierByUUID(EntityPlayer player, UUID modUUID) {
        for (IAttributeInstance attr : player.getAttributeMap().getAllAttributes()) {
            AttributeModifier modifier = attr.getModifier(modUUID);
            if (modifier != null) {
                attr.removeModifier(modifier);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.player.getUniqueID();
        playerStates.remove(uuid);
        activeModules.remove(uuid);
        // 【优化】清除缓存
        activeModulesCache.remove(uuid);
        coreStackCache.remove(uuid);
        cacheExpireTime.remove(uuid);
        EventContext.clearPlayerCooldowns(uuid);
    }
}
