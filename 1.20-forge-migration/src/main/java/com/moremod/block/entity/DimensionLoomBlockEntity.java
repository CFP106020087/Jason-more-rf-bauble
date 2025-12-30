package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 维度织布机BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 用于维度相关合成和转换
 * - 消耗RF能量
 * - 多个输入/输出槽
 */
public class DimensionLoomBlockEntity extends BlockEntity {

    private static final int ENERGY_CAPACITY = 100000;
    private static final int ENERGY_PER_TICK = 50;
    private static final int SLOT_COUNT = 9; // 输入6 + 输出3

    private Component customName;

    // 能量存储
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, 5000, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }
    };
    private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> energy);

    // 物品槽
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    // 状态
    private int progress = 0;
    private int maxProgress = 200;
    private boolean isRunning = false;

    public DimensionLoomBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIMENSION_LOOM.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        // TODO: 实现维度织布机逻辑
        // - 检查输入物品
        // - 消耗能量
        // - 进行合成
    }

    public void setCustomName(Component name) {
        this.customName = name;
    }

    public Component getCustomName() {
        return customName;
    }

    public IItemHandler getItemHandler() {
        return inventory;
    }

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyHandler.invalidate();
        itemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.putBoolean("IsRunning", isRunning);
        tag.put("Inventory", inventory.serializeNBT());
        if (customName != null) {
            tag.putString("CustomName", Component.Serializer.toJson(customName));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        int fe = tag.getInt("Energy");
        energy.receiveEnergy(fe, false);
        progress = tag.getInt("Progress");
        isRunning = tag.getBoolean("IsRunning");
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        if (tag.contains("CustomName")) {
            customName = Component.Serializer.fromJson(tag.getString("CustomName"));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }
}
