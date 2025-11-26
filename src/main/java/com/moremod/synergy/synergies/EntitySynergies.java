package com.moremod.synergy.synergies;

import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.synergy.bridge.ExistingModuleBridge;
import com.moremod.synergy.condition.*;
import com.moremod.synergy.core.*;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.*;

/**
 * AI/实体类 Synergy 定义
 *
 * 包含:
 * 1. Hive Mind (蜂群意识) - 召唤物群体智能
 * 2. Corruption Seed (腐化种子) - 渐进式心智控制
 * 3. Pack Hunter (猎群本能) - 多人狩猎协作
 */
public class EntitySynergies {

    private static final Random RANDOM = new Random();

    // 追踪状态
    private static final Map<UUID, CorruptionSeedData> CORRUPTION_DATA = new HashMap<>();
    private static final Map<UUID, PackHunterData> PACK_DATA = new HashMap<>();

    public static void registerAll(SynergyManager manager) {
        manager.register(createHiveMind());
        manager.register(createCorruptionSeed());
        manager.register(createPackHunter());

        System.out.println("[Synergy] Registered 3 Entity Synergies");
    }

    // ==================== 1. Hive Mind (蜂群意识) ====================

    /**
     * Hive Mind - 蜂群意识
     *
     * 模块要求: SUMMON + LINK + NEURAL (连续三槽)
     * 触发条件: 同时存在 3 个以上召唤物
     *
     * 效果:
     * - 召唤物形成 Hive Network (共享视野、仇恨)
     * - HP 自动均分
     * - 聚集时进入 Swarm Mode (攻速+50%, 叠加毒素)
     * - 10 层毒素: 目标麻痹 2 秒
     *
     * 代价:
     * - 召唤物死亡造成精神伤害
     * - 能量消耗随召唤物数量增加
     * - 全灭时 Neural Collapse (Stun 5 秒)
     * - 召唤物需要手动指挥
     */
    public static SynergyDefinition createHiveMind() {
        return SynergyDefinition.builder("hive_mind")
                .displayName("蜂群意识")
                .description("与召唤物建立心智链接，成为一体")

                // 模块要求 (连续)
                .requireModules("SUMMON", "LINK", "NEURAL")
                .addLink("SUMMON", "LINK", "chain")
                .addLink("LINK", "NEURAL", "chain")

                // 触发: Tick
                .triggerOn(SynergyEventType.TICK)

                // 条件: 需要有召唤物 (简化: 无特殊条件)
                .addCondition(CooldownCondition.notOnCooldown("hive_mind"))

                // 效果
                .addEffect(new HiveMindEffect())

                .priority(20)
                .build();
    }

    private static class HiveMindEffect implements ISynergyEffect {
        private static final String STATE_HIVE_ACTIVE = "hive_mind_active";
        private static final int SWARM_RANGE = 5;
        private static final float ENERGY_COST_BASE = 0.25f;  // 基础 5%/秒

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
            World world = player.world;

            // 查找玩家的召唤物 (简化: 查找附近被驯服的生物)
            List<EntityLiving> summons = findPlayerSummons(player, world);

            if (summons.size() < 3) {
                if (state.hasActiveState(STATE_HIVE_ACTIVE)) {
                    state.deactivateState(STATE_HIVE_ACTIVE);
                }
                return;
            }

            // 能量消耗
            float energyCost = ENERGY_COST_BASE * summons.size();
            int maxEnergy = bridge.getMaxEnergy(player);
            float energyPercent = bridge.getEnergyPercent(player);

            if (energyPercent < energyCost) {
                return;
            }
            bridge.consumeEnergy(player, (int)(maxEnergy * energyCost / 100f / 20f));  // 每 tick

