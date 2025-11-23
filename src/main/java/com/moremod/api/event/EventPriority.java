package com.moremod.api.event;

/**
 * 事件优先级
 *
 * 数字越小优先级越高（先执行）
 */
public enum EventPriority {
    HIGHEST(0),
    HIGH(100),
    NORMAL(500),
    LOW(900),
    LOWEST(1000),
    MONITOR(10000); // 最后执行，只读监控

    private final int value;

    EventPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
