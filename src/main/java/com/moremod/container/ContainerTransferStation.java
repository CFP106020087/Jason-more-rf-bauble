package com.moremod.container;

import com.moremod.compat.crafttweaker.TransferRuneManager;
import com.moremod.tile.TileEntityTransferStation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 转移台 Container
 */
public class ContainerTransferStation extends Container {

    private final TileEntityTransferStation tile;

    public ContainerTransferStation(InventoryPlayer playerInv, TileEntityTransferStation tile) {
        this.tile = tile;

        // 源宝石槽（左上）- 槽位0
        this.addSlotToContainer(new SlotItemHandler(tile.getInventory(), 0, 26, 25) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return tile.getInventory().isItemValid(0, stack);
            }
        });

        // 目标宝石槽（中上）- 槽位1
        this.addSlotToContainer(new SlotItemHandler(tile.getInventory(), 1, 71, 25) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return tile.getInventory().isItemValid(1, stack);
            }
        });

        // 材料槽（左下）- 槽位2
        this.addSlotToContainer(new SlotItemHandler(tile.getInventory(), 2, 26, 53) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return tile.getInventory().isItemValid(2, stack);
            }
        });

        // 输出槽（右侧）- 槽位3
        this.addSlotToContainer(new SlotItemHandler(tile.getInventory(), 3, 134, 35) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false; // 输出槽不允许放入
            }

            @Override
            public ItemStack onTake(EntityPlayer player, ItemStack stack) {
                // 取出时的逻辑（如果需要）
                return super.onTake(player, stack);
            }
        });

        // 玩家背包槽（3×9）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(
                        playerInv,
                        col + row * 9 + 9,
                        8 + col * 18,
                        84 + row * 18
                ));
            }
        }

        // 玩家快捷栏（1×9）
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(
                    playerInv,
                    col,
                    8 + col * 18,
                    142
            ));
        }
    }

    // ==========================================
    // 业务方法
    // ==========================================

    /**
     * 执行转移操作
     *
     * @param player 玩家
     * @return 是否成功
     */
    public boolean performTransfer(EntityPlayer player) {
        if (tile.getWorld().isRemote) {
            return false; // 客户端不执行
        }

        return tile.performTransfer(player);
    }

    /**
     * 检查是否可以转移
     */
    public boolean canTransfer() {
        return tile.canTransfer();
    }

    /**
     * 获取错误信息
     */
    public String getErrorMessage() {
        return tile.getErrorMessage();
    }

    /**
     * 获取需要的经验等级（从TileEntity获取，支持动态计算）
     */
    public int getRequiredXp() {
        return tile.getRequiredXp();
    }

    /**
     * 获取当前成功率
     */
    public float getSuccessRate() {
        return tile.getSuccessRate();
    }

    /**
     * 获取当前词条上限
     */
    public int getMaxAffixLimit() {
        return tile.getMaxAffixLimit();
    }

    /**
     * 获取TileEntity（供GUI使用）
     */
    public TileEntityTransferStation getTile() {
        return tile;
    }

    // ==========================================
    // Container标准方法
    // ==========================================

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile.getWorld().getTileEntity(tile.getPos()) == tile
                && playerIn.getDistanceSq(tile.getPos().add(0.5, 0.5, 0.5)) <= 64;
    }

    /**
     * Shift+点击物品的处理 - 修复版
     */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack currentStack = slot.getStack();
            itemstack = currentStack.copy();

            // 从机器槽位取出（槽位0-3）
            if (index < 4) {
                // 尝试放入玩家背包
                if (!this.mergeItemStack(currentStack, 4, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(currentStack, itemstack);
            }
            // 从玩家背包/快捷栏放入机器（槽位4+）
            else {
                boolean placed = false;
                
                // 优先级1: 检查是否是符文（最具体的条件）
                if (!placed && TransferRuneManager.isValidRune(currentStack)) {
                    if (this.mergeItemStack(currentStack, 2, 3, false)) {
                        placed = true;
                    }
                }
                
                // 优先级2: 尝试放入源宝石槽（如果不是符文）
                if (!placed && tile.getInventory().isItemValid(0, currentStack)) {
                    if (this.mergeItemStack(currentStack, 0, 1, false)) {
                        placed = true;
                    }
                }
                
                // 优先级3: 尝试放入目标宝石槽（如果不是符文且不适合源槽）
                if (!placed && tile.getInventory().isItemValid(1, currentStack)) {
                    if (this.mergeItemStack(currentStack, 1, 2, false)) {
                        placed = true;
                    }
                }
                
                // 如果都放不进去，尝试在背包和快捷栏之间移动
                if (!placed) {
                    // 从背包到快捷栏
                    if (index >= 4 && index < 4 + 27) {
                        if (!this.mergeItemStack(currentStack, 4 + 27, 4 + 36, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                    // 从快捷栏到背包
                    else if (index >= 4 + 27 && index < 4 + 36) {
                        if (!this.mergeItemStack(currentStack, 4, 4 + 27, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (currentStack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (currentStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, currentStack);
        }

        return itemstack;
    }

    /**
     * 容器关闭时
     * 注意：正常情况下不返还物品，因为TileEntity会保存它们
     * 只在TileEntity被破坏时才会掉落物品（由Block处理）
     */
    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        // 不主动返还物品，让TileEntity保存它们
        // 如果需要特殊的返还逻辑，可以在这里添加
    }
}