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
 * 战斗规则类 Synergy 定义
 *
 * 包含:
 * 1. Counter Weave (反击之织) - 完美格挡反击链
 * 2. Glass Cannon Protocol (玻璃炮协议) - 低 HP 爆发模式
 * 3. Void Strike (虚空打击) - 无视防御的背刺
 */
public class CombatSynergies {

    private static final Random RANDOM = new Random();

    // 追踪玩家状态
    private static final Map<UUID, CounterWeaveData> COUNTER_DATA = new HashMap<>();
    private static final Map<UUID, VoidStrikeData> VOID_STRIKE_DATA = new HashMap<>();

    public static void registerAll(SynergyManager manager) {
        manager.register(createCounterWeave());
        manager.register(createGlassCannon());
        manager.register(createVoidStrike());

        System.out.println("[Synergy] Registered 3 Combat Synergies");
    }

    // ==================== 1. Counter Weave (反击之织) ====================

    /**
     * Counter Weave - 反击之织
     *
     * 模块要求: PARRY + REFLEX + MOMENTUM (三角形排列)
     * 触发条件: 受到近战攻击前 0.3 秒内按下格挡
     *
     * 效果:
     * - 完美格挡: 伤害归零
     * - 进入 Counter Window (0.5秒): 下次攻击必定暴击 + Stagger
     * - Riposte Chain: 可连续反击
     *
     * 代价:
     * - 格挡过早受 x1.5 伤害
     * - 连续 3 次后第 4 次必定 Miss
     * - 每次消耗 10% 能量
     * - 对远程无效
     */
    public static SynergyDefinition createCounterWeave() {
        return SynergyDefinition.builder("counter_weave")
                .displayName("反击之织")
                .description("完美格挡开启反击的可能")

                // 模块要求 (三角形)
                .requireModules("PARRY", "REFLEX", "MOMENTUM")
                .addLink("PARRY", "REFLEX", "triangle")
                .addLink("REFLEX", "MOMENTUM", "triangle")
                .addLink("MOMENTUM", "PARRY", "triangle")

                // 触发: 完美格挡 / 受伤 / 攻击
                .triggerOn(SynergyEventType.PERFECT_BLOCK, SynergyEventType.HURT, SynergyEventType.ATTACK)

                // 效果
                .addEffect(new CounterWeaveEffect())

                .priority(5)
                .build();
    }

    private static class CounterWeaveEffect implements ISynergyEffect {
        private static final String STATE_COUNTER_WINDOW = "counter_window";
        private static final int COUNTER_WINDOW_DURATION = 10;  // 0.5 秒
        private static final int MAX_CHAIN = 3;

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
            World world = player.world;

            UUID playerId = player.getUniqueID();
            CounterWeaveData data = COUNTER_DATA.computeIfAbsent(playerId, k -> new CounterWeaveData());

            switch (context.getEventType()) {
                case PERFECT_BLOCK:
                    // 完美格挡成功
                    handlePerfectBlock(player, state, data, bridge);
                    break;

                case HURT:
                    // 受伤 - 检查是否在格挡
                    handleHurt(player, state, data, context);
                    break;

                case ATTACK:
                    // 攻击 - 检查是否在 Counter Window
                    handleAttack(player, state, data, context);
                    break;
            }
        }

