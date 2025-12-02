package com.moremod.energy;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class JetpackEnergyCapability implements ICapabilitySerializable<NBTTagCompound> {

    private final EnergyStorageCustom storage;
    private final ItemStack stack;

    public JetpackEnergyCapability(ItemStack stack, int capacity, int maxReceive) {
        this.stack = stack;
        this.storage = new EnergyStorageCustom(capacity, maxReceive);
        readNBT();
    }

    /** 自定义 EnergyStorage 类（可写入能量） */
    public static class EnergyStorageCustom extends EnergyStorage {
        public EnergyStorageCustom(int capacity, int maxReceive) {
            super(capacity, maxReceive, 0);
        }

        public void setEnergy(int energy) {
            this.energy = Math.min(energy, capacity);
        }
    }

    public EnergyStorageCustom getStorage() {
        return storage;
    }

    /** 从 NBT 读取 */
    public void readNBT() {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("JetpackEnergy")) {
            storage.setEnergy(tag.getInteger("JetpackEnergy"));
        }
    }

    /** 写入 NBT */
    public void writeNBT() {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger("JetpackEnergy", storage.getEnergyStored());
    }

    // --- Forge Capabilities 接口实现 ---

    @Override
    public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, @Nullable net.minecraft.util.EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY;
    }

    @Nullable
    @Override
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.util.EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(storage);
        }
        return null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("JetpackEnergy", storage.getEnergyStored());
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        storage.setEnergy(nbt.getInteger("JetpackEnergy"));
    }
}
