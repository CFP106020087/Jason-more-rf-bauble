package com.moremod.module.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 模块描述符接口 - 定义模块的元数据
 */
public interface IModuleDescriptor {

    /**
     * 获取模块ID
     * @return 模块ID
     */
    @Nonnull
    String getModuleId();

    /**
     * 获取模块名称
     * @return 模块名称
     */
    @Nonnull
    String getName();

    /**
     * 获取模块版本
     * @return 版本号
     */
    @Nonnull
    String getVersion();

    /**
     * 获取模块描述
     * @return 描述文本
     */
    @Nullable
    String getDescription();

    /**
     * 获取作者列表
     * @return 作者数组
     */
    @Nonnull
    String[] getAuthors();

    /**
     * 获取依赖列表
     * @return 依赖模块ID数组
     */
    @Nonnull
    String[] getDependencies();

    /**
     * 是否为可选模块
     * @return true 可选, false 必需
     */
    boolean isOptional();

    /**
     * 获取优先级（用于加载顺序）
     * @return 优先级（数字越小优先级越高）
     */
    int getPriority();

    /**
     * 获取自定义属性
     * @param key 属性键
     * @return 属性值
     */
    @Nullable
    Object getProperty(@Nonnull String key);
}
