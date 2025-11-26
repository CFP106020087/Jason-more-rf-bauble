package com.moremod.synergy.synergies;

import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.synergy.bridge.ExistingModuleBridge;
import com.moremod.synergy.condition.*;
import com.moremod.synergy.core.*;
import com.moremod.synergy.effect.*;
import net.minecraft.entity.Entity;
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

import java.util.List;
import java.util.Random;

/**
 * 空间/维度类 Synergy 定义
 *
 * 包含:
 * 1. Rift Walker (裂隙行者) - 相位闪避 + 随机传送裂隙
 * 2. Gravity Anchor (重力锚点) - 固定站桩的控场重力场
 * 3. Dimensional Pocket (维度口袋) - 1v1 强制决斗维度
 */
public class SpatialSynergies {

    private static final Random RANDOM = new Random();

    public static void registerAll(SynergyManager manager) {
        manager.register(createRiftWalker());
        manager.register(createGravityAnchor());
        manager.register(createDimensionalPocket());

        System.out.println("[Synergy] Registered 3 Spatial Synergies");
    }

    // ==================== 1. Rift Walker (裂隙行者) ====================

    /**
     * Rift Walker - 裂隙行者
     *
     * 模块要求: TELEPORT + PHASE + ENERGY_CORE (三角形排列)
     * 触发条件: 能量 >= 80% 时闪避（潜行 + 移动）
     *
     * 效果:
     * - 进入 0.8 秒相位状态（无碰撞、无敌、穿墙）
     * - 相位结束时撕开裂隙，3 秒内经过的实体随机传送 15 格
     *
     * 代价:
     * - 每次消耗 25% 最大能量
     * - 相位后 2 秒无法回能
     * - 连续 3 次后触发 Spatial Rejection
     */
    public static SynergyDefinition createRiftWalker() {
        return SynergyDefinition.builder("rift_walker")
                .displayName("裂隙行者")
                .description("相位闪避穿越现实，在身后撕开随机传送的裂隙")

                // 模块要求 (三角形)
                .requireModules("TELEPORT", "PHASE", "ENERGY_CORE")
                .addLink("TELEPORT", "PHASE", "triangle")
                .addLink("PHASE", "ENERGY_CORE", "triangle")
                .addLink("ENERGY_CORE", "TELEPORT", "triangle")

                // 触发: 潜行时的 tick
                .triggerOn(SynergyEventType.DODGE, SynergyEventType.SNEAK)

                // 条件
                .addCondition(EnergyThresholdCondition.atLeast(80f))
                .addCondition(CooldownCondition.notOnCooldown("rift_walker"))
                .addCondition(PlayerStateCondition.isSneaking())

                // 效果
                .addEffect(new RiftWalkerEffect())

                .priority(10)
                .build();
    }

    private static class RiftWalkerEffect implements ISynergyEffect {
        private static final String STATE_PHASE = "rift_walker_phase";
        private static final String STATE_NO_REGEN = "rift_walker_no_regen";
        private static final int PHASE_DURATION = 16;  // 0.8 秒
        private static final int NO_REGEN_DURATION = 40;  // 2 秒
        private static final int RIFT_DURATION = 60;  // 3 秒

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();

            // 检查连续使用次数
            int useCount = (int) state.getTempModifier("rift_walker_uses", 0f);

            // 消耗能量 (25%)
            int maxEnergy = bridge.getMaxEnergy(player);
            bridge.consumeEnergy(player, (int)(maxEnergy * 0.25f));

            // 记录原始位置
            Vec3d startPos = player.getPositionVector();

            // 激活相位状态
            state.setInPhaseState(true);
            state.activateState(STATE_PHASE, PHASE_DURATION, () -> {
                // 相位结束回调
                state.setInPhaseState(false);

                // 创建裂隙效果
                createRift(player, startPos);

                // 激活无法回能状态
                state.activateState(STATE_NO_REGEN, NO_REGEN_DURATION);
            });

