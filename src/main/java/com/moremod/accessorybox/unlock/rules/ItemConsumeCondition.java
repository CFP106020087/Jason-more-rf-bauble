package com.moremod.accessorybox.unlock.rules;

import com.moremod.accessorybox.unlock.rules.progress.ProgressTracker;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 物品消耗条件
 * 检查玩家是否已消耗指定数量的物品
 */
public class ItemConsumeCondition implements UnlockCondition {
    private final String itemId;
    private final int requiredCount;

    public ItemConsumeCondition(String itemId, int requiredCount) {
        this.itemId = itemId;
        this.requiredCount = requiredCount;
    }

    @Override
    public boolean check(EntityPlayer player) {
        int consumed = ProgressTracker.getItemConsumeCount(player, itemId);
        return consumed >= requiredCount;
    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public String getType() {
        return "item_consume";
    }

    @Override
    public String getDescription() {
        return "消耗 " + requiredCount + "x " + itemId;
    }

    public String getItemId() {
        return itemId;
    }

    public int getRequiredCount() {
        return requiredCount;
    }
}
