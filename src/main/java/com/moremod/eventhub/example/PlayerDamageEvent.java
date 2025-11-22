package com.moremod.eventhub.example;

import com.moremod.api.event.IEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

/**
 * 示例事件：玩家受伤事件
 * 演示如何创建一个普通事件（不可取消）
 */
public class PlayerDamageEvent implements IEvent {

    private final EntityPlayer player;
    private final DamageSource source;
    private float damage;

    public PlayerDamageEvent(EntityPlayer player, DamageSource source, float damage) {
        this.player = player;
        this.source = source;
        this.damage = damage;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public DamageSource getSource() {
        return source;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public String getEventName() {
        return "PlayerDamageEvent[" + player.getName() + ", damage=" + damage + "]";
    }
}
