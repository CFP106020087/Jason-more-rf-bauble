package com.moremod.util;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

/**
 * Boss战方块追踪器
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class BossBlockTracker {

    /**
     * Boss需要实现的接口
     * 注意：不要声明Entity已有的方法，避免抽象方法冲突
     */
    public interface IBossBlockTrackable {
        // 这些方法Entity类已经有了，不需要在接口中声明
        // 接口只是作为标记接口使用
    }

    // 存储Boss战期间玩家放置的方块
    private static final Map<Integer, Set<BlockPos>> bossPlayerBlocks = new HashMap<>();

    // 活跃的Boss列表
    private static final Map<Integer, EntityLiving> activeBosses = new HashMap<>();

    // Boss检测范围
    private static final int BOSS_DETECTION_RANGE = 50;

    /**
     * 当Boss开始战斗时调用
     */
    public static void startTracking(EntityLiving boss) {
        if (boss.world.isRemote) return;

        activeBosses.put(boss.getEntityId(), boss);
        bossPlayerBlocks.computeIfAbsent(boss.getEntityId(), k -> new HashSet<>());
    }

    /**
     * 当Boss死亡或战斗结束时调用
     */
    public static void stopTracking(EntityLiving boss) {
        if (boss.world.isRemote) return;

        activeBosses.remove(boss.getEntityId());
        bossPlayerBlocks.remove(boss.getEntityId());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.PlaceEvent event) {
        if (event.getPlayer() == null || event.getWorld().isRemote) return;

        World world = event.getWorld();
        BlockPos pos = event.getPos();

        Iterator<Map.Entry<Integer, EntityLiving>> iterator = activeBosses.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, EntityLiving> entry = iterator.next();
            EntityLiving boss = entry.getValue();

            if (boss.world.provider.getDimension() != world.provider.getDimension()) {
                continue;
            }

            if (!boss.isEntityAlive()) {
                iterator.remove();
                bossPlayerBlocks.remove(entry.getKey());
                continue;
            }

            double distance = boss.getDistance(pos.getX(), pos.getY(), pos.getZ());
            if (distance <= BOSS_DETECTION_RANGE) {
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

        for (Set<BlockPos> blocks : bossPlayerBlocks.values()) {
            blocks.remove(pos);
        }
    }

    /**
     * 检查方块是否是在Boss战中放置的
     */
    public static boolean isBossPlayerBlock(EntityLiving boss, BlockPos pos) {
        Set<BlockPos> blocks = bossPlayerBlocks.get(boss.getEntityId());
        return blocks != null && blocks.contains(pos);
    }

    /**
     * 获取Boss战中玩家放置的所有方块
     */
    public static List<BlockPos> getBossPlayerBlocks(EntityLiving boss, int maxDistance) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> blocks = bossPlayerBlocks.get(boss.getEntityId());

        if (blocks == null) return result;

        BlockPos bossPos = boss.getPosition();
        int maxDistSq = maxDistance * maxDistance;
        
        for (BlockPos pos : blocks) {
            if (pos.distanceSq(bossPos) <= maxDistSq) {
                result.add(pos);
            }
        }

        return result;
    }

    /**
     * 移除Boss战中的方块记录
     */
    public static void removeBlock(EntityLiving boss, BlockPos pos) {
        Set<BlockPos> blocks = bossPlayerBlocks.get(boss.getEntityId());
        if (blocks != null) {
            blocks.remove(pos);
        }
    }

    /**
     * 清理死亡Boss的记录
     */
    public static void cleanupDeadBosses() {
        Iterator<Map.Entry<Integer, EntityLiving>> it = activeBosses.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, EntityLiving> entry = it.next();
            if (!entry.getValue().isEntityAlive()) {
                it.remove();
                bossPlayerBlocks.remove(entry.getKey());
            }
        }
    }

    /**
     * 获取指定Boss附近的方块数量
     */
    public static int getBlockCount(int bossId) {
        Set<BlockPos> blocks = bossPlayerBlocks.get(bossId);
        return blocks != null ? blocks.size() : 0;
    }

    /**
     * 检查是否有活跃的Boss战
     */
    public static boolean hasActiveBosses() {
        return !activeBosses.isEmpty();
    }
}