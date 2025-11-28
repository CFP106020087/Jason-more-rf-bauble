package com.moremod.synergy.synergies;

import com.moremod.combat.TrueDamageHelper;
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
 * 能量类 Synergy 定义 - 中期质变核心
 *
 * 设计理念：能量系统质变，从被动消耗者变为主动掌控者
 *
 * 1. Energy Overload (能量过载) - 高能态增伤护盾，超载爆发
 * 2. Combat Generator (战斗发电机) - 战斗中大量回能，连杀机制
 * 3. Perpetual Motion (永动机) - 能量自循环，近乎无限续航
 */
public class EnergySynergies {

    public static void registerAll(SynergyManager manager) {
        manager.register(createEnergyOverload());
        manager.register(createCombatGenerator());
        manager.register(createPerpetualMotion());

        System.out.println("[Synergy] Registered 3 Energy Synergies (Enhanced)");
    }

    /**
     * 能量过载 Synergy - 高能态质变
     * 需要: ENERGY_CAPACITY + ENERGY_EFFICIENCY + VOID_ENERGY
     *
     * 质变效果:
     * - 能量>80%进入【高能态】
     * - 高能态：所有攻击附加能量真伤 (当前能量*0.01%)
     * - 高能态：自动能量护盾 (每秒消耗能量生成等额护盾)
     * - 高能态：移速+30%，跳跃高度+50%
     * - 能量100%时可触发【过载爆发】：消耗50%能量，周围真伤AOE
     */
    public static SynergyDefinition createEnergyOverload() {
        return SynergyDefinition.builder("energy_overload")
                .displayName("能量过载")
                .description("高能态：能量护盾，过载爆发")
                .category("energy")

                .requireModules("ENERGY_CAPACITY", "ENERGY_EFFICIENCY", "VOID_ENERGY")
                .addLink("ENERGY_CAPACITY", "ENERGY_EFFICIENCY", "triangle")
                .addLink("ENERGY_EFFICIENCY", "VOID_ENERGY", "triangle")
                .addLink("VOID_ENERGY", "ENERGY_CAPACITY", "triangle")

                .triggerOn(SynergyEventType.TICK, SynergyEventType.ATTACK)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
                        World world = player.world;

                        int capacityLevel = context.getModuleLevel("ENERGY_CAPACITY");
                        int efficiencyLevel = context.getModuleLevel("ENERGY_EFFICIENCY");
                        int voidLevel = context.getModuleLevel("VOID_ENERGY");
                        int totalLevel = capacityLevel + efficiencyLevel + voidLevel;

                        int currentEnergy = bridge.getCurrentEnergy(player);
                        int maxEnergy = bridge.getMaxEnergy(player);
                        float energyPercent = maxEnergy > 0 ? (float) currentEnergy / maxEnergy : 0;

                        // 高能态阈值: 80%
                        boolean isHighEnergy = energyPercent >= 0.80f;

                        if (context.getEventType() == SynergyEventType.TICK) {
                            // === 高能态状态管理 ===
                            player.getEntityData().setBoolean("synergy_overload_high", isHighEnergy);

                            if (isHighEnergy) {
                                // 每秒处理
                                if (world.getTotalWorldTime() % 20 == 0) {
                                    // === 能量护盾生成 ===
                                    // 消耗能量生成护盾 (每秒消耗 100*等级 能量，生成等级*2护盾)
                                    int shieldEnergyCost = 100 * totalLevel;
                                    if (currentEnergy > shieldEnergyCost) {
                                        bridge.consumeEnergy(player, shieldEnergyCost);

                                        float shieldGain = totalLevel * 2.0f;
                                        float maxShield = 10.0f + totalLevel * 3.0f;
                                        float currentShield = player.getAbsorptionAmount();
                                        float newShield = Math.min(currentShield + shieldGain, maxShield);

                                        if (newShield > currentShield) {
                                            player.setAbsorptionAmount(newShield);
                                        }
                                    }

                                    // === 高能态增益 ===
                                    // 移速+30%
                                    if (!player.isPotionActive(MobEffects.SPEED)) {
                                        player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 30, 1, false, false));
                                    }
                                    // 跳跃+50%
                                    if (!player.isPotionActive(MobEffects.JUMP_BOOST)) {
                                        player.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 30, 1, false, false));
                                    }

