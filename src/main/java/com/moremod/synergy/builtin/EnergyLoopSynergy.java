package com.moremod.synergy.builtin;

import com.moremod.synergy.condition.ModuleCombinationCondition;
import com.moremod.synergy.core.ModuleChain;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.effect.EnergyDrainEffect;
import com.moremod.synergy.effect.EnergyRefundEffect;

/**
 * 能量循环 Synergy - Energy Loop
 *
 * 描述：
 * - 当玩家同时拥有"能量效率模块"和"动能发电模块"时激活
 * - 消耗能量时，有一定概率退还部分能量
 * - 形成能量循环，提高续航能力
 *
 * 所需模块：
 * - ENERGY_EFFICIENCY（能量效率）
 * - KINETIC_GENERATOR（动能发电）
 *
 * 正面效果：
 * - 20% 概率退还 50 RF 能量
 *
 * 负面效果（Drawback）：
 * - 每秒消耗 10 RF 维持能量循环回路
 */
public class EnergyLoopSynergy {

    public static final String ID = "ENERGY_LOOP";

    public static SynergyDefinition create() {
        return new SynergyDefinition.Builder(ID)
                .displayName("能量循环")
                .description("能量效率 + 动能发电 → 20% 概率退还能量 | Drawback: -10 RF/s")

                // 所需模块组合
                .chain(ModuleChain.linear(
                        "ENERGY_EFFICIENCY",
                        "KINETIC_GENERATOR"
                ))

                // 条件：拥有两个模块且都激活
                .condition(new ModuleCombinationCondition(
                        true, // 要求激活
                        "ENERGY_EFFICIENCY",
                        "KINETIC_GENERATOR"
                ))

                // 正面效果：20% 概率退还 50 RF
                .effect(new EnergyRefundEffect(
                        50,      // 固定退还 50 RF
                        0.2f,    // 20% 概率
                        true     // 显示消息
                ))

                // 负面效果（Drawback）：每秒消耗 10 RF
                .effect(new EnergyDrainEffect(
                        10,      // 每秒 10 RF
                        20,      // 每 20 tick（1秒）触发
                        false,   // 不显示消息
                        true     // 能量耗尽时停止
                ))

                .priority(100)
                .enabled(true)
                .build();
    }
}
