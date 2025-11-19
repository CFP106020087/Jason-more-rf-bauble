package com.moremod.system;

import com.moremod.system.FleshRejectionHUDManager;
import com.moremod.client.gui.EventHUDOverlay;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 急救物品HUD提示扩展
 * 为FleshRejectionFirstAidHooks提供HUD提示功能
 */
public class FleshRejectionFirstAidHUDExtension {
    
    /**
     * 使用急救包
     */
    @SideOnly(Side.CLIENT)
    public static void onFirstAidUsed(EntityPlayer player, float reductionAmount) {
        if (player.world.isRemote) {
            EventHUDOverlay.push(TextFormatting.GREEN + "🏥 急救治疗 -" + String.format("%.1f", reductionAmount) + "%");
            EventHUDOverlay.push(TextFormatting.AQUA + "💊 暂时缓解血肉排异");
        }
    }
    
    /**
     * 使用强效急救包
     */
    @SideOnly(Side.CLIENT)
    public static void onStrongFirstAidUsed(EntityPlayer player, float reductionAmount) {
        if (player.world.isRemote) {
            EventHUDOverlay.push(TextFormatting.GREEN + "💉 强效治疗 -" + String.format("%.1f", reductionAmount) + "%");
            EventHUDOverlay.push(TextFormatting.LIGHT_PURPLE + "🧬 深度修复组织损伤");
        }
    }
    
    /**
     * 使用适应药剂
     */
    @SideOnly(Side.CLIENT)
    public static void onAdaptationPotionUsed(EntityPlayer player, float adaptBoost) {
        if (player.world.isRemote) {
            EventHUDOverlay.push(TextFormatting.LIGHT_PURPLE + "🧪 适应药剂 +" + String.format("%.1f", adaptBoost) + "%");
            EventHUDOverlay.push(TextFormatting.AQUA + "🧬 加速身体适应进程");
        }
    }
    
    /**
     * 使用稳定剂
     */
    @SideOnly(Side.CLIENT)
    public static void onStabilizerUsed(EntityPlayer player, int duration) {
        if (player.world.isRemote) {
            EventHUDOverlay.push(TextFormatting.YELLOW + "⚗ 稳定剂生效");
            EventHUDOverlay.push(TextFormatting.GOLD + "🛡 排异增长暂停 " + (duration/20) + "秒");
        }
    }
    
    /**
     * 使用核心校准器
     */
    @SideOnly(Side.CLIENT)
    public static void onCoreCalibrationUsed(EntityPlayer player) {
        if (player.world.isRemote) {
            EventHUDOverlay.push(TextFormatting.AQUA + "⚙ 核心校准完成");
            EventHUDOverlay.push(TextFormatting.GREEN + "✨ 排异系统重置");
        }
    }
    
    /**
     * 提示急救物品在冷却中
     */
    @SideOnly(Side.CLIENT)
    public static void onFirstAidCooldown(EntityPlayer player, int remainingSeconds) {
        if (player.world.isRemote) {
            EventHUDOverlay.push(TextFormatting.RED + "⏱ 急救冷却中: " + remainingSeconds + "秒");
        }
    }
    
    /**
     * 突破状态下使用急救物品（无效）
     */
    @SideOnly(Side.CLIENT)
    public static void onTranscendedUseAttempt(EntityPlayer player) {
        if (player.world.isRemote) {
            EventHUDOverlay.push(TextFormatting.GOLD + "✨ 你已超越血肉");
            EventHUDOverlay.push(TextFormatting.YELLOW + "💫 无需急救治疗");
        }
    }
    
    /**
     * 低排异时使用急救（浪费）
     */
    @SideOnly(Side.CLIENT)
    public static void onWasteWarning(EntityPlayer player, float currentRejection) {
        if (player.world.isRemote && currentRejection < 10) {
            EventHUDOverlay.push(TextFormatting.YELLOW + "⚠ 排异值较低");
            EventHUDOverlay.push(TextFormatting.GOLD + "💡 建议在危急时使用");
        }
    }
}