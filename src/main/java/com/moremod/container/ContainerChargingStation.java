package com.moremod.container;

import com.moremod.tile.TileEntityChargingStation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 充能站容器 - 支持能量同步
 */
public class ContainerChargingStation extends Container {

    private final TileEntityChargingStation tile;
    private int lastEnergy = -1;

    public ContainerChargingStation(InventoryPlayer playerInventory, TileEntityChargingStation tile) {
        this.tile = tile;
        IItemHandler inventory = tile.getInventory();

        // 充電槽 3x3
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlotToContainer(new ChargingSlot(inventory, row * 3 + col, 62 + col * 18, 17 + row * 18));
            }
        }

        // 玩家背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // 玩家快捷欄
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        int currentEnergy = tile.getEnergyStored();

        if (currentEnergy != lastEnergy) {
            for (IContainerListener listener : this.listeners) {
                // 使用两个int来传输能量值（支持大数值）
                listener.sendWindowProperty(this, 0, currentEnergy & 0xFFFF);
                listener.sendWindowProperty(this, 1, (currentEnergy >> 16) & 0xFFFF);
            }
            lastEnergy = currentEnergy;
        }
    }

    @Override
    public void updateProgressBar(int id, int data) {
        if (id == 0) {
            lastEnergy = (lastEnergy & 0xFFFF0000) | (data & 0xFFFF);
        } else if (id == 1) {
            lastEnergy = (lastEnergy & 0x0000FFFF) | ((data & 0xFFFF) << 16);
            // 更新到TileEntity（客户端）
            tile.setClientEnergy(lastEnergy);
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return playerIn.getDistanceSq(tile.getPos()) <= 64;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();

            // 從充電槽移出
            if (index < 9) {
                if (!mergeItemStack(stackInSlot, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 從背包移入充電槽
            else {
                if (stackInSlot.hasCapability(CapabilityEnergy.ENERGY, null)) {
                    if (!mergeItemStack(stackInSlot, 0, 9, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
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

    public TileEntityChargingStation getTile() {
        return tile;
    }

    public int getEnergy() {
        return lastEnergy >= 0 ? lastEnergy : tile.getEnergyStored();
    }

    /**
     * 只接受可充電物品的槽
     */
    private static class ChargingSlot extends SlotItemHandler {
        public ChargingSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack.hasCapability(CapabilityEnergy.ENERGY, null);
        }
    }
}
