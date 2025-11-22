package com.moremod.api.capability;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * 能力描述符接口
 * 用于注册能力而不暴露具体实现类
 *
 * <p>设计原则：
 * <ul>
 *   <li>描述符是能力的元数据</li>
 *   <li>描述符定义能力的创建规则</li>
 *   <li>描述符支持条件判断（是否应用于某个对象）</li>
 *   <li>通过描述符而非 Class 注册，实现完全解耦</li>
 * </ul>
 *
 * @param <T> 宿主类型
 */
public interface ICapabilityDescriptor<T> {

    /**
     * 获取能力的唯一标识符
     * @return 能力 ID
     */
    String getCapabilityId();

    /**
     * 获取能力提供者
     * @return 能力提供者实例
     */
    ICapabilityProvider<T, ? extends ICapability<T>> getProvider();

    /**
     * 获取宿主类型
     * @return 宿主类的 Class 对象
     */
    Class<T> getHostType();

    /**
     * 判断能力是否应该附加到指定宿主
     * @param host 宿主对象
     * @return true 如果应该附加
     */
    boolean shouldAttachTo(T host);

    /**
     * 设置附加条件
     * @param predicate 条件判断器
     * @return 自身（用于链式调用）
     */
    ICapabilityDescriptor<T> setAttachCondition(Predicate<T> predicate);

    /**
     * 获取优先级（用于决定能力的加载顺序）
     * 数字越小优先级越高
     * @return 优先级值
     */
    default int getPriority() {
        return 1000;
    }

    /**
     * 是否自动序列化
     * @return true 如果应该自动序列化
     */
    default boolean shouldAutoSerialize() {
        return true;
    }

    /**
     * 是否自动同步
     * @return true 如果应该自动同步到客户端
     */
    default boolean shouldAutoSync() {
        return false;
    }

    /**
     * 获取描述信息（用于调试）
     * @return 描述文本
     */
    @Nullable
    default String getDescription() {
        return null;
    }
}
