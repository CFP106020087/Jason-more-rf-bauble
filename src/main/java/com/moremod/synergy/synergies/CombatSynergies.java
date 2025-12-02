package com.moremod.synergy.synergies;

import com.moremod.util.combat.TrueDamageHelper;
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
 * 战斗类 Synergy 定义 - 中期质变核心
 *
 * 设计理念：高能量成本换取质变效果，而非简单数值叠加
 *
 * 1. Berserker (狂战士) - 血怒形态，真伤吸血，无视护甲
 * 2. Hunter (追猎者) - 猎物标记，必定暴击，击杀重置
 * 3. Iron Wall (铁壁) - 伤害阈值，重击减免，反伤护盾
 */
public class CombatSynergies {

    public static void registerAll(SynergyManager manager) {
        manager.register(createBerserker());
        manager.register(createHunter());
        manager.register(createIronWall());

        System.out.println("[Synergy] Registered 3 Combat Synergies (Enhanced)");
    }

    /**
     * 狂战士 Synergy - 血怒形态
     * 需要: DAMAGE_BOOST + THORNS + HEALTH_REGEN
     *
     * 质变效果:
     * - 30%血量以下进入【血怒形态】
     * - 血怒时：攻击造成额外真伤 (30% + 等级*5%)
     * - 血怒时：真伤的50%转化为生命吸取
     * - 血怒时：免疫击退，攻速提升
     * - 受伤时：对攻击者造成等额真伤反弹 (等级*10%)
     */
    public static SynergyDefinition createBerserker() {
        return SynergyDefinition.builder("berserker")
                .displayName("狂战士")
                .description("血怒形态：真伤吸血，战意不灭")
                .category("combat")

                .requireModules("DAMAGE_BOOST", "THORNS", "HEALTH_REGEN")
                .addLink("DAMAGE_BOOST", "THORNS", "triangle")
                .addLink("THORNS", "HEALTH_REGEN", "triangle")
                .addLink("HEALTH_REGEN", "DAMAGE_BOOST", "triangle")

                .triggerOn(SynergyEventType.ATTACK, SynergyEventType.HURT, SynergyEventType.TICK)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;

                        float healthPercent = player.getHealth() / player.getMaxHealth();
                        int damageLevel = context.getModuleLevel("DAMAGE_BOOST");
                        int thornsLevel = context.getModuleLevel("THORNS");
                        int regenLevel = context.getModuleLevel("HEALTH_REGEN");
                        int totalLevel = damageLevel + thornsLevel + regenLevel;

                        // 血怒阈值: 30%
                        boolean isRaging = healthPercent < 0.30f;

                        if (context.getEventType() == SynergyEventType.TICK) {
                            // 血怒状态维护
                            if (isRaging) {
                                // 标记血怒状态
                                player.getEntityData().setBoolean("synergy_berserker_rage", true);
                                player.getEntityData().setLong("synergy_berserker_rage_time", world.getTotalWorldTime());

                                // 血怒视觉效果 (每秒)
                                if (world.getTotalWorldTime() % 20 == 0) {
                                    for (int i = 0; i < 8; i++) {
                                        double angle = (i / 8.0) * Math.PI * 2;
                                        world.spawnParticle(EnumParticleTypes.REDSTONE,
                                                player.posX + Math.cos(angle) * 0.6,
                                                player.posY + 1.0,
                                                player.posZ + Math.sin(angle) * 0.6,
                                                1, 0, 0);
                                    }
                                }

                                // 血怒状态免疫击退 (通过给予短暂抗性)
                                if (!player.isPotionActive(MobEffects.RESISTANCE)) {
                                    // 不给药水，而是通过NBT标记在受伤事件中处理
                                }
                            } else {
                                player.getEntityData().setBoolean("synergy_berserker_rage", false);
                            }
                            return;
                        }

                        if (context.getEventType() == SynergyEventType.ATTACK) {
                            EntityLivingBase target = context.getTarget();
                            if (target == null) return;

                            // 检查是否在血怒状态
                            boolean raging = player.getEntityData().getBoolean("synergy_berserker_rage");

                            if (raging) {
                                // 血怒攻击：造成额外真伤
                                float trueDamagePercent = 0.30f + damageLevel * 0.05f;
                                float trueDamage = context.getOriginalDamage() * trueDamagePercent;

                                if (trueDamage > 0.5f) {
                                    TrueDamageHelper.applyWrappedTrueDamage(target, player, trueDamage,
                                            TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE);

                                    // 真伤吸血 (50% + regenLevel*5%)
                                    float lifestealPercent = 0.50f + regenLevel * 0.05f;
                                    float healAmount = trueDamage * lifestealPercent;
                                    player.heal(healAmount);

                                    // 血红粒子爆发
                                    for (int i = 0; i < 15; i++) {
                                        world.spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR,
                                                target.posX + (world.rand.nextDouble() - 0.5) * target.width * 2,
                                                target.posY + world.rand.nextDouble() * target.height,
                                                target.posZ + (world.rand.nextDouble() - 0.5) * target.width * 2,
                                                0, 0.15, 0);
                                    }

                                    // 吸血效果
                                    world.spawnParticle(EnumParticleTypes.HEART,
                                            player.posX, player.posY + 2, player.posZ,
                                            0, 0.1, 0);
                                }
                            } else {
                                // 非血怒：普通额外伤害 (血量越低越高)
                                float bonusPercent = (1.0f - healthPercent) * 0.3f * (1 + damageLevel * 0.1f);
                                float bonusDamage = context.getOriginalDamage() * bonusPercent;
                                if (bonusDamage > 0.5f) {
                                    target.attackEntityFrom(DamageSource.causePlayerDamage(player), bonusDamage);
                                }
                            }

                        } else if (context.getEventType() == SynergyEventType.HURT) {
                            // 受伤时：真伤反弹
                            EntityLivingBase attacker = context.getTarget();
                            if (attacker != null && attacker != player) {
                                float reflectPercent = thornsLevel * 0.10f; // 每级10%反弹
                                float reflectDamage = context.getOriginalDamage() * reflectPercent;

                                if (reflectDamage > 0.5f) {
                                    TrueDamageHelper.applyWrappedTrueDamage(attacker, player, reflectDamage,
                                            TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE);

                                    // 反弹粒子
                                    world.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                                            attacker.posX, attacker.posY + 1, attacker.posZ,
                                            0, 0.2, 0);
                                }
                            }

