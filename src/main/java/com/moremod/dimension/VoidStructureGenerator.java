package com.moremod.dimension;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

/**
 * 虚空结构生成器 - 优化修复版
 * 修复了批量方块操作和内存管理问题
 */
public class VoidStructureGenerator {

    private static final Random random = new Random();
    private static final int BATCH_SIZE = 500;  // 批处理大小
    private static final int MAX_CHUNKS_PER_GENERATION = 20;  // 每次最多加载的区块数

    // 批量更新器
    private static class BatchBlockUpdater {
        private final Map<BlockPos, IBlockState> pendingBlocks = new HashMap<>();
        private final World world;

        public BatchBlockUpdater(World world) {
            this.world = world;
        }

        public void addBlock(BlockPos pos, IBlockState state) {
            pendingBlocks.put(pos, state);

            if (pendingBlocks.size() >= BATCH_SIZE) {
                flush();
            }
        }

        public void flush() {
            if (pendingBlocks.isEmpty()) return;

            // 按区块分组以优化更新
            Map<ChunkPos, List<Map.Entry<BlockPos, IBlockState>>> byChunk = new HashMap<>();

            for (Map.Entry<BlockPos, IBlockState> entry : pendingBlocks.entrySet()) {
                ChunkPos chunkPos = new ChunkPos(entry.getKey());
                byChunk.computeIfAbsent(chunkPos, k -> new ArrayList<>()).add(entry);
            }

            // 批量更新每个区块
            for (Map.Entry<ChunkPos, List<Map.Entry<BlockPos, IBlockState>>> chunkEntry : byChunk.entrySet()) {
                Chunk chunk = world.getChunk(chunkEntry.getKey().x, chunkEntry.getKey().z);

                for (Map.Entry<BlockPos, IBlockState> blockEntry : chunkEntry.getValue()) {
                    world.setBlockState(blockEntry.getKey(), blockEntry.getValue(), 2);
                }

                chunk.markDirty();
            }

            pendingBlocks.clear();
        }
    }

    // 结构类型枚举（权重仅相对，自动归一）
    public enum StructureType {
        // 原有结构
        FLOATING_ISLAND("浮空岛", 0.08),
        CRYSTAL_FORMATION("水晶阵", 0.06),
        ANCIENT_PLATFORM("远古平台", 0.05),
        VOID_BRIDGE("虚空桥", 0.04),
        TREASURE_VAULT("宝库", 0.03),
        GARDEN_SPHERE("花园球", 0.05),
        RUINED_TOWER("废弃塔", 0.06),
        ENERGY_CORE("能量核心", 0.04),
        MINING_OUTPOST("采矿前哨", 0.05),
        VOID_FORTRESS("虚空要塞", 0.03),
        CRYSTAL_GARDEN("水晶花园", 0.05),

        // 碎片化主题
        NETHER_SHARD("下界碎片", 0.06),
        END_FRAGMENT("末地碎片", 0.05),
        RUINED_PORTAL_CLUSTER("废弃传送门集群", 0.04),
        PRISMARINE_SHARD("海晶碎片", 0.04),
        GRAVITY_RIBBON("重力缎带", 0.02),
        RIFT_SPIRE("裂隙尖塔", 0.02),
        OVERWORLD_CHUNK("浮空原野块", 0.05),

        // 时空破碎主题
        TEMPORAL_FRACTURE("时间裂痕", 0.03),
        DIMENSIONAL_TEAR("维度撕裂点", 0.03),
        VOID_VORTEX("虚空漩涡", 0.02),
        SHATTERED_REALITY("破碎现实", 0.03),
        FROZEN_EXPLOSION("冻结爆炸", 0.02),
        INVERTED_RUINS("倒悬废墟", 0.03),
        REALITY_SPLICE("现实拼接", 0.02),
        CHAOS_NEXUS("混沌枢纽", 0.02),
        TIME_LOOP_FRAGMENT("时间循环残片", 0.02),
        VOID_CORAL("虚空珊瑚", 0.03),
        MIRROR_SHARD("镜像碎片", 0.02),
        QUANTUM_SCAFFOLD("量子脚手架", 0.02);

        public final String name;
        public final double chance;

        StructureType(String name, double chance) {
            this.name = name;
            this.chance = chance;
        }
    }

