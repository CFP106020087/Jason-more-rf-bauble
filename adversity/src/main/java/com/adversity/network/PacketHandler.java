package com.adversity.network;

import com.adversity.Adversity;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 网络包处理器 - 管理客户端与服务端之间的数据同步
 */
public class PacketHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Adversity.MODID);

    private static int id = 0;

    /**
     * 注册所有网络包
     */
    public static void init() {
        // 同步怪物难度数据到客户端
        INSTANCE.registerMessage(
            PacketSyncAdversity.Handler.class,
            PacketSyncAdversity.class,
            id++,
            Side.CLIENT
        );

        Adversity.LOGGER.info("Network packets registered");
    }
}
