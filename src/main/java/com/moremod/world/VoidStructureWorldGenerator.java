package com.moremod.world;

import com.moremod.dimension.VoidStructureGenerator;
import com.moremod.dimension.VoidStructureGenerator.StructureType;
import com.moremod.dungeon.DungeonLayoutGenerator;
import com.moremod.dungeon.DungeonTypes.DungeonLayout;
import com.moremod.dungeon.TreeDungeonPlacer;
import com.moremod.dimension.PersonalDimensionManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.ref.WeakReference;

/**
 * 虚空结构世界生成器 - 服务器友好修复版
 *
 * 关键修复：
 * 1) 生成任务不再持有 World 强引用 → 用 (dimId, pos) 解决世界卸载内存泄露。
 * 2) 队列处理统一交给 ServerTick → 即使不触发 IWorldGenerator 也能按时生成与清理。
 * 3) 只在玩家在线且位于私人维度时生成，且严格限制在其领地范围内。
 */
public class VoidStructureWorldGenerator implements IWorldGenerator {

    // ===== 配置常量 =====
    private static final int DUNGEON_SPACING = 500;
    private static final int VOID_STRUCTURE_SPACING = 128;
    private static final int MIN_DISTANCE_FROM_DUNGEON = 200;
    private static final int MAX_CACHED_POSITIONS = 1000;
    private static final long CACHE_CLEANUP_INTERVAL = 300_000L; // 5分钟
    private static final int GENERATION_QUEUE_MAX_SIZE = 256;

    private static final int PLAYER_GENERATION_RADIUS = 400;
    private static final int MAX_STRUCTURES_PER_PLAYER = 50;
    private static final long PLAYER_COOLDOWN_MS = 5_000L;

    // ===== 空间索引（按维度分片）=====
    private static class SpatialIndex {
        private final Map<ChunkPos, Set<BlockPos>> chunkToPositions = new ConcurrentHashMap<>();
        private final AtomicInteger size = new AtomicInteger(0);

