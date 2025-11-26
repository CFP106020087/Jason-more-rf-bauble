// ===== BossBlockTracker.java - 优化版：只在Boss战时追踪 =====
package com.moremod.util;

import com.moremod.entity.boss.EntityRiftwarden;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

@Mod.EventBusSubscriber(modid = "moremod")
public class BossBlockTracker {

    // 存储Boss战期间玩家放置的方块
    // Key: Boss实体ID, Value: 该Boss战斗中玩家放置的方块
    private static final Map<Integer, Set<BlockPos>> bossPlayerBlocks = new HashMap<>();

    // 活跃的Boss列表（正在战斗中的）
    private static final Map<Integer, EntityRiftwarden> activeBosses = new HashMap<>();

    // Boss检测范围
    private static final int BOSS_DETECTION_RANGE = 50;

    /**
     * 当Boss开始战斗时调用（在Boss的addTrackingPlayer方法中调用）
     */
    public static void startTracking(EntityRiftwarden boss) {
        if (boss.world.isRemote) return;

        activeBosses.put(boss.getEntityId(), boss);
        bossPlayerBlocks.computeIfAbsent(boss.getEntityId(), k -> new HashSet<>());
    }

    /**
     * 当Boss死亡或战斗结束时调用
     */
    public static void stopTracking(EntityRiftwarden boss) {
        if (boss.world.isRemote) return;

        activeBosses.remove(boss.getEntityId());
        bossPlayerBlocks.remove(boss.getEntityId());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.PlaceEvent event) {
        if (event.getPlayer() == null || event.getWorld().isRemote) return;

        World world = event.getWorld();
        BlockPos pos = event.getPos();

        // 使用迭代器安全遍历并移除死亡的Boss
        Iterator<Map.Entry<Integer, EntityRiftwarden>> iterator = activeBosses.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, EntityRiftwarden> entry = iterator.next();
            EntityRiftwarden boss = entry.getValue();

            if (boss.world.provider.getDimension() != world.provider.getDimension()) continue;

            if (!boss.isEntityAlive()) {
                iterator.remove();
                bossPlayerBlocks.remove(entry.getKey());
                continue;
            }

            // 检查距离
            double distance = boss.getDistance(pos.getX(), pos.getY(), pos.getZ());
            if (distance <= BOSS_DETECTION_RANGE) {
                // 记录这个方块
                Set<BlockPos> blocks = bossPlayerBlocks.get(boss.getEntityId());
                if (blocks != null) {
                    blocks.add(pos.toImmutable());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote) return;

        BlockPos pos = event.getPos();

        // 从所有Boss的记录中移除这个方块
        for (Set<BlockPos> blocks : bossPlayerBlocks.values()) {
            blocks.remove(pos);
        }
    }

    /**
     * 检查方块是否是在Boss战中放置的
     */
    public static boolean isBossPlayerBlock(EntityRiftwarden boss, BlockPos pos) {
        Set<BlockPos> blocks = bossPlayerBlocks.get(boss.getEntityId());
        return blocks != null && blocks.contains(pos);
    }

    /**
     * 获取Boss战中玩家放置的所有方块
     */
    public static List<BlockPos> getBossPlayerBlocks(EntityRiftwarden boss, int maxDistance) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> blocks = bossPlayerBlocks.get(boss.getEntityId());

        if (blocks == null) return result;

        BlockPos bossPos = boss.getPosition();
        for (BlockPos pos : blocks) {
            if (pos.distanceSq(bossPos) <= maxDistance * maxDistance) {
                result.add(pos);
            }
        }

        return result;
    }

    /**
     * 移除Boss战中的方块记录
     */
    public static void removeBlock(EntityRiftwarden boss, BlockPos pos) {
        Set<BlockPos> blocks = bossPlayerBlocks.get(boss.getEntityId());
        if (blocks != null) {
            blocks.remove(pos);
        }
    }

    /**
     * 清理死亡Boss的记录
     */
    public static void cleanupDeadBosses() {
        Iterator<Map.Entry<Integer, EntityRiftwarden>> it = activeBosses.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, EntityRiftwarden> entry = it.next();
            if (!entry.getValue().isEntityAlive()) {
                it.remove();
                bossPlayerBlocks.remove(entry.getKey());
            }
        }
    }
}