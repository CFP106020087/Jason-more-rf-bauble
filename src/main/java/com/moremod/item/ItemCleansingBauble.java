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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.List;

public class ItemCleansingBauble extends Item implements IBauble {

    public static final int MAX_ENERGY = 100000;
    public static final int ENERGY_COST_PER_TICK = 20;

    public ItemCleansingBauble() {
        setRegistryName("cleansing_bauble");
        setTranslationKey("cleansing_bauble");
        setCreativeTab(CreativeTabs.MISC);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.AMULET;
    }

    // 能量管理
    public int getEnergyStored(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null ? tag.getInteger("Energy") : 0;
    }

    public void setEnergyStored(ItemStack stack, int amount) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger("Energy", MathHelper.clamp(amount, 0, MAX_ENERGY));
    }

    public boolean consumeEnergy(ItemStack stack, int amount) {
        int stored = getEnergyStored(stack);
        if (stored >= amount) {
            setEnergyStored(stack, stored - amount);
            return true;
        }
        return false;
    }

    // 能量条显示
    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0 - ((double) getEnergyStored(stack) / MAX_ENERGY);
    }

    // 信息显示
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.YELLOW + "Energy Stored: " + getEnergyStored(stack) + " / " + MAX_ENERGY + " RF");
        tooltip.add(TextFormatting.GRAY + "Removes negative effects while charged.");
    }

    // Forge 能量兼容（Cyclic 支持）
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new CapabilityProviderCleansing(stack);
    }

    private static class CapabilityProviderCleansing implements ICapabilitySerializable<NBTTagCompound> {
        private final ItemStack stack;
        private final IEnergyStorage wrapper;

        public CapabilityProviderCleansing(ItemStack stack) {
            this.stack = stack;
            this.wrapper = new IEnergyStorage() {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    int stored = getEnergyStored();
                    int received = Math.min(MAX_ENERGY - stored, maxReceive);
                    if (!simulate) {
                        ((ItemCleansingBauble) stack.getItem()).setEnergyStored(stack, stored + received);
                    }
                    return received;
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    int stored = getEnergyStored();
                    int extracted = Math.min(stored, maxExtract);
                    if (!simulate) {
                        ((ItemCleansingBauble) stack.getItem()).setEnergyStored(stack, stored - extracted);
                    }
                    return extracted;
                }

                @Override
                public int getEnergyStored() {
                    return ((ItemCleansingBauble) stack.getItem()).getEnergyStored(stack);
                }

                @Override
                public int getMaxEnergyStored() {
                    return MAX_ENERGY;
                }

                @Override
                public boolean canExtract() {
                    return true;
                }

                @Override
                public boolean canReceive() {
                    return true;
                }
            };
        }

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY ? (T) wrapper : null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return new NBTTagCompound();
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {}
    }
}