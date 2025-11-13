package com.moremod.accessorybox.unlock.rules.progress;

import com.moremod.accessorybox.unlock.rules.UnlockCondition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * 背包物品条件
 */
public class InventoryItemCondition implements UnlockCondition {
    private final String itemId;
    private final int requiredCount;

    public InventoryItemCondition(String itemId, int requiredCount) {
        this.itemId = itemId;
        this.requiredCount = requiredCount;
    }

    @Override
    public boolean check(EntityPlayer player) {
        String[] parts = itemId.split(":");
        String modId = parts[0];
        String itemName = parts[1];
        int meta = parts.length > 2 ? Integer.parseInt(parts[2]) : -1;

        Item item = Item.REGISTRY.getObject(new ResourceLocation(modId, itemName));
        if (item == null) return false;

        int count = 0;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                if (meta < 0 || stack.getMetadata() == meta) {
                    count += stack.getCount();
                }
            }
        }

        return count >= requiredCount;
    }

    @Override
    public boolean isTemporary() {
        return true;  // ⭐ 背包条件是临时的：物品数量不足时槽位重新锁定
    }

    @Override
    public String getType() {
        return "inventory_item";
    }

    @Override
    public String getDescription() {
        return "持有 " + requiredCount + "x " + itemId;
    }
}