        public void add(BlockPos pos) {
            ChunkPos chunkPos = new ChunkPos(pos);
            chunkToPositions.computeIfAbsent(chunkPos, k -> ConcurrentHashMap.newKeySet()).add(pos);
            size.incrementAndGet();
        }
        public boolean hasNearby(BlockPos pos, double distance) {
            int chunkRadius = ((int) distance >> 4) + 1;
            ChunkPos centerChunk = new ChunkPos(pos);
            for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                    Set<BlockPos> positions = chunkToPositions.get(new ChunkPos(centerChunk.x + dx, centerChunk.z + dz));
                    if (positions == null) continue;
                    for (BlockPos existing : positions) {
                        if (pos.getDistance(existing.getX(), existing.getY(), existing.getZ()) < distance) return true;
                    }
                }
            }
            return false;
        }
        public void clear() { chunkToPositions.clear(); size.set(0); }
        public int size() { return size.get(); }
        public void cleanup(int maxSize) {
            if (size.get() <= maxSize) return;
            int toRemove = size.get() - maxSize;
            Iterator<Map.Entry<ChunkPos, Set<BlockPos>>> it = chunkToPositions.entrySet().iterator();
            while (it.hasNext() && toRemove > 0) {
                Map.Entry<ChunkPos, Set<BlockPos>> e = it.next();
                int n = e.getValue().size();
                toRemove -= n; size.addAndGet(-n);
                it.remove();
            }
        }
    }

    // ===== 延迟生成任务（无 World 强引用）=====
    private static class GenerationTask {
        final int dimId;
        final BlockPos pos;
        final StructureType type;
        final UUID playerId;
        final long ts;

        GenerationTask(int dimId, BlockPos pos, StructureType type, UUID playerId) {
            this.dimId = dimId;
            this.pos = pos;
            this.type = type;
            this.playerId = playerId;
            this.ts = System.currentTimeMillis();
        }
        boolean isValid() { return System.currentTimeMillis() - ts < 60_000L; }
    }

    // ===== 索引与队列 =====
    private static final Map<Integer, SpatialIndex> DUNGEON_INDEX_BY_DIM = new ConcurrentHashMap<>();
    private static final Map<Integer, SpatialIndex> VOID_INDEX_BY_DIM    = new ConcurrentHashMap<>();
    private static final Queue<GenerationTask> QUEUE = new ConcurrentLinkedQueue<>();

    // 玩家计数/节流
    private static final Map<UUID, AtomicInteger> playerStructureCount = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> playerLastGen = new ConcurrentHashMap<>();

    // 统计
    private static final AtomicInteger totalStructuresGenerated = new AtomicInteger(0);
    private static final AtomicInteger totalDungeonsGenerated   = new AtomicInteger(0);

    // 清理
    private static long lastCleanupTime = System.currentTimeMillis();

    // 事件注册
    private static boolean eventRegistered = false;
    private static WeakReference<VoidStructureWorldGenerator> instanceRef;

    public VoidStructureWorldGenerator() {
        if (!eventRegistered) {
            MinecraftForge.EVENT_BUS.register(this);
            eventRegistered = true;
            instanceRef = new WeakReference<>(this);
            System.out.println("[虚空结构] 生成器初始化（修复版）");
        }
    }

    // ===== IWorldGenerator：仅做“发现与排程”，不直接重活 =====
    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.isRemote) return;
        if (world.provider.getDimension() != PersonalDimensionManager.PERSONAL_DIM_ID) return;

        // 无玩家 → 跳过
        if (!PersonalDimensionManager.hasPlayersInDimension(world)) return;

        // 轻量清理（避免在 worldgen 重活）
        lightCleanupIfNeeded();

        BlockPos chunkPos = new BlockPos(chunkX * 16 + 8, 100, chunkZ * 16 + 8);

        // 最近玩家且在半径内
        EntityPlayer nearest = findNearestPlayer(world, chunkPos, PLAYER_GENERATION_RADIUS);
        if (nearest == null) return;

        // 属地检查（如果找到所有者，则必须在其领地内）
        UUID owner = PersonalDimensionManager.findSpaceOwner(chunkPos);
        if (owner != null) {
            if (!isPlayerInDimension(world, owner)) return;
            PersonalDimensionManager.PersonalSpace sp = PersonalDimensionManager.getPlayerSpace(owner);
            if (sp != null && !sp.isInTerritory(chunkPos)) return;
        }

        // 获取索引
        SpatialIndex dIndex = getDungeonIndex(world.provider.getDimension());
        SpatialIndex vIndex = getVoidIndex(world.provider.getDimension());

        // 不在任何玩家房间附近（扩展边框）生成
        if (isInAnyPlayerSpace(chunkPos)) return;

        // 低频地牢：仅排程
        if ((chunkX % 8) == 0 && (chunkZ % 8) == 0 && random.nextInt(20) == 0) {
            if (!dIndex.hasNearby(chunkPos, DUNGEON_SPACING)) {
                scheduleDungeon(world.provider.getDimension(), chunkPos, random.nextLong(), nearest.getUniqueID());
            }
        }

        // 虚空结构：按玩家维度生成
        scheduleVoidForPlayer(world, chunkX, chunkZ, nearest, dIndex, vIndex);
    }

    // ===== 把生成任务搬到 Tick 线程中，避免卡在 worldgen =====
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        // 队列处理
        processQueue();

        // 周期清理
        hardCleanupIfNeeded();
    }

    // ====== 排程逻辑 ======
    private void scheduleVoidForPlayer(World world, int chunkX, int chunkZ, EntityPlayer player,
                                       SpatialIndex dIndex, SpatialIndex vIndex) {
        UUID pid = player.getUniqueID();

        // 玩家生成限制 & 冷却
        AtomicInteger cnt = playerStructureCount.computeIfAbsent(pid, k -> new AtomicInteger(0));
        if (cnt.get() >= MAX_STRUCTURES_PER_PLAYER) return;
        Long last = playerLastGen.get(pid);
        if (last != null && System.currentTimeMillis() - last < PLAYER_COOLDOWN_MS) return;

        BlockPos center = new BlockPos(chunkX * 16 + 8, 100, chunkZ * 16 + 8);
        double dist = player.getDistance(center.getX(), center.getY(), center.getZ());

        float chance = genChanceByDistance(dist);
        Random gridRng = seededGridRandom(world, chunkX, chunkZ, pid);

        if (gridRng.nextFloat() >= 0.3f * chance) return;

        // 网格内随机偏移
        int offsetX = gridRng.nextInt(VOID_STRUCTURE_SPACING / 2) - VOID_STRUCTURE_SPACING / 4;
        int offsetZ = gridRng.nextInt(VOID_STRUCTURE_SPACING / 2) - VOID_STRUCTURE_SPACING / 4;
        int baseY   = 80 + gridRng.nextInt(80);

        BlockPos pos = new BlockPos(center.getX() + offsetX, baseY, center.getZ() + offsetZ);

        // 限制：仍需在该玩家领地内
        PersonalDimensionManager.PersonalSpace sp = PersonalDimensionManager.getPlayerSpace(pid);
        if (sp == null || !sp.isInTerritory(pos)) return;

        // 快速重叠检查
        if (dIndex.hasNearby(pos, MIN_DISTANCE_FROM_DUNGEON)) return;
        if (vIndex.hasNearby(pos, 50)) return;
        if (isInAnyPlayerSpace(pos)) return;

        // 排程
        StructureType type = VoidStructureGenerator.selectRandomStructure();
        enqueue(new GenerationTask(world.provider.getDimension(), pos, type, pid));

        cnt.incrementAndGet();
        playerLastGen.put(pid, System.currentTimeMillis());
        System.out.println("[虚空结构] 排程给 " + player.getName() + " 的 " + type.name + " @ " + pos
                + " (" + cnt.get() + "/" + MAX_STRUCTURES_PER_PLAYER + ")");
    }

    private void scheduleDungeon(int dimId, BlockPos pos, long seed, UUID playerId) {
        // 先占坑，避免其他线程重复排程
        getDungeonIndex(dimId).add(pos);
        // 真正执行放到队列里（用特殊类型标识）
        enqueue(new GenerationTask(dimId, pos, StructureType.VOID_FORTRESS /*占位无关*/, playerId) {
            final long dungeonSeed = seed;
        });
    }

    private void enqueue(GenerationTask task) {
        if (QUEUE.size() >= GENERATION_QUEUE_MAX_SIZE) return;
        QUEUE.offer(task);
    }

    // ====== 队列处理（真正生成）======
    private void processQueue() {
        int budget = 3; // 每tick最多处理 3 个
        while (budget > 0 && !QUEUE.isEmpty()) {
            GenerationTask t = QUEUE.poll();
            if (t == null) break;
            if (!t.isValid()) continue;

            WorldServer w = DimensionManager.getWorld(t.dimId);
            if (w == null) continue;
            if (!PersonalDimensionManager.hasPlayersInDimension(w)) continue;
            if (!isPlayerInDimension(w, t.playerId)) continue;

            // 附近需有人
            if (!hasNearbyPlayer(w, t.pos, 500)) { QUEUE.offer(t); continue; }

            // 区块齐备检查（避免级联）
            if (!areChunksReady(w, t.pos, getEstimatedRadius(t.type))) { QUEUE.offer(t); continue; }

            // Dungeon 任务（用占位类型进队列时）——用实例检查 seed 字段
            if (t.getClass() != GenerationTask.class) {
                try {
                    long seed = (long) t.getClass().getDeclaredField("dungeonSeed").get(t);
                    generateDungeonInternal(w, t.pos, seed);
                    totalDungeonsGenerated.incrementAndGet();
                } catch (Throwable ex) {
                    // 回退：失败不再重排
                    System.err.println("[虚空结构] 地牢生成失败 @ " + t.pos);
                    ex.printStackTrace();
                }
                budget--;
                continue;
            }

            // Void 结构
            try {
                VoidStructureGenerator.generateStructureOptimized(w, t.pos, t.type);
                getVoidIndex(t.dimId).add(t.pos);
                int n = totalStructuresGenerated.incrementAndGet();
                if (n % 100 == 0) {
                    System.out.println("[虚空结构] 总结构数 = " + n);
                }
            } catch (Throwable ex) {
                System.err.println("[虚空结构] 生成失败 @ " + t.pos + " type=" + t.type);
                ex.printStackTrace();
            }
            budget--;
        }
    }

    // ====== 细节工具 ======
    private boolean areChunksReady(World w, BlockPos pos, int radius) {
        ChunkPos c = new ChunkPos(pos);
        int r = (radius >> 4) + 1;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!w.isChunkGeneratedAt(c.x + dx, c.z + dz)) return false;
            }
        }
        return true;
    }

    private int getEstimatedRadius(StructureType type) {
        switch (type) {
            case FLOATING_ISLAND:  return 15;
            case VOID_FORTRESS:    return 20;
            case VOID_BRIDGE:      return 30;
            case DIMENSIONAL_TEAR: return 25;
            default:               return 10;
        }
    }

    private Random seededGridRandom(World world, int chunkX, int chunkZ, UUID pid) {
        int gridX = Math.floorDiv(chunkX * 16, VOID_STRUCTURE_SPACING);
        int gridZ = Math.floorDiv(chunkZ * 16, VOID_STRUCTURE_SPACING);
        long seed = (long) gridX * 341873128712L
                + (long) gridZ * 132897987541L
                + world.getSeed()
                + pid.hashCode();
        return new Random(seed);
    }

    private float genChanceByDistance(double d) {
        if (d < 100) return 1.0f;
        if (d < 200) return 0.8f;
        if (d < 300) return 0.5f;
        if (d < 400) return 0.2f;
        return 0.0f;
    }

    private SpatialIndex getDungeonIndex(int dimId) { return DUNGEON_INDEX_BY_DIM.computeIfAbsent(dimId, k -> new SpatialIndex()); }
    private SpatialIndex getVoidIndex(int dimId)    { return VOID_INDEX_BY_DIM.computeIfAbsent(dimId, k -> new SpatialIndex()); }

    private EntityPlayer findNearestPlayer(World world, BlockPos pos, double maxRadius) {
        if (!(world instanceof WorldServer)) return null;
        EntityPlayer nearest = null; double best = maxRadius + 1;
        for (EntityPlayer p : ((WorldServer) world).playerEntities) {
            if (p.dimension != PersonalDimensionManager.PERSONAL_DIM_ID || p.isDead) continue;
            double d = p.getDistance(pos.getX(), pos.getY(), pos.getZ());
            if (d < best) { best = d; nearest = p; }
        }
        return nearest;
    }

    private boolean hasNearbyPlayer(World world, BlockPos pos, double r) {
        if (!(world instanceof WorldServer)) return false;
        for (EntityPlayer p : ((WorldServer) world).playerEntities) {
            if (p.dimension != PersonalDimensionManager.PERSONAL_DIM_ID || p.isDead) continue;
            if (p.getDistance(pos.getX(), pos.getY(), pos.getZ()) <= r) return true;
        }
        return false;
    }

    private boolean isPlayerInDimension(World world, UUID pid) {
        if (!(world instanceof WorldServer)) return false;
        EntityPlayer p = ((WorldServer) world).getMinecraftServer().getPlayerList().getPlayerByUUID(pid);
        return p != null && !p.isDead && p.dimension == PersonalDimensionManager.PERSONAL_DIM_ID;
    }

    /** 扩展边框，避免贴着房间外墙生成 */
    private boolean isInAnyPlayerSpace(BlockPos pos) {
        Collection<PersonalDimensionManager.PersonalSpace> spaces = PersonalDimensionManager.getAllSpaces();
        if (spaces.isEmpty()) return false;
        for (PersonalDimensionManager.PersonalSpace sp : spaces) {
            BlockPos min = sp.outerMinPos.add(-100, -50, -100);
            BlockPos max = sp.outerMaxPos.add(100, 50, 100);
            if (pos.getX() >= min.getX() && pos.getX() <= max.getX()
                    && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                    && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ()) {
                return true;
            }
        }
        return false;
    }

    // ====== Dungeon 生成（在 Tick 中真正执行）======
    private void generateDungeonInternal(World world, BlockPos pos, long seed) {
        try {
            DungeonLayoutGenerator gen = new DungeonLayoutGenerator();
            int size = 512;
            DungeonLayout layout = gen.generateLayout(pos, size, seed);
            new TreeDungeonPlacer(world).placeDungeon(layout);
            System.out.println("[虚空结构] 生成树状地牢 @ " + pos + "，房间数=" + layout.getRooms().size());
        } catch (Exception e) {
            System.err.println("[虚空结构] 地牢生成失败 @ " + pos);
            e.printStackTrace();
        }
    }

    // ====== 生命周期事件 ======
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
        if (e.getWorld().isRemote) return;
        if (e.getWorld().provider.getDimension() == PersonalDimensionManager.PERSONAL_DIM_ID) {
            getDungeonIndex(PersonalDimensionManager.PERSONAL_DIM_ID);
            getVoidIndex(PersonalDimensionManager.PERSONAL_DIM_ID);
            System.out.println("[虚空结构] 私人维度加载，索引预热完毕");
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload e) {
        if (e.getWorld().isRemote) return;
        int dimId = e.getWorld().provider.getDimension();

        SpatialIndex di = DUNGEON_INDEX_BY_DIM.remove(dimId);
        if (di != null) { System.out.println("[虚空结构] 卸载维度 " + dimId + "，清理地牢索引 " + di.size()); di.clear(); }

        SpatialIndex vi = VOID_INDEX_BY_DIM.remove(dimId);
        if (vi != null) { System.out.println("[虚空结构] 卸载维度 " + dimId + "，清理结构索引 " + vi.size()); vi.clear(); }

        // 清理该维度的队列任务
        QUEUE.removeIf(t -> t.dimId == dimId);
    }

    // ====== 清理/统计 API（对外保持不变）======
    public static void onServerStopping() {
        System.out.println("[虚空结构] 服务器停止，清理生成器缓存并反注册事件");
        DUNGEON_INDEX_BY_DIM.clear();
        VOID_INDEX_BY_DIM.clear();
        QUEUE.clear();
        playerStructureCount.clear();
        playerLastGen.clear();
        if (instanceRef != null) {
            VoidStructureWorldGenerator inst = instanceRef.get();
            if (inst != null) MinecraftForge.EVENT_BUS.unregister(inst);
            instanceRef = null;
        }
        eventRegistered = false;
        System.out.println("[虚空结构] 总结构: " + totalStructuresGenerated.get()
                + "，总地牢: " + totalDungeonsGenerated.get());
    }

    public static void clearAllData() {
        DUNGEON_INDEX_BY_DIM.clear();
        VOID_INDEX_BY_DIM.clear();
        QUEUE.clear();
        playerStructureCount.clear();
        playerLastGen.clear();
        lastCleanupTime = System.currentTimeMillis();
        System.out.println("[虚空结构] 手动清理所有结构数据");
    }

    public static String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("[虚空结构] 统计:\n");
        sb.append("  总生成结构: ").append(totalStructuresGenerated.get()).append("\n");
        sb.append("  总生成地牢: ").append(totalDungeonsGenerated.get()).append("\n");
        sb.append("  待生成队列: ").append(QUEUE.size()).append("\n");
        sb.append("  玩家结构计数: ").append(playerStructureCount.size()).append(" 名玩家\n");
        for (Map.Entry<UUID, AtomicInteger> e : playerStructureCount.entrySet()) {
            sb.append("    ").append(e.getKey()).append(": ").append(e.getValue().get()).append("\n");
        }
        for (Map.Entry<Integer, SpatialIndex> e : DUNGEON_INDEX_BY_DIM.entrySet()) {
            sb.append("  维度 ").append(e.getKey()).append(": 地牢索引 ").append(e.getValue().size()).append("\n");
        }
        for (Map.Entry<Integer, SpatialIndex> e : VOID_INDEX_BY_DIM.entrySet()) {
            sb.append("  维度 ").append(e.getKey()).append(": 结构索引 ").append(e.getValue().size()).append("\n");
        }
        Runtime rt = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
        sb.append("  内存使用: ").append(usedMB).append(" MB\n");
        return sb.toString();
    }

    // ====== 清理策略（轻/重）======
    private void lightCleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > CACHE_CLEANUP_INTERVAL / 2) {
            // 清理过期任务（不做重度对象清理）
            QUEUE.removeIf(t -> !t.isValid());
        }
    }

    private void hardCleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > CACHE_CLEANUP_INTERVAL) {
            lastCleanupTime = now;
            DUNGEON_INDEX_BY_DIM.values().forEach(i -> i.cleanup(MAX_CACHED_POSITIONS / 2));
            VOID_INDEX_BY_DIM.values().forEach(i -> i.cleanup(MAX_CACHED_POSITIONS));
            QUEUE.removeIf(t -> !t.isValid());

            // 清理长时间未生成的玩家节流数据
            playerLastGen.entrySet().removeIf(e -> now - e.getValue() > 3_600_000L);
            System.out.println("[虚空结构] 定期清理完成，队列=" + QUEUE.size());
        }
    }
}
