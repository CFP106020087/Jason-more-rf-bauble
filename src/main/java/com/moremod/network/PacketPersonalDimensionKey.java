package com.moremod.network;

import com.moremod.item.ItemDimensionalRipper;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 私人维度按键数据包
 * 客户端按U键时发送到服务器
 */
public class PacketPersonalDimensionKey implements IMessage {

    public PacketPersonalDimensionKey() {}

    @Override
    public void fromBytes(ByteBuf buf) {
        // 无需传递额外数据
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 无需传递额外数据
    }

    public static class Handler implements IMessageHandler<PacketPersonalDimensionKey, IMessage> {
        @Override
        public IMessage onMessage(PacketPersonalDimensionKey message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                // 调用ItemDimensionalRipper的私人维度处理方法
                ItemDimensionalRipper.handlePersonalDimensionKey(player);
            });
            return null;
        }
    }
}