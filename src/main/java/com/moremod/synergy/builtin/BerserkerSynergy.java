package com.moremod.synergy.builtin;

import com.moremod.synergy.condition.ModuleCombinationCondition;
import com.moremod.synergy.condition.PlayerStateCondition;
import com.moremod.synergy.core.ModuleChain;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.effect.DamageModifierEffect;
import com.moremod.synergy.effect.HealthDrainEffect;

/**
 * 狂战士 Synergy - Berserker
 *
 * 描述：
 * - 当玩家同时拥有"伤害提升"和"生命恢复"时激活
 * - 生命值越低，伤害越高（狂战士姿态）
 * - 持续流失生命值以保持低血量状态
 *
 * 所需模块：
 * - DAMAGE_BOOST（伤害提升）
 * - HEALTH_REGEN（生命恢复）
 *
 * 正面效果：
 * - 生命值 < 40% 时，伤害 +60%（狂暴状态）
 *
 * 负面效果（Drawback）：
 * - 持续流失 0.5 HP/s（维持低血量）
 */
public class BerserkerSynergy {

    public static final String ID = "BERSERKER";

    public static SynergyDefinition create() {
        return new SynergyDefinition.Builder(ID)
                .displayName("狂战士")
                .description("伤害提升 + 生命恢复 → 低血量时伤害 +60% | Drawback: -0.5 HP/s")

                // 所需模块链
                .chain(ModuleChain.linear(
                        "DAMAGE_BOOST",
                        "HEALTH_REGEN"
                ))

                // 条件1：拥有两个模块且都激活
                .condition(new ModuleCombinationCondition(
                        true, // 要求激活
                        "DAMAGE_BOOST",
                        "HEALTH_REGEN"
                ))

                // 条件2：生命值低于 40%（狂战士状态）
                .condition(PlayerStateCondition.healthBelow(0.4f))

                // 正面效果：伤害 +60%
                .effect(new DamageModifierEffect(
                        1.6f,   // 伤害倍率 1.6x
                        0f,     // 无固定加成
                        false   // 不显示消息
                ))

                // 负面效果（Drawback）：持续流失生命值
                .effect(new HealthDrainEffect(
                        0.5f,   // 每秒 0.5 HP
                        20,     // 每秒触发
                        false,  // 不显示消息
                        true    // 不会致死（最少保留 1.0 HP）
                ))

                .priority(120) // 中高优先级
                .enabled(true)
                .build();
    }
}
