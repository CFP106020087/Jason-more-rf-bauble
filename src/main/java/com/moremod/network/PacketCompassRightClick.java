package com.moremod.network;

import com.moremod.item.ItemExplorerCompass;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketCompassRightClick implements IMessage {

    public PacketCompassRightClick() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketCompassRightClick, IMessage> {
        @Override
        public IMessage onMessage(PacketCompassRightClick message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                ItemExplorerCompass.handleRightClick(player);
            });
            return null;
        }
    }
}