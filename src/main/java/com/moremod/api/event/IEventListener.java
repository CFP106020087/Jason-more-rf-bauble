package com.moremod.api.event;

/**
 * 事件监听器接口
 *
 * @param <T> 监听的事件类型
 */
@FunctionalInterface
public interface IEventListener<T extends IEvent> {

    /**
     * 处理事件
     *
     * @param event 事件实例
     */
    void onEvent(T event);
}
