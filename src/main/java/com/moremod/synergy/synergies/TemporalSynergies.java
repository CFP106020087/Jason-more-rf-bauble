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
 * 时间类 Synergy 定义
 *
 * 包含:
 * 1. Temporal Debt (时间债务) - 借用未来能量的爆发
 * 2. Causality Loop (因果循环) - 死亡回溯 + 预判视野
 * 3. Echo Chamber (回声室) - 召唤过去动作残影
 */
public class TemporalSynergies {

    private static final Random RANDOM = new Random();

    public static void registerAll(SynergyManager manager) {
        manager.register(createTemporalDebt());
        manager.register(createCausalityLoop());
        manager.register(createEchoChamber());

        System.out.println("[Synergy] Registered 3 Temporal Synergies");
    }

    // ==================== 1. Temporal Debt (时间债务) ====================

    /**
     * Temporal Debt - 时间债务
     *
     * 模块要求: TIME + OVERCLOCK + 任意攻击模块 (对称排列)
     * 触发条件: 能量低于 20% 时主动激活
     *
     * 效果:
     * - 进入 Borrowed Time 状态 8 秒
     * - 瞬间满能量 + 所有技能无冷却
     * - 攻击附带 Temporal Shatter (3秒后额外 50% 伤害)
     *
     * 代价:
     * - 8 秒后进入 Time Debt 状态 20 秒
     * - Time Debt: 能量回复 -90%, 冷却 x3, 移动速度 -40%
     * - 每次使用增加 15% Rejection
     */
    public static SynergyDefinition createTemporalDebt() {
        return SynergyDefinition.builder("temporal_debt")
                .displayName("时间债务")
                .description("向未来借用力量，但必须偿还")

                // 模块要求 (对称)
                .requireModules("TIME", "OVERCLOCK", "DAMAGE_BOOST")
                .addLink("TIME", "OVERCLOCK", "symmetric")
                .addLink("OVERCLOCK", "DAMAGE_BOOST", "symmetric")

                // 触发: 低能量时
                .triggerOn(SynergyEventType.ENERGY_LOW, SynergyEventType.MANUAL)

                // 条件
                .addCondition(EnergyThresholdCondition.below(20f))
                .addCondition(CooldownCondition.notOnCooldown("temporal_debt"))
                .addCondition(ActiveStateCondition.noState("time_debt"))  // 不能在 Time Debt 中使用

                // 效果
                .addEffect(new TemporalDebtEffect())

                .priority(5)
                .build();
    }

    private static class TemporalDebtEffect implements ISynergyEffect {
        private static final String STATE_BORROWED_TIME = "borrowed_time";
        private static final String STATE_TIME_DEBT = "time_debt";
        private static final int BORROWED_DURATION = 160;  // 8 秒
        private static final int DEBT_DURATION = 400;  // 20 秒

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();

            // 检查是否已在 Borrowed Time
            if (state.hasActiveState(STATE_BORROWED_TIME)) {
                return;
            }

            // 满能量
            int maxEnergy = bridge.getMaxEnergy(player);
            bridge.addEnergy(player, maxEnergy);

            // 激活 Borrowed Time 状态
            state.setInBorrowedTime(true);
            state.activateState(STATE_BORROWED_TIME, BORROWED_DURATION, () -> {
                // Borrowed Time 结束，进入 Time Debt
                enterTimeDebt(player, state);
            });

