package com.moremod.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

public class PlayerTimeDataStorage implements Capability.IStorage<IPlayerTimeData> {

    @Override
    public NBTBase writeNBT(Capability<IPlayerTimeData> capability, IPlayerTimeData instance, EnumFacing side) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("totalDays", instance.getTotalDaysPlayed());
        nbt.setLong("lastLogin", instance.getLastLoginTime());
        nbt.setLong("totalPlayTime", instance.getTotalPlayTime());
        nbt.setBoolean("hasEquippedTemporalHeart", instance.hasEquippedTemporalHeart());
        nbt.setBoolean("firstTimeEquip", instance.isFirstTimeEquip());  // 保存第一次装备标记
        return nbt;
    }

    @Override
    public void readNBT(Capability<IPlayerTimeData> capability, IPlayerTimeData instance, EnumFacing side, NBTBase nbt) {
        if (nbt instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) nbt;
            instance.setTotalDaysPlayed(compound.getInteger("totalDays"));
            instance.setLastLoginTime(compound.getLong("lastLogin"));
            instance.setTotalPlayTime(compound.getLong("totalPlayTime"));
            instance.setHasEquippedTemporalHeart(compound.getBoolean("hasEquippedTemporalHeart"));

            // 读取第一次装备标记，如果不存在默认为true
            if (compound.hasKey("firstTimeEquip")) {
                instance.setFirstTimeEquip(compound.getBoolean("firstTimeEquip"));
            } else {
                instance.setFirstTimeEquip(true);  // 兼容旧数据
            }
        }
    }
}