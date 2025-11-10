package com.moremod.container;

import com.moremod.recipe.SwordUpgradeRegistry;
import com.moremod.tile.TileEntitySwordUpgradeStationMaterial;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerSwordUpgradeStationMaterial extends Container {

    private final TileEntitySwordUpgradeStationMaterial tile;

    public ContainerSwordUpgradeStationMaterial(InventoryPlayer playerInv, TileEntitySwordUpgradeStationMaterial tile) {
        this.tile = tile;
        IItemHandler handler = tile.getCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

        // 0: 基底剑  1: 材料  2: 输出
        this.addSlotToContainer(new SlotItemHandler(handler, TileEntitySwordUpgradeStationMaterial.SLOT_BASE, 44, 35));
        this.addSlotToContainer(new SlotItemHandler(handler, TileEntitySwordUpgradeStationMaterial.SLOT_MAT,  80, 35));
        this.addSlotToContainer(new OutputSlot(handler, TileEntitySwordUpgradeStationMaterial.SLOT_OUT, 134, 35));

        // 玩家背包
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int hot = 0; hot < 9; ++hot)
            this.addSlotToContainer(new Slot(playerInv, hot, 8 + hot * 18, 142));
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return this.tile.getWorld().getTileEntity(this.tile.getPos()) == this.tile &&
                playerIn.getDistanceSq(this.tile.getPos().add(0.5, 0.5, 0.5)) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();

            final int teSlotCount = 3; // 0..2
            if (index < teSlotCount) {
                // 从台子到玩家
                if (!this.mergeItemStack(stackInSlot, teSlotCount, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家到台子（优先材料槽，其次基底槽）
                if (!this.mergeItemStack(stackInSlot,
                        TileEntitySwordUpgradeStationMaterial.SLOT_MAT,
                        TileEntitySwordUpgradeStationMaterial.SLOT_MAT + 1, false)) {
                    if (!this.mergeItemStack(stackInSlot,
                            TileEntitySwordUpgradeStationMaterial.SLOT_BASE,
                            TileEntitySwordUpgradeStationMaterial.SLOT_BASE + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stackInSlot.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();

            if (stackInSlot.getCount() == itemstack.getCount()) return ItemStack.EMPTY;

            slot.onTake(playerIn, stackInSlot);
        }
        return itemstack;
    }

    private class OutputSlot extends SlotItemHandler {
        public OutputSlot(IItemHandler itemHandler, int index, int x, int y) {
            super(itemHandler, index, x, y);
        }
        @Override public boolean isItemValid(ItemStack stack) { return false; }

        // 只按 XP 判定（避免 super.canTakeStack 的“模拟抽取”误判）
        @Override
        public boolean canTakeStack(EntityPlayer player) {
            ItemStack out = getStack();
            if (out.isEmpty()) return false;

            int needXp = 0;
            ItemStack mat = tile.getStackInSlot(TileEntitySwordUpgradeStationMaterial.SLOT_MAT);
            if (!mat.isEmpty()) {
                SwordUpgradeRegistry.Recipe r = SwordUpgradeRegistry.getRecipe(mat.getItem());
                needXp = (r == null) ? 0 : r.xpCost;
            }
            return getPlayerTotalXp(player) >= needXp;
        }

        @Override
        public ItemStack onTake(EntityPlayer player, ItemStack stack) {
            int needXp = 0;
            ItemStack mat = tile.getStackInSlot(TileEntitySwordUpgradeStationMaterial.SLOT_MAT);
            if (!mat.isEmpty()) {
                SwordUpgradeRegistry.Recipe r = SwordUpgradeRegistry.getRecipe(mat.getItem());
                needXp = (r == null) ? 0 : r.xpCost;
            }
            if (needXp > 0) player.addExperience(-needXp);

            // 真正消耗材料与基底
            tile.finishUpgrade();
            return super.onTake(player, stack);
        }

        private int getPlayerTotalXp(EntityPlayer p) {
            int level = p.experienceLevel;
            int total = getExperienceForLevel(level);
            total += Math.round(p.experience * p.xpBarCap());
            return total;
        }
        private int getExperienceForLevel(int level) {
            if (level <= 0) return 0;
            if (level <= 16) return level * level + 6 * level;
            else if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
            else return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }
}
