package com.moremod.api.event;

/**
 * 事件监听器优先级枚举
 * 数值越小，优先级越高（越早执行）
 *
 * 设计原则：
 * - 提供标准的优先级级别
 * - 支持自定义数值优先级
 * - 不依赖任何实现类
 */
public enum EventPriority {
    /**
     * 最高优先级，最先执行
     * 用于需要在所有其他监听器之前运行的关键逻辑
     */
    HIGHEST(0),

    /**
     * 高优先级
     * 用于重要的监听器，需要在大多数监听器之前运行
     */
    HIGH(100),

    /**
     * 普通优先级（默认）
     * 用于大多数常规监听器
     */
    NORMAL(500),

    /**
     * 低优先级
     * 用于不太重要的监听器，在大多数监听器之后运行
     */
    LOW(900),

    /**
     * 最低优先级，最后执行
     * 用于监控、日志等不影响事件结果的监听器
     */
    LOWEST(1000);

    private final int value;

    EventPriority(int value) {
        this.value = value;
    }

    /**
     * 获取优先级数值
     * @return 优先级数值，越小优先级越高
     */
    public int getValue() {
        return value;
    }
}
