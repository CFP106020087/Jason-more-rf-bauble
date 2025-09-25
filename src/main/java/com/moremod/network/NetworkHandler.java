package com.moremod.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 网络处理器注册 - 完整版
 */
public class NetworkHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("moremod");

    private static int packetId = 0;

    /**
     * 初始化网络处理器
     * 在你的主模组类的 preInit 中调用这个方法
     */
    public static void init() {
        System.out.println("[moremod] 初始化网络处理器...");

        // 注册机械核心更新数据包
        INSTANCE.registerMessage(
                PacketMechanicalCoreUpdate.Handler.class,
                PacketMechanicalCoreUpdate.class,
                packetId++,
                Side.SERVER
        );

        // 注册GUI开关数据包
        INSTANCE.registerMessage(
                PacketOpenGui.Handler.class,
                PacketOpenGui.class,
                packetId++,
                Side.SERVER
        );


        // 注册升级选择数据包
        INSTANCE.registerMessage(
                PacketUpgradeSelection.Handler.class,
                PacketUpgradeSelection.class,
                packetId++,
                Side.SERVER
        );

        System.out.println("[moremod] 网络处理器初始化完成，注册了 " + packetId + " 个数据包");
    }
}