package com.moremod.upgrades;

import com.moremod.upgrades.api.IUpgradeModule;
import com.moremod.capability.module.IMechCoreModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 模块注册中心（Mechanical Core 升级模块系统）
 *
 * 功能：
 *  ✔ 注册所有模块实例（旧 IUpgradeModule + 新 IMechCoreModule）
 *  ✔ 透过 moduleId 获取模块
 *  ✔ 支持新旧系统并存（迁移期间）
 *
 * 使用方式：
 *   ModuleRegistry.register(MagicAbsorbModule.INSTANCE);      // 旧系统
 *   ModuleRegistry.registerNew(FlightModule.INSTANCE);        // 新系统
 */
public class ModuleRegistry {

    private static final Logger logger = LogManager.getLogger(ModuleRegistry.class);

    /** 旧系统模块映射（兼容性） */
    private static final Map<String, IUpgradeModule> MODULE_MAP = new HashMap<>();

    /** 新系统模块映射 */
    private static final Map<String, IMechCoreModule> NEW_MODULE_MAP = new HashMap<>();

    // ────────────────────────────────────────────────────────────
    // 旧系统支持（兼容性）
    // ────────────────────────────────────────────────────────────

    /**
     * 注册旧系统模块（兼容性）
     */
    @Deprecated
    public static void register(IUpgradeModule module) {
        if (module == null) return;

        String id = module.getModuleId();
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException(
                "moduleId cannot be null or empty! module=" + module.getClass().getName());
        }

        MODULE_MAP.put(id, module);
        logger.debug("Registered old module: {}", id);
    }

    /**
     * 获取旧系统模块
     */
    @Deprecated
    public static IUpgradeModule get(String moduleId) {
        return MODULE_MAP.get(moduleId);
    }

    /**
     * 获取所有旧系统模块（只读）
     */
    @Deprecated
    public static Map<String, IUpgradeModule> getAllModules() {
        return Collections.unmodifiableMap(MODULE_MAP);
    }

    // ────────────────────────────────────────────────────────────
    // 新系统支持
    // ────────────────────────────────────────────────────────────

    /**
     * 注册新系统模块
     */
    public static void registerNew(IMechCoreModule module) {
        if (module == null) return;

        String id = module.getModuleId();
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException(
                "moduleId cannot be null or empty! module=" + module.getClass().getName());
        }

        NEW_MODULE_MAP.put(id, module);
        logger.info("Registered new module: {} [{}]", module.getDisplayName(), id);
    }

    /**
     * 获取新系统模块
     */
    public static IMechCoreModule getNew(String moduleId) {
        return NEW_MODULE_MAP.get(moduleId);
    }

    /**
     * 获取所有新系统模块（只读）
     */
    public static Map<String, IMechCoreModule> getAllNewModules() {
        return Collections.unmodifiableMap(NEW_MODULE_MAP);
    }

    // ────────────────────────────────────────────────────────────
    // 初始化
    // ────────────────────────────────────────────────────────────

    /**
     * 初始化模块系统
     * 在 Mod 启动时调用
     */
    public static void init() {
        logger.info("Initializing Module Registry...");
        logger.info("Old modules: {}, New modules: {}",
                   MODULE_MAP.size(),
                   NEW_MODULE_MAP.size());
    }
}
