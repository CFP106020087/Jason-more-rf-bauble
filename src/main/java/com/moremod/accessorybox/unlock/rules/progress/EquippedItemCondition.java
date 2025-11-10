package com.moremod.accessorybox.unlock.rules.progress;

import com.moremod.accessorybox.unlock.rules.UnlockCondition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * 装备物品条件
 */
public class EquippedItemCondition implements UnlockCondition {
    private final String itemId;
    private final String slot;

    public EquippedItemCondition(String itemId, String slot) {
        this.itemId = itemId;
        this.slot = slot;
    }

    @Override
    public boolean check(EntityPlayer player) {
        String[] parts = itemId.split(":");
        String modId = parts[0];
        String itemName = parts[1];
        int meta = parts.length > 2 ? Integer.parseInt(parts[2]) : -1;

        Item item = Item.REGISTRY.getObject(new ResourceLocation(modId, itemName));
        if (item == null) return false;

        if (slot != null) {
            // 检查指定槽位
            EntityEquipmentSlot equipSlot = parseSlot(slot);
            if (equipSlot != null) {
                ItemStack stack = player.getItemStackFromSlot(equipSlot);
                return !stack.isEmpty() && stack.getItem() == item &&
                        (meta < 0 || stack.getMetadata() == meta);
            }
        } else {
            // 检查所有装备槽位
            for (EntityEquipmentSlot equipSlot : EntityEquipmentSlot.values()) {
                ItemStack stack = player.getItemStackFromSlot(equipSlot);
                if (!stack.isEmpty() && stack.getItem() == item &&
                        (meta < 0 || stack.getMetadata() == meta)) {
                    return true;
                }
            }
        }

        return false;
    }

    private EntityEquipmentSlot parseSlot(String slotName) {
        switch (slotName.toLowerCase()) {
            case "head": case "helmet": return EntityEquipmentSlot.HEAD;
            case "chest": case "chestplate": return EntityEquipmentSlot.CHEST;
            case "legs": case "leggings": return EntityEquipmentSlot.LEGS;
            case "feet": case "boots": return EntityEquipmentSlot.FEET;
            case "mainhand": case "hand": return EntityEquipmentSlot.MAINHAND;
            case "offhand": return EntityEquipmentSlot.OFFHAND;
            default: return null;
        }
    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public String getType() {
        return "equipped_item";
    }

    @Override
    public String getDescription() {
        return "装备 " + itemId + (slot != null ? " (" + slot + ")" : "");
    }
}
