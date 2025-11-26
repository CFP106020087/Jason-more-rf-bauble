package com.moremod.synergy.effect;

import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.synergy.core.SynergyContext;
import com.moremod.synergy.core.SynergyPlayerState;

/**
 * 冷却效果
 *
 * 设置指定 Synergy 的冷却时间。
 */
public class CooldownEffect implements ISynergyEffect {

    private final String synergyId;
    private final long cooldownMs;

    public CooldownEffect(String synergyId, long cooldownMs) {
        this.synergyId = synergyId;
        this.cooldownMs = cooldownMs;
    }

    @Override
    public void apply(SynergyContext context) {
        SynergyPlayerState state = SynergyPlayerState.get(context.getPlayer());
        state.setCooldown(synergyId, cooldownMs);
    }

    @Override
    public String getDescription() {
        return "Set cooldown: " + synergyId + " " + (cooldownMs / 1000f) + "s";
    }

    public static CooldownEffect set(String synergyId, long cooldownMs) {
        return new CooldownEffect(synergyId, cooldownMs);
    }

    public static CooldownEffect setSeconds(String synergyId, float seconds) {
        return new CooldownEffect(synergyId, (long)(seconds * 1000));
    }
}
