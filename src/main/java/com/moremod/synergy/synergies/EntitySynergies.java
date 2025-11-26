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
 * 范围类 Synergy 定义 - 使用实际存在的模块
 *
 * 1. Area Control (范围控制) - 范围拓展 + 护甲强化 + 护盾生成
 * 2. Void Harvester (虚空收割) - 虚空能量 + 范围拓展 + 伤害提升
 * 3. Speed Demon (速度恶魔) - 速度提升 + 移动加速 + 追击打击
 */
public class EntitySynergies {

    public static void registerAll(SynergyManager manager) {
        manager.register(createAreaControl());
        manager.register(createVoidHarvester());
        manager.register(createSpeedDemon());

        System.out.println("[Synergy] Registered 3 Area Synergies");
    }

    /**
     * 范围控制 Synergy
     * 需要: RANGE_EXTENSION + ARMOR_ENHANCEMENT + SHIELD_GENERATOR
     * 效果: 对周围敌人造成减速，自身获得护盾
     */
    public static SynergyDefinition createAreaControl() {
        return SynergyDefinition.builder("area_control")
                .displayName("范围控制")
                .description("掌控战场的主导权")

                .requireModules("RANGE_EXTENSION", "ARMOR_ENHANCEMENT", "SHIELD_GENERATOR")
                .addLink("RANGE_EXTENSION", "ARMOR_ENHANCEMENT", "triangle")
                .addLink("ARMOR_ENHANCEMENT", "SHIELD_GENERATOR", "triangle")
                .addLink("SHIELD_GENERATOR", "RANGE_EXTENSION", "triangle")

                .triggerOn(SynergyEventType.TICK)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;

                        // 每2秒触发
                        if (player.ticksExisted % 40 != 0) return;

                        int rangeLevel = context.getModuleLevel("RANGE_EXTENSION");
                        int armorLevel = context.getModuleLevel("ARMOR_ENHANCEMENT");
                        int shieldLevel = context.getModuleLevel("SHIELD_GENERATOR");

                        double radius = 3.0 + rangeLevel * 1.0;

                        // 范围减速敌人
                        AxisAlignedBB area = player.getEntityBoundingBox().grow(radius);
                        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, area,
                                e -> e != player && !e.isDead);

