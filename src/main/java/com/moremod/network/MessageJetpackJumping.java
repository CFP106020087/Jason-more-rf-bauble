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
            UUID uuid = player.getUniqueID();

            // 主线程执行
            player.getServerWorld().addScheduledTask(() -> {
                EventHandlerJetpack.jetpackJumping.put(uuid, message.isJumping);
                // Debug
                //System.out.println("服务端收到跳跃包: " + message.isJumping);
            });

            return null;
        }
    }
}
