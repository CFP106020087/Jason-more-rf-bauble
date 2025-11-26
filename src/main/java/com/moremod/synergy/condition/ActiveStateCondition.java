package com.moremod.synergy.condition;

import com.moremod.synergy.api.ISynergyCondition;
import com.moremod.synergy.core.SynergyContext;
import com.moremod.synergy.core.SynergyPlayerState;

/**
 * 激活状态条件
 *
 * 检查玩家是否有指定的激活状态。
 */
public class ActiveStateCondition implements ISynergyCondition {

    private final String stateId;
    private final boolean requireActive;

    public ActiveStateCondition(String stateId, boolean requireActive) {
        this.stateId = stateId;
        this.requireActive = requireActive;
    }

    @Override
    public boolean test(SynergyContext context) {
        SynergyPlayerState state = SynergyPlayerState.get(context.getPlayer());
        boolean isActive = state.hasActiveState(stateId);
        return requireActive == isActive;
    }

    @Override
    public String getDescription() {
        return (requireActive ? "Has state: " : "No state: ") + stateId;
    }

    public static ActiveStateCondition hasState(String stateId) {
        return new ActiveStateCondition(stateId, true);
    }

    public static ActiveStateCondition noState(String stateId) {
        return new ActiveStateCondition(stateId, false);
    }
}
