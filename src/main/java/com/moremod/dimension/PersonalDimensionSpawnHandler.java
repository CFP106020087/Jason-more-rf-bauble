package com.moremod.dimension;

import com.moremod.entity.boss.EntityRiftwarden;
import com.moremod.entity.boss.EntityStoneSentinel;
import com.moremod.entity.EntityCursedKnight;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 个人维度生成处理器 - TPS优化版
 *
 * 主要优化：
 * 1. 大幅降低搜索范围和迭代次数
 * 2. 添加地面高度缓存
 * 3. 延迟验证，避免阻塞主线程
 * 4. 缓存失效机制
 */
public class PersonalDimensionSpawnHandler {

    // ========== ✅ 优化：降低搜索范围 ==========
    private static final int SPAWNER_SEARCH_RADIUS = 4;   // 从6降到4
    private static final int BOSS_SEARCH_RADIUS = 12;     // 从20降到12
    private static final int MAX_SEARCH_ITERATIONS = 20;  // 从100降到20

    // ========== ✅ 优化：降低地面搜索范围 ==========
    private static final int GROUND_SEARCH_DOWN = 5;      // 从10降到5
    private static final int GROUND_SEARCH_UP = 2;        // 从5降到2
    private static final int HORIZONTAL_SEARCH_RADIUS = 2; // 从3降到2

    // 缓存系统
    private static final Map<ChunkPos, Set<BlockPos>> spawnerCache = new ConcurrentHashMap<>();
    private static final Map<ChunkPos, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 60000; // 1分钟缓存

    // ========== ✅ 新增：地面高度缓存 ==========
    private static final Map<ChunkPos, int[][]> groundHeightCache = new ConcurrentHashMap<>();
    private static final int CACHE_SIZE = 16; // 区块内16x16

    // Boss实体缓存
    private static final Set<Entity> activeBosses = Collections.newSetFromMap(new WeakHashMap<>());

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (event.getWorld().provider.getDimension() != PersonalDimensionManager.PERSONAL_DIM_ID) {
            return;
        }

        // 刷怪笼生成的实体允许
        if (event.isSpawner()) {
            if (event.getEntityLiving() != null) {
                adjustEntityToGroundCached(event.getWorld(), event.getEntityLiving());
            }
            event.setResult(Event.Result.ALLOW);
            return;
        }

