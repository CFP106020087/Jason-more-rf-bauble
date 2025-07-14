package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class CrudeEnergyBarrierEventHandler extends Item implements IBauble {

    public static final int MAX_ENERGY = 20000;
    public static final int COST_PER_HIT = 1000;

    public CrudeEnergyBarrierEventHandler() {
        setRegistryName("crude_energy_barrier");
        setTranslationKey("crude_energy_barrier");
        setCreativeTab(CreativeTabs.COMBAT);
        setMaxStackSize(1);
    }

    // ✔ 注册 Forge 能量能力
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new EnergyStorageProvider(stack);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.BODY;
    }

    // ✔ 拦截伤害方法入口
    public static boolean tryBlock(net.minecraftforge.event.entity.living.LivingAttackEvent event, ItemStack stack) {
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null && energy.getEnergyStored() >= COST_PER_HIT) {
            energy.extractEnergy(COST_PER_HIT, false);
            event.setCanceled(true); // 免疫伤害
            return true;
        }
        return false;
    }

    // ✔ 能量能力提供类
    public static class EnergyStorageProvider implements ICapabilityProvider {

        private final ItemStack stack;
        private final EnergyStorageWrapper energyWrapper;

        public EnergyStorageProvider(ItemStack stack) {
            this.stack = stack;
            this.energyWrapper = new EnergyStorageWrapper(stack);
        }

        @Override
        public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY;
        }

        @Nullable
        @Override
        public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
            if (capability == CapabilityEnergy.ENERGY) {
                return CapabilityEnergy.ENERGY.cast(energyWrapper);
            }
            return null;
        }
    }

    // ✔ 能量实际逻辑封装
    public static class EnergyStorageWrapper implements IEnergyStorage {

        private final ItemStack stack;

        public EnergyStorageWrapper(ItemStack stack) {
            this.stack = stack;
        }

        private NBTTagCompound getOrCreateTag() {
            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            return stack.getTagCompound();
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int energy = getEnergyStored();
            int received = Math.min(MAX_ENERGY - energy, maxReceive);
            if (!simulate) {
                getOrCreateTag().setInteger("Energy", energy + received);
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int energy = getEnergyStored();
            int extracted = Math.min(energy, maxExtract);
            if (!simulate) {
                getOrCreateTag().setInteger("Energy", energy - extracted);
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return getOrCreateTag().getInteger("Energy");
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
    }
}
