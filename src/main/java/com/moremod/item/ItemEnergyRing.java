package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import cofh.redstoneflux.api.IEnergyContainerItem;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.upgrades.EnergyEfficiencyManager;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.RING;
    }

    // ===== 新增：IBauble 必要方法實現 =====
    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
        // 持續效果：當有能量時給予速度加成
        if (player instanceof EntityPlayer && !player.world.isRemote) {
            int energy = getEnergyStored(itemstack);
            if (energy > MAX_ENERGY * 0.1) { // 能量大於10%時激活
                // 速度加成
                ((EntityPlayer) player).addPotionEffect(
                        new PotionEffect(MobEffects.SPEED, 5, 0, true, false));

                // 高能量時額外效果
                if (energy > MAX_ENERGY * 0.75) {
                    ((EntityPlayer) player).addPotionEffect(
                            new PotionEffect(MobEffects.STRENGTH, 5, 0, true, false));
                }
            }
        }
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer && !player.world.isRemote) {
            int energy = getEnergyStored(itemstack);
            String status = energy > 0 ? "已激活" : "能量不足";
            ((EntityPlayer) player).sendStatusMessage(
                    new TextComponentString(TextFormatting.GOLD + "虛空征服者之環" + status), true);


        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer && !player.world.isRemote) {
            ((EntityPlayer) player).sendStatusMessage(
                    new TextComponentString(TextFormatting.RED + "虛空征服者之環已離線"), true);

            // 移除效果
            player.removePotionEffect(MobEffects.SPEED);
            player.removePotionEffect(MobEffects.STRENGTH);
        }
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        // 检查是否是玩家
        if (!(player instanceof EntityPlayer)) {
            return false;
        }

        EntityPlayer entityPlayer = (EntityPlayer) player;

        // ✨ 新增：虚空征服者之环需要先装备机械核心
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(entityPlayer);
            if (baubles != null) {
                boolean hasMechanicalCore = false;

                // 遍历所有饰品栏位
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack bauble = baubles.getStackInSlot(i);
                    if (!bauble.isEmpty() && bauble.getItem() instanceof ItemMechanicalCore) {
                        hasMechanicalCore = true;
                        break;
                    }
                }

                if (!hasMechanicalCore) {
                    // 如果没有装备机械核心，发送提示消息
                    if (!entityPlayer.world.isRemote) {
                        entityPlayer.sendStatusMessage(
                                new TextComponentString(
                                        TextFormatting.RED + "✗ 虚空征服者之环需要先装备机械核心！"
                                ), true);
                    }
                    return false;
                }
            }
        } catch (Exception e) {
            // 如果出现异常，默认不允许装备
            return false;
        }

        // 已装备机械核心，允许装备
        return true;
    }
    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    // ===== 修改：檢查所有槽位（包括14-20） =====
    public static boolean tryUseRing(EntityPlayer player) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler != null) {
            // 檢查所有槽位，包括擴展的14-20
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (stack.getItem() instanceof ItemEnergyRing) {
                    IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
                    if (energy != null) {
                        int actualCost = EnergyEfficiencyManager.calculateActualCost(player, COST_PER_ATTACK);

                        if (energy.extractEnergy(actualCost, true) >= actualCost) {
                            energy.extractEnergy(actualCost, false);
                            if (actualCost < COST_PER_ATTACK && COST_PER_ATTACK > 0) {
                                EnergyEfficiencyManager.showEfficiencySaving(player, COST_PER_ATTACK, actualCost);
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean tryUseRingForTrigger(EntityPlayer player) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (stack.getItem() instanceof ItemEnergyRing) {
                    IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
                    if (energy != null) {
                        int actualCost = EnergyEfficiencyManager.calculateActualCost(player, COST_PER_TRIGGER);

                        if (energy.extractEnergy(actualCost, true) >= actualCost) {
                            energy.extractEnergy(actualCost, false);
                            if (actualCost < COST_PER_TRIGGER) {
                                EnergyEfficiencyManager.showEfficiencySaving(player, COST_PER_TRIGGER, actualCost);
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static int getStoredFromBaubles(EntityPlayer player) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (stack.getItem() instanceof ItemEnergyRing) {
                    IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
                    return energy != null ? energy.getEnergyStored() : 0;
                }
            }
        }
        return 0;
    }

    // ===== IEnergyContainerItem 實現（保持不變） =====
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

    // addInformation 方法保持原樣...
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        // 原有的 tooltip 代碼保持不變
        int currentEnergy = getEnergyStored(stack);
        double energyPercent = (double) currentEnergy / MAX_ENERGY;

        tooltip.add(TextFormatting.GOLD + "" + TextFormatting.BOLD + "虛空征服者之環");
        tooltip.add(TextFormatting.GRAY + "失落科技 - 終極攻擊系統");
        tooltip.add("");

        TextFormatting energyColor = energyPercent > 0.75 ? TextFormatting.AQUA :
                energyPercent > 0.5 ? TextFormatting.BLUE :
                        energyPercent > 0.25 ? TextFormatting.YELLOW : TextFormatting.RED;

        tooltip.add(TextFormatting.YELLOW + "能量: " + energyColor + String.format("%,d", currentEnergy) +
                TextFormatting.GRAY + " / " + TextFormatting.AQUA + String.format("%,d", MAX_ENERGY) +
                TextFormatting.GRAY + " RF (" + String.format("%.1f", energyPercent * 100) + "%)");

        // 其餘 tooltip 內容...
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new CapabilityProviderEnergyRing(stack);
    }

    // CapabilityProviderEnergyRing 內部類保持不變
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