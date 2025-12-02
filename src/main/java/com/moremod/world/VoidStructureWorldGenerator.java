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

public class VoidStructureWorldGenerator implements IWorldGenerator {

    private static final int DUNGEON_SPACING = 500;
    private static final int VOID_STRUCTURE_SPACING = 128;
    private static final int MIN_DISTANCE_FROM_DUNGEON = 200;
    private static final int MAX_CACHED_POSITIONS = 1000;
    private static final long CACHE_CLEANUP_INTERVAL = 300_000L;
    private static final int GENERATION_QUEUE_MAX_SIZE = 256;

    private static final int PLAYER_GENERATION_RADIUS = 400;
    private static final int MAX_STRUCTURES_PER_PLAYER = 50;
    private static final long PLAYER_COOLDOWN_MS = 5_000L;

    private static final int MAX_RETRY_COUNT = 10;
    private static final int TICK_BUDGET = 5;
    private static final int MAX_REQUEUE_PER_TICK = 20;

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

    private static class DungeonTask extends GenerationTask {
        final long dungeonSeed;
        DungeonTask(int dimId, BlockPos pos, long seed, UUID playerId) {
            super(dimId, pos, null, playerId);
            this.dungeonSeed = seed;
        }
    }

    private static class GenerationTask {
        final int dimId;
        final BlockPos pos;
        final StructureType type;
        final UUID playerId;
        final long ts;
        int retryCount = 0;
        GenerationTask(int dimId, BlockPos pos, StructureType type, UUID playerId) {
            this.dimId = dimId;
            this.pos = pos;
            this.type = type;
            this.playerId = playerId;
            this.ts = System.currentTimeMillis();
        }

        boolean isValid() {
            return System.currentTimeMillis() - ts < 60_000L && retryCount < MAX_RETRY_COUNT;
        }
        boolean isDungeon() { return this instanceof DungeonTask; }
    }

    private static final Map<Integer, SpatialIndex> DUNGEON_INDEX_BY_DIM = new ConcurrentHashMap<>();
    private static final Map<Integer, SpatialIndex> VOID_INDEX_BY_DIM = new ConcurrentHashMap<>();
    private static final Queue<GenerationTask> QUEUE = new ConcurrentLinkedQueue<>();
    private static final Map<UUID, AtomicInteger> playerStructureCount = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> playerLastGen = new ConcurrentHashMap<>();
    private static final AtomicInteger totalStructuresGenerated = new AtomicInteger(0);
    private static final AtomicInteger totalDungeonsGenerated = new AtomicInteger(0);
    private static long lastCleanupTime = System.currentTimeMillis();
    private static boolean eventRegistered = false;
    private static WeakReference<VoidStructureWorldGenerator> instanceRef;

    public VoidStructureWorldGenerator() {
        if (!eventRegistered) {
            MinecraftForge.EVENT_BUS.register(this);
            eventRegistered = true;
            instanceRef = new WeakReference<>(this);
        }
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.isRemote) return;
        if (world.provider.getDimension() != PersonalDimensionManager.PERSONAL_DIM_ID) return;
        if (!PersonalDimensionManager.hasPlayersInDimension(world)) return;
        lightCleanupIfNeeded();

        BlockPos chunkPos = new BlockPos(chunkX * 16 + 8, 100, chunkZ * 16 + 8);
        EntityPlayer nearest = findNearestPlayer(world, chunkPos, PLAYER_GENERATION_RADIUS);
        if (nearest == null) return;
        UUID owner = PersonalDimensionManager.findSpaceOwner(chunkPos);
        if (owner != null) {
            if (!isPlayerInDimension(world, owner)) return;
            PersonalDimensionManager.PersonalSpace sp = PersonalDimensionManager.getPlayerSpace(owner);
            if (sp != null && !sp.isInTerritory(chunkPos)) return;
        }

        SpatialIndex dIndex = getDungeonIndex(world.provider.getDimension());
        SpatialIndex vIndex = getVoidIndex(world.provider.getDimension());
        if (isInAnyPlayerSpace(chunkPos)) return;

        if ((chunkX % 4) == 0 && (chunkZ % 4) == 0 && random.nextInt(5) == 0) {
            if (!dIndex.hasNearby(chunkPos, DUNGEON_SPACING)) {
                scheduleDungeon(world.provider.getDimension(), chunkPos, random.nextLong(), nearest.getUniqueID());
            }
        }

