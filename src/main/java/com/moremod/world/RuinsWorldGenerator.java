package com.moremod.world;

import com.moremod.init.ModBlocks;
import com.moremod.init.ModItems;
import com.moremod.printer.ItemPrintTemplate;
import com.moremod.printer.PrinterRecipe;
import com.moremod.printer.PrinterRecipeRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * 科技废墟世界生成器 v2.0
 *
 * 在主世界野外生成残破的科技感废墟建筑
 * 内含稀有机械方块、打印模版和故障装备
 *
 * 改进：
 * - 生成前清除周围方块，不做平坦度检查
 * - 更精细的建筑细节
 * - 更丰富的装饰元素
 */
public class RuinsWorldGenerator implements IWorldGenerator {

    // 生成配置
    private static final int MIN_Y = 50;            // 最低生成高度
    private static final int SPAWN_CHANCE = 120;    // 生成概率 (1/120 区块)
    private static final int MIN_DISTANCE_FROM_SPAWN = 250;  // 距离出生点最小距离

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {

        // 只在主世界生成
        if (world.provider.getDimension() != 0) {
            return;
        }

        // 随机概率检查
        if (random.nextInt(SPAWN_CHANCE) != 0) {
            return;
        }

        int worldX = chunkX * 16 + 8;
        int worldZ = chunkZ * 16 + 8;

        // 检查距离出生点的距离
        if (Math.abs(worldX) < MIN_DISTANCE_FROM_SPAWN && Math.abs(worldZ) < MIN_DISTANCE_FROM_SPAWN) {
            return;
        }

        // 找到地表高度
        BlockPos basePos = findGroundLevel(world, worldX, worldZ, random);
        if (basePos == null) {
            return;
        }

        // 随机选择废墟类型
        int ruinType = random.nextInt(6);
        switch (ruinType) {
            case 0:
                generateResearchOutpost(world, basePos, random);
                break;
            case 1:
                generateMechanicalComplex(world, basePos, random);
                break;
            case 2:
                generateSignalTower(world, basePos, random);
                break;
            case 3:
                generateUndergroundVault(world, basePos, random);
                break;
            case 4:
                generateCrashedTransport(world, basePos, random);
                break;
            case 5:
                generateFactoryRuins(world, basePos, random);
                break;
        }

        System.out.println("[Ruins] 在 " + basePos + " 生成了科技废墟 (类型: " + ruinType + ")");
    }

    /**
     * 找到地表高度（不检查平坦度）
     */
    private BlockPos findGroundLevel(World world, int x, int z, Random random) {
        x += random.nextInt(8) - 4;
        z += random.nextInt(8) - 4;

        // 从高处向下搜索第一个实心方块
        for (int y = 200; y >= MIN_Y; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            IBlockState belowState = world.getBlockState(pos.down());

            if (state.getBlock() == Blocks.AIR && belowState.getMaterial().isSolid()) {
                return pos;
            }
        }
        return null;
    }

