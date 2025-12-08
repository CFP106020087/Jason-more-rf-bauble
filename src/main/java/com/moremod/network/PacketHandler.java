package com.moremod.network;

import com.moremod.client.ClientHandlerPacketSyncRejectionData;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
    public static final SimpleNetworkWrapper INSTANCE =
            NetworkRegistry.INSTANCE.newSimpleChannel("moremod_channel");
    private static int id = 0;

    public static int nextId() {
        return id++;
    }
    public static void registerMessages() {
        int id = 0;

        // 🕰️ 时光之心数据同步包注册（S->C）
        INSTANCE.registerMessage(
                PacketSyncPlayerTime.Handler.class,
                PacketSyncPlayerTime.class,
                id++,
                Side.CLIENT
        );
        INSTANCE.registerMessage(
                PacketPurificationReroll.Handler.class,
                PacketPurificationReroll.class,
                id++,
                Side.SERVER
        );
        // === 喷气背包相关（C->S）===
        INSTANCE.registerMessage(
                MessageJetpackJumping.Handler.class,
                MessageJetpackJumping.class,
                id++,
                Side.SERVER
        );


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
        // 模式切换（C->S）
        INSTANCE.registerMessage(
                PacketActivateBoost.Handler.class,
                PacketActivateBoost.class,
                id++,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                ExtendedModeHandler.class,              // handler
                MessageToggleJetpackMode.class,         // message
                id++,
                Side.SERVER
        );
        // NBT 同步（S->C）
        INSTANCE.registerMessage(
                MessageSyncJetpackTagToClient.Handler.class,
                MessageSyncJetpackTagToClient.class,
                id++,
                Side.CLIENT
        );

        // === 其他（C->S）===
        INSTANCE.registerMessage(
                PacketUpdateVillagerTransformer.Handler.class,
                PacketUpdateVillagerTransformer.class,
                id++,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                PacketTransporterConfig.Handler.class,
                PacketTransporterConfig.class,
                id++,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                PacketTradingStationButton.Handler.class,
                PacketTradingStationButton.class,
                id++,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                PacketOpenVoidBackpack.Handler.class,
                PacketOpenVoidBackpack.class,
                id++,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                PacketCompassLeftClick.Handler.class,
                PacketCompassLeftClick.class,
                id++,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                PacketCompassRightClick.Handler.class,
                PacketCompassRightClick.class,
                id++,
                Side.SERVER
        );

        // === 先前漏註冊的兩個（C->S）===

        // 1) 書本創建（請統一用本 channel，不要在其他 channel 再註冊一次）
        INSTANCE.registerMessage(
                PacketCreateEnchantedBook.Handler.class,
                PacketCreateEnchantedBook.class,
                id++,
                Side.SERVER
        );

        // 2) 劍升級按鈕（造成 Undefined discriminator 的元兇）
        INSTANCE.registerMessage(
                PacketRemoveSingleGem.Handler.class,
                PacketRemoveSingleGem.class,
                id++,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                PacketStarUpgrade.Handler.class,
                PacketStarUpgrade.class,
                id++,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                PacketRemoveAllGems.Handler.class,
                PacketRemoveAllGems.class,
                id++,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                PacketExtractAffix.Handler.class,
                PacketExtractAffix.class,
                id++,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                PacketDecomposeGem.Handler.class,
                PacketDecomposeGem.class,
                id++,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                PacketPurifyGem.Handler.class,
                PacketPurifyGem.class,
                id++,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                PacketTransferGem.Handler.class,
                PacketTransferGem.class,
                id++,
                Side.SERVER
        );

            // 客户端 → 服务器：自动攻击触发
            INSTANCE.registerMessage(
                    MessageAutoAttackTrigger.Handler.class,
                    MessageAutoAttackTrigger.class,
                    id++,
                    Side.SERVER
            );

        // === 人性值系统数据同步 (S->C) ===
        INSTANCE.registerMessage(
                PacketSyncHumanityData.Handler.class,
                PacketSyncHumanityData.class,
                id++,
                Side.CLIENT
        );

        // === Synergy 链结站操作 (C->S) ===
        INSTANCE.registerMessage(
                PacketSynergyStationAction.Handler.class,
                PacketSynergyStationAction.class,
                id++,
                Side.SERVER
        );

        // === 破碎之神升格动画 (S->C) ===
        INSTANCE.registerMessage(
                PacketAscensionAnimation.Handler.class,
                PacketAscensionAnimation.class,
                id++,
                Side.CLIENT
        );

        // === 香巴拉宁静光环技能 (C->S) ===
        INSTANCE.registerMessage(
                PacketShambhalaPeaceAura.Handler.class,
                PacketShambhalaPeaceAura.class,
                id++,
                Side.SERVER
        );

        // === 假玩家激活器配置 (C->S) ===
        INSTANCE.registerMessage(
                PacketFakePlayerActivatorConfig.Handler.class,
                PacketFakePlayerActivatorConfig.class,
                id++,
                Side.SERVER
        );

        System.out.println("[MoreMod] 网络包注册完成，共 " + id + " 个消息类型");
    }
}
