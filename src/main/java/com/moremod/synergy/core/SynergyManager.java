package com.moremod.synergy.core;

import com.moremod.synergy.api.IInstalledModuleView;
import com.moremod.synergy.api.IModuleProvider;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Synergy 管理器
 *
 * 单例模式，负责：
 * - 注册和管理所有 SynergyDefinition
 * - 在事件触发时检测并执行匹配的 Synergy
 * - 提供 Synergy 查询接口
 *
 * 线程安全：使用 ConcurrentHashMap 存储注册的 Synergy
 */
public class SynergyManager {

    // ==================== 单例 ====================

    private static final SynergyManager INSTANCE = new SynergyManager();

    public static SynergyManager getInstance() {
        return INSTANCE;
    }

    // ==================== 字段 ====================

    /** 注册的 Synergy 定义（按 ID 映射） */
    private final Map<String, SynergyDefinition> synergyMap = new ConcurrentHashMap<>();

    /** 按事件类型索引的 Synergy（用于快速查找） */
    private final Map<SynergyEventType, List<SynergyDefinition>> eventIndex = new ConcurrentHashMap<>();

    /** 模块提供者（桥接层） */
    private IModuleProvider moduleProvider;

    /** 是否启用 Synergy 系统 */
    private boolean enabled = true;

    /** 调试模式 */
    private boolean debugMode = false;

    // ==================== 玩家激活追踪 ====================

    /**
     * 玩家已激活的 Synergy
     * Key: 玩家 UUID
     * Value: 已激活的 Synergy ID 集合
     */
    private final Map<UUID, Set<String>> playerActivatedSynergies = new ConcurrentHashMap<>();

    /**
     * 是否需要通过方块激活才能使用 Synergy
     * 如果为 false，则所有满足条件的 Synergy 自动生效（旧行为）
     */
    private boolean requireBlockActivation = true;

    // ==================== 构造器 ====================

    private SynergyManager() {
        // 初始化事件索引
        for (SynergyEventType type : SynergyEventType.values()) {
            eventIndex.put(type, new ArrayList<>());
        }
    }

    // ==================== 初始化 ====================

    /**
     * 初始化管理器
     *
     * @param provider 模块提供者（用于获取玩家已安装模块）
     */
    public void init(@Nonnull IModuleProvider provider) {
        this.moduleProvider = Objects.requireNonNull(provider, "ModuleProvider cannot be null");
        log("SynergyManager initialized with provider: " + provider.getClass().getSimpleName());
    }

    /**
     * 检查管理器是否已初始化
     */
    public boolean isInitialized() {
        return moduleProvider != null;
    }

    // ==================== 注册 ====================

    /**
     * 注册一个 Synergy 定义
     *
     * @param definition Synergy 定义
     * @throws IllegalArgumentException 如果 ID 已存在
     */
    public void register(@Nonnull SynergyDefinition definition) {
        Objects.requireNonNull(definition, "SynergyDefinition cannot be null");

        String id = definition.getId();
        if (synergyMap.containsKey(id)) {
            throw new IllegalArgumentException("Synergy with ID '" + id + "' already registered!");
        }

        synergyMap.put(id, definition);

        // 更新事件索引
        for (SynergyEventType eventType : definition.getTriggerEvents()) {
            eventIndex.get(eventType).add(definition);
        }

        log("Registered Synergy: " + id + " (triggers: " + definition.getTriggerEvents() + ")");
    }

    /**
     * 注销一个 Synergy 定义
     *
     * @param id Synergy ID
     * @return 被移除的定义，如果不存在返回 null
     */
    @Nullable
    public SynergyDefinition unregister(@Nonnull String id) {
        SynergyDefinition removed = synergyMap.remove(id);
        if (removed != null) {
            // 从事件索引中移除
            for (SynergyEventType eventType : removed.getTriggerEvents()) {
                eventIndex.get(eventType).remove(removed);
            }
            log("Unregistered Synergy: " + id);
        }
        return removed;
    }

    /**
     * 清除所有注册的 Synergy
     */
    public void clearAll() {
        synergyMap.clear();
        for (List<SynergyDefinition> list : eventIndex.values()) {
            list.clear();
        }
        log("Cleared all Synergies");
    }

    // ==================== 查询 ====================

    /**
     * 获取指定 ID 的 Synergy 定义
     */
    @Nullable
    public SynergyDefinition get(@Nonnull String id) {
        return synergyMap.get(id);
    }

