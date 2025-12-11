package com.moremod.tile;

import com.moremod.init.ModFluids;
import com.moremod.init.ModItems;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 植物油壓榨機TileEntity
 *
 * 功能：
 * - 消耗RF能量
 * - 將農作物壓榨成植物油桶
 */
public class TileEntityPlantOilPress extends TileEntity implements ITickable {

    // 配置
    private static final int ENERGY_CAPACITY = 100000;     // 100k RF
    private static final int ENERGY_PER_TICK = 50;         // 每tick消耗 50 RF
    private static final int PROCESS_TIME = 8;             // 0.4秒壓榨時間（5倍加速）
    private static final int FLUID_CAPACITY = 16000;       // 16桶液體容量

    // 原料轉換率（每種作物產出多少mB植物油）- 產量5倍
    private static final Map<Item, Integer> OIL_YIELD = new HashMap<>();
    static {
        OIL_YIELD.put(Items.WHEAT, 250);          // 小麥 -> 250 mB (5倍)
        OIL_YIELD.put(Items.POTATO, 400);         // 馬鈴薯 -> 400 mB (5倍)
        OIL_YIELD.put(Items.CARROT, 300);         // 胡蘿蔔 -> 300 mB (5倍)
        OIL_YIELD.put(Items.BEETROOT, 350);       // 甜菜根 -> 350 mB (5倍)
        OIL_YIELD.put(Items.MELON, 200);          // 西瓜 -> 200 mB (5倍)
        OIL_YIELD.put(Items.PUMPKIN_SEEDS, 500);  // 南瓜籽 -> 500 mB (5倍)
        OIL_YIELD.put(Items.MELON_SEEDS, 500);    // 西瓜籽 -> 500 mB (5倍)
        OIL_YIELD.put(Items.WHEAT_SEEDS, 150);    // 小麥種子 -> 150 mB (5倍)
        OIL_YIELD.put(Items.BEETROOT_SEEDS, 200); // 甜菜根種子 -> 200 mB (5倍)
        OIL_YIELD.put(Items.APPLE, 300);          // 蘋果 -> 300 mB (5倍)
    }

    private static final int MB_PER_BUCKET = 1000;

    // 能量存儲
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, 5000, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                markDirty();
            }
            return received;
        }
    };

    // 液體儲罐 - 支持管道輸出
    private final FluidTank fluidTank = new FluidTank(FLUID_CAPACITY) {
        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return false; // 不接受外部輸入
        }

        @Override
        public boolean canDrain() {
            return true; // 允許管道抽取
        }

        @Override
        protected void onContentsChanged() {
            TileEntityPlantOilPress.this.markDirty();
            if (world != null && !world.isRemote) {
                world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
            }
        }
    };

    // 物品槽位：0=輸入（農作物），1=輸出（植物油桶）
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                return isValidInput(stack);
            }
            return false; // 輸出槽不能手動放入
        }
    };

    // 壓榨進度
    private int progress = 0;
    private boolean isProcessing = false;

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        // 檢查是否可以開始/繼續壓榨
        ItemStack input = inventory.getStackInSlot(0);
        boolean hasInput = !input.isEmpty() && isValidInput(input);
        boolean hasEnergy = energy.getEnergyStored() >= ENERGY_PER_TICK;
        int yield = hasInput ? getOilYield(input) : 0;
        boolean hasSpace = fluidTank.getFluidAmount() + yield <= fluidTank.getCapacity();

        if (hasInput && hasEnergy && hasSpace) {
            isProcessing = true;

            // 消耗能量
            energy.extractEnergy(ENERGY_PER_TICK, false);
            progress++;

            if (progress >= PROCESS_TIME) {
                // 壓榨完成 - 將油存入液體儲罐
                FluidStack oilStack = new FluidStack(ModFluids.getPlantOil(), yield);
                fluidTank.fillInternal(oilStack, true);

                input.shrink(1);
                inventory.setStackInSlot(0, input);
                progress = 0;

                // 嘗試輸出桶
                tryOutputBucket();
            }
        } else {
            isProcessing = false;
            if (progress > 0 && !hasInput) {
                progress = 0; // 沒有輸入，重置進度
            }
        }

        // 持續嘗試輸出桶
        if (fluidTank.getFluidAmount() >= MB_PER_BUCKET) {
            tryOutputBucket();
        }
    }

    private void tryOutputBucket() {
        if (fluidTank.getFluidAmount() >= MB_PER_BUCKET) {
            ItemStack output = inventory.getStackInSlot(1);
            ItemStack newBucket = new ItemStack(ModItems.PLANT_OIL_BUCKET);

            if (output.isEmpty()) {
                inventory.setStackInSlot(1, newBucket);
                fluidTank.drainInternal(MB_PER_BUCKET, true);
            } else if (output.getItem() == ModItems.PLANT_OIL_BUCKET && output.getCount() < output.getMaxStackSize()) {
                output.grow(1);
                inventory.setStackInSlot(1, output);
                fluidTank.drainInternal(MB_PER_BUCKET, true);
            }
        }
    }

    /**
     * 檢查物品是否可以壓榨
     */
    public static boolean isValidInput(ItemStack stack) {
        return OIL_YIELD.containsKey(stack.getItem());
    }

    /**
     * 獲取物品的植物油產量
     */
    public static int getOilYield(ItemStack stack) {
        return OIL_YIELD.getOrDefault(stack.getItem(), 0);
    }

    // ===== Getters =====

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return PROCESS_TIME;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    public int getFluidCapacity() {
        return fluidTank.getCapacity();
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    // ===== Capabilities =====

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY ||
               capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ||
               capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY ||
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
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) fluidTank;
        }
        return super.getCapability(capability, facing);
    }

    // ===== NBT =====

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setInteger("Energy", energy.getEnergyStored());
        compound.setInteger("Progress", progress);
        compound.setBoolean("IsProcessing", isProcessing);
        // 保存液體儲罐
        NBTTagCompound tankTag = new NBTTagCompound();
        fluidTank.writeToNBT(tankTag);
        compound.setTag("FluidTank", tankTag);
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
        progress = compound.getInteger("Progress");
        isProcessing = compound.getBoolean("IsProcessing");
        // 讀取液體儲罐
        if (compound.hasKey("FluidTank")) {
            fluidTank.readFromNBT(compound.getCompoundTag("FluidTank"));
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
