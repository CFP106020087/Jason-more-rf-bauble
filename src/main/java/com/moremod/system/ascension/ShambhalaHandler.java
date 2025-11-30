package com.moremod.system.ascension;

import com.moremod.config.ShambhalaConfig;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.network.PacketAscensionAnimation;
import com.moremod.network.PacketHandler;
import com.moremod.network.PacketSyncHumanityData;
import com.moremod.system.humanity.AscensionRoute;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 香巴拉处理器
 * Shambhala Handler
 *
 * 永恒齿轮圣化身 - 绝对的盾
 * Avatar of Eternal Gearwork Shambhala - The Absolute Shield
 *
 * 与破碎之神形成对偶：
 * - 破碎之神：人性归零，绝对的矛，全攻无守
 * - 香巴拉：人性圆满，绝对的盾，全守无攻
 *
 * 核心机制：只要有能量，就永远不会倒下
 */
public class ShambhalaHandler {

    // ========== 状态追踪（用于 ASM 钩子可靠性） ==========

    /** 备用香巴拉状态追踪 */
    private static final Set<UUID> shambhalaBackup = new HashSet<>();

    /** 能量护盾冷却（防止无限触发） */
    private static final Map<UUID, Integer> shieldCooldowns = new HashMap<>();

    /** 反伤循环防护标记 */
    private static final Set<UUID> reflectingPlayers = new HashSet<>();

    /** 香巴拉反伤伤害源标识 */
    public static final String SHAMBHALA_REFLECT_SOURCE = "shambhala_reflect";

    // ========== 核心状态检查 ==========

    /**
     * 检查玩家是否是香巴拉
     */
    public static boolean isShambhala(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        if (shambhalaBackup.contains(playerId)) {
            return true;
        }
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        boolean isShambhala = data != null && data.getAscensionRoute() == AscensionRoute.SHAMBHALA;
        if (isShambhala) {
            shambhalaBackup.add(playerId);
        }
        return isShambhala;
    }

    /**
     * 注册玩家为香巴拉
     */
    public static void registerShambhala(EntityPlayer player) {
        shambhalaBackup.add(player.getUniqueID());
    }

    /**
     * 取消注册香巴拉状态
     */
    public static void unregisterShambhala(EntityPlayer player) {
        shambhalaBackup.remove(player.getUniqueID());
    }

    // ========== 能量系统 ==========

    /**
     * 从机械核心消耗能量
     * @return 是否成功消耗
     */
    public static boolean consumeEnergy(EntityPlayer player, int amount) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return false;

        IEnergyStorage storage = ItemMechanicalCore.getEnergyStorage(core);
        if (storage == null) return false;