    /**
     * 优化版：在指定位置周围生成随机虚空结构
     */
    public static void generateNearbyStructuresOptimized(WorldServer world, BlockPos centerPos, int radius) {
        if (world.isRemote) return;

        int structureCount = 3 + random.nextInt(5); // 3-7个结构

        int minDist = Math.min(50, Math.max(16, radius / 4));
        int maxDist = Math.max(minDist + 16, radius);

        // 限制区块加载
        Set<ChunkPos> loadedChunks = new HashSet<>();

        for (int i = 0; i < structureCount; i++) {
            int distance = minDist + random.nextInt(Math.max(1, maxDist - minDist + 1));
            double angle = random.nextDouble() * Math.PI * 2;

            int x = centerPos.getX() + (int)(Math.cos(angle) * distance);
            int z = centerPos.getZ() + (int)(Math.sin(angle) * distance);
            int y = 96 + random.nextInt(64);

            BlockPos structurePos = new BlockPos(x, y, z);
            ChunkPos chunkPos = new ChunkPos(structurePos);

            // 限制加载的区块数量
            if (loadedChunks.size() < MAX_CHUNKS_PER_GENERATION) {
                loadedChunks.add(chunkPos);
                world.getChunk(chunkPos.x, chunkPos.z);

                StructureType type = selectRandomStructure();
                generateStructureOptimized(world, structurePos, type);

                System.out.println("[虚空结构] 生成 " + type.name + " at " + structurePos);
            }
        }
    }

