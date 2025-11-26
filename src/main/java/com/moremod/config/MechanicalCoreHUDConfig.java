package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 机械核心HUD配置 - 纯净版（仅包含显示配置）
 * 所有按键设置已移至 KeyBindHandler
 */
@Config(modid = "moremod", name = "moremod/mechanical_core_hud", category = "client")
@Config.LangKey("config.moremod.hud")
public class MechanicalCoreHUDConfig {

    // ===== 基础设置 =====

    @Config.Comment("启用机械核心HUD")
    @Config.LangKey("config.moremod.hud.enabled")
    public static boolean enabled = true;

    @Config.Comment("HUD位置")
    @Config.LangKey("config.moremod.hud.position")
    public static HUDPosition position = HUDPosition.TOP_LEFT;

    @Config.Comment("HUD X偏移")
    @Config.RangeInt(min = 0, max = 500)
    public static int xOffset = 10;

    @Config.Comment("HUD Y偏移")
    @Config.RangeInt(min = 0, max = 500)
    public static int yOffset = 10;

    @Config.Comment("HUD缩放")
    @Config.RangeDouble(min = 0.5, max = 2.0)
    public static double scale = 1.0;

    // ===== 显示选项 =====

    @Config.Comment("显示能量流")
    public static boolean showEnergyFlow = true;

    @Config.Comment("显示效率信息")
    public static boolean showEfficiency = true;

    @Config.Comment("显示活跃升级")
    public static boolean showActiveUpgrades = true;

    @Config.Comment("显示警告")
    public static boolean showWarnings = true;

    @Config.Comment("显示产能模块")
    public static boolean showGenerators = true;

    @Config.Comment("显示战斗信息")
    public static boolean showCombatInfo = true;

    @Config.Comment("显示被动效果")
    public static boolean showPassiveEffects = true;

    // ===== 显示模式设置 =====

    @Config.Comment("紧凑模式（减少显示的功能数量）")
    public static boolean compactMode = false;

    @Config.Comment("功能图标大小")
    @Config.RangeDouble(min = 0.5, max = 2.0)
    public static double iconScale = 1.0;

    @Config.Comment("最大显示升级数量（正常模式）")
    @Config.RangeInt(min = 3, max = 10)
    public static int maxDisplayUpgrades = 5;

    @Config.Comment("最大显示升级数量（紧凑模式）")
    @Config.RangeInt(min = 1, max = 5)
    public static int maxDisplayUpgradesCompact = 3;

    @Config.Comment("优先显示关键功能")
    public static boolean prioritizeImportantUpgrades = true;

    @Config.Comment("自动隐藏无关功能")
    public static boolean autoHideIrrelevant = false;

    @Config.Comment("显示滚动提示")
    public static boolean showScrollHints = true;

    // ===== 视觉效果 =====

    @Config.Comment("使用动画效果")
    public static boolean useAnimations = true;

    @Config.Comment("背景透明度")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double backgroundAlpha = 0.7;

    @Config.Comment("启用脉冲效果（低能量时）")
    public static boolean enablePulseEffect = true;

    @Config.Comment("启用能量条闪烁效果")
    public static boolean enableEnergyBarShimmer = true;

    // ===== 高级设置 =====

    @Config.Comment("能量流采样时间（tick）")
    @Config.RangeInt(min = 5, max = 60)
    public static int energyFlowSampleSize = 20;

    @Config.Comment("HUD更新频率（每N tick更新一次）")
    @Config.RangeInt(min = 1, max = 20)
    public static int updateFrequency = 1;

    @Config.Comment("启用性能优化模式")
    public static boolean performanceMode = false;

    @Config.Comment("减少动画计算")
    public static boolean reduceAnimationCalculations = false;

    // ===== 颜色设置 =====

    @Config.Comment("自定义能量条颜色")
    public static boolean useCustomEnergyColors = false;

    @Config.Comment("高能量颜色 (十六进制，如 00FF00)")
    public static String highEnergyColor = "00FF00";

    @Config.Comment("中等能量颜色 (十六进制，如 FFFF00)")
    public static String mediumEnergyColor = "FFFF00";

