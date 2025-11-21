package com.moremod.synergy.builtin;

import com.moremod.synergy.condition.ModuleCombinationCondition;
import com.moremod.synergy.core.ModuleChain;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.effect.DebuffEffect;
import com.moremod.synergy.effect.EnergyRefundEffect;

/**
 * 奥术过载 Synergy - Arcane Overload
 *
 * 描述：
 * - 当玩家同时拥有"魔力吸收"和"能量效率"时激活
 * - 大幅提升能量获取效率
 * - 但过载的奥术能量会削弱玩家的物理能力
 *
 * 所需模块：
 * - MAGIC_ABSORB（魔力吸收）
 * - ENERGY_EFFICIENCY（能量效率）
 *
 * 正面效果：
 * - 50% 几率返还 100 RF（双倍能量效率）
 *
 * 负面效果（Drawback）：
 * - 虚弱 II 效果（奥术能量削弱物理力量）
 * - 挖掘疲劳 I 效果
 */
public class ArcaneOverloadSynergy {

    public static final String ID = "ARCANE_OVERLOAD";

    public static SynergyDefinition create() {
        return new SynergyDefinition.Builder(ID)
                .displayName("奥术过载")
                .description("魔力吸收 + 能量效率 → 50% 返还 100 RF | Drawback: 虚弱 II + 挖掘疲劳 I")

                // 所需模块链
                .chain(ModuleChain.linear(
                        "MAGIC_ABSORB",
                        "ENERGY_EFFICIENCY"
                ))

                // 条件：拥有两个模块且都激活
                .condition(new ModuleCombinationCondition(
                        true, // 要求激活
                        "MAGIC_ABSORB",
                        "ENERGY_EFFICIENCY"
                ))

                // 正面效果：高几率返还大量能量
                .effect(new EnergyRefundEffect(
                        100,    // 返还 100 RF
                        0.5f,   // 50% 几率
                        false   // 不显示消息（太频繁）
                ))

                // 负面效果1：虚弱 II（攻击力大幅降低）
                .effect(DebuffEffect.weakness(
                        1,      // 虚弱 II
                        12      // 持续 12 秒
                ))

                // 负面效果2：挖掘疲劳 I
                .effect(DebuffEffect.miningFatigue(
                        0,      // 挖掘疲劳 I
                        12      // 持续 12 秒
                ))

                .priority(80)
                .enabled(true)
                .build();
    }
}
