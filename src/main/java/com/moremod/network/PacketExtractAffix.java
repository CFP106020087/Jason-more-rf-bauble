package com.moremod.network;

import com.moremod.tile.TileEntityExtractionStation;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketExtractAffix implements IMessage {
    
    private BlockPos pos;
    private int affixIndex;
    
    public PacketExtractAffix() {}
    
    public PacketExtractAffix(BlockPos pos, int affixIndex) {
        this.pos = pos;
        this.affixIndex = affixIndex;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        affixIndex = buf.readInt();
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeInt(affixIndex);
    }
    
    public static class Handler implements IMessageHandler<PacketExtractAffix, IMessage> {
        @Override
        public IMessage onMessage(PacketExtractAffix message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = player.world.getTileEntity(message.pos);
                if (te instanceof TileEntityExtractionStation) {
                    TileEntityExtractionStation tile = (TileEntityExtractionStation) te;
                    tile.extractAffix(message.affixIndex, player);
                }
            });
            return null;
        }
    }
}