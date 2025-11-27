package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 故障系统配置
 */
@Config(modid = "moremod", name = "moremod/malfunction_system")
@Config.LangKey("config.moremod.malfunction")
public class MalfunctionConfig {

    @Config.Comment("故障效果设置")
    @Config.LangKey("config.moremod.malfunction.effects")
    public static EffectSettings effects = new EffectSettings();

    @Config.Comment("能量消耗设置")
    @Config.LangKey("config.moremod.malfunction.energy")
    public static EnergySettings energy = new EnergySettings();

    @Config.Comment("伤害设置")
    @Config.LangKey("config.moremod.malfunction.damage")
    public static DamageSettings damage = new DamageSettings();

    @Config.Comment("环境判定设置（雨天免疫 / 坐船豁免 等）")
    @Config.LangKey("config.moremod.malfunction.environment")
    public static EnvironmentSettings environment = new EnvironmentSettings();

    public static class EffectSettings {

        @Config.Comment({
                "允许故障系统施加的药水效果白名单",
                "格式: modid:potion_name 或 minecraft:potion_name",
                "例如: minecraft:slowness, minecraft:mining_fatigue",
                "留空则使用默认列表"
        })
        @Config.LangKey("config.moremod.malfunction.effects.whitelist")
        public String[] potionWhitelist = new String[] {
                "minecraft:slowness",
                "minecraft:mining_fatigue",
                "minecraft:nausea",
                "minecraft:blindness",
                "minecraft:hunger",
                "minecraft:weakness",
                "minecraft:poison",
                "minecraft:wither"
        };

        @Config.Comment({
                "每个药水效果的最大等级限制",
                "格式: modid:potion_name:max_level",
                "例如: minecraft:poison:1 表示中毒最高1级",
                "未指定的效果使用故障等级作为上限"
        })
        @Config.LangKey("config.moremod.malfunction.effects.maxLevels")
        public String[] potionMaxLevels = new String[] {
                "minecraft:poison:1",
                "minecraft:wither:1",
                "minecraft:nausea:0",
                "minecraft:blindness:0"
        };

        @Config.Comment({
                "每个药水效果的持续时间（秒）",
                "格式: modid:potion_name:min_duration:max_duration",
                "例如: minecraft:slowness:3:10",
                "未指定的效果使用默认时间（3-8秒）"
        })
        @Config.LangKey("config.moremod.malfunction.effects.durations")
        public String[] potionDurations = new String[] {
                "minecraft:blindness:1:3",
                "minecraft:nausea:2:4",
                "minecraft:poison:3:5",
                "minecraft:wither:2:4",
                "minecraft:slowness:5:10",
                "minecraft:mining_fatigue:5:10",
                "minecraft:weakness:5:10",
                "minecraft:hunger:5:10"
        };

        @Config.Comment("同屏最多同时存在的负面效果数量")
        @Config.LangKey("config.moremod.malfunction.effects.maxSimultaneous")
        @Config.RangeInt(min = 1, max = 5)
        public int maxSimultaneousEffects = 3;

        @Config.Comment("负面效果触发间隔（秒）")
        @Config.LangKey("config.moremod.malfunction.effects.interval")
        @Config.RangeInt(min = 1, max = 60)
        public int effectInterval = 3;
    }

    public static class EnergySettings {

        @Config.Comment("基础能量消耗速率（RF/秒）")
        @Config.LangKey("config.moremod.malfunction.energy.baseRate")
        @Config.RangeInt(min = 10, max = 1000)
        public int baseEnergyDrainRate = 100;

        @Config.Comment("每级故障增加的能量消耗百分比（×）")
        @Config.LangKey("config.moremod.malfunction.energy.levelMultiplier")
        @Config.RangeDouble(min = 0.1, max = 5.0)
        public double energyDrainMultiplier = 0.5;

        @Config.Comment("雨天故障的能量消耗倍率（×）")
        @Config.LangKey("config.moremod.malfunction.energy.rainMultiplier")
        @Config.RangeDouble(min = 0.1, max = 2.0)
        public double rainDrainMultiplier = 0.5;

        @Config.Comment("完全浸水时的能量消耗倍率（×）")
        @Config.LangKey("config.moremod.malfunction.energy.underwaterMultiplier")
        @Config.RangeDouble(min = 1.0, max = 5.0)
        public double underwaterDrainMultiplier = 2.0;
    }

    public static class DamageSettings {

        @Config.Comment("能量耗尽后是否对玩家造成伤害")
        @Config.LangKey("config.moremod.malfunction.damage.enabled")
        public boolean damageEnabled = true;

        @Config.Comment("基础伤害值（半心）")
        @Config.LangKey("config.moremod.malfunction.damage.baseAmount")
        @Config.RangeDouble(min = 0.5, max = 10.0)
        public double baseDamage = 1.0;

        @Config.Comment("每级故障额外伤害（半心）")
        @Config.LangKey("config.moremod.malfunction.damage.levelBonus")
        @Config.RangeDouble(min = 0.0, max = 5.0)
        public double damagePerLevel = 1.0;

        @Config.Comment("伤害间隔（秒）")
        @Config.LangKey("config.moremod.malfunction.damage.interval")
        @Config.RangeInt(min = 1, max = 10)
        public int damageInterval = 2;

        @Config.Comment("伤害是否无视护甲")
        @Config.LangKey("config.moremod.malfunction.damage.bypassArmor")
        public boolean bypassArmor = true;
    }

    /** 环境判定：雨天免疫/坐船豁免 */
    public static class EnvironmentSettings {

        @Config.Comment("在雨/雷雨天气下是否视为‘不进水’（免疫雨天）")
        @Config.LangKey("config.moremod.malfunction.environment.ignoreRain")
        public boolean ignoreRain = true;

        @Config.Comment("乘坐船只时是否一律视为‘不在水中’（允许搭船）")
        @Config.LangKey("config.moremod.malfunction.environment.allowBoats")
        public boolean allowBoats = true;
    }

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
