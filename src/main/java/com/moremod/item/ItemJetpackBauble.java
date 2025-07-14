// ItemJetpackBauble.java
package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemJetpackBauble extends Item implements IBauble {

    private final String name;
    private final int maxEnergy;
    private final int energyPerTick;
    private final double ascendSpeed;
    private final double descendSpeed;
    private final double moveSpeed;

    public ItemJetpackBauble(String name, int maxEnergy, int energyPerTick,
                             double ascendSpeed, double descendSpeed, double moveSpeed) {
        this.name = name;
        this.maxEnergy = maxEnergy;
        this.energyPerTick = energyPerTick;
        this.ascendSpeed = ascendSpeed;
        this.descendSpeed = descendSpeed;
        this.moveSpeed = moveSpeed;

        setRegistryName(name);
        setTranslationKey(name);
        setMaxStackSize(1);
        setMaxDamage(100);
        setCreativeTab(CreativeTabs.TRANSPORTATION);
    }

    public int getMaxEnergy() { return maxEnergy; }
    public int getEnergyPerTick() { return energyPerTick; }
    public double getAscendSpeed() { return ascendSpeed; }
    public double getDescendSpeed() { return descendSpeed; }
    public double getMoveSpeed() { return moveSpeed; }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.BODY;
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new ICapabilitySerializable<NBTTagCompound>() {
            private final EnergyStorageInternal storage = new EnergyStorageInternal(stack, maxEnergy);

            @Override
            public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
                return capability == CapabilityEnergy.ENERGY;
            }

            @Override
            public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
                return capability == CapabilityEnergy.ENERGY ? CapabilityEnergy.ENERGY.cast(storage) : null;
            }

            @Override
            public NBTTagCompound serializeNBT() {
                return storage.serializeNBT();
            }

            @Override
            public void deserializeNBT(NBTTagCompound nbt) {
                storage.deserializeNBT(nbt);
            }
        };
    }

    public static IEnergyStorage getEnergyStorage(ItemStack stack) {
        return stack.getCapability(CapabilityEnergy.ENERGY, null);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        IEnergyStorage storage = getEnergyStorage(stack);
        if (storage == null || storage.getMaxEnergyStored() == 0) return 1.0;
        return 1.0 - ((double) storage.getEnergyStored() / storage.getMaxEnergyStored());
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return 0x00FFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        IEnergyStorage storage = getEnergyStorage(stack);
        if (storage != null) {
            tooltip.add(TextFormatting.GREEN + "Energy: " + storage.getEnergyStored() + " / " + storage.getMaxEnergyStored());
        }
        tooltip.add(TextFormatting.GRAY + I18n.translateToLocal("tooltip.moremod.jetpack.instructions"));
        // 移除右键充电提示
        // tooltip.add(TextFormatting.YELLOW + "Right-click to fully charge.");
    }

    private static class EnergyStorageInternal implements IEnergyStorage {
        private final ItemStack container;
        private final int capacity;
        // 移除 maxReceive 限制

        public EnergyStorageInternal(ItemStack stack, int capacity) {
            this.container = stack;
            this.capacity = capacity;
            // 移除 maxReceive 参数
            initNBT();
        }

        private void initNBT() {
            if (!container.hasTagCompound()) container.setTagCompound(new NBTTagCompound());
            if (!container.getTagCompound().hasKey("Energy")) container.getTagCompound().setInteger("Energy", 0);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int energy = getEnergyStored();
            // 移除充能速度限制，只受容量限制
            int received = Math.min(capacity - energy, maxReceive);
            if (!simulate) setEnergy(energy + received);
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int energy = getEnergyStored();
            int extracted = Math.min(energy, maxExtract);
            if (!simulate) setEnergy(energy - extracted);
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return container.hasTagCompound() ? container.getTagCompound().getInteger("Energy") : 0;
        }

        public void setEnergy(int value) {
            if (!container.hasTagCompound()) container.setTagCompound(new NBTTagCompound());
            container.getTagCompound().setInteger("Energy", Math.max(0, Math.min(capacity, value)));
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        public NBTTagCompound serializeNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Energy", getEnergyStored());
            return tag;
        }

        public void deserializeNBT(NBTTagCompound nbt) {
            setEnergy(nbt.getInteger("Energy"));
        }
    }
}