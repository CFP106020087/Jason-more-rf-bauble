package com.moremod.sponsor.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 赞助者物品网络处理器
 */
public class SponsorNetworkHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("moremod_sponsor");

    private static int packetId = 0;

    /**
     * 注册网络包
     */
    public static void init() {
        // 客户端 -> 服务器: 切换技能
        INSTANCE.registerMessage(
            PacketToggleSkill.Handler.class,
            PacketToggleSkill.class,
            packetId++,
            Side.SERVER
        );

        System.out.println("[moremod/sponsor] 网络处理器已初始化");
    }

    /**
     * 发送消息到服务器
     */
    public static void sendToServer(IMessage message) {
        INSTANCE.sendToServer(message);
    }
}
