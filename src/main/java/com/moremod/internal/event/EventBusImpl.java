package com.moremod.internal.event;

import com.moremod.api.event.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 事件总线实现
 *
 * 特性：
 *  ✓ 线程安全
 *  ✓ 优先级支持
 *  ✓ 可取消事件
 *  ✓ 异步事件
 *  ✓ 性能优化
 */
public class EventBusImpl implements IEventBus {

    private static final Logger logger = LogManager.getLogger(EventBusImpl.class);

    /** 监听器注册表 (EventClass → Listeners) */
    private final Map<Class<?>, List<ListenerEntry<? extends IEvent>>> listeners;

    /** 对象 → 注册的监听器映射（用于注销） */
    private final Map<Object, List<ListenerEntry<? extends IEvent>>> ownerMap;

    /** 异步事件线程池 */
    private final ExecutorService asyncExecutor;

    /** 总线名称（用于调试） */
    private final String name;

    public EventBusImpl() {
        this("DefaultEventBus");
    }

    public EventBusImpl(String name) {
        this.name = name;
        this.listeners = new ConcurrentHashMap<>();
        this.ownerMap = new ConcurrentHashMap<>();
        this.asyncExecutor = Executors.newFixedThreadPool(
            2,
            r -> {
                Thread t = new Thread(r, name + "-Async");
                t.setDaemon(true);
                return t;
            }
        );

        logger.info("EventBus '{}' initialized", name);
    }

    // ────────────────────────────────────────────────────────────
    // 注册监听器
    // ────────────────────────────────────────────────────────────

    @Override
    public void register(Object listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        List<ListenerScanner.RegisteredListener> scanned = ListenerScanner.scan(listener);

        if (scanned.isEmpty()) {
            logger.warn(
                "No @Subscribe methods found in: {}",
                listener.getClass().getName()
            );
            return;
        }

        List<ListenerEntry<? extends IEvent>> entries = new ArrayList<>();

        for (ListenerScanner.RegisteredListener reg : scanned) {
            ListenerEntry<IEvent> entry = new ListenerEntry<>(
                reg.listener,
                reg.priority,
                reg.receiveCancelled,
                listener
            );

            // 添加到监听器表
            listeners.computeIfAbsent(reg.eventClass, k -> new CopyOnWriteArrayList<>())
                     .add(entry);

            entries.add(entry);
        }

        // 排序（优先级）
        for (List<ListenerEntry<? extends IEvent>> list : listeners.values()) {
            Collections.sort(list);
        }

        // 记录所有者映射
        ownerMap.put(listener, entries);

        logger.debug(
            "Registered {} listeners from: {}",
            scanned.size(),
            listener.getClass().getSimpleName()
        );
    }

    @Override
    public void unregister(Object listener) {
        if (listener == null) return;

        List<ListenerEntry<? extends IEvent>> entries = ownerMap.remove(listener);
        if (entries == null) {
            logger.warn(
                "Attempted to unregister unknown listener: {}",
                listener.getClass().getName()
            );
            return;
        }

        // 从所有事件类型中移除
        for (List<ListenerEntry<? extends IEvent>> list : listeners.values()) {
            list.removeAll(entries);
        }

        logger.debug(
            "Unregistered {} listeners from: {}",
            entries.size(),
            listener.getClass().getSimpleName()
        );
    }

    @Override
    public <T extends IEvent> void addListener(
        Class<T> eventClass,
        IEventListener<T> listener,
        EventPriority priority
    ) {
        if (eventClass == null || listener == null) {
            throw new IllegalArgumentException("Event class and listener cannot be null");
        }

        ListenerEntry<T> entry = new ListenerEntry<>(
            listener,
            priority,
            false,
            listener // 自己作为 owner
        );

        List<ListenerEntry<? extends IEvent>> list =
            listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>());

        list.add(entry);
        Collections.sort(list);

        ownerMap.computeIfAbsent(listener, k -> new ArrayList<>()).add(entry);
    }

    // ────────────────────────────────────────────────────────────
    // 发布事件
    // ────────────────────────────────────────────────────────────

    @Override
    public boolean post(IEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        // 获取监听器列表
        List<ListenerEntry<? extends IEvent>> listenerList = getListenersFor(event.getClass());

        if (listenerList.isEmpty()) {
            return false;
        }

        // 调用所有监听器
        for (ListenerEntry<? extends IEvent> entry : listenerList) {
            if (!entry.shouldReceive(event)) {
                continue;
            }

            try {
                @SuppressWarnings("unchecked")
                ListenerEntry<IEvent> typedEntry = (ListenerEntry<IEvent>) entry;
                typedEntry.invoke(event);
            } catch (Exception e) {
                logger.error(
                    "Error dispatching event {} to listener",
                    event.getEventName(), e
                );
            }
        }

        // 返回是否被取消
        if (event instanceof ICancellableEvent) {
            return ((ICancellableEvent) event).isCancelled();
        }

        return false;
    }

    @Override
    public void postAsync(IEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        asyncExecutor.execute(() -> {
            try {
                post(event);
            } catch (Exception e) {
                logger.error(
                    "Error in async event dispatch: {}",
                    event.getEventName(), e
                );
            }
        });
    }

    // ────────────────────────────────────────────────────────────
    // 工具方法
    // ────────────────────────────────────────────────────────────

    /**
     * 获取事件类型的所有监听器（包括父类）
     */
    private List<ListenerEntry<? extends IEvent>> getListenersFor(Class<?> eventClass) {
        List<ListenerEntry<? extends IEvent>> result = new ArrayList<>();

        // 获取直接监听器
        List<ListenerEntry<? extends IEvent>> direct = listeners.get(eventClass);
        if (direct != null) {
            result.addAll(direct);
        }

        // 获取父类/接口监听器
        for (Map.Entry<Class<?>, List<ListenerEntry<? extends IEvent>>> entry : listeners.entrySet()) {
            if (entry.getKey().isAssignableFrom(eventClass) && entry.getKey() != eventClass) {
                result.addAll(entry.getValue());
            }
        }

        // 按优先级排序
        Collections.sort(result);

        return result;
    }

    @Override
    public int getListenerCount() {
        return listeners.values().stream()
            .mapToInt(List::size)
            .sum();
    }

    @Override
    public void shutdown() {
        listeners.clear();
        ownerMap.clear();
        asyncExecutor.shutdown();
        logger.info("EventBus '{}' shutdown", name);
    }
}
