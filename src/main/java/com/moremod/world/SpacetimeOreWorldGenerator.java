package com.moremod.world;

import com.moremod.dimension.PersonalDimensionManager;
import com.moremod.init.ModBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * 时空碎片矿石世界生成器
 * 在私人维度的虚空中稀有生成
 */
public class SpacetimeOreWorldGenerator implements IWorldGenerator {

    // 生成配置
    private static final int MIN_Y = 40;  // 最低生成高度
    private static final int MAX_Y = 200; // 最高生成高度
    private static final int CHANCE = 50; // 生成概率 (1/50的区块会生成)
    private static final int MIN_DISTANCE_FROM_SPAWN = 100; // 距离任何玩家空间的最小距离

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {

        // 只在私人维度生成
        if (world.provider.getDimension() != PersonalDimensionManager.PERSONAL_DIM_ID) {
            return;
        }

        // 随机概率检查
        if (random.nextInt(CHANCE) != 0) {
            return;
        }

        // 检查是否远离所有玩家空间
        int worldX = chunkX * 16 + 8;
        int worldZ = chunkZ * 16 + 8;

        if (isNearPlayerSpace(worldX, worldZ)) {
            return;
        }

        // 生成矿石
        generateOreCluster(world, random, worldX, worldZ);
    }

    /**
     * 检查位置是否靠近玩家空间
     */
    private boolean isNearPlayerSpace(int x, int z) {
        // 获取所有玩家空间并检查距离
        for (PersonalDimensionManager.PersonalSpace space : PersonalDimensionManager.getAllSpaces()) {
            double distance = Math.sqrt(
                    Math.pow(x - space.centerPos.getX(), 2) +
                            Math.pow(z - space.centerPos.getZ(), 2)
            );

            if (distance < MIN_DISTANCE_FROM_SPAWN) {
                return true; // 太靠近玩家空间
            }
        }

        return false;
    }

    /**
     * 生成矿石簇
     */
    private void generateOreCluster(World world, Random random, int centerX, int centerZ) {
        // 随机Y坐标
        int y = MIN_Y + random.nextInt(MAX_Y - MIN_Y);

        // 随机偏移以避免总是在区块中心
        int x = centerX + random.nextInt(8) - 4;
        int z = centerZ + random.nextInt(8) - 4;

        BlockPos center = new BlockPos(x, y, z);

        // 获取矿石方块
        IBlockState oreBlock;
        try {
            oreBlock = ModBlocks.SPACETIME_SHARD_ORE.getDefaultState();
        } catch (Exception e) {
            oreBlock = Blocks.GLOWSTONE.getDefaultState(); // 备用
        }

        // 生成浮空矿石平台（稀有且独特的形状）
        generateFloatingOreStructure(world, center, oreBlock, random);

        System.out.println("[矿石生成] 在 " + center + " 生成了时空碎片矿石");
    }

    /**
     * 生成浮空矿石结构
     */
    private void generateFloatingOreStructure(World world, BlockPos center, IBlockState ore, Random random) {
        int structureType = random.nextInt(3);

        switch (structureType) {
            case 0: // 小型浮岛
                generateFloatingIsland(world, center, ore, random);
                break;
            case 1: // 矿石环
                generateOreRing(world, center, ore, random);
                break;
            case 2: // 垂直矿脉
                generateVerticalVein(world, center, ore, random);
                break;
        }
    }

    /**
     * 生成小型浮岛
     */
    private void generateFloatingIsland(World world, BlockPos center, IBlockState ore, Random random) {
        // 主体平台 (3x3)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = center.add(x, 0, z);
                if (world.getBlockState(pos).getBlock() == Blocks.AIR) {
                    world.setBlockState(pos, ore, 2);
                }
            }
        }

        // 上层 (2x2)
        if (random.nextBoolean()) {
            for (int x = -1; x <= 0; x++) {
                for (int z = -1; z <= 0; z++) {
                    BlockPos pos = center.add(x, 1, z);
                    if (world.getBlockState(pos).getBlock() == Blocks.AIR) {
                        world.setBlockState(pos, ore, 2);
                    }
                }
            }
        }

        // 下层悬挂矿石
        for (int i = 0; i < 3; i++) {
            BlockPos pos = center.add(
                    random.nextInt(3) - 1,
                    -1 - random.nextInt(2),
                    random.nextInt(3) - 1
            );
            if (world.getBlockState(pos).getBlock() == Blocks.AIR) {
                world.setBlockState(pos, ore, 2);
            }
        }
    }

    /**
     * 生成矿石环
     */
    private void generateOreRing(World world, BlockPos center, IBlockState ore, Random random) {
        int radius = 2 + random.nextInt(2); // 2-3格半径

        for (int angle = 0; angle < 360; angle += 45) {
            double rad = Math.toRadians(angle);
            int x = (int)(Math.cos(rad) * radius);
            int z = (int)(Math.sin(rad) * radius);

            BlockPos pos = center.add(x, 0, z);
            if (world.getBlockState(pos).getBlock() == Blocks.AIR) {
                world.setBlockState(pos, ore, 2);

                // 偶尔添加垂直延伸
                if (random.nextInt(3) == 0) {
                    BlockPos above = pos.up();
                    if (world.getBlockState(above).getBlock() == Blocks.AIR) {
                        world.setBlockState(above, ore, 2);
                    }
                }
            }
        }

        // 中心核心
        if (random.nextBoolean()) {
            world.setBlockState(center, ore, 2);
        }
    }

    /**
     * 生成垂直矿脉
     */
    private void generateVerticalVein(World world, BlockPos center, IBlockState ore, Random random) {
        int height = 5 + random.nextInt(5); // 5-9格高

        for (int y = 0; y < height; y++) {
            BlockPos pos = center.up(y);

            // 主矿脉
            if (world.getBlockState(pos).getBlock() == Blocks.AIR) {
                world.setBlockState(pos, ore, 2);
            }

            // 随机分支
            if (random.nextInt(3) == 0) {
                BlockPos branch = pos.add(
                        random.nextInt(3) - 1,
                        0,
                        random.nextInt(3) - 1
                );
                if (world.getBlockState(branch).getBlock() == Blocks.AIR) {
                    world.setBlockState(branch, ore, 2);
                }
            }
        }
    }
}