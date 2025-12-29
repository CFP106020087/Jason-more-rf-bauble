package com.moremod.dimension;

import com.moremod.entity.boss.EntityRiftwarden;
import com.moremod.entity.boss.EntityStoneSentinel;
import com.moremod.entity.EntityCursedKnight;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 个人维度生成处理器 - 严格验证修复版
 *
 * ✅ 关键修复：
 * 1. 修复生物生成在方块中 - 改进地面搜索和位置调整
 * 2. 加强验证严格性 - 缩短宽限期，拒绝指令生成的生物
 * 3. 刷怪笼生成立即标记，不需要验证
 */
public class PersonalDimensionSpawnHandler {

    // 搜索参数
    private static final int SPAWNER_SEARCH_RADIUS = 8;
    private static final int BOSS_SEARCH_RADIUS = 15;
    private static final int MAX_SEARCH_ITERATIONS = 50;

    private static final int GROUND_SEARCH_DOWN = 6;
    private static final int GROUND_SEARCH_UP = 3;

    // 缓存系统
    private static final Map<ChunkPos, Set<BlockPos>> spawnerCache = new ConcurrentHashMap<>();
    private static final Map<ChunkPos, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 60000;

    // Boss实体缓存
    private static final Set<Entity> activeBosses = Collections.newSetFromMap(new WeakHashMap<>());

    // ✅ 修改：缩短宽限期到1秒（仅用于刷怪笼生成延迟）
    private static final Map<UUID, Long> pendingValidation = new ConcurrentHashMap<>();
    private static final long VALIDATION_GRACE_PERIOD = 1000; // 1秒

    // ✅ 新增：验证失败记录（防止反复尝试）
    private static final Set<UUID> validationFailed = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (event.getWorld().provider.getDimension() != PersonalDimensionManager.PERSONAL_DIM_ID) return;

        // ✅ 刷怪笼生成 - 立即标记并允许
        if (event.isSpawner()) {
            if (event.getEntityLiving() != null) {
                event.getEntityLiving().addTag("spawner_spawned");
                adjustEntityToGround(event.getWorld(), event.getEntityLiving());
                cacheNearbySpawners(event.getWorld(), event.getEntityLiving().getPosition());
            }
            event.setResult(Event.Result.ALLOW);
            return;
        }

