package com.moremod.internal.event;

import com.moremod.api.event.EventPriority;
import com.moremod.api.event.IEvent;
import com.moremod.api.event.IEventListener;
import com.moremod.api.event.ICancellableEvent;

/**
 * 监听器注册条目
 *
 * 内部使用，封装监听器元数据
 */
class ListenerEntry<T extends IEvent> implements Comparable<ListenerEntry<T>> {

    private final IEventListener<T> listener;
    private final EventPriority priority;
    private final boolean receiveCancelled;
    private final Object owner; // 用于注销

    public ListenerEntry(
        IEventListener<T> listener,
        EventPriority priority,
        boolean receiveCancelled,
        Object owner
    ) {
        this.listener = listener;
        this.priority = priority;
        this.receiveCancelled = receiveCancelled;
        this.owner = owner;
    }

    public void invoke(T event) {
        listener.onEvent(event);
    }

    public boolean shouldReceive(T event) {
        if (event instanceof ICancellableEvent) {
            ICancellableEvent cancellable = (ICancellableEvent) event;
            if (cancellable.isCancelled() && !receiveCancelled) {
                return false;
            }
        }
        return true;
    }

    public Object getOwner() {
        return owner;
    }

    @Override
    public int compareTo(ListenerEntry<T> other) {
        return Integer.compare(this.priority.getValue(), other.priority.getValue());
    }
}
