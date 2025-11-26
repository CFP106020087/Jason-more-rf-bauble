package com.moremod.synergy.effect;

import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.synergy.core.SynergyContext;
import com.moremod.synergy.core.SynergyPlayerState;

/**
 * 排异值效果
 *
 * 增加或减少玩家的排异值。
 */
public class RejectionEffect implements ISynergyEffect {

    private final float amount;

    public RejectionEffect(float amount) {
        this.amount = amount;
    }

    @Override
    public void apply(SynergyContext context) {
        SynergyPlayerState state = SynergyPlayerState.get(context.getPlayer());
        state.addRejection(amount);

        // 如果排异值过高，可能触发负面效果
        if (state.isRejectionCritical()) {
            // 这里可以触发排异惩罚
            System.out.println("[Synergy] Warning: " + context.getPlayer().getName() +
                    " rejection is critical: " + state.getRejection() + "%");
        }
    }

    @Override
    public String getDescription() {
        return (amount >= 0 ? "Add " : "Remove ") + Math.abs(amount) + "% rejection";
    }

    public static RejectionEffect add(float percent) {
        return new RejectionEffect(percent);
    }

    public static RejectionEffect remove(float percent) {
        return new RejectionEffect(-percent);
    }
}
