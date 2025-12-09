package com.moremod.multiblock;

import com.moremod.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 重生倉多方塊結構驗證器
 *
 * 結構設計 (3x3x3):
 *
 * 第0層 (地板 - 核心層):
 *   I I I
 *   I C I    C = 重生倉核心, I = 鐵塊/金塊/鑽石塊/黑曜石
 *   I I I
 *
 * 第1層 (玩家空間):
 *   I . I
 *   . . .    . = 空氣 (玩家傳送空間)
 *   I . I
 *
 * 第2層 (天花板):
 *   I I I
 *   I G I    G = 螢光石/海晶燈 (照明)
 *   I I I
 */
public class MultiblockRespawnChamber {

    /**
     * 檢查多方塊結構是否完整
     * @param world 世界
     * @param corePos 核心方塊位置
     * @return 結構是否有效
     */
    public static boolean checkStructure(World world, BlockPos corePos) {
        // 第0層 - 地板 (核心所在層)
        if (!checkFloorLayer(world, corePos)) return false;

        // 第1層 - 玩家空間
        if (!checkMiddleLayer(world, corePos.up())) return false;

        // 第2層 - 天花板
        if (!checkCeilingLayer(world, corePos.up(2))) return false;

        return true;
    }

    /**
     * 第0層 - 地板層
     * 中心是核心，周圍8格是框架
     */
    private static boolean checkFloorLayer(World world, BlockPos centerPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();

                if (x == 0 && z == 0) {
                    // 中心必須是重生倉核心
                    if (block != ModBlocks.RESPAWN_CHAMBER_CORE) {
                        return false;
                    }
                } else {
                    // 周圍8格必須是框架方塊
                    if (!isValidFrameBlock(block)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 第1層 - 中間層（玩家空間）
     * 四角是框架，中間5格是空氣
     */
    private static boolean checkMiddleLayer(World world, BlockPos centerPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();

                // 四角必須是框架
                if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                    if (!isValidFrameBlock(block)) {
                        return false;
                    }
                } else {
                    // 中間（十字區域）必須是空氣或可通過的方塊
                    if (!block.isPassable(world, checkPos)) {
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
     * 周圍是框架，中心是光源
     */
    private static boolean checkCeilingLayer(World world, BlockPos centerPos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();

                if (x == 0 && z == 0) {
                    // 中心必須是光源方塊
                    if (!isValidLightSource(block)) {
                        return false;
                    }
                } else {
                    // 周圍必須是框架
                    if (!isValidFrameBlock(block)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 檢查是否為有效的框架方塊
     * 支持：鐵塊、金塊、鑽石塊、黑曜石
     */
    private static boolean isValidFrameBlock(Block block) {
        return block == Blocks.IRON_BLOCK ||
               block == Blocks.GOLD_BLOCK ||
               block == Blocks.DIAMOND_BLOCK ||
               block == Blocks.OBSIDIAN ||
               block == Blocks.EMERALD_BLOCK;
    }

    /**
     * 檢查是否為有效的光源方塊
     */
    private static boolean isValidLightSource(Block block) {
        return block == Blocks.GLOWSTONE ||
               block == Blocks.SEA_LANTERN ||
               block == Blocks.REDSTONE_LAMP ||
               block == Blocks.BEACON;
    }

    /**
     * 獲取玩家傳送目標位置（核心上方1格）
     */
    public static BlockPos getTeleportPosition(BlockPos corePos) {
        return corePos.up();
    }

    /**
     * 獲取框架等級（影響重生效果）
     * @return 1 = 鐵, 2 = 金, 3 = 鑽石/黑曜石, 4 = 綠寶石
     */
    public static int getFrameTier(World world, BlockPos corePos) {
        int ironCount = 0;
        int goldCount = 0;
        int diamondCount = 0;
        int emeraldCount = 0;
        int obsidianCount = 0;

        // 統計所有框架方塊 (3x3x3結構)
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (y == 0 && x == 0 && z == 0) continue; // 跳過核心
                    if (y == 2 && x == 0 && z == 0) continue; // 跳過光源

                    BlockPos checkPos = corePos.add(x, y, z);
                    Block block = world.getBlockState(checkPos).getBlock();

                    if (block == Blocks.IRON_BLOCK) ironCount++;
                    else if (block == Blocks.GOLD_BLOCK) goldCount++;
                    else if (block == Blocks.DIAMOND_BLOCK) diamondCount++;
                    else if (block == Blocks.OBSIDIAN) obsidianCount++;
                    else if (block == Blocks.EMERALD_BLOCK) emeraldCount++;
                }
            }
        }

        // 根據最多的材料決定等級
        if (emeraldCount >= 10) return 4;
        if (diamondCount >= 10 || obsidianCount >= 10) return 3;
        if (goldCount >= 10) return 2;
        return 1;
    }

    /**
     * 獲取建造指南
     */
    public static String getBuildGuide() {
        StringBuilder guide = new StringBuilder();
        guide.append("§b=== 重生倉建造指南 ===§r\n\n");

        guide.append("§e第0層（地板）:§r\n");
        guide.append("  I I I\n");
        guide.append("  I C I  ← 重生倉核心\n");
        guide.append("  I I I\n\n");

        guide.append("§e第1層（玩家空間）:§r\n");
        guide.append("  I . I\n");
        guide.append("  . . .  ← 傳送目標\n");
        guide.append("  I . I\n\n");

        guide.append("§e第2層（天花板）:§r\n");
        guide.append("  I I I\n");
        guide.append("  I G I  ← 光源（螢光石/海晶燈）\n");
        guide.append("  I I I\n\n");

        guide.append("§6圖例:§r\n");
        guide.append("C = 重生倉核心\n");
        guide.append("I = 框架方塊（鐵/金/鑽石/黑曜石/綠寶石塊）\n");
        guide.append("G = 光源（螢光石/海晶燈/紅石燈/信標）\n");
        guide.append(". = 空氣\n\n");

        guide.append("§a使用方法:§r\n");
        guide.append("1. 右鍵重生倉核心進行綁定\n");
        guide.append("2. 當破碎之神停機重啟時，自動傳送至此\n\n");

        guide.append("§c注意:§r 結構不完整時無法綁定！\n");

        return guide.toString();
    }
}
