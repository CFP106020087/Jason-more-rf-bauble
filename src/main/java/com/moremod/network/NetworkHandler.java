package com.moremod.network;
import com.moremod.network.PacketMechanicalCoreUpdate;
import com.moremod.network.PacketOpenGui;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class NetworkHandler {

    public static final String CHANNEL_NAME = "moremod";
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);

    @Deprecated
    public static final SimpleNetworkWrapper INSTANCE = CHANNEL;

    private static int ID = 0;
    private static int nextId() { return ID++; }

    public static void init() {
        System.out.println("[moremod] 初始化網路處理器...");

        // 原有封包
        CHANNEL.registerMessage(
                PacketMechanicalCoreUpdate.Handler.class,
                PacketMechanicalCoreUpdate.class,
                nextId(), Side.SERVER
        );

        CHANNEL.registerMessage(
                PacketOpenGui.Handler.class,
                PacketOpenGui.class,
                nextId(), Side.SERVER
        );

        CHANNEL.registerMessage(
                PacketUpgradeSelection.Handler.class,
                PacketUpgradeSelection.class,
                nextId(), Side.SERVER
        );

        // 🏪🏪🏪 新增：村民交易機數據包 🏪🏪🏪

        // 交易索引切換（左右箭頭）
        CHANNEL.registerMessage(
                MessageChangeTradeIndex.Handler.class,
                MessageChangeTradeIndex.class,
                nextId(), Side.SERVER
        );
        System.out.println("[moremod] ✅ 已註冊 MessageChangeTradeIndex（交易切換）");

        // 執行交易（按鈕）
        CHANNEL.registerMessage(
                MessageExecuteTrade.Handler.class,
                MessageExecuteTrade.class,
                nextId(), Side.SERVER
        );
        System.out.println("[moremod] ✅ 已註冊 MessageExecuteTrade（執行交易）");

        System.out.println("[moremod] 網路處理器初始化完成，已註冊 " + ID + " 個封包");
    }

    private NetworkHandler() {}
}