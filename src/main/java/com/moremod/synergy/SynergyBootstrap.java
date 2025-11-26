package com.moremod.synergy;

import com.moremod.synergy.bridge.ExistingModuleBridge;
import com.moremod.synergy.core.SynergyEventHandler;
import com.moremod.synergy.core.SynergyManager;
import com.moremod.synergy.examples.ExampleSynergies;
import com.moremod.synergy.synergies.*;
import net.minecraftforge.common.MinecraftForge;

/**
 * Synergy 系统启动器
 *
 * 负责初始化和注册 Synergy 系统。
 *
 * 使用方式：
 *   在主 Mod 类的 init() 或 postInit() 阶段调用：
 *   SynergyBootstrap.init();
 *
 * 移除方式：
 *   1. 删除或注释掉对 SynergyBootstrap.init() 的调用
 *   2. 可选：删除整个 com.moremod.synergy 包
 *   3. 不需要修改任何其他代码
 */
public class SynergyBootstrap {

    private static boolean initialized = false;
    private static SynergyEventHandler eventHandler;

    /**
     * 初始化 Synergy 系统
     *
     * 这个方法是安全的，可以多次调用（只会初始化一次）。
     */
    public static void init() {
        if (initialized) {
            System.out.println("[Synergy] System already initialized, skipping...");
            return;
        }

        try {
            System.out.println("[Synergy] ========== Initializing Synergy System ==========");

            // 1. 初始化 SynergyManager，设置模块提供者
            SynergyManager manager = SynergyManager.getInstance();
            manager.init(ExistingModuleBridge.getInstance());
            System.out.println("[Synergy] ✓ SynergyManager initialized with ExistingModuleBridge");

            // 2. 注册事件处理器
            eventHandler = new SynergyEventHandler();
            MinecraftForge.EVENT_BUS.register(eventHandler);
            System.out.println("[Synergy] ✓ SynergyEventHandler registered");

            // 3. 注册示例 Synergy
            ExampleSynergies.registerAll();
            System.out.println("[Synergy] ✓ Example synergies registered");

            // 4. 注册 Phase 1 Synergies (简化版)
            Phase1Synergies.registerAll(manager);
            System.out.println("[Synergy] ✓ Phase 1 synergies registered");

            // 5. 注册高级 Synergy (复杂版 - Phase 2)
            registerAdvancedSynergies(manager);
            System.out.println("[Synergy] ✓ Advanced synergies registered");

            // 6. 打印统计信息
            System.out.println("[Synergy] " + manager.getStats());

            initialized = true;
            System.out.println("[Synergy] ========== Synergy System Ready ==========\n");

        } catch (Exception e) {
            System.err.println("[Synergy] ❌ Failed to initialize Synergy system: " + e.getMessage());
            e.printStackTrace();
            initialized = false;
        }
    }

    /**
     * 关闭 Synergy 系统
     *
     * 清理所有注册的 Synergy 和事件监听器。
     */
    public static void shutdown() {
        if (!initialized) {
            return;
        }

        try {
            System.out.println("[Synergy] Shutting down Synergy system...");

            // 注销事件处理器
            if (eventHandler != null) {
                MinecraftForge.EVENT_BUS.unregister(eventHandler);
                eventHandler = null;
            }

            // 清除所有 Synergy
            SynergyManager.getInstance().clearAll();

            initialized = false;
            System.out.println("[Synergy] System shut down successfully");

        } catch (Exception e) {
            System.err.println("[Synergy] Error during shutdown: " + e.getMessage());
        }
    }

    /**
     * 检查 Synergy 系统是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 启用/禁用 Synergy 系统
     *
     * 禁用后，所有 Synergy 效果将停止触发，但系统保持初始化状态。
     */
    public static void setEnabled(boolean enabled) {
        SynergyManager.getInstance().setEnabled(enabled);
    }

    /**
     * 启用/禁用调试模式
     *
     * 调试模式会输出额外的日志信息。
     */
    public static void setDebugMode(boolean debug) {
        SynergyManager.getInstance().setDebugMode(debug);
    }

    /**
     * 获取 SynergyManager 实例
     *
     * 用于外部代码注册自定义 Synergy。
     */
    public static SynergyManager getManager() {
        return SynergyManager.getInstance();
    }

    /**
     * 注册所有高级 Synergy (Phase 2 - 复杂版)
     *
     * 包含 6 大类共 18 个 Synergy:
     * - 空间/维度类 (3): Rift Walker, Gravity Anchor, Dimensional Pocket
     * - 时间类 (3): Temporal Debt, Causality Loop, Echo Chamber
     * - 能量/资源类 (3): Meltdown Protocol, Parasitic Link, Energy Weaving
     * - 战斗规则类 (3): Counter Weave, Glass Cannon, Void Strike
     * - AI/实体类 (3): Hive Mind, Corruption Seed, Pack Hunter
     * - 环境/领域类 (3): Domain Expansion, Reality Fracture, Sanctuary
     *
     * 注意: Phase 1 简化版 Synergies 在 Phase1Synergies 中注册
     */
    private static void registerAdvancedSynergies(SynergyManager manager) {
        System.out.println("[Synergy] Registering advanced synergies...");

        // 空间/维度类
        SpatialSynergies.registerAll(manager);

        // 时间类
        TemporalSynergies.registerAll(manager);

        // 能量/资源类
        EnergySynergies.registerAll(manager);

        // 战斗规则类
        CombatSynergies.registerAll(manager);

        // AI/实体类
        EntitySynergies.registerAll(manager);

        // 环境/领域类
        DomainSynergies.registerAll(manager);

        System.out.println("[Synergy] Total advanced synergies: 18");
    }
}
