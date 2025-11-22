package com.moremod.capability.framework;

import com.moremod.api.capability.ICapabilityContainer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Forge Capability 提供者
 * 将我们的能力容器桥接到 Forge 的 Capability 系统
 */
public class CapabilityContainerProvider<T> implements ICapabilitySerializable<NBTTagCompound> {

    @CapabilityInject(ICapabilityContainer.class)
    public static final Capability<ICapabilityContainer<?>> CAPABILITY = null;

    private final ICapabilityContainer<T> container;

    public CapabilityContainerProvider(ICapabilityContainer<T> container) {
        this.container = container;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CAPABILITY;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <C> C getCapability(@Nonnull Capability<C> capability, @Nullable EnumFacing facing) {
        return capability == CAPABILITY ? (C) container : null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        container.serializeNBT(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        container.deserializeNBT(nbt);
    }

    /**
     * Capability 存储（Forge 要求）
     */
    public static class Storage implements Capability.IStorage<ICapabilityContainer<?>> {

        @Nullable
        @Override
        public NBTBase writeNBT(Capability<ICapabilityContainer<?>> capability,
                               ICapabilityContainer<?> instance,
                               EnumFacing side) {
            NBTTagCompound nbt = new NBTTagCompound();
            instance.serializeNBT(nbt);
            return nbt;
        }

        @Override
        public void readNBT(Capability<ICapabilityContainer<?>> capability,
                           ICapabilityContainer<?> instance,
                           EnumFacing side,
                           NBTBase nbt) {
            if (nbt instanceof NBTTagCompound) {
                instance.deserializeNBT((NBTTagCompound) nbt);
            }
        }
    }
}
