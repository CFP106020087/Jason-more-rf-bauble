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
 * 便携电池物品 - 1.20 Forge版本
 *
 * 功能：
 * - 存储RF能量
 * - 放入背包自动为装备充能
 * - 无限制传输速率
 *
 * TODO (Phase 4): 迁移到Curios API实现饰品功能
 */
public class BatteryBaubleItem extends Item {

    public static final int MAX_ENERGY = 10_000_000;

    public BatteryBaubleItem() {
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
        return 0x00FF00; // 绿色
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            IEnergyStorage storage = getEnergyStorage(stack);
            if (storage != null) {
                double percentage = (double) storage.getEnergyStored() / storage.getMaxEnergyStored() * 100;
                String statusColor = percentage > 50 ? ChatFormatting.GREEN.toString() :
                        percentage > 25 ? ChatFormatting.YELLOW.toString() : ChatFormatting.RED.toString();

                player.displayClientMessage(Component.literal(
                        ChatFormatting.GOLD + "[便携电池] " + statusColor +
                                String.format("电量：%.1f%% (%s / %s RF)",
                                        percentage,
                                        formatEnergy(storage.getEnergyStored()),
                                        formatEnergy(storage.getMaxEnergyStored()))), true);

                if (percentage < 10) {
                    player.displayClientMessage(Component.literal(
                            ChatFormatting.RED + "警告：电池电量过低，请及时充电！"), false);
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        IEnergyStorage storage = getEnergyStorage(stack);
        if (storage != null) {
            tooltip.add(Component.literal(ChatFormatting.GREEN + "能量：" +
                    formatEnergy(storage.getEnergyStored()) + " / " + formatEnergy(storage.getMaxEnergyStored())));

            double percentage = (double) storage.getEnergyStored() / storage.getMaxEnergyStored() * 100;
            String percentageColor = percentage > 75 ? ChatFormatting.GREEN.toString() :
                    percentage > 50 ? ChatFormatting.YELLOW.toString() :
                            percentage > 25 ? ChatFormatting.GOLD.toString() : ChatFormatting.RED.toString();
            tooltip.add(Component.literal(percentageColor + "电量：" + String.format("%.1f", percentage) + "%"));
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal(ChatFormatting.AQUA + "功能："));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "  • 放入背包自动为装备和饰品充能"));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "  • 兼容所有支持RF/FE能量的物品"));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "  • 无限制传输速率，快速充电"));
    }

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

    /**
     * 能量能力提供者 - 使用ItemStack的NBT存储能量
     */
    public static class EnergyCapabilityProvider implements ICapabilityProvider {
        private final ItemStack stack;
        private final LazyOptional<IEnergyStorage> energyHandler;

        public EnergyCapabilityProvider(ItemStack stack) {
            this.stack = stack;
            this.energyHandler = LazyOptional.of(() -> new ItemEnergyStorage(stack, MAX_ENERGY));
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

    /**
     * 物品能量存储实现 - 存储在ItemStack的NBT中
     */
    public static class ItemEnergyStorage implements IEnergyStorage {
        private final ItemStack stack;
        private final int maxEnergy;

        public ItemEnergyStorage(ItemStack stack, int maxEnergy) {
            this.stack = stack;
            this.maxEnergy = maxEnergy;
        }

        private int getEnergy() {
            CompoundTag tag = stack.getOrCreateTag();
            return tag.getInt("Energy");
        }

        private void setEnergy(int energy) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putInt("Energy", Math.max(0, Math.min(energy, maxEnergy)));
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int stored = getEnergy();
            int received = Math.min(maxEnergy - stored, maxReceive);
            if (!simulate && received > 0) {
                setEnergy(stored + received);
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int stored = getEnergy();
            int extracted = Math.min(stored, maxExtract);
            if (!simulate && extracted > 0) {
                setEnergy(stored - extracted);
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return getEnergy();
        }

        @Override
        public int getMaxEnergyStored() {
            return maxEnergy;
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
