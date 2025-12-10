package com.moremod.quarry.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 采石场网络包处理器
 */
public class PacketHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("moremod_quarry");

    private static int packetId = 0;

    public static void init() {
        // 客户端 -> 服务器
        INSTANCE.registerMessage(
                PacketQuarryButton.Handler.class,
                PacketQuarryButton.class,
                packetId++,
                Side.SERVER
        );

        INSTANCE.registerMessage(
                PacketSelectBiome.Handler.class,
                PacketSelectBiome.class,
                packetId++,
                Side.SERVER
        );

        // 服务器 -> 客户端
        INSTANCE.registerMessage(
                PacketQuarrySync.Handler.class,
                PacketQuarrySync.class,
                packetId++,
                Side.CLIENT
        );
    }
}
