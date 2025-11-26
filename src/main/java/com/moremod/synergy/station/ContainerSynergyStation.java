package com.moremod.synergy.station;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

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

        // 添加玩家物品栏槽位（可选，如果需要与物品交互）
        // 这里保留玩家物品栏以便未来扩展（比如需要消耗物品来激活链结）

        // 玩家物品栏 (3行9列)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(
                        playerInventory,
                        col + row * 9 + 9,  // 槽位索引从9开始（0-8是快捷栏）
                        8 + col * 18,       // X坐标
                        84 + row * 18       // Y坐标
                ));
            }
        }

        // 快捷栏 (1行9列)
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(
                    playerInventory,
                    col,
                    8 + col * 18,   // X坐标
                    142             // Y坐标
            ));
        }
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
}
