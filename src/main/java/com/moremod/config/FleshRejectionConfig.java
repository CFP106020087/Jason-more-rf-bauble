package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = "moremod", name = "moremod/flesh_rejection", category = "flesh_rejection")
@Config.LangKey("config.moremod.flesh_rejection")
public class FleshRejectionConfig {

    @Config.Comment({
            "血肉排异系统配置",
            "Flesh Rejection System Configuration"
    })
    @Config.Name("排异系统总开关 | Rejection System Master Switch")
    public static boolean enableRejectionSystem = true;

    // ============================================================
    // 基础参数（平衡調整）
    // ============================================================

    @Config.Comment({
            "排异增长速率（每秒每模块等级）",
            "建议值: 0.006 (10模块約28分钟达到100排异)",
            "Growth rate per second per module level"
    })
    @Config.Name("排异增长速率 | Rejection Growth Rate")
    @Config.RangeDouble(min = 0.001, max = 1.0)
    public static double rejectionGrowthRate = 0.006;

    @Config.Comment({
            "最大排异值",
            "Maximum rejection level"
    })
    @Config.Name("最大排异值 | Max Rejection")
    @Config.RangeInt(min = 50, max = 500)
    public static int maxRejection = 100;

    @Config.Comment({
            "每个模块等级提供的适应度",
            "当前值: 10 (需要12个激活模块等级达到120适应度)",
            "Adaptation points per module level"
    })
    @Config.Name("模块适应度系数 | Adaptation Per Module")
    @Config.RangeDouble(min = 1.0, max = 50.0)
    public static double adaptationPerModule = 10.0;

    @Config.Comment({
            "突破所需适应度阈值",
            "建议值: 120",
            "Adaptation threshold for transcendence"
    })
    @Config.Name("突破阈值 | Transcendence Threshold")
    @Config.RangeInt(min = 50, max = 500)
    public static int adaptationThreshold = 120;

    @Config.Comment({
            "神经同步器提供的额外适应度",
            "Neural Synchronizer bonus adaptation"
    })
    @Config.Name("神经同步器加成 | Neural Synchronizer Bonus")
    @Config.RangeInt(min = 0, max = 200)
    public static int neuralSynchronizerBonus = 100;

    // ============================================================
    // 睡眠 / 休息（加強恢復）
    // ============================================================

    @Config.Comment({
            "啟用睡眠/休息自然下降排異",
            "Enable sleep-based rejection decay"
    })
    @Config.Name("啟用睡眠排異下降 | Enable Sleep Decay")
    public static boolean enableSleepDecay = true;

    @Config.Comment({
            "玩家正在睡覺時，每秒下降的排異值",
            "Rejection decay per second while sleeping"
    })
    @Config.Name("睡眠下降速率 | Sleep Decay Rate")
    @Config.RangeDouble(min = 0.01, max = 10.0)
    public static double sleepDecayRate = 1.0;

    @Config.Comment({
            "玩家睡醒後是否繼續自然下降排異",
            "Continue rejection decay after waking up?"
    })
    @Config.Name("睡醒後持續下降 | Continue Decay After Wake")
    public static boolean continueDecayAfterWake = true;

    @Config.Comment({
            "玩家睡醒後持續下降排異的秒數",
            "Duration of post-sleep decay in seconds"
    })
    @Config.Name("睡醒後持續下降秒數 | Post-Sleep Decay Duration")
    @Config.RangeInt(min = 1, max = 600)
    public static int postSleepDecayDuration = 120;

    @Config.Comment({
            "玩家睡醒後每秒額外下降排異值",
            "Rejection decay per second after waking up"
    })
    @Config.Name("睡醒後下降速率 | Post-Sleep Decay Rate")
    @Config.RangeDouble(min = 0.01, max = 5.0)
    public static double postSleepDecayRate = 0.5;

    @Config.Comment({
            "睡眠恢復持續時間（tick）",
            "Sleep recovery duration in ticks"
    })
    @Config.Name("睡眠恢復持續時間 | Sleep Recovery Duration")
    @Config.RangeInt(min = 20, max = 72000)
    public static int sleepRecoveryDuration = 1200;

    @Config.Comment({
            "睡眠恢復每秒降低的排異值",
            "Rejection reduction per second during recovery"
    })
    @Config.Name("睡眠恢復每秒降低 | Sleep Recovery Per Second")
    @Config.RangeDouble(min = 0.01, max = 10.0)
    public static float sleepRecoveryPerSecond = 0.8F;

    // ============================================================
    // 飢餓 / 口渴 / 熬夜（減弱影響）
    // ============================================================

    @Config.Comment({
            "啟用狀態排異加速",
            "Enable condition-based rejection"
    })
    @Config.Name("啟用狀態排異加速 | Enable Condition Rejection")
    public static boolean enableConditionRejection = true;