    /**
     * 获取所有注册的 Synergy 定义
     */
    @Nonnull
    public Collection<SynergyDefinition> getAll() {
        return Collections.unmodifiableCollection(synergyMap.values());
    }

    /**
     * 获取所有注册的 Synergy 定义（别名）
     */
    @Nonnull
    public Collection<SynergyDefinition> getAllSynergies() {
        return getAll();
    }

    /**
     * 获取指定事件类型的所有 Synergy 定义
     */
    @Nonnull
    public List<SynergyDefinition> getByEventType(@Nonnull SynergyEventType eventType) {
        List<SynergyDefinition> result = new ArrayList<>(eventIndex.getOrDefault(eventType, Collections.emptyList()));
        // 添加 ANY 类型的 Synergy
        if (eventType != SynergyEventType.ANY) {
            result.addAll(eventIndex.getOrDefault(SynergyEventType.ANY, Collections.emptyList()));
        }
        return result;
    }

    /**
     * 获取玩家当前满足条件的所有 Synergy
     */
    @Nonnull
    public List<SynergyDefinition> getActiveSynergies(@Nonnull EntityPlayer player, @Nonnull SynergyEventType eventType) {
        if (!enabled || !isInitialized()) {
            return Collections.emptyList();
        }

        SynergyContext context = createContext(player, eventType, null, null, 0);
        if (context == null) {
            return Collections.emptyList();
        }

        List<SynergyDefinition> result = new ArrayList<>();
        for (SynergyDefinition def : getByEventType(eventType)) {
            if (def.matches(context)) {
                result.add(def);
            }
        }

        // 按优先级排序
        result.sort(Comparator.comparingInt(SynergyDefinition::getPriority));
        return result;
    }

    // ==================== 事件处理 ====================

