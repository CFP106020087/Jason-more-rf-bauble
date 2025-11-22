package com.moremod.api.capability;

import net.minecraft.nbt.NBTTagCompound;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/**
 * NoOp 能力容器（Fallback 实现）
 * 当能力系统实现层缺失时使用
 *
 * <p>设计原则：
 * <ul>
 *   <li>此类位于 API 包，确保始终可用</li>
 *   <li>所有操作都是无操作的（No-Op）</li>
 *   <li>不会抛出异常，保证系统稳定性</li>
 *   <li>主 mod 在能力系统缺失时会自动使用此容器</li>
 * </ul>
 *
 * @param <T> 宿主类型
 */
public final class NoOpCapabilityContainer<T> implements ICapabilityContainer<T> {

    private final T host;

    public NoOpCapabilityContainer(T host) {
        this.host = host;
    }

    @Nullable
    @Override
    public <C extends ICapability<T>> C getCapability(Class<C> capabilityType) {
        return null;
    }

    @Nullable
    @Override
    public ICapability<T> getCapability(String capabilityId) {
        return null;
    }

    @Override
    public boolean hasCapability(Class<? extends ICapability<T>> capabilityType) {
        return false;
    }

    @Override
    public boolean hasCapability(String capabilityId) {
        return false;
    }

    @Override
    public boolean attachCapability(ICapability<T> capability) {
        // NoOp: 无法附加能力
        return false;
    }

    @Nullable
    @Override
    public ICapability<T> removeCapability(String capabilityId) {
        return null;
    }

    @Override
    public Collection<ICapability<T>> getAllCapabilities() {
        return Collections.emptyList();
    }

    @Override
    public void serializeNBT(NBTTagCompound nbt) {
        // NoOp: 无需序列化
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        // NoOp: 无需反序列化
    }

    @Override
    public T getHost() {
        return host;
    }

    @Override
    public void tick() {
        // NoOp: 无需更新
    }

    @Override
    public void clear() {
        // NoOp: 已经为空
    }

    @Override
    public ICapabilityContainer<T> copyTo(T newHost) {
        return new NoOpCapabilityContainer<>(newHost);
    }
}
