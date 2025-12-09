package com.moremod.tile;

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
    private static final int PROCESS_TIME = 200;           // 10秒壓榨時間

    // 原料轉換率（每種作物產出多少mB植物油）
    private static final Map<Item, Integer> OIL_YIELD = new HashMap<>();
    static {
        OIL_YIELD.put(Items.WHEAT, 50);           // 小麥 -> 50 mB
        OIL_YIELD.put(Items.POTATO, 80);          // 馬鈴薯 -> 80 mB
        OIL_YIELD.put(Items.CARROT, 60);          // 胡蘿蔔 -> 60 mB
        OIL_YIELD.put(Items.BEETROOT, 70);        // 甜菜根 -> 70 mB
        OIL_YIELD.put(Items.MELON, 40);           // 西瓜 -> 40 mB
        OIL_YIELD.put(Items.PUMPKIN_SEEDS, 100);  // 南瓜籽 -> 100 mB
        OIL_YIELD.put(Items.MELON_SEEDS, 100);    // 西瓜籽 -> 100 mB
        OIL_YIELD.put(Items.WHEAT_SEEDS, 30);     // 小麥種子 -> 30 mB
        OIL_YIELD.put(Items.BEETROOT_SEEDS, 40);  // 甜菜根種子 -> 40 mB
        OIL_YIELD.put(Items.APPLE, 60);           // 蘋果 -> 60 mB
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
    private int storedOilMB = 0;  // 內部儲存的植物油（mB）
    private boolean isProcessing = false;

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        // 檢查是否可以開始/繼續壓榨
        ItemStack input = inventory.getStackInSlot(0);
        boolean hasInput = !input.isEmpty() && isValidInput(input);
        boolean hasEnergy = energy.getEnergyStored() >= ENERGY_PER_TICK;

        if (hasInput && hasEnergy) {
            isProcessing = true;

            // 消耗能量
            energy.extractEnergy(ENERGY_PER_TICK, false);
            progress++;

            if (progress >= PROCESS_TIME) {
                // 壓榨完成
                int yield = getOilYield(input);
                storedOilMB += yield;
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
        if (storedOilMB >= MB_PER_BUCKET) {
            tryOutputBucket();
        }
    }

    private void tryOutputBucket() {
        if (storedOilMB >= MB_PER_BUCKET) {
            ItemStack output = inventory.getStackInSlot(1);
            ItemStack newBucket = new ItemStack(ModItems.PLANT_OIL_BUCKET);

            if (output.isEmpty()) {
                inventory.setStackInSlot(1, newBucket);
                storedOilMB -= MB_PER_BUCKET;
            } else if (output.getItem() == ModItems.PLANT_OIL_BUCKET && output.getCount() < output.getMaxStackSize()) {
                output.grow(1);
                inventory.setStackInSlot(1, output);
                storedOilMB -= MB_PER_BUCKET;
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
        compound.setInteger("Progress", progress);
        compound.setInteger("StoredOilMB", storedOilMB);
        compound.setBoolean("IsProcessing", isProcessing);
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
        storedOilMB = compound.getInteger("StoredOilMB");
        isProcessing = compound.getBoolean("IsProcessing");
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