    @Config.Comment("低能量颜色 (十六进制，如 FF8800)")
    public static String lowEnergyColor = "FF8800";

    @Config.Comment("危险能量颜色 (十六进制，如 FF0000)")
    public static String criticalEnergyColor = "FF0000";

    // ===== 附魔系统设置 =====

    @Config.Comment("附魔增强冷却时间（秒）")
    @Config.RangeInt(min = 30, max = 300)
    public static int enchantBoostCooldownSec = 120;

    @Config.Comment("附魔增强持续时间（秒）")
    @Config.RangeInt(min = 10, max = 120)
    public static int enchantBoostDurationSec = 60;

    // ===== 运行时状态（不保存到配置文件）=====

    private static boolean hudVisible = true; // HUD可见状态，由KeyBindHandler控制

    // ===== 枚举定义 =====

    public enum HUDPosition {
        TOP_LEFT("左上"),
        TOP_RIGHT("右上"),
        BOTTOM_LEFT("左下"),
        BOTTOM_RIGHT("右下"),
        CUSTOM("自定义");

        private final String displayName;

        HUDPosition(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ===== 辅助方法 =====

    /**
     * 检查HUD是否应该显示
     * 由 KeyBindHandler 控制实际的显示状态
     */
    public static boolean shouldHideHUD() {
        // 首先检查配置文件中的总开关
        if (!enabled) {
            return true; // 如果配置中禁用了，就隐藏
        }

        // 然后检查运行时的显示状态（由 KeyBindHandler 管理）
        return !hudVisible;
    }

    /**
     * 获取HUD是否可见
     */
    public static boolean isHudVisible() {
        return enabled && hudVisible;
    }

    /**
     * 设置HUD可见性（供 KeyBindHandler 调用）
     */
    public static void setHudVisible(boolean visible) {
        hudVisible = visible;
    }

    /**
     * 获取当前应该显示的最大升级数量
     */
    public static int getCurrentMaxDisplayUpgrades() {
        return compactMode ? maxDisplayUpgradesCompact : maxDisplayUpgrades;
    }

    /**
     * 解析十六进制颜色字符串为整数
     */
    public static int parseColorString(String colorStr) {
        try {
            // 移除可能的 # 前缀
            if (colorStr.startsWith("#")) {
                colorStr = colorStr.substring(1);
            }
            return (int) Long.parseLong(colorStr, 16) | 0xFF000000;
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF; // 默认白色
        }
    }

    /**
     * 获取自定义能量颜色
     */
    public static int getEnergyColor(float percent) {
        if (!useCustomEnergyColors) {
            // 使用默认颜色逻辑
            if (percent > 0.6f) return 0xFF00FF00; // 绿色
            if (percent > 0.3f) return 0xFFFFFF00; // 黄色
            if (percent > 0.1f) return 0xFFFF8800; // 橙色
            return 0xFFFF0000; // 红色
        }

        // 使用自定义颜色
        if (percent > 0.6f) return parseColorString(highEnergyColor);
        if (percent > 0.3f) return parseColorString(mediumEnergyColor);
        if (percent > 0.1f) return parseColorString(lowEnergyColor);
        return parseColorString(criticalEnergyColor);
    }

    /**
     * 获取有效的能量流采样大小
     */
    public static int getEffectiveEnergyFlowSampleSize() {
        if (performanceMode) {
            return Math.min(energyFlowSampleSize, 10); // 性能模式下限制采样
        }
        return energyFlowSampleSize;
    }

    /**
     * 检查是否应该启用动画
     */
    public static boolean shouldUseAnimations() {
        if (performanceMode && reduceAnimationCalculations) {
            return false;
        }
        return useAnimations;
    }

    /**
     * 获取有效的更新频率
     */
    public static int getEffectiveUpdateFrequency() {
        if (performanceMode) {
            return Math.max(updateFrequency, 3); // 性能模式下降低更新频率
        }
        return updateFrequency;
    }

    /**
     * 验证颜色字符串是否有效
     */
    public static boolean isValidColorString(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return false;
        }

        try {
            String cleanColor = colorStr.startsWith("#") ? colorStr.substring(1) : colorStr;
            if (cleanColor.length() != 6) {
                return false;
            }
            Long.parseLong(cleanColor, 16);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 重置颜色设置为默认值
     */
    public static void resetColorsToDefault() {
        useCustomEnergyColors = false;
        highEnergyColor = "00FF00";
        mediumEnergyColor = "FFFF00";
        lowEnergyColor = "FF8800";
        criticalEnergyColor = "FF0000";
    }

    /**
     * 重置所有设置为默认值
     */
    public static void resetToDefaults() {
        enabled = true;
        position = HUDPosition.TOP_LEFT;
        xOffset = 10;
        yOffset = 10;
        scale = 1.0;

        showEnergyFlow = true;
        showEfficiency = true;
        showActiveUpgrades = true;
        showWarnings = true;
        showGenerators = true;
        showCombatInfo = true;
        showPassiveEffects = true;

        compactMode = false;
        iconScale = 1.0;
        maxDisplayUpgrades = 5;
        maxDisplayUpgradesCompact = 3;

        hudVisible = true;
        prioritizeImportantUpgrades = true;
        autoHideIrrelevant = false;
        showScrollHints = true;

        useAnimations = true;
        backgroundAlpha = 0.7;
        enablePulseEffect = true;
        enableEnergyBarShimmer = true;

        energyFlowSampleSize = 20;
        updateFrequency = 1;
        performanceMode = false;
        reduceAnimationCalculations = false;

        enchantBoostCooldownSec = 120;
        enchantBoostDurationSec = 60;

        resetColorsToDefault();
    }

    /**
     * 获取当前配置的摘要信息
     */
    public static String getConfigSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("机械核心HUD配置摘要:\n");
        summary.append("状态: ").append(isHudVisible() ? "显示" : "隐藏").append("\n");
        summary.append("位置: ").append(position.getDisplayName()).append("\n");
        summary.append("缩放: ").append(String.format("%.1f", scale)).append("\n");
        summary.append("显示模式: ").append(compactMode ? "紧凑" : "正常").append("\n");
        summary.append("最大显示数: ").append(getCurrentMaxDisplayUpgrades()).append("\n");
        summary.append("动画效果: ").append(useAnimations ? "启用" : "禁用").append("\n");
        summary.append("性能模式: ").append(performanceMode ? "启用" : "禁用").append("\n");
        summary.append("附魔增强冷却: ").append(enchantBoostCooldownSec).append("秒\n");
        summary.append("附魔增强持续: ").append(enchantBoostDurationSec).append("秒\n");
        return summary.toString();
    }

    // ===== 配置同步 =====

    @Mod.EventBusSubscriber(modid = "moremod")
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals("moremod")) {
                ConfigManager.sync("moremod", Config.Type.INSTANCE);

                // 验证配置
                validateConfig();
            }
        }

