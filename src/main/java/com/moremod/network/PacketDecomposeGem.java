package com.moremod.network;

import com.moremod.tile.TileEntityExtractionStation;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketDecomposeGem implements IMessage {
    
    private BlockPos pos;
    
    public PacketDecomposeGem() {}
    
    public PacketDecomposeGem(BlockPos pos) {
        this.pos = pos;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
    }
    
    public static class Handler implements IMessageHandler<PacketDecomposeGem, IMessage> {
        @Override
        public IMessage onMessage(PacketDecomposeGem message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = player.world.getTileEntity(message.pos);
                if (te instanceof TileEntityExtractionStation) {
                    TileEntityExtractionStation tile = (TileEntityExtractionStation) te;
                    tile.decomposeGem(player);
                }
            });
            return null;
        }
    }
}