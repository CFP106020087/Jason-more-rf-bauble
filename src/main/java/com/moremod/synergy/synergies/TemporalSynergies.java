package com.moremod.synergy.synergies;

import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.synergy.bridge.ExistingModuleBridge;
import com.moremod.synergy.condition.*;
import com.moremod.synergy.core.*;
import com.moremod.synergy.effect.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * 生存类 Synergy 定义 - 使用实际存在的模块
 *
 * 1. Phoenix (不死鸟) - 生命恢复 + 药水恢复 + 自动灭火
 * 2. Self Sufficient (自给自足) - 饥饿管理 + 生命恢复 + 能量效率
 * 3. XP Master (经验大师) - 经验增幅 + 魔力熔炉 + 战斗充能
 */
public class TemporalSynergies {

    public static void registerAll(SynergyManager manager) {
        manager.register(createPhoenix());
        manager.register(createSelfSufficient());
        manager.register(createXPMaster());

        System.out.println("[Synergy] Registered 3 Survival Synergies");
    }

    /**
     * 不死鸟 Synergy
     * 需要: HEALTH_REGEN + REGENERATION + FIRE_EXTINGUISH
     * 效果: 低血量时大幅恢复，火焰伤害转化为治疗
     */
    public static SynergyDefinition createPhoenix() {
        return SynergyDefinition.builder("phoenix")
                .displayName("不死鸟")
                .description("浴火重生，生生不息")

                .requireModules("HEALTH_REGEN", "REGENERATION", "FIRE_EXTINGUISH")
                .addLink("HEALTH_REGEN", "REGENERATION", "triangle")
                .addLink("REGENERATION", "FIRE_EXTINGUISH", "triangle")
                .addLink("FIRE_EXTINGUISH", "HEALTH_REGEN", "triangle")

                .triggerOn(SynergyEventType.TICK, SynergyEventType.HURT)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;

                        int regenLevel = context.getModuleLevel("HEALTH_REGEN");
                        int potionLevel = context.getModuleLevel("REGENERATION");
                        int fireLevel = context.getModuleLevel("FIRE_EXTINGUISH");

                        float healthPercent = player.getHealth() / player.getMaxHealth();

                        if (context.getEventType() == SynergyEventType.TICK) {
                            // 低血量时触发强化恢复（持续回血）
                            if (healthPercent < 0.3f && player.ticksExisted % 10 == 0) {
                                int totalLevel = regenLevel + potionLevel + fireLevel;
                                float healAmount = 0.5f + totalLevel * 0.15f;
                                player.heal(healAmount);

                                // 不死鸟光环粒子
                                for (int i = 0; i < 8; i++) {
                                    double angle = (i / 8.0) * Math.PI * 2;
                                    world.spawnParticle(EnumParticleTypes.FLAME,
                                            player.posX + Math.cos(angle) * 0.6,
                                            player.posY + 1,
                                            player.posZ + Math.sin(angle) * 0.6,
                                            0, 0.05, 0);
                                }
                            }

                            // 着火时直接灭火并转化为治疗
                            if (player.isBurning()) {
                                // 快速灭火（概率基于等级）
                                if (player.ticksExisted % (Math.max(1, 10 - fireLevel * 2)) == 0) {
                                    player.extinguish();
                                }

                                // 火焰转化为治疗
                                if (player.ticksExisted % 10 == 0) {
                                    player.heal(0.5f + fireLevel * 0.3f);
                                    world.spawnParticle(EnumParticleTypes.FLAME,
                                            player.posX, player.posY + 1, player.posZ,
                                            0.3, 0.3, 0.3);
                                }
                            }

                        } else if (context.getEventType() == SynergyEventType.HURT) {
                            // 受伤时设置持续恢复标记
                            if (world.rand.nextFloat() < 0.3f) {
                                int duration = 60 + potionLevel * 20;
                                player.getEntityData().setLong("synergy_phoenix_regen", world.getTotalWorldTime() + duration);
                                player.getEntityData().setFloat("synergy_phoenix_heal_rate", 0.3f + potionLevel * 0.1f);

                                // 直接恢复一部分
                                player.heal(1.0f + potionLevel * 0.5f);

                                world.playSound(null, player.posX, player.posY, player.posZ,
                                        SoundEvents.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 0.3f, 1.5f);
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Enhanced regeneration at low HP, fire heals you";
                    }
                })
                .addEffect(MessageEffect.actionBar("🔥 不死鸟!", TextFormatting.GOLD))