            // 增强效果
            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, BORROWED_DURATION, 1, false, true));
            player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, BORROWED_DURATION, 1, false, true));
            player.addPotionEffect(new PotionEffect(MobEffects.HASTE, BORROWED_DURATION, 2, false, true));

            // 增加排异值
            state.addRejection(15f);

            // 视觉效果
            spawnBorrowedTimeParticles(player);

            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "⏳ 时间债务: Borrowed Time 激活！" +
                    TextFormatting.GRAY + " (8秒后进入债务状态)"));

            // 音效
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 2.0f);
        }

        private void enterTimeDebt(EntityPlayer player, SynergyPlayerState state) {
            state.setInBorrowedTime(false);
            state.setInTimeDebt(true);

            state.activateState(STATE_TIME_DEBT, DEBT_DURATION, () -> {
                // Time Debt 结束
                state.setInTimeDebt(false);
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "⏳ 时间债务: 债务已清偿"));
            });

            // 惩罚效果
            player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, DEBT_DURATION, 1, false, true));
            player.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, DEBT_DURATION, 2, false, true));
            player.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, DEBT_DURATION, 0, false, true));

            // 设置冷却
            state.setCooldown("temporal_debt", DEBT_DURATION * 50 + 30000);  // Debt 期间 + 30 秒

            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "⏳ 时间债务: 进入 Time Debt 状态！" +
                    TextFormatting.GRAY + " (能量回复-90%, 冷却x3, 速度-40%)"));
        }

        private void spawnBorrowedTimeParticles(EntityPlayer player) {
            World world = player.world;
            for (int i = 0; i < 50; i++) {
                double angle = (i / 50.0) * Math.PI * 2;
                double radius = 1.5;
                double x = player.posX + Math.cos(angle) * radius;
                double z = player.posZ + Math.sin(angle) * radius;
                double y = player.posY + (i / 50.0) * 2;

                world.spawnParticle(EnumParticleTypes.END_ROD,
                        x, y, z, 0, 0.05, 0);
            }
        }

        @Override
        public String getDescription() {
            return "Borrow power from the future";
        }
    }

    // ==================== 2. Causality Loop (因果循环) ====================

    /**
     * Causality Loop - 因果循环
     *
     * 模块要求: TIME + MEMORY + SOUL (连续三槽)
     * 触发条件: 受到致命伤害时自动触发
     *
     * 效果:
     * - 不会死亡，时间回溯 5 秒
     * - 回到 5 秒前的位置、HP、能量
     * - 获得 3 秒 Foresight (可看到攻击预判线)
     * - 周围敌人 Déjà vu: 攻击速度 -30%
     *
     * 代价:
     * - HP 上限永久降低 10%
     * - 每次触发增加 30 秒额外冷却
     * - 第三次后失效直到重新配置
     */
    public static SynergyDefinition createCausalityLoop() {
        return SynergyDefinition.builder("causality_loop")
                .displayName("因果循环")
                .description("死亡时回溯时间，但代价是生命的一部分")

                // 模块要求 (连续)
                .requireModules("TIME", "MEMORY", "SOUL")
                .addLink("TIME", "MEMORY", "chain")
                .addLink("MEMORY", "SOUL", "chain")

                // 触发: 致命伤害
                .triggerOn(SynergyEventType.FATAL_DAMAGE, SynergyEventType.DEATH)

                // 条件
                .addCondition(CooldownCondition.notOnCooldown("causality_loop"))
                .addCondition(new CausalityLoopUsageCondition())  // 自定义条件检查使用次数

                // 效果
                .addEffect(new CausalityLoopEffect())

                .priority(1)  // 最高优先级
                .build();
    }

    private static class CausalityLoopUsageCondition implements com.moremod.synergy.api.ISynergyCondition {
        @Override
        public boolean test(SynergyContext context) {
            SynergyPlayerState state = SynergyPlayerState.get(context.getPlayer());
            return state.getCausalityLoopCount() < 3;
        }

        @Override
        public String getDescription() {
            return "Causality Loop uses < 3";
        }
    }

    private static class CausalityLoopEffect implements ISynergyEffect {
        private static final String STATE_FORESIGHT = "foresight";
        private static final int REWIND_TICKS = 100;  // 5 秒
        private static final int FORESIGHT_DURATION = 60;  // 3 秒
        private static final int DEJA_VU_RADIUS = 10;

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            World world = player.world;

            // 获取 5 秒前的位置快照
            SynergyPlayerState.PositionSnapshot snapshot = state.getPositionAt(REWIND_TICKS);
            if (snapshot == null) {
                // 没有足够的历史数据
                snapshot = state.getPositionAt(state.getStandingTicks());  // 用最早的
                if (snapshot == null) {
                    return;  // 无法回溯
                }
            }

            // 取消死亡
            player.setHealth(snapshot.health);

            // 传送回历史位置
            player.setPositionAndUpdate(snapshot.x, snapshot.y, snapshot.z);

            // 激活 Foresight 状态
            state.activateState(STATE_FORESIGHT, FORESIGHT_DURATION);

            // 给予预判视野效果
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, FORESIGHT_DURATION, 0, false, false));
            player.addPotionEffect(new PotionEffect(MobEffects.GLOWING, FORESIGHT_DURATION, 0, false, false));

            // 对周围敌人施加 Déjà vu
            AxisAlignedBB areaBox = new AxisAlignedBB(
                    player.posX - DEJA_VU_RADIUS, player.posY - 3, player.posZ - DEJA_VU_RADIUS,
                    player.posX + DEJA_VU_RADIUS, player.posY + 3, player.posZ + DEJA_VU_RADIUS
            );

            List<EntityLivingBase> enemies = world.getEntitiesWithinAABB(
                    EntityLivingBase.class, areaBox,
                    e -> e != player && !(e instanceof EntityPlayer)
            );

            for (EntityLivingBase enemy : enemies) {
                enemy.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100, 1, false, true));
                enemy.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 100, 1, false, true));

                // 粒子效果
                for (int i = 0; i < 10; i++) {
                    world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                            enemy.posX + RANDOM.nextGaussian() * 0.5,
                            enemy.posY + 1,
                            enemy.posZ + RANDOM.nextGaussian() * 0.5,
                            0, 0.1, 0);
                }
            }

            // 永久降低 HP 上限 10%
            state.addMaxHealthModifier(-10f);

            // 增加使用次数
            state.incrementCausalityLoop();
            int uses = state.getCausalityLoopCount();

            // 设置冷却 (基础 + 每次额外 30 秒)
            long cooldown = 30000 + state.getCausalityLoopCooldown();
            state.setCooldown("causality_loop", cooldown);

            // 视觉效果 - 时间回溯
            spawnRewindParticles(player, snapshot);

            // 音效
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0f, 0.5f);

            // 提示
            String warningText = "";
            if (uses >= 3) {
                warningText = TextFormatting.RED + " [已达上限，Synergy 失效]";
            } else {
                warningText = TextFormatting.GRAY + " [剩余 " + (3 - uses) + " 次]";
            }

            player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "⟲ 因果循环: 时间回溯完成！" +
                    TextFormatting.DARK_RED + " HP上限-10%" + warningText));
        }

        private void spawnRewindParticles(EntityPlayer player, SynergyPlayerState.PositionSnapshot target) {
            World world = player.world;

            // 从当前位置到目标位置的粒子轨迹
            Vec3d start = player.getPositionVector();
            Vec3d end = target.toVec3d();
            Vec3d direction = end.subtract(start).normalize();
            double distance = start.distanceTo(end);

            for (int i = 0; i < 50; i++) {
                double progress = i / 50.0;
                double x = start.x + (end.x - start.x) * progress;
                double y = start.y + 1 + (end.y + 1 - start.y - 1) * progress;
                double z = start.z + (end.z - start.z) * progress;

                world.spawnParticle(EnumParticleTypes.PORTAL,
                        x + RANDOM.nextGaussian() * 0.2,
                        y + RANDOM.nextGaussian() * 0.2,
                        z + RANDOM.nextGaussian() * 0.2,
                        0, 0, 0);
            }

            // 到达位置的爆发效果
            for (int i = 0; i < 30; i++) {
                world.spawnParticle(EnumParticleTypes.END_ROD,
                        end.x + RANDOM.nextGaussian() * 0.5,
                        end.y + 1 + RANDOM.nextGaussian(),
                        end.z + RANDOM.nextGaussian() * 0.5,
                        RANDOM.nextGaussian() * 0.1,
                        0.2,
                        RANDOM.nextGaussian() * 0.1);
            }
        }

        @Override
        public String getDescription() {
            return "Rewind time upon fatal damage";
        }
    }

    // ==================== 3. Echo Chamber (回声室) ====================

    /**
     * Echo Chamber - 回声室
     *
     * 模块要求: TIME + CLONE + AMPLIFY (环形排列)
     * 触发条件: 3 秒内命中同一目标 5 次
     *
     * 效果:
     * - 召唤 3 个时间残影，复制过去 5 秒动作
     * - 残影持续 6 秒，造成 40% 伤害
     * - 4 实体同时命中触发 Temporal Convergence: 目标冻结 2 秒后受 x1.5 伤害
     *
     * 代价:
     * - 攻击会误伤残影
     * - 残影被击杀造成反噬
     * - 每次使用消耗模块耐久 5%
     * - 45 秒冷却
     */
    public static SynergyDefinition createEchoChamber() {
        return SynergyDefinition.builder("echo_chamber")
                .displayName("回声室")
                .description("召唤时间残影，重复过去的动作")

                // 模块要求 (环形)
                .requireModules("TIME", "CLONE", "AMPLIFY")
                .addLink("TIME", "CLONE", "ring")
                .addLink("CLONE", "AMPLIFY", "ring")
                .addLink("AMPLIFY", "TIME", "ring")

                // 触发: 连击
                .triggerOn(SynergyEventType.COMBO, SynergyEventType.ATTACK)

                // 条件
                .addCondition(ComboCondition.atLeast(5))
                .addCondition(CooldownCondition.notOnCooldown("echo_chamber"))

                // 效果
                .addEffect(new EchoChamberEffect())

                .priority(15)
                .build();
    }

    private static class EchoChamberEffect implements ISynergyEffect {
        private static final String STATE_ECHOES_ACTIVE = "echo_chamber_active";
        private static final int ECHO_COUNT = 3;
        private static final int ECHO_DURATION = 120;  // 6 秒
        private static final float ECHO_DAMAGE_MULTIPLIER = 0.4f;

        // 存储已创建的残影（实际实现中应该是真正的实体）
        private static final Map<UUID, List<EchoData>> ACTIVE_ECHOES = new HashMap<>();

        @Override
        public void apply(SynergyContext context) {
            EntityPlayer player = context.getPlayer();
            SynergyPlayerState state = SynergyPlayerState.get(player);
            World world = player.world;

            // 重置连击计数
            state.resetCombo();

            // 获取位置历史
            List<SynergyPlayerState.PositionSnapshot> history = state.getPositionHistory(100);

            // 创建残影
            List<EchoData> echoes = new ArrayList<>();
            for (int i = 0; i < ECHO_COUNT; i++) {
                int historyIndex = Math.min(i * 30 + 20, history.size() - 1);  // 分散在历史中
                if (historyIndex >= 0 && historyIndex < history.size()) {
                    SynergyPlayerState.PositionSnapshot snapshot = history.get(historyIndex);
                    EchoData echo = new EchoData(
                            snapshot.x, snapshot.y, snapshot.z,
                            player.getHealth() * ECHO_DAMAGE_MULTIPLIER,
                            ECHO_DURATION
                    );
                    echoes.add(echo);

                    // 生成残影粒子
                    spawnEchoSpawnParticles(world, snapshot.x, snapshot.y, snapshot.z);
                }
            }

            ACTIVE_ECHOES.put(player.getUniqueID(), echoes);

            // 激活状态
            state.activateState(STATE_ECHOES_ACTIVE, ECHO_DURATION, () -> {
                // 残影消失
                ACTIVE_ECHOES.remove(player.getUniqueID());
                player.sendMessage(new TextComponentString(
                        TextFormatting.AQUA + "◈ 回声室: 残影消散"));
            });

            // 设置冷却
            state.setCooldown("echo_chamber", 45000);  // 45 秒

            // 增加排异值 (模块耐久消耗简化为排异)
            state.addRejection(5f);

            // 视觉效果
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_ILLUSION_ILLAGER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.0f, 1.0f);

            player.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "◈ 回声室: " + echoes.size() + " 个时间残影已召唤！"));
        }

        private void spawnEchoSpawnParticles(World world, double x, double y, double z) {
            for (int i = 0; i < 20; i++) {
                world.spawnParticle(EnumParticleTypes.SPELL_INSTANT,
                        x + RANDOM.nextGaussian() * 0.3,
                        y + 1 + RANDOM.nextGaussian() * 0.5,
                        z + RANDOM.nextGaussian() * 0.3,
                        0, 0.05, 0);
            }
        }

        @Override
        public String getDescription() {
            return "Summon temporal echoes of yourself";
        }

        // 残影数据类
        private static class EchoData {
            public double x, y, z;
            public float health;
            public int remainingTicks;

            public EchoData(double x, double y, double z, float health, int ticks) {
                this.x = x;
                this.y = y;
                this.z = z;
                this.health = health;
                this.remainingTicks = ticks;
            }
        }
    }
}
