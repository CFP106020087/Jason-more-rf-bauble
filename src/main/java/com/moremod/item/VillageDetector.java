package com.moremod.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.Village;
import net.minecraft.world.World;

import java.util.List;

/**
 * 村庄检测器 - 1.12.2 版本
 *
 * 规则：村庄必须有至少 3 扇门才算有效村庄
 * 探测范围：300 格
 */
public class VillageDetector {

    /**
     * 有效村庄的最小门数
     */
    private static final int MIN_DOOR_COUNT = 3;

    /**
     * 最大探测距离（格）
     */
    private static final int MAX_SEARCH_DISTANCE = 300;

    /**
     * 查找最近的村庄（必须有至少3扇门，300格内）
     */
    public static BlockPos findNearestVillage(EntityPlayer player) {
        World world = player.world;
        BlockPos playerPos = player.getPosition();

        // 获取世界中的所有村庄
        List<Village> villages = world.villageCollection.getVillageList();

        if (villages.isEmpty()) {
            return null;
        }

        Village nearestVillage = null;
        double nearestDistanceSq = MAX_SEARCH_DISTANCE * MAX_SEARCH_DISTANCE; // 使用平方值优化性能

        // 遍历所有村庄，找到最近的有效村庄
        for (Village village : villages) {
            // 检查门数量 - 必须至少有3扇门
            if (village.getNumVillageDoors() < MIN_DOOR_COUNT) {
                continue;  // 跳过无效村庄
            }

            BlockPos villageCenter = village.getCenter();
            double distanceSq = playerPos.distanceSq(villageCenter);

            // 超出300格范围，跳过
            if (distanceSq > nearestDistanceSq) {
                continue;
            }

            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearestVillage = village;
            }
        }

        return nearestVillage != null ? nearestVillage.getCenter() : null;
    }

    /**
     * 检查指定位置是否在有效村庄范围内
     * （村庄必须有至少3扇门，300格内）
     */
    public static boolean isInVillage(World world, BlockPos pos) {
        Village village = world.villageCollection.getNearestVillage(pos, MAX_SEARCH_DISTANCE);

        if (village == null) {
            return false;
        }

        // 检查门数量
        return village.getNumVillageDoors() >= MIN_DOOR_COUNT;
    }

    /**
     * 获取最近村庄的半径
     * （只返回有效村庄的半径，300格内）
     */
    public static int getNearestVillageRadius(EntityPlayer player) {
        World world = player.world;
        BlockPos playerPos = player.getPosition();
        Village village = world.villageCollection.getNearestVillage(playerPos, MAX_SEARCH_DISTANCE);

        if (village == null || village.getNumVillageDoors() < MIN_DOOR_COUNT) {
            return 0;
        }

        return village.getVillageRadius();
    }

    /**
     * 获取村庄信息（用于调试）
     */
    public static String getVillageInfo(Village village) {
        if (village == null) {
            return "无村庄";
        }

        int doors = village.getNumVillageDoors();
        int villagers = village.getNumVillagers();
        BlockPos center = village.getCenter();
        int radius = village.getVillageRadius();
        boolean isValid = doors >= MIN_DOOR_COUNT;

        return String.format(
                "村庄 [%s] - 门: %d, 村民: %d, 中心: %s, 半径: %d",
                isValid ? "有效" : "无效",
                doors,
                villagers,
                center.toString(),
                radius
        );
    }

    /**
     * 统计附近的有效村庄数量（使用默认300格范围）
     */
    public static int countNearbyValidVillages(EntityPlayer player) {
        return countNearbyValidVillages(player, MAX_SEARCH_DISTANCE);
    }

    /**
     * 统计附近的有效村庄数量（自定义范围）
     */
    public static int countNearbyValidVillages(EntityPlayer player, int searchRadius) {
        World world = player.world;
        BlockPos playerPos = player.getPosition();
        List<Village> villages = world.villageCollection.getVillageList();

        int count = 0;
        double searchRadiusSq = searchRadius * searchRadius; // 使用平方值避免开方运算

        for (Village village : villages) {
            // 必须有至少3扇门
            if (village.getNumVillageDoors() < MIN_DOOR_COUNT) {
                continue;
            }

            BlockPos villageCenter = village.getCenter();
            double distanceSq = playerPos.distanceSq(villageCenter);

            if (distanceSq <= searchRadiusSq) {
                count++;
            }
        }

        return count;
    }
}