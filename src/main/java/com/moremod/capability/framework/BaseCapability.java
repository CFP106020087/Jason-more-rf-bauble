package com.moremod.capability.framework;

import com.moremod.api.capability.ICapability;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

/**
 * 能力基类
 * 提供常用功能的默认实现
 *
 * @param <T> 宿主类型
 */
public abstract class BaseCapability<T> implements ICapability<T> {

    private final String capabilityId;
    private boolean dirty = false;
    protected T host;

    protected BaseCapability(String capabilityId) {
        this.capabilityId = capabilityId;
    }

    @Override
    public String getCapabilityId() {
        return capabilityId;
    }

    @Override
    public void serializeNBT(NBTTagCompound nbt) {
        // 子类可以覆盖此方法以序列化数据
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        // 子类可以覆盖此方法以反序列化数据
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void markDirty() {
        this.dirty = true;
    }

    @Override
    public void clearDirty() {
        this.dirty = false;
    }

    @Nullable
    @Override
    public ICapability<T> copyTo(T host) {
        // 默认不复制，子类需要覆盖此方法
        return null;
    }

    @Override
    public void onAttached(T host) {
        this.host = host;
    }

    @Override
    public void onDetached(T host) {
        this.host = null;
    }

    @Override
    public void tick(T host) {
        // 默认无操作，子类可以覆盖
    }

    protected T getHost() {
        return host;
    }
}
