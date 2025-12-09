package com.moremod.quarry.tile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 采石场代理方块（Actuator）
 * 用于传递能量和红石信号到核心
 */
public class TileQuarryActuator extends TileEntity {
    
    private EnumFacing facing = EnumFacing.DOWN;
    private boolean powered = false;
    private boolean needsRedstoneUpdate = true;
    
    public TileQuarryActuator() {
    }
    
    public TileQuarryActuator(EnumFacing facing) {
        this.facing = facing;
    }
    
    // ==================== 核心方法 ====================
    
    /**
     * 获取指向的核心方块
     */
    @Nullable
    public TileQuantumQuarry getCore() {
        if (world == null || pos == null) return null;
        
        BlockPos corePos = pos.offset(facing);
        TileEntity te = world.getTileEntity(corePos);
        
        if (te instanceof TileQuantumQuarry) {
            return (TileQuantumQuarry) te;
        }
        return null;
    }
    
    /**
     * 获取朝向（指向核心的方向）
     */
    public EnumFacing getFacing() {
        return facing;
    }
    
    /**
     * 设置朝向
     */
    public void setFacing(EnumFacing facing) {
        this.facing = facing;
        markDirty();
    }
    
    /**
     * 检查红石状态
     */
    public boolean isPowered() {
        if (needsRedstoneUpdate) {
            needsRedstoneUpdate = false;
            powered = world.isBlockPowered(pos);
        }
        return powered;
    }
    
    /**
     * 邻居方块变化时调用
     */
    public void onNeighborChanged() {
        needsRedstoneUpdate = true;
        
        // 通知核心更新红石状态
        TileQuantumQuarry core = getCore();
        if (core != null) {
            core.updateRedstoneState();
        }
    }
    
    // ==================== NBT ====================
    
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Facing", facing.getIndex());
        return compound;
    }
    
    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        facing = EnumFacing.byIndex(compound.getInteger("Facing"));
    }
    
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setInteger("Facing", facing.getIndex());
        return tag;
    }
    
    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        facing = EnumFacing.byIndex(tag.getInteger("Facing"));
    }
    
    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }
    
    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }
    
    // ==================== Capability 代理 ====================
    
    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing side) {
        // 代理能量和物品传输到核心
        if (capability == CapabilityEnergy.ENERGY || 
            capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            TileQuantumQuarry core = getCore();
            return core != null && core.hasCapability(capability, facing.getOpposite());
        }
        return super.hasCapability(capability, side);
    }
    
    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing side) {
        if (capability == CapabilityEnergy.ENERGY) {
            TileQuantumQuarry core = getCore();
            if (core != null) {
                return core.getCapability(capability, facing.getOpposite());
            }
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            TileQuantumQuarry core = getCore();
            if (core != null) {
                return core.getCapability(capability, facing.getOpposite());
            }
        }
        return super.getCapability(capability, side);
    }
}
