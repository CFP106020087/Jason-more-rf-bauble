package com.moremod.system.ascension;

import com.moremod.config.BrokenGodConfig;
import com.moremod.item.ItemMechanicalCore;
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
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 破碎之神处理器
 * Broken God Handler
 *
 * 管理破碎之神升格的所有特殊逻辑：
 * - 停机模式 (Shutdown Mode)
 * - 升格条件检查
 * - 升格执行
 * - 扭曲脉冲冷却
 */
public class BrokenGodHandler {

    // ========== 状态追踪（用于 ASM 钩子可靠性） ==========

    /**
     * 备用破碎之神状态追踪
     * 当 Capability 不可用时使用此 Set 作为后备
     */
    private static final Set<UUID> brokenGodBackup = new HashSet<>();

    /**
     * 备用停机状态追踪
     * 当 Capability 不可用时使用此 Map 作为后备
     */
    private static final Map<UUID, Integer> shutdownBackup = new HashMap<>();

    /** 扭曲脉冲冷却追踪 */
    private static final Map<UUID, Integer> pulseCooldowns = new HashMap<>();

    // ========== 核心状态检查 ==========

    /**
     * 检查玩家是否是破碎之神
     * 同时检查 Capability 和备用 Set（确保 ASM 钩子可靠性）
     */
    public static boolean isBrokenGod(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        // 优先检查备用 Set（ASM 钩子可靠性）
        if (brokenGodBackup.contains(playerId)) {
            return true;
        }
        // 然后检查 Capability
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        boolean isBrokenGod = data != null && data.getAscensionRoute() == AscensionRoute.BROKEN_GOD;
        // 如果 Capability 确认是破碎之神，同步到备用 Set
        if (isBrokenGod) {
            brokenGodBackup.add(playerId);
        }
        return isBrokenGod;
    }

    /**
     * 注册玩家为破碎之神（在玩家登录时调用，确保备用追踪有效）
     */
    public static void registerBrokenGod(EntityPlayer player) {
        brokenGodBackup.add(player.getUniqueID());
    }

    /**
     * 取消注册破碎之神状态
     */
    public static void unregisterBrokenGod(EntityPlayer player) {
        brokenGodBackup.remove(player.getUniqueID());
    }

    /**
     * 检查玩家是否处于停机状态
     * 同时检查 Capability 和备用 Map（确保 ASM 钩子可靠性）
     */
    public static boolean isInShutdown(EntityPlayer player) {
        // 优先检查备用 Map（ASM 钩子可靠性）
        UUID playerId = player.getUniqueID();
        if (shutdownBackup.containsKey(playerId) && shutdownBackup.get(playerId) > 0) {
            return true;
        }
        // 然后检查 Capability
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        return data != null && data.isInShutdown();
    }

