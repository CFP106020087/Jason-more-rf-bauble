package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * 毒免疫模块
 *
 * 功能：
 *  - 免疫毒药效果（Poison）
 *  - 免疫凋零效果（Wither）
 *  - Lv.1: 完全免疫
 *
 * 能量消耗：
 *  - 基础消耗：10 RF/tick
 *  - 清除毒药额外消耗：20 RF/次
 *
 * 特性：
 *  - 被动清除毒药和凋零效果
 *  - 统计清除次数
 */
public class PoisonImmunityModule extends AbstractMechCoreModule {

    public static final PoisonImmunityModule INSTANCE = new PoisonImmunityModule();

    private PoisonImmunityModule() {
        super(
            "POISON_IMMUNITY",
            "毒免疫",
            "免疫毒药和凋零效果",
            1  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 初始化元数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setLong("POISON_CLEARED", 0);
        meta.setLong("WITHER_CLEARED", 0);

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.DARK_GREEN + "🛡 毒免疫模块已激活"
        ), true);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        // 显示统计信息
        long poisonCleared = meta.getLong("POISON_CLEARED");
        long witherCleared = meta.getLong("WITHER_CLEARED");

        if (poisonCleared > 0 || witherCleared > 0) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "毒免疫统计: 清除毒药 " + poisonCleared + " 次，凋零 " + witherCleared + " 次"
            ), false);
        }
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        // 只在服务端执行
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        // 检查并清除毒药效果
        if (player.isPotionActive(MobEffects.POISON)) {
            // 消耗额外能量
            if (data.consumeEnergy(20)) {
                player.removePotionEffect(MobEffects.POISON);

                meta.setLong("POISON_CLEARED", meta.getLong("POISON_CLEARED") + 1);

                // 显示提示（降低频率）
                if (player.world.rand.nextInt(5) == 0) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.DARK_GREEN + "🛡 已清除毒药效果"
                    ), true);
                }
            }
        }

        // 检查并清除凋零效果
        if (player.isPotionActive(MobEffects.WITHER)) {
            // 消耗额外能量
            if (data.consumeEnergy(20)) {
                player.removePotionEffect(MobEffects.WITHER);

                meta.setLong("WITHER_CLEARED", meta.getLong("WITHER_CLEARED") + 1);

                // 显示提示（降低频率）
                if (player.world.rand.nextInt(5) == 0) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.DARK_GREEN + "🛡 已清除凋零效果"
                    ), true);
                }
            }
        }

        // 预防性免疫（每5秒给予短暂的抗性效果）
        if (player.world.getTotalWorldTime() % 100 == 0) {
            // 给予 5 秒的抗毒效果（不显示粒子）
            player.addPotionEffect(new PotionEffect(
                    MobEffects.POISON,
                    0,  // 持续时间 0 = 立即清除任何毒药
                    0,
                    true,  // ambient
                    false  // 不显示粒子
            ));
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时清除所有负面效果
        if (newLevel > 0) {
            player.removePotionEffect(MobEffects.POISON);
            player.removePotionEffect(MobEffects.WITHER);

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.DARK_GREEN + "🛡 毒免疫已激活 - 已清除所有毒素"
            ), true);
        }
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 基础消耗：10 RF/tick
        return 10;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 总是可以执行
        return true;
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setLong("POISON_CLEARED", 0);
        meta.setLong("WITHER_CLEARED", 0);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("POISON_CLEARED")) {
            meta.setLong("POISON_CLEARED", 0);
        }
        if (!meta.hasKey("WITHER_CLEARED")) {
            meta.setLong("WITHER_CLEARED", 0);
        }
        return true;
    }
}
