// ContainerBottlingMachine.java
package com.moremod.container;

import com.moremod.tile.TileEntityBottlingMachine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ContainerBottlingMachine extends Container {

    private final TileEntityBottlingMachine tileEntity;
    private int processTime;
    private int maxProcessTime;
    private int fluidAmount;
    private int fluidCapacity;

    public ContainerBottlingMachine(InventoryPlayer playerInventory, TileEntityBottlingMachine tileEntity) {
        this.tileEntity = tileEntity;
        IItemHandler itemHandler = tileEntity.getItemHandler();

        // 添加机器槽位
        // 输入槽 (index 0)
        this.addSlotToContainer(new SlotItemHandler(itemHandler, 0, 56, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return itemHandler.isItemValid(0, stack);
            }
        });

        // 输出槽 (index 1)
        this.addSlotToContainer(new SlotItemHandler(itemHandler, 1, 116, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false; // 输出槽不允许放入物品
            }
        });

        // 流体容器槽 (index 2)
        this.addSlotToContainer(new SlotItemHandler(itemHandler, 2, 26, 53) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return itemHandler.isItemValid(2, stack);
            }
        });

        // 添加玩家背包槽位
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(new Slot(playerInventory,
                        col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // 添加玩家快捷栏槽位
        for (int col = 0; col < 9; ++col) {
            this.addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        for (IContainerListener listener : listeners) {
            if (processTime != tileEntity.getProcessTime()) {
                listener.sendWindowProperty(this, 0, tileEntity.getProcessTime());
            }
            if (maxProcessTime != tileEntity.getMaxProcessTime()) {
                listener.sendWindowProperty(this, 1, tileEntity.getMaxProcessTime());
            }
            if (fluidAmount != tileEntity.getFluidTank().getFluidAmount()) {
                listener.sendWindowProperty(this, 2, tileEntity.getFluidTank().getFluidAmount());
            }
            if (fluidCapacity != tileEntity.getFluidTank().getCapacity()) {
                listener.sendWindowProperty(this, 3, tileEntity.getFluidTank().getCapacity());
            }
        }

        processTime = tileEntity.getProcessTime();
        maxProcessTime = tileEntity.getMaxProcessTime();
        fluidAmount = tileEntity.getFluidTank().getFluidAmount();
        fluidCapacity = tileEntity.getFluidTank().getCapacity();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data) {
        tileEntity.setField(id, data);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tileEntity.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            // 从机器槽位转移到玩家背包
            if (index < 3) {
                if (!this.mergeItemStack(itemstack1, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(itemstack1, itemstack);
            }
            // 从玩家背包转移到机器槽位
            else {
                // 尝试放入输入槽
                if (tileEntity.getItemHandler().isItemValid(0, itemstack1)) {
                    if (!this.mergeItemStack(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // 尝试放入流体容器槽
                else if (tileEntity.getItemHandler().isItemValid(2, itemstack1)) {
                    if (!this.mergeItemStack(itemstack1, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // 在背包和快捷栏之间转移
                else if (index < 30) {
                    if (!this.mergeItemStack(itemstack1, 30, 39, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 39) {
                    if (!this.mergeItemStack(itemstack1, 3, 30, false)) {
                        return ItemStack.EMPTY;
                    }
                }
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

// ===========================================
// GuiBottlingMachine.java
// ===========================================
