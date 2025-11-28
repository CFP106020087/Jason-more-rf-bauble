package com.moremod.system.humanity;

import com.moremod.config.BrokenGodConfig;
import com.moremod.config.HumanityConfig;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.network.PacketHandler;
import com.moremod.network.PacketSyncHumanityData;
import com.moremod.system.FleshRejectionSystem;
import com.moremod.util.BaublesSyncUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.*;

/**
 * 人性值光谱系统 - 核心逻辑
 * Humanity Spectrum System - Core Logic
 *
 * 人性值不是道德判断，而是「你在世界物理规则中属于哪个分类」
 * - 高人性(>60%) = 猎人协议路线，精准打击，研究敌人
 * - 低人性(<40%) = 异常协议路线，纯粹暴力，存在扭曲
 * - 灰域(40-60%) = 不稳定状态，量子叠加
 */
public class HumanitySpectrumSystem {

    // ========== 同步优化系统（与FleshRejectionSystem一致的模式）==========

    /** 脏标记：玩家UUID → 是否需要同步 */
    private static final Set<UUID> dirtyPlayers = new HashSet<>();

    /** 强制同步标记：需要立即同步的玩家 */
    private static final Set<UUID> forceSyncPlayers = new HashSet<>();

    /** 同步冷却：玩家UUID → 剩余tick */
    private static final Map<UUID, Integer> syncCooldown = new HashMap<>();

    /** 正常同步间隔：20tick = 1秒 */
    private static final int SYNC_INTERVAL = 20;

    /** 强制同步间隔：5tick = 0.25秒 */
    private static final int FORCE_SYNC_INTERVAL = 5;

    /** 战斗状态超时（tick）：5秒 */
    public static final int COMBAT_TIMEOUT = 100;

    /** 异常维度列表 */
    private static final Set<Integer> ABNORMAL_DIMENSIONS = new HashSet<>();

    static {
        // 默认异常维度：末地、下界
        ABNORMAL_DIMENSIONS.add(-1); // 下界
        ABNORMAL_DIMENSIONS.add(1);  // 末地
        // 私人维度等可以在初始化时添加
    }

    /**
     * 添加异常维度
     */
    public static void addAbnormalDimension(int dimId) {
        ABNORMAL_DIMENSIONS.add(dimId);
    }

    // ========== 同步管理 ==========

    /**
     * 每tick调用一次，处理批量同步
     */
    public static void tickSyncSystem(EntityPlayerMP player) {
        UUID playerId = player.getUniqueID();

        // 检查强制同步
        if (forceSyncPlayers.contains(playerId)) {
            int cooldown = syncCooldown.getOrDefault(playerId, 0);
            if (cooldown <= 0) {
                performSync(player);
                forceSyncPlayers.remove(playerId);
                dirtyPlayers.remove(playerId);
                syncCooldown.put(playerId, FORCE_SYNC_INTERVAL);
            }
            return;
        }

        // 检查正常同步
        if (dirtyPlayers.contains(playerId)) {
            int cooldown = syncCooldown.getOrDefault(playerId, 0);
            if (cooldown <= 0) {
                performSync(player);
                dirtyPlayers.remove(playerId);
                syncCooldown.put(playerId, SYNC_INTERVAL);
            }
        }

        // 减少冷却
        if (syncCooldown.containsKey(playerId)) {
            int cooldown = syncCooldown.get(playerId) - 1;
            if (cooldown <= 0) {
                syncCooldown.remove(playerId);
            } else {
                syncCooldown.put(playerId, cooldown);
            }
        }
    }

    private static void markDirty(EntityPlayer player) {
        dirtyPlayers.add(player.getUniqueID());
    }

    private static void markForceSync(EntityPlayer player) {
        forceSyncPlayers.add(player.getUniqueID());
    }

    /**
     * 强制同步玩家的人性值数据到客户端
     * 用于玩家登录时的初始同步
     */
    public static void forceSync(EntityPlayer player) {
        markForceSync(player);
    }

