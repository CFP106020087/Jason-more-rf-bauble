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
 * 🏪 村民交易机 - 交易索引切换消息
 * 客户端 -> 服务端：通知切换到上一个/下一个交易
 */
public class MessageChangeTradeIndex implements IMessage {

    private BlockPos pos;
    private boolean next; // true=下一个, false=上一个

    // 无参构造器（必须）
    public MessageChangeTradeIndex() {}

    // 有参构造器
    public MessageChangeTradeIndex(BlockPos pos, boolean next) {
        this.pos = pos;
        this.next = next;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
        this.next = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeBoolean(next);
    }

    public static class Handler implements IMessageHandler<MessageChangeTradeIndex, IMessage> {
        @Override
        public IMessage onMessage(MessageChangeTradeIndex message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            // 必须在主线程执行
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = player.world.getTileEntity(message.pos);

                if (te instanceof TileTradingStation) {
                    TileTradingStation station = (TileTradingStation) te;

                    if (message.next) {
                        station.nextTrade();
                        System.out.println("[MessageChangeTradeIndex] 玩家 " + player.getName() + " 切换到下一个交易");
                    } else {
                        station.previousTrade();
                        System.out.println("[MessageChangeTradeIndex] 玩家 " + player.getName() + " 切换到上一个交易");
                    }
                } else {
                    System.err.println("[MessageChangeTradeIndex] ❌ TileEntity 不是 TradingStation 类型！");
                    if (te != null) {
                        System.err.println("[MessageChangeTradeIndex] 实际类型: " + te.getClass().getName());
                    }
                }
            });

            return null; // 不需要回复
        }
    }
}
