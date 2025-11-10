package com.moremod.client.gui;

import com.moremod.item.ItemSageBook;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

public class ContainerSageBook extends Container {

    private final EntityPlayer player;
    private final EnumHand hand;
    private final ItemStack sageBookStack;

    public ContainerSageBook(EntityPlayer player, EnumHand hand) {
        this.player = player;
        this.hand = hand;
        this.sageBookStack = player.getHeldItem(hand);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        // 确保玩家仍然持有贤者之书
        ItemStack currentStack = player.getHeldItem(hand);
        return !currentStack.isEmpty() && currentStack.getItem() instanceof ItemSageBook;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
    }

    public EnumHand getHand() {
        return hand;
    }
}
