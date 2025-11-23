package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * 隐身模块
 *
 * 功能：
 *  - 降低敌对生物检测范围
 *  - Lv.1: 基础隐身（30秒持续，20秒冷却）
 *  - Lv.2: 高级隐身 + 静音（45秒持续，30秒冷却）
 *  - Lv.3: 完美隐身 + 静音 + 抗性提升II（60秒持续，45秒冷却）
 *
 * 能量消耗：
 *  - 基础消耗：50 - level*10 RF/tick
 *  - 连续使用惩罚：+10 RF/tick per consecutive use
 *
 * 特性：
 *  - 持续时间系统（30s/45s/60s）
 *  - 冷却系统（20s/30s/45s）
 *  - 连续使用惩罚（1.5x 冷却倍率）
 */
public class StealthModule extends AbstractMechCoreModule {

    public static final StealthModule INSTANCE = new StealthModule();

    // 持续时间（毫秒）
    private static final long[] DURATION_MS = { 30000L, 45000L, 60000L };
    // 冷却时间（毫秒）
    private static final long[] COOLDOWN_MS = { 20000L, 30000L, 45000L };
    // 连续使用惩罚倍率
    private static final float CONSECUTIVE_PENALTY = 1.5f;
    // 连续使用重置时间（2分钟）
    private static final long CONSECUTIVE_RESET_TIME = 120000L;

    private StealthModule() {
        super(
            "STEALTH",
            "隐身系统",
            "降低敌对生物检测范围",
            3  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 隐身模块激活时不自动开启隐身，需要玩家手动触发
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setBoolean("STEALTH_ACTIVE", false);
        meta.setLong("STEALTH_START_TIME", 0);
        meta.setLong("COOLDOWN_END_TIME", 0);
        meta.setInteger("CONSECUTIVE_USES", 0);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 停用时关闭隐身
        disableStealth(player, data, false);
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        // 检查冷却时间
        long cooldownEnd = meta.getLong("COOLDOWN_END_TIME");
        long now = System.currentTimeMillis();
        if (cooldownEnd > 0 && now >= cooldownEnd) {
            // 冷却结束，检查是否需要重置连续使用计数
            long lastStart = meta.getLong("STEALTH_START_TIME");
            if (lastStart == 0 || now - cooldownEnd > CONSECUTIVE_RESET_TIME) {
                meta.setInteger("CONSECUTIVE_USES", 0);
            }
            meta.setLong("COOLDOWN_END_TIME", 0);
        }

        // 如果隐身未激活，跳过
        if (!meta.getBoolean("STEALTH_ACTIVE")) return;

        // 检查持续时间
        long startTime = meta.getLong("STEALTH_START_TIME");
        if (startTime > 0) {
            long duration = DURATION_MS[Math.min(level - 1, 2)];
            long elapsed = now - startTime;

            // 显示剩余时间提示
            if (player.world.getTotalWorldTime() % 20 == 0) {
                long remain = duration - elapsed;
                if (remain > 0 && remain <= 10000) {
                    int sec = (int) (remain / 1000);
                    TextFormatting color = sec <= 5 ? TextFormatting.RED : TextFormatting.YELLOW;
                    player.sendStatusMessage(new TextComponentString(
                            color + "⏱ 隐身剩余: " + sec + "秒"
                    ), true);
                }
            }

            // 检查是否超时
            if (elapsed >= duration) {
                disableStealth(player, data, true);
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GRAY + "隐身持续时间结束"
                ), true);
                return;
            }
        }

        // 能量消耗
        int consecutiveUses = meta.getInteger("CONSECUTIVE_USES");
        int baseCost = 50 - level * 10;
        int energyCost = baseCost + (consecutiveUses * 10);