        int extracted = storage.extractEnergy(amount, true);
        if (extracted >= amount) {
            storage.extractEnergy(amount, false);
            return true;
        }
        return false;
    }

    /**
     * 获取当前能量
     */
    public static int getCurrentEnergy(EntityPlayer player) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return 0;

        IEnergyStorage storage = ItemMechanicalCore.getEnergyStorage(core);
        return storage != null ? storage.getEnergyStored() : 0;
    }

    /**
     * 检查是否有足够能量
     */
    public static boolean hasEnergy(EntityPlayer player, int amount) {
        return getCurrentEnergy(player) >= amount;
    }

    // ========== 能量护盾（核心机制） ==========

    /**
     * 尝试用能量吸收伤害
     * @param player 玩家
     * @param damage 原始伤害
     * @return 实际应受伤害（可能为0）
     */
    public static float tryAbsorbDamage(EntityPlayer player, float damage) {
        if (!isShambhala(player)) return damage;

        // 计算吸收伤害需要的能量
        int energyRequired = (int) (damage * ShambhalaConfig.energyPerDamage);

        // 尝试消耗能量
        if (consumeEnergy(player, energyRequired)) {
            // 完全吸收
            spawnAbsorbParticles(player);
            return 0;
        }

        // 能量不足，计算部分吸收
        int currentEnergy = getCurrentEnergy(player);
        if (currentEnergy > 0) {
            float absorbedDamage = (float) currentEnergy / ShambhalaConfig.energyPerDamage;
            consumeEnergy(player, currentEnergy); // 消耗所有剩余能量
            spawnAbsorbParticles(player);
            return Math.max(0, damage - absorbedDamage);
        }

        return damage;
    }

    /**
     * 检查是否正在执行反伤（防止循环）
     */
    public static boolean isReflecting(EntityPlayer player) {
        return reflectingPlayers.contains(player.getUniqueID());
    }

    /**
     * 检查伤害源是否来自香巴拉反伤
     */
    public static boolean isShambhalaReflectDamage(net.minecraft.util.DamageSource source) {
        if (source == null) return false;
        // 检查伤害类型名称
        if (SHAMBHALA_REFLECT_SOURCE.equals(source.getDamageType())) return true;
        // 检查是否是荆棘伤害且来源是香巴拉玩家
        if (source.getDamageType().equals("thorns") && source.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer src = (EntityPlayer) source.getTrueSource();
            return isShambhala(src);
        }
        return false;
    }

    /**
     * 反射伤害给攻击者（带AoE和循环防护）
     * @param damageSource 原始伤害源（用于检测循环）
     */
    public static void reflectDamage(EntityPlayer player, net.minecraft.entity.Entity attacker,
                                      float originalDamage, net.minecraft.util.DamageSource damageSource) {
        if (!isShambhala(player)) return;
        if (attacker == null) return;

        // 循环防护：检查是否是反伤造成的伤害
        if (isShambhalaReflectDamage(damageSource)) return;
        if (isReflecting(player)) return;

        // 标记正在反伤
        reflectingPlayers.add(player.getUniqueID());

        try {
            float reflectDamage = originalDamage * (float) ShambhalaConfig.thornsReflectMultiplier;
            double aoeRadius = ShambhalaConfig.thornsAoeRadius;

            // 计算总能量消耗（主目标 + AoE）
            int baseCost = (int) (reflectDamage * ShambhalaConfig.energyPerReflect);

            // 主目标反伤
            if (attacker instanceof net.minecraft.entity.EntityLivingBase) {
                if (consumeEnergy(player, baseCost)) {
                    applyReflectDamageToTarget(player, (net.minecraft.entity.EntityLivingBase) attacker, reflectDamage);
                } else {
                    return; // 没能量就不反伤
                }
            }

            // AoE 反伤
            if (aoeRadius > 0 && attacker instanceof net.minecraft.entity.EntityLivingBase) {
                net.minecraft.util.math.AxisAlignedBB aabb = new net.minecraft.util.math.AxisAlignedBB(
                        attacker.posX - aoeRadius, attacker.posY - aoeRadius, attacker.posZ - aoeRadius,
                        attacker.posX + aoeRadius, attacker.posY + aoeRadius, attacker.posZ + aoeRadius
                );

                java.util.List<net.minecraft.entity.EntityLivingBase> nearbyMobs =
                        player.world.getEntitiesWithinAABB(net.minecraft.entity.EntityLivingBase.class, aabb,
                                e -> e != player && e != attacker && !(e instanceof EntityPlayer));

                float aoeDamage = reflectDamage * 0.5f; // AoE伤害减半
                int aoeCost = (int) (aoeDamage * ShambhalaConfig.energyPerReflect);

                for (net.minecraft.entity.EntityLivingBase mob : nearbyMobs) {
                    if (consumeEnergy(player, aoeCost)) {
                        applyReflectDamageToTarget(player, mob, aoeDamage);
                    } else {
                        break; // 能量不足停止AoE
                    }
                }
            }

        } finally {
            // 清除反伤标记
            reflectingPlayers.remove(player.getUniqueID());
        }
    }

    /**
     * 对单个目标施加反伤
     */
    private static void applyReflectDamageToTarget(EntityPlayer player, net.minecraft.entity.EntityLivingBase target, float damage) {
        if (ShambhalaConfig.thornsTrueDamage) {
            // 使用包装的真实伤害
            com.moremod.combat.TrueDamageHelper.applyWrappedTrueDamage(
                    target, player, damage,
                    com.moremod.combat.TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE
            );
        } else {
            // 使用自定义的反伤伤害源
            net.minecraft.util.DamageSource reflectSource = new net.minecraft.util.DamageSource(SHAMBHALA_REFLECT_SOURCE);
            reflectSource.setDamageBypassesArmor();
            target.attackEntityFrom(reflectSource, damage);
        }

        // 反伤粒子效果
        if (player.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) player.world;
            ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                    target.posX, target.posY + target.height / 2, target.posZ,
                    15, 0.5, 0.5, 0.5, 0.1);
        }
    }

    /**
     * 旧版反射方法（兼容）
     */
    public static void reflectDamage(EntityPlayer player, net.minecraft.entity.Entity attacker, float originalDamage) {
        reflectDamage(player, attacker, originalDamage, null);
    }

    // ========== 升格条件检查 ==========

    /**
     * 检查是否满足香巴拉升格条件
     */
    public static boolean canAscend(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return false;

        // 已经升格
        if (data.getAscensionRoute() != AscensionRoute.NONE) return false;

        // 条件1: 人性值 >= 阈值
        float humanity = data.getHumanity();
        if (humanity < ShambhalaConfig.ascensionHumanityThreshold) return false;

        // 条件2: 高人性累计时间（秒）
        long highHumanityTicks = data.getHighHumanityTicks();
        long requiredTicks = ShambhalaConfig.requiredHighHumanitySeconds * 20L;
        if (highHumanityTicks < requiredTicks) return false;

        // 条件3: 装备机械核心
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return false;

        // 条件4: 安装模块数量
        int installedCount = ItemMechanicalCore.getTotalInstalledUpgrades(core);
        if (installedCount < ShambhalaConfig.requiredModuleCount) return false;

        return true;
    }

    /**
     * 获取升格进度信息
     */
    public static String[] getAscensionProgress(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null) return new String[]{"数据不可用"};

        float humanity = data.getHumanity();
        long highHumanityTicks = data.getHighHumanityTicks();
        long requiredTicks = ShambhalaConfig.requiredHighHumanitySeconds * 20L;
        long secondsProgress = highHumanityTicks / 20;

        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        int modules = core.isEmpty() ? 0 : ItemMechanicalCore.getTotalInstalledUpgrades(core);

        return new String[]{
                String.format("人性值: %.1f%% / %.1f%%", humanity, ShambhalaConfig.ascensionHumanityThreshold),
                String.format("高人性时间: %d / %d 秒", secondsProgress, ShambhalaConfig.requiredHighHumanitySeconds),
                String.format("模块数量: %d / %d", modules, ShambhalaConfig.requiredModuleCount)
        };
    }

    /**
     * 执行香巴拉升格
     */
    public static void performAscension(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null) return;

        // 添加到备用 Set
        shambhalaBackup.add(player.getUniqueID());

        // 设置升格路线
        data.setAscensionRoute(AscensionRoute.SHAMBHALA);

        // 固定人性值为 100
        data.setHumanity(100);

        // 发送升格动画包到客户端（使用香巴拉专属动画）
        if (player instanceof EntityPlayerMP) {
            PacketHandler.INSTANCE.sendTo(PacketAscensionAnimation.shambhala(), (EntityPlayerMP) player);
        }

        // 发送升格消息
        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "═══════════════════════════════════\n" +
                TextFormatting.AQUA + "" + TextFormatting.BOLD + "  [ 机巧香巴拉 · 永恒齿轮圣化身 ]\n" +
                TextFormatting.WHITE + "  Avatar of Eternal Gearwork Shambhala\n\n" +
                TextFormatting.GRAY + "  你已成为机械与人性的完美容器。\n" +
                TextFormatting.GRAY + "  只要齿轮仍在转动，你便永不倒下。\n\n" +
                TextFormatting.YELLOW + "  ◆ 获得：绝对防御、因果反噬、净化之力\n" +
                TextFormatting.RED + "  ◆ 代价：伤害输出削弱、防御消耗能量\n" +
                TextFormatting.GOLD + "═══════════════════════════════════"
        ));

        // 装备替换消息
        player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "\n所有饰品已被卸除。\n" +
                TextFormatting.AQUA + "你的身体被重构为永恒机械的圣像。"
        ));

        // 替换饰品
        ShambhalaItems.replacePlayerBaubles(player);

        // 粒子效果 - 金色光环
        if (player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;
            for (int i = 0; i < 100; i++) {
                double angle = (i / 100.0) * Math.PI * 2;
                double radius = 2.0 + (i % 10) * 0.2;
                double x = player.posX + Math.cos(angle) * radius;
                double z = player.posZ + Math.sin(angle) * radius;
                double y = player.posY + (i / 100.0) * 3;

                world.spawnParticle(EnumParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0.02);
            }
            // 中心光柱
            for (int y = 0; y < 50; y++) {
                world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                        player.posX, player.posY + y * 0.2, player.posZ,
                        3, 0.1, 0, 0.1, 0.01);
            }
        }

        // 音效 - 信标激活
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                net.minecraft.init.SoundEvents.BLOCK_BEACON_ACTIVATE,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    // ========== 粒子效果 ==========

    private static void spawnAbsorbParticles(EntityPlayer player) {
        if (player.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) player.world;
            ws.spawnParticle(EnumParticleTypes.END_ROD,
                    player.posX, player.posY + 1, player.posZ,
                    10, 0.5, 0.5, 0.5, 0.05);
        }
    }

    // ========== 清理 ==========

    /**
     * 玩家退出时清理
     */
    public static void cleanupPlayer(UUID playerId) {
        shambhalaBackup.remove(playerId);
        shieldCooldowns.remove(playerId);
    }

    /**
     * 世界卸载时清空所有静态状态
     */
    public static void clearAllState() {
        shambhalaBackup.clear();
        shieldCooldowns.clear();
        reflectingPlayers.clear();
    }
}
