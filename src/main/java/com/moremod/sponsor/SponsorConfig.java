package com.moremod.sponsor;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 赞助者物品配置
 * 可以通过主开关完全禁用所有赞助者物品
 */
@Config(modid = "moremod", name = "moremod/sponsor_items")
@Config.LangKey("config.moremod.sponsor.title")
public class SponsorConfig {

    @Config.Comment({
        "赞助者物品系统主开关",
        "设为 false 将禁用所有赞助者武器/盔甲/饰品/真伤附魔",
        "需要重启游戏才能生效"
    })
    @Config.RequiresMcRestart
    public static boolean enabled = false;

    @Config.Comment("赞助者物品设置")
    @Config.LangKey("config.moremod.sponsor.items")
    public static final SponsorItemSettings items = new SponsorItemSettings();

    @Config.Comment("赞助者武器设置")
    @Config.LangKey("config.moremod.sponsor.weapons")
    public static final SponsorWeaponSettings weapons = new SponsorWeaponSettings();

    @Config.Comment("赞助者盔甲设置")
    @Config.LangKey("config.moremod.sponsor.armor")
    public static final SponsorArmorSettings armor = new SponsorArmorSettings();

    @Config.Comment("赞助者饰品设置")
    @Config.LangKey("config.moremod.sponsor.baubles")
    public static final SponsorBaubleSettings baubles = new SponsorBaubleSettings();

    public static class SponsorItemSettings {
        @Config.Comment("是否在创造模式标签页显示赞助者物品")
        public boolean showInCreativeTab = true;

        @Config.Comment("是否允许赞助者物品被附魔")
        public boolean allowEnchanting = true;

        @Config.Comment("赞助者物品是否发光")
        public boolean hasGlowEffect = true;
    }

    public static class SponsorWeaponSettings {
        @Config.Comment("启用赞助者武器")
        public boolean enabled = true;

        @Config.Comment("赞助者武器基础攻击力加成")
        @Config.RangeDouble(min = 0.0, max = 100.0)
        public double baseDamageBonus = 5.0;

        @Config.Comment("赞助者武器攻击速度")
        @Config.RangeDouble(min = 0.1, max = 4.0)
        public double attackSpeed = 1.6;
    }

    public static class SponsorArmorSettings {
        @Config.Comment("启用赞助者盔甲")
        public boolean enabled = true;

        @Config.Comment("赞助者盔甲基础护甲值加成")
        @Config.RangeInt(min = 0, max = 50)
        public int baseArmorBonus = 3;

        @Config.Comment("赞助者盔甲韧性")
        @Config.RangeDouble(min = 0.0, max = 10.0)
        public double toughness = 2.0;
    }

    public static class SponsorBaubleSettings {
        @Config.Comment("启用赞助者饰品")
        public boolean enabled = true;

        @Config.Comment("赞助者饰品被动效果间隔(tick)")
        @Config.RangeInt(min = 1, max = 200)
        public int effectInterval = 20;
    }

    /**
     * 检查赞助者系统是否完全启用
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * 检查武器是否启用
     */
    public static boolean isWeaponsEnabled() {
        return enabled && weapons.enabled;
    }

    /**
     * 检查盔甲是否启用
     */
    public static boolean isArmorEnabled() {
        return enabled && armor.enabled;
    }

    /**
     * 检查饰品是否启用
     */
    public static boolean isBaublesEnabled() {
        return enabled && baubles.enabled;
    }

    @Mod.EventBusSubscriber(modid = "moremod")
    public static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if ("moremod".equals(event.getModID())) {
                ConfigManager.sync("moremod", Config.Type.INSTANCE);
                System.out.println("[moremod] 赞助者物品配置已更新");
            }
        }
    }
}
