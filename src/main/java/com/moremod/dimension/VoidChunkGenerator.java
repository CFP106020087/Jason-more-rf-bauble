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

/**
 * 虚空区块生成器
 * 生成完全空的区块，不包含任何方块或生物
 */
public class VoidChunkGenerator implements IChunkGenerator {

    private final World world;

    public VoidChunkGenerator(World world) {
        this.world = world;
    }

    /**
     * 生成区块 - 完全空的
     */
    @Override
    public Chunk generateChunk(int x, int z) {
        ChunkPrimer chunkprimer = new ChunkPrimer();

        // 不生成任何方块，保持完全空虚

        Chunk chunk = new Chunk(this.world, chunkprimer, x, z);

        // 设置生物群系为虚空
        byte[] biomeArray = chunk.getBiomeArray();
        for (int i = 0; i < biomeArray.length; ++i) {
            biomeArray[i] = (byte) Biome.getIdForBiome(Biomes.VOID);
        }

        chunk.generateSkylightMap();
        return chunk;
    }

    /**
     * 用于地形生成 - 虚空不需要
     */
    @Override
    public void populate(int x, int z) {
        // 虚空维度不生成任何东西
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
}