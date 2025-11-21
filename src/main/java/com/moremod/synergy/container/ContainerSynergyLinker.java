package com.moremod.synergy.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Synergy Linker 容器
 *
 * 说明：
 * - 服务端容器逻辑
 * - 没有物品槽位（纯 GUI 交互）
 * - 仅添加玩家背包槽位
 */
public class ContainerSynergyLinker extends Container {

    private final EntityPlayer player;

    public ContainerSynergyLinker(EntityPlayer player) {
        this.player = player;

        // 添加玩家背包（标准 9x3 + 快捷栏）
        addPlayerInventory(player);
    }

    /**
     * 添加玩家背包槽位
     */
    private void addPlayerInventory(EntityPlayer player) {
        // 主背包 (9x3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9;
                int x = 8 + col * 18;
                int y = 84 + row * 18; // GUI 高度调整
                addSlotToContainer(new Slot(player.inventory, index, x, y));
            }
        }

        // 快捷栏 (9x1)
        for (int col = 0; col < 9; col++) {
            int x = 8 + col * 18;
            int y = 142; // GUI 高度调整
            addSlotToContainer(new Slot(player.inventory, col, x, y));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        // 没有自定义槽位，不需要处理 Shift+点击
        return ItemStack.EMPTY;
    }

    public EntityPlayer getPlayer() {
        return player;
    }
}
