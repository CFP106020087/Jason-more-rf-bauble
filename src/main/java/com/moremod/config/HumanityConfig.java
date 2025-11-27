package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 人性值光谱系统配置
 * Humanity Spectrum System Configuration
 */
@Config(modid = "moremod", name = "moremod/humanity_spectrum", category = "humanity_spectrum")
@Config.LangKey("config.moremod.humanity_spectrum")
public class HumanityConfig {

    // ============================================================
    // 系统总开关
    // ============================================================

    @Config.Comment({
            "人性值系统总开关",
            "Enable/Disable Humanity Spectrum System"
    })
    @Config.Name("启用人性值系统 | Enable Humanity System")
    public static boolean enableHumanitySystem = true;

    @Config.Comment({
            "突破后的初始人性值",
            "Initial humanity value after transcendence"
    })
    @Config.Name("初始人性值 | Initial Humanity")
    @Config.RangeDouble(min = 0, max = 100)
    public static double initialHumanity = 75.0;

    @Config.Comment({
            "崩解状态持续时间(秒)",
            "Dissolution state duration in seconds"
    })
    @Config.Name("崩解持续时间 | Dissolution Duration")
    @Config.RangeInt(min = 10, max = 300)
    public static int dissolutionDuration = 60;

    @Config.Comment({
            "崩解期间每秒造成的真实伤害（无视护甲）",
            "True damage dealt per second during dissolution (bypasses armor)"
    })
    @Config.Name("崩解伤害/秒 | Dissolution Damage/sec")
    @Config.RangeDouble(min = 0, max = 5)
    public static double dissolutionDamagePerSec = 1.0;

    @Config.Comment({
            "存在锚定持续时间(小时)",
            "Existence anchor duration in hours"
    })
    @Config.Name("存在锚定时长 | Existence Anchor Duration")
    @Config.RangeInt(min = 1, max = 72)
    public static int existenceAnchorDuration = 24;

    // ============================================================
    // 显示设置
    // ============================================================

    @Config.Comment({
            "显示人性值HUD",
            "Show humanity HUD"
    })
    @Config.Name("显示HUD | Show HUD")
    public static boolean showHumanityHUD = true;

    @Config.Comment({
            "启用视觉扭曲效果（低人性时）",
            "Enable visual distortion effects at low humanity"
    })
    @Config.Name("启用视觉扭曲 | Enable Visual Distortion")
    public static boolean enableVisualDistortion = true;

    @Config.Comment({
            "HUD X轴偏移",
            "HUD X offset"
    })
    @Config.Name("HUD X偏移 | HUD X Offset")
    public static int hudXOffset = 10;

    @Config.Comment({
            "HUD Y轴偏移",
            "HUD Y offset"
    })
    @Config.Name("HUD Y偏移 | HUD Y Offset")
    public static int hudYOffset = 10;

    // ============================================================
    // 人性值流失配置 - 扭曲模块使用
    // ============================================================

    @Config.Comment({
            "使用虚空发电机消耗的人性值",
            "Humanity drain from Void Generator use"
    })
    @Config.Name("虚空发电机消耗 | Void Generator Drain")
    @Config.RangeDouble(min = 0, max = 20)
    public static double voidGeneratorDrain = 3.0;

    @Config.Comment({
            "使用裂隙脉冲消耗的人性值",
            "Humanity drain from Rift Pulse use"
    })
    @Config.Name("裂隙脉冲消耗 | Rift Pulse Drain")
    @Config.RangeDouble(min = 0, max = 20)
    public static double riftPulseDrain = 5.0;

    @Config.Comment({
            "使用相位转移消耗的人性值",
            "Humanity drain from Phase Shift use"
    })
    @Config.Name("相位转移消耗 | Phase Shift Drain")
    @Config.RangeDouble(min = 0, max = 20)
    public static double phaseShiftDrain = 2.0;

    @Config.Comment({
            "维度跳跃消耗的人性值",
            "Humanity drain from Dimension Hop"
    })
    @Config.Name("维度跳跃消耗 | Dimension Hop Drain")
    @Config.RangeDouble(min = 0, max = 30)
    public static double dimensionHopDrain = 8.0;

    // ============================================================
    // 人性值流失配置 - 持续消耗
    // ============================================================

