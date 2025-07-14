// TileEntityEnergyHellBookshelf.java
package com.moremod.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;

import javax.annotation.Nullable;

public class TileEntityEnergyHellBookshelf extends TileEntity implements ITickable {

    private final EnergyStorage energy = new EnergyStorage(100000, 500, 0);
    private int cachedLevel = 0;

    @Override
    public void update() {
        if (world.isRemote) return;

        int newLevel = energy.getEnergyStored() * 15 / energy.getMaxEnergyStored();
        if (newLevel != cachedLevel) {
            cachedLevel = newLevel;
            IBlockState state = world.getBlockState(pos);
            world.setBlockState(pos, state.withProperty(com.moremod.block.BlockEnergyHellBookshelf.LEVEL, newLevel), 2);
        }
    }

    public float getCurrentPower() {
        return 2.0F + cachedLevel * 0.2F;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(energy);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("energy", energy.getEnergyStored());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        energy.receiveEnergy(compound.getInteger("energy"), false);
    }
}
