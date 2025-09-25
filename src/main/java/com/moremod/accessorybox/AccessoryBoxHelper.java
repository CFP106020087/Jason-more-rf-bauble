package com.moremod.accessorybox;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.cap.BaublesCapabilities;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

public class AccessoryBoxHelper {

    /**
     * 檢查物品是否可以放入額外槽位 (14-20)
     */
    public static boolean isValidForExtraSlot(int slot, ItemStack stack, EntityLivingBase player) {
        // 空物品總是有效
        if (stack == null || stack.isEmpty()) {
            return true;
        }

        // 檢查是否是飾品
        if (!stack.hasCapability(BaublesCapabilities.CAPABILITY_ITEM_BAUBLE, null)) {
            return false;
        }

        IBauble bauble = stack.getCapability(BaublesCapabilities.CAPABILITY_ITEM_BAUBLE, null);
        if (bauble == null) {
            return false;
        }

        // 檢查是否可以裝備
        if (!bauble.canEquip(stack, player)) {
            return false;
        }

        // 獲取飾品類型
        BaubleType type = bauble.getBaubleType(stack);

        // TRINKET 可以放在任何槽位
        if (type == BaubleType.TRINKET) {
            return true;
        }

        // 其他類型檢查槽位對應
        switch(slot) {
            case 14: return type == BaubleType.AMULET;
            case 15:
            case 16: return type == BaubleType.RING;
            case 17: return type == BaubleType.BELT;
            case 18: return type == BaubleType.HEAD;
            case 19: return type == BaubleType.BODY;
            case 20: return type == BaubleType.CHARM;
            case 21: return type == BaubleType.CHARM;

            default: return false;
        }
    }
}