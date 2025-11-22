package com.moremod.eventhub.internal;

import com.moremod.api.event.IEventListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 事件监听器扫描器
 * 扫描对象中所有带有 @IEventListener 注解的方法
 *
 * 设计原则：
 * - 不暴露到公共 API，仅供事件系统内部使用
 * - 使用反射扫描注解方法
 * - 支持继承的方法
 * - 验证方法签名的正确性
 */
public final class EventScanner {

    /**
     * 私有构造函数，防止实例化
     */
    private EventScanner() {
    }

    /**
     * 扫描对象中的所有事件监听器方法
     *
     * @param listener 要扫描的对象
     * @return 监听器方法列表
     */
    public static List<ListenerMethod> scanListener(Object listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        List<ListenerMethod> methods = new ArrayList<>();
        Class<?> clazz = listener.getClass();

        // 扫描当前类及其所有父类的方法
        while (clazz != null && clazz != Object.class) {
            for (Method method : clazz.getDeclaredMethods()) {
                // 检查方法是否有 @IEventListener 注解
                IEventListener annotation = method.getAnnotation(IEventListener.class);
                if (annotation != null) {
                    try {
                        // 创建 ListenerMethod（会自动验证方法签名）
                        ListenerMethod listenerMethod = new ListenerMethod(listener, method, annotation);
                        methods.add(listenerMethod);
                    } catch (Exception e) {
                        // 记录错误但继续扫描其他方法
                        System.err.println("[EventScanner] Failed to register listener method: " +
                                method + " - " + e.getMessage());
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }

        return methods;
    }

    /**
     * 扫描类并创建实例
     *
     * @param listenerClass 要扫描的类
     * @return 监听器方法列表
     * @throws Exception 如果无法实例化类
     */
    public static List<ListenerMethod> scanClass(Class<?> listenerClass) throws Exception {
        if (listenerClass == null) {
            throw new IllegalArgumentException("Listener class cannot be null");
        }

        // 尝试获取无参构造函数并创建实例
        Object instance = listenerClass.newInstance();
        return scanListener(instance);
    }
}
