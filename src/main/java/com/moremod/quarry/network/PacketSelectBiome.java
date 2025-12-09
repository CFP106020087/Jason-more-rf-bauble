package com.moremod.quarry.network;

import com.moremod.quarry.tile.TileQuantumQuarry;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 选择生物群系网络包
 */
public class PacketSelectBiome implements IMessage {
    
    private BlockPos pos;
    private int biomeId;
    
    public PacketSelectBiome() {
    }
    
    public PacketSelectBiome(BlockPos pos, int biomeId) {
        this.pos = pos;
        this.biomeId = biomeId;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        biomeId = buf.readInt();
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeInt(biomeId);
    }
    
    public static class Handler implements IMessageHandler<PacketSelectBiome, IMessage> {
        
        @Override
        public IMessage onMessage(PacketSelectBiome message, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                World world = ctx.getServerHandler().player.world;
                TileEntity te = world.getTileEntity(message.pos);
                
                if (te instanceof TileQuantumQuarry) {
                    TileQuantumQuarry quarry = (TileQuantumQuarry) te;
                    Biome biome = Biome.REGISTRY.getObjectById(message.biomeId);
                    quarry.setSelectedBiome(biome);
                }
            });
            return null;
        }
    }
}
