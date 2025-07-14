package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import javax.annotation.Nullable;
import java.util.List;

public class ItemCrudeEnergyBarrier extends Item implements IBauble {

    public static final int MAX_ENERGY = 20000;
    public static final int COST_PER_BLOCK = 500;

    public ItemCrudeEnergyBarrier() {
        setRegistryName("crude_energy_barrier");
        setTranslationKey("crude_energy_barrier");
        setCreativeTab(CreativeTabs.COMBAT);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.BODY;
    }

    public static int getEnergyStored(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null ? tag.getInteger("Energy") : 0;
    }

    public static void setEnergyStored(ItemStack stack, int amount) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger("Energy", Math.max(0, Math.min(MAX_ENERGY, amount)));
    }

    public boolean consumeEnergy(ItemStack stack, int amount) {
        int stored = getEnergyStored(stack);
        if (stored >= amount) {
            setEnergyStored(stack, stored - amount);
            return true;
        }
        return false;
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
        int energy = getEnergyStored(stack);
        tooltip.add(TextFormatting.YELLOW + "能量：" + energy + " / " + MAX_ENERGY + " RF");
        tooltip.add(TextFormatting.RED + "25% 几率格挡近战攻击");
        tooltip.add(TextFormatting.GRAY + "需要外部能量设备充电");
        tooltip.add(TextFormatting.DARK_GRAY + "每次格挡消耗 " + COST_PER_BLOCK + " RF");
    }

    // 移除右键充能功能 - 现在什么也不做
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            int energy = getEnergyStored(stack);
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "粗劣护盾当前能量：" +
                            TextFormatting.YELLOW + energy + "/" + MAX_ENERGY + " RF"
            ));

            if (energy < COST_PER_BLOCK) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "能量不足，无法提供保护！需要外部充电设备补充能量。"
                ));
            } else {
                int blocksLeft = energy / COST_PER_BLOCK;
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "护盾可格挡约 " + blocksLeft + " 次攻击"
                ));
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new CapabilityProviderCrudeBarrier(stack);
    }

    private static class CapabilityProviderCrudeBarrier implements ICapabilitySerializable<NBTTagCompound> {
        private final ItemStack stack;
        private final IEnergyStorage wrapper;

        public CapabilityProviderCrudeBarrier(ItemStack stack) {
            this.stack = stack;
            this.wrapper = new IEnergyStorage() {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    int stored = getEnergyStored();
                    int received = Math.min(MAX_ENERGY - stored, maxReceive);
                    if (!simulate) setEnergyStored(stack, stored + received);
                    return received;
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    int stored = getEnergyStored();
                    int extracted = Math.min(stored, maxExtract);
                    if (!simulate) setEnergyStored(stack, stored - extracted);
                    return extracted;
                }

                @Override
                public int getEnergyStored() {
                    return ItemCrudeEnergyBarrier.getEnergyStored(stack);
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

    public static boolean isMeleeDamage(DamageSource source) {
        return source.getImmediateSource() instanceof net.minecraft.entity.Entity &&
                !source.isProjectile() &&
                !source.isMagicDamage() &&
                !source.isExplosion() &&
                !source.isFireDamage();
    }

    public static boolean tryBlock(LivingAttackEvent event, ItemStack stack) {
        if (!isMeleeDamage(event.getSource())) return false;

        int energy = getEnergyStored(stack);
        if (energy < COST_PER_BLOCK) return false;

        // 50% 几率格挡
        if (event.getEntityLiving().getRNG().nextFloat() > 0.25f) return false;

        setEnergyStored(stack, energy - COST_PER_BLOCK);
        event.setCanceled(true);

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!player.world.isRemote) {
            player.sendStatusMessage(
                    new TextComponentString(TextFormatting.GRAY + "[粗劣护盾] 格挡了近战攻击（剩余：" + getEnergyStored(stack) + " RF）"),
                    true
            );
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ITEM_SHIELD_BLOCK,
                    player.getSoundCategory(), 0.3F, 1.4F);
        }
        return true;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getEnergyStored(stack) > 0;
    }
}