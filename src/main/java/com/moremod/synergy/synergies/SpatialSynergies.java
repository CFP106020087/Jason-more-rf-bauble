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
 * 辅助类 Synergy 定义 - 使用实际存在的模块
 *
 * 1. Flight Master (飞行大师) - 飞行模块 + 能量效率 + 速度提升
 * 2. Miner's Eye (矿工之眼) - 矿物透视 + 隐身潜行 + 经验增幅
 * 3. Extreme Environment (极端环境) - 温度调节 + 防水模块 + 护甲强化
 */
public class SpatialSynergies {

    public static void registerAll(SynergyManager manager) {
        manager.register(createFlightMaster());
        manager.register(createMinersEye());
        manager.register(createExtremeEnvironment());

        System.out.println("[Synergy] Registered 3 Utility Synergies");
    }

    /**
     * 飞行大师 Synergy
     * 需要: FLIGHT_MODULE + ENERGY_EFFICIENCY + SPEED_BOOST
     * 效果: 飞行时消耗降低，速度提升
     */
    public static SynergyDefinition createFlightMaster() {
        return SynergyDefinition.builder("flight_master")
                .displayName("飞行大师")
                .description("掌控天空的王者")

                .requireModules("FLIGHT_MODULE", "ENERGY_EFFICIENCY", "SPEED_BOOST")
                .addLink("FLIGHT_MODULE", "ENERGY_EFFICIENCY", "triangle")
                .addLink("ENERGY_EFFICIENCY", "SPEED_BOOST", "triangle")
                .addLink("SPEED_BOOST", "FLIGHT_MODULE", "triangle")

                .triggerOn(SynergyEventType.TICK)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;

                        // 只在飞行时触发
                        if (!player.capabilities.isFlying) return;

                        int flightLevel = context.getModuleLevel("FLIGHT_MODULE");
                        int efficiencyLevel = context.getModuleLevel("ENERGY_EFFICIENCY");
                        int speedLevel = context.getModuleLevel("SPEED_BOOST");

                        // 直接飞行加速：根据飞行方向增强动量
                        if (player.ticksExisted % 2 == 0) {
                            double speedMultiplier = 1.0 + (flightLevel + speedLevel) * 0.03;
                            double currentHSpeed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);

                            // 水平飞行加速
                            if (currentHSpeed > 0.05 && currentHSpeed < 1.0) {
                                player.motionX *= speedMultiplier;
                                player.motionZ *= speedMultiplier;
                            }

                            // 垂直飞行辅助
                            if (Math.abs(player.motionY) > 0.05) {
                                player.motionY *= 1.0 + speedLevel * 0.02;
                            }
                        }

                        // 飞行时恢复能量(模拟效率提升)
                        if (player.ticksExisted % 20 == 0) {
                            int energyReturn = 5 * efficiencyLevel;
                            ExistingModuleBridge.getInstance().addEnergy(player, energyReturn);
                        }

