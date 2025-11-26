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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

/**
 * 终极类 Synergy 定义 - 使用实际存在的模块 (4模块组合)
 *
 * 1. Ultimate Defense (终极防御) - 护甲强化 + 护盾生成 + 黄条护盾 + 能量效率
 * 2. Ultimate Offense (终极攻击) - 伤害提升 + 攻击速度 + 追击打击 + 战斗充能
 * 3. Ultimate Survival (终极生存) - 生命恢复 + 饥饿管理 + 温度调节 + 自动灭火
 */
public class DomainSynergies {

    public static void registerAll(SynergyManager manager) {
        manager.register(createUltimateDefense());
        manager.register(createUltimateOffense());
        manager.register(createUltimateSurvival());

        System.out.println("[Synergy] Registered 3 Ultimate Synergies");
    }

    /**
     * 终极防御 Synergy
     * 需要: ARMOR_ENHANCEMENT + SHIELD_GENERATOR + YELLOW_SHIELD + ENERGY_EFFICIENCY
     * 效果: 强大的防御光环，受伤时有概率完全格挡
     */
    public static SynergyDefinition createUltimateDefense() {
        return SynergyDefinition.builder("ultimate_defense")
                .displayName("终极防御")
                .description("不可破的绝对防御")

                .requireModules("ARMOR_ENHANCEMENT", "SHIELD_GENERATOR", "YELLOW_SHIELD", "ENERGY_EFFICIENCY")
                .addLink("ARMOR_ENHANCEMENT", "SHIELD_GENERATOR", "diamond")
                .addLink("SHIELD_GENERATOR", "YELLOW_SHIELD", "diamond")
                .addLink("YELLOW_SHIELD", "ENERGY_EFFICIENCY", "diamond")
                .addLink("ENERGY_EFFICIENCY", "ARMOR_ENHANCEMENT", "diamond")

                .triggerOn(SynergyEventType.HURT, SynergyEventType.TICK)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;

                        int armorLevel = context.getModuleLevel("ARMOR_ENHANCEMENT");
                        int shieldGenLevel = context.getModuleLevel("SHIELD_GENERATOR");
                        int yellowLevel = context.getModuleLevel("YELLOW_SHIELD");
                        int efficiencyLevel = context.getModuleLevel("ENERGY_EFFICIENCY");

                        int totalLevel = armorLevel + shieldGenLevel + yellowLevel + efficiencyLevel;

                        if (context.getEventType() == SynergyEventType.HURT) {
                            // 有概率完全格挡伤害
                            float blockChance = 0.05f + totalLevel * 0.02f;
                            if (world.rand.nextFloat() < blockChance) {
                                // 格挡成功 - 通过恢复等量生命来模拟
                                float damage = context.getOriginalDamage();
                                player.heal(damage);

                                // 格挡特效
                                for (int i = 0; i < 20; i++) {
                                    double angle = (i / 20.0) * Math.PI * 2;
                                    world.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                                            player.posX + Math.cos(angle) * 1.0,
                                            player.posY + 1,
                                            player.posZ + Math.sin(angle) * 1.0,
                                            0, 0.1, 0);
                                }

                                world.playSound(null, player.posX, player.posY, player.posZ,
                                        SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 1.0f);
                            }

                            // 受伤时恢复护盾
                            float shieldAmount = 2.0f + shieldGenLevel * 0.5f;
                            float maxShield = 8.0f + yellowLevel * 2.0f;
                            float current = player.getAbsorptionAmount();
                            player.setAbsorptionAmount(Math.min(current + shieldAmount, maxShield));

                        } else if (context.getEventType() == SynergyEventType.TICK) {
                            // 每秒被动恢复护盾
                            if (player.ticksExisted % 20 == 0) {
                                float current = player.getAbsorptionAmount();
                                float maxShield = 4.0f + yellowLevel;
                                if (current < maxShield) {
                                    player.setAbsorptionAmount(Math.min(current + 0.5f, maxShield));
                                }
                            }

                            // 防御光环粒子
                            if (player.ticksExisted % 40 == 0) {
                                for (int i = 0; i < 8; i++) {
                                    double angle = (player.ticksExisted / 20.0 + i * 0.785) % (Math.PI * 2);
                                    world.spawnParticle(EnumParticleTypes.CRIT,
                                            player.posX + Math.cos(angle) * 1.2,
                                            player.posY + 0.5,
                                            player.posZ + Math.sin(angle) * 1.2,
                                            0, 0.02, 0);
                                }
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Chance to block damage, passive shield regen";
                    }
                })
                .addEffect(MessageEffect.actionBar("🛡 终极防御!", TextFormatting.AQUA))
                .addEffect(EnergyEffect.consume(20))

                .priority(10)
                .build();
    }

    /**
     * 终极攻击 Synergy
     * 需要: DAMAGE_BOOST + ATTACK_SPEED + PURSUIT + COMBAT_CHARGER
     * 效果: 攻击时叠加伤害，达到最大层数时爆发
     */
    public static SynergyDefinition createUltimateOffense() {
        return SynergyDefinition.builder("ultimate_offense")
                .displayName("终极攻击")
                .description("毁灭一切的绝对力量")

                .requireModules("DAMAGE_BOOST", "ATTACK_SPEED", "PURSUIT", "COMBAT_CHARGER")
                .addLink("DAMAGE_BOOST", "ATTACK_SPEED", "diamond")
                .addLink("ATTACK_SPEED", "PURSUIT", "diamond")
                .addLink("PURSUIT", "COMBAT_CHARGER", "diamond")
                .addLink("COMBAT_CHARGER", "DAMAGE_BOOST", "diamond")

                .triggerOn(SynergyEventType.ATTACK, SynergyEventType.KILL)

                .addCondition(TargetCondition.isNotPlayer())

                .addEffect(new ISynergyEffect() {
                    private int attackStacks = 0;
                    private long lastAttackTime = 0;

                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        EntityLivingBase target = context.getTarget();
                        World world = player.world;

                        int damageLevel = context.getModuleLevel("DAMAGE_BOOST");
                        int speedLevel = context.getModuleLevel("ATTACK_SPEED");
                        int pursuitLevel = context.getModuleLevel("PURSUIT");
                        int chargerLevel = context.getModuleLevel("COMBAT_CHARGER");

                        int totalLevel = damageLevel + speedLevel + pursuitLevel + chargerLevel;
                        int maxStacks = 10 + totalLevel;

                        long currentTime = world.getTotalWorldTime();

                        if (context.getEventType() == SynergyEventType.ATTACK) {
                            // 检查连击
                            if (currentTime - lastAttackTime < 40) {
                                attackStacks = Math.min(attackStacks + 1, maxStacks);
                            } else {
                                attackStacks = 1;
                            }
                            lastAttackTime = currentTime;

                            // 基于层数的额外伤害
                            if (target != null) {
                                float bonusDamage = context.getOriginalDamage() * 0.05f * attackStacks;
                                target.attackEntityFrom(DamageSource.causePlayerDamage(player), bonusDamage);

                                // 攻击速度加成
                                player.addPotionEffect(new PotionEffect(MobEffects.HASTE, 40, Math.min(2, attackStacks / 5), false, false));

                                // 高层数特效
                                if (attackStacks >= 5) {
                                    world.spawnParticle(EnumParticleTypes.SWEEP_ATTACK,
                                            target.posX, target.posY + 1, target.posZ,
                                            0, 0, 0);
                                }

                                // 满层数爆发
                                if (attackStacks >= maxStacks) {
                                    float burstDamage = context.getOriginalDamage() * 2.0f;
                                    target.attackEntityFrom(DamageSource.causePlayerDamage(player), burstDamage);

                                    // 范围爆炸
                                    AxisAlignedBB area = target.getEntityBoundingBox().grow(3.0);
                                    List<EntityLivingBase> nearby = world.getEntitiesWithinAABB(EntityLivingBase.class, area,
                                            e -> e != player && e != target && !e.isDead);
                                    for (EntityLivingBase entity : nearby) {
                                        entity.attackEntityFrom(DamageSource.causePlayerDamage(player), burstDamage * 0.5f);
                                    }

                                    world.playSound(null, player.posX, player.posY, player.posZ,
                                            SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 1.5f);

                                    // 爆发特效
                                    for (int i = 0; i < 30; i++) {
                                        world.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                                                target.posX + (world.rand.nextDouble() - 0.5) * 3,
                                                target.posY + world.rand.nextDouble() * 2,
                                                target.posZ + (world.rand.nextDouble() - 0.5) * 3,
                                                0, 0.2, 0);
                                    }

                                    attackStacks = 0;
                                }
                            }

                            // 恢复能量
                            ExistingModuleBridge.getInstance().addEnergy(player, 20 + chargerLevel * 10);

                        } else if (context.getEventType() == SynergyEventType.KILL) {
                            // 击杀时重置为满层
                            attackStacks = maxStacks;

                            player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 100, 1, false, true));

                            ExistingModuleBridge.getInstance().addEnergy(player, 200 + totalLevel * 30);
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Stack damage on consecutive attacks, burst at max";
                    }
                })
                .addEffect(MessageEffect.actionBar("⚔ 终极攻击!", TextFormatting.RED))

                .priority(5)
                .build();
    }

    /**
     * 终极生存 Synergy
     * 需要: HEALTH_REGEN + HUNGER_THIRST + TEMPERATURE_CONTROL + FIRE_EXTINGUISH
     * 效果: 完全自给自足，几乎不死
     */
    public static SynergyDefinition createUltimateSurvival() {
        return SynergyDefinition.builder("ultimate_survival")
                .displayName("终极生存")
                .description("适应一切环境的完美生命体")

                .requireModules("HEALTH_REGEN", "HUNGER_THIRST", "TEMPERATURE_CONTROL", "FIRE_EXTINGUISH")
                .addLink("HEALTH_REGEN", "HUNGER_THIRST", "diamond")
                .addLink("HUNGER_THIRST", "TEMPERATURE_CONTROL", "diamond")
                .addLink("TEMPERATURE_CONTROL", "FIRE_EXTINGUISH", "diamond")
                .addLink("FIRE_EXTINGUISH", "HEALTH_REGEN", "diamond")

                .triggerOn(SynergyEventType.TICK, SynergyEventType.ENVIRONMENTAL_DAMAGE)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;

                        int regenLevel = context.getModuleLevel("HEALTH_REGEN");
                        int hungerLevel = context.getModuleLevel("HUNGER_THIRST");
                        int tempLevel = context.getModuleLevel("TEMPERATURE_CONTROL");
                        int fireLevel = context.getModuleLevel("FIRE_EXTINGUISH");

                        int totalLevel = regenLevel + hungerLevel + tempLevel + fireLevel;

                        if (context.getEventType() == SynergyEventType.TICK) {
                            // 每秒恢复生命
                            if (player.ticksExisted % 20 == 0 && player.getHealth() < player.getMaxHealth()) {
                                float healAmount = 0.5f + totalLevel * 0.1f;
                                player.heal(healAmount);
                            }

                            // 每10秒恢复饱食度
                            if (player.ticksExisted % 200 == 0 && player.getFoodStats().getFoodLevel() < 20) {
                                player.getFoodStats().addStats(1 + hungerLevel / 2, 0.5f);
                            }

                            // 免疫各种状态效果
                            if (player.ticksExisted % 40 == 0) {
                                player.removePotionEffect(MobEffects.HUNGER);
                                player.removePotionEffect(MobEffects.POISON);

                                if (totalLevel >= 10) {
                                    player.removePotionEffect(MobEffects.WITHER);
                                }
                            }

                            // 火焰/寒冷环境适应
                            if (player.isBurning()) {
                                player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 100, 0, false, false));
                                player.heal(0.1f * fireLevel);
                            }

                            // 生存光环粒子
                            if (player.ticksExisted % 60 == 0) {
                                world.spawnParticle(EnumParticleTypes.HEART,
                                        player.posX, player.posY + 2, player.posZ,
                                        0.3, 0.3, 0.3);
                            }

                        } else if (context.getEventType() == SynergyEventType.ENVIRONMENTAL_DAMAGE) {
                            // 环境伤害转化为治疗
                            float heal = 1.0f + totalLevel * 0.2f;
                            player.heal(heal);

                            // 恢复饱食度
                            if (player.getFoodStats().getFoodLevel() < 20) {
                                player.getFoodStats().addStats(1, 0.5f);
                            }

                            world.playSound(null, player.posX, player.posY, player.posZ,
                                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.3f, 1.5f);
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Constant regen, immune to negative effects";
                    }
                })
                .addEffect(MessageEffect.actionBar("💚 终极生存!", TextFormatting.GREEN))

                .priority(15)
                .build();
    }
}
