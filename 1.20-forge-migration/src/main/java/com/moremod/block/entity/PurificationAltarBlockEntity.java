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
 * 提纯祭坛BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 用于提纯和升级材料
 * - 6个输入槽 + 1个输出槽
 * - 消耗RF能量进行合成
 */
public class PurificationAltarBlockEntity extends BlockEntity {

    private static final int ENERGY_CAPACITY = 100000;
    private static final int ENERGY_PER_TICK = 80;
    private static final int PROCESS_TIME = 200;
    private static final int INPUT_SLOTS = 6;
    private static final int OUTPUT_SLOT = 6;
    private static final int TOTAL_SLOTS = 7;

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

    // 物品槽: 6个输入 + 1个输出
    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot < OUTPUT_SLOT; // 只有输入槽可以接收物品
        }
    };
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    // 状态
    private int progress = 0;
    private boolean isRunning = false;

    public PurificationAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PURIFICATION_ALTAR.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        // 检查是否可以处理
        boolean canProcess = hasValidRecipe() && energy.getEnergyStored() >= ENERGY_PER_TICK;

        if (canProcess) {
            if (!isRunning) {
                isRunning = true;
            }
            energy.extractEnergy(ENERGY_PER_TICK, false);
            progress++;

            if (progress >= PROCESS_TIME) {
                // 完成处理
                processRecipe();
                progress = 0;
                isRunning = false;
            }
            setChanged();
        } else {
            if (isRunning || progress > 0) {
                progress = 0;
                isRunning = false;
                setChanged();
            }
        }
    }

    private boolean hasValidRecipe() {
        // TODO: 实现配方验证系统
        // 简化版：检查是否有输入物品且输出槽有空间
        boolean hasInput = false;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                hasInput = true;
                break;
            }
        }

        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        boolean hasOutputSpace = output.isEmpty() || output.getCount() < output.getMaxStackSize();

        return hasInput && hasOutputSpace;
    }

    private void processRecipe() {
        // TODO: 实现配方处理
        // 简化版：消耗第一个输入物品
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack input = inventory.getStackInSlot(i);
            if (!input.isEmpty()) {
                input.shrink(1);
                break;
            }
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
        return progress * 100 / PROCESS_TIME;
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
