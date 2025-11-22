package com.moremod.module.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * 模块容器接口 - 管理模块的生命周期和访问
 */
public interface IModuleContainer {

    /**
     * 注册模块
     * @param module 模块实例
     * @return true 注册成功, false 失败
     */
    boolean registerModule(@Nonnull IModule module);

    /**
     * 注销模块
     * @param moduleId 模块ID
     * @return true 注销成功, false 失败
     */
    boolean unregisterModule(@Nonnull String moduleId);

    /**
     * 获取模块
     * @param moduleId 模块ID
     * @return 模块实例，不存在则返回null
     */
    @Nullable
    IModule getModule(@Nonnull String moduleId);

    /**
     * 检查模块是否已注册
     * @param moduleId 模块ID
     * @return true 已注册, false 未注册
     */
    boolean hasModule(@Nonnull String moduleId);

    /**
     * 获取所有已注册模块
     * @return 模块集合
     */
    @Nonnull
    Collection<IModule> getAllModules();

    /**
     * 获取所有激活的模块
     * @return 激活模块集合
     */
    @Nonnull
    Collection<IModule> getActiveModules();

    /**
     * 初始化所有模块
     * @param context 模块上下文
     */
    void initializeAll(@Nonnull IModuleContext context);

    /**
     * 加载所有模块
     * @param context 模块上下文
     */
    void loadAll(@Nonnull IModuleContext context);

    /**
     * 卸载所有模块
     * @param context 模块上下文
     */
    void unloadAll(@Nonnull IModuleContext context);

    /**
     * 附加所有模块到宿主
     * @param host 模块宿主
     * @param context 模块上下文
     */
    void attachAll(@Nonnull IModuleHost host, @Nonnull IModuleContext context);

    /**
     * 从宿主分离所有模块
     * @param host 模块宿主
     * @param context 模块上下文
     */
    void detachAll(@Nonnull IModuleHost host, @Nonnull IModuleContext context);

    /**
     * tick所有模块
     * @param host 模块宿主
     * @param context 模块上下文
     */
    void tickAll(@Nonnull IModuleHost host, @Nonnull IModuleContext context);

    /**
     * 发送模块间消息
     * @param senderId 发送者模块ID
     * @param targetId 目标模块ID (null表示广播)
     * @param message 消息内容
     * @param context 模块上下文
     * @return 响应结果（广播时返回第一个非null响应）
     */
    @Nullable
    Object sendMessage(@Nonnull String senderId, @Nullable String targetId,
                       @Nonnull Object message, @Nonnull IModuleContext context);
}
