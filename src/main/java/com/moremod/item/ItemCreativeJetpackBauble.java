// ItemCreativeJetpackBauble.java
package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.moremod.creativetab.moremodCreativeTab;
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

public class ItemCreativeJetpackBauble extends Item implements IBauble {

    // 速度模式枚举
    public enum SpeedMode {
        SLOW(0, "Slow", 0.2, 0.15, 0.25, TextFormatting.GREEN),
        NORMAL(1, "Normal", 0.5, 0.3, 0.4, TextFormatting.YELLOW),
        FAST(2, "Fast", 0.8, 0.5, 0.6, TextFormatting.RED),
        ULTRA(3, "Ultra", 1.2, 0.8, 0.9, TextFormatting.LIGHT_PURPLE);

        private final int id;
        private final String name;
        private final double ascendSpeed;
        private final double descendSpeed;
        private final double moveSpeed;
        private final TextFormatting color;

        SpeedMode(int id, String name, double ascendSpeed, double descendSpeed,
                  double moveSpeed, TextFormatting color) {
            this.id = id;
            this.name = name;
            this.ascendSpeed = ascendSpeed;
            this.descendSpeed = descendSpeed;
            this.moveSpeed = moveSpeed;
            this.color = color;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public double getAscendSpeed() { return ascendSpeed; }
        public double getDescendSpeed() { return descendSpeed; }
        public double getMoveSpeed() { return moveSpeed; }
        public TextFormatting getColor() { return color; }

        public static SpeedMode fromId(int id) {
            for (SpeedMode mode : values()) {
                if (mode.id == id) return mode;
            }
            return NORMAL;
        }

        public SpeedMode next() {
            int nextId = (this.id + 1) % values().length;
            return fromId(nextId);
        }
    }

    private final String name;

    public ItemCreativeJetpackBauble(String name) {
        this.name = name;
        setRegistryName(name);
        setTranslationKey(name);
        setMaxStackSize(1);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    // 获取当前速度模式
    public SpeedMode getSpeedMode(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        int modeId = stack.getTagCompound().getInteger("SpeedMode");
        return SpeedMode.fromId(modeId);
    }

    // 设置速度模式
    public void setSpeedMode(ItemStack stack, SpeedMode mode) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger("SpeedMode", mode.getId());
    }

    // 切换到下一个速度模式
    public void nextSpeedMode(ItemStack stack, EntityPlayer player) {
        SpeedMode currentMode = getSpeedMode(stack);
        SpeedMode nextMode = currentMode.next();
        setSpeedMode(stack, nextMode);

        if (player != null && !player.world.isRemote) {
            String message = "§7Creative Jetpack Speed: " + nextMode.getColor() + nextMode.getName();
            player.sendMessage(new TextComponentString(message));
        }
    }

    // 获取当前模式的速度值
    public double getAscendSpeed(ItemStack stack) {
        return getSpeedMode(stack).getAscendSpeed();
    }

    public double getDescendSpeed(ItemStack stack) {
        return getSpeedMode(stack).getDescendSpeed();
    }

    public double getMoveSpeed(ItemStack stack) {
        return getSpeedMode(stack).getMoveSpeed();
    }

    // 为了兼容现有的EventHandlerJetpack，提供这些方法
    public double getAscendSpeed() {
        return 0.5; // 默认速度，实际会被重写
    }

    public double getDescendSpeed() {
        return 0.3;
    }

    public double getMoveSpeed() {
        return 0.4;
    }

    public int getEnergyPerTick() {
        return 0; // 创造模式不消耗能量
    }

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

    // 右键点击切换速度模式
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            nextSpeedMode(stack, player);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    // 创造模式能量系统 - 无限能量
    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound nbt) {
        return new ICapabilitySerializable<NBTTagCompound>() {
            private final CreativeEnergyStorage storage = new CreativeEnergyStorage();

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
                return new NBTTagCompound();
            }

            @Override
            public void deserializeNBT(NBTTagCompound nbt) {
                // 创造模式不需要保存能量数据
            }
        };
    }

    public static IEnergyStorage getEnergyStorage(ItemStack stack) {
        return stack.getCapability(CapabilityEnergy.ENERGY, null);
    }

    // 创造模式不显示耐久度条
    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        SpeedMode mode = getSpeedMode(stack);

        tooltip.add(TextFormatting.GOLD + "Creative Mode Jetpack");
        tooltip.add(TextFormatting.GREEN + "Energy: ∞ (Unlimited)");
        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + "Speed Mode: " + mode.getColor() + mode.getName());
        tooltip.add(TextFormatting.GRAY + "Ascend: " + String.format("%.1f", mode.getAscendSpeed()));
        tooltip.add(TextFormatting.GRAY + "Descend: " + String.format("%.1f", mode.getDescendSpeed()));
        tooltip.add(TextFormatting.GRAY + "Move: " + String.format("%.1f", mode.getMoveSpeed()));
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "Hold Space to ascend, Hold Shift to descend");
        tooltip.add(TextFormatting.YELLOW + "Shift + Right-click to change speed mode");
        tooltip.add(TextFormatting.AQUA + "No energy consumption!");
    }

    // 检查是否有无限能量
    public boolean hasUnlimitedEnergy(ItemStack stack) {
        return true;
    }

    // 创造模式能量存储 - 始终满能量，不会消耗
    private static class CreativeEnergyStorage implements IEnergyStorage {

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0; // 创造模式不需要充电
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return maxExtract; // 总是返回请求的能量量，但实际不消耗
        }

        @Override
        public int getEnergyStored() {
            return Integer.MAX_VALUE; // 显示为最大能量
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false; // 不需要充电
        }
    }
}