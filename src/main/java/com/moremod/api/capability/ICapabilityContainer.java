package com.moremod.api.capability;

import net.minecraft.nbt.NBTTagCompound;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * 能力容器接口
 * 负责管理附加在对象上的所有能力
 *
 * <p>设计原则：
 * <ul>
 *   <li>容器是能力的所有者和管理者</li>
 *   <li>容器负责能力的生命周期</li>
 *   <li>容器负责序列化和同步所有能力</li>
 *   <li>容器支持动态添加和移除能力</li>
 * </ul>
 *
 * @param <T> 宿主类型
 */
public interface ICapabilityContainer<T> {

    /**
     * 获取指定类型的能力
     * @param capabilityType 能力类型
     * @param <C> 能力泛型
     * @return 能力实例，如果不存在则返回 null
     */
    @Nullable
    <C extends ICapability<T>> C getCapability(Class<C> capabilityType);

    /**
     * 根据 ID 获取能力
     * @param capabilityId 能力 ID
     * @return 能力实例，如果不存在则返回 null
     */
    @Nullable
    ICapability<T> getCapability(String capabilityId);

    /**
     * 检查是否拥有指定类型的能力
     * @param capabilityType 能力类型
     * @return true 如果存在
     */
    boolean hasCapability(Class<? extends ICapability<T>> capabilityType);

    /**
     * 检查是否拥有指定 ID 的能力
     * @param capabilityId 能力 ID
     * @return true 如果存在
     */
    boolean hasCapability(String capabilityId);

    /**
     * 动态附加能力
     * @param capability 能力实例
     * @return true 如果成功附加
     */
    boolean attachCapability(ICapability<T> capability);

    /**
     * 移除能力
     * @param capabilityId 能力 ID
     * @return 被移除的能力，如果不存在则返回 null
     */
    @Nullable
    ICapability<T> removeCapability(String capabilityId);

    /**
     * 获取所有能力
     * @return 能力集合（只读）
     */
    Collection<ICapability<T>> getAllCapabilities();

    /**
     * 序列化所有能力
     * @param nbt NBT 标签
     */
    void serializeNBT(NBTTagCompound nbt);

    /**
     * 反序列化所有能力
     * @param nbt NBT 标签
     */
    void deserializeNBT(NBTTagCompound nbt);

    /**
     * 获取宿主对象
     * @return 宿主对象
     */
    T getHost();

    /**
     * Tick 更新所有能力
     */
    void tick();

    /**
     * 清空所有能力
     */
    void clear();

    /**
     * 复制容器到新宿主
     * @param newHost 新宿主对象
     * @return 新的容器实例
     */
    ICapabilityContainer<T> copyTo(T newHost);
}
