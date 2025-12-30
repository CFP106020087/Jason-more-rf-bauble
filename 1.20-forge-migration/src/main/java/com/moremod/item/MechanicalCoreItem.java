package com.moremod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 机械核心物品 - 1.20 Forge版本
 *
 * 核心功能：
 * - 存储RF能量供升级模块使用
 * - 支持多种升级模块安装
 * - 提供被动效果（护盾、生命恢复等）
 *
 * TODO (Phase 4): 完整迁移到Curios API
 * - 实现ICurioItem接口
 * - 迁移所有升级模块效果
 * - 迁移电池系统集成
 */
public class MechanicalCoreItem extends Item {

    public static final int MAX_ENERGY = 500_000;
    public static final int MAX_UPGRADE_SLOTS = 8;

    public MechanicalCoreItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new EnergyCapabilityProvider(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        IEnergyStorage storage = getEnergyStorage(stack);
        if (storage == null || storage.getMaxEnergyStored() == 0) return 0;
        return Math.round(13.0F * storage.getEnergyStored() / storage.getMaxEnergyStored());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        IEnergyStorage storage = getEnergyStorage(stack);
        if (storage == null) return 0xFF0000;
        float ratio = (float) storage.getEnergyStored() / storage.getMaxEnergyStored();
        if (ratio > 0.5f) return 0x00FF00; // 绿色
        if (ratio > 0.25f) return 0xFFFF00; // 黄色
        return 0xFF0000; // 红色
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                // 蹲下右键：打开升级界面（TODO）
                player.displayClientMessage(Component.literal(
                        ChatFormatting.GOLD + "[机械核心] " + ChatFormatting.GRAY + "升级界面开发中..."
                ), true);
            } else {
                // 右键：显示状态
                showStatus(player, stack);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void showStatus(Player player, ItemStack stack) {
        IEnergyStorage storage = getEnergyStorage(stack);
        CompoundTag tag = stack.getOrCreateTag();

        player.displayClientMessage(Component.literal(
                ChatFormatting.DARK_PURPLE + "=== 机械核心状态 ==="
        ), false);

        // 能量状态
        if (storage != null) {
            double percentage = (double) storage.getEnergyStored() / storage.getMaxEnergyStored() * 100;
            String color = percentage > 50 ? ChatFormatting.GREEN.toString() :
                    percentage > 25 ? ChatFormatting.YELLOW.toString() : ChatFormatting.RED.toString();

            player.displayClientMessage(Component.literal(
                    color + "能量: " + formatEnergy(storage.getEnergyStored()) +
                            " / " + formatEnergy(storage.getMaxEnergyStored()) + " RF"
            ), false);
        }

        // 升级槽位
        int usedSlots = getUsedUpgradeSlots(stack);
        player.displayClientMessage(Component.literal(
                ChatFormatting.AQUA + "升级槽位: " + usedSlots + " / " + MAX_UPGRADE_SLOTS
        ), false);

        // 已安装升级
        if (usedSlots > 0) {
            player.displayClientMessage(Component.literal(
                    ChatFormatting.GRAY + "已安装升级: " + getInstalledUpgradesString(stack)
            ), false);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        IEnergyStorage storage = getEnergyStorage(stack);

        // 能量显示
        if (storage != null) {
            tooltip.add(Component.literal(ChatFormatting.GREEN + "能量：" +
                    formatEnergy(storage.getEnergyStored()) + " / " + formatEnergy(storage.getMaxEnergyStored())));
        }

        // 升级槽位
        int usedSlots = getUsedUpgradeSlots(stack);
        tooltip.add(Component.literal(ChatFormatting.AQUA + "升级槽位：" + usedSlots + " / " + MAX_UPGRADE_SLOTS));

        tooltip.add(Component.empty());
        tooltip.add(Component.literal(ChatFormatting.YELLOW + "功能："));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "  • 安装升级模块获得被动效果"));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "  • 消耗RF能量维持效果"));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "  • 右键查看状态，蹲下右键管理升级"));

        tooltip.add(Component.empty());
        tooltip.add(Component.literal(ChatFormatting.DARK_GRAY + "可用升级：护盾、生命恢复、速度..."));
    }

    // ===== 升级管理方法 =====

    public int getUsedUpgradeSlots(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("Upgrades")) return 0;
        CompoundTag upgrades = tag.getCompound("Upgrades");
        return upgrades.getAllKeys().size();
    }

    public boolean hasUpgrade(ItemStack stack, String upgradeId) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("Upgrades")) return false;
        return tag.getCompound("Upgrades").contains(upgradeId);
    }

    public boolean installUpgrade(ItemStack stack, String upgradeId, int level) {
        if (getUsedUpgradeSlots(stack) >= MAX_UPGRADE_SLOTS) return false;
        if (hasUpgrade(stack, upgradeId)) return false;

        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag upgrades = tag.getCompound("Upgrades");
        upgrades.putInt(upgradeId, level);
        tag.put("Upgrades", upgrades);
        return true;
    }

    public boolean removeUpgrade(ItemStack stack, String upgradeId) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("Upgrades")) return false;

        CompoundTag upgrades = tag.getCompound("Upgrades");
        if (!upgrades.contains(upgradeId)) return false;

        upgrades.remove(upgradeId);
        tag.put("Upgrades", upgrades);
        return true;
    }

    private String getInstalledUpgradesString(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("Upgrades")) return "无";

        CompoundTag upgrades = tag.getCompound("Upgrades");
        if (upgrades.isEmpty()) return "无";

        StringBuilder sb = new StringBuilder();
        for (String key : upgrades.getAllKeys()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(key);
        }
        return sb.toString();
    }

    // ===== 能量相关 =====

    @Nullable
    public static IEnergyStorage getEnergyStorage(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
    }

    private String formatEnergy(int energy) {
        if (energy >= 1_000_000) {
            return String.format("%.1fM", energy / 1_000_000.0);
        } else if (energy >= 1000) {
            return String.format("%.1fK", energy / 1000.0);
        } else {
            return String.valueOf(energy);
        }
    }

    // ===== 能量能力提供者 =====

    public static class EnergyCapabilityProvider implements ICapabilityProvider {
        private final ItemStack stack;
        private final LazyOptional<IEnergyStorage> energyHandler;

        public EnergyCapabilityProvider(ItemStack stack) {
            this.stack = stack;
            this.energyHandler = LazyOptional.of(() ->
                    new BatteryBaubleItem.ItemEnergyStorage(stack, MAX_ENERGY));
        }

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
            if (cap == ForgeCapabilities.ENERGY) {
                return energyHandler.cast();
            }
            return LazyOptional.empty();
        }
    }
}
