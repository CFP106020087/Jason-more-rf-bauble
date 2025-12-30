package com.moremod.block.entity;

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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 转移台BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 物品传输中转站
 * - 支持过滤和自动化
 */
public class TransferStationBlockEntity extends BlockEntity implements MenuProvider {

    private static final int SLOT_COUNT = 9;
    private static final int TRANSFER_RATE = 8; // 每tick传输物品数

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    private int tickCounter = 0;

    public TransferStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRANSFER_STATION.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        tickCounter++;

        // 每5tick传输一次
        if (tickCounter % 5 != 0) return;

        // 尝试向相邻容器输出物品
        transferItems();
    }

    private void transferItems() {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = getBlockPos().relative(direction);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);

            if (neighbor != null) {
                neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).ifPresent(targetHandler -> {
                    // 尝试传输物品
                    for (int i = 0; i < inventory.getSlots(); i++) {
                        ItemStack stack = inventory.getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            // 尝试插入到目标
                            ItemStack toTransfer = stack.copy();
                            toTransfer.setCount(Math.min(TRANSFER_RATE, stack.getCount()));

                            for (int j = 0; j < targetHandler.getSlots(); j++) {
                                toTransfer = targetHandler.insertItem(j, toTransfer, false);
                                if (toTransfer.isEmpty()) break;
                            }

                            // 更新源槽位
                            int transferred = Math.min(TRANSFER_RATE, stack.getCount()) - toTransfer.getCount();
                            if (transferred > 0) {
                                inventory.extractItem(i, transferred, false);
                                setChanged();
                            }
                        }
                    }
                });
            }
        }
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    // ===== MenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.moremod.transfer_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // TODO: 创建自定义的TransferStationMenu
        return null;
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
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
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
}
