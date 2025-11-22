package com.moremod.module.integration;

import com.moremod.module.api.IModuleContext;
import com.moremod.module.service.ModuleService;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

/**
 * 事件系统软集成
 *
 * 特性:
 * - 反射检测 Forge 事件总线
 * - 可选依赖（事件系统不存在时不会崩溃）
 * - 自动注册/注销监听器
 */
public class EventBusIntegration {

    private static boolean forgeEventBusAvailable = false;
    private static Object eventBusInstance = null;
    private static Method registerMethod = null;
    private static Method unregisterMethod = null;

    static {
        tryInitialize();
    }

    /**
     * 尝试初始化事件总线集成
     */
    private static void tryInitialize() {
        try {
            // 尝试加载 Forge EventBus
            Class<?> eventBusClass = Class.forName("net.minecraftforge.common.MinecraftForge");
            Object eventBus = eventBusClass.getField("EVENT_BUS").get(null);

            // 获取注册方法
            Class<?> busClass = eventBus.getClass();
            registerMethod = busClass.getMethod("register", Object.class);
            unregisterMethod = busClass.getMethod("unregister", Object.class);

            eventBusInstance = eventBus;
            forgeEventBusAvailable = true;

            System.out.println("[EventBusIntegration] Forge EventBus detected and ready");
        } catch (Throwable t) {
            System.out.println("[EventBusIntegration] Forge EventBus not available: " + t.getMessage());
            forgeEventBusAvailable = false;
        }
    }

    /**
     * 检查事件总线是否可用
     */
    public static boolean isAvailable() {
        return forgeEventBusAvailable;
    }

    /**
     * 注册事件监听器
     *
     * @param listener 监听器对象
     * @return true 成功, false 失败
     */
    public static boolean registerListener(@Nonnull Object listener) {
        if (!forgeEventBusAvailable) {
            return false;
        }

        try {
            registerMethod.invoke(eventBusInstance, listener);
            System.out.println("[EventBusIntegration] Registered listener: " + listener.getClass().getSimpleName());
            return true;
        } catch (Throwable t) {
            System.err.println("[EventBusIntegration] Failed to register listener: " + t.getMessage());
            return false;
        }
    }

    /**
     * 注销事件监听器
     *
     * @param listener 监听器对象
     * @return true 成功, false 失败
     */
    public static boolean unregisterListener(@Nonnull Object listener) {
        if (!forgeEventBusAvailable) {
            return false;
        }

        try {
            unregisterMethod.invoke(eventBusInstance, listener);
            System.out.println("[EventBusIntegration] Unregistered listener: " + listener.getClass().getSimpleName());
            return true;
        } catch (Throwable t) {
            System.err.println("[EventBusIntegration] Failed to unregister listener: " + t.getMessage());
            return false;
        }
    }

    /**
     * 为模块自动注册事件监听器
     *
     * @param module 模块对象
     * @param context 模块上下文
     * @return true 成功, false 失败
     */
    public static boolean registerModuleListener(@Nonnull Object module, @Nonnull IModuleContext context) {
        if (!isAvailable()) {
            context.debug("Event bus not available, skipping listener registration");
            return false;
        }

        return registerListener(module);
    }

    /**
     * 为模块自动注销事件监听器
     *
     * @param module 模块对象
     * @param context 模块上下文
     * @return true 成功, false 失败
     */
    public static boolean unregisterModuleListener(@Nonnull Object module, @Nonnull IModuleContext context) {
        if (!isAvailable()) {
            return false;
        }

        return unregisterListener(module);
    }
}