                        for (EntityLivingBase entity : entities) {
                            entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 0, false, true));

                            // 粒子连线
                            world.spawnParticle(EnumParticleTypes.CRIT,
                                    entity.posX, entity.posY + 1, entity.posZ,
                                    0, 0, 0);
                        }

                        // 有敌人在范围内时获得护盾
                        if (!entities.isEmpty()) {
                            float shieldAmount = 0.5f + shieldLevel * 0.5f;
                            float maxShield = 4.0f + armorLevel * 1.0f;
                            float current = player.getAbsorptionAmount();

                            if (current < maxShield) {
                                player.setAbsorptionAmount(Math.min(current + shieldAmount, maxShield));
                            }
                        }

                        // 消耗能量
                        ExistingModuleBridge.getInstance().consumeEnergy(player, 30);
                    }

                    @Override
                    public String getDescription() {
                        return "Slow nearby enemies, gain shield";
                    }
                })

                .priority(40)
                .build();
    }

    /**
     * 虚空收割 Synergy
     * 需要: VOID_ENERGY + RANGE_EXTENSION + DAMAGE_BOOST
     * 效果: 攻击时对周围敌人造成范围伤害
     */
    public static SynergyDefinition createVoidHarvester() {
        return SynergyDefinition.builder("void_harvester")
                .displayName("虚空收割")
                .description("虚空的力量吞噬一切")

                .requireModules("VOID_ENERGY", "RANGE_EXTENSION", "DAMAGE_BOOST")
                .addLink("VOID_ENERGY", "RANGE_EXTENSION", "chain")
                .addLink("RANGE_EXTENSION", "DAMAGE_BOOST", "chain")

                .triggerOn(SynergyEventType.ATTACK)

                .addCondition(TargetCondition.isNotPlayer())

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        EntityLivingBase target = context.getTarget();
                        if (target == null) return;

                        World world = player.world;

                        int voidLevel = context.getModuleLevel("VOID_ENERGY");
                        int rangeLevel = context.getModuleLevel("RANGE_EXTENSION");
                        int damageLevel = context.getModuleLevel("DAMAGE_BOOST");

                        double radius = 2.0 + rangeLevel * 0.5;
                        float aoeDamage = 2.0f + (voidLevel + damageLevel) * 0.5f;

                        // 对目标周围的敌人造成范围伤害
                        AxisAlignedBB area = target.getEntityBoundingBox().grow(radius);
                        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, area,
                                e -> e != player && e != target && !e.isDead && !(e instanceof EntityPlayer));

                        for (EntityLivingBase entity : entities) {
                            entity.attackEntityFrom(DamageSource.causePlayerDamage(player), aoeDamage);

                            // 虚空粒子
                            for (int i = 0; i < 5; i++) {
                                world.spawnParticle(EnumParticleTypes.PORTAL,
                                        entity.posX + (world.rand.nextDouble() - 0.5) * entity.width,
                                        entity.posY + world.rand.nextDouble() * entity.height,
                                        entity.posZ + (world.rand.nextDouble() - 0.5) * entity.width,
                                        0, 0, 0);
                            }
                        }

                        if (!entities.isEmpty()) {
                            world.playSound(null, player.posX, player.posY, player.posZ,
                                    SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 0.5f, 0.5f);
                        }

                        // 消耗能量
                        ExistingModuleBridge.getInstance().consumeEnergy(player, 50);
                    }

                    @Override
                    public String getDescription() {
                        return "AOE damage on attack";
                    }
                })
                .addEffect(MessageEffect.actionBar("🌀 虚空收割!", TextFormatting.DARK_PURPLE))

                .priority(35)
                .build();
    }

    /**
     * 速度恶魔 Synergy
     * 需要: SPEED_BOOST + MOVEMENT_SPEED + PURSUIT
     * 效果: 移动时叠加速度，达到最大速度时攻击增强
     */
    public static SynergyDefinition createSpeedDemon() {
        return SynergyDefinition.builder("speed_demon")
                .displayName("速度恶魔")
                .description("速度就是一切")

                .requireModules("SPEED_BOOST", "MOVEMENT_SPEED", "PURSUIT")
                .addLink("SPEED_BOOST", "MOVEMENT_SPEED", "triangle")
                .addLink("MOVEMENT_SPEED", "PURSUIT", "triangle")
                .addLink("PURSUIT", "SPEED_BOOST", "triangle")

                .triggerOn(SynergyEventType.TICK, SynergyEventType.ATTACK)

                .addEffect(new ISynergyEffect() {
                    private int speedStacks = 0;

                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;

                        int speedLevel = context.getModuleLevel("SPEED_BOOST");
                        int moveLevel = context.getModuleLevel("MOVEMENT_SPEED");
                        int pursuitLevel = context.getModuleLevel("PURSUIT");

                        int maxStacks = 5 + (speedLevel + moveLevel + pursuitLevel) / 2;

                        if (context.getEventType() == SynergyEventType.TICK) {
                            // 移动时叠加速度层数
                            double speed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);

                            if (speed > 0.2 && player.ticksExisted % 10 == 0) {
                                speedStacks = Math.min(speedStacks + 1, maxStacks);

                                // 速度加成
                                int amplifier = Math.min(2, speedStacks / 3);
                                player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 30, amplifier, false, false));

                                // 速度粒子
                                if (speedStacks >= 3) {
                                    world.spawnParticle(EnumParticleTypes.CLOUD,
                                            player.posX - player.motionX,
                                            player.posY + 0.5,
                                            player.posZ - player.motionZ,
                                            0, 0, 0);
                                }
                            } else if (speed < 0.1 && player.ticksExisted % 20 == 0) {
                                // 静止时减少层数
                                speedStacks = Math.max(0, speedStacks - 1);
                            }

                        } else if (context.getEventType() == SynergyEventType.ATTACK) {
                            // 高速度层数时攻击增强
                            if (speedStacks >= 3) {
                                EntityLivingBase target = context.getTarget();
                                if (target != null) {
                                    float bonusDamage = context.getOriginalDamage() * 0.1f * speedStacks;
                                    target.attackEntityFrom(DamageSource.causePlayerDamage(player), bonusDamage);

                                    world.playSound(null, player.posX, player.posY, player.posZ,
                                            SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.5f, 1.5f);
                                }

                                // 攻击后消耗部分层数
                                speedStacks = speedStacks / 2;
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Stack speed while moving, bonus damage at max speed";
                    }
                })

                .priority(45)
                .build();
    }
}
