package com.moremod.eventhub.example;

import com.moremod.api.event.ICancellableEvent;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 示例事件：玩家登录事件
 * 演示如何创建一个可取消的事件
 */
public class PlayerLoginEvent implements ICancellableEvent {

    private final EntityPlayer player;
    private boolean cancelled = false;
    private String cancelReason = "";

    public PlayerLoginEvent(EntityPlayer player) {
        this.player = player;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String reason) {
        this.cancelReason = reason;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public String getEventName() {
        return "PlayerLoginEvent[" + player.getName() + "]";
    }
}
