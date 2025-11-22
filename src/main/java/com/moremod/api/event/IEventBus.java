package com.moremod.api.event;

/**
 * 事件总线接口
 *
 * 核心职责：
 *  ✓ 注册/注销监听器
 *  ✓ 发布事件
 *  ✓ 提供 No-Op 实现用于 fallback
 */
public interface IEventBus {

    /**
     * 注册事件监听器（对象实例，扫描 @Subscribe 方法）
     *
     * @param listener 监听器对象
     */
    void register(Object listener);

    /**
     * 注销监听器
     *
     * @param listener 监听器对象
     */
    void unregister(Object listener);

    /**
     * 注册函数式监听器
     *
     * @param eventClass 事件类型
     * @param listener 监听器函数
     * @param priority 优先级
     */
    <T extends IEvent> void addListener(
        Class<T> eventClass,
        IEventListener<T> listener,
        EventPriority priority
    );

    /**
     * 发布事件（同步）
     *
     * @param event 事件实例
     * @return 事件是否被取消（不可取消事件返回 false）
     */
    boolean post(IEvent event);

    /**
     * 发布事件（异步）
     *
     * @param event 事件实例
     */
    void postAsync(IEvent event);

    /**
     * 获取已注册监听器数量（用于调试）
     */
    int getListenerCount();

    /**
     * 清空所有监听器
     */
    void shutdown();
}
