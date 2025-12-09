package com.moremod.tile;

import com.moremod.init.ModFluids;
import com.moremod.init.ModItems;
import com.moremod.item.energy.ItemOilBucket;
import com.moremod.item.energy.ItemPlantOilBucket;
import com.moremod.item.energy.ItemSpeedUpgrade;
import net.minecraft.init.Items;
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
 * 石油發電機TileEntity
 *
 * 功能：
 * - 燃燒石油或植物油
 * - 產生RF能量
 * - 自動輸出到相鄰機器
 * - 支持增速插件（最多4個，每個+50%速度）
 */
public class TileEntityOilGenerator extends TileEntity implements ITickable {

    // 配置
    private static final int ENERGY_CAPACITY = 1000000;    // 1M RF
    private static final int BASE_RF_PER_TICK = 200;       // 基礎每tick產生 200 RF
    private static final int UPGRADE_SLOTS = 4;            // 增速插件槽數量
    private static final float SPEED_PER_UPGRADE = 0.5f;   // 每個插件增加50%速度

    // 自訂能量存儲（對外只輸出，對內可添加）
    private final GeneratorEnergyStorage energy = new GeneratorEnergyStorage(ENERGY_CAPACITY, 10000);

    /**
     * 發電機專用能量存儲 - 對外只允許輸出，內部可添加能量
     */
    private static class GeneratorEnergyStorage extends EnergyStorage {
        public GeneratorEnergyStorage(int capacity, int maxExtract) {
            super(capacity, 0, maxExtract); // maxReceive=0 防止外部輸入
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            return extracted;
        }

        @Override
        public boolean canReceive() {
            return false; // 對外不接收
        }

        /**
         * 內部添加能量（用於發電）
         */
        public int addEnergyInternal(int amount) {
            int stored = this.energy;
            int toAdd = Math.min(amount, capacity - stored);
            if (toAdd > 0) {
                this.energy += toAdd;
            }
            return toAdd;
        }

        /**
         * 設置能量值（用於NBT載入）
         */
        public void setEnergy(int amount) {
            this.energy = Math.min(amount, capacity);
        }
    }

