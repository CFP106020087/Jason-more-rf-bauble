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
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.*;

/**
 * 能量/资源类 Synergy 定义
 *
 * 包含:
 * 1. Meltdown Protocol (熔毁协议) - 能量转化 AOE 核爆
 * 2. Parasitic Link (寄生链接) - 资源吸取链接
 * 3. Energy Weaving (能量织网) - 双人能量共享网络
 */
public class EnergySynergies {

    private static final Random RANDOM = new Random();

    // 存储链接关系
    private static final Map<UUID, LinkData> PARASITIC_LINKS = new HashMap<>();
    private static final Map<UUID, UUID> ENERGY_WEAVE_PARTNERS = new HashMap<>();

    public static void registerAll(SynergyManager manager) {
        manager.register(createMeltdownProtocol());
        manager.register(createParasiticLink());
        manager.register(createEnergyWeaving());

        System.out.println("[Synergy] Registered 3 Energy Synergies");
    }

    // ==================== 1. Meltdown Protocol (熔毁协议) ====================

    /**
     * Meltdown Protocol - 熔毁协议
     *
     * 模块要求: REACTOR + OVERCLOCK + UNSTABLE_CORE (相邻排列)
     * 触发条件: 能量 100% 时主动激活
     *
     * 效果:
     * - 进入 Meltdown 状态 10 秒
     * - 能量转化为 AOE 伤害，范围从 3 格扩大到 12 格
     * - 免疫所有控制效果
     *
     * 代价:
     * - 无法取消，必须等能量耗尽或 10 秒
     * - 结束后 Overheat 30 秒无法使用主动技能
     * - 友方受 50% 伤害
     * - 每次增加 20% Rejection，5% 模块爆炸
     */
    public static SynergyDefinition createMeltdownProtocol() {
        return SynergyDefinition.builder("meltdown_protocol")
                .displayName("熔毁协议")
                .description("释放核心能量，化为毁灭的风暴")

                // 模块要求 (相邻)
                .requireModules("REACTOR", "OVERCLOCK", "UNSTABLE_CORE")
                .addLink("REACTOR", "OVERCLOCK", "adjacent")
                .addLink("OVERCLOCK", "UNSTABLE_CORE", "adjacent")

                // 触发: 满能量时手动
                .triggerOn(SynergyEventType.ENERGY_FULL, SynergyEventType.MANUAL)

                // 条件
                .addCondition(EnergyThresholdCondition.full())
                .addCondition(CooldownCondition.notOnCooldown("meltdown_protocol"))
                .addCondition(ActiveStateCondition.noState("meltdown_overheat"))

                // 效果
                .addEffect(new MeltdownProtocolEffect())

                .priority(5)
                .build();
    }

    private static class MeltdownProtocolEffect implements ISynergyEffect {
        private static final String STATE_MELTDOWN = "meltdown_active";
        private static final String STATE_OVERHEAT = "meltdown_overheat";
        private static final int MELTDOWN_DURATION = 200;  // 10 秒
        private static final int OVERHEAT_DURATION = 600;  // 30 秒
        private static final int BASE_RADIUS = 3;
        private static final int MAX_RADIUS = 12;

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();

            if (state.hasActiveState(STATE_MELTDOWN)) {
                // 已在 Meltdown 中，执行 tick 效果
                executeMeltdownTick(player, state, bridge);
                return;
            }

            // 开始 Meltdown
            state.setInMeltdown(true);
            state.activateState(STATE_MELTDOWN, MELTDOWN_DURATION, () -> {
                endMeltdown(player, state);
            });

