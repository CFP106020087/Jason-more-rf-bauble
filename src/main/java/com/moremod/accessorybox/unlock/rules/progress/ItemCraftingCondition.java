package com.moremod.accessorybox.unlock.rules.progress;

import com.moremod.accessorybox.unlock.rules.UnlockCondition;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 合成物品条件
 */
public class ItemCraftingCondition implements UnlockCondition {
    private final String itemId;
    private final int requiredCount;

    public ItemCraftingCondition(String itemId, int requiredCount) {
        this.itemId = itemId;
        this.requiredCount = requiredCount;
    }

    @Override
    public boolean check(EntityPlayer player) {
        return ProgressTracker.getItemCraftCount(player, itemId) >= requiredCount;
    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public String getType() {
        return "item_crafting";
    }

    @Override
    public String getDescription() {
        return "合成 " + requiredCount + "x " + itemId;
    }
}
