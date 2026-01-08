package com.adversity.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

/**
 * Capability 存储处理器
 */
public class AdversityCapabilityStorage implements Capability.IStorage<IAdversityCapability> {

    @Nullable
    @Override
    public NBTBase writeNBT(Capability<IAdversityCapability> capability, IAdversityCapability instance, EnumFacing side) {
        return instance.serializeNBT();
    }

    @Override
    public void readNBT(Capability<IAdversityCapability> capability, IAdversityCapability instance, EnumFacing side, NBTBase nbt) {
        if (nbt instanceof NBTTagCompound) {
            instance.deserializeNBT((NBTTagCompound) nbt);
        }
    }
}
