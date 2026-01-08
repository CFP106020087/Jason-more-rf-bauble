package com.adversity.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 玩家难度 Capability 提供器
 */
public class PlayerDifficultyProvider implements ICapabilitySerializable<NBTTagCompound> {

    private final IPlayerDifficulty capability;

    public PlayerDifficultyProvider() {
        this.capability = new PlayerDifficulty();
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> cap, @Nullable EnumFacing facing) {
        return cap == CapabilityHandler.PLAYER_DIFFICULTY_CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> cap, @Nullable EnumFacing facing) {
        if (cap == CapabilityHandler.PLAYER_DIFFICULTY_CAPABILITY) {
            return CapabilityHandler.PLAYER_DIFFICULTY_CAPABILITY.cast(capability);
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