    /**
     * 生成具体结构 - 优化版（防止级联世代）
     */
    public static void generateStructureOptimized(World world, BlockPos pos, StructureType type) {
        BatchBlockUpdater updater = new BatchBlockUpdater(world);

        // 检查是否安全生成（不会触发新区块加载）
        ChunkPos centerChunk = new ChunkPos(pos);
        int radius = getStructureRadius(type);

        // 计算结构会影响的区块范围
        int minChunkX = (pos.getX() - radius) >> 4;
        int maxChunkX = (pos.getX() + radius) >> 4;
        int minChunkZ = (pos.getZ() - radius) >> 4;
        int maxChunkZ = (pos.getZ() + radius) >> 4;

        // 如果结构只在单个区块内，直接生成
        if (minChunkX == maxChunkX && minChunkZ == maxChunkZ) {
            generateStructureInternal(world, pos, type, updater);
            updater.flush();
            return;
        }

        // 对于跨区块的结构，只生成当前已加载区块的部分
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!world.isChunkGeneratedAt(cx, cz)) {
                    // 如果有未加载的区块，推迟整个结构的生成
                    System.out.println("[虚空结构] 推迟生成 " + type.name + " @ " + pos + " (等待区块加载)");
                    return;
                }
            }
        }

        // 所有需要的区块都已加载，安全生成
        generateStructureInternal(world, pos, type, updater);
        updater.flush();
    }

    private static void generateStructureInternal(World world, BlockPos pos, StructureType type, BatchBlockUpdater updater) {
        switch (type) {
            case FLOATING_ISLAND:
                generateFloatingIslandOptimized(world, pos, updater);
                break;
            case CRYSTAL_FORMATION:
                generateCrystalFormationOptimized(world, pos, updater);
                break;
            case ANCIENT_PLATFORM:
                generateAncientPlatformOptimized(world, pos, updater);
                break;
            // ... 其他结构类型
            default:
                generateGenericStructure(world, pos, updater);
                break;
        }
    }

    private static int getStructureRadius(StructureType type) {
        // 返回每种结构类型的最大半径
        switch (type) {
            case FLOATING_ISLAND:
                return 15;
            case CRYSTAL_FORMATION:
                return 8;
            case ANCIENT_PLATFORM:
                return 6;
            case VOID_BRIDGE:
                return 30;
            case TREASURE_VAULT:
                return 5;
            case GARDEN_SPHERE:
                return 7;
            case RUINED_TOWER:
                return 5;
            case ENERGY_CORE:
                return 10;
            case MINING_OUTPOST:
                return 7;
            case VOID_FORTRESS:
                return 20;
            case CRYSTAL_GARDEN:
                return 6;
            case NETHER_SHARD:
                return 9;
            case END_FRAGMENT:
                return 9;
            case RUINED_PORTAL_CLUSTER:
                return 10;
            case PRISMARINE_SHARD:
                return 6;
            case GRAVITY_RIBBON:
                return 40;
            case RIFT_SPIRE:
                return 8;
            case OVERWORLD_CHUNK:
                return 10;
            case TEMPORAL_FRACTURE:
                return 15;
            case DIMENSIONAL_TEAR:
                return 25;
            case VOID_VORTEX:
                return 20;
            case SHATTERED_REALITY:
                return 15;
            case FROZEN_EXPLOSION:
                return 12;
            case INVERTED_RUINS:
                return 10;
            case REALITY_SPLICE:
                return 8;
            case CHAOS_NEXUS:
                return 15;
            case TIME_LOOP_FRAGMENT:
                return 8;
            case VOID_CORAL:
                return 10;
            case MIRROR_SHARD:
                return 10;
            case QUANTUM_SCAFFOLD:
                return 20;
            default:
                return 10;
        }
    }

    // ===================== 优化后的结构生成方法 =====================

    private static void generateFloatingIslandOptimized(World world, BlockPos center, BatchBlockUpdater updater) {
        int radius = 8 + random.nextInt(7);

        // 收集所有方块更改
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -3; y <= 2; y++) {
                    double dist = Math.sqrt(x*x + z*z + y*y*2);
                    if (dist <= radius) {
                        BlockPos p = center.add(x, y, z);
                        if (y < 0) {
                            updater.addBlock(p, Blocks.STONE.getDefaultState());
                            if (random.nextDouble() < 0.05) {
                                updater.addBlock(p, selectRandomOre());
                            }
                        } else if (y == 0) {
                            updater.addBlock(p, Blocks.GRASS.getDefaultState());
                        }
                    }
                }
            }
        }

        // 树木生成需要在flush后进行，因为需要检查地面
        updater.flush();

        // 生成树木
        for (int i = 0; i < radius/2; i++) {
            int x = center.getX() + random.nextInt(radius*2) - radius;
            int z = center.getZ() + random.nextInt(radius*2) - radius;
            BlockPos treePos = new BlockPos(x, center.getY() + 1, z);
            if (world.getBlockState(treePos.down()).getBlock() == Blocks.GRASS) {
                generateSmallTreeOptimized(world, treePos, updater);
            }
        }

        // 添加光源
        for (int i = 0; i < 3; i++) {
            BlockPos glowPos = center.add(
                    random.nextInt(radius) - radius/2, -2, random.nextInt(radius) - radius/2
            );
            updater.addBlock(glowPos, Blocks.GLOWSTONE.getDefaultState());
        }

        updater.flush();
    }

    private static void generateCrystalFormationOptimized(World world, BlockPos center, BatchBlockUpdater updater) {
        // 主水晶柱
        for (int y = 0; y < 8; y++) {
            updater.addBlock(center.up(y), Blocks.SEA_LANTERN.getDefaultState());
        }

        // 周围水晶
        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI * 2 * i) / 6;
            int x = (int)(Math.cos(angle) * 5);
            int z = (int)(Math.sin(angle) * 5);
            int h = 3 + random.nextInt(3);
            for (int y = 0; y < h; y++) {
                BlockPos p = center.add(x, y, z);
                IBlockState crystal = random.nextBoolean() ? Blocks.QUARTZ_BLOCK.getDefaultState()
                        : Blocks.PACKED_ICE.getDefaultState();
                updater.addBlock(p, crystal);
            }
        }

        // 底座
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (Math.abs(x) + Math.abs(z) <= 4) {
                    updater.addBlock(center.add(x, -1, z), Blocks.PURPUR_BLOCK.getDefaultState());
                }
            }
        }

        // 信标
        updater.addBlock(center.up(9), Blocks.BEACON.getDefaultState());

        updater.flush();
    }

    private static void generateAncientPlatformOptimized(World world, BlockPos center, BatchBlockUpdater updater) {
        // 平台
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                if (Math.abs(x) == 5 || Math.abs(z) == 5) {
                    updater.addBlock(center.add(x, 0, z), Blocks.OBSIDIAN.getDefaultState());
                    if (Math.abs(x) == 5 && Math.abs(z) == 5) {
                        for (int y = 1; y <= 4; y++) {
                            updater.addBlock(center.add(x, y, z), Blocks.OBSIDIAN.getDefaultState());
                        }
                        updater.addBlock(center.add(x, 5, z), Blocks.END_ROD.getDefaultState());
                    }
                } else {
                    updater.addBlock(center.add(x, 0, z), Blocks.STONE.getDefaultState());
                }
            }
        }

        updater.flush();

        // 箱子需要在flush后生成
        BlockPos chestPos = center.up(1);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
        TileEntityChest chest = (TileEntityChest) world.getTileEntity(chestPos);
        if (chest != null) {
            addRandomLoot(chest);
        }

        // 浮空光源
        for (int i = 0; i < 4; i++) {
            BlockPos floatPos = center.add(random.nextInt(11) - 5, 3 + random.nextInt(3), random.nextInt(11) - 5);
            updater.addBlock(floatPos, Blocks.GLOWSTONE.getDefaultState());
        }

        updater.flush();
    }

    private static void generateGenericStructure(World world, BlockPos center, BatchBlockUpdater updater) {
        // 通用简单结构
        int size = 3 + random.nextInt(3);

        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                if (Math.abs(x) + Math.abs(z) <= size) {
                    updater.addBlock(center.add(x, 0, z), Blocks.STONE.getDefaultState());
                }
            }
        }

        updater.addBlock(center.up(), Blocks.GLOWSTONE.getDefaultState());
        updater.flush();
    }

    // ===================== 核心辅助方法 =====================

    /**
     * 随机选择结构类型
     */
    public static StructureType selectRandomStructure() {
        double total = 0.0;
        for (StructureType t : StructureType.values()) {
            total += t.chance;
        }
        double roll = random.nextDouble() * total;
        double acc = 0.0;
        for (StructureType t : StructureType.values()) {
            acc += t.chance;
            if (roll <= acc) return t;
        }
        return StructureType.FLOATING_ISLAND; // 兜底
    }

    /**
     * 选择随机矿石
     */
    private static IBlockState selectRandomOre() {
        double r = random.nextDouble();
        if (r < 0.30) return Blocks.COAL_ORE.getDefaultState();
        if (r < 0.55) return Blocks.IRON_ORE.getDefaultState();
        if (r < 0.70) return Blocks.GOLD_ORE.getDefaultState();
        if (r < 0.85) return Blocks.REDSTONE_ORE.getDefaultState();
        if (r < 0.95) return Blocks.LAPIS_ORE.getDefaultState();
        return Blocks.DIAMOND_ORE.getDefaultState();
    }

    /**
     * 生成小树 - 优化版
     */
    private static void generateSmallTreeOptimized(World world, BlockPos pos, BatchBlockUpdater updater) {
        // 树干
        for (int y = 0; y < 4; y++) {
            updater.addBlock(pos.up(y), Blocks.LOG.getDefaultState());
        }

        // 树叶
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 3; y <= 5; y++) {
                    if (Math.abs(x) + Math.abs(z) <= 3 - (y - 3)) {
                        BlockPos leaf = pos.add(x, y, z);
                        if (world.getBlockState(leaf).getBlock() == Blocks.AIR) {
                            updater.addBlock(leaf, Blocks.LEAVES.getDefaultState());
                        }
                    }
                }
            }
        }
    }

    /**
     * 添加随机战利品到箱子
     */
    private static void addRandomLoot(TileEntityChest chest) {
        for (int i = 0; i < 3 + random.nextInt(5); i++) {
            ItemStack loot = selectRandomLoot();
            chest.setInventorySlotContents(i, loot);
        }
    }

    /**
     * 选择随机战利品
     */
    private static ItemStack selectRandomLoot() {
        ItemStack[] loot = {
                new ItemStack(Items.DIAMOND, 1 + random.nextInt(3)),
                new ItemStack(Items.EMERALD, 2 + random.nextInt(5)),
                new ItemStack(Items.GOLDEN_APPLE, 1),
                new ItemStack(Items.ENDER_PEARL, 4 + random.nextInt(12)),
                new ItemStack(Items.EXPERIENCE_BOTTLE, 8 + random.nextInt(16)),
                new ItemStack(Items.ENCHANTED_BOOK, 1),
                new ItemStack(Items.TOTEM_OF_UNDYING, 1),
                new ItemStack(Blocks.BEACON, 1),
                new ItemStack(Items.ELYTRA, 1)
        };
        return loot[random.nextInt(loot.length)];
    }

    /**
     * 添加采矿战利品
     */
    private static void addMiningLoot(TileEntityChest chest) {
        ItemStack[] loot = {
                new ItemStack(Items.DIAMOND_PICKAXE, 1),
                new ItemStack(Items.IRON_INGOT, 16 + random.nextInt(16)),
                new ItemStack(Items.GOLD_INGOT, 8 + random.nextInt(8)),
                new ItemStack(Items.REDSTONE, 32 + random.nextInt(32)),
                new ItemStack(Blocks.TNT, 4 + random.nextInt(4)),
                new ItemStack(Items.ENDER_PEARL, 2 + random.nextInt(4)),
                new ItemStack(Items.NETHER_STAR, 1)
        };
        for (int i = 0; i < 3 + random.nextInt(3); i++) {
            chest.setInventorySlotContents(i, loot[random.nextInt(loot.length)]);
        }
    }

    /**
     * 选择随机花朵
     */
    private static IBlockState selectRandomFlower() {
        IBlockState[] flowers = {
                Blocks.YELLOW_FLOWER.getDefaultState(),
                Blocks.RED_FLOWER.getDefaultState()
        };
        return flowers[random.nextInt(flowers.length)];
    }

    /**
     * 生成小平台 - 优化版
     */
    private static void generateSmallPlatformOptimized(World world, BlockPos center, int radius, BatchBlockUpdater updater) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (Math.abs(x) + Math.abs(z) <= radius) {
                    updater.addBlock(center.add(x, 0, z), Blocks.STONEBRICK.getDefaultState());
                }
            }
        }
    }

    // 内存管理：定期清理
    private static long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL = 60000; // 1分钟

    public static void performCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > CLEANUP_INTERVAL) {
            // 清理任何缓存的数据
            random.setSeed(System.currentTimeMillis());
            lastCleanupTime = now;
        }
    }
}