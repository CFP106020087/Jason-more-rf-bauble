package com.moremod.capability.framework;

import com.moremod.api.capability.ICapabilityDescriptor;
import com.moremod.api.capability.ICapabilityRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 能力注册表实现
 * 此类可被删除，系统会自动 fallback
 */
public class CapabilityRegistryImpl implements ICapabilityRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    // 使用 ConcurrentHashMap 保证线程安全
    private final Map<String, ICapabilityDescriptor<?>> descriptorById = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<ICapabilityDescriptor<?>>> descriptorsByHost = new ConcurrentHashMap<>();
    private volatile boolean frozen = false;

    @Override
    public <T> boolean registerCapability(ICapabilityDescriptor<T> descriptor) {
        if (frozen) {
            LOGGER.warn("Cannot register capability '{}' - registry is frozen", descriptor.getCapabilityId());
            return false;
        }

        String id = descriptor.getCapabilityId();
        if (descriptorById.containsKey(id)) {
            LOGGER.error("Capability '{}' is already registered", id);
            return false;
        }

        descriptorById.put(id, descriptor);

        // 按宿主类型索引
        Class<T> hostType = descriptor.getHostType();
        descriptorsByHost.computeIfAbsent(hostType, k -> new ArrayList<>()).add(descriptor);

        LOGGER.info("Registered capability: {} for host type: {}", id, hostType.getSimpleName());
        return true;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> ICapabilityDescriptor<T> getDescriptor(String capabilityId) {
        return (ICapabilityDescriptor<T>) descriptorById.get(capabilityId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<ICapabilityDescriptor<T>> getDescriptorsForHost(Class<T> hostType) {
        List<ICapabilityDescriptor<?>> descriptors = descriptorsByHost.get(hostType);
        if (descriptors == null) {
            return Collections.emptyList();
        }

        // 按优先级排序
        return descriptors.stream()
                .map(d -> (ICapabilityDescriptor<T>) d)
                .sorted(Comparator.comparingInt(ICapabilityDescriptor::getPriority))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ICapabilityDescriptor<?>> getAllDescriptors() {
        return Collections.unmodifiableCollection(descriptorById.values());
    }

    @Override
    public boolean isRegistered(String capabilityId) {
        return descriptorById.containsKey(capabilityId);
    }

    @Override
    public boolean unregisterCapability(String capabilityId) {
        if (frozen) {
            LOGGER.warn("Cannot unregister capability '{}' - registry is frozen", capabilityId);
            return false;
        }

        ICapabilityDescriptor<?> descriptor = descriptorById.remove(capabilityId);
        if (descriptor != null) {
            // 从宿主类型索引中移除
            List<ICapabilityDescriptor<?>> hostDescriptors = descriptorsByHost.get(descriptor.getHostType());
            if (hostDescriptors != null) {
                hostDescriptors.remove(descriptor);
            }
            LOGGER.info("Unregistered capability: {}", capabilityId);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        if (frozen) {
            LOGGER.warn("Cannot clear registry - it is frozen");
            return;
        }
        descriptorById.clear();
        descriptorsByHost.clear();
        LOGGER.info("Capability registry cleared");
    }

    @Override
    public void freeze() {
        if (!frozen) {
            frozen = true;
            LOGGER.info("Capability registry frozen with {} capabilities", descriptorById.size());
        }
    }

    @Override
    public boolean isFrozen() {
        return frozen;
    }
}
