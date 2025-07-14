package com.moremod.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("moremod_channel");

    public static void registerMessages() {
        int id = 0;

        // 客户端 -> 服务端
        INSTANCE.registerMessage(
                MessageJetpackJumping.Handler.class,
                MessageJetpackJumping.class,
                id++,
                Side.SERVER
        );

        // 新增：Shift键下降消息
        INSTANCE.registerMessage(
                MessageJetpackSneaking.Handler.class,
                MessageJetpackSneaking.class,
                id++,
                Side.SERVER
        );

        INSTANCE.registerMessage(
                MessageToggleJetpackMode.Handler.class,
                MessageToggleJetpackMode.class,
                id++,
                Side.SERVER
        );

        // 服务端 -> 客户端 （NBT 同步）
        INSTANCE.registerMessage(
                MessageSyncJetpackTagToClient.Handler.class,
                MessageSyncJetpackTagToClient.class,
                id++,
                Side.CLIENT
        );
    }
}