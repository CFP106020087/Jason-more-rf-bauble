package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 血肉排异HUD提示配置
 */
@Config(modid = "moremod", name = "MoreMod/FleshRejectionHUD", category = "flesh_rejection_hud")
@Mod.EventBusSubscriber(modid = "moremod")
public class FleshRejectionHUDConfig {
    
    @Config.Comment("启用HUD提示系统")
    @Config.LangKey("config.moremod.hud.enable")
    public static boolean enableHUD = true;
    
    @Config.Comment("启用新手引导提示（首次触发机制时显示）")
    @Config.LangKey("config.moremod.hud.guides")
    public static boolean enableGuides = true;
    
    @Config.Comment("显示排异值变化提示")
    @Config.LangKey("config.moremod.hud.rejection_changes")
    public static boolean showRejectionChanges = true;
    
    @Config.Comment("显示状态效果提示（失误、出血等）")
    @Config.LangKey("config.moremod.hud.status_effects")
    public static boolean showStatusEffects = true;
    
    @Config.Comment("显示恢复提示（睡眠、急救等）")
    @Config.LangKey("config.moremod.hud.recovery")
    public static boolean showRecoveryHints = true;
    
    @Config.Comment("显示警告提示（高排异警告）")
    @Config.LangKey("config.moremod.hud.warnings")
    public static boolean showWarnings = true;
    
    @Config.Comment("显示里程碑提示（适应度进度）")
    @Config.LangKey("config.moremod.hud.milestones")
    public static boolean showMilestones = true;
    
    @Config.Comment("最小显示排异变化值（低于此值不显示）")
    @Config.LangKey("config.moremod.hud.min_change")
    @Config.RangeDouble(min = 0.1, max = 5.0)
    public static double minChangeToShow = 0.5;
    
    @Config.Comment("HUD消息显示时间（毫秒）")
    @Config.LangKey("config.moremod.hud.duration")
    @Config.RangeInt(min = 500, max = 5000)
    public static int hudMessageDuration = 1500;
    
    @Config.Comment("HUD消息冷却时间（毫秒，防止刷屏）")
    @Config.LangKey("config.moremod.hud.cooldown")
    @Config.RangeInt(min = 500, max = 5000)
    public static int hudCooldown = 2000;
    
    @Config.Comment("HUD位置（0=右中，1=右上，2=右下，3=左中）")
    @Config.LangKey("config.moremod.hud.position")
    @Config.RangeInt(min = 0, max = 3)
    public static int hudPosition = 0;
    
    @Config.Comment("详细模式（显示更多技术细节）")
    @Config.LangKey("config.moremod.hud.verbose")
    public static boolean verboseMode = false;
    
    @Config.Comment("调试模式（显示所有内部数值变化）")
    @Config.LangKey("config.moremod.hud.debug")
    public static boolean debugMode = false;
    
    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals("moremod")) {
            ConfigManager.sync("moremod", Config.Type.INSTANCE);
        }
    }
}