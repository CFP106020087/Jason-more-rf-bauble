package com.moremod.item;

import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBatteryBauble extends Item {
    public static final int MAX_TRANSFER = Integer.MAX_VALUE; // 或你想设定的最大值

    public static final int MAX_ENERGY = 10000000; // 总能量上限

    public ItemBatteryBauble() {
        setRegistryName("battery_bauble");
        setTranslationKey("battery_bauble");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new EnergyCapabilityProvider(stack);
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
        return 0x00FF00;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        IEnergyStorage storage = getEnergyStorage(stack);
        if (storage != null) {
            // 能量显示
            tooltip.add(TextFormatting.GREEN + "能量：" + formatEnergy(storage.getEnergyStored()) + " / " + formatEnergy(storage.getMaxEnergyStored()));

            // 能量百分比
            double percentage = (double) storage.getEnergyStored() / storage.getMaxEnergyStored() * 100;
            String percentageColor = percentage > 75 ? TextFormatting.GREEN.toString() :
                    percentage > 50 ? TextFormatting.YELLOW.toString() :
                            percentage > 25 ? TextFormatting.GOLD.toString() : TextFormatting.RED.toString();
            tooltip.add(percentageColor + "电量：" + String.format("%.1f", percentage) + "%");
        }

        tooltip.add("");

        // 功能描述
        tooltip.add(TextFormatting.AQUA + "功能：");
        tooltip.add(TextFormatting.GRAY + "  • 放入背包自动为装备和饰品充能");
        tooltip.add(TextFormatting.GRAY + "  • 兼容所有支持RF/FE能量的物品");
        tooltip.add(TextFormatting.GRAY + "  • 无限制传输速率，快速充电");

        tooltip.add("");

        // 使用说明
        tooltip.add(TextFormatting.YELLOW + "使用说明：");
        tooltip.add(TextFormatting.GRAY + "  • 将电池放在背包任意位置");
        tooltip.add(TextFormatting.GRAY + "  • 自动检测并充能需要电力的物品");
        tooltip.add(TextFormatting.GRAY + "  • 支持多个电池同时工作");

        tooltip.add("");

        // 充电方式
        tooltip.add(TextFormatting.LIGHT_PURPLE + "充电方式：");
        tooltip.add(TextFormatting.GRAY + "  • 使用各种模组的充电设备");
        tooltip.add(TextFormatting.GRAY + "  • 兼容IC2、TE、EIO等能量系统");
        tooltip.add(TextFormatting.GRAY + "  • 放入充电器、充电台等设备");

        tooltip.add("");

        // 技术规格
        tooltip.add(TextFormatting.DARK_GRAY + "技术规格：");
        tooltip.add(TextFormatting.DARK_GRAY + "  • 容量：" + formatEnergy(MAX_ENERGY) + " RF");
        tooltip.add(TextFormatting.DARK_GRAY + "  • 传输：无限制");
        tooltip.add(TextFormatting.DARK_GRAY + "  • 兼容：通用RF/FE标准");
    }

    // 移除右键充能功能
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            // 显示当前状态信息
            IEnergyStorage storage = getEnergyStorage(stack);
            if (storage != null) {
                double percentage = (double) storage.getEnergyStored() / storage.getMaxEnergyStored() * 100;
                String statusColor = percentage > 50 ? TextFormatting.GREEN.toString() :
                        percentage > 25 ? TextFormatting.YELLOW.toString() : TextFormatting.RED.toString();

                player.sendMessage(new TextComponentString(
                        TextFormatting.GOLD + "[便携电池] " + statusColor +
                                String.format("电量：%.1f%% (%s / %s RF)",
                                        percentage,
                                        formatEnergy(storage.getEnergyStored()),
                                        formatEnergy(storage.getMaxEnergyStored()))));

                if (percentage < 10) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "警告：电池电量过低，请及时充电！"));
                } else if (percentage > 90) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "电池电量充足，可正常使用。"));
                }
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    // 格式化能量显示
    private String formatEnergy(int energy) {
        if (energy >= 1000000) {
            return String.format("%.1fM", energy / 1000000.0);
        } else if (energy >= 1000) {
            return String.format("%.1fK", energy / 1000.0);
        } else {
            return String.valueOf(energy);
        }
    }

    public static class EnergyCapabilityProvider implements ICapabilitySerializable<NBTTagCompound> {
        private final ItemStack stack;
        private int energy;

        private final IEnergyStorage storage = new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                int received = Math.min(MAX_ENERGY - energy, maxReceive); // 不限制传输速率
                if (!simulate && received > 0) {
                    energy += received;
                    updateNBT();
                }
                return received;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                int extracted = Math.min(energy, maxExtract);
                if (!simulate && extracted > 0) {
                    energy -= extracted;
                    updateNBT();
                }
                return extracted;
            }

            @Override
            public int getEnergyStored() {
                return energy;
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

        public EnergyCapabilityProvider(ItemStack stack) {
            this.stack = stack;
            if (stack.hasTagCompound() && stack.getTagCompound().hasKey("Energy")) {
                this.energy = stack.getTagCompound().getInteger("Energy");
            }
        }

        private void updateNBT() {
            if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound().setInteger("Energy", this.energy);
        }

        @Override
        public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY;
        }

        @Override
        public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY ? CapabilityEnergy.ENERGY.cast(storage) : null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Energy", this.energy);
            return tag;
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            if (nbt.hasKey("Energy")) {
                this.energy = nbt.getInteger("Energy");
                updateNBT();
            }
        }
    }
}