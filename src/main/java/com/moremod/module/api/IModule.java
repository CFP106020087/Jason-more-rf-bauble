package com.moremod.module.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 模块接口 - 定义模块的基本行为
 *
 * 生命周期: init → load → attach → tick → detach → unload
 *
 * 设计原则:
 * - 无硬依赖: 模块不应直接依赖具体实现
 * - 可选集成: 事件/能力系统通过上下文可选访问
 * - 失败安全: 任何阶段失败不影响宿主运行
 */
public interface IModule {

    /**
     * 获取模块唯一标识符
     * @return 模块ID (格式: namespace:name, 如 "moremod:energy_boost")
     */
    @Nonnull
    String getModuleId();

    /**
     * 获取模块显示名称
     * @return 模块名称
     */
    @Nonnull
    String getDisplayName();

    /**
     * 获取模块版本
     * @return 版本字符串
     */
    @Nonnull
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * 获取模块描述符（元数据）
     * @return 模块描述符
     */
    @Nullable
    IModuleDescriptor getDescriptor();

    /**
     * 初始化模块（仅调用一次）
     * 用于注册监听器、初始化数据结构等
     *
     * @param context 模块上下文（提供服务访问）
     * @return true 初始化成功, false 失败
     */
    boolean init(@Nonnull IModuleContext context);

    /**
     * 加载模块（可多次调用，如重载配置）
     * 用于加载配置、资源等
     *
     * @param context 模块上下文
     * @return true 加载成功, false 失败
     */
    boolean load(@Nonnull IModuleContext context);

    /**
     * 附加到宿主（每次宿主激活时调用）
     *
     * @param host 模块宿主（玩家、物品、世界等）
     * @param context 模块上下文
     * @return true 附加成功, false 失败
     */
    boolean attach(@Nonnull IModuleHost host, @Nonnull IModuleContext context);

    /**
     * 每tick更新（仅当附加时调用）
     *
     * @param host 模块宿主
     * @param context 模块上下文
     */
    void onTick(@Nonnull IModuleHost host, @Nonnull IModuleContext context);

    /**
     * 从宿主分离（每次宿主停用时调用）
     *
     * @param host 模块宿主
     * @param context 模块上下文
     */
    void detach(@Nonnull IModuleHost host, @Nonnull IModuleContext context);

    /**
     * 卸载模块（可多次调用）
     * 用于释放资源、保存数据等
     *
     * @param context 模块上下文
     */
    void unload(@Nonnull IModuleContext context);

    /**
     * 模块是否已激活
     * @return true 已激活, false 未激活
     */
    boolean isActive();

    /**
     * 模块是否兼容当前环境
     * @param context 模块上下文
     * @return true 兼容, false 不兼容
     */
    default boolean isCompatible(@Nonnull IModuleContext context) {
        return true;
    }

    /**
     * 获取模块依赖（可选）
     * @return 依赖的模块ID列表
     */
    @Nonnull
    default String[] getDependencies() {
        return new String[0];
    }

    /**
     * 处理模块间消息（可选）
     *
     * @param senderId 发送者模块ID
     * @param message 消息内容
     * @param context 模块上下文
     * @return 处理结果（可为null）
     */
    @Nullable
    default Object handleMessage(@Nonnull String senderId, @Nonnull Object message,
                                  @Nonnull IModuleContext context) {
        return null;
    }
}
