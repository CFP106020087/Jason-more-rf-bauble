package com.moremod.container;

import com.moremod.tile.TileEntitySwordUpgradeStation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/**
 * 剑升级站 Container - 宝石系统（新版）
 *
 * 核心改动：
 * - getInlayList() 返回宝石镶嵌信息（包含完整宝石和词条）
 * - 不再使用materialId字符串
 */
public class ContainerSwordUpgradeStation extends Container {

    // ==================== GUI坐标常量 ====================

    public static final int GUI_WIDTH = 256;
    public static final int GUI_HEIGHT = 256;
    public static final int SLOT_SIZE = 18;
    public static final int SLOT_INSET = 1;

    // 输出槽
    public static final int OUTPUT_X = 121;
    public static final int OUTPUT_Y = 56;

    // 宝石网格（2x3）
    public static final int MATERIAL_GRID_X = 26;
    public static final int MATERIAL_GRID_Y = 37;
    public static final int MATERIAL_COLS = 2;
    public static final int MATERIAL_ROWS = 3;

    // 剑槽
    public static final int SWORD_X = 35;
    public static final int SWORD_Y = 91;

    // 玩家背包
    public static final int PLAYER_INV_X = 7;
    public static final int PLAYER_INV_Y = 139;
    public static final int PLAYER_INV_COLS = 9;
    public static final int PLAYER_INV_ROWS = 3;

    // 快捷栏
    public static final int HOTBAR_X = 7;
    public static final int HOTBAR_Y = PLAYER_INV_Y + PLAYER_INV_ROWS * SLOT_SIZE + 3;
    public static final int HOTBAR_COLS = 9;

    // ==================== 槽位索引 ====================

    public static final int SLOT_OUTPUT = TileEntitySwordUpgradeStation.SLOT_OUTPUT;
    public static final int SLOT_MAT0 = TileEntitySwordUpgradeStation.SLOT_MAT0;
    public static final int SLOT_MAT1 = TileEntitySwordUpgradeStation.SLOT_MAT1;
    public static final int SLOT_MAT2 = TileEntitySwordUpgradeStation.SLOT_MAT2;
    public static final int SLOT_MAT3 = TileEntitySwordUpgradeStation.SLOT_MAT3;
    public static final int SLOT_MAT4 = TileEntitySwordUpgradeStation.SLOT_MAT4;
    public static final int SLOT_MAT5 = TileEntitySwordUpgradeStation.SLOT_MAT5;
    public static final int SLOT_SWORD = TileEntitySwordUpgradeStation.SLOT_SWORD;

    public static final int MATERIAL_SLOT_START = TileEntitySwordUpgradeStation.MATERIAL_SLOT_START;
    public static final int MATERIAL_SLOT_END = TileEntitySwordUpgradeStation.MATERIAL_SLOT_END;

    // ==================== 实例变量 ====================

    private final TileEntitySwordUpgradeStation tile;
    private final IItemHandler handler;

    // ==================== 构造函数 ====================

    public ContainerSwordUpgradeStation(InventoryPlayer playerInv, TileEntitySwordUpgradeStation tile) {
        this.tile = tile;
        this.handler = tile.getCapability(
                net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

        if (handler == null) {
            throw new IllegalStateException("TileEntitySwordUpgradeStation has no ITEM_HANDLER capability!");
        }

        // 添加升级站槽位
        addUpgradeStationSlots();

        // 添加玩家物品栏
        addPlayerInventorySlots(playerInv);
    }

    /**
     * 添加升级站槽位
     */
    private void addUpgradeStationSlots() {
        // 槽位0: 输出槽
        this.addSlotToContainer(new SlotItemHandler(handler, SLOT_OUTPUT,
                OUTPUT_X + SLOT_INSET, OUTPUT_Y + SLOT_INSET) {
            @Override
            public void onSlotChanged() {
                super.onSlotChanged();
                tile.updatePreview();
            }

            @Override
            public boolean canTakeStack(EntityPlayer playerIn) {
                ItemStack stack = this.getStack();
                if (!stack.isEmpty() && stack.hasTagCompound() &&
                        stack.getTagCompound().getBoolean("Preview")) {
                    return false;
                }
                return super.canTakeStack(playerIn);
            }
        });

        // 槽位1-6: 宝石网格（2x3）
        int[] matSlots = {SLOT_MAT0, SLOT_MAT1, SLOT_MAT2, SLOT_MAT3, SLOT_MAT4, SLOT_MAT5};
        int idx = 0;

        for (int row = 0; row < MATERIAL_ROWS; row++) {
            for (int col = 0; col < MATERIAL_COLS; col++) {
                final int slotIndex = matSlots[idx];
                int x = MATERIAL_GRID_X + col * SLOT_SIZE + SLOT_INSET;
                int y = MATERIAL_GRID_Y + row * SLOT_SIZE + SLOT_INSET;

                this.addSlotToContainer(new SlotItemHandler(handler, slotIndex, x, y) {
                    @Override
                    public void onSlotChanged() {
                        super.onSlotChanged();
                        tile.updatePreview();
                    }

                    @Override
                    public boolean canTakeStack(EntityPlayer playerIn) {
                        ItemStack stack = this.getStack();
                        if (!stack.isEmpty() && stack.hasTagCompound() &&
                                stack.getTagCompound().getBoolean("Preview")) {
                            return false;
                        }
                        return super.canTakeStack(playerIn);
                    }
                });
                idx++;
            }
        }

        // 槽位7: 剑槽
        this.addSlotToContainer(new SlotItemHandler(handler, SLOT_SWORD,
                SWORD_X + SLOT_INSET, SWORD_Y + SLOT_INSET) {
            @Override
            public void onSlotChanged() {
                super.onSlotChanged();
                tile.updatePreview();
            }

            @Override
            public boolean canTakeStack(EntityPlayer playerIn) {
                ItemStack stack = this.getStack();
                if (!stack.isEmpty() && stack.hasTagCompound() &&
                        stack.getTagCompound().getBoolean("Preview")) {
                    return false;
                }
                return super.canTakeStack(playerIn);
            }
        });
    }

    /**
     * 添加玩家物品栏
     */
    private void addPlayerInventorySlots(InventoryPlayer playerInv) {
        // 主背包（9x3）
        for (int row = 0; row < PLAYER_INV_ROWS; row++) {
            for (int col = 0; col < PLAYER_INV_COLS; col++) {
                int x = PLAYER_INV_X + col * SLOT_SIZE + SLOT_INSET;
                int y = PLAYER_INV_Y + row * SLOT_SIZE + SLOT_INSET;
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, x, y));
            }
        }

