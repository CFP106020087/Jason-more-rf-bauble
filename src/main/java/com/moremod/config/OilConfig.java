package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 石油生成配置
 */
@Config(modid = "moremod", name = "moremod/oil_generation")
public class OilConfig {

    @Config.Comment({
        "石油生成概率 (0.0 - 1.0)",
        "0.15 = 15% 的区块有石油",
        "默认: 0.15"
    })
    @Config.Name("石油生成概率")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double oilChance = 0.15;

    @Config.Comment({
        "最小石油储量 (mB)",
        "默认: 600000 (600k mB)"
    })
    @Config.Name("最小储量")
    @Config.RangeInt(min = 1000, max = 100000000)
    public static int minOilAmount = 600000;

    @Config.Comment({
        "最大石油储量 (mB)",
        "默认: 30000000 (30M mB)"
    })
    @Config.Name("最大储量")
    @Config.RangeInt(min = 10000, max = 100000000)
    public static int maxOilAmount = 30000000;

    @Config.Comment({
        "石油最浅深度 (Y坐标)",
        "默认: 10"
    })
    @Config.Name("最浅深度")
    @Config.RangeInt(min = 1, max = 255)
    public static int minDepth = 10;

    @Config.Comment({
        "石油最深深度 (Y坐标)",
        "默认: 40"
    })
    @Config.Name("最深深度")
    @Config.RangeInt(min = 1, max = 255)
    public static int maxDepth = 40;

    @Config.Comment({
        "禁止生成石油的维度ID列表",
        "例如: 1 = 末地, -1 = 下界",
        "默认禁止: 末地(1), 下界(-1)"
    })
    @Config.Name("禁止的维度")
    public static int[] blacklistedDimensions = { 1, -1 };

    @Config.Comment({
        "是否使用维度白名单模式",
        "true = 只在白名单维度生成石油",
        "false = 在所有维度生成，除了黑名单维度",
        "默认: false"
    })
    @Config.Name("使用白名单模式")
    public static boolean useWhitelist = false;

    @Config.Comment({
        "允许生成石油的维度ID列表（仅当白名单模式开启时生效）",
        "默认: 0 = 主世界"
    })
    @Config.Name("允许的维度")
    public static int[] whitelistedDimensions = { 0 };

    @Config.Comment({
        "探测器使用冷却时间 (tick)",
        "20 tick = 1秒",
        "默认: 100 (5秒)"
    })
    @Config.Name("探测冷却")
    @Config.RangeInt(min = 0, max = 6000)
    public static int scanCooldown = 100;

    // ============== 运行时缓存 ==============
    private static Set<Integer> cachedBlacklist = null;
    private static Set<Integer> cachedWhitelist = null;

    /**
     * 检查维度是否允许生成石油
     */
    public static boolean isDimensionAllowed(int dimensionId) {
        if (useWhitelist) {
            return getWhitelistSet().contains(dimensionId);
        } else {
            return !getBlacklistSet().contains(dimensionId);
        }
    }

    private static Set<Integer> getBlacklistSet() {
        if (cachedBlacklist == null) {
            cachedBlacklist = new HashSet<>();
            for (int dim : blacklistedDimensions) {
                cachedBlacklist.add(dim);
            }
        }
        return cachedBlacklist;
    }

    private static Set<Integer> getWhitelistSet() {
        if (cachedWhitelist == null) {
            cachedWhitelist = new HashSet<>();
            for (int dim : whitelistedDimensions) {
                cachedWhitelist.add(dim);
            }
        }
        return cachedWhitelist;
    }

    /**
     * 清除缓存
     */
    public static void invalidateCache() {
        cachedBlacklist = null;
        cachedWhitelist = null;
    }

    @Mod.EventBusSubscriber(modid = "moremod")
    public static class ConfigSyncHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals("moremod")) {
                ConfigManager.sync("moremod", Config.Type.INSTANCE);
                invalidateCache();
            }
        }
    }
}
