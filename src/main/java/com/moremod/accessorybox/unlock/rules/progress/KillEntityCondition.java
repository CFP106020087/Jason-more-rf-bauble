package com.moremod.accessorybox.unlock.rules.progress;

import com.moremod.accessorybox.unlock.rules.UnlockCondition;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 击杀实体条件
 */
public class KillEntityCondition implements UnlockCondition {
    private final String entityId;
    private final int requiredCount;

    public KillEntityCondition(String entityId, int requiredCount) {
        this.entityId = entityId;
        this.requiredCount = requiredCount;
    }

    @Override
    public boolean check(EntityPlayer player) {
        return ProgressTracker.getEntityKillCount(player, entityId) >= requiredCount;
    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public String getType() {
        return "kill_entity";
    }

    @Override
    public String getDescription() {
        return "击杀 " + requiredCount + "x " + entityId;
    }
}
