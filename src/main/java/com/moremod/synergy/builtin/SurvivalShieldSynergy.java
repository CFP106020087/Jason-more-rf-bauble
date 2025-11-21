package com.moremod.synergy.builtin;

import com.moremod.synergy.condition.ModuleCombinationCondition;
import com.moremod.synergy.condition.PlayerStateCondition;
import com.moremod.synergy.core.ModuleChain;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.effect.ShieldGrantEffect;

/**
 * 生存护盾 Synergy - Survival Shield
 *
 * 描述：
 * - 当玩家同时拥有"护盾发生器"和"生命恢复模块"时激活
 * - 在生命值低于 50% 时，自动授予临时护盾
 * - 提供额外的生存能力
 *
 * 所需模块：
 * - YELLOW_SHIELD（护盾发生器）
 * - HEALTH_REGEN（生命恢复）
 *
 * 效果：
 * - 生命值 < 50% 时，每秒授予 1.0 点护盾（最多 10.0 点）
 */
public class SurvivalShieldSynergy {

    public static final String ID = "SURVIVAL_SHIELD";

    public static SynergyDefinition create() {
        return new SynergyDefinition.Builder(ID)
                .displayName("生存护盾")
                .description("护盾发生器 + 生命恢复 → 低血量时自动授予护盾")

                // 所需模块链
                .chain(ModuleChain.linear(
                        "YELLOW_SHIELD",
                        "HEALTH_REGEN"
                ))

                // 条件1：拥有两个模块且都激活
                .condition(new ModuleCombinationCondition(
                        true, // 要求激活
                        "YELLOW_SHIELD",
                        "HEALTH_REGEN"
                ))

                // 条件2：生命值低于 50%
                .condition(PlayerStateCondition.healthBelow(0.5f))

                // 效果：授予 1.0 点护盾（叠加，最多 10.0）
                .effect(new ShieldGrantEffect(
                        1.0f,   // 每次授予 1.0 点
                        false,  // 叠加模式
                        10.0f,  // 最多 10.0 点
                        true    // 显示消息
                ))

                .priority(50) // 高优先级，尽早授予护盾
                .enabled(true)
                .build();
    }
}
