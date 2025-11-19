package com.moremod.item;

import com.moremod.system.FleshRejectionSystem;
import com.moremod.config.FleshRejectionConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 機械核心排異系統 Tooltip 輔助類
 * 獨立類，可直接在 ItemMechanicalCore.addInformation() 中調用
 */
@SideOnly(Side.CLIENT)
public class RejectionTooltipHelper {
    
    /**
     * 主入口 - 在 ItemMechanicalCore.addInformation() 中調用
     * 
     * 使用方式：
     * RejectionTooltipHelper.addRejectionInfo(tooltip, stack, worldIn);
     */
    public static void addRejectionInfo(List<String> tooltip, ItemStack stack, World world) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;
        
        // 獲取數據
        float rejection = FleshRejectionSystem.getRejectionLevel(player);
        float adaptation = FleshRejectionSystem.getAdaptationLevel(player);
        boolean transcended = FleshRejectionSystem.hasTranscended(player);
        
        // 添加排異信息區塊
        tooltip.add(""); // 空行分隔
        
        // 視覺化排異條
        tooltip.add(createRejectionBar(rejection, adaptation, transcended));
        
        // 根據按鍵狀態顯示不同信息
        if (GuiScreen.isShiftKeyDown()) {
            addDetailedMechanics(tooltip, rejection, adaptation, transcended, player);
        } else {
            addContextualHints(tooltip, rejection, adaptation, player);
        }
    }
    
    /**
     * 創建視覺化排異條
     */
    private static String createRejectionBar(float rejection, float adaptation, boolean transcended) {
        if (transcended && adaptation >= FleshRejectionConfig.adaptationThreshold) {
            return TextFormatting.LIGHT_PURPLE + "【機械飛升】 " + 
                   TextFormatting.DARK_PURPLE + "免疫排異";
        }
        
        StringBuilder bar = new StringBuilder();
        bar.append(getBarColor(rejection)).append("排異 ");
        
        // 進度條
        bar.append(TextFormatting.DARK_GRAY).append("[");
        
        int segments = 20; // 條的長度
        int filled = (int)(rejection / 100f * segments);
        
        for (int i = 0; i < segments; i++) {
            if (i < filled) {
                // 根據數值選擇顏色
                if (i < segments * 0.2) bar.append(TextFormatting.GREEN);
                else if (i < segments * 0.4) bar.append(TextFormatting.YELLOW);
                else if (i < segments * 0.6) bar.append(TextFormatting.GOLD);
                else if (i < segments * 0.8) bar.append(TextFormatting.RED);
                else bar.append(TextFormatting.DARK_RED);
                
                bar.append("▮");
            } else {
                bar.append(TextFormatting.DARK_GRAY).append("▯");
            }
        }
        
        bar.append(TextFormatting.DARK_GRAY).append("] ");
        
        // 狀態文字
        bar.append(getStatusText(rejection));
        
        // 適應度提示
        if (adaptation > 50 && !transcended) {
            bar.append(TextFormatting.AQUA).append(" [適應中]");
        }
        
        return bar.toString();
    }
    
    /**
     * 獲取狀態顏色
     */
    private static TextFormatting getBarColor(float rejection) {
        if (rejection < 20) return TextFormatting.GREEN;
        if (rejection < 40) return TextFormatting.YELLOW;
        if (rejection < 60) return TextFormatting.GOLD;
        if (rejection < 80) return TextFormatting.RED;
        return TextFormatting.DARK_RED;
    }
    
    /**
     * 獲取狀態描述
     */
    private static String getStatusText(float rejection) {
        TextFormatting color = getBarColor(rejection);
        
        if (rejection < 20) 
            return color + "機械同步";
        else if (rejection < 40) 
            return color + "輕微排斥";
        else if (rejection < 60) 
            return color + "神經紊亂";
        else if (rejection < 80) 
            return color + "組織崩解";
        else 
            return color + "" + TextFormatting.BOLD + "瀕死狀態";
    }
    
    /**
     * 添加情境化提示（簡略模式）
     */
    private static void addContextualHints(List<String> tooltip, float rejection, float adaptation, EntityPlayer player) {
        
        // 根據排異等級顯示不同提示
        if (rejection < 20) {
            // 初期 - 簡單提示
            if (adaptation < 30) {
                tooltip.add(TextFormatting.GRAY + "機械正在與血肉融合...");
            }
            
        } else if (rejection < 40) {
            // 輕微 - 開始提醒
            tooltip.add(TextFormatting.YELLOW + "⚠ 身體開始排斥機械");
            
            // 智能檢測問題
            if (player.getFoodStats().getFoodLevel() < 14) {
                tooltip.add(TextFormatting.GOLD + " ▸ 飢餓加劇症狀");
            }
            
            long positiveEffects = player.getActivePotionEffects().stream()
                .filter(e -> !e.getPotion().isBadEffect())
                .count();
            if (positiveEffects >= 3) {
                tooltip.add(TextFormatting.GOLD + " ▸ 藥水負荷過高");
            }
            
        } else if (rejection < 60) {
            // 中度 - 顯示影響
            tooltip.add(TextFormatting.RED + "⚡ 運動神經受損");
            int missChance = (int)((rejection - 40) / 60f * 100);
            tooltip.add(TextFormatting.GRAY + " ▸ 攻擊失誤 " + missChance + "%");
            tooltip.add(TextFormatting.AQUA + " → 睡眠可恢復");
            
        } else if (rejection < 80) {
            // 嚴重 - 警告
            tooltip.add(TextFormatting.RED + "" + TextFormatting.BOLD + "⚠ 器官衰竭");
            tooltip.add(TextFormatting.RED + " ▸ 隨機自傷");
            tooltip.add(TextFormatting.RED + " ▸ 防禦失效");
            tooltip.add(TextFormatting.YELLOW + " → 立即休息！");
            
        } else {
            // 危急 - 最終警告
            tooltip.add(TextFormatting.DARK_RED + "" + TextFormatting.BOLD + "💀 生命垂危");
            tooltip.add(TextFormatting.DARK_RED + " ▸ 血肉溶解中");
            tooltip.add(TextFormatting.GRAY + "" + TextFormatting.ITALIC + " 死亡將重置狀態");
        }
        
        // 提示查看詳情
        if (rejection > 20) {
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "按住 " + 
                       TextFormatting.GRAY + "Shift " + 
                       TextFormatting.DARK_GRAY + "查看機制詳情");
        }
    }
    
    /**
     * 添加詳細機制說明（Shift模式）
     */
    private static void addDetailedMechanics(List<String> tooltip, float rejection, float adaptation, 
                                            boolean transcended, EntityPlayer player) {
        
        // 標題
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_AQUA + "▬▬▬ 血肉排異機制 ▬▬▬");
        
        // 當前數值
        tooltip.add(TextFormatting.WHITE + "狀態數據：");
        tooltip.add(String.format(" • 排異值: " + getBarColor(rejection) + "%.1f" + 
                                 TextFormatting.GRAY + "/100", rejection));
        tooltip.add(String.format(" • 適應度: " + TextFormatting.AQUA + "%.1f" + 
                                 TextFormatting.GRAY + "/100", adaptation));
        
        if (transcended) {
            tooltip.add(TextFormatting.LIGHT_PURPLE + " • " + TextFormatting.BOLD + "已突破血肉極限");
        }
        
        // 排異來源
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "排異觸發：");
        tooltip.add(TextFormatting.GRAY + " • 飢餓/口渴 " + TextFormatting.RED + "▲");
        tooltip.add(TextFormatting.GRAY + " • 正面藥水 " + TextFormatting.RED + "▲▲");
        tooltip.add(TextFormatting.GRAY + " • 受到傷害 " + TextFormatting.RED + "▲▲▲");
        tooltip.add(TextFormatting.GRAY + " • 熬夜 " + TextFormatting.DARK_RED + "▲▲▲▲");
        
        // 緩解方式
        tooltip.add("");
        tooltip.add(TextFormatting.GREEN + "抑制方法：");
        tooltip.add(TextFormatting.GRAY + " • 睡眠休息 " + TextFormatting.GREEN + "▼▼▼");
        tooltip.add(TextFormatting.GRAY + " • 保持飽食 " + TextFormatting.GREEN + "▼");
        tooltip.add(TextFormatting.GRAY + " • 提升適應 " + TextFormatting.AQUA + "◆");
        
        // 當前懲罰
        if (rejection > 40) {
            tooltip.add("");
            tooltip.add(TextFormatting.RED + "當前懲罰：");
            
            // 攻擊失誤
            if (rejection >= FleshRejectionConfig.attackMissStart) {
                int miss = (int)((rejection - FleshRejectionConfig.attackMissStart) / 
                                (100 - FleshRejectionConfig.attackMissStart) * 
                                FleshRejectionConfig.maxMissChance * 100);
                tooltip.add(TextFormatting.GRAY + " • 攻擊失誤: " + 
                          TextFormatting.RED + miss + "%");
            }
            
            // 藥水阻斷
            if (rejection >= 40) {
                int block = (int)((rejection - 40) * 1.67f);
                tooltip.add(TextFormatting.GRAY + " • 藥水失效: " + 
                          TextFormatting.RED + block + "%");
            }
            
            // 藥水上限
            if (rejection >= FleshRejectionConfig.potionLimitStart) {
                int max = (int)(FleshRejectionConfig.potionMaxAtZero * 
                               (1.0 - rejection / FleshRejectionConfig.maxRejection));
                tooltip.add(TextFormatting.GRAY + " • 藥水上限: " + 
                          TextFormatting.GOLD + max);
            }
            
            // 無敵削減
            if (rejection >= FleshRejectionConfig.invulnerabilityReductionStart) {
                float reduction = (rejection - FleshRejectionConfig.invulnerabilityReductionStart) / 
                                (100 - FleshRejectionConfig.invulnerabilityReductionStart);
                int percent = (int)(reduction * (1 - FleshRejectionConfig.minInvulnerabilityRatio) * 100);
                tooltip.add(TextFormatting.GRAY + " • 無敵時間: " + 
                          TextFormatting.RED + "-" + percent + "%");
            }
            
            // 出血
            if (rejection >= FleshRejectionConfig.invulnerabilityReductionStart) {
                int bleed = (int)(FleshRejectionConfig.bleedingBaseChance * 100 + 
                                 (rejection - FleshRejectionConfig.invulnerabilityReductionStart) * 
                                 FleshRejectionConfig.bleedingChanceGrowth * 100);
                tooltip.add(TextFormatting.GRAY + " • 出血機率: " + 
                          TextFormatting.DARK_RED + bleed + "%");
            }
        }
        
        // 突破提示
        if (!transcended && adaptation > 70) {
            tooltip.add("");
            tooltip.add(TextFormatting.LIGHT_PURPLE + "◆ 接近機械飛升 ◆");
            tooltip.add(TextFormatting.GRAY + "" + TextFormatting.ITALIC + 
                       "適應度滿後可突破血肉極限");
        }
    }
}