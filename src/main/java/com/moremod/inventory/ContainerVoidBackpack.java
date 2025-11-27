package com.moremod.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * 虚空背包容器
 * 支持可变大小的虚空背包（9/18/27格）+ 玩家完整背包
 */
public class ContainerVoidBackpack extends Container {

    private final IInventory voidInventory;
    private final EntityPlayer player;
    private final int numRows;
    private final int voidSlotCount;

    /**
     * 构造函数
     * @param player 玩家对象
     * @param voidInv 虚空背包库存
     * @param size 虚空背包总容量（9/18/27）
     */
    public ContainerVoidBackpack(EntityPlayer player, IInventory voidInv, int size) {
        this.player = player;
        this.voidInventory = voidInv;
        this.numRows = size / 9;
        this.voidSlotCount = size;

        System.out.println("[ContainerVoidBackpack] 创建容器");
        System.out.println("[ContainerVoidBackpack] 虚空背包容量: " + size + " 格");
        System.out.println("[ContainerVoidBackpack] 虚空背包行数: " + numRows);

        // 添加虚空背包槽位
        addVoidBackpackSlots(voidInv);

        // 添加玩家背包槽位
        addPlayerInventorySlots(player.inventory);

        System.out.println("[ContainerVoidBackpack] 总槽位数: " + this.inventorySlots.size());
    }

    /**
     * 重载构造函数 - 兼容 InventoryPlayer 参数
     */
    public ContainerVoidBackpack(InventoryPlayer playerInv, IInventory voidInv, int size) {
        this(playerInv.player, voidInv, size);
    }

    /**
     * 添加虚空背包槽位
     */
    private void addVoidBackpackSlots(IInventory voidInv) {
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9;
                int x = 8 + col * 18;
                int y = 18 + row * 18;

                this.addSlotToContainer(new Slot(voidInv, slotIndex, x, y));
            }
        }
        System.out.println("[ContainerVoidBackpack] 已添加 " + voidSlotCount + " 个虚空背包槽位");
    }

    /**
     * 添加玩家背包槽位
     */
    private void addPlayerInventorySlots(InventoryPlayer playerInv) {
        // 玩家背包起始Y坐标
        int playerInvY = 18 + numRows * 18 + 14;

        // 玩家背包主体（3行x9列 = 27格）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9; // 9-35
                int x = 8 + col * 18;
                int y = playerInvY + row * 18;

                this.addSlotToContainer(new Slot(playerInv, slotIndex, x, y));
            }
        }

        // 玩家快捷栏（1行x9列 = 9格）
        int hotbarY = playerInvY + 3 * 18 + 4;
        for (int col = 0; col < 9; col++) {
            int slotIndex = col; // 0-8
            int x = 8 + col * 18;

            this.addSlotToContainer(new Slot(playerInv, slotIndex, x, hotbarY));
        }

        System.out.println("[ContainerVoidBackpack] 已添加 36 个玩家背包槽位");
    }

    /**
     * 检查玩家是否可以与容器交互
     */
    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        // 虚空背包始终可以访问（因为是全局共享的）
        return true;
    }

    /**
     * Shift+点击物品转移
     */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            itemstack = slotStack.copy();

            // 虚空背包槽位范围：0 到 (voidSlotCount - 1)
            // 玩家背包槽位范围：voidSlotCount 到 (voidSlotCount + 35)

            if (index < voidSlotCount) {
                // 点击的是虚空背包槽位 -> 尝试转移到玩家背包
                System.out.println("[ContainerVoidBackpack] Shift+点击虚空背包槽位 " + index + "，转移到玩家背包");

                if (!this.mergeItemStack(slotStack, voidSlotCount, this.inventorySlots.size(), true)) {
                    System.out.println("[ContainerVoidBackpack] 转移失败：玩家背包已满");
                    return ItemStack.EMPTY;
                }
            } else {
                // 点击的是玩家背包槽位 -> 尝试转移到虚空背包
                System.out.println("[ContainerVoidBackpack] Shift+点击玩家背包槽位 " + index + "，转移到虚空背包");

                if (!this.mergeItemStack(slotStack, 0, voidSlotCount, false)) {
                    System.out.println("[ContainerVoidBackpack] 转移失败：虚空背包已满");
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, slotStack);
        }

        return itemstack;
    }

    /**
     * 容器关闭时调用
     */
    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        System.out.println("[ContainerVoidBackpack] 容器已关闭");

        // 虚空背包是持久化的，不需要在这里掉落物品
        // 如果你的 InventoryVoidBackpack 需要保存数据，在这里调用保存方法
        if (voidInventory instanceof InventoryVoidBackpack) {
            ((InventoryVoidBackpack) voidInventory).markDirty();
        }
    }

    /**
     * 检测容器内容变化
     */
    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        // 如果需要同步虚空背包数据到客户端，在这里处理
    }

    /**
     * 获取虚空背包槽位数量
     */
    public int getVoidSlotCount() {
        return voidSlotCount;
    }

    /**
     * 获取虚空背包行数
     */
    public int getNumRows() {
        return numRows;
    }

    /**
     * 调试方法：打印所有槽位信息
     */
    public void debugPrintSlots() {
        System.out.println("[ContainerVoidBackpack] ===== 槽位调试信息 =====");
        System.out.println("[ContainerVoidBackpack] 虚空背包槽位: 0-" + (voidSlotCount - 1));
        System.out.println("[ContainerVoidBackpack] 玩家背包槽位: " + voidSlotCount + "-" + (voidSlotCount + 35));
        System.out.println("[ContainerVoidBackpack] 总槽位数: " + this.inventorySlots.size());

        for (int i = 0; i < this.inventorySlots.size(); i++) {
            Slot slot = this.inventorySlots.get(i);
            if (slot.getHasStack()) {
                System.out.println("[ContainerVoidBackpack] 槽位 " + i + ": " +
                        slot.getStack().getDisplayName() + " x" + slot.getStack().getCount());
            }
        }
        System.out.println("[ContainerVoidBackpack] ========================");
    }
}