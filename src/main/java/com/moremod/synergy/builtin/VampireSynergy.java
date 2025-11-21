package com.moremod.synergy.builtin;

import com.moremod.synergy.condition.EventTypeCondition;
import com.moremod.synergy.condition.ModuleCombinationCondition;
import com.moremod.synergy.core.ModuleChain;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.effect.DebuffEffect;
import com.moremod.synergy.effect.LifestealEffect;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * 吸血鬼 Synergy - Vampire
 *
 * 描述：
 * - 当玩家同时拥有"伤害提升"和"生命恢复"时激活
 * - 攻击敌人时吸取生命值
 * - 但吸血的诅咒会削弱玩家的力量和饱食度
 *
 * 所需模块：
 * - DAMAGE_BOOST（伤害提升）
 * - HEALTH_REGEN（生命恢复）
 *
 * 正面效果：
 * - 吸血 25%（恢复伤害的 25%，最多 5 HP）
 *
 * 负面效果（Drawback）：
 * - 虚弱 I 效果（吸血诅咒削弱力量）
 * - 饥饿 I 效果（需要持续进食）
 */
public class VampireSynergy {

    public static final String ID = "VAMPIRE";

    public static SynergyDefinition create() {
        return new SynergyDefinition.Builder(ID)
                .displayName("吸血鬼")
                .description("伤害提升 + 生命恢复 → 吸血 25% | Drawback: 虚弱 I + 饥饿 I")

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

                // 条件2：只在攻击时触发
                .condition(new EventTypeCondition(LivingHurtEvent.class))

                // 正面效果：吸血 25%
                .effect(new LifestealEffect(
                        0.25f,  // 恢复伤害的 25%
                        5.0f,   // 单次最多 5 HP
                        true    // 显示消息
                ))

                // 负面效果1：虚弱 I（吸血诅咒）
                .effect(DebuffEffect.weakness(
                        0,      // 虚弱 I
                        10      // 持续 10 秒
                ))

                // 负面效果2：饥饿 I（需要进食）
                .effect(DebuffEffect.hunger(
                        0,      // 饥饿 I
                        10      // 持续 10 秒
                ))

                .priority(110)
                .enabled(true)
                .build();
    }
}
