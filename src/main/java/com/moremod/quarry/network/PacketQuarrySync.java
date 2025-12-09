package com.moremod.quarry.network;

import com.moremod.quarry.QuarryMode;
import com.moremod.quarry.tile.TileQuantumQuarry;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 服务器 -> 客户端 数据同步包
 */
public class PacketQuarrySync implements IMessage {
    
    private BlockPos pos;
    private int energy;
    private int mode;
    private int biomeId;
    private int progress;
    private boolean structureValid;
    private boolean redstoneControl;
    
    public PacketQuarrySync() {
    }
    
    public PacketQuarrySync(TileQuantumQuarry tile) {
        this.pos = tile.getPos();
        this.energy = tile.getEnergyStored();
        this.mode = tile.getMode().getMeta();
        this.biomeId = tile.getSelectedBiome() != null ? 
            net.minecraft.world.biome.Biome.REGISTRY.getIDForObject(tile.getSelectedBiome()) : -1;
        this.progress = tile.getProgress();
        this.structureValid = tile.isStructureValid();
        this.redstoneControl = tile.isRedstoneControlEnabled();
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        energy = buf.readInt();
        mode = buf.readInt();
        biomeId = buf.readInt();
        progress = buf.readInt();
        structureValid = buf.readBoolean();
        redstoneControl = buf.readBoolean();
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeInt(energy);
        buf.writeInt(mode);
        buf.writeInt(biomeId);
        buf.writeInt(progress);
        buf.writeBoolean(structureValid);
        buf.writeBoolean(redstoneControl);
    }
    
    public static class Handler implements IMessageHandler<PacketQuarrySync, IMessage> {
        
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketQuarrySync message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                TileEntity te = Minecraft.getMinecraft().world.getTileEntity(message.pos);
                
                if (te instanceof TileQuantumQuarry) {
                    TileQuantumQuarry quarry = (TileQuantumQuarry) te;
                    quarry.setClientEnergy(message.energy);
                    quarry.setMode(QuarryMode.fromMeta(message.mode));
                    quarry.setSelectedBiomeById(message.biomeId);
                    quarry.setClientProgress(message.progress);
                    quarry.setClientStructureValid(message.structureValid);
                    quarry.setRedstoneControlEnabled(message.redstoneControl);
                }
            });
            return null;
        }
    }
}
