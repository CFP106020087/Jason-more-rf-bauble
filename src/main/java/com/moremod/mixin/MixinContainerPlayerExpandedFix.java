package com.moremod.mixin;

import baubles.api.IBauble;
import baubles.api.cap.BaublesCapabilities;
import baubles.api.cap.IBaublesItemHandler;
import baubles.common.container.ContainerPlayerExpanded;
import baubles.common.container.SlotBauble;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * 修复 ContainerPlayerExpanded 的 transferStackInSlot 方法
 * 基于反编译源代码的精确修复
 */
@Mixin(value = ContainerPlayerExpanded.class, remap = false)
public abstract class MixinContainerPlayerExpandedFix extends Container {

    @Shadow
    public IBaublesItemHandler baubles;

    @Shadow
    private EntityPlayer thePlayer;

    /**
     * @author 重衡
     * @reason 修复额外槽位导致的 Shift+点击索引越界（Index: 71, Size: 71）
     */
    @Overwrite
    public ItemStack func_82846_b(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            EntityEquipmentSlot entityequipmentslot = EntityLiving.getSlotForItemStack(itemstack);

            // ⭐ 动态获取槽位数
            int slotShift = this.baubles.getSlots();
            int totalSlots = this.inventorySlots.size();

            // 计算实际的槽位范围
            int inventoryStart = 9 + slotShift;      // 背包开始
            int inventoryEnd = totalSlots;           // 背包结束
            int hotbarStart = inventoryStart;        // 快捷栏开始
            int hotbarEnd = hotbarStart + 9;         // 快捷栏结束
            int offhandSlot = inventoryEnd - 1;      // 副手槽位（最后一个）

            System.out.println("[ContainerFix] Shift+点击: index=" + index +
                    ", slotShift=" + slotShift +
                    ", totalSlots=" + totalSlots);

            if (index == 0) {
                // 合成结果槽位
                if (!this.mergeItemStack(itemstack1, inventoryStart, inventoryEnd, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(itemstack1, itemstack);

            } else if (index >= 1 && index < 5) {
                // 合成输入槽位（1-4）
                if (!this.mergeItemStack(itemstack1, inventoryStart, inventoryEnd, false)) {
                    return ItemStack.EMPTY;
                }

            } else if (index >= 5 && index < 9) {
                // 盔甲槽位（5-8）
                if (!this.mergeItemStack(itemstack1, inventoryStart, inventoryEnd, false)) {
                    return ItemStack.EMPTY;
                }

            } else if (index >= 9 && index < 9 + slotShift) {
                // Baubles 槽位（9 到 9+slotShift-1）
                if (!this.mergeItemStack(itemstack1, inventoryStart, inventoryEnd, false)) {
                    return ItemStack.EMPTY;
                }

            } else if (entityequipmentslot.getSlotType() == EntityEquipmentSlot.Type.ARMOR &&
                    !this.inventorySlots.get(8 - entityequipmentslot.getIndex()).getHasStack()) {
                // 从背包移动到盔甲槽位
                int i = 8 - entityequipmentslot.getIndex();
                if (!this.mergeItemStack(itemstack1, i, i + 1, false)) {
                    return ItemStack.EMPTY;
                }

            } else if (entityequipmentslot == EntityEquipmentSlot.OFFHAND &&
                    !this.inventorySlots.get(offhandSlot).getHasStack()) {
                // ⭐ 修复：使用正确的副手槽位索引
                if (!this.mergeItemStack(itemstack1, offhandSlot, offhandSlot + 1, false)) {
                    return ItemStack.EMPTY;
                }

            } else if (itemstack.hasCapability(BaublesCapabilities.CAPABILITY_ITEM_BAUBLE, null)) {
                // 从背包移动到 Baubles 槽位
                IBauble bauble = itemstack.getCapability(BaublesCapabilities.CAPABILITY_ITEM_BAUBLE, null);

                for (int baubleSlot : bauble.getBaubleType(itemstack).getValidSlots()) {
                    if (bauble.canEquip(itemstack1, this.thePlayer) &&
                            !this.inventorySlots.get(baubleSlot + 9).getHasStack() &&
                            !this.mergeItemStack(itemstack1, baubleSlot + 9, baubleSlot + 10, false)) {
                        return ItemStack.EMPTY;
                    }

                    if (itemstack1.getCount() == 0) {
                        break;
                    }
                }

            } else if (index >= inventoryStart && index < hotbarEnd) {
                // 快捷栏（inventoryStart 到 hotbarEnd-1）
                if (!this.mergeItemStack(itemstack1, hotbarEnd, inventoryEnd - 1, false)) {
                    // 减去 1 是因为最后一个是副手槽位
                    return ItemStack.EMPTY;
                }

            } else if (index >= hotbarEnd && index < inventoryEnd - 1) {
                // 背包主体（hotbarEnd 到 inventoryEnd-2）
                if (!this.mergeItemStack(itemstack1, hotbarStart, hotbarEnd, false)) {
                    return ItemStack.EMPTY;
                }

            } else if (!this.mergeItemStack(itemstack1, inventoryStart, inventoryEnd, false)) {
                // 其他情况
                return ItemStack.EMPTY;
            }

            // 清理和更新
            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            // 处理 Bauble 卸载事件
            if (itemstack1.isEmpty() &&
                    !this.baubles.isEventBlocked() &&
                    slot instanceof SlotBauble &&
                    itemstack.hasCapability(BaublesCapabilities.CAPABILITY_ITEM_BAUBLE, null)) {
                ((IBauble)itemstack.getCapability(BaublesCapabilities.CAPABILITY_ITEM_BAUBLE, null))
                        .onUnequipped(itemstack, playerIn);
            }

            ItemStack itemstack2 = slot.onTake(playerIn, itemstack1);
            if (index == 0) {
                playerIn.dropItem(itemstack2, false);
            }
        }

        return itemstack;
    }
}