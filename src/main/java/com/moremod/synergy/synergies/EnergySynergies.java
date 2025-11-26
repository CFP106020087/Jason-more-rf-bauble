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
 * 能量类 Synergy 定义 - 使用实际存在的模块
 *
 * 1. Energy Overload (能量过载) - 能量容量 + 能量效率 + 虚空能量
 * 2. Combat Generator (战斗发电机) - 战斗充能 + 伤害提升 + 攻击速度
 * 3. Perpetual Motion (永动机) - 动能发电 + 太阳能发电 + 能量效率
 */
public class EnergySynergies {

    public static void registerAll(SynergyManager manager) {
        manager.register(createEnergyOverload());
        manager.register(createCombatGenerator());
        manager.register(createPerpetualMotion());

        System.out.println("[Synergy] Registered 3 Energy Synergies");
    }

    /**
     * 能量过载 Synergy
     * 需要: ENERGY_CAPACITY + ENERGY_EFFICIENCY + VOID_ENERGY
     * 效果: 能量满时获得增益，能量消耗时有概率不消耗
     */
    public static SynergyDefinition createEnergyOverload() {
        return SynergyDefinition.builder("energy_overload")
                .displayName("能量过载")
                .description("能量核心超载运行，释放隐藏潜力")

                .requireModules("ENERGY_CAPACITY", "ENERGY_EFFICIENCY", "VOID_ENERGY")
                .addLink("ENERGY_CAPACITY", "ENERGY_EFFICIENCY", "triangle")
                .addLink("ENERGY_EFFICIENCY", "VOID_ENERGY", "triangle")
                .addLink("VOID_ENERGY", "ENERGY_CAPACITY", "triangle")

                .triggerOn(SynergyEventType.TICK)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();

                        int capacityLevel = context.getModuleLevel("ENERGY_CAPACITY");
                        int efficiencyLevel = context.getModuleLevel("ENERGY_EFFICIENCY");
                        int voidLevel = context.getModuleLevel("VOID_ENERGY");

                        float energyPercent = bridge.getEnergyPercent(player);

                        // 能量超过80%时获得增益
                        if (energyPercent > 0.8f) {
                            int totalLevel = capacityLevel + efficiencyLevel + voidLevel;

                            // 速度加成
                            if (player.ticksExisted % 40 == 0) {
                                player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 60, 0, false, false));
                            }

                            // 每秒有概率恢复生命
                            if (player.ticksExisted % 20 == 0 && player.world.rand.nextFloat() < 0.3f) {
                                player.heal(0.5f + totalLevel * 0.1f);
                            }

