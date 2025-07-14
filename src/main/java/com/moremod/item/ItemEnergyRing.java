package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.BaublesApi;
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
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.List;

public class ItemEnergyRing extends Item implements IBauble, IEnergyContainerItem {

    public static final int MAX_ENERGY = 100000;
    public static final int COST_PER_ATTACK = 0;
    public static final int COST_PER_TRIGGER = 2000;

    public ItemEnergyRing() {
        setTranslationKey("energy_ring");
        setRegistryName("energy_ring");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.RING;
    }




    public static boolean tryUseRing(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (stack.getItem() instanceof ItemEnergyRing) {
                ItemEnergyRing ring = (ItemEnergyRing) stack.getItem();
                if (ring.getEnergyStored(stack) >= COST_PER_ATTACK) {
                    ring.extractEnergy(stack, COST_PER_ATTACK, false);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean tryUseRingForTrigger(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (stack.getItem() instanceof ItemEnergyRing) {
                ItemEnergyRing ring = (ItemEnergyRing) stack.getItem();
                if (ring.getEnergyStored(stack) >= COST_PER_TRIGGER) {
                    ring.extractEnergy(stack, COST_PER_TRIGGER, false);
                    return true;
                }
            }
        }
        return false;
    }

    public static int getStoredFromBaubles(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (stack.getItem() instanceof ItemEnergyRing) {
                return ((ItemEnergyRing) stack.getItem()).getEnergyStored(stack);
            }
        }
        return 0;
    }

    @Override
    public int receiveEnergy(ItemStack stack, int maxReceive, boolean simulate) {
        int stored = getEnergyStored(stack);
        int accepted = Math.min(MAX_ENERGY - stored, maxReceive);
        if (!simulate) {
            stack.getOrCreateSubCompound("Energy").setInteger("rf", stored + accepted);
        }
        return accepted;
    }

    @Override
    public int extractEnergy(ItemStack stack, int maxExtract, boolean simulate) {
        int stored = getEnergyStored(stack);
        int extracted = Math.min(stored, maxExtract);
        if (!simulate) {
            stack.getOrCreateSubCompound("Energy").setInteger("rf", stored - extracted);
        }
        return extracted;
    }

    @Override
    public int getEnergyStored(ItemStack stack) {
        return stack.hasTagCompound() && stack.getSubCompound("Energy") != null
                ? stack.getSubCompound("Energy").getInteger("rf")
                : 0;
    }

    @Override
    public int getMaxEnergyStored(ItemStack stack) {
        return MAX_ENERGY;
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
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            receiveEnergy(stack, MAX_ENERGY, false);
            player.sendMessage(new TextComponentString("Energy Ring fully recharged."));
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new CapabilityProviderEnergyRing(stack);
    }

    private static class CapabilityProviderEnergyRing implements ICapabilitySerializable<NBTTagCompound> {
        private final ItemStack stack;
        private final IEnergyStorage wrapper;

        public CapabilityProviderEnergyRing(ItemStack stack) {
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