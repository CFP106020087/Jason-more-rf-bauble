package com.moremod.synergy.examples;

import com.moremod.synergy.api.ISynergyCondition;
import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.synergy.bridge.ExistingModuleBridge;
import com.moremod.synergy.condition.*;
import com.moremod.synergy.core.SynergyContext;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.core.SynergyEventType;
import com.moremod.synergy.core.SynergyManager;
import com.moremod.synergy.effect.*;
import net.minecraft.util.text.TextFormatting;

/**
 * 示例 Synergy 定义
 *
 * 这个类展示如何创建和注册 Synergy 规则。
 * 包含 3 个完整的示例：
 *
 * 1. Energy Loop Synergy (能量循环)
 *    - 需要模块：KINETIC_GENERATOR + SOLAR_GENERATOR (能量生成模块组合)
 *    - 效果：每秒有概率返还能量
 *
 * 2. Combat Echo Synergy (战斗回响)
 *    - 需要模块：CRITICAL_STRIKE + DAMAGE_BOOST (暴击 + 伤害增幅)
 *    - 效果：暴击时造成额外真实伤害
 *
 * 3. Survival Shield Synergy (生存护盾)
 *    - 需要模块：YELLOW_SHIELD + FIRE_EXTINGUISH (护盾 + 自动灭火)
 *    - 效果：受到环境伤害时获得额外护盾
 */
public class ExampleSynergies {

    /**
     * 注册所有示例 Synergy
     */
    public static void registerAll() {
        SynergyManager manager = SynergyManager.getInstance();

        // 1. 能量循环 Synergy
        manager.register(createEnergyLoopSynergy());

        // 2. 战斗回响 Synergy
        manager.register(createCombatEchoSynergy());

        // 3. 生存护盾 Synergy
        manager.register(createSurvivalShieldSynergy());

        System.out.println("[Synergy] Registered " + manager.getSynergyCount() + " example synergies");
    }

    /**
     * 能量循环 Synergy
     *
     * 当玩家同时拥有「动能发电」和「太阳能发电」模块时，
     * 每秒有 20% 概率恢复少量能量。
     *
     * 设计理念：多种发电模块协同工作，产生额外的能量收益。
     */
    public static SynergyDefinition createEnergyLoopSynergy() {
        return SynergyDefinition.builder("energy_loop")
                .displayName("能量循环")
                .description("动能发电和太阳能发电模块的协同效果：每秒有概率获得额外能量")

                // 所需模块
                .requireModules("KINETIC_GENERATOR", "SOLAR_GENERATOR")
                .addLink("KINETIC_GENERATOR", "SOLAR_GENERATOR", "synergy")

                // 触发事件：每秒 tick
                .triggerOn(SynergyEventType.TICK)

                // 条件：20% 概率触发
                .addCondition(RandomChanceCondition.percent(20))

                // 效果：恢复能量 + 显示消息
                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        // 根据两个模块的等级计算能量
                        int kineticLevel = context.getModuleLevel("KINETIC_GENERATOR");
                        int solarLevel = context.getModuleLevel("SOLAR_GENERATOR");
                        int totalLevel = kineticLevel + solarLevel;

                        // 能量 = 50 + 总等级 * 25
                        int energy = 50 + totalLevel * 25;

                        ExistingModuleBridge.getInstance().addEnergy(context.getPlayer(), energy);
                    }

