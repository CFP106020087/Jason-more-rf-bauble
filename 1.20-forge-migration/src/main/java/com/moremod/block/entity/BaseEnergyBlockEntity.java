package com.moremod.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
 * 能量方块实体基类 - 1.20 Forge版本
 *
 * 1.12 -> 1.20 API变更:
 * - TileEntity -> BlockEntity
 * - ITickable.update() -> BlockEntityTicker lambda
 * - NBTTagCompound -> CompoundTag
 * - writeToNBT/readFromNBT -> saveAdditional/load
 * - world -> level
 * - markDirty() -> setChanged()
 * - hasCapability/getCapability -> getCapability with LazyOptional
 */
public abstract class BaseEnergyBlockEntity extends BlockEntity {

    protected final ModEnergyStorage energy;
    protected final ItemStackHandler inventory;
    protected final LazyOptional<IEnergyStorage> energyHandler;
    protected final LazyOptional<IItemHandler> itemHandler;

    public BaseEnergyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                  int energyCapacity, int maxReceive, int maxExtract, int inventorySize) {
        super(type, pos, state);

        this.energy = new ModEnergyStorage(energyCapacity, maxReceive, maxExtract) {
            @Override
            public void onEnergyChanged() {
                setChanged();
                syncToClient();
            }
        };

        this.inventory = createInventory(inventorySize);
        this.energyHandler = LazyOptional.of(() -> energy);
        this.itemHandler = LazyOptional.of(() -> inventory);
    }

    protected ItemStackHandler createInventory(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                syncToClient();
            }
        };
    }

    /**
     * 服务端tick - 子类实现
     */
    public abstract void serverTick();

    // ===== Energy =====

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    public int receiveEnergy(int amount, boolean simulate) {
        return energy.receiveEnergy(amount, simulate);
    }

    public int extractEnergy(int amount, boolean simulate) {
        return energy.extractEnergy(amount, simulate);
    }

    // ===== Capabilities =====

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

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.put("Inventory", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getInt("Energy"));
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
    }

    // ===== 网络同步 =====

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

    protected void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * 自定义EnergyStorage，支持变化回调
     */
    public static class ModEnergyStorage extends EnergyStorage {

        public ModEnergyStorage(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                onEnergyChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                onEnergyChanged();
            }
            return extracted;
        }

        public void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(capacity, energy));
        }

        public void onEnergyChanged() {
            // 子类覆盖
        }
    }
}
