package com.moremod.system.humanity;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

/**
 * 人性值数据存储
 * Humanity Data Storage
 */
public class HumanityDataStorage implements Capability.IStorage<IHumanityData> {

    @Nullable
    @Override
    public NBTBase writeNBT(Capability<IHumanityData> capability, IHumanityData instance, EnumFacing side) {
        return instance.serializeNBT();
    }

    @Override
    public void readNBT(Capability<IHumanityData> capability, IHumanityData instance, EnumFacing side, NBTBase nbt) {
        if (nbt instanceof NBTTagCompound) {
            instance.deserializeNBT((NBTTagCompound) nbt);
        }
    }
}
