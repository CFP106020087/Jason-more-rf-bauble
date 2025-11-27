package com.moremod.util;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * BlockSniffer 风格的扫描器 - Minecraft 1.12.2 完整版
 * ✅ 添加了统一的 API 方法供 ItemExplorerCompass 调用
 */
public class CompassScanner {

    private static final int[][] CHUNK_SPIRAL;

    static {
        CHUNK_SPIRAL = generateSpiralOrder(15);
    }

    private static int[][] generateSpiralOrder(int maxRadius) {
        java.util.List<int[]> order = new java.util.ArrayList<>();
        order.add(new int[]{0, 0});

        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                order.add(new int[]{x, radius});
            }
            for (int z = radius - 1; z >= -radius; z--) {
                order.add(new int[]{radius, z});
            }
            for (int x = radius - 1; x >= -radius; x--) {
                order.add(new int[]{x, -radius});
            }
            for (int z = -radius + 1; z < radius; z++) {
                order.add(new int[]{-radius, z});
            }
        }

        return order.toArray(new int[0][]);
    }

    public static class ScanConfig {
        public int hRange = 4;
        public int vRange = 32;
        public int scanMode = 0;
        public int depthMin = 0;
        public int depthMax = 64;

        public int getYMin(BlockPos playerPos) {
            return scanMode == 0 ? depthMin : playerPos.getY() - vRange;
        }

        public int getYMax(BlockPos playerPos) {
            return scanMode == 0 ? depthMax : playerPos.getY() + vRange;
        }
    }

    // ===== 统一 API 方法 - 供 ItemExplorerCompass 调用 =====

    /**
     * 查找最近的村庄
     */
    public static BlockPos findNearestVillage(World world, BlockPos playerPos, ScanConfig config) {
        EntityPlayer player = world.getClosestPlayer(playerPos.getX(), playerPos.getY(), playerPos.getZ(), -1, false);
        if (player == null) return null;
        return scanForVillage(player, config);
    }

    /**
     * 查找最近的地牢
     */
    public static BlockPos findNearestDungeon(World world, BlockPos playerPos, ScanConfig config) {
        EntityPlayer player = world.getClosestPlayer(playerPos.getX(), playerPos.getY(), playerPos.getZ(), -1, false);
        if (player == null) return null;
        return scanForDungeon(player, config);
    }

    /**
     * 查找最近的箱子
     */
    public static BlockPos findNearestChest(World world, BlockPos playerPos, ScanConfig config) {
        EntityPlayer player = world.getClosestPlayer(playerPos.getX(), playerPos.getY(), playerPos.getZ(), -1, false);
        if (player == null) return null;
        return scanForChest(player, config);
    }

    /**
     * 查找最近的 Waystone (传送石碑)
     */
    public static BlockPos findNearestWaystone(World world, BlockPos playerPos, ScanConfig config) {
        EntityPlayer player = world.getClosestPlayer(playerPos.getX(), playerPos.getY(), playerPos.getZ(), -1, false);
        if (player == null) return null;

        // Waystone 模组的方块注册名
        // 尝试常见的 Waystone 方块
        String[] waystoneNames = {
                "waystones:waystone",
                "waystones:mossy_waystone",
                "waystones:sandy_waystone"
        };

        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (String blockName : waystoneNames) {
            BlockPos pos = scanForBlock(player, config, blockName);
            if (pos != null) {
                double distance = playerPos.distanceSq(pos);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = pos;
                }
            }
        }

        return nearest;
    }

    // ===== 村庄扫描 - 新增功能 =====

    /**
     * 扫描村庄结构
     * 通过检测村庄特征方块（门、床、工作站等）来定位村庄
     */
    public static BlockPos scanForVillage(EntityPlayer player, ScanConfig config) {
        World world = player.world;
        BlockPos playerPos = player.getPosition();

        int yMin = Math.max(0, config.getYMin(playerPos));
        int yMax = Math.min(255, config.getYMax(playerPos));

        BlockPos nearestVillage = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int i = 0; i < CHUNK_SPIRAL.length; i++) {
            int[] offset = CHUNK_SPIRAL[i];

            if (Math.abs(offset[0]) > config.hRange || Math.abs(offset[1]) > config.hRange) {
                continue;
            }

            int chunkX = (int)Math.floor(player.posX / 16) + offset[0];
            int chunkZ = (int)Math.floor(player.posZ / 16) + offset[1];

            if (!world.isChunkGeneratedAt(chunkX, chunkZ)) {
                continue;
            }

            Chunk chunk = world.getChunk(chunkX, chunkZ);

            if (chunk.isEmpty()) {
                continue;
            }

            BlockPos chunkResult = scanChunkForVillage(chunk, yMin, yMax, player);
            if (chunkResult != null) {
                double distance = playerPos.distanceSq(chunkResult);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestVillage = chunkResult;

                    // 如果找到很近的村庄，直接返回
                    if (distance < 400) {
                        return nearestVillage;
                    }
                }
            }
        }

        return nearestVillage;
    }

    /**
     * 在区块中扫描村庄特征方块
     */
    private static BlockPos scanChunkForVillage(Chunk chunk, int yMin, int yMax, EntityPlayer player) {
        World world = player.world;
        BlockPos playerPos = player.getPosition();

        BlockPos nearestInChunk = null;
        double nearestDistance = Double.MAX_VALUE;
        int villageBlockCount = 0;

        int chunkBaseX = chunk.x << 4;
        int chunkBaseZ = chunk.z << 4;

        for (int y = yMax; y >= yMin; y--) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos localPos = new BlockPos(x, y, z);
                    IBlockState state = chunk.getBlockState(localPos);
                    Block block = state.getBlock();

                    if (block == Blocks.AIR) {
                        continue;
                    }

                    // 检测村庄特征方块
                    boolean isVillageBlock = false;

                    // 门类
                    if (block == Blocks.DARK_OAK_DOOR || block == Blocks.OAK_DOOR ||
                            block == Blocks.SPRUCE_DOOR || block == Blocks.BIRCH_DOOR ||
                            block == Blocks.JUNGLE_DOOR || block == Blocks.ACACIA_DOOR) {
                        isVillageBlock = true;
                    }
                    // 床（村民需要床）
                    else if (block == Blocks.BED) {
                        isVillageBlock = true;
                    }
                    // 工作站方块
                    else if (block == Blocks.CRAFTING_TABLE || block == Blocks.FURNACE ||
                            block == Blocks.BREWING_STAND || block == Blocks.ANVIL ||
                            block == Blocks.BOOKSHELF) {
                        isVillageBlock = true;
                    }
                    // 村庄建筑特征
                    else if (block == Blocks.COBBLESTONE || block == Blocks.PLANKS ||
                            block == Blocks.LOG || block == Blocks.LOG2) {
                        // 这些是常见建筑材料，但需要更多数量才认为是村庄
                        villageBlockCount++;
                        if (villageBlockCount < 10) {
                            continue;
                        }
                        isVillageBlock = true;
                    }

                    if (isVillageBlock) {
                        int worldX = chunkBaseX + x;
                        int worldZ = chunkBaseZ + z;
                        BlockPos worldPos = new BlockPos(worldX, y, worldZ);

                        double distance = playerPos.distanceSq(worldPos);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestInChunk = worldPos;
                        }
                    }
                }
            }
        }

        return nearestInChunk;
    }

    // ===== 原有方法保持不变 =====

    public static BlockPos scanForDungeon(EntityPlayer player, ScanConfig config) {
        World world = player.world;
        BlockPos playerPos = player.getPosition();

        int yMin = Math.max(0, config.getYMin(playerPos));
        int yMax = Math.min(255, config.getYMax(playerPos));

        for (int i = 0; i < CHUNK_SPIRAL.length; i++) {
            int[] offset = CHUNK_SPIRAL[i];

            if (Math.abs(offset[0]) > config.hRange || Math.abs(offset[1]) > config.hRange) {
                continue;
            }

            int chunkX = (int)Math.floor(player.posX / 16) + offset[0];
            int chunkZ = (int)Math.floor(player.posZ / 16) + offset[1];

            if (!world.isChunkGeneratedAt(chunkX, chunkZ)) {
                continue;
            }

            Chunk chunk = world.getChunk(chunkX, chunkZ);

            if (chunk.isEmpty()) {
                continue;
            }

            BlockPos result = scanChunkForDungeon(chunk, yMin, yMax, player);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private static BlockPos scanChunkForDungeon(Chunk chunk, int yMin, int yMax, EntityPlayer player) {
        World world = player.world;

        int chunkBaseX = chunk.x << 4;
        int chunkBaseZ = chunk.z << 4;

        for (int y = yMax; y >= yMin; y--) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos localPos = new BlockPos(x, y, z);
                    IBlockState state = chunk.getBlockState(localPos);
                    Block block = state.getBlock();

                    if (block == Blocks.AIR) {
                        continue;
                    }

                    int worldX = chunkBaseX + x;
                    int worldZ = chunkBaseZ + z;
                    BlockPos worldPos = new BlockPos(worldX, y, worldZ);

                    TileEntity te = world.getTileEntity(worldPos);
                    if (te instanceof TileEntityMobSpawner) {
                        return worldPos;
                    }
                }
            }
        }

        return null;
    }

    public static BlockPos scanForChest(EntityPlayer player, ScanConfig config) {
        World world = player.world;
        BlockPos playerPos = player.getPosition();

        int yMin = Math.max(0, config.getYMin(playerPos));
        int yMax = Math.min(255, config.getYMax(playerPos));

        BlockPos nearestChest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (int i = 0; i < CHUNK_SPIRAL.length; i++) {
            int[] offset = CHUNK_SPIRAL[i];

            if (Math.abs(offset[0]) > config.hRange || Math.abs(offset[1]) > config.hRange) {
                continue;
            }

            int chunkX = (int)Math.floor(player.posX / 16) + offset[0];
            int chunkZ = (int)Math.floor(player.posZ / 16) + offset[1];

            if (!world.isChunkGeneratedAt(chunkX, chunkZ)) {
                continue;
            }

            Chunk chunk = world.getChunk(chunkX, chunkZ);

            if (chunk.isEmpty()) {
                continue;
            }

            BlockPos chunkResult = scanChunkForChest(chunk, yMin, yMax, player);
            if (chunkResult != null) {
                double distance = playerPos.distanceSq(chunkResult);
                if (distance < nearestDistance && distance > 9) {
                    nearestDistance = distance;
                    nearestChest = chunkResult;

                    if (distance < 100) {
                        return nearestChest;
                    }
                }
            }
        }

        return nearestChest;
    }

    private static BlockPos scanChunkForChest(Chunk chunk, int yMin, int yMax, EntityPlayer player) {
        World world = player.world;
        BlockPos playerPos = player.getPosition();

        BlockPos nearestInChunk = null;
        double nearestDistance = Double.MAX_VALUE;

        int chunkBaseX = chunk.x << 4;
        int chunkBaseZ = chunk.z << 4;

        for (int y = yMax; y >= yMin; y--) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos localPos = new BlockPos(x, y, z);
                    IBlockState state = chunk.getBlockState(localPos);
                    Block block = state.getBlock();

                    if (block == Blocks.AIR) {
                        continue;
                    }

                    int worldX = chunkBaseX + x;
                    int worldZ = chunkBaseZ + z;
                    BlockPos worldPos = new BlockPos(worldX, y, worldZ);

                    TileEntity te = world.getTileEntity(worldPos);
                    if (te instanceof TileEntityChest) {
                        double distance = playerPos.distanceSq(worldPos);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestInChunk = worldPos;
                        }
                    }
                }
            }
        }

        return nearestInChunk;
    }

    /**
     * 扫描特定方块（用于 Waystone 等）
     */
    public static BlockPos scanForBlock(EntityPlayer player, ScanConfig config, String blockName) {
        World world = player.world;
        BlockPos playerPos = player.getPosition();

        int yMin = Math.max(0, config.getYMin(playerPos));
        int yMax = Math.min(255, config.getYMax(playerPos));

        // 解析方块注册名
        ResourceLocation targetBlockRL = new ResourceLocation(blockName);
        Block targetBlock = Block.REGISTRY.getObject(targetBlockRL);

        if (targetBlock == null || targetBlock == Blocks.AIR) {
            return null;
        }

        BlockPos nearestBlock = null;
        double nearestDistance = Double.MAX_VALUE;

        // 螺旋遍历区块
        for (int i = 0; i < CHUNK_SPIRAL.length; i++) {
            int[] offset = CHUNK_SPIRAL[i];

            if (Math.abs(offset[0]) > config.hRange || Math.abs(offset[1]) > config.hRange) {
                continue;
            }

            int chunkX = (int)Math.floor(player.posX / 16) + offset[0];
            int chunkZ = (int)Math.floor(player.posZ / 16) + offset[1];

            if (!world.isChunkGeneratedAt(chunkX, chunkZ)) {
                continue;
            }

            Chunk chunk = world.getChunk(chunkX, chunkZ);

            if (chunk.isEmpty()) {
                continue;
            }

            BlockPos chunkResult = scanChunkForBlock(chunk, yMin, yMax, targetBlock, player);
            if (chunkResult != null) {
                double distance = playerPos.distanceSq(chunkResult);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestBlock = chunkResult;

                    if (distance < 100) {
                        return nearestBlock;
                    }
                }
            }
        }

        return nearestBlock;
    }

    /**
     * 在区块中扫描特定方块
     */
    private static BlockPos scanChunkForBlock(Chunk chunk, int yMin, int yMax, Block targetBlock, EntityPlayer player) {
        World world = player.world;
        BlockPos playerPos = player.getPosition();

        BlockPos nearestInChunk = null;
        double nearestDistance = Double.MAX_VALUE;

        int chunkBaseX = chunk.x << 4;
        int chunkBaseZ = chunk.z << 4;

        for (int y = yMax; y >= yMin; y--) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos localPos = new BlockPos(x, y, z);
                    IBlockState state = chunk.getBlockState(localPos);
                    Block block = state.getBlock();

                    if (block == Blocks.AIR) {
                        continue;
                    }

                    if (block == targetBlock) {
                        int worldX = chunkBaseX + x;
                        int worldZ = chunkBaseZ + z;
                        BlockPos worldPos = new BlockPos(worldX, y, worldZ);

                        double distance = playerPos.distanceSq(worldPos);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestInChunk = worldPos;
                        }
                    }
                }
            }
        }

        return nearestInChunk;
    }

    public static java.util.List<BlockPos> getNearbyChests(EntityPlayer player, ScanConfig config) {
        java.util.List<BlockPos> chests = new java.util.ArrayList<>();
        World world = player.world;
        BlockPos playerPos = player.getPosition();

        int yMin = Math.max(0, config.getYMin(playerPos));
        int yMax = Math.min(255, config.getYMax(playerPos));

        for (int i = 0; i < CHUNK_SPIRAL.length; i++) {
            int[] offset = CHUNK_SPIRAL[i];

            if (Math.abs(offset[0]) > config.hRange || Math.abs(offset[1]) > config.hRange) {
                continue;
            }

            int chunkX = (int)Math.floor(player.posX / 16) + offset[0];
            int chunkZ = (int)Math.floor(player.posZ / 16) + offset[1];

            if (!world.isChunkGeneratedAt(chunkX, chunkZ)) {
                continue;
            }

            Chunk chunk = world.getChunk(chunkX, chunkZ);

            if (chunk.isEmpty()) {
                continue;
            }

            int chunkBaseX = chunk.x << 4;
            int chunkBaseZ = chunk.z << 4;

            for (int y = yMax; y >= yMin; y--) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos localPos = new BlockPos(x, y, z);
                        IBlockState state = chunk.getBlockState(localPos);
                        Block block = state.getBlock();

                        if (block == Blocks.AIR) {
                            continue;
                        }

                        int worldX = chunkBaseX + x;
                        int worldZ = chunkBaseZ + z;
                        BlockPos worldPos = new BlockPos(worldX, y, worldZ);

                        TileEntity te = world.getTileEntity(worldPos);
                        if (te instanceof TileEntityChest) {
                            double distance = playerPos.distanceSq(worldPos);
                            int radius = config.hRange * 16;
                            if (distance <= radius * radius) {
                                chests.add(worldPos);
                            }
                        }
                    }
                }
            }
        }

        return chests;
    }
}