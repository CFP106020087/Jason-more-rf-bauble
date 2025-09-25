package com.moremod.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("moremod_channel");

    public static void registerMessages() {
        int id = 0;

        // 🕰️ 时光之心数据同步包注册
        INSTANCE.registerMessage(
                PacketSyncPlayerTime.Handler.class,
                PacketSyncPlayerTime.class,
                id++,
                Side.CLIENT
        );

        // === 喷气背包相关消息 ===

        // 客户端 -> 服务端：跳跃状态
        INSTANCE.registerMessage(
                MessageJetpackJumping.Handler.class,
                MessageJetpackJumping.class,
                id++,
                Side.SERVER
        );

        // 客户端 -> 服务端：下降状态
        INSTANCE.registerMessage(
                MessageJetpackSneaking.Handler.class,
                MessageJetpackSneaking.class,
                id++,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                PacketDimensionalRipperKey.Handler.class,
                PacketDimensionalRipperKey.class,
                id++,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                PacketPersonalDimensionKey.Handler.class,
                PacketPersonalDimensionKey.class,
                id++,
                Side.SERVER
        );

        // 客户端 -> 服务端：模式切换
        // 选项1：使用扩展处理器（同时支持喷气背包和机械核心）
        INSTANCE.registerMessage(
                PacketActivateBoost.Handler.class,  // 使用新的扩展处理器
                PacketActivateBoost.class,
                id++,
                Side.SERVER
        );

        // 客户端 -> 服务端：模式切换
        // 选项1：使用扩展处理器（同时支持喷气背包和机械核心）
        INSTANCE.registerMessage(
                ExtendedModeHandler.class,  // 使用新的扩展处理器
                MessageToggleJetpackMode.class,
                id++,
                Side.SERVER
        );


        /* 选项2：如果你想保持原有处理器并添加新的消息类型
        INSTANCE.registerMessage(
                MessageToggleJetpackMode.Handler.class,
                MessageToggleJetpackMode.class,
                id++,
                Side.SERVER
        );

        // 机械核心专用模式切换消息
        INSTANCE.registerMessage(
                MessageToggleCoreMode.Handler.class,
                MessageToggleCoreMode.class,
                id++,
                Side.SERVER
        );
        */

        // 服务端 -> 客户端：NBT同步
        INSTANCE.registerMessage(
                MessageSyncJetpackTagToClient.Handler.class,
                MessageSyncJetpackTagToClient.class,
                id++,
                Side.CLIENT
        );

        // === 机械核心相关消息 ===

        // 服务端 -> 客户端：机械核心NBT同步（如果需要）
        /*
        INSTANCE.registerMessage(
                MessageSyncCoreToClient.Handler.class,
                MessageSyncCoreToClient.class,
                id++,
                Side.CLIENT
        );
        */

        // === 其他消息 ===

        // 村民转换器更新包
        INSTANCE.registerMessage(
                PacketUpdateVillagerTransformer.Handler.class,
                PacketUpdateVillagerTransformer.class,
                id++,
                Side.SERVER
        );
    }
}