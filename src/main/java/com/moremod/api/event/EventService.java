package com.moremod.api.event;

import com.moremod.api.event.internal.NoOpEventBus;

/**
 * 事件服务定位器（Service Locator）
 * 提供全局访问事件总线的入口点
 *
 * 设计原则：
 * - 使用 Lazy Provider 模式，延迟加载实现
 * - 不使用单例静态硬绑定，支持运行时替换
 * - 自动检测事件系统实现是否存在
 * - 如果实现不存在，自动 fallback 到 NoOpEventBus
 * - 线程安全
 *
 * 使用示例：
 * <pre>
 * {@code
 * // 获取事件总线
 * IEventBus bus = EventService.getBus();
 *
 * // 注册监听器
 * bus.register(new MyListener());
 *
 * // 触发事件
 * bus.post(new MyEvent());
 * }
 * </pre>
 */
public final class EventService {

    /**
     * 事件总线提供者接口
     * 实现类需要提供事件总线的实例
     */
    public interface IEventBusProvider {
        IEventBus provide();
    }

    /**
     * 当前事件总线提供者
     * 使用 volatile 保证线程安全
     */
    private static volatile IEventBusProvider provider = null;

    /**
     * 懒加载标志
     */
    private static volatile boolean initialized = false;

    /**
     * 私有构造函数，防止实例化
     */
    private EventService() {
    }

    /**
     * 获取事件总线实例
     *
     * 首次调用时会尝试加载真正的事件系统实现，
     * 如果加载失败，会自动 fallback 到 NoOpEventBus
     *
     * @return 事件总线实例（永远不会返回 null）
     */
    public static IEventBus getBus() {
        if (!initialized) {
            synchronized (EventService.class) {
                if (!initialized) {
                    tryLoadImplementation();
                    initialized = true;
                }
            }
        }

        if (provider != null) {
            return provider.provide();
        }

        // Fallback to No-Op implementation
        return NoOpEventBus.INSTANCE;
    }

    /**
     * 设置事件总线提供者
     *
     * 允许事件系统实现在运行时注册自己
     * 这是唯一允许外部代码设置提供者的方法
     *
     * @param newProvider 新的事件总线提供者
     */
    public static synchronized void setProvider(IEventBusProvider newProvider) {
        if (newProvider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
        provider = newProvider;
        initialized = true;
    }

    /**
     * 重置事件服务
     *
     * 清除当前提供者，下次调用 getBus() 时会重新加载
     * 主要用于测试和热重载场景
     */
    public static synchronized void reset() {
        provider = null;
        initialized = false;
    }

    /**
     * 检查是否使用的是真正的事件系统实现
     *
     * @return true 如果使用真正的实现，false 如果使用 NoOpEventBus
     */
    public static boolean isRealImplementation() {
        IEventBus bus = getBus();
        return !(bus instanceof NoOpEventBus);
    }

    /**
     * 尝试加载事件系统实现
     *
     * 使用反射尝试加载 eventhub 包中的实现类
     * 如果加载失败（类不存在），不会抛出异常，而是静默失败
     */
    private static void tryLoadImplementation() {
        try {
            // 尝试加载 EventBusProvider 类
            // 这个类位于 com.moremod.eventhub 包中
            // 如果 eventhub 包被删除，这里会抛出 ClassNotFoundException
            Class<?> providerClass = Class.forName("com.moremod.eventhub.EventBusProvider");

            // 调用 EventBusProvider.register() 静态方法
            // 这会将真正的实现注册到 EventService
            providerClass.getMethod("register").invoke(null);

        } catch (ClassNotFoundException e) {
            // 实现类不存在，使用 NoOpEventBus
            // 这是正常情况，不需要记录日志
        } catch (Exception e) {
            // 其他异常，记录警告但不影响运行
            System.err.println("[EventService] Failed to load event system implementation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取当前提供者的调试信息
     *
     * @return 提供者信息字符串
     */
    public static String getProviderInfo() {
        IEventBus bus = getBus();
        if (bus instanceof NoOpEventBus) {
            return "NoOpEventBus (Event system implementation not found)";
        } else {
            return bus.getClass().getName();
        }
    }
}
