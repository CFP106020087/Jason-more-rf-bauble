package com.moremod.synergy.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

/**
 * Synergy Linker 容器 - 简化版
 *
 * 说明：
 * - 由于 GUI 现在继承 GuiScreen 而不是 GuiContainer
 * - 这个 Container 只是一个占位符，不处理任何物品槽位
 * - 仅用于满足某些注册需求（如果使用 GuiContainer 则需要）
 *
 * 注意：
 * - 如果你的 GuiHandler 返回 null Container，GUI 会是纯 GuiScreen
 * - 如果返回这个 Container，GUI 可以兼容两种方式
 */
public class ContainerSynergyLinker extends Container {

    private final EntityPlayer player;

    public ContainerSynergyLinker(EntityPlayer player) {
        this.player = player;
        // 不添加任何槽位
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    public EntityPlayer getPlayer() {
        return player;
    }
}
