package com.moremod.eventhub.example;

import com.moremod.api.event.EventPriority;
import com.moremod.api.event.IEventListener;

/**
 * 示例监听器类
 * 演示如何创建事件监听器
 */
public class ExampleListener {

    /**
     * 监听玩家登录事件（高优先级）
     * 高优先级监听器会先执行
     */
    @IEventListener(priority = EventPriority.HIGH)
    public void onPlayerLoginHigh(PlayerLoginEvent event) {
        System.out.println("[HIGH] Player logging in: " + event.getPlayer().getName());

        // 可以取消事件
        if (event.getPlayer().getName().equals("Hacker")) {
            event.cancel();
            event.setCancelReason("You are banned!");
            System.out.println("[HIGH] Login cancelled for hacker");
        }
    }

    /**
     * 监听玩家登录事件（普通优先级）
     * 如果事件被取消，这个方法不会被调用（因为 receiveCancelled = false）
     */
    @IEventListener(priority = EventPriority.NORMAL)
    public void onPlayerLogin(PlayerLoginEvent event) {
        System.out.println("[NORMAL] Welcome " + event.getPlayer().getName() + "!");
    }

    /**
     * 监听玩家登录事件（低优先级，接收已取消的事件）
     * 即使事件被取消，这个方法仍然会被调用
     */
    @IEventListener(priority = EventPriority.LOW, receiveCancelled = true)
    public void onPlayerLoginMonitor(PlayerLoginEvent event) {
        if (event.isCancelled()) {
            System.out.println("[MONITOR] Login was cancelled: " + event.getCancelReason());
        } else {
            System.out.println("[MONITOR] Login successful for " + event.getPlayer().getName());
        }
    }

    /**
     * 监听玩家受伤事件
     */
    @IEventListener
    public void onPlayerDamage(PlayerDamageEvent event) {
        System.out.println("Player " + event.getPlayer().getName() +
                " took " + event.getDamage() + " damage from " + event.getSource());

        // 可以修改事件数据
        if (event.getDamage() > 10.0f) {
            event.setDamage(10.0f);
            System.out.println("Damage capped at 10.0");
        }
    }
}
