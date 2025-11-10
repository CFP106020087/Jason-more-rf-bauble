// ============================================
// PacketExecuteTrade.java - 修复版
// 位置: com/moremod/network/PacketExecuteTrade.java
// ============================================
package com.moremod.network;

import com.moremod.container.ContainerTradingStation;
import com.moremod.tile.TileTradingStation;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 🏪 村民交易机 - 执行交易数据包
 * 客户端 -> 服务端：执行当前交易
 */
public class PacketExecuteTrade implements IMessage {

    public PacketExecuteTrade() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketExecuteTrade, IMessage> {

        @Override
        public IMessage onMessage(PacketExecuteTrade message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                // ✅ 修改：使用 ContainerTradingStation 和 TileTradingStation
                if (player.openContainer instanceof ContainerTradingStation) {
                    TileTradingStation te =
                            ((ContainerTradingStation) player.openContainer).getTile();

                    if (te != null) {
                        te.executeTrade();
                        System.out.println("[PacketExecuteTrade] 玩家 " + player.getName() + " 执行交易");
                    } else {
                        System.err.println("[PacketExecuteTrade] ❌ TileEntity 为 null");
                    }
                }
            });
            return null;
        }
    }
}

