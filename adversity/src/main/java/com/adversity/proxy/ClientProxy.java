package com.adversity.proxy;

import com.adversity.Adversity;
import com.adversity.client.AdversityClientHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // 注册客户端事件处理器（用于渲染词条标识等）
        MinecraftForge.EVENT_BUS.register(new AdversityClientHandler());

        Adversity.LOGGER.info("Client handlers registered");
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }
}