    /**
     * 清除建筑区域（移除所有方块）
     */
    private void clearBuildingArea(World world, BlockPos center, int radiusX, int radiusZ, int height) {
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                for (int y = 0; y <= height; y++) {
                    setBlockSafe(world, center.add(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    /**
     * 平整地基（填充实心方块）
     */
    private void levelGround(World world, BlockPos center, int radiusX, int radiusZ, int depth) {
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                for (int y = -1; y >= -depth; y--) {
                    BlockPos pos = center.add(x, y, z);
                    if (world.isAirBlock(pos) || !world.getBlockState(pos).getMaterial().isSolid()) {
                        setBlockSafe(world, pos, Blocks.STONEBRICK.getDefaultState());
                    }
                }
            }
        }
    }

    /**
     * 类型0: 研究前哨站 (8x8x6) - 小型科研设施
     */
    private void generateResearchOutpost(World world, BlockPos pos, Random random) {
        // 清除并平整区域
        clearBuildingArea(world, pos, 5, 5, 8);
        levelGround(world, pos, 5, 5, 3);

        // 混凝土地基
        fillArea(world, pos.add(-4, -1, -4), pos.add(4, -1, 4), Blocks.CONCRETE.getStateFromMeta(8));

        // 主体框架 - 钢铁骨架
        for (int y = 0; y <= 5; y++) {
            // 四角立柱
            for (int[] c : new int[][]{{-4,-4}, {-4,4}, {4,-4}, {4,4}}) {
                if (random.nextFloat() > 0.15f) {
                    setBlockSafe(world, pos.add(c[0], y, c[1]), Blocks.IRON_BLOCK.getDefaultState());
                }
            }
        }

        // 墙壁 - 分层设计
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                boolean isWall = (Math.abs(x) == 3 || Math.abs(z) == 3);
                if (!isWall) continue;

                for (int y = 0; y <= 4; y++) {
                    if (random.nextFloat() > 0.25f) {
                        if (y < 2) {
                            setBlockSafe(world, pos.add(x, y, z), getRandomRuinBlock(random));
                        } else if (y == 2) {
                            // 窗户层
                            setBlockSafe(world, pos.add(x, y, z), Blocks.IRON_BARS.getDefaultState());
                        } else {
                            setBlockSafe(world, pos.add(x, y, z), Blocks.QUARTZ_BLOCK.getDefaultState());
                        }
                    }
                }
            }
        }

        // 内部设施
        // 实验桌
        setBlockSafe(world, pos.add(-1, 0, 0), Blocks.BREWING_STAND.getDefaultState());
        setBlockSafe(world, pos.add(1, 0, 0), Blocks.CAULDRON.getDefaultState());

        // 电脑终端（用红石灯代表）
        setBlockSafe(world, pos.add(-2, 0, -2), Blocks.REDSTONE_LAMP.getDefaultState());
        setBlockSafe(world, pos.add(-2, 1, -2), Blocks.DAYLIGHT_DETECTOR.getDefaultState());

        // 能量核心区
        setBlockSafe(world, pos.add(2, 0, 2), Blocks.REDSTONE_BLOCK.getDefaultState());
        setBlockSafe(world, pos.add(2, 1, 2), Blocks.SEA_LANTERN.getDefaultState());

        // 管道和线缆
        for (int i = -2; i <= 2; i++) {
            if (random.nextFloat() > 0.3f) {
                setBlockSafe(world, pos.add(i, 4, -3), Blocks.END_ROD.getDefaultState());
                setBlockSafe(world, pos.add(i, 4, 3), Blocks.END_ROD.getDefaultState());
            }
        }

        // 屋顶（破损）
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (random.nextFloat() > 0.35f) {
                    setBlockSafe(world, pos.add(x, 5, z), Blocks.STONE_SLAB.getDefaultState());
                }
            }
        }

        placeRuinContents(world, pos, random, 2);
    }

    /**
     * 类型1: 机械综合体 (12x12x8) - 中型工业设施
     */
    private void generateMechanicalComplex(World world, BlockPos pos, Random random) {
        clearBuildingArea(world, pos, 7, 7, 10);
        levelGround(world, pos, 7, 7, 4);

        // 厚实地基
        fillArea(world, pos.add(-6, -2, -6), pos.add(6, -1, 6), Blocks.STONEBRICK.getDefaultState());

        // 外墙框架
        for (int y = 0; y <= 7; y++) {
            // 加强柱
            for (int[] c : new int[][]{{-6,-6}, {-6,6}, {6,-6}, {6,6}, {-6,0}, {6,0}, {0,-6}, {0,6}}) {
                if (random.nextFloat() > 0.2f) {
                    IBlockState pillar = y < 4 ? Blocks.IRON_BLOCK.getDefaultState() : Blocks.QUARTZ_BLOCK.getDefaultState();
                    setBlockSafe(world, pos.add(c[0], y, c[1]), pillar);
                }
            }

            // 横梁
            if (y == 3 || y == 6) {
                for (int i = -5; i <= 5; i++) {
                    if (random.nextFloat() > 0.25f) {
                        setBlockSafe(world, pos.add(i, y, -6), Blocks.IRON_BARS.getDefaultState());
                        setBlockSafe(world, pos.add(i, y, 6), Blocks.IRON_BARS.getDefaultState());
                        setBlockSafe(world, pos.add(-6, y, i), Blocks.IRON_BARS.getDefaultState());
                        setBlockSafe(world, pos.add(6, y, i), Blocks.IRON_BARS.getDefaultState());
                    }
                }
            }
        }

        // 内部生产线
        // 传送带（活塞模拟）
        for (int z = -4; z <= 4; z += 2) {
            setBlockSafe(world, pos.add(-3, 0, z), Blocks.PISTON.getDefaultState());
            setBlockSafe(world, pos.add(3, 0, z), Blocks.STICKY_PISTON.getDefaultState());
        }

        // 中央加工单元
        fillArea(world, pos.add(-1, 0, -1), pos.add(1, 2, 1), Blocks.IRON_BLOCK.getDefaultState());
        setBlockSafe(world, pos.add(0, 1, 0), Blocks.REDSTONE_BLOCK.getDefaultState());
        setBlockSafe(world, pos.add(0, 3, 0), Blocks.BEACON.getDefaultState());

        // 储存区
        setBlockSafe(world, pos.add(-4, 0, -4), Blocks.HOPPER.getDefaultState());
        setBlockSafe(world, pos.add(-4, 1, -4), Blocks.CHEST.getDefaultState());
        setBlockSafe(world, pos.add(4, 0, 4), Blocks.HOPPER.getDefaultState());
        setBlockSafe(world, pos.add(4, 1, 4), Blocks.CHEST.getDefaultState());

        // 电力系统
        for (int x = -2; x <= 2; x += 2) {
            for (int z = -2; z <= 2; z += 2) {
                if (random.nextBoolean()) {
                    setBlockSafe(world, pos.add(x, 0, z), Blocks.REDSTONE_LAMP.getDefaultState());
                }
            }
        }

        placeRuinContents(world, pos, random, 3);
    }

    /**
     * 类型2: 信号塔 (6x6x18) - 高层通讯塔
     */
    private void generateSignalTower(World world, BlockPos pos, Random random) {
        clearBuildingArea(world, pos, 4, 4, 20);
        levelGround(world, pos, 4, 4, 3);

        // 塔基
        fillArea(world, pos.add(-3, -1, -3), pos.add(3, 1, 3), Blocks.STONEBRICK.getDefaultState());
        fillArea(world, pos.add(-2, 0, -2), pos.add(2, 1, 2), Blocks.AIR.getDefaultState());

        // 塔身
        for (int y = 2; y <= 16; y++) {
            int radius = y < 10 ? 3 : (y < 14 ? 2 : 1);
            float damage = 0.1f + (y * 0.03f);

            // 边框
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    boolean isEdge = (Math.abs(x) == radius || Math.abs(z) == radius);
                    if (isEdge && random.nextFloat() > damage) {
                        IBlockState block = y % 4 == 0 ? Blocks.QUARTZ_BLOCK.getDefaultState() : getRandomRuinBlock(random);
                        setBlockSafe(world, pos.add(x, y, z), block);
                    }
                }
            }

            // 内部楼梯
            if (y % 2 == 0 && radius > 1) {
                setBlockSafe(world, pos.add(0, y, 0), Blocks.OAK_STAIRS.getDefaultState());
            }

            // 楼层平台
            if (y % 5 == 0 && y < 15) {
                for (int x = -radius+1; x <= radius-1; x++) {
                    for (int z = -radius+1; z <= radius-1; z++) {
                        if (random.nextFloat() > 0.3f) {
                            setBlockSafe(world, pos.add(x, y, z), Blocks.IRON_TRAPDOOR.getDefaultState());
                        }
                    }
                }
            }
        }

        // 天线
        for (int y = 17; y <= 20; y++) {
            setBlockSafe(world, pos.add(0, y, 0), Blocks.END_ROD.getDefaultState());
        }

        // 顶部信号灯
        setBlockSafe(world, pos.add(0, 21, 0), Blocks.REDSTONE_LAMP.getDefaultState());
        setBlockSafe(world, pos.add(0, 22, 0), Blocks.REDSTONE_BLOCK.getDefaultState());

        // 塔基设备
        setBlockSafe(world, pos.add(-2, 0, -2), Blocks.JUKEBOX.getDefaultState());
        setBlockSafe(world, pos.add(2, 0, 2), Blocks.REDSTONE_LAMP.getDefaultState());

        placeRuinContents(world, pos, random, 2);
    }

    /**
     * 类型3: 地下金库 (10x10x-8) - 深埋地下的安全设施
     */
    private void generateUndergroundVault(World world, BlockPos pos, Random random) {
        BlockPos vaultPos = pos.down(10);

        // 挖掘地下空间
        fillArea(world, vaultPos.add(-5, 0, -5), vaultPos.add(5, 6, 5), Blocks.AIR.getDefaultState());

        // 加固外壳
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = -1; y <= 7; y++) {
                    boolean isShell = (Math.abs(x) == 5 || Math.abs(z) == 5 || y == -1 || y == 7);
                    if (isShell) {
                        IBlockState shell = y == -1 ? Blocks.OBSIDIAN.getDefaultState() : Blocks.STONEBRICK.getDefaultState();
                        setBlockSafe(world, vaultPos.add(x, y, z), shell);
                    }
                }
            }
        }

        // 入口竖井（螺旋楼梯）
        for (int y = 0; y <= 9; y++) {
            int dx = (y % 4 == 0) ? 1 : (y % 4 == 1) ? 0 : (y % 4 == 2) ? -1 : 0;
            int dz = (y % 4 == 0) ? 0 : (y % 4 == 1) ? 1 : (y % 4 == 2) ? 0 : -1;

            setBlockSafe(world, pos.add(dx, -y, dz), Blocks.STONE_SLAB.getDefaultState());
            setBlockSafe(world, pos.add(0, -y, 0), Blocks.AIR.getDefaultState());
        }

        // 内部分区
        // 储藏室
        fillArea(world, vaultPos.add(-4, 1, -4), vaultPos.add(-2, 3, -2), Blocks.IRON_BARS.getDefaultState());
        fillArea(world, vaultPos.add(-3, 1, -3), vaultPos.add(-3, 2, -3), Blocks.AIR.getDefaultState());

        // 控制室
        setBlockSafe(world, vaultPos.add(3, 1, -3), Blocks.REDSTONE_LAMP.getDefaultState());
        setBlockSafe(world, vaultPos.add(3, 2, -3), Blocks.LEVER.getDefaultState());
        setBlockSafe(world, vaultPos.add(2, 1, -3), Blocks.STONE_BUTTON.getDefaultState());

        // 能量核心
        setBlockSafe(world, vaultPos.add(0, 1, 3), Blocks.GLOWSTONE.getDefaultState());
        setBlockSafe(world, vaultPos.add(0, 2, 3), Blocks.BEACON.getDefaultState());

        // 照明
        for (int x = -3; x <= 3; x += 3) {
            for (int z = -3; z <= 3; z += 3) {
                setBlockSafe(world, vaultPos.add(x, 5, z), Blocks.SEA_LANTERN.getDefaultState());
            }
        }

        placeRuinContents(world, vaultPos.up(), random, 4);
    }

    /**
     * 类型4: 坠毁运输船 (14x8x6) - 倾斜的飞船残骸
     */
    private void generateCrashedTransport(World world, BlockPos pos, Random random) {
        // 撞击坑
        for (int x = -6; x <= 6; x++) {
            for (int z = -4; z <= 4; z++) {
                int depth = 3 - (int) Math.sqrt(x * x / 2.0 + z * z);
                if (depth > 0) {
                    for (int y = 0; y > -depth; y--) {
                        setBlockSafe(world, pos.add(x, y, z), Blocks.AIR.getDefaultState());
                    }
                    setBlockSafe(world, pos.add(x, -depth, z), Blocks.SOUL_SAND.getDefaultState());
                }
            }
        }

        // 船体框架（倾斜）
        for (int x = -5; x <= 5; x++) {
            int tilt = x / 3;  // 倾斜
            for (int z = -3; z <= 3; z++) {
                // 底部
                if (random.nextFloat() > 0.3f) {
                    setBlockSafe(world, pos.add(x, tilt, z), Blocks.IRON_BLOCK.getDefaultState());
                }
                // 侧面
                if (Math.abs(z) == 3 && random.nextFloat() > 0.35f) {
                    setBlockSafe(world, pos.add(x, tilt + 1, z), getRandomRuinBlock(random));
                    setBlockSafe(world, pos.add(x, tilt + 2, z), Blocks.IRON_BARS.getDefaultState());
                }
            }
        }

        // 驾驶舱
        fillArea(world, pos.add(-5, 1, -1), pos.add(-4, 3, 1), Blocks.GLASS.getDefaultState());
        setBlockSafe(world, pos.add(-5, 2, 0), Blocks.REDSTONE_LAMP.getDefaultState());

        // 引擎残骸
        setBlockSafe(world, pos.add(4, 3, -2), Blocks.FURNACE.getDefaultState());
        setBlockSafe(world, pos.add(4, 3, 2), Blocks.FURNACE.getDefaultState());
        setBlockSafe(world, pos.add(5, 3, 0), Blocks.REDSTONE_BLOCK.getDefaultState());

        // 散落的货物
        for (int i = 0; i < 10; i++) {
            int rx = random.nextInt(14) - 7;
            int rz = random.nextInt(10) - 5;
            Block cargo = random.nextInt(4) == 0 ? Blocks.CHEST :
                         random.nextInt(3) == 0 ? Blocks.IRON_BLOCK : Blocks.QUARTZ_BLOCK;
            setBlockSafe(world, pos.add(rx, 0, rz), cargo.getDefaultState());
        }

        // 火焰和烟雾效果（用火把代替）
        for (int i = 0; i < 5; i++) {
            setBlockSafe(world, pos.add(random.nextInt(8) - 4, 1, random.nextInt(6) - 3),
                        Blocks.TORCH.getDefaultState());
        }

        placeRuinContents(world, pos, random, 3);
    }

    /**
     * 类型5: 废弃工厂 (16x16x10) - 大型工业废墟
     */
    private void generateFactoryRuins(World world, BlockPos pos, Random random) {
        clearBuildingArea(world, pos, 9, 9, 12);
        levelGround(world, pos, 9, 9, 4);

        // 厚实地基
        fillArea(world, pos.add(-8, -2, -8), pos.add(8, -1, 8), Blocks.STONEBRICK.getDefaultState());

        // 主厂房框架
        for (int y = 0; y <= 9; y++) {
            // 承重柱
            for (int x = -7; x <= 7; x += 7) {
                for (int z = -7; z <= 7; z += 7) {
                    if (random.nextFloat() > 0.15f) {
                        setBlockSafe(world, pos.add(x, y, z), Blocks.IRON_BLOCK.getDefaultState());
                    }
                }
            }

            // 中间支撑
            if (y == 0 || y == 4 || y == 8) {
                for (int x = -6; x <= 6; x++) {
                    if (random.nextFloat() > 0.3f) {
                        setBlockSafe(world, pos.add(x, y, -7), getRandomRuinBlock(random));
                        setBlockSafe(world, pos.add(x, y, 7), getRandomRuinBlock(random));
                    }
                }
                for (int z = -6; z <= 6; z++) {
                    if (random.nextFloat() > 0.3f) {
                        setBlockSafe(world, pos.add(-7, y, z), getRandomRuinBlock(random));
                        setBlockSafe(world, pos.add(7, y, z), getRandomRuinBlock(random));
                    }
                }
            }
        }

        // 生产设备
        // 熔炉阵列
        for (int z = -5; z <= 5; z += 2) {
            setBlockSafe(world, pos.add(-5, 0, z), Blocks.FURNACE.getDefaultState());
            setBlockSafe(world, pos.add(-5, 1, z), Blocks.HOPPER.getDefaultState());
        }

        // 中央组装台
        fillArea(world, pos.add(-2, 0, -2), pos.add(2, 0, 2), Blocks.CRAFTING_TABLE.getDefaultState());
        setBlockSafe(world, pos.add(0, 1, 0), Blocks.ANVIL.getDefaultState());

        // 仓储区
        for (int x = 3; x <= 5; x++) {
            for (int z = -5; z <= 5; z += 2) {
                setBlockSafe(world, pos.add(x, 0, z), Blocks.CHEST.getDefaultState());
            }
        }

        // 传送带系统
        for (int z = -4; z <= 4; z++) {
            setBlockSafe(world, pos.add(0, 0, z), Blocks.PISTON.getDefaultState());
        }

        // 烟囱
        for (int y = 0; y <= 14; y++) {
            float decay = 0.1f + (y * 0.04f);
            if (random.nextFloat() > decay) {
                setBlockSafe(world, pos.add(5, y, -5), Blocks.BRICK_BLOCK.getDefaultState());
                setBlockSafe(world, pos.add(6, y, -5), Blocks.BRICK_BLOCK.getDefaultState());
                setBlockSafe(world, pos.add(5, y, -6), Blocks.BRICK_BLOCK.getDefaultState());
                setBlockSafe(world, pos.add(6, y, -6), Blocks.BRICK_BLOCK.getDefaultState());
            }
        }

        // 控制室
        fillArea(world, pos.add(-6, 5, -6), pos.add(-4, 7, -4), Blocks.GLASS.getDefaultState());
        setBlockSafe(world, pos.add(-5, 5, -5), Blocks.REDSTONE_LAMP.getDefaultState());
        setBlockSafe(world, pos.add(-5, 6, -5), Blocks.AIR.getDefaultState());

        // 屋顶残片
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                if (random.nextFloat() > 0.55f) {
                    setBlockSafe(world, pos.add(x, 9, z), Blocks.STONE_SLAB.getDefaultState());
                }
            }
        }

        placeRuinContents(world, pos, random, 4);
    }

    /**
     * 放置废墟内容物（特殊方块和战利品）
     */
    private void placeRuinContents(World world, BlockPos center, Random random, int lootTier) {
        // 放置战利品箱
        BlockPos chestPos = center.add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
        setBlockSafe(world, chestPos, Blocks.CHEST.getDefaultState());

        // 填充战利品
        TileEntity te = world.getTileEntity(chestPos);
        if (te instanceof TileEntityChest) {
            fillChestWithLoot((TileEntityChest) te, random, lootTier);
        }

        // 随机放置特殊机械方块 (非常稀有)
        if (random.nextFloat() < 0.15f * lootTier) {  // 5-15%概率
            BlockPos specialPos = center.add(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            placeSpecialBlock(world, specialPos, random);
        }
    }

    /**
     * 填充战利品箱
     */
    private void fillChestWithLoot(TileEntityChest chest, Random random, int tier) {
        // 基础材料
        int materialCount = 3 + random.nextInt(5);
        for (int i = 0; i < materialCount; i++) {
            int slot = random.nextInt(27);
            ItemStack material = getRandomMaterial(random, tier);
            chest.setInventorySlotContents(slot, material);
        }

        // 打印模版 (稀有)
        if (random.nextFloat() < 0.1f * tier) {
            int slot = random.nextInt(27);
            ItemStack template = createRandomTemplate(random);
            chest.setInventorySlotContents(slot, template);
        }

        // 故障装备 (非常稀有) - 暂时用钻石装备代替
        if (random.nextFloat() < 0.05f * tier) {
            int slot = random.nextInt(27);
            ItemStack gear = getRandomGear(random);
            chest.setInventorySlotContents(slot, gear);
        }
    }

    /**
     * 获取随机废墟方块
     */
    private IBlockState getRandomRuinBlock(Random random) {
        int type = random.nextInt(10);
        switch (type) {
            case 0: return Blocks.STONEBRICK.getStateFromMeta(1);  // 苔石砖
            case 1: return Blocks.STONEBRICK.getStateFromMeta(2);  // 裂石砖
            case 2: return Blocks.IRON_BLOCK.getDefaultState();
            case 3: return Blocks.QUARTZ_BLOCK.getDefaultState();
            case 4: return Blocks.CONCRETE.getStateFromMeta(8);    // 灰色混凝土
            default: return Blocks.STONEBRICK.getDefaultState();
        }
    }

    /**
     * 放置特殊方块
     */
    private void placeSpecialBlock(World world, BlockPos pos, Random random) {
        int type = random.nextInt(10);
        Block specialBlock;

        try {
            switch (type) {
                case 0:
                case 1:
                    // 时间加速器 (残破)
                    specialBlock = ModBlocks.TEMPORAL_ACCELERATOR;
                    break;
                case 2:
                    // 保护立场生成器
                    specialBlock = ModBlocks.PROTECTION_FIELD_GENERATOR;
                    break;
                case 3:
                    // 重生站核心
                    specialBlock = ModBlocks.RESPAWN_CHAMBER_CORE;
                    break;
                case 4:
                    // 维度织布机
                    specialBlock = ModBlocks.dimensionLoom;
                    break;
                case 5:
                    // 打印机
                    specialBlock = ModBlocks.PRINTER;
                    break;
                default:
                    // 默认放置铁块
                    specialBlock = Blocks.IRON_BLOCK;
            }

            if (specialBlock != null) {
                setBlockSafe(world, pos, specialBlock.getDefaultState());
                System.out.println("[Ruins] 在 " + pos + " 放置了特殊方块: " + specialBlock.getRegistryName());
            }
        } catch (Exception e) {
            // 如果方块不存在，放置默认方块
            setBlockSafe(world, pos, Blocks.IRON_BLOCK.getDefaultState());
        }
    }

    /**
     * 获取随机材料
     */
    private ItemStack getRandomMaterial(Random random, int tier) {
        int type = random.nextInt(10 + tier * 3);
        int count = 1 + random.nextInt(tier * 2);

        switch (type) {
            case 0:
            case 1:
            case 2:
                return new ItemStack(net.minecraft.init.Items.IRON_INGOT, count);
            case 3:
            case 4:
                return new ItemStack(net.minecraft.init.Items.GOLD_INGOT, count);
            case 5:
            case 6:
                return new ItemStack(net.minecraft.init.Items.REDSTONE, count * 2);
            case 7:
                return new ItemStack(net.minecraft.init.Items.DIAMOND, Math.max(1, count / 2));
            case 8:
                return new ItemStack(net.minecraft.init.Items.EMERALD, Math.max(1, count / 2));
            case 9:
                return new ItemStack(net.minecraft.init.Items.ENDER_PEARL, Math.max(1, count / 3));
            default:
                // 尝试返回模组材料
                try {
                    if (ModItems.ANCIENT_CORE_FRAGMENT != null && random.nextBoolean()) {
                        return new ItemStack(ModItems.ANCIENT_CORE_FRAGMENT, 1);
                    }
                    if (ModItems.RIFT_CRYSTAL != null) {
                        return new ItemStack(ModItems.RIFT_CRYSTAL, 1);
                    }
                } catch (Exception e) {
                    // 忽略
                }
                return new ItemStack(net.minecraft.init.Items.QUARTZ, count);
        }
    }

    /**
     * 创建随机打印模版
     */
    private ItemStack createRandomTemplate(Random random) {
        try {
            if (ModItems.PRINT_TEMPLATE != null) {
                // 从配方注册表获取所有已注册的配方
                java.util.Collection<PrinterRecipe> recipes = PrinterRecipeRegistry.getAllRecipes();
                if (!recipes.isEmpty()) {
                    // 转换为数组并随机选择
                    PrinterRecipe[] recipeArray = recipes.toArray(new PrinterRecipe[0]);
                    PrinterRecipe selectedRecipe = recipeArray[random.nextInt(recipeArray.length)];
                    return ItemPrintTemplate.createTemplate(ModItems.PRINT_TEMPLATE, selectedRecipe.getTemplateId());
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        // 备用返回
        return new ItemStack(net.minecraft.init.Items.PAPER, 1);
    }

    /**
     * 获取随机装备
     */
    private ItemStack getRandomGear(Random random) {
        // 暂时返回钻石装备，后续可替换为故障装备
        int type = random.nextInt(5);
        switch (type) {
            case 0: return new ItemStack(net.minecraft.init.Items.DIAMOND_HELMET);
            case 1: return new ItemStack(net.minecraft.init.Items.DIAMOND_CHESTPLATE);
            case 2: return new ItemStack(net.minecraft.init.Items.DIAMOND_LEGGINGS);
            case 3: return new ItemStack(net.minecraft.init.Items.DIAMOND_BOOTS);
            default: return new ItemStack(net.minecraft.init.Items.DIAMOND_SWORD);
        }
    }

    /**
     * 安全设置方块
     */
    private void setBlockSafe(World world, BlockPos pos, IBlockState state) {
        if (world.isBlockLoaded(pos)) {
            world.setBlockState(pos, state, 2);
        }
    }

    /**
     * 填充区域
     */
    private void fillArea(World world, BlockPos from, BlockPos to, IBlockState state) {
        for (int x = from.getX(); x <= to.getX(); x++) {
            for (int y = from.getY(); y <= to.getY(); y++) {
                for (int z = from.getZ(); z <= to.getZ(); z++) {
                    setBlockSafe(world, new BlockPos(x, y, z), state);
                }
            }
        }
    }
}
