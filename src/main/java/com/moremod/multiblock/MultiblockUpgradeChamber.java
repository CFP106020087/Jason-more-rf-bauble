package com.moremod.multiblock;

import com.moremod.init.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 升級艙多方塊結構驗證器 (美學增強版)
 *
 * 結構設計 (3x3x4 高):
 * 修正：中間兩層留空，適配玩家身高
 * 風格：支持 玻璃、海燈籠、螢石 打造科幻感
 *
 * 第0層 (地板): 核心 + 四角支柱
 * 第1層 (腳部): 四角支柱(可用玻璃) + 中間空氣
 * 第2層 (頭部): 四角支柱(可用玻璃) + 中間空氣
 * 第3層 (天花板): 封頂(可用光源)
 */
public class MultiblockUpgradeChamber {

    public static boolean checkStructure(World world, BlockPos corePos) {
        // 第0層 - 地板
        if (!checkFloorLayer(world, corePos)) return false;

        // 第1層 - 腳部空間 (中間必須空)
        if (!checkPlayerLayer(world, corePos.up())) return false;

        // 第2層 - 頭部空間 (中間必須空) - 新增層級！
        if (!checkPlayerLayer(world, corePos.up(2))) return false;

        // 第3層 - 天花板
        if (!checkCeilingLayer(world, corePos.up(3))) return false;

        return true;
    }

    // === 結構檢查邏輯 ===

    private static boolean checkFloorLayer(World world, BlockPos centerPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();

                if (x == 0 && z == 0) {
                    if (block != ModBlocks.UPGRADE_CHAMBER_CORE) return false;
                } else if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                    if (!isValidStructuralBlock(world, checkPos)) return false;
                }
            }
        }
        return true;
    }

    private static boolean checkPlayerLayer(World world, BlockPos centerPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();

                if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                    // 四角支柱 (強烈推薦用玻璃！)
                    if (!isValidStructuralBlock(world, checkPos)) return false;
                } else {
                    // 中間十字 + 中心必須是空氣 (讓玩家站立)
                    // 這裡檢查 isPassable，允許火把或空氣，但不允許實體方塊
                    if (!block.isPassable(world, checkPos)) {
                        // 簡單處理：只允許空氣
                        if (block != Blocks.AIR) return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean checkCeilingLayer(World world, BlockPos centerPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                // 天花板允許放燈
                if (!isValidStructuralBlock(world, checkPos)) return false;
            }
        }
        return true;
    }

    // === 核心判斷邏輯 ===

    /**
     * 判斷是否為合法的結構方塊 (科幻美學)
     * 允許：玻璃、發光方塊、以及原本的貴重金屬
     */
    private static boolean isValidStructuralBlock(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();

        // 1. 基礎框架
        if (isValidFrameBlock(block)) return true;

        // 2. 科幻透明感
        if (block == Blocks.GLASS || block == Blocks.STAINED_GLASS) return true;
        if (block == Blocks.GLASS_PANE || block == Blocks.STAINED_GLASS_PANE) return true;

        // 3. 光源與能量感
        if (block == Blocks.SEA_LANTERN) return true; // 海燈籠
        if (block == Blocks.GLOWSTONE) return true;   // 螢石
        if (block == Blocks.REDSTONE_LAMP) return true; // 紅石燈
        if (block == Blocks.BEACON) return true;      // 信標(土豪！)

        // 4. 造型
        if (block instanceof BlockSlab) return true;
        if (block instanceof BlockStairs) return true;

        return false;
    }

    private static boolean isValidFrameBlock(Block block) {
        return block == Blocks.IRON_BLOCK ||
                block == Blocks.GOLD_BLOCK ||
                block == Blocks.DIAMOND_BLOCK ||
                block == Blocks.EMERALD_BLOCK;
    }

    /**
     * 等級計算 (掃描4層)
     * 越多綠寶石/鑽石，等級越高
     */
    public static int getFrameTier(World world, BlockPos corePos) {
        int tierScore = 0;

        for (int y = 0; y <= 3; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (y == 0 && x == 0 && z == 0) continue;

                    BlockPos pos = corePos.add(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();

                    if (block == Blocks.EMERALD_BLOCK) tierScore += 4;
                    else if (block == Blocks.DIAMOND_BLOCK) tierScore += 3;
                    else if (block == Blocks.GOLD_BLOCK) tierScore += 2;
                    else if (block == Blocks.IRON_BLOCK) tierScore += 1;
                }
            }
        }

        // 分數閾值調整 (因為現在可以混搭玻璃，所以閾值要設得合理)
        if (tierScore >= 40) return 4; // 大量綠寶石/鑽石
        if (tierScore >= 25) return 3;
        if (tierScore >= 10) return 2;
        return 1;
    }

    public static String getBuildGuide() {
        return "§b結構: 3x3x4\n§7支柱可用: 玻璃/海燈籠/螢石\n§e中間兩層留空給玩家站立";
    }
}