        // 拒绝自然生成
        event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSpecialSpawn(LivingSpawnEvent.SpecialSpawn event) {
        if (event.getWorld().provider.getDimension() != PersonalDimensionManager.PERSONAL_DIM_ID) return;

        if (event.getEntityLiving() != null) {
            adjustEntityToGround(event.getWorld(), event.getEntityLiving());

            // 如果是刷怪笼触发的特殊生成
            if (event.getSpawner() != null) {
                event.getEntityLiving().addTag("spawner_spawned");
                cacheNearbySpawners(event.getWorld(), event.getEntityLiving().getPosition());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getWorld().provider.getDimension() != PersonalDimensionManager.PERSONAL_DIM_ID) {
            return;
        }

        if (event.getWorld().isRemote) {
            return;
        }

        Entity entity = event.getEntity();

        // 玩家总是允许
        if (entity instanceof EntityPlayer) {
            return;
        }

        // Boss实体
        if (entity instanceof EntityRiftwarden) {
            entity.addTag("boss_entity");
            entity.addTag("riftwarden_boss");
            activeBosses.add(entity);
            adjustEntityToGround(event.getWorld(), entity);
            return;
        }

        if (entity instanceof EntityStoneSentinel) {
            entity.addTag("boss_entity");
            entity.addTag("stone_sentinel_boss");
            activeBosses.add(entity);
            adjustEntityToGround(event.getWorld(), entity);
            return;
        }

        // ========== ✅ 修复：更严格的验证逻辑 ==========
        if (entity instanceof EntityCursedKnight || isWeepingAngel(entity) || entity instanceof EntityLiving) {
            UUID entityId = entity.getUniqueID();
            
            // ✅ 如果已验证失败，直接拒绝
            if (validationFailed.contains(entityId)) {
                event.setCanceled(true);
                return;
            }

            // ✅ 如果已有合法标签，直接允许
            if (entity.getTags().contains("spawner_spawned") ||
                entity.getTags().contains("boss_summoned") ||
                entity.getTags().contains("altar_spawned") ||
                entity.getTags().contains("player_summoned")) {
                System.out.println("[生成处理] 允许带标签的实体: " + entity.getClass().getSimpleName() + " 标签: " + entity.getTags());
                adjustEntityToGround(event.getWorld(), entity);
                return;
            }

            // ✅ 第一次见到这个实体 - 给予短暂宽限期（仅1秒）
            if (!pendingValidation.containsKey(entityId)) {
                // 立即检查缓存
                if (hasNearbySpawnerCached(event.getWorld(), entity.getPosition()) ||
                    hasNearbyBossCached(entity.getPosition())) {
                    entity.addTag("spawner_spawned");
                    adjustEntityToGround(event.getWorld(), entity);
                    return;
                }
                
                // 没找到刷怪笼 - 给1秒宽限期（可能是刚生成还没缓存）
                pendingValidation.put(entityId, System.currentTimeMillis());
                adjustEntityToGround(event.getWorld(), entity);
                cacheNearbySpawners(event.getWorld(), entity.getPosition());
                return;
            }

            // ✅ 宽限期检查
            long spawnTime = pendingValidation.get(entityId);
            long elapsed = System.currentTimeMillis() - spawnTime;
            
            if (elapsed < VALIDATION_GRACE_PERIOD) {
                return; // 还在1秒宽限期内
            }

            // ✅ 宽限期已过 - 最后一次验证
            if (hasNearbySpawnerCached(event.getWorld(), entity.getPosition()) ||
                hasNearbyBossCached(entity.getPosition())) {
                entity.addTag("spawner_spawned");
                pendingValidation.remove(entityId);
                return;
            }

            // ✅ 验证失败 - 标记并移除
            System.out.println("[生成处理] 拒绝非法生物: " + entity.getClass().getSimpleName() + " @ " + entity.getPosition());
            validationFailed.add(entityId);
            pendingValidation.remove(entityId);
            event.setCanceled(true);
        }
    }

    // ========== ✅ 修复：改进的实体位置调整 ==========
    private void adjustEntityToGround(World world, Entity entity) {
        if (entity == null) return;
        
        BlockPos currentPos = entity.getPosition();
        
        // ✅ 关键修复：检查实体是否卡在方块里
        if (isStuckInBlock(world, currentPos)) {
            // 如果卡在方块里，向上移动到空气中
            for (int y = currentPos.getY(); y <= Math.min(255, currentPos.getY() + 10); y++) {
                BlockPos testPos = new BlockPos(currentPos.getX(), y, currentPos.getZ());
                if (world.isAirBlock(testPos) && world.isAirBlock(testPos.up())) {
                    entity.setPosition(testPos.getX() + 0.5, testPos.getY(), testPos.getZ() + 0.5);
                    currentPos = testPos;
                    break;
                }
            }
        }
        
        // 现在检查是否有合适的地面
        if (isValidSpawnPosition(world, currentPos)) {
            return;
        }

        // ✅ 改进：先向下搜索地面
        BlockPos groundPos = findNearestGround(world, currentPos);
        if (groundPos != null) {
            entity.setPosition(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5);
            return;
        }
        
        // ✅ 如果向下找不到，尝试周围位置
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;
                
                BlockPos offsetPos = currentPos.add(dx, 0, dz);
                groundPos = findNearestGround(world, offsetPos);
                if (groundPos != null) {
                    entity.setPosition(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5);
                    return;
                }
            }
        }
    }

    /**
     * ✅ 新增：检查实体是否卡在固体方块里
     */
    private boolean isStuckInBlock(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.isFullCube() && !world.isAirBlock(pos);
    }

    /**
     * ✅ 新增：寻找最近的合适地面
     */
    private BlockPos findNearestGround(World world, BlockPos start) {
        // 向下搜索
        for (int y = start.getY(); y >= Math.max(0, start.getY() - GROUND_SEARCH_DOWN); y--) {
            BlockPos testPos = new BlockPos(start.getX(), y, start.getZ());
            if (isValidSpawnPosition(world, testPos)) {
                return testPos;
            }
        }
        
        // 向上搜索（如果向下找不到）
        for (int y = start.getY() + 1; y <= Math.min(255, start.getY() + GROUND_SEARCH_UP); y++) {
            BlockPos testPos = new BlockPos(start.getX(), y, start.getZ());
            if (isValidSpawnPosition(world, testPos)) {
                return testPos;
            }
        }
        
        return null;
    }

    /**
     * ✅ 改进：更严格的生成位置检查
     */
    private boolean isValidSpawnPosition(World world, BlockPos pos) {
        if (pos.getY() < 0 || pos.getY() > 255) return false;
        
        BlockPos belowPos = pos.down();
        IBlockState belowState = world.getBlockState(belowPos);
        
        // 下方必须是固体方块（不能是基岩）
        if (!belowState.isFullCube() || belowState.getBlock() == Blocks.BEDROCK) {
            return false;
        }

        // 当前位置必须是空气或可通过（不能是固体方块）
        IBlockState currentState = world.getBlockState(pos);
        if (currentState.isFullCube() || !world.isAirBlock(pos)) {
            return false;
        }
        
        // 头顶必须是空气或可通过
        IBlockState aboveState = world.getBlockState(pos.up());
        if (aboveState.isFullCube() || !world.isAirBlock(pos.up())) {
            return false;
        }

        // 额外检查：不能生成在岩浆或水上
        if (belowState.getBlock() == Blocks.LAVA || 
            belowState.getBlock() == Blocks.FLOWING_LAVA ||
            belowState.getBlock() == Blocks.WATER ||
            belowState.getBlock() == Blocks.FLOWING_WATER) {
            return false;
        }

        return true;
    }

    // ========== 刷怪笼缓存 ==========
    private void cacheNearbySpawners(World world, BlockPos center) {
        ChunkPos chunkPos = new ChunkPos(center);
        
        // 如果最近缓存过，跳过
        Long lastCache = cacheTimestamps.get(chunkPos);
        if (lastCache != null && System.currentTimeMillis() - lastCache < 5000) {
            return;
        }

        Set<BlockPos> spawners = new HashSet<>();
        
        int searchRadius = SPAWNER_SEARCH_RADIUS;
        int iterations = 0;

        for (int x = -searchRadius; x <= searchRadius && iterations < MAX_SEARCH_ITERATIONS; x++) {
            for (int z = -searchRadius; z <= searchRadius && iterations < MAX_SEARCH_ITERATIONS; z++) {
                for (int y = -searchRadius; y <= searchRadius && iterations < MAX_SEARCH_ITERATIONS; y++) {
                    BlockPos checkPos = center.add(x, y, z);
                    
                    if (checkPos.getY() < 0 || checkPos.getY() > 255) continue;
                    
                    TileEntity te = world.getTileEntity(checkPos);
                    if (te instanceof TileEntityMobSpawner) {
                        spawners.add(checkPos);
                    }
                    
                    iterations++;
                }
            }
        }

        if (!spawners.isEmpty()) {
            spawnerCache.put(chunkPos, spawners);
            cacheTimestamps.put(chunkPos, System.currentTimeMillis());
        }
    }

    private boolean hasNearbySpawnerCached(World world, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        
        // 检查当前区块和相邻区块
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos checkChunk = new ChunkPos(chunkPos.x + dx, chunkPos.z + dz);
                Set<BlockPos> cachedSpawners = spawnerCache.get(checkChunk);
                
                if (cachedSpawners != null) {
                    for (BlockPos spawnerPos : cachedSpawners) {
                        double distance = pos.distanceSq(spawnerPos);
                        if (distance <= SPAWNER_SEARCH_RADIUS * SPAWNER_SEARCH_RADIUS) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

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

    private void cleanupInactiveBosses() {
        activeBosses.removeIf(boss -> boss.isDead || !boss.isAddedToWorld());
    }

    private boolean isWeepingAngel(Entity entity) {
        String className = entity.getClass().getName();
        return className.contains("WeepingAngel") || 
               entity.getClass().getSimpleName().equals("EntityWeepingAngel");
    }

    /**
     * 世界卸载时清理
     */
    public static void onWorldUnload() {
        spawnerCache.clear();
        cacheTimestamps.clear();
        activeBosses.clear();
        pendingValidation.clear();
        validationFailed.clear();
        System.out.println("[生成处理] 缓存已清理");
    }

    /**
     * 定期清理过期数据
     */
    public static void cleanupPendingValidation() {
        long now = System.currentTimeMillis();
        pendingValidation.entrySet().removeIf(entry -> 
            now - entry.getValue() > VALIDATION_GRACE_PERIOD * 3
        );
        
        // 清理验证失败记录（避免无限增长）
        if (validationFailed.size() > 1000) {
            validationFailed.clear();
        }
    }

    /**
     * 获取统计信息
     */
    public static String getStatistics() {
        return String.format("[生成处理] 刷怪笼缓存: %d区块, Boss: %d, 待验证: %d, 已拒绝: %d",
            spawnerCache.size(), activeBosses.size(), pendingValidation.size(), validationFailed.size());
    }
}