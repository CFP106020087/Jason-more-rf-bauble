package com.moremod.ritual;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 祭坛阶层枚举
 *
 * 一阶：基础8基座结构
 * 二阶：8基座 + 外围装饰方块（红石灯/书架等）
 * 三阶：8基座 + 石英地板 + 外侧7格高柱子与房梁
 */
public enum AltarTier {

    TIER_1(1, "基础祭坛", 0.0f, 0.0f),      // 基础成功率，无翻倍
    TIER_2(2, "进阶祭坛", 0.10f, 0.05f),    // +10%成功率，5%翻倍几率
    TIER_3(3, "大师祭坛", 0.25f, 0.15f);    // +25%成功率，15%翻倍几率

    private final int level;
    private final String displayName;
    private final float successBonus;      // 成功率加成
    private final float doubleEffectChance; // 效果翻倍几率

    AltarTier(int level, String displayName, float successBonus, float doubleEffectChance) {
        this.level = level;
        this.displayName = displayName;
        this.successBonus = successBonus;
        this.doubleEffectChance = doubleEffectChance;
    }

    public int getLevel() { return level; }
    public String getDisplayName() { return displayName; }
    public float getSuccessBonus() { return successBonus; }
    public float getDoubleEffectChance() { return doubleEffectChance; }

    /**
     * 检测世界中祭坛的阶层
     * @param world 世界
     * @param corePos 祭坛核心位置
     * @return 检测到的最高阶层
     */
    public static AltarTier detectTier(World world, BlockPos corePos) {
        // 从高到低检测，返回最高匹配的阶层
        if (checkTier3Structure(world, corePos)) {
            return TIER_3;
        }
        if (checkTier2Structure(world, corePos)) {
            return TIER_2;
        }
        // 一阶只需要基座存在（由TileEntityRitualCore检测）
        return TIER_1;
    }

    /**
     * 二阶祭坛结构检测
     * 在8基座外围放置装饰方块（红石灯、书架、雕刻石砖等）
     *
     * 结构示意（俯视）：
     *       D   D   D
     *     D   P   P   D
     *   D   P       P   D
     *     D     核     D
     *   D   P       P   D
     *     D   P   P   D
     *       D   D   D
     *
     * D = 装饰方块（以下任一）：红石灯、书架、雕刻石砖、海晶灯
     */
    private static boolean checkTier2Structure(World world, BlockPos corePos) {
        // 外围装饰方块位置（距离核心4-5格）
        BlockPos[] decorPositions = {
            // 四个方向 距离4
            corePos.add(4, 0, 0), corePos.add(-4, 0, 0),
            corePos.add(0, 0, 4), corePos.add(0, 0, -4),
            // 四个角落 距离3,3
            corePos.add(3, 0, 3), corePos.add(3, 0, -3),
            corePos.add(-3, 0, 3), corePos.add(-3, 0, -3),
            // 额外角落位置
            corePos.add(4, 0, 1), corePos.add(4, 0, -1),
            corePos.add(-4, 0, 1), corePos.add(-4, 0, -1),
            corePos.add(1, 0, 4), corePos.add(-1, 0, 4),
            corePos.add(1, 0, -4), corePos.add(-1, 0, -4)
        };

        int validCount = 0;
        for (BlockPos pos : decorPositions) {
            if (isValidTier2DecorBlock(world, pos)) {
                validCount++;
            }
        }

        // 需要至少12个装饰方块
        return validCount >= 12;
    }

    /**
     * 三阶祭坛结构检测
     * 8基座 + 石英地板 + 外侧柱子与房梁
     *
     * 结构要求：
     * - 地板：7x7石英块/平滑石英块
     * - 四角柱子：高度7格的石英柱
     * - 房梁：连接柱子顶部的石英台阶/石英块
     */
    private static boolean checkTier3Structure(World world, BlockPos corePos) {
        // 首先必须满足二阶条件
        if (!checkTier2Structure(world, corePos)) {
            return false;
        }

        // 检测石英地板（5x5核心区域）
        int floorCount = 0;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos floorPos = corePos.add(x, -1, z);
                if (isQuartzBlock(world, floorPos)) {
                    floorCount++;
                }
            }
        }

        // 需要至少20个石英地板（5x5 - 5个基座位置）
        if (floorCount < 20) {
            return false;
        }

        // 检测四角柱子（距离核心4格对角）
        BlockPos[] pillarBases = {
            corePos.add(4, 0, 4),
            corePos.add(4, 0, -4),
            corePos.add(-4, 0, 4),
            corePos.add(-4, 0, -4)
        };

        int validPillars = 0;
        for (BlockPos base : pillarBases) {
            if (checkPillar(world, base, 7)) { // 至少7格高
                validPillars++;
            }
        }

        // 需要至少3个有效柱子
        return validPillars >= 3;
    }

    /**
     * 检查是否为有效的二阶装饰方块
     */
    private static boolean isValidTier2DecorBlock(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        return block == Blocks.REDSTONE_LAMP ||
               block == Blocks.LIT_REDSTONE_LAMP ||
               block == Blocks.BOOKSHELF ||
               block == Blocks.STONEBRICK ||  // 包括雕刻石砖
               block == Blocks.SEA_LANTERN ||
               block == Blocks.GLOWSTONE ||
               block == Blocks.PRISMARINE ||
               block == Blocks.END_BRICKS ||
               block == Blocks.PURPUR_BLOCK ||
               block == Blocks.NETHER_BRICK;
    }

    /**
     * 检查是否为石英类方块
     */
    private static boolean isQuartzBlock(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        return block == Blocks.QUARTZ_BLOCK ||
               block == Blocks.QUARTZ_STAIRS ||
               block == Blocks.STONE_SLAB ||   // 石英台阶
               block == Blocks.STONE_SLAB2 ||
               block == Blocks.PURPUR_BLOCK || // 紫珀块也算
               block == Blocks.END_BRICKS;
    }

    /**
     * 检查柱子结构
     */
    private static boolean checkPillar(World world, BlockPos base, int minHeight) {
        int height = 0;
        for (int y = 0; y < 10; y++) { // 最多检测10格高
            BlockPos checkPos = base.add(0, y, 0);
            IBlockState state = world.getBlockState(checkPos);
            Block block = state.getBlock();

            if (block == Blocks.QUARTZ_BLOCK ||
                block == Blocks.PURPUR_PILLAR ||
                block == Blocks.END_BRICKS ||
                block == Blocks.STONEBRICK ||
                block == Blocks.NETHER_BRICK) {
                height++;
            } else if (height > 0) {
                break; // 柱子断了
            }
        }

        return height >= minHeight;
    }

    /**
     * 根据等级获取对应的AltarTier
     */
    public static AltarTier fromLevel(int level) {
        for (AltarTier tier : values()) {
            if (tier.level == level) {
                return tier;
            }
        }
        return TIER_1;
    }
}
