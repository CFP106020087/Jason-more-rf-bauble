package com.moremod.dimension;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 虚空区块生成器 - 优化修复版
 * 优化了内存使用和生成性能
 */
public class VoidChunkGenerator implements IChunkGenerator {

    private final World world;

    // 缓存ChunkPrimer以避免重复分配
    private static final ThreadLocal<ChunkPrimer> primerCache = ThreadLocal.withInitial(ChunkPrimer::new);

    // 缓存生物群系ID
    private static final byte VOID_BIOME_ID;

    // 生成统计
    private static long chunksGenerated = 0;
    private static long lastReportTime = 0;

    static {
        VOID_BIOME_ID = (byte) Biome.getIdForBiome(Biomes.VOID);
    }

    public VoidChunkGenerator(World world) {
        this.world = world;
    }

    /**
     * 生成区块 - 优化版
     */
    @Override
    public Chunk generateChunk(int x, int z) {
        // 使用线程本地缓存的ChunkPrimer
        ChunkPrimer chunkprimer = primerCache.get();

        // 清空primer（重用对象）
        clearPrimer(chunkprimer);

        // 不生成任何方块，保持完全空虚

        // 创建区块
        Chunk chunk = new Chunk(this.world, chunkprimer, x, z);

        // 设置生物群系为虚空 - 优化的填充方式
        byte[] biomeArray = chunk.getBiomeArray();
        java.util.Arrays.fill(biomeArray, VOID_BIOME_ID);

        // 生成天空光照图
        chunk.generateSkylightMap();

        // 更新统计
        updateStatistics();

        return chunk;
    }

    /**
     * 清空ChunkPrimer
     */
    private void clearPrimer(ChunkPrimer primer) {
        // ChunkPrimer默认就是空的，所以通常不需要清理
        // 但如果被重用，可能需要清理
        // 这里保留方法以备将来需要
    }

    /**
     * 更新生成统计
     */
    private void updateStatistics() {
        chunksGenerated++;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastReportTime > 60000) { // 每分钟报告一次
            System.out.println("[虚空生成器] 已生成 " + chunksGenerated + " 个区块");
            lastReportTime = currentTime;
        }
    }

    /**
     * 用于地形生成 - 虚空不需要
     */
    @Override
    public void populate(int x, int z) {
        // 虚空维度不生成任何东西
        // 直接返回，避免任何处理
    }

    /**
     * 生成结构 - 虚空不生成结构
     */
    @Override
    public boolean generateStructures(Chunk chunkIn, int x, int z) {
        return false;
    }

    /**
     * 获取可能生成的生物列表 - 返回空列表
     */
    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
        return Collections.emptyList(); // 绝对不生成任何生物
    }

    /**
     * 获取最近的结构 - 虚空没有结构
     */
    @Nullable
    @Override
    public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position, boolean findUnexplored) {
        return null;
    }

    /**
     * 重新创建结构 - 虚空不需要
     */
    @Override
    public void recreateStructures(Chunk chunkIn, int x, int z) {
        // 虚空维度不生成结构
    }

    /**
     * 是否在结构内 - 永远返回false
     */
    @Override
    public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
        return false;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        // 清理线程本地缓存
        primerCache.remove();

        // 重置统计
        chunksGenerated = 0;
        lastReportTime = 0;
    }
}