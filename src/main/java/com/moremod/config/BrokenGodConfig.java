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
            "升格所需的激活模块数量（模块等级总和）",
            "Minimum active module count required for ascension (sum of all module levels)"
    })
    @Config.Name("激活模块要求 | Required Active Modules")
    @Config.RangeInt(min = 1, max = 100)
    public static int requiredModuleCount = 50;

    @Config.Comment({
            "升格所需的低人性值累计时间（秒）",
            "Cumulative time at low humanity required for ascension (seconds)",
            "当人性值低于阈值时，每秒累计1秒",
            "默认3天 = 259200秒"
    })
    @Config.Name("低人性累计时间 | Low Humanity Time")
    @Config.RangeInt(min = 60, max = 604800)
    public static int requiredLowHumanitySeconds = 259200; // 3天

    @Config.Comment({
            "低人性值阈值（低于此值时开始累计时间）",
            "Low humanity threshold (accumulates time when below this)"
    })
    @Config.Name("低人性阈值 | Low Humanity Threshold")
    @Config.RangeDouble(min = 5, max = 30)
    public static double lowHumanityThreshold = 15.0;

    // ============================================================
    // 停机模式 (Shutdown Mode)
    // ============================================================

    @Config.Comment({
            "停机模式持续时间（tick）",
            "Shutdown mode duration in ticks"
    })
    @Config.Name("停机时间 | Shutdown Ticks")
    @Config.RangeInt(min = 20, max = 400)
    public static int shutdownTicks = 400;

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

    // 注：critDamage 已迁移至破碎_臂遗物

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
    // 注：bypassInvulnerability, trueDamage 已迁移至破碎遗物系统
    // ============================================================

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
    // 破碎_心核 (Broken Heartcore)
    // ============================================================

    @Config.Comment({
            "压缩后的最大生命值",
            "Compressed maximum health"
    })
    @Config.Name("心核:压缩血量 | Heartcore: Compressed HP")
    @Config.RangeDouble(min = 4, max = 20)
    public static double heartcoreCompressedHP = 10.0;

    @Config.Comment({
            "生命汲取比例 (1.0 = 100%伤害转化为治疗)",
            "Life steal ratio"
    })
    @Config.Name("心核:吸血比例 | Heartcore: Lifesteal Ratio")
    @Config.RangeDouble(min = 0.5, max = 2.0)
    public static double heartcoreLifestealRatio = 1.0;

    @Config.Comment({
            "溢出吸血转化为吸收之心的最大值",
            "Maximum absorption hearts from overflow lifesteal"
    })
    @Config.Name("心核:最大吸收 | Heartcore: Max Absorption")
    @Config.RangeDouble(min = 0, max = 40)
    public static double heartcoreMaxAbsorption = 20.0;

    @Config.Comment({
            "狂战士最大伤害倍率（血量为1时的倍率）",
            "Berserker max damage multiplier at 1 HP"
    })
    @Config.Name("心核:狂战士倍率 | Heartcore: Berserker Multiplier")
    @Config.RangeDouble(min = 2.0, max = 10.0)
    public static double heartcoreBerserkerMaxMultiplier = 5.0;

    // ============================================================
    // 破碎_臂 (Broken Arm)
    // ============================================================

    @Config.Comment({
            "基础伤害倍率（所有攻击）",
            "Base damage multiplier (all attacks)"
    })
    @Config.Name("破碎臂:伤害倍率 | Arm: Damage Multiplier")
    @Config.RangeDouble(min = 1.0, max = 5.0)
    public static double armDamageMultiplier = 2.0;

    @Config.Comment({
            "暴击伤害倍率",
            "Critical damage multiplier"
    })
    @Config.Name("破碎臂:暴击伤害 | Arm: Crit Multiplier")
    @Config.RangeDouble(min = 1.5, max = 5.0)
    public static double armCritMultiplier = 3.0;

    @Config.Comment({
            "护甲粉碎光环范围（格）",
            "Armor shred aura range in blocks"
    })
    @Config.Name("破碎臂:护甲粉碎范围 | Arm: Armor Shred Range")
    @Config.RangeDouble(min = 5, max = 20)
    public static double armArmorShredRange = 10.0;

    @Config.Comment({
            "攻击距离延长（格）",
            "Attack range extension in blocks"
    })
    @Config.Name("破碎臂:距离延长 | Arm: Range Extension")
    @Config.RangeDouble(min = 1.0, max = 5.0)
    public static double armRangeExtension = 3.0;

    // ============================================================
    // 破碎_手 (Broken Hand)
    // ============================================================

    @Config.Comment({
            "攻击速度倍率 (3.0 = 攻速×3)",
            "Attack speed multiplier"
    })
    @Config.Name("破碎手:攻速倍率 | Hand: Speed Multiplier")
    @Config.RangeDouble(min = 1.5, max = 5.0)
    public static double handSpeedMultiplier = 3.0;

    @Config.Comment({
            "近战伤害加成 (1.0 = +100%)",
            "Melee damage bonus"
    })
    @Config.Name("破碎手:近战加成 | Hand: Melee Bonus")
    @Config.RangeDouble(min = 0.5, max = 2.0)
    public static double handMeleeDamageBonus = 1.0;

    @Config.Comment({
            "是否重置攻击冷却",
            "Reset attack cooldown after hit"
    })
    @Config.Name("破碎手:重置冷却 | Hand: Reset Cooldown")
    public static boolean handResetCooldown = true;

    // ============================================================
    // 破碎_枷锁 (Broken Shackles)
    // ============================================================

    @Config.Comment({
            "时空冻结光环范围（格）",
            "Freeze aura range in blocks"
    })
    @Config.Name("枷锁:光环范围 | Shackles: Aura Range")
    @Config.RangeDouble(min = 5, max = 20)
    public static double shacklesAuraRange = 10.0;

    @Config.Comment({
            "自身移速降低比例",
            "Self movement speed reduction"
    })
    @Config.Name("枷锁:自身减速 | Shackles: Self Slow")
    @Config.RangeDouble(min = 0.1, max = 0.5)
    public static double shacklesSelfSlow = 0.3;

    @Config.Comment({
            "所受伤害减免比例",
            "Damage reduction ratio"
    })
    @Config.Name("枷锁:伤害减免 | Shackles: Damage Reduction")
    @Config.RangeDouble(min = 0.3, max = 0.7)
    public static double shacklesDamageReduction = 0.5;

    @Config.Comment({
            "混乱效果持续时间（tick）",
            "Confusion effect duration in ticks"
    })
    @Config.Name("枷锁:混乱时间 | Shackles: Confusion Duration")
    @Config.RangeInt(min = 20, max = 200)
    public static int shacklesConfusionDuration = 60;

    // ============================================================
    // 破碎_投影 (Broken Projection)
    // ============================================================

    @Config.Comment({
            "幻影打击真伤比例 (1.0 = 100%额外真伤)",
            "Phantom strike true damage ratio"
    })
    @Config.Name("投影:真伤比例 | Projection: True Damage Ratio")
    @Config.RangeDouble(min = 0.5, max = 2.0)
    public static double projectionTrueDamageRatio = 1.0;

    @Config.Comment({
            "斩杀阈值（目标血量百分比）",
            "Execute threshold (target HP percentage)"
    })
    @Config.Name("投影:斩杀阈值 | Projection: Execute Threshold")
    @Config.RangeDouble(min = 0.3, max = 0.6)
    public static double projectionExecuteThreshold = 0.5;

    // ============================================================
    // 破碎_终结 (Broken Terminus)
    // ============================================================

    @Config.Comment({
            "伤害放大倍率",
            "Damage amplification multiplier"
    })
    @Config.Name("终结:伤害倍率 | Terminus: Damage Multiplier")
    @Config.RangeDouble(min = 1.5, max = 10.0)
    public static double terminusDamageMultiplier = 5.0;

    @Config.Comment({
            "击杀回复HP",
            "HP restored on kill"
    })
    @Config.Name("终结:击杀回血 | Terminus: Kill Heal")
    @Config.RangeDouble(min = 1, max = 20)
    public static double terminusKillHeal = 5.0;

    @Config.Comment({
            "击杀获得吸收之心",
            "Absorption gained on kill"
    })
    @Config.Name("终结:击杀吸收 | Terminus: Kill Absorption")
    @Config.RangeDouble(min = 0, max = 20)
    public static double terminusKillAbsorption = 10.0;

    // ============================================================
    // 破碎之神吸收之心上限
    // ============================================================

    @Config.Comment({
            "破碎之神吸收之心（黄条）上限",
            "Maximum absorption hearts for Broken God players",
            "0 = 无上限（不推荐）"
    })
    @Config.Name("吸收之心上限 | Max Absorption Hearts")
    @Config.RangeDouble(min = 0, max = 200)
    public static double brokenGodMaxAbsorption = 100.0;

    // ============================================================
    // 视觉效果设置
    // ============================================================

    @Config.Comment({
            "启用破碎之神视觉覆盖效果（数字噪点、扫描线、撕裂效果）",
            "Enable Broken God visual overlay effects (digital noise, scanlines, glitch)",
            "关闭后破碎之神状态下不会显示特殊视觉效果"
    })
    @Config.Name("启用视觉效果 | Enable Visual Overlay")
    public static boolean enableVisualOverlay = true;

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
