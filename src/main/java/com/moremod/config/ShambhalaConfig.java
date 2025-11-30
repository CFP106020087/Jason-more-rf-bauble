package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 香巴拉配置
 * Shambhala Ascension Configuration
 *
 * 永恒齿轮圣化身 - 绝对的盾
 * Avatar of Eternal Gearwork Shambhala - The Absolute Shield
 */
@Config(modid = "moremod", name = "moremod/shambhala", category = "shambhala")
@Config.LangKey("config.moremod.shambhala")
public class ShambhalaConfig {

    // ============================================================
    // 升格条件
    // ============================================================

    @Config.Comment({
            "升格所需的最小人性值阈值",
            "Minimum humanity threshold for ascension"
    })
    @Config.Name("人性阈值 | Humanity Threshold")
    @Config.RangeDouble(min = 80, max = 100)
    public static double ascensionHumanityThreshold = 90.0;

    @Config.Comment({
            "升格所需的最少模块数量",
            "Minimum installed modules required for ascension"
    })
    @Config.Name("模块数量要求 | Required Module Count")
    @Config.RangeInt(min = 1, max = 50)
    public static int requiredModuleCount = 20;

    @Config.Comment({
            "升格所需的高人性值累计时间（游戏日数）",
            "Days at high humanity required for ascension",
            "1游戏日 = 24000 ticks"
    })
    @Config.Name("高人性天数要求 | High Humanity Days")
    @Config.RangeInt(min = 1, max = 30)
    public static int requiredHighHumanityDays = 7;

    @Config.Comment({
            "高人性值阈值（高于此值时开始累计时间）",
            "High humanity threshold (accumulates time when above this)"
    })
    @Config.Name("高人性阈值 | High Humanity Threshold")
    @Config.RangeDouble(min = 70, max = 95)
    public static double highHumanityThreshold = 90.0;

    // ============================================================
    // 核心机制：能量护盾
    // ============================================================

    @Config.Comment({
            "每点伤害消耗的能量（RF）",
            "Energy cost per damage point absorbed"
    })
    @Config.Name("能量消耗/伤害 | Energy Per Damage")
    @Config.RangeInt(min = 100, max = 10000)
    public static int energyPerDamage = 1000;

    @Config.Comment({
            "反伤每点消耗的能量（RF）",
            "Energy cost per damage point reflected"
    })
    @Config.Name("反伤能量消耗 | Reflect Energy Cost")
    @Config.RangeInt(min = 50, max = 5000)
    public static int energyPerReflect = 500;

    @Config.Comment({
            "清除debuff每次消耗的能量（RF）",
            "Energy cost per debuff cleanse"
    })
    @Config.Name("清除debuff能量 | Cleanse Energy Cost")
    @Config.RangeInt(min = 500, max = 10000)
    public static int energyPerCleanse = 2000;

    // ============================================================
    // 代价：伤害削弱
    // ============================================================

    @Config.Comment({
            "造成伤害的削弱比例 (0.5 = 伤害减半)",
            "Damage output reduction (0.5 = halved damage)"
    })
    @Config.Name("伤害削弱 | Damage Reduction")
    @Config.RangeDouble(min = 0.1, max = 0.9)
    public static double damageOutputReduction = 0.5;

    // ============================================================
    // 香巴拉_核心 (Shambhala Core) - 不灭之心
    // ============================================================

    @Config.Comment({
            "最大生命值加成",
            "Maximum health bonus"
    })
    @Config.Name("核心:生命加成 | Core: Health Bonus")
    @Config.RangeDouble(min = 10, max = 100)
    public static double coreHealthBonus = 40.0;

    @Config.Comment({
            "能量不足时的最低血量锁定",
            "Minimum health lock when energy available"
    })
    @Config.Name("核心:血量锁定 | Core: Health Lock")
    @Config.RangeDouble(min = 0.5, max = 5)
    public static double coreHealthLock = 1.0;

    // ============================================================
    // 香巴拉_壁垒 (Shambhala Bastion) - 绝对防御
    // ============================================================

