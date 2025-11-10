package com.moremod.network;

import com.moremod.tile.TileTradingStation;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 🏪 村民交易机 - 执行交易消息
 * 客户端 -> 服务端：通知执行当前选中的交易
 */
public class MessageExecuteTrade implements IMessage {

    private BlockPos pos;

    // 无参构造器（必须）
    public MessageExecuteTrade() {}

    // 有参构造器
    public MessageExecuteTrade(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
    }

    public static class Handler implements IMessageHandler<MessageExecuteTrade, IMessage> {
        @Override
        public IMessage onMessage(MessageExecuteTrade message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            // 必须在主线程执行
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = player.world.getTileEntity(message.pos);

                if (te instanceof TileTradingStation) {
                    TileTradingStation station = (TileTradingStation) te;
                    station.executeTrade();
                    System.out.println("[MessageExecuteTrade] 玩家 " + player.getName() + " 执行交易");
                } else {
                    System.err.println("[MessageExecuteTrade] ❌ TileEntity 不是 TradingStation 类型！");
                    if (te != null) {
                        System.err.println("[MessageExecuteTrade] 实际类型: " + te.getClass().getName());
                    }
                }
            });

            return null; // 不需要回复
        }
    }
}
