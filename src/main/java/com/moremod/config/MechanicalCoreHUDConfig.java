package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 机械核心HUD配置 - 最终适配版
 * 完美配合 MechanicalCoreHUD (V5/V6) 使用
 */
@Config(modid = "moremod", name = "moremod/mechanical_core_hud", category = "client")
@Config.LangKey("config.moremod.hud")
public class MechanicalCoreHUDConfig {

    // ==================== 基础设置 ====================

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

    // ==================== 显示模块开关 ====================

    @Config.Comment("显示能量流 (RF/t)")
    public static boolean showEnergyFlow = true;

    @Config.Comment("显示效率信息")
    public static boolean showEfficiency = true;

    @Config.Comment("显示活跃升级列表")
    public static boolean showActiveUpgrades = true;

    @Config.Comment("显示低能量/排异警告")
    public static boolean showWarnings = true;

    @Config.Comment("显示产能模块状态 (太阳能/动能等)")
    public static boolean showGenerators = true;

    @Config.Comment("显示战斗信息 (伤害/攻速加成)")
    public static boolean showCombatInfo = true;

    @Config.Comment("显示被动效果 (护甲/容量等)")
    public static boolean showPassiveEffects = true;

    // ==================== 列表显示设置 ====================

    @Config.Comment("紧凑模式（只显示图标，减少文字）")
    public static boolean compactMode = false;

    @Config.Comment("功能图标缩放比例")
    @Config.RangeDouble(min = 0.5, max = 2.0)
    public static double iconScale = 1.0;

    @Config.Comment("最大显示升级数量（正常模式）")
    @Config.RangeInt(min = 3, max = 20)
    public static int maxDisplayUpgrades = 10;

    @Config.Comment("最大显示升级数量（紧凑模式）")
    @Config.RangeInt(min = 1, max = 10)
    public static int maxDisplayUpgradesCompact = 5;

    @Config.Comment("优先显示关键功能 (如飞行、夜视)")
    public static boolean prioritizeImportantUpgrades = true;

    @Config.Comment("自动隐藏无关功能")
    public static boolean autoHideIrrelevant = false;

    @Config.Comment("显示滚动提示 (如 [1-10/15])")
    public static boolean showScrollHints = true;

    // ==================== 视觉特效 ====================

    @Config.Comment("使用动画效果 (脉冲、闪烁)")
    public static boolean useAnimations = true;

    @Config.Comment("背景透明度")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double backgroundAlpha = 0.7;

    @Config.Comment("启用低能量脉冲警报")
    public static boolean enablePulseEffect = true;

    @Config.Comment("启用能量条高光流光")
    public static boolean enableEnergyBarShimmer = true;

    // ==================== 高级性能设置 ====================

    @Config.Comment("能量流采样时间（tick）- 越大数值越平滑但延迟越高")
    @Config.RangeInt(min = 5, max = 60)
    public static int energyFlowSampleSize = 20;

    @Config.Comment("HUD更新频率（每N tick更新一次）- 增加可提高FPS")
    @Config.RangeInt(min = 1, max = 20)
    public static int updateFrequency = 1;

    @Config.Comment("启用性能优化模式 (减少粒子和复杂计算)")
    public static boolean performanceMode = false;

    @Config.Comment("减少动画计算频率")
    public static boolean reduceAnimationCalculations = false;

    // ==================== 颜色自定义 ====================

    @Config.Comment("启用自定义能量条颜色")
    public static boolean useCustomEnergyColors = false;

    @Config.Comment("高能量颜色 (十六进制，如 00FF00)")
    public static String highEnergyColor = "00FF00";

    @Config.Comment("中等能量颜色 (十六进制，如 FFFF00)")
    public static String mediumEnergyColor = "FFFF00";

    @Config.Comment("低能量颜色 (十六进制，如 FF8800)")
    public static String lowEnergyColor = "FF8800";

    @Config.Comment("危险能量颜色 (十六进制，如 FF0000)")
    public static String criticalEnergyColor = "FF0000";

    // ==================== 游戏性配置 (可选) ====================

    @Config.Comment("附魔增强冷却时间（秒）")
    @Config.RangeInt(min = 30, max = 300)
    public static int enchantBoostCooldownSec = 120;

    @Config.Comment("附魔增强持续时间（秒）")
    @Config.RangeInt(min = 10, max = 120)
    public static int enchantBoostDurationSec = 60;

    // ==================== 运行时状态 (不保存) ====================

    // HUD可见状态，由 KeyBindHandler 控制，不写入文件
    private static boolean hudVisible = true; 

    // ==================== 枚举定义 ====================

    public enum HUDPosition {
        TOP_LEFT("左上"),
        TOP_RIGHT("右上"),
        BOTTOM_LEFT("左下"),
        BOTTOM_RIGHT("右下"),
        CUSTOM("自定义");

        private final String displayName;
        HUDPosition(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // ==================== 核心辅助方法 (HUD 类必需) ====================

    /**
     * 检查HUD是否应该显示
     */
    public static boolean shouldHideHUD() {
        return !enabled || !hudVisible;
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
     * 检查是否应该启用动画
     */
    public static boolean shouldUseAnimations() {
        if (performanceMode && reduceAnimationCalculations) return false;
        return useAnimations;
    }

    /**
     * 获取自定义能量颜色 (支持 Hex 解析)
     */
    public static int getEnergyColor(float percent) {
        if (!useCustomEnergyColors) {
            if (percent > 0.6f) return 0xFF00FF00; // 绿
            if (percent > 0.3f) return 0xFFFFFF00; // 黄
            if (percent > 0.1f) return 0xFFFF8800; // 橙
            return 0xFFFF0000; // 红
        }

        if (percent > 0.6f) return parseColorString(highEnergyColor);
        if (percent > 0.3f) return parseColorString(mediumEnergyColor);
        if (percent > 0.1f) return parseColorString(lowEnergyColor);
        return parseColorString(criticalEnergyColor);
    }

    /**
     * 解析十六进制颜色字符串
     */
    public static int parseColorString(String colorStr) {
        try {
            if (colorStr.startsWith("#")) colorStr = colorStr.substring(1);
            return (int) Long.parseLong(colorStr, 16) | 0xFF000000;
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF;
        }
    }

    // ==================== 配置同步系统 ====================

    @Mod.EventBusSubscriber(modid = "moremod")
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals("moremod")) {
                ConfigManager.sync("moremod", Config.Type.INSTANCE);
                validateConfig();
            }
        }

        private static void validateConfig() {
            // 简单验证，防止非法数值
            if (scale < 0.5) scale = 0.5;
            if (maxDisplayUpgrades < 1) maxDisplayUpgrades = 1;
            if (backgroundAlpha < 0) backgroundAlpha = 0;
        }
    }
}