package com.moremod.quarry.gui;

import com.moremod.quarry.QuarryMode;
import com.moremod.quarry.tile.TileQuantumQuarry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 量子采石场 GUI Container
 */
public class ContainerQuantumQuarry extends Container {
    
    private final TileQuantumQuarry tile;
    private final InventoryPlayer playerInventory;
    
    // 同步数据
    private int lastEnergy = -1;
    private int lastMode = -1;
    private int lastBiomeId = -1;
    private int lastProgress = -1;
    private boolean lastStructureValid = false;
    private boolean lastRedstoneControl = false;
    
    // 槽位索引
    private static final int ENCHANT_SLOT = 0;
    private static final int FILTER_SLOT = 1;
    private static final int OUTPUT_START = 2;
    private static final int OUTPUT_END = 19;  // 18 个输出槽
    private static final int PLAYER_INV_START = 20;
    private static final int PLAYER_INV_END = 55;  // 36 个玩家槽位
    
    public ContainerQuantumQuarry(InventoryPlayer playerInventory, TileQuantumQuarry tile) {
        this.tile = tile;
        this.playerInventory = playerInventory;
        
        // 附魔书槽位 (8, 17)
        // 这里需要通过反射或公开方法获取 enchantSlot
        // 临时方案：直接访问 capability
        
        // 由于 TileQuantumQuarry 的槽位是 private，我们需要添加 getter 方法
        // 这里假设已经添加了 getEnchantSlot() 和 getFilterSlot() 方法
        
        // 附魔书槽位
        addSlotToContainer(new SlotItemHandler(tile.getEnchantSlot(), 0, 8, 17));
        
        // 过滤器槽位
        addSlotToContainer(new SlotItemHandler(tile.getFilterSlot(), 0, 8, 53));
        
        // 输出缓冲槽位 (3x6)
        IItemHandler outputBuffer = tile.getOutputBuffer();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                int index = row * 6 + col;
                int x = 62 + col * 18;
                int y = 17 + row * 18;
                addSlotToContainer(new SlotItemHandler(outputBuffer, index, x, y) {
                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        return false;  // 输出槽不能放入物品
                    }
                });
            }
        }
        
        // 玩家物品栏
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        
        // 玩家快捷栏
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }
    
    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        
        for (IContainerListener listener : listeners) {
            // 同步能量
            if (lastEnergy != tile.getEnergyStored()) {
                listener.sendWindowProperty(this, 0, tile.getEnergyStored() & 0xFFFF);
                listener.sendWindowProperty(this, 1, (tile.getEnergyStored() >> 16) & 0xFFFF);
            }
            
            // 同步模式
            if (lastMode != tile.getMode().getMeta()) {
                listener.sendWindowProperty(this, 2, tile.getMode().getMeta());
            }
            
            // 同步生物群系
            int biomeId = tile.getSelectedBiome() != null ? 
                Biome.REGISTRY.getIDForObject(tile.getSelectedBiome()) : -1;
            if (lastBiomeId != biomeId) {
                listener.sendWindowProperty(this, 3, biomeId);
            }
            
            // 同步进度
            if (lastProgress != tile.getProgress()) {
                listener.sendWindowProperty(this, 4, tile.getProgress());
            }
            
            // 同步结构状态
            if (lastStructureValid != tile.isStructureValid()) {
                listener.sendWindowProperty(this, 5, tile.isStructureValid() ? 1 : 0);
            }
            
            // 同步红石控制
            if (lastRedstoneControl != tile.isRedstoneControlEnabled()) {
                listener.sendWindowProperty(this, 6, tile.isRedstoneControlEnabled() ? 1 : 0);
            }
        }
        
        lastEnergy = tile.getEnergyStored();
        lastMode = tile.getMode().getMeta();
        lastBiomeId = tile.getSelectedBiome() != null ? 
            Biome.REGISTRY.getIDForObject(tile.getSelectedBiome()) : -1;
        lastProgress = tile.getProgress();
        lastStructureValid = tile.isStructureValid();
        lastRedstoneControl = tile.isRedstoneControlEnabled();
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data) {
        switch (id) {
            case 0:
                // 能量低16位
                int energy = tile.getEnergyStored();
                tile.setClientEnergy((energy & 0xFFFF0000) | (data & 0xFFFF));
                break;
            case 1:
                // 能量高16位
                energy = tile.getEnergyStored();
                tile.setClientEnergy((energy & 0x0000FFFF) | ((data & 0xFFFF) << 16));
                break;
            case 2:
                tile.setMode(QuarryMode.fromMeta(data));
                break;
            case 3:
                tile.setSelectedBiomeById(data);
                break;
            case 4:
                tile.setClientProgress(data);
                break;
            case 5:
                tile.setClientStructureValid(data == 1);
                break;
            case 6:
                tile.setRedstoneControlEnabled(data == 1);
                break;
        }
    }
    
    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return playerIn.getDistanceSq(tile.getPos()) <= 64;
    }
    
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        
        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            itemstack = slotStack.copy();
            
            // 从机器槽位转移到玩家
            if (index < PLAYER_INV_START) {
                if (!mergeItemStack(slotStack, PLAYER_INV_START, PLAYER_INV_END + 1, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 从玩家转移到机器
            else {
                // 尝试放入附魔槽
                if (tile.getEnchantSlot().isItemValid(0, slotStack)) {
                    if (!mergeItemStack(slotStack, ENCHANT_SLOT, ENCHANT_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // 尝试放入过滤槽
                else if (!mergeItemStack(slotStack, FILTER_SLOT, FILTER_SLOT + 1, false)) {
                    // 玩家物品栏和快捷栏之间转移
                    if (index < PLAYER_INV_START + 27) {
                        if (!mergeItemStack(slotStack, PLAYER_INV_START + 27, PLAYER_INV_END + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        if (!mergeItemStack(slotStack, PLAYER_INV_START, PLAYER_INV_START + 27, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
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
    
    // ==================== 按钮操作 ====================
    
    /**
     * 处理 GUI 按钮点击
     */
    public void handleButtonClick(int buttonId, EntityPlayer player) {
        switch (buttonId) {
            case 0:
                // 切换模式
                tile.cycleMode();
                break;
            case 1:
                // 切换红石控制
                tile.toggleRedstoneControl();
                break;
            // 生物群系选择由专门的包处理
        }
    }
    
    public TileQuantumQuarry getTile() {
        return tile;
    }
}
