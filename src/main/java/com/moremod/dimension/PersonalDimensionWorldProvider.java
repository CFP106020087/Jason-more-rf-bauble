package com.moremod.dimension;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.ArrayList;

/**
 * 私人维度世界提供者
 * 创建一个完全虚空的维度，完全禁止生物生成
 */
public class PersonalDimensionWorldProvider extends WorldProvider {

    @Override
    public DimensionType getDimensionType() {
        return PersonalDimensionType.PERSONAL_DIM_TYPE;
    }

    @Override
    public void init() {
        // 设置为单一生物群系（虚空）
        this.biomeProvider = new BiomeProviderSingle(net.minecraft.init.Biomes.VOID);

        // 设置维度ID
        this.setDimension(PersonalDimensionManager.PERSONAL_DIM_ID);

        // 完全禁用生物生成
        this.doesWaterVaporize = false;
        this.hasSkyLight = true;
        this.nether = false;

        // 禁止所有类型的生物生成
        this.setAllowedSpawnTypes(false, false);
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        // 使用虚空区块生成器（禁止生物生成）
        return new VoidChunkGenerator(world);
    }

    @Override
    public boolean isSurfaceWorld() {
        return false;
    }

    @Override
    public boolean canRespawnHere() {
        return false; // 不能在这里重生
    }

    @Override
    public float calculateCelestialAngle(long worldTime, float partialTicks) {
        return 0.5F; // 永远是正午（提供稳定光照）
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Vec3d getFogColor(float p_76562_1_, float p_76562_2_) {
        // 深紫色雾气
        return new Vec3d(0.1, 0.0, 0.2);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Vec3d getSkyColor(Entity cameraEntity, float partialTicks) {
        // 深紫色天空
        return new Vec3d(0.2, 0.1, 0.3);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isSkyColored() {
        return true;
    }

    @Override
    public boolean hasSkyLight() {
        return true;
    }

    @Override
    public boolean isNether() {
        return false;
    }

    @Override
    public float getCloudHeight() {
        return 260.0F; // 云层在很高的地方，不影响视野
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean doesXZShowFog(int x, int z) {
        return false;
    }

    @Override
    public BlockPos getSpawnPoint() {
        return new BlockPos(0, 128, 0);
    }

    @Override
    public int getAverageGroundLevel() {
        return 0; // 虚空没有地面
    }

    @Override
    public double getVoidFogYFactor() {
        return 1.0D; // 虚空雾从y=0开始
    }

    @Override
    public boolean doesWaterVaporize() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IRenderHandler getSkyRenderer() {
        return new PersonalDimensionSkyRenderer();
    }

    /**
     * 完全禁止作为生成坐标
     */
    @Override
    public boolean canCoordinateBeSpawn(int x, int z) {
        return false; // 任何坐标都不能作为生成点
    }
}