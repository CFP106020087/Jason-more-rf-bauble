package com.moremod.synergy.condition;

import com.moremod.synergy.api.ISynergyCondition;
import com.moremod.synergy.core.SynergyContext;
import com.moremod.synergy.core.SynergyPlayerState;

/**
 * 冷却条件
 *
 * 检查指定的 Synergy 是否不在冷却中。
 */
public class CooldownCondition implements ISynergyCondition {

    private final String synergyId;

    public CooldownCondition(String synergyId) {
        this.synergyId = synergyId;
    }

    @Override
    public boolean test(SynergyContext context) {
        SynergyPlayerState state = SynergyPlayerState.get(context.getPlayer());
        return !state.isOnCooldown(synergyId);
    }

    @Override
    public String getDescription() {
        return "Not on cooldown: " + synergyId;
    }

    public static CooldownCondition notOnCooldown(String synergyId) {
        return new CooldownCondition(synergyId);
    }
}
