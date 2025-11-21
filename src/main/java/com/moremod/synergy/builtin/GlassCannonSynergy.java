package com.moremod.synergy.builtin;

import com.moremod.synergy.condition.ModuleCombinationCondition;
import com.moremod.synergy.core.ModuleChain;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.effect.DamageModifierEffect;
import com.moremod.synergy.effect.IncomingDamageModifierEffect;

/**
 * 玻璃大炮 Synergy - Glass Cannon
 *
 * 描述：
 * - 当玩家同时拥有"暴击强化"和"伤害提升"时激活
 * - 提供极高的输出伤害，但代价是变得非常脆弱
 * - 经典的"高风险高回报"玩法
 *
 * 所需模块：
 * - CRITICAL_STRIKE（暴击强化）
 * - DAMAGE_BOOST（伤害提升）
 *
 * 正面效果：
 * - 伤害 +50%（极高输出）
 *
 * 负面效果（Drawback）：
 * - 受到的伤害 +30%（玻璃大炮，非常脆弱）
 */
public class GlassCannonSynergy {

    public static final String ID = "GLASS_CANNON";

    public static SynergyDefinition create() {
        return new SynergyDefinition.Builder(ID)
                .displayName("玻璃大炮")
                .description("暴击强化 + 伤害提升 → 伤害 +50% | Drawback: 受伤 +30%")

                // 所需模块链（暴击 + 伤害提升）
                .chain(ModuleChain.linear(
                        "CRITICAL_STRIKE",
                        "DAMAGE_BOOST"
                ))

                // 条件：拥有两个模块且都激活
                .condition(new ModuleCombinationCondition(
                        true, // 要求激活
                        "CRITICAL_STRIKE",
                        "DAMAGE_BOOST"
                ))

                // 正面效果：伤害 +50%
                .effect(new DamageModifierEffect(
                        1.5f,   // 伤害倍率 1.5x
                        0f,     // 无固定加成
                        false   // 不显示消息（太频繁）
                ))

                // 负面效果（Drawback）：受到的伤害 +30%
                .effect(new IncomingDamageModifierEffect(
                        1.3f,   // 受伤倍率 1.3x
                        0f,     // 无固定加成
                        true    // 显示警告消息
                ))

                .priority(100) // 中等优先级
                .enabled(true)
                .build();
    }
}