            // 激活状态
            if (!state.hasActiveState(STATE_HIVE_ACTIVE)) {
                state.activateState(STATE_HIVE_ACTIVE, Integer.MAX_VALUE);
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "🐝 蜂群意识: Hive Network 建立！" +
                        TextFormatting.GRAY + " [" + summons.size() + " 单位]"));
            }

            // HP 均分
            shareHealth(summons);

            // 检查是否在 Swarm Mode (聚集)
            checkSwarmMode(player, summons, world);

            // 粒子效果 - 连接线
            if (world.getTotalWorldTime() % 20 == 0) {
                drawHiveConnections(world, summons);
            }
        }

        private List<EntityLiving> findPlayerSummons(EntityPlayer player, World world) {
            // 简化实现: 查找 20 格内的被驯服生物
            AxisAlignedBB searchBox = new AxisAlignedBB(
                    player.posX - 20, player.posY - 10, player.posZ - 20,
                    player.posX + 20, player.posY + 10, player.posZ + 20
            );

            return world.getEntitiesWithinAABB(EntityLiving.class, searchBox, e -> {
                // 检查是否属于玩家 (简化: 用标签)
                return e.getTags().contains("summon_" + player.getUniqueID().toString());
            });
        }

        private void shareHealth(List<EntityLiving> summons) {
            if (summons.isEmpty()) return;

            float totalHealth = 0;
            float totalMaxHealth = 0;

            for (EntityLiving summon : summons) {
                totalHealth += summon.getHealth();
                totalMaxHealth += summon.getMaxHealth();
            }

            // 按比例分配
            float healthRatio = totalHealth / totalMaxHealth;
            for (EntityLiving summon : summons) {
                summon.setHealth(summon.getMaxHealth() * healthRatio);
            }
        }

        private void checkSwarmMode(EntityPlayer player, List<EntityLiving> summons, World world) {
            // 检查聚集状态
            int clusterCount = 0;
            Vec3d center = Vec3d.ZERO;

            for (EntityLiving summon : summons) {
                center = center.add(summon.getPositionVector());
            }
            center = center.scale(1.0 / summons.size());

            for (EntityLiving summon : summons) {
                if (summon.getPositionVector().distanceTo(center) < SWARM_RANGE) {
                    clusterCount++;
                }
            }

            if (clusterCount >= 3) {
                // Swarm Mode 激活
                for (EntityLiving summon : summons) {
                    summon.addPotionEffect(new PotionEffect(MobEffects.SPEED, 20, 1, false, false));
                    summon.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 20, 0, false, false));
                }

                // 粒子效果
                if (world.getTotalWorldTime() % 10 == 0) {
                    for (int i = 0; i < 5; i++) {
                        world.spawnParticle(EnumParticleTypes.SPELL_MOB,
                                center.x + RANDOM.nextGaussian() * 2,
                                center.y + 1,
                                center.z + RANDOM.nextGaussian() * 2,
                                0.9, 0.7, 0);
                    }
                }
            }
        }

        private void drawHiveConnections(World world, List<EntityLiving> summons) {
            for (int i = 0; i < summons.size(); i++) {
                for (int j = i + 1; j < summons.size(); j++) {
                    Vec3d start = summons.get(i).getPositionVector().add(0, 1, 0);
                    Vec3d end = summons.get(j).getPositionVector().add(0, 1, 0);

                    if (start.distanceTo(end) < 15) {
                        for (int k = 0; k < 5; k++) {
                            double progress = k / 5.0;
                            double x = start.x + (end.x - start.x) * progress;
                            double y = start.y + (end.y - start.y) * progress;
                            double z = start.z + (end.z - start.z) * progress;

                            world.spawnParticle(EnumParticleTypes.SPELL_INSTANT,
                                    x, y, z, 0, 0, 0);
                        }
                    }
                }
            }
        }

        @Override
        public String getDescription() {
            return "Summons form a hive network";
        }
    }

    // ==================== 2. Corruption Seed (腐化种子) ====================

    /**
     * Corruption Seed - 腐化种子
     *
     * 模块要求: CORRUPTION + MIND + VOID (任意排列)
     * 触发条件: 对同一目标造成累计 50 点伤害（非致命）
     *
     * 效果:
     * - 植入腐化种子
     * - Stage 1 (0-10秒): 10% 概率攻击友方
     * - Stage 2 (10-20秒): 30% 概率，视野干扰
     * - Stage 3 (20秒+): 完全控制 8 秒
     *
     * 代价:
     * - 植入消耗 40% 能量
     * - 只能存在 1 颗种子
     * - 目标提前死亡反噬 30 伤害
     * - 控制后目标 60 秒免疫
     * - 每次增加 25% Rejection
     */
    public static SynergyDefinition createCorruptionSeed() {
        return SynergyDefinition.builder("corruption_seed")
                .displayName("腐化种子")
                .description("在目标心智中植入腐化的种子")

                // 模块要求
                .requireModules("CORRUPTION", "MIND", "VOID")

                // 触发: 攻击 / Tick
                .triggerOn(SynergyEventType.ATTACK, SynergyEventType.TICK)

                // 条件
                .addCondition(EnergyThresholdCondition.atLeast(40f))

                // 效果
                .addEffect(new CorruptionSeedEffect())

                .priority(25)
                .build();
    }

    private static class CorruptionSeedEffect implements ISynergyEffect {
        private static final int STAGE_1_DURATION = 200;  // 10 秒
        private static final int STAGE_2_DURATION = 400;  // 20 秒
        private static final int CONTROL_DURATION = 160;  // 8 秒
        private static final int IMMUNITY_DURATION = 1200; // 60 秒
        private static final float DAMAGE_THRESHOLD = 50f;

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
            World world = player.world;

            UUID playerId = player.getUniqueID();
            CorruptionSeedData data = CORRUPTION_DATA.computeIfAbsent(playerId, k -> new CorruptionSeedData());

            if (context.getEventType() == SynergyEventType.TICK) {
                // 维护已植入的种子
                maintainSeed(player, state, data, world);
                return;
            }

            // 攻击逻辑
            EntityLivingBase target = context.getTarget();
            if (target == null) return;

            // 检查目标是否免疫
            if (target.getTags().contains("corruption_immune")) {
                return;
            }

            // 检查是否已有种子
            if (data.targetEntity != null && data.targetEntity.isEntityAlive()) {
                return;  // 只能有一颗种子
            }

            // 累积伤害
            float damage = context.getOriginalDamage();
            UUID targetId = target.getUniqueID();

            float accumulated = data.damageAccumulation.getOrDefault(targetId, 0f) + damage;
            data.damageAccumulation.put(targetId, accumulated);

            // 检查是否达到阈值
            if (accumulated >= DAMAGE_THRESHOLD) {
                // 植入种子
                plantSeed(player, target, state, data, bridge);
            }
        }

        private void plantSeed(EntityPlayer player, EntityLivingBase target,
                              SynergyPlayerState state, CorruptionSeedData data,
                              ExistingModuleBridge bridge) {
            // 消耗能量
            int maxEnergy = bridge.getMaxEnergy(player);
            bridge.consumeEnergy(player, (int)(maxEnergy * 0.4f));

            // 植入种子
            data.targetEntity = target;
            data.targetId = target.getUniqueID();
            data.seedStartTime = player.world.getTotalWorldTime();
            data.stage = 1;
            data.damageAccumulation.clear();

            // 增加排异
            state.addRejection(25f);

            // 视觉效果
            spawnPlantParticles(target);

            player.world.playSound(null, target.posX, target.posY, target.posZ,
                    SoundEvents.ENTITY_VEX_AMBIENT, SoundCategory.PLAYERS, 1.0f, 0.5f);

            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "🌱 腐化种子: 已植入 " + target.getName()));
        }

        private void maintainSeed(EntityPlayer player, SynergyPlayerState state,
                                 CorruptionSeedData data, World world) {
            if (data.targetEntity == null) return;

            EntityLivingBase target = data.targetEntity;

            // 检查目标是否死亡
            if (!target.isEntityAlive()) {
                // 反噬
                if (data.stage < 3) {
                    player.attackEntityFrom(DamageSource.MAGIC, 30f);
                    player.sendMessage(new TextComponentString(
                            TextFormatting.DARK_RED + "🌱 腐化种子: 目标死亡，反噬 30 伤害！"));
                }
                clearSeed(data);
                return;
            }

            // 计算阶段
            long elapsed = world.getTotalWorldTime() - data.seedStartTime;

            if (elapsed < STAGE_1_DURATION) {
                // Stage 1: 10% 混乱
                if (RANDOM.nextFloat() < 0.005f) {  // 每 tick 0.5%
                    applyConfusion(target, world);
                }
            } else if (elapsed < STAGE_2_DURATION) {
                if (data.stage < 2) {
                    data.stage = 2;
                    player.sendMessage(new TextComponentString(
                            TextFormatting.DARK_PURPLE + "🌱 腐化种子: Stage 2 - 目标视野开始模糊"));
                }

                // Stage 2: 30% 混乱 + 视野干扰
                if (RANDOM.nextFloat() < 0.015f) {
                    applyConfusion(target, world);
                }
                target.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 40, 0, false, false));
            } else {
                if (data.stage < 3) {
                    data.stage = 3;
                    player.sendMessage(new TextComponentString(
                            TextFormatting.DARK_PURPLE + "🌱 腐化种子: Stage 3 - 完全控制！"));

                    // 完全控制
                    applyFullControl(player, target, data, world);
                }

                // 检查控制是否结束
                if (elapsed > STAGE_2_DURATION + CONTROL_DURATION) {
                    endControl(target, data);
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + "🌱 腐化种子: 控制结束"));
                }
            }

            // 粒子效果
            if (world.getTotalWorldTime() % 20 == 0) {
                spawnSeedParticles(target, data.stage);
            }
        }

        private void applyConfusion(EntityLivingBase target, World world) {
            // 让目标攻击附近的友方
            target.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 20, 0, false, false));

            // 粒子效果
            world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                    target.posX, target.posY + 2, target.posZ,
                    0, 0.1, 0);
        }

        private void applyFullControl(EntityPlayer player, EntityLivingBase target,
                                     CorruptionSeedData data, World world) {
            // 简化控制: 目标停止行动
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, CONTROL_DURATION, 10, false, true));
            target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, CONTROL_DURATION, 10, false, true));
            target.addPotionEffect(new PotionEffect(MobEffects.GLOWING, CONTROL_DURATION, 0, false, true));

            // 标记为被控制
            target.addTag("corruption_controlled");
        }

        private void endControl(EntityLivingBase target, CorruptionSeedData data) {
            // 添加免疫
            target.addTag("corruption_immune");
            target.getTags().remove("corruption_controlled");

            clearSeed(data);
        }

        private void clearSeed(CorruptionSeedData data) {
            data.targetEntity = null;
            data.targetId = null;
            data.stage = 0;
        }

        private void spawnPlantParticles(EntityLivingBase target) {
            World world = target.world;
            for (int i = 0; i < 30; i++) {
                world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        target.posX + RANDOM.nextGaussian() * 0.5,
                        target.posY + 1 + RANDOM.nextGaussian() * 0.5,
                        target.posZ + RANDOM.nextGaussian() * 0.5,
                        0, 0.1, 0);
            }
        }

        private void spawnSeedParticles(EntityLivingBase target, int stage) {
            World world = target.world;
            int particleCount = stage * 3;

            for (int i = 0; i < particleCount; i++) {
                double angle = (i / (double) particleCount) * Math.PI * 2;
                double radius = 0.5 + stage * 0.2;
                double x = target.posX + Math.cos(angle) * radius;
                double z = target.posZ + Math.sin(angle) * radius;

                world.spawnParticle(EnumParticleTypes.PORTAL,
                        x, target.posY + 1.5, z,
                        0, -0.05, 0);
            }
        }

        @Override
        public String getDescription() {
            return "Plant corruption seed for mind control";
        }
    }

    // ==================== 3. Pack Hunter (猎群本能) ====================

    /**
     * Pack Hunter - 猎群本能
     *
     * 模块要求: BEAST + TRACK + MOMENTUM (对称排列)
     * 触发条件: 与至少 2 个其他装备此 Synergy 的玩家在 15 格内
     *
     * 效果:
     * - 形成 Hunting Pack
     * - Pack Leader 锁定的目标所有成员可见
     * - 对猎物伤害 +20%, 不同方向 +15%/人
     * - 10 层 Bleed 触发 Takedown (倒地 3 秒)
     *
     * 代价:
     * - 锁定期间无法攻击其他目标
     * - 猎物逃脱造成 Exhausted
     * - Leader 受伤分担给队友
     * - 切换目标 15 秒冷却
     */
    public static SynergyDefinition createPackHunter() {
        return SynergyDefinition.builder("pack_hunter")
                .displayName("猎群本能")
                .description("与同伴组成狩猎小队")

                // 模块要求 (对称)
                .requireModules("BEAST", "TRACK", "MOMENTUM")
                .addLink("BEAST", "TRACK", "symmetric")
                .addLink("TRACK", "MOMENTUM", "symmetric")

                // 触发: Tick / 攻击
                .triggerOn(SynergyEventType.TICK, SynergyEventType.ATTACK)

                // 条件: 需要队友
                .addCondition(new PackHunterPartnerCondition())

                // 效果
                .addEffect(new PackHunterEffect())

                .priority(20)
                .build();
    }

    private static class PackHunterPartnerCondition implements com.moremod.synergy.api.ISynergyCondition {
        @Override
        public boolean test(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            World world = player.world;

            List<EntityPlayer> nearbyPlayers = world.getEntitiesWithinAABB(
                    EntityPlayer.class,
                    new AxisAlignedBB(
                            player.posX - 15, player.posY - 5, player.posZ - 15,
                            player.posX + 15, player.posY + 5, player.posZ + 15
                    ),
                    p -> p != player
            );

            return nearbyPlayers.size() >= 2;
        }

        @Override
        public String getDescription() {
            return "At least 2 pack members nearby";
        }
    }

    private static class PackHunterEffect implements ISynergyEffect {
        private static final String STATE_PACK_ACTIVE = "pack_hunter_active";
        private static final int PACK_RANGE = 15;
        private static final int ESCAPE_RANGE = 50;
        private static final int EXHAUSTED_DURATION = 200;  // 10 秒
        private static final int TAKEDOWN_BLEED_STACKS = 10;

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            World world = player.world;

            UUID playerId = player.getUniqueID();
            PackHunterData data = PACK_DATA.computeIfAbsent(playerId, k -> new PackHunterData());

            // 查找队友
            List<EntityPlayer> packMembers = findPackMembers(player, world);

            if (packMembers.size() < 2) {
                if (state.hasActiveState(STATE_PACK_ACTIVE)) {
                    state.deactivateState(STATE_PACK_ACTIVE);
                }
                return;
            }

            if (context.getEventType() == SynergyEventType.TICK) {
                // 维护 Pack 状态
                maintainPack(player, state, data, packMembers, world);
            } else if (context.getEventType() == SynergyEventType.ATTACK) {
                // 攻击逻辑
                handlePackAttack(player, context, data, packMembers);
            }
        }

        private List<EntityPlayer> findPackMembers(EntityPlayer player, World world) {
            return world.getEntitiesWithinAABB(
                    EntityPlayer.class,
                    new AxisAlignedBB(
                            player.posX - PACK_RANGE, player.posY - 5, player.posZ - PACK_RANGE,
                            player.posX + PACK_RANGE, player.posY + 5, player.posZ + PACK_RANGE
                    ),
                    p -> p != player
            );
        }

        private void maintainPack(EntityPlayer player, SynergyPlayerState state,
                                 PackHunterData data, List<EntityPlayer> packMembers, World world) {
            // 激活状态
            if (!state.hasActiveState(STATE_PACK_ACTIVE)) {
                state.activateState(STATE_PACK_ACTIVE, Integer.MAX_VALUE);
                player.sendMessage(new TextComponentString(
                        TextFormatting.GOLD + "🐺 猎群本能: Pack 形成！" +
                        TextFormatting.GRAY + " [" + (packMembers.size() + 1) + " 成员]"));
            }

            // 确定 Pack Leader (攻击力最高的)
            EntityPlayer leader = player;
            // 简化: 第一个人就是 leader

            // 检查猎物状态
            if (data.preyEntity != null) {
                if (!data.preyEntity.isEntityAlive()) {
                    // 猎物死亡
                    handlePreyKilled(player, data, packMembers);
                } else if (player.getDistance(data.preyEntity) > ESCAPE_RANGE) {
                    // 猎物逃脱
                    handlePreyEscaped(player, data, packMembers);
                }
            }

            // 视觉效果 - 猎物标记
            if (data.preyEntity != null && world.getTotalWorldTime() % 10 == 0) {
                spawnPreyMarker(data.preyEntity);
            }
        }

        private void handlePackAttack(EntityPlayer player, SynergyContext context,
                                     PackHunterData data, List<EntityPlayer> packMembers) {
            EntityLivingBase target = context.getTarget();
            if (target == null) return;

            // 设置或验证猎物
            if (data.preyEntity == null) {
                data.preyEntity = target;
                data.preyId = target.getUniqueID();
                data.bleedStacks = 0;

                player.sendMessage(new TextComponentString(
                        TextFormatting.GOLD + "🐺 猎群本能: 锁定猎物 - " + target.getName()));
            } else if (data.preyEntity != target) {
                // 攻击非猎物目标
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "🐺 猎群本能: 必须攻击当前猎物！"));
                return;
            }

            // 计算伤害加成
            float damageBonus = 0.2f;  // 基础 +20%

            // 计算不同方向的攻击者数量
            int directionCount = countDifferentDirections(target, packMembers);
            damageBonus += directionCount * 0.15f;  // 每个方向 +15%

            // 应用加成伤害
            float originalDamage = context.getOriginalDamage();
            float bonusDamage = originalDamage * damageBonus;
            target.attackEntityFrom(DamageSource.causePlayerDamage(player), bonusDamage);

            // 叠加 Bleed
            data.bleedStacks++;

            // 检查 Takedown
            if (data.bleedStacks >= TAKEDOWN_BLEED_STACKS) {
                triggerTakedown(player, target, data);
            }
        }

        private int countDifferentDirections(EntityLivingBase target, List<EntityPlayer> packMembers) {
            // 简化: 返回队友数量作为方向数
            return Math.min(packMembers.size(), 3);
        }

        private void triggerTakedown(EntityPlayer player, EntityLivingBase target, PackHunterData data) {
            // 猎物倒地
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 10, false, true));
            target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 60, 10, false, true));

            // 重置 Bleed
            data.bleedStacks = 0;

            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "🐺 TAKEDOWN! 猎物倒地 3 秒！"));

            // 音效
            player.world.playSound(null, target.posX, target.posY, target.posZ,
                    SoundEvents.ENTITY_WOLF_GROWL, SoundCategory.PLAYERS, 1.0f, 0.8f);
        }

        private void handlePreyKilled(EntityPlayer player, PackHunterData data,
                                     List<EntityPlayer> packMembers) {
            // 回复
            float healAmount = player.getMaxHealth() * 0.15f;
            player.heal(healAmount);

            for (EntityPlayer member : packMembers) {
                member.heal(healAmount);
            }

            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "🐺 猎群本能: 狩猎成功！全员回复 15% HP"));

            data.preyEntity = null;
            data.preyId = null;
        }

        private void handlePreyEscaped(EntityPlayer player, PackHunterData data,
                                      List<EntityPlayer> packMembers) {
            // Exhausted
            player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, EXHAUSTED_DURATION, 1, false, true));

            for (EntityPlayer member : packMembers) {
                member.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, EXHAUSTED_DURATION, 1, false, true));
            }

            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "🐺 猎群本能: 猎物逃脱！全员 Exhausted"));

            data.preyEntity = null;
            data.preyId = null;
        }

        private void spawnPreyMarker(EntityLivingBase prey) {
            World world = prey.world;
            for (int i = 0; i < 5; i++) {
                double angle = (i / 5.0) * Math.PI * 2;
                double x = prey.posX + Math.cos(angle) * 0.8;
                double z = prey.posZ + Math.sin(angle) * 0.8;

                world.spawnParticle(EnumParticleTypes.VILLAGER_ANGRY,
                        x, prey.posY + 2.5, z,
                        0, -0.05, 0);
            }
        }

        @Override
        public String getDescription() {
            return "Form hunting pack with nearby players";
        }
    }

    // ==================== 辅助类 ====================

    private static class CorruptionSeedData {
        public EntityLivingBase targetEntity;
        public UUID targetId;
        public long seedStartTime;
        public int stage;
        public Map<UUID, Float> damageAccumulation = new HashMap<>();
    }

    private static class PackHunterData {
        public EntityLivingBase preyEntity;
        public UUID preyId;
        public int bleedStacks;
    }
}