        /**
         * 验证并修正配置
         */
        private static void validateConfig() {
            // 验证颜色设置
            if (!isValidColorString(highEnergyColor)) {
                highEnergyColor = "00FF00";
            }
            if (!isValidColorString(mediumEnergyColor)) {
                mediumEnergyColor = "FFFF00";
            }
            if (!isValidColorString(lowEnergyColor)) {
                lowEnergyColor = "FF8800";
            }
            if (!isValidColorString(criticalEnergyColor)) {
                criticalEnergyColor = "FF0000";
            }

            // 验证数值范围
            if (scale < 0.5) scale = 0.5;
            if (scale > 2.0) scale = 2.0;

            if (backgroundAlpha < 0.0) backgroundAlpha = 0.0;
            if (backgroundAlpha > 1.0) backgroundAlpha = 1.0;

            if (maxDisplayUpgrades < 1) maxDisplayUpgrades = 1;
            if (maxDisplayUpgradesCompact < 1) maxDisplayUpgradesCompact = 1;

            if (energyFlowSampleSize < 5) energyFlowSampleSize = 5;
            if (updateFrequency < 1) updateFrequency = 1;

            if (enchantBoostCooldownSec < 30) enchantBoostCooldownSec = 30;
            if (enchantBoostDurationSec < 10) enchantBoostDurationSec = 10;
        }
    }
}