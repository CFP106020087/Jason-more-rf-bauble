package com.moremod.container;

import com.moremod.tile.TileEntityMegaChest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 超大容量箱子容器
 * 108 格存儲 + 玩家背包
 */
public class ContainerMegaChest extends Container {

    private final TileEntityMegaChest tile;
    private final ItemStackHandler chestInventory;

    // GUI 尺寸常量
    public static final int CHEST_ROWS = 12;
    public static final int CHEST_COLS = 9;
    public static final int SLOT_SIZE = 18;

    // Y 座標
    public static final int CHEST_START_Y = 18;
    public static final int PLAYER_INV_START_Y = CHEST_START_Y + CHEST_ROWS * SLOT_SIZE + 14;
    public static final int HOTBAR_START_Y = PLAYER_INV_START_Y + 3 * SLOT_SIZE + 4;

    public ContainerMegaChest(InventoryPlayer playerInventory, TileEntityMegaChest tile) {
        this.tile = tile;
        this.chestInventory = tile.getInventory();

        // 添加箱子槽位 (12行 x 9列 = 108格)
        int slotIndex = 0;
        for (int row = 0; row < CHEST_ROWS; row++) {
            for (int col = 0; col < CHEST_COLS; col++) {
                addSlotToContainer(new SlotItemHandler(chestInventory, slotIndex++,
                        8 + col * SLOT_SIZE, CHEST_START_Y + row * SLOT_SIZE));
            }
        }

        // 添加玩家背包槽位 (3行 x 9列)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * SLOT_SIZE, PLAYER_INV_START_Y + row * SLOT_SIZE));
            }
        }

        // 添加快捷欄槽位 (1行 x 9列)
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInventory, col,
                    8 + col * SLOT_SIZE, HOTBAR_START_Y));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile.getWorld().getTileEntity(tile.getPos()) == tile &&
               player.getDistanceSq(tile.getPos().getX() + 0.5, tile.getPos().getY() + 0.5, tile.getPos().getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            returnStack = slotStack.copy();

            int chestSlots = CHEST_ROWS * CHEST_COLS;  // 108

            if (index < chestSlots) {
                // 從箱子移到玩家背包
                if (!mergeItemStack(slotStack, chestSlots, chestSlots + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 從玩家背包移到箱子
                if (!mergeItemStack(slotStack, 0, chestSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (slotStack.getCount() == returnStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }

        return returnStack;
    }
}
