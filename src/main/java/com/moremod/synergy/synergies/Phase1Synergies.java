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
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

/**
 * Phase 1 Synergies - 简化版协同效果
 *
 * 设计原则:
 * - 2 个模块组合
 * - 单一触发条件
 * - 单一核心效果
 * - 单一代价类型 (RF / 冷却 / 护盾 / HP)
 *
 * 包含 8 个 Synergy:
 * 1. Overcharge Protocol (过载协议) - 击杀后强化下次攻击
 * 2. Phantom Step (幽影步) - 潜行冲刺时短暂隐身
 * 3. Kinetic Barrier (动能壁垒) - 护盾破碎时击退敌人
 * 4. Blood Pact (血之契约) - 低血量时紧急回血
 * 5. Hunter's Mark (猎人印记) - 攻击时标记目标
 * 6. Reactive Armor (反应式装甲) - 受伤时反弹伤害
 * 7. Solar Flare (太阳耀斑) - 阳光下燃烧周围敌人
 * 8. Neural Sync (神经同步) - 完美格挡后减速敌人
 */
public class Phase1Synergies {

    private static final Random RANDOM = new Random();

    public static void registerAll(SynergyManager manager) {
        manager.register(createOverchargeProtocol());
        manager.register(createPhantomStep());
        manager.register(createKineticBarrier());
        manager.register(createBloodPact());
        manager.register(createHuntersMark());
        manager.register(createReactiveArmor());
        manager.register(createSolarFlare());
        manager.register(createNeuralSync());

        System.out.println("[Synergy] Registered 8 Phase 1 Synergies");
    }

    // ==================== 1. Overcharge Protocol (过载协议) ====================
    /**
     * 模块: DAMAGE_BOOST + COMBAT_CHARGER
     * 触发: 击杀敌人
     * 效果: 下一次攻击造成 3x 伤害
     * 代价: 5000 RF
     */
    public static SynergyDefinition createOverchargeProtocol() {
        return SynergyDefinition.builder("overcharge_protocol")
                .displayName("过载协议")
                .description("击杀后积蓄能量，下次攻击爆发")
                .requireModules("DAMAGE_BOOST", "COMBAT_CHARGER")
                .addLink("DAMAGE_BOOST", "COMBAT_CHARGER")
                .triggerOn(SynergyEventType.KILL, SynergyEventType.ATTACK)
                .addCondition(EnergyThresholdCondition.atLeast(5000))
                .addEffect(new OverchargeEffect())
                .priority(10)
                .build();
    }

    private static class OverchargeEffect implements ISynergyEffect {
        private static final String STATE_OVERCHARGED = "overcharged";
        private static final int DURATION = 200; // 10 秒
        private static final int ENERGY_COST = 5000;

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();

