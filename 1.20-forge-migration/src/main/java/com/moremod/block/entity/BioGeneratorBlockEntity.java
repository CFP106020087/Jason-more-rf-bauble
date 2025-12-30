package com.moremod.block.entity;

import com.moremod.block.BioGeneratorBlock;
import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * 生物质发电机BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 使用有机物发电
 * - 产出40 RF/t
 * - 容量50,000 RF
 */
public class BioGeneratorBlockEntity extends BaseEnergyBlockEntity {

    private static final int MAX_ENERGY = 50000;
    private static final int RF_PER_TICK = 40;
    private static final int MAX_EXTRACT = 1000;
    private static final int FUEL_SLOTS = 9;

    // 燃料值映射 (RF总产出)
    private static final Map<Item, Integer> FUEL_VALUES = new HashMap<>();

    static {
        // 种子类 - 低能量
        FUEL_VALUES.put(Items.WHEAT_SEEDS, 200);
        FUEL_VALUES.put(Items.MELON_SEEDS, 200);
        FUEL_VALUES.put(Items.PUMPKIN_SEEDS, 200);
        FUEL_VALUES.put(Items.BEETROOT_SEEDS, 200);
        FUEL_VALUES.put(Items.TORCHFLOWER_SEEDS, 200);
        FUEL_VALUES.put(Items.PITCHER_POD, 200);

        // 作物类 - 中等能量
        FUEL_VALUES.put(Items.WHEAT, 400);
        FUEL_VALUES.put(Items.CARROT, 400);
        FUEL_VALUES.put(Items.POTATO, 400);
        FUEL_VALUES.put(Items.BEETROOT, 400);
        FUEL_VALUES.put(Items.MELON_SLICE, 300);
        FUEL_VALUES.put(Items.APPLE, 500);
        FUEL_VALUES.put(Items.SUGAR_CANE, 300);
        FUEL_VALUES.put(Items.SWEET_BERRIES, 250);
        FUEL_VALUES.put(Items.GLOW_BERRIES, 300);

        // 树苗 - 较高能量
        FUEL_VALUES.put(Items.OAK_SAPLING, 800);
        FUEL_VALUES.put(Items.SPRUCE_SAPLING, 800);
        FUEL_VALUES.put(Items.BIRCH_SAPLING, 800);
        FUEL_VALUES.put(Items.JUNGLE_SAPLING, 800);
        FUEL_VALUES.put(Items.ACACIA_SAPLING, 800);
        FUEL_VALUES.put(Items.DARK_OAK_SAPLING, 800);
        FUEL_VALUES.put(Items.MANGROVE_PROPAGULE, 800);
        FUEL_VALUES.put(Items.CHERRY_SAPLING, 800);

        // 腐肉等 - 较高能量
        FUEL_VALUES.put(Items.ROTTEN_FLESH, 600);

        // 其他植物
        FUEL_VALUES.put(Items.SHORT_GRASS, 150);
        FUEL_VALUES.put(Items.TALL_GRASS, 200);
        FUEL_VALUES.put(Items.POPPY, 200);
        FUEL_VALUES.put(Items.DANDELION, 200);
        FUEL_VALUES.put(Items.VINE, 250);
        FUEL_VALUES.put(Items.LILY_PAD, 300);
        FUEL_VALUES.put(Items.CACTUS, 400);
        FUEL_VALUES.put(Items.PUMPKIN, 600);
        FUEL_VALUES.put(Items.MELON, 800);
        FUEL_VALUES.put(Items.MOSS_BLOCK, 500);
        FUEL_VALUES.put(Items.KELP, 200);
        FUEL_VALUES.put(Items.SEAGRASS, 150);
    }

    // 当前燃烧进度
    private int burnTime = 0;
    private int maxBurnTime = 0;
    private boolean generating = false;

    public BioGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIO_GENERATOR.get(), pos, state,
                MAX_ENERGY, 0, MAX_EXTRACT, FUEL_SLOTS);
    }

    @Override
    protected ItemStackHandler createInventory(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return getFuelValue(stack) > 0;
            }
        };
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        boolean wasGenerating = generating;

        // 如果正在燃烧
        if (burnTime > 0) {
            burnTime--;
            generating = true;

            // 产生能量
            if (getEnergyStored() < getMaxEnergyStored()) {
                int toAdd = Math.min(RF_PER_TICK, getMaxEnergyStored() - getEnergyStored());
                energy.setEnergy(getEnergyStored() + toAdd);
            }
        } else {
            generating = false;

            // 尝试消耗新燃料
            if (getEnergyStored() < getMaxEnergyStored()) {
                tryConsumeFuel();
            }
        }

        // 输出能量到相邻方块
        if (getEnergyStored() > 0) {
            outputEnergy();
        }

        // 状态变化时更新方块
        if (wasGenerating != generating) {
            setChanged();
            BlockState state = level.getBlockState(getBlockPos());
            if (state.hasProperty(BioGeneratorBlock.ACTIVE)) {
                level.setBlock(getBlockPos(), state.setValue(BioGeneratorBlock.ACTIVE, generating), 3);
            }
        }
    }

    private void tryConsumeFuel() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                int fuelValue = getFuelValue(stack);
                if (fuelValue > 0) {
                    maxBurnTime = fuelValue / RF_PER_TICK;
                    burnTime = maxBurnTime;
                    generating = true;

                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        inventory.setStackInSlot(i, ItemStack.EMPTY);
                    }
                    setChanged();
                    return;
                }
            }
        }
    }

    private void outputEnergy() {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = getBlockPos().relative(direction);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor != null) {
                neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(neighborStorage -> {
                    if (neighborStorage.canReceive()) {
                        int toTransfer = Math.min(MAX_EXTRACT, getEnergyStored());
                        int transferred = neighborStorage.receiveEnergy(toTransfer, false);
                        if (transferred > 0) {
                            extractEnergy(transferred, false);
                        }
                    }
                });
            }
        }
    }

    public static int getFuelValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Integer value = FUEL_VALUES.get(stack.getItem());
        return value != null ? value : 0;
    }

    public ItemStack addFuel(ItemStack stack) {
        if (getFuelValue(stack) <= 0) return stack;

        ItemStack remaining = stack.copy();
        for (int i = 0; i < inventory.getSlots() && !remaining.isEmpty(); i++) {
            remaining = inventory.insertItem(i, remaining, false);
        }
        setChanged();
        return remaining;
    }

    public int getFuelCount() {
        int count = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            count += inventory.getStackInSlot(i).getCount();
        }
        return count;
    }

    public void dropInventory() {
        if (level == null || level.isClientSide()) return;

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, getBlockPos().getX() + 0.5,
                        getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5, stack.copy());
            }
        }
    }

    public boolean isGenerating() {
        return generating;
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
        tag.putBoolean("Generating", generating);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        burnTime = tag.getInt("BurnTime");
        maxBurnTime = tag.getInt("MaxBurnTime");
        generating = tag.getBoolean("Generating");
    }
}
