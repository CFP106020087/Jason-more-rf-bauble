package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
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
 * 剑升级工作站BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 用于升级武器属性
 * - 消耗特定材料提升武器等级
 * - 支持多种升级类型
 */
public class SwordUpgradeStationBlockEntity extends BlockEntity {

    private static final int ENERGY_CAPACITY = 200000;
    private static final int ENERGY_PER_UPGRADE = 5000;
    private static final int UPGRADE_TIME = 100;

    // 槽位：武器槽(0) + 材料槽(1-3) + 预览槽(4)
    private static final int WEAPON_SLOT = 0;
    private static final int MATERIAL_START = 1;
    private static final int MATERIAL_END = 3;
    private static final int PREVIEW_SLOT = 4;
    private static final int TOTAL_SLOTS = 5;

    // 能量存储
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, 10000, 0) {
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
    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == WEAPON_SLOT || (slot >= MATERIAL_START && slot <= MATERIAL_END)) {
                updatePreview();
            }
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (slot == WEAPON_SLOT) {
                return stack.getItem() instanceof SwordItem;
            }
            if (slot >= MATERIAL_START && slot <= MATERIAL_END) {
                return isValidUpgradeMaterial(stack);
            }
            return false; // 预览槽不接受物品
        }
    };
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    // 状态
    private int upgradeProgress = 0;
    private boolean isUpgrading = false;

    public SwordUpgradeStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SWORD_UPGRADE_STATION.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        // 检查是否可以升级
        boolean canUpgrade = hasValidUpgrade() && energy.getEnergyStored() >= ENERGY_PER_UPGRADE;

        if (canUpgrade && isUpgrading) {
            upgradeProgress++;

            if (upgradeProgress >= UPGRADE_TIME) {
                // 完成升级
                performUpgrade();
                upgradeProgress = 0;
                isUpgrading = false;
            }
            setChanged();
        }
    }

    private boolean isValidUpgradeMaterial(ItemStack stack) {
        // TODO: 实现材料验证
        // 简化版：接受任何物品
        return true;
    }

    private boolean hasValidUpgrade() {
        ItemStack weapon = inventory.getStackInSlot(WEAPON_SLOT);
        if (weapon.isEmpty() || !(weapon.getItem() instanceof SwordItem)) {
            return false;
        }

        // 检查是否有材料
        for (int i = MATERIAL_START; i <= MATERIAL_END; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void updatePreview() {
        // TODO: 实现预览逻辑
        // 根据武器和材料计算升级后的效果
    }

    private void performUpgrade() {
        // TODO: 实现升级逻辑
        energy.extractEnergy(ENERGY_PER_UPGRADE, false);

        // 消耗材料
        for (int i = MATERIAL_START; i <= MATERIAL_END; i++) {
            ItemStack material = inventory.getStackInSlot(i);
            if (!material.isEmpty()) {
                material.shrink(1);
            }
        }

        // 应用升级到武器
        ItemStack weapon = inventory.getStackInSlot(WEAPON_SLOT);
        // TODO: 添加NBT数据或附魔
    }

    public void startUpgrade() {
        if (hasValidUpgrade() && !isUpgrading) {
            isUpgrading = true;
            upgradeProgress = 0;
            setChanged();
        }
    }

    public void cancelUpgrade() {
        isUpgrading = false;
        upgradeProgress = 0;
        setChanged();
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

    public int getUpgradeProgress() {
        return upgradeProgress;
    }

    public int getMaxUpgradeTime() {
        return UPGRADE_TIME;
    }

    public boolean isUpgrading() {
        return isUpgrading;
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
        tag.putInt("UpgradeProgress", upgradeProgress);
        tag.putBoolean("IsUpgrading", isUpgrading);
        tag.put("Inventory", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        int fe = tag.getInt("Energy");
        energy.receiveEnergy(fe, false);
        upgradeProgress = tag.getInt("UpgradeProgress");
        isUpgrading = tag.getBoolean("IsUpgrading");
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
