package com.moremod.module.service;

import com.moremod.module.api.*;
import com.moremod.module.fallback.NoOpModuleContainer;
import com.moremod.module.fallback.NoOpModuleContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 模块服务定位器 - Service Locator 模式
 *
 * 特性:
 * - 全局访问点
 * - 自动降级到 No-Op 实现
 * - 线程安全
 * - 可在运行时替换实现
 */
public class ModuleService {

    private static volatile IModuleContainer moduleContainer = null;
    private static volatile IModuleContext moduleContext = null;
    private static volatile boolean initialized = false;
    private static final Object lock = new Object();

    /**
     * 初始化模块服务
     *
     * @param container 模块容器
     * @param context 模块上下文
     */
    public static void initialize(@Nonnull IModuleContainer container, @Nonnull IModuleContext context) {
        synchronized (lock) {
            moduleContainer = container;
            moduleContext = context;
            initialized = true;
            System.out.println("[ModuleService] Module system initialized");
        }
    }

    /**
     * 关闭模块服务
     */
    public static void shutdown() {
        synchronized (lock) {
            if (initialized && moduleContainer != null && moduleContext != null) {
                try {
                    moduleContainer.unloadAll(moduleContext);
                } catch (Throwable t) {
                    System.err.println("[ModuleService] Error during shutdown: " + t.getMessage());
                    t.printStackTrace();
                }
            }
            moduleContainer = null;
            moduleContext = null;
            initialized = false;
            System.out.println("[ModuleService] Module system shutdown");
        }
    }

    /**
     * 获取模块容器（Null-Safe）
     *
     * @return 模块容器，如果未初始化则返回 No-Op 实现
     */
    @Nonnull
    public static IModuleContainer getContainer() {
        IModuleContainer container = moduleContainer;
        return container != null ? container : NoOpModuleContainer.INSTANCE;
    }

    /**
     * 获取模块上下文（Null-Safe）
     *
     * @return 模块上下文，如果未初始化则返回 No-Op 实现
     */
    @Nonnull
    public static IModuleContext getContext() {
        IModuleContext context = moduleContext;
        return context != null ? context : NoOpModuleContext.INSTANCE;
    }

    /**
     * 检查模块系统是否已初始化
     *
     * @return true 已初始化, false 未初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 检查模块系统是否可用
     *
     * @return true 可用, false 不可用
     */
    public static boolean isAvailable() {
        return initialized && moduleContainer != null && moduleContext != null;
    }

    /**
     * 获取指定模块（Null-Safe）
     *
     * @param moduleId 模块ID
     * @return 模块实例，不存在则返回null
     */
    @Nullable
    public static IModule getModule(@Nonnull String moduleId) {
        return getContainer().getModule(moduleId);
    }

    /**
     * 检查模块是否存在
     *
     * @param moduleId 模块ID
     * @return true 存在, false 不存在
     */
    public static boolean hasModule(@Nonnull String moduleId) {
        return getContainer().hasModule(moduleId);
    }

    /**
     * 安全地执行模块操作
     *
     * @param operation 操作
     * @return true 成功, false 失败
     */
    public static boolean safeExecute(@Nonnull Runnable operation) {
        try {
            if (isAvailable()) {
                operation.run();
                return true;
            }
            return false;
        } catch (Throwable t) {
            System.err.println("[ModuleService] Safe execution failed: " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }

    /**
     * 安全地执行模块操作（带返回值）
     *
     * @param operation 操作
     * @param <T> 返回值类型
     * @return 操作结果，失败则返回null
     */
    @Nullable
    public static <T> T safeExecute(@Nonnull SafeOperation<T> operation) {
        try {
            if (isAvailable()) {
                return operation.execute();
            }
            return null;
        } catch (Throwable t) {
            System.err.println("[ModuleService] Safe execution failed: " + t.getMessage());
            t.printStackTrace();
            return null;
        }
    }

    /**
     * 安全操作接口
     */
    @FunctionalInterface
    public interface SafeOperation<T> {
        T execute() throws Exception;
    }
}
