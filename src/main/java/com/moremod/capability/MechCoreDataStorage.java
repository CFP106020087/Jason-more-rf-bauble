package com.moremod.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;

/**
 * Mechanical Core 数据存储
 *
 * 处理 Capability 的 NBT 序列化和反序列化
 */
public class MechCoreDataStorage implements IStorage<IMechCoreData> {

    @Override
    public NBTBase writeNBT(Capability<IMechCoreData> capability, IMechCoreData instance, EnumFacing side) {
        return instance.serializeNBT();
    }

    @Override
    public void readNBT(Capability<IMechCoreData> capability, IMechCoreData instance, EnumFacing side, NBTBase nbt) {
        if (nbt instanceof NBTTagCompound) {
            instance.deserializeNBT((NBTTagCompound) nbt);
        }
    }
}
