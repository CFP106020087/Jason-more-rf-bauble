package com.moremod.synergy.effect;

import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.synergy.core.SynergyContext;
import com.moremod.synergy.core.SynergyPlayerState;

/**
 * 状态激活效果
 *
 * 激活一个临时状态，可以设置过期回调。
 */
public class StateActivationEffect implements ISynergyEffect {

    private final String stateId;
    private final int durationTicks;
    private final Runnable onExpireCallback;

    public StateActivationEffect(String stateId, int durationTicks) {
        this(stateId, durationTicks, null);
    }

    public StateActivationEffect(String stateId, int durationTicks, Runnable onExpireCallback) {
        this.stateId = stateId;
        this.durationTicks = durationTicks;
        this.onExpireCallback = onExpireCallback;
    }

    @Override
    public void apply(SynergyContext context) {
        SynergyPlayerState state = SynergyPlayerState.get(context.getPlayer());
        state.activateState(stateId, durationTicks, onExpireCallback);
    }

    @Override
    public String getDescription() {
        return "Activate state: " + stateId + " for " + (durationTicks / 20f) + "s";
    }

    public static StateActivationEffect activate(String stateId, int durationTicks) {
        return new StateActivationEffect(stateId, durationTicks);
    }

    public static StateActivationEffect activateSeconds(String stateId, float seconds) {
        return new StateActivationEffect(stateId, (int)(seconds * 20));
    }
}