            if (context.getEventType() == SynergyEventType.KILL) {
                // 击杀时激活过载
                if (!state.hasActiveState(STATE_OVERCHARGED)) {
                    bridge.consumeEnergy(player, ENERGY_COST);
                    state.activateState(STATE_OVERCHARGED, DURATION);

                    player.sendMessage(new TextComponentString(
                            TextFormatting.GOLD + "⚡ 过载协议: 能量积蓄完毕！" +
                            TextFormatting.GRAY + " (下次攻击 x3)"));

                    spawnChargeParticles(player);
                }
            } else if (context.getEventType() == SynergyEventType.ATTACK) {
                // 攻击时释放过载
                if (state.hasActiveState(STATE_OVERCHARGED)) {
                    EntityLivingBase target = context.getTarget();
                    if (target != null) {
                        float damage = context.getOriginalDamage() * 2; // 额外 2x
                        target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage);

                        state.deactivateState(STATE_OVERCHARGED);

                        player.sendMessage(new TextComponentString(
                                TextFormatting.RED + "⚡ 过载释放! " +
                                TextFormatting.WHITE + String.format("%.1f", damage) + " 额外伤害"));

                        spawnReleaseParticles(player, target);
                    }
                }
            }
        }

        private void spawnChargeParticles(EntityPlayer player) {
            World world = player.world;
            for (int i = 0; i < 20; i++) {
                world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                        player.posX + RANDOM.nextGaussian() * 0.5,
                        player.posY + 1 + RANDOM.nextGaussian() * 0.5,
                        player.posZ + RANDOM.nextGaussian() * 0.5,
                        0, 0.1, 0);
            }
        }

        private void spawnReleaseParticles(EntityPlayer player, EntityLivingBase target) {
            World world = player.world;
            for (int i = 0; i < 30; i++) {
                world.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                        target.posX + RANDOM.nextGaussian() * 0.5,
                        target.posY + 1 + RANDOM.nextGaussian() * 0.5,
                        target.posZ + RANDOM.nextGaussian() * 0.5,
                        RANDOM.nextGaussian() * 0.2, 0.3, RANDOM.nextGaussian() * 0.2);
            }
            world.playSound(null, target.posX, target.posY, target.posZ,
                    SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.PLAYERS, 0.5f, 2.0f);
        }

        @Override
        public String getDescription() {
            return "Kill to charge, next attack deals 3x damage";
        }
    }

    // ==================== 2. Phantom Step (幽影步) ====================
    /**
     * 模块: MOVEMENT_SPEED + STEALTH
     * 触发: 潜行状态下开始冲刺
     * 效果: 3 秒隐身 + 移速提升
     * 代价: 15 秒冷却
     */
    public static SynergyDefinition createPhantomStep() {
        return SynergyDefinition.builder("phantom_step")
                .displayName("幽影步")
                .description("潜行冲刺时化为幽影")
                .requireModules("MOVEMENT_SPEED", "STEALTH")
                .addLink("MOVEMENT_SPEED", "STEALTH")
                .triggerOn(SynergyEventType.SPRINT)
                .addCondition(CooldownCondition.notOnCooldown("phantom_step"))
                .addCondition(PlayerStateCondition.isSneaking())
                .addEffect(new PhantomStepEffect())
                .addEffect(CooldownEffect.setSeconds("phantom_step", 15))
                .priority(20)
                .build();
    }

    private static class PhantomStepEffect implements ISynergyEffect {
        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();

            // 3 秒隐身 + 速度
            player.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 60, 0, false, false));
            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 60, 2, false, false));

            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_GRAY + "◈ 幽影步: 化为虚影..."));

            // 粒子效果
            World world = player.world;
            for (int i = 0; i < 15; i++) {
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        player.posX + RANDOM.nextGaussian() * 0.3,
                        player.posY + RANDOM.nextDouble() * 1.8,
                        player.posZ + RANDOM.nextGaussian() * 0.3,
                        0, 0.05, 0);
            }
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 0.5f, 1.5f);
        }

        @Override
        public String getDescription() {
            return "Become invisible for 3 seconds";
        }
    }

    // ==================== 3. Kinetic Barrier (动能壁垒) ====================
    /**
     * 模块: YELLOW_SHIELD + KINETIC_GENERATOR
     * 触发: 护盾破碎时
     * 效果: 击退 5 格内所有敌人
     * 代价: 30 秒冷却
     */
    public static SynergyDefinition createKineticBarrier() {
        return SynergyDefinition.builder("kinetic_barrier")
                .displayName("动能壁垒")
                .description("护盾破碎时释放冲击波")
                .requireModules("YELLOW_SHIELD", "KINETIC_GENERATOR")
                .addLink("YELLOW_SHIELD", "KINETIC_GENERATOR")
                .triggerOn(SynergyEventType.HURT)
                .addCondition(CooldownCondition.notOnCooldown("kinetic_barrier"))
                .addEffect(new KineticBarrierEffect())
                .priority(15)
                .build();
    }

    private static class KineticBarrierEffect implements ISynergyEffect {
        private static final float RADIUS = 5.0f;

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();

            // 检查护盾是否刚刚破碎（吸收值从有到无）
            float absorption = player.getAbsorptionAmount();
            if (absorption > 0) return; // 护盾还在

            // 检查是否有护盾模块在工作
            int shieldLevel = context.getModuleLevel("YELLOW_SHIELD");
            if (shieldLevel <= 0) return;

            SynergyPlayerState state = SynergyPlayerState.get(player);
            World world = player.world;

            // 击退周围敌人
            AxisAlignedBB area = new AxisAlignedBB(
                    player.posX - RADIUS, player.posY - RADIUS, player.posZ - RADIUS,
                    player.posX + RADIUS, player.posY + RADIUS, player.posZ + RADIUS);

            List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
                    EntityLivingBase.class, area, e -> e != player && e.isEntityAlive());

            for (EntityLivingBase entity : entities) {
                // 计算击退方向
                double dx = entity.posX - player.posX;
                double dz = entity.posZ - player.posZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0) {
                    double strength = (RADIUS - dist) / RADIUS * 2.0;
                    entity.motionX += (dx / dist) * strength;
                    entity.motionY += 0.3;
                    entity.motionZ += (dz / dist) * strength;
                    entity.velocityChanged = true;
                }
            }

            // 设置冷却
            state.setCooldown("kinetic_barrier", 30000);

            player.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "◎ 动能壁垒: 冲击波释放！" +
                    TextFormatting.GRAY + " (击退 " + entities.size() + " 个敌人)"));

            // 视觉效果
            for (int i = 0; i < 50; i++) {
                double angle = (i / 50.0) * Math.PI * 2;
                double x = player.posX + Math.cos(angle) * RADIUS * (i % 2 == 0 ? 0.5 : 1.0);
                double z = player.posZ + Math.sin(angle) * RADIUS * (i % 2 == 0 ? 0.5 : 1.0);
                world.spawnParticle(EnumParticleTypes.CLOUD, x, player.posY + 0.5, z, 0, 0.1, 0);
            }
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8f, 1.5f);
        }

        @Override
        public String getDescription() {
            return "Knockback nearby enemies when shield breaks";
        }
    }

    // ==================== 4. Blood Pact (血之契约) ====================
    /**
     * 模块: HEALTH_REGEN + VOID_ENERGY
     * 触发: 生命值低于 30%
     * 效果: 立即恢复 50% 生命值
     * 代价: 60 秒内最大生命值 -20%
     */
    public static SynergyDefinition createBloodPact() {
        return SynergyDefinition.builder("blood_pact")
                .displayName("血之契约")
                .description("以未来换取现在的生存")
                .requireModules("HEALTH_REGEN", "VOID_ENERGY")
                .addLink("HEALTH_REGEN", "VOID_ENERGY")
                .triggerOn(SynergyEventType.LOW_HEALTH, SynergyEventType.HURT)
                .addCondition(CooldownCondition.notOnCooldown("blood_pact"))
                .addCondition(HealthThresholdCondition.critical()) // < 30%
                .addEffect(new BloodPactEffect())
                .priority(5)
                .build();
    }

    private static class BloodPactEffect implements ISynergyEffect {
        private static final String STATE_BLOOD_DEBT = "blood_debt";
        private static final int DEBT_DURATION = 1200; // 60 秒

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);

            // 检查是否已经在血债状态
            if (state.hasActiveState(STATE_BLOOD_DEBT)) return;

            // 恢复 50% HP
            float healAmount = player.getMaxHealth() * 0.5f;
            player.heal(healAmount);

            // 激活血债状态 (-20% 最大 HP)
            state.addMaxHealthModifier(-20f);
            state.activateState(STATE_BLOOD_DEBT, DEBT_DURATION, () -> {
                // 60秒后恢复
                state.addMaxHealthModifier(20f);
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "◆ 血之契约: 契约完成，代价解除"));
            });

            // 设置冷却 (60秒 + 血债持续时间)
            state.setCooldown("blood_pact", 120000);

            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "◆ 血之契约: 以血为约！" +
                    TextFormatting.GRAY + " (HP +50%, 最大HP -20% 持续60秒)"));

            // 视觉效果
            World world = player.world;
            for (int i = 0; i < 30; i++) {
                world.spawnParticle(EnumParticleTypes.REDSTONE,
                        player.posX + RANDOM.nextGaussian() * 0.5,
                        player.posY + 1 + RANDOM.nextGaussian() * 0.5,
                        player.posZ + RANDOM.nextGaussian() * 0.5,
                        0.8, 0, 0);
            }
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 0.5f, 0.5f);
        }

        @Override
        public String getDescription() {
            return "Emergency heal 50% HP, reduce max HP by 20% for 60s";
        }
    }

    // ==================== 5. Hunter's Mark (猎人印记) ====================
    /**
     * 模块: ORE_VISION + PURSUIT
     * 触发: 攻击敌人
     * 效果: 标记目标，10 秒内可透视追踪
     * 代价: 3000 RF
     */
    public static SynergyDefinition createHuntersMark() {
        return SynergyDefinition.builder("hunters_mark")
                .displayName("猎人印记")
                .description("标记猎物，无处遁形")
                .requireModules("ORE_VISION", "PURSUIT")
                .addLink("ORE_VISION", "PURSUIT")
                .triggerOn(SynergyEventType.ATTACK)
                .addCondition(EnergyThresholdCondition.atLeast(3000))
                .addCondition(CooldownCondition.notOnCooldown("hunters_mark"))
                .addCondition(TargetCondition.isNotPlayer())
                .addEffect(new HuntersMarkEffect())
                .priority(30)
                .build();
    }

    private static class HuntersMarkEffect implements ISynergyEffect {
        private static final int MARK_DURATION = 200; // 10 秒
        private static final int ENERGY_COST = 3000;

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            EntityLivingBase target = context.getTarget();
            if (target == null) return;

            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
            SynergyPlayerState state = SynergyPlayerState.get(player);

            // 消耗能量
            bridge.consumeEnergy(player, ENERGY_COST);

            // 给目标添加发光效果
            target.addPotionEffect(new PotionEffect(MobEffects.GLOWING, MARK_DURATION, 0, false, false));

            // 设置冷却 (5秒)
            state.setCooldown("hunters_mark", 5000);

            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "◎ 猎人印记: 目标已标记！" +
                    TextFormatting.GRAY + " (10秒透视)"));

            // 视觉效果
            World world = player.world;
            for (int i = 0; i < 10; i++) {
                world.spawnParticle(EnumParticleTypes.END_ROD,
                        target.posX + RANDOM.nextGaussian() * 0.3,
                        target.posY + target.height + 0.5,
                        target.posZ + RANDOM.nextGaussian() * 0.3,
                        0, 0.1, 0);
            }
            world.playSound(null, target.posX, target.posY, target.posZ,
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.5f, 2.0f);
        }

        @Override
        public String getDescription() {
            return "Mark target with glowing for 10 seconds";
        }
    }

    // ==================== 6. Reactive Armor (反应式装甲) ====================
    /**
     * 模块: THORNS + YELLOW_SHIELD
     * 触发: 受到伤害时
     * 效果: 反弹 200% 伤害给攻击者
     * 代价: 消耗 2 点护盾
     */
    public static SynergyDefinition createReactiveArmor() {
        return SynergyDefinition.builder("reactive_armor")
                .displayName("反应式装甲")
                .description("以护盾换取强力反击")
                .requireModules("THORNS", "YELLOW_SHIELD")
                .addLink("THORNS", "YELLOW_SHIELD")
                .triggerOn(SynergyEventType.HURT)
                .addEffect(new ReactiveArmorEffect())
                .priority(10)
                .build();
    }

    private static class ReactiveArmorEffect implements ISynergyEffect {
        private static final float SHIELD_COST = 2.0f;
        private static final float REFLECT_MULTIPLIER = 2.0f;

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();

            // 检查是否有足够护盾
            float absorption = player.getAbsorptionAmount();
            if (absorption < SHIELD_COST) return;

            // 获取攻击者
            EntityLivingBase attacker = context.getAttacker();
            if (attacker == null || attacker == player) return;

            // 消耗护盾
            player.setAbsorptionAmount(absorption - SHIELD_COST);

            // 反弹伤害
            float damage = context.getOriginalDamage() * REFLECT_MULTIPLIER;
            attacker.attackEntityFrom(DamageSource.causeThornsDamage(player), damage);

            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "⛨ 反应式装甲: 反弹 " +
                    TextFormatting.RED + String.format("%.1f", damage) +
                    TextFormatting.YELLOW + " 伤害！"));

            // 视觉效果
            World world = player.world;
            world.spawnParticle(EnumParticleTypes.CRIT,
                    attacker.posX, attacker.posY + 1, attacker.posZ,
                    0.5, 0.5, 0.5);
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 1.2f);
        }

        @Override
        public String getDescription() {
            return "Reflect 200% damage, costs 2 shield points";
        }
    }

    // ==================== 7. Solar Flare (太阳耀斑) ====================
    /**
     * 模块: SOLAR_GENERATOR + FIRE_EXTINGUISH
     * 触发: 在阳光下 + 能量 > 80%
     * 效果: 点燃 8 格内所有敌人
     * 代价: 消耗 50% 当前能量
     */
    public static SynergyDefinition createSolarFlare() {
        return SynergyDefinition.builder("solar_flare")
                .displayName("太阳耀斑")
                .description("积蓄阳光释放烈焰")
                .requireModules("SOLAR_GENERATOR", "FIRE_EXTINGUISH")
                .addLink("SOLAR_GENERATOR", "FIRE_EXTINGUISH")
                .triggerOn(SynergyEventType.SKILL_ACTIVATE, SynergyEventType.SNEAK)
                .addCondition(CooldownCondition.notOnCooldown("solar_flare"))
                .addCondition(EnergyThresholdCondition.atLeastPercent(80f))
                .addEffect(new SolarFlareEffect())
                .priority(25)
                .build();
    }

    private static class SolarFlareEffect implements ISynergyEffect {
        private static final float RADIUS = 8.0f;
        private static final int FIRE_DURATION = 100; // 5 秒

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            World world = player.world;

            // 检查是否在阳光下
            if (!world.canSeeSky(player.getPosition().up())) {
                return;
            }

            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
            SynergyPlayerState state = SynergyPlayerState.get(player);

            // 消耗 50% 当前能量
            int currentEnergy = bridge.getCurrentEnergy(player);
            bridge.consumeEnergy(player, currentEnergy / 2);

            // 点燃周围敌人
            AxisAlignedBB area = new AxisAlignedBB(
                    player.posX - RADIUS, player.posY - RADIUS, player.posZ - RADIUS,
                    player.posX + RADIUS, player.posY + RADIUS, player.posZ + RADIUS);

            List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
                    EntityLivingBase.class, area, e -> e != player && e.isEntityAlive());

            for (EntityLivingBase entity : entities) {
                entity.setFire(FIRE_DURATION / 20);
                // 额外火焰伤害
                entity.attackEntityFrom(DamageSource.IN_FIRE, 4.0f);
            }

            // 设置冷却 (20秒)
            state.setCooldown("solar_flare", 20000);

            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "☀ 太阳耀斑: 烈焰爆发！" +
                    TextFormatting.GRAY + " (点燃 " + entities.size() + " 个敌人)"));

            // 视觉效果
            for (int i = 0; i < 100; i++) {
                double angle = RANDOM.nextDouble() * Math.PI * 2;
                double dist = RANDOM.nextDouble() * RADIUS;
                double x = player.posX + Math.cos(angle) * dist;
                double z = player.posZ + Math.sin(angle) * dist;
                world.spawnParticle(EnumParticleTypes.FLAME, x, player.posY + 0.5, z,
                        0, 0.2, 0);
            }
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.8f);
        }

        @Override
        public String getDescription() {
            return "Set nearby enemies on fire in sunlight";
        }
    }

    // ==================== 8. Neural Sync (神经同步) ====================
    /**
     * 模块: NEURAL_SYNCHRONIZER + ATTACK_SPEED
     * 触发: 完美格挡
     * 效果: 减速 5 格内所有敌人 3 秒
     * 代价: 10 秒冷却
     */
    public static SynergyDefinition createNeuralSync() {
        return SynergyDefinition.builder("neural_sync")
                .displayName("神经同步")
                .description("以反应速度压制敌人")
                .requireModules("NEURAL_SYNCHRONIZER", "ATTACK_SPEED")
                .addLink("NEURAL_SYNCHRONIZER", "ATTACK_SPEED")
                .triggerOn(SynergyEventType.PERFECT_BLOCK, SynergyEventType.BLOCK)
                .addCondition(CooldownCondition.notOnCooldown("neural_sync"))
                .addEffect(new NeuralSyncEffect())
                .addEffect(CooldownEffect.setSeconds("neural_sync", 10))
                .priority(20)
                .build();
    }

    private static class NeuralSyncEffect implements ISynergyEffect {
        private static final float RADIUS = 5.0f;
        private static final int SLOW_DURATION = 60; // 3 秒

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            World world = player.world;

            // 减速周围敌人
            AxisAlignedBB area = new AxisAlignedBB(
                    player.posX - RADIUS, player.posY - RADIUS, player.posZ - RADIUS,
                    player.posX + RADIUS, player.posY + RADIUS, player.posZ + RADIUS);

            List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
                    EntityLivingBase.class, area, e -> e != player && e.isEntityAlive());

            for (EntityLivingBase entity : entities) {
                entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, SLOW_DURATION, 2, false, true));
            }

            player.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "◈ 神经同步: 敌人行动迟缓！" +
                    TextFormatting.GRAY + " (减速 " + entities.size() + " 个敌人)"));

            // 视觉效果 - 波纹扩散
            for (int ring = 1; ring <= 3; ring++) {
                double radius = ring * (RADIUS / 3);
                for (int i = 0; i < 20; i++) {
                    double angle = (i / 20.0) * Math.PI * 2;
                    double x = player.posX + Math.cos(angle) * radius;
                    double z = player.posZ + Math.sin(angle) * radius;
                    world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, x, player.posY + 1, z,
                            0, 0.05, 0);
                }
            }
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.5f);
        }

        @Override
        public String getDescription() {
            return "Slow nearby enemies for 3 seconds";
        }
    }
}
