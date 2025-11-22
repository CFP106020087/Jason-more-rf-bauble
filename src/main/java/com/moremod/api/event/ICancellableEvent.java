package com.moremod.api.event;

/**
 * 可取消事件接口
 *
 * 实现此接口的事件可以被监听器取消
 */
public interface ICancellableEvent extends IEvent {

    /**
     * 是否已被取消
     */
    boolean isCancelled();

    /**
     * 设置取消状态
     */
    void setCancelled(boolean cancelled);
}
