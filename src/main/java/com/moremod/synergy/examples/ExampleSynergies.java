package com.moremod.synergy.examples;

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
 * 示例 Synergy 定义 - 使用实际存在的模块
 *
 * 1. Energy Loop (能量循环) - 动能发电 + 太阳能发电
 * 2. Combat Echo (战斗回响) - 伤害提升 + 攻击速度
 * 3. Survival Shield (生存护盾) - 黄条护盾 + 自动灭火
 */
public class ExampleSynergies {

    public static void registerAll() {
        SynergyManager manager = SynergyManager.getInstance();

        manager.register(createEnergyLoopSynergy());
        manager.register(createCombatEchoSynergy());
        manager.register(createSurvivalShieldSynergy());

        System.out.println("[Synergy] Registered 3 example synergies");
    }

    /**
     * 能量循环 Synergy
     * 需要: KINETIC_GENERATOR + SOLAR_GENERATOR
     * 效果: 每秒有20%概率恢复能量
     */
    public static SynergyDefinition createEnergyLoopSynergy() {
        return SynergyDefinition.builder("energy_loop")
                .displayName("能量循环")
                .description("动能发电和太阳能发电模块的协同效果：每秒有概率获得额外能量")

                .requireModules("KINETIC_GENERATOR", "SOLAR_GENERATOR")
                .addLink("KINETIC_GENERATOR", "SOLAR_GENERATOR", "synergy")

                .triggerOn(SynergyEventType.TICK)

                .addCondition(RandomChanceCondition.percent(20))

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        int kineticLevel = context.getModuleLevel("KINETIC_GENERATOR");
                        int solarLevel = context.getModuleLevel("SOLAR_GENERATOR");
                        int totalLevel = kineticLevel + solarLevel;

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
     * 需要: DAMAGE_BOOST + ATTACK_SPEED
     * 效果: 攻击时有概率造成额外伤害
     */
    public static SynergyDefinition createCombatEchoSynergy() {
        return SynergyDefinition.builder("combat_echo")
                .displayName("战斗回响")
                .description("伤害提升和攻击速度的协同效果：攻击时有概率造成额外伤害")

                .requireModules("DAMAGE_BOOST", "ATTACK_SPEED")
                .addLink("DAMAGE_BOOST", "ATTACK_SPEED", "synergy")

                .triggerOn(SynergyEventType.ATTACK)

                .addCondition(RandomChanceCondition.percent(25))
                .addCondition(TargetCondition.isNotPlayer())

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        if (context.getTarget() == null) return;

                        int damageLevel = context.getModuleLevel("DAMAGE_BOOST");
                        int speedLevel = context.getModuleLevel("ATTACK_SPEED");

                        float baseDamage = context.getOriginalDamage();
                        float bonusDamage = baseDamage * 0.15f * (damageLevel + speedLevel) / 2f;

                        if (bonusDamage > 0.5f) {
                            net.minecraft.util.DamageSource source =
                                    net.minecraft.util.DamageSource.causePlayerDamage(context.getPlayer());
                            context.getTarget().attackEntityFrom(source, bonusDamage);

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
                        return "Deal extra damage on attack";
                    }
                })
                .addEffect(MessageEffect.actionBar("⚔ 战斗回响!", TextFormatting.GOLD))

                .priority(50)
                .build();
    }

    /**
     * 生存护盾 Synergy
     * 需要: YELLOW_SHIELD + FIRE_EXTINGUISH
     * 效果: 受到环境伤害时获得护盾
     */
    public static SynergyDefinition createSurvivalShieldSynergy() {
        return SynergyDefinition.builder("survival_shield")
                .displayName("生存护盾")
                .description("护盾和灭火模块的协同效果：受到环境伤害时获得额外护盾")

                .requireModules("YELLOW_SHIELD", "FIRE_EXTINGUISH")
                .addLink("YELLOW_SHIELD", "FIRE_EXTINGUISH", "synergy")

                .triggerOn(SynergyEventType.ENVIRONMENTAL_DAMAGE)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        int shieldLevel = context.getModuleLevel("YELLOW_SHIELD");
                        int fireLevel = context.getModuleLevel("FIRE_EXTINGUISH");

                        float shieldAmount = 2.0f + (shieldLevel + fireLevel) / 2.0f;
                        float maxShield = shieldLevel * 4.0f;

                        float current = context.getPlayer().getAbsorptionAmount();
                        float newAmount = Math.min(current + shieldAmount, maxShield);

                        if (newAmount > current) {
                            context.getPlayer().setAbsorptionAmount(newAmount);

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
                .addEffect(EnergyEffect.consume(30))

                .priority(80)
                .build();
    }
}
