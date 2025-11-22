package com.moremod.module.fallback;

import com.moremod.module.api.IModuleContainer;
import com.moremod.module.api.IModuleContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * No-Op 模块上下文 - 当模块系统不可用时的安全替代
 */
public class NoOpModuleContext implements IModuleContext {

    public static final NoOpModuleContext INSTANCE = new NoOpModuleContext();

    private NoOpModuleContext() {}

    @Nullable
    @Override
    public <T> T getService(@Nonnull Class<T> serviceClass) {
        return null;
    }

    @Nullable
    @Override
    public Object getService(@Nonnull String serviceName) {
        return null;
    }

    @Nonnull
    @Override
    public IModuleContainer getModuleContainer() {
        return NoOpModuleContainer.INSTANCE;
    }

    @Nullable
    @Override
    public <T> T getConfig(@Nonnull String key, @Nullable T defaultValue) {
        return defaultValue;
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Override
    public void log(@Nonnull String level, @Nonnull String message) {
        // No-Op (不输出日志)
    }
}
