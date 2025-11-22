package com.moremod.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 事件监听器注解
 * 标记在方法上，表示该方法是一个事件监听器
 *
 * 使用示例：
 * <pre>
 * {@code
 * @IEventListener(priority = EventPriority.HIGH)
 * public void onPlayerLogin(PlayerLoginEvent event) {
 *     // 处理玩家登录事件
 * }
 * }
 * </pre>
 *
 * 设计原则：
 * - 仅作为标记，不包含任何逻辑
 * - 不依赖任何实现类
 * - 可以在没有事件系统的情况下编译
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IEventListener {

    /**
     * 监听器优先级
     * @return 优先级，默认为 NORMAL
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * 是否接收已取消的事件
     * @return true 表示即使事件被取消也会接收，false 表示忽略已取消的事件
     */
    boolean receiveCancelled() default false;
}
