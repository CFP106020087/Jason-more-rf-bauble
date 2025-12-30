package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 渔网BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 放置在水面上自动钓鱼
 * - 需要下方有水源
 */
public class FishingNetBlockEntity extends BlockEntity {

    private static final int OUTPUT_SLOTS = 9;
    private static final int FISHING_INTERVAL = 400; // 20秒

    private final ItemStackHandler inventory = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    private int fishingTimer = 0;
    private int tickCounter = 0;

    public FishingNetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FISHING_NET.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        tickCounter++;

        // 检查是否有水
        if (!hasWaterBelow()) {
            fishingTimer = 0;
            return;
        }

        fishingTimer++;

        // 钓鱼间隔
        if (fishingTimer >= FISHING_INTERVAL) {
            fishingTimer = 0;
            tryFish();
        }
    }

    public boolean hasWaterBelow() {
        if (level == null) return false;
        BlockPos below = getBlockPos().below();
        return level.getBlockState(below).is(Blocks.WATER);
    }

    private void tryFish() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        // 检查是否有空间存储
        if (!hasSpaceInInventory()) return;

        // 使用钓鱼战利品表
        ResourceLocation lootTableId = new ResourceLocation("minecraft", "gameplay/fishing");
        LootTable lootTable = serverLevel.getServer().getLootData().getLootTable(lootTableId);

        LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(getBlockPos()))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .withLuck(0) // 基础幸运
                .create(LootContextParamSets.FISHING);

        List<ItemStack> loot = lootTable.getRandomItems(params);

        for (ItemStack stack : loot) {
            insertIntoInventory(stack);
        }

        setChanged();
    }

    private boolean hasSpaceInInventory() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void insertIntoInventory(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < inventory.getSlots() && !remaining.isEmpty(); i++) {
            remaining = inventory.insertItem(i, remaining, false);
        }
        // 如果还有剩余，掉落
        if (!remaining.isEmpty() && level != null) {
            Containers.dropItemStack(level, getBlockPos().getX() + 0.5,
                    getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5, remaining);
        }
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
        tag.putInt("FishingTimer", fishingTimer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        fishingTimer = tag.getInt("FishingTimer");
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