                .priority(70)
                .build();
    }

    /**
     * 自给自足 Synergy
     * 需要: HUNGER_THIRST + HEALTH_REGEN + ENERGY_EFFICIENCY
     * 效果: 减少饥饿消耗，能量转化为饱食度
     */
    public static SynergyDefinition createSelfSufficient() {
        return SynergyDefinition.builder("self_sufficient")
                .displayName("自给自足")
                .description("机械体完美的能量循环")

                .requireModules("HUNGER_THIRST", "HEALTH_REGEN", "ENERGY_EFFICIENCY")
                .addLink("HUNGER_THIRST", "HEALTH_REGEN", "chain")
                .addLink("HEALTH_REGEN", "ENERGY_EFFICIENCY", "chain")

                .triggerOn(SynergyEventType.TICK)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();

                        // 每5秒触发一次
                        if (player.ticksExisted % 100 != 0) return;

                        int hungerLevel = context.getModuleLevel("HUNGER_THIRST");
                        int regenLevel = context.getModuleLevel("HEALTH_REGEN");
                        int efficiencyLevel = context.getModuleLevel("ENERGY_EFFICIENCY");

                        ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
                        float energyPercent = bridge.getEnergyPercent(player);

                        // 有足够能量时恢复饱食度
                        if (energyPercent > 0.5f && player.getFoodStats().getFoodLevel() < 18) {
                            int foodRestore = 1 + (hungerLevel + efficiencyLevel) / 2;
                            player.getFoodStats().addStats(foodRestore, 0.5f);

                            // 消耗少量能量
                            bridge.consumeEnergy(player, 100 * foodRestore);

                            player.world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                                    player.posX, player.posY + 1.5, player.posZ,
                                    0.3, 0.3, 0.3);
                        }

                        // 饱食度满时缓慢恢复生命
                        if (player.getFoodStats().getFoodLevel() >= 18 && player.getHealth() < player.getMaxHealth()) {
                            player.heal(0.5f + regenLevel * 0.2f);
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Convert energy to food, full hunger heals";
                    }
                })

                .priority(65)
                .build();
    }

    /**
     * 经验大师 Synergy
     * 需要: EXP_AMPLIFIER + MAGIC_ABSORB + COMBAT_CHARGER
     * 效果: 经验获取大幅提升，经验转化为能量
     */
    public static SynergyDefinition createXPMaster() {
        return SynergyDefinition.builder("xp_master")
                .displayName("经验大师")
                .description("知识就是力量")

                .requireModules("EXP_AMPLIFIER", "MAGIC_ABSORB", "COMBAT_CHARGER")
                .addLink("EXP_AMPLIFIER", "MAGIC_ABSORB", "triangle")
                .addLink("MAGIC_ABSORB", "COMBAT_CHARGER", "triangle")
                .addLink("COMBAT_CHARGER", "EXP_AMPLIFIER", "triangle")

                .triggerOn(SynergyEventType.KILL)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;

                        int expLevel = context.getModuleLevel("EXP_AMPLIFIER");
                        int magicLevel = context.getModuleLevel("MAGIC_ABSORB");
                        int chargerLevel = context.getModuleLevel("COMBAT_CHARGER");

                        int totalLevel = expLevel + magicLevel + chargerLevel;

                        // 击杀时获得额外经验
                        int bonusXP = 5 + totalLevel * 3;
                        player.addExperience(bonusXP);

                        // 经验转化为能量
                        int energyBonus = bonusXP * 20;
                        ExistingModuleBridge.getInstance().addEnergy(player, energyBonus);

                        // 经验粒子效果
                        for (int i = 0; i < 10; i++) {
                            world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                                    player.posX + (world.rand.nextDouble() - 0.5) * 2,
                                    player.posY + world.rand.nextDouble() * 2,
                                    player.posZ + (world.rand.nextDouble() - 0.5) * 2,
                                    0, 0.1, 0);
                        }

                        // 高等级时设置伤害加成标记
                        if (totalLevel >= 6 && world.rand.nextFloat() < 0.2f) {
                            player.getEntityData().setLong("synergy_xp_damage_boost", world.getTotalWorldTime() + 200);
                            player.getEntityData().setFloat("synergy_xp_damage_amount", 3.0f + totalLevel * 0.5f);

                            // 力量获得特效
                            world.playSound(null, player.posX, player.posY, player.posZ,
                                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.5f);

                            for (int i = 0; i < 8; i++) {
                                world.spawnParticle(EnumParticleTypes.CRIT,
                                        player.posX + (world.rand.nextDouble() - 0.5),
                                        player.posY + 1 + world.rand.nextDouble(),
                                        player.posZ + (world.rand.nextDouble() - 0.5),
                                        0, 0.1, 0);
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Bonus XP on kill, XP converts to energy";
                    }
                })
                .addEffect(MessageEffect.actionBar("✨ 经验获取!", TextFormatting.GREEN))

                .priority(50)
                .build();
    }
}
