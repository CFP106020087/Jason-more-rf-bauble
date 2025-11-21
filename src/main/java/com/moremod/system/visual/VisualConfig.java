package com.moremod.system.visual;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 排异视觉效果配置
 * 控制心跳音效和暗角效果的触发阈值
 */
@Config(modid = "moremod", name = "MoreMod/RejectionVisuals", category = "visuals")
@SideOnly(Side.CLIENT)
public class VisualConfig {

    @Config.Comment("心跳音效开始阈值（排异%）")
    @Config.RangeDouble(min = 0, max = 100)
    public static float HEARTBEAT_START = 50f;

    @Config.Comment("心跳最慢间隔（毫秒）")
    @Config.RangeInt(min = 100, max = 2000)
    public static int HEARTBEAT_MAX_INTERVAL = 800;
    
    @Config.Comment("心跳最快间隔（毫秒）")
    @Config.RangeInt(min = 100, max = 1000)
    public static int HEARTBEAT_MIN_INTERVAL = 300;

    @Config.Comment("血液暗角开始阈值（排异%）")
    @Config.RangeDouble(min = 0, max = 100)
    public static float VIGNETTE_START = 40f;
    
    @Config.Comment("启用心跳音效")
    public static boolean ENABLE_HEARTBEAT = true;
    
    @Config.Comment("启用血液暗角效果")
    public static boolean ENABLE_VIGNETTE = true;
    
    @Config.Comment("启用屏幕震动效果（高排异时）")
    public static boolean ENABLE_SCREEN_SHAKE = true;
    
    @Config.Comment("屏幕震动开始阈值（排异%）")
    @Config.RangeDouble(min = 0, max = 100)
    public static float SCREEN_SHAKE_START = 70f;
    
    @Config.Comment("启用视野变窄效果（高排异时）")
    public static boolean ENABLE_FOV_CHANGE = false;
    
    @Config.Comment("视野变窄开始阈值（排异%）")
    @Config.RangeDouble(min = 0, max = 100)
    public static float FOV_CHANGE_START = 60f;
    
    @Config.Comment("最大视野缩减量")
    @Config.RangeDouble(min = 0, max = 0.5)
    public static float MAX_FOV_REDUCTION = 0.2f;
    
    @Config.Comment("启用调试模式")
    public static boolean DEBUG_MODE = false;
    
    @Config.Comment("HUD显示排异数值")
    public static boolean SHOW_REJECTION_NUMBER = true;
    
    @Config.Comment("HUD显示适应度数值")
    public static boolean SHOW_ADAPTATION_NUMBER = true;
    
    @Config.Comment("HUD X位置偏移")
    @Config.RangeInt(min = -500, max = 500)
    public static int HUD_X_OFFSET = 10;
    
    @Config.Comment("HUD Y位置偏移")
    @Config.RangeInt(min = -500, max = 500)
    public static int HUD_Y_OFFSET = 10;
    
    @Config.Comment("HUD位置（0=左上, 1=右上, 2=左下, 3=右下）")
    @Config.RangeInt(min = 0, max = 3)
    public static int HUD_POSITION = 0;
    
    @Config.Comment("血液暗角颜色调整 - 红色")
    @Config.RangeDouble(min = 0, max = 1)
    public static float VIGNETTE_RED = 1.0f;
    
    @Config.Comment("血液暗角颜色调整 - 绿色")
    @Config.RangeDouble(min = 0, max = 1)
    public static float VIGNETTE_GREEN = 0.0f;
    
    @Config.Comment("血液暗角颜色调整 - 蓝色")
    @Config.RangeDouble(min = 0, max = 1)
    public static float VIGNETTE_BLUE = 0.0f;
    
    @Config.Comment("心跳音量倍率")
    @Config.RangeDouble(min = 0, max = 2)
    public static float HEARTBEAT_VOLUME_MULTIPLIER = 1.0f;
    
    @Config.Comment("粒子效果强度")
    @Config.RangeDouble(min = 0, max = 2)
    public static float PARTICLE_INTENSITY = 1.0f;
    
    @Config.Comment("启用血液粒子效果")
    public static boolean ENABLE_BLOOD_PARTICLES = true;
    
    @Config.Comment("血液粒子开始阈值（排异%）")
    @Config.RangeDouble(min = 0, max = 100)
    public static float BLOOD_PARTICLES_START = 80f;
    
    // 配置重载处理
    @Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
    public static class ConfigSync {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals("moremod")) {
                ConfigManager.sync("moremod", Config.Type.INSTANCE);
                
                // 配置更新后重置音效系统
                if (event.getConfigID().equals("RejectionVisuals")) {
                    HeartbeatEffect.reset();
                    System.out.println("[排异视觉] 配置已更新");
                }
            }
        }
    }
    
    /**
     * 验证配置有效性
     */
    public static void validateConfig() {
        // 确保心跳间隔合理
        if (HEARTBEAT_MIN_INTERVAL >= HEARTBEAT_MAX_INTERVAL) {
            HEARTBEAT_MIN_INTERVAL = HEARTBEAT_MAX_INTERVAL - 100;
            System.err.println("[视觉配置] 心跳间隔配置错误，已自动修正");
        }
        
        // 确保阈值合理
        if (VIGNETTE_START > 100) {
            VIGNETTE_START = 40f;
            System.err.println("[视觉配置] 暗角阈值超出范围，已重置");
        }
        
        if (HEARTBEAT_START > 100) {
            HEARTBEAT_START = 50f;
            System.err.println("[视觉配置] 心跳阈值超出范围，已重置");
        }
    }
}
