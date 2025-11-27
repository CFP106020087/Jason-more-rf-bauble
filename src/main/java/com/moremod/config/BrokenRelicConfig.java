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
            "暴击伤害倍率",
            "Critical damage multiplier"
    })
    @Config.Name("破碎臂:暴击伤害 | Arm: Crit Multiplier")
    @Config.RangeDouble(min = 1.5, max = 5.0)
    public static double armCritMultiplier = 3.0;

    @Config.Comment({
            "护甲穿透比例 (1.0 = 100%无视护甲)",
            "Armor penetration ratio"
    })
    @Config.Name("破碎臂:护甲穿透 | Arm: Armor Penetration")
    @Config.RangeDouble(min = 0.5, max = 1.0)
    public static double armArmorPenetration = 1.0;

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
    @Config.RangeDouble(min = 1.5, max = 3.0)
    public static double terminusDamageMultiplier = 2.0;

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
