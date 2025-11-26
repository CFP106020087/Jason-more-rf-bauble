package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.upgrades.EnergyEfficiencyManager;
import net.minecraft.client.gui.GuiScreen;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemCleansingBauble extends Item implements IBauble {

    public static final int MAX_ENERGY = 100000;
    public static final int ENERGY_COST_PER_TICK = 20;  // 原始消耗

    public ItemCleansingBauble() {
        setRegistryName("cleansing_bauble");
        setTranslationKey("cleansing_bauble");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.TRINKET; // 默认类型，通过 canEquip 允许任意槽位
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        // 检查是否是玩家
        if (!(player instanceof EntityPlayer)) {
            return false;
        }

        EntityPlayer entityPlayer = (EntityPlayer) player;

        // 检查是否已装备机械核心
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
                                        TextFormatting.RED + "✗ 净化饰品需要先装备机械核心才能运行！"
                                ), true);
                    }
                    return false;
                }
            }
        } catch (Exception e) {
            // 如果出现异常，默认不允许装备
            return false;
        }

        // 允许装备到任意槽位
        return true;
    }
    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        return true; // 允许从任意槽位卸下
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
        // 当物品被佩戴时每tick调用一次
        // 这里可以添加一些佩戴时的特殊效果
        // 目前保持为空，净化功能在事件处理器中处理
    }

    // ===== 🎯 修改：改用 IEnergyStorage 的能量管理 =====
    public int getEnergyStored(ItemStack stack) {
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        return energy != null ? energy.getEnergyStored() : 0;
    }

    public void setEnergyStored(ItemStack stack, int amount) {
        // 通过 IEnergyStorage 设置能量（间接方式）
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null) {
            // 先清空，再填充到目标值
            energy.extractEnergy(energy.getEnergyStored(), false);
            energy.receiveEnergy(amount, false);
        }
    }

    // ===== 🔄 修改：支持能量效率的 consumeEnergy 方法 =====
    public boolean consumeEnergy(ItemStack stack, int amount) {
        return consumeEnergy(stack, amount, null);
    }

    public boolean consumeEnergy(ItemStack stack, int originalAmount, @Nullable EntityPlayer player) {
        // 如果有玩家，计算实际消耗
        int actualAmount = originalAmount;
        if (player != null) {
            actualAmount = EnergyEfficiencyManager.calculateActualCost(player, originalAmount);
        }

        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null && energy.extractEnergy(actualAmount, true) >= actualAmount) {
            energy.extractEnergy(actualAmount, false);

            // 显示节省提示（仅在有明显节省时）
            if (player != null && !player.world.isRemote && actualAmount < originalAmount) {
                // 每20 tick（1秒）显示一次节省信息，避免刷屏
                if (player.world.getTotalWorldTime() % 20 == 0) {
                    int saved = originalAmount - actualAmount;
                    int percentage = (int)((saved / (float)originalAmount) * 100);
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.GREEN + "⚡ 净化效率提升: 节省 " + percentage + "% 能量"
                    ), true);
                }
            }

            return true;
        }
        return false;
    }

    // ===== 🎯 新增：获取实际消耗值（用于显示） =====
    public static int getActualConsumption(EntityPlayer player) {
        return player != null ?
                EnergyEfficiencyManager.calculateActualCost(player, ENERGY_COST_PER_TICK) :
                ENERGY_COST_PER_TICK;
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

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null) {
            float f = (float) energy.getEnergyStored() / (float) energy.getMaxEnergyStored();
            return MathHelper.hsvToRGB(f / 3.0F, 1.0F, 1.0F);
        }
        return super.getRGBDurabilityForDisplay(stack);
    }

    // 信息显示
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        int energy = getEnergyStored(stack);
        double energyPercent = (double) energy / MAX_ENERGY * 100;

        // 主要功能描述
        tooltip.add(TextFormatting.AQUA + "" + TextFormatting.BOLD + "虚拟投影净化矩阵");
        tooltip.add("");

        // 魔幻科技风格的描述
        tooltip.add(TextFormatting.LIGHT_PURPLE + "一段又一段虚拟投影的魔法祷文高速回转着，");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "每回转一圈便象征着对于净化祷告的演算解，");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "此技术被认为是科技对于魔法认识的一大进步。");
        tooltip.add("");

        // 能量状态显示
        if (energyPercent > 75) {
            tooltip.add(TextFormatting.GREEN + "" + TextFormatting.BOLD + "◉ 祷文矩阵运转稳定");
            tooltip.add(TextFormatting.GREEN + "投影回路呈现完美的几何循环");
        } else if (energyPercent > 50) {
            tooltip.add(TextFormatting.YELLOW + "" + TextFormatting.BOLD + "◉ 祷文矩阵运转良好");
            tooltip.add(TextFormatting.YELLOW + "投影回路略有波动但仍可维持");
        } else if (energyPercent > 25) {
            tooltip.add(TextFormatting.GOLD + "" + TextFormatting.BOLD + "◉ 祷文矩阵运转不稳");
            tooltip.add(TextFormatting.GOLD + "投影回路开始出现间歇性闪烁");
        } else if (energyPercent > 0) {
            tooltip.add(TextFormatting.RED + "" + TextFormatting.BOLD + "◉ 祷文矩阵濒临崩溃");
            tooltip.add(TextFormatting.RED + "投影回路勉强维持最后的光芒");
        } else {
            tooltip.add(TextFormatting.DARK_RED + "" + TextFormatting.BOLD + "◉ 祷文矩阵已停止运转");
            tooltip.add(TextFormatting.DARK_RED + "投影回路完全暗淡无光");
        }

        tooltip.add("");

        // 🎯 修复：安全获取玩家
        EntityPlayer player = null;
        int actualCost = ENERGY_COST_PER_TICK;

        // 只在客户端且世界存在时尝试获取玩家
        if (worldIn != null && worldIn.isRemote) {
            try {
                player = net.minecraft.client.Minecraft.getMinecraft().player;
                if (player != null) {
                    actualCost = getActualConsumption(player);
                }
            } catch (Exception e) {
                // 如果获取失败，使用默认值
                actualCost = ENERGY_COST_PER_TICK;
            }
        }

        // 数值信息
        tooltip.add(TextFormatting.YELLOW + "魔力储量: " + String.format("%,d", energy) + " / " + String.format("%,d", MAX_ENERGY) + " RF");

        // 根据是否有效率加成显示不同颜色
        if (actualCost < ENERGY_COST_PER_TICK) {
            tooltip.add(TextFormatting.GREEN + "运转消耗: " + actualCost + " RF/tick (优化后)");
        } else {
            tooltip.add(TextFormatting.GRAY + "运转消耗: " + actualCost + " RF/tick");
        }

        // 能量效率支持提示
        tooltip.add(TextFormatting.GREEN + "⚡ 支持机械核心能量效率加成");
        tooltip.add("");

        // 功能说明
        if (energy >= actualCost) {
            tooltip.add(TextFormatting.GREEN + "✓ 负面效果净化: 激活中");

            // 显示可持续时间
            int ticksRemaining = energy / actualCost;
            int secondsRemaining = ticksRemaining / 20;
            if (secondsRemaining > 60) {
                int minutes = secondsRemaining / 60;
                int seconds = secondsRemaining % 60;
                tooltip.add(TextFormatting.GRAY + "剩余运行时间: " + minutes + "分" + seconds + "秒");
            } else {
                tooltip.add(TextFormatting.GRAY + "剩余运行时间: " + secondsRemaining + "秒");
            }
        } else {
            tooltip.add(TextFormatting.RED + "✗ 负面效果净化: 停用中");
            tooltip.add(TextFormatting.GRAY + "需要充足的魔力来启动净化仪式");
        }

        // Shift显示详细信息
        if (GuiScreen.isShiftKeyDown()) {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_AQUA + "=== 详细信息 ===");

            // 显示当前效率
            if (player != null && actualCost < ENERGY_COST_PER_TICK) {
                int saved = ENERGY_COST_PER_TICK - actualCost;
                int percentage = EnergyEfficiencyManager.getEfficiencyPercentage(player);
                tooltip.add(TextFormatting.GREEN + "当前效率加成: " + percentage + "%");
                tooltip.add(TextFormatting.GREEN + "每tick节省: " + saved + " RF");

                // 计算每分钟节省量
                int savedPerMinute = saved * 20 * 60;
                tooltip.add(TextFormatting.GREEN + "每分钟节省: " + String.format("%,d", savedPerMinute) + " RF");
            } else {
                tooltip.add(TextFormatting.GRAY + "当前效率加成: 0%");
                tooltip.add(TextFormatting.DARK_GRAY + "装备机械核心可减少能量消耗");
            }

            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "净化效果:");
            tooltip.add(TextFormatting.DARK_GRAY + "• 清除所有负面效果");
            tooltip.add(TextFormatting.DARK_GRAY + "• 免疫凋零与中毒");
            tooltip.add(TextFormatting.DARK_GRAY + "• 持续净化诅咒");
        } else {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "<按住Shift查看详细信息>");
        }

        tooltip.add("");
        tooltip.add(TextFormatting.ITALIC + "" + TextFormatting.DARK_PURPLE + "\"当科学与魔法的边界变得模糊，");
        tooltip.add(TextFormatting.ITALIC + "" + TextFormatting.DARK_PURPLE + " 真正的奇迹便由此诞生。\"");
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
                        setEnergyStoredInternal(stored + received);
                    }
                    return received;
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    int stored = getEnergyStored();
                    int extracted = Math.min(stored, maxExtract);
                    if (!simulate) {
                        setEnergyStoredInternal(stored - extracted);
                    }
                    return extracted;
                }

                @Override
                public int getEnergyStored() {
                    NBTTagCompound tag = stack.getTagCompound();
                    return tag != null ? tag.getInteger("Energy") : 0;
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

                private void setEnergyStoredInternal(int amount) {
                    NBTTagCompound tag = stack.getTagCompound();
                    if (tag == null) {
                        tag = new NBTTagCompound();
                        stack.setTagCompound(tag);
                    }
                    tag.setInteger("Energy", MathHelper.clamp(amount, 0, MAX_ENERGY));
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

    // 创造模式充能
    @Override
    public void onCreated(ItemStack stack, World worldIn, EntityPlayer playerIn) {
        super.onCreated(stack, worldIn, playerIn);
        if (playerIn.capabilities.isCreativeMode) {
            IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
            if (energy != null) {
                energy.receiveEnergy(MAX_ENERGY, false);
            }
        }
    }
}