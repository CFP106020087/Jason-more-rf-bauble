package com.moremod.printer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 打印机容器
 *
 * 槽位布局:
 * - 槽位 0: 模版槽
 * - 槽位 1-9: 材料槽 (3x3)
 * - 槽位 10: 输出槽
 */
public class ContainerPrinter extends Container {

    private final TileEntityPrinter tile;

    // 槽位常量
    private static final int TEMPLATE_SLOT = 0;
    private static final int MATERIAL_SLOT_START = 1;
    private static final int MATERIAL_SLOT_COUNT = 9;
    private static final int OUTPUT_SLOT = 10;
    private static final int TE_SLOT_COUNT = 11;

    public ContainerPrinter(InventoryPlayer playerInventory, TileEntityPrinter tile) {
        this.tile = tile;
        IItemHandler inventory = tile.getInventory();

        // 模版槽 (左上角)
        addSlotToContainer(new TemplateSlot(inventory, TEMPLATE_SLOT, 26, 35));

        // 材料槽 3x3 (中间)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = MATERIAL_SLOT_START + row * 3 + col;
                addSlotToContainer(new SlotItemHandler(inventory, index, 62 + col * 18, 17 + row * 18));
            }
        }

        // 输出槽 (右侧)
        addSlotToContainer(new OutputSlot(inventory, OUTPUT_SLOT, 134, 35));

        // 玩家背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // 玩家快捷栏
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile.canPlayerUse(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();

            // 从打印机槽移出
            if (index < TE_SLOT_COUNT) {
                if (!mergeItemStack(stackInSlot, TE_SLOT_COUNT, TE_SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 从背包移入打印机槽
            else {
                // 优先放入模版槽
                if (stackInSlot.getItem() instanceof ItemPrintTemplate) {
                    if (!mergeItemStack(stackInSlot, TEMPLATE_SLOT, TEMPLATE_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // 否则放入材料槽
                else {
                    if (!mergeItemStack(stackInSlot, MATERIAL_SLOT_START, MATERIAL_SLOT_START + MATERIAL_SLOT_COUNT, false)) {
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

        return itemstack;
    }

    public TileEntityPrinter getTile() {
        return tile;
    }

    /**
     * 模版槽 - 只接受打印模版
     */
    private static class TemplateSlot extends SlotItemHandler {
        public TemplateSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack.getItem() instanceof ItemPrintTemplate;
        }
    }

    /**
     * 输出槽 - 不接受放入
     */
    private static class OutputSlot extends SlotItemHandler {
        public OutputSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }
    }
}
