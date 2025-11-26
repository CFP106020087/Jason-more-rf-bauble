package com.moremod.accessorybox.unlock;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 网络处理器
 */
public class ModNetworkHandler {
    
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("moremod_1");
    
    private static int packetId = 0;
    
    public static void registerPackets() {
        // 注册同步解锁槽位数据包
        INSTANCE.registerMessage(
            PacketSyncUnlockedSlots.Handler.class,
            PacketSyncUnlockedSlots.class,
            packetId++,
            Side.CLIENT
        );
        
        System.out.println("[ModNetwork] 注册网络数据包完成");
    }
}
