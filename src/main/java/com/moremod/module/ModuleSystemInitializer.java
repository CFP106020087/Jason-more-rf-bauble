package com.moremod.module;

import com.moremod.module.api.IModuleContainer;
import com.moremod.module.api.IModuleContext;
import com.moremod.module.example.EnergyBoostModule;
import com.moremod.module.impl.ModuleContainerImpl;
import com.moremod.module.impl.ModuleContextImpl;
import com.moremod.module.integration.CapabilityIntegration;
import com.moremod.module.integration.EventBusIntegration;
import com.moremod.module.service.ModuleService;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 模块系统初始化器
 *
 * 使用方法：
 * 1. 在 Mod 主类的 preInit 中调用 initialize()
 * 2. 在 Mod 主类的 postInit 中调用 loadModules()
 * 3. 在游戏关闭时调用 shutdown()
 *
 * 示例：
 * <pre>
 * {@code
 * @Mod.EventHandler
 * public void preInit(FMLPreInitializationEvent event) {
 *     ModuleSystemInitializer.initialize(event.getSide().isClient());
 * }
 *
 * @Mod.EventHandler
 * public void postInit(FMLPostInitializationEvent event) {
 *     ModuleSystemInitializer.loadModules();
 * }
 * }
 * </pre>
 */
public class ModuleSystemInitializer {

    private static boolean initialized = false;
    private static IModuleContainer container = null;
    private static IModuleContext context = null;

    /**
     * 初始化模块系统
     *
     * @param isClientSide 是否为客户端
     */
    public static void initialize(boolean isClientSide) {
        if (initialized) {
            System.out.println("[ModuleSystemInitializer] Already initialized, skipping");
            return;
        }

        try {
            System.out.println("[ModuleSystemInitializer] Initializing module system...");

            // 创建容器和上下文
            container = new ModuleContainerImpl(true);  // debug = true
            context = new ModuleContextImpl(container, isClientSide, true);

            // 注册服务到上下文
            registerServices((ModuleContextImpl) context);

            // 初始化 Service Locator
            ModuleService.initialize(container, context);

            // 注册模块
            registerModules();

            // 初始化所有模块
            container.initializeAll(context);

            initialized = true;
            System.out.println("[ModuleSystemInitializer] Module system initialized successfully");

        } catch (Throwable t) {
            System.err.println("[ModuleSystemInitializer] Failed to initialize module system: " + t.getMessage());
            t.printStackTrace();

            // 即使失败也不崩溃，使用 No-Op 实现
            initialized = false;
        }
    }

    /**
     * 加载所有模块
     */
    public static void loadModules() {
        if (!initialized || container == null || context == null) {
            System.out.println("[ModuleSystemInitializer] Module system not initialized, skipping load");
            return;
        }

        try {
            System.out.println("[ModuleSystemInitializer] Loading modules...");
            container.loadAll(context);
            System.out.println("[ModuleSystemInitializer] Modules loaded successfully");
        } catch (Throwable t) {
            System.err.println("[ModuleSystemInitializer] Failed to load modules: " + t.getMessage());
            t.printStackTrace();
        }
    }

    /**
     * 关闭模块系统
     */
    public static void shutdown() {
        if (!initialized) {
            return;
        }

        try {
            System.out.println("[ModuleSystemInitializer] Shutting down module system...");
            ModuleService.shutdown();
            initialized = false;
            container = null;
            context = null;
            System.out.println("[ModuleSystemInitializer] Module system shut down successfully");
        } catch (Throwable t) {
            System.err.println("[ModuleSystemInitializer] Error during shutdown: " + t.getMessage());
            t.printStackTrace();
        }
    }

    /**
     * 注册所有模块
     */
    private static void registerModules() {
        if (container == null) {
            return;
        }

        // 注册示例模块
        container.registerModule(new EnergyBoostModule());

        // 在此处注册其他模块
        // container.registerModule(new YourCustomModule());
    }

    /**
     * 注册服务到上下文
     */
    private static void registerServices(ModuleContextImpl context) {
        // 注册事件总线集成服务
        if (EventBusIntegration.isAvailable()) {
            context.registerService("EventBus", EventBusIntegration.class);
            System.out.println("[ModuleSystemInitializer] EventBus service registered");
        }

        // 注册 Capability 集成服务
        if (CapabilityIntegration.isAvailable()) {
            context.registerService("Capability", CapabilityIntegration.class);
            System.out.println("[ModuleSystemInitializer] Capability service registered");
        }

        // 可以注册其他服务
        // context.registerService(YourServiceClass.class, yourServiceInstance);
    }

    /**
     * 检查模块系统是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
