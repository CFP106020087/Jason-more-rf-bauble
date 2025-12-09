package com.moremod.multiblock;

import com.moremod.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 抽油機多方塊結構驗證器
 *
 * 結構設計 (3x3x4 高):
 *
 * 第0層 (地基):
 *   I I I
 *   I C I    C = 抽油機核心, I = 鐵塊
 *   I I I
 *
 * 第1層 (機體):
 *   I . I
 *   . P .    P = 活塞（管道）, . = 空氣
 *   I . I
 *
 * 第2層 (機體):
 *   I . I
 *   . P .
 *   I . I
 *
 * 第3層 (頂部):
 *   I I I
 *   I I I
 *   I I I
 */
public class MultiblockOilExtractor {

    /**
     * 檢查多方塊結構是否完整
     */
    public static boolean checkStructure(World world, BlockPos corePos) {
        // 第0層 - 地基
        if (!checkBaseLayer(world, corePos)) return false;

        // 第1-2層 - 機體
        if (!checkMiddleLayer(world, corePos.up())) return false;
        if (!checkMiddleLayer(world, corePos.up(2))) return false;

        // 第3層 - 頂部
        if (!checkTopLayer(world, corePos.up(3))) return false;

        return true;
    }

    /**
     * 第0層 - 地基（核心周圍是鐵塊）
     */
    private static boolean checkBaseLayer(World world, BlockPos centerPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();

                if (x == 0 && z == 0) {
                    // 中心必須是抽油機核心
                    if (block != ModBlocks.OIL_EXTRACTOR_CORE) {
                        return false;
                    }
                } else {
                    // 周圍必須是鐵塊
                    if (!isValidFrameBlock(block)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 第1-2層 - 機體（四角鐵塊，中心活塞或鐵塊）
     */
    private static boolean checkMiddleLayer(World world, BlockPos centerPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();

                // 四角必須是鐵塊
                if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                    if (!isValidFrameBlock(block)) {
                        return false;
                    }
                } else if (x == 0 && z == 0) {
                    // 中心必須是活塞或鐵塊（作為管道）
                    if (block != Blocks.PISTON && block != Blocks.STICKY_PISTON && !isValidFrameBlock(block)) {
                        return false;
                    }
                }
                // 其他位置（十字）可以是空氣或任何方塊
            }
        }
        return true;
    }

    /**
     * 第3層 - 頂部（全部鐵塊）
     */
    private static boolean checkTopLayer(World world, BlockPos centerPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();

                if (!isValidFrameBlock(block)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 有效的框架方塊
     */
    private static boolean isValidFrameBlock(Block block) {
        return block == Blocks.IRON_BLOCK ||
               block == Blocks.GOLD_BLOCK ||
               block == Blocks.DIAMOND_BLOCK;
    }

    /**
     * 獲取建造指南
     */
    public static String getBuildGuide() {
        StringBuilder guide = new StringBuilder();
        guide.append("§b=== 抽油機建造指南 ===§r\n\n");

        guide.append("§e第0層（地基）:§r\n");
        guide.append("  I I I\n");
        guide.append("  I C I\n");
        guide.append("  I I I\n\n");

        guide.append("§e第1-2層（機體）:§r\n");
        guide.append("  I . I\n");
        guide.append("  . P .\n");
        guide.append("  I . I\n\n");

        guide.append("§e第3層（頂部）:§r\n");
        guide.append("  I I I\n");
        guide.append("  I I I\n");
        guide.append("  I I I\n\n");

        guide.append("§6圖例:§r\n");
        guide.append("C = 抽油機核心\n");
        guide.append("I = 鐵塊/金塊/鑽石塊\n");
        guide.append("P = 活塞（管道）\n");
        guide.append(". = 空氣\n\n");

        guide.append("§a使用方法:§r\n");
        guide.append("1. 在有石油的區塊建造\n");
        guide.append("2. 對核心供電\n");
        guide.append("3. 從核心提取石油桶\n");

        return guide.toString();
    }
}
