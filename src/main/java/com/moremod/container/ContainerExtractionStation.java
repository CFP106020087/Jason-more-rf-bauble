package com.moremod.container;

import com.moremod.compat.crafttweaker.GemNBTHelper;
import com.moremod.tile.TileEntityExtractionStation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 提取台 Container
 * 槽位布局：
 * 0-5: 右侧6个输出槽
 * 6:   左侧输入槽
 */
public class ContainerExtractionStation extends Container {
    
    private final TileEntityExtractionStation tile;
    private final EntityPlayer player;
    
    // ============ 右侧6个输出槽（20×20格子，+2px居中16×16） ============
    public static final int RIGHT_X = 191;              // 189+2
    public static final int RIGHT_SLOT_0_Y = 33;        // 31+2
    public static final int RIGHT_SLOT_1_Y = 76;        // 74+2
    public static final int RIGHT_SLOT_2_Y = 120;       // 118+2
    public static final int RIGHT_SLOT_3_Y = 167;       // 165+2
    public static final int RIGHT_SLOT_4_Y = 211;       // 209+2
    public static final int RIGHT_SLOT_5_Y = 255;       // 253+2
    
    // ============ 左侧输入槽（20×20格子，+2px居中16×16） ============
    public static final int LEFT_SLOT_X = 51;           // 49+2
    public static final int LEFT_SLOT_Y = 142;          // 140+2
    
    // ============ 玩家背包（16×16槽位，间距18） ============
    public static final int PLAYER_INV_START_X = 49;    // 48.5→49
    public static final int PLAYER_INV_START_Y = 318;   // 317.5→318
    public static final int PLAYER_INV_SPACING = 18;
    
    // ============ 快捷栏 ============
    public static final int HOTBAR_Y = 375;             // 374.5→375
    
    public ContainerExtractionStation(InventoryPlayer playerInv, TileEntityExtractionStation tile, EntityPlayer player) {
        this.tile = tile;
        this.player = player;
        
        // ==========================================
        // 右侧6个输出槽（槽位0-5）
        // ==========================================
        int[] rightYCoords = {
            RIGHT_SLOT_0_Y, RIGHT_SLOT_1_Y, RIGHT_SLOT_2_Y,
            RIGHT_SLOT_3_Y, RIGHT_SLOT_4_Y, RIGHT_SLOT_5_Y
        };
        
        for (int i = 0; i < 6; i++) {
            final int index = i;
            this.addSlotToContainer(new SlotItemHandler(
                tile.getInventory(), i, RIGHT_X, rightYCoords[i]
            ) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return false; // 输出槽不允许放入
                }
                
                @Override
                public ItemStack onTake(EntityPlayer player, ItemStack stack) {
                    // 取出时可以添加额外逻辑
                    return super.onTake(player, stack);
                }
            });
        }
        
        // ==========================================
        // 左侧输入槽（槽位6）
        // ==========================================
        this.addSlotToContainer(new SlotItemHandler(
            tile.getInventory(), 6, LEFT_SLOT_X, LEFT_SLOT_Y
        ) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                // 只接受已鉴定的宝石
                return GemNBTHelper.isGem(stack) && GemNBTHelper.isIdentified(stack);
            }
        });
        
        // ==========================================
        // 玩家背包 3×9（槽位7-33）
        // ==========================================
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
        
        // ==========================================
        // 快捷栏 1×9（槽位34-42）
        // ==========================================
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
    // Container标准方法
    // ==========================================
    
    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile.getWorld().getTileEntity(tile.getPos()) == tile
            && playerIn.getDistanceSq(tile.getPos().add(0.5, 0.5, 0.5)) <= 64;
    }
    
    /**
     * Shift+点击物品的处理
     */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            
            // 从机器输出槽取出（槽位0-5）
            if (index < 6) {
                if (!this.mergeItemStack(itemstack1, 7, 43, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(itemstack1, itemstack);
            }
            // 从输入槽取出（槽位6）
            else if (index == 6) {
                if (!this.mergeItemStack(itemstack1, 7, 43, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 从玩家背包/快捷栏放入机器
            else if (GemNBTHelper.isGem(itemstack1) && GemNBTHelper.isIdentified(itemstack1)) {
                // 尝试放入输入槽（槽位6）
                if (!this.mergeItemStack(itemstack1, 6, 7, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // 在玩家背包和快捷栏之间搬运
            else if (index < 34) {
                // 从背包到快捷栏
                if (!this.mergeItemStack(itemstack1, 34, 43, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 43 && !this.mergeItemStack(itemstack1, 7, 34, false)) {
                // 从快捷栏到背包
                return ItemStack.EMPTY;
            }
            
            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
            
            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            
            slot.onTake(playerIn, itemstack1);
        }
        
        return itemstack;
    }
    
    /**
     * 获取TileEntity
     */
    public TileEntityExtractionStation getTile() {
        return tile;
    }
    
    /**
     * 获取玩家
     */
    public EntityPlayer getPlayer() {
        return player;
    }
}