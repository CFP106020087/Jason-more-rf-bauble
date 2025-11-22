package com.moremod.api.event;

/**
 * 可取消的事件接口
 * 继承此接口的事件可以被监听器取消
 *
 * 设计原则：
 * - 提供取消状态的读写能力
 * - 不依赖任何实现类
 * - 实现类需要自己维护 cancelled 状态
 */
public interface ICancellableEvent extends IEvent {

    /**
     * 检查事件是否已被取消
     * @return true 如果事件已被取消
     */
    boolean isCancelled();

    /**
     * 设置事件的取消状态
     * @param cancel true 取消事件，false 恢复事件
     */
    void setCancelled(boolean cancel);

    /**
     * 取消事件的便捷方法
     */
    default void cancel() {
        setCancelled(true);
    }
}