                            // 粒子效果
                            if (player.ticksExisted % 10 == 0) {
                                player.world.spawnParticle(EnumParticleTypes.END_ROD,
                                        player.posX, player.posY + 1.5, player.posZ,
                                        (player.world.rand.nextDouble() - 0.5) * 0.3,
                                        0.1,
                                        (player.world.rand.nextDouble() - 0.5) * 0.3);
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "High energy grants passive buffs";
                    }
                })

                .priority(40)
                .build();
    }

    /**
     * 战斗发电机 Synergy
     * 需要: COMBAT_CHARGER + DAMAGE_BOOST + ATTACK_SPEED
     * 效果: 攻击时恢复大量能量，连击加成
     */
    public static SynergyDefinition createCombatGenerator() {
        return SynergyDefinition.builder("combat_generator")
                .displayName("战斗发电机")
                .description("以战养战，战斗中恢复能量")

                .requireModules("COMBAT_CHARGER", "DAMAGE_BOOST", "ATTACK_SPEED")
                .addLink("COMBAT_CHARGER", "DAMAGE_BOOST", "chain")
                .addLink("DAMAGE_BOOST", "ATTACK_SPEED", "chain")

                .triggerOn(SynergyEventType.ATTACK, SynergyEventType.KILL)

                .addCondition(TargetCondition.isNotPlayer())

                .addEffect(new ISynergyEffect() {
                    private int comboCount = 0;
                    private long lastAttackTime = 0;

                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
                        World world = player.world;

                        int chargerLevel = context.getModuleLevel("COMBAT_CHARGER");
                        int damageLevel = context.getModuleLevel("DAMAGE_BOOST");
                        int speedLevel = context.getModuleLevel("ATTACK_SPEED");

                        long currentTime = world.getTotalWorldTime();

                        if (context.getEventType() == SynergyEventType.ATTACK) {
                            // 检查连击
                            if (currentTime - lastAttackTime < 30) {
                                comboCount = Math.min(comboCount + 1, 10);
                            } else {
                                comboCount = 1;
                            }
                            lastAttackTime = currentTime;

                            // 基础能量恢复 + 连击加成
                            int baseEnergy = 30 + chargerLevel * 15;
                            int comboBonus = comboCount * 5;
                            int totalEnergy = baseEnergy + comboBonus;

                            bridge.addEnergy(player, totalEnergy);

                            // 连击粒子
                            if (comboCount >= 3) {
                                world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                                        player.posX, player.posY + 2, player.posZ,
                                        0.3, 0.3, 0.3);
                            }

                        } else if (context.getEventType() == SynergyEventType.KILL) {
                            // 击杀恢复大量能量
                            int killEnergy = 200 + (chargerLevel + damageLevel + speedLevel) * 50;
                            bridge.addEnergy(player, killEnergy);

                            world.playSound(null, player.posX, player.posY, player.posZ,
                                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.2f);

                            // 重置连击为最大
                            comboCount = 10;
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Restore energy on attack, bonus on kill";
                    }
                })
                .addEffect(MessageEffect.actionBar("⚡ 战斗充能!", TextFormatting.GOLD))

                .priority(35)
                .build();
    }

    /**
     * 永动机 Synergy
     * 需要: KINETIC_GENERATOR + SOLAR_GENERATOR + ENERGY_EFFICIENCY
     * 效果: 大幅提高能量生成，几乎无消耗
     */
    public static SynergyDefinition createPerpetualMotion() {
        return SynergyDefinition.builder("perpetual_motion")
                .displayName("永动机")
                .description("趋近于永动的能量循环系统")

                .requireModules("KINETIC_GENERATOR", "SOLAR_GENERATOR", "ENERGY_EFFICIENCY")
                .addLink("KINETIC_GENERATOR", "SOLAR_GENERATOR", "triangle")
                .addLink("SOLAR_GENERATOR", "ENERGY_EFFICIENCY", "triangle")
                .addLink("ENERGY_EFFICIENCY", "KINETIC_GENERATOR", "triangle")

                .triggerOn(SynergyEventType.TICK)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
                        World world = player.world;

                        // 只在每秒触发
                        if (player.ticksExisted % 20 != 0) return;

                        int kineticLevel = context.getModuleLevel("KINETIC_GENERATOR");
                        int solarLevel = context.getModuleLevel("SOLAR_GENERATOR");
                        int efficiencyLevel = context.getModuleLevel("ENERGY_EFFICIENCY");

                        int totalLevel = kineticLevel + solarLevel + efficiencyLevel;

                        // 基础能量生成
                        int baseEnergy = 20 + totalLevel * 10;

                        // 移动时额外发电
                        double speed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
                        if (speed > 0.1) {
                            baseEnergy += (int)(speed * 100 * kineticLevel);
                        }

                        // 白天额外发电
                        if (world.isDaytime() && world.canSeeSky(player.getPosition())) {
                            baseEnergy += 10 * solarLevel;
                        }

                        // 效率加成
                        baseEnergy = (int)(baseEnergy * (1.0f + efficiencyLevel * 0.15f));

                        bridge.addEnergy(player, baseEnergy);

                        // 永动机粒子
                        if (player.ticksExisted % 40 == 0) {
                            for (int i = 0; i < 3; i++) {
                                double angle = (player.ticksExisted / 10.0 + i * 2.1) % (Math.PI * 2);
                                world.spawnParticle(EnumParticleTypes.REDSTONE,
                                        player.posX + Math.cos(angle) * 0.5,
                                        player.posY + 1 + Math.sin(player.ticksExisted / 5.0) * 0.3,
                                        player.posZ + Math.sin(angle) * 0.5,
                                        0, 1, 0);
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Greatly enhanced passive energy generation";
                    }
                })

                .priority(50)
                .build();
    }
}
