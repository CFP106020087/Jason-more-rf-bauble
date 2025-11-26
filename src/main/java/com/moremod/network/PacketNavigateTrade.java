package com.moremod.network;

import com.moremod.container.ContainerTradingStation;
import com.moremod.tile.TileTradingStation;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 🏪 村民交易机 - 切换交易数据包
 * 客户端 -> 服务端
 */
public class PacketNavigateTrade implements IMessage {

    private boolean next;

    public PacketNavigateTrade() {}

    public PacketNavigateTrade(boolean next) {
        this.next = next;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        next = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(next);
    }

    public static class Handler implements IMessageHandler<PacketNavigateTrade, IMessage> {

        @Override
        public IMessage onMessage(PacketNavigateTrade message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                // ✅ 修改：使用 ContainerTradingStation
                if (player.openContainer instanceof ContainerTradingStation) {
                    TileTradingStation te =
                            ((ContainerTradingStation) player.openContainer).getTile();

                    if (te != null) {
                        if (message.next) {
                            te.nextTrade();
                            System.out.println("[PacketNavigateTrade] 切换到下一个交易");
                        } else {
                            te.previousTrade();
                            System.out.println("[PacketNavigateTrade] 切换到上一个交易");
                        }
                    }
                }
            });
            return null;
        }
    }
}
