package com.moremod.accessorybox;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class SlotMirrorBauble extends SlotItemHandler {
    private final int baubleSlot;
    private final EntityPlayer player;
    private final IBaublesItemHandler baublesHandler;

    public SlotMirrorBauble(IBaublesItemHandler baublesHandler, EntityPlayer player, int baubleSlot, int x, int y) {
        super(baublesHandler, baubleSlot, x, y);
        this.baubleSlot = baubleSlot;
        this.player = player;
        this.baublesHandler = baublesHandler;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IBauble)) {
            return false;
        }

        IBauble bauble = (IBauble) stack.getItem();
        BaubleType type = bauble.getBaubleType(stack);

        // TRINKET 可以放在任何槽位
        if (type == BaubleType.TRINKET) {
            return bauble.canEquip(stack, player);
        }

        // 其他類型檢查特定槽位
        boolean typeMatch = false;
        switch(baubleSlot) {
            case 14:
                typeMatch = type == BaubleType.AMULET;
                break;
            case 15:
            case 16:
                typeMatch = type == BaubleType.RING;
                break;
            case 17:
                typeMatch = type == BaubleType.BELT;
                break;
            case 18:
                typeMatch = type == BaubleType.HEAD;
                break;
            case 19:
                typeMatch = type == BaubleType.BODY;
                break;
            case 20:
                typeMatch = type == BaubleType.CHARM;
                break;
            case 21:
                typeMatch = type == BaubleType.CHARM;
                break;
        }

        return typeMatch && bauble.canEquip(stack, player);
    }

    @Override
    public void putStack(ItemStack stack) {
        ItemStack oldStack = this.getStack();

        // 觸發卸下事件
        if (!oldStack.isEmpty() && oldStack.getItem() instanceof IBauble) {
            ((IBauble) oldStack.getItem()).onUnequipped(oldStack, player);
        }

        // 設置新物品
        super.putStack(stack);

        // 觸發裝備事件
        if (!stack.isEmpty() && stack.getItem() instanceof IBauble) {
            ((IBauble) stack.getItem()).onEquipped(stack, player);
        }
    }

    @Override
    public ItemStack onTake(EntityPlayer playerIn, ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof IBauble) {
            ((IBauble) stack.getItem()).onUnequipped(stack, playerIn);
        }
        return super.onTake(playerIn, stack);
    }

    @Override
    public boolean canTakeStack(EntityPlayer playerIn) {
        ItemStack stack = this.getStack();
        if (!stack.isEmpty() && stack.getItem() instanceof IBauble) {
            return ((IBauble) stack.getItem()).canUnequip(stack, playerIn);
        }
        return true;
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }
}