        if (!data.consumeEnergy(energyCost)) {
            disableStealth(player, data, true);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "⚡ 能量不足，隐身已关闭"
            ), true);
            return;
        }

        // 维持隐身效果（每秒）
        if (player.world.getTotalWorldTime() % 20 == 0) {
            maintainStealthEffects(player, level);
        }

        // 高等级粒子效果（每 0.5 秒）
        if (level >= 3 && player.world.getTotalWorldTime() % 10 == 0) {
            spawnStealthParticles(player);
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时重新应用效果
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        if (meta.getBoolean("STEALTH_ACTIVE") && newLevel > 0) {
            maintainStealthEffects(player, newLevel);
        }
    }

    /**
     * 启用隐身
     */
    public void enableStealth(EntityPlayer player, IMechCoreData data) {
        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "未安装隐身模块"
            ), true);
            return;
        }

        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        // 检查冷却
        long cooldownEnd = meta.getLong("COOLDOWN_END_TIME");
        long now = System.currentTimeMillis();
        if (cooldownEnd > 0 && now < cooldownEnd) {
            int remainSec = (int) ((cooldownEnd - now) / 1000);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "⏱ 隐身冷却中... 剩余 " + remainSec + " 秒"
            ), true);
            return;
        }

        // 检查能量
        int consecutiveUses = meta.getInteger("CONSECUTIVE_USES");
        int baseCost = 50 - level * 10;
        int energyCost = baseCost + (consecutiveUses * 10);
        if (data.getEnergy() < energyCost * 20) {  // 至少需要 1 秒的能量
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "⚡ 能量不足，隐身无法开启"
            ), true);
            return;
        }

        // 启用隐身
        meta.setBoolean("STEALTH_ACTIVE", true);
        meta.setLong("STEALTH_START_TIME", now);

        player.getEntityData().setBoolean("MechanicalCoreStealthActive", true);
        player.getEntityData().setInteger("MechanicalCoreStealthLevel", level);

        maintainStealthEffects(player, level);

        // 发送消息
        long duration = DURATION_MS[Math.min(level - 1, 2)];
        int durationSec = (int) (duration / 1000);
        String msg = getStealthMessage(level) + String.format(" %s(持续%d秒)", TextFormatting.WHITE, durationSec);
        if (consecutiveUses > 0) {
            msg += String.format(" %s连续×%d", TextFormatting.YELLOW, consecutiveUses + 1);
        }
        player.sendStatusMessage(new TextComponentString(msg), true);
    }

    /**
     * 禁用隐身
     */
    public void disableStealth(EntityPlayer player, IMechCoreData data, boolean withCooldown) {
        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        if (!meta.getBoolean("STEALTH_ACTIVE")) return;

        int level = data.getModuleLevel(getModuleId());
        int consecutiveUses = meta.getInteger("CONSECUTIVE_USES");

        // 清除效果
        meta.setBoolean("STEALTH_ACTIVE", false);
        player.getEntityData().setBoolean("MechanicalCoreStealthActive", false);
        player.getEntityData().removeTag("MechanicalCoreStealthLevel");

        player.setInvisible(false);
        player.setSilent(false);
        player.removePotionEffect(MobEffects.INVISIBILITY);

        if (withCooldown) {
            // 设置冷却
            long baseCooldown = COOLDOWN_MS[Math.min(level - 1, 2)];
            long cooldown = (long) (baseCooldown * Math.pow(CONSECUTIVE_PENALTY, consecutiveUses));
            meta.setLong("COOLDOWN_END_TIME", System.currentTimeMillis() + cooldown);
            meta.setInteger("CONSECUTIVE_USES", consecutiveUses + 1);

            int cooldownSec = (int) (cooldown / 1000);
            String msg = String.format("%s隐身已关闭 - 冷却: %d秒", TextFormatting.GRAY, cooldownSec);
            if (consecutiveUses > 0) {
                msg += String.format(" %s(连续使用×%d)", TextFormatting.YELLOW, consecutiveUses + 1);
            }
            player.sendStatusMessage(new TextComponentString(msg), true);
        } else {
            // 手动关闭，冷却时间减半
            long baseCooldown = COOLDOWN_MS[Math.min(level - 1, 2)] / 2;
            meta.setLong("COOLDOWN_END_TIME", System.currentTimeMillis() + baseCooldown);

            int cooldownSec = (int) (baseCooldown / 1000);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "隐身已关闭 - 冷却: " + cooldownSec + "秒"
            ), true);
        }

        meta.setLong("STEALTH_START_TIME", 0);
    }

    /**
     * 切换隐身状态
     */
    public void toggleStealth(EntityPlayer player, IMechCoreData data) {
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        if (meta.getBoolean("STEALTH_ACTIVE")) {
            disableStealth(player, data, false);
        } else {
            enableStealth(player, data);
        }
    }

    /**
     * 维持隐身效果
     */
    private void maintainStealthEffects(EntityPlayer player, int level) {
        // 基础隐身
        player.setInvisible(true);
        player.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 100, 0, false, false));

        // Lv.2+: 静音
        if (level >= 2) {
            player.setSilent(true);
        }

        // Lv.3: 抗性提升II
        if (level >= 3) {
            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 40, 1, false, false));
        }
    }

    /**
     * 生成隐身粒子
     */
    private void spawnStealthParticles(EntityPlayer player) {
        for (int i = 0; i < 3; i++) {
            player.world.spawnParticle(
                    EnumParticleTypes.SMOKE_NORMAL,
                    player.posX + (Math.random() - 0.5) * 0.5,
                    player.posY + Math.random() * 2,
                    player.posZ + (Math.random() - 0.5) * 0.5,
                    0, 0.01, 0
            );
        }
    }

    /**
     * 获取隐身激活消息
     */
    private String getStealthMessage(int level) {
        switch (level) {
            case 1:
                return TextFormatting.GRAY + "👤 基础隐身已激活";
            case 2:
                return TextFormatting.DARK_GRAY + "🌫 高级隐身已激活";
            case 3:
                return TextFormatting.DARK_PURPLE + "👻 完美隐身已激活";
            default:
                return TextFormatting.GRAY + "隐身已激活";
        }
    }

    /**
     * 检查隐身是否激活
     */
    public boolean isStealthActive(IMechCoreData data) {
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        return meta.getBoolean("STEALTH_ACTIVE");
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 隐身是动态消耗，这里返回 0
        // 实际消耗在 onTick 中计算
        return 0;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 总是可以执行（能量检查在 enableStealth 中）
        return true;
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setBoolean("STEALTH_ACTIVE", false);
        meta.setLong("STEALTH_START_TIME", 0);
        meta.setLong("COOLDOWN_END_TIME", 0);
        meta.setInteger("CONSECUTIVE_USES", 0);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("STEALTH_ACTIVE")) {
            meta.setBoolean("STEALTH_ACTIVE", false);
        }
        if (!meta.hasKey("STEALTH_START_TIME")) {
            meta.setLong("STEALTH_START_TIME", 0);
        }
        if (!meta.hasKey("COOLDOWN_END_TIME")) {
            meta.setLong("COOLDOWN_END_TIME", 0);
        }
        if (!meta.hasKey("CONSECUTIVE_USES")) {
            meta.setInteger("CONSECUTIVE_USES", 0);
        }
        return true;
    }
}
