package com.moremod.module.impl;

import com.moremod.module.api.IModuleContainer;
import com.moremod.module.api.IModuleContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 模块上下文默认实现
 */
public class ModuleContextImpl implements IModuleContext {

    private final IModuleContainer container;
    private final Map<Class<?>, Object> servicesByClass = new HashMap<>();
    private final Map<String, Object> servicesByName = new HashMap<>();
    private final Map<String, Object> config = new HashMap<>();
    private final boolean isClientSide;
    private final boolean debug;

    public ModuleContextImpl(IModuleContainer container, boolean isClientSide) {
        this(container, isClientSide, false);
    }

    public ModuleContextImpl(IModuleContainer container, boolean isClientSide, boolean debug) {
        this.container = container;
        this.isClientSide = isClientSide;
        this.debug = debug;
    }

    /**
     * 注册服务（内部方法）
     */
    public <T> void registerService(@Nonnull Class<T> serviceClass, @Nonnull T service) {
        servicesByClass.put(serviceClass, service);
    }

    /**
     * 注册服务（通过名称）
     */
    public void registerService(@Nonnull String serviceName, @Nonnull Object service) {
        servicesByName.put(serviceName, service);
    }

    /**
     * 设置配置值（内部方法）
     */
    public void setConfig(@Nonnull String key, @Nullable Object value) {
        if (value != null) {
            config.put(key, value);
        } else {
            config.remove(key);
        }
    }

    @Nullable
    @Override
    public <T> T getService(@Nonnull Class<T> serviceClass) {
        Object service = servicesByClass.get(serviceClass);
        if (service != null && serviceClass.isInstance(service)) {
            return serviceClass.cast(service);
        }
        return null;
    }

    @Nullable
    @Override
    public Object getService(@Nonnull String serviceName) {
        return servicesByName.get(serviceName);
    }

    @Nonnull
    @Override
    public IModuleContainer getModuleContainer() {
        return container;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getConfig(@Nonnull String key, @Nullable T defaultValue) {
        Object value = config.get(key);
        if (value != null) {
            try {
                return (T) value;
            } catch (ClassCastException e) {
                log("warn", "Config type mismatch for key: " + key);
            }
        }
        return defaultValue;
    }

    @Override
    public boolean isClientSide() {
        return isClientSide;
    }

    @Override
    public void log(@Nonnull String level, @Nonnull String message) {
        if (debug || !"debug".equals(level)) {
            System.out.println("[ModuleContext] [" + level.toUpperCase() + "] " + message);
        }
    }
}
