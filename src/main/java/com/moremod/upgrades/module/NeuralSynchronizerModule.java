// com/moremod/upgrades/special/NeuralSynchronizerModule.java
package com.moremod.upgrades.module;

import com.moremod.upgrades.api.IUpgradeModule;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.system.FleshRejectionSystem;
import com.moremod.upgrades.energy.EnergyDepletionManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * 神经同步器 - 大幅提升适应度，减缓排异反应
 */
public class NeuralSynchronizerModule implements IUpgradeModule {
    
    public static final NeuralSynchronizerModule INSTANCE = new NeuralSynchronizerModule();
    
    private static final int ADAPTATION_BONUS = 100;  // 提供100点适应度
    private static final float REJECTION_REDUCTION_RATE = 0.005f;  // 每秒减少0.005排异值
    
    @Override
    public String getModuleId() {
        return "NEURAL_SYNCHRONIZER";
    }
    
    @Override
    public String getDisplayName() {
        return "神经同步器";
    }
    
    @Override
    public int getMaxLevel() {
        return 1;  // 只有1级
    }
    
    @Override
    public void onTick(EntityPlayer player, ItemStack core, int level) {
        if (player.world.isRemote) return;
        if (player.ticksExisted % 20 != 0) return;  // 每秒执行
        
        // 消耗能量
        int energyCost = getPassiveEnergyCost(level);
        if (!ItemMechanicalCore.consumeEnergy(core, energyCost, true)) {
            return;  // 能量不足
        }
        
        // 缓慢减少排异值
        float currentRejection = FleshRejectionSystem.getRejectionLevel(player);
        if (currentRejection > 0 && !FleshRejectionSystem.hasTranscended(player)) {
            FleshRejectionSystem.setRejectionLevel(player, 
                Math.max(0, currentRejection - REJECTION_REDUCTION_RATE));
        }
        
        // 每100秒提示一次状态
        if (player.ticksExisted % 2000 == 0) {
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
        
        // 额外效果：清晰思维（移除负面效果）
        if (player.ticksExisted % 100 == 0) {  // 每5秒
            removeNegativeEffects(player);
        }
    }
    
    @Override
    public void onEquip(EntityPlayer player, ItemStack core, int level) {
        // 装备时立即提供适应度（通过系统自动计算）
        player.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "⚡ 神经同步器已激活"
        ));
        
        // 检查是否达到突破条件
        FleshRejectionSystem.RejectionStatus status = FleshRejectionSystem.getStatus(player);
        if (status != null) {
            if (status.adaptation >= 120 && !status.transcended) {
                player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 适应度足够，即将突破血肉排异！"
                ));
            } else if (!status.transcended) {
                float needed = 120 - status.adaptation;
                player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "当前适应度: " + 
                    String.format("%.0f", status.adaptation) + 
                    " (还需 " + String.format("%.0f", needed) + " 点)"
                ));
            }
        }
    }
    
    @Override
    public void onUnequip(EntityPlayer player, ItemStack core, int level) {
        // 卸下时警告
        FleshRejectionSystem.RejectionStatus status = FleshRejectionSystem.getStatus(player);
        if (status != null && !status.transcended) {
            float newAdaptation = status.adaptation - ADAPTATION_BONUS;
            if (newAdaptation < 120) {
                player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "⚠ 警告：失去神经同步器，适应度下降至 " + 
                    String.format("%.0f", newAdaptation)
                ));
            }
        }
    }
    
    @Override
    public int getPassiveEnergyCost(int level) {
        return 50;  // 50 RF/s - 中等消耗
    }
    
    @Override
    public boolean canRunWithEnergyStatus(EnergyDepletionManager.EnergyStatus status) {
        // 在所有状态下都能工作（太重要了）
        return true;
    }
    
    @Override
    public void handleEvent(Event event, EntityPlayer player, ItemStack core, int level) {
        // 神经同步器没有主动效果
    }
    
    /**
     * 移除负面效果
     */
    private void removeNegativeEffects(EntityPlayer player) {
        // 移除混乱、恶心、失明等负面效果
        player.removePotionEffect(Potion.getPotionFromResourceLocation("minecraft:nausea"));
        player.removePotionEffect(Potion.getPotionFromResourceLocation("minecraft:blindness"));
        player.removePotionEffect(Potion.getPotionFromResourceLocation("minecraft:confusion"));
        player.removePotionEffect(Potion.getPotionFromResourceLocation("minecraft:weakness"));
        player.removePotionEffect(Potion.getPotionFromResourceLocation("minecraft:mining_fatigue"));
        
        // 停止出血
        int bleeding = FleshRejectionSystem.getBleedingTicks(player);
        if (bleeding > 0 && bleeding > 100) {
            // 缓慢减少出血时间
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
}