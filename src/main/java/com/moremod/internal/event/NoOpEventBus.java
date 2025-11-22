package com.moremod.internal.event;

import com.moremod.api.event.EventPriority;
import com.moremod.api.event.IEvent;
import com.moremod.api.event.IEventBus;
import com.moremod.api.event.IEventListener;

/**
 * No-Op 事件总线实现
 *
 * 用途：
 *  ✓ Fallback 实现
 *  ✓ 测试环境
 *  ✓ 禁用事件系统时使用
 *  ✓ 保证主代码不会因为缺少实现而崩溃
 */
public class NoOpEventBus implements IEventBus {

    private static final NoOpEventBus INSTANCE = new NoOpEventBus();

    public static NoOpEventBus getInstance() {
        return INSTANCE;
    }

    private NoOpEventBus() {
        // Singleton
    }

    @Override
    public void register(Object listener) {
        // Do nothing
    }

    @Override
    public void unregister(Object listener) {
        // Do nothing
    }

    @Override
    public <T extends IEvent> void addListener(
        Class<T> eventClass,
        IEventListener<T> listener,
        EventPriority priority
    ) {
        // Do nothing
    }

    @Override
    public boolean post(IEvent event) {
        // Always return false (not cancelled)
        return false;
    }

    @Override
    public void postAsync(IEvent event) {
        // Do nothing
    }

    @Override
    public int getListenerCount() {
        return 0;
    }

    @Override
    public void shutdown() {
        // Do nothing
    }
}
