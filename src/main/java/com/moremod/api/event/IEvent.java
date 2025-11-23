package com.moremod.api.event;

/**
 * 事件基础接口
 *
 * 所有自定义事件必须实现此接口
 *
 * 设计原则：
 *  ✓ 标记接口（Marker Interface）
 *  ✓ 不强制任何方法
 *  ✓ 事件类可以是纯 POJO
 */
public interface IEvent {

    /**
     * 获取事件名称（用于调试）
     * 默认实现返回类名
     */
    default String getEventName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 事件是否应该异步处理
     * 默认同步处理
     */
    default boolean isAsync() {
        return false;
    }
}
