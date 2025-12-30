package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
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
 * 提取站BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 从物品中提取特殊成分
 * - 消耗RF能量
 * - 输入槽 + 输出槽
 */
public class ExtractionStationBlockEntity extends BlockEntity {

    private static final int ENERGY_CAPACITY = 50000;
    private static final int ENERGY_PER_TICK = 30;
    private static final int PROCESS_TIME = 100;
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    // 能量存储
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, 2000, 0) {
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

    // 物品槽: 1个输入 + 1个输出
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot == INPUT_SLOT; // 只有输入槽可以接收物品
        }
    };
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    // 状态
    private int progress = 0;
    private boolean isRunning = false;

    public ExtractionStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXTRACTION_STATION.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);

        // 检查是否可以处理
        boolean canProcess = !input.isEmpty() &&
                             energy.getEnergyStored() >= ENERGY_PER_TICK &&
                             canOutputResult(input, output);

        if (canProcess) {
            isRunning = true;
            energy.extractEnergy(ENERGY_PER_TICK, false);
            progress++;

            if (progress >= PROCESS_TIME) {
                // 完成处理
                processItem();
                progress = 0;
            }
            setChanged();
        } else {
            if (isRunning) {
                isRunning = false;
                setChanged();
            }
        }
    }

    private boolean canOutputResult(ItemStack input, ItemStack output) {
        ItemStack result = getExtractionResult(input);
        if (result.isEmpty()) return false;
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItem(output, result)) return false;
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private ItemStack getExtractionResult(ItemStack input) {
        // TODO: 实现提取配方系统
        // 简化版：返回空
        return ItemStack.EMPTY;
    }

    private void processItem() {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        ItemStack result = getExtractionResult(input);

        if (!result.isEmpty()) {
            ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
            if (output.isEmpty()) {
                inventory.setStackInSlot(OUTPUT_SLOT, result.copy());
            } else {
                output.grow(result.getCount());
            }

            input.shrink(1);
        }
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
        return PROCESS_TIME;
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
