package com.moremod.container;

import com.moremod.compat.crafttweaker.GemNBTHelper;
import com.moremod.tile.TileEntityExtractionStation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerExtractionStation extends Container {
    
    private final TileEntityExtractionStation tile;
    private final EntityPlayer player;

    public ContainerExtractionStation(IInventory playerInventory, TileEntityExtractionStation tile, EntityPlayer player) {
        this.tile = tile;
        this.player = player;
        
        // 输入槽 (56, 35)
        this.addSlotToContainer(new SlotItemHandler(
            (net.minecraftforge.items.IItemHandler) tile.getCapability(
                net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null
            ), 0, 56, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return GemNBTHelper.isGem(stack);
            }
        });
        
        // 输出槽 (116, 35)
        this.addSlotToContainer(new SlotItemHandler(
            (net.minecraftforge.items.IItemHandler) tile.getCapability(
                net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null
            ), 1, 116, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }
        });
        
        // 玩家背包
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 
                    8 + j * 18, 84 + i * 18));
            }
        }
        
        // 玩家快捷栏
        for (int i = 0; i < 9; i++) {
            this.addSlotToContainer(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
    
    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile.getWorld().getTileEntity(tile.getPos()) == tile
            && playerIn.getDistanceSq(tile.getPos().add(0.5, 0.5, 0.5)) <= 64;
    }
    
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            
            if (index < 2) {
                if (!this.mergeItemStack(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (GemNBTHelper.isGem(itemstack1)) {
                if (!this.mergeItemStack(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 29) {
                if (!this.mergeItemStack(itemstack1, 29, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 38 && !this.mergeItemStack(itemstack1, 2, 29, false)) {
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
    
    public TileEntityExtractionStation getTile() {
        return tile;
    }
    
    public EntityPlayer getPlayer() {
        return player;
    }
}