package com.moremod.synergy.condition;

import com.moremod.synergy.api.ISynergyCondition;
import com.moremod.synergy.core.SynergyContext;
import com.moremod.synergy.core.SynergyPlayerState;

/**
 * 连击条件
 *
 * 检查玩家是否在指定时间内命中了指定次数。
 */
public class ComboCondition implements ISynergyCondition {

    private final int requiredHits;

    public ComboCondition(int requiredHits) {
        this.requiredHits = requiredHits;
    }

    @Override
    public boolean test(SynergyContext context) {
        SynergyPlayerState state = SynergyPlayerState.get(context.getPlayer());
        return state.getComboCount() >= requiredHits;
    }

    @Override
    public String getDescription() {
        return "Combo >= " + requiredHits;
    }

    public static ComboCondition atLeast(int hits) {
        return new ComboCondition(hits);
    }
}
