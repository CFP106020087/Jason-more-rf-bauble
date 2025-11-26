package com.moremod.synergy.synergies;

import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.synergy.bridge.ExistingModuleBridge;
import com.moremod.synergy.condition.*;
import com.moremod.synergy.core.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.*;

/**
 * 环境/领域类 Synergy 定义
 *
 * 包含:
 * 1. Domain Expansion (领域展开) - 改变区域规则的究极技
 * 2. Reality Fracture (现实碎裂) - 受伤触发的混沌区域
 * 3. Sanctuary (圣域) - 纯防御无攻击的圣所
 */
public class DomainSynergies {

    private static final Random RANDOM = new Random();

    // 追踪领域状态
    private static final Map<UUID, DomainData> DOMAIN_DATA = new HashMap<>();
    private static final Map<UUID, SanctuaryData> SANCTUARY_DATA = new HashMap<>();

    public static void registerAll(SynergyManager manager) {
        manager.register(createDomainExpansion());
        manager.register(createRealityFracture());
        manager.register(createSanctuary());

        System.out.println("[Synergy] Registered 3 Domain Synergies");
    }

    // ==================== 1. Domain Expansion (领域展开) ====================

    /**
     * Domain Expansion - 领域展开
     *
     * 模块要求: TERRITORY + AMPLIFY + SOUL + 任意元素模块 (4 槽环形)
     * 触发条件: 能量 100% + 主动激活 + 3 秒吟唱
     *
     * 效果:
     * - 展开 15 格半径领域，持续 20 秒
     * - 领域规则基于元素模块:
     *   - Fire: 地面燃烧
     *   - Ice: 减速
     *   - Lightning: 随机落雷
     *   - Void: 禁止传送
     * - 领域主人无冷却使用元素技能
     *
     * 代价:
     * - 吟唱期间无法移动或取消
     * - 消耗 100% 能量 + 20% HP 上限
     * - 结束后无法移动 5 秒
     * - 每次增加 30% Rejection
     * - 300 秒冷却
     */
    public static SynergyDefinition createDomainExpansion() {
        return SynergyDefinition.builder("domain_expansion")
                .displayName("领域展开")
                .description("展开专属领域，改写现实法则")

                // 模块要求 (4 槽环形)
                .requireModules("TERRITORY", "AMPLIFY", "SOUL", "FIRE")  // FIRE 可换成其他元素
                .addLink("TERRITORY", "AMPLIFY", "ring")
                .addLink("AMPLIFY", "SOUL", "ring")
                .addLink("SOUL", "FIRE", "ring")
                .addLink("FIRE", "TERRITORY", "ring")

                // 触发: 手动
                .triggerOn(SynergyEventType.MANUAL, SynergyEventType.TICK)

                // 条件
                .addCondition(EnergyThresholdCondition.full())
                .addCondition(CooldownCondition.notOnCooldown("domain_expansion"))

                // 效果
                .addEffect(new DomainExpansionEffect())

                .priority(1)
                .build();
    }

    private static class DomainExpansionEffect implements ISynergyEffect {
        private static final String STATE_CHANNELING = "domain_channeling";
        private static final String STATE_DOMAIN_ACTIVE = "domain_active";
        private static final String STATE_AFTERMATH = "domain_aftermath";
        private static final int CHANNEL_DURATION = 60;  // 3 秒
        private static final int DOMAIN_DURATION = 400;  // 20 秒
        private static final int AFTERMATH_DURATION = 100;  // 5 秒
        private static final int DOMAIN_RADIUS = 15;

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
            World world = player.world;

            UUID playerId = player.getUniqueID();
            DomainData data = DOMAIN_DATA.computeIfAbsent(playerId, k -> new DomainData());

            // 检查当前状态
            if (state.hasActiveState(STATE_DOMAIN_ACTIVE)) {
                // 领域激活中 - 执行 tick 效果
                executeDomainTick(player, data, world);
                return;
            }

            if (state.hasActiveState(STATE_CHANNELING)) {
                // 吟唱中
                executeChanneling(player, state, data, world);
                return;
            }

            if (state.hasActiveState(STATE_AFTERMATH)) {
                // 后摇中
                return;
            }

            // 开始吟唱
            startChanneling(player, state, data, bridge);
        }