    /**
     * 获取停机剩余时间
     */
    public static int getShutdownTimer(EntityPlayer player) {
        // 优先从 Capability 获取
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data != null && data.getShutdownTimer() > 0) {
            return data.getShutdownTimer();
        }
        // 备用 Map
        return shutdownBackup.getOrDefault(player.getUniqueID(), 0);
    }

    // ========== 停机模式控制 ==========

    /**
     * 进入停机模式（代替死亡）
     */
    public static void enterShutdown(EntityPlayer player) {
        if (!isBrokenGod(player)) return;
        if (isInShutdown(player)) return;

        int shutdownTicks = BrokenGodConfig.shutdownTicks;
        UUID playerId = player.getUniqueID();

        // 设置备用 Map（确保 ASM 钩子可靠性）
        shutdownBackup.put(playerId, shutdownTicks);

        // 尝试设置 Capability
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data != null) {
            data.setShutdownTimer(shutdownTicks);
            // 同步到客户端（用于显示 overlay）
            if (player instanceof EntityPlayerMP) {
                PacketHandler.INSTANCE.sendTo(new PacketSyncHumanityData(data), (EntityPlayerMP) player);
            }
        }

        // 设置 HP 为 1（不是 0，避免触发死亡）
        player.setHealth(1.0f);

        // First Aid 兼容：设置所有身体部位为最小血量
        setFirstAidMinHealth(player);

        // 给予无敌帧，防止进入停机后立即被其他伤害杀死
        player.hurtResistantTime = Math.max(player.hurtResistantTime, 20);

        // 清除运动
        player.motionX = 0;
        player.motionY = 0;
        player.motionZ = 0;

        // 发送停机消息
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_GRAY + "═══════════════════════════════\n" +
                TextFormatting.GRAY + "[ SYSTEM SHUTDOWN ]\n" +
                TextFormatting.DARK_GRAY + "核心损伤检测...启动紧急重启协议\n" +
                TextFormatting.DARK_GRAY + "预计恢复时间: " + (BrokenGodConfig.shutdownTicks / 20) + " 秒\n" +
                TextFormatting.DARK_GRAY + "═══════════════════════════════"
        ));

        // 停机音效 - 使用铁傀儡受伤音效模拟机械停机
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                net.minecraft.init.SoundEvents.ENTITY_IRONGOLEM_HURT,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 0.3f);

        // 粒子效果
        if (player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    player.posX, player.posY + 1, player.posZ,
                    30, 0.5, 0.5, 0.5, 0.05);
        }
    }

    /**
     * 每 tick 更新停机状态
     */
    public static void tickShutdown(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        IHumanityData data = HumanityCapabilityHandler.getData(player);

        // 获取当前计时器（优先 Capability，备用 Map）
        int timer;
        if (data != null && data.getShutdownTimer() > 0) {
            timer = data.getShutdownTimer();
        } else {
            timer = shutdownBackup.getOrDefault(playerId, 0);
        }

        if (timer <= 0) {
            shutdownBackup.remove(playerId);
            return;
        }

        // 减少计时器
        timer--;

        // 同时更新 Capability 和备用 Map
        shutdownBackup.put(playerId, timer);
        if (data != null) {
            data.setShutdownTimer(timer);
            // 每5tick同步到客户端（用于 overlay 进度更新）
            if (timer % 5 == 0 && player instanceof EntityPlayerMP) {
                PacketHandler.INSTANCE.sendTo(new PacketSyncHumanityData(data), (EntityPlayerMP) player);
            }
        }

        // 停机期间：
        // - 禁止移动
        player.motionX = 0;
        player.motionY = Math.max(player.motionY, -0.1); // 允许轻微下落
        player.motionZ = 0;

        // - 保持最小血量
        if (player.getHealth() < 1.0f) {
            player.setHealth(1.0f);
        }

        // - First Aid 兼容：每 tick 确保所有身体部位不会降到 0
        setFirstAidMinHealth(player);

        // 停机粒子效果（每20tick）
        if (timer % 20 == 0 && player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;
            world.spawnParticle(EnumParticleTypes.REDSTONE,
                    player.posX, player.posY + 1, player.posZ,
                    5, 0.3, 0.3, 0.3, 0);
        }

        // 重启完成
        if (timer == 0) {
            exitShutdown(player);
        }
    }

    /**
     * 退出停机模式
     */
    private static void exitShutdown(EntityPlayer player) {
        UUID playerId = player.getUniqueID();

        // ⚠️ 重要：先恢复生命值和给予无敌帧，再清除停机状态
        // 这样可以防止在清除状态后立即被伤害杀死

        // First Aid 兼容：先恢复所有身体部位到满血（必须在 setHealth 之前）
        healFirstAidFull(player);

        // 恢复生命值（在清除保护状态之前）
        player.setHealth((float) BrokenGodConfig.restartHeal);

        // First Aid 兼容：再次确保所有部位满血（防止 setHealth 触发同步问题）
        healFirstAidFull(player);

        // 给予短暂无敌帧，防止退出停机后立即受伤
        player.hurtResistantTime = 60; // 3秒无敌帧（增加以确保安全）

        // 现在才清除备用 Map
        shutdownBackup.remove(playerId);

        // 清除 Capability
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data != null) {
            data.setShutdownTimer(0);
            // 同步到客户端
            if (player instanceof EntityPlayerMP) {
                PacketHandler.INSTANCE.sendTo(new PacketSyncHumanityData(data), (EntityPlayerMP) player);
            }
        }

        // 发送重启消息
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "═══════════════════════════════\n" +
                TextFormatting.GREEN + "[ SYSTEM REBOOT COMPLETE ]\n" +
                TextFormatting.GRAY + "所有系统已恢复运行\n" +
                TextFormatting.GRAY + "核心状态: " + TextFormatting.GREEN + "在线\n" +
                TextFormatting.GREEN + "═══════════════════════════════"
        ));

        // 重启音效 - 使用经验升级音效表示系统恢复
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                net.minecraft.init.SoundEvents.ENTITY_PLAYER_LEVELUP,
                net.minecraft.util.SoundCategory.PLAYERS, 0.8f, 1.2f);

        // 粒子效果
        if (player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;
            world.spawnParticle(EnumParticleTypes.END_ROD,
                    player.posX, player.posY + 1, player.posZ,
                    30, 0.5, 1, 0.5, 0.1);
        }
    }

    // ========== 扭曲脉冲冷却 ==========

    /**
     * 检查扭曲脉冲是否可用
     */
    public static boolean canUseDistortionPulse(EntityPlayer player) {
        return !pulseCooldowns.containsKey(player.getUniqueID()) ||
                pulseCooldowns.get(player.getUniqueID()) <= 0;
    }

    /**
     * 触发扭曲脉冲冷却
     */
    public static void triggerPulseCooldown(EntityPlayer player) {
        pulseCooldowns.put(player.getUniqueID(), BrokenGodConfig.pulseCooldown);
    }

    /**
     * 更新扭曲脉冲冷却
     */
    public static void tickPulseCooldown(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        if (pulseCooldowns.containsKey(playerId)) {
            int cd = pulseCooldowns.get(playerId);
            if (cd > 0) {
                pulseCooldowns.put(playerId, cd - 1);
            } else {
                pulseCooldowns.remove(playerId);
            }
        }
    }

    // ========== 升格条件检查 ==========

    /**
     * 检查是否满足破碎之神升格条件
     */
    public static boolean canAscend(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return false;

        // 已经升格
        if (data.getAscensionRoute() != AscensionRoute.NONE) return false;

        // 条件1: 人性值 <= 阈值
        float humanity = data.getHumanity();
        if (humanity > BrokenGodConfig.ascensionHumanityThreshold) return false;

        // 条件2: 低人性累计时间（秒）
        long lowHumanitySeconds = data.getLowHumanityTicks() / 20;
        if (lowHumanitySeconds < BrokenGodConfig.requiredLowHumanitySeconds) return false;

        // 条件3: 装备机械核心
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return false;

        // 条件4: 安装模块数量
        int installedCount = ItemMechanicalCore.getTotalInstalledUpgrades(core);
        if (installedCount < BrokenGodConfig.requiredModuleCount) return false;

        return true;
    }

    /**
     * 执行破碎之神升格
     */
    public static void performAscension(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null) return;

        // 添加到备用 Set（确保 ASM 钩子可靠性）
        brokenGodBackup.add(player.getUniqueID());

        // 设置升格路线
        data.setAscensionRoute(AscensionRoute.BROKEN_GOD);

        // 固定人性值为 0
        data.setHumanity(0);

        // 发送升格消息
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "═══════════════════════════════\n" +
                TextFormatting.BOLD + "" + TextFormatting.DARK_PURPLE + "[ Broken God ]\n" +
                TextFormatting.GRAY + "你的情绪熄灭。\n" +
                TextFormatting.GRAY + "你的灵魂沉静。\n" +
                TextFormatting.GRAY + "你成为了纯粹的力量。\n" +
                TextFormatting.DARK_PURPLE + "═══════════════════════════════"
        ));

        // 装备替换消息
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "\n所有饰品已被卸除。\n" +
                TextFormatting.DARK_RED + "你的身体被重组为破碎机械的圣像。"
        ));

        // 替换饰品
        BrokenGodItems.replacePlayerBaubles(player);

        // 粒子效果
        if (player.world instanceof WorldServer) {
            WorldServer world = (WorldServer) player.world;
            for (int i = 0; i < 100; i++) {
                double angle = (i / 100.0) * Math.PI * 2;
                double radius = 3.0 + (i % 10) * 0.3;
                double x = player.posX + Math.cos(angle) * radius;
                double z = player.posZ + Math.sin(angle) * radius;
                double y = player.posY + (i / 100.0) * 5;

                world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, 1, 0, 0, 0, 0.05);
            }
        }

        // 音效
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                net.minecraft.init.SoundEvents.ENTITY_WITHER_SPAWN,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 0.5f);
    }

    // ========== 清理 ==========

    /**
     * 玩家退出时清理
     */
    public static void cleanupPlayer(UUID playerId) {
        shutdownBackup.remove(playerId);
        pulseCooldowns.remove(playerId);
    }

    // ========== First Aid 兼容 ==========

    /** 检查 First Aid 是否已加载 */
    private static boolean isFirstAidLoaded() {
        return Loader.isModLoaded("firstaid");
    }

    /**
     * 治愈 First Aid 所有身体部位到满血
     * 在停机结束时调用，防止 First Aid 的延迟伤害结算导致死亡
     */
    public static void healFirstAidFull(EntityPlayer player) {
        if (!isFirstAidLoaded()) return;
        try {
            healFirstAidFullInternal(player);
        } catch (Throwable ignored) {
        }
    }

    @Optional.Method(modid = "firstaid")
    private static void healFirstAidFullInternal(EntityPlayer player) {
        try {
            if (!player.hasCapability(ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem.INSTANCE, null)) {
                return;
            }
            ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel model =
                    (ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel)
                            player.getCapability(ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem.INSTANCE, null);
            if (model == null) return;

            // 恢复所有部位到满血
            model.HEAD.currentHealth = model.HEAD.getMaxHealth();
            model.BODY.currentHealth = model.BODY.getMaxHealth();
            model.LEFT_ARM.currentHealth = model.LEFT_ARM.getMaxHealth();
            model.RIGHT_ARM.currentHealth = model.RIGHT_ARM.getMaxHealth();
            model.LEFT_LEG.currentHealth = model.LEFT_LEG.getMaxHealth();
            model.RIGHT_LEG.currentHealth = model.RIGHT_LEG.getMaxHealth();
        } catch (Throwable ignored) {
        }
    }

    /**
     * 设置 First Aid 所有身体部位为最小血量（1点）
     * 在进入停机时调用
     */
    public static void setFirstAidMinHealth(EntityPlayer player) {
        if (!isFirstAidLoaded()) return;
        try {
            setFirstAidMinHealthInternal(player);
        } catch (Throwable ignored) {
        }
    }

    @Optional.Method(modid = "firstaid")
    private static void setFirstAidMinHealthInternal(EntityPlayer player) {
        try {
            if (!player.hasCapability(ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem.INSTANCE, null)) {
                return;
            }
            ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel model =
                    (ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel)
                            player.getCapability(ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem.INSTANCE, null);
            if (model == null) return;

            // 设置所有部位为 1 点血量（防止任何部位为 0 触发死亡）
            model.HEAD.currentHealth = Math.max(1.0f, model.HEAD.currentHealth);
            model.BODY.currentHealth = Math.max(1.0f, model.BODY.currentHealth);
            model.LEFT_ARM.currentHealth = Math.max(1.0f, model.LEFT_ARM.currentHealth);
            model.RIGHT_ARM.currentHealth = Math.max(1.0f, model.RIGHT_ARM.currentHealth);
            model.LEFT_LEG.currentHealth = Math.max(1.0f, model.LEFT_LEG.currentHealth);
            model.RIGHT_LEG.currentHealth = Math.max(1.0f, model.RIGHT_LEG.currentHealth);
        } catch (Throwable ignored) {
        }
    }
}