    @Config.Comment({
            "超限模式每秒消耗的人性值",
            "Humanity drain per second during Overclock"
    })
    @Config.Name("超限模式消耗/秒 | Overclock Drain/sec")
    @Config.RangeDouble(min = 0, max = 1)
    public static double overclockDrainPerSec = 0.05;

    @Config.Comment({
            "熬夜开始消耗人性值的时间(分钟游戏时间)",
            "Minutes before sleep deprivation starts draining humanity"
    })
    @Config.Name("熬夜开始时间(分钟) | Sleep Deprivation Start")
    @Config.RangeInt(min = 30, max = 200)
    public static int sleepDeprivationStartMinutes = 72;

    @Config.Comment({
            "熬夜每秒消耗的人性值",
            "Humanity drain per second from sleep deprivation"
    })
    @Config.Name("熬夜消耗/秒 | Sleep Deprivation Drain/sec")
    @Config.RangeDouble(min = 0, max = 0.5)
    public static double sleepDeprivationDrainPerSec = 0.02;

    @Config.Comment({
            "异常维度滞留每秒消耗的人性值",
            "Humanity drain per second in abnormal dimensions"
    })
    @Config.Name("异常维度消耗/秒 | Abnormal Dimension Drain/sec")
    @Config.RangeDouble(min = 0, max = 0.5)
    public static double abnormalDimensionDrainPerSec = 0.03;

    @Config.Comment({
            "屠杀被动生物消耗的人性值",
            "Humanity drain from killing passive mobs"
    })
    @Config.Name("杀死被动生物消耗 | Kill Passive Mob Drain")
    @Config.RangeDouble(min = 0, max = 5)
    public static double killPassiveMobDrain = 0.5;

    @Config.Comment({
            "屠杀村民消耗的人性值",
            "Humanity drain from killing villagers"
    })
    @Config.Name("杀死村民消耗 | Kill Villager Drain")
    @Config.RangeDouble(min = 0, max = 15)
    public static double killVillagerDrain = 3.0;

    @Config.Comment({
            "低人性战斗状态每秒消耗的人性值（人性<50%时）",
            "Humanity drain per second during combat when humanity < 50%"
    })
    @Config.Name("低人性战斗消耗/秒 | Low Humanity Combat Drain/sec")
    @Config.RangeDouble(min = 0, max = 0.5)
    public static double combatDrainPerSec = 0.1;

    // ============================================================
    // 人性值恢复配置
    // ============================================================

    @Config.Comment({
            "睡眠恢复的人性值",
            "Humanity restored from sleeping"
    })
    @Config.Name("睡眠恢复 | Sleep Restore")
    @Config.RangeDouble(min = 0, max = 30)
    public static double sleepRestore = 8.0;

    @Config.Comment({
            "睡眠最多恢复到的人性值上限",
            "Maximum humanity that can be restored through sleep"
    })
    @Config.Name("睡眠恢复上限 | Sleep Restore Cap")
    @Config.RangeDouble(min = 50, max = 100)
    public static double sleepRestoreCap = 85.0;

    @Config.Comment({
            "吃熟食恢复的人性值",
            "Humanity restored from eating cooked food"
    })
    @Config.Name("熟食恢复 | Cooked Food Restore")
    @Config.RangeDouble(min = 0, max = 2)
    public static double cookedFoodRestore = 0.3;

    @Config.Comment({
            "吃复杂料理恢复的人性值",
            "Humanity restored from complex meals"
    })
    @Config.Name("复杂料理恢复 | Complex Meal Restore")
    @Config.RangeDouble(min = 0, max = 3)
    public static double complexMealRestore = 0.8;

    @Config.Comment({
            "吃金苹果等恢复的人性值",
            "Humanity restored from golden food"
    })
    @Config.Name("金苹果恢复 | Golden Food Restore")
    @Config.RangeDouble(min = 0, max = 5)
    public static double goldenFoodRestore = 1.5;

    @Config.Comment({
            "主世界日光下每秒恢复的人性值",
            "Humanity restored per second under sunlight in overworld"
    })
    @Config.Name("日光恢复/秒 | Sunlight Restore/sec")
    @Config.RangeDouble(min = 0, max = 0.1)
    public static double sunlightRestorePerSec = 0.01;

    @Config.Comment({
            "与村民交易恢复的人性值",
            "Humanity restored from trading with villagers"
    })
    @Config.Name("交易恢复 | Trade Restore")
    @Config.RangeDouble(min = 0, max = 5)
    public static double villagerTradeRestore = 1.5;

