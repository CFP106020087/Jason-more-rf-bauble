package com.moremod.block.entity;

import com.moremod.block.BloodGeneratorBlock;
import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 血液发电机BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 从沾血武器中提取能量，转换为RF
 * - 输入槽(沾血武器) -> 输出槽(干净武器)
 */
public class BloodGeneratorBlockEntity extends BlockEntity implements MenuProvider {

    // 配置
    private static final int MAX_ENERGY = 5000000;      // 500万RF容量
    private static final int MAX_OUTPUT = 50000;        // 每tick最大输出
    private static final int EXTRACTION_RATE = 10000;   // 每tick提取血液能量
    private static final int CONVERSION_EFFICIENCY = 100; // 转换效率 100%

    // 能量存储
    private final ModifiableEnergyStorage energyStorage = new ModifiableEnergyStorage(MAX_ENERGY, 0, MAX_OUTPUT);
    private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> energyStorage);

    // 物品存储: 0=输入槽(沾血武器), 1=输出槽(干净武器)
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                syncToClient();
            }
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (slot == 0) {
                // 输入槽：接受武器类物品
                return isValidWeapon(stack);
            }
            return true; // 输出槽允许任何（用于内部移动）
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1; // 每槽只能放一个物品
        }
    };

    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    // 运行状态
    private boolean isActive = false;
    private int currentBloodEnergy = 0;
    private int currentFleshChunks = 0;
    private int totalEnergyToExtract = 0;
    private int extractedEnergy = 0;

    public BloodGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLOOD_GENERATOR.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        boolean wasActive = isActive;

        // 尝试输出能量到相邻方块
        if (energyStorage.getEnergyStored() > 0) {
            outputEnergy();
        }

        // 检查是否有正在处理的物品
        if (totalEnergyToExtract > 0 && extractedEnergy < totalEnergyToExtract) {
            processExtraction();
            isActive = true;
        } else if (!inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(1).isEmpty()) {
            startExtraction();
        } else {
            isActive = false;
        }

        // 更新方块状态
        if (wasActive != isActive) {
            BloodGeneratorBlock.setActiveState(level, worldPosition, isActive);
            setChanged();
        }
    }

    /**
     * 检查是否是有效武器
     */
    public static boolean isValidWeapon(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
    }

    /**
     * 检查武器是否有血液数据
     */
    public static boolean hasBloodData(ItemStack stack) {
        if (stack.isEmpty()) return false;
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        return tag.contains("BloodEnergy") || tag.contains("FleshChunks") || tag.contains("BossKills");
    }

    /**
     * 获取血液能量
     */
    public static int getBloodEnergy(ItemStack stack) {
        if (stack.isEmpty() || stack.getTag() == null) return 0;
        return stack.getTag().getInt("BloodEnergy");
    }

    /**
     * 获取肉块数量
     */
    public static int getFleshChunks(ItemStack stack) {
        if (stack.isEmpty() || stack.getTag() == null) return 0;
        return stack.getTag().getInt("FleshChunks");
    }

    /**
     * 获取Boss击杀数
     */
    public static int getBossKills(ItemStack stack) {
        if (stack.isEmpty() || stack.getTag() == null) return 0;
        return stack.getTag().getInt("BossKills");
    }

    /**
     * 清除血液数据
     */
    public static void clearBloodData(ItemStack stack) {
        if (stack.isEmpty() || stack.getTag() == null) return;
        CompoundTag tag = stack.getTag();
        tag.remove("BloodEnergy");
        tag.remove("FleshChunks");
        tag.remove("BossKills");
    }

    private void startExtraction() {
        ItemStack inputStack = inventory.getStackInSlot(0);
        if (inputStack.isEmpty() || !hasBloodData(inputStack)) {
            return;
        }

        currentBloodEnergy = getBloodEnergy(inputStack);
        currentFleshChunks = getFleshChunks(inputStack);
        int bossKills = getBossKills(inputStack);

        // 肉块能量值
        int fleshEnergy = 500;
        totalEnergyToExtract = currentBloodEnergy +
                (currentFleshChunks * fleshEnergy) +
                (bossKills * 50000);
        extractedEnergy = 0;

        isActive = true;
        setChanged();
        syncToClient();
    }

    private void processExtraction() {
        int spaceAvailable = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        if (spaceAvailable <= 0) return;

        int toExtract = Math.min(EXTRACTION_RATE, totalEnergyToExtract - extractedEnergy);
        toExtract = Math.min(toExtract, spaceAvailable);

        if (toExtract > 0) {
            int rfGenerated = (toExtract * CONVERSION_EFFICIENCY) / 100;
            energyStorage.addEnergy(rfGenerated);
            extractedEnergy += toExtract;
        }

        if (extractedEnergy >= totalEnergyToExtract) {
            finishExtraction();
        }

        setChanged();
    }

    private void finishExtraction() {
        ItemStack inputStack = inventory.getStackInSlot(0);
        if (!inputStack.isEmpty()) {
            clearBloodData(inputStack);
            inventory.setStackInSlot(1, inputStack.copy());
            inventory.setStackInSlot(0, ItemStack.EMPTY);
        }

        currentBloodEnergy = 0;
        currentFleshChunks = 0;
        totalEnergyToExtract = 0;
        extractedEnergy = 0;
        isActive = false;

        setChanged();
        syncToClient();
    }

    private void outputEnergy() {
        int toOutput = Math.min(MAX_OUTPUT, energyStorage.getEnergyStored());
        if (toOutput <= 0) return;

        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor != null) {
                neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(storage -> {
                    if (storage.canReceive()) {
                        int accepted = storage.receiveEnergy(
                                Math.min(toOutput, energyStorage.getEnergyStored()), false);
                        if (accepted > 0) {
                            energyStorage.removeEnergy(accepted);
                        }
                    }
                });
            }
        }
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ===== Getters =====

    public boolean isActive() { return isActive; }
    public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    public int getMaxEnergy() { return MAX_ENERGY; }
    public int getTotalEnergyToExtract() { return totalEnergyToExtract; }
    public int getExtractedEnergy() { return extractedEnergy; }

    public float getProgress() {
        if (totalEnergyToExtract <= 0) return 0;
        return (float) extractedEnergy / totalEnergyToExtract;
    }

    // ===== MenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.moremod.blood_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // TODO: 返回BloodGeneratorMenu实例
        return null;
    }

    // ===== Capabilities =====

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
        energyHandler.invalidate();
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putBoolean("Active", isActive);
        tag.putInt("CurrentBlood", currentBloodEnergy);
        tag.putInt("CurrentFlesh", currentFleshChunks);
        tag.putInt("TotalEnergy", totalEnergyToExtract);
        tag.putInt("Extracted", extractedEnergy);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        energyStorage.setEnergy(tag.getInt("Energy"));
        isActive = tag.getBoolean("Active");
        currentBloodEnergy = tag.getInt("CurrentBlood");
        currentFleshChunks = tag.getInt("CurrentFlesh");
        totalEnergyToExtract = tag.getInt("TotalEnergy");
        extractedEnergy = tag.getInt("Extracted");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    // ===== 内部可修改能量存储 =====
    private static class ModifiableEnergyStorage extends EnergyStorage {
        public ModifiableEnergyStorage(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        public void setEnergy(int energy) {
            this.energy = Math.min(energy, this.capacity);
        }

        public void addEnergy(int amount) {
            this.energy = Math.min(this.energy + amount, this.capacity);
        }

        public void removeEnergy(int amount) {
            this.energy = Math.max(0, this.energy - amount);
        }
    }
}
