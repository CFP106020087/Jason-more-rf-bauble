package com.moremod.synergy.core;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Synergy 注册表 - 管理所有 Synergy 定义
 *
 * 说明：
 * - 单例模式，线程安全
 * - 不依赖 ModuleRegistry，完全独立
 * - 支持动态注册和查询
 */
public final class SynergyRegistry {

    private static final SynergyRegistry INSTANCE = new SynergyRegistry();

    private final Map<String, SynergyDefinition> synergies = new LinkedHashMap<>();
    private final Object lock = new Object();

    private SynergyRegistry() {
        // 私有构造器，单例模式
    }

    public static SynergyRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 注册 Synergy
     *
     * @param synergy Synergy 定义
     * @throws IllegalArgumentException 如果 ID 已存在
     */
    public void register(SynergyDefinition synergy) {
        if (synergy == null) {
            throw new IllegalArgumentException("Synergy definition cannot be null");
        }

        synchronized (lock) {
            String id = synergy.getId();
            if (synergies.containsKey(id)) {
                throw new IllegalArgumentException("Synergy ID already registered: " + id);
            }
            synergies.put(id, synergy);
            System.out.println("[SynergyRegistry] Registered synergy: " + synergy);
        }
    }

    /**
     * 批量注册 Synergy
     *
     * @param synergies Synergy 定义列表
     */
    public void registerAll(SynergyDefinition... synergies) {
        for (SynergyDefinition synergy : synergies) {
            register(synergy);
        }
    }

    /**
     * 获取 Synergy
     *
     * @param id Synergy ID
     * @return Synergy 定义，如果不存在则返回 null
     */
    public SynergyDefinition get(String id) {
        synchronized (lock) {
            return synergies.get(id);
        }
    }

    /**
     * 检查 Synergy 是否存在
     *
     * @param id Synergy ID
     * @return true 表示存在
     */
    public boolean has(String id) {
        synchronized (lock) {
            return synergies.containsKey(id);
        }
    }

    /**
     * 获取所有已注册的 Synergy
     *
     * @return Synergy 定义列表（不可修改）
     */
    public List<SynergyDefinition> getAllSynergies() {
        synchronized (lock) {
            return Collections.unmodifiableList(new ArrayList<>(synergies.values()));
        }
    }

    /**
     * 获取所有已启用的 Synergy
     *
     * @return 已启用的 Synergy 定义列表
     */
    public List<SynergyDefinition> getEnabledSynergies() {
        synchronized (lock) {
            return synergies.values().stream()
                    .filter(SynergyDefinition::isEnabled)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 根据所需模块查找 Synergy
     *
     * @param installedModuleIds 已安装的模块 ID 集合
     * @return 可能激活的 Synergy 列表（已排序）
     */
    public List<SynergyDefinition> findApplicableSynergies(Set<String> installedModuleIds) {
        synchronized (lock) {
            return synergies.values().stream()
                    .filter(SynergyDefinition::isEnabled)
                    .filter(s -> installedModuleIds.containsAll(s.getRequiredModules()))
                    .sorted(Comparator.comparingInt(SynergyDefinition::getPriority))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 取消注册 Synergy（谨慎使用）
     *
     * @param id Synergy ID
     * @return 被移除的 Synergy 定义，如果不存在则返回 null
     */
    public SynergyDefinition unregister(String id) {
        synchronized (lock) {
            SynergyDefinition removed = synergies.remove(id);
            if (removed != null) {
                System.out.println("[SynergyRegistry] Unregistered synergy: " + id);
            }
            return removed;
        }
    }

    /**
     * 清空所有注册（谨慎使用）
     */
    public void clear() {
        synchronized (lock) {
            synergies.clear();
            System.out.println("[SynergyRegistry] Cleared all synergies");
        }
    }

    /**
     * 获取注册数量
     *
     * @return Synergy 数量
     */
    public int size() {
        synchronized (lock) {
            return synergies.size();
        }
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return String.format("SynergyRegistry[%d synergies registered]", synergies.size());
        }
    }
}
