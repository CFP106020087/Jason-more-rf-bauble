package com.moremod.capability.framework;

import com.moremod.api.capability.ICapability;
import com.moremod.api.capability.ICapabilityContainer;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 能力容器实现
 * 此类可被删除，系统会自动 fallback 到 NoOpCapabilityContainer
 *
 * @param <T> 宿主类型
 */
public class CapabilityContainerImpl<T> implements ICapabilityContainer<T> {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String NBT_CAPABILITIES = "Capabilities";

    private final T host;
    private final Map<String, ICapability<T>> capabilitiesById = new ConcurrentHashMap<>();
    private final Map<Class<?>, ICapability<T>> capabilitiesByType = new ConcurrentHashMap<>();

    public CapabilityContainerImpl(T host) {
        this.host = host;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <C extends ICapability<T>> C getCapability(Class<C> capabilityType) {
        return (C) capabilitiesByType.get(capabilityType);
    }

    @Nullable
    @Override
    public ICapability<T> getCapability(String capabilityId) {
        return capabilitiesById.get(capabilityId);
    }

    @Override
    public boolean hasCapability(Class<? extends ICapability<T>> capabilityType) {
        return capabilitiesByType.containsKey(capabilityType);
    }

    @Override
    public boolean hasCapability(String capabilityId) {
        return capabilitiesById.containsKey(capabilityId);
    }

    @Override
    public boolean attachCapability(ICapability<T> capability) {
        String id = capability.getCapabilityId();

        if (capabilitiesById.containsKey(id)) {
            LOGGER.warn("Capability '{}' is already attached to host", id);
            return false;
        }

        capabilitiesById.put(id, capability);
        capabilitiesByType.put(capability.getClass().getInterfaces()[0], capability);

        try {
            capability.onAttached(host);
        } catch (Exception e) {
            LOGGER.error("Error attaching capability '{}'", id, e);
            capabilitiesById.remove(id);
            capabilitiesByType.remove(capability.getClass().getInterfaces()[0]);
            return false;
        }

        return true;
    }

    @Nullable
    @Override
    public ICapability<T> removeCapability(String capabilityId) {
        ICapability<T> capability = capabilitiesById.remove(capabilityId);
        if (capability != null) {
            capabilitiesByType.remove(capability.getClass().getInterfaces()[0]);
            try {
                capability.onDetached(host);
            } catch (Exception e) {
                LOGGER.error("Error detaching capability '{}'", capabilityId, e);
            }
        }
        return capability;
    }

    @Override
    public Collection<ICapability<T>> getAllCapabilities() {
        return Collections.unmodifiableCollection(capabilitiesById.values());
    }

    @Override
    public void serializeNBT(NBTTagCompound nbt) {
        NBTTagCompound capabilitiesNBT = new NBTTagCompound();

        for (Map.Entry<String, ICapability<T>> entry : capabilitiesById.entrySet()) {
            try {
                NBTTagCompound capabilityNBT = new NBTTagCompound();
                entry.getValue().serializeNBT(capabilityNBT);
                capabilitiesNBT.setTag(entry.getKey(), capabilityNBT);
            } catch (Exception e) {
                LOGGER.error("Error serializing capability '{}'", entry.getKey(), e);
            }
        }

        if (!capabilitiesNBT.isEmpty()) {
            nbt.setTag(NBT_CAPABILITIES, capabilitiesNBT);
        }
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        if (!nbt.hasKey(NBT_CAPABILITIES)) {
            return;
        }

        NBTTagCompound capabilitiesNBT = nbt.getCompoundTag(NBT_CAPABILITIES);

        for (String key : capabilitiesNBT.getKeySet()) {
            ICapability<T> capability = capabilitiesById.get(key);
            if (capability != null) {
                try {
                    capability.deserializeNBT(capabilitiesNBT.getCompoundTag(key));
                } catch (Exception e) {
                    LOGGER.error("Error deserializing capability '{}'", key, e);
                }
            } else {
                LOGGER.warn("No capability registered for ID '{}' during deserialization", key);
            }
        }
    }

    @Override
    public T getHost() {
        return host;
    }

    @Override
    public void tick() {
        for (ICapability<T> capability : capabilitiesById.values()) {
            try {
                capability.tick(host);
            } catch (Exception e) {
                LOGGER.error("Error ticking capability '{}'", capability.getCapabilityId(), e);
            }
        }
    }

    @Override
    public void clear() {
        // 先调用所有能力的 onDetached
        for (ICapability<T> capability : capabilitiesById.values()) {
            try {
                capability.onDetached(host);
            } catch (Exception e) {
                LOGGER.error("Error detaching capability '{}'", capability.getCapabilityId(), e);
            }
        }

        capabilitiesById.clear();
        capabilitiesByType.clear();
    }

    @Override
    public ICapabilityContainer<T> copyTo(T newHost) {
        CapabilityContainerImpl<T> newContainer = new CapabilityContainerImpl<>(newHost);

        for (Map.Entry<String, ICapability<T>> entry : capabilitiesById.entrySet()) {
            try {
                ICapability<T> copiedCapability = entry.getValue().copyTo(newHost);
                if (copiedCapability != null) {
                    newContainer.attachCapability(copiedCapability);
                }
            } catch (Exception e) {
                LOGGER.error("Error copying capability '{}'", entry.getKey(), e);
            }
        }

        return newContainer;
    }
}