    /**
     * 处理事件并触发匹配的 Synergy
     *
     * 这是主要的入口点，由 SynergyEventHandler 调用
     *
     * @param player 触发事件的玩家
     * @param eventType 事件类型
     * @param event 原始 Forge 事件（可选）
     * @param target 目标实体（可选）
     * @param damage 伤害值（可选）
     * @return 触发的 Synergy 数量
     */
    public int processEvent(@Nonnull EntityPlayer player,
                            @Nonnull SynergyEventType eventType,
                            @Nullable Event event,
                            @Nullable EntityLivingBase target,
                            float damage) {
        if (!enabled || !isInitialized()) {
            return 0;
        }

        SynergyContext context = createContext(player, eventType, event, target, damage);
        if (context == null) {
            return 0;
        }

        int triggered = 0;
        List<SynergyDefinition> candidates = getByEventType(eventType);

        // 按优先级排序
        candidates.sort(Comparator.comparingInt(SynergyDefinition::getPriority));

        for (SynergyDefinition def : candidates) {
            try {
                // 检查玩家是否已通过方块激活此 Synergy
                if (!isSynergyActivatedForPlayer(player, def.getId())) {
                    continue; // 未激活，跳过
                }

                if (def.matches(context)) {
                    if (debugMode) {
                        log("Triggering Synergy: " + def.getId() + " for player " + player.getName());
                    }
                    def.execute(context);
                    triggered++;
                }
            } catch (Exception e) {
                System.err.println("[Synergy] Error processing " + def.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        return triggered;
    }

    /**
     * 简化版事件处理（无目标、无伤害）
     */
    public int processEvent(@Nonnull EntityPlayer player, @Nonnull SynergyEventType eventType) {
        return processEvent(player, eventType, null, null, 0);
    }

    /**
     * 创建 Synergy 上下文
     */
    @Nullable
    private SynergyContext createContext(EntityPlayer player,
                                         SynergyEventType eventType,
                                         Event event,
                                         EntityLivingBase target,
                                         float damage) {
        if (moduleProvider == null) {
            return null;
        }

        ItemStack core = moduleProvider.getMechanicalCore(player);
        if (core == null || core.isEmpty()) {
            return null;
        }

        List<IInstalledModuleView> modules = moduleProvider.getInstalledModules(player);

        return SynergyContext.builder(player)
                .mechanicalCore(core)
                .modules(modules)
                .eventType(eventType)
                .triggerEvent(event)
                .target(target)
                .originalDamage(damage)
                .build();
    }

    // ==================== 配置 ====================

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        log("Synergy system " + (enabled ? "enabled" : "disabled"));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
        log("Debug mode " + (debug ? "enabled" : "disabled"));
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * 获取模块提供者（用于外部查询）
     */
    @Nullable
    public IModuleProvider getModuleProvider() {
        return moduleProvider;
    }

    /**
     * 设置是否需要通过方块激活
     */
    public void setRequireBlockActivation(boolean require) {
        this.requireBlockActivation = require;
        log("Block activation requirement: " + (require ? "enabled" : "disabled"));
    }

    /**
     * 获取是否需要通过方块激活
     */
    public boolean isRequireBlockActivation() {
        return requireBlockActivation;
    }

    // ==================== 玩家 Synergy 激活管理 ====================

    /**
     * 激活玩家的指定 Synergy
     * @param player 玩家
     * @param synergyId Synergy ID
     * @return 是否成功激活
     */
    public boolean activateSynergyForPlayer(@Nonnull EntityPlayer player, @Nonnull String synergyId) {
        if (!synergyMap.containsKey(synergyId)) {
            log("Cannot activate unknown synergy: " + synergyId);
            return false;
        }

        UUID playerId = player.getUniqueID();
        Set<String> activated = playerActivatedSynergies.computeIfAbsent(playerId, k -> new CopyOnWriteArraySet<>());
        boolean added = activated.add(synergyId);

        if (added && debugMode) {
            log("Activated synergy '" + synergyId + "' for player " + player.getName());
        }
        return added;
    }

    /**
     * 停用玩家的指定 Synergy
     * @param player 玩家
     * @param synergyId Synergy ID
     * @return 是否成功停用
     */
    public boolean deactivateSynergyForPlayer(@Nonnull EntityPlayer player, @Nonnull String synergyId) {
        UUID playerId = player.getUniqueID();
        Set<String> activated = playerActivatedSynergies.get(playerId);
        if (activated == null) {
            return false;
        }

        boolean removed = activated.remove(synergyId);
        if (removed && debugMode) {
            log("Deactivated synergy '" + synergyId + "' for player " + player.getName());
        }
        return removed;
    }

    /**
     * 停用玩家的所有 Synergy
     * @param player 玩家
     */
    public void deactivateAllSynergiesForPlayer(@Nonnull EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        playerActivatedSynergies.remove(playerId);
        if (debugMode) {
            log("Deactivated all synergies for player " + player.getName());
        }
    }

    /**
     * 检查玩家是否已激活指定 Synergy
     * @param player 玩家
     * @param synergyId Synergy ID
     * @return 是否已激活
     */
    public boolean isSynergyActivatedForPlayer(@Nonnull EntityPlayer player, @Nonnull String synergyId) {
        if (!requireBlockActivation) {
            return true; // 如果不需要方块激活，则所有 Synergy 都视为已激活
        }

        UUID playerId = player.getUniqueID();
        Set<String> activated = playerActivatedSynergies.get(playerId);
        return activated != null && activated.contains(synergyId);
    }

    /**
     * 获取玩家已激活的所有 Synergy ID
     * @param player 玩家
     * @return 已激活的 Synergy ID 集合（不可修改）
     */
    @Nonnull
    public Set<String> getActivatedSynergiesForPlayer(@Nonnull EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        Set<String> activated = playerActivatedSynergies.get(playerId);
        return activated != null ? Collections.unmodifiableSet(activated) : Collections.emptySet();
    }

    /**
     * 清理玩家数据（玩家登出时调用）
     * @param playerId 玩家 UUID
     */
    public void cleanupPlayer(@Nonnull UUID playerId) {
        playerActivatedSynergies.remove(playerId);
    }

    // ==================== 统计 ====================

    /**
     * 获取已注册的 Synergy 数量
     */
    public int getSynergyCount() {
        return synergyMap.size();
    }

    /**
     * 获取统计信息
     */
    public String getStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Synergy System Stats ===\n");
        sb.append("Enabled: ").append(enabled).append("\n");
        sb.append("Initialized: ").append(isInitialized()).append("\n");
        sb.append("Total Synergies: ").append(synergyMap.size()).append("\n");
        sb.append("By Event Type:\n");
        for (SynergyEventType type : SynergyEventType.values()) {
            int count = eventIndex.get(type).size();
            if (count > 0) {
                sb.append("  - ").append(type.name()).append(": ").append(count).append("\n");
            }
        }
        return sb.toString();
    }

    // ==================== 工具方法 ====================

    private void log(String message) {
        System.out.println("[SynergyManager] " + message);
    }
}
