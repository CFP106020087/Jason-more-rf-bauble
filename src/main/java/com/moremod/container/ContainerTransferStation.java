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

    // 槽位坐标常量（最终置中）
    // 上方槽位：+2px置中
    public static final int SLOT_0_X = 40;   // 左上源宝石 (39+2)
    public static final int SLOT_0_Y = 63;   // (62+2)
    public static final int SLOT_1_X = 121;  // 中心输出（保持不变）
    public static final int SLOT_1_Y = 139;
    public static final int SLOT_2_X = 201;  // 右上目标宝石 (200+2)
    public static final int SLOT_2_Y = 63;   // (62+2)
    public static final int SLOT_3_X = 47;   // 左下符文（保持不变）
    public static final int SLOT_3_Y = 197;

    // 玩家背包坐标：向右2px（+2）改为+1px，向上9px（-9）
    public static final int PLAYER_INV_START_X = 48;   // 47+1 (原49改为48)
    public static final int PLAYER_INV_START_Y = 263;  // 272-9
    public static final int PLAYER_INV_SPACING = 18;

    // 快捷栏：向上9px（-9）
    public static final int HOTBAR_Y = 320;  // 329-9

    public ContainerTransferStation(InventoryPlayer playerInv, TileEntityTransferStation tile) {
        this.tile = tile;

        // ==========================
        // 机器四个槽位（最终定稿）
        // ==========================

        // 源宝石槽（左上）- 槽位0
        this.addSlotToContainer(new SlotItemHandler(tile.getInventory(), 0, SLOT_0_X, SLOT_0_Y) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return tile.getInventory().isItemValid(0, stack);
            }
        });

        // 输出槽（中心）- 槽位1
        this.addSlotToContainer(new SlotItemHandler(tile.getInventory(), 1, SLOT_1_X, SLOT_1_Y) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false; // 输出槽不允许放入
            }

            @Override
            public ItemStack onTake(EntityPlayer player, ItemStack stack) {
                // 如果你要在取走时做额外逻辑，可以放这里
                return super.onTake(player, stack);
            }
        });

        // 目标宝石槽（右上）- 槽位2
        this.addSlotToContainer(new SlotItemHandler(tile.getInventory(), 2, SLOT_2_X, SLOT_2_Y) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return tile.getInventory().isItemValid(2, stack);
            }
        });

        // 符文槽（左下）- 槽位3
        this.addSlotToContainer(new SlotItemHandler(tile.getInventory(), 3, SLOT_3_X, SLOT_3_Y) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                // 只允许转移符文
                return TransferRuneManager.isValidRune(stack);
            }
        });

        // ==========================
        // 玩家背包槽（3×9）
        // ==========================

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(
                        playerInv,
                        col + row * 9 + 9,
                        PLAYER_INV_START_X + col * PLAYER_INV_SPACING,
                        PLAYER_INV_START_Y + row * PLAYER_INV_SPACING
                ));
            }
        }

        // ==========================
        // 玩家快捷栏（1×9）
        // ==========================

        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(
                    playerInv,
                    col,
                    PLAYER_INV_START_X + col * PLAYER_INV_SPACING,
                    HOTBAR_Y
            ));
        }
    }

    // ==========================================
    // 业务方法（GUI / Packet 会调用）
    // ==========================================

    /**
     * 执行转移操作（PacketTransferGem 会调用）
     */
    public boolean performTransfer(EntityPlayer player) {
        if (tile.getWorld().isRemote) {
            return false; // 客户端不执行
        }
        return tile.performTransfer(player);
    }

    /**
     * 检查是否可以转移（GUI 用来控制按钮状态）
     */
    public boolean canTransfer() {
        return tile.canTransfer();
    }

    /**
     * 获取错误信息（GUI 状态栏/tooltip 用）
     */
    public String getErrorMessage() {
        return tile.getErrorMessage();
    }

    /**
     * 获取需要的经验等级
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
     * 获取 TileEntity（如果 GUI 以后要用到）
     */
    public TileEntityTransferStation getTile() {
        return tile;
    }

    // ==========================================
    // Container 标准方法
    // ==========================================

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile.getWorld().getTileEntity(tile.getPos()) == tile
                && playerIn.getDistanceSq(
                tile.getPos().getX() + 0.5,
                tile.getPos().getY() + 0.5,
                tile.getPos().getZ() + 0.5
        ) <= 64;
    }

    /**
     * Shift+点击物品的处理
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
                // 尝试放入玩家背包+快捷栏
                if (!this.mergeItemStack(currentStack, 4, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(currentStack, itemstack);
            }
            // 从玩家背包/快捷栏放入机器（槽位4+）
            else {
                boolean placed = false;

                // 优先级1: 检查是否是符文 -> 放入槽位3（符文槽）
                if (!placed && TransferRuneManager.isValidRune(currentStack)) {
                    if (this.mergeItemStack(currentStack, 3, 4, false)) {
                        placed = true;
                    }
                }

                // 优先级2: 放入源宝石槽（槽位0）
                if (!placed && tile.getInventory().isItemValid(0, currentStack)) {
                    if (this.mergeItemStack(currentStack, 0, 1, false)) {
                        placed = true;
                    }
                }

                // 优先级3: 放入目标宝石槽（槽位2）
                if (!placed && tile.getInventory().isItemValid(2, currentStack)) {
                    if (this.mergeItemStack(currentStack, 2, 3, false)) {
                        placed = true;
                    }
                }

                // 如果都放不进去，在玩家背包/快捷栏之间搬运
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

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        // 一般不在这里返还物品，TileEntity 自己存
    }
}