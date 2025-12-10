package com.moremod.container;

import com.moremod.energy.BloodEnergyHandler;
import com.moremod.tile.TileEntityBloodGenerator;
import net.minecraft.entity.player.EntityPlayer;
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
 * 血液发电机容器
 */
public class ContainerBloodGenerator extends Container {

    private final TileEntityBloodGenerator te;

    // 同步字段
    private int lastEnergy = -1;
    private int lastTotalEnergy = -1;
    private int lastExtracted = -1;

    public ContainerBloodGenerator(InventoryPlayer playerInv, TileEntityBloodGenerator te) {
        this.te = te;

        IItemHandler handler = te.getInventory();

        // 输入槽 (沾血武器)
        this.addSlotToContainer(new SlotItemHandler(handler, 0, 56, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return BloodEnergyHandler.isValidWeapon(stack) && BloodEnergyHandler.hasBloodData(stack);
            }

            @Override
            public int getSlotStackLimit() {
                return 1;
            }
        });

        // 输出槽 (干净武器)
        this.addSlotToContainer(new SlotItemHandler(handler, 1, 116, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false; // 不能手动放入
            }
        });

        // 玩家背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }

        // 玩家快捷栏
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(playerInv, col,
                    8 + col * 18, 142));
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        int energy = te.getEnergyStored();
        int totalEnergy = te.getTotalEnergyToExtract();
        int extracted = te.getExtractedEnergy();

        for (IContainerListener listener : listeners) {
            if (lastEnergy != energy) {
                // 分割发送大数值 (能量值可能超过short范围)
                listener.sendWindowProperty(this, 0, energy & 0xFFFF);
                listener.sendWindowProperty(this, 1, (energy >> 16) & 0xFFFF);
            }
            if (lastTotalEnergy != totalEnergy) {
                listener.sendWindowProperty(this, 2, totalEnergy & 0xFFFF);
                listener.sendWindowProperty(this, 3, (totalEnergy >> 16) & 0xFFFF);
            }
            if (lastExtracted != extracted) {
                listener.sendWindowProperty(this, 4, extracted & 0xFFFF);
                listener.sendWindowProperty(this, 5, (extracted >> 16) & 0xFFFF);
            }
        }

        lastEnergy = energy;
        lastTotalEnergy = totalEnergy;
        lastExtracted = extracted;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data) {
        switch (id) {
            case 0:
                // 能量低16位
                int currentEnergy = te.getEnergyStored();
                te.setClientEnergy((currentEnergy & 0xFFFF0000) | (data & 0xFFFF));
                break;
            case 1:
                // 能量高16位
                currentEnergy = te.getEnergyStored();
                te.setClientEnergy((currentEnergy & 0xFFFF) | ((data & 0xFFFF) << 16));
                break;
            case 2:
                // 总能量低16位
                te.setClientTotalEnergy((te.getTotalEnergyToExtract() & 0xFFFF0000) | (data & 0xFFFF));
                break;
            case 3:
                // 总能量高16位
                te.setClientTotalEnergy((te.getTotalEnergyToExtract() & 0xFFFF) | ((data & 0xFFFF) << 16));
                break;
            case 4:
                // 已提取低16位
                te.setClientExtracted((te.getExtractedEnergy() & 0xFFFF0000) | (data & 0xFFFF));
                break;
            case 5:
                // 已提取高16位
                te.setClientExtracted((te.getExtractedEnergy() & 0xFFFF) | ((data & 0xFFFF) << 16));
                break;
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return playerIn.getDistanceSq(te.getPos().getX() + 0.5,
                te.getPos().getY() + 0.5,
                te.getPos().getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();

            // 从机器槽位转移到玩家背包
            if (index < 2) {
                if (!mergeItemStack(stackInSlot, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 从玩家背包转移到输入槽
            else {
                if (BloodEnergyHandler.isValidWeapon(stackInSlot) &&
                    BloodEnergyHandler.hasBloodData(stackInSlot)) {
                    if (!mergeItemStack(stackInSlot, 0, 1, false)) {
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

    public TileEntityBloodGenerator getTileEntity() {
        return te;
    }
}
