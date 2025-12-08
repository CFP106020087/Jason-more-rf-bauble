package com.moremod.container;

import com.moremod.item.ritual.ItemFakePlayerCore;
import com.moremod.tile.TileEntityFakePlayerActivator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 假玩家激活器 Container
 */
public class ContainerFakePlayerActivator extends Container {

    private final TileEntityFakePlayerActivator tile;
    private final int PLAYER_INV_START;

    public ContainerFakePlayerActivator(InventoryPlayer playerInv, TileEntityFakePlayerActivator tile) {
        this.tile = tile;
        IItemHandler inv = tile.getInventory();

        // 假玩家核心槽 (中间上方)
        this.addSlotToContainer(new SlotItemHandler(inv, 0, 80, 8) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() instanceof ItemFakePlayerCore;
            }
        });

        // 工具/物品槽 3x3 (左侧)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = 1 + row * 3 + col;
                this.addSlotToContainer(new SlotItemHandler(inv, slotIndex, 8 + col * 18, 30 + row * 18));
            }
        }

        PLAYER_INV_START = this.inventorySlots.size();

        // 玩家背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 102 + row * 18));
            }
        }

        // 玩家快捷栏
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 160));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player.getDistanceSq(tile.getPos()) <= 64;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            result = stackInSlot.copy();

            // 从机器槽位移到玩家背包
            if (index < PLAYER_INV_START) {
                if (!this.mergeItemStack(stackInSlot, PLAYER_INV_START, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 从玩家背包移到机器槽位
            else {
                // 假玩家核心 -> 核心槽
                if (stackInSlot.getItem() instanceof ItemFakePlayerCore) {
                    if (!this.mergeItemStack(stackInSlot, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // 其他物品 -> 工具槽
                else {
                    if (!this.mergeItemStack(stackInSlot, 1, 10, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return result;
    }

    public TileEntityFakePlayerActivator getTile() {
        return tile;
    }
}
