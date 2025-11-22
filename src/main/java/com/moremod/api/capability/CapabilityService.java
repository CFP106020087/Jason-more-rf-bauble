package com.moremod.api.capability;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

/**
 * 能力系统服务定位器
 * 主 mod 通过此类访问能力系统
 *
 * <p>设计原则：
 * <ul>
 *   <li>此类位于 API 包，确保主 mod 可见</li>
 *   <li>当实现层存在时，使用真实实现</li>
 *   <li>当实现层缺失时，自动 fallback 到 NoOp</li>
 *   <li>主 mod 只能看到接口，不能看到实现</li>
 * </ul>
 */
public final class CapabilityService {

    private static final Logger LOGGER = LogManager.getLogger();
    private static ICapabilityRegistry registry;
    private static boolean initialized = false;
    private static boolean usingFallback = false;

    private CapabilityService() {
        // 工具类，禁止实例化
    }

    /**
     * 初始化能力系统
     * 应该在 Mod 的 PreInit 阶段调用
     */
    public static void initialize() {
        if (initialized) {
            LOGGER.warn("CapabilityService already initialized");
            return;
        }

        // 尝试加载实现层
        try {
            Class<?> implClass = Class.forName("com.moremod.capability.framework.CapabilityRegistryImpl");
            registry = (ICapabilityRegistry) implClass.newInstance();
            usingFallback = false;
            LOGGER.info("CapabilityService initialized with full implementation");
        } catch (ClassNotFoundException e) {
            // 实现层不存在，使用 NoOp fallback
            registry = new NoOpCapabilityRegistry();
            usingFallback = true;
            LOGGER.warn("Capability framework implementation not found, using NoOp fallback");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize capability framework, using NoOp fallback", e);
            registry = new NoOpCapabilityRegistry();
            usingFallback = true;
        }

        initialized = true;
    }

    /**
     * 获取能力注册表
     * @return 能力注册表实例
     */
    public static ICapabilityRegistry getRegistry() {
        if (!initialized) {
            initialize();
        }
        return registry;
    }

    /**
     * 创建能力容器
     * @param host 宿主对象
     * @param <T> 宿主类型
     * @return 能力容器实例
     */
    public static <T> ICapabilityContainer<T> createContainer(T host) {
        if (!initialized) {
            initialize();
        }

        if (usingFallback) {
            return new NoOpCapabilityContainer<>(host);
        }

        try {
            Class<?> implClass = Class.forName("com.moremod.capability.framework.CapabilityContainerImpl");
            @SuppressWarnings("unchecked")
            ICapabilityContainer<T> container = (ICapabilityContainer<T>)
                implClass.getConstructor(Object.class).newInstance(host);
            return container;
        } catch (Exception e) {
            LOGGER.error("Failed to create capability container, using NoOp fallback", e);
            return new NoOpCapabilityContainer<>(host);
        }
    }

    /**
     * 是否使用 Fallback 模式
     * @return true 如果使用 NoOp fallback
     */
    public static boolean isUsingFallback() {
        return usingFallback;
    }

    /**
     * 重置服务（仅用于测试）
     */
    public static void reset() {
        registry = null;
        initialized = false;
        usingFallback = false;
    }

    /**
     * NoOp 注册表实现（Fallback）
     */
    private static class NoOpCapabilityRegistry implements ICapabilityRegistry {

        @Override
        public <T> boolean registerCapability(ICapabilityDescriptor<T> descriptor) {
            return false;
        }

        @Nullable
        @Override
        public <T> ICapabilityDescriptor<T> getDescriptor(String capabilityId) {
            return null;
        }

        @Override
        public <T> java.util.Collection<ICapabilityDescriptor<T>> getDescriptorsForHost(Class<T> hostType) {
            return java.util.Collections.emptyList();
        }

        @Override
        public java.util.Collection<ICapabilityDescriptor<?>> getAllDescriptors() {
            return java.util.Collections.emptyList();
        }

        @Override
        public boolean isRegistered(String capabilityId) {
            return false;
        }

        @Override
        public boolean unregisterCapability(String capabilityId) {
            return false;
        }

        @Override
        public void clear() {
            // NoOp
        }

        @Override
        public void freeze() {
            // NoOp
        }

        @Override
        public boolean isFrozen() {
            return true;
        }
    }
}
