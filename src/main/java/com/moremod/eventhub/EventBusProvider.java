package com.moremod.eventhub;

import com.moremod.api.event.EventService;
import com.moremod.api.event.IEventBus;

/**
 * 事件总线提供者
 * 负责将 EventBusImpl 注册到 EventService
 *
 * 设计原则：
 * - 作为事件系统实现与 API 层的唯一连接点
 * - 使用懒加载单例模式
 * - 通过 EventService 反射调用 register() 方法来自动注册
 * - 如果这个类不存在，EventService 会自动 fallback 到 NoOpEventBus
 *
 * 注意：
 * - 这个类必须有一个 public static void register() 方法
 * - EventService 会通过反射调用这个方法
 * - 如果 eventhub 包被删除，EventService 会捕获 ClassNotFoundException 并使用 NoOpEventBus
 */
public final class EventBusProvider {

    /**
     * 单例实例（懒加载）
     */
    private static volatile EventBusImpl instance = null;

    /**
     * 私有构造函数，防止外部实例化
     */
    private EventBusProvider() {
    }

    /**
     * 注册事件总线实现到 EventService
     *
     * 这个方法会被 EventService 通过反射调用
     * 必须是 public static 方法
     */
    public static void register() {
        EventService.setProvider(new EventService.IEventBusProvider() {
            @Override
            public IEventBus provide() {
                return getInstance();
            }
        });

        System.out.println("[EventBusProvider] EventBus implementation registered successfully");
    }

    /**
     * 获取事件总线实例（懒加载单例）
     *
     * @return 事件总线实例
     */
    public static EventBusImpl getInstance() {
        if (instance == null) {
            synchronized (EventBusProvider.class) {
                if (instance == null) {
                    instance = new EventBusImpl(isDebugEnabled());
                    System.out.println("[EventBusProvider] EventBus instance created");
                }
            }
        }
        return instance;
    }

    /**
     * 重置事件总线实例
     *
     * 主要用于测试或热重载场景
     * 会清除所有已注册的监听器
     */
    public static synchronized void reset() {
        if (instance != null) {
            instance.clear();
            instance = null;
            System.out.println("[EventBusProvider] EventBus instance reset");
        }
    }

    /**
     * 检查是否启用调试模式
     *
     * 可以通过 JVM 参数 -DeventBus.debug=true 启用
     *
     * @return true 如果启用调试模式
     */
    private static boolean isDebugEnabled() {
        String debug = System.getProperty("eventBus.debug", "false");
        return "true".equalsIgnoreCase(debug);
    }

    /**
     * 获取事件总线统计信息
     *
     * @return 统计信息字符串
     */
    public static String getStats() {
        if (instance == null) {
            return "EventBus not initialized";
        }
        return instance.toString();
    }
}
