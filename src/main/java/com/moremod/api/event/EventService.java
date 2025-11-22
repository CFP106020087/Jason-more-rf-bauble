package com.moremod.api.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 事件服务定位器
 *
 * 职责：
 *  ✓ 提供全局事件总线访问
 *  ✓ Service Locator 模式
 *  ✓ 自动 fallback 到 No-Op 实现
 *  ✓ 主代码的唯一依赖点
 *
 * 使用方式：
 * <pre>
 * {@code
 * // 发布事件
 * EventService.getBus().post(new MyEvent());
 *
 * // 注册监听器
 * EventService.getBus().register(myListener);
 * }
 * </pre>
 */
public class EventService {

    private static final Logger logger = LogManager.getLogger("EventService");

    /** 当前事件总线实例 */
    private static volatile IEventBus instance = null;

    /** 是否已初始化 */
    private static volatile boolean initialized = false;

    /**
     * 获取事件总线
     *
     * 如果未初始化，自动尝试加载实现
     * 如果加载失败，返回 No-Op 实现
     */
    public static IEventBus getBus() {
        if (!initialized) {
            synchronized (EventService.class) {
                if (!initialized) {
                    initialize();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化事件总线
     *
     * 优先级：
     *  1. 手动设置的实例
     *  2. 反射加载 EventBusImpl
     *  3. Fallback 到 NoOpEventBus
     */
    private static void initialize() {
        if (instance != null) {
            initialized = true;
            return;
        }

        // 尝试加载完整实现
        try {
            Class<?> implClass = Class.forName("com.moremod.internal.event.EventBusImpl");
            instance = (IEventBus) implClass.newInstance();
            logger.info("EventService initialized with EventBusImpl");
        } catch (ClassNotFoundException e) {
            // 实现类不存在，使用 No-Op
            logger.warn("EventBusImpl not found, using NoOpEventBus (events disabled)");
            loadNoOp();
        } catch (Exception e) {
            // 实例化失败，使用 No-Op
            logger.error("Failed to initialize EventBusImpl, using NoOpEventBus", e);
            loadNoOp();
        }

        initialized = true;
    }

    /**
     * 加载 No-Op 实现
     */
    private static void loadNoOp() {
        try {
            Class<?> noOpClass = Class.forName("com.moremod.internal.event.NoOpEventBus");
            java.lang.reflect.Method getInstance = noOpClass.getMethod("getInstance");
            instance = (IEventBus) getInstance.invoke(null);
        } catch (Exception e) {
            // 连 No-Op 都加载不了，创建最小实现
            logger.error("Failed to load NoOpEventBus, using minimal fallback", e);
            instance = createMinimalFallback();
        }
    }

    /**
     * 创建最小 Fallback（内联实现）
     */
    private static IEventBus createMinimalFallback() {
        return new IEventBus() {
            @Override
            public void register(Object listener) {}

            @Override
            public void unregister(Object listener) {}

            @Override
            public <T extends IEvent> void addListener(
                Class<T> eventClass,
                IEventListener<T> listener,
                EventPriority priority
            ) {}

            @Override
            public boolean post(IEvent event) {
                return false;
            }

            @Override
            public void postAsync(IEvent event) {}

            @Override
            public int getListenerCount() {
                return 0;
            }

            @Override
            public void shutdown() {}
        };
    }

    /**
     * 手动设置事件总线实例
     *
     * 用于：
     *  - 测试环境注入 Mock
     *  - 自定义实现
     *  - 插件系统替换
     *
     * @param bus 事件总线实例
     */
    public static synchronized void setInstance(IEventBus bus) {
        if (initialized) {
            throw new IllegalStateException(
                "EventService already initialized, cannot replace instance"
            );
        }
        instance = bus;
        initialized = true;
        logger.info("EventService initialized with custom instance: {}", bus.getClass().getName());
    }

    /**
     * 重置服务（仅用于测试）
     */
    public static synchronized void reset() {
        if (instance != null) {
            instance.shutdown();
        }
        instance = null;
        initialized = false;
    }

    /**
     * 快捷方法：发布事件
     */
    public static boolean post(IEvent event) {
        return getBus().post(event);
    }

    /**
     * 快捷方法：注册监听器
     */
    public static void register(Object listener) {
        getBus().register(listener);
    }

    /**
     * 快捷方法：注销监听器
     */
    public static void unregister(Object listener) {
        getBus().unregister(listener);
    }
}
