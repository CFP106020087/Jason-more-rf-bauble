package com.moremod.tile;

import com.moremod.init.ModFluids;
import com.moremod.init.ModItems;
import com.moremod.item.energy.ItemOilProspector;
import com.moremod.item.energy.ItemSpeedUpgrade;
import com.moremod.multiblock.MultiblockOilExtractor;
import net.minecraft.init.Items;
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
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/**
 * 抽油機核心TileEntity
 *
 * 功能：
 * - 消耗RF能量
 * - 從區塊石油礦脈中提取石油
 * - 儲存石油並轉換為石油桶
 * - 支持增速插件（最多4個，每個+50%速度）
 */
public class TileEntityOilExtractorCore extends TileEntity implements ITickable {

    // 配置
    private static final int ENERGY_CAPACITY = 500000;     // 500k RF
    private static final int ENERGY_PER_TICK = 100;        // 每tick消耗 100 RF
    private static final int BASE_OIL_PER_TICK = 30;       // 基礎每tick提取 30 mB (3倍加速)
    private static final int MAX_OIL_STORAGE = 16000;      // 內部儲油 16000 mB (16桶)
    private static final int MB_PER_BUCKET = 1000;         // 1桶 = 1000 mB
    private static final int UPGRADE_SLOTS = 4;            // 增速插件槽數量
    private static final float SPEED_PER_UPGRADE = 0.5f;   // 每個插件增加50%速度

    // 能量存儲 (maxExtract 需要 > 0 才能內部消耗能量)
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, 10000, ENERGY_PER_TICK * 2) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                markDirty();
            }
            return received;
        }
    };

    // 增速插件槽
    private final ItemStackHandler upgradeInventory = new ItemStackHandler(UPGRADE_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            cachedSpeedMultiplier = -1; // 重新計算速度倍率
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidUpgrade(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1; // 每格只能放1個
        }
    };

    // 緩存的速度倍率
    private float cachedSpeedMultiplier = -1;

    /**
     * 檢查物品是否是有效的增速材料
     */
    public static boolean isValidUpgrade(ItemStack stack) {
        if (stack.getItem() instanceof ItemSpeedUpgrade) {
            return true;
        }
        return stack.getItem() == Items.REDSTONE ||
               stack.getItem() == Items.GLOWSTONE_DUST ||
               stack.getItem() == Items.BLAZE_POWDER ||
               stack.getItem() == Items.EMERALD;
    }

    /**
     * 計算當前速度倍率
     */
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

    // 石油液體槽（支持管道抽取）
    private final FluidTank fluidTank = new FluidTank(MAX_OIL_STORAGE) {
        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return false; // 不接受外部輸入
        }

        @Override
        public boolean canDrain() {
            return true; // 允許抽取
        }

        @Override
        protected void onContentsChanged() {
            markDirty();
        }
    };

    // 石油儲存（兼容舊版，實際使用 fluidTank）
    private int storedOil = 0;           // 內部儲油量 (mB) - 僅用於遷移
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
            int currentFluid = fluidTank.getFluidAmount();
            boolean hasStorageSpace = currentFluid < MAX_OIL_STORAGE;

            isRunning = structureValid && hasOil && hasEnergy && hasStorageSpace && getRemainingOil() > 0;
        }

        if (isRunning) {
            // 計算實際能量消耗（速度越快，消耗越多）
            float speedMultiplier = getSpeedMultiplier();
            int actualEnergyPerTick = (int)(ENERGY_PER_TICK * speedMultiplier);

            // 消耗能量
            if (energy.extractEnergy(actualEnergyPerTick, false) >= actualEnergyPerTick) {
                // 提取石油到液體槽（應用速度倍率）
                int currentFluid = fluidTank.getFluidAmount();
                int oilPerTick = (int)(BASE_OIL_PER_TICK * speedMultiplier);
                int canExtract = Math.min(oilPerTick, MAX_OIL_STORAGE - currentFluid);
                int remaining = getRemainingOil();
                int actualExtract = Math.min(canExtract, remaining);

                if (actualExtract > 0) {
                    // 填充液體槽
                    Fluid crudeOil = ModFluids.getCrudeOil();
                    if (crudeOil != null) {
                        fluidTank.fillInternal(new FluidStack(crudeOil, actualExtract), true);
                    }
                    extractedTotal += actualExtract;
                    markDirty();

                    // 粒子效果（速度越快，粒子越多）
                    int particleInterval = Math.max(5, 20 - (int)(speedMultiplier * 5));
                    if (tickCounter % particleInterval == 0) {
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
        return fluidTank.getFluidAmount() / MB_PER_BUCKET;
    }

    /**
     * 提取一桶石油（手動右鍵）
     */
    public ItemStack extractOilBucket() {
        if (fluidTank.getFluidAmount() >= MB_PER_BUCKET) {
            fluidTank.drainInternal(MB_PER_BUCKET, true);
            markDirty();
            return new ItemStack(ModItems.CRUDE_OIL_BUCKET);
        }
        return ItemStack.EMPTY;
    }

    /**
     * 獲取液體槽（供管道系統使用）
     */
    public FluidTank getFluidTank() {
        return fluidTank;
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
        return fluidTank.getFluidAmount();
    }

    public int getMaxOilStorage() {
        return MAX_OIL_STORAGE;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getUpgradeCount() {
        int count = 0;
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            if (!upgradeInventory.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public int getOilPerTick() {
        return (int)(BASE_OIL_PER_TICK * getSpeedMultiplier());
    }

    // ===== Capabilities =====

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return true;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return (T) energy;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) fluidTank;
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) upgradeInventory;
        }
        return super.getCapability(capability, facing);
    }

    // ===== NBT =====

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Energy", energy.getEnergyStored());
        compound.setInteger("ExtractedTotal", extractedTotal);
        compound.setBoolean("IsRunning", isRunning);
        // 保存液體槽
        fluidTank.writeToNBT(compound);
        // 保存增速插件
        compound.setTag("UpgradeInventory", upgradeInventory.serializeNBT());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        int fe = compound.getInteger("Energy");
        while (energy.getEnergyStored() < fe && energy.receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        extractedTotal = compound.getInteger("ExtractedTotal");
        isRunning = compound.getBoolean("IsRunning");
        // 讀取液體槽
        fluidTank.readFromNBT(compound);
        // 讀取增速插件
        if (compound.hasKey("UpgradeInventory")) {
            upgradeInventory.deserializeNBT(compound.getCompoundTag("UpgradeInventory"));
            cachedSpeedMultiplier = -1; // 重新計算
        }

        // 兼容舊版：如果有 StoredOil，遷移到液體槽
        if (compound.hasKey("StoredOil")) {
            int oldOil = compound.getInteger("StoredOil");
            if (oldOil > 0 && fluidTank.getFluidAmount() == 0) {
                Fluid crudeOil = ModFluids.getCrudeOil();
                if (crudeOil != null) {
                    fluidTank.fillInternal(new FluidStack(crudeOil, oldOil), true);
                }
            }
        }
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
