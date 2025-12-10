package com.moremod.multiblock;

import com.moremod.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 打印机多方块结构检查器
 *
 * 结构布局 (5x3x5):
 * - 第0层 (地面): 金属框架 + 中心核心
 * - 第1层 (机械层): 机械臂支撑
 * - 第2层 (顶部): 能量输入接口
 *
 * 使用材料:
 * - 打印机核心方块 (中心)
 * - 铁块 (结构框架)
 * - 红石块 (能量导体)
 * - 石英块 (装饰/绝缘)
 */
public class MultiblockPrinter {

    /**
     * 检查多方块结构是否完整
     * @param world 世界
     * @param corePos 打印机核心方块位置
     * @return 结构是否完整
     */
    public static boolean checkStructure(World world, BlockPos corePos) {
        // 检查第0层 (地面层)
        if (!checkLayer0(world, corePos)) return false;

        // 检查第1层 (机械层)
        if (!checkLayer1(world, corePos.up())) return false;

        // 结构完整
        return true;
    }

    /**
     * 第0层检查 - 3x3 基座
     * 布局:
     *   I I I
     *   I C I
     *   I I I
     * C = 核心 (已放置)
     * I = 铁块
     */
    private static boolean checkLayer0(World world, BlockPos centerPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;  // 跳过核心位置

                BlockPos checkPos = centerPos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();

                // 周围必须是铁块
                if (block != Blocks.IRON_BLOCK) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 第1层检查 - 机械支撑层
     * 布局:
     *   . R .
     *   R . R
     *   . R .
     * R = 红石块
     * . = 空气
     */
    private static boolean checkLayer1(World world, BlockPos centerPos) {
        // 四个方向的红石块
        BlockPos[] redstonePositions = {
            centerPos.add(0, 0, -1),  // 北
            centerPos.add(0, 0, 1),   // 南
            centerPos.add(-1, 0, 0),  // 西
            centerPos.add(1, 0, 0)    // 东
        };

        for (BlockPos pos : redstonePositions) {
            Block block = world.getBlockState(pos).getBlock();
            if (block != Blocks.REDSTONE_BLOCK) {
                return false;
            }
        }

        // 中心必须是空气（机械臂活动空间）
        if (world.getBlockState(centerPos).getBlock() != Blocks.AIR) {
            return false;
        }

        return true;
    }

    /**
     * 获取建造指南
     */
    public static String getBuildGuide() {
        StringBuilder guide = new StringBuilder();
        guide.append("\u00a7b=== 打印机建造指南 ===\u00a7r\n\n");

        guide.append("\u00a7e第0层 (地面):\u00a7r\n");
        guide.append("  I I I\n");
        guide.append("  I \u00a76C\u00a7r I\n");
        guide.append("  I I I\n\n");

        guide.append("\u00a7e第1层 (上方):\u00a7r\n");
        guide.append("  . R .\n");
        guide.append("  R . R\n");
        guide.append("  . R .\n\n");

        guide.append("\u00a76图例:\u00a7r\n");
        guide.append("C = 打印机核心 (中心放置)\n");
        guide.append("I = 铁块\n");
        guide.append("R = 红石块\n");
        guide.append(". = 空气\n");

        return guide.toString();
    }

    /**
     * 尝试形成多方块结构（用于视觉反馈）
     */
    public static void tryFormStructure(World world, BlockPos corePos) {
        if (world.isRemote) return;

        boolean formed = checkStructure(world, corePos);
        if (formed) {
            // 可以在这里添加结构形成的粒子效果
            System.out.println("[Printer] 多方块结构已形成于 " + corePos);
        }
    }
}
