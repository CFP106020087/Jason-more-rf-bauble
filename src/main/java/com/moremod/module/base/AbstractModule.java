package com.moremod.module.base;

import com.moremod.module.api.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 抽象模块基类 - 提供默认实现
 *
 * 继承此类可快速创建新模块，只需覆盖需要的方法
 */
public abstract class AbstractModule implements IModule {

    private final String moduleId;
    private final String displayName;
    private final IModuleDescriptor descriptor;
    private boolean active = false;

    protected AbstractModule(@Nonnull String moduleId, @Nonnull String displayName) {
        this.moduleId = moduleId;
        this.displayName = displayName;
        this.descriptor = createDescriptor();
    }

    /**
     * 创建模块描述符（子类可覆盖）
     */
    protected IModuleDescriptor createDescriptor() {
        return null;  // 默认无描述符
    }

    @Nonnull
    @Override
    public String getModuleId() {
        return moduleId;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    @Override
    public IModuleDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public boolean init(@Nonnull IModuleContext context) {
        context.debug("Initializing module: " + moduleId);
        return true;
    }

    @Override
    public boolean load(@Nonnull IModuleContext context) {
        context.debug("Loading module: " + moduleId);
        return true;
    }

    @Override
    public boolean attach(@Nonnull IModuleHost host, @Nonnull IModuleContext context) {
        context.debug("Attaching module " + moduleId + " to host: " + host.getHostId());
        active = true;
        return true;
    }

    @Override
    public void onTick(@Nonnull IModuleHost host, @Nonnull IModuleContext context) {
        // 默认不执行任何操作
    }

    @Override
    public void detach(@Nonnull IModuleHost host, @Nonnull IModuleContext context) {
        context.debug("Detaching module " + moduleId + " from host: " + host.getHostId());
        active = false;
    }

    @Override
    public void unload(@Nonnull IModuleContext context) {
        context.debug("Unloading module: " + moduleId);
        active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    /**
     * 手动设置激活状态（仅供子类使用）
     */
    protected void setActive(boolean active) {
        this.active = active;
    }
}
