package com.moremod.synergy.builtin;

import com.moremod.synergy.condition.ModuleCombinationCondition;
import com.moremod.synergy.core.ModuleChain;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.effect.DebuffEffect;
import com.moremod.synergy.effect.IncomingDamageModifierEffect;
import com.moremod.synergy.effect.ShieldGrantEffect;

/**
 * 堡垒 Synergy - Fortress
 *
 * 描述：
 * - 当玩家同时拥有"护盾发生器"和"护甲强化"时激活
 * - 提供极高的防御能力，几乎无敌
 * - 但沉重的防御装备会大幅降低移动速度
 *
 * 所需模块：
 * - YELLOW_SHIELD（护盾发生器）
 * - ARMOR_BOOST（护甲强化）
 *
 * 正面效果：
 * - 持续授予 2.0 点护盾（叠加，最多 20.0 点）
 * - 受到的伤害 -40%（极高减伤）
 *
 * 负面效果（Drawback）：
 * - 缓慢 II 效果（沉重的防御装备）
 */
public class FortressSynergy {

    public static final String ID = "FORTRESS";

    public static SynergyDefinition create() {
        return new SynergyDefinition.Builder(ID)
                .displayName("堡垒")
                .description("护盾发生器 + 护甲强化 → 护盾 + 减伤 40% | Drawback: 缓慢 II")

                // 所需模块链
                .chain(ModuleChain.linear(
                        "YELLOW_SHIELD",
                        "ARMOR_BOOST"
                ))

                // 条件：拥有两个模块且都激活
                .condition(new ModuleCombinationCondition(
                        true, // 要求激活
                        "YELLOW_SHIELD",
                        "ARMOR_BOOST"
                ))

                // 正面效果1：持续授予护盾
                .effect(new ShieldGrantEffect(
                        2.0f,   // 每次授予 2.0 点
                        false,  // 叠加模式
                        20.0f,  // 最多 20.0 点
                        false   // 不显示消息
                ))

                // 正面效果2：减伤 40%
                .effect(new IncomingDamageModifierEffect(
                        0.6f,   // 受伤倍率 0.6x（减少 40%）
                        0f,     // 无固定加成
                        false   // 不显示消息
                ))

                // 负面效果：缓慢 II（沉重装备）
                .effect(DebuffEffect.slowness(
                        1,      // 缓慢 II
                        10      // 持续 10 秒
                ))

                .priority(70)
                .enabled(true)
                .build();
    }
}
