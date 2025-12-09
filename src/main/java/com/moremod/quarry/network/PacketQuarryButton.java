package com.moremod.quarry.network;

import com.moremod.quarry.tile.TileQuantumQuarry;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 按钮点击网络包
 */
public class PacketQuarryButton implements IMessage {
    
    private BlockPos pos;
    private int buttonId;
    
    public PacketQuarryButton() {
    }
    
    public PacketQuarryButton(BlockPos pos, int buttonId) {
        this.pos = pos;
        this.buttonId = buttonId;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        buttonId = buf.readInt();
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeInt(buttonId);
    }
    
    public static class Handler implements IMessageHandler<PacketQuarryButton, IMessage> {
        
        @Override
        public IMessage onMessage(PacketQuarryButton message, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                World world = ctx.getServerHandler().player.world;
                TileEntity te = world.getTileEntity(message.pos);
                
                if (te instanceof TileQuantumQuarry) {
                    TileQuantumQuarry quarry = (TileQuantumQuarry) te;
                    
                    switch (message.buttonId) {
                        case 0:
                            quarry.cycleMode();
                            break;
                        case 1:
                            quarry.toggleRedstoneControl();
                            break;
                    }
                }
            });
            return null;
        }
    }
}
