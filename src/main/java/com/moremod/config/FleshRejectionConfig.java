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

    // ========== 基础参数 ==========
    
    @Config.Comment({
        "排异增长速率（每秒每模块等级）",
        "建议值: 0.01 (10分钟达到100排异需要约16个模块等级)",
        "Growth rate per second per module level"
    })
    @Config.Name("排异增长速率 | Rejection Growth Rate")
    @Config.RangeDouble(min = 0.001, max = 1.0)
    public static double rejectionGrowthRate = 0.01;

    @Config.Comment({
        "最大排异值",
        "Maximum rejection level"
    })
    @Config.Name("最大排异值 | Max Rejection")
    @Config.RangeInt(min = 50, max = 500)
    public static int maxRejection = 100;

    @Config.Comment({
        "每个模块等级提供的适应度",
        "建议值: 15 (需要8个模块等级达到120适应度)",
        "Adaptation points per module level"
    })
    @Config.Name("模块适应度系数 | Adaptation Per Module")
    @Config.RangeDouble(min = 1.0, max = 50.0)
    public static double adaptationPerModule = 15.0;

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

    // ========== 出血系统 ==========

    @Config.Comment({
        "启用出血系统",
        "Enable bleeding system"
    })
    @Config.Name("启用出血 | Enable Bleeding")
    public static boolean enableBleeding = true;

    @Config.Comment({
        "受伤时触发出血的基础概率（排异40时）",
        "Base bleeding chance at 40 rejection"
    })
    @Config.Name("出血基础概率 | Bleeding Base Chance")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double bleedingBaseChance = 0.05;

    @Config.Comment({
        "排异值每增加1点，出血概率增加",
        "Bleeding chance increase per rejection point"
    })
    @Config.Name("出血概率增长率 | Bleeding Chance Growth")
    @Config.RangeDouble(min = 0.0, max = 0.01)
    public static double bleedingChanceGrowth = 0.0025;

    @Config.Comment({
        "出血持续时间（tick）",
        "Bleeding duration in ticks (20 ticks = 1 second)"
    })
    @Config.Name("出血持续时间 | Bleeding Duration")
    @Config.RangeInt(min = 20, max = 600)
    public static int bleedingDuration = 60;

    @Config.Comment({
        "低排异时出血伤害（<80）",
        "Bleeding damage when rejection < 80"
    })
    @Config.Name("出血伤害（低） | Bleeding Damage Low")
    @Config.RangeDouble(min = 0.1, max = 5.0)
    public static double bleedingDamageLow = 0.5;

    @Config.Comment({
        "高排异时出血伤害（>=80）",
        "Bleeding damage when rejection >= 80"
    })
    @Config.Name("出血伤害（高） | Bleeding Damage High")
    @Config.RangeDouble(min = 0.5, max = 10.0)
    public static double bleedingDamageHigh = 1.0;

    // ========== 无敌帧缩短 ==========

    @Config.Comment({
        "启用无敌帧缩短",
        "Enable invulnerability reduction"
    })
    @Config.Name("启用无敌帧缩短 | Enable Invulnerability Reduction")
    public static boolean enableInvulnerabilityReduction = true;

    @Config.Comment({
        "开始缩短无敌帧的最低排异值",
        "Minimum rejection to start reducing invulnerability"
    })
    @Config.Name("无敌帧缩短起始排异值 | Invuln Reduction Start")
    @Config.RangeInt(min = 0, max = 100)
    public static int invulnerabilityReductionStart = 40;

    @Config.Comment({
        "最低无敌帧保留比例（100排异时）",
        "Minimum invulnerability frame ratio at 100 rejection"
    })
    @Config.Name("最低无敌帧比例 | Min Invuln Ratio")
    @Config.RangeDouble(min = 0.1, max = 1.0)
    public static double minInvulnerabilityRatio = 0.3;

    // ========== 攻击失误系统 ==========

    @Config.Comment({
        "启用攻击失误",
        "Enable attack miss"
    })
    @Config.Name("启用攻击失误 | Enable Attack Miss")
    public static boolean enableAttackMiss = true;

    @Config.Comment({
        "开始出现攻击失误的最低排异值",
        "Minimum rejection to start missing attacks"
    })
    @Config.Name("攻击失误起始排异值 | Attack Miss Start")
    @Config.RangeInt(min = 0, max = 100)
    public static int attackMissStart = 40;

    @Config.Comment({
        "最大攻击失误概率（100排异时）",
        "Maximum attack miss chance at 100 rejection"
    })
    @Config.Name("最大失误概率 | Max Miss Chance")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double maxMissChance = 0.15;

    // ========== 攻击自伤系统 ==========

    @Config.Comment({
        "启用攻击自伤（高排异时攻击失误可能反伤自己）",
        "Enable attack self-damage"
    })
    @Config.Name("启用攻击自伤 | Enable Attack Self Damage")
    public static boolean enableAttackSelfDamage = true;

    @Config.Comment({
        "触发自伤的最低排异值",
        "Minimum rejection to trigger self damage"
    })
    @Config.Name("自伤起始排异值 | Self Damage Start")
    @Config.RangeInt(min = 0, max = 100)
    public static int selfDamageStart = 80;

    @Config.Comment({
        "攻击失误时触发自伤的概率",
        "Chance to trigger self damage on miss"
    })
    @Config.Name("自伤触发概率 | Self Damage Chance")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double selfDamageChance = 0.1;

    @Config.Comment({
        "自伤伤害值",
        "Self damage amount"
    })
    @Config.Name("自伤伤害 | Self Damage Amount")
    @Config.RangeDouble(min = 0.5, max = 10.0)
    public static double selfDamageAmount = 2.0;

    // ========== 死亡惩罚 ==========

    @Config.Comment({
        "死亡时排异值保留比例（0.5 = 保留50%）",
        "Rejection retention ratio on death (0.5 = keep 50%)"
    })
    @Config.Name("死亡排异值保留 | Death Rejection Retention")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double deathRejectionRetention = 0.5;

    @Config.Comment({
        "死亡时是否保留突破状态",
        "Keep transcendence status on death"
    })
    @Config.Name("死亡保留突破 | Keep Transcendence On Death")
    public static boolean keepTranscendenceOnDeath = true;

    @Config.Comment({
        "死亡时最低排异值（即使减半也不会低于此值）",
        "Minimum rejection after death"
    })
    @Config.Name("死亡最低排异值 | Min Rejection After Death")
    @Config.RangeDouble(min = 0.0, max = 50.0)
    public static double minRejectionAfterDeath = 0.0;

    // ========== 稳定剂配置 ==========

    @Config.Comment({
        "稳定剂降低的排异值",
        "Rejection reduction by stabilizer"
    })
    @Config.Name("稳定剂效果 | Stabilizer Reduction")
    @Config.RangeDouble(min = 10.0, max = 100.0)
    public static double stabilizerReduction = 30.0;

    @Config.Comment({
        "稳定剂冷却时间（tick）",
        "Stabilizer cooldown in ticks (20 ticks = 1 second)"
    })
    @Config.Name("稳定剂冷却 | Stabilizer Cooldown")
    @Config.RangeInt(min = 100, max = 6000)
    public static int stabilizerCooldown = 600;

    @Config.Comment({
        "使用稳定剂的最低排异值要求",
        "Minimum rejection to use stabilizer"
    })
    @Config.Name("稳定剂使用门槛 | Stabilizer Min Rejection")
    @Config.RangeDouble(min = 0.0, max = 50.0)
    public static double stabilizerMinRejection = 10.0;

    // ========== 调试选项 ==========

    @Config.Comment({
        "启用调试信息输出到聊天框",
        "Enable debug messages in chat"
    })
    @Config.Name("调试模式 | Debug Mode")
    public static boolean debugMode = false;

    @Config.Comment({
        "在HUD显示详细的调试信息",
        "Show detailed debug info in HUD"
    })
    @Config.Name("HUD调试信息 | HUD Debug Info")
    public static boolean hudDebugInfo = false;

    // 配置变更监听
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