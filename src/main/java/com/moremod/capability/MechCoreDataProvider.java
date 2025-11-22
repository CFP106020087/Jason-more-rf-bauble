package com.moremod.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Mechanical Core 数据提供器
 *
 * 将 Capability 附加到玩家实体
 */
public class MechCoreDataProvider implements ICapabilitySerializable<NBTBase> {

    private final IMechCoreData instance = new MechCoreDataImpl();

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == IMechCoreData.CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == IMechCoreData.CAPABILITY) {
            return IMechCoreData.CAPABILITY.cast(instance);
        }
        return null;
    }

    @Override
    public NBTBase serializeNBT() {
        return IMechCoreData.CAPABILITY.getStorage().writeNBT(
            IMechCoreData.CAPABILITY,
            instance,
            null
        );
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        IMechCoreData.CAPABILITY.getStorage().readNBT(
            IMechCoreData.CAPABILITY,
            instance,
            null,
            nbt
        );
    }
}
