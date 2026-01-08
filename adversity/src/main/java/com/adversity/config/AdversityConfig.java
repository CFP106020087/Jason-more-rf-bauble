package com.adversity.config;

import com.adversity.Adversity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.IMob;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;

@Config(modid = Adversity.MODID, name = "adversity")
public class AdversityConfig {

    @Config.Comment({
        "Entity Filter Settings",
        "实体过滤设置"
    })
    public static final EntityFilter entityFilter = new EntityFilter();

    @Config.Comment({
        "Difficulty Settings",
        "难度设置"
    })
    public static final DifficultySettings difficulty = new DifficultySettings();

    public static class EntityFilter {

        @Config.Comment({
            "If true, only entities in the whitelist will be affected.",
            "If false, all hostile mobs (IMob) + whitelist will be affected.",
            "若为 true，只有白名单中的实体会被影响。",
            "若为 false，所有敌对生物 + 白名单中的实体会被影响。"
        })
        public boolean whitelistOnly = false;

        @Config.Comment({
            "Entity whitelist. Format: modid:entity_name",
            "These entities will ALWAYS be affected by the difficulty system.",
            "实体白名单。格式: modid:entity_name",
            "这些实体将始终受到难度系统影响。",
            "",
            "Examples:",
            "  minecraft:zombie",
            "  lycanitesmobs:geonach",
            "  iceandfire:firedragon"
        })
        public String[] whitelist = new String[] {
            // 默认为空，敌对生物自动包含
        };

        @Config.Comment({
            "Entity blacklist. Format: modid:entity_name",
            "These entities will NEVER be affected, even if they are hostile.",
            "实体黑名单。格式: modid:entity_name",
            "这些实体永远不会受到影响，即使它们是敌对生物。",
            "",
            "Examples:",
            "  minecraft:ender_dragon",
            "  minecraft:wither"
        })
        public String[] blacklist = new String[] {
            // 默认排除原版 Boss
            "minecraft:ender_dragon",
            "minecraft:wither"
        };
    }

    public static class DifficultySettings {

        @Config.Comment({
            "Distance (in blocks) per 1 difficulty point",
            "每增加多少格距离增加 1 点难度"
        })
        @Config.RangeDouble(min = 100, max = 5000)
        public double blocksPerDifficulty = 500;

        @Config.Comment({
            "Days per 1 difficulty point",
            "每过多少天增加 1 点难度"
        })
        @Config.RangeDouble(min = 1, max = 100)
        public double daysPerDifficulty = 5;

        @Config.Comment({
            "Health multiplier per difficulty point (e.g., 0.15 = +15% per point)",
            "每点难度的生命值倍率 (例如 0.15 = 每点 +15%)"
        })
        @Config.RangeDouble(min = 0, max = 1)
        public double healthMultiplierPerDifficulty = 0.15;

        @Config.Comment({
            "Damage multiplier per difficulty point (e.g., 0.08 = +8% per point)",
            "每点难度的伤害倍率 (例如 0.08 = 每点 +8%)"
        })
        @Config.RangeDouble(min = 0, max = 1)
        public double damageMultiplierPerDifficulty = 0.08;

        @Config.Comment({
            "Maximum difficulty from distance",
            "距离难度的最大值"
        })
        @Config.RangeDouble(min = 1, max = 50)
        public double maxDistanceDifficulty = 10;

        @Config.Comment({
            "Maximum difficulty from time",
            "时间难度的最大值"
        })
        @Config.RangeDouble(min = 1, max = 50)
        public double maxTimeDifficulty = 8;
    }

    // ==================== 运行时缓存 ====================

    private static Set<ResourceLocation> whitelistCache = new HashSet<>();
    private static Set<ResourceLocation> blacklistCache = new HashSet<>();
    private static boolean cacheInitialized = false;

    /**
     * 刷新缓存
     */
    public static void refreshCache() {
        whitelistCache.clear();
        blacklistCache.clear();

        for (String entry : entityFilter.whitelist) {
            if (entry != null && !entry.isEmpty()) {
                whitelistCache.add(new ResourceLocation(entry.trim()));
            }
        }

        for (String entry : entityFilter.blacklist) {
            if (entry != null && !entry.isEmpty()) {
                blacklistCache.add(new ResourceLocation(entry.trim()));
            }
        }

        cacheInitialized = true;
        Adversity.LOGGER.info("Config cache refreshed: {} whitelist, {} blacklist",
            whitelistCache.size(), blacklistCache.size());
    }

    /**
     * 检查实体是否应该被难度系统处理
     */
    public static boolean shouldProcess(EntityLiving entity) {
        if (!cacheInitialized) {
            refreshCache();
        }

        ResourceLocation entityId = EntityList.getKey(entity);
        if (entityId == null) {
            return false;
        }

        // 黑名单优先 - 永远不处理
        if (blacklistCache.contains(entityId)) {
            return false;
        }

        // 在白名单中 - 始终处理
        if (whitelistCache.contains(entityId)) {
            return true;
        }

        // 白名单模式 - 只处理白名单
        if (entityFilter.whitelistOnly) {
            return false;
        }

        // 默认模式 - 处理敌对生物
        return entity instanceof IMob;
    }

    // ==================== 配置同步 ====================

    @Mod.EventBusSubscriber(modid = Adversity.MODID)
    public static class ConfigSyncHandler {

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(Adversity.MODID)) {
                ConfigManager.sync(Adversity.MODID, Config.Type.INSTANCE);
                refreshCache();
            }
        }
    }
}
