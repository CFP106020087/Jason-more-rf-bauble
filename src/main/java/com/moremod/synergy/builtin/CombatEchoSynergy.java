package com.moremod.synergy.builtin;

import com.moremod.synergy.condition.EventTypeCondition;
import com.moremod.synergy.condition.ModuleCombinationCondition;
import com.moremod.synergy.core.ModuleChain;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.effect.DamageModifierEffect;
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
 * 效果：
 * - 伤害 +25%
 */
public class CombatEchoSynergy {

    public static final String ID = "COMBAT_ECHO";

    public static SynergyDefinition create() {
        return new SynergyDefinition.Builder(ID)
                .displayName("战斗回响")
                .description("伤害提升 + 攻击速度 → 额外造成 25% 回响伤害")

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

                // 条件2：只在 LivingHurtEvent 中触发
                .condition(new EventTypeCondition(LivingHurtEvent.class))

                // 效果：伤害 +25%
                .effect(new DamageModifierEffect(
                        1.25f,  // 伤害倍率 1.25x
                        0f,     // 无固定加成
                        true    // 显示消息
                ))

                .priority(150) // 较高优先级，在其他伤害加成之后
                .enabled(true)
                .build();
    }
}
