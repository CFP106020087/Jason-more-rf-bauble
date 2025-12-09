package com.moremod.tile;

import com.moremod.init.ModItems;
import com.moremod.item.energy.ItemOilProspector;
import com.moremod.multiblock.MultiblockOilExtractor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;

import javax.annotation.Nullable;

/**
 * 抽油機核心TileEntity
 *
 * 功能：
 * - 消耗RF能量
 * - 從區塊石油礦脈中提取石油
 * - 儲存石油並轉換為石油桶
 */
public class TileEntityOilExtractorCore extends TileEntity implements ITickable {

    // 配置
    private static final int ENERGY_CAPACITY = 500000;     // 500k RF
    private static final int ENERGY_PER_TICK = 100;        // 每tick消耗 100 RF
    private static final int OIL_PER_TICK = 10;            // 每tick提取 10 mB
    private static final int MAX_OIL_STORAGE = 16000;      // 內部儲油 16000 mB (16桶)
    private static final int MB_PER_BUCKET = 1000;         // 1桶 = 1000 mB

    // 能量存儲
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, 10000, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                markDirty();
            }
            return received;
        }
    };

    // 石油儲存
    private int storedOil = 0;           // 內部儲油量 (mB)
    private int extractedTotal = 0;      // 已從礦脈提取的總量
    private boolean isRunning = false;
    private int tickCounter = 0;

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        tickCounter++;

        // 每10tick檢測一次結構
        if (tickCounter % 10 == 0) {
            boolean structureValid = MultiblockOilExtractor.checkStructure(world, pos);
            boolean hasOil = hasOilInChunk();
            boolean hasEnergy = energy.getEnergyStored() >= ENERGY_PER_TICK;
            boolean hasStorageSpace = storedOil < MAX_OIL_STORAGE;

            isRunning = structureValid && hasOil && hasEnergy && hasStorageSpace && getRemainingOil() > 0;
        }

        if (isRunning) {
            // 消耗能量
            if (energy.extractEnergy(ENERGY_PER_TICK, false) >= ENERGY_PER_TICK) {
                // 提取石油
                int canExtract = Math.min(OIL_PER_TICK, MAX_OIL_STORAGE - storedOil);
                int remaining = getRemainingOil();
                int actualExtract = Math.min(canExtract, remaining);

                if (actualExtract > 0) {
                    storedOil += actualExtract;
                    extractedTotal += actualExtract;
                    markDirty();

                    // 粒子效果
                    if (tickCounter % 20 == 0) {
                        spawnExtractionParticles();
                    }
                } else {
                    isRunning = false;
                }
            }
        }
    }

    /**
     * 檢查當前區塊是否有石油
     */
    private boolean hasOilInChunk() {
        ChunkPos chunkPos = new ChunkPos(pos);
        ItemOilProspector.OilVeinData data = ItemOilProspector.getOilVeinData(world, chunkPos);
        return data.hasOil;
    }

    /**
     * 獲取區塊剩餘石油量
     */
    public int getRemainingOil() {
        ChunkPos chunkPos = new ChunkPos(pos);
        ItemOilProspector.OilVeinData data = ItemOilProspector.getOilVeinData(world, chunkPos);
        if (!data.hasOil) return 0;
        return Math.max(0, data.amount - extractedTotal);
    }

    /**
     * 獲取可提取的桶數
     */
    public int getAvailableBuckets() {
        return storedOil / MB_PER_BUCKET;
    }

    /**
     * 提取一桶石油
     */
    public ItemStack extractOilBucket() {
        if (storedOil >= MB_PER_BUCKET) {
            storedOil -= MB_PER_BUCKET;
            markDirty();
            return new ItemStack(ModItems.CRUDE_OIL_BUCKET);
        }
        return ItemStack.EMPTY;
    }

    private void spawnExtractionParticles() {
        for (int i = 0; i < 5; i++) {
            double x = pos.getX() + 0.5 + (world.rand.nextDouble() - 0.5) * 0.5;
            double y = pos.getY() + 1.5 + world.rand.nextDouble();
            double z = pos.getZ() + 0.5 + (world.rand.nextDouble() - 0.5) * 0.5;
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0, 0.05, 0);
        }
    }

    // ===== Getters =====

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    public int getStoredOil() {
        return storedOil;
    }

    public int getMaxOilStorage() {
        return MAX_OIL_STORAGE;
    }

    public boolean isRunning() {
        return isRunning;
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
        compound.setInteger("StoredOil", storedOil);
        compound.setInteger("ExtractedTotal", extractedTotal);
        compound.setBoolean("IsRunning", isRunning);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        int fe = compound.getInteger("Energy");
        while (energy.getEnergyStored() < fe && energy.receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        storedOil = compound.getInteger("StoredOil");
        extractedTotal = compound.getInteger("ExtractedTotal");
        isRunning = compound.getBoolean("IsRunning");
    }

    // ===== 網絡同步 =====

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
