package com.moremod.api.event;

/**
 * 事件基础接口
 * 所有自定义事件必须实现此接口
 *
 * 设计原则：
 * - 最小化接口，不包含任何业务逻辑
 * - 不依赖任何实现类
 * - 可以被主 mod 和事件系统同时使用
 */
public interface IEvent {

    /**
     * 获取事件名称（用于调试和日志）
     * @return 事件名称
     */
    default String getEventName() {
        return this.getClass().getSimpleName();
    }
}
