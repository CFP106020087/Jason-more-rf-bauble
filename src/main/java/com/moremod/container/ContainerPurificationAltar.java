package com.moremod.container;

import com.moremod.tile.TileEntityPurificationAltar;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 提纯祭坛 - Container
 * 
 * 槽位布局：
 * - 槽位 0-4: 输入槽（5个宝石）
 * - 槽位 5: 输出槽（提纯后的宝石）
 * - 槽位 6-41: 玩家背包+快捷栏
 */
public class ContainerPurificationAltar extends Container {
    
    private final TileEntityPurificationAltar tile;
    private final EntityPlayer player;
    
    public ContainerPurificationAltar(InventoryPlayer playerInv, TileEntityPurificationAltar tile) {
        this.tile = tile;
        this.player = playerInv.player;
        
        // 输入槽（5个）- 排列成弧形
        // 槽位位置计算：以中心为基准，向两侧扩展
        int[] xPositions = {26, 44, 62, 80, 98}; // 5个槽位的X坐标
        int centerY = 26; // Y坐标
        
        for (int i = 0; i < 5; i++) {
            this.addSlotToContainer(new SlotGemInput(tile.getInventory(), i, 
                xPositions[i], centerY));
        }
        
        // 输出槽 - 居中靠下
        this.addSlotToContainer(new SlotOutput(tile.getInventory(), 5, 62, 70));
        
        // 玩家背包（3行9列）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(playerInv, 
                    col + row * 9 + 9, 
                    8 + col * 18, 
                    98 + row * 18));
            }
        }
        
        // 玩家快捷栏（1行9列）
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(playerInv, col, 
                8 + col * 18, 156));
        }
    }
    
    // ==========================================
    // 槽位类定义
    // ==========================================
    
    /**
     * 宝石输入槽 - 只接受精炼宝石
     */
    private static class SlotGemInput extends SlotItemHandler {
        public SlotGemInput(net.minecraftforge.items.IItemHandler itemHandler, 
                           int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }
        
        @Override
        public boolean isItemValid(ItemStack stack) {
            // 可以添加更严格的检查
            // 例如：只接受精炼宝石
            return true;
        }
    }
    
    /**
     * 输出槽 - 只能取出，不能放入
     */
    private static class SlotOutput extends SlotItemHandler {
        public SlotOutput(net.minecraftforge.items.IItemHandler itemHandler, 
                         int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }
        
        @Override
        public boolean isItemValid(ItemStack stack) {
            return false; // 不能放入
        }
    }
    
    // ==========================================
    // Container 基础方法
    // ==========================================
    
    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile.getWorld().getTileEntity(tile.getPos()) == tile &&
               playerIn.getDistanceSq(tile.getPos().add(0.5, 0.5, 0.5)) <= 64.0;
    }
    
    /**
     * Shift点击处理
     */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        
        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();
            
            // 从输出槽取出
            if (index == 5) {
                // 输出槽 → 玩家背包
                if (!this.mergeItemStack(stackInSlot, 6, 42, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(stackInSlot, itemstack);
            }
            // 从玩家背包放入
            else if (index >= 6) {
                // 玩家背包 → 输入槽
                if (!this.mergeItemStack(stackInSlot, 0, 5, false)) {
                    // 背包 ↔ 快捷栏
                    if (index < 33) {
                        if (!this.mergeItemStack(stackInSlot, 33, 42, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index < 42 && 
                              !this.mergeItemStack(stackInSlot, 6, 33, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            // 从输入槽取出
            else if (!this.mergeItemStack(stackInSlot, 6, 42, false)) {
                return ItemStack.EMPTY;
            }
            
            if (stackInSlot.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
            
            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            
            slot.onTake(playerIn, stackInSlot);
        }
        
        return itemstack;
    }
    
    // ==========================================
    // 提纯控制
    // ==========================================
    
    /**
     * 开始提纯
     */
    public boolean startPurifying() {
        return tile.startPurifying(player);
    }
    
    /**
     * 检查是否可以提纯
     */
    public boolean canPurify() {
        return tile.canPurify();
    }
    
    /**
     * 获取输入宝石数量
     */
    public int getInputGemCount() {
        return tile.getInputGemCount();
    }
    
    /**
     * 获取预测品质
     */
    public int getPredictedQuality() {
        return tile.getPredictedQuality();
    }
    
    /**
     * 获取需要的经验
     */
    public int getRequiredXP() {
        return tile.getRequiredXP();
    }
    
    /**
     * 是否正在提纯
     */
    public boolean isPurifying() {
        return tile.isPurifying();
    }
    
    /**
     * 获取提纯进度
     */
    public int getPurifyProgress() {
        return tile.getPurifyProgress();
    }
    
    /**
     * 获取最大提纯时间
     */
    public int getMaxPurifyTime() {
        return tile.getMaxPurifyTime();
    }
    
    public TileEntityPurificationAltar getTile() {
        return tile;
    }
    
    public EntityPlayer getPlayer() {
        return player;
    }
}