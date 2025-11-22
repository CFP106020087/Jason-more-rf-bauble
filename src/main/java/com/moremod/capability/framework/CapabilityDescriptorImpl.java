package com.moremod.capability.framework;

import com.moremod.api.capability.ICapability;
import com.moremod.api.capability.ICapabilityDescriptor;
import com.moremod.api.capability.ICapabilityProvider;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * 能力描述符实现
 * 此类可被删除，系统会自动 fallback
 *
 * @param <T> 宿主类型
 */
public class CapabilityDescriptorImpl<T> implements ICapabilityDescriptor<T> {

    private final String capabilityId;
    private final ICapabilityProvider<T, ? extends ICapability<T>> provider;
    private final Class<T> hostType;
    private Predicate<T> attachCondition;
    private int priority = 1000;
    private boolean autoSerialize = true;
    private boolean autoSync = false;
    private String description;

    public CapabilityDescriptorImpl(
            String capabilityId,
            ICapabilityProvider<T, ? extends ICapability<T>> provider,
            Class<T> hostType) {
        this.capabilityId = capabilityId;
        this.provider = provider;
        this.hostType = hostType;
        this.attachCondition = host -> true; // 默认总是附加
    }

    @Override
    public String getCapabilityId() {
        return capabilityId;
    }

    @Override
    public ICapabilityProvider<T, ? extends ICapability<T>> getProvider() {
        return provider;
    }

    @Override
    public Class<T> getHostType() {
        return hostType;
    }

    @Override
    public boolean shouldAttachTo(T host) {
        return attachCondition.test(host);
    }

    @Override
    public ICapabilityDescriptor<T> setAttachCondition(Predicate<T> predicate) {
        this.attachCondition = predicate;
        return this;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public CapabilityDescriptorImpl<T> setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public boolean shouldAutoSerialize() {
        return autoSerialize;
    }

    public CapabilityDescriptorImpl<T> setAutoSerialize(boolean autoSerialize) {
        this.autoSerialize = autoSerialize;
        return this;
    }

    @Override
    public boolean shouldAutoSync() {
        return autoSync;
    }

    public CapabilityDescriptorImpl<T> setAutoSync(boolean autoSync) {
        this.autoSync = autoSync;
        return this;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    public CapabilityDescriptorImpl<T> setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * 构建器模式创建描述符
     */
    public static <T> Builder<T> builder(
            String capabilityId,
            ICapabilityProvider<T, ? extends ICapability<T>> provider,
            Class<T> hostType) {
        return new Builder<>(capabilityId, provider, hostType);
    }

    public static class Builder<T> {
        private final CapabilityDescriptorImpl<T> descriptor;

        private Builder(String capabilityId,
                       ICapabilityProvider<T, ? extends ICapability<T>> provider,
                       Class<T> hostType) {
            this.descriptor = new CapabilityDescriptorImpl<>(capabilityId, provider, hostType);
        }

        public Builder<T> attachCondition(Predicate<T> predicate) {
            descriptor.setAttachCondition(predicate);
            return this;
        }

        public Builder<T> priority(int priority) {
            descriptor.setPriority(priority);
            return this;
        }

        public Builder<T> autoSerialize(boolean autoSerialize) {
            descriptor.setAutoSerialize(autoSerialize);
            return this;
        }

        public Builder<T> autoSync(boolean autoSync) {
            descriptor.setAutoSync(autoSync);
            return this;
        }

        public Builder<T> description(String description) {
            descriptor.setDescription(description);
            return this;
        }

        public CapabilityDescriptorImpl<T> build() {
            return descriptor;
        }
    }
}
