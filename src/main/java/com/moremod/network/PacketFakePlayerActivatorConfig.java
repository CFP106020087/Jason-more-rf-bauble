package com.moremod.network;

import com.moremod.tile.TileEntityFakePlayerActivator;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 假玩家激活器配置网络包
 */
public class PacketFakePlayerActivatorConfig implements IMessage {

    public enum Action {
        CYCLE_MODE,
        SET_MODE,
        ADJUST_INTERVAL,
        SET_INTERVAL,
        TOGGLE_CHUNK_LOAD
    }

    private BlockPos pos;
    private Action action;
    private int value;

    public PacketFakePlayerActivatorConfig() {}

    public PacketFakePlayerActivatorConfig(BlockPos pos, Action action, int value) {
        this.pos = pos;
        this.action = action;
        this.value = value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        action = Action.values()[buf.readByte()];
        value = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeByte(action.ordinal());
        buf.writeInt(value);
    }

    public static class Handler implements IMessageHandler<PacketFakePlayerActivatorConfig, IMessage> {
        @Override
        public IMessage onMessage(PacketFakePlayerActivatorConfig message, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                World world = ctx.getServerHandler().player.world;
                TileEntity te = world.getTileEntity(message.pos);

                if (te instanceof TileEntityFakePlayerActivator) {
                    TileEntityFakePlayerActivator tile = (TileEntityFakePlayerActivator) te;

                    switch (message.action) {
                        case CYCLE_MODE:
                            tile.cycleMode();
                            break;
                        case SET_MODE:
                            tile.setMode(message.value);
                            break;
                        case ADJUST_INTERVAL:
                            tile.adjustInterval(message.value);
                            break;
                        case SET_INTERVAL:
                            tile.setActionInterval(message.value);
                            break;
                        case TOGGLE_CHUNK_LOAD:
                            tile.setChunkLoading(!tile.isChunkLoadingEnabled());
                            break;
                    }
                }
            });
            return null;
        }
    }
}
