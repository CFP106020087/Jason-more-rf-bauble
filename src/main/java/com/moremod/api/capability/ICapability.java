package com.moremod.api.capability;

import net.minecraft.nbt.NBTTagCompound;
import javax.annotation.Nullable;

/**
 * 能力基础接口
 * 所有自定义能力都必须实现此接口
 *
 * <p>设计原则：
 * <ul>
 *   <li>能力是附加在对象上的可选功能模块</li>
 *   <li>能力可以被序列化和反序列化</li>
 *   <li>能力可以被复制到新实例</li>
 *   <li>能力支持同步标记（用于网络同步）</li>
 * </ul>
 *
 * @param <T> 能力的宿主类型（如 EntityPlayer, ItemStack 等）
 */
public interface ICapability<T> {

    /**
     * 获取能力的唯一标识符
     * @return 能力 ID（如 "moremod:auto_attack"）
     */
    String getCapabilityId();

    /**
     * 序列化能力数据到 NBT
     * @param nbt NBT 标签
     */
    void serializeNBT(NBTTagCompound nbt);

    /**
     * 从 NBT 反序列化能力数据
     * @param nbt NBT 标签
     */
    void deserializeNBT(NBTTagCompound nbt);

    /**
     * 是否需要同步到客户端
     * @return true 如果需要同步
     */
    default boolean shouldSync() {
        return false;
    }

    /**
     * 能力是否已标记为脏（需要同步）
     * @return true 如果已修改且需要同步
     */
    default boolean isDirty() {
        return false;
    }

    /**
     * 标记能力为脏（触发同步）
     */
    default void markDirty() {
        // NoOp by default
    }

    /**
     * 清除脏标记
     */
    default void clearDirty() {
        // NoOp by default
    }

    /**
     * 复制能力数据到新实例
     * 用于玩家重生等场景
     * @param host 新的宿主对象
     * @return 新的能力实例
     */
    @Nullable
    ICapability<T> copyTo(T host);

    /**
     * 当能力被附加到宿主时调用
     * @param host 宿主对象
     */
    default void onAttached(T host) {
        // NoOp by default
    }

    /**
     * 当能力从宿主移除时调用
     * @param host 宿主对象
     */
    default void onDetached(T host) {
        // NoOp by default
    }

    /**
     * Tick 更新（如果能力需要定期更新）
     * @param host 宿主对象
     */
    default void tick(T host) {
        // NoOp by default
    }
}