        scheduleVoidForPlayer(world, chunkX, chunkZ, nearest, dIndex, vIndex);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        processQueue();
        hardCleanupIfNeeded();
    }

    private void processQueue() {
        int budget = TICK_BUDGET;
        int requeueCount = 0;
        while (budget > 0 && !QUEUE.isEmpty()) {
            GenerationTask t = QUEUE.poll();
            if (t == null) break;
            if (!t.isValid()) continue;

            WorldServer w = DimensionManager.getWorld(t.dimId);
            if (w == null) continue;
            if (!PersonalDimensionManager.hasPlayersInDimension(w)) continue;
            boolean shouldRequeue = false;
            if (!hasNearbyPlayer(w, t.pos, 500)) shouldRequeue = true;
            else if (!areChunksReady(w, t.pos, getEstimatedRadius(t))) shouldRequeue = true;

            if (shouldRequeue) {
                t.retryCount++;
                if (requeueCount < MAX_REQUEUE_PER_TICK && t.isValid()) {
                    QUEUE.offer(t);
                    requeueCount++;
                }
                budget--;
                continue;
            }

            try {
                if (t.isDungeon()) {
                    DungeonTask dt = (DungeonTask) t;
                    generateDungeonInternal(w, dt.pos, dt.dungeonSeed);
                    getDungeonIndex(t.dimId).add(t.pos);
                    totalDungeonsGenerated.incrementAndGet();
                } else {
                    VoidStructureGenerator.generateStructureOptimized(w, t.pos, t.type);
                    getVoidIndex(t.dimId).add(t.pos);
                    totalStructuresGenerated.incrementAndGet();
                }
            } catch (Throwable ignored) {}
            budget--;
        }
    }

    private void scheduleVoidForPlayer(World world, int chunkX, int chunkZ, EntityPlayer player,
                                       SpatialIndex dIndex, SpatialIndex vIndex) {
        UUID pid = player.getUniqueID();
        AtomicInteger cnt = playerStructureCount.computeIfAbsent(pid, k -> new AtomicInteger(0));
        if (cnt.get() >= MAX_STRUCTURES_PER_PLAYER) return;
        Long last = playerLastGen.get(pid);
        if (last != null && System.currentTimeMillis() - last < PLAYER_COOLDOWN_MS) return;

        BlockPos center = new BlockPos(chunkX * 16 + 8, 100, chunkZ * 16 + 8);
        double dist = player.getDistance(center.getX(), center.getY(), center.getZ());
        float chance = genChanceByDistance(dist);
        Random gridRng = seededGridRandom(world, chunkX, chunkZ, pid);
        if (gridRng.nextFloat() >= 0.3f * chance) return;

        int offsetX = gridRng.nextInt(VOID_STRUCTURE_SPACING / 2) - VOID_STRUCTURE_SPACING / 4;
        int offsetZ = gridRng.nextInt(VOID_STRUCTURE_SPACING / 2) - VOID_STRUCTURE_SPACING / 4;
        int baseY = 80 + gridRng.nextInt(80);
        BlockPos pos = new BlockPos(center.getX() + offsetX, baseY, center.getZ() + offsetZ);
        PersonalDimensionManager.PersonalSpace sp = PersonalDimensionManager.getPlayerSpace(pid);
        if (sp == null || !sp.isInTerritory(pos)) return;
        if (dIndex.hasNearby(pos, MIN_DISTANCE_FROM_DUNGEON)) return;
        if (vIndex.hasNearby(pos, 50)) return;
        if (isInAnyPlayerSpace(pos)) return;

        StructureType type = VoidStructureGenerator.selectRandomStructure();
        enqueue(new GenerationTask(world.provider.getDimension(), pos, type, pid));
        cnt.incrementAndGet();
        playerLastGen.put(pid, System.currentTimeMillis());
    }

    private void scheduleDungeon(int dimId, BlockPos pos, long seed, UUID playerId) {
        getDungeonIndex(dimId).add(pos);
        enqueue(new DungeonTask(dimId, pos, seed, playerId));
    }

    private void enqueue(GenerationTask task) {
        if (QUEUE.size() >= GENERATION_QUEUE_MAX_SIZE) return;
        QUEUE.offer(task);
    }

    private boolean areChunksReady(World w, BlockPos pos, int radius) {
        ChunkPos c = new ChunkPos(pos);
        int r = (radius >> 4) + 1;
        r = Math.min(r, 1);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!w.isChunkGeneratedAt(c.x + dx, c.z + dz)) return false;
            }
        }
        return true;
    }

    private int getEstimatedRadius(GenerationTask task) {
        if (task.isDungeon()) return 15;
        if (task.type == null) return 10;
        switch (task.type) {
            case FLOATING_ISLAND: return 15;
            case VOID_FORTRESS: return 20;
            case VOID_BRIDGE: return 30;
            case DIMENSIONAL_TEAR: return 25;
            default: return 10;
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

    private SpatialIndex getDungeonIndex(int dimId) {
        return DUNGEON_INDEX_BY_DIM.computeIfAbsent(dimId, k -> new SpatialIndex());
    }

    private SpatialIndex getVoidIndex(int dimId) {
        return VOID_INDEX_BY_DIM.computeIfAbsent(dimId, k -> new SpatialIndex());
    }

    private EntityPlayer findNearestPlayer(World world, BlockPos pos, double maxRadius) {
        if (!(world instanceof WorldServer)) return null;
        EntityPlayer nearest = null;
        double best = maxRadius + 1;
        for (EntityPlayer p : ((WorldServer) world).playerEntities) {
            if (p.dimension != PersonalDimensionManager.PERSONAL_DIM_ID || p.isDead) continue;
            double d = p.getDistance(pos.getX(), pos.getY(), pos.getZ());
            if (d < best) {
                best = d;
                nearest = p;
            }
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

    private void generateDungeonInternal(World world, BlockPos pos, long seed) {
        try {
            System.out.println("[地牢生成] 开始生成地牢于 " + pos + ", seed=" + seed);
            DungeonLayoutGenerator gen = new DungeonLayoutGenerator();
            int size = 512;
            DungeonLayout layout = gen.generateLayout(pos, size, seed);
            System.out.println("[地牢生成] 布局生成完成，房间数: " + layout.getRooms().size());
            new TreeDungeonPlacer(world).placeDungeon(layout);
            System.out.println("[地牢生成] 地牢放置完成");
        } catch (Exception e) {
            System.err.println("[地牢生成] 生成失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
        if (e.getWorld().isRemote) return;
        if (e.getWorld().provider.getDimension() == PersonalDimensionManager.PERSONAL_DIM_ID) {
            getDungeonIndex(PersonalDimensionManager.PERSONAL_DIM_ID);
            getVoidIndex(PersonalDimensionManager.PERSONAL_DIM_ID);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload e) {
        if (e.getWorld().isRemote) return;
        int dimId = e.getWorld().provider.getDimension();
        SpatialIndex di = DUNGEON_INDEX_BY_DIM.remove(dimId);
        if (di != null) di.clear();
        SpatialIndex vi = VOID_INDEX_BY_DIM.remove(dimId);
        if (vi != null) vi.clear();
        QUEUE.removeIf(t -> t.dimId == dimId);
    }

    public static void onServerStopping() {
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
    }

    public static void clearAllData() {
        DUNGEON_INDEX_BY_DIM.clear();
        VOID_INDEX_BY_DIM.clear();
        QUEUE.clear();
        playerStructureCount.clear();
        playerLastGen.clear();
        lastCleanupTime = System.currentTimeMillis();
    }

    public static String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("[虚空结构] 统计:\n");
        sb.append("  总结构: ").append(totalStructuresGenerated.get()).append("\n");
        sb.append("  总地牢: ").append(totalDungeonsGenerated.get()).append("\n");
        sb.append("  队列大小: ").append(QUEUE.size()).append("\n");
        sb.append("  玩家数: ").append(playerStructureCount.size()).append("\n");
        for (Map.Entry<UUID, AtomicInteger> e : playerStructureCount.entrySet()) {
            sb.append("    ").append(e.getKey()).append(": ").append(e.getValue().get()).append("\n");
        }
        return sb.toString();
    }

    private void lightCleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > CACHE_CLEANUP_INTERVAL / 2) {
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
            playerLastGen.entrySet().removeIf(e -> now - e.getValue() > 3_600_000L);
        }
    }
}
