package com.moremod.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class TileEntityInfiniteEnergy extends TileEntity implements ITickable {

    private static final int MAX_OUTPUT = 1000;

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        for (EnumFacing side : EnumFacing.values()) {
            BlockPos neighborPos = pos.offset(side);
            TileEntity neighbor = world.getTileEntity(neighborPos);
            if (neighbor == null) continue;

            if (neighbor.hasCapability(CapabilityEnergy.ENERGY, side.getOpposite())) {
                IEnergyStorage storage = neighbor.getCapability(CapabilityEnergy.ENERGY, side.getOpposite());
                if (storage != null && storage.canReceive()) {
                    storage.receiveEnergy(MAX_OUTPUT, false);
                }
            }
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(CREATIVE_ENERGY_STORAGE);
        }
        return super.getCapability(capability, facing);
    }

    private final IEnergyStorage CREATIVE_ENERGY_STORAGE = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return maxExtract; }
        @Override public int getEnergyStored() { return Integer.MAX_VALUE; }
        @Override public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };
}