    @Config.Comment({
            "基础伤害减免比例",
            "Base damage reduction percentage"
    })
    @Config.Name("壁垒:伤害减免 | Bastion: Damage Reduction")
    @Config.RangeDouble(min = 0.3, max = 0.9)
    public static double bastionDamageReduction = 0.7;

    @Config.Comment({
            "护甲加成",
            "Armor bonus"
    })
    @Config.Name("壁垒:护甲加成 | Bastion: Armor Bonus")
    @Config.RangeDouble(min = 10, max = 30)
    public static double bastionArmorBonus = 20.0;

    @Config.Comment({
            "韧性加成",
            "Toughness bonus"
    })
    @Config.Name("壁垒:韧性加成 | Bastion: Toughness Bonus")
    @Config.RangeDouble(min = 5, max = 20)
    public static double bastionToughnessBonus = 10.0;

    // ============================================================
    // 香巴拉_棘刺 (Shambhala Thorns) - 因果反噬
    // ============================================================

    @Config.Comment({
            "反伤倍率",
            "Damage reflection multiplier"
    })
    @Config.Name("棘刺:反伤倍率 | Thorns: Reflect Multiplier")
    @Config.RangeDouble(min = 1.0, max = 10.0)
    public static double thornsReflectMultiplier = 3.0;

    @Config.Comment({
            "反伤是否为真实伤害",
            "Whether reflected damage is true damage"
    })
    @Config.Name("棘刺:真实伤害 | Thorns: True Damage")
    public static boolean thornsTrueDamage = true;

    // ============================================================
    // 香巴拉_净化 (Shambhala Purify) - 免疫与净化
    // ============================================================

    @Config.Comment({
            "自动清除负面效果的间隔（tick）",
            "Interval for auto cleansing debuffs"
    })
    @Config.Name("净化:清除间隔 | Purify: Cleanse Interval")
    @Config.RangeInt(min = 10, max = 100)
    public static int purifyCleanseInterval = 20;

    @Config.Comment({
            "是否免疫凋零",
            "Immune to wither effect"
    })
    @Config.Name("净化:凋零免疫 | Purify: Wither Immune")
    public static boolean purifyWitherImmune = true;

    @Config.Comment({
            "是否免疫中毒",
            "Immune to poison effect"
    })
    @Config.Name("净化:中毒免疫 | Purify: Poison Immune")
    public static boolean purifyPoisonImmune = true;

    // ============================================================
    // 香巴拉_隐匿 (Shambhala Veil) - 反侦察
    // ============================================================

    @Config.Comment({
            "怪物侦测距离减少比例",
            "Mob detection range reduction"
    })
    @Config.Name("隐匿:侦测减少 | Veil: Detection Reduction")
    @Config.RangeDouble(min = 0.5, max = 1.0)
    public static double veilDetectionReduction = 0.8;

    @Config.Comment({
            "是否在潜行时完全隐身",
            "Invisible to mobs while sneaking"
    })
    @Config.Name("隐匿:潜行隐身 | Veil: Sneak Invisible")
    public static boolean veilSneakInvisible = true;

    // ============================================================
    // 香巴拉_圣域 (Shambhala Sanctuary) - 终极防线
    // ============================================================

    @Config.Comment({
            "圣域光环范围（格）",
            "Sanctuary aura range in blocks"
    })
    @Config.Name("圣域:光环范围 | Sanctuary: Aura Range")
    @Config.RangeDouble(min = 3, max = 15)
    public static double sanctuaryAuraRange = 8.0;

    @Config.Comment({
            "圣域内友军伤害减免",
            "Damage reduction for allies in sanctuary"
    })
    @Config.Name("圣域:友军减伤 | Sanctuary: Ally Protection")
    @Config.RangeDouble(min = 0.1, max = 0.5)
    public static double sanctuaryAllyProtection = 0.3;

    @Config.Comment({
            "圣域每tick能量消耗",
            "Energy cost per tick for sanctuary aura"
    })
    @Config.Name("圣域:每tick能耗 | Sanctuary: Energy Per Tick")
    @Config.RangeInt(min = 1, max = 100)
    public static int sanctuaryEnergyPerTick = 10;

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