            // 给予无敌和相位效果
            player.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, PHASE_DURATION, 0, false, false));
            player.setNoGravity(true);
            player.noClip = true;

            // 更新使用次数
            useCount++;
            state.setTempModifier("rift_walker_uses", useCount);

            // 检查 Spatial Rejection
            if (useCount >= 3) {
                state.addRejection(30f);
                state.setTempModifier("rift_walker_uses", 0f);
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_PURPLE + "⚠ Spatial Rejection: 下次传送有 30% 概率失控"));
            }

            // 设置冷却
            state.setCooldown("rift_walker", 2000);  // 2 秒基础冷却

            // 视觉效果
            spawnPhaseParticles(player);
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.5f);

            player.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "◈ 裂隙行者: 相位状态激活"));
        }

        private void createRift(EntityPlayer player, Vec3d position) {
            World world = player.world;

            // 恢复正常状态
            player.setNoGravity(false);
            player.noClip = false;

            // 裂隙存在 3 秒，每 tick 检查经过的实体
            // 这里简化实现，实际应该用调度器
            AxisAlignedBB riftBox = new AxisAlignedBB(
                    position.x - 1, position.y - 1, position.z - 1,
                    position.x + 1, position.y + 3, position.z + 1
            );

            // 获取裂隙范围内的实体
            List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, riftBox);
            for (Entity entity : entities) {
                if (entity != player && entity instanceof EntityLivingBase) {
                    // 随机传送
                    double angle = RANDOM.nextDouble() * Math.PI * 2;
                    double distance = 5 + RANDOM.nextDouble() * 10;  // 5-15 格
                    double newX = entity.posX + Math.cos(angle) * distance;
                    double newZ = entity.posZ + Math.sin(angle) * distance;

                    entity.setPositionAndUpdate(newX, entity.posY, newZ);

                    // 粒子效果
                    for (int i = 0; i < 20; i++) {
                        world.spawnParticle(EnumParticleTypes.PORTAL,
                                entity.posX, entity.posY + 1, entity.posZ,
                                RANDOM.nextGaussian() * 0.5,
                                RANDOM.nextGaussian() * 0.5,
                                RANDOM.nextGaussian() * 0.5);
                    }
                }
            }

            // 裂隙视觉效果
            for (int i = 0; i < 50; i++) {
                world.spawnParticle(EnumParticleTypes.PORTAL,
                        position.x + RANDOM.nextGaussian() * 0.5,
                        position.y + 1 + RANDOM.nextGaussian() * 1.5,
                        position.z + RANDOM.nextGaussian() * 0.5,
                        0, 0, 0);
            }
        }

        private void spawnPhaseParticles(EntityPlayer player) {
            World world = player.world;
            for (int i = 0; i < 30; i++) {
                world.spawnParticle(EnumParticleTypes.END_ROD,
                        player.posX + RANDOM.nextGaussian() * 0.5,
                        player.posY + 1 + RANDOM.nextGaussian(),
                        player.posZ + RANDOM.nextGaussian() * 0.5,
                        0, 0.05, 0);
            }
        }

        @Override
        public String getDescription() {
            return "Enter phase state and create teleportation rift";
        }
    }

    // ==================== 2. Gravity Anchor (重力锚点) ====================

    /**
     * Gravity Anchor - 重力锚点
     *
     * 模块要求: GRAVITY + SHIELD + STABILITY (相邻排列)
     * 触发条件: 站立不动 1.5 秒后自动激活
     *
     * 效果:
     * - 8 格半径重力场
     * - 场内抛射物速度 -70%
     * - 场内敌人跳跃减半，无法传送
     * - 自己免疫击退
     *
     * 代价:
     * - 激活期间无法移动
     * - 能量消耗 8%/秒
     * - 移动后 5 秒冷却
     */
    public static SynergyDefinition createGravityAnchor() {
        return SynergyDefinition.builder("gravity_anchor")
                .displayName("重力锚点")
                .description("站桩创建重力场，减缓一切")

                // 模块要求 (相邻)
                .requireModules("GRAVITY", "SHIELD", "STABILITY")
                .addLink("GRAVITY", "SHIELD", "adjacent")
                .addLink("SHIELD", "STABILITY", "adjacent")

                // 触发: 每 tick
                .triggerOn(SynergyEventType.TICK)

                // 条件
                .addCondition(PlayerStateCondition.isStandingStill(30))  // 1.5 秒
                .addCondition(CooldownCondition.notOnCooldown("gravity_anchor"))

                // 效果
                .addEffect(new GravityAnchorEffect())

                .priority(20)
                .build();
    }

    private static class GravityAnchorEffect implements ISynergyEffect {
        private static final String STATE_ANCHOR = "gravity_anchor_active";
        private static final int FIELD_RADIUS = 8;
        private static final float ENERGY_COST_PER_TICK = 0.4f;  // 8%/秒 = 0.4%/tick

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();

            // 检查是否已激活
            boolean isActive = state.hasActiveState(STATE_ANCHOR);

            // 检查能量
            float energyPercent = bridge.getEnergyPercent(player);
            if (energyPercent < ENERGY_COST_PER_TICK) {
                if (isActive) {
                    deactivateAnchor(player, state);
                }
                return;
            }

            // 消耗能量
            int maxEnergy = bridge.getMaxEnergy(player);
            bridge.consumeEnergy(player, (int)(maxEnergy * ENERGY_COST_PER_TICK / 100f));

            // 激活状态
            if (!isActive) {
                state.activateState(STATE_ANCHOR, Integer.MAX_VALUE);
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "⚓ 重力锚点: 重力场展开"));
            }

            // 应用重力场效果
            applyGravityField(player);

            // 给予自己稳定效果
            player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 5, 10, false, false));
            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 5, 2, false, false));

            // 检查是否移动了
            if (!state.isStandingStill(1)) {
                deactivateAnchor(player, state);
            }
        }

        private void applyGravityField(EntityPlayer player) {
            World world = player.world;
            AxisAlignedBB fieldBox = new AxisAlignedBB(
                    player.posX - FIELD_RADIUS, player.posY - 2, player.posZ - FIELD_RADIUS,
                    player.posX + FIELD_RADIUS, player.posY + FIELD_RADIUS, player.posZ + FIELD_RADIUS
            );

            // 影响范围内的实体
            List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
                    EntityLivingBase.class, fieldBox,
                    e -> e != player
            );

            for (EntityLivingBase entity : entities) {
                // 减速效果
                entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 10, 1, false, false));
                entity.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 10, -3, false, false));  // 负数减少跳跃

                // 标记无法传送 (需要在传送逻辑中检查)
                entity.addTag("gravity_anchored");
            }

            // 场内粒子效果
            if (world.getTotalWorldTime() % 5 == 0) {
                for (int i = 0; i < 10; i++) {
                    double angle = RANDOM.nextDouble() * Math.PI * 2;
                    double radius = RANDOM.nextDouble() * FIELD_RADIUS;
                    double x = player.posX + Math.cos(angle) * radius;
                    double z = player.posZ + Math.sin(angle) * radius;

                    world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                            x, player.posY + 0.1, z,
                            0, 0.02, 0);
                }
            }
        }

        private void deactivateAnchor(EntityPlayer player, SynergyPlayerState state) {
            state.deactivateState(STATE_ANCHOR);
            state.setCooldown("gravity_anchor", 5000);  // 5 秒冷却

            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "⚓ 重力锚点: 场域消散"));
        }

        @Override
        public String getDescription() {
            return "Create gravity field when standing still";
        }
    }

    // ==================== 3. Dimensional Pocket (维度口袋) ====================

    /**
     * Dimensional Pocket - 维度口袋
     *
     * 模块要求: VOID + STORAGE + TIME (环形任意三槽)
     * 触发条件: 主动技能 (需要通过其他系统触发)
     *
     * 效果:
     * - 创建私人维度，拉入自己和目标
     * - 维度内时间流速 x2
     * - 15 秒后或一方死亡结束
     *
     * 代价:
     * - 消耗 60% 能量
     * - 维度内死亡掉落所有模块
     * - 180 秒冷却
     */
    public static SynergyDefinition createDimensionalPocket() {
        return SynergyDefinition.builder("dimensional_pocket")
                .displayName("维度口袋")
                .description("将目标拉入私人决斗维度")

                // 模块要求 (环形)
                .requireModules("VOID", "STORAGE", "TIME")
                .addLink("VOID", "STORAGE", "ring")
                .addLink("STORAGE", "TIME", "ring")
                .addLink("TIME", "VOID", "ring")

                // 触发: 手动激活 (击中目标时)
                .triggerOn(SynergyEventType.ATTACK)

                // 条件
                .addCondition(CooldownCondition.notOnCooldown("dimensional_pocket"))
                .addCondition(EnergyThresholdCondition.atLeast(60f))
                .addCondition(PlayerStateCondition.isSneaking())  // 需要潜行 + 攻击

                // 效果
                .addEffect(new DimensionalPocketEffect())

                .priority(5)
                .build();
    }

    private static class DimensionalPocketEffect implements ISynergyEffect {
        private static final String STATE_IN_POCKET = "dimensional_pocket_active";
        private static final int POCKET_DURATION = 300;  // 15 秒

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            EntityLivingBase target = context.getTarget();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();

            if (target == null) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "✖ 维度口袋: 需要目标"));
                return;
            }

            // 消耗能量 (60%)
            int maxEnergy = bridge.getMaxEnergy(player);
            bridge.consumeEnergy(player, (int)(maxEnergy * 0.6f));

            // 记录原始位置
            Vec3d playerOriginalPos = player.getPositionVector();
            Vec3d targetOriginalPos = target.getPositionVector();

            // 激活维度口袋状态
            state.activateState(STATE_IN_POCKET, POCKET_DURATION, () -> {
                // 结束回调 - 返回原位置
                player.setPositionAndUpdate(playerOriginalPos.x, playerOriginalPos.y, playerOriginalPos.z);
                if (target.isEntityAlive()) {
                    target.setPositionAndUpdate(targetOriginalPos.x, targetOriginalPos.y, targetOriginalPos.z);
                }
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_PURPLE + "◈ 维度口袋: 空间崩塌，返回现实"));
            });

            // 传送到「维度空间」(实际上是远离的位置)
            // 真正的维度实现需要自定义维度，这里简化为传送到远方
            double pocketX = player.posX + (RANDOM.nextBoolean() ? 10000 : -10000);
            double pocketZ = player.posZ + (RANDOM.nextBoolean() ? 10000 : -10000);

            // 找到安全的 Y 坐标
            int safeY = player.world.getSeaLevel() + 50;

            // 传送双方
            player.setPositionAndUpdate(pocketX, safeY, pocketZ);
            target.setPositionAndUpdate(pocketX + 5, safeY, pocketZ);

            // 时间加速效果 (模拟)
            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, POCKET_DURATION, 1, false, false));
            player.addPotionEffect(new PotionEffect(MobEffects.HASTE, POCKET_DURATION, 1, false, false));

            // 设置冷却
            state.setCooldown("dimensional_pocket", 180000);  // 180 秒

            // 视觉效果
            spawnDimensionalParticles(player);
            if (target instanceof EntityPlayer) {
                spawnDimensionalParticles((EntityPlayer) target);
            }

            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "◈ 维度口袋: 进入私人空间，持续 15 秒"));

            // 音效
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 1.0f, 0.5f);
        }

        private void spawnDimensionalParticles(EntityPlayer player) {
            World world = player.world;
            for (int i = 0; i < 100; i++) {
                world.spawnParticle(EnumParticleTypes.PORTAL,
                        player.posX + RANDOM.nextGaussian(),
                        player.posY + 1 + RANDOM.nextGaussian() * 2,
                        player.posZ + RANDOM.nextGaussian(),
                        RANDOM.nextGaussian() * 0.5,
                        RANDOM.nextGaussian() * 0.5,
                        RANDOM.nextGaussian() * 0.5);
            }
        }

        @Override
        public String getDescription() {
            return "Pull target into private dimensional arena";
        }
    }
}
