package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 破碎终局饰品配置
 * Broken Endgame Relic Configuration
 *
 * 所有数值都可在此调整
 */
@Config(modid = "moremod", name = "moremod/broken_relics", category = "broken_relics")
@Config.LangKey("config.moremod.broken_relics")
public class BrokenRelicConfig {

    // ============================================================
    // 通用设置
    // ============================================================

    @Config.Comment({
            "人性值高于此阈值时，破碎饰品效果减弱",
            "Effects weaken when humanity above this threshold"
    })
    @Config.Name("人性阈值 | Humanity Threshold")
    @Config.RangeDouble(min = 20, max = 80)
    public static double humanityWeakenThreshold = 50.0;

    @Config.Comment({
            "高人性时效果衰减比例 (0.6 = 效果降至60%)",
            "Effect reduction ratio at high humanity"
    })
    @Config.Name("高人性衰减 | High Humanity Reduction")
    @Config.RangeDouble(min = 0.3, max = 1.0)
    public static double highHumanityEffectRatio = 0.6;

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
            "生命汲取比例 (0.8 = 80%伤害转化为治疗)",
            "Life steal ratio"
    })
    @Config.Name("心核:吸血比例 | Heartcore: Lifesteal Ratio")
    @Config.RangeDouble(min = 0.3, max = 1.0)
    public static double heartcoreLifestealRatio = 0.8;

    @Config.Comment({
            "溢出吸血转化为吸收之心的最大值",
            "Maximum absorption hearts from overflow lifesteal"
    })
    @Config.Name("心核:最大吸收 | Heartcore: Max Absorption")
    @Config.RangeDouble(min = 0, max = 20)
    public static double heartcoreMaxAbsorption = 8.0;

    // ============================================================
    // 破碎_眼 (Broken Eye)
    // ============================================================

    @Config.Comment({
            "暴击伤害倍率",
            "Critical damage multiplier"
    })
    @Config.Name("破碎眼:暴击伤害 | Eye: Crit Multiplier")
    @Config.RangeDouble(min = 1.2, max = 3.0)
    public static double eyeCritMultiplier = 1.5;

    @Config.Comment({
            "护甲忽略比例 (0.5 = 忽略50%护甲)",
            "Armor ignore ratio"
    })
    @Config.Name("破碎眼:护甲穿透 | Eye: Armor Penetration")
    @Config.RangeDouble(min = 0.2, max = 0.8)
    public static double eyeArmorIgnore = 0.5;

    @Config.Comment({
            "攻击距离延长（格）",
            "Attack range extension in blocks"
    })
    @Config.Name("破碎眼:距离延长 | Eye: Range Extension")
    @Config.RangeDouble(min = 0.5, max = 3.0)
    public static double eyeRangeExtension = 1.0;

    // ============================================================
    // 破碎_手 (Broken Hand)
    // ============================================================

    @Config.Comment({
            "攻击冷却缩减倍率 (0.5 = 冷却减半)",
            "Attack cooldown reduction multiplier"
    })
    @Config.Name("破碎手:冷却缩减 | Hand: Cooldown Reduction")
    @Config.RangeDouble(min = 0.3, max = 0.8)
    public static double handCooldownReduction = 0.5;

    @Config.Comment({
            "幻象打击追加伤害比例",
            "Phantom strike bonus damage ratio"
    })
    @Config.Name("破碎手:幻象伤害 | Hand: Phantom Damage")
    @Config.RangeDouble(min = 0.2, max = 0.8)
    public static double handPhantomDamageRatio = 0.4;

    // ============================================================
    // 破碎_枷锁 (Broken Shackles)
    // ============================================================

    @Config.Comment({
            "减速光环范围（格）",
            "Slow aura range in blocks"
    })
    @Config.Name("枷锁:光环范围 | Shackles: Aura Range")
    @Config.RangeDouble(min = 4, max = 16)
    public static double shacklesAuraRange = 8.0;

    @Config.Comment({
            "减速等级 (2 = Slowness III)",
            "Slowness amplifier"
    })
    @Config.Name("枷锁:减速等级 | Shackles: Slow Level")
    @Config.RangeInt(min = 0, max = 4)
    public static int shacklesSlowLevel = 2;

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
    @Config.RangeDouble(min = 0.1, max = 0.5)
    public static double shacklesDamageReduction = 0.3;

    // ============================================================
    // 破碎_投影 (Broken Projection)
    // ============================================================

    @Config.Comment({
            "幻象分身伤害比例",
            "Phantom twin damage ratio"
    })
    @Config.Name("投影:分身伤害 | Projection: Twin Damage")
    @Config.RangeDouble(min = 0.3, max = 0.8)
    public static double projectionTwinDamageRatio = 0.5;

    @Config.Comment({
            "斩杀阈值（目标血量百分比）",
            "Execute threshold (target HP percentage)"
    })
    @Config.Name("投影:斩杀阈值 | Projection: Execute Threshold")
    @Config.RangeDouble(min = 0.2, max = 0.5)
    public static double projectionExecuteThreshold = 0.35;

    // ============================================================
    // 破碎_终结 (Broken Terminus)
    // ============================================================

    @Config.Comment({
            "普通伤害放大倍率",
            "Normal damage amplification multiplier"
    })
    @Config.Name("终结:伤害倍率 | Terminus: Damage Multiplier")
    @Config.RangeDouble(min = 1.2, max = 2.0)
    public static double terminusDamageMultiplier = 1.5;

    @Config.Comment({
            "追加真伤比例",
            "Bonus true damage ratio"
    })
    @Config.Name("终结:真伤比例 | Terminus: True Damage Ratio")
    @Config.RangeDouble(min = 0.1, max = 0.5)
    public static double terminusTrueDamageRatio = 0.3;

    @Config.Comment({
            "每次人性消耗间隔（tick）",
            "Humanity drain interval in ticks"
    })
    @Config.Name("终结:消耗间隔 | Terminus: Drain Interval")
    @Config.RangeInt(min = 20, max = 200)
    public static int terminusHumanityDrainInterval = 100;

    @Config.Comment({
            "每次人性消耗量",
            "Humanity drain amount per interval"
    })
    @Config.Name("终结:人性消耗 | Terminus: Humanity Drain")
    @Config.RangeDouble(min = 0.5, max = 5.0)
    public static double terminusHumanityDrainAmount = 1.5;

    @Config.Comment({
            "每秒生命流失",
            "HP bleed per second"
    })
    @Config.Name("终结:生命流失 | Terminus: HP Bleed")
    @Config.RangeDouble(min = 0.1, max = 2.0)
    public static double terminusHPBleedPerSec = 0.5;

    @Config.Comment({
            "人性消耗下限（不会低于此值）",
            "Minimum humanity (won't drain below)"
    })
    @Config.Name("终结:人性下限 | Terminus: Min Humanity")
    @Config.RangeDouble(min = 0, max = 10)
    public static double terminusMinHumanity = 1.0;

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
