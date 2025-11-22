package com.moremod.synergy.effect;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.upgrades.energy.EnergyDepletionManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 能量状态锁定效果 - 强制EMERGENCY模式
 *
 * 机制：
 * - Synergy激活期间，能量状态强制锁定在EMERGENCY
 * - 即使能量充满，仍然只有"重要模块"可用
 * - 高耗能模块（矿物透视、隐身、飞行）永久禁用
 * - 每10 tick检查一次
 */
public class EnergyStateLockEffect implements ISynergyEffect {

    private static final int CHECK_INTERVAL = 10; // 每10 tick检查一次

    // 被禁用的高耗能模块列表
    private static final String[] BANNED_UPGRADES = {
        "ORE_VISION",      // 矿物透视
        "STEALTH",         // 隐身
        "FLIGHT_MODULE"    // 飞行
    };

    @Override
    public String getEffectId() {
        return "energy_state_lock";
    }

    @Override
    @SubscribeEvent
    public void onPlayerTick(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        // 每10 tick检查一次
        if (player.ticksExisted % CHECK_INTERVAL != 0) return;

        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;

        // 1. 强制进入EMERGENCY状态
        forceEmergencyMode(core, player);

        // 2. 禁用高耗能模块
        disableBannedUpgrades(core, player);
    }

    /**
     * 强制进入EMERGENCY模式
     */
    private void forceEmergencyMode(ItemStack core, EntityPlayer player) {
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            core.setTagCompound(nbt);
        }

        // 设置强制紧急模式标记
        if (!nbt.getBoolean("TidalOverload_EmergencyLock")) {
            nbt.setBoolean("TidalOverload_EmergencyLock", true);
            nbt.setBoolean("EmergencyMode", true);

            // 首次锁定时通知玩家
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "⚠ 能量状态已锁定：EMERGENCY模式"
            ));
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "高耗能模块已被永久禁用"
            ));
        }

        // 持续设置紧急模式标记
        nbt.setBoolean("EmergencyMode", true);
    }

    /**
     * 禁用被封禁的高耗能模块
     */
    private void disableBannedUpgrades(ItemStack core, EntityPlayer player) {
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return;

        for (String upgradeId : BANNED_UPGRADES) {
            // 检查模块是否安装
            int level = ItemMechanicalCore.getUpgradeLevel(core, upgradeId);
            if (level <= 0) continue;

            // 检查模块是否已启用
            boolean isEnabled = ItemMechanicalCore.isUpgradeEnabled(core, upgradeId);
            if (!isEnabled) continue;

            // 禁用模块
            ItemMechanicalCore.setUpgradeEnabled(core, upgradeId, false);

            // 通知玩家（每60秒提醒一次）
            if (player.ticksExisted % 1200 == 0) {
                player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "⚡ " + getUpgradeDisplayName(upgradeId) +
                    " 已被潮汐过载禁用"
                ), true);
            }
        }
    }

    /**
     * 获取模块显示名称
     */
    private String getUpgradeDisplayName(String upgradeId) {
        switch (upgradeId) {
            case "ORE_VISION":
                return "矿物透视";
            case "STEALTH":
                return "隐身系统";
            case "FLIGHT_MODULE":
                return "飞行系统";
            default:
                return upgradeId;
        }
    }

    /**
     * Synergy停用时调用 - 解除锁定
     */
    public void onSynergyDeactivate(EntityPlayer player, ItemStack core) {
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return;

        // 清除紧急模式锁定
        nbt.removeTag("TidalOverload_EmergencyLock");
        nbt.setBoolean("EmergencyMode", false);

        player.sendMessage(new TextComponentString(
            TextFormatting.GREEN + "✓ 能量状态锁定已解除"
        ));
        player.sendMessage(new TextComponentString(
            TextFormatting.GRAY + "高耗能模块已恢复可用"
        ));
    }
}
