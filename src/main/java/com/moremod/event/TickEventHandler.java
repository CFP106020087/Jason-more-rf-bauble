package com.moremod.event;

import com.moremod.enchantment.EnchantmentBoostHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Tick 事件处理器
 * 用于定期清理过期的附魔增幅状态
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class TickEventHandler {

    private static int tickCounter = 0;

    /**
     * 每 20 tick（1秒）清理一次过期状态
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;

            if (tickCounter >= 20) {
                tickCounter = 0;
                EnchantmentBoostHelper.tickCleanup();
            }
        }
    }
}