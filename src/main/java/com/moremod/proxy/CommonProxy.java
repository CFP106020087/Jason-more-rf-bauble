package com.moremod.proxy;

import com.moremod.client.render.RenderRiftPortal;
import com.moremod.entity.EntityRiftPortal;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        // 通用的预初始化代码
        RenderingRegistry.registerEntityRenderingHandler(
                EntityRiftPortal.class,
                manager -> new RenderRiftPortal(manager)); }

    public void init(FMLInitializationEvent event) {
        // 通用的初始化代码
    }

    public void postInit(FMLPostInitializationEvent event) {
        // 通用的后初始化代码
    }
}