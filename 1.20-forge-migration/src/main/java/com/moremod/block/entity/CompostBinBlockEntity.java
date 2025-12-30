package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * 堆肥桶BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 将有机物转化为骨粉
 */
public class CompostBinBlockEntity extends BlockEntity {

    private static final int MAX_COMPOST = 64;
    private static final int COMPOST_TIME = 100; // 5秒完成一次

    // 存储有机物的虚拟计数
    private int storedCompost = 0;
    private int compostProgress = 0;

    // 输出存储
    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final LazyOptional<IItemHandler> outputHandler = LazyOptional.of(() -> output);

    // 输入包装器 - 用于管道自动输入
    private final IItemHandler inputHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        @Nonnull
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        @Nonnull
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (!isValidInputStatic(stack)) {
                return stack;
            }
            if (simulate) {
                int canAdd = MAX_COMPOST - storedCompost;
                if (canAdd <= 0) return stack;
                int toAdd = Math.min(canAdd, stack.getCount());
                ItemStack remaining = stack.copy();
                remaining.shrink(toAdd);
                return remaining;
            } else {
                return addCompostMaterial(stack);
            }
        }

        @Override
        @Nonnull
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return isValidInputStatic(stack);
        }
    };

    private final LazyOptional<IItemHandler> inputHandlerLazy = LazyOptional.of(() -> inputHandler);

    // 有效的有机物列表
    private static final Set<Item> VALID_INPUTS = new HashSet<>();

    static {
        // 种子类
        VALID_INPUTS.add(Items.WHEAT_SEEDS);
        VALID_INPUTS.add(Items.MELON_SEEDS);
        VALID_INPUTS.add(Items.PUMPKIN_SEEDS);
        VALID_INPUTS.add(Items.BEETROOT_SEEDS);
        VALID_INPUTS.add(Items.TORCHFLOWER_SEEDS);
        VALID_INPUTS.add(Items.PITCHER_POD);
        // 食物残渣
        VALID_INPUTS.add(Items.ROTTEN_FLESH);
        VALID_INPUTS.add(Items.SPIDER_EYE);
        VALID_INPUTS.add(Items.POISONOUS_POTATO);
        VALID_INPUTS.add(Items.APPLE);
        VALID_INPUTS.add(Items.MELON_SLICE);
        VALID_INPUTS.add(Items.CARROT);
        VALID_INPUTS.add(Items.POTATO);
        VALID_INPUTS.add(Items.BEETROOT);
        VALID_INPUTS.add(Items.WHEAT);
        // 植物
        VALID_INPUTS.add(Items.SUGAR_CANE);
        VALID_INPUTS.add(Items.SHORT_GRASS);
        VALID_INPUTS.add(Items.TALL_GRASS);
        VALID_INPUTS.add(Items.FERN);
        VALID_INPUTS.add(Items.LARGE_FERN);
        VALID_INPUTS.add(Items.DEAD_BUSH);
        VALID_INPUTS.add(Items.VINE);
        VALID_INPUTS.add(Items.LILY_PAD);
        VALID_INPUTS.add(Items.DANDELION);
        VALID_INPUTS.add(Items.POPPY);
        VALID_INPUTS.add(Items.BLUE_ORCHID);
        VALID_INPUTS.add(Items.ALLIUM);
        VALID_INPUTS.add(Items.AZURE_BLUET);
        VALID_INPUTS.add(Items.OAK_SAPLING);
        VALID_INPUTS.add(Items.SPRUCE_SAPLING);
        VALID_INPUTS.add(Items.BIRCH_SAPLING);
        VALID_INPUTS.add(Items.JUNGLE_SAPLING);
        VALID_INPUTS.add(Items.ACACIA_SAPLING);
        VALID_INPUTS.add(Items.DARK_OAK_SAPLING);
        VALID_INPUTS.add(Items.CHERRY_SAPLING);
        VALID_INPUTS.add(Items.MANGROVE_PROPAGULE);
        // 树叶
        VALID_INPUTS.add(Items.OAK_LEAVES);
        VALID_INPUTS.add(Items.SPRUCE_LEAVES);
        VALID_INPUTS.add(Items.BIRCH_LEAVES);
        VALID_INPUTS.add(Items.JUNGLE_LEAVES);
        VALID_INPUTS.add(Items.ACACIA_LEAVES);
        VALID_INPUTS.add(Items.DARK_OAK_LEAVES);
        VALID_INPUTS.add(Items.CHERRY_LEAVES);
        VALID_INPUTS.add(Items.MANGROVE_LEAVES);
        VALID_INPUTS.add(Items.AZALEA_LEAVES);
        VALID_INPUTS.add(Items.FLOWERING_AZALEA_LEAVES);
    }

    public CompostBinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPOST_BIN.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        // 需要有机物才能进行堆肥
        if (storedCompost <= 0) {
            compostProgress = 0;
            return;
        }

        compostProgress++;

        // 每秒生成粒子效果
        if (compostProgress % 20 == 0 && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    getBlockPos().getX() + 0.5,
                    getBlockPos().getY() + 0.6,
                    getBlockPos().getZ() + 0.5,
                    2, 0.3, 0.1, 0.3, 0.0);
        }

        // 完成堆肥
        if (compostProgress >= COMPOST_TIME) {
            int consumed = Math.min(storedCompost, 8);
            storedCompost -= consumed;

            int boneMealCount = Math.max(1, consumed / 4);
            ItemStack boneMeal = new ItemStack(Items.BONE_MEAL, boneMealCount);

            ItemStack existing = output.getStackInSlot(0);
            if (existing.isEmpty()) {
                output.setStackInSlot(0, boneMeal);
            } else if (existing.is(Items.BONE_MEAL) && existing.getCount() + boneMealCount <= 64) {
                existing.grow(boneMealCount);
            }

            compostProgress = 0;
            setChanged();

            // 完成效果
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        getBlockPos().getX() + 0.5,
                        getBlockPos().getY() + 0.8,
                        getBlockPos().getZ() + 0.5,
                        15, 0.3, 0.2, 0.3, 0.0);
            }
        }
    }

    public static boolean isValidInputStatic(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return VALID_INPUTS.contains(stack.getItem());
    }

    public boolean isValidInput(ItemStack stack) {
        return isValidInputStatic(stack);
    }

    public ItemStack addCompostMaterial(ItemStack stack) {
        if (!isValidInput(stack)) return stack;

        int canAdd = MAX_COMPOST - storedCompost;
        if (canAdd <= 0) return stack;

        int toAdd = Math.min(canAdd, stack.getCount());
        storedCompost += toAdd;
        setChanged();

        ItemStack remaining = stack.copy();
        remaining.shrink(toAdd);
        return remaining;
    }

    public ItemStack extractOutput() {
        ItemStack result = output.getStackInSlot(0).copy();
        output.setStackInSlot(0, ItemStack.EMPTY);
        setChanged();
        return result;
    }

    public void dropInventory() {
        if (level == null || level.isClientSide()) return;

        ItemStack outputStack = output.getStackInSlot(0);
        if (!outputStack.isEmpty()) {
            Containers.dropItemStack(level,
                    getBlockPos().getX() + 0.5,
                    getBlockPos().getY() + 0.5,
                    getBlockPos().getZ() + 0.5,
                    outputStack.copy());
        }
    }

    // ===== Getters =====

    public int getCompostProgress() {
        if (storedCompost <= 0) return 0;
        return (compostProgress * 100) / COMPOST_TIME;
    }

    public int getStoredAmount() {
        return storedCompost;
    }

    public int getOutputCount() {
        ItemStack stack = output.getStackInSlot(0);
        return stack.isEmpty() ? 0 : stack.getCount();
    }

    // ===== Capabilities =====

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == Direction.DOWN) {
                return outputHandler.cast();
            } else if (side == Direction.UP) {
                return inputHandlerLazy.cast();
            } else {
                // 侧面：返回组合处理器
                return LazyOptional.of(() -> new CombinedItemHandler()).cast();
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        outputHandler.invalidate();
        inputHandlerLazy.invalidate();
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("StoredCompost", storedCompost);
        tag.putInt("Progress", compostProgress);
        tag.put("Output", output.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedCompost = tag.getInt("StoredCompost");
        compostProgress = tag.getInt("Progress");
        if (tag.contains("Output")) {
            output.deserializeNBT(tag.getCompound("Output"));
        }
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

    /**
     * 组合物品处理器 - 用于侧面管道
     */
    private class CombinedItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        @Nonnull
        public ItemStack getStackInSlot(int slot) {
            if (slot == 0) {
                return ItemStack.EMPTY;
            } else {
                return output.getStackInSlot(0);
            }
        }

        @Override
        @Nonnull
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (slot == 0) {
                return inputHandler.insertItem(0, stack, simulate);
            }
            return stack;
        }

        @Override
        @Nonnull
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 1) {
                return output.extractItem(0, amount, simulate);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            if (slot == 0) {
                return isValidInputStatic(stack);
            }
            return false;
        }
    }
}
