package com.moremod.synergy.synergies;

import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.synergy.bridge.ExistingModuleBridge;
import com.moremod.synergy.condition.*;
import com.moremod.synergy.core.*;
import com.moremod.synergy.effect.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * 战斗类 Synergy 定义 - 使用实际存在的模块
 *
 * 1. Berserker (狂战士) - 伤害提升 + 反伤荆棘 + 生命恢复
 * 2. Hunter (追猎者) - 追击打击 + 移动加速 + 攻击速度
 * 3. Iron Wall (铁壁) - 护甲强化 + 护盾生成 + 黄条护盾
 */
public class CombatSynergies {

    public static void registerAll(SynergyManager manager) {
        manager.register(createBerserker());
        manager.register(createHunter());
        manager.register(createIronWall());

        System.out.println("[Synergy] Registered 3 Combat Synergies");
    }

    /**
     * 狂战士 Synergy
     * 需要: DAMAGE_BOOST + THORNS + HEALTH_REGEN
     * 效果: 低血量时伤害大幅提升，受伤时反弹伤害并恢复生命
     */
    public static SynergyDefinition createBerserker() {
        return SynergyDefinition.builder("berserker")
                .displayName("狂战士")
                .description("以血换力，越战越强")

                .requireModules("DAMAGE_BOOST", "THORNS", "HEALTH_REGEN")
                .addLink("DAMAGE_BOOST", "THORNS", "triangle")
                .addLink("THORNS", "HEALTH_REGEN", "triangle")
                .addLink("HEALTH_REGEN", "DAMAGE_BOOST", "triangle")

                .triggerOn(SynergyEventType.ATTACK, SynergyEventType.HURT)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;

                        float healthPercent = player.getHealth() / player.getMaxHealth();

                        if (context.getEventType() == SynergyEventType.ATTACK) {
                            // 攻击时：低血量增伤
                            EntityLivingBase target = context.getTarget();
                            if (target == null) return;

                            int damageLevel = context.getModuleLevel("DAMAGE_BOOST");

                            // 血量越低伤害越高，最高50%额外伤害
                            float bonusMultiplier = (1.0f - healthPercent) * 0.5f;
                            float bonusDamage = context.getOriginalDamage() * bonusMultiplier * (1 + damageLevel * 0.1f);

                            if (bonusDamage > 0.5f) {
                                target.attackEntityFrom(DamageSource.causePlayerDamage(player), bonusDamage);

                                // 红色粒子效果
                                for (int i = 0; i < 8; i++) {
                                    world.spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR,
                                            target.posX + (world.rand.nextDouble() - 0.5) * target.width,
                                            target.posY + world.rand.nextDouble() * target.height,
                                            target.posZ + (world.rand.nextDouble() - 0.5) * target.width,
                                            0, 0.1, 0);
                                }
                            }

                        } else if (context.getEventType() == SynergyEventType.HURT) {
                            // 受伤时：恢复少量生命
                            int regenLevel = context.getModuleLevel("HEALTH_REGEN");
                            float healAmount = 1.0f + regenLevel * 0.5f;

                            player.heal(healAmount);

                            // 低血量时获得短暂力量
                            if (healthPercent < 0.3f) {
                                player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 60, 1, false, true));
                                world.playSound(null, player.posX, player.posY, player.posZ,
                                        SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.5f, 0.8f);
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Low HP grants bonus damage, taking damage heals slightly";
                    }
                })

                .priority(20)
                .build();
    }

    /**
     * 追猎者 Synergy
     * 需要: PURSUIT + MOVEMENT_SPEED + ATTACK_SPEED
     * 效果: 攻击后获得速度加成，连续攻击叠加效果
     */
    public static SynergyDefinition createHunter() {
        return SynergyDefinition.builder("hunter")
                .displayName("追猎者")
                .description("锁定猎物，追击不息")

                .requireModules("PURSUIT", "MOVEMENT_SPEED", "ATTACK_SPEED")
                .addLink("PURSUIT", "MOVEMENT_SPEED", "chain")
                .addLink("MOVEMENT_SPEED", "ATTACK_SPEED", "chain")

                .triggerOn(SynergyEventType.ATTACK)

                .addCondition(TargetCondition.isNotPlayer())

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        EntityLivingBase target = context.getTarget();
                        if (target == null) return;

                        int pursuitLevel = context.getModuleLevel("PURSUIT");
                        int speedLevel = context.getModuleLevel("MOVEMENT_SPEED");
                        int attackSpeedLevel = context.getModuleLevel("ATTACK_SPEED");

                        // 获得速度和急迫效果
                        int duration = 40 + (pursuitLevel + speedLevel) * 10;
                        int amplifier = Math.min(2, (pursuitLevel + speedLevel + attackSpeedLevel) / 3);

                        player.addPotionEffect(new PotionEffect(MobEffects.SPEED, duration, amplifier, false, true));
                        player.addPotionEffect(new PotionEffect(MobEffects.HASTE, duration, amplifier, false, true));

                        // 对逃跑的目标造成额外伤害
                        double targetSpeed = Math.sqrt(target.motionX * target.motionX + target.motionZ * target.motionZ);
                        if (targetSpeed > 0.1) {
                            float bonusDamage = context.getOriginalDamage() * 0.2f * pursuitLevel;
                            target.attackEntityFrom(DamageSource.causePlayerDamage(player), bonusDamage);

                            player.world.spawnParticle(EnumParticleTypes.SWEEP_ATTACK,
                                    target.posX, target.posY + 1, target.posZ,
                                    0, 0, 0);
                        }

                        // 消耗少量能量
                        ExistingModuleBridge.getInstance().consumeEnergy(player, 20);
                    }

                    @Override
                    public String getDescription() {
                        return "Gain speed on attack, bonus damage to fleeing targets";
                    }
                })
                .addEffect(MessageEffect.actionBar("🏃 追猎!", TextFormatting.AQUA))

                .priority(30)
                .build();
    }

    /**
     * 铁壁 Synergy
     * 需要: ARMOR_ENHANCEMENT + SHIELD_GENERATOR + YELLOW_SHIELD
     * 效果: 受伤时减少伤害并生成护盾
     */
    public static SynergyDefinition createIronWall() {
        return SynergyDefinition.builder("iron_wall")
                .displayName("铁壁")
                .description("坚不可摧的防御")

                .requireModules("ARMOR_ENHANCEMENT", "SHIELD_GENERATOR", "YELLOW_SHIELD")
                .addLink("ARMOR_ENHANCEMENT", "SHIELD_GENERATOR", "triangle")
                .addLink("SHIELD_GENERATOR", "YELLOW_SHIELD", "triangle")
                .addLink("YELLOW_SHIELD", "ARMOR_ENHANCEMENT", "triangle")

                .triggerOn(SynergyEventType.HURT)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;

                        int armorLevel = context.getModuleLevel("ARMOR_ENHANCEMENT");
                        int shieldGenLevel = context.getModuleLevel("SHIELD_GENERATOR");
                        int yellowShieldLevel = context.getModuleLevel("YELLOW_SHIELD");

                        int totalLevel = armorLevel + shieldGenLevel + yellowShieldLevel;

                        // 生成护盾
                        float shieldAmount = 1.0f + totalLevel * 0.5f;
                        float maxShield = 4.0f + yellowShieldLevel * 2.0f;

                        float current = player.getAbsorptionAmount();
                        float newAmount = Math.min(current + shieldAmount, maxShield);

                        if (newAmount > current) {
                            player.setAbsorptionAmount(newAmount);

                            // 护盾粒子效果
                            for (int i = 0; i < 15; i++) {
                                double angle = (i / 15.0) * Math.PI * 2;
                                world.spawnParticle(EnumParticleTypes.CRIT,
                                        player.posX + Math.cos(angle) * 0.8,
                                        player.posY + 1,
                                        player.posZ + Math.sin(angle) * 0.8,
                                        0, 0.05, 0);
                            }

                            world.playSound(null, player.posX, player.posY, player.posZ,
                                    SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.5f, 1.2f);
                        }

                        // 获得短暂抗性
                        if (totalLevel >= 6) {
                            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 40, 0, false, true));
                        }

                        // 消耗能量
                        ExistingModuleBridge.getInstance().consumeEnergy(player, 50);
                    }

                    @Override
                    public String getDescription() {
                        return "Generate shield when taking damage";
                    }
                })
                .addEffect(MessageEffect.actionBar("🛡 铁壁!", TextFormatting.BLUE))

                .priority(25)
                .build();
    }
}
