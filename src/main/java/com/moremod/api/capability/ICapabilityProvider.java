package com.moremod.api.capability;

import javax.annotation.Nullable;

/**
 * 能力提供者接口
 * 实现此接口的对象可以提供能力
 *
 * <p>设计原则：
 * <ul>
 *   <li>提供者负责创建能力实例</li>
 *   <li>提供者不应该持有能力实例的强引用</li>
 *   <li>提供者应该是无状态的</li>
 * </ul>
 *
 * @param <T> 宿主类型
 * @param <C> 能力类型
 */
public interface ICapabilityProvider<T, C extends ICapability<T>> {

    /**
     * 创建能力实例
     * @param host 宿主对象
     * @return 新的能力实例
     */
    C createCapability(T host);

    /**
     * 获取能力类型
     * @return 能力接口的 Class 对象
     */
    Class<C> getCapabilityType();

    /**
     * 获取能力 ID
     * @return 能力的唯一标识符
     */
    String getCapabilityId();
}
