package com.moremod.world;

import com.moremod.dimension.VoidStructureGenerator;
import com.moremod.dimension.VoidStructureGenerator.StructureType;
import com.moremod.dungeon.DungeonLayoutGenerator;
import com.moremod.dungeon.DungeonTypes.DungeonLayout;
import com.moremod.dungeon.TreeDungeonPlacer;
import com.moremod.dimension.PersonalDimensionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VoidStructureWorldGenerator implements IWorldGenerator {

    private static final int DUNGEON_SPACING = 500;
    private static final int VOID_STRUCTURE_SPACING = 128; // 虚空结构间距
    private static final int MIN_DISTANCE_FROM_DUNGEON = 200; // 与地牢的最小距离

    private static final Map<Integer, Set<BlockPos>> PLACED_DUNGEONS_BY_DIM = new ConcurrentHashMap<>();
    private static final Map<Integer, Set<BlockPos>> PLACED_VOID_STRUCTURES_BY_DIM = new ConcurrentHashMap<>();
    private static boolean eventRegistered = false;

    public VoidStructureWorldGenerator() {
        if (!eventRegistered) {
            MinecraftForge.EVENT_BUS.register(this);
            eventRegistered = true;
            System.out.println("[虚空结构] 世界生成器初始化");
        }
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {

        if (world.provider.getDimension() != PersonalDimensionManager.PERSONAL_DIM_ID) return;

        Set<BlockPos> placedDungeons = getPlacedDungeonsForDimension(world.provider.getDimension());
        Set<BlockPos> placedVoidStructures = getPlacedVoidStructuresForDimension(world.provider.getDimension());

        BlockPos worldCenter = new BlockPos(0, 64, 0);
        BlockPos chunkPos = new BlockPos(chunkX * 16, 100, chunkZ * 16);

        double distanceFromCenter = chunkPos.getDistance(worldCenter.getX(), worldCenter.getY(), worldCenter.getZ());
        if (distanceFromCenter < 300) return;

        if (isInPlayerSpace(chunkPos)) return;

        // 生成地牢的逻辑
        if ((chunkX % 8) == 0 && (chunkZ % 8) == 0 && random.nextInt(20) == 0) {
            if (canPlaceDungeon(chunkPos, placedDungeons)) {
                generateDungeon(world, chunkPos, random.nextLong(), placedDungeons);
            }
        }

        // 生成虚空结构的逻辑
        generateVoidStructures(world, chunkX, chunkZ, random, placedDungeons, placedVoidStructures);
    }

    private void generateVoidStructures(World world, int chunkX, int chunkZ, Random random,
                                        Set<BlockPos> placedDungeons, Set<BlockPos> placedVoidStructures) {

        // 使用网格系统生成虚空结构
        int gridX = Math.floorDiv(chunkX * 16, VOID_STRUCTURE_SPACING);
        int gridZ = Math.floorDiv(chunkZ * 16, VOID_STRUCTURE_SPACING);

        // 基于网格坐标生成唯一种子
        long gridSeed = (long) gridX * 341873128712L + (long) gridZ * 132897987541L + world.getSeed();
        Random gridRandom = new Random(gridSeed);

        // 每个网格单元有概率生成虚空结构
        if (gridRandom.nextFloat() < 0.3f) { // 30%概率在每个网格生成
            // 在网格内随机偏移位置
            int offsetX = gridRandom.nextInt(VOID_STRUCTURE_SPACING / 2) - VOID_STRUCTURE_SPACING / 4;
            int offsetZ = gridRandom.nextInt(VOID_STRUCTURE_SPACING / 2) - VOID_STRUCTURE_SPACING / 4;
            int baseY = 80 + gridRandom.nextInt(80); // Y坐标在80-160之间

            BlockPos structurePos = new BlockPos(
                    gridX * VOID_STRUCTURE_SPACING + offsetX,
                    baseY,
                    gridZ * VOID_STRUCTURE_SPACING + offsetZ
            );

            // 检查是否在当前chunk范围内
            if (!isInChunk(structurePos, chunkX, chunkZ)) return;

            // 检查是否与地牢太近
            if (isTooCloseToAnyDungeon(structurePos, placedDungeons)) return;

            // 检查是否在玩家空间内
            if (isInPlayerSpace(structurePos)) return;

            // 检查是否与其他虚空结构重叠
            if (isTooCloseToAnyVoidStructure(structurePos, placedVoidStructures)) return;

            // 生成虚空结构
            generateSingleVoidStructure(world, structurePos, gridRandom, placedVoidStructures);
        }

        // 额外的小型结构群落生成（较低概率）
        if (random.nextFloat() < 0.05f) { // 5%概率生成结构群
            BlockPos clusterCenter = new BlockPos(chunkX * 16 + 8, 100 + random.nextInt(40), chunkZ * 16 + 8);

            if (!isTooCloseToAnyDungeon(clusterCenter, placedDungeons) && !isInPlayerSpace(clusterCenter)) {
                generateStructureCluster(world, clusterCenter, random, placedVoidStructures, placedDungeons);
            }
        }
    }

    private void generateSingleVoidStructure(World world, BlockPos pos, Random random, Set<BlockPos> placedStructures) {
        try {
            StructureType type = VoidStructureGenerator.selectRandomStructure();
            VoidStructureGenerator.generateStructure(world, pos, type);

            synchronized (placedStructures) {
                placedStructures.add(pos);
            }

            System.out.println("[虚空结构] 生成 " + type.name + " @ " + pos);
        } catch (Exception e) {
            System.err.println("[虚空结构] 生成失败 @ " + pos);
            e.printStackTrace();
        }
    }

    private void generateStructureCluster(World world, BlockPos center, Random random,
                                          Set<BlockPos> placedStructures, Set<BlockPos> placedDungeons) {
        int structureCount = 2 + random.nextInt(4); // 2-5个结构
        int clusterRadius = 60 + random.nextInt(40); // 60-100格半径

        for (int i = 0; i < structureCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int distance = 30 + random.nextInt(clusterRadius - 30);

            int x = center.getX() + (int)(Math.cos(angle) * distance);
            int z = center.getZ() + (int)(Math.sin(angle) * distance);
            int y = 80 + random.nextInt(60);

            BlockPos structurePos = new BlockPos(x, y, z);

            // 检查所有限制条件
            if (!isTooCloseToAnyDungeon(structurePos, placedDungeons) &&
                    !isTooCloseToAnyVoidStructure(structurePos, placedStructures) &&
                    !isInPlayerSpace(structurePos)) {

                generateSingleVoidStructure(world, structurePos, random, placedStructures);
            }
        }
    }

    private boolean isInChunk(BlockPos pos, int chunkX, int chunkZ) {
        int posChunkX = pos.getX() >> 4;
        int posChunkZ = pos.getZ() >> 4;
        return posChunkX == chunkX && posChunkZ == chunkZ;
    }

    private boolean isTooCloseToAnyDungeon(BlockPos pos, Set<BlockPos> dungeons) {
        synchronized (dungeons) {
            for (BlockPos dungeonPos : dungeons) {
                if (pos.getDistance(dungeonPos.getX(), dungeonPos.getY(), dungeonPos.getZ()) < MIN_DISTANCE_FROM_DUNGEON) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTooCloseToAnyVoidStructure(BlockPos pos, Set<BlockPos> structures) {
        synchronized (structures) {
            for (BlockPos structurePos : structures) {
                if (pos.getDistance(structurePos.getX(), structurePos.getY(), structurePos.getZ()) < 50) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<BlockPos> getPlacedVoidStructuresForDimension(int dimensionId) {
        return PLACED_VOID_STRUCTURES_BY_DIM.computeIfAbsent(dimensionId,
                k -> Collections.synchronizedSet(new HashSet<>()));
    }

    private boolean isInPlayerSpace(BlockPos pos) {
        for (PersonalDimensionManager.PersonalSpace space : PersonalDimensionManager.getAllSpaces()) {
            BlockPos expandedMin = space.outerMinPos.add(-100, -50, -100);
            BlockPos expandedMax = space.outerMaxPos.add(100, 50, 100);
            if (pos.getX() >= expandedMin.getX() && pos.getX() <= expandedMax.getX() &&
                    pos.getY() >= expandedMin.getY() && pos.getY() <= expandedMax.getY() &&
                    pos.getZ() >= expandedMin.getZ() && pos.getZ() <= expandedMax.getZ()) {
                return true;
            }
        }
        return false;
    }

    private Set<BlockPos> getPlacedDungeonsForDimension(int dimensionId) {
        return PLACED_DUNGEONS_BY_DIM.computeIfAbsent(dimensionId,
                k -> Collections.synchronizedSet(new HashSet<>()));
    }

    private boolean canPlaceDungeon(BlockPos pos, Set<BlockPos> placedDungeons) {
        synchronized (placedDungeons) {
            for (BlockPos existing : placedDungeons) {
                if (pos.getDistance(existing.getX(), existing.getY(), existing.getZ()) < DUNGEON_SPACING) {
                    return false;
                }
            }
        }
        return true;
    }

    private void generateDungeon(World world, BlockPos pos, long seed, Set<BlockPos> placedDungeons) {
        try {
            DungeonLayoutGenerator gen = new DungeonLayoutGenerator();
            int dungeonSize = 512;
            DungeonLayout layout = gen.generateLayout(pos, dungeonSize, seed);

            TreeDungeonPlacer placer = new TreeDungeonPlacer(world);
            placer.placeDungeon(layout);

            synchronized (placedDungeons) {
                placedDungeons.add(pos);
            }
            System.out.println("[虚空结构] 生成树状地牢 @ " + pos + "，房间数: " + layout.getRooms().size());
        } catch (Exception e) {
            System.err.println("[虚空结构] 生成地牢失败 @ " + pos);
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld().isRemote) return;
        if (event.getWorld().provider.getDimension() == PersonalDimensionManager.PERSONAL_DIM_ID) {
            System.out.println("[虚空结构] 私人维度加载，准备结构生成");
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) return;
        int dimId = event.getWorld().provider.getDimension();

        // 清理地牢记录
        if (PLACED_DUNGEONS_BY_DIM.containsKey(dimId)) {
            Set<BlockPos> dungeons = PLACED_DUNGEONS_BY_DIM.get(dimId);
            if (dungeons != null) {
                System.out.println("[虚空结构] 清理维度 " + dimId + " 的 " + dungeons.size() + " 个地牢记录");
                dungeons.clear();
            }
        }

        // 清理虚空结构记录
        if (PLACED_VOID_STRUCTURES_BY_DIM.containsKey(dimId)) {
            Set<BlockPos> structures = PLACED_VOID_STRUCTURES_BY_DIM.get(dimId);
            if (structures != null) {
                System.out.println("[虚空结构] 清理维度 " + dimId + " 的 " + structures.size() + " 个虚空结构记录");
                structures.clear();
            }
        }
    }

    public static void onServerStopping() {
        System.out.println("[虚空结构] 服务器关闭，清理所有结构数据");
        PLACED_DUNGEONS_BY_DIM.clear();
        PLACED_VOID_STRUCTURES_BY_DIM.clear();
    }

    public static void clearAllData() {
        PLACED_DUNGEONS_BY_DIM.clear();
        PLACED_VOID_STRUCTURES_BY_DIM.clear();
        System.out.println("[虚空结构] 手动清理所有结构数据");
    }

    public static String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("[虚空结构] 统计:\n");
        for (Map.Entry<Integer, Set<BlockPos>> entry : PLACED_DUNGEONS_BY_DIM.entrySet()) {
            sb.append("  维度 ").append(entry.getKey())
                    .append(": ").append(entry.getValue().size())
                    .append(" 个地牢\n");
        }
        for (Map.Entry<Integer, Set<BlockPos>> entry : PLACED_VOID_STRUCTURES_BY_DIM.entrySet()) {
            sb.append("  维度 ").append(entry.getKey())
                    .append(": ").append(entry.getValue().size())
                    .append(" 个虚空结构\n");
        }
        return sb.toString();
    }
}