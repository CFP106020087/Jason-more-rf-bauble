package com.moremod.tile;

import com.moremod.init.ModItems;
import com.moremod.item.energy.ItemOilBucket;
import com.moremod.item.energy.ItemPlantOilBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/**
 * 石油發電機TileEntity
 *
 * 功能：
 * - 燃燒石油或植物油
 * - 產生RF能量
 * - 自動輸出到相鄰機器
 */
public class TileEntityOilGenerator extends TileEntity implements ITickable {

    // 配置
    private static final int ENERGY_CAPACITY = 1000000;    // 1M RF
    private static final int RF_PER_TICK = 200;            // 每tick產生 200 RF

    // 能量存儲（可輸出但不接收）
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, 0, 10000) {
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                markDirty();
            }
            return extracted;
        }

        // 允許內部添加能量
        public int addEnergy(int amount, boolean simulate) {
            int stored = getEnergyStored();
            int toAdd = Math.min(amount, getMaxEnergyStored() - stored);
            if (toAdd > 0 && !simulate) {
                this.receiveEnergy(toAdd, false);
            }
            return toAdd;
        }
    };

    // 燃料槽
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidFuel(stack);
        }
    };

    // 燃燒狀態
    private int burnTime = 0;
    private int maxBurnTime = 0;
    private int currentRFPerTick = 0;
    private int tickCounter = 0;

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        tickCounter++;

        boolean wasBurning = isBurning();

        // 如果正在燃燒
        if (burnTime > 0) {
            burnTime--;

            // 產生能量
            if (energy.getEnergyStored() < energy.getMaxEnergyStored()) {
                int toAdd = Math.min(currentRFPerTick, energy.getMaxEnergyStored() - energy.getEnergyStored());
                energy.receiveEnergy(toAdd, false);
            }

            // 粒子效果
            if (tickCounter % 10 == 0) {
                spawnBurningParticles();
            }
        }

        // 嘗試消耗新燃料
        if (burnTime <= 0 && energy.getEnergyStored() < energy.getMaxEnergyStored()) {
            ItemStack fuel = inventory.getStackInSlot(0);
            if (!fuel.isEmpty() && isValidFuel(fuel)) {
                FuelData fuelData = getFuelData(fuel);
                if (fuelData != null) {
                    burnTime = fuelData.burnTime;
                    maxBurnTime = fuelData.burnTime;
                    currentRFPerTick = fuelData.rfPerTick;
                    fuel.shrink(1);
                    inventory.setStackInSlot(0, fuel);
                    markDirty();
                }
            }
        }

        // 自動輸出能量到相鄰方塊
        if (energy.getEnergyStored() > 0) {
            pushEnergyToNeighbors();
        }

        // 同步客戶端
        if (wasBurning != isBurning()) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    private void pushEnergyToNeighbors() {
        for (EnumFacing facing : EnumFacing.values()) {
            TileEntity neighbor = world.getTileEntity(pos.offset(facing));
            if (neighbor != null && neighbor.hasCapability(CapabilityEnergy.ENERGY, facing.getOpposite())) {
                IEnergyStorage neighborEnergy = neighbor.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
                if (neighborEnergy != null && neighborEnergy.canReceive()) {
                    int toTransfer = Math.min(energy.getEnergyStored(), 10000); // 每次最多傳輸 10k
                    int accepted = neighborEnergy.receiveEnergy(toTransfer, false);
                    if (accepted > 0) {
                        energy.extractEnergy(accepted, false);
                    }
                }
            }
        }
    }

    private void spawnBurningParticles() {
        double x = pos.getX() + 0.5 + (world.rand.nextDouble() - 0.5) * 0.3;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5 + (world.rand.nextDouble() - 0.5) * 0.3;
        world.spawnParticle(EnumParticleTypes.FLAME, x, y, z, 0, 0.05, 0);
        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y + 0.2, z, 0, 0.02, 0);
    }

    /**
     * 檢查物品是否是有效燃料
     */
    public static boolean isValidFuel(ItemStack stack) {
        return stack.getItem() == ModItems.CRUDE_OIL_BUCKET ||
               stack.getItem() == ModItems.PLANT_OIL_BUCKET;
    }

    /**
     * 獲取燃料數據
     */
    @Nullable
    public static FuelData getFuelData(ItemStack stack) {
        if (stack.getItem() == ModItems.CRUDE_OIL_BUCKET) {
            return new FuelData(ItemOilBucket.BURN_TIME, ItemOilBucket.RF_PER_BUCKET / ItemOilBucket.BURN_TIME);
        } else if (stack.getItem() == ModItems.PLANT_OIL_BUCKET) {
            return new FuelData(ItemPlantOilBucket.BURN_TIME, ItemPlantOilBucket.RF_PER_BUCKET / ItemPlantOilBucket.BURN_TIME);
        }
        return null;
    }

    public static class FuelData {
        public final int burnTime;
        public final int rfPerTick;

        public FuelData(int burnTime, int rfPerTick) {
            this.burnTime = burnTime;
            this.rfPerTick = rfPerTick;
        }
    }

    // ===== Getters =====

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    public int getRFPerTick() {
        return isBurning() ? currentRFPerTick : 0;
    }

    // ===== Capabilities =====

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY ||
               capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ||
               super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return (T) energy;
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) inventory;
        }
        return super.getCapability(capability, facing);
    }

    // ===== NBT =====

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setInteger("Energy", energy.getEnergyStored());
        compound.setInteger("BurnTime", burnTime);
        compound.setInteger("MaxBurnTime", maxBurnTime);
        compound.setInteger("RFPerTick", currentRFPerTick);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Inventory")) {
            inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        }
        int fe = compound.getInteger("Energy");
        while (energy.getEnergyStored() < fe && energy.receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        burnTime = compound.getInteger("BurnTime");
        maxBurnTime = compound.getInteger("MaxBurnTime");
        currentRFPerTick = compound.getInteger("RFPerTick");
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
