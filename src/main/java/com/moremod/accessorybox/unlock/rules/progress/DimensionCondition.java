package com.moremod.accessorybox.unlock.rules.progress;

import com.moremod.accessorybox.unlock.rules.UnlockCondition;
import net.minecraft.entity.player.EntityPlayer;
/**
 * 维度条件
 */
public class DimensionCondition implements UnlockCondition {
    private final int dimensionId;

    public DimensionCondition(int dimensionId) {
        this.dimensionId = dimensionId;
    }

    @Override
    public boolean check(EntityPlayer player) {
        return player.dimension == dimensionId;
    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public String getType() {
        return "dimension";
    }

    @Override
    public String getDescription() {
        return "维度 " + dimensionId;
    }
}