    // 物品槽：0=燃料槽, 1-4=增速插件槽
    private final ItemStackHandler inventory = new ItemStackHandler(1 + UPGRADE_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            if (slot > 0) {
                // 增速槽變更，重新計算速度倍率
                cachedSpeedMultiplier = -1;
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                return isValidFuel(stack);
            } else {
                // 增速插件槽：接受紅石、螢石粉、烈焰粉作為增速材料
                return isValidUpgrade(stack);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == 0) return 64;
            return 1; // 增速槽每格只能放1個
        }
    };

    // 緩存的速度倍率
    private float cachedSpeedMultiplier = -1;

    /**
     * 檢查物品是否是有效的增速材料
     */
    public static boolean isValidUpgrade(ItemStack stack) {
        // 專用增速插件
        if (stack.getItem() instanceof ItemSpeedUpgrade) {
            return true;
        }
        // 備用材料：紅石、螢石粉、烈焰粉、綠寶石
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
            for (int i = 1; i <= UPGRADE_SLOTS; i++) {
                if (!inventory.getStackInSlot(i).isEmpty()) {
                    upgradeCount++;
                }
            }
            cachedSpeedMultiplier = 1.0f + (upgradeCount * SPEED_PER_UPGRADE);
        }
        return cachedSpeedMultiplier;
    }

    // 液體燃料槽（支持管道輸入）
    private static final int FLUID_CAPACITY = 16000; // 16桶
    private final FluidTank fluidTank = new FluidTank(FLUID_CAPACITY) {
        @Override
        public int fill(FluidStack resource, boolean doFill) {
            // 基本檢查
            if (resource == null || resource.amount <= 0) return 0;
            if (resource.getFluid() == null) return 0;

            // 檢查液體是否是有效燃料
            if (!isValidFluidFuel(resource)) return 0;

            // 如果已有液體，檢查類型是否相同
            if (fluid != null && !fluid.isFluidEqual(resource)) return 0;

            // 計算可以填充的量
            int currentAmount = (fluid == null) ? 0 : fluid.amount;
            int space = capacity - currentAmount;
            int toFill = Math.min(space, resource.amount);

            if (toFill <= 0) return 0;

            if (doFill) {
                if (fluid == null) {
                    fluid = new FluidStack(resource, toFill);
                } else {
                    fluid.amount += toFill;
                }
                onContentsChanged();
            }

            return toFill;
        }

        @Override
        public boolean canFillFluidType(FluidStack fluidStack) {
            return isValidFluidFuel(fluidStack);
        }

        @Override
        public boolean canFill() {
            return true; // 允許外部填充
        }

        @Override
        public boolean canDrain() {
            return false; // 不允許抽出
        }

        @Override
        protected void onContentsChanged() {
            markDirty();
            syncToClient();
        }
    };

    private void syncToClient() {
        if (world != null && !world.isRemote) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    /**
     * 檢查液體是否是有效燃料
     */
    private static boolean isValidFluidFuel(FluidStack fluid) {
        if (fluid == null || fluid.getFluid() == null) return false;
        String fluidName = fluid.getFluid().getName().toLowerCase();
        return fluidName.contains("oil") ||           // 通用油類
               fluidName.contains("crude") ||         // 原油
               fluidName.contains("fuel") ||          // 燃料
               fluidName.contains("petroleum") ||     // 石油
               fluidName.equals("creosote");          // 木餾油
    }

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

            // 產生能量（使用內部添加方法，考慮速度倍率）
            if (energy.getEnergyStored() < energy.getMaxEnergyStored()) {
                float speedMultiplier = getSpeedMultiplier();
                int actualRFPerTick = (int)(currentRFPerTick * speedMultiplier);
                int toAdd = Math.min(actualRFPerTick, energy.getMaxEnergyStored() - energy.getEnergyStored());
                energy.addEnergyInternal(toAdd);
                markDirty();
            }

            // 粒子效果（更多插件=更多粒子）
            int particleInterval = Math.max(3, 10 - (int)(getSpeedMultiplier() * 2));
            if (tickCounter % particleInterval == 0) {
                spawnBurningParticles();
            }
        }

        // 嘗試消耗新燃料
        if (burnTime <= 0 && energy.getEnergyStored() < energy.getMaxEnergyStored()) {
            // 優先消耗物品燃料
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
            // 如果沒有物品燃料，嘗試消耗液體燃料
            else if (fluidTank.getFluidAmount() >= 1000) {
                FluidStack fluidStack = fluidTank.getFluid();
                if (fluidStack != null) {
                    FuelData fuelData = getFluidFuelData(fluidStack.getFluid());
                    if (fuelData != null) {
                        fluidTank.drainInternal(1000, true); // 消耗1桶
                        burnTime = fuelData.burnTime;
                        maxBurnTime = fuelData.burnTime;
                        currentRFPerTick = fuelData.rfPerTick;
                        markDirty();
                    }
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
     * 獲取燃料數據（物品）
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

    /**
     * 獲取燃料數據（液體）
     * 支持本模組和其他模組的油類液體
     */
    @Nullable
    public static FuelData getFluidFuelData(Fluid fluid) {
        if (fluid == null) return null;
        String name = fluid.getName().toLowerCase();

        // 原油類（高效率）
        if (name.equals("crude_oil") || name.contains("crude") || name.equals("oil") || name.contains("petroleum")) {
            return new FuelData(ItemOilBucket.BURN_TIME, ItemOilBucket.RF_PER_BUCKET / ItemOilBucket.BURN_TIME);
        }
        // 植物油類（中等效率）
        else if (name.equals("plant_oil") || name.contains("seed_oil") || name.contains("cooking")) {
            return new FuelData(ItemPlantOilBucket.BURN_TIME, ItemPlantOilBucket.RF_PER_BUCKET / ItemPlantOilBucket.BURN_TIME);
        }
        // 燃料類（高效率）
        else if (name.contains("fuel") || name.contains("diesel") || name.contains("gasoline")) {
            return new FuelData(ItemOilBucket.BURN_TIME * 2, (ItemOilBucket.RF_PER_BUCKET * 2) / (ItemOilBucket.BURN_TIME * 2));
        }
        // 木餾油（低效率）
        else if (name.equals("creosote")) {
            return new FuelData(ItemPlantOilBucket.BURN_TIME / 2, ItemPlantOilBucket.RF_PER_BUCKET / ItemPlantOilBucket.BURN_TIME / 2);
        }
        // 其他油類（通用效率）
        else if (name.contains("oil")) {
            return new FuelData(ItemOilBucket.BURN_TIME, ItemOilBucket.RF_PER_BUCKET / ItemOilBucket.BURN_TIME);
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
        if (!isBurning()) return 0;
        return (int)(currentRFPerTick * getSpeedMultiplier());
    }

    public int getBaseRFPerTick() {
        return isBurning() ? currentRFPerTick : 0;
    }

    public int getUpgradeCount() {
        int count = 0;
        for (int i = 1; i <= UPGRADE_SLOTS; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    // ===== Getters for fluid =====

    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    public int getFluidCapacity() {
        return FLUID_CAPACITY;
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    // ===== 液體處理器包裝（確保所有面都能輸入）=====

    private final IFluidHandler fluidHandlerWrapper = new IFluidHandler() {
        @Override
        public IFluidTankProperties[] getTankProperties() {
            return fluidTank.getTankProperties();
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return fluidTank.fill(resource, doFill);
        }

        @Override
        @Nullable
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return null; // 不允許抽出
        }

        @Override
        @Nullable
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return null; // 不允許抽出
        }
    };

    // ===== Capabilities =====

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        // 所有面都支持所有 capability
        if (capability == CapabilityEnergy.ENERGY) {
            return true;
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true; // 所有面都支持液體輸入
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
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) inventory;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            // 使用包裝器確保所有面都能輸入
            return (T) fluidHandlerWrapper;
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
        // 保存液體槽
        fluidTank.writeToNBT(compound);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Inventory")) {
            inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        }
        // 使用直接設置方法載入能量
        energy.setEnergy(compound.getInteger("Energy"));
        burnTime = compound.getInteger("BurnTime");
        maxBurnTime = compound.getInteger("MaxBurnTime");
        currentRFPerTick = compound.getInteger("RFPerTick");
        // 讀取液體槽
        fluidTank.readFromNBT(compound);
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
