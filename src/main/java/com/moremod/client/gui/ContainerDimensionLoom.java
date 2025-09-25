package com.moremod.client.gui;

import com.moremod.tile.TileEntityDimensionLoom;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ContainerDimensionLoom extends Container {

    private final TileEntityDimensionLoom tileEntity;
    private int processTime;
    private int maxProcessTime;

    public ContainerDimensionLoom(InventoryPlayer playerInventory, TileEntityDimensionLoom tileEntity) {
        this.tileEntity = tileEntity;

        // 修正：3x3输入格子位置要完全匹配工作台GUI的格子位置
        // 工作台的3x3格子位置是 (30, 17)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlotToContainer(new Slot(tileEntity, i * 3 + j, 30 + j * 18, 17 + i * 18));
            }
        }

        // 输出格子在工作台输出位置 (124, 35)
        this.addSlotToContainer(new SlotFurnaceOutput(playerInventory.player, tileEntity, 9, 124, 35));

        // 玩家背包 - 工作台的背包位置
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // 玩家快捷栏 - 工作台的快捷栏位置
        for (int i = 0; i < 9; ++i) {
            this.addSlotToContainer(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendAllWindowProperties(this, this.tileEntity);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        for (int i = 0; i < this.listeners.size(); ++i) {
            IContainerListener listener = this.listeners.get(i);

            if (this.processTime != this.tileEntity.getField(0)) {
                listener.sendWindowProperty(this, 0, this.tileEntity.getField(0));
            }
            if (this.maxProcessTime != this.tileEntity.getField(1)) {
                listener.sendWindowProperty(this, 1, this.tileEntity.getField(1));
            }
        }

        this.processTime = this.tileEntity.getField(0);
        this.maxProcessTime = this.tileEntity.getField(1);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void updateProgressBar(int id, int data) {
        this.tileEntity.setField(id, data);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return this.tileEntity.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            // 如果是输出格子
            if (index == 9) {
                if (!this.mergeItemStack(itemstack1, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(itemstack1, itemstack);
            }
            // 如果是玩家背包或快捷栏
            else if (index >= 10) {
                // 尝试放入输入格子
                if (!this.mergeItemStack(itemstack1, 0, 9, false)) {
                    // 在背包和快捷栏之间移动
                    if (index < 37) {
                        if (!this.mergeItemStack(itemstack1, 37, 46, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.mergeItemStack(itemstack1, 10, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            // 如果是输入格子
            else if (!this.mergeItemStack(itemstack1, 10, 46, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }

        return itemstack;
    }
}
