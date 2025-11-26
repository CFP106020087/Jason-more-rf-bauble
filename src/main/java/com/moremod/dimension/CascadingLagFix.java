package com.moremod.dimension;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

/**
 * 修复级联世代延迟的工具类
 */
public class CascadingLagFix {

    /**
     * 检查位置是否在已加载的区块内
     * 防止跨区块生成
     */
    public static boolean isSafeToGenerate(World world, BlockPos pos, int chunkX, int chunkZ) {
        ChunkPos targetChunk = new ChunkPos(pos);

        // 只允许在当前区块或已加载的区块内生成
        if (targetChunk.x == chunkX && targetChunk.z == chunkZ) {
            return true;
        }

        // 检查目标区块是否已加载
        return world.isChunkGeneratedAt(targetChunk.x, targetChunk.z);
    }

    /**
     * 获取结构的安全生成边界
     * 确保结构不会超出当前区块
     */
    public static BlockPos[] getSafeBounds(BlockPos center, int radius, int chunkX, int chunkZ) {
        int chunkMinX = chunkX * 16;
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunkZ * 16;
        int chunkMaxZ = chunkMinZ + 15;

        int minX = Math.max(center.getX() - radius, chunkMinX);
        int maxX = Math.min(center.getX() + radius, chunkMaxX);
        int minZ = Math.max(center.getZ() - radius, chunkMinZ);
        int maxZ = Math.min(center.getZ() + radius, chunkMaxZ);

        return new BlockPos[] {
                new BlockPos(minX, center.getY() - radius, minZ),
                new BlockPos(maxX, center.getY() + radius, maxZ)
        };
    }

    /**
     * 延迟跨区块的结构生成
     */
    public static class DeferredStructure {
        public final BlockPos pos;
        public final int radius;
        public final StructureGenerator generator;

        public DeferredStructure(BlockPos pos, int radius, StructureGenerator generator) {
            this.pos = pos;
            this.radius = radius;
            this.generator = generator;
        }

        public boolean canGenerateIn(int chunkX, int chunkZ) {
            ChunkPos minChunk = new ChunkPos((pos.getX() - radius) >> 4, (pos.getZ() - radius) >> 4);
            ChunkPos maxChunk = new ChunkPos((pos.getX() + radius) >> 4, (pos.getZ() + radius) >> 4);

            return chunkX >= minChunk.x && chunkX <= maxChunk.x &&
                    chunkZ >= minChunk.z && chunkZ <= maxChunk.z;
        }
    }

    @FunctionalInterface
    public interface StructureGenerator {
        void generate(World world, BlockPos pos);
    }
}