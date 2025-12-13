package com.moremod.proxy;

import com.moremod.printer.PrinterRecipeRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        // 通用的预初始化代码
        // 注册打印机默认配方（在CraftTweaker之前）
        PrinterRecipeRegistry.registerDefaultRecipes();
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