        // 拒绝其他自然生成
        event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSpecialSpawn(LivingSpawnEvent.SpecialSpawn event) {
        if (event.getWorld().provider.getDimension() != PersonalDimensionManager.PERSONAL_DIM_ID) {
            return;
        }

        if (event.getEntityLiving() != null) {
            adjustEntityToGroundCached(event.getWorld(), event.getEntityLiving());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getWorld().provider.getDimension() != PersonalDimensionManager.PERSONAL_DIM_ID) {
            return;
        }

        // ✅ 确保是 WorldServer
        if (event.getWorld().isRemote) {
            return; // 客户端世界不处理
        }

        Entity entity = event.getEntity();

        // 玩家总是允许
        if (entity instanceof EntityPlayer) {
            return;
        }

        // 处理虚空守望者Boss
        if (entity instanceof EntityRiftwarden) {
            entity.addTag("boss_entity");
            entity.addTag("riftwarden_boss");
            activeBosses.add(entity);
            adjustEntityToGroundCached(event.getWorld(), entity);
            return;
        }

        // 处理石像哨兵Boss
        if (entity instanceof EntityStoneSentinel) {
            entity.addTag("boss_entity");
            entity.addTag("stone_sentinel_boss");
            activeBosses.add(entity);
            adjustEntityToGroundCached(event.getWorld(), entity);
            return;
        }

        // ========== ✅ 优化：延迟验证逻辑 ==========
        // 处理诅咒骑士（守卫怪物）
        if (entity instanceof EntityCursedKnight) {
            // 如果已经有标签，直接允许
            if (entity.getTags().contains("spawner_spawned") ||
                    entity.getTags().contains("boss_summoned") ||
                    entity.getTags().contains("player_summoned")) {
                adjustEntityToGroundCached(event.getWorld(), entity);
                return;
            }

            // ✅ 关键优化：先允许加入，延迟验证
            entity.addTag("pending_validation");
            adjustEntityToGroundCached(event.getWorld(), entity);

            // 在下一tick验证（避免阻塞主线程）
            final World world = event.getWorld();
            if (world instanceof WorldServer) {
                ((WorldServer) world).addScheduledTask(() -> {
                    if (entity.isDead) return;

                    // 简化验证：只检查缓存
                    if (hasNearbySpawnerCached(world, entity.getPosition()) ||
                            hasNearbyBossCached(entity.getPosition())) {
                        entity.addTag("spawner_spawned");
                        entity.getTags().remove("pending_validation");
                    } else {
                        // 验证失败，移除实体
                        entity.setDead();
                    }
                });
            }
            return;
        }

        // 处理哭泣天使
        if (entity.getClass().getName().contains("WeepingAngel") ||
                entity.getClass().getSimpleName().equals("EntityWeepingAngel")) {

            if (entity.getTags().contains("spawner_spawned") ||
                    entity.getTags().contains("boss_summoned") ||
                    entity.getTags().contains("player_summoned")) {
                adjustEntityToGroundCached(event.getWorld(), entity);
                return;
            }

            // ✅ 延迟验证
            entity.addTag("pending_validation");
            adjustEntityToGroundCached(event.getWorld(), entity);

            final World world = event.getWorld();
            if (world instanceof WorldServer) {
                ((WorldServer) world).addScheduledTask(() -> {
                    if (entity.isDead) return;
                    if (hasNearbySpawnerCached(world, entity.getPosition()) ||
                            hasNearbyBossCached(entity.getPosition())) {
                        entity.addTag("spawner_spawned");
                        entity.getTags().remove("pending_validation");
                    } else {
                        entity.setDead();
                    }
                });
            }
            return;
        }

        // 跳过非生物实体
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }

        // 处理其他生物实体
        if (entity instanceof EntityLiving) {
            if (entity.getTags().contains("spawner_spawned") ||
                    entity.getTags().contains("player_summoned") ||
                    entity.getTags().contains("boss_summoned")) {
                adjustEntityToGroundCached(event.getWorld(), entity);
                return;
            }

            // ✅ 延迟验证
            entity.addTag("pending_validation");
            adjustEntityToGroundCached(event.getWorld(), entity);

            final World world = event.getWorld();
            if (world instanceof WorldServer) {
                ((WorldServer) world).addScheduledTask(() -> {
                    if (entity.isDead) return;
                    if (hasNearbySpawnerCached(world, entity.getPosition()) ||
                            hasNearbyBossCached(entity.getPosition())) {
                        entity.addTag("spawner_spawned");
                        entity.getTags().remove("pending_validation");
                    } else {
                        entity.setDead();
                    }
                });
            }
            return;
        }
    }

    // ========== ✅ 新增：带缓存的地面调整 ==========
    /**
     * 使用缓存调整实体到地面位置
     */
    private void adjustEntityToGroundCached(World world, Entity entity) {
        BlockPos originalPos = entity.getPosition();
        ChunkPos chunkPos = new ChunkPos(originalPos);

        // 检查地面高度缓存
        int[][] heightMap = groundHeightCache.get(chunkPos);
        if (heightMap == null) {
            heightMap = new int[CACHE_SIZE][CACHE_SIZE];
            for (int i = 0; i < CACHE_SIZE; i++) {
                Arrays.fill(heightMap[i], -1);
            }
            groundHeightCache.put(chunkPos, heightMap);
        }

        int localX = originalPos.getX() & 15;
        int localZ = originalPos.getZ() & 15;
        int cachedY = heightMap[localX][localZ];

        if (cachedY > 0 && cachedY < 256) {
            // ✅ 使用缓存的高度
            entity.setPosition(
                    originalPos.getX() + 0.5,
                    cachedY + 0.1,
                    originalPos.getZ() + 0.5
            );
            entity.motionX = 0;
            entity.motionY = 0;
            entity.motionZ = 0;
            if (entity instanceof EntityLiving) {
                ((EntityLiving) entity).onGround = true;
            }
            return;
        }

        // ✅ 缓存未命中，快速搜索
        BlockPos groundPos = findGroundPositionFast(world, originalPos);

        if (groundPos != null) {
            // 更新缓存
            heightMap[localX][localZ] = groundPos.getY();

            entity.setPosition(
                    groundPos.getX() + 0.5,
                    groundPos.getY() + 0.1,
                    groundPos.getZ() + 0.5
            );
            entity.motionX = 0;
            entity.motionY = 0;
            entity.motionZ = 0;
            if (entity instanceof EntityLiving) {
                ((EntityLiving) entity).onGround = true;
            }
        }
    }

    // ========== ✅ 新增：快速地面搜索（只向下） ==========
    /**
     * 快速地面搜索 - 只向下搜索，不水平搜索
     */
    private BlockPos findGroundPositionFast(World world, BlockPos startPos) {
        // 只向下搜索
        for (int y = startPos.getY(); y >= Math.max(1, startPos.getY() - GROUND_SEARCH_DOWN); y--) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
            if (isValidGroundPosition(world, checkPos)) {
                return checkPos;
            }
        }

        // 如果向下没找到，尝试少量向上搜索
        for (int y = startPos.getY() + 1; y <= Math.min(255, startPos.getY() + GROUND_SEARCH_UP); y++) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
            if (isValidGroundPosition(world, checkPos)) {
                return checkPos;
            }
        }

        return null;
    }

    /**
     * 检查是否是有效的地面位置
     */
    private boolean isValidGroundPosition(World world, BlockPos pos) {
        BlockPos belowPos = pos.down();
        BlockPos abovePos = pos.up();

        IBlockState belowState = world.getBlockState(belowPos);
        if (!belowState.isFullCube() || belowState.getBlock() == Blocks.BEDROCK) {
            return false;
        }

        IBlockState currentState = world.getBlockState(pos);
        if (!isPassableBlock(world, pos, currentState)) {
            return false;
        }

        IBlockState aboveState = world.getBlockState(abovePos);
        if (!isPassableBlock(world, abovePos, aboveState)) {
            return false;
        }

        if (belowState.getBlock() == Blocks.LAVA ||
                belowState.getBlock() == Blocks.FLOWING_LAVA ||
                currentState.getBlock() == Blocks.LAVA ||
                currentState.getBlock() == Blocks.FLOWING_LAVA) {
            return false;
        }

        return true;
    }

    /**
     * 检查方块是否可通过
     */
    private boolean isPassableBlock(World world, BlockPos pos, IBlockState state) {
        if (world.isAirBlock(pos)) {
            return true;
        }

        return !state.isFullCube() &&
                !state.causesSuffocation() &&
                state.getBlock() != Blocks.WATER &&
                state.getBlock() != Blocks.FLOWING_WATER &&
                state.getBlock() != Blocks.LAVA &&
                state.getBlock() != Blocks.FLOWING_LAVA;
    }

    // ========== ✅ 新增：基于缓存的快速验证 ==========
    /**
     * 检查缓存中是否有附近的刷怪笼（不执行搜索）
     */
    private boolean hasNearbySpawnerCached(World world, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        Set<BlockPos> cachedSpawners = spawnerCache.get(chunkPos);

        if (cachedSpawners != null) {
            for (BlockPos spawnerPos : cachedSpawners) {
                double distance = pos.distanceSq(spawnerPos);
                if (distance <= SPAWNER_SEARCH_RADIUS * SPAWNER_SEARCH_RADIUS) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查缓存中是否有附近的Boss（不执行搜索）
     */
    private boolean hasNearbyBossCached(BlockPos pos) {
        cleanupInactiveBosses();

        for (Entity boss : activeBosses) {
            if (boss.isDead || !boss.isAddedToWorld()) {
                continue;
            }

            double distance = boss.getDistance(pos.getX(), pos.getY(), pos.getZ());
            if (distance <= BOSS_SEARCH_RADIUS) {
                return true;
            }
        }

        return false;
    }

    /**
     * 优化版：检查附近是否有刷怪笼
     */
    private boolean hasNearbySpawnerOptimized(World world, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);

        // 检查缓存
        long currentTime = System.currentTimeMillis();
        Long cacheTime = cacheTimestamps.get(chunkPos);

        if (cacheTime != null && currentTime - cacheTime < CACHE_DURATION) {
            Set<BlockPos> cachedSpawners = spawnerCache.get(chunkPos);
            if (cachedSpawners != null) {
                for (BlockPos spawnerPos : cachedSpawners) {
                    double distance = pos.distanceSq(spawnerPos);
                    if (distance <= SPAWNER_SEARCH_RADIUS * SPAWNER_SEARCH_RADIUS) {
                        return true;
                    }
                }
                return false;
            }
        }

        // 如果缓存无效，执行搜索
        Set<BlockPos> spawners = new HashSet<>();
        int iterations = 0;

        // 使用螺旋搜索模式
        for (int layer = 0; layer <= SPAWNER_SEARCH_RADIUS && iterations < MAX_SEARCH_ITERATIONS; layer++) {
            for (int x = -layer; x <= layer; x++) {
                for (int y = -layer; y <= layer; y++) {
                    for (int z = -layer; z <= layer; z++) {
                        if (Math.abs(x) != layer && Math.abs(y) != layer && Math.abs(z) != layer) {
                            continue;
                        }

                        BlockPos checkPos = pos.add(x, y, z);

                        if (checkPos.getY() < 0 || checkPos.getY() > 255) {
                            continue;
                        }

                        iterations++;
                        if (iterations >= MAX_SEARCH_ITERATIONS) {
                            break;
                        }

                        TileEntity te = world.getTileEntity(checkPos);
                        if (te instanceof TileEntityMobSpawner) {
                            spawners.add(checkPos);
                            updateCache(chunkPos, spawners, currentTime);
                            return true;
                        }
                    }
                }
            }
        }

        updateCache(chunkPos, spawners, currentTime);
        return false;
    }

    /**
     * 优化版：检查附近是否有Boss实体
     */
    private boolean hasNearbyBossOptimized(World world, BlockPos pos) {
        cleanupInactiveBosses();

        for (Entity boss : activeBosses) {
            if (boss.isDead || !boss.isAddedToWorld()) {
                continue;
            }

            double distance = boss.getDistance(pos.getX(), pos.getY(), pos.getZ());
            if (distance <= BOSS_SEARCH_RADIUS) {
                return true;
            }
        }

        return false;
    }

    /**
     * 更新缓存
     */
    private void updateCache(ChunkPos chunkPos, Set<BlockPos> spawners, long timestamp) {
        spawnerCache.put(chunkPos, spawners);
        cacheTimestamps.put(chunkPos, timestamp);

        // 清理过期缓存
        if (cacheTimestamps.size() > 100) {
            cleanupCache(timestamp);
        }
    }

    /**
     * 清理过期缓存
     */
    private void cleanupCache(long currentTime) {
        Iterator<Map.Entry<ChunkPos, Long>> it = cacheTimestamps.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ChunkPos, Long> entry = it.next();
            if (currentTime - entry.getValue() > CACHE_DURATION) {
                spawnerCache.remove(entry.getKey());
                it.remove();
            }
        }
    }

    /**
     * 清理无效的Boss引用
     */
    private void cleanupInactiveBosses() {
        activeBosses.removeIf(boss -> boss.isDead || !boss.isAddedToWorld());
    }

    /**
     * 世界卸载时清理缓存
     */
    public static void onWorldUnload() {
        spawnerCache.clear();
        cacheTimestamps.clear();
        activeBosses.clear();
        groundHeightCache.clear(); // ✅ 清理地面缓存
    }
}