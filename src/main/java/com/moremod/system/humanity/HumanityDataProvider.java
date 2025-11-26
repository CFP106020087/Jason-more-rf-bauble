package com.moremod.system.humanity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 人性值数据提供者
 * Humanity Data Provider
 */
public class HumanityDataProvider implements ICapabilitySerializable<NBTTagCompound> {

    @CapabilityInject(IHumanityData.class)
    public static final Capability<IHumanityData> HUMANITY_CAP = null;

    private IHumanityData instance = new HumanityDataImpl();

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == HUMANITY_CAP;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        return capability == HUMANITY_CAP ? (T) instance : null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return (NBTTagCompound) HUMANITY_CAP.getStorage().writeNBT(HUMANITY_CAP, instance, null);
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        HUMANITY_CAP.getStorage().readNBT(HUMANITY_CAP, instance, null, nbt);
    }
}
