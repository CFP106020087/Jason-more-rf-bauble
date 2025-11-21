package com.moremod.synergy.init;

import com.moremod.synergy.bridge.ExistingModuleBridge;
import com.moremod.synergy.builtin.CombatEchoSynergy;
import com.moremod.synergy.builtin.EnergyLoopSynergy;
import com.moremod.synergy.builtin.SurvivalShieldSynergy;
import com.moremod.synergy.core.SynergyManager;
import com.moremod.synergy.core.SynergyRegistry;

/**
 * Synergy 系统启动器
 *
 * 说明：
 * - 唯一需要在主 mod 类中调用的类
 * - 负责初始化 Synergy 系统的所有组件
 * - 如果要移除 Synergy 包，只需删除主 mod 类中对此类的调用即可
 *
 * 使用方式：
 * <pre>
 * {@code
 * // 在你的主 mod 类的 preInit 或 init 中调用：
 * SynergyBootstrap.initialize();
 * }
 * </pre>
 *
 * 移除方式：
 * - 注释掉或删除上述调用
 * - 删除整个 com.moremod.synergy 包
 * - 编译时会有编译错误，但不影响游戏运行
 */
public final class SynergyBootstrap {

    private static boolean initialized = false;

    private SynergyBootstrap() {
        // 工具类，禁止实例化
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 初始化 Synergy 系统
     *
     * 说明：
     * - 只能调用一次
     * - 应该在 FMLPreInitializationEvent 或 FMLInitializationEvent 中调用
     * - 如果调用多次，后续调用会被忽略
     */
    public static void initialize() {
        if (initialized) {
            System.out.println("[SynergyBootstrap] Already initialized, skipping.");
            return;
        }

        System.out.println("[SynergyBootstrap] Initializing Synergy System...");

        try {
            // 步骤 1：设置模块提供者（桥接现有系统）
            initializeModuleProvider();

            // 步骤 2：注册内置 Synergy 规则
            registerBuiltinSynergies();

            // 步骤 3：完成初始化
            initialized = true;

            System.out.println("[SynergyBootstrap] Synergy System initialized successfully!");
            System.out.println("[SynergyBootstrap] Registered " +
                    SynergyRegistry.getInstance().size() + " synergies.");

        } catch (Exception e) {
            System.err.println("[SynergyBootstrap] Failed to initialize Synergy System:");
            e.printStackTrace();
            throw new RuntimeException("Synergy System initialization failed", e);
        }
    }

    /**
     * 初始化模块提供者
     */
    private static void initializeModuleProvider() {
        System.out.println("[SynergyBootstrap] Setting up module provider bridge...");

        // 使用现有模块系统的桥接器
        SynergyManager.getInstance().setModuleProvider(ExistingModuleBridge.getInstance());

        System.out.println("[SynergyBootstrap] Module provider bridge set up successfully.");
    }

    /**
     * 注册内置 Synergy 规则
     */
    private static void registerBuiltinSynergies() {
        System.out.println("[SynergyBootstrap] Registering built-in synergies...");

        SynergyRegistry registry = SynergyRegistry.getInstance();

        // 注册能量循环 Synergy
        try {
            registry.register(EnergyLoopSynergy.create());
            System.out.println("[SynergyBootstrap] Registered: " + EnergyLoopSynergy.ID);
        } catch (Exception e) {
            System.err.println("[SynergyBootstrap] Failed to register EnergyLoopSynergy:");
            e.printStackTrace();
        }

        // 注册战斗回响 Synergy
        try {
            registry.register(CombatEchoSynergy.create());
            System.out.println("[SynergyBootstrap] Registered: " + CombatEchoSynergy.ID);
        } catch (Exception e) {
            System.err.println("[SynergyBootstrap] Failed to register CombatEchoSynergy:");
            e.printStackTrace();
        }

        // 注册生存护盾 Synergy
        try {
            registry.register(SurvivalShieldSynergy.create());
            System.out.println("[SynergyBootstrap] Registered: " + SurvivalShieldSynergy.ID);
        } catch (Exception e) {
            System.err.println("[SynergyBootstrap] Failed to register SurvivalShieldSynergy:");
            e.printStackTrace();
        }

        System.out.println("[SynergyBootstrap] Built-in synergies registered.");
    }

    /**
     * 检查 Synergy 系统是否已初始化
     *
     * @return true 表示已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取系统状态信息（用于调试）
     *
     * @return 状态信息字符串
     */
    public static String getStatusInfo() {
        if (!initialized) {
            return "Synergy System: NOT INITIALIZED";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Synergy System: INITIALIZED\n");
        sb.append("Registered Synergies: ").append(SynergyRegistry.getInstance().size()).append("\n");
        sb.append("Module Provider: ").append(
                SynergyManager.getInstance().getModuleProvider() != null ? "SET" : "NOT SET"
        ).append("\n");

        return sb.toString();
    }
}