                            // 血怒时受伤恢复少量生命
                            if (isRaging) {
                                float healOnHit = 1.0f + regenLevel * 0.5f;
                                player.heal(healOnHit);
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "30%血量以下进入血怒：真伤攻击+吸血，真伤反弹";
                    }
                })

                .priority(20)
                .build();
    }

    /**
     * 追猎者 Synergy - 猎杀印记
     * 需要: PURSUIT + MOVEMENT_SPEED + ATTACK_SPEED
     *
     * 质变效果:
     * - 攻击敌人标记为【猎物】，持续10秒
     * - 对猎物攻击：必定暴击 (200% + 等级*20% 伤害)
     * - 对猎物攻击：瞬移到猎物身后
     * - 击杀猎物：获得【狩猎祝福】5秒 (移速+50%，下次攻击额外真伤)
     * - 猎物逃跑时：造成额外真伤
     */
    public static SynergyDefinition createHunter() {
        return SynergyDefinition.builder("hunter")
                .displayName("追猎者")
                .description("锁定猎物，必杀一击")
                .category("combat")

                .requireModules("PURSUIT", "MOVEMENT_SPEED", "ATTACK_SPEED")
                .addLink("PURSUIT", "MOVEMENT_SPEED", "chain")
                .addLink("MOVEMENT_SPEED", "ATTACK_SPEED", "chain")

                .triggerOn(SynergyEventType.ATTACK, SynergyEventType.KILL)

                .addCondition(TargetCondition.isNotPlayer())

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;

                        int pursuitLevel = context.getModuleLevel("PURSUIT");
                        int speedLevel = context.getModuleLevel("MOVEMENT_SPEED");
                        int attackSpeedLevel = context.getModuleLevel("ATTACK_SPEED");
                        int totalLevel = pursuitLevel + speedLevel + attackSpeedLevel;

                        if (context.getEventType() == SynergyEventType.ATTACK) {
                            EntityLivingBase target = context.getTarget();
                            if (target == null) return;

                            // 检查目标是否已被标记为猎物
                            boolean isMarkedPrey = target.getEntityData().getBoolean("synergy_hunter_prey");
                            long markTime = target.getEntityData().getLong("synergy_hunter_prey_time");
                            boolean markValid = isMarkedPrey && (world.getTotalWorldTime() - markTime < 200); // 10秒

                            if (markValid) {
                                // === 猎物攻击：质变效果 ===

                                // 1. 必定暴击伤害 (200% + 等级*20%)
                                float critMultiplier = 2.0f + totalLevel * 0.20f;
                                float critDamage = context.getOriginalDamage() * (critMultiplier - 1.0f);

                                // 转化为真伤
                                TrueDamageHelper.applyWrappedTrueDamage(target, player, critDamage,
                                        TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE);

                                // 2. 瞬移到猎物身后
                                double targetYaw = Math.toRadians(target.rotationYaw);
                                double behindX = target.posX + Math.sin(targetYaw) * 1.5;
                                double behindZ = target.posZ - Math.cos(targetYaw) * 1.5;

                                // 检查目标位置是否安全
                                if (!world.isRemote && world.getBlockState(new net.minecraft.util.math.BlockPos(behindX, target.posY, behindZ)).getMaterial().isReplaceable()) {
                                    player.setPositionAndUpdate(behindX, target.posY, behindZ);
                                    player.rotationYaw = target.rotationYaw;

                                    // 瞬移粒子
                                    for (int i = 0; i < 20; i++) {
                                        world.spawnParticle(EnumParticleTypes.PORTAL,
                                                player.posX + (world.rand.nextDouble() - 0.5) * 1.5,
                                                player.posY + world.rand.nextDouble() * 2,
                                                player.posZ + (world.rand.nextDouble() - 0.5) * 1.5,
                                                0, 0.1, 0);
                                    }
                                }

                                // 暴击音效和粒子
                                world.playSound(null, target.posX, target.posY, target.posZ,
                                        SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 1.0f);

                                for (int i = 0; i < 10; i++) {
                                    world.spawnParticle(EnumParticleTypes.CRIT,
                                            target.posX + (world.rand.nextDouble() - 0.5) * target.width,
                                            target.posY + world.rand.nextDouble() * target.height,
                                            target.posZ + (world.rand.nextDouble() - 0.5) * target.width,
                                            0, 0.2, 0);
                                }

                            } else {
                                // === 首次攻击：标记猎物 ===
                                target.getEntityData().setBoolean("synergy_hunter_prey", true);
                                target.getEntityData().setLong("synergy_hunter_prey_time", world.getTotalWorldTime());

                                // 标记粒子
                                for (int i = 0; i < 8; i++) {
                                    world.spawnParticle(EnumParticleTypes.VILLAGER_ANGRY,
                                            target.posX, target.posY + target.height + 0.5, target.posZ,
                                            (world.rand.nextDouble() - 0.5) * 0.3,
                                            0.1,
                                            (world.rand.nextDouble() - 0.5) * 0.3);
                                }

                                world.playSound(null, target.posX, target.posY, target.posZ,
                                        SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.8f, 1.5f);
                            }

                            // 猎物逃跑额外真伤
                            double targetSpeed = Math.sqrt(target.motionX * target.motionX + target.motionZ * target.motionZ);
                            if (targetSpeed > 0.15) {
                                float fleeDamage = context.getOriginalDamage() * 0.3f * pursuitLevel;
                                TrueDamageHelper.applyWrappedTrueDamage(target, player, fleeDamage,
                                        TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE);
                            }

                        } else if (context.getEventType() == SynergyEventType.KILL) {
                            // 击杀猎物：获得狩猎祝福
                            player.getEntityData().setBoolean("synergy_hunter_blessing", true);
                            player.getEntityData().setLong("synergy_hunter_blessing_time", world.getTotalWorldTime());
                            player.getEntityData().setFloat("synergy_hunter_blessing_bonus", totalLevel * 0.5f);

                            // 移速加成 (50% + 等级*10%)
                            int speedDuration = 100; // 5秒
                            int speedAmplifier = 1 + speedLevel / 2;
                            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, speedDuration, speedAmplifier, false, false));

                            // 击杀音效和粒子
                            world.playSound(null, player.posX, player.posY, player.posZ,
                                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.5f);

                            for (int i = 0; i < 25; i++) {
                                world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                                        player.posX + (world.rand.nextDouble() - 0.5) * 2,
                                        player.posY + world.rand.nextDouble() * 2,
                                        player.posZ + (world.rand.nextDouble() - 0.5) * 2,
                                        0, 0.15, 0);
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "标记猎物后必定暴击+瞬移背刺，击杀获得祝福";
                    }
                })

                .priority(30)
                .build();
    }

    /**
     * 铁壁 Synergy - 绝对防御
     * 需要: ARMOR_ENHANCEMENT + SHIELD_GENERATOR + YELLOW_SHIELD
     *
     * 质变效果:
     * - 【伤害阈值】：单次伤害超过最大HP 15%时，减免60%
     * - 【受击护盾】：受伤时生成大量护盾 (等级*2)
     * - 【反伤领域】：护盾存在时，周围敌人持续受到真伤
     * - 【不动如山】：护盾满时免疫击退和控制效果
     */
    public static SynergyDefinition createIronWall() {
        return SynergyDefinition.builder("iron_wall")
                .displayName("铁壁")
                .description("绝对防御：重击减免，反伤领域")
                .category("combat")

                .requireModules("ARMOR_ENHANCEMENT", "SHIELD_GENERATOR", "YELLOW_SHIELD")
                .addLink("ARMOR_ENHANCEMENT", "SHIELD_GENERATOR", "triangle")
                .addLink("SHIELD_GENERATOR", "YELLOW_SHIELD", "triangle")
                .addLink("YELLOW_SHIELD", "ARMOR_ENHANCEMENT", "triangle")

                .triggerOn(SynergyEventType.HURT, SynergyEventType.TICK)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;

                        int armorLevel = context.getModuleLevel("ARMOR_ENHANCEMENT");
                        int shieldGenLevel = context.getModuleLevel("SHIELD_GENERATOR");
                        int yellowShieldLevel = context.getModuleLevel("YELLOW_SHIELD");
                        int totalLevel = armorLevel + shieldGenLevel + yellowShieldLevel;

                        float maxShield = 8.0f + totalLevel * 2.0f;

                        if (context.getEventType() == SynergyEventType.TICK) {
                            // === 反伤领域 (护盾存在时) ===
                            float currentShield = player.getAbsorptionAmount();
                            if (currentShield > 0 && world.getTotalWorldTime() % 20 == 0) {
                                // 每秒对周围敌人造成真伤
                                double range = 3.0 + totalLevel * 0.5;
                                AxisAlignedBB aoe = player.getEntityBoundingBox().grow(range);
                                List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aoe,
                                        e -> e != player && e.isEntityAlive() && !(e instanceof EntityPlayer));

                                float auraDamage = 2.0f + totalLevel * 0.5f;

                                for (EntityLivingBase target : targets) {
                                    TrueDamageHelper.applyWrappedTrueDamage(target, player, auraDamage,
                                            TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE);

                                    // 反伤粒子
                                    world.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                                            target.posX, target.posY + 1, target.posZ,
                                            0, 0.1, 0);
                                }

                                // 领域视觉效果
                                if (!targets.isEmpty()) {
                                    for (int i = 0; i < 12; i++) {
                                        double angle = (i / 12.0) * Math.PI * 2;
                                        world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                                                player.posX + Math.cos(angle) * range,
                                                player.posY + 0.5,
                                                player.posZ + Math.sin(angle) * range,
                                                0, 0.05, 0);
                                    }
                                }
                            }

                            // 护盾满时标记
                            player.getEntityData().setBoolean("synergy_ironwall_full",
                                    currentShield >= maxShield * 0.9f);

                            return;
                        }

                        if (context.getEventType() == SynergyEventType.HURT) {
                            float incomingDamage = context.getOriginalDamage();
                            float maxHP = player.getMaxHealth();

                            // === 伤害阈值系统 ===
                            float damageThreshold = maxHP * 0.15f; // 15% 最大生命值
                            if (incomingDamage > damageThreshold) {
                                // 重击减免 60% + 等级*3%
                                float reduction = 0.60f + totalLevel * 0.03f;
                                float reducedDamage = incomingDamage * (1.0f - reduction);
                                float blocked = incomingDamage - reducedDamage;

                                // 恢复被减免的伤害值 (模拟减伤)
                                player.heal(blocked);

                                // 重击格挡音效
                                world.playSound(null, player.posX, player.posY, player.posZ,
                                        SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 0.5f);

                                // 格挡粒子
                                for (int i = 0; i < 20; i++) {
                                    double angle = (i / 20.0) * Math.PI * 2;
                                    world.spawnParticle(EnumParticleTypes.CRIT,
                                            player.posX + Math.cos(angle) * 1.2,
                                            player.posY + 1,
                                            player.posZ + Math.sin(angle) * 1.2,
                                            0, 0.1, 0);
                                }
                            }

                            // === 受击护盾生成 ===
                            float shieldGain = 2.0f + totalLevel * 1.0f;
                            float currentShield = player.getAbsorptionAmount();
                            float newShield = Math.min(currentShield + shieldGain, maxShield);

                            if (newShield > currentShield) {
                                player.setAbsorptionAmount(newShield);

                                // 护盾生成粒子
                                for (int i = 0; i < 10; i++) {
                                    double angle = world.rand.nextDouble() * Math.PI * 2;
                                    double radius = 0.5 + world.rand.nextDouble() * 0.5;
                                    world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                                            player.posX + Math.cos(angle) * radius,
                                            player.posY + 1 + world.rand.nextDouble(),
                                            player.posZ + Math.sin(angle) * radius,
                                            0, 0.05, 0);
                                }
                            }

                            // === 不动如山 (护盾满时免疫击退) ===
                            if (currentShield >= maxShield * 0.9f) {
                                // 通过重置动量来模拟免疫击退
                                player.motionX = 0;
                                player.motionZ = 0;
                                player.velocityChanged = true;

                                // 清除负面效果
                                player.removePotionEffect(MobEffects.SLOWNESS);
                                player.removePotionEffect(MobEffects.WEAKNESS);

                                // 不动如山粒子
                                world.spawnParticle(EnumParticleTypes.BLOCK_DUST,
                                        player.posX, player.posY, player.posZ,
                                        0.3, 0.1, 0.3);
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "重击减免60%，护盾反伤领域，护盾满免疫击退";
                    }
                })

                .priority(25)
                .build();
    }
}
