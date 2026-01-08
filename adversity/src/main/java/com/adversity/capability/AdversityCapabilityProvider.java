package com.adversity.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Capability 提供器 - 附加到实体上
 */
public class AdversityCapabilityProvider implements ICapabilitySerializable<NBTTagCompound> {

    private final IAdversityCapability capability;

    public AdversityCapabilityProvider() {
        this.capability = new AdversityCapability();
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> cap, @Nullable EnumFacing facing) {
        return cap == CapabilityHandler.ADVERSITY_CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> cap, @Nullable EnumFacing facing) {
        if (cap == CapabilityHandler.ADVERSITY_CAPABILITY) {
            return CapabilityHandler.ADVERSITY_CAPABILITY.cast(capability);
        }
        return null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return capability.serializeNBT();
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        capability.deserializeNBT(nbt);
    }
}
