package com.moremod.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class ContainerSageBook extends Container {

    public ContainerSageBook(EntityPlayer player) {
        // 不需要添加slot，因为这只是一个选择界面
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}