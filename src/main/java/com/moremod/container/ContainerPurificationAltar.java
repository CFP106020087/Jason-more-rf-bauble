package com.moremod.container;

import com.moremod.tile.TileEntityPurificationAltar;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 提纯祭坛 - Container
 * 
 * 槽位布局：
 * - 槽位 0-5: 左侧6个垂直输入槽
 * - 槽位 6: 右侧主输出槽
 * - 槽位 7-33: 玩家背包 (27个)
 * - 槽位 34-42: 快捷栏 (9个)
 */
public class ContainerPurificationAltar extends Container {
    
    private final TileEntityPurificationAltar tile;
    private final EntityPlayer player;
    
    // 槽位索引常量
    private static final int INPUT_SLOT_START = 0;
    private static final int INPUT_SLOT_END = 5;      // 包含
    private static final int OUTPUT_SLOT = 6;
    private static final int PLAYER_INV_START = 7;
    private static final int PLAYER_INV_END = 33;     // 包含
    private static final int HOTBAR_START = 34;
    private static final int HOTBAR_END = 42;         // 包含
    
    public ContainerPurificationAltar(InventoryPlayer playerInv, TileEntityPurificationAltar tile) {
        this.tile = tile;
        this.player = playerInv.player;
        
        // ===============================
        // 左侧6个垂直输入槽
        // ===============================
        int[] inputY = {30, 74, 119, 167, 213, 253};
        int[] inputX = {46, 46, 46, 46, 47, 46};  // Slot 4 的x是47
        
        for (int i = 0; i < 6; i++) {
            this.addSlotToContainer(new SlotGemInput(tile.getInventory(), i, 
                inputX[i], inputY[i]));
        }
        
        // ===============================
        // 右侧主输出槽
        // ===============================
        this.addSlotToContainer(new SlotOutput(tile.getInventory(), 6, 190, 142));
        
        // ===============================
        // 玩家背包（3行9列）
        // ===============================
        // 起始: (48, 319), 间距: 18  (原47,318 → 往右1往下1)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(playerInv, 
                    col + row * 9 + 9,  // 背包槽位从9开始
                    48 + col * 18, 
                    319 + row * 18));
            }
        }
        
        // ===============================
        // 快捷栏（1行9列）
        // ===============================
        // 起始: (48, 377), 间距: 18  (原47,376 → 往右1往下1)
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(playerInv, col, 
                48 + col * 18, 377));
        }
    }
    
    // ==========================================
    // 槽位类定义
    // ==========================================
    
    /**
     * 宝石输入槽 - 只接受未鉴定宝石
     */
    private static class SlotGemInput extends SlotItemHandler {
        public SlotGemInput(net.minecraftforge.items.IItemHandler itemHandler, 
                           int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }
        
        @Override
        public boolean isItemValid(ItemStack stack) {
            // TODO: 添加检查 - 只接受未鉴定宝石
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
            
            // 从输出槽取出 (索引6)
            if (index == OUTPUT_SLOT) {
                // 输出槽 → 玩家背包+快捷栏
                if (!this.mergeItemStack(stackInSlot, PLAYER_INV_START, HOTBAR_END + 1, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(stackInSlot, itemstack);
            }
            // 从玩家背包/快捷栏放入
            else if (index >= PLAYER_INV_START) {
                // 尝试放入输入槽 (0-5)
                if (!this.mergeItemStack(stackInSlot, INPUT_SLOT_START, INPUT_SLOT_END + 1, false)) {
                    // 背包 ↔ 快捷栏
                    if (index <= PLAYER_INV_END) {
                        // 背包 → 快捷栏
                        if (!this.mergeItemStack(stackInSlot, HOTBAR_START, HOTBAR_END + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        // 快捷栏 → 背包
                        if (!this.mergeItemStack(stackInSlot, PLAYER_INV_START, PLAYER_INV_END + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }
            // 从输入槽取出 (0-5)
            else if (index <= INPUT_SLOT_END) {
                if (!this.mergeItemStack(stackInSlot, PLAYER_INV_START, HOTBAR_END + 1, false)) {
                    return ItemStack.EMPTY;
                }
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
    
    public boolean startPurifying() {
        return tile.startPurifying(player);
    }
    
    public boolean canPurify() {
        return tile.canPurify();
    }
    
    public int getInputGemCount() {
        return tile.getInputGemCount();
    }
    
    public int getPredictedQuality() {
        return tile.getPredictedQuality();
    }
    
    public int getRequiredXP() {
        return tile.getRequiredXP();
    }
    
    public boolean isPurifying() {
        return tile.isPurifying();
    }
    
    public int getPurifyProgress() {
        return tile.getPurifyProgress();
    }
    
    public int getMaxPurifyTime() {
        return tile.getMaxPurifyTime();
    }
    
    public TileEntityPurificationAltar getTile() {
        return tile;
    }
    
    public BlockPos getTilePos() {
        return tile.getPos();
    }
    
    public EntityPlayer getPlayer() {
        return player;
    }
}