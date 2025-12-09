package com.moremod.multiblock;

import com.moremod.init.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 抽油機多方塊結構驗證器 (美學增強版)
 *
 * 結構設計 (3x3x4 高):
 * 支持使用 樓梯、半磚、柵欄、圍牆 進行裝飾
 *
 * 第0層 (地基): 四角支柱 + 中心核心
 * 第1-2層 (機體): 四角支柱(可鏤空) + 中心活塞
 * 第3層 (頂部): 封頂(可用半磚/樓梯)
 */
public class MultiblockOilExtractor {

    public static boolean checkStructure(World world, BlockPos corePos) {
        // 第0層 - 地基
        if (!checkBaseLayer(world, corePos)) return false;

        // 第1-2層 - 機體 (中間是活塞)
        if (!checkMiddleLayer(world, corePos.up())) return false;
        if (!checkMiddleLayer(world, corePos.up(2))) return false;

        // 第3層 - 頂部
        if (!checkTopLayer(world, corePos.up(3))) return false;

        return true;
    }

    // === 結構檢查邏輯 ===

    private static boolean checkBaseLayer(World world, BlockPos centerPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();

                if (x == 0 && z == 0) {
                    // 中心必須是核心
                    if (block != ModBlocks.OIL_EXTRACTOR_CORE) return false;
                } else if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                    // 四角必須是結構方塊 (允許樓梯/圍牆等)
                    if (!isValidStructuralBlock(world, checkPos)) return false;
                }
            }
        }
        return true;
    }

    private static boolean checkMiddleLayer(World world, BlockPos centerPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();

                if (x == 0 && z == 0) {
                    // 中心必須是活塞 (動力源)
                    if (block != Blocks.PISTON && block != Blocks.STICKY_PISTON) return false;
                } else if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                    // 四角支柱 (允許柵欄/鐵柵欄透視)
                    if (!isValidStructuralBlock(world, checkPos)) return false;
                }
                // 十字位置允許空氣
            }
        }
        return true;
    }

    private static boolean checkTopLayer(World world, BlockPos centerPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                // 頂部全覆蓋檢查 (允許半磚/樓梯做造型)
                if (!isValidStructuralBlock(world, checkPos)) return false;
            }
        }
        return true;
    }

    // === 核心判斷邏輯 ===

    /**
     * 判斷是否為合法的結構方塊 (美學核心)
     * 允許：貴重金屬塊、圍牆、柵欄、樓梯、半磚、鐵柵欄
     */
    private static boolean isValidStructuralBlock(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();

        // 1. 基礎框架 (鐵/金/鑽石)
        if (isValidFrameBlock(block)) return true;

        // 2. 工業風裝飾
        if (block == Blocks.IRON_BARS) return true; // 鐵柵欄
        if (block == Blocks.OBSIDIAN) return true;  // 黑曜石
        if (block instanceof BlockWall) return true; // 圓石牆等
        if (block instanceof BlockFence) return true; // 柵欄
        if (block instanceof BlockStairs) return true; // 樓梯
        if (block instanceof BlockSlab) return true; // 半磚

        return false;
    }

    /**
     * 用於計算等級的方塊 (只統計貴重金屬)
     */
    private static boolean isValidFrameBlock(Block block) {
        return block == Blocks.IRON_BLOCK ||
                block == Blocks.GOLD_BLOCK ||
                block == Blocks.DIAMOND_BLOCK ||
                block == Blocks.EMERALD_BLOCK; // 如果有的話
    }

    /**
     * 獲取結構等級 (只掃描貴重金屬塊數量)
     */
    public static int getFrameTier(World world, BlockPos corePos) {
        int ironCount = 0, goldCount = 0, diamondCount = 0;

        // 掃描範圍 3x3x4
        for (int y = 0; y <= 3; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (y == 0 && x == 0 && z == 0) continue;
                    BlockPos pos = corePos.add(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();

                    if (block == Blocks.IRON_BLOCK) ironCount++;
                    else if (block == Blocks.GOLD_BLOCK) goldCount++;
                    else if (block == Blocks.DIAMOND_BLOCK) diamondCount++;
                }
            }
        }

        if (diamondCount >= 4) return 3; // 只要有4個鑽石塊就算T3
        if (goldCount >= 8) return 2;
        return 1; // 默認T1
    }

    public static String getBuildGuide() {
        return "§b結構: 3x3x4\n§7支柱可用: 牆/柵欄/鐵柵欄/樓梯\n§e等級取決於鐵/金/鑽石塊數量";
    }
}