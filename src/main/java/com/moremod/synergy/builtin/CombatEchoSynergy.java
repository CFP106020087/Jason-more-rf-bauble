package com.moremod.synergy.builtin;

import com.moremod.synergy.condition.EventTypeCondition;
import com.moremod.synergy.condition.ModuleCombinationCondition;
import com.moremod.synergy.core.ModuleChain;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.effect.DamageModifierEffect;
import com.moremod.synergy.effect.EnergyDrainEffect;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * 战斗回响 Synergy - Combat Echo
 *
 * 描述：
 * - 当玩家同时拥有"伤害提升模块"和"攻击速度模块"时激活
 * - 攻击敌人时，额外造成基础伤害的 25% 回响伤害
 * - 攻击速度越快，回响触发越频繁
 *
 * 所需模块：
 * - DAMAGE_BOOST（伤害提升）
 * - ATTACK_SPEED（攻击速度）
 *
 * 正面效果：
 * - 伤害 +25%
 *
 * 负面效果（Drawback）：
 * - 每秒消耗 15 RF 维持战斗回响状态
 */
public class CombatEchoSynergy {

    public static final String ID = "COMBAT_ECHO";

    public static SynergyDefinition create() {
        return new SynergyDefinition.Builder(ID)
                .displayName("战斗回响")
                .description("伤害提升 + 攻击速度 → 伤害 +25% | Drawback: -15 RF/s")

                // 所需模块链（强调顺序：先提升伤害，再提升速度）
                .chain(ModuleChain.linear(
                        "DAMAGE_BOOST",
                        "ATTACK_SPEED"
                ))

                // 条件1：拥有两个模块且都激活
                .condition(new ModuleCombinationCondition(
                        true, // 要求激活
                        "DAMAGE_BOOST",
                        "ATTACK_SPEED"
                ))

                // 条件2：只在 LivingHurtEvent 中触发伤害加成
                .condition(new EventTypeCondition(LivingHurtEvent.class))

                // 正面效果：伤害 +25%
                .effect(new DamageModifierEffect(
                        1.25f,  // 伤害倍率 1.25x
                        0f,     // 无固定加成
                        true    // 显示消息
                ))

                // 负面效果（Drawback）：每秒消耗 15 RF
                .effect(new EnergyDrainEffect(
                        15,      // 每秒 15 RF
                        20,      // 每 20 tick（1秒）触发
                        false,   // 不显示消息
                        true     // 能量耗尽时停止
                ))

                .priority(150) // 较高优先级，在其他伤害加成之后
                .enabled(true)
                .build();
    }
}
