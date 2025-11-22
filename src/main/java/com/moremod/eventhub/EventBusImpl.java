package com.moremod.eventhub;

import com.moremod.api.event.IEvent;
import com.moremod.api.event.IEventBus;
import com.moremod.eventhub.internal.EventScanner;
import com.moremod.eventhub.internal.ListenerMethod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 事件总线实现类
 * 这是事件系统的核心实现，可以被完整删除而不影响主 mod 编译
 *
 * 设计原则：
 * - 线程安全：使用 ConcurrentHashMap 和 CopyOnWriteArrayList
 * - 高性能：事件类型映射，避免每次都扫描所有监听器
 * - 优先级支持：监听器按优先级排序执行
 * - 取消支持：支持可取消事件
 * - 零耦合：只依赖公共 API 接口
 *
 * 特性：
 * - 同步事件触发
 * - 按优先级顺序调用监听器
 * - 支持事件继承（父类事件也会被触发）
 * - 支持运行时注册/注销
 * - 异常隔离（单个监听器异常不影响其他监听器）
 */
public class EventBusImpl implements IEventBus {

    /**
     * 监听器注册表
     * Key: 监听器对象
     * Value: 该对象的所有监听器方法
     */
    private final Map<Object, List<ListenerMethod>> listenerRegistry;

    /**
     * 事件类型映射表
     * Key: 事件类型
     * Value: 监听该事件类型的所有方法（已排序）
     */
    private final Map<Class<?>, List<ListenerMethod>> eventTypeMap;

    /**
     * 是否启用调试日志
     */
    private final boolean debug;

    /**
     * 构造事件总线
     */
    public EventBusImpl() {
        this(false);
    }

    /**
     * 构造事件总线（可指定是否启用调试）
     *
     * @param debug 是否启用调试日志
     */
    public EventBusImpl(boolean debug) {
        this.listenerRegistry = new ConcurrentHashMap<>();
        this.eventTypeMap = new ConcurrentHashMap<>();
        this.debug = debug;
    }

    @Override
    public IEventBus register(Object listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        // 避免重复注册
        if (listenerRegistry.containsKey(listener)) {
            if (debug) {
                System.out.println("[EventBus] Listener already registered: " + listener);
            }
            return this;
        }

        // 扫描监听器方法
        List<ListenerMethod> methods = EventScanner.scanListener(listener);

        if (methods.isEmpty()) {
            if (debug) {
                System.out.println("[EventBus] Warning: Listener has no @IEventListener methods: " + listener);
            }
            return this;
        }

        // 注册到监听器表
        listenerRegistry.put(listener, methods);

        // 更新事件类型映射
        for (ListenerMethod method : methods) {
            registerEventType(method);
        }

        if (debug) {
            System.out.println("[EventBus] Registered listener: " + listener +
                    " with " + methods.size() + " methods");
        }

        return this;
    }

    @Override
    public IEventBus unregister(Object listener) {
        if (listener == null) {
            return this;
        }

        List<ListenerMethod> methods = listenerRegistry.remove(listener);

        if (methods != null) {
            // 从事件类型映射中移除
            for (ListenerMethod method : methods) {
                unregisterEventType(method);
            }

            if (debug) {
                System.out.println("[EventBus] Unregistered listener: " + listener);
            }
        }

        return this;
    }

    @Override
    public <T extends IEvent> T post(T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        if (debug) {
            System.out.println("[EventBus] Posting event: " + event.getEventName());
        }

        // 获取事件类型对应的所有监听器
        Class<?> eventClass = event.getClass();
        List<ListenerMethod> listeners = getListenersForEvent(eventClass);

        if (listeners.isEmpty()) {
            if (debug) {
                System.out.println("[EventBus] No listeners for event: " + event.getEventName());
            }
            return event;
        }

        // 调用所有监听器
        int successCount = 0;
        int errorCount = 0;

        for (ListenerMethod listener : listeners) {
            try {
                listener.invoke(event);
                successCount++;
            } catch (Exception e) {
                errorCount++;
                // 异常隔离：单个监听器出错不影响其他监听器
                System.err.println("[EventBus] Error invoking listener: " + listener);
                e.printStackTrace();
            }
        }

        if (debug) {
            System.out.println("[EventBus] Event processed: " + event.getEventName() +
                    " (success=" + successCount + ", errors=" + errorCount + ")");
        }

        return event;
    }

    @Override
    public IEventBus registerClass(Class<?> listenerClass) {
        if (listenerClass == null) {
            throw new IllegalArgumentException("Listener class cannot be null");
        }

        try {
            List<ListenerMethod> methods = EventScanner.scanClass(listenerClass);

            if (methods.isEmpty()) {
                if (debug) {
                    System.out.println("[EventBus] Warning: Class has no @IEventListener methods: " + listenerClass);
                }
                return this;
            }

            // 获取实例（已由 EventScanner 创建）
            Object instance = methods.get(0).getListener();

            // 注册实例
            listenerRegistry.put(instance, methods);

            // 更新事件类型映射
            for (ListenerMethod method : methods) {
                registerEventType(method);
            }

            if (debug) {
                System.out.println("[EventBus] Registered class: " + listenerClass.getSimpleName() +
                        " with " + methods.size() + " methods");
            }

        } catch (Exception e) {
            System.err.println("[EventBus] Failed to register class: " + listenerClass);
            e.printStackTrace();
        }

        return this;
    }

    @Override
    public IEventBus clear() {
        listenerRegistry.clear();
        eventTypeMap.clear();

        if (debug) {
            System.out.println("[EventBus] All listeners cleared");
        }

        return this;
    }

    /**
     * 将监听器方法注册到事件类型映射
     */
    private void registerEventType(ListenerMethod method) {
        Class<?> eventType = method.getEventType();

        // 获取或创建监听器列表
        List<ListenerMethod> listeners = eventTypeMap.computeIfAbsent(
                eventType,
                k -> new CopyOnWriteArrayList<>()
        );

        // 添加并排序
        listeners.add(method);
        Collections.sort(listeners);
    }

    /**
     * 从事件类型映射中移除监听器方法
     */
    private void unregisterEventType(ListenerMethod method) {
        Class<?> eventType = method.getEventType();
        List<ListenerMethod> listeners = eventTypeMap.get(eventType);

        if (listeners != null) {
            listeners.remove(method);

            // 如果列表为空，移除映射
            if (listeners.isEmpty()) {
                eventTypeMap.remove(eventType);
            }
        }
    }

    /**
     * 获取事件类型对应的所有监听器
     * 支持事件继承（也会返回父类事件的监听器）
     */
    private List<ListenerMethod> getListenersForEvent(Class<?> eventClass) {
        List<ListenerMethod> result = new ArrayList<>();

        // 遍历事件类的继承层次
        Class<?> clazz = eventClass;
        while (clazz != null && IEvent.class.isAssignableFrom(clazz)) {
            List<ListenerMethod> listeners = eventTypeMap.get(clazz);
            if (listeners != null) {
                result.addAll(listeners);
            }
            clazz = clazz.getSuperclass();
        }

        // 按优先级排序
        Collections.sort(result);

        return result;
    }

    /**
     * 获取已注册的监听器数量
     */
    public int getListenerCount() {
        return listenerRegistry.size();
    }

    /**
     * 获取已注册的事件类型数量
     */
    public int getEventTypeCount() {
        return eventTypeMap.size();
    }

    @Override
    public String toString() {
        return String.format("EventBusImpl[listeners=%d, eventTypes=%d]",
                getListenerCount(), getEventTypeCount());
    }
}