    @Config.Comment({
            "飢餓→排異公式中的指數",
            "Exponent for hunger → rejection formula"
    })
    @Config.Name("飢餓曲線指數 | Hunger Curve Exponent")
    @Config.RangeDouble(min = 1.0, max = 3.0)
    public static double hungerCurve = 1.3;

    @Config.Comment({
            "飢餓→排異的係數",
            "Multiplier for hunger → rejection"
    })
    @Config.Name("飢餓排異係數 | Hunger Rejection Factor")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double hungerRejectionFactor = 0.008;

    @Config.Comment({
            "脫水→排異的係數",
            "Multiplier for thirst → rejection"
    })
    @Config.Name("口渴排異係數 | Thirst Rejection Factor")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double thirstRejectionFactor = 0.01;

    // 舊版本兼容
    @Config.Name("舊_飢餓排異增長 | Legacy Hunger Rejection Gain")
    @Config.RangeDouble(min = 0.0, max = 5.0)
    public static double hungerRejectionGain = 0.2;

    @Config.Name("舊_口渴排異增長 | Legacy Thirst Rejection Gain")
    @Config.RangeDouble(min = 0.0, max = 5.0)
    public static double thirstRejectionGain = 0.25;

    @Config.Name("舊_熬夜排異增長 | Legacy Insomnia Rejection Gain")
    @Config.RangeDouble(min = 0.0, max = 5.0)
    public static double insomniaRejectionGain = 0.3;

    @Config.Comment({
            "失眠階段1天數",
            "No-sleep days for insomnia stage 1"
    })
    @Config.Name("失眠階段1天數 | Insomnia Stage1 Days")
    @Config.RangeInt(min = 0, max = 30)
    public static int insomniaStage1Days = 2;

    @Config.Name("失眠階段2天數 | Insomnia Stage2 Days")
    @Config.RangeInt(min = 0, max = 30)
    public static int insomniaStage2Days = 3;

    @Config.Name("失眠階段3天數 | Insomnia Stage3 Days")
    @Config.RangeInt(min = 0, max = 30)
    public static int insomniaStage3Days = 5;

    @Config.Comment({
            "失眠階段1排異加成",
            "Insomnia stage 1 rejection bonus"
    })
    @Config.Name("失眠階段1加成 | Insomnia Stage1 Boost")
    @Config.RangeDouble(min = 0.0, max = 2.0)
    public static float insomniaStage1Boost = 0.05F;

    @Config.Name("失眠階段2加成 | Insomnia Stage2 Boost")
    @Config.RangeDouble(min = 0.0, max = 3.0)
    public static float insomniaStage2Boost = 0.15F;

    @Config.Name("失眠階段3加成 | Insomnia Stage3 Boost")
    @Config.RangeDouble(min = 0.0, max = 5.0)
    public static float insomniaStage3Boost = 0.25F;

    // ============================================================
    // 藥水限制（漸進式）
    // ============================================================

    @Config.Comment({
            "啟用正面藥水限制",
            "Enable positive potion rejection"
    })
    @Config.Name("啟用藥水排異 | Enable Potion Rejection")
    public static boolean enablePotionRejection = true;

    @Config.Comment({
            "藥水排異係數",
            "Multiplier for positive potion → rejection"
    })
    @Config.Name("藥水排異係數 | Potion Rejection Factor")
    @Config.RangeDouble(min = 0.0, max = 5.0)
    public static double potionRejectionFactor = 0.2;

    @Config.Comment({
            "開始限制藥水的排異值",
            "Rejection level to start potion limiting"
    })
    @Config.Name("藥水壓制起始排異 | Potion Limit Start")
    @Config.RangeInt(min = 0, max = 100)
    public static int potionLimitStart = 40;

    @Config.Comment({
            "零排異最大藥水數量",
            "Max active potion effects at 0 rejection"
    })
    @Config.Name("零排異最大藥水數量 | Potion Max At Zero")
    @Config.RangeInt(min = 0, max = 32)
    public static int potionMaxAtZero = 8;

    // 舊版本兼容
    @Config.Name("舊_正面藥水最大值 | Legacy Max Positive Effects")
    @Config.RangeInt(min = 0, max = 20)
    public static int maxPositiveEffects = 8;

    @Config.Name("舊_藥水壓制起始排異 | Legacy Potion Suppression Start")
    @Config.RangeInt(min = 0, max = 100)
    public static int potionSuppressionStart = 40;

    @Config.Name("舊_藥水壓制最小值 | Legacy Potion Suppression End Value")
    @Config.RangeInt(min = 0, max = 5)
    public static int potionSuppressionEndValue = 0;

