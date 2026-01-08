package com.adversity.client;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端缓存 - 存储从服务端同步过来的怪物数据
 * 用于客户端渲染词条信息
 */
@SideOnly(Side.CLIENT)
public class ClientAdversityCache {

    /**
     * 缓存的实体数据
     */
    public static class CachedEntityData {
        public final int tier;
        public final float difficultyLevel;
        public final float healthMultiplier;
        public final float damageMultiplier;
        public final List<ResourceLocation> affixIds;
        public final long timestamp;

        public CachedEntityData(int tier, float difficultyLevel, float healthMultiplier,
                                float damageMultiplier, List<ResourceLocation> affixIds) {
            this.tier = tier;
            this.difficultyLevel = difficultyLevel;
            this.healthMultiplier = healthMultiplier;
            this.damageMultiplier = damageMultiplier;
            this.affixIds = new ArrayList<>(affixIds);
            this.timestamp = System.currentTimeMillis();
        }

        /**
         * 检查缓存是否过期（10秒）
         */
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 10000;
        }
    }

    // 实体ID -> 缓存数据
    private static final Map<Integer, CachedEntityData> CACHE = new ConcurrentHashMap<>();

    // 上次清理时间
    private static long lastCleanup = 0;
    private static final long CLEANUP_INTERVAL = 30000; // 30秒清理一次

    /**
     * 更新实体数据
     */
    public static void updateEntity(int entityId, int tier, float difficultyLevel,
                                    float healthMultiplier, float damageMultiplier,
                                    List<ResourceLocation> affixIds) {
        CachedEntityData data = new CachedEntityData(tier, difficultyLevel,
                                                      healthMultiplier, damageMultiplier, affixIds);
        CACHE.put(entityId, data);

        // 定期清理过期数据
        cleanupIfNeeded();
    }

    /**
     * 获取实体数据
     */
    @Nullable
    public static CachedEntityData getEntityData(int entityId) {
        CachedEntityData data = CACHE.get(entityId);
        if (data != null && data.isExpired()) {
            CACHE.remove(entityId);
            return null;
        }
        return data;
    }

    /**
     * 移除实体数据（当实体被移除时）
     */
    public static void removeEntity(int entityId) {
        CACHE.remove(entityId);
    }

    /**
     * 清除所有缓存（如切换世界时）
     */
    public static void clearAll() {
        CACHE.clear();
    }

    /**
     * 定期清理过期数据
     */
    private static void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > CLEANUP_INTERVAL) {
            lastCleanup = now;
            CACHE.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }

    /**
     * 获取缓存大小（调试用）
     */
    public static int getCacheSize() {
        return CACHE.size();
    }
}