    @Config.Comment({
            "收获作物恢复的人性值",
            "Humanity restored from harvesting crops"
    })
    @Config.Name("收获恢复 | Harvest Restore")
    @Config.RangeDouble(min = 0, max = 1)
    public static double harvestRestore = 0.2;

    @Config.Comment({
            "喂养动物恢复的人性值",
            "Humanity restored from feeding animals"
    })
    @Config.Name("喂养恢复 | Feed Animal Restore")
    @Config.RangeDouble(min = 0, max = 1)
    public static double feedAnimalRestore = 0.3;

    // ============================================================
    // 猎人协议配置
    // ============================================================

    @Config.Comment({
            "样本基础掉率",
            "Base drop rate for biological samples"
    })
    @Config.Name("样本基础掉率 | Base Sample Drop Rate")
    @Config.RangeDouble(min = 0, max = 1)
    public static double baseSampleDropRate = 0.05;

    @Config.Comment({
            "人性每+10%增加的掉率",
            "Drop rate bonus per 10% humanity"
    })
    @Config.Name("人性掉率加成 | Humanity Drop Bonus")
    @Config.RangeDouble(min = 0, max = 0.1)
    public static double humanityDropBonus = 0.02;

    @Config.Comment({
            "灰域(40-60%人性)掉率乘数",
            "Drop rate multiplier in grey zone (40-60% humanity)"
    })
    @Config.Name("灰域掉率乘数 | Grey Zone Drop Multiplier")
    @Config.RangeDouble(min = 0.1, max = 1)
    public static double greyZoneDropMultiplier = 0.5;

    @Config.Comment({
            "对未分析生物的伤害惩罚",
            "Damage penalty against unknown enemies"
    })
    @Config.Name("未知敌人伤害惩罚 | Unknown Enemy Penalty")
    @Config.RangeDouble(min = 0, max = 0.5)
    public static double unknownEnemyPenalty = 0.10;

    // ============================================================
    // 异常协议配置
    // ============================================================

    @Config.Comment({
            "畸变脉冲触发概率（10%以下人性）",
            "Distortion pulse trigger chance (below 10% humanity)"
    })
    @Config.Name("畸变脉冲概率 | Distortion Pulse Chance")
    @Config.RangeDouble(min = 0, max = 1)
    public static double distortionPulseChance = 0.30;

    // ============================================================
    // 极低人性惩罚配置 (<10%)
    // ============================================================

    @Config.Comment({
            "极低人性(<10%)时，无痛麻木触发概率",
            "玩家因感受不到疼痛而无法保护要害，受到的伤害优先命中头部和躯干",
            "Painless numbness trigger chance - received damage prioritizes head/body (First Aid)"
    })
    @Config.Name("无痛麻木概率 | Painless Numbness Chance")
    @Config.RangeDouble(min = 0, max = 1)
    public static double extremeLowHumanityCritChance = 0.35;

    @Config.Comment({
            "无痛麻木时的伤害转移比例",
            "多少四肢伤害会被转移到头部和躯干（0.5 = 50%四肢伤害转移到要害）",
            "How much limb damage is transferred to head/body (0.5 = 50% transferred)"
    })
    @Config.Name("伤害转移比例 | Vital Damage Transfer Ratio")
    @Config.RangeDouble(min = 1.0, max = 3.0)
    public static double extremeLowHumanityCritMultiplier = 1.5;

    @Config.Comment({
            "极低人性(<10%)时，攻击是否会波及周围生物",
            "Whether attacks spread to nearby entities at extreme low humanity"
    })
    @Config.Name("攻击波及周围 | AoE Damage Spread")
    public static boolean extremeLowHumanityAoEDamage = true;

    @Config.Comment({
            "攻击波及范围（方块）",
            "AoE damage spread range in blocks"
    })
    @Config.Name("波及范围 | AoE Range")
    @Config.RangeDouble(min = 1, max = 8)
    public static double extremeLowHumanityAoERange = 3.0;

    @Config.Comment({
            "波及伤害占原伤害的比例",
            "AoE damage as percentage of original damage"
    })
    @Config.Name("波及伤害比例 | AoE Damage Ratio")
    @Config.RangeDouble(min = 0.1, max = 1.0)
    public static double extremeLowHumanityAoEDamageRatio = 0.3;

