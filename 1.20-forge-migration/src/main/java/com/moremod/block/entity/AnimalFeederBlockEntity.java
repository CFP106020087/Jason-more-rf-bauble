package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * 动物喂食器BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 自动喂养周围的动物使其繁殖
 * - 范围8格
 */
public class AnimalFeederBlockEntity extends BlockEntity {

    private static final int RANGE = 8;
    private static final int COOLDOWN_TICKS = 200; // 10秒冷却
    private static final int FOOD_SLOTS = 9;

    // 有效的食物列表
    private static final Set<net.minecraft.world.item.Item> VALID_FOODS = Set.of(
            Items.WHEAT,
            Items.CARROT,
            Items.POTATO,
            Items.BEETROOT,
            Items.WHEAT_SEEDS,
            Items.MELON_SEEDS,
            Items.PUMPKIN_SEEDS,
            Items.BEETROOT_SEEDS,
            Items.GOLDEN_CARROT,
            Items.APPLE,
            Items.GOLDEN_APPLE,
            Items.SWEET_BERRIES,
            Items.GLOW_BERRIES,
            Items.DANDELION,
            Items.SEAGRASS,
            Items.KELP,
            Items.BAMBOO
    );

    private final ItemStackHandler inventory = new ItemStackHandler(FOOD_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidFood(stack);
        }
    };

    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    private int cooldown = 0;
    private int tickCounter = 0;

    public AnimalFeederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANIMAL_FEEDER.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        tickCounter++;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // 每20tick检查一次
        if (tickCounter % 20 != 0) return;

        // 查找可喂养的动物
        AABB area = new AABB(getBlockPos()).inflate(RANGE);
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, area,
                animal -> animal.getAge() == 0 && !animal.isInLove());

        if (animals.isEmpty()) return;

        // 尝试喂养动物
        for (Animal animal : animals) {
            ItemStack food = findFoodFor(animal);
            if (!food.isEmpty()) {
                // 让动物进入繁殖状态
                animal.setInLove(null);
                food.shrink(1);
                cooldown = COOLDOWN_TICKS;
                setChanged();
                break; // 一次只喂一只
            }
        }
    }

    private ItemStack findFoodFor(Animal animal) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && animal.isFood(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean isValidFood(ItemStack stack) {
        return VALID_FOODS.contains(stack.getItem());
    }

    public ItemStack addFood(ItemStack stack) {
        if (!isValidFood(stack)) return stack;

        ItemStack remaining = stack.copy();
        for (int i = 0; i < inventory.getSlots() && !remaining.isEmpty(); i++) {
            remaining = inventory.insertItem(i, remaining, false);
        }
        setChanged();
        return remaining;
    }

    public int getFoodCount() {
        int count = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            count += inventory.getStackInSlot(i).getCount();
        }
        return count;
    }

    public int getCooldownSeconds() {
        return cooldown / 20;
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

    // ===== Capabilities =====

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        cooldown = tag.getInt("Cooldown");
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
}
