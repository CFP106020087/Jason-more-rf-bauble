package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 抽油机核心BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 消耗RF能量
 * - 从区块石油矿脉中提取石油
 * - 储存石油并转换为石油桶
 * - 支持增速插件（最多4个）
 */
public class OilExtractorCoreBlockEntity extends BlockEntity {

    // 配置
    private static final int ENERGY_CAPACITY = 500000;
    private static final int ENERGY_PER_TICK = 100;
    private static final int BASE_OIL_PER_TICK = 30;
    private static final int MAX_OIL_STORAGE = 16000;
    private static final int MB_PER_BUCKET = 1000;
    private static final int UPGRADE_SLOTS = 4;
    private static final float SPEED_PER_UPGRADE = 0.5f;

    // 能量存储
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, 10000, ENERGY_PER_TICK * 2) {
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

    // 增速插件槽
    private final ItemStackHandler upgradeInventory = new ItemStackHandler(UPGRADE_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            cachedSpeedMultiplier = -1;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return isValidUpgrade(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> upgradeInventory);

    private float cachedSpeedMultiplier = -1;

    // 石油液体槽
    private final FluidTank fluidTank = new FluidTank(MAX_OIL_STORAGE) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return false; // 不接受外部输入
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> fluidTank);

    // 状态
    private int extractedTotal = 0;
    private boolean isRunning = false;
    private int tickCounter = 0;

    public OilExtractorCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OIL_EXTRACTOR_CORE.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        tickCounter++;

        // 每10tick检测一次
        if (tickCounter % 10 == 0) {
            // TODO: 检查多方块结构和石油矿脉
            boolean hasEnergy = energy.getEnergyStored() >= ENERGY_PER_TICK;
            boolean hasStorageSpace = fluidTank.getFluidAmount() < MAX_OIL_STORAGE;

            // 简化版：假设结构有效，有固定石油量
            isRunning = hasEnergy && hasStorageSpace && getRemainingOil() > 0;
        }

        if (isRunning) {
            float speedMultiplier = getSpeedMultiplier();
            int actualEnergyPerTick = (int) (ENERGY_PER_TICK * speedMultiplier);

            if (energy.extractEnergy(actualEnergyPerTick, false) >= actualEnergyPerTick) {
                int oilPerTick = (int) (BASE_OIL_PER_TICK * speedMultiplier);
                int canExtract = Math.min(oilPerTick, MAX_OIL_STORAGE - fluidTank.getFluidAmount());
                int remaining = getRemainingOil();
                int actualExtract = Math.min(canExtract, remaining);

                if (actualExtract > 0) {
                    // 使用水作为替代（未定义石油流体）
                    fluidTank.fill(new FluidStack(Fluids.WATER, actualExtract),
                            IFluidHandler.FluidAction.EXECUTE);
                    extractedTotal += actualExtract;
                    setChanged();

                    // 粒子效果
                    if (tickCounter % 20 == 0 && level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SMOKE,
                                worldPosition.getX() + 0.5,
                                worldPosition.getY() + 1.5,
                                worldPosition.getZ() + 0.5,
                                5, 0.25, 0.5, 0.25, 0.02);
                    }
                } else {
                    isRunning = false;
                }
            }
        }
    }

    public static boolean isValidUpgrade(ItemStack stack) {
        return stack.is(Items.REDSTONE) ||
               stack.is(Items.GLOWSTONE_DUST) ||
               stack.is(Items.BLAZE_POWDER) ||
               stack.is(Items.EMERALD);
    }

    public float getSpeedMultiplier() {
        if (cachedSpeedMultiplier < 0) {
            int upgradeCount = 0;
            for (int i = 0; i < UPGRADE_SLOTS; i++) {
                if (!upgradeInventory.getStackInSlot(i).isEmpty()) {
                    upgradeCount++;
                }
            }
            cachedSpeedMultiplier = 1.0f + (upgradeCount * SPEED_PER_UPGRADE);
        }
        return cachedSpeedMultiplier;
    }

    public int getRemainingOil() {
        // TODO: 从区块数据读取石油矿脉信息
        // 简化版：假设有100000 mB石油
        return Math.max(0, 100000 - extractedTotal);
    }

    public int getAvailableBuckets() {
        return fluidTank.getFluidAmount() / MB_PER_BUCKET;
    }

    public ItemStack extractOilBucket() {
        if (fluidTank.getFluidAmount() >= MB_PER_BUCKET) {
            fluidTank.drain(MB_PER_BUCKET, IFluidHandler.FluidAction.EXECUTE);
            setChanged();
            // 使用水桶作为替代
            return new ItemStack(Items.WATER_BUCKET);
        }
        return ItemStack.EMPTY;
    }

    // ===== Getters =====

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    public int getStoredOil() {
        return fluidTank.getFluidAmount();
    }

    public int getMaxOilStorage() {
        return MAX_OIL_STORAGE;
    }

    public boolean isRunning() {
        return isRunning;
    }

    // ===== Capabilities =====

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHandler.cast();
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
        fluidHandler.invalidate();
        itemHandler.invalidate();
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("ExtractedTotal", extractedTotal);
        tag.putBoolean("IsRunning", isRunning);
        tag.put("FluidTank", fluidTank.writeToNBT(new CompoundTag()));
        tag.put("UpgradeInventory", upgradeInventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        int fe = tag.getInt("Energy");
        energy.receiveEnergy(fe, false);
        extractedTotal = tag.getInt("ExtractedTotal");
        isRunning = tag.getBoolean("IsRunning");
        if (tag.contains("FluidTank")) {
            fluidTank.readFromNBT(tag.getCompound("FluidTank"));
        }
        if (tag.contains("UpgradeInventory")) {
            upgradeInventory.deserializeNBT(tag.getCompound("UpgradeInventory"));
            cachedSpeedMultiplier = -1;
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
