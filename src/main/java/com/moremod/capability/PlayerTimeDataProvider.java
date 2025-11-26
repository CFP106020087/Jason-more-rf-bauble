package com.moremod.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlayerTimeDataProvider implements ICapabilitySerializable<NBTTagCompound> {

    @CapabilityInject(IPlayerTimeData.class)
    public static final Capability<IPlayerTimeData> PLAYER_TIME_CAP = null;

    private IPlayerTimeData instance = new PlayerTimeDataImpl();

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == PLAYER_TIME_CAP;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        return capability == PLAYER_TIME_CAP ? (T) instance : null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return (NBTTagCompound) PLAYER_TIME_CAP.getStorage().writeNBT(PLAYER_TIME_CAP, instance, null);
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        PLAYER_TIME_CAP.getStorage().readNBT(PLAYER_TIME_CAP, instance, null, nbt);
    }
}