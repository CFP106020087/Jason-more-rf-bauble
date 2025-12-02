// ============================================
// 文件路徑: src/main/java/com/moremod/container/ContainerTradingStation.java
// 說明: 村民交易機容器 - 處理物品槽位和同步
// ✅ 修复: 添加第二个输入槽位,支持双物品交易
// ============================================

package com.moremod.container;

import com.moremod.tile.TileTradingStation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 🏪 村民交易機容器
 *
 * 功能:
 * - 定義物品槽位佈局
 * - 處理物品轉移(Shift+點擊)
 * - 同步數據到客戶端
 * 
 * ✅ 修复内容:
 * - 增加第二个输入槽位(槽位2)
 * - 更新输出槽为槽位3
 * - 调整Shift+点击逻辑
 */
public class ContainerTradingStation extends Container {

    private final TileTradingStation tile;

    /**
     * 構造器
     *
     * @param player 玩家
     * @param tile 交易機TileEntity
     */
    public ContainerTradingStation(EntityPlayer player, TileTradingStation tile) {
        this.tile = tile;

        // ========== 機器槽位 ==========

        // 槽位 0: 村民膠囊槽 (26, 53)
        this.addSlotToContainer(new SlotItemHandler(tile.getItemHandler(), 0, 26, 53));

        // ✅ 槽位 1: 輸入槽1 (26, 89) - 第一个交易物品
        this.addSlotToContainer(new SlotItemHandler(tile.getItemHandler(), 1, 26, 89));

        // ✅ 槽位 2: 輸入槽2 (50, 89) - 第二个交易物品(可选)
        this.addSlotToContainer(new SlotItemHandler(tile.getItemHandler(), 2, 50, 89));

        // ✅ 槽位 3: 輸出槽 (116, 89) - 只能取出,不能放入
        this.addSlotToContainer(new SlotItemHandler(tile.getItemHandler(), 3, 116, 89) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false; // 輸出槽不允許放入物品
            }
        });

        // ========== 玩家背包槽位 ==========

        // 玩家背包主區域 (3行9列)
        // 位置: (8, 125) 起始
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(
                        player.inventory,
                        col + row * 9 + 9,  // 槽位索引(9-35)
                        8 + col * 18,        // X 座標
                        125 + row * 18       // Y 座標
                ));
            }
        }

        // 玩家快捷欄 (1行9列)
        // 位置: (8, 183) 起始
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(
                    player.inventory,
                    col,              // 槽位索引(0-8)
                    8 + col * 18,     // X 座標
                    183               // Y 座標
            ));
        }
    }

    /**
     * 檢查玩家是否可以使用此容器
     *
     * @param playerIn 玩家
     * @return true=可以使用, false=不能使用
     */
    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        // 檢查TileEntity是否仍然存在
        if (tile.getWorld().getTileEntity(tile.getPos()) != tile) {
            return false;
        }

        // 檢查玩家距離(最遠64格)
        return playerIn.getDistanceSq(
                tile.getPos().getX() + 0.5,
                tile.getPos().getY() + 0.5,
                tile.getPos().getZ() + 0.5
        ) <= 64.0;
    }

    /**
     * ✅ 修复: Shift+點擊時的物品轉移邏輯 - 支持双输入槽
     *
     * @param playerIn 玩家
     * @param index 被點擊的槽位索引
     * @return 轉移的物品
     */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();

            // ✅ 槽位佈局:
            // 0-3: 機器槽位 (村民胶囊 + 输入1 + 输入2 + 输出)
            // 4-39: 玩家背包和快捷欄

            if (index < 4) {
                // 從機器槽位 -> 玩家背包
                if (!this.mergeItemStack(stackInSlot, 4, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }

                // 槽位變化時的額外處理
                slot.onSlotChange(stackInSlot, itemstack);
            } else {
                // 從玩家背包 -> 機器槽位

                // 優先嘗試放入村民膠囊槽(槽位0)
                if (!this.mergeItemStack(stackInSlot, 0, 1, false)) {
                    // 如果不是膠囊,嘗試放入輸入槽1(槽位1)
                    if (!this.mergeItemStack(stackInSlot, 1, 2, false)) {
                        // 如果失败,尝试放入输入槽2(槽位2)
                        if (!this.mergeItemStack(stackInSlot, 2, 3, false)) {
                            // 如果都失敗,嘗試在背包內轉移
                            if (index < 31) {
                                // 從背包主區域 -> 快捷欄
                                if (!this.mergeItemStack(stackInSlot, 31, 40, false)) {
                                    return ItemStack.EMPTY;
                                }
                            } else {
                                // 從快捷欄 -> 背包主區域
                                if (!this.mergeItemStack(stackInSlot, 4, 31, false)) {
                                    return ItemStack.EMPTY;
                                }
                            }
                        }
                    }
                }
            }

            // 處理物品數量變化
            if (stackInSlot.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            // 如果沒有轉移任何物品,返回空
            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            // 拿起物品時的處理
            slot.onTake(playerIn, stackInSlot);
        }

        return itemstack;
    }

    /**
     * 獲取關聯的TileEntity
     *
     * @return 交易機TileEntity
     */
    public TileTradingStation getTile() {
        return tile;
    }

    /**
     * 容器關閉時調用
     *
     * @param playerIn 玩家
     */
    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
    }
}
