package com.moremod.system.humanity.intel;

import com.moremod.system.humanity.IHumanityData;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * 情报数据辅助类
 * Intel Data Helper
 *
 * 提供情报系统的数据访问和计算方法
 */
public class IntelDataHelper {

    // 每种生物的最大情报等级 (防止无限堆叠)
    public static final int MAX_INTEL_LEVEL = 50;

    // 最大伤害倍率上限 (5x = +400%)
    public static final float MAX_DAMAGE_MULTIPLIER = 5.0f;

    /**
     * 获取玩家对特定生物的情报等级
     */
    public static int getIntelLevel(@Nullable IHumanityData data, ResourceLocation entityId) {
        if (data == null || entityId == null) return 0;
        return data.getIntelLevel(entityId);
    }

    /**
     * 增加玩家对特定生物的情报等级
     * @return 是否成功增加 (false = 已达上限)
     */
    public static boolean incrementIntelLevel(@Nullable IHumanityData data, ResourceLocation entityId) {
        if (data == null || entityId == null) return false;

        int current = data.getIntelLevel(entityId);
        if (current >= MAX_INTEL_LEVEL) return false;

        data.setIntelLevel(entityId, current + 1);
        return true;
    }

    /**
     * 检查是否已达到情报等级上限
     */
    public static boolean isMaxLevel(@Nullable IHumanityData data, ResourceLocation entityId) {
        return getIntelLevel(data, entityId) >= MAX_INTEL_LEVEL;
    }

    /**
     * 设置玩家对特定生物的情报等级
     */
    public static void setIntelLevel(@Nullable IHumanityData data, ResourceLocation entityId, int level) {
        if (data == null || entityId == null) return;
        data.setIntelLevel(entityId, Math.min(MAX_INTEL_LEVEL, Math.max(0, level)));
    }

    /**
     * 计算对特定生物的伤害加成倍率
     * @return 1.0 = 无加成, 1.1 = +10%, 最大 5.0 (+400%)
     */
    public static float calculateDamageMultiplier(@Nullable IHumanityData data, ResourceLocation entityId) {
        if (data == null || entityId == null) return 1.0f;

        int level = data.getIntelLevel(entityId);
        if (level <= 0) return 1.0f;

        // 每级 +10% 伤害, 上限 5x
        float multiplier = 1.0f + (level * ItemIntelBook.DAMAGE_BONUS_PER_BOOK);
        return Math.min(MAX_DAMAGE_MULTIPLIER, multiplier);
    }

    /**
     * 计算对目标实体的伤害加成倍率
     */
    public static float calculateDamageMultiplier(@Nullable IHumanityData data, EntityLivingBase target) {
        if (data == null || target == null) return 1.0f;

        ResourceLocation entityId = net.minecraft.entity.EntityList.getKey(target);
        return calculateDamageMultiplier(data, entityId);
    }

    /**
     * 获取所有已学习的情报
     */
    public static Map<ResourceLocation, Integer> getAllIntel(@Nullable IHumanityData data) {
        if (data == null) return java.util.Collections.emptyMap();
        return data.getLearnedIntel();
    }

    /**
     * 检查玩家是否学习过该生物的情报
     */
    public static boolean hasIntel(@Nullable IHumanityData data, ResourceLocation entityId) {
        return getIntelLevel(data, entityId) > 0;
    }

    /**
     * 获取情报总学习数量（用于统计）
     */
    public static int getTotalIntelLearned(@Nullable IHumanityData data) {
        if (data == null) return 0;

        int total = 0;
        for (int level : data.getLearnedIntel().values()) {
            total += level;
        }
        return total;
    }

    /**
     * 获取已学习情报的种类数量
     */
    public static int getIntelTypesLearned(@Nullable IHumanityData data) {
        if (data == null) return 0;

        int count = 0;
        for (int level : data.getLearnedIntel().values()) {
            if (level > 0) count++;
        }
        return count;
    }
}
