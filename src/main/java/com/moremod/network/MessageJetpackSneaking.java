package com.moremod.network;

import com.moremod.eventHandler.EventHandlerJetpack;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

public class MessageJetpackSneaking implements IMessage {
    public boolean sneaking;

    public MessageJetpackSneaking() {
        // 默认构造函数，用于网络序列化
    }

    public MessageJetpackSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.sneaking = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.sneaking);
    }

    public static class Handler implements IMessageHandler<MessageJetpackSneaking, IMessage> {
        @Override
        public IMessage onMessage(MessageJetpackSneaking message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            UUID uuid = player.getUniqueID();

            // 主线程执行 - 与MessageJetpackJumping保持一致
            player.getServerWorld().addScheduledTask(() -> {
                EventHandlerJetpack.jetpackSneaking.put(uuid, message.sneaking);
                // Debug
                //System.out.println("服务端收到Shift键包: " + message.sneaking);
            });

            return null;
        }
    }
}