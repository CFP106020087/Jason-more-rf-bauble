package com.moremod.printer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
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

    // 同步字段ID
    private static final int FIELD_ENERGY_LOW = 0;
    private static final int FIELD_ENERGY_HIGH = 1;
    private static final int FIELD_PROGRESS = 2;
    private static final int FIELD_MAX_PROGRESS = 3;
    private static final int FIELD_IS_PROCESSING = 4;

    // 缓存值（用于检测变化）
    private int cachedEnergyLow = -1;
    private int cachedEnergyHigh = -1;
    private int cachedProgress = -1;
    private int cachedMaxProgress = -1;
    private int cachedIsProcessing = -1;

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
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        // 获取当前值
        int energy = tile.getEnergyStored();
        int energyLow = energy & 0xFFFF;
        int energyHigh = (energy >> 16) & 0xFFFF;
        int progress = tile.getProgress();
        int maxProgress = tile.getMaxProgress();
        int isProcessing = tile.isProcessing() ? 1 : 0;

        // 发送变化给所有监听器
        for (IContainerListener listener : this.listeners) {
            if (cachedEnergyLow != energyLow) {
                listener.sendWindowProperty(this, FIELD_ENERGY_LOW, energyLow);
            }
            if (cachedEnergyHigh != energyHigh) {
                listener.sendWindowProperty(this, FIELD_ENERGY_HIGH, energyHigh);
            }
            if (cachedProgress != progress) {
                listener.sendWindowProperty(this, FIELD_PROGRESS, progress);
            }
            if (cachedMaxProgress != maxProgress) {
                listener.sendWindowProperty(this, FIELD_MAX_PROGRESS, maxProgress);
            }
            if (cachedIsProcessing != isProcessing) {
                listener.sendWindowProperty(this, FIELD_IS_PROCESSING, isProcessing);
            }
        }

        // 更新缓存
        cachedEnergyLow = energyLow;
        cachedEnergyHigh = energyHigh;
        cachedProgress = progress;
        cachedMaxProgress = maxProgress;
        cachedIsProcessing = isProcessing;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data) {
        switch (id) {
            case FIELD_ENERGY_LOW:
                cachedEnergyLow = data;
                updateTileEnergy();
                break;
            case FIELD_ENERGY_HIGH:
                cachedEnergyHigh = data;
                updateTileEnergy();
                break;
            case FIELD_PROGRESS:
                tile.setClientProgress(data);
                break;
            case FIELD_MAX_PROGRESS:
                tile.setClientMaxProgress(data);
                break;
            case FIELD_IS_PROCESSING:
                tile.setClientProcessing(data == 1);
                break;
        }
    }

    private void updateTileEnergy() {
        if (cachedEnergyLow >= 0 && cachedEnergyHigh >= 0) {
            int energy = cachedEnergyLow | (cachedEnergyHigh << 16);
            tile.setClientEnergy(energy);
        }
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