        // 快捷栏（9x1）
        for (int col = 0; col < HOTBAR_COLS; col++) {
            int x = HOTBAR_X + col * SLOT_SIZE + SLOT_INSET;
            int y = HOTBAR_Y + SLOT_INSET;
            this.addSlotToContainer(new Slot(playerInv, col, x, y));
        }
    }

    // ==================== Container方法 ====================

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        tile.updatePreview();
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer playerIn) {
        return tile.isUsableByPlayer(playerIn);
    }

    /**
     * Shift+点击逻辑
     */
    @Nonnull
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return empty;

        ItemStack stackInSlot = slot.getStack();
        ItemStack stackCopy = stackInSlot.copy();

        final int OUTPUT = 0;
        final int MAT_START = 1;
        final int MAT_END = 6;
        final int SWORD = 7;
        final int INV_START = 8;
        final int INV_END = INV_START + 27; // 35
        final int HOT_START = INV_END;
        final int HOT_END = HOT_START + 9; // 44

        TileEntitySwordUpgradeStation.Mode mode = tile.getCurrentMode();

        if (index == OUTPUT) {
            // 输出槽 -> 玩家背包
            if (mode == TileEntitySwordUpgradeStation.Mode.UPGRADE) {
                if (!this.mergeItemStack(stackInSlot, INV_START, HOT_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(stackInSlot, stackCopy);
                slot.onTake(playerIn, stackInSlot);
            } else {
                if (!this.mergeItemStack(stackInSlot, INV_START, HOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (index >= MAT_START && index <= MAT_END) {
            // 宝石槽 -> 玩家背包
            if (mode == TileEntitySwordUpgradeStation.Mode.REMOVAL) {
                return ItemStack.EMPTY;
            }
            if (!this.mergeItemStack(stackInSlot, INV_START, HOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index == SWORD) {
            // 剑槽 -> 玩家背包
            if (mode == TileEntitySwordUpgradeStation.Mode.REMOVAL) {
                return ItemStack.EMPTY;
            }
            if (!this.mergeItemStack(stackInSlot, INV_START, HOT_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 玩家背包 -> 升级站
            if (stackInSlot.getItem() instanceof net.minecraft.item.ItemSword) {
                if (mode == TileEntitySwordUpgradeStation.Mode.IDLE ||
                        mode == TileEntitySwordUpgradeStation.Mode.UPGRADE) {
                    if (!this.mergeItemStack(stackInSlot, SWORD, SWORD + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (mode == TileEntitySwordUpgradeStation.Mode.REMOVAL) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 材料物品
                if (mode == TileEntitySwordUpgradeStation.Mode.UPGRADE ||
                        mode == TileEntitySwordUpgradeStation.Mode.IDLE) {
                    if (!this.mergeItemStack(stackInSlot, MAT_START, MAT_END + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stackInSlot.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }

        if (stackInSlot.getCount() == stackCopy.getCount()) {
            return ItemStack.EMPTY;
        }

        return stackCopy;
    }

    // ==================== 业务逻辑方法 ====================

    public void performStarUpgrade() {
        tile.performStarUpgrade();
    }

    public void performRemoveAll(EntityPlayer player) {
        tile.removeAllGems(player);
    }

    public void performRemoveSingleGem(int slotIndex, EntityPlayer player) {
        tile.removeSingleGem(slotIndex, player);
    }

    public boolean canPerformStarUpgrade() {
        return tile.canPerformStarUpgrade();
    }

    public boolean canPerformRemoveAll() {
        return tile.canPerformRemoveAll();
    }

    public TileEntitySwordUpgradeStation.Mode getCurrentMode() {
        return tile.getCurrentMode();
    }

    /**
     * ✅ 核心改动：获取镶嵌列表（新宝石系统）
     * 返回包含完整宝石物品和词条的列表
     */
    public java.util.List<TileEntitySwordUpgradeStation.InlayInfo> getInlayList() {
        return tile.getInlayList();
    }

    public TileEntitySwordUpgradeStation getTile() {
        return tile;
    }
}