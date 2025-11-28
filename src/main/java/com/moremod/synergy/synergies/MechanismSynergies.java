package com.moremod.synergy.synergies;

import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.synergy.bridge.ExistingModuleBridge;
import com.moremod.synergy.condition.*;
import com.moremod.synergy.core.*;
import com.moremod.synergy.effect.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 机制类 Synergy 定义 - 独特的游戏机制
 *
 * 设计原则：
 * - 不使用药水效果
 * - 不使用简单属性修改
 * - 使用 Event/NBT/自定义数值 实现有趣机制
 *
 * 1. Momentum Engine (动能引擎) - 动能积累释放
 * 2. Time Fracture (时间裂隙) - 延迟回响伤害
 * 3. Gravity Well (重力井) - 引力场控制
 * 4. Soul Harvest (灵魂收割) - 击杀层数斩杀
 * 5. Phase Shift (相位偏移) - 致命伤害闪避
 * 6. Chain Lightning (连锁闪电) - 伤害链式传递
 */
public class MechanismSynergies {

    // 用于存储玩家的延迟伤害数据
    private static final Map<UUID, DelayedDamageTracker> delayedDamageMap = new HashMap<>();

    public static void registerAll(SynergyManager manager) {
        manager.register(createMomentumEngine());
        manager.register(createTimeFracture());
        manager.register(createGravityWell());
        manager.register(createSoulHarvest());
        manager.register(createPhaseShift());
        manager.register(createChainLightning());

        System.out.println("[Synergy] Registered 6 Mechanism Synergies");
    }

    /**
     * 动能引擎 Synergy
     * 需要: KINETIC_GENERATOR + SPEED_BOOST + MOVEMENT_SPEED
     *
     * 机制:
     * - 移动时积累动能点数（基于速度）
     * - 动能上限 = 100 + totalLevel * 20
     * - 攻击时消耗动能，每点动能 = 0.1 额外伤害
     * - 动能达到上限时，下次攻击触发"动能爆发"：
     *   - 造成积累动能 * 0.5 的范围伤害
     *   - 击退周围敌人
     *   - 消耗全部动能
     */
    public static SynergyDefinition createMomentumEngine() {
        return SynergyDefinition.builder("momentum_engine")
                .displayName("动能引擎")
                .description("积蓄移动的力量，释放毁灭一击")

                .requireModules("KINETIC_GENERATOR", "SPEED_BOOST", "MOVEMENT_SPEED")
                .addLink("KINETIC_GENERATOR", "SPEED_BOOST", "gear")
                .addLink("SPEED_BOOST", "MOVEMENT_SPEED", "gear")
                .addLink("MOVEMENT_SPEED", "KINETIC_GENERATOR", "gear")

                .triggerOn(SynergyEventType.TICK, SynergyEventType.ATTACK)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;
                        NBTTagCompound data = player.getEntityData();

                        int kineticLevel = context.getModuleLevel("KINETIC_GENERATOR");
                        int speedBoostLevel = context.getModuleLevel("SPEED_BOOST");
                        int moveSpeedLevel = context.getModuleLevel("MOVEMENT_SPEED");
                        int totalLevel = kineticLevel + speedBoostLevel + moveSpeedLevel;

                        int maxMomentum = 100 + totalLevel * 20;
                        int currentMomentum = data.getInteger("synergy_momentum");

                        if (context.getEventType() == SynergyEventType.TICK) {
                            // 每tick基于移动速度积累动能
                            double speed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);

                            if (speed > 0.1) {
                                // 速度越快，积累越快
                                int gain = (int) (speed * 10 * (1 + kineticLevel * 0.2));
                                currentMomentum = Math.min(currentMomentum + gain, maxMomentum);
                                data.setInteger("synergy_momentum", currentMomentum);

                                // 高动能时产生粒子
                                if (currentMomentum > maxMomentum * 0.8 && player.ticksExisted % 5 == 0) {
                                    world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                                            player.posX, player.posY + 0.5, player.posZ,
                                            (world.rand.nextDouble() - 0.5) * 0.3,
                                            0.1,
                                            (world.rand.nextDouble() - 0.5) * 0.3);
                                }
                            }

