package com.moremod.api.capability;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * 能力注册表接口
 * 负责管理所有能力描述符的注册
 *
 * <p>设计原则：
 * <ul>
 *   <li>注册表是全局单例</li>
 *   <li>通过描述符注册，而非直接注册类</li>
 *   <li>支持查询和遍历所有已注册能力</li>
 *   <li>注册表只在初始化阶段可写</li>
 * </ul>
 */
public interface ICapabilityRegistry {

    /**
     * 注册能力描述符
     * @param descriptor 能力描述符
     * @param <T> 宿主类型
     * @return true 如果注册成功
     */
    <T> boolean registerCapability(ICapabilityDescriptor<T> descriptor);

    /**
     * 根据 ID 获取能力描述符
     * @param capabilityId 能力 ID
     * @param <T> 宿主类型
     * @return 能力描述符，如果不存在则返回 null
     */
    @Nullable
    <T> ICapabilityDescriptor<T> getDescriptor(String capabilityId);

    /**
     * 获取指定宿主类型的所有能力描述符
     * @param hostType 宿主类型
     * @param <T> 宿主类型
     * @return 能力描述符集合
     */
    <T> Collection<ICapabilityDescriptor<T>> getDescriptorsForHost(Class<T> hostType);

    /**
     * 获取所有已注册的能力描述符
     * @return 所有描述符的集合
     */
    Collection<ICapabilityDescriptor<?>> getAllDescriptors();

    /**
     * 检查能力是否已注册
     * @param capabilityId 能力 ID
     * @return true 如果已注册
     */
    boolean isRegistered(String capabilityId);

    /**
     * 取消注册能力（慎用，通常只在测试中使用）
     * @param capabilityId 能力 ID
     * @return true 如果取消注册成功
     */
    boolean unregisterCapability(String capabilityId);

    /**
     * 清空所有注册（慎用，通常只在测试中使用）
     */
    void clear();

    /**
     * 冻结注册表（禁止进一步注册）
     * 通常在 Mod 加载完成后调用
     */
    void freeze();

    /**
     * 注册表是否已冻结
     * @return true 如果已冻结
     */
    boolean isFrozen();
}
