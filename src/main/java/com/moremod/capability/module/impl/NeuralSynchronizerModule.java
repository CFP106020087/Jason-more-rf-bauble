package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import com.moremod.system.FleshRejectionSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * 神经同步器模块
 *
 * 功能：
 *  - 大幅提升适应度（+100点）
 *  - 缓慢减少排异值（0.005/秒）
 *  - 清除负面效果（恶心、失明、混乱、虚弱、挖掘疲劳）
 *  - 减缓出血
 *
 * 能量消耗：
 *  - 被动消耗：50 RF/秒
 *
 * 依赖系统：
 *  - FleshRejectionSystem（血肉排异系统）
 *
 * 注意：
 *  - 此模块对于克服血肉排异至关重要
 *  - 提供的适应度加成可能使玩家达到突破阈值（120点）
 */
public class NeuralSynchronizerModule extends AbstractMechCoreModule {

    public static final NeuralSynchronizerModule INSTANCE = new NeuralSynchronizerModule();

    // 常量
    private static final int ADAPTATION_BONUS = 100;  // 提供 100 点适应度
    private static final float REJECTION_REDUCTION_RATE = 0.005f;  // 每秒减少 0.005 排异值
    private static final int STATUS_DISPLAY_INTERVAL = 2000;  // 每 100 秒显示一次状态（2000 ticks）
    private static final int EFFECT_CLEAR_INTERVAL = 100;  // 每 5 秒清除负面效果（100 ticks）

    private NeuralSynchronizerModule() {
        super(
            "NEURAL_SYNCHRONIZER",
            "神经同步器",
            "提升适应度并减缓血肉排异",
            1  // 最大等级（只有 1 级）
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.AQUA + "⚡ 神经同步器已激活"
        ), true);

        // 检查突破条件
        FleshRejectionSystem.RejectionStatus status = FleshRejectionSystem.getStatus(player);
        if (status != null) {
            if (status.adaptation >= 120 && !status.transcended) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 适应度足够，即将突破血肉排异！"
                ), false);
            } else if (!status.transcended) {
                float needed = 120 - status.adaptation;
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.YELLOW + "当前适应度: " +
                                String.format("%.0f", status.adaptation) +
                                " (还需 " + String.format("%.0f", needed) + " 点)"
                ), false);
            }
        }
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 卸下时警告
        FleshRejectionSystem.RejectionStatus status = FleshRejectionSystem.getStatus(player);
        if (status != null && !status.transcended) {
            float newAdaptation = status.adaptation - ADAPTATION_BONUS;
            if (newAdaptation < 120) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 警告：失去神经同步器，适应度下降至 " +
                                String.format("%.0f", newAdaptation)
                ), true);
            }
        }
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        // 每秒执行一次（20 ticks）
        if (player.world.getTotalWorldTime() % 20 == 0) {
            // 减少排异值
            reduceRejection(player);
        }

        // 每 100 秒显示一次状态
        if (player.world.getTotalWorldTime() % STATUS_DISPLAY_INTERVAL == 0) {
            displayStatus(player);
        }

        // 每 5 秒清除负面效果
        if (player.world.getTotalWorldTime() % EFFECT_CLEAR_INTERVAL == 0) {
            removeNegativeEffects(player);
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 神经同步器只有 1 级，不会有等级变化
    }

    /**
     * 减少排异值
     */
    private void reduceRejection(EntityPlayer player) {
        float currentRejection = FleshRejectionSystem.getRejectionLevel(player);

        // 只在未突破且有排异值时减少
        if (currentRejection > 0 && !FleshRejectionSystem.hasTranscended(player)) {
            float newRejection = Math.max(0, currentRejection - REJECTION_REDUCTION_RATE);
            FleshRejectionSystem.setRejectionLevel(player, newRejection);
        }
    }

    /**
     * 显示同步状态
     */
    private void displayStatus(EntityPlayer player) {
        FleshRejectionSystem.RejectionStatus status = FleshRejectionSystem.getStatus(player);

        if (status != null && !status.transcended) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.AQUA + "⚡ 神经同步中 | " +
                            TextFormatting.GRAY + "排异: " +
                            getColorForRejection(status.rejection) +
                            String.format("%.1f%%", status.rejection) +
                            TextFormatting.GRAY + " | 适应: " +
                            TextFormatting.GREEN + String.format("%.0f", status.adaptation)
            ), true);
        }
    }

    /**
     * 移除负面效果
     */
    private void removeNegativeEffects(EntityPlayer player) {
        // 移除恶心、失明、混乱等负面效果
        player.removePotionEffect(Potion.getPotionFromResourceLocation("minecraft:nausea"));
        player.removePotionEffect(Potion.getPotionFromResourceLocation("minecraft:blindness"));
        player.removePotionEffect(Potion.getPotionFromResourceLocation("minecraft:confusion"));
        player.removePotionEffect(Potion.getPotionFromResourceLocation("minecraft:weakness"));
        player.removePotionEffect(Potion.getPotionFromResourceLocation("minecraft:mining_fatigue"));

        // 减缓出血
        int bleeding = FleshRejectionSystem.getBleedingTicks(player);
        if (bleeding > 100) {
            // 每 5 秒减少 20 ticks 的出血时间
            FleshRejectionSystem.setBleedingTicks(player, bleeding - 20);
        }
    }

    /**
     * 获取排异值对应的颜色
     */
    private TextFormatting getColorForRejection(float rejection) {
        if (rejection >= 80) return TextFormatting.DARK_RED;
        if (rejection >= 60) return TextFormatting.RED;
        if (rejection >= 40) return TextFormatting.YELLOW;
        if (rejection >= 20) return TextFormatting.GREEN;
        return TextFormatting.AQUA;
    }

    /**
     * 获取适应度加成
     */
    public int getAdaptationBonus() {
        return ADAPTATION_BONUS;
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 50 RF/秒（每 tick 消耗 50/20 = 2.5 RF，向上取整为 3）
        return 3;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        return data.getEnergy() >= getPassiveEnergyCost(data.getModuleLevel(getModuleId()));
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        // 神经同步器不需要额外的元数据
        return new NBTTagCompound();
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        // 无需验证
        return true;
    }
}
