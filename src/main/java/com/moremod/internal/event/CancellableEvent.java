package com.moremod.internal.event;

import com.moremod.api.event.ICancellableEvent;

/**
 * 可取消事件基类
 *
 * 提供默认实现，减少样板代码
 */
public abstract class CancellableEvent implements ICancellableEvent {

    private boolean cancelled = false;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