                    @Override
                    public String getDescription() {
                        return "Restore energy based on generator levels";
                    }
                })
                .addEffect(MessageEffect.actionBar("⚡ 能量循环 +", TextFormatting.YELLOW))

                .priority(100)
                .build();
    }

    /**
     * 战斗回响 Synergy
     *
     * 当玩家同时拥有「暴击」和「伤害增幅」模块时，
     * 暴击时额外造成真实伤害。
     *
     * 设计理念：战斗模块的协同增强暴击效果。
     */
    public static SynergyDefinition createCombatEchoSynergy() {
        return SynergyDefinition.builder("combat_echo")
                .displayName("战斗回响")
                .description("暴击和伤害增幅的协同效果：暴击时造成额外真实伤害")

                // 所需模块
                .requireModules("CRITICAL_STRIKE", "DAMAGE_BOOST")
                .addLink("CRITICAL_STRIKE", "DAMAGE_BOOST", "synergy")

                // 触发事件：暴击时
                .triggerOn(SynergyEventType.CRITICAL_HIT)

                // 条件：目标是怪物
                .addCondition(TargetCondition.isNotPlayer())

                // 效果：额外真实伤害
                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        if (context.getTarget() == null) return;

                        // 根据模块等级计算伤害
                        int critLevel = context.getModuleLevel("CRITICAL_STRIKE");
                        int damageLevel = context.getModuleLevel("DAMAGE_BOOST");

                        // 真实伤害 = 原始伤害 * 10% * (暴击等级 + 伤害等级) / 2
                        float baseDamage = context.getOriginalDamage();
                        float trueDamage = baseDamage * 0.1f * (critLevel + damageLevel) / 2f;

                        if (trueDamage > 0.5f) {
                            // 造成真实伤害
                            net.minecraft.util.DamageSource source =
                                    net.minecraft.util.DamageSource.causePlayerDamage(context.getPlayer());
                            source.setDamageBypassesArmor();
                            context.getTarget().attackEntityFrom(source, trueDamage);

                            // 粒子效果
                            context.getPlayer().world.spawnParticle(
                                    net.minecraft.util.EnumParticleTypes.CRIT_MAGIC,
                                    context.getTarget().posX,
                                    context.getTarget().posY + context.getTarget().height / 2,
                                    context.getTarget().posZ,
                                    0.5, 0.5, 0.5
                            );
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Deal extra true damage on critical hit";
                    }
                })
                .addEffect(MessageEffect.actionBar("⚔ 战斗回响!", TextFormatting.GOLD))

                .priority(50)
                .build();
    }

    /**
     * 生存护盾 Synergy
     *
     * 当玩家同时拥有「黄条护盾」和「自动灭火」模块时，
     * 受到环境伤害（火、溺水等）时获得临时护盾。
     *
     * 设计理念：防护模块的协同提供额外的环境保护。
     */
    public static SynergyDefinition createSurvivalShieldSynergy() {
        return SynergyDefinition.builder("survival_shield")
                .displayName("生存护盾")
                .description("护盾和灭火模块的协同效果：受到环境伤害时获得额外护盾")

                // 所需模块
                .requireModules("YELLOW_SHIELD", "FIRE_EXTINGUISH")
                .addLink("YELLOW_SHIELD", "FIRE_EXTINGUISH", "synergy")

                // 触发事件：受到环境伤害
                .triggerOn(SynergyEventType.ENVIRONMENTAL_DAMAGE)

                // 条件：总是触发（环境伤害已经在事件类型中筛选）

                // 效果：添加吸收心
                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        // 根据模块等级计算护盾量
                        int shieldLevel = context.getModuleLevel("YELLOW_SHIELD");
                        int fireLevel = context.getModuleLevel("FIRE_EXTINGUISH");

                        // 护盾量 = 2 + (护盾等级 + 灭火等级) / 2
                        float shieldAmount = 2.0f + (shieldLevel + fireLevel) / 2.0f;

                        // 护盾上限 = 护盾模块等级 * 4
                        float maxShield = shieldLevel * 4.0f;

                        // 添加护盾
                        float current = context.getPlayer().getAbsorptionAmount();
                        float newAmount = Math.min(current + shieldAmount, maxShield);

                        if (newAmount > current) {
                            context.getPlayer().setAbsorptionAmount(newAmount);

                            // 粒子效果
                            for (int i = 0; i < 10; i++) {
                                context.getPlayer().world.spawnParticle(
                                        net.minecraft.util.EnumParticleTypes.WATER_SPLASH,
                                        context.getPlayer().posX + (context.getPlayer().getRNG().nextDouble() - 0.5),
                                        context.getPlayer().posY + context.getPlayer().getRNG().nextDouble() * 2,
                                        context.getPlayer().posZ + (context.getPlayer().getRNG().nextDouble() - 0.5),
                                        0, 0.1, 0
                                );
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Gain absorption hearts when taking environmental damage";
                    }
                })
                .addEffect(MessageEffect.actionBar("💛 生存护盾激活!", TextFormatting.YELLOW))

                // 消耗能量
                .addEffect(EnergyEffect.consume(30))

                .priority(80)
                .build();
    }
}
