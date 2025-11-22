package com.moremod.api.event;

/**
 * 事件总线接口
 * 这是主 mod 与事件系统交互的唯一入口
 *
 * 设计原则：
 * - 主 mod 只能看到这个接口，不能直接依赖实现类
 * - 提供最小化的 API：注册、注销、触发
 * - 不暴露任何内部实现细节
 * - 支持方法链式调用
 *
 * 使用示例：
 * <pre>
 * {@code
 * IEventBus bus = EventService.getBus();
 * bus.register(new MyEventListener());
 * bus.post(new MyCustomEvent());
 * }
 * </pre>
 */
public interface IEventBus {

    /**
     * 注册事件监听器对象
     *
     * 会扫描对象中所有带有 @IEventListener 注解的方法，
     * 并将它们注册为对应事件类型的监听器
     *
     * @param listener 监听器对象（包含 @IEventListener 注解的方法）
     * @return this 事件总线自身，支持链式调用
     */
    IEventBus register(Object listener);

    /**
     * 注销事件监听器对象
     *
     * 移除之前通过 register() 注册的所有监听器
     *
     * @param listener 要注销的监听器对象
     * @return this 事件总线自身，支持链式调用
     */
    IEventBus unregister(Object listener);

    /**
     * 触发事件
     *
     * 按照优先级顺序调用所有注册的监听器
     * 如果事件实现了 ICancellableEvent 且被取消，后续监听器可能不会被调用
     * （取决于监听器的 receiveCancelled 设置）
     *
     * @param event 要触发的事件对象
     * @return 事件对象本身（可能已被监听器修改）
     */
    <T extends IEvent> T post(T event);

    /**
     * 注册事件监听器类（会自动实例化）
     *
     * 某些情况下可能需要注册类而不是对象，
     * 事件总线会尝试创建该类的实例并注册
     *
     * @param listenerClass 监听器类
     * @return this 事件总线自身，支持链式调用
     */
    IEventBus registerClass(Class<?> listenerClass);

    /**
     * 清除所有已注册的监听器
     * 主要用于测试或重新初始化
     *
     * @return this 事件总线自身，支持链式调用
     */
    IEventBus clear();
}
