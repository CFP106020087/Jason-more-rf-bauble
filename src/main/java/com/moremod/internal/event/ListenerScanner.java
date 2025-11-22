package com.moremod.internal.event;

import com.moremod.api.event.IEvent;
import com.moremod.api.event.IEventListener;
import com.moremod.api.event.Subscribe;
import com.moremod.api.event.EventPriority;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 监听器扫描器
 *
 * 职责：
 *  ✓ 扫描对象中的 @Subscribe 方法
 *  ✓ 验证方法签名
 *  ✓ 创建 ListenerEntry
 */
public class ListenerScanner {

    private static final Logger logger = LogManager.getLogger(ListenerScanner.class);

    /**
     * 扫描对象中的所有监听器方法
     *
     * @param listener 监听器对象
     * @return 监听器条目列表
     */
    public static List<RegisteredListener> scan(Object listener) {
        List<RegisteredListener> results = new ArrayList<>();
        Class<?> clazz = listener.getClass();

        // 扫描所有方法
        for (Method method : clazz.getDeclaredMethods()) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation == null) continue;

            // 验证方法签名
            if (!isValidListenerMethod(method)) {
                logger.warn(
                    "Invalid @Subscribe method: {}#{} - must be public, non-static, " +
                    "with single IEvent parameter",
                    clazz.getName(), method.getName()
                );
                continue;
            }

            // 获取事件类型
            Class<?> eventType = method.getParameterTypes()[0];
            if (!IEvent.class.isAssignableFrom(eventType)) {
                logger.warn(
                    "Invalid @Subscribe method: {}#{} - parameter must implement IEvent",
                    clazz.getName(), method.getName()
                );
                continue;
            }

            // 创建监听器包装器
            method.setAccessible(true);
            IEventListener<IEvent> wrapper = event -> {
                try {
                    method.invoke(listener, event);
                } catch (Exception e) {
                    logger.error(
                        "Error invoking listener {}#{}",
                        clazz.getName(), method.getName(), e
                    );
                }
            };

            // 创建条目
            @SuppressWarnings("unchecked")
            Class<? extends IEvent> eventClass = (Class<? extends IEvent>) eventType;

            results.add(new RegisteredListener(
                eventClass,
                wrapper,
                annotation.priority(),
                annotation.receiveCancelled(),
                listener
            ));
        }

        return results;
    }

    /**
     * 验证方法签名
     */
    private static boolean isValidListenerMethod(Method method) {
        // 必须是 public
        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }

        // 不能是 static
        if (Modifier.isStatic(method.getModifiers())) {
            return false;
        }

        // 必须只有一个参数
        if (method.getParameterCount() != 1) {
            return false;
        }

        // 返回值必须是 void
        if (method.getReturnType() != void.class) {
            return false;
        }

        return true;
    }

    /**
     * 注册的监听器信息
     */
    public static class RegisteredListener {
        public final Class<? extends IEvent> eventClass;
        public final IEventListener<IEvent> listener;
        public final EventPriority priority;
        public final boolean receiveCancelled;
        public final Object owner;

        public RegisteredListener(
            Class<? extends IEvent> eventClass,
            IEventListener<IEvent> listener,
            EventPriority priority,
            boolean receiveCancelled,
            Object owner
        ) {
            this.eventClass = eventClass;
            this.listener = listener;
            this.priority = priority;
            this.receiveCancelled = receiveCancelled;
            this.owner = owner;
        }
    }
}
