package com.moremod.proxy;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        // 通用的预初始化代码

 }
    public void registerNetworkMessages() {
        // only server messages here
        com.moremod.network.PacketHandler.registerMessages();
    }

    public void init(FMLInitializationEvent event) {
        // 通用的初始化代码
    }

    public void postInit(FMLPostInitializationEvent event) {
        // 通用的后初始化代码
    }
}