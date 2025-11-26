package com.moremod.network;

import com.moremod.eventHandler.EventHandlerJetpack;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

public class MessageJetpackSneaking implements IMessage {

    public boolean isSneaking;

    public MessageJetpackSneaking() {}

    public MessageJetpackSneaking(boolean isSneaking) {
        this.isSneaking = isSneaking;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.isSneaking = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.isSneaking);
    }

    public static class Handler implements IMessageHandler<MessageJetpackSneaking, IMessage> {
        @Override
        public IMessage onMessage(MessageJetpackSneaking message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            if (player == null || player.world == null) {
                System.err.println("[Jetpack] ERROR: Player or world is null!");
                return null;
            }

            UUID uuid = player.getUniqueID();
            boolean isSneaking = message.isSneaking;

            // 在主线程安全地更新状态
            player.getServerWorld().addScheduledTask(() -> {
                // 更新潜行状态
                EventHandlerJetpack.jetpackSneaking.put(uuid, isSneaking);

                // 不调用 updatePlayerFlightState 以避免干扰其他飞行源
                // 飞行状态会在 onPlayerTick 中自然更新
            });

            return null;
        }
    }
}