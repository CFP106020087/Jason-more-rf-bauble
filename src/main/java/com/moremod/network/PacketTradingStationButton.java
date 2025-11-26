

// ============================================
// 2. PacketTradingStationButton.java - 简化版本
// 位置: com/moremod/network/PacketTradingStationButton.java
// ============================================
package com.moremod.network;

import com.moremod.tile.TileTradingStation;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 🏪 自动交易机按钮操作（Client -> Server）
 */
public class PacketTradingStationButton implements IMessage {

    private BlockPos pos;
    private Action action;

    public enum Action {
        TOGGLE_MODE,   // 切换自动/手动模式
        PREV_TRADE,    // 上一个交易
        NEXT_TRADE     // 下一个交易
    }

    public PacketTradingStationButton() {}

    public PacketTradingStationButton(BlockPos pos, Action action) {
        this.pos = pos;
        this.action = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
        this.action = Action.values()[buf.readByte() & 0xFF];
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeByte(action.ordinal());
    }

    public static class Handler implements IMessageHandler<PacketTradingStationButton, IMessage> {

        @Override
        public IMessage onMessage(PacketTradingStationButton msg, MessageContext ctx) {
            final WorldServer world = ctx.getServerHandler().player.getServerWorld();

            world.addScheduledTask(() -> {
                // 检查方块是否加载
                if (!world.isBlockLoaded(msg.pos)) {
                    System.err.println("[PacketTradingStationButton] 方块未加载: " + msg.pos);
                    return;
                }

                // 获取 TileEntity
                TileEntity te = world.getTileEntity(msg.pos);
                if (!(te instanceof TileTradingStation)) {
                    System.err.println("[PacketTradingStationButton] TileEntity 不是 TileTradingStation");
                    return;
                }

                TileTradingStation tile = (TileTradingStation) te;

                // 执行操作
                switch (msg.action) {
                    case TOGGLE_MODE:
                        // 切换自动模式（如果你的 TileTradingStation 有这个方法）
                        // tile.toggleAutoMode();
                        System.out.println("[PacketTradingStationButton] 切换模式");
                        break;

                    case PREV_TRADE:
                        tile.previousTrade();
                        System.out.println("[PacketTradingStationButton] 上一个交易");
                        break;

                    case NEXT_TRADE:
                        tile.nextTrade();
                        System.out.println("[PacketTradingStationButton] 下一个交易");
                        break;
                }
            });

            return null;
        }
    }
}