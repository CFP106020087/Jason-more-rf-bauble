package com.moremod.upgrades;

import com.moremod.upgrades.api.IUpgradeModule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 模块注册中心（Mechanical Core 升级模块系统）
 *
 * 功能：
 *  ✔ 注册所有 IUpgradeModule 实例
 *  ✔ 透过 moduleId 获取模块
 *  ✔ MechanicalCoreExtended 用它来调用模块逻辑
 *
 * 使用方式：
 *   ModuleRegistry.register(MagicAbsorbModule.INSTANCE);
 *   ModuleRegistry.register(NeuralSynchronizerModule.INSTANCE);
 */
public class ModuleRegistry {

    /** 所有注册过的模块实例（按 moduleId 映射） */
    private static final Map<String, IUpgradeModule> MODULE_MAP = new HashMap<>();

    /**
     * 注册模块（通常在启动阶段由 ModUpgradeItems 调用一次）
     */
    public static void register(IUpgradeModule module) {
        if (module == null) return;

        String id = module.getModuleId();
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException(
                "moduleId cannot be null or empty! module=" + module.getClass().getName());
        }

        MODULE_MAP.put(id, module);
    }

    /**
     * 根据 moduleId 获取模块实例
     */
    public static IUpgradeModule get(String moduleId) {
        return MODULE_MAP.get(moduleId);
    }

    /**
     * 获取所有模块（只读）
     */
    public static Map<String, IUpgradeModule> getAllModules() {
        return Collections.unmodifiableMap(MODULE_MAP);
    }

    /**
     * 供 ModUpgradeItems 用来初始化模块系统
     * （你可以扩展初始化内容）
     */
    public static void init() {
        // 目前不需要内容，但保留接口方便未来扩展
    }
}
