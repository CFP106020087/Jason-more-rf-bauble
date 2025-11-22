package com.moremod.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法为事件监听器
 *
 * 使用方式：
 * <pre>
 * {@code
 * @Subscribe(priority = EventPriority.HIGH)
 * public void onPlayerDamage(PlayerDamageEvent event) {
 *     // 处理逻辑
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {

    /**
     * 优先级
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * 是否接收已取消的事件
     */
    boolean receiveCancelled() default false;
}