        private void handlePerfectBlock(EntityPlayer player, SynergyPlayerState state,
                                       CounterWeaveData data, ExistingModuleBridge bridge) {
            // 消耗能量
            int maxEnergy = bridge.getMaxEnergy(player);
            bridge.consumeEnergy(player, (int)(maxEnergy * 0.10f));

            // 激活 Counter Window
            state.activateState(STATE_COUNTER_WINDOW, COUNTER_WINDOW_DURATION);
            data.inCounterWindow = true;

            // 视觉效果
            spawnBlockParticles(player);

            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 1.5f);

            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "⚔ 反击之织: 完美格挡！" +
                    TextFormatting.WHITE + " [反击窗口开启]"));
        }

        private void handleHurt(EntityPlayer player, SynergyPlayerState state,
                               CounterWeaveData data, SynergyContext context) {
            // 如果不在格挡状态，且正在尝试格挡但时机错误
            if (player.isActiveItemStackBlocking() && !state.hasActiveState(STATE_COUNTER_WINDOW)) {
                // 格挡过早，受到 1.5x 伤害
                // 注意：实际实现需要修改伤害事件
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "⚔ 反击之织: 格挡过早！"));
            }
        }

        private void handleAttack(EntityPlayer player, SynergyPlayerState state,
                                 CounterWeaveData data, SynergyContext context) {
            EntityLivingBase target = context.getTarget();
            if (target == null) return;

            // 检查是否在 Counter Window
            if (!state.hasActiveState(STATE_COUNTER_WINDOW) && !data.inCounterWindow) {
                return;
            }

            // 检查连击次数
            data.chainCount++;
            if (data.chainCount > MAX_CHAIN) {
                // 第 4 次必定 Miss
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "⚔ 反击之织: 连击过载，攻击落空！"));
                data.chainCount = 0;
                data.inCounterWindow = false;
                return;
            }

            // 执行反击
            float originalDamage = context.getOriginalDamage();
            float bonusDamage = originalDamage * 0.5f;  // 额外 50% 伤害 (模拟暴击)

            // 造成 Stagger
            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 30, 2, false, false));
            target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 30, 1, false, false));

            // 额外伤害
            DamageSource source = DamageSource.causePlayerDamage(player);
            target.attackEntityFrom(source, bonusDamage);

            // 给予无敌帧
            player.hurtResistantTime = 10;

            // 检查是否触发 Riposte Chain (目标正在攻击)
            if (target.isSwingInProgress) {
                // 延长 Counter Window
                state.activateState(STATE_COUNTER_WINDOW, COUNTER_WINDOW_DURATION);

                player.sendMessage(new TextComponentString(
                        TextFormatting.GOLD + "⚔ Riposte Chain! [" + data.chainCount + "/" + MAX_CHAIN + "]"));
            } else {
                data.inCounterWindow = false;
            }

            // 视觉效果
            spawnCounterParticles(player, target);

            player.world.playSound(null, target.posX, target.posY, target.posZ,
                    SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 1.2f);
        }

        private void spawnBlockParticles(EntityPlayer player) {
            World world = player.world;
            Vec3d lookVec = player.getLookVec();

            for (int i = 0; i < 15; i++) {
                world.spawnParticle(EnumParticleTypes.CRIT,
                        player.posX + lookVec.x + RANDOM.nextGaussian() * 0.3,
                        player.posY + 1.2 + RANDOM.nextGaussian() * 0.3,
                        player.posZ + lookVec.z + RANDOM.nextGaussian() * 0.3,
                        lookVec.x * 0.2, 0.1, lookVec.z * 0.2);
            }
        }

        private void spawnCounterParticles(EntityPlayer player, EntityLivingBase target) {
            World world = player.world;

            for (int i = 0; i < 20; i++) {
                world.spawnParticle(EnumParticleTypes.SWEEP_ATTACK,
                        target.posX + RANDOM.nextGaussian() * 0.5,
                        target.posY + 1 + RANDOM.nextGaussian() * 0.5,
                        target.posZ + RANDOM.nextGaussian() * 0.5,
                        0, 0, 0);
            }
        }

        @Override
        public String getDescription() {
            return "Perfect block enables counter attack chain";
        }
    }

    // ==================== 2. Glass Cannon Protocol (玻璃炮协议) ====================

    /**
     * Glass Cannon Protocol - 玻璃炮协议
     *
     * 模块要求: BERSERK + SACRIFICE + 任意两个攻击模块 (填满 4+ 槽)
     * 触发条件: HP 低于 30% 时自动激活
     *
     * 效果:
     * - 所有伤害 x3
     * - 攻击附带 Armor Shred (无视 50% 护甲)
     * - 击杀回复 20% HP
     * - Bloodlust: 击杀延长状态
     *
     * 代价:
     * - 受到伤害 x2
     * - 无法使用防御技能
     * - HP 无法超过 30%
     * - 结束后 Stunned 3 秒
     * - 死亡时爆炸伤害队友
     */
    public static SynergyDefinition createGlassCannon() {
        return SynergyDefinition.builder("glass_cannon")
                .displayName("玻璃炮协议")
                .description("以脆弱换取毁灭性的力量")

                // 模块要求 (4+ 槽)
                .requireModules("BERSERK", "SACRIFICE", "DAMAGE_BOOST", "CRITICAL_STRIKE")

                // 触发: 低 HP / Tick / 攻击 / 击杀
                .triggerOn(SynergyEventType.LOW_HEALTH, SynergyEventType.TICK,
                          SynergyEventType.ATTACK, SynergyEventType.KILL)

                // 条件
                .addCondition(HealthThresholdCondition.critical())  // HP < 30%

                // 效果
                .addEffect(new GlassCannonEffect())

                .priority(10)
                .build();
    }

    private static class GlassCannonEffect implements ISynergyEffect {
        private static final String STATE_GLASS_CANNON = "glass_cannon_active";
        private static final int BASE_DURATION = 200;  // 10 秒基础
        private static final int MAX_DURATION = 600;   // 30 秒上限
        private static final int KILL_EXTENSION = 100; // 击杀延长 5 秒
        private static final float DAMAGE_MULTIPLIER = 3.0f;
        private static final float ARMOR_SHRED = 0.5f;

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            World world = player.world;

            boolean isActive = state.isInGlassCannon();

            switch (context.getEventType()) {
                case LOW_HEALTH:
                case TICK:
                    if (!isActive && player.getHealth() <= player.getMaxHealth() * 0.3f) {
                        // 激活 Glass Cannon
                        activateGlassCannon(player, state);
                    } else if (isActive) {
                        // 维护状态
                        maintainGlassCannon(player, state);
                    }
                    break;

                case ATTACK:
                    if (isActive) {
                        // 强化攻击
                        enhanceAttack(player, context);
                    }
                    break;

                case KILL:
                    if (isActive) {
                        // 击杀奖励
                        handleKill(player, state);
                    }
                    break;
            }
        }

        private void activateGlassCannon(EntityPlayer player, SynergyPlayerState state) {
            state.setInGlassCannon(true);
            state.activateState(STATE_GLASS_CANNON, BASE_DURATION, () -> {
                endGlassCannon(player, state);
            });

            // 增强效果
            player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, BASE_DURATION, 2, false, true));
            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, BASE_DURATION, 1, false, true));

            // 视觉效果
            spawnActivationParticles(player);

            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.5f, 2.0f);

            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "☠ 玻璃炮协议: 启动！" +
                    TextFormatting.GRAY + " (伤害x3, 受伤x2, HP上限30%)"));
        }

        private void maintainGlassCannon(EntityPlayer player, SynergyPlayerState state) {
            // 限制 HP 不超过 30%
            float maxAllowedHP = player.getMaxHealth() * 0.3f;
            if (player.getHealth() > maxAllowedHP) {
                player.setHealth(maxAllowedHP);
            }

            // 粒子效果
            if (player.world.getTotalWorldTime() % 10 == 0) {
                World world = player.world;
                for (int i = 0; i < 5; i++) {
                    world.spawnParticle(EnumParticleTypes.FLAME,
                            player.posX + RANDOM.nextGaussian() * 0.3,
                            player.posY + 1 + RANDOM.nextGaussian() * 0.5,
                            player.posZ + RANDOM.nextGaussian() * 0.3,
                            0, 0.02, 0);
                }
            }
        }

        private void enhanceAttack(EntityPlayer player, SynergyContext context) {
            EntityLivingBase target = context.getTarget();
            if (target == null) return;

            // 额外伤害 (模拟 x3)
            float originalDamage = context.getOriginalDamage();
            float bonusDamage = originalDamage * (DAMAGE_MULTIPLIER - 1);  // 额外 2x

            // Armor Shred 效果
            DamageSource source = DamageSource.causePlayerDamage(player);
            source.setDamageBypassesArmor();  // 简化：直接无视护甲

            target.attackEntityFrom(source, bonusDamage * ARMOR_SHRED);

            // 普通伤害
            target.attackEntityFrom(DamageSource.causePlayerDamage(player), bonusDamage * (1 - ARMOR_SHRED));

            // 视觉效果
            player.world.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                    target.posX, target.posY + 1.5, target.posZ,
                    RANDOM.nextGaussian() * 0.3, 0.5, RANDOM.nextGaussian() * 0.3);
        }

        private void handleKill(EntityPlayer player, SynergyPlayerState state) {
            // 回复 HP (但限制在 30%)
            float healAmount = player.getMaxHealth() * 0.2f;
            float currentHP = player.getHealth();
            float maxAllowedHP = player.getMaxHealth() * 0.3f;
            float newHP = Math.min(currentHP + healAmount, maxAllowedHP);
            player.setHealth(newHP);

            // 延长状态
            state.extendState(STATE_GLASS_CANNON, KILL_EXTENSION, MAX_DURATION);

            // 刷新增益
            int remaining = state.getStateRemainingTicks(STATE_GLASS_CANNON);
            player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, remaining, 2, false, true));
            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, remaining, 1, false, true));

            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "☠ Bloodlust! 状态延长 5 秒"));

            // 音效
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 0.5f);
        }

        private void endGlassCannon(EntityPlayer player, SynergyPlayerState state) {
            state.setInGlassCannon(false);

            // Stunned 3 秒
            player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 10, false, true));
            player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 60, 0, false, true));

            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "☠ 玻璃炮协议: 终止。" +
                    TextFormatting.DARK_GRAY + " (Stunned 3 秒)"));
        }

        private void spawnActivationParticles(EntityPlayer player) {
            World world = player.world;
            for (int i = 0; i < 30; i++) {
                double angle = (i / 30.0) * Math.PI * 2;
                double x = player.posX + Math.cos(angle) * 1.5;
                double z = player.posZ + Math.sin(angle) * 1.5;

                world.spawnParticle(EnumParticleTypes.FLAME,
                        x, player.posY + 0.1, z,
                        0, 0.2, 0);
            }
        }

        @Override
        public String getDescription() {
            return "Low HP triggers devastating damage boost";
        }
    }

    // ==================== 3. Void Strike (虚空打击) ====================

    /**
     * Void Strike - 虚空打击
     *
     * 模块要求: VOID + CRITICAL + PHASE (任意排列)
     * 触发条件: 背刺（从目标背后攻击）
     *
     * 效果:
     * - 虚空伤害: 无视护甲和护盾
     * - 不触发目标受伤事件
     * - 伤害延迟 1 秒显示
     * - 暴击时: Void Mark (回复 -50%, 再次背刺必定暴击)
     *
     * 代价:
     * - 自己受 10% 虚空反噬
     * - 正面攻击无效
     * - 每次消耗 15% 能量
     * - 叠加 Void Corruption
     */
    public static SynergyDefinition createVoidStrike() {
        return SynergyDefinition.builder("void_strike")
                .displayName("虚空打击")
                .description("从虚空穿透一切防御")

                // 模块要求
                .requireModules("VOID", "CRITICAL_STRIKE", "PHASE")

                // 触发: 背刺 / 攻击
                .triggerOn(SynergyEventType.BACKSTAB, SynergyEventType.ATTACK)

                // 条件
                .addCondition(EnergyThresholdCondition.atLeast(15f))

                // 效果
                .addEffect(new VoidStrikeEffect())

                .priority(15)
                .build();
    }

    private static class VoidStrikeEffect implements ISynergyEffect {
        private static final int VOID_MARK_DURATION = 200;  // 10 秒
        private static final float BACKLASH_RATIO = 0.10f;
        private static final float ENERGY_COST = 0.15f;

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            EntityLivingBase target = context.getTarget();

            if (target == null) return;

            // 检查是否为背刺
            if (!isBackstab(player, target)) {
                return;  // 正面攻击无效
            }

            SynergyPlayerState state = SynergyPlayerState.get(player);
            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
            World world = player.world;

            // 消耗能量
            int maxEnergy = bridge.getMaxEnergy(player);
            bridge.consumeEnergy(player, (int)(maxEnergy * ENERGY_COST));

            // 获取或创建 Void Strike 数据
            UUID playerId = player.getUniqueID();
            VoidStrikeData data = VOID_STRIKE_DATA.computeIfAbsent(playerId, k -> new VoidStrikeData());

            // 检查目标是否有 Void Mark
            boolean hasVoidMark = target.getTags().contains("void_mark");
            boolean isCritical = hasVoidMark || RANDOM.nextFloat() < 0.25f;  // 有标记必暴，否则 25% 暴击

            // 计算虚空伤害
            float originalDamage = context.getOriginalDamage();
            float voidDamage = originalDamage * (isCritical ? 1.5f : 1.0f);

            // 造成虚空伤害 (无视护甲)
            DamageSource voidSource = DamageSource.OUT_OF_WORLD;  // 模拟虚空伤害
            target.attackEntityFrom(voidSource, voidDamage);

            // 如果暴击，添加 Void Mark
            if (isCritical) {
                target.addTag("void_mark");
                // 标记持续时间需要通过其他方式管理

                // 减少目标回复
                target.addPotionEffect(new PotionEffect(MobEffects.WITHER, VOID_MARK_DURATION, 0, false, false));

                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_PURPLE + "◆ Void Mark! 目标回复 -50%"));
            }

            // 自身虚空反噬 (延迟 1 秒)
            float backlashDamage = voidDamage * BACKLASH_RATIO;
            // 简化实现：立即造成伤害，实际应该延迟
            player.attackEntityFrom(DamageSource.OUT_OF_WORLD, backlashDamage);

            // 叠加 Void Corruption
            data.corruptionStacks++;
            state.addMaxHealthModifier(-2f);  // 每层 -2% HP

            // 视觉效果
            spawnVoidParticles(target);

            player.world.playSound(null, target.posX, target.posY, target.posZ,
                    SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 0.5f, 0.5f);

            String critText = isCritical ? TextFormatting.GOLD + " [暴击!]" : "";
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "◆ 虚空打击: " +
                    String.format("%.1f", voidDamage) + " 伤害" + critText +
                    TextFormatting.DARK_GRAY + " (反噬 " + String.format("%.1f", backlashDamage) + ")"));
        }

        private boolean isBackstab(EntityPlayer attacker, EntityLivingBase target) {
            // 计算攻击者相对于目标的方向
            Vec3d toAttacker = attacker.getPositionVector().subtract(target.getPositionVector()).normalize();
            Vec3d targetLook = Vec3d.fromPitchYaw(0, target.rotationYaw).normalize();

            // 如果攻击者在目标背后 (点积为负)
            double dot = toAttacker.x * targetLook.x + toAttacker.z * targetLook.z;
            return dot < -0.5;  // 大致在 120 度后方
        }

        private void spawnVoidParticles(EntityLivingBase target) {
            World world = target.world;

            for (int i = 0; i < 20; i++) {
                world.spawnParticle(EnumParticleTypes.PORTAL,
                        target.posX + RANDOM.nextGaussian() * 0.5,
                        target.posY + 1 + RANDOM.nextGaussian() * 0.5,
                        target.posZ + RANDOM.nextGaussian() * 0.5,
                        0, -0.1, 0);
            }

            for (int i = 0; i < 10; i++) {
                world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        target.posX + RANDOM.nextGaussian() * 0.3,
                        target.posY + 1,
                        target.posZ + RANDOM.nextGaussian() * 0.3,
                        0, 0.05, 0);
            }
        }

        @Override
        public String getDescription() {
            return "Backstab deals void damage ignoring defenses";
        }
    }

    // ==================== 辅助类 ====================

    private static class CounterWeaveData {
        public boolean inCounterWindow = false;
        public int chainCount = 0;
        public long lastBlockTime = 0;
    }

    private static class VoidStrikeData {
        public int corruptionStacks = 0;
    }
}
