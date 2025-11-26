package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 破碎之神配置
 * Broken God Ascension Configuration
 */
@Config(modid = "moremod", name = "moremod/broken_god", category = "broken_god")
@Config.LangKey("config.moremod.broken_god")
public class BrokenGodConfig {

    // ============================================================
    // 升格条件
    // ============================================================

    @Config.Comment({
            "升格所需的最大人性值阈值",
            "Maximum humanity threshold for ascension"
    })
    @Config.Name("人性阈值 | Humanity Threshold")
    @Config.RangeDouble(min = 0, max = 20)
    public static double ascensionHumanityThreshold = 5.0;

    @Config.Comment({
            "升格所需的最少模块数量",
            "Minimum installed modules required for ascension"
    })
    @Config.Name("模块数量要求 | Required Module Count")
    @Config.RangeInt(min = 1, max = 50)
    public static int requiredModuleCount = 20;

    @Config.Comment({
            "升格所需的崩解存活次数",
            "Dissolution survivals required for ascension"
    })
    @Config.Name("崩解存活次数 | Dissolution Survivals")
    @Config.RangeInt(min = 1, max = 10)
    public static int requiredDissolutionSurvivals = 3;

    // ============================================================
    // 停机模式 (Shutdown Mode)
    // ============================================================

    @Config.Comment({
            "停机模式持续时间（tick）",
            "Shutdown mode duration in ticks"
    })
    @Config.Name("停机时间 | Shutdown Ticks")
    @Config.RangeInt(min = 20, max = 200)
    public static int shutdownTicks = 40;

    @Config.Comment({
            "重启后恢复的生命值",
            "Health restored after restart"
    })
    @Config.Name("重启恢复血量 | Restart Heal")
    @Config.RangeDouble(min = 0.5, max = 20)
    public static double restartHeal = 1.0;

    @Config.Comment({
            "停机期间是否无敌",
            "Invulnerable during shutdown"
    })
    @Config.Name("停机无敌 | Shutdown Invulnerable")
    public static boolean invulnerableDuringShutdown = true;

    @Config.Comment({
            "阻止死亡画面",
            "Prevent death screen from appearing"
    })
    @Config.Name("阻止死亡画面 | Prevent Death Screen")
    public static boolean preventDeathScreen = true;

    // ============================================================
    // 战斗能力
    // ============================================================

    @Config.Comment({
            "近战伤害加成 (0.6 = +60%)",
            "Melee damage bonus"
    })
    @Config.Name("近战伤害加成 | Melee Damage Bonus")
    @Config.RangeDouble(min = 0, max = 2)
    public static double meleeDamageBonus = 0.6;

    @Config.Comment({
            "攻击速度加成 (0.15 = +15%)",
            "Attack speed bonus"
    })
    @Config.Name("攻击速度加成 | Attack Speed Bonus")
    @Config.RangeDouble(min = 0, max = 1)
    public static double attackSpeedBonus = 0.15;

    @Config.Comment({
            "暴击伤害倍率",
            "Critical damage multiplier"
    })
    @Config.Name("暴击伤害 | Crit Damage")
    @Config.RangeDouble(min = 1, max = 5)
    public static double critDamage = 2.0;

    @Config.Comment({
            "敌人困惑概率",
            "Chance to confuse enemies on hit"
    })
    @Config.Name("困惑概率 | Confusion Chance")
    @Config.RangeDouble(min = 0, max = 1)
    public static double confusionChance = 0.3;

    @Config.Comment({
            "困惑持续时间（tick）",
            "Confusion duration in ticks"
    })
    @Config.Name("困惑时间 | Confusion Duration")
    @Config.RangeInt(min = 20, max = 200)
    public static int confusionDuration = 60;

    // ============================================================
    // 扭曲脉冲 (Distortion Pulse)
    // ============================================================

    @Config.Comment({
            "扭曲脉冲触发伤害阈值",
            "Damage threshold to trigger distortion pulse"
    })
    @Config.Name("脉冲触发伤害 | Pulse Trigger Damage")
    @Config.RangeDouble(min = 5, max = 50)
    public static double pulseTriggerDamage = 20.0;

    @Config.Comment({
            "扭曲脉冲范围",
            "Distortion pulse range"
    })
    @Config.Name("脉冲范围 | Pulse Range")
    @Config.RangeDouble(min = 5, max = 20)
    public static double pulseRange = 10.0;

    @Config.Comment({
            "扭曲脉冲真实伤害",
            "Distortion pulse true damage"
    })
    @Config.Name("脉冲伤害 | Pulse Damage")
    @Config.RangeDouble(min = 1, max = 20)
    public static double pulseDamage = 8.0;

    @Config.Comment({
            "扭曲脉冲冷却时间（tick）",
            "Distortion pulse cooldown in ticks"
    })
    @Config.Name("脉冲冷却 | Pulse Cooldown")
    @Config.RangeInt(min = 20, max = 400)
    public static int pulseCooldown = 80;

    @Config.Comment({
            "扭曲脉冲致盲时间（tick）",
            "Distortion pulse blindness duration"
    })
    @Config.Name("脉冲致盲时间 | Pulse Blindness")
    @Config.RangeInt(min = 10, max = 100)
    public static int pulseBlindnessDuration = 20;

    // ============================================================
    // 破碎三件套属性
    // ============================================================

    @Config.Comment({
            "破碎之手：是否无视无敌帧",
            "Broken Hand: Bypass i-frames"
    })
    @Config.Name("无视无敌帧 | Bypass Invulnerability")
    public static boolean bypassInvulnerability = true;

    @Config.Comment({
            "破碎之手：是否造成真实伤害",
            "Broken Hand: Deal true damage (bypass armor)"
    })
    @Config.Name("真实伤害 | True Damage")
    public static boolean trueDamage = true;

    @Config.Comment({
            "破碎之心：最小血量（不会低于此值）",
            "Broken Heart: Minimum health (won't drop below)"
    })
    @Config.Name("最小血量 | Minimum Health")
    @Config.RangeDouble(min = 0.5, max = 5)
    public static double minimumHealth = 1.0;

    @Config.Comment({
            "破碎之眼：是否启用实体高亮",
            "Broken Eye: Enable entity outline (ESP)"
    })
    @Config.Name("实体高亮 | Entity Outline")
    public static boolean entityOutline = true;

    @Config.Comment({
            "破碎之眼：怪物侦测距离减少比例",
            "Broken Eye: Mob detection range reduction"
    })
    @Config.Name("侦测减少 | Detection Reduction")
    @Config.RangeDouble(min = 0, max = 1)
    public static double detectionReduction = 0.5;

    // ============================================================
    // 配置变更监听
    // ============================================================

    @Mod.EventBusSubscriber(modid = "moremod")
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals("moremod")) {
                ConfigManager.sync("moremod", Config.Type.INSTANCE);
            }
        }
    }
}