    @Config.Comment({
            "完全禁止交易的人性阈值",
            "Humanity threshold below which all trading is blocked"
    })
    @Config.Name("禁止交易阈值 | No Trade Threshold")
    @Config.RangeDouble(min = 0, max = 30)
    public static double noTradeThreshold = 10.0;

    @Config.Comment({
            "异常场最大半径（0%人性时）",
            "Maximum anomaly field radius at 0% humanity"
    })
    @Config.Name("异常场最大半径 | Max Anomaly Field Radius")
    @Config.RangeDouble(min = 1, max = 10)
    public static double maxAnomalyFieldRadius = 5.0;

    @Config.Comment({
            "低人性治疗效果降低比例",
            "Healing reduction at low humanity"
    })
    @Config.Name("低人性治疗降低 | Low Humanity Healing Reduction")
    @Config.RangeDouble(min = 0, max = 0.9)
    public static double lowHumanityHealingReduction = 0.50;

    // ============================================================
    // 灰域配置
    // ============================================================

    @Config.Comment({
            "观测坍缩触发概率",
            "Quantum collapse trigger chance"
    })
    @Config.Name("观测坍缩概率 | Quantum Collapse Chance")
    @Config.RangeDouble(min = 0, max = 1)
    public static double quantumCollapseChance = 0.20;

    @Config.Comment({
            "观测坍缩人性偏移范围",
            "Humanity shift range on quantum collapse"
    })
    @Config.Name("观测坍缩偏移范围 | Quantum Collapse Shift Range")
    @Config.RangeDouble(min = 5, max = 30)
    public static double quantumCollapseShiftRange = 15.0;

    // ============================================================
    // NPC交互配置
    // ============================================================

    @Config.Comment({
            "低人性NPC交互阈值（低于此值无法交互）",
            "Humanity threshold for NPC interaction"
    })
    @Config.Name("NPC交互阈值 | NPC Interaction Threshold")
    @Config.RangeDouble(min = 0, max = 50)
    public static double npcInteractionThreshold = 25.0;

    @Config.Comment({
            "使用Synergy链结站所需的人性值阈值",
            "Humanity threshold required to use Synergy Station"
    })
    @Config.Name("链结站人性阈值 | Synergy Station Threshold")
    @Config.RangeDouble(min = 0, max = 100)
    public static double synergyStationThreshold = 60.0;

    @Config.Comment({
            "中低人性(25-50%)交易价格乘数",
            "Trade price multiplier at medium-low humanity"
    })
    @Config.Name("中低人性价格乘数 | Mid-Low Humanity Price Multiplier")
    @Config.RangeDouble(min = 1, max = 5)
    public static double midLowHumanityPriceMultiplier = 3.0;

    @Config.Comment({
            "中等人性(50-75%)交易价格乘数",
            "Trade price multiplier at medium humanity"
    })
    @Config.Name("中等人性价格乘数 | Medium Humanity Price Multiplier")
    @Config.RangeDouble(min = 1, max = 3)
    public static double mediumHumanityPriceMultiplier = 1.5;

    // ============================================================
    // 异常协议技能消耗
    // ============================================================

    @Config.Comment({
            "相位闪烁消耗的人性值",
            "Humanity cost for Phase Blink"
    })
    @Config.Name("相位闪烁消耗 | Phase Blink Cost")
    @Config.RangeDouble(min = 0, max = 15)
    public static double phaseBlinkCost = 3.0;

    @Config.Comment({
            "虚空撕裂消耗的人性值",
            "Humanity cost for Void Tear"
    })
    @Config.Name("虚空撕裂消耗 | Void Tear Cost")
    @Config.RangeDouble(min = 0, max = 20)
    public static double voidTearCost = 5.0;

    @Config.Comment({
            "存在抹消消耗的人性值",
            "Humanity cost for Existence Erasure"
    })
    @Config.Name("存在抹消消耗 | Existence Erasure Cost")
    @Config.RangeDouble(min = 0, max = 30)
    public static double existenceErasureCost = 10.0;

    // ============================================================
    // 调试选项
    // ============================================================

    @Config.Comment({
            "启用调试信息",
            "Enable debug messages"
    })
    @Config.Name("调试模式 | Debug Mode")
    public static boolean debugMode = false;

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
