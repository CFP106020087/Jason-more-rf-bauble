package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import cofh.redstoneflux.api.IEnergyContainerItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
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

public class ItemSpearBauble extends Item implements IBauble, IEnergyContainerItem {

    public static final int MAX_ENERGY = 100000;
    public static final int COST_PER_ATTACK = 0;
    public static final int COST_PER_TRIGGER = 2000;

    public ItemSpearBauble() {
        setTranslationKey("spear_bauble");
        setRegistryName("spear_bauble");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.TRINKET; // 默认类型，通过 canEquip 允许任意槽位
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        return true; // 允许装备到任意槽位
    }

    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        return true; // 允许从任意槽位卸下
    }

    public static boolean tryUseRing(EntityPlayer player) {
        return tryUseRingWithCost(player, COST_PER_ATTACK);
    }

    public static boolean tryUseRingForTrigger(EntityPlayer player) {
        return tryUseRingWithCost(player, COST_PER_TRIGGER);
    }

    private static boolean tryUseRingWithCost(EntityPlayer player, int cost) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (stack.getItem() instanceof ItemSpearBauble) {
                ItemSpearBauble ring = (ItemSpearBauble) stack.getItem();
                if (ring.getEnergyStored(stack) >= cost) {
                    ring.extractEnergy(stack, cost, false);
                    return true;
                }
            }
        }
        return false;
    }

    public static int getStoredFromBaubles(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (stack.getItem() instanceof ItemSpearBauble) {
                return ((ItemSpearBauble) stack.getItem()).getEnergyStored(stack);
            }
        }
        return 0;
    }

    @Override
    public int receiveEnergy(ItemStack stack, int maxReceive, boolean simulate) {
        int stored = getEnergyStored(stack);
        int accepted = Math.min(MAX_ENERGY - stored, maxReceive);
        if (!simulate) {
            setEnergyStored(stack, stored + accepted);
        }
        return accepted;
    }

    @Override
    public int extractEnergy(ItemStack stack, int maxExtract, boolean simulate) {
        int stored = getEnergyStored(stack);
        int extracted = Math.min(stored, maxExtract);
        if (!simulate) {
            setEnergyStored(stack, stored - extracted);
        }
        return extracted;
    }

    @Override
    public int getEnergyStored(ItemStack stack) {
        NBTTagCompound tag = getOrCreateEnergyTag(stack);
        return tag.getInteger("rf");
    }

    @Override
    public int getMaxEnergyStored(ItemStack stack) {
        return MAX_ENERGY;
    }

    private void setEnergyStored(ItemStack stack, int amount) {
        getOrCreateEnergyTag(stack).setInteger("rf", Math.max(0, Math.min(MAX_ENERGY, amount)));
    }

    private NBTTagCompound getOrCreateEnergyTag(ItemStack stack) {
        NBTTagCompound nbt = stack.getOrCreateSubCompound("Energy");
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagInfo("Energy", nbt);
        }
        return nbt;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0 - ((double) getEnergyStored(stack) / MAX_ENERGY);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.AQUA + "Stored Energy: " + getEnergyStored(stack) + " / " + MAX_ENERGY + " RF");
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new CapabilityProviderEnergy(stack);
    }

    private static class CapabilityProviderEnergy implements ICapabilitySerializable<NBTTagCompound> {
        private final ItemStack stack;
        private final IEnergyStorage wrapper;

        public CapabilityProviderEnergy(ItemStack stack) {
            this.stack = stack;
            this.wrapper = new IEnergyStorage() {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    return ((IEnergyContainerItem) stack.getItem()).receiveEnergy(stack, maxReceive, simulate);
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    return ((IEnergyContainerItem) stack.getItem()).extractEnergy(stack, maxExtract, simulate);
                }

                @Override
                public int getEnergyStored() {
                    return ((IEnergyContainerItem) stack.getItem()).getEnergyStored(stack);
                }

                @Override
                public int getMaxEnergyStored() {
                    return ((IEnergyContainerItem) stack.getItem()).getMaxEnergyStored(stack);
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
        public void deserializeNBT(NBTTagCompound nbt) {
        }
    }
}