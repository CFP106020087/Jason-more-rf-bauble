package com.moremod.module.fallback;

import com.moremod.module.api.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/**
 * No-Op 模块容器 - 当模块系统不可用时的安全替代
 *
 * 特性:
 * - 所有操作都不抛出异常
 * - 不执行任何实际操作
 * - 保证游戏在没有模块系统时仍能正常运行
 */
public class NoOpModuleContainer implements IModuleContainer {

    public static final NoOpModuleContainer INSTANCE = new NoOpModuleContainer();

    private NoOpModuleContainer() {}

    @Override
    public boolean registerModule(@Nonnull IModule module) {
        return false;  // 静默失败
    }

    @Override
    public boolean unregisterModule(@Nonnull String moduleId) {
        return false;
    }

    @Nullable
    @Override
    public IModule getModule(@Nonnull String moduleId) {
        return null;
    }

    @Override
    public boolean hasModule(@Nonnull String moduleId) {
        return false;
    }

    @Nonnull
    @Override
    public Collection<IModule> getAllModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Collection<IModule> getActiveModules() {
        return Collections.emptyList();
    }

    @Override
    public void initializeAll(@Nonnull IModuleContext context) {
        // No-Op
    }

    @Override
    public void loadAll(@Nonnull IModuleContext context) {
        // No-Op
    }

    @Override
    public void unloadAll(@Nonnull IModuleContext context) {
        // No-Op
    }

    @Override
    public void attachAll(@Nonnull IModuleHost host, @Nonnull IModuleContext context) {
        // No-Op
    }

    @Override
    public void detachAll(@Nonnull IModuleHost host, @Nonnull IModuleContext context) {
        // No-Op
    }

    @Override
    public void tickAll(@Nonnull IModuleHost host, @Nonnull IModuleContext context) {
        // No-Op
    }

    @Nullable
    @Override
    public Object sendMessage(@Nonnull String senderId, @Nullable String targetId,
                              @Nonnull Object message, @Nonnull IModuleContext context) {
        return null;
    }
}
