package com.moremod.multiblock;

import com.moremod.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 升級艙多方塊結構驗證器
 *
 * 結構設計 (3x3x4):
 *
 * 第0層 (地板 - 核心層) - 四角框架，中間開放供管道連接:
 *   I . I
 *   . C .    C = 升級艙核心, I = 鐵塊, . = 任意(管道空間)
 *   I . I
 *
 * 第1層 (玩家空間下層):
 *   I . I
 *   . . .    . = 空氣 (玩家進入空間)
 *   I . I
 *
 * 第2層 (玩家空間上層):
 *   I . I
 *   . . .    . = 空氣 (玩家頭部空間)
 *   I . I
 *
 * 第3層 (天花板):
 *   I I I
 *   I I I
 *   I I I
 */
public class MultiblockUpgradeChamber {

    /**
     * 檢查多方塊結構是否完整
     * @param world 世界
     * @param corePos 核心方塊位置
     * @return 結構是否有效
     */
    public static boolean checkStructure(World world, BlockPos corePos) {
        // 第0層 - 地板 (核心所在層)
        if (!checkFloorLayer(world, corePos)) return false;

        // 第1層 - 玩家空間下層
        if (!checkMiddleLayer(world, corePos.up())) return false;

        // 第2層 - 玩家空間上層
        if (!checkMiddleLayer(world, corePos.up(2))) return false;

        // 第3層 - 天花板
        if (!checkCeilingLayer(world, corePos.up(3))) return false;

        return true;
    }

    /**
     * 第0層 - 地板層（四角框架，中間開放供管道連接）
     * 中心是核心，四角是框架，四邊可接管道
     */
    private static boolean checkFloorLayer(World world, BlockPos centerPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();

                if (x == 0 && z == 0) {
                    // 中心必須是升級艙核心
                    if (block != ModBlocks.UPGRADE_CHAMBER_CORE) {
                        return false;
                    }
                } else if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                    // 四角必須是框架方塊
                    if (!isValidFrameBlock(block)) {
                        return false;
                    }
                }
                // 四邊（十字位置）可以是任意方塊，供管道連接
            }
        }
        return true;
    }

    /**
     * 第1層 - 中間層
     * 四角是鐵塊，中間5格是空氣（玩家進入空間）
     */
    private static boolean checkMiddleLayer(World world, BlockPos centerPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();

                // 四角必須是鐵塊
                if ((Math.abs(x) == 1 && Math.abs(z) == 1)) {
                    if (!isValidFrameBlock(block)) {
                        return false;
                    }
                } else {
                    // 中間必須是空氣（或可通過的方塊）
                    if (!block.isPassable(world, checkPos)) {
                        // 允許玩家在裡面
                        if (block != Blocks.AIR) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * 第2層 - 天花板層
     * 全部是鐵塊
     */
    private static boolean checkCeilingLayer(World world, BlockPos centerPos) {
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
     * 檢查是否為有效的框架方塊
     * 支持：鐵塊、金塊、鑽石塊（更高級的框架提供更快的升級速度）
     */
    private static boolean isValidFrameBlock(Block block) {
        return block == Blocks.IRON_BLOCK ||
               block == Blocks.GOLD_BLOCK ||
               block == Blocks.DIAMOND_BLOCK ||
               block == Blocks.EMERALD_BLOCK;
    }

    /**
     * 獲取框架等級（影響升級速度和能量效率）
     * @return 1 = 鐵, 2 = 金, 3 = 鑽石, 4 = 綠寶石
     */
    public static int getFrameTier(World world, BlockPos corePos) {
        int ironCount = 0;
        int goldCount = 0;
        int diamondCount = 0;
        int emeraldCount = 0;

        // 統計所有框架方塊 (3x3x4結構)
        for (int y = 0; y <= 3; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (y == 0 && x == 0 && z == 0) continue; // 跳過核心

                    BlockPos checkPos = corePos.add(x, y, z);
                    Block block = world.getBlockState(checkPos).getBlock();

                    if (block == Blocks.IRON_BLOCK) ironCount++;
                    else if (block == Blocks.GOLD_BLOCK) goldCount++;
                    else if (block == Blocks.DIAMOND_BLOCK) diamondCount++;
                    else if (block == Blocks.EMERALD_BLOCK) emeraldCount++;
                }
            }
        }

        // 根據最多的材料決定等級
        if (emeraldCount >= 12) return 4;
        if (diamondCount >= 12) return 3;
        if (goldCount >= 12) return 2;
        return 1;
    }

    /**
     * 獲取建造指南
     */
    public static String getBuildGuide() {
        StringBuilder guide = new StringBuilder();
        guide.append("§b=== 模組升級艙建造指南 ===§r\n\n");

        guide.append("§e第0層（地板）:§r\n");
        guide.append("  I . I\n");
        guide.append("  . C .  ← 管道連接處\n");
        guide.append("  I . I\n\n");

        guide.append("§e第1層（玩家下層）:§r\n");
        guide.append("  I . I\n");
        guide.append("  . . .  ← 玩家進入\n");
        guide.append("  I . I\n\n");

        guide.append("§e第2層（玩家上層）:§r\n");
        guide.append("  I . I\n");
        guide.append("  . . .  ← 頭部空間\n");
        guide.append("  I . I\n\n");

        guide.append("§e第3層（天花板）:§r\n");
        guide.append("  I I I\n");
        guide.append("  I I I\n");
        guide.append("  I I I\n\n");

        guide.append("§6圖例:§r\n");
        guide.append("C = 升級艙核心\n");
        guide.append("I = 框架方塊（鐵/金/鑽石/綠寶石塊）\n");
        guide.append(". = 空氣/管道\n\n");

        guide.append("§a使用方法:§r\n");
        guide.append("1. 將升級模組放入核心\n");
        guide.append("2. 用管道輸入能量\n");
        guide.append("3. 裝備機械核心，走進升級艙\n");
        guide.append("4. 等待升級完成\n\n");

        guide.append("§c注意:§r 能量不足會導致升級失敗！\n");

        return guide.toString();
    }
}
