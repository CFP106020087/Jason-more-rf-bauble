package com.moremod.network;

import com.moremod.eventHandler.EventHandlerJetpack;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

public class MessageJetpackJumping implements IMessage {

    public boolean isJumping;

    public MessageJetpackJumping() {}

    public MessageJetpackJumping(boolean isJumping) {
        this.isJumping = isJumping;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.isJumping = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.isJumping);
    }

    public static class Handler implements IMessageHandler<MessageJetpackJumping, IMessage> {
        @Override
        public IMessage onMessage(MessageJetpackJumping message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            if (player == null || player.world == null) {
                System.err.println("[Jetpack] ERROR: Player or world is null!");
                return null;
            }

            UUID uuid = player.getUniqueID();
            boolean isJumping = message.isJumping;

            // 在主线程安全地更新状态
            player.getServerWorld().addScheduledTask(() -> {
                // 更新跳跃状态
                EventHandlerJetpack.jetpackJumping.put(uuid, isJumping);

                // 不调用 updatePlayerFlightState 以避免干扰其他飞行源
                // 飞行状态会在 onPlayerTick 中自然更新
            });

            return null;
        }
    }
}