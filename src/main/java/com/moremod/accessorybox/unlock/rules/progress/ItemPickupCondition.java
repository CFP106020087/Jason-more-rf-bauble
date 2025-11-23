package com.moremod.accessorybox.unlock.rules.progress;

import com.moremod.accessorybox.unlock.rules.UnlockCondition;
import com.moremod.accessorybox.unlock.rules.UnlockRulesConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * 拾取物品条件
 */
public class ItemPickupCondition implements UnlockCondition {
    private final String itemId;
    private final int requiredCount;

    public ItemPickupCondition(String itemId, int requiredCount) {
        this.itemId = itemId;
        this.requiredCount = requiredCount;
    }

    @Override
    public boolean check(EntityPlayer player) {
        int count = ProgressTracker.getItemPickupCount(player, itemId);
        
        // 如果配置允许，加上背包中已有的数量
        if (UnlockRulesConfig.countExistingItems) {
            count += countInInventory(player, itemId);
        }
        
        return count >= requiredCount;
    }

    private int countInInventory(EntityPlayer player, String itemId) {
        String[] parts = itemId.split(":");
        String modId = parts[0];
        String itemName = parts[1];
        int meta = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        
        Item item = Item.REGISTRY.getObject(new ResourceLocation(modId, itemName));
        if (item == null) return 0;
        
        int count = 0;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                if (parts.length <= 2 || stack.getMetadata() == meta) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public String getType() {
        return "item_pickup";
    }

    @Override
    public String getDescription() {
        return "拾取 " + requiredCount + "x " + itemId;
    }
}