                                    // 高能粒子效果
                                    for (int i = 0; i < 5; i++) {
                                        double angle = world.rand.nextDouble() * Math.PI * 2;
                                        world.spawnParticle(EnumParticleTypes.END_ROD,
                                                player.posX + Math.cos(angle) * 0.5,
                                                player.posY + 1 + world.rand.nextDouble(),
                                                player.posZ + Math.sin(angle) * 0.5,
                                                0, 0.05, 0);
                                    }
                                }

                                // === 过载爆发 (100%能量 + 蹲下) ===
                                if (energyPercent >= 0.99f && player.isSneaking()) {
                                    long lastOverload = player.getEntityData().getLong("synergy_overload_last");
                                    if (world.getTotalWorldTime() - lastOverload > 200) { // 10秒冷却
                                        // 消耗50%能量
                                        int overloadCost = maxEnergy / 2;
                                        bridge.consumeEnergy(player, overloadCost);

                                        // AOE真伤
                                        double range = 6.0 + totalLevel;
                                        float aoeDamage = 15.0f + totalLevel * 3.0f;
                                        AxisAlignedBB aoe = player.getEntityBoundingBox().grow(range);
                                        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aoe,
                                                e -> e != player && e.isEntityAlive());

                                        for (EntityLivingBase target : targets) {
                                            TrueDamageHelper.applyWrappedTrueDamage(target, player, aoeDamage,
                                                    TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE);
                                        }

                                        player.getEntityData().setLong("synergy_overload_last", world.getTotalWorldTime());

                                        // 过载爆发视觉效果
                                        world.playSound(null, player.posX, player.posY, player.posZ,
                                                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 1.5f);

                                        for (int i = 0; i < 50; i++) {
                                            double angle = world.rand.nextDouble() * Math.PI * 2;
                                            double radius = world.rand.nextDouble() * range;
                                            world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                                                    player.posX + Math.cos(angle) * radius,
                                                    player.posY + world.rand.nextDouble() * 2,
                                                    player.posZ + Math.sin(angle) * radius,
                                                    0, 0.1, 0);
                                        }
                                    }
                                }
                            }
                            return;
                        }

                        if (context.getEventType() == SynergyEventType.ATTACK) {
                            EntityLivingBase target = context.getTarget();
                            if (target == null) return;

                            // === 高能态攻击：能量真伤 ===
                            if (isHighEnergy) {
                                // 真伤 = 当前能量 * 0.01% * 等级系数
                                float energyDamagePercent = 0.0001f * (1 + totalLevel * 0.2f);
                                float energyDamage = currentEnergy * energyDamagePercent;

                                if (energyDamage > 1.0f) {
                                    TrueDamageHelper.applyWrappedTrueDamage(target, player, energyDamage,
                                            TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE);

                                    // 能量攻击粒子
                                    for (int i = 0; i < 8; i++) {
                                        world.spawnParticle(EnumParticleTypes.PORTAL,
                                                target.posX + (world.rand.nextDouble() - 0.5) * target.width,
                                                target.posY + world.rand.nextDouble() * target.height,
                                                target.posZ + (world.rand.nextDouble() - 0.5) * target.width,
                                                0, 0.1, 0);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "80%能量进入高能态：护盾+增益，100%可过载爆发AOE真伤";
                    }
                })

                .priority(40)
                .build();
    }

    /**
     * 战斗发电机 Synergy - 以战养能
     * 需要: COMBAT_CHARGER + DAMAGE_BOOST + ATTACK_SPEED
     *
     * 质变效果:
     * - 攻击回能：基础 200 + 伤害*50% RF
     * - 连击系统：2秒内连续攻击叠加连击层数(最高10层)
     * - 连击奖励：每层+10%回能，10层时额外爆发
     * - 击杀爆发：击杀回复 最大能量*10%，重置连击到满
     * - 暴击触发：暴击时额外真伤 (回复能量*0.1%)
     */
    public static SynergyDefinition createCombatGenerator() {
        return SynergyDefinition.builder("combat_generator")
                .displayName("战斗发电机")
                .description("以战养能：连击回能，击杀爆发")
                .category("energy")

                .requireModules("COMBAT_CHARGER", "DAMAGE_BOOST", "ATTACK_SPEED")
                .addLink("COMBAT_CHARGER", "DAMAGE_BOOST", "chain")
                .addLink("DAMAGE_BOOST", "ATTACK_SPEED", "chain")

                .triggerOn(SynergyEventType.ATTACK, SynergyEventType.KILL)

                .addCondition(TargetCondition.isNotPlayer())

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
                        World world = player.world;

                        int chargerLevel = context.getModuleLevel("COMBAT_CHARGER");
                        int damageLevel = context.getModuleLevel("DAMAGE_BOOST");
                        int speedLevel = context.getModuleLevel("ATTACK_SPEED");
                        int totalLevel = chargerLevel + damageLevel + speedLevel;

                        if (context.getEventType() == SynergyEventType.ATTACK) {
                            EntityLivingBase target = context.getTarget();
                            if (target == null) return;

                            long currentTime = world.getTotalWorldTime();
                            long lastAttackTime = player.getEntityData().getLong("synergy_combatgen_last");
                            int comboCount = player.getEntityData().getInteger("synergy_combatgen_combo");

                            // === 连击系统 ===
                            if (currentTime - lastAttackTime < 40) { // 2秒内
                                comboCount = Math.min(comboCount + 1, 10);
                            } else {
                                comboCount = 1;
                            }

                            player.getEntityData().setLong("synergy_combatgen_last", currentTime);
                            player.getEntityData().setInteger("synergy_combatgen_combo", comboCount);

                            // === 攻击回能 ===
                            // 基础200 + 伤害*50 + 等级加成
                            int baseEnergy = 200 + (int) (context.getOriginalDamage() * 50);
                            // 连击加成：每层+10%
                            float comboMultiplier = 1.0f + comboCount * 0.10f;
                            int totalEnergy = (int) (baseEnergy * comboMultiplier * (1 + totalLevel * 0.1f));

                            bridge.addEnergy(player, totalEnergy);

                            // === 10层连击爆发 ===
                            if (comboCount >= 10) {
                                // 额外真伤 (回复能量的0.5%)
                                float burstDamage = totalEnergy * 0.005f;
                                if (burstDamage > 1.0f) {
                                    TrueDamageHelper.applyWrappedTrueDamage(target, player, burstDamage,
                                            TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE);
                                }

                                // 连击满粒子
                                for (int i = 0; i < 15; i++) {
                                    world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                                            player.posX + (world.rand.nextDouble() - 0.5) * 1.5,
                                            player.posY + world.rand.nextDouble() * 2,
                                            player.posZ + (world.rand.nextDouble() - 0.5) * 1.5,
                                            0, 0.1, 0);
                                }

                                world.playSound(null, player.posX, player.posY, player.posZ,
                                        SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.3f, 2.0f);
                            }

                            // 连击粒子
                            if (comboCount >= 3) {
                                int particleCount = Math.min(comboCount, 8);
                                for (int i = 0; i < particleCount; i++) {
                                    world.spawnParticle(EnumParticleTypes.SPELL_INSTANT,
                                            player.posX + (world.rand.nextDouble() - 0.5),
                                            player.posY + 1 + world.rand.nextDouble(),
                                            player.posZ + (world.rand.nextDouble() - 0.5),
                                            0, 0.05, 0);
                                }
                            }

                        } else if (context.getEventType() == SynergyEventType.KILL) {
                            // === 击杀爆发 ===
                            int maxEnergy = bridge.getMaxEnergy(player);
                            int killEnergy = (int) (maxEnergy * 0.10f * (1 + totalLevel * 0.05f)); // 10% + 等级加成
                            bridge.addEnergy(player, killEnergy);

                            // 重置连击到满
                            player.getEntityData().setInteger("synergy_combatgen_combo", 10);
                            player.getEntityData().setLong("synergy_combatgen_last", world.getTotalWorldTime());

                            // 击杀爆发效果
                            world.playSound(null, player.posX, player.posY, player.posZ,
                                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.2f);

                            for (int i = 0; i < 20; i++) {
                                double angle = world.rand.nextDouble() * Math.PI * 2;
                                world.spawnParticle(EnumParticleTypes.TOTEM,
                                        player.posX + Math.cos(angle) * 0.8,
                                        player.posY + 1 + world.rand.nextDouble(),
                                        player.posZ + Math.sin(angle) * 0.8,
                                        (world.rand.nextDouble() - 0.5) * 0.2,
                                        0.2,
                                        (world.rand.nextDouble() - 0.5) * 0.2);
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "攻击回能200+伤害*50RF，连击叠加最高+100%，击杀回10%最大能量";
                    }
                })

                .priority(35)
                .build();
    }

    /**
     * 永动机 Synergy - 能量永续
     * 需要: KINETIC_GENERATOR + SOLAR_GENERATOR + ENERGY_EFFICIENCY
     *
     * 质变效果:
     * - 基础被动发电提升300%
     * - 移动时：每格发电量翻倍
     * - 白天+露天：太阳能发电翻倍
     * - 能量循环：所有模块能量消耗减少50%
     * - 永动护体：能量>50%时，受伤消耗能量代替生命(1000RF=1HP)
     */
    public static SynergyDefinition createPerpetualMotion() {
        return SynergyDefinition.builder("perpetual_motion")
                .displayName("永动机")
                .description("能量永续：发电翻倍，消耗减半")
                .category("energy")

                .requireModules("KINETIC_GENERATOR", "SOLAR_GENERATOR", "ENERGY_EFFICIENCY")
                .addLink("KINETIC_GENERATOR", "SOLAR_GENERATOR", "triangle")
                .addLink("SOLAR_GENERATOR", "ENERGY_EFFICIENCY", "triangle")
                .addLink("ENERGY_EFFICIENCY", "KINETIC_GENERATOR", "triangle")

                .triggerOn(SynergyEventType.TICK, SynergyEventType.HURT)

                .addEffect(new ISynergyEffect() {
                    private double lastPosX = 0, lastPosZ = 0;

                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
                        World world = player.world;

                        int kineticLevel = context.getModuleLevel("KINETIC_GENERATOR");
                        int solarLevel = context.getModuleLevel("SOLAR_GENERATOR");
                        int efficiencyLevel = context.getModuleLevel("ENERGY_EFFICIENCY");
                        int totalLevel = kineticLevel + solarLevel + efficiencyLevel;

                        if (context.getEventType() == SynergyEventType.TICK) {
                            // 只在每秒处理
                            if (world.getTotalWorldTime() % 20 != 0) return;

                            int totalEnergy = 0;

                            // === 基础发电 (300% 提升) ===
                            int baseEnergy = 50 * totalLevel; // 基础 50*等级/秒
                            totalEnergy += baseEnergy;

                            // === 移动发电 (翻倍) ===
                            double dx = player.posX - lastPosX;
                            double dz = player.posZ - lastPosZ;
                            double distance = Math.sqrt(dx * dx + dz * dz);

                            if (distance > 0.1) {
                                int kineticEnergy = (int) (distance * 100 * kineticLevel);
                                totalEnergy += kineticEnergy * 2; // 翻倍
                            }

                            lastPosX = player.posX;
                            lastPosZ = player.posZ;

                            // === 太阳能发电 (翻倍) ===
                            if (world.isDaytime() && world.canSeeSky(player.getPosition())) {
                                int solarEnergy = 80 * solarLevel;
                                totalEnergy += solarEnergy * 2; // 翻倍
                            }

                            // === 效率加成 ===
                            float efficiencyBonus = 1.0f + efficiencyLevel * 0.20f;
                            totalEnergy = (int) (totalEnergy * efficiencyBonus);

                            bridge.addEnergy(player, totalEnergy);

                            // 标记永动机状态 (用于其他系统减少消耗)
                            player.getEntityData().setBoolean("synergy_perpetual_active", true);
                            player.getEntityData().setFloat("synergy_perpetual_reduction", 0.50f); // 50%减耗

                            // 永动机粒子
                            if (world.getTotalWorldTime() % 40 == 0) {
                                for (int i = 0; i < 3; i++) {
                                    double angle = (world.getTotalWorldTime() / 10.0 + i * 2.1) % (Math.PI * 2);
                                    world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                                            player.posX + Math.cos(angle) * 0.6,
                                            player.posY + 1 + Math.sin(world.getTotalWorldTime() / 5.0) * 0.3,
                                            player.posZ + Math.sin(angle) * 0.6,
                                            0, 0.02, 0);
                                }
                            }

                            return;
                        }

                        if (context.getEventType() == SynergyEventType.HURT) {
                            // === 永动护体：能量代替生命 ===
                            int currentEnergy = bridge.getCurrentEnergy(player);
                            int maxEnergy = bridge.getMaxEnergy(player);

                            if (maxEnergy > 0 && (float) currentEnergy / maxEnergy > 0.50f) {
                                float incomingDamage = context.getOriginalDamage();

                                // 1000 RF = 1 HP
                                int energyCost = (int) (incomingDamage * 1000);
                                int availableEnergy = currentEnergy - (maxEnergy / 2); // 只使用超过50%的部分

                                if (availableEnergy > 0) {
                                    int actualCost = Math.min(energyCost, availableEnergy);
                                    float damageBlocked = actualCost / 1000.0f;

                                    bridge.consumeEnergy(player, actualCost);
                                    player.heal(damageBlocked); // 恢复被能量吸收的伤害

                                    // 护体粒子
                                    for (int i = 0; i < 10; i++) {
                                        double angle = world.rand.nextDouble() * Math.PI * 2;
                                        world.spawnParticle(EnumParticleTypes.END_ROD,
                                                player.posX + Math.cos(angle) * 0.8,
                                                player.posY + 1,
                                                player.posZ + Math.sin(angle) * 0.8,
                                                0, 0.1, 0);
                                    }

                                    world.playSound(null, player.posX, player.posY, player.posZ,
                                            SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.3f, 1.5f);
                                }
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "发电300%+移动/太阳能翻倍，消耗-50%，能量护体";
                    }
                })

                .priority(50)
                .build();
    }
}
