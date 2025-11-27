package com.moremod.client.gui;

import com.moremod.tile.TileEntityItemTransporter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 物品传输器容器
 */
public class ContainerItemTransporter extends Container {

    private final TileEntityItemTransporter tile;

    public ContainerItemTransporter(EntityPlayer player, TileEntityItemTransporter tile) {
        this.tile = tile;

        // 内部槽位（中央显示）
        this.addSlotToContainer(new SlotItemHandler(tile.inventory, 0, 80, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false; // 玩家不能直接放入
            }
        });

        // 过滤槽位（3行4列）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                int index = row * 4 + col;
                this.addSlotToContainer(new SlotItemHandler(
                        tile.getFilterInventory(),
                        index,
                        8 + col * 18,
                        70 + row * 18
                ) {
                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        return true; // 允许放入过滤物品
                    }

                    @Override
                    public int getSlotStackLimit() {
                        return 1; // 每格只能放1个（作为样本）
                    }
                });
            }
        }

        // 玩家物品栏
        int xOffset = 8;
        int yOffset = 161;

        // 主物品栏（3x9）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(
                        player.inventory,
                        col + row * 9 + 9,
                        xOffset + col * 18,
                        yOffset + row * 18
                ));
            }
        }

        // 快捷栏（1x9）
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(
                    player.inventory,
                    col,
                    xOffset + col * 18,
                    yOffset + 58
            ));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile.getWorld().getTileEntity(tile.getPos()) == tile &&
                playerIn.getDistanceSq(tile.getPos()) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            itemstack = slotStack.copy();

            int filterStart = 1;
            int filterEnd = 13;
            int playerStart = 13;
            int playerEnd = playerStart + 36;

            // 从过滤槽位Shift点击
            if (index >= filterStart && index < filterEnd) {
                if (!this.mergeItemStack(slotStack, playerStart, playerEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // 从玩家物品栏Shift点击
            else if (index >= playerStart && index < playerEnd) {
                // 尝试放入过滤槽位
                if (!this.mergeItemStack(slotStack, filterStart, filterEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, slotStack);
        }

        return itemstack;
    }

    public TileEntityItemTransporter getTile() {
        return tile;
    }
}