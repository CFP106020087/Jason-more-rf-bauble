package com.moremod.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/**
 * 能量链接器 TileEntity
 *
 * 功能：
 * - 与另一个能量链接器配对，无线传输能量
 * - 输入模式：从周围抽取能量，发送到配对的链接器
 * - 输出模式：接收配对链接器的能量，输出到周围机器
 * - 跨维度传输（需要末影珍珠绑定）
 * - 传输效率：95%（5%损耗）
 */
public class TileEntityEnergyLink extends TileEntity implements ITickable {

    // 配置
    private static final int ENERGY_CAPACITY = 1000000;      // 1M RF 缓存
    private static final int MAX_TRANSFER = 50000;           // 每tick最大传输 50k RF
    private static final float TRANSFER_EFFICIENCY = 0.95f;  // 95% 效率

    // 模式
    public enum LinkMode {
        INPUT,   // 输入模式：抽取周围能量，发送给配对
        OUTPUT   // 输出模式：接收配对能量，输出到周围
    }

    // 能量存储
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, MAX_TRANSFER, MAX_TRANSFER) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                markDirty();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                markDirty();
            }
            return extracted;
        }
    };

    // 配对信息
    private BlockPos linkedPos = null;
    private int linkedDimension = 0;
    private LinkMode mode = LinkMode.INPUT;
    private int tickCounter = 0;

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        tickCounter++;

        // 每tick处理能量传输
        if (mode == LinkMode.INPUT) {
            // 输入模式：从周围抽取能量
            pullEnergyFromNeighbors();
            // 发送到配对的链接器
            if (tickCounter % 2 == 0) {
                sendEnergyToLinked();
            }
        } else {
            // 输出模式：向周围输出能量
            pushEnergyToNeighbors();
        }
    }

    /**
     * 从周围方块抽取能量
     */
    private void pullEnergyFromNeighbors() {
        int spaceAvailable = energy.getMaxEnergyStored() - energy.getEnergyStored();
        if (spaceAvailable <= 0) return;

        int toPull = Math.min(MAX_TRANSFER, spaceAvailable);

        for (EnumFacing facing : EnumFacing.values()) {
            if (toPull <= 0) break;

            TileEntity neighbor = world.getTileEntity(pos.offset(facing));
            if (neighbor != null && neighbor.hasCapability(CapabilityEnergy.ENERGY, facing.getOpposite())) {
                IEnergyStorage neighborEnergy = neighbor.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
                if (neighborEnergy != null && neighborEnergy.canExtract()) {
                    int extracted = neighborEnergy.extractEnergy(toPull, false);
                    if (extracted > 0) {
                        energy.receiveEnergy(extracted, false);
                        toPull -= extracted;
                    }
                }
            }
        }
    }

    /**
     * 向周围方块输出能量
     */
    private void pushEnergyToNeighbors() {
        int stored = energy.getEnergyStored();
        if (stored <= 0) return;

        int toPush = Math.min(MAX_TRANSFER, stored);

        for (EnumFacing facing : EnumFacing.values()) {
            if (toPush <= 0) break;

            TileEntity neighbor = world.getTileEntity(pos.offset(facing));
            if (neighbor != null && neighbor.hasCapability(CapabilityEnergy.ENERGY, facing.getOpposite())) {
                IEnergyStorage neighborEnergy = neighbor.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
                if (neighborEnergy != null && neighborEnergy.canReceive()) {
                    int accepted = neighborEnergy.receiveEnergy(toPush, false);
                    if (accepted > 0) {
                        extractEnergyInternal(accepted);
                        toPush -= accepted;
                    }
                }
            }
        }
    }

    /**
     * 发送能量到配对的链接器
     */
    private void sendEnergyToLinked() {
        if (linkedPos == null) return;
        if (energy.getEnergyStored() <= 0) return;

        // 获取配对的链接器
        TileEntityEnergyLink linkedTile = getLinkedTile();
        if (linkedTile == null || linkedTile.mode != LinkMode.OUTPUT) return;

        // 计算可传输的能量（考虑效率损耗）
        int toSend = Math.min(MAX_TRANSFER, energy.getEnergyStored());
        int actualReceived = (int)(toSend * TRANSFER_EFFICIENCY);

        // 尝试传输
        int spaceAvailable = linkedTile.getSpaceAvailable();
        int actualTransfer = Math.min(actualReceived, spaceAvailable);

        if (actualTransfer > 0) {
            // 消耗能量（消耗完整量，但只传输效率百分比）
            int consumed = (int)(actualTransfer / TRANSFER_EFFICIENCY);
            extractEnergyInternal(consumed);
            linkedTile.receiveEnergyInternal(actualTransfer);
        }
    }

    /**
     * 获取配对的链接器
     */
    @Nullable
    private TileEntityEnergyLink getLinkedTile() {
        if (linkedPos == null || world == null) return null;

        // 同维度
        if (linkedDimension == world.provider.getDimension()) {
            TileEntity te = world.getTileEntity(linkedPos);
            if (te instanceof TileEntityEnergyLink) {
                return (TileEntityEnergyLink) te;
            }
        } else {
            // 跨维度
            net.minecraft.world.WorldServer linkedWorld = world.getMinecraftServer().getWorld(linkedDimension);
            if (linkedWorld != null) {
                TileEntity te = linkedWorld.getTileEntity(linkedPos);
                if (te instanceof TileEntityEnergyLink) {
                    return (TileEntityEnergyLink) te;
                }
            }
        }
        return null;
    }

    /**
     * 设置配对位置
     */
    public boolean setLinkedPos(BlockPos targetPos, int targetDimension) {
        // 不能链接自己
        if (targetPos.equals(pos) && targetDimension == world.provider.getDimension()) {
            return false;
        }

        this.linkedPos = targetPos;
        this.linkedDimension = targetDimension;
        markDirty();
        return true;
    }

    /**
     * 清除配对
     */
    public void clearLink() {
        this.linkedPos = null;
        this.linkedDimension = 0;
        markDirty();
    }

    /**
     * 切换模式
     */
    public void toggleMode() {
        mode = (mode == LinkMode.INPUT) ? LinkMode.OUTPUT : LinkMode.INPUT;
        markDirty();
        // 同步到客户端
        if (world != null && !world.isRemote) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    /**
     * 内部接收能量
     */
    public void receiveEnergyInternal(int amount) {
        energy.receiveEnergy(amount, false);
        markDirty();
    }

    /**
     * 内部提取能量
     */
    private void extractEnergyInternal(int amount) {
        try {
            java.lang.reflect.Field field = EnergyStorage.class.getDeclaredField("energy");
            field.setAccessible(true);
            int stored = energy.getEnergyStored();
            field.setInt(energy, Math.max(0, stored - amount));
        } catch (Exception ignored) {}
        markDirty();
    }

    /**
     * 获取可用空间
     */
    public int getSpaceAvailable() {
        return energy.getMaxEnergyStored() - energy.getEnergyStored();
    }

    // ===== Getters =====

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    public LinkMode getMode() {
        return mode;
    }

    public boolean isLinked() {
        return linkedPos != null;
    }

    public BlockPos getLinkedPos() {
        return linkedPos;
    }

    public int getLinkedDimension() {
        return linkedDimension;
    }

    /**
     * 客户端设置能量
     */
    public void setClientEnergy(int value) {
        try {
            java.lang.reflect.Field field = EnergyStorage.class.getDeclaredField("energy");
            field.setAccessible(true);
            field.setInt(energy, value);
        } catch (Exception ignored) {}
    }

    // ===== Capabilities =====

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return (T) energy;
        }
        return super.getCapability(capability, facing);
    }

    // ===== NBT =====

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Energy", energy.getEnergyStored());
        compound.setInteger("Mode", mode.ordinal());

        if (linkedPos != null) {
            compound.setInteger("LinkedX", linkedPos.getX());
            compound.setInteger("LinkedY", linkedPos.getY());
            compound.setInteger("LinkedZ", linkedPos.getZ());
            compound.setInteger("LinkedDim", linkedDimension);
            compound.setBoolean("HasLink", true);
        } else {
            compound.setBoolean("HasLink", false);
        }

        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        setClientEnergy(compound.getInteger("Energy"));
        mode = LinkMode.values()[compound.getInteger("Mode") % LinkMode.values().length];

        if (compound.getBoolean("HasLink")) {
            linkedPos = new BlockPos(
                compound.getInteger("LinkedX"),
                compound.getInteger("LinkedY"),
                compound.getInteger("LinkedZ")
            );
            linkedDimension = compound.getInteger("LinkedDim");
        } else {
            linkedPos = null;
        }
    }

    // ===== 网络同步 =====

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }
}
