package com.moremod.network;

import com.moremod.synergy.network.PacketToggleSynergy;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import static com.dhanantry.scapeandrunparasites.util.handlers.SRPPacketHandler.nextID;
import static crafttweaker.mc1120.CraftTweaker.NETWORK;

public class PacketHandler {
    public static final SimpleNetworkWrapper INSTANCE =
            NetworkRegistry.INSTANCE.newSimpleChannel("moremod_channel");

    public static void registerMessages() {
        int id = 0;

        // 🕰️ 时光之心数据同步包注册（S->C）
        INSTANCE.registerMessage(
                PacketSyncPlayerTime.Handler.class,
                PacketSyncPlayerTime.class,
                id++,
                Side.CLIENT
        );

        // === 喷气背包相关（C->S）===
        INSTANCE.registerMessage(
                MessageJetpackJumping.Handler.class,
                MessageJetpackJumping.class,
                id++,
                Side.SERVER
        );
        INSTANCE.registerMessage(
                PacketSyncRejectionData.Handler.class,
                PacketSyncRejectionData.class,
                id++,
                Side.CLIENT
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

        // 🔗 Synergy系统：切换Synergy激活状态
        INSTANCE.registerMessage(
                PacketToggleSynergy.Handler.class,
                PacketToggleSynergy.class,
                id++,
                Side.SERVER
        );

        System.out.println("[MoreMod] 网络包注册完成，共 " + id + " 个消息类型");
    }
}