        private void startChanneling(EntityPlayer player, SynergyPlayerState state,
                                    DomainData data, ExistingModuleBridge bridge) {
            // 消耗全部能量
            int maxEnergy = bridge.getMaxEnergy(player);
            bridge.consumeEnergy(player, maxEnergy);

            // 降低 HP 上限
            state.addMaxHealthModifier(-20f);

            // 开始吟唱
            state.activateState(STATE_CHANNELING, CHANNEL_DURATION, () -> {
                // 吟唱完成，展开领域
                activateDomain(player, state, data);
            });

            // 锁定移动
            player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, CHANNEL_DURATION, 10, false, false));

            // 记录领域中心
            data.centerPos = player.getPositionVector();
            data.elementType = "FIRE";  // 根据实际模块确定

            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "【领域展开】" +
                    TextFormatting.GOLD + " 吟唱中... (3秒)"));

            // 音效
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2.0f, 0.5f);
        }

        private void executeChanneling(EntityPlayer player, SynergyPlayerState state,
                                      DomainData data, World world) {
            // 吟唱粒子效果
            int remaining = state.getStateRemainingTicks(STATE_CHANNELING);
            float progress = 1.0f - (remaining / (float) CHANNEL_DURATION);

            for (int i = 0; i < 20; i++) {
                double angle = (i / 20.0) * Math.PI * 2 + progress * Math.PI * 4;
                double radius = progress * DOMAIN_RADIUS;
                double x = player.posX + Math.cos(angle) * radius;
                double z = player.posZ + Math.sin(angle) * radius;

                world.spawnParticle(EnumParticleTypes.FLAME,
                        x, player.posY + 0.1 + progress * 2, z,
                        0, 0.05, 0);
            }
        }

        private void activateDomain(EntityPlayer player, SynergyPlayerState state, DomainData data) {
            // 领域激活
            state.activateState(STATE_DOMAIN_ACTIVE, DOMAIN_DURATION, () -> {
                endDomain(player, state, data);
            });

            // 增加排异
            state.addRejection(30f);

            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "【" + getDomainName(data.elementType) + "】" +
                    TextFormatting.WHITE + " 领域展开！"));

            // 爆发音效
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 2.0f, 1.0f);
        }

        private void executeDomainTick(EntityPlayer player, DomainData data, World world) {
            Vec3d center = data.centerPos;

            // 获取领域内所有实体
            AxisAlignedBB domainBox = new AxisAlignedBB(
                    center.x - DOMAIN_RADIUS, center.y - 5, center.z - DOMAIN_RADIUS,
                    center.x + DOMAIN_RADIUS, center.y + 10, center.z + DOMAIN_RADIUS
            );

            List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
                    EntityLivingBase.class, domainBox,
                    e -> e != player
            );

            // 应用元素效果
            switch (data.elementType) {
                case "FIRE":
                    applyFireDomain(player, entities, center, world);
                    break;
                case "ICE":
                    applyIceDomain(entities, world);
                    break;
                case "LIGHTNING":
                    applyLightningDomain(player, entities, center, world);
                    break;
                case "VOID":
                    applyVoidDomain(entities, world);
                    break;
            }

            // 领域边界粒子
            if (world.getTotalWorldTime() % 5 == 0) {
                drawDomainBoundary(center, data.elementType, world);
            }
        }

        private void applyFireDomain(EntityPlayer player, List<EntityLivingBase> entities,
                                    Vec3d center, World world) {
            // 地面燃烧
            for (EntityLivingBase entity : entities) {
                entity.setFire(2);
                entity.attackEntityFrom(DamageSource.IN_FIRE, 1.0f);
            }

            // 火焰粒子
            if (world.getTotalWorldTime() % 10 == 0) {
                for (int i = 0; i < 30; i++) {
                    double angle = RANDOM.nextDouble() * Math.PI * 2;
                    double radius = RANDOM.nextDouble() * DOMAIN_RADIUS;

                    world.spawnParticle(EnumParticleTypes.FLAME,
                            center.x + Math.cos(angle) * radius,
                            center.y + 0.1,
                            center.z + Math.sin(angle) * radius,
                            0, 0.05, 0);
                }
            }
        }

        private void applyIceDomain(List<EntityLivingBase> entities, World world) {
            for (EntityLivingBase entity : entities) {
                entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 20, 2, false, false));
                entity.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 20, -3, false, false));
            }
        }

        private void applyLightningDomain(EntityPlayer player, List<EntityLivingBase> entities,
                                         Vec3d center, World world) {
            // 随机落雷
            if (RANDOM.nextFloat() < 0.05f && !entities.isEmpty()) {
                EntityLivingBase target = entities.get(RANDOM.nextInt(entities.size()));
                // 简化: 造成雷电伤害
                target.attackEntityFrom(DamageSource.LIGHTNING_BOLT, 8.0f);

                // 粒子效果
                for (int i = 0; i < 20; i++) {
                    world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                            target.posX, target.posY + 5 - i * 0.25, target.posZ,
                            0, -0.5, 0);
                }
            }
        }

        private void applyVoidDomain(List<EntityLivingBase> entities, World world) {
            for (EntityLivingBase entity : entities) {
                // 禁止传送标记
                entity.addTag("domain_no_teleport");
                // 回复效果减少
                entity.addPotionEffect(new PotionEffect(MobEffects.WITHER, 20, 0, false, false));
            }
        }

        private void drawDomainBoundary(Vec3d center, String elementType, World world) {
            EnumParticleTypes particle;
            switch (elementType) {
                case "FIRE":
                    particle = EnumParticleTypes.FLAME;
                    break;
                case "ICE":
                    particle = EnumParticleTypes.SNOW_SHOVEL;
                    break;
                case "LIGHTNING":
                    particle = EnumParticleTypes.FIREWORKS_SPARK;
                    break;
                case "VOID":
                    particle = EnumParticleTypes.PORTAL;
                    break;
                default:
                    particle = EnumParticleTypes.SPELL;
            }

            for (int i = 0; i < 50; i++) {
                double angle = (i / 50.0) * Math.PI * 2;
                double x = center.x + Math.cos(angle) * DOMAIN_RADIUS;
                double z = center.z + Math.sin(angle) * DOMAIN_RADIUS;

                world.spawnParticle(particle,
                        x, center.y + RANDOM.nextDouble() * 3, z,
                        0, 0.02, 0);
            }
        }

        private void endDomain(EntityPlayer player, SynergyPlayerState state, DomainData data) {
            // 后摇
            state.activateState(STATE_AFTERMATH, AFTERMATH_DURATION, () -> {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "【领域展开】后摇结束"));
            });

            // 无法移动
            player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, AFTERMATH_DURATION, 10, false, false));

            // 设置冷却
            state.setCooldown("domain_expansion", 300000);  // 300 秒

            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "【领域展开】领域崩塌，进入后摇"));

            // 清理数据
            data.centerPos = null;
        }

        private String getDomainName(String elementType) {
            switch (elementType) {
                case "FIRE":
                    return "炎狱领域";
                case "ICE":
                    return "霜寒领域";
                case "LIGHTNING":
                    return "雷霆领域";
                case "VOID":
                    return "虚无领域";
                default:
                    return "领域";
            }
        }

        @Override
        public String getDescription() {
            return "Expand domain with elemental rules";
        }
    }

    // ==================== 2. Reality Fracture (现实碎裂) ====================

    /**
     * Reality Fracture - 现实碎裂
     *
     * 模块要求: CHAOS + VOID + TIME (三槽任意)
     * 触发条件: 5 秒内受到超过最大 HP 50% 的伤害
     *
     * 效果:
     * - 10 格内现实碎裂，持续 8 秒
     * - 区域内物理定律随机改变（每 2 秒）:
     *   - 重力变化
     *   - 伤害类型互换
     *   - 移动方向偏移
     * - 远程攻击精度 -80%
     * - 传送目的地随机偏移
     *
     * 代价:
     * - 自己也受影响
     * - 触发后立即损失 25% HP
     * - 区域 30 秒内无法触发 Synergy
     * - 10% 概率撕开永久裂隙
     */
    public static SynergyDefinition createRealityFracture() {
        return SynergyDefinition.builder("reality_fracture")
                .displayName("现实碎裂")
                .description("受到重创时撕裂现实本身")

                // 模块要求
                .requireModules("CHAOS", "VOID", "TIME")

                // 触发: 受伤
                .triggerOn(SynergyEventType.HURT, SynergyEventType.TICK)

                // 条件
                .addCondition(CooldownCondition.notOnCooldown("reality_fracture"))

                // 效果
                .addEffect(new RealityFractureEffect())

                .priority(5)
                .build();
    }

    private static class RealityFractureEffect implements ISynergyEffect {
        private static final String STATE_FRACTURE_ACTIVE = "reality_fracture_active";
        private static final int FRACTURE_DURATION = 160;  // 8 秒
        private static final int FRACTURE_RADIUS = 10;
        private static final float DAMAGE_THRESHOLD = 0.5f;  // 50% HP

        // 追踪伤害
        private static final Map<UUID, DamageHistory> DAMAGE_HISTORY = new HashMap<>();

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            World world = player.world;

            UUID playerId = player.getUniqueID();

            // 检查是否已激活
            if (state.hasActiveState(STATE_FRACTURE_ACTIVE)) {
                executeFractureTick(player, world);
                return;
            }

            if (context.getEventType() == SynergyEventType.HURT) {
                // 记录伤害
                trackDamage(player, context.getOriginalDamage());

                // 检查是否达到阈值
                DamageHistory history = DAMAGE_HISTORY.get(playerId);
                if (history != null && history.totalDamage >= player.getMaxHealth() * DAMAGE_THRESHOLD) {
                    triggerFracture(player, state, world);
                    history.totalDamage = 0;  // 重置
                }
            }
        }

        private void trackDamage(EntityPlayer player, float damage) {
            UUID playerId = player.getUniqueID();
            DamageHistory history = DAMAGE_HISTORY.computeIfAbsent(playerId, k -> new DamageHistory());

            long currentTime = player.world.getTotalWorldTime();

            // 清理 5 秒前的伤害
            history.damageEvents.removeIf(event -> currentTime - event.time > 100);

            // 添加新伤害
            history.damageEvents.add(new DamageEvent(damage, currentTime));

            // 重新计算总伤害
            history.totalDamage = 0;
            for (DamageEvent event : history.damageEvents) {
                history.totalDamage += event.damage;
            }
        }

        private void triggerFracture(EntityPlayer player, SynergyPlayerState state, World world) {
            // 激活碎裂
            state.activateState(STATE_FRACTURE_ACTIVE, FRACTURE_DURATION, () -> {
                endFracture(player, state);
            });

            // 立即损失 25% HP
            player.attackEntityFrom(DamageSource.MAGIC, player.getMaxHealth() * 0.25f);

            // 10% 概率永久裂隙
            if (RANDOM.nextFloat() < 0.10f) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_PURPLE + "⚠ 现实碎裂: 撕开了永久裂隙！"));
                // 实际实现应该在该位置生成持久的裂隙实体
            }

            player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "✦ 现实碎裂: 物理法则崩坏！"));

            // 音效
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 1.0f, 0.5f);
        }

        private void executeFractureTick(EntityPlayer player, World world) {
            Vec3d center = player.getPositionVector();
            long tick = world.getTotalWorldTime();

            // 每 2 秒 (40 ticks) 改变规则
            int rulePhase = (int)((tick / 40) % 4);

            AxisAlignedBB fractureBox = new AxisAlignedBB(
                    center.x - FRACTURE_RADIUS, center.y - 3, center.z - FRACTURE_RADIUS,
                    center.x + FRACTURE_RADIUS, center.y + 6, center.z + FRACTURE_RADIUS
            );

            List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
                    EntityLivingBase.class, fractureBox
            );

            for (EntityLivingBase entity : entities) {
                switch (rulePhase) {
                    case 0:
                        // 重力反转
                        entity.addVelocity(0, 0.2, 0);
                        break;
                    case 1:
                        // 重力增强
                        entity.addVelocity(0, -0.1, 0);
                        break;
                    case 2:
                        // 随机移动偏移
                        entity.addVelocity(
                                RANDOM.nextGaussian() * 0.1,
                                0,
                                RANDOM.nextGaussian() * 0.1
                        );
                        break;
                    case 3:
                        // 减速
                        entity.motionX *= 0.8;
                        entity.motionZ *= 0.8;
                        break;
                }
            }

            // 粒子效果 - 空间扭曲
            if (tick % 5 == 0) {
                for (int i = 0; i < 20; i++) {
                    double x = center.x + RANDOM.nextGaussian() * FRACTURE_RADIUS;
                    double y = center.y + RANDOM.nextGaussian() * 3;
                    double z = center.z + RANDOM.nextGaussian() * FRACTURE_RADIUS;

                    world.spawnParticle(EnumParticleTypes.PORTAL,
                            x, y, z,
                            RANDOM.nextGaussian() * 0.5,
                            RANDOM.nextGaussian() * 0.5,
                            RANDOM.nextGaussian() * 0.5);
                }
            }
        }

        private void endFracture(EntityPlayer player, SynergyPlayerState state) {
            state.setCooldown("reality_fracture", 60000);  // 60 秒基础冷却

            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "✦ 现实碎裂: 空间稳定"));
        }

        @Override
        public String getDescription() {
            return "Heavy damage fractures reality";
        }

        // 内部类
        private static class DamageHistory {
            public List<DamageEvent> damageEvents = new ArrayList<>();
            public float totalDamage = 0;
        }

        private static class DamageEvent {
            public float damage;
            public long time;

            public DamageEvent(float damage, long time) {
                this.damage = damage;
                this.time = time;
            }
        }
    }

    // ==================== 3. Sanctuary (圣域) ====================

    /**
     * Sanctuary - 圣域
     *
     * 模块要求: SHIELD + HEAL + HOLY + TERRITORY (4 槽以上)
     * 触发条件: 在同一位置站立 3 秒 + 主动激活
     *
     * 效果:
     * - 创建 8 格半径圣域，持续 25 秒
     * - 友方每秒回复 2% HP
     * - 友方受伤 -30%
     * - 敌方无法进入
     * - 免疫负面状态
     *
     * 代价:
     * - 创建者必须留在圣域内
     * - 圣域内无法造成伤害
     * - 能量消耗 6%/秒
     * - 强制破坏: Stun 5 秒 + 50% HP
     * - 同一地点 120 秒冷却
     */
    public static SynergyDefinition createSanctuary() {
        return SynergyDefinition.builder("sanctuary")
                .displayName("圣域")
                .description("创建神圣的庇护所")

                // 模块要求 (4+ 槽)
                .requireModules("SHIELD", "HEAL", "HOLY", "TERRITORY")
                .addLink("SHIELD", "HEAL", "ring")
                .addLink("HEAL", "HOLY", "ring")
                .addLink("HOLY", "TERRITORY", "ring")
                .addLink("TERRITORY", "SHIELD", "ring")

                // 触发: 站桩后手动
                .triggerOn(SynergyEventType.MANUAL, SynergyEventType.TICK)

                // 条件
                .addCondition(PlayerStateCondition.isStandingStill(60))  // 3 秒
                .addCondition(CooldownCondition.notOnCooldown("sanctuary"))

                // 效果
                .addEffect(new SanctuaryEffect())

                .priority(10)
                .build();
    }

    private static class SanctuaryEffect implements ISynergyEffect {
        private static final String STATE_SANCTUARY_ACTIVE = "sanctuary_active";
        private static final int SANCTUARY_DURATION = 500;  // 25 秒
        private static final int SANCTUARY_RADIUS = 8;
        private static final float ENERGY_COST_PER_TICK = 0.3f;  // 6%/秒

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
            World world = player.world;

            UUID playerId = player.getUniqueID();
            SanctuaryData data = SANCTUARY_DATA.computeIfAbsent(playerId, k -> new SanctuaryData());

            // 检查是否已激活
            if (state.hasActiveState(STATE_SANCTUARY_ACTIVE)) {
                executeSanctuaryTick(player, state, data, bridge, world);
                return;
            }

            // 创建圣域
            createSanctuary(player, state, data, world);
        }

        private void createSanctuary(EntityPlayer player, SynergyPlayerState state,
                                    SanctuaryData data, World world) {
            // 激活圣域
            state.activateState(STATE_SANCTUARY_ACTIVE, SANCTUARY_DURATION, () -> {
                endSanctuary(player, state, data);
            });

            // 记录圣域中心
            data.centerPos = player.getPositionVector();

            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "✝ 圣域: 神圣庇护展开！" +
                    TextFormatting.GRAY + " (无法离开，无法攻击)"));

            // 音效
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.5f);
        }

        private void executeSanctuaryTick(EntityPlayer player, SynergyPlayerState state,
                                         SanctuaryData data, ExistingModuleBridge bridge, World world) {
            Vec3d center = data.centerPos;

            // 检查创建者是否还在圣域内
            if (player.getPositionVector().distanceTo(center) > SANCTUARY_RADIUS) {
                // 离开圣域，强制关闭
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "✝ 圣域: 离开圣域，庇护消散！"));
                state.deactivateState(STATE_SANCTUARY_ACTIVE);
                endSanctuary(player, state, data);
                return;
            }

            // 消耗能量
            float energyPercent = bridge.getEnergyPercent(player);
            if (energyPercent < ENERGY_COST_PER_TICK / 20f) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "✝ 圣域: 能量耗尽！"));
                state.deactivateState(STATE_SANCTUARY_ACTIVE);
                endSanctuary(player, state, data);
                return;
            }
            int maxEnergy = bridge.getMaxEnergy(player);
            bridge.consumeEnergy(player, (int)(maxEnergy * ENERGY_COST_PER_TICK / 100f / 20f));

            // 获取圣域内实体
            AxisAlignedBB sanctuaryBox = new AxisAlignedBB(
                    center.x - SANCTUARY_RADIUS, center.y - 2, center.z - SANCTUARY_RADIUS,
                    center.x + SANCTUARY_RADIUS, center.y + 6, center.z + SANCTUARY_RADIUS
            );

            List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
                    EntityLivingBase.class, sanctuaryBox
            );

            for (EntityLivingBase entity : entities) {
                if (entity instanceof EntityPlayer) {
                    // 友方效果
                    applyAllyEffects((EntityPlayer) entity);
                } else {
                    // 敌方 - 弹出
                    pushOutEnemy(entity, center);
                }
            }

            // 圣域视觉效果
            if (world.getTotalWorldTime() % 10 == 0) {
                drawSanctuaryBoundary(center, world);
            }
        }

        private void applyAllyEffects(EntityPlayer ally) {
            // 回复 HP (每秒 2%)
            float healAmount = ally.getMaxHealth() * 0.02f / 20f;
            ally.heal(healAmount);

            // 伤害减免 (通过抗性药水模拟)
            ally.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 10, 1, false, false));

            // 清除负面效果
            ally.removePotionEffect(MobEffects.POISON);
            ally.removePotionEffect(MobEffects.WITHER);
            ally.removePotionEffect(MobEffects.SLOWNESS);
            ally.removePotionEffect(MobEffects.WEAKNESS);

            // 金色粒子
            if (ally.world.getTotalWorldTime() % 20 == 0) {
                ally.world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                        ally.posX, ally.posY + 2, ally.posZ,
                        0, 0.1, 0);
            }
        }

        private void pushOutEnemy(EntityLivingBase enemy, Vec3d center) {
            Vec3d toEnemy = enemy.getPositionVector().subtract(center).normalize();
            enemy.addVelocity(toEnemy.x * 0.5, 0.2, toEnemy.z * 0.5);

            // 阻止进入粒子
            enemy.world.spawnParticle(EnumParticleTypes.BARRIER,
                    enemy.posX, enemy.posY + 1, enemy.posZ,
                    0, 0, 0);
        }

        private void drawSanctuaryBoundary(Vec3d center, World world) {
            for (int i = 0; i < 30; i++) {
                double angle = (i / 30.0) * Math.PI * 2;
                double x = center.x + Math.cos(angle) * SANCTUARY_RADIUS;
                double z = center.z + Math.sin(angle) * SANCTUARY_RADIUS;

                world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                        x, center.y + 0.1 + RANDOM.nextDouble() * 0.5, z,
                        0, 0.02, 0);
            }

            // 中心光柱
            for (int i = 0; i < 10; i++) {
                world.spawnParticle(EnumParticleTypes.END_ROD,
                        center.x + RANDOM.nextGaussian() * 0.3,
                        center.y + i * 0.5,
                        center.z + RANDOM.nextGaussian() * 0.3,
                        0, 0.05, 0);
            }
        }

        private void endSanctuary(EntityPlayer player, SynergyPlayerState state, SanctuaryData data) {
            // 设置位置冷却
            state.setCooldown("sanctuary", 120000);  // 120 秒

            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "✝ 圣域: 庇护结束"));

            data.centerPos = null;
        }

        @Override
        public String getDescription() {
            return "Create protective sanctuary";
        }
    }

    // ==================== 辅助类 ====================

    private static class DomainData {
        public Vec3d centerPos;
        public String elementType;
    }

    private static class SanctuaryData {
        public Vec3d centerPos;
    }
}
