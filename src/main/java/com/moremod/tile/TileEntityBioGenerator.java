package com.moremod.tile;

import com.moremod.item.energy.ItemSpeedUpgrade;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 生物质发电机TileEntity - 使用有机物发电
 * 支持升级插件提升效率
 */
public class TileEntityBioGenerator extends TileEntity implements ITickable {

    // 能量存储
    private static final int MAX_ENERGY = 100000;
    private static final int BASE_RF_PER_TICK = 200;  // 基础 200 RF/tick (5倍)
    private static final int UPGRADE_SLOTS = 4;
    private static final float SPEED_PER_UPGRADE = 0.5f;

    private final EnergyStorageInternal energyStorage = new EnergyStorageInternal(MAX_ENERGY, 0, 5000);

    // 燃料存储 (9格) + 升级槽 (4格)
    private final ItemStackHandler fuelInventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return getFuelValue(stack) > 0;
        }
    };

    // 升级插件槽
    private final ItemStackHandler upgradeInventory = new ItemStackHandler(UPGRADE_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            cachedSpeedMultiplier = -1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidUpgrade(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private float cachedSpeedMultiplier = -1;

    // 当前燃烧进度
    private int burnTime = 0;
    private int maxBurnTime = 0;
    private boolean generating = false;

    // 燃料值映射 (RF总产出)
    private static final Map<Item, Integer> FUEL_VALUES = new HashMap<>();

    static {
        // 种子类 - 低能量
        FUEL_VALUES.put(Items.WHEAT_SEEDS, 200);
        FUEL_VALUES.put(Items.MELON_SEEDS, 200);
        FUEL_VALUES.put(Items.PUMPKIN_SEEDS, 200);
        FUEL_VALUES.put(Items.BEETROOT_SEEDS, 200);

        // 作物类 - 中等能量
        FUEL_VALUES.put(Items.WHEAT, 400);
        FUEL_VALUES.put(Items.CARROT, 400);
        FUEL_VALUES.put(Items.POTATO, 400);
        FUEL_VALUES.put(Items.BEETROOT, 400);
        FUEL_VALUES.put(Items.MELON, 300);
        FUEL_VALUES.put(Items.APPLE, 500);
        FUEL_VALUES.put(Items.REEDS, 300);

        // 树苗 - 较高能量
        FUEL_VALUES.put(Item.getItemFromBlock(Blocks.SAPLING), 800);

        // 腐肉等 - 较高能量
        FUEL_VALUES.put(Items.ROTTEN_FLESH, 600);

        // 其他植物
        FUEL_VALUES.put(Item.getItemFromBlock(Blocks.TALLGRASS), 150);
        FUEL_VALUES.put(Item.getItemFromBlock(Blocks.RED_FLOWER), 200);
        FUEL_VALUES.put(Item.getItemFromBlock(Blocks.YELLOW_FLOWER), 200);
        FUEL_VALUES.put(Item.getItemFromBlock(Blocks.VINE), 250);
        FUEL_VALUES.put(Item.getItemFromBlock(Blocks.WATERLILY), 300);
        FUEL_VALUES.put(Item.getItemFromBlock(Blocks.CACTUS), 400);
        FUEL_VALUES.put(Item.getItemFromBlock(Blocks.PUMPKIN), 600);
        FUEL_VALUES.put(Item.getItemFromBlock(Blocks.MELON_BLOCK), 800);
    }

    /**
     * 检查是否是有效的升级材料
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
     * 计算速度倍率
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

    public int getUpgradeCount() {
        int count = 0;
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            if (!upgradeInventory.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public ItemStackHandler getUpgradeInventory() {
        return upgradeInventory;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        boolean wasGenerating = generating;

        // 如果正在燃烧
        if (burnTime > 0) {
            burnTime--;
            generating = true;

            // 产生能量（应用速度倍率）
            if (energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
                float speedMultiplier = getSpeedMultiplier();
                int actualRFPerTick = (int)(BASE_RF_PER_TICK * speedMultiplier);
                int toAdd = Math.min(actualRFPerTick, energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored());
                ((EnergyStorageInternal) energyStorage).addEnergy(toAdd);
            }
        } else {
            generating = false;

            // 尝试消耗新燃料
            if (energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
                tryConsumeFuel();
            }
        }

        // 输出能量到相邻方块
        if (energyStorage.getEnergyStored() > 0) {
            outputEnergy();
        }

        // 状态变化时更新方块
        if (wasGenerating != generating) {
            markDirty();
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    /**
     * 尝试消耗燃料
     */
    private void tryConsumeFuel() {
        for (int i = 0; i < fuelInventory.getSlots(); i++) {
            ItemStack stack = fuelInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                int fuelValue = getFuelValue(stack);
                if (fuelValue > 0) {
                    // 计算燃烧时间 (fuelValue / BASE_RF_PER_TICK)
                    maxBurnTime = fuelValue / BASE_RF_PER_TICK;
                    burnTime = maxBurnTime;
                    generating = true;

                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        fuelInventory.setStackInSlot(i, ItemStack.EMPTY);
                    }
                    markDirty();
                    return;
                }
            }
        }
    }

    /**
     * 输出能量到相邻方块
     */
    private void outputEnergy() {
        for (EnumFacing facing : EnumFacing.values()) {
            TileEntity neighbor = world.getTileEntity(pos.offset(facing));
            if (neighbor != null && neighbor.hasCapability(CapabilityEnergy.ENERGY, facing.getOpposite())) {
                IEnergyStorage neighborStorage = neighbor.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite());
                if (neighborStorage != null && neighborStorage.canReceive()) {
                    int toTransfer = Math.min(1000, energyStorage.getEnergyStored());
                    int transferred = neighborStorage.receiveEnergy(toTransfer, false);
                    if (transferred > 0) {
                        ((EnergyStorageInternal) energyStorage).extractEnergyInternal(transferred);
                    }
                }
            }
        }
    }

    /**
     * 获取燃料值
     */
    public static int getFuelValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Integer value = FUEL_VALUES.get(stack.getItem());
        return value != null ? value : 0;
    }

    /**
     * 添加燃料
     */
    public ItemStack addFuel(ItemStack stack) {
        if (getFuelValue(stack) <= 0) return stack;

        ItemStack remaining = stack.copy();
        for (int i = 0; i < fuelInventory.getSlots() && !remaining.isEmpty(); i++) {
            remaining = fuelInventory.insertItem(i, remaining, false);
        }
        markDirty();
        return remaining;
    }

    /**
     * 获取燃料总数
     */
    public int getFuelCount() {
        int count = 0;
        for (int i = 0; i < fuelInventory.getSlots(); i++) {
            count += fuelInventory.getStackInSlot(i).getCount();
        }
        return count;
    }

    /**
     * 掉落库存
     */
    public void dropInventory() {
        if (world == null || world.isRemote) return;

        for (int i = 0; i < fuelInventory.getSlots(); i++) {
            ItemStack stack = fuelInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                EntityItem entityItem = new EntityItem(world,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    stack.copy());
                world.spawnEntity(entityItem);
            }
        }
    }

    // ========== Getters ==========

    public boolean isGenerating() {
        return generating;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    // ========== NBT ==========

    public int getRFPerTick() {
        if (!generating) return 0;
        return (int)(BASE_RF_PER_TICK * getSpeedMultiplier());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Fuel", fuelInventory.serializeNBT());
        compound.setTag("Upgrades", upgradeInventory.serializeNBT());
        compound.setInteger("Energy", energyStorage.getEnergyStored());
        compound.setInteger("BurnTime", burnTime);
        compound.setInteger("MaxBurnTime", maxBurnTime);
        compound.setBoolean("Generating", generating);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        fuelInventory.deserializeNBT(compound.getCompoundTag("Fuel"));
        if (compound.hasKey("Upgrades")) {
            upgradeInventory.deserializeNBT(compound.getCompoundTag("Upgrades"));
            cachedSpeedMultiplier = -1;
        }
        ((EnergyStorageInternal) energyStorage).setEnergy(compound.getInteger("Energy"));
        burnTime = compound.getInteger("BurnTime");
        maxBurnTime = compound.getInteger("MaxBurnTime");
        generating = compound.getBoolean("Generating");
    }

    // ========== Capability ==========

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return true;
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(energyStorage);
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(fuelInventory);
        }
        return super.getCapability(capability, facing);
    }

    // 内部能量存储类，允许直接修改能量
    private static class EnergyStorageInternal extends EnergyStorage {
        public EnergyStorageInternal(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0; // 不接受外部能量
        }

        public void addEnergy(int energy) {
            this.energy = Math.min(this.energy + energy, this.capacity);
        }

        public void setEnergy(int energy) {
            this.energy = Math.min(energy, this.capacity);
        }

        public void extractEnergyInternal(int energy) {
            this.energy = Math.max(0, this.energy - energy);
        }
    }
}
