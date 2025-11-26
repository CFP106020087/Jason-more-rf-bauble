package com.moremod.client;

import com.moremod.init.ModItems;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import software.bernie.geckolib3.GeckoLib;

@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
public class ClientItemISTERBinder {

    @SubscribeEvent
    public static void onModelRegister(ModelRegistryEvent e) {
        // 先确保 GeckoLib 初始化（多次调用也安全）
        try { GeckoLib.initialize(); } catch (Throwable ignored) {}

        if (ModItems.SWORD_CHENGYUE != null) {
            ModItems.SWORD_CHENGYUE.registerISTER();
            System.out.println("[MoreMod] ISTER bound for Sword ChengYue");
        }
    }
}
