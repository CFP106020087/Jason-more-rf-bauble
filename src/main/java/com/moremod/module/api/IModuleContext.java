package com.moremod.module.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 模块上下文接口 - 提供模块运行时环境和服务访问
 *
 * 设计原则:
 * - 服务可选: 所有服务都可能不存在（返回null）
 * - 无硬依赖: 通过字符串标识访问服务，避免编译时依赖
 * - 失败安全: 服务不可用时模块应能继续运行
 */
public interface IModuleContext {

    /**
     * 获取服务（通过类型）
     * @param serviceClass 服务类
     * @param <T> 服务类型
     * @return 服务实例，不存在则返回null
     */
    @Nullable
    <T> T getService(@Nonnull Class<T> serviceClass);

    /**
     * 获取服务（通过名称）
     * @param serviceName 服务名称
     * @return 服务实例，不存在则返回null
     */
    @Nullable
    Object getService(@Nonnull String serviceName);

    /**
     * 检查服务是否可用
     * @param serviceClass 服务类
     * @return true 可用, false 不可用
     */
    default boolean hasService(@Nonnull Class<?> serviceClass) {
        return getService(serviceClass) != null;
    }

    /**
     * 检查服务是否可用（通过名称）
     * @param serviceName 服务名称
     * @return true 可用, false 不可用
     */
    default boolean hasService(@Nonnull String serviceName) {
        return getService(serviceName) != null;
    }

    /**
     * 获取模块容器
     * @return 模块容器
     */
    @Nonnull
    IModuleContainer getModuleContainer();

    /**
     * 获取配置值
     * @param key 配置键
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 配置值
     */
    @Nullable
    <T> T getConfig(@Nonnull String key, @Nullable T defaultValue);

    /**
     * 是否处于客户端
     * @return true 客户端, false 服务端
     */
    boolean isClientSide();

    /**
     * 是否处于服务端
     * @return true 服务端, false 客户端
     */
    default boolean isServerSide() {
        return !isClientSide();
    }

    /**
     * 记录日志
     * @param level 日志级别 ("debug", "info", "warn", "error")
     * @param message 日志消息
     */
    void log(@Nonnull String level, @Nonnull String message);

    /**
     * 记录调试日志
     * @param message 消息
     */
    default void debug(@Nonnull String message) {
        log("debug", message);
    }

    /**
     * 记录信息日志
     * @param message 消息
     */
    default void info(@Nonnull String message) {
        log("info", message);
    }

    /**
     * 记录警告日志
     * @param message 消息
     */
    default void warn(@Nonnull String message) {
        log("warn", message);
    }

    /**
     * 记录错误日志
     * @param message 消息
     */
    default void error(@Nonnull String message) {
        log("error", message);
    }
}