    @Config.Name("舊_藥水排異增長 | Legacy Potion Rejection Gain")
    @Config.RangeDouble(min = 0.01, max = 5.0)
    public static double potionRejectionGain = 0.3;

    // ============================================================
    // 出血系統
    // ============================================================

    @Config.Comment({
            "啟用出血系統",
            "Enable bleeding system"
    })
    @Config.Name("啟用出血 | Enable Bleeding")
    public static boolean enableBleeding = true;

    @Config.Comment({
            "出血基礎概率",
            "Base bleeding chance"
    })
    @Config.Name("出血基礎概率 | Bleeding Base Chance")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double bleedingBaseChance = 0.03;

    @Config.Comment({
            "出血概率增長率",
            "Bleeding chance growth per rejection point"
    })
    @Config.Name("出血概率增長率 | Bleeding Chance Growth")
    @Config.RangeDouble(min = 0.0, max = 0.01)
    public static double bleedingChanceGrowth = 0.0025;

    @Config.Comment({
            "出血持續時間（tick）",
            "Bleeding duration in ticks"
    })
    @Config.Name("出血持續時間 | Bleeding Duration")
    @Config.RangeInt(min = 20, max = 600)
    public static int bleedingDuration = 40;

    @Config.Comment({
            "低排異時出血傷害",
            "Bleeding damage when rejection < 80"
    })
    @Config.Name("出血傷害（低） | Bleeding Damage Low")
    @Config.RangeDouble(min = 0.1, max = 5.0)
    public static double bleedingDamageLow = 0.3;

    @Config.Comment({
            "高排異時出血傷害",
            "Bleeding damage when rejection >= 80"
    })
    @Config.Name("出血傷害（高） | Bleeding Damage High")
    @Config.RangeDouble(min = 0.5, max = 10.0)
    public static double bleedingDamageHigh = 0.7;

    // ============================================================
    // 無敵幀縮短
    // ============================================================

    @Config.Comment({
            "啟用無敵幀縮短",
            "Enable invulnerability reduction"
    })
    @Config.Name("啟用無敵幀縮短 | Enable Invulnerability Reduction")
    public static boolean enableInvulnerabilityReduction = true;

    @Config.Comment({
            "開始縮短無敵幀的排異值",
            "Minimum rejection for invuln reduction"
    })
    @Config.Name("無敵幀縮短起始排異值 | Invuln Reduction Start")
    @Config.RangeInt(min = 0, max = 100)
    public static int invulnerabilityReductionStart = 50;

    @Config.Comment({
            "最低無敵幀保留比例",
            "Minimum invulnerability frame ratio"
    })
    @Config.Name("最低無敵幀比例 | Min Invuln Ratio")
    @Config.RangeDouble(min = 0.1, max = 1.0)
    public static double minInvulnerabilityRatio = 0.5;

    // ============================================================
    // 攻擊失誤系統
    // ============================================================

    @Config.Comment({
            "啟用攻擊失誤",
            "Enable attack miss"
    })
    @Config.Name("啟用攻擊失誤 | Enable Attack Miss")
    public static boolean enableAttackMiss = true;

    @Config.Comment({
            "開始出現攻擊失誤的排異值",
            "Minimum rejection for attack miss"
    })
    @Config.Name("攻擊失誤起始排異值 | Attack Miss Start")
    @Config.RangeInt(min = 0, max = 100)
    public static int attackMissStart = 50;

    @Config.Comment({
            "最大攻擊失誤概率",
            "Maximum attack miss chance"
    })
    @Config.Name("最大失誤概率 | Max Miss Chance")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double maxMissChance = 0.12;

    // ============================================================
    // 攻擊自傷系統
    // ============================================================

    @Config.Comment({
            "啟用攻擊自傷",
            "Enable attack self-damage"
    })
    @Config.Name("啟用攻擊自傷 | Enable Attack Self Damage")
    public static boolean enableAttackSelfDamage = true;

    @Config.Comment({
            "觸發自傷的排異值",
            "Minimum rejection for self damage"
    })
    @Config.Name("自傷起始排異值 | Self Damage Start")
    @Config.RangeInt(min = 0, max = 100)
    public static int selfDamageStart = 85;

    @Config.Comment({
            "自傷觸發概率",
            "Self damage chance"
    })
    @Config.Name("自傷觸發概率 | Self Damage Chance")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double selfDamageChance = 0.08;

    @Config.Comment({
            "自傷傷害值",
            "Self damage amount"
    })
    @Config.Name("自傷傷害 | Self Damage Amount")
    @Config.RangeDouble(min = 0.5, max = 10.0)
    public static double selfDamageAmount = 1.5;

