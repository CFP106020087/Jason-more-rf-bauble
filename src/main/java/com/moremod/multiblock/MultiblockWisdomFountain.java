package com.moremod.multiblock;

import com.moremod.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MultiblockWisdomFountain {

    public static boolean checkStructure(World world, BlockPos centerPos) {
        // 检查核心3x3x3结构
        if (!checkCoreStructure(world, centerPos)) return false;

        // 检查外围装饰石英凹槽（7x7）
        if (!checkOuterRim(world, centerPos)) return false;

        // 检查顶部水源
        if (!checkTopWaterSource(world, centerPos.up(3))) return false;

        return true;
    }

    // 核心3x3x3结构
    private static boolean checkCoreStructure(World world, BlockPos centerPos) {
        // 第一层（基座）
        if (!checkLayer1(world, centerPos)) return false;

        // 第二层（水池）
        if (!checkLayer2(world, centerPos.up())) return false;

        // 第三层（顶部）
        if (!checkLayer3(world, centerPos.up(2))) return false;

        return true;
    }

    private static boolean checkLayer1(World world, BlockPos pos) {
        // 3x3守护者石块基座，中心是智慧之泉核心
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = pos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();

                if (x == 0 && z == 0) {
                    // 中心必须是智慧之泉核心
                    if (block != ModBlocks.WISDOM_FOUNTAIN_CORE) {
                        return false;
                    }
                } else {
                    // 周围是守护者石块
                    if (block != ModBlocks.GUARDIAN_STONE_BLOCK) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean checkLayer2(World world, BlockPos pos) {
        // 3x3符文虚空石环，中心是水
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = pos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();

                if (x == 0 && z == 0) {
                    // 中心必须是水
                    if (block != Blocks.WATER && block != Blocks.FLOWING_WATER) {
                        return false;
                    }
                } else {
                    // 周围是符文虚空石块
                    if (block != ModBlocks.RUNED_VOID_STONE_BLOCK) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean checkLayer3(World world, BlockPos pos) {
        // 顶层：中心是远古核心块，四角是海晶灯
        BlockPos centerPos = pos.add(0, 0, 0);
        if (world.getBlockState(centerPos).getBlock() != ModBlocks.ANCIENT_CORE_BLOCK) {
            return false;
        }

        // 四角海晶灯
        BlockPos[] corners = {
                pos.add(-1, 0, -1),
                pos.add(-1, 0, 1),
                pos.add(1, 0, -1),
                pos.add(1, 0, 1)
        };

        for (BlockPos corner : corners) {
            if (world.getBlockState(corner).getBlock() != Blocks.SEA_LANTERN) {
                return false;
            }
        }

        return true;
    }

    // 外围装饰石英凹槽（7x7）
    private static boolean checkOuterRim(World world, BlockPos centerPos) {
        // 7x7外围，距离核心1格

        // 检查凹槽底部（Y-1层）
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                // 跳过内部3x3区域
                if (Math.abs(x) <= 1 && Math.abs(z) <= 1) continue;

                // 跳过四角
                if (Math.abs(x) == 3 && Math.abs(z) == 3) continue;

                // 凹槽区域（第2圈）
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    BlockPos checkPos = centerPos.add(x, -1, z);
                    Block block = world.getBlockState(checkPos).getBlock();

                    // 凹槽底部必须是石英块
                    if (block != Blocks.QUARTZ_BLOCK) {
                        return false;
                    }
                }
            }
        }

        // 检查地面层石英边框（Y+0层）
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                // 跳过内部和四角
                if (Math.abs(x) <= 1 && Math.abs(z) <= 1) continue;
                if (Math.abs(x) == 3 && Math.abs(z) == 3) continue;

                BlockPos checkPos = centerPos.add(x, 0, z);
                Block block = world.getBlockState(checkPos).getBlock();

                // 最外圈必须是石英块（边框）
                if (Math.abs(x) == 3 || Math.abs(z) == 3) {
                    if (block != Blocks.QUARTZ_BLOCK) {
                        return false;
                    }
                }
                // 凹槽区域可以是空的
                else if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    // 允许是空气、水或其他装饰
                    // 不做强制要求
                }
            }
        }

        return true;
    }

    // 顶部必须有水源（喷泉效果）
    private static boolean checkTopWaterSource(World world, BlockPos pos) {
        Block centerWater = world.getBlockState(pos).getBlock();

        // 核心正上方必须是水
        if (centerWater != Blocks.WATER && centerWater != Blocks.FLOWING_WATER) {
            return false;
        }

        return true;
    }

    // 激活喷泉时的特效
    public static void activateFountain(World world, BlockPos centerPos) {
        if (world.isRemote) {
            // 喷泉粒子效果
            BlockPos waterPos = centerPos.up(3);

            for (int i = 0; i < 20; i++) {
                double x = waterPos.getX() + 0.5 + (world.rand.nextDouble() - 0.5) * 0.3;
                double y = waterPos.getY() + 0.2;
                double z = waterPos.getZ() + 0.5 + (world.rand.nextDouble() - 0.5) * 0.3;

                world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.WATER_SPLASH,
                        x, y, z,
                        (world.rand.nextDouble() - 0.5) * 0.2,
                        0.4,
                        (world.rand.nextDouble() - 0.5) * 0.2
                );
            }

            // 魔法粒子效果
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10;
                double radius = 2.5;
                double x = centerPos.getX() + 0.5 + Math.cos(angle) * radius;
                double y = centerPos.getY() + 0.5;
                double z = centerPos.getZ() + 0.5 + Math.sin(angle) * radius;

                world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.ENCHANTMENT_TABLE,
                        x, y, z,
                        0, 0.2, 0
                );
            }
        }
    }

    // 构建指南
    public static String getBuildGuide() {
        StringBuilder guide = new StringBuilder();
        guide.append("§b=== 智慧之泉建造指南 ===§r\n\n");

        guide.append("§e第0层（基座）:§r\n");
        guide.append("  Q Q Q Q Q  \n");
        guide.append("Q Q - - - Q Q\n");
        guide.append("Q - G G G - Q\n");
        guide.append("Q - G W G - Q\n");
        guide.append("Q - G G G - Q\n");
        guide.append("Q Q - - - Q Q\n");
        guide.append("  Q Q Q Q Q  \n\n");

        guide.append("§e第1层（水池）:§r\n");
        guide.append("    R R R    \n");
        guide.append("    R W R    \n");
        guide.append("    R R R    \n\n");

        guide.append("§e第2层（顶部）:§r\n");
        guide.append("    S . S    \n");
        guide.append("    . A .    \n");
        guide.append("    S . S    \n\n");

        guide.append("§e第3层:§r 中心放置水源\n\n");

        guide.append("§6图例:§r\n");
        guide.append("Q = 石英块\n");
        guide.append("- = 凹槽（深1格）\n");
        guide.append("G = 守护者石块\n");
        guide.append("W = 智慧之泉核心/水\n");
        guide.append("R = 符文虚空石块\n");
        guide.append("A = 远古核心块\n");
        guide.append("S = 海晶灯\n");

        return guide.toString();
    }
}