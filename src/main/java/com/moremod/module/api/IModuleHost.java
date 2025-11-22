package com.moremod.module.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 模块宿主接口 - 定义可以承载模块的对象
 *
 * 模块宿主可以是：
 * - 玩家 (EntityPlayer)
 * - 物品 (ItemStack)
 * - 世界 (World)
 * - 自定义实体或TileEntity
 */
public interface IModuleHost {

    /**
     * 获取宿主唯一标识符
     * @return 宿主ID
     */
    @Nonnull
    String getHostId();

    /**
     * 获取宿主类型
     * @return 宿主类型 (如 "player", "item", "world")
     */
    @Nonnull
    String getHostType();

    /**
     * 获取宿主的原生对象（可选）
     * @return 原生对象 (如 EntityPlayer, ItemStack等)
     */
    @Nullable
    Object getNativeHost();

    /**
     * 读取宿主数据
     * @param key 数据键
     * @return 数据值
     */
    @Nullable
    Object getHostData(@Nonnull String key);

    /**
     * 写入宿主数据
     * @param key 数据键
     * @param value 数据值
     */
    void setHostData(@Nonnull String key, @Nullable Object value);

    /**
     * 宿主是否有效（未被销毁）
     * @return true 有效, false 无效
     */
    boolean isValid();

    /**
     * 宿主是否支持持久化
     * @return true 支持, false 不支持
     */
    default boolean supportsPersistence() {
        return false;
    }
}