    // ============================================================
    // 死亡懲罰
    // ============================================================

    @Config.Comment({
            "死亡時排異值保留比例",
            "Rejection retention ratio on death"
    })
    @Config.Name("死亡排異值保留 | Death Rejection Retention")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double deathRejectionRetention = 0.3;

    @Config.Comment({
            "死亡時是否保留突破狀態",
            "Keep transcendence status on death"
    })
    @Config.Name("死亡保留突破 | Keep Transcendence On Death")
    public static boolean keepTranscendenceOnDeath = true;

    @Config.Comment({
            "死亡時最低排異值",
            "Minimum rejection after death"
    })
    @Config.Name("死亡最低排異值 | Min Rejection After Death")
    @Config.RangeDouble(min = 0.0, max = 50.0)
    public static double minRejectionAfterDeath = 0.0;

    // ============================================================
    // 穩定劑配置
    // ============================================================

    @Config.Comment({
            "穩定劑降低的排異值",
            "Rejection reduction by stabilizer"
    })
    @Config.Name("穩定劑效果 | Stabilizer Reduction")
    @Config.RangeDouble(min = 10.0, max = 100.0)
    public static double stabilizerReduction = 40.0;

    @Config.Comment({
            "穩定劑冷卻時間（tick）",
            "Stabilizer cooldown in ticks"
    })
    @Config.Name("穩定劑冷卻 | Stabilizer Cooldown")
    @Config.RangeInt(min = 100, max = 6000)
    public static int stabilizerCooldown = 400;

    @Config.Comment({
            "使用穩定劑的最低排異值",
            "Minimum rejection to use stabilizer"
    })
    @Config.Name("穩定劑使用門檻 | Stabilizer Min Rejection")
    @Config.RangeDouble(min = 0.0, max = 50.0)
    public static double stabilizerMinRejection = 5.0;

    // ============================================================
    // 調試選項
    // ============================================================

    @Config.Comment({
            "啟用調試信息",
            "Enable debug messages"
    })
    @Config.Name("調試模式 | Debug Mode")
    public static boolean debugMode = false;

    @Config.Comment({
            "HUD顯示調試信息",
            "Show debug info in HUD"
    })
    @Config.Name("HUD調試信息 | HUD Debug Info")
    public static boolean hudDebugInfo = false;

    // ============================================================
    // HUD提示配置
    // ============================================================

    @Config.Comment("启用HUD提示系统")
    @Config.Name("啟用HUD提示 | Enable HUD Hints")
    public static boolean enableHUD = true;

    @Config.Comment("启用新手引导提示（首次触发机制时显示）")
    @Config.Name("啟用引導 | Enable Guides")
    public static boolean enableGuides = true;

    @Config.Comment("显示排异值变化提示")
    @Config.Name("顯示排異變化 | Show Rejection Changes")
    public static boolean showRejectionChanges = true;

    @Config.Comment("显示状态效果提示（失误、出血等）")
    @Config.Name("顯示狀態效果 | Show Status Effects")
    public static boolean showStatusEffects = true;

    @Config.Comment("显示恢复提示（睡眠、急救等）")
    @Config.Name("顯示恢復提示 | Show Recovery Hints")
    public static boolean showRecoveryHints = true;

    @Config.Comment("显示警告提示（高排异警告）")
    @Config.Name("顯示警告 | Show Warnings")
    public static boolean showWarnings = true;

    @Config.Comment("显示里程碑提示（适应度进度）")
    @Config.Name("顯示里程碑 | Show Milestones")
    public static boolean showMilestones = true;

    @Config.Comment("最小显示排异变化值（低于此值不显示）")
    @Config.Name("最小變化顯示值 | Min Change To Show")
    @Config.RangeDouble(min = 0.1, max = 5.0)
    public static double minChangeToShow = 0.5;

    @Config.Comment("HUD消息显示时间（毫秒）")
    @Config.Name("HUD消息時長 | HUD Message Duration")
    @Config.RangeInt(min = 500, max = 5000)
    public static int hudMessageDuration = 1500;

    @Config.Comment("HUD消息冷却时间（毫秒，防止刷屏）")
    @Config.Name("HUD消息冷卻 | HUD Cooldown")
    @Config.RangeInt(min = 500, max = 5000)
    public static int hudCooldown = 2000;

    @Config.Comment("HUD位置（0=右中，1=右上，2=右下，3=左中）")
    @Config.Name("HUD位置 | HUD Position")
    @Config.RangeInt(min = 0, max = 3)
    public static int hudPosition = 0;

    @Config.Comment("详细模式（显示更多技术细节）")
    @Config.Name("詳細模式 | Verbose Mode")
    public static boolean verboseMode = false;

    // ============================================================
    // 配置變更監聽
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