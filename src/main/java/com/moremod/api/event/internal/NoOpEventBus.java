package com.moremod.api.event.internal;

import com.moremod.api.event.IEvent;
import com.moremod.api.event.IEventBus;

/**
 * 空操作事件总线（No-Op EventBus）
 * 当真正的事件系统实现不存在时使用的 fallback 实现
 *
 * 设计原则：
 * - 所有操作都是空操作（No-Op），不会抛出异常
 * - 保证主 mod 在没有事件系统实现的情况下仍能正常编译和运行
 * - 不依赖任何事件系统的实现类
 * - 作为默认实现，确保系统的健壮性
 *
 * 使用场景：
 * - 事件系统 eventhub 包被删除时自动启用
 * - 测试环境中不需要事件功能时
 * - 最小化依赖的部署场景
 */
public final class NoOpEventBus implements IEventBus {

    /**
     * 单例实例
     */
    public static final NoOpEventBus INSTANCE = new NoOpEventBus();

    /**
     * 私有构造函数，防止外部实例化
     */
    private NoOpEventBus() {
    }

    /**
     * 空操作：不注册任何监听器
     */
    @Override
    public IEventBus register(Object listener) {
        // No-Op: 什么都不做
        return this;
    }

    /**
     * 空操作：不注销任何监听器
     */
    @Override
    public IEventBus unregister(Object listener) {
        // No-Op: 什么都不做
        return this;
    }

    /**
     * 空操作：不触发任何事件，直接返回事件对象
     */
    @Override
    public <T extends IEvent> T post(T event) {
        // No-Op: 直接返回事件，不触发任何监听器
        return event;
    }

    /**
     * 空操作：不注册任何类
     */
    @Override
    public IEventBus registerClass(Class<?> listenerClass) {
        // No-Op: 什么都不做
        return this;
    }

    /**
     * 空操作：没有监听器需要清除
     */
    @Override
    public IEventBus clear() {
        // No-Op: 什么都不做
        return this;
    }

    @Override
    public String toString() {
        return "NoOpEventBus[disabled]";
    }
}
