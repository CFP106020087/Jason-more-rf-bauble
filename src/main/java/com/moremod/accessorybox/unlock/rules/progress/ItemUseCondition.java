package com.moremod.accessorybox.unlock.rules.progress;

import com.moremod.accessorybox.unlock.rules.UnlockCondition;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 使用物品条件
 */
public class ItemUseCondition implements UnlockCondition {
    private final String itemId;

    public ItemUseCondition(String itemId) {
        this.itemId = itemId;
    }

    @Override
    public boolean check(EntityPlayer player) {
        return ProgressTracker.getItemUseCount(player, itemId) > 0;
    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public String getType() {
        return "item_use";
    }

    @Override
    public String getDescription() {
        return "使用 " + itemId;
    }
}
