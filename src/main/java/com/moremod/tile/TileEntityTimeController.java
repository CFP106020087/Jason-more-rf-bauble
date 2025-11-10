package com.moremod.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class TileEntityTimeController extends TileEntity implements ITickable, IEnergyStorage {

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

    // ===== 能量参数 =====
    private static final int MAX_ENERGY = 100000;
    private static final int MAX_RECEIVE = 1000;
    private static final int MIN_ENERGY_TO_WORK = 100;

    // ===== 状态 =====
    private int energyStored = 0;
    private boolean active = false;
    private Mode mode = Mode.PAUSE;
    private int speedLevel = 5;  // 0-15

    // ===== 内部计数器 =====
    private int tickCounter = 0;
    private long lastProcessedTime = -1;

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        tickCounter++;

        // 计算能量需求
        int energyNeeded = mode.getEnergyCost() * (1 + speedLevel / 3);

        // 检查能量并更新激活状态
        boolean canWork = energyStored >= energyNeeded && energyStored >= MIN_ENERGY_TO_WORK;
        setActiveInternal(canWork);

        if (!active) return;

        if (!(world instanceof WorldServer)) return;
        WorldServer worldServer = (WorldServer) world;
        if (worldServer.provider.getDimension() != 0) return;

        long currentTime = worldServer.getWorldTime();
        long dayTime = currentTime % 24000;
        long totalTime = worldServer.getTotalWorldTime();

        if (totalTime == lastProcessedTime) return;
        lastProcessedTime = totalTime;

        // 消耗能量
        energyStored = Math.max(0, energyStored - energyNeeded);
        markDirty();

        // 执行时间控制
        switch (mode) {
            case PAUSE:
                worldServer.setWorldTime(currentTime - 1);
                break;

            case REVERSE:
                int reverseAmount = 1 + speedLevel;
                worldServer.setWorldTime(currentTime - reverseAmount - 1);
                break;

            case SLOW:
                int slowFactor = Math.max(2, speedLevel + 2);
                if (tickCounter % slowFactor != 0) {
                    worldServer.setWorldTime(currentTime - 1);
                }
                break;

            case ACCELERATE:
                int accelAmount = speedLevel * 2;
                worldServer.setWorldTime(currentTime + accelAmount);
                break;

            case DAY_ONLY:
                if (dayTime < 1000 || dayTime >= 13000) {
                    long targetTime = (currentTime / 24000) * 24000 + 6000;
                    worldServer.setWorldTime(targetTime);
                }
                break;

            case NIGHT_ONLY:
                if (dayTime >= 1000 && dayTime < 13000) {
                    long targetTime = (currentTime / 24000) * 24000 + 18000;
                    worldServer.setWorldTime(targetTime);
                }
                break;
        }

        // 每秒更新比较器
        if (tickCounter % 20 == 0) {
            world.updateComparatorOutputLevel(pos, world.getBlockState(pos).getBlock());
        }
    }

    // ===== 内部状态管理 =====
    private void setActiveInternal(boolean newActive) {
        if (this.active != newActive) {
            this.active = newActive;
            markDirty();
            if (!world.isRemote) {
                syncBlock();
            }
        }
    }

    private void syncBlock() {
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        world.markBlockRangeForRenderUpdate(pos, pos);
        world.checkLight(pos);
    }

    // ===== IEnergyStorage 实现 =====
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        int toReceive = Math.min(MAX_ENERGY - energyStored, Math.min(MAX_RECEIVE, maxReceive));
        if (!simulate && toReceive > 0) {
            energyStored += toReceive;
            markDirty();
        }
        return toReceive;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0; // 不允许提取
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

    // ===== Capability =====
    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(this);
        }
        return super.getCapability(capability, facing);
    }

    // ===== 公共方法 =====
    public boolean isActive() {
        return active;
    }

    public void setSpeedLevel(int level) {
        int clamped = Math.max(0, Math.min(15, level));
        if (this.speedLevel != clamped) {
            this.speedLevel = clamped;
            markDirty();
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
        markDirty();
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

    // ===== NBT 序列化 =====
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Energy", energyStored);
        compound.setBoolean("Active", active);
        compound.setInteger("Mode", mode.ordinal());
        compound.setInteger("SpeedLevel", speedLevel);
        compound.setInteger("TickCounter", tickCounter);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        energyStored = compound.getInteger("Energy");
        active = compound.getBoolean("Active");
        speedLevel = compound.getInteger("SpeedLevel");
        tickCounter = compound.getInteger("TickCounter");

        int modeOrdinal = compound.getInteger("Mode");
        if (modeOrdinal >= 0 && modeOrdinal < Mode.values().length) {
            this.mode = Mode.values()[modeOrdinal];
        }
    }

    // ===== 网络同步 =====
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setBoolean("Active", active);
        tag.setInteger("Mode", mode.ordinal());
        tag.setInteger("Energy", energyStored);
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
        active = tag.getBoolean("Active");
        energyStored = tag.getInteger("Energy");
        int modeOrdinal = tag.getInteger("Mode");
        if (modeOrdinal >= 0 && modeOrdinal < Mode.values().length) {
            this.mode = Mode.values()[modeOrdinal];
        }
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }
}