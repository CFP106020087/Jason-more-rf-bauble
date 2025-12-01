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
    @Config.RangeInt(min = 1, max = 60)
    public static int requiredModuleCount = 40;

    @Config.Comment({
            "升格所需的高人性值累计时间（秒）",
            "Seconds at high humanity required for ascension",
            "默认7天 = 604800秒"
    })
    @Config.Name("高人性秒数要求 | High Humanity Seconds")
    @Config.RangeInt(min = 60, max = 604800)
    public static int requiredHighHumanitySeconds = 604800; // 7天

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
            "反伤倍率（香巴拉攻击力下降，所以反伤倍率较高）",
            "Damage reflection multiplier (higher due to reduced attack)"
    })
    @Config.Name("棘刺:反伤倍率 | Thorns: Reflect Multiplier")
    @Config.RangeDouble(min = 1.0, max = 20.0)
    public static double thornsReflectMultiplier = 5.0;

    @Config.Comment({
            "反伤是否为真实伤害（使用包装后的setHealth）",
            "Whether reflected damage is true damage (wrapped setHealth)"
    })
    @Config.Name("棘刺:真实伤害 | Thorns: True Damage")
    public static boolean thornsTrueDamage = true;

    @Config.Comment({
            "范围反伤半径（0=仅攻击者，>0=以攻击者为中心的范围）",
            "AoE reflect radius (0=attacker only, >0=radius around attacker)"
    })
    @Config.Name("棘刺:范围半径 | Thorns: AoE Radius")
    @Config.RangeDouble(min = 0, max = 10)
    public static double thornsAoeRadius = 3.0;

    // ============================================================
    // 香巴拉_净化 (Shambhala Purify) - 被动免疫负面效果
    // ============================================================

    @Config.Comment({
            "被动免疫负面效果的每秒能量消耗（RF/秒）",
            "Energy cost per second for passive negative effect immunity",
            "只要有足够能量，就会完全免疫所有负面效果（包括模组效果）"
    })
    @Config.Name("净化:每秒能耗 | Purify: Energy Per Second")
    @Config.RangeInt(min = 100, max = 10000)
    public static int purifyEnergyPerSecond = 500;

    @Config.Comment({
            "是否免疫所有负面效果（包括模组添加的）",
            "Immune to ALL negative effects (including mod effects)",
            "如果关闭，只免疫原版负面效果"
    })
    @Config.Name("净化:免疫全部 | Purify: Immune All")
    public static boolean purifyImmuneAll = true;

    // ============================================================
    // 香巴拉_宁静 (Shambhala Veil) - 宁静光环 / 仇恨消除
    // ============================================================

    @Config.Comment({
            "仇恨消除技能的能量消耗",
            "Energy cost for aggro cancel skill"
    })
    @Config.Name("宁静:技能能耗 | Veil: Skill Energy Cost")
    @Config.RangeInt(min = 10000, max = 500000)
    public static int veilSkillEnergyCost = 50000;

    @Config.Comment({
            "仇恨消除技能的范围（格）",
            "Range for aggro cancel skill"
    })
    @Config.Name("宁静:技能范围 | Veil: Skill Range")
    @Config.RangeDouble(min = 5, max = 50)
    public static double veilSkillRange = 16.0;

    @Config.Comment({
            "仇恨消除技能的冷却时间（tick）",
            "Cooldown for aggro cancel skill in ticks"
    })
    @Config.Name("宁静:技能冷却 | Veil: Skill Cooldown")
    @Config.RangeInt(min = 20, max = 1200)
    public static int veilSkillCooldown = 200;

    @Config.Comment({
            "宁静光环持续时间（tick）",
            "Duration of peace aura effect in ticks",
            "进入光环范围的生物会持续被清除仇恨"
    })
    @Config.Name("宁静:光环持续时间 | Veil: Aura Duration")
    @Config.RangeInt(min = 20, max = 600)
    public static int veilAuraDuration = 200; // 默认10秒

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

    @Config.Comment({
            "圣域恢复饥饿/口渴的间隔（tick）",
            "Interval for hunger/thirst restoration in sanctuary (ticks)",
            "默认60tick = 3秒"
    })
    @Config.Name("圣域:恢复间隔 | Sanctuary: Restoration Interval")
    @Config.RangeInt(min = 20, max = 200)
    public static int sanctuaryRestorationInterval = 60;

    @Config.Comment({
            "圣域每次恢复的饥饿值",
            "Hunger restored per interval in sanctuary"
    })
    @Config.Name("圣域:饥饿恢复量 | Sanctuary: Hunger Restoration")
    @Config.RangeInt(min = 1, max = 10)
    public static int sanctuaryHungerRestoration = 2;

    @Config.Comment({
            "圣域每次恢复的口渴值",
            "Thirst restored per interval in sanctuary"
    })
    @Config.Name("圣域:口渴恢复量 | Sanctuary: Thirst Restoration")
    @Config.RangeInt(min = 1, max = 10)
    public static int sanctuaryThirstRestoration = 2;

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
