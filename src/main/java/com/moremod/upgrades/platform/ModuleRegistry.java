package com.moremod.upgrades.platform;

import com.moremod.upgrades.api.IUpgradeModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * 模块注册表
 *
 * 功能：
 * - 注册和管理所有 IUpgradeModule
 * - 按类别组织模块
 * - 提供模块查询功能
 *
 * 设计：
 * - 单例模式
 * - 线程安全
 */
public class ModuleRegistry {

    private static final ModuleRegistry INSTANCE = new ModuleRegistry();

    private final Map<String, IUpgradeModule> modules = new LinkedHashMap<>();
    private final Map<String, List<IUpgradeModule>> modulesByCategory = new HashMap<>();
    private boolean initialized = false;

    private ModuleRegistry() {}

    public static ModuleRegistry getInstance() {
        return INSTANCE;
    }

    // ===== 模块注册 =====

    /**
     * 注册模块
     */
    public synchronized void register(@Nonnull IUpgradeModule module) {
        String moduleId = normalizeId(module.getModuleId());

        if (modules.containsKey(moduleId)) {
            System.err.println("[ModuleRegistry] 模块已注册，跳过: " + moduleId);
            return;
        }

        modules.put(moduleId, module);
        System.out.println("[ModuleRegistry] 注册模块: " + moduleId + " (" + module.getDisplayName() + ")");
    }

    /**
     * 批量注册模块
     */
    public synchronized void registerAll(@Nonnull IUpgradeModule... modulesToRegister) {
        for (IUpgradeModule module : modulesToRegister) {
            register(module);
        }
    }

    /**
     * 标记初始化完成
     */
    public synchronized void markInitialized() {
        if (!initialized) {
            initialized = true;
            System.out.println("[ModuleRegistry] 模块注册完成，共 " + modules.size() + " 个模块");
        }
    }

    // ===== 模块查询 =====

    /**
     * 获取模块
     */
    @Nullable
    public IUpgradeModule getModule(@Nonnull String moduleId) {
        return modules.get(normalizeId(moduleId));
    }

    /**
     * 检查模块是否存在
     */
    public boolean hasModule(@Nonnull String moduleId) {
        return modules.containsKey(normalizeId(moduleId));
    }

    /**
     * 获取所有模块
     */
    @Nonnull
    public Collection<IUpgradeModule> getAllModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    /**
     * 获取所有模块ID
     */
    @Nonnull
    public Set<String> getAllModuleIds() {
        return Collections.unmodifiableSet(modules.keySet());
    }

    /**
     * 获取模块数量
     */
    public int getModuleCount() {
        return modules.size();
    }

    // ===== 工具方法 =====

    private static String normalizeId(String moduleId) {
        return moduleId == null ? "" : moduleId.trim().toUpperCase();
    }

    /**
     * 重置注册表（仅用于测试）
     */
    public synchronized void reset() {
        modules.clear();
        modulesByCategory.clear();
        initialized = false;
        System.out.println("[ModuleRegistry] 注册表已重置");
    }

    @Override
    public String toString() {
        return String.format("ModuleRegistry{modules=%d, initialized=%s}",
                modules.size(), initialized);
    }
}