                        // 飞行尾迹粒子
                        if (player.ticksExisted % 5 == 0) {
                            world.spawnParticle(EnumParticleTypes.CLOUD,
                                    player.posX - player.motionX * 2,
                                    player.posY,
                                    player.posZ - player.motionZ * 2,
                                    0, 0, 0);
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Enhanced flight speed and efficiency";
                    }
                })

                .priority(60)
                .build();
    }

    /**
     * 矿工之眼 Synergy
     * 需要: ORE_VISION + STEALTH + EXP_AMPLIFIER
     * 效果: 挖掘时获得额外经验和隐身
     */
    public static SynergyDefinition createMinersEye() {
        return SynergyDefinition.builder("miners_eye")
                .displayName("矿工之眼")
                .description("地底探索者的终极装备")

                .requireModules("ORE_VISION", "STEALTH", "EXP_AMPLIFIER")
                .addLink("ORE_VISION", "STEALTH", "chain")
                .addLink("STEALTH", "EXP_AMPLIFIER", "chain")

                .triggerOn(SynergyEventType.TICK)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;

                        // 地下时触发 (Y < 60)
                        if (player.posY >= 60) return;

                        int visionLevel = context.getModuleLevel("ORE_VISION");
                        int stealthLevel = context.getModuleLevel("STEALTH");
                        int expLevel = context.getModuleLevel("EXP_AMPLIFIER");

                        // 设置矿工之眼标记（供其他系统使用，如HUD渲染或矿石高亮）
                        player.getEntityData().setBoolean("synergy_miners_eye", true);
                        player.getEntityData().setInteger("synergy_miners_eye_level", visionLevel);
                        player.getEntityData().setLong("synergy_miners_eye_time", world.getTotalWorldTime() + 100);

                        // 潜行时减少怪物仇恨范围（通过标记实现）
                        if (player.isSneaking()) {
                            player.getEntityData().setBoolean("synergy_stealth_active", true);
                            player.getEntityData().setInteger("synergy_stealth_level", stealthLevel);
                            player.getEntityData().setLong("synergy_stealth_time", world.getTotalWorldTime() + 60);

                            // 潜行时的隐匿粒子
                            if (player.ticksExisted % 20 == 0) {
                                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                                        player.posX, player.posY + 0.5, player.posZ,
                                        0.1, 0.1, 0.1);
                            }
                        }

                        // 地下挖掘时额外经验（通过标记实现）
                        player.getEntityData().setInteger("synergy_exp_bonus", expLevel);

                        // 地下粒子效果
                        if (player.ticksExisted % 30 == 0 && !world.canSeeSky(player.getPosition())) {
                            world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                                    player.posX, player.posY + 1, player.posZ,
                                    0.5, 0.5, 0.5);
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Underground bonuses: ore detection and stealth";
                    }
                })

                .priority(45)
                .build();
    }

    /**
     * 极端环境 Synergy
     * 需要: TEMPERATURE_CONTROL + WATERPROOF_MODULE + ARMOR_ENHANCEMENT
     * 效果: 免疫环境伤害，水下呼吸
     */
    public static SynergyDefinition createExtremeEnvironment() {
        return SynergyDefinition.builder("extreme_environment")
                .displayName("极端环境")
                .description("适应任何环境的生存系统")

                .requireModules("TEMPERATURE_CONTROL", "WATERPROOF_MODULE", "ARMOR_ENHANCEMENT")
                .addLink("TEMPERATURE_CONTROL", "WATERPROOF_MODULE", "triangle")
                .addLink("WATERPROOF_MODULE", "ARMOR_ENHANCEMENT", "triangle")
                .addLink("ARMOR_ENHANCEMENT", "TEMPERATURE_CONTROL", "triangle")

                .triggerOn(SynergyEventType.TICK, SynergyEventType.ENVIRONMENTAL_DAMAGE)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;

                        int tempLevel = context.getModuleLevel("TEMPERATURE_CONTROL");
                        int waterLevel = context.getModuleLevel("WATERPROOF_MODULE");
                        int armorLevel = context.getModuleLevel("ARMOR_ENHANCEMENT");

                        int totalLevel = tempLevel + waterLevel + armorLevel;

                        if (context.getEventType() == SynergyEventType.TICK) {
                            // 水下时直接恢复氧气
                            if (player.isInWater()) {
                                int airRestoreRate = 10 + waterLevel * 5;
                                int newAir = Math.min(player.getAir() + airRestoreRate, 300);
                                player.setAir(newAir);

                                // 水下气泡效果
                                if (player.ticksExisted % 20 == 0) {
                                    world.spawnParticle(EnumParticleTypes.WATER_BUBBLE,
                                            player.posX, player.posY + 1.5, player.posZ,
                                            0.2, 0.2, 0.2);
                                }
                            }

                            // 着火时直接灭火并恢复
                            if (player.isBurning()) {
                                // 减少火焰燃烧时间
                                int newFireTicks = Math.max(0, player.fire - 5 - tempLevel * 2);
                                player.fire = newFireTicks;

                                // 火焰伤害转化为治疗
                                if (player.ticksExisted % 20 == 0) {
                                    player.heal(0.5f + tempLevel * 0.2f);
                                    world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                                            player.posX, player.posY + 1, player.posZ,
                                            0.3, 0.3, 0.3);
                                }
                            }

                            // 高等级时被动护盾（代替抗性）
                            if (totalLevel >= 9 && player.ticksExisted % 100 == 0) {
                                float currentShield = player.getAbsorptionAmount();
                                float maxShield = 2.0f + armorLevel;
                                if (currentShield < maxShield) {
                                    player.setAbsorptionAmount(currentShield + 1.0f);
                                }
                            }

                        } else if (context.getEventType() == SynergyEventType.ENVIRONMENTAL_DAMAGE) {
                            // 环境伤害时恢复少量生命
                            float heal = 0.5f + totalLevel * 0.2f;
                            player.heal(heal);

                            world.playSound(null, player.posX, player.posY, player.posZ,
                                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.3f, 1.5f);
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Adapt to extreme environments";
                    }
                })
                .addEffect(MessageEffect.actionBar("🌡 环境适应!", TextFormatting.DARK_AQUA))

                .priority(55)
                .build();
    }
}
