package com.moremod.block.entity;

import com.moremod.block.TimeControllerBlock;
import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 时间控制器BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 控制世界时间
 * - 消耗RF能量
 */
public class TimeControllerBlockEntity extends BlockEntity implements IEnergyStorage {

    public enum Mode {
        ACCELERATE("加速", 40),
        SLOW("减速", 20),
        PAUSE("暂停", 10),
        REVERSE("倒流", 80),
        DAY_ONLY("永昼", 30),
        NIGHT_ONLY("永夜", 30);

        private final String zhName;
        private final int energyCost;

        Mode(String zhName, int energyCost) {
            this.zhName = zhName;
            this.energyCost = energyCost;
        }

        public String zhName() { return zhName; }
        public int getEnergyCost() { return energyCost; }
    }

    // 能量参数
    private static final int MAX_ENERGY = 100000;
    private static final int MAX_RECEIVE = 1000;
    private static final int MIN_ENERGY_TO_WORK = 100;

    // 状态
    private int energyStored = 0;
    private boolean active = false;
    private Mode mode = Mode.PAUSE;
    private int speedLevel = 5; // 0-15

    // 内部计数器
    private int tickCounter = 0;
    private long lastProcessedTime = -1;

    private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> this);

    public TimeControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIME_CONTROLLER.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        tickCounter++;

        // 计算能量需求
        int energyNeeded = mode.getEnergyCost() * (1 + speedLevel / 3);

        // 检查能量并更新激活状态
        boolean canWork = energyStored >= energyNeeded && energyStored >= MIN_ENERGY_TO_WORK;
        setActiveInternal(canWork);

        if (!active) return;

        if (!(level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.dimension() != Level.OVERWORLD) return;

        long currentTime = serverLevel.getDayTime();
        long dayTime = currentTime % 24000;
        long gameTime = serverLevel.getGameTime();

        if (gameTime == lastProcessedTime) return;
        lastProcessedTime = gameTime;

        // 消耗能量
        energyStored = Math.max(0, energyStored - energyNeeded);
        setChanged();

        // 执行时间控制
        switch (mode) {
            case PAUSE:
                serverLevel.setDayTime(currentTime - 1);
                break;

            case REVERSE:
                int reverseAmount = 1 + speedLevel;
                serverLevel.setDayTime(currentTime - reverseAmount - 1);
                break;

            case SLOW:
                int slowFactor = Math.max(2, speedLevel + 2);
                if (tickCounter % slowFactor != 0) {
                    serverLevel.setDayTime(currentTime - 1);
                }
                break;

            case ACCELERATE:
                int accelAmount = speedLevel * 2;
                serverLevel.setDayTime(currentTime + accelAmount);
                break;

            case DAY_ONLY:
                if (dayTime < 1000 || dayTime >= 13000) {
                    long targetTime = (currentTime / 24000) * 24000 + 6000;
                    serverLevel.setDayTime(targetTime);
                }
                break;

            case NIGHT_ONLY:
                if (dayTime >= 1000 && dayTime < 13000) {
                    long targetTime = (currentTime / 24000) * 24000 + 18000;
                    serverLevel.setDayTime(targetTime);
                }
                break;
        }

        // 每秒更新比较器
        if (tickCounter % 20 == 0) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    private void setActiveInternal(boolean newActive) {
        if (this.active != newActive) {
            this.active = newActive;
            setChanged();
            if (level != null && !level.isClientSide()) {
                TimeControllerBlock.setActiveState(level, worldPosition, active);
                syncBlock();
            }
        }
    }

    private void syncBlock() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ===== IEnergyStorage 实现 =====

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        int toReceive = Math.min(MAX_ENERGY - energyStored, Math.min(MAX_RECEIVE, maxReceive));
        if (!simulate && toReceive > 0) {
            energyStored += toReceive;
            setChanged();
        }
        return toReceive;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return energyStored;
    }

    @Override
    public int getMaxEnergyStored() {
        return MAX_ENERGY;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    // ===== 公共方法 =====

    public boolean isActive() {
        return active;
    }

    public void setSpeedLevel(int level) {
        int clamped = Math.max(0, Math.min(15, level));
        if (this.speedLevel != clamped) {
            this.speedLevel = clamped;
            setChanged();
            syncBlock();
        }
    }

    public int getSpeedLevel() {
        return speedLevel;
    }

    public void cycleMode() {
        Mode[] modes = Mode.values();
        this.mode = modes[(mode.ordinal() + 1) % modes.length];
        this.tickCounter = 0;
        setChanged();
        syncBlock();
    }

    public Mode getMode() {
        return mode;
    }

    public String getStatusText() {
        int energyNeeded = mode.getEnergyCost() * (1 + speedLevel / 3);
        return String.format("%s (速度:%d) - 能量:%d/%d RF (消耗:%d RF/t) - %s",
                mode.zhName(),
                speedLevel,
                energyStored,
                MAX_ENERGY,
                energyNeeded,
                active ? "运行中" : "停止");
    }

    public int getEnergyPerTick() {
        return mode.getEnergyCost() * (1 + speedLevel / 3);
    }

    // ===== Capabilities =====

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyHandler.invalidate();
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energyStored);
        tag.putBoolean("Active", active);
        tag.putInt("Mode", mode.ordinal());
        tag.putInt("SpeedLevel", speedLevel);
        tag.putInt("TickCounter", tickCounter);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyStored = tag.getInt("Energy");
        active = tag.getBoolean("Active");
        speedLevel = tag.getInt("SpeedLevel");
        tickCounter = tag.getInt("TickCounter");

        int modeOrdinal = tag.getInt("Mode");
        if (modeOrdinal >= 0 && modeOrdinal < Mode.values().length) {
            this.mode = Mode.values()[modeOrdinal];
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("Active", active);
        tag.putInt("Mode", mode.ordinal());
        tag.putInt("Energy", energyStored);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        active = tag.getBoolean("Active");
        energyStored = tag.getInt("Energy");
        int modeOrdinal = tag.getInt("Mode");
        if (modeOrdinal >= 0 && modeOrdinal < Mode.values().length) {
            this.mode = Mode.values()[modeOrdinal];
        }
    }
}