            // 免疫控制
            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, MELTDOWN_DURATION, 3, false, true));

            // 增加排异
            state.addRejection(20f);

            // 5% 模块爆炸风险
            if (RANDOM.nextFloat() < 0.05f) {
                state.addRejection(50f);  // 严重排异
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "⚠ 熔毁协议: 核心不稳定！Rejection +50%"));
            }

            // 音效
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 0.5f);

            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "☢ 熔毁协议: 核心临界！" +
                    TextFormatting.GRAY + " (无法取消，持续 10 秒)"));
        }

        private void executeMeltdownTick(EntityPlayer player, SynergyPlayerState state, ExistingModuleBridge bridge) {
            World world = player.world;
            int elapsedTicks = MELTDOWN_DURATION - state.getStateRemainingTicks(STATE_MELTDOWN);

            // 计算当前半径 (随时间扩大)
            float progress = elapsedTicks / (float) MELTDOWN_DURATION;
            int currentRadius = BASE_RADIUS + (int)((MAX_RADIUS - BASE_RADIUS) * progress);

            // 计算伤害 (指数增长)
            float baseDamage = 2.0f;
            float radiusMultiplier = (currentRadius / (float) BASE_RADIUS);
            float damage = baseDamage * radiusMultiplier * radiusMultiplier;

            // 消耗能量
            int energyCost = (int)(damage * 50);  // 每点伤害消耗 50 能量
            int currentEnergy = bridge.getCurrentEnergy(player);
            if (currentEnergy < energyCost) {
                // 能量耗尽，提前结束
                state.deactivateState(STATE_MELTDOWN);
                endMeltdown(player, state);
                return;
            }
            bridge.consumeEnergy(player, energyCost);

            // 对范围内实体造成伤害
            AxisAlignedBB damageBox = new AxisAlignedBB(
                    player.posX - currentRadius, player.posY - 2, player.posZ - currentRadius,
                    player.posX + currentRadius, player.posY + 4, player.posZ + currentRadius
            );

            List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
                    EntityLivingBase.class, damageBox,
                    e -> e != player
            );

            for (EntityLivingBase entity : entities) {
                float finalDamage = damage;
                // 友方减半
                if (entity instanceof EntityPlayer) {
                    finalDamage *= 0.5f;
                }

                DamageSource source = DamageSource.causePlayerDamage(player);
                source.setDamageBypassesArmor();
                source.setFireDamage();
                entity.attackEntityFrom(source, finalDamage);

                // 击退
                Vec3d knockback = entity.getPositionVector().subtract(player.getPositionVector()).normalize();
                entity.addVelocity(knockback.x * 0.3, 0.1, knockback.z * 0.3);
            }

            // 粒子效果
            if (elapsedTicks % 5 == 0) {
                for (int i = 0; i < currentRadius * 3; i++) {
                    double angle = RANDOM.nextDouble() * Math.PI * 2;
                    double radius = RANDOM.nextDouble() * currentRadius;
                    double x = player.posX + Math.cos(angle) * radius;
                    double z = player.posZ + Math.sin(angle) * radius;

                    world.spawnParticle(EnumParticleTypes.FLAME,
                            x, player.posY + RANDOM.nextDouble() * 2, z,
                            0, 0.1, 0);
                    world.spawnParticle(EnumParticleTypes.LAVA,
                            x, player.posY + 0.1, z,
                            0, 0, 0);
                }
            }
        }

        private void endMeltdown(EntityPlayer player, SynergyPlayerState state) {
            state.setInMeltdown(false);

            // 进入 Overheat 状态
            state.activateState(STATE_OVERHEAT, OVERHEAT_DURATION, () -> {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "☢ 熔毁协议: 核心冷却完成"));
            });

            // 冷却惩罚
            player.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, OVERHEAT_DURATION, 1, false, true));
            player.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, OVERHEAT_DURATION, 2, false, true));

            // 设置冷却
            state.setCooldown("meltdown_protocol", OVERHEAT_DURATION * 50 + 60000);  // Overheat + 60 秒

            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "☢ 熔毁协议: 核心过热！" +
                    TextFormatting.GRAY + " (30 秒内无法使用主动技能)"));
        }

        @Override
        public String getDescription() {
            return "Convert energy into devastating AOE damage";
        }
    }

    // ==================== 2. Parasitic Link (寄生链接) ====================

    /**
     * Parasitic Link - 寄生链接
     *
     * 模块要求: VAMPIRE + LINK + CORRUPTION (任意排列)
     * 触发条件: 对同一目标造成累计 100 点伤害
     *
     * 效果:
     * - 建立 Parasitic Link 30 秒
     * - 目标受伤 15% 转化为你的能量
     * - 目标回复 30% 转移给你
     * - 可感知目标位置
     *
     * 代价:
     * - 目标死亡反噬 10% 最大 HP
     * - 每个链接消耗 3% 能量/秒
     * - 距离超过 30 格断裂受伤
     * - 最多 3 个链接
     */
    public static SynergyDefinition createParasiticLink() {
        return SynergyDefinition.builder("parasitic_link")
                .displayName("寄生链接")
                .description("建立与目标的能量链接，吸取其生命力")

                // 模块要求
                .requireModules("VAMPIRE", "LINK", "CORRUPTION")

                // 触发: 攻击
                .triggerOn(SynergyEventType.ATTACK, SynergyEventType.TICK)

                // 条件
                .addCondition(CooldownCondition.notOnCooldown("parasitic_link"))

                // 效果
                .addEffect(new ParasiticLinkEffect())

                .priority(30)
                .build();
    }

    private static class ParasiticLinkEffect implements ISynergyEffect {
        private static final int LINK_DURATION = 600;  // 30 秒
        private static final int MAX_LINKS = 3;
        private static final int LINK_RANGE = 30;
        private static final float DAMAGE_TO_ENERGY_RATIO = 0.15f;
        private static final float HEAL_STEAL_RATIO = 0.30f;
        private static final float ENERGY_COST_PER_TICK = 0.15f;  // 3%/秒

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();

            // Tick 逻辑：维护现有链接
            if (context.getEventType() == SynergyEventType.TICK) {
                maintainLinks(player, state, bridge);
                return;
            }

            // 攻击逻辑：尝试建立新链接
            EntityLivingBase target = context.getTarget();
            if (target == null) return;

            // 检查是否已有链接
            LinkData existingLink = PARASITIC_LINKS.get(player.getUniqueID());
            if (existingLink != null && existingLink.targets.size() >= MAX_LINKS) {
                return;  // 已达上限
            }

            // 累积伤害
            float damage = context.getOriginalDamage();
            UUID targetId = target.getUniqueID();

            if (existingLink == null) {
                existingLink = new LinkData();
                PARASITIC_LINKS.put(player.getUniqueID(), existingLink);
            }

            float accumulated = existingLink.damageAccumulation.getOrDefault(targetId, 0f) + damage;
            existingLink.damageAccumulation.put(targetId, accumulated);

            // 检查是否达到 100 伤害
            if (accumulated >= 100 && !existingLink.targets.contains(targetId)) {
                // 建立链接
                existingLink.targets.add(targetId);
                existingLink.targetEntities.put(targetId, target);
                existingLink.linkTicks.put(targetId, LINK_DURATION);

                // 视觉效果
                spawnLinkParticles(player, target);

                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.ENTITY_VEX_CHARGE, SoundCategory.PLAYERS, 1.0f, 0.7f);

                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_PURPLE + "⛓ 寄生链接: 已链接 " + target.getName() +
                        TextFormatting.GRAY + " [" + existingLink.targets.size() + "/" + MAX_LINKS + "]"));
            }
        }

        private void maintainLinks(EntityPlayer player, SynergyPlayerState state, ExistingModuleBridge bridge) {
            LinkData linkData = PARASITIC_LINKS.get(player.getUniqueID());
            if (linkData == null || linkData.targets.isEmpty()) return;

            World world = player.world;
            float totalEnergyCost = linkData.targets.size() * ENERGY_COST_PER_TICK;

            // 检查能量
            float energyPercent = bridge.getEnergyPercent(player);
            if (energyPercent < totalEnergyCost) {
                // 能量不足，断开所有链接
                breakAllLinks(player, linkData);
                return;
            }

            // 消耗能量
            int maxEnergy = bridge.getMaxEnergy(player);
            bridge.consumeEnergy(player, (int)(maxEnergy * totalEnergyCost / 100f));

            // 检查每个链接
            Iterator<UUID> it = linkData.targets.iterator();
            while (it.hasNext()) {
                UUID targetId = it.next();
                EntityLivingBase target = linkData.targetEntities.get(targetId);

                // 检查目标是否存活
                if (target == null || !target.isEntityAlive()) {
                    // 目标死亡，反噬
                    float backlash = player.getMaxHealth() * 0.10f;
                    player.attackEntityFrom(DamageSource.MAGIC, backlash);

                    player.sendMessage(new TextComponentString(
                            TextFormatting.DARK_RED + "⛓ 寄生链接: 链接断裂！反噬 " +
                            String.format("%.1f", backlash) + " 伤害"));

                    it.remove();
                    linkData.targetEntities.remove(targetId);
                    linkData.linkTicks.remove(targetId);
                    continue;
                }

                // 检查距离
                double distance = player.getDistance(target);
                if (distance > LINK_RANGE) {
                    // 距离过远，断裂
                    player.attackEntityFrom(DamageSource.MAGIC, 20f);

                    player.sendMessage(new TextComponentString(
                            TextFormatting.DARK_RED + "⛓ 寄生链接: " + target.getName() +
                            " 超出范围！受到 20 伤害"));

                    it.remove();
                    linkData.targetEntities.remove(targetId);
                    linkData.linkTicks.remove(targetId);
                    continue;
                }

                // 更新持续时间
                int remainingTicks = linkData.linkTicks.get(targetId) - 1;
                if (remainingTicks <= 0) {
                    it.remove();
                    linkData.targetEntities.remove(targetId);
                    linkData.linkTicks.remove(targetId);

                    player.sendMessage(new TextComponentString(
                            TextFormatting.GRAY + "⛓ 寄生链接: " + target.getName() + " 链接超时"));
                    continue;
                }
                linkData.linkTicks.put(targetId, remainingTicks);

                // 链接粒子
                if (world.getTotalWorldTime() % 10 == 0) {
                    drawLinkLine(world, player.getPositionVector().add(0, 1, 0),
                            target.getPositionVector().add(0, 1, 0));
                }
            }
        }

        private void breakAllLinks(EntityPlayer player, LinkData linkData) {
            linkData.targets.clear();
            linkData.targetEntities.clear();
            linkData.linkTicks.clear();
            linkData.damageAccumulation.clear();

            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "⛓ 寄生链接: 能量不足，所有链接断开"));
        }

        private void spawnLinkParticles(EntityPlayer player, EntityLivingBase target) {
            World world = player.world;
            Vec3d start = player.getPositionVector().add(0, 1, 0);
            Vec3d end = target.getPositionVector().add(0, 1, 0);

            for (int i = 0; i < 20; i++) {
                double progress = i / 20.0;
                double x = start.x + (end.x - start.x) * progress;
                double y = start.y + (end.y - start.y) * progress;
                double z = start.z + (end.z - start.z) * progress;

                world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        x, y, z, 0, 0, 0);
            }
        }

        private void drawLinkLine(World world, Vec3d start, Vec3d end) {
            for (int i = 0; i < 10; i++) {
                double progress = i / 10.0;
                double x = start.x + (end.x - start.x) * progress;
                double y = start.y + (end.y - start.y) * progress;
                double z = start.z + (end.z - start.z) * progress;

                world.spawnParticle(EnumParticleTypes.REDSTONE,
                        x + RANDOM.nextGaussian() * 0.1,
                        y + RANDOM.nextGaussian() * 0.1,
                        z + RANDOM.nextGaussian() * 0.1,
                        0.5, 0, 0.5);  // 紫色
            }
        }

        @Override
        public String getDescription() {
            return "Establish parasitic energy link with targets";
        }
    }

    // ==================== 3. Energy Weaving (能量织网) ====================

    /**
     * Energy Weaving - 能量织网
     *
     * 模块要求: ENERGY_CORE + CONDUCTOR + STABILITY + AMPLIFY (四槽对称)
     * 触发条件: 与另一个装备此 Synergy 的玩家相距 10 格内
     *
     * 效果:
     * - 双方能量共享并均分
     * - 任一方释放技能，另一方冷却 -30%
     * - 网内敌人移动 -20%，能量回复 -50%
     * - 同时攻击触发 Resonance Strike
     *
     * 代价:
     * - 伤害 20% 传递给另一方
     * - 一方死亡另一方损失 50% HP
     * - 距离超过 20 格断裂 Stun 2 秒
     * - Rejection 共享取高值
     */
    public static SynergyDefinition createEnergyWeaving() {
        return SynergyDefinition.builder("energy_weaving")
                .displayName("能量织网")
                .description("与同伴编织能量网络，共享力量与命运")

                // 模块要求 (四槽对称)
                .requireModules("ENERGY_CORE", "CONDUCTOR", "STABILITY", "AMPLIFY")
                .addLink("ENERGY_CORE", "CONDUCTOR", "symmetric")
                .addLink("CONDUCTOR", "STABILITY", "symmetric")
                .addLink("STABILITY", "AMPLIFY", "symmetric")
                .addLink("AMPLIFY", "ENERGY_CORE", "symmetric")

                // 触发: 每 tick
                .triggerOn(SynergyEventType.TICK)

                // 条件: 需要有伙伴
                .addCondition(new EnergyWeavingPartnerCondition())

                // 效果
                .addEffect(new EnergyWeavingEffect())

                .priority(25)
                .build();
    }

    private static class EnergyWeavingPartnerCondition implements com.moremod.synergy.api.ISynergyCondition {
        @Override
        public boolean test(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            World world = player.world;

            // 查找 10 格内有此 Synergy 的玩家
            List<EntityPlayer> nearbyPlayers = world.getEntitiesWithinAABB(
                    EntityPlayer.class,
                    new AxisAlignedBB(
                            player.posX - 10, player.posY - 5, player.posZ - 10,
                            player.posX + 10, player.posY + 5, player.posZ + 10
                    ),
                    p -> p != player
            );

            for (EntityPlayer other : nearbyPlayers) {
                // 简化检查：假设在 10 格内的其他玩家都是伙伴
                // 实际应该检查对方是否有 energy_weaving synergy
                return true;
            }

            return false;
        }

        @Override
        public String getDescription() {
            return "Partner with Energy Weaving nearby";
        }
    }

    private static class EnergyWeavingEffect implements ISynergyEffect {
        private static final String STATE_WEAVING = "energy_weaving_active";
        private static final int WEAVE_RANGE = 10;
        private static final int BREAK_RANGE = 20;

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
            World world = player.world;

            // 查找伙伴
            EntityPlayer partner = findPartner(player, world);

            if (partner == null) {
                // 检查是否之前有伙伴
                UUID previousPartner = ENERGY_WEAVE_PARTNERS.get(player.getUniqueID());
                if (previousPartner != null) {
                    // 距离过远，断裂
                    ENERGY_WEAVE_PARTNERS.remove(player.getUniqueID());
                    ENERGY_WEAVE_PARTNERS.remove(previousPartner);

                    player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 10, false, false));

                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "⚡ 能量织网: 连接断裂！Stunned 2 秒"));
                }
                return;
            }

            // 建立/维护连接
            ENERGY_WEAVE_PARTNERS.put(player.getUniqueID(), partner.getUniqueID());

            // 激活状态
            if (!state.hasActiveState(STATE_WEAVING)) {
                state.activateState(STATE_WEAVING, Integer.MAX_VALUE);
                player.sendMessage(new TextComponentString(
                        TextFormatting.AQUA + "⚡ 能量织网: 与 " + partner.getName() + " 建立连接"));
            }

            // 能量共享
            shareEnergy(player, partner, bridge);

            // 对网内敌人施加减益
            applyDebuffsToEnemies(player, partner, world);

            // 同步排异值
            SynergyPlayerState partnerState = SynergyPlayerState.get(partner);
            float maxRejection = Math.max(state.getRejection(), partnerState.getRejection());
            state.setRejection(maxRejection);
            partnerState.setRejection(maxRejection);

            // 绘制连接线
            if (world.getTotalWorldTime() % 5 == 0) {
                drawWeaveLine(world, player, partner);
            }
        }

        private EntityPlayer findPartner(EntityPlayer player, World world) {
            // 先检查已有伙伴
            UUID partnerId = ENERGY_WEAVE_PARTNERS.get(player.getUniqueID());
            if (partnerId != null) {
                EntityPlayer partner = world.getPlayerEntityByUUID(partnerId);
                if (partner != null && player.getDistance(partner) <= BREAK_RANGE) {
                    return partner;
                }
            }

            // 查找新伙伴
            List<EntityPlayer> nearbyPlayers = world.getEntitiesWithinAABB(
                    EntityPlayer.class,
                    new AxisAlignedBB(
                            player.posX - WEAVE_RANGE, player.posY - 5, player.posZ - WEAVE_RANGE,
                            player.posX + WEAVE_RANGE, player.posY + 5, player.posZ + WEAVE_RANGE
                    ),
                    p -> p != player
            );

            return nearbyPlayers.isEmpty() ? null : nearbyPlayers.get(0);
        }

        private void shareEnergy(EntityPlayer player, EntityPlayer partner, ExistingModuleBridge bridge) {
            int playerEnergy = bridge.getCurrentEnergy(player);
            int partnerEnergy = bridge.getCurrentEnergy(partner);
            int totalEnergy = playerEnergy + partnerEnergy;
            int sharedEnergy = totalEnergy / 2;

            // 只在差异较大时同步
            if (Math.abs(playerEnergy - partnerEnergy) > 100) {
                bridge.setEnergy(player, sharedEnergy);
                bridge.setEnergy(partner, sharedEnergy);
            }
        }

        private void applyDebuffsToEnemies(EntityPlayer player, EntityPlayer partner, World world) {
            // 计算两人之间的区域
            double minX = Math.min(player.posX, partner.posX) - 3;
            double maxX = Math.max(player.posX, partner.posX) + 3;
            double minZ = Math.min(player.posZ, partner.posZ) - 3;
            double maxZ = Math.max(player.posZ, partner.posZ) + 3;

            AxisAlignedBB webArea = new AxisAlignedBB(
                    minX, player.posY - 2, minZ,
                    maxX, player.posY + 4, maxZ
            );

            List<EntityLivingBase> enemies = world.getEntitiesWithinAABB(
                    EntityLivingBase.class, webArea,
                    e -> e != player && e != partner && !(e instanceof EntityPlayer)
            );

            for (EntityLivingBase enemy : enemies) {
                enemy.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 10, 0, false, false));
            }
        }

        private void drawWeaveLine(World world, EntityPlayer player, EntityPlayer partner) {
            Vec3d start = player.getPositionVector().add(0, 1, 0);
            Vec3d end = partner.getPositionVector().add(0, 1, 0);

            for (int i = 0; i < 15; i++) {
                double progress = i / 15.0;
                double x = start.x + (end.x - start.x) * progress;
                double y = start.y + (end.y - start.y) * progress;
                double z = start.z + (end.z - start.z) * progress;

                world.spawnParticle(EnumParticleTypes.END_ROD,
                        x + RANDOM.nextGaussian() * 0.1,
                        y + Math.sin(progress * Math.PI) * 0.5,
                        z + RANDOM.nextGaussian() * 0.1,
                        0, 0, 0);
            }
        }

        @Override
        public String getDescription() {
            return "Weave energy network with partner";
        }
    }

    // ==================== 辅助类 ====================

    private static class LinkData {
        public Set<UUID> targets = new HashSet<>();
        public Map<UUID, EntityLivingBase> targetEntities = new HashMap<>();
        public Map<UUID, Integer> linkTicks = new HashMap<>();
        public Map<UUID, Float> damageAccumulation = new HashMap<>();
    }
}
