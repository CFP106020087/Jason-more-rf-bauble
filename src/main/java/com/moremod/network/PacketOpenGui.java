package com.moremod.network;

import com.moremod.moremod;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * GUI开启数据包
 * 用于客户端请求打开GUI
 */
public class PacketOpenGui implements IMessage {

    private int guiId;

    // 默认构造函数
    public PacketOpenGui() {
        this.guiId = 0;
    }

    public PacketOpenGui(int guiId) {
        this.guiId = guiId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.guiId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.guiId);
    }

    /**
     * 服务器端处理器
     */
    public static class Handler implements IMessageHandler<PacketOpenGui, IMessage> {

        @Override
        public IMessage onMessage(PacketOpenGui message, MessageContext ctx) {
            if (ctx.getServerHandler() != null && ctx.getServerHandler().player != null) {
                EntityPlayerMP player = ctx.getServerHandler().player;
                WorldServer worldServer = (WorldServer) player.world;

                worldServer.addScheduledTask(() -> {
                    // 在服务器端打开GUI
                    player.openGui(moremod.INSTANCE, message.guiId, player.world, 0, 0, 0);
                });
            }
            return null;
        }
    }
}