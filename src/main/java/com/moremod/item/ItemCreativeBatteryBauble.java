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
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.List;

public class ItemCreativeBatteryBauble extends Item {

    public static final int DISPLAY_MAX_ENERGY = 2147483647; // Integer.MAX_VALUE
    public static final int MAX_TRANSFER = Integer.MAX_VALUE;

    public ItemCreativeBatteryBauble() {
        setRegistryName("creative_battery_bauble");
        setTranslationKey("creative_battery_bauble");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new CreativeEnergyCapabilityProvider(stack);
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
        // 创造电池永远显示满电
        return 0.0;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        // 彩虹色能量条，表示无限能量
        return 0xFFD700; // 金色
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        // 创造物品发光效果
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        // 能量显示 - 无限
        tooltip.add(TextFormatting.GOLD + "" + TextFormatting.BOLD + "能量：∞ (无限)");
        tooltip.add(TextFormatting.YELLOW + "电量：100% (创造模式)");

        tooltip.add("");

        // 创造模式特性
        tooltip.add(TextFormatting.LIGHT_PURPLE + "" + TextFormatting.BOLD + "创造模式特性：");
        tooltip.add(TextFormatting.GRAY + "  • 永不耗尽的无限能量源");
        tooltip.add(TextFormatting.GRAY + "  • 可以无限供应任何设备");
        tooltip.add(TextFormatting.GRAY + "  • 无视任何能量消耗");

        tooltip.add("");

        // 功能描述
        tooltip.add(TextFormatting.AQUA + "功能：");
        tooltip.add(TextFormatting.GRAY + "  • 放入背包自动为所有装备充能");
        tooltip.add(TextFormatting.GRAY + "  • 无限制能量输出，瞬间充满");
        tooltip.add(TextFormatting.GRAY + "  • 兼容所有支持RF/FE能量的物品");

        tooltip.add("");

        // 使用说明
        tooltip.add(TextFormatting.YELLOW + "使用说明：");
        tooltip.add(TextFormatting.GRAY + "  • 将创造电池放在背包任意位置");
        tooltip.add(TextFormatting.GRAY + "  • 自动为所有耗能装备提供无限电力");
        tooltip.add(TextFormatting.GRAY + "  • 可与普通电池共存使用");

        tooltip.add("");

        // 特殊说明
        tooltip.add(TextFormatting.GOLD + "特殊功能：");
        tooltip.add(TextFormatting.GRAY + "  • 永远不会被消耗或损坏");
        tooltip.add(TextFormatting.GRAY + "  • 提供无限RF/FE能量输出");

    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            // 显示创造电池状态
            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "" + TextFormatting.BOLD + "[创造电池] " +
                            TextFormatting.LIGHT_PURPLE + "无限能量源已激活"));

            player.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "能量状态：∞ / ∞ RF (100%)"));

            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "正在为背包中的所有设备提供无限电力..."));

            // 特殊效果：显示一些"技术"信息
            if (player.isSneaking()) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_GRAY + ">> 量子能量矩阵：在线"));
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_GRAY + ">> 零点能量场：稳定"));
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_GRAY + ">> 能量输出效率：∞%"));
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    public static class CreativeEnergyCapabilityProvider implements ICapabilitySerializable<NBTTagCompound> {
        private final ItemStack stack;

        private final IEnergyStorage storage = new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                // 创造电池不需要接收能量，但为了兼容性返回接收量
                return maxReceive;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                // 无限提供能量，永远返回请求的量
                return maxExtract;
            }

            @Override
            public int getEnergyStored() {
                // 永远返回"满"能量
                return DISPLAY_MAX_ENERGY;
            }

            @Override
            public int getMaxEnergyStored() {
                // 显示最大值
                return DISPLAY_MAX_ENERGY;
            }

            @Override
            public boolean canExtract() {
                return true;
            }

            @Override
            public boolean canReceive() {
                return true; // 为了兼容性
            }
        };

        public CreativeEnergyCapabilityProvider(ItemStack stack) {
            this.stack = stack;
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
            // 创造电池不需要保存数据
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Energy", DISPLAY_MAX_ENERGY);
            tag.setBoolean("Creative", true);
            return tag;
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            // 创造电池不需要读取数据，永远是满的
        }
    }
}