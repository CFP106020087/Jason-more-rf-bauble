package com.adversity.proxy;

import com.adversity.Adversity;
import com.adversity.affix.AffixRegistry;
import com.adversity.capability.CapabilityHandler;
import com.adversity.difficulty.DifficultyManager;
import com.adversity.event.MobEventHandler;
import com.adversity.network.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        // 注册 Capability
        CapabilityHandler.register();

        // 初始化词条注册表
        AffixRegistry.init();

        // 初始化网络包处理
        PacketHandler.init();

        Adversity.LOGGER.info("Capability, Affix Registry and Network initialized");
    }

    public void init(FMLInitializationEvent event) {
        // 注册事件处理器
        MinecraftForge.EVENT_BUS.register(new MobEventHandler());
        MinecraftForge.EVENT_BUS.register(new CapabilityHandler());

        // 初始化难度管理器
        DifficultyManager.init();

        Adversity.LOGGER.info("Event handlers registered");
    }

    public void postInit(FMLPostInitializationEvent event) {
        Adversity.LOGGER.info("Adversity loaded successfully");
    }
}
