package com.moremod.eventhub.example;

import com.moremod.api.event.EventService;
import com.moremod.api.event.IEventBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

/**
 * 事件系统使用示例
 * 演示如何在主 mod 中使用事件系统
 */
public class ExampleUsage {

    /**
     * 初始化事件系统（在 mod 初始化时调用）
     */
    public static void init() {
        // 获取事件总线
        IEventBus bus = EventService.getBus();

        // 检查是否使用的是真正的实现
        if (EventService.isRealImplementation()) {
            System.out.println("Event system loaded: " + EventService.getProviderInfo());
        } else {
            System.out.println("Event system not available, using No-Op implementation");
        }

        // 注册监听器对象
        bus.register(new ExampleListener());

        // 或者注册监听器类（会自动实例化）
        // bus.registerClass(ExampleListener.class);

        System.out.println("Event listeners registered");
    }

    /**
     * 触发玩家登录事件（示例）
     */
    public static void handlePlayerLogin(EntityPlayer player) {
        IEventBus bus = EventService.getBus();

        // 创建并触发事件
        PlayerLoginEvent event = new PlayerLoginEvent(player);
        bus.post(event);

        // 检查事件是否被取消
        if (event.isCancelled()) {
            System.out.println("Login cancelled: " + event.getCancelReason());
            // 拒绝玩家登录
            player.connection.disconnect(event.getCancelReason());
        } else {
            System.out.println("Login accepted");
            // 允许玩家登录
        }
    }

    /**
     * 触发玩家受伤事件（示例）
     */
    public static float handlePlayerDamage(EntityPlayer player, DamageSource source, float damage) {
        IEventBus bus = EventService.getBus();

        // 创建并触发事件
        PlayerDamageEvent event = new PlayerDamageEvent(player, source, damage);
        bus.post(event);

        // 返回修改后的伤害值
        return event.getDamage();
    }

    /**
     * 注销监听器（示例）
     */
    public static void cleanup() {
        IEventBus bus = EventService.getBus();

        // 清除所有监听器
        bus.clear();

        System.out.println("Event listeners cleared");
    }
}