                            // 动能自然衰减（静止时）
                            if (speed < 0.05 && currentMomentum > 0 && player.ticksExisted % 20 == 0) {
                                data.setInteger("synergy_momentum", Math.max(0, currentMomentum - 5));
                            }

                        } else if (context.getEventType() == SynergyEventType.ATTACK) {
                            EntityLivingBase target = context.getTarget();
                            if (target == null || currentMomentum <= 0) return;

                            // 检查是否达到满动能
                            boolean isBurst = currentMomentum >= maxMomentum;

                            if (isBurst) {
                                // 动能爆发！
                                float burstDamage = currentMomentum * 0.5f;

                                // 范围伤害
                                AxisAlignedBB area = player.getEntityBoundingBox().grow(4.0);
                                List<EntityLivingBase> nearby = world.getEntitiesWithinAABB(
                                        EntityLivingBase.class, area,
                                        e -> e != player && e instanceof IMob && e.isEntityAlive());

                                for (EntityLivingBase entity : nearby) {
                                    entity.attackEntityFrom(
                                            SynergyEventHandler.causeSynergyDamage(player),
                                            burstDamage * 0.7f);

                                    // 击退
                                    Vec3d knockback = entity.getPositionVector()
                                            .subtract(player.getPositionVector())
                                            .normalize()
                                            .scale(2.0);
                                    entity.motionX += knockback.x;
                                    entity.motionY += 0.5;
                                    entity.motionZ += knockback.z;
                                    entity.velocityChanged = true;
                                }

                                // 主目标全额伤害
                                target.attackEntityFrom(
                                        SynergyEventHandler.causeSynergyDamage(player),
                                        burstDamage);

                                // 爆发特效
                                for (int i = 0; i < 40; i++) {
                                    double angle = (i / 40.0) * Math.PI * 2;
                                    double radius = 3.0;
                                    world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                                            player.posX + Math.cos(angle) * radius,
                                            player.posY + 1,
                                            player.posZ + Math.sin(angle) * radius,
                                            Math.cos(angle) * 0.3, 0.1, Math.sin(angle) * 0.3);
                                }

                                world.playSound(null, player.posX, player.posY, player.posZ,
                                        SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 1.5f);

                                // 清空动能
                                data.setInteger("synergy_momentum", 0);

                            } else {
                                // 普通攻击消耗动能增伤
                                float bonusDamage = currentMomentum * 0.1f;
                                target.attackEntityFrom(
                                        SynergyEventHandler.causeSynergyDamage(player),
                                        bonusDamage);

                                // 消耗 30% 动能
                                data.setInteger("synergy_momentum", (int)(currentMomentum * 0.7));

                                // 小粒子
                                world.spawnParticle(EnumParticleTypes.CRIT,
                                        target.posX, target.posY + 1, target.posZ,
                                        0, 0.2, 0);
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Accumulate momentum while moving, release on attack";
                    }
                })
                .addEffect(EnergyEffect.consume(10))

                .priority(35)
                .build();
    }

    /**
     * 时间裂隙 Synergy
     * 需要: VOID_ENERGY + ATTACK_SPEED + ENERGY_EFFICIENCY
     *
     * 机制:
     * - 攻击造成的伤害会产生"时间回响"
     * - 回响在 0.5/1.0/1.5 秒后再次造成 30%/25%/20% 伤害
     * - 回响最多叠加 5 层
     * - 每层回响有独立的延迟
     */
    public static SynergyDefinition createTimeFracture() {
        return SynergyDefinition.builder("time_fracture")
                .displayName("时间裂隙")
                .description("撕裂时间，让伤害回响")

                .requireModules("VOID_ENERGY", "ATTACK_SPEED", "ENERGY_EFFICIENCY")
                .addLink("VOID_ENERGY", "ATTACK_SPEED", "temporal")
                .addLink("ATTACK_SPEED", "ENERGY_EFFICIENCY", "temporal")
                .addLink("ENERGY_EFFICIENCY", "VOID_ENERGY", "temporal")

                .triggerOn(SynergyEventType.ATTACK, SynergyEventType.TICK)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;

                        int voidLevel = context.getModuleLevel("VOID_ENERGY");
                        int attackSpeedLevel = context.getModuleLevel("ATTACK_SPEED");
                        int efficiencyLevel = context.getModuleLevel("ENERGY_EFFICIENCY");
                        int totalLevel = voidLevel + attackSpeedLevel + efficiencyLevel;

                        UUID playerUUID = player.getUniqueID();
                        DelayedDamageTracker tracker = delayedDamageMap.computeIfAbsent(
                                playerUUID, k -> new DelayedDamageTracker());

                        if (context.getEventType() == SynergyEventType.ATTACK) {
                            EntityLivingBase target = context.getTarget();
                            if (target == null) return;

                            float baseDamage = context.getOriginalDamage();
                            long currentTime = world.getTotalWorldTime();

                            // 创建3层时间回响
                            float[] damageRatios = {0.30f, 0.25f, 0.20f};
                            int[] delays = {10, 20, 30}; // 0.5s, 1s, 1.5s

                            for (int i = 0; i < 3; i++) {
                                float echoDamage = baseDamage * damageRatios[i] * (1 + totalLevel * 0.1f);
                                long triggerTime = currentTime + delays[i];

                                tracker.addEcho(target.getEntityId(), echoDamage, triggerTime, player);
                            }

                            // 时间裂隙视觉效果
                            for (int i = 0; i < 8; i++) {
                                world.spawnParticle(EnumParticleTypes.PORTAL,
                                        target.posX + (world.rand.nextDouble() - 0.5) * 1.5,
                                        target.posY + world.rand.nextDouble() * 2,
                                        target.posZ + (world.rand.nextDouble() - 0.5) * 1.5,
                                        0, -0.1, 0);
                            }

                            world.playSound(null, target.posX, target.posY, target.posZ,
                                    SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.PLAYERS, 0.3f, 2.0f);

                        } else if (context.getEventType() == SynergyEventType.TICK) {
                            // 处理延迟伤害
                            long currentTime = world.getTotalWorldTime();
                            tracker.processEchoes(world, currentTime);
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Attacks create delayed damage echoes";
                    }
                })
                .addEffect(EnergyEffect.consume(15))

                .priority(40)
                .build();
    }

    /**
     * 重力井 Synergy
     * 需要: RANGE_EXTENSION + ARMOR_ENHANCEMENT + VOID_ENERGY
     *
     * 机制:
     * - 蹲下时激活重力场（范围 = 5 + totalLevel 格）
     * - 范围内敌人被缓慢拉向玩家
     * - 敌人在场内每秒受到 1 + voidLevel * 0.5 真伤
     * - 玩家免疫击退
     * - 消耗能量维持
     */
    public static SynergyDefinition createGravityWell() {
        return SynergyDefinition.builder("gravity_well")
                .displayName("重力井")
                .description("扭曲空间，万物归一")

                .requireModules("RANGE_EXTENSION", "ARMOR_ENHANCEMENT", "VOID_ENERGY")
                .addLink("RANGE_EXTENSION", "ARMOR_ENHANCEMENT", "gravity")
                .addLink("ARMOR_ENHANCEMENT", "VOID_ENERGY", "gravity")
                .addLink("VOID_ENERGY", "RANGE_EXTENSION", "gravity")

                .triggerOn(SynergyEventType.TICK, SynergyEventType.HURT)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;

                        int rangeLevel = context.getModuleLevel("RANGE_EXTENSION");
                        int armorLevel = context.getModuleLevel("ARMOR_ENHANCEMENT");
                        int voidLevel = context.getModuleLevel("VOID_ENERGY");
                        int totalLevel = rangeLevel + armorLevel + voidLevel;

                        if (context.getEventType() == SynergyEventType.HURT) {
                            // 激活重力井时免疫击退
                            if (player.isSneaking()) {
                                player.motionX = 0;
                                player.motionZ = 0;
                                player.velocityChanged = true;
                            }
                            return;
                        }

                        // TICK事件 - 只在蹲下时激活
                        if (!player.isSneaking()) return;

                        // 检查能量
                        if (!ExistingModuleBridge.getInstance().hasEnergy(player, 5)) return;

                        double range = 5.0 + totalLevel;
                        AxisAlignedBB area = player.getEntityBoundingBox().grow(range);

                        List<EntityLivingBase> enemies = world.getEntitiesWithinAABB(
                                EntityLivingBase.class, area,
                                e -> e != player && e instanceof IMob && e.isEntityAlive());

                        if (enemies.isEmpty()) return;

                        // 消耗能量
                        ExistingModuleBridge.getInstance().consumeEnergy(player, 5);

                        for (EntityLivingBase enemy : enemies) {
                            // 计算拉力
                            Vec3d pull = player.getPositionVector()
                                    .subtract(enemy.getPositionVector());
                            double distance = pull.length();

                            if (distance > 1.5) {
                                // 拉向玩家（近处拉力更强）
                                double pullStrength = 0.08 * (1 - distance / range);
                                Vec3d pullVec = pull.normalize().scale(pullStrength);

                                enemy.motionX += pullVec.x;
                                enemy.motionZ += pullVec.z;
                                enemy.velocityChanged = true;
                            }

                            // 每秒造成真伤
                            if (player.ticksExisted % 20 == 0) {
                                float damage = 1.0f + voidLevel * 0.5f;
                                enemy.attackEntityFrom(
                                        SynergyEventHandler.causeSynergyDamage(player).setDamageBypassesArmor(),
                                        damage);
                            }

                            // 引力粒子
                            if (player.ticksExisted % 5 == 0) {
                                Vec3d particleDir = pull.normalize().scale(0.2);
                                world.spawnParticle(EnumParticleTypes.PORTAL,
                                        enemy.posX, enemy.posY + 1, enemy.posZ,
                                        particleDir.x, particleDir.y, particleDir.z);
                            }
                        }

                        // 重力井中心粒子
                        if (player.ticksExisted % 10 == 0) {
                            for (int i = 0; i < 16; i++) {
                                double angle = (i / 16.0) * Math.PI * 2;
                                double r = range * 0.8;
                                world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                                        player.posX + Math.cos(angle) * r,
                                        player.posY + 0.5,
                                        player.posZ + Math.sin(angle) * r,
                                        -Math.cos(angle) * 0.1, 0, -Math.sin(angle) * 0.1);
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Crouch to create gravity field pulling enemies";
                    }
                })

                .priority(45)
                .build();
    }

    /**
     * 灵魂收割 Synergy
     * 需要: EXPERIENCE_MULTIPLIER + DAMAGE_BOOST + COMBAT_CHARGER
     *
     * 机制:
     * - 击杀敌人获得灵魂层数（存NBT）
     * - 层数上限 = 10 + totalLevel * 2
     * - 每层增加 2% 伤害
     * - 达到上限时，下次攻击对 <30% HP 目标直接斩杀
     * - 层数每 10 秒衰减 1 层（如果没有击杀）
     */
    public static SynergyDefinition createSoulHarvest() {
        return SynergyDefinition.builder("soul_harvest")
                .displayName("灵魂收割")
                .description("收割灵魂，积蓄死亡")

                .requireModules("EXPERIENCE_MULTIPLIER", "DAMAGE_BOOST", "COMBAT_CHARGER")
                .addLink("EXPERIENCE_MULTIPLIER", "DAMAGE_BOOST", "soul")
                .addLink("DAMAGE_BOOST", "COMBAT_CHARGER", "soul")
                .addLink("COMBAT_CHARGER", "EXPERIENCE_MULTIPLIER", "soul")

                .triggerOn(SynergyEventType.KILL, SynergyEventType.ATTACK, SynergyEventType.TICK)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;
                        NBTTagCompound data = player.getEntityData();

                        int expLevel = context.getModuleLevel("EXPERIENCE_MULTIPLIER");
                        int damageLevel = context.getModuleLevel("DAMAGE_BOOST");
                        int chargerLevel = context.getModuleLevel("COMBAT_CHARGER");
                        int totalLevel = expLevel + damageLevel + chargerLevel;

                        int maxSouls = 10 + totalLevel * 2;
                        int currentSouls = data.getInteger("synergy_soul_stacks");
                        long lastKillTime = data.getLong("synergy_soul_last_kill");
                        long currentTime = world.getTotalWorldTime();

                        if (context.getEventType() == SynergyEventType.KILL) {
                            // 击杀获得灵魂
                            int soulGain = 1 + expLevel / 2;
                            currentSouls = Math.min(currentSouls + soulGain, maxSouls);
                            data.setInteger("synergy_soul_stacks", currentSouls);
                            data.setLong("synergy_soul_last_kill", currentTime);

                            // 灵魂收集特效
                            EntityLivingBase target = context.getTarget();
                            if (target != null) {
                                for (int i = 0; i < 10; i++) {
                                    world.spawnParticle(EnumParticleTypes.SPELL_MOB,
                                            target.posX + (world.rand.nextDouble() - 0.5),
                                            target.posY + 1 + world.rand.nextDouble(),
                                            target.posZ + (world.rand.nextDouble() - 0.5),
                                            0.2, 0.8, 0.2); // 绿色
                                }
                            }

                            // 满层提示
                            if (currentSouls >= maxSouls) {
                                world.playSound(null, player.posX, player.posY, player.posZ,
                                        SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.3f, 2.0f);
                            }

                            // 能量回复
                            ExistingModuleBridge.getInstance().addEnergy(player, 50 + chargerLevel * 20);

                        } else if (context.getEventType() == SynergyEventType.ATTACK) {
                            EntityLivingBase target = context.getTarget();
                            if (target == null || currentSouls <= 0) return;

                            // 基础伤害加成（每层2%）
                            float bonusDamage = context.getOriginalDamage() * (currentSouls * 0.02f);
                            target.attackEntityFrom(
                                    SynergyEventHandler.causeSynergyDamage(player),
                                    bonusDamage);

                            // 满层斩杀判定
                            if (currentSouls >= maxSouls) {
                                float targetHealthRatio = target.getHealth() / target.getMaxHealth();
                                float executeThreshold = 0.3f + damageLevel * 0.02f;

                                if (targetHealthRatio <= executeThreshold) {
                                    // 斩杀！
                                    target.attackEntityFrom(
                                            SynergyEventHandler.causeSynergyDamage(player)
                                                    .setDamageBypassesArmor(),
                                            target.getHealth() + 100);

                                    // 斩杀特效
                                    world.playSound(null, target.posX, target.posY, target.posZ,
                                            SoundEvents.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 1.0f, 0.5f);

                                    for (int i = 0; i < 30; i++) {
                                        world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                                                target.posX + (world.rand.nextDouble() - 0.5) * 2,
                                                target.posY + world.rand.nextDouble() * 2,
                                                target.posZ + (world.rand.nextDouble() - 0.5) * 2,
                                                0, 0.3, 0);
                                    }

                                    // 斩杀后重置层数
                                    data.setInteger("synergy_soul_stacks", 0);
                                }
                            }

                        } else if (context.getEventType() == SynergyEventType.TICK) {
                            // 层数衰减（10秒没击杀衰减1层）
                            if (currentSouls > 0 && currentTime - lastKillTime > 200) {
                                if (player.ticksExisted % 200 == 0) {
                                    data.setInteger("synergy_soul_stacks", currentSouls - 1);
                                }
                            }

                            // 灵魂光环粒子
                            if (currentSouls > 0 && player.ticksExisted % 20 == 0) {
                                int particles = Math.min(currentSouls / 3, 5);
                                for (int i = 0; i < particles; i++) {
                                    double angle = (player.ticksExisted / 10.0 + i * Math.PI * 2 / particles) % (Math.PI * 2);
                                    world.spawnParticle(EnumParticleTypes.SPELL_MOB,
                                            player.posX + Math.cos(angle) * 0.8,
                                            player.posY + 1.5,
                                            player.posZ + Math.sin(angle) * 0.8,
                                            0.1, 0.5, 0.1);
                                }
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Kill to stack souls, execute low HP targets at max";
                    }
                })

                .priority(30)
                .build();
    }

    /**
     * 相位偏移 Synergy
     * 需要: STEALTH + FLIGHT_MODULE + WATERPROOF
     *
     * 机制:
     * - 受到致命伤害时（会死亡），触发相位偏移
     * - 进入相位状态 3 秒：
     *   - 无敌
     *   - 可穿墙
     *   - 移速提升
     * - 冷却时间 = 60 - totalLevel * 5 秒（最低30秒）
     * - 消耗 50% 当前能量
     */
    public static SynergyDefinition createPhaseShift() {
        return SynergyDefinition.builder("phase_shift")
                .displayName("相位偏移")
                .description("在死亡边缘偏移到另一个相位")

                .requireModules("STEALTH", "FLIGHT_MODULE", "WATERPROOF")
                .addLink("STEALTH", "FLIGHT_MODULE", "phase")
                .addLink("FLIGHT_MODULE", "WATERPROOF", "phase")
                .addLink("WATERPROOF", "STEALTH", "phase")

                .triggerOn(SynergyEventType.HURT, SynergyEventType.TICK)

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        World world = player.world;
                        NBTTagCompound data = player.getEntityData();

                        int stealthLevel = context.getModuleLevel("STEALTH");
                        int flightLevel = context.getModuleLevel("FLIGHT_MODULE");
                        int waterproofLevel = context.getModuleLevel("WATERPROOF");
                        int totalLevel = stealthLevel + flightLevel + waterproofLevel;

                        long currentTime = world.getTotalWorldTime();
                        long phaseEndTime = data.getLong("synergy_phase_end");
                        long phaseCooldown = data.getLong("synergy_phase_cooldown");

                        boolean isPhased = currentTime < phaseEndTime;

                        if (context.getEventType() == SynergyEventType.HURT) {
                            // 相位中无敌
                            if (isPhased) {
                                // 恢复伤害（模拟无敌）
                                player.heal(context.getOriginalDamage());

                                // 相位闪烁特效
                                world.spawnParticle(EnumParticleTypes.PORTAL,
                                        player.posX, player.posY + 1, player.posZ,
                                        0, 0, 0);
                                return;
                            }

                            // 检查是否致命伤害
                            float damage = context.getOriginalDamage();
                            float healthAfter = player.getHealth() - damage;

                            if (healthAfter <= 0 && currentTime > phaseCooldown) {
                                // 触发相位偏移！
                                int cooldownSeconds = Math.max(30, 60 - totalLevel * 5);
                                int phaseDuration = 60 + totalLevel * 10; // 3-4秒

                                data.setLong("synergy_phase_end", currentTime + phaseDuration);
                                data.setLong("synergy_phase_cooldown", currentTime + cooldownSeconds * 20L);

                                // 恢复致命伤害
                                player.heal(damage);

                                // 恢复到1HP
                                if (player.getHealth() <= 0) {
                                    player.setHealth(1.0f);
                                }

                                // 消耗50%能量
                                int currentEnergy = ExistingModuleBridge.getInstance().getEnergy(player);
                                ExistingModuleBridge.getInstance().consumeEnergy(player, currentEnergy / 2);

                                // 相位触发特效
                                for (int i = 0; i < 50; i++) {
                                    world.spawnParticle(EnumParticleTypes.PORTAL,
                                            player.posX + (world.rand.nextDouble() - 0.5) * 3,
                                            player.posY + world.rand.nextDouble() * 2,
                                            player.posZ + (world.rand.nextDouble() - 0.5) * 3,
                                            (world.rand.nextDouble() - 0.5) * 0.5,
                                            world.rand.nextDouble() * 0.3,
                                            (world.rand.nextDouble() - 0.5) * 0.5);
                                }

                                world.playSound(null, player.posX, player.posY, player.posZ,
                                        SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 0.5f);
                            }

                        } else if (context.getEventType() == SynergyEventType.TICK) {
                            if (isPhased) {
                                // 相位状态效果

                                // 移速提升（直接修改motion）
                                if (player.moveForward > 0) {
                                    double boost = 0.1 + stealthLevel * 0.02;
                                    Vec3d look = player.getLookVec();
                                    player.motionX += look.x * boost;
                                    player.motionZ += look.z * boost;
                                }

                                // 可以穿墙（设置noClip）
                                player.noClip = true;

                                // 相位粒子
                                if (player.ticksExisted % 2 == 0) {
                                    world.spawnParticle(EnumParticleTypes.PORTAL,
                                            player.posX + (world.rand.nextDouble() - 0.5),
                                            player.posY + world.rand.nextDouble() * 2,
                                            player.posZ + (world.rand.nextDouble() - 0.5),
                                            0, 0, 0);
                                }

                                // 快结束时警告
                                long remaining = phaseEndTime - currentTime;
                                if (remaining == 20) {
                                    world.playSound(null, player.posX, player.posY, player.posZ,
                                            SoundEvents.BLOCK_NOTE_PLING, SoundCategory.PLAYERS, 0.5f, 0.5f);
                                }

                            } else {
                                // 确保退出相位时恢复正常
                                if (data.getBoolean("synergy_was_phased")) {
                                    player.noClip = false;
                                    data.setBoolean("synergy_was_phased", false);

                                    // 退出相位特效
                                    world.playSound(null, player.posX, player.posY, player.posZ,
                                            SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 0.5f, 1.5f);
                                }

                                // 记录是否在相位中（用于下一tick检测退出）
                                if (isPhased) {
                                    data.setBoolean("synergy_was_phased", true);
                                }
                            }
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Phase shift on fatal damage, become invulnerable briefly";
                    }
                })

                .priority(1) // 最高优先级（救命用）
                .build();
    }

    /**
     * 连锁闪电 Synergy
     * 需要: SOLAR_GENERATOR + DAMAGE_BOOST + RANGE_EXTENSION
     *
     * 机制:
     * - 攻击时，伤害链式传递到附近敌人
     * - 链条数 = 2 + totalLevel（最多8条）
     * - 每次传递伤害衰减 20%
     * - 白天（太阳能充足时）伤害+50%
     * - 范围 = 4 + rangeLevel 格
     */
    public static SynergyDefinition createChainLightning() {
        return SynergyDefinition.builder("chain_lightning")
                .displayName("连锁闪电")
                .description("雷霆之力，链式毁灭")

                .requireModules("SOLAR_GENERATOR", "DAMAGE_BOOST", "RANGE_EXTENSION")
                .addLink("SOLAR_GENERATOR", "DAMAGE_BOOST", "lightning")
                .addLink("DAMAGE_BOOST", "RANGE_EXTENSION", "lightning")
                .addLink("RANGE_EXTENSION", "SOLAR_GENERATOR", "lightning")

                .triggerOn(SynergyEventType.ATTACK)

                .addCondition(TargetCondition.isNotPlayer())

                .addEffect(new ISynergyEffect() {
                    @Override
                    public void apply(SynergyContext context) {
                        EntityPlayer player = context.getPlayer();
                        EntityLivingBase primaryTarget = context.getTarget();
                        if (primaryTarget == null) return;

                        World world = player.world;

                        int solarLevel = context.getModuleLevel("SOLAR_GENERATOR");
                        int damageLevel = context.getModuleLevel("DAMAGE_BOOST");
                        int rangeLevel = context.getModuleLevel("RANGE_EXTENSION");
                        int totalLevel = solarLevel + damageLevel + rangeLevel;

                        int maxChains = Math.min(2 + totalLevel, 8);
                        double chainRange = 4.0 + rangeLevel;

                        // 白天加成
                        boolean isDaytime = world.isDaytime() && world.canSeeSky(player.getPosition());
                        float dayBonus = isDaytime ? 1.5f : 1.0f;

                        float baseDamage = context.getOriginalDamage() * 0.5f * dayBonus;

                        // 链式传递
                        EntityLivingBase currentTarget = primaryTarget;
                        java.util.Set<Integer> hitEntities = new java.util.HashSet<>();
                        hitEntities.add(primaryTarget.getEntityId());

                        float currentDamage = baseDamage;

                        for (int chain = 0; chain < maxChains; chain++) {
                            // 寻找下一个目标
                            AxisAlignedBB searchArea = currentTarget.getEntityBoundingBox().grow(chainRange);
                            List<EntityLivingBase> candidates = world.getEntitiesWithinAABB(
                                    EntityLivingBase.class, searchArea,
                                    e -> e != player &&
                                         e instanceof IMob &&
                                         e.isEntityAlive() &&
                                         !hitEntities.contains(e.getEntityId()));

                            if (candidates.isEmpty()) break;

                            // 选择最近的
                            EntityLivingBase nextTarget = null;
                            double minDist = Double.MAX_VALUE;
                            for (EntityLivingBase candidate : candidates) {
                                double dist = candidate.getDistanceSq(currentTarget);
                                if (dist < minDist) {
                                    minDist = dist;
                                    nextTarget = candidate;
                                }
                            }

                            if (nextTarget == null) break;

                            // 造成伤害
                            nextTarget.attackEntityFrom(
                                    SynergyEventHandler.causeSynergyDamage(player),
                                    currentDamage);

                            // 闪电粒子连线
                            drawLightningParticles(world, currentTarget, nextTarget);

                            // 准备下一次链接
                            hitEntities.add(nextTarget.getEntityId());
                            currentTarget = nextTarget;
                            currentDamage *= 0.8f; // 衰减20%
                        }

                        // 闪电音效
                        if (hitEntities.size() > 1) {
                            world.playSound(null, primaryTarget.posX, primaryTarget.posY, primaryTarget.posZ,
                                    SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.PLAYERS,
                                    0.3f, 1.5f + world.rand.nextFloat() * 0.5f);
                        }

                        // 能量消耗
                        ExistingModuleBridge.getInstance().consumeEnergy(player, 30);
                    }

                    private void drawLightningParticles(World world, EntityLivingBase from, EntityLivingBase to) {
                        Vec3d start = new Vec3d(from.posX, from.posY + from.height / 2, from.posZ);
                        Vec3d end = new Vec3d(to.posX, to.posY + to.height / 2, to.posZ);
                        Vec3d diff = end.subtract(start);

                        int particles = (int) (diff.length() * 3);
                        for (int i = 0; i < particles; i++) {
                            double t = (double) i / particles;
                            Vec3d pos = start.add(diff.scale(t));

                            // 添加随机偏移使其看起来像闪电
                            double jitter = 0.2;
                            world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                                    pos.x + (world.rand.nextDouble() - 0.5) * jitter,
                                    pos.y + (world.rand.nextDouble() - 0.5) * jitter,
                                    pos.z + (world.rand.nextDouble() - 0.5) * jitter,
                                    0, 0, 0);
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Attacks chain to nearby enemies";
                    }
                })

                .priority(50)
                .build();
    }

    // ==================== 辅助类 ====================

    /**
     * 延迟伤害追踪器
     */
    private static class DelayedDamageTracker {
        private final java.util.List<DamageEcho> echoes = new java.util.ArrayList<>();

        public void addEcho(int targetId, float damage, long triggerTime, EntityPlayer source) {
            // 限制最大回响数
            if (echoes.size() >= 15) {
                echoes.remove(0);
            }
            echoes.add(new DamageEcho(targetId, damage, triggerTime, source));
        }

        public void processEchoes(World world, long currentTime) {
            java.util.Iterator<DamageEcho> iterator = echoes.iterator();
            while (iterator.hasNext()) {
                DamageEcho echo = iterator.next();

                if (currentTime >= echo.triggerTime) {
                    // 触发回响伤害
                    net.minecraft.entity.Entity entity = world.getEntityByID(echo.targetId);
                    if (entity instanceof EntityLivingBase && entity.isEntityAlive()) {
                        EntityLivingBase target = (EntityLivingBase) entity;
                        target.attackEntityFrom(
                                SynergyEventHandler.causeSynergyDamage(echo.source),
                                echo.damage);

                        // 回响特效
                        world.spawnParticle(EnumParticleTypes.PORTAL,
                                target.posX, target.posY + 1, target.posZ,
                                0, 0.2, 0);
                    }
                    iterator.remove();
                }
            }
        }

        private static class DamageEcho {
            final int targetId;
            final float damage;
            final long triggerTime;
            final EntityPlayer source;

            DamageEcho(int targetId, float damage, long triggerTime, EntityPlayer source) {
                this.targetId = targetId;
                this.damage = damage;
                this.triggerTime = triggerTime;
                this.source = source;
            }
        }
    }
}
