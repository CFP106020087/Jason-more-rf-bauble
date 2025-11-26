package com.moremod.synergy.station;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Synergy 链结站 Container
 *
 * 这个 Container 不需要处理物理物品槽位，
 * 因为链结操作是基于玩家已安装的模块 ID 进行的。
 *
 * 主要功能：
 * - 与 TileEntity 同步
 * - 处理网络消息
 * - 验证玩家距离
 */
public class ContainerSynergyStation extends Container {

    private final TileEntitySynergyStation tileEntity;
    private final EntityPlayer player;

    public ContainerSynergyStation(InventoryPlayer playerInventory, TileEntitySynergyStation te) {
        this.tileEntity = te;
        this.player = playerInventory.player;

        // 注意：这个 Container 不添加任何槽位
        // 因为 Synergy 链结站是纯 GUI 操作（基于模块 ID），不涉及物品传输
        // 添加玩家物品栏槽位会导致与 GuiScreen 不兼容而产生复制 bug
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tileEntity.isUsableByPlayer(playerIn);
    }

    /**
     * 获取关联的 TileEntity
     */
    public TileEntitySynergyStation getTileEntity() {
        return tileEntity;
    }

    /**
     * 获取玩家
     */
    public EntityPlayer getPlayer() {
        return player;
    }

    // ==================== 链结槽位操作（通过网络包调用）====================

    /**
     * 设置链结槽位中的模块
     * @param slot 槽位索引 (0-5)
     * @param moduleId 模块 ID，空字符串表示清空
     */
    public void setModuleInSlot(int slot, String moduleId) {
        if (slot >= 0 && slot < TileEntitySynergyStation.LINK_SLOT_COUNT) {
            tileEntity.setModuleInSlot(slot, moduleId);
        }
    }

    /**
     * 清空指定槽位
     */
    public void clearSlot(int slot) {
        tileEntity.clearSlot(slot);
    }

    /**
     * 清空所有槽位
     */
    public void clearAllSlots() {
        tileEntity.clearAllSlots();
    }

    /**
     * 切换激活状态
     */
    public void toggleActivated() {
        tileEntity.toggleActivated();
    }

    /**
     * 阻止 Shift-Click 移动物品
     * 这个 Container 不需要物品传输功能，所以直接返回空
     */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        // 不允许任何物品传输，防止复制bug
        return ItemStack.EMPTY;
    }
}
