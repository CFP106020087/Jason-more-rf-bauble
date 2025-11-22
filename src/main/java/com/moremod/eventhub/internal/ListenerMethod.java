package com.moremod.eventhub.internal;

import com.moremod.api.event.EventPriority;
import com.moremod.api.event.ICancellableEvent;
import com.moremod.api.event.IEvent;
import com.moremod.api.event.IEventListener;

import java.lang.reflect.Method;

/**
 * 监听器方法封装类
 * 封装了事件监听器方法的所有信息和调用逻辑
 *
 * 设计原则：
 * - 不暴露到公共 API，仅供事件系统内部使用
 * - 封装反射调用的复杂性
 * - 提供优先级比较能力
 * - 线程安全（immutable）
 */
public final class ListenerMethod implements Comparable<ListenerMethod> {

    private final Object listener;
    private final Method method;
    private final Class<?> eventType;
    private final int priority;
    private final boolean receiveCancelled;

    /**
     * 构造监听器方法
     *
     * @param listener 监听器对象实例
     * @param method 监听器方法
     * @param annotation 事件监听器注解
     */
    public ListenerMethod(Object listener, Method method, IEventListener annotation) {
        this.listener = listener;
        this.method = method;
        this.priority = annotation.priority().getValue();
        this.receiveCancelled = annotation.receiveCancelled();

        // 验证方法签名
        Class<?>[] params = method.getParameterTypes();
        if (params.length != 1) {
            throw new IllegalArgumentException(
                    "Event listener method must have exactly one parameter: " + method);
        }

        if (!IEvent.class.isAssignableFrom(params[0])) {
            throw new IllegalArgumentException(
                    "Event listener parameter must implement IEvent: " + method);
        }

        this.eventType = params[0];

        // 设置方法可访问（支持私有方法）
        method.setAccessible(true);
    }

    /**
     * 调用监听器方法
     *
     * @param event 事件对象
     * @throws Exception 如果调用失败
     */
    public void invoke(IEvent event) throws Exception {
        // 检查事件类型是否匹配
        if (!eventType.isInstance(event)) {
            return;
        }

        // 检查是否应该接收已取消的事件
        if (!receiveCancelled && event instanceof ICancellableEvent) {
            if (((ICancellableEvent) event).isCancelled()) {
                return;
            }
        }

        // 调用监听器方法
        method.invoke(listener, event);
    }

    /**
     * 获取事件类型
     */
    public Class<?> getEventType() {
        return eventType;
    }

    /**
     * 获取优先级
     */
    public int getPriority() {
        return priority;
    }

    /**
     * 获取监听器对象
     */
    public Object getListener() {
        return listener;
    }

    /**
     * 优先级比较
     * 优先级数值越小，排序越靠前
     */
    @Override
    public int compareTo(ListenerMethod other) {
        return Integer.compare(this.priority, other.priority);
    }

    @Override
    public String toString() {
        return String.format("ListenerMethod[%s.%s, priority=%d, eventType=%s]",
                listener.getClass().getSimpleName(),
                method.getName(),
                priority,
                eventType.getSimpleName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ListenerMethod)) return false;
        ListenerMethod other = (ListenerMethod) obj;
        return listener == other.listener && method.equals(other.method);
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(listener) + method.hashCode();
    }
}
