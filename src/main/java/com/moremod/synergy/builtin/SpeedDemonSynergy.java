package com.moremod.synergy.builtin;

import com.moremod.synergy.condition.ModuleCombinationCondition;
import com.moremod.synergy.core.ModuleChain;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.effect.DebuffEffect;
import com.moremod.synergy.effect.EnergyDrainEffect;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;

/**
 * 速度恶魔 Synergy - Speed Demon
 *
 * 描述：
 * - 当玩家同时拥有"攻击速度"和"动能发生器"时激活
 * - 极致的速度加成（移动 + 攻击）
 * - 但消耗大量能量维持超高速状态
 *
 * 所需模块：
 * - ATTACK_SPEED（攻击速度）
 * - KINETIC_GENERATOR（动能发生器）
 *
 * 正面效果：
 * - 速度 II 效果（极速移动）
 * - 急迫 I 效果（攻击/挖掘速度提升）
 *
 * 负面效果（Drawback）：
 * - 每秒消耗 30 RF（高速移动耗能）
 * - 饥饿 II 效果（高速代谢）
 */
public class SpeedDemonSynergy {

    public static final String ID = "SPEED_DEMON";

    public static SynergyDefinition create() {
        return new SynergyDefinition.Builder(ID)
                .displayName("速度恶魔")
                .description("攻击速度 + 动能发生器 → 速度 II + 急迫 I | Drawback: -30 RF/s + 饥饿 II")

                // 所需模块链
                .chain(ModuleChain.linear(
                        "ATTACK_SPEED",
                        "KINETIC_GENERATOR"
                ))

                // 条件：拥有两个模块且都激活
                .condition(new ModuleCombinationCondition(
                        true, // 要求激活
                        "ATTACK_SPEED",
                        "KINETIC_GENERATOR"
                ))

                // 正面效果1：速度 II
                .effect(new com.moremod.synergy.effect.PotionBuffEffect(
                        MobEffects.SPEED,
                        1,      // 速度 II
                        200     // 持续 10 秒
                ))

                // 正面效果2：急迫 I（攻击/挖掘速度）
                .effect(new com.moremod.synergy.effect.PotionBuffEffect(
                        MobEffects.HASTE,
                        0,      // 急迫 I
                        200     // 持续 10 秒
                ))

                // 负面效果1：大量能量消耗
                .effect(new EnergyDrainEffect(
                        30,     // 每秒 30 RF
                        20,     // 每秒触发
                        false,  // 不显示消息
                        true    // 能量耗尽时停止
                ))

                // 负面效果2：饥饿 II（高速代谢）
                .effect(DebuffEffect.hunger(
                        1,      // 饥饿 II
                        10      // 持续 10 秒
                ))

                .priority(90)
                .enabled(true)
                .build();
    }
}
