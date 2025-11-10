package com.moremod.accessorybox.unlock.rules.progress;

import com.moremod.accessorybox.unlock.rules.UnlockCondition;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 等级条件
 */
public class LevelCondition implements UnlockCondition {
    private final int requiredLevel;

    public LevelCondition(int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    @Override
    public boolean check(EntityPlayer player) {
        return player.experienceLevel >= requiredLevel;
    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public String getType() {
        return "level";
    }

    @Override
    public String getDescription() {
        return "等级 " + requiredLevel;
    }
}