    /**
     * 立即同步玩家的人性值数据到客户端（不等待 tick）
     * 用于命令等需要即时反馈的场景
     */
    public static void syncNow(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            performSync((EntityPlayerMP) player);
            // 清除脏标记
            UUID playerId = player.getUniqueID();
            dirtyPlayers.remove(playerId);
            forceSyncPlayers.remove(playerId);
        }
    }

    private static void performSync(EntityPlayerMP player) {
        BaublesSyncUtil.safeSyncAll(player);

        // 同步人性值数据到客户端
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data != null) {
            PacketHandler.INSTANCE.sendTo(new PacketSyncHumanityData(data), player);
        }
    }

    public static void cleanupPlayer(UUID playerId) {
        dirtyPlayers.remove(playerId);
        forceSyncPlayers.remove(playerId);
        syncCooldown.remove(playerId);
    }

    // ========== 核心检查 ==========

    /**
     * 检查玩家是否已激活人性值系统
     *
     * 人性值系统只在以下条件下激活：
     * 1. 配置已启用
     * 2. 玩家装备了机械核心
     * 3. 排异系统已突破 (hasTranscended == true)
     * 4. 排异值为0 (RejectionLevel == 0)
     * 5. 内部状态 systemActive == true
     *
     * 当排异重新激活（适应度下降）时，人性值系统必须完全关闭
     */
    public static boolean isSystemActive(EntityPlayer player) {
        if (!HumanityConfig.enableHumanitySystem) return false;

        // 必须装备机械核心
        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) return false;

        // 检查排异系统状态 - 必须已突破且排异值为0
        boolean transcended = FleshRejectionSystem.hasTranscended(player);
        float rejectionLevel = FleshRejectionSystem.getRejectionLevel(player);

        // 如果未突破或排异值>0，人性值系统不应该激活
        if (!transcended || rejectionLevel > 0) {
            return false;
        }

        IHumanityData data = HumanityCapabilityHandler.getData(player);
        return data != null && data.isSystemActive();
    }

    /**
     * 获取人性值
     */
    public static float getHumanity(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        return data != null ? data.getHumanity() : 75f;
    }

    /**
     * 设置人性值
     */
    public static void setHumanity(EntityPlayer player, float value) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        // 检查存在锚定
        if (data.isExistenceAnchored(player.world.getTotalWorldTime())) {
            value = Math.max(10f, value); // 锚定期间不能低于10%
        }

        data.setHumanity(value);
        markDirty(player);
    }

    /**
     * 修改人性值
     */
    public static void modifyHumanity(EntityPlayer player, float delta) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        float newValue = data.getHumanity() + delta;

        // 检查存在锚定
        if (data.isExistenceAnchored(player.world.getTotalWorldTime())) {
            newValue = Math.max(10f, newValue);
        }

        data.setHumanity(newValue);
        markDirty(player);

        // 检查崩解（破碎之神不受崩解影响）
        if (data.getHumanity() <= 0f && !data.isDissolutionActive()
                && data.getAscensionRoute() != AscensionRoute.BROKEN_GOD) {
            startDissolution(player);
        }
    }

    // ========== 激活系统 ==========

    /**
     * 激活人性值系统（在排异系统突破时调用）
     */
    public static void activateSystem(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || data.isSystemActive()) return;

        // 检查是否是首次激活（人性值为默认值或0）
        float currentHumanity = data.getHumanity();
        boolean isFirstActivation = (currentHumanity == HumanityDataImpl.DEFAULT_HUMANITY || currentHumanity <= 0);

        data.activateSystem();

        // 只有首次激活时才重置为初始值，否则保留之前的人性值
        if (isFirstActivation) {
            data.setHumanity((float) HumanityConfig.initialHumanity);
        }

        // 发送消息 - 使用实际人性值
        float displayHumanity = data.getHumanity();
        player.sendMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "═══════════════════════════════\n" +
                TextFormatting.BOLD + "" + TextFormatting.LIGHT_PURPLE + "【人机融合完成】\n" +
                TextFormatting.GRAY + "你的肉体已接纳机械。\n" +
                TextFormatting.GRAY + "但世界开始重新审视你的存在...\n" +
                TextFormatting.YELLOW + "人性值系统已激活。当前人性值: " +
                String.format("%.0f%%", displayHumanity) + "\n" +
                TextFormatting.LIGHT_PURPLE + "═══════════════════════════════"
        ));

        // 粒子效果
        if (player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;
            for (int i = 0; i < 30; i++) {
                double offsetX = (world.rand.nextDouble() - 0.5) * 2;
                double offsetY = world.rand.nextDouble() * 2;
                double offsetZ = (world.rand.nextDouble() - 0.5) * 2;

                world.spawnParticle(
                        EnumParticleTypes.END_ROD,
                        player.posX + offsetX,
                        player.posY + offsetY,
                        player.posZ + offsetZ,
                        3, 0, 0.1, 0, 0.05
                );
            }
        }

        markForceSync(player);
    }

    /**
     * 停用人性值系统（当排异重新激活时调用）
     *
     * 执行完整的状态重置：
     * - 设置 systemActive = false
     * - 终止崩解状态
     * - 取消分析进度
     * - 清除临时状态
     * - 强制同步到客户端
     */
    public static void deactivateSystem(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null) return;

        // 调用 IHumanityData 的停用方法（执行完整重置）
        data.deactivateSystem();

        // 发送消息通知玩家
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "═══════════════════════════════\n" +
                TextFormatting.BOLD + "" + TextFormatting.DARK_RED + "【人性值系统已关闭】\n" +
                TextFormatting.GRAY + "排异反应重新激活，人机融合状态中断。\n" +
                TextFormatting.GRAY + "人性值系统进入休眠状态...\n" +
                TextFormatting.DARK_RED + "═══════════════════════════════"
        ));

        // 粒子效果
        if (player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;
            for (int i = 0; i < 20; i++) {
                double offsetX = (world.rand.nextDouble() - 0.5) * 2;
                double offsetY = world.rand.nextDouble() * 2;
                double offsetZ = (world.rand.nextDouble() - 0.5) * 2;

                world.spawnParticle(
                        EnumParticleTypes.SMOKE_LARGE,
                        player.posX + offsetX,
                        player.posY + offsetY,
                        player.posZ + offsetZ,
                        3, 0, 0.1, 0, 0.02
                );
            }
        }

        // 强制同步到客户端
        markForceSync(player);
    }

    // ========== 崩解机制 ==========

    /**
     * 开始崩解状态
     */
    public static void startDissolution(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        // 使用配置的崩解持续时间（秒转换为tick）
        int durationTicks = HumanityConfig.dissolutionDuration * 20;
        data.startDissolution(durationTicks);

        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "═══════════════════════════════\n" +
                TextFormatting.BOLD + "" + TextFormatting.DARK_RED + "【存在崩解】\n" +
                TextFormatting.GRAY + "你的存在正在从世界法则中脱落...\n" +
                TextFormatting.RED + "在 " + HumanityConfig.dissolutionDuration + " 秒内存活，\n" +
                TextFormatting.RED + "或者面对永恒的虚无。\n" +
                TextFormatting.DARK_PURPLE + "═══════════════════════════════"
        ));

        markForceSync(player);
    }

    /**
     * 处理崩解状态tick（每秒调用一次）
     */
    public static void tickDissolution(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isDissolutionActive()) return;

        int remaining = data.getDissolutionTicks();

        if (remaining <= 0) {
            // 崩解结束，存活
            endDissolutionSurvived(player);
            return;
        }

        // 每秒减少20 ticks（因为此方法每秒调用一次）
        data.setDissolutionTicks(remaining - 20);

        // 周期性崩解伤害（以秒为单位检查）
        int remainingSeconds = remaining / 20;
        int damageIntervalSeconds = HumanityConfig.dissolutionDamageInterval;
        if (damageIntervalSeconds > 0 && remainingSeconds % damageIntervalSeconds == 0) {
            float maxHealth = player.getMaxHealth();
            float damage = maxHealth * (float) HumanityConfig.dissolutionDamagePercent;
            if (damage > 0) {
                player.attackEntityFrom(DamageSourceDissolution.DISSOLUTION, damage);
            }
        }

        // 发送倒计时提醒
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.DARK_RED + "【崩解中】" + TextFormatting.RED + remainingSeconds + "秒"
        ), true);

        // 粒子效果
        if (player.world instanceof WorldServer) {
            spawnDissolutionParticles(player);
        }

        markDirty(player);
    }

    /**
     * 崩解存活结束
     */
    private static void endDissolutionSurvived(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null) return;

        data.endDissolution(true);
        data.setHumanity(15f);

        // 应用存在锚定（24小时）
        long anchorDuration = (long) HumanityConfig.existenceAnchorDuration * 60 * 60 * 20;
        data.setExistenceAnchorUntil(player.world.getTotalWorldTime() + anchorDuration);

        player.sendMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "【崩解终止】" + TextFormatting.GRAY +
                " 你的存在被强制锚定。" + HumanityConfig.existenceAnchorDuration +
                "小时内无法再次进入崩解状态。"
        ));

        markForceSync(player);
    }

    /**
     * 崩解中死亡处理（在PlayerEvent.Clone中调用）
     */
    public static void handleDissolutionDeath(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null) return;

        if (data.isDissolutionActive()) {
            data.endDissolution(false);
            data.setHumanity(50f);
        }
    }

    // ========== 主更新逻辑 ==========

    /**
     * 每秒更新（在PlayerTickEvent中调用）
     */
    public static void updateHumanity(EntityPlayer player) {
        if (player.world.isRemote) return;
        if (!HumanityConfig.enableHumanitySystem) return;
        if (player.ticksExisted % 20 != 0) return;

        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        // 破碎之神不受人性值系统影响（人性值固定为0）
        if (data.getAscensionRoute() == AscensionRoute.BROKEN_GOD) {
            return;
        }

        // 处理崩解状态
        if (data.isDissolutionActive()) {
            tickDissolution(player);
            return;
        }

        float delta = 0f;

        // 异常维度滞留
        if (isInAbnormalDimension(player)) {
            delta -= HumanityConfig.abnormalDimensionDrainPerSec;
        }

        // 日光恢复
        if (isInSunlight(player)) {
            delta += HumanityConfig.sunlightRestorePerSec;
        }

        // 低人性战斗消耗
        float humanity = data.getHumanity();
        if (humanity < 50f && data.isInCombat(player.ticksExisted, COMBAT_TIMEOUT)) {
            delta -= HumanityConfig.combatDrainPerSec;
        }

        // 熬夜消耗
        ((HumanityDataImpl) data).tickSleepDeprivation();
        long ticksSinceSleep = data.getTicksSinceLastSleep();
        long deprivationStart = (long) HumanityConfig.sleepDeprivationStartMinutes * 60 * 20;
        if (ticksSinceSleep > deprivationStart) {
            delta -= HumanityConfig.sleepDeprivationDrainPerSec;
        }

        // 处理分析进度
        tickAnalysis(player, data);

        // 应用变化
        if (delta != 0) {
            modifyHumanity(player, delta);
        }

        // 追踪低人性累计时间（用于破碎之神升格条件）
        // 注意：此方法每秒调用一次（ticksExisted % 20 == 0）
        if (humanity < BrokenGodConfig.lowHumanityThreshold) {
            // 每秒累加20 tick
            data.addLowHumanityTicks(20);
        }

        // 调试模式
        if (HumanityConfig.debugMode) {
            long lowHumanitySeconds = data.getLowHumanityTicks() / 20;
            player.sendStatusMessage(new TextComponentString(
                    String.format("§7[人性] %.1f%% | 变化:%.3f/s | 战斗:%s | 低人性时间:%ds",
                            data.getHumanity(), delta,
                            data.isInCombat(player.ticksExisted, COMBAT_TIMEOUT) ? "是" : "否",
                            lowHumanitySeconds)
            ), true);
        }
    }

    /**
     * 处理分析进度
     */
    private static void tickAnalysis(EntityPlayer player, IHumanityData data) {
        ResourceLocation analyzing = data.getAnalyzingEntity();
        if (analyzing == null) return;

        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;

        // 获取可用能量
        net.minecraftforge.energy.IEnergyStorage energyStorage = ItemMechanicalCore.getEnergyStorage(core);
        if (energyStorage == null) return;
        int availableEnergy = energyStorage.getEnergyStored();
        if (availableEnergy <= 0) return;

        // 消耗能量并推进分析
        int consumed = data.tickAnalysis(availableEnergy);
        if (consumed > 0) {
            energyStorage.extractEnergy(consumed, false);

            // 检查是否完成
            if (data.getAnalyzingEntity() == null) {
                // 分析完成
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "【分析完成】" + TextFormatting.WHITE +
                        " 生物档案已升级: " + analyzing.toString()
                ));

                if (player.world instanceof WorldServer) {
                    WorldServer world = (WorldServer) player.world;
                    world.playSound(null, player.posX, player.posY, player.posZ,
                            net.minecraft.init.SoundEvents.ENTITY_PLAYER_LEVELUP,
                            net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 1.5f);
                }
            }

            markDirty(player);
        }
    }

    // ========== 环境检测 ==========

    /**
     * 检查是否在异常维度
     */
    public static boolean isInAbnormalDimension(EntityPlayer player) {
        return ABNORMAL_DIMENSIONS.contains(player.world.provider.getDimension());
    }

    /**
     * 检查是否在阳光下
     */
    public static boolean isInSunlight(EntityPlayer player) {
        World world = player.world;

        // 必须是主世界
        if (world.provider.getDimension() != 0) return false;

        // 必须是白天
        if (!world.isDaytime()) return false;

        // 必须能看到天空
        return world.canSeeSky(player.getPosition());
    }

    // ========== 战斗系统 ==========

    /**
     * 记录战斗状态
     */
    public static void markCombat(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data != null && data.isSystemActive()) {
            data.setLastCombatTime(player.ticksExisted);
        }
    }

    /**
     * 计算伤害修正（高人性：猎人协议）
     */
    public static float calculateHunterProtocolDamageMultiplier(EntityPlayer player, EntityLivingBase target) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return 1.0f;

        float humanity = data.getHumanity();
        if (humanity < 40f) return 1.0f; // 低人性不使用猎人协议

        ResourceLocation entityId = net.minecraft.entity.EntityList.getKey(target);
        if (entityId == null) return 1.0f;

        BiologicalProfile profile = data.getProfile(entityId);

        if (profile != null && data.getActiveProfiles().contains(entityId)) {
            return 1.0f + profile.getDamageBonus();
        } else {
            // 对未分析生物的惩罚
            return 1.0f - (float) HumanityConfig.unknownEnemyPenalty;
        }
    }

    /**
     * 检查猎人协议暴击
     */
    public static boolean checkHunterProtocolCrit(EntityPlayer player, EntityLivingBase target) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return false;

        float humanity = data.getHumanity();
        if (humanity < 40f) return false;

        ResourceLocation entityId = net.minecraft.entity.EntityList.getKey(target);
        if (entityId == null) return false;

        BiologicalProfile profile = data.getProfile(entityId);
        if (profile == null || !data.getActiveProfiles().contains(entityId)) return false;

        float critChance = profile.getCritBonus();
        return critChance > 0 && player.world.rand.nextFloat() < critChance;
    }

    /**
     * 计算伤害修正（低人性：异常协议）
     */
    public static float calculateAnomalyProtocolDamageMultiplier(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return 1.0f;

        float humanity = data.getHumanity();
        if (humanity >= 50f) return 1.0f;

        // 基础伤害加成
        float anomalyBonus;
        if (humanity <= 10f) {
            anomalyBonus = 0.60f;  // +60%
        } else if (humanity <= 25f) {
            anomalyBonus = 0.40f;  // +40%
        } else {
            anomalyBonus = 0.20f;  // +20%
        }

        return 1.0f + anomalyBonus;
    }

    /**
     * 检查畸变脉冲触发
     */
    public static boolean checkDistortionPulse(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return false;

        float humanity = data.getHumanity();
        if (humanity > 10f) return false;

        return player.world.rand.nextFloat() < HumanityConfig.distortionPulseChance;
    }

    /**
     * 触发畸变脉冲
     */
    public static void triggerDistortionPulse(EntityPlayer player) {
        float damage = (float) player.getEntityAttribute(
                net.minecraft.entity.SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue() * 0.5f;

        List<EntityLivingBase> entities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class,
                player.getEntityBoundingBox().grow(3.0),
                e -> e != player
        );

        for (EntityLivingBase entity : entities) {
            // 真实伤害，无视护甲 - 使用自定义伤害源防止递归
            entity.attackEntityFrom(HumanityEventHandler.DISTORTION_DAMAGE, damage);
        }

        // 粒子效果
        if (player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;
            for (int i = 0; i < 20; i++) {
                double angle = (i / 20.0) * Math.PI * 2;
                double x = player.posX + Math.cos(angle) * 3;
                double z = player.posZ + Math.sin(angle) * 3;

                world.spawnParticle(
                        EnumParticleTypes.PORTAL,
                        x, player.posY + 1, z,
                        5, 0, 0.5, 0, 0.1
                );
            }
        }

        // 音效
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                net.minecraft.init.SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 0.5f);
    }

    // ========== 异常场效果 ==========

    /**
     * 应用异常场效果（在PlayerTickEvent中调用）
     */
    public static void applyAnomalyFieldEffect(EntityPlayer player) {
        if (player.world.isRemote) return;

        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        float humanity = data.getHumanity();
        if (humanity >= 50f) return;

        // 灰域间歇性激活
        if (humanity >= 40f) {
            int cycle = (int) (player.ticksExisted % (30 * 20));
            if (cycle >= 10 * 20) return;
        }

        // 计算半径
        float radius = (float) ((50f - humanity) / 10f);

        List<EntityLivingBase> entities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class,
                player.getEntityBoundingBox().grow(radius),
                e -> e != player && !(e instanceof EntityPlayer && ((EntityPlayer) e).isOnSameTeam(player))
        );

        for (EntityLivingBase entity : entities) {
            // 减速效果
            int slownessLevel;
            if (humanity <= 10f) {
                slownessLevel = 2;
            } else if (humanity <= 25f) {
                slownessLevel = 1;
            } else {
                slownessLevel = 0;
            }

            entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 30, slownessLevel, true, false));

            // 凋零效果 (25%以下)
            if (humanity <= 25f) {
                entity.addPotionEffect(new PotionEffect(MobEffects.WITHER, 40, 0, true, false));
            }
        }
    }

    // ========== 样本系统 ==========

    /**
     * 处理生物死亡时的样本掉落
     */
    public static void handleEntityKill(EntityPlayer player, EntityLivingBase target) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        float humanity = data.getHumanity();
        ResourceLocation entityId = net.minecraft.entity.EntityList.getKey(target);
        if (entityId == null) return;

        // 记录击杀
        data.incrementKillCount(entityId);

        // 低人性无法获得样本
        if (humanity < 40f) return;

        // 计算掉率
        float dropChance = (float) HumanityConfig.baseSampleDropRate +
                (humanity / 10f) * (float) HumanityConfig.humanityDropBonus;

        // 灰域减半
        if (humanity < 60f) {
            dropChance *= (float) HumanityConfig.greyZoneDropMultiplier;
        }

        // Boss必掉
        String entityPath = entityId.toString().toLowerCase();
        if (BiologicalProfile.isBossEntity(entityPath)) {
            dropChance = 1.0f;
        }

        if (player.world.rand.nextFloat() < dropChance) {
            data.addSample(entityId);

            // 通知玩家
            BiologicalProfile profile = data.getProfile(entityId);
            if (profile != null) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "【样本获取】" + TextFormatting.WHITE +
                        " " + entityId.getPath() +
                        TextFormatting.GRAY + " (样本数: " + profile.getSampleCount() +
                        ", 等级: " + profile.getCurrentTier().displayNameCN + ")"
                ));
            }

            markDirty(player);
        }
    }

    // ========== 人性值变化事件 ==========

    /**
     * 击杀被动生物
     */
    public static void onKillPassiveMob(EntityPlayer player, Entity target) {
        if (target instanceof EntityVillager) {
            modifyHumanity(player, -(float) HumanityConfig.killVillagerDrain);
        } else if (target instanceof EntityAnimal) {
            modifyHumanity(player, -(float) HumanityConfig.killPassiveMobDrain);
        }
    }

    /**
     * 睡眠恢复
     */
    public static void onPlayerSleep(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        float currentHumanity = data.getHumanity();
        float restoreAmount = (float) HumanityConfig.sleepRestore;
        float cap = (float) HumanityConfig.sleepRestoreCap;

        float newHumanity = Math.min(currentHumanity + restoreAmount, cap);
        data.setHumanity(newHumanity);
        data.resetSleepDeprivation();

        markDirty(player);
    }

    /**
     * 进食恢复
     */
    public static void onPlayerEat(EntityPlayer player, ItemStack food) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        float restore = 0f;

        // 金苹果等
        String itemName = food.getItem().getRegistryName().toString().toLowerCase();
        if (itemName.contains("golden") || itemName.contains("enchanted")) {
            restore = (float) HumanityConfig.goldenFoodRestore;
        }
        // 复杂料理（检测模组食物）
        else if (itemName.contains("harvestcraft") || itemName.contains("pam") ||
                itemName.contains("cuisine") || itemName.contains("meal")) {
            restore = (float) HumanityConfig.complexMealRestore;
        }
        // 普通熟食
        else if (itemName.contains("cooked") || itemName.contains("steak") ||
                itemName.contains("bread")) {
            restore = (float) HumanityConfig.cookedFoodRestore;
        }

        if (restore > 0) {
            modifyHumanity(player, restore);
        }
    }

    /**
     * 与村民交易
     */
    public static void onVillagerTrade(EntityPlayer player) {
        modifyHumanity(player, (float) HumanityConfig.villagerTradeRestore);
    }

    /**
     * 收获作物
     */
    public static void onHarvestCrop(EntityPlayer player) {
        modifyHumanity(player, (float) HumanityConfig.harvestRestore);
    }

    /**
     * 喂养动物
     */
    public static void onFeedAnimal(EntityPlayer player) {
        modifyHumanity(player, (float) HumanityConfig.feedAnimalRestore);
    }

    // ========== 灰域机制 ==========

    /**
     * 检查量子叠加（致命伤害时调用）
     * @return true表示取消伤害
     */
    public static boolean checkQuantumCollapse(EntityPlayer player, float damage) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return false;

        float humanity = data.getHumanity();
        if (humanity < 40f || humanity > 60f) return false;

        // 检查是否致命
        if (player.getHealth() - damage > 0) return false;

        // 概率触发
        if (player.world.rand.nextFloat() >= HumanityConfig.quantumCollapseChance) return false;

        // 人性随机偏移
        float range = (float) HumanityConfig.quantumCollapseShiftRange;
        float shift = (player.world.rand.nextFloat() - 0.5f) * 2 * range;
        data.modifyHumanity(shift);

        // 效果
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                net.minecraft.init.SoundEvents.BLOCK_END_PORTAL_SPAWN,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 2.0f);

        if (player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;
            for (int i = 0; i < 30; i++) {
                world.spawnParticle(
                        EnumParticleTypes.END_ROD,
                        player.posX + (world.rand.nextDouble() - 0.5) * 2,
                        player.posY + world.rand.nextDouble() * 2,
                        player.posZ + (world.rand.nextDouble() - 0.5) * 2,
                        3, 0, 0.5, 0, 0.1
                );
            }
        }

        String direction = shift > 0 ? "§b人性回升" : "§5坠入深渊";
        player.sendMessage(new TextComponentString("§d【观测坍缩】§r " + direction +
                String.format(" (%.1f%%)", data.getHumanity())));

        markForceSync(player);
        return true;
    }

    // ========== 粒子效果 ==========

    private static void spawnDissolutionParticles(EntityPlayer player) {
        if (!(player.world instanceof WorldServer)) return;

        WorldServer world = (WorldServer) player.world;

        for (int i = 0; i < 10; i++) {
            double x = player.posX + (world.rand.nextDouble() - 0.5) * 2;
            double y = player.posY + world.rand.nextDouble() * 2;
            double z = player.posZ + (world.rand.nextDouble() - 0.5) * 2;

            world.spawnParticle(
                    EnumParticleTypes.END_ROD,
                    x, y, z,
                    1,
                    (world.rand.nextDouble() - 0.5) * 0.5,
                    world.rand.nextDouble() * 0.5,
                    (world.rand.nextDouble() - 0.5) * 0.5,
                    0.1
            );
        }
    }

    // ========== 状态查询 ==========

    /**
     * 获取人性值状态
     */
    public static HumanityStatus getStatus(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return null;

        HumanityStatus status = new HumanityStatus();
        status.humanity = data.getHumanity();
        status.systemActive = data.isSystemActive();
        status.dissolutionActive = data.isDissolutionActive();
        status.dissolutionTicks = data.getDissolutionTicks();
        status.inCombat = data.isInCombat(player.ticksExisted, COMBAT_TIMEOUT);
        status.profileCount = data.getProfiles().size();
        status.activeProfileCount = data.getActiveProfiles().size();
        status.maxActiveProfiles = data.getMaxActiveProfiles();
        status.analyzingEntity = data.getAnalyzingEntity();
        status.analysisProgress = data.getAnalysisProgress();
        status.existenceAnchored = data.isExistenceAnchored(player.world.getTotalWorldTime());

        return status;
    }

    /**
     * 人性值状态数据类
     */
    public static class HumanityStatus {
        public float humanity;
        public boolean systemActive;
        public boolean dissolutionActive;
        public int dissolutionTicks;
        public boolean inCombat;
        public int profileCount;
        public int activeProfileCount;
        public int maxActiveProfiles;
        public ResourceLocation analyzingEntity;
        public int analysisProgress;
        public boolean existenceAnchored;

        @Override
        public String toString() {
            return String.format(
                    "人性:%.1f%% | 崩解:%s | 战斗:%s | 档案:%d/%d | 分析:%s",
                    humanity,
                    dissolutionActive ? "是(" + dissolutionTicks / 20 + "s)" : "否",
                    inCombat ? "是" : "否",
                    activeProfileCount, maxActiveProfiles,
                    analyzingEntity != null ? analysisProgress + "%" : "无"
            );
        }
    }

    /**
     * 获取同步统计
     */
    public static String getSyncStats() {
        return String.format("脏数据: %d | 强制同步: %d | 冷却中: %d",
                dirtyPlayers.size(), forceSyncPlayers.size(), syncCooldown.size());
    }
}
