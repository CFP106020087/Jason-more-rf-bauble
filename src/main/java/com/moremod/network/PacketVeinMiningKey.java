package com.moremod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 范围挖掘按键同步包
 * 客户端 -> 服务端：同步玩家是否按住范围挖掘触发键（~键）
 */
public class PacketVeinMiningKey implements IMessage {

    // 服务端追踪哪些玩家正在按住按键
    private static final Set<UUID> playersHoldingKey = new HashSet<>();

    public boolean isHolding;

    public PacketVeinMiningKey() {}

    public PacketVeinMiningKey(boolean isHolding) {
        this.isHolding = isHolding;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.isHolding = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.isHolding);
    }

    /**
     * 检查玩家是否正在按住范围挖掘触发键
     */
    public static boolean isPlayerHoldingKey(UUID playerId) {
        return playersHoldingKey.contains(playerId);
    }

    /**
     * 清除玩家的按键状态（玩家退出时调用）
     */
    public static void clearPlayerState(UUID playerId) {
        playersHoldingKey.remove(playerId);
    }

    public static class Handler implements IMessageHandler<PacketVeinMiningKey, IMessage> {
        @Override
        public IMessage onMessage(PacketVeinMiningKey message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            if (player == null || player.world == null) {
                return null;
            }

            UUID uuid = player.getUniqueID();

            // 在主线程安全地更新状态
            player.getServerWorld().addScheduledTask(() -> {
                if (message.isHolding) {
                    playersHoldingKey.add(uuid);
                } else {
                    playersHoldingKey.remove(uuid);
                }
            });

            return null;
        }
    }
}
