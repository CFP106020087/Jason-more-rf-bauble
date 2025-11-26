package com.moremod.network;

import com.moremod.item.ItemExplorerCompass;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 罗盘左键点击数据包
 * 客户端发送到服务端，触发粒子射线效果
 */
public class PacketCompassLeftClick implements IMessage {

    public PacketCompassLeftClick() {
        // 空构造函数，网络需要
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // 这个包不需要传输数据
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 这个包不需要传输数据
    }

    /**
     * 服务端处理器
     */
    public static class Handler implements IMessageHandler<PacketCompassLeftClick, IMessage> {
        @Override
        public IMessage onMessage(PacketCompassLeftClick message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            // 在主线程执行
            player.getServerWorld().addScheduledTask(() -> {
                ItemExplorerCompass.handleLeftClick(player);
            });

            return null;
        }
    }
}