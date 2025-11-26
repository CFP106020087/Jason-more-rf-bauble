package com.moremod.network;

import com.moremod.item.ItemDimensionalRipper;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 维度撕裂者按键数据包
 * 客户端按键时发送到服务器
 */
public class PacketDimensionalRipperKey implements IMessage {

    public PacketDimensionalRipperKey() {}

    @Override
    public void fromBytes(ByteBuf buf) {
        // 无需传递额外数据
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 无需传递额外数据
    }

    public static class Handler implements IMessageHandler<PacketDimensionalRipperKey, IMessage> {
        @Override
        public IMessage onMessage(PacketDimensionalRipperKey message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                // 调用ItemDimensionalRipper的处理方法
                ItemDimensionalRipper.handleKeyPress(player);
            });
            return null;
        }
    }
}