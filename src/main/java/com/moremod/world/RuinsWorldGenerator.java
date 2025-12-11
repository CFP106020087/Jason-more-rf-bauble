package com.moremod.world;

import com.moremod.init.ModBlocks;
import com.moremod.init.ModItems;
import com.moremod.printer.ItemPrintTemplate;
import com.moremod.printer.PrinterRecipe;
import com.moremod.printer.PrinterRecipeRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.Enchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * 科技废墟世界生成器 v3.0
 *
 * 在主世界野外生成残破的科技感废墟建筑
 * 内含稀有机械方块、打印模版和故障装备
 *
 * v3.0 改进：
 * - 增加结构类型 (10种)
 * - 扩大结构尺寸
 * - 提高稀有方块概率
 * - 新增量子采矿机废墟
 * - 新增故障装备系统
 */
public class RuinsWorldGenerator implements IWorldGenerator {

    // ============== 生成配置 ==============
    private static final int MIN_Y = 50;
    private static final int SPAWN_CHANCE = 60;              // 提高生成率 (1/60 区块)
    private static final int MIN_DISTANCE_FROM_SPAWN = 200;  // 降低距离要求

    // 稀有方块概率系数 (原来的2倍)
    private static final float SPECIAL_BLOCK_CHANCE_MULTIPLIER = 2.0f;

    // ============== 结构类型枚举 ==============
    public enum RuinType {
        RESEARCH_OUTPOST("研究前哨站", 12, 12, 8, 2),
        MECHANICAL_COMPLEX("机械综合体", 18, 18, 12, 3),
        SIGNAL_TOWER("信号塔", 10, 10, 25, 2),
        UNDERGROUND_VAULT("地下金库", 14, 14, 10, 4),
        CRASHED_TRANSPORT("坠毁运输船", 20, 12, 8, 3),
        FACTORY_RUINS("废弃工厂", 24, 24, 14, 4),
        QUANTUM_QUARRY_SITE("量子采矿场", 16, 16, 10, 5),      // 新增
        ENERGY_STATION("能量中继站", 14, 14, 16, 4),           // 新增
        DATA_CENTER("数据中心废墟", 20, 20, 8, 3),             // 新增
        TEMPORAL_LAB("时间实验室", 16, 16, 12, 5);             // 新增

        public final String name;
        public final int sizeX, sizeZ, sizeY;
        public final int lootTier;

        RuinType(String name, int sizeX, int sizeZ, int sizeY, int lootTier) {
            this.name = name;
            this.sizeX = sizeX;
            this.sizeZ = sizeZ;
            this.sizeY = sizeY;
            this.lootTier = lootTier;
        }
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.provider.getDimension() != 0) return;
        if (random.nextInt(SPAWN_CHANCE) != 0) return;

        int worldX = chunkX * 16 + 8;
        int worldZ = chunkZ * 16 + 8;

        if (Math.abs(worldX) < MIN_DISTANCE_FROM_SPAWN && Math.abs(worldZ) < MIN_DISTANCE_FROM_SPAWN) {
            return;
        }

        BlockPos basePos = findGroundLevel(world, worldX, worldZ, random);
        if (basePos == null) return;

        // 随机选择废墟类型
        RuinType[] types = RuinType.values();
        RuinType selectedType = types[random.nextInt(types.length)];

        generateRuin(world, basePos, random, selectedType);
        System.out.println("[Ruins] 在 " + basePos + " 生成了 " + selectedType.name + " (Tier " + selectedType.lootTier + ")");
    }

    private void generateRuin(World world, BlockPos pos, Random random, RuinType type) {
        switch (type) {
            case RESEARCH_OUTPOST:
                generateResearchOutpost(world, pos, random);
                break;
            case MECHANICAL_COMPLEX:
                generateMechanicalComplex(world, pos, random);
                break;
            case SIGNAL_TOWER:
                generateSignalTower(world, pos, random);
                break;
            case UNDERGROUND_VAULT:
                generateUndergroundVault(world, pos, random);
                break;
            case CRASHED_TRANSPORT:
                generateCrashedTransport(world, pos, random);
                break;
            case FACTORY_RUINS:
                generateFactoryRuins(world, pos, random);
                break;
            case QUANTUM_QUARRY_SITE:
                generateQuantumQuarrySite(world, pos, random);
                break;
            case ENERGY_STATION:
                generateEnergyStation(world, pos, random);
                break;
            case DATA_CENTER:
                generateDataCenter(world, pos, random);
                break;
            case TEMPORAL_LAB:
                generateTemporalLab(world, pos, random);
                break;
        }
    }

    // ============== 新增结构: 量子采矿场 (16x16x10) ==============
    private void generateQuantumQuarrySite(World world, BlockPos pos, Random random) {
        clearBuildingArea(world, pos, 9, 9, 12);
        levelGround(world, pos, 9, 9, 5);

        // 采矿坑 (中央大坑)
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                int depth = 6 - (int)(Math.sqrt(x*x + z*z) * 0.8);
                if (depth > 0) {
                    for (int y = 0; y > -depth; y--) {
                        setBlockSafe(world, pos.add(x, y, z), Blocks.AIR.getDefaultState());
                    }
                    // 坑底矿石
                    if (random.nextFloat() < 0.3f) {
                        setBlockSafe(world, pos.add(x, -depth, z), getRandomOre(random));
                    } else {
                        setBlockSafe(world, pos.add(x, -depth, z), Blocks.STONE.getDefaultState());
                    }
                }
            }
        }

        // 采矿框架 (破损的)
        for (int y = 0; y <= 8; y++) {
            // 四角立柱
            for (int[] c : new int[][]{{-7,-7}, {-7,7}, {7,-7}, {7,7}}) {
                if (random.nextFloat() > 0.2f) {
                    setBlockSafe(world, pos.add(c[0], y, c[1]), Blocks.IRON_BLOCK.getDefaultState());
                }
            }
            // 横梁
            if (y == 4 || y == 8) {
                for (int i = -6; i <= 6; i++) {
                    if (random.nextFloat() > 0.3f) {
                        setBlockSafe(world, pos.add(i, y, -7), Blocks.IRON_BARS.getDefaultState());
                        setBlockSafe(world, pos.add(i, y, 7), Blocks.IRON_BARS.getDefaultState());
                        setBlockSafe(world, pos.add(-7, y, i), Blocks.IRON_BARS.getDefaultState());
                        setBlockSafe(world, pos.add(7, y, i), Blocks.IRON_BARS.getDefaultState());
                    }
                }
            }
        }

        // ★ 核心: 破损的量子采矿机 ★
        BlockPos quarryPos = pos.add(0, 1, 0);
        placeQuantumQuarry(world, quarryPos, random);

        // 控制台
        setBlockSafe(world, pos.add(-5, 0, -5), Blocks.REDSTONE_LAMP.getDefaultState());
        setBlockSafe(world, pos.add(-5, 1, -5), Blocks.DAYLIGHT_DETECTOR.getDefaultState());
        setBlockSafe(world, pos.add(-6, 0, -5), Blocks.LEVER.getDefaultState());

        // 能量管道
        for (int i = -4; i <= 0; i++) {
            if (random.nextFloat() > 0.25f) {
                setBlockSafe(world, pos.add(i, 0, -5), Blocks.END_ROD.getDefaultState());
            }
        }

        // 储矿箱
        for (int z = 3; z <= 6; z += 2) {
            setBlockSafe(world, pos.add(6, 0, z), Blocks.CHEST.getDefaultState());
            setBlockSafe(world, pos.add(6, 0, -z), Blocks.CHEST.getDefaultState());
        }

        // 散落的矿石
        for (int i = 0; i < 8; i++) {
            BlockPos orePos = pos.add(random.nextInt(12) - 6, 0, random.nextInt(12) - 6);
            if (world.isAirBlock(orePos) && !world.isAirBlock(orePos.down())) {
                setBlockSafe(world, orePos, getRandomOreBlock(random));
            }
        }

        placeRuinContents(world, pos.add(5, 0, 0), random, RuinType.QUANTUM_QUARRY_SITE.lootTier);
        placeRuinContents(world, pos.add(-5, 0, 3), random, RuinType.QUANTUM_QUARRY_SITE.lootTier);
    }

    // 放置破损的量子采矿机
    private void placeQuantumQuarry(World world, BlockPos pos, Random random) {
        try {
            // 尝试放置量子采矿机方块
            if (ModBlocks.QUANTUM_QUARRY != null) {
                setBlockSafe(world, pos, ModBlocks.QUANTUM_QUARRY.getDefaultState());
                System.out.println("[Ruins] 放置了破损的量子采矿机于 " + pos);
            } else {
                // 备用: 用铁块+红石块模拟
                setBlockSafe(world, pos, Blocks.IRON_BLOCK.getDefaultState());
                setBlockSafe(world, pos.up(), Blocks.REDSTONE_BLOCK.getDefaultState());
            }
        } catch (Exception e) {
            setBlockSafe(world, pos, Blocks.IRON_BLOCK.getDefaultState());
            setBlockSafe(world, pos.up(), Blocks.REDSTONE_BLOCK.getDefaultState());
        }

        // 周围的破损零件
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (random.nextFloat() < 0.4f) {
                    Block debris = random.nextBoolean() ? Blocks.IRON_BLOCK : Blocks.QUARTZ_BLOCK;
                    setBlockSafe(world, pos.add(dx, 0, dz), debris.getDefaultState());
                }
            }
        }
    }

    // ============== 新增结构: 能量中继站 (14x14x16) ==============
    private void generateEnergyStation(World world, BlockPos pos, Random random) {
        clearBuildingArea(world, pos, 8, 8, 18);
        levelGround(world, pos, 8, 8, 4);

        // 基座
        fillArea(world, pos.add(-6, -1, -6), pos.add(6, 0, 6), Blocks.QUARTZ_BLOCK.getDefaultState());

        // 能量塔
        for (int y = 1; y <= 14; y++) {
            int radius = y < 4 ? 4 : (y < 8 ? 3 : (y < 12 ? 2 : 1));
            float damage = 0.05f + (y * 0.02f);

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    boolean isEdge = (Math.abs(x) == radius || Math.abs(z) == radius);
                    if (isEdge && random.nextFloat() > damage) {
                        IBlockState block = (y % 3 == 0) ? Blocks.SEA_LANTERN.getDefaultState()
                                                         : Blocks.QUARTZ_BLOCK.getDefaultState();
                        setBlockSafe(world, pos.add(x, y, z), block);
                    }
                }
            }

            // 内部能量核心
            if (y >= 2 && y <= 10 && y % 2 == 0) {
                setBlockSafe(world, pos.add(0, y, 0), Blocks.BEACON.getDefaultState());
            }
        }

        // 顶部能量球
        setBlockSafe(world, pos.add(0, 15, 0), Blocks.GLOWSTONE.getDefaultState());
        setBlockSafe(world, pos.add(0, 16, 0), Blocks.END_ROD.getDefaultState());

        // 四角能量柱
        for (int[] c : new int[][]{{-5,-5}, {-5,5}, {5,-5}, {5,5}}) {
            for (int y = 1; y <= 6; y++) {
                if (random.nextFloat() > 0.15f) {
                    setBlockSafe(world, pos.add(c[0], y, c[1]), Blocks.PURPUR_PILLAR.getDefaultState());
                }
            }
            setBlockSafe(world, pos.add(c[0], 7, c[1]), Blocks.END_ROD.getDefaultState());
        }

        // 控制室
        fillArea(world, pos.add(-6, 1, -6), pos.add(-4, 3, -4), Blocks.GLASS.getDefaultState());
        setBlockSafe(world, pos.add(-5, 1, -5), Blocks.AIR.getDefaultState());
        setBlockSafe(world, pos.add(-5, 2, -5), Blocks.AIR.getDefaultState());

        placeRuinContents(world, pos.add(-5, 1, -5), random, RuinType.ENERGY_STATION.lootTier);

        // 额外特殊方块
        if (random.nextFloat() < 0.5f) {
            placeSpecialBlock(world, pos.add(3, 1, 3), random);
        }
    }

    // ============== 新增结构: 数据中心废墟 (20x20x8) ==============
    private void generateDataCenter(World world, BlockPos pos, Random random) {
        clearBuildingArea(world, pos, 11, 11, 10);
        levelGround(world, pos, 11, 11, 4);

        // 地基
        fillArea(world, pos.add(-10, -1, -10), pos.add(10, -1, 10), Blocks.CONCRETE.getStateFromMeta(15));

        // 主体框架
        for (int y = 0; y <= 6; y++) {
            for (int[] c : new int[][]{{-9,-9}, {-9,9}, {9,-9}, {9,9}, {-9,0}, {9,0}, {0,-9}, {0,9}}) {
                if (random.nextFloat() > 0.15f) {
                    setBlockSafe(world, pos.add(c[0], y, c[1]), Blocks.IRON_BLOCK.getDefaultState());
                }
            }
        }

        // 服务器机架阵列
        for (int x = -7; x <= 7; x += 3) {
            for (int z = -7; z <= 7; z += 4) {
                if (random.nextFloat() > 0.2f) {
                    // 服务器机架
                    for (int y = 0; y <= 3; y++) {
                        IBlockState rack = (y % 2 == 0) ? Blocks.IRON_BLOCK.getDefaultState()
                                                        : Blocks.REDSTONE_LAMP.getDefaultState();
                        setBlockSafe(world, pos.add(x, y, z), rack);
                    }
                }
            }
        }

        // 冷却系统
        for (int z = -6; z <= 6; z += 4) {
            setBlockSafe(world, pos.add(-8, 0, z), Blocks.PACKED_ICE.getDefaultState());
            setBlockSafe(world, pos.add(8, 0, z), Blocks.PACKED_ICE.getDefaultState());
        }

        // 线缆通道
        for (int x = -8; x <= 8; x++) {
            if (random.nextFloat() > 0.3f) {
                setBlockSafe(world, pos.add(x, 5, 0), Blocks.END_ROD.getDefaultState());
            }
        }

        placeRuinContents(world, pos.add(0, 0, 5), random, RuinType.DATA_CENTER.lootTier);
        placeRuinContents(world, pos.add(0, 0, -5), random, RuinType.DATA_CENTER.lootTier);
    }

    // ============== 新增结构: 时间实验室 (16x16x12) ==============
    private void generateTemporalLab(World world, BlockPos pos, Random random) {
        clearBuildingArea(world, pos, 9, 9, 14);
        levelGround(world, pos, 9, 9, 5);

        // 时空扭曲地基 (交错图案)
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                IBlockState floor = ((x + z) % 2 == 0) ? Blocks.OBSIDIAN.getDefaultState()
                                                       : Blocks.QUARTZ_BLOCK.getDefaultState();
                setBlockSafe(world, pos.add(x, -1, z), floor);
            }
        }

        // 主体结构 (扭曲的)
        for (int y = 0; y <= 10; y++) {
            int twist = (y / 3) % 2;
            int radius = 6 - (y / 3);
            if (radius < 2) radius = 2;

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    boolean isEdge = (Math.abs(x) == radius || Math.abs(z) == radius);
                    if (isEdge && random.nextFloat() > 0.2f) {
                        int offsetX = twist * (random.nextInt(3) - 1);
                        int offsetZ = twist * (random.nextInt(3) - 1);
                        IBlockState block = random.nextFloat() < 0.3f ? Blocks.END_STONE.getDefaultState()
                                                                      : getRandomRuinBlock(random);
                        setBlockSafe(world, pos.add(x + offsetX, y, z + offsetZ), block);
                    }
                }
            }
        }

        // 时间核心 (中央)
        for (int y = 1; y <= 5; y++) {
            setBlockSafe(world, pos.add(0, y, 0), Blocks.BEACON.getDefaultState());
        }

        // 时间加速器 (必定生成)
        try {
            if (ModBlocks.TEMPORAL_ACCELERATOR != null) {
                setBlockSafe(world, pos.add(2, 1, 0), ModBlocks.TEMPORAL_ACCELERATOR.getDefaultState());
                setBlockSafe(world, pos.add(-2, 1, 0), ModBlocks.TEMPORAL_ACCELERATOR.getDefaultState());
            }
        } catch (Exception ignored) {}

        // 浮空时钟碎片
        for (int i = 0; i < 6; i++) {
            int fx = random.nextInt(10) - 5;
            int fy = 6 + random.nextInt(4);
            int fz = random.nextInt(10) - 5;
            setBlockSafe(world, pos.add(fx, fy, fz), Blocks.END_ROD.getDefaultState());
        }

        // 实验日志和战利品
        placeRuinContents(world, pos.add(4, 0, 4), random, RuinType.TEMPORAL_LAB.lootTier);
        placeRuinContents(world, pos.add(-4, 0, -4), random, RuinType.TEMPORAL_LAB.lootTier);

        // 高概率特殊方块
        if (random.nextFloat() < 0.7f) {
            placeSpecialBlock(world, pos.add(0, 6, 0), random);
        }
    }

    // ============== 原有结构 (扩大尺寸) ==============

    private void generateResearchOutpost(World world, BlockPos pos, Random random) {
        clearBuildingArea(world, pos, 7, 7, 10);
        levelGround(world, pos, 7, 7, 4);

        fillArea(world, pos.add(-6, -1, -6), pos.add(6, -1, 6), Blocks.CONCRETE.getStateFromMeta(8));

        for (int y = 0; y <= 7; y++) {
            for (int[] c : new int[][]{{-6,-6}, {-6,6}, {6,-6}, {6,6}}) {
                if (random.nextFloat() > 0.15f) {
                    setBlockSafe(world, pos.add(c[0], y, c[1]), Blocks.IRON_BLOCK.getDefaultState());
                }
            }
        }

        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                boolean isWall = (Math.abs(x) == 5 || Math.abs(z) == 5);
                if (!isWall) continue;
                for (int y = 0; y <= 6; y++) {
                    if (random.nextFloat() > 0.25f) {
                        if (y < 3) {
                            setBlockSafe(world, pos.add(x, y, z), getRandomRuinBlock(random));
                        } else if (y == 3) {
                            setBlockSafe(world, pos.add(x, y, z), Blocks.IRON_BARS.getDefaultState());
                        } else {
                            setBlockSafe(world, pos.add(x, y, z), Blocks.QUARTZ_BLOCK.getDefaultState());
                        }
                    }
                }
            }
        }

        setBlockSafe(world, pos.add(-2, 0, 0), Blocks.BREWING_STAND.getDefaultState());
        setBlockSafe(world, pos.add(2, 0, 0), Blocks.CAULDRON.getDefaultState());
        setBlockSafe(world, pos.add(-3, 0, -3), Blocks.REDSTONE_LAMP.getDefaultState());
        setBlockSafe(world, pos.add(3, 0, 3), Blocks.SEA_LANTERN.getDefaultState());

        placeRuinContents(world, pos, random, RuinType.RESEARCH_OUTPOST.lootTier);
    }

    private void generateMechanicalComplex(World world, BlockPos pos, Random random) {
        clearBuildingArea(world, pos, 10, 10, 14);
        levelGround(world, pos, 10, 10, 5);

        fillArea(world, pos.add(-9, -2, -9), pos.add(9, -1, 9), Blocks.STONEBRICK.getDefaultState());

        for (int y = 0; y <= 10; y++) {
            for (int[] c : new int[][]{{-9,-9}, {-9,9}, {9,-9}, {9,9}, {-9,0}, {9,0}, {0,-9}, {0,9}}) {
                if (random.nextFloat() > 0.2f) {
                    IBlockState pillar = y < 5 ? Blocks.IRON_BLOCK.getDefaultState() : Blocks.QUARTZ_BLOCK.getDefaultState();
                    setBlockSafe(world, pos.add(c[0], y, c[1]), pillar);
                }
            }
        }

        // 生产线
        for (int z = -6; z <= 6; z += 2) {
            setBlockSafe(world, pos.add(-5, 0, z), Blocks.PISTON.getDefaultState());
            setBlockSafe(world, pos.add(5, 0, z), Blocks.STICKY_PISTON.getDefaultState());
        }

        fillArea(world, pos.add(-2, 0, -2), pos.add(2, 3, 2), Blocks.IRON_BLOCK.getDefaultState());
        setBlockSafe(world, pos.add(0, 2, 0), Blocks.BEACON.getDefaultState());

        placeRuinContents(world, pos, random, RuinType.MECHANICAL_COMPLEX.lootTier);
        if (random.nextFloat() < 0.4f) {
            placeSpecialBlock(world, pos.add(4, 0, 4), random);
        }
    }

    private void generateSignalTower(World world, BlockPos pos, Random random) {
        clearBuildingArea(world, pos, 6, 6, 28);
        levelGround(world, pos, 6, 6, 4);

        fillArea(world, pos.add(-5, -1, -5), pos.add(5, 2, 5), Blocks.STONEBRICK.getDefaultState());
        fillArea(world, pos.add(-4, 0, -4), pos.add(4, 2, 4), Blocks.AIR.getDefaultState());

        for (int y = 3; y <= 22; y++) {
            int radius = y < 12 ? 4 : (y < 18 ? 3 : 2);
            float damage = 0.08f + (y * 0.02f);

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    boolean isEdge = (Math.abs(x) == radius || Math.abs(z) == radius);
                    if (isEdge && random.nextFloat() > damage) {
                        IBlockState block = y % 5 == 0 ? Blocks.QUARTZ_BLOCK.getDefaultState() : getRandomRuinBlock(random);
                        setBlockSafe(world, pos.add(x, y, z), block);
                    }
                }
            }
        }

        for (int y = 23; y <= 26; y++) {
            setBlockSafe(world, pos.add(0, y, 0), Blocks.END_ROD.getDefaultState());
        }
        setBlockSafe(world, pos.add(0, 27, 0), Blocks.REDSTONE_LAMP.getDefaultState());

        placeRuinContents(world, pos, random, RuinType.SIGNAL_TOWER.lootTier);
    }

    private void generateUndergroundVault(World world, BlockPos pos, Random random) {
        BlockPos vaultPos = pos.down(12);

        fillArea(world, vaultPos.add(-7, 0, -7), vaultPos.add(7, 8, 7), Blocks.AIR.getDefaultState());

        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                for (int y = -1; y <= 9; y++) {
                    boolean isShell = (Math.abs(x) == 7 || Math.abs(z) == 7 || y == -1 || y == 9);
                    if (isShell) {
                        IBlockState shell = y == -1 ? Blocks.OBSIDIAN.getDefaultState() : Blocks.STONEBRICK.getDefaultState();
                        setBlockSafe(world, vaultPos.add(x, y, z), shell);
                    }
                }
            }
        }

        // 入口
        for (int y = 0; y <= 11; y++) {
            setBlockSafe(world, pos.add(0, -y, 0), Blocks.AIR.getDefaultState());
            if (y % 2 == 0) {
                setBlockSafe(world, pos.add(1, -y, 0), Blocks.STONE_SLAB.getDefaultState());
            }
        }

        setBlockSafe(world, vaultPos.add(0, 1, 0), Blocks.BEACON.getDefaultState());
        setBlockSafe(world, vaultPos.add(0, 2, 0), Blocks.BEACON.getDefaultState());

        placeRuinContents(world, vaultPos.up(), random, RuinType.UNDERGROUND_VAULT.lootTier);
        placeRuinContents(world, vaultPos.add(4, 1, 4), random, RuinType.UNDERGROUND_VAULT.lootTier);

        if (random.nextFloat() < 0.6f) {
            placeSpecialBlock(world, vaultPos.add(-4, 1, -4), random);
        }
    }

    private void generateCrashedTransport(World world, BlockPos pos, Random random) {
        for (int x = -9; x <= 9; x++) {
            for (int z = -5; z <= 5; z++) {
                int depth = 4 - (int) Math.sqrt(x * x / 2.0 + z * z);
                if (depth > 0) {
                    for (int y = 0; y > -depth; y--) {
                        setBlockSafe(world, pos.add(x, y, z), Blocks.AIR.getDefaultState());
                    }
                    setBlockSafe(world, pos.add(x, -depth, z), Blocks.SOUL_SAND.getDefaultState());
                }
            }
        }

        for (int x = -7; x <= 7; x++) {
            int tilt = x / 3;
            for (int z = -4; z <= 4; z++) {
                if (random.nextFloat() > 0.3f) {
                    setBlockSafe(world, pos.add(x, tilt, z), Blocks.IRON_BLOCK.getDefaultState());
                }
                if (Math.abs(z) == 4 && random.nextFloat() > 0.35f) {
                    setBlockSafe(world, pos.add(x, tilt + 1, z), getRandomRuinBlock(random));
                    setBlockSafe(world, pos.add(x, tilt + 2, z), Blocks.IRON_BARS.getDefaultState());
                }
            }
        }

        for (int i = 0; i < 12; i++) {
            int rx = random.nextInt(18) - 9;
            int rz = random.nextInt(12) - 6;
            Block cargo = random.nextInt(4) == 0 ? Blocks.CHEST : random.nextInt(3) == 0 ? Blocks.IRON_BLOCK : Blocks.QUARTZ_BLOCK;
            setBlockSafe(world, pos.add(rx, 0, rz), cargo.getDefaultState());
        }

        placeRuinContents(world, pos, random, RuinType.CRASHED_TRANSPORT.lootTier);
        placeRuinContents(world, pos.add(5, 0, 0), random, RuinType.CRASHED_TRANSPORT.lootTier);
    }

    private void generateFactoryRuins(World world, BlockPos pos, Random random) {
        clearBuildingArea(world, pos, 13, 13, 16);
        levelGround(world, pos, 13, 13, 5);

        fillArea(world, pos.add(-12, -2, -12), pos.add(12, -1, 12), Blocks.STONEBRICK.getDefaultState());

        for (int y = 0; y <= 12; y++) {
            for (int x = -11; x <= 11; x += 11) {
                for (int z = -11; z <= 11; z += 11) {
                    if (random.nextFloat() > 0.15f) {
                        setBlockSafe(world, pos.add(x, y, z), Blocks.IRON_BLOCK.getDefaultState());
                    }
                }
            }
        }

        for (int z = -8; z <= 8; z += 2) {
            setBlockSafe(world, pos.add(-8, 0, z), Blocks.FURNACE.getDefaultState());
            setBlockSafe(world, pos.add(-8, 1, z), Blocks.HOPPER.getDefaultState());
        }

        fillArea(world, pos.add(-3, 0, -3), pos.add(3, 0, 3), Blocks.CRAFTING_TABLE.getDefaultState());
        setBlockSafe(world, pos.add(0, 1, 0), Blocks.ANVIL.getDefaultState());

        for (int x = 5; x <= 8; x++) {
            for (int z = -8; z <= 8; z += 3) {
                setBlockSafe(world, pos.add(x, 0, z), Blocks.CHEST.getDefaultState());
            }
        }

        // 烟囱
        for (int y = 0; y <= 18; y++) {
            float decay = 0.08f + (y * 0.03f);
            if (random.nextFloat() > decay) {
                for (int dx = 0; dx <= 1; dx++) {
                    for (int dz = 0; dz <= 1; dz++) {
                        setBlockSafe(world, pos.add(8 + dx, y, -8 + dz), Blocks.BRICK_BLOCK.getDefaultState());
                    }
                }
            }
        }

        placeRuinContents(world, pos, random, RuinType.FACTORY_RUINS.lootTier);
        placeRuinContents(world, pos.add(-6, 0, 6), random, RuinType.FACTORY_RUINS.lootTier);

        if (random.nextFloat() < 0.5f) {
            placeSpecialBlock(world, pos.add(6, 0, -6), random);
        }
    }

    // ============== 辅助方法 ==============

    private BlockPos findGroundLevel(World world, int x, int z, Random random) {
        x += random.nextInt(8) - 4;
        z += random.nextInt(8) - 4;

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

    private void clearBuildingArea(World world, BlockPos center, int radiusX, int radiusZ, int height) {
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                for (int y = 0; y <= height; y++) {
                    setBlockSafe(world, center.add(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

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

    private void placeRuinContents(World world, BlockPos center, Random random, int lootTier) {
        BlockPos chestPos = center.add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
        setBlockSafe(world, chestPos, Blocks.CHEST.getDefaultState());

        TileEntity te = world.getTileEntity(chestPos);
        if (te instanceof TileEntityChest) {
            fillChestWithLoot((TileEntityChest) te, random, lootTier);
        }

        // ★ 提高特殊方块概率 (原来的2倍) ★
        float chance = 0.25f * lootTier * SPECIAL_BLOCK_CHANCE_MULTIPLIER;
        if (random.nextFloat() < chance) {
            BlockPos specialPos = center.add(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            placeSpecialBlock(world, specialPos, random);
        }
    }

    private void fillChestWithLoot(TileEntityChest chest, Random random, int tier) {
        int materialCount = 4 + random.nextInt(6);
        for (int i = 0; i < materialCount; i++) {
            int slot = random.nextInt(27);
            ItemStack material = getRandomMaterial(random, tier);
            chest.setInventorySlotContents(slot, material);
        }

        // 提高打印模版概率
        if (random.nextFloat() < 0.15f * tier) {
            int slot = random.nextInt(27);
            ItemStack template = createRandomTemplate(random);
            chest.setInventorySlotContents(slot, template);
        }

        // ★ 故障装备 (提高概率) ★
        if (random.nextFloat() < 0.1f * tier) {
            int slot = random.nextInt(27);
            ItemStack gear = createFaultyGear(random, tier);
            chest.setInventorySlotContents(slot, gear);
        }
    }

    // ★★★ 创建故障装备 ★★★
    private ItemStack createFaultyGear(Random random, int tier) {
        ItemStack gear;
        int type = random.nextInt(8);

        switch (type) {
            case 0: gear = new ItemStack(Items.DIAMOND_HELMET); break;
            case 1: gear = new ItemStack(Items.DIAMOND_CHESTPLATE); break;
            case 2: gear = new ItemStack(Items.DIAMOND_LEGGINGS); break;
            case 3: gear = new ItemStack(Items.DIAMOND_BOOTS); break;
            case 4: gear = new ItemStack(Items.DIAMOND_SWORD); break;
            case 5: gear = new ItemStack(Items.DIAMOND_PICKAXE); break;
            case 6: gear = new ItemStack(Items.DIAMOND_AXE); break;
            default: gear = new ItemStack(Items.DIAMOND_SHOVEL); break;
        }

        // 添加故障属性
        NBTTagCompound nbt = gear.getTagCompound();
        if (nbt == null) nbt = new NBTTagCompound();

        // 显示名称和 Lore
        NBTTagCompound display = new NBTTagCompound();
        String[] prefixes = {"故障的", "损坏的", "不稳定的", "异常的", "超频的"};
        display.setString("Name", "§c" + prefixes[random.nextInt(prefixes.length)] + gear.getDisplayName());

        NBTTagList lore = new NBTTagList();
        lore.appendTag(new net.minecraft.nbt.NBTTagString("§7来自远古科技废墟的遗物"));
        lore.appendTag(new net.minecraft.nbt.NBTTagString("§7状态: §c不稳定"));
        lore.appendTag(new net.minecraft.nbt.NBTTagString("§e等级: " + tier));
        display.setTag("Lore", lore);
        nbt.setTag("display", display);

        // 随机附魔
        gear.setTagCompound(nbt);
        int enchantCount = 1 + random.nextInt(tier);
        for (int i = 0; i < enchantCount; i++) {
            try {
                switch (random.nextInt(8)) {
                    case 0: gear.addEnchantment(Enchantments.PROTECTION, 2 + random.nextInt(3)); break;
                    case 1: gear.addEnchantment(Enchantments.SHARPNESS, 3 + random.nextInt(3)); break;
                    case 2: gear.addEnchantment(Enchantments.EFFICIENCY, 3 + random.nextInt(3)); break;
                    case 3: gear.addEnchantment(Enchantments.UNBREAKING, 2 + random.nextInt(2)); break;
                    case 4: gear.addEnchantment(Enchantments.FIRE_ASPECT, 1 + random.nextInt(2)); break;
                    case 5: gear.addEnchantment(Enchantments.LOOTING, 2 + random.nextInt(2)); break;
                    case 6: gear.addEnchantment(Enchantments.FORTUNE, 2 + random.nextInt(2)); break;
                    case 7: gear.addEnchantment(Enchantments.MENDING, 1); break;
                }
            } catch (Exception ignored) {}
        }

        // 设置耐久度损耗
        int maxDamage = gear.getMaxDamage();
        gear.setItemDamage(random.nextInt(maxDamage / 2));

        return gear;
    }

    private void placeSpecialBlock(World world, BlockPos pos, Random random) {
        Block specialBlock = null;
        int type = random.nextInt(12);

        try {
            switch (type) {
                case 0:
                case 1:
                    specialBlock = ModBlocks.TEMPORAL_ACCELERATOR;
                    break;
                case 2:
                case 3:
                    specialBlock = ModBlocks.PROTECTION_FIELD_GENERATOR;
                    break;
                case 4:
                    specialBlock = ModBlocks.RESPAWN_CHAMBER_CORE;
                    break;
                case 5:
                    specialBlock = ModBlocks.dimensionLoom;
                    break;
                case 6:
                case 7:
                    specialBlock = ModBlocks.PRINTER;
                    break;
                case 8:
                    specialBlock = ModBlocks.QUANTUM_QUARRY;
                    break;
                case 9:
                    specialBlock = ModBlocks.UPGRADE_CHAMBER_CORE;
                    break;
                case 10:
                    specialBlock = ModBlocks.SIMPLE_WISDOM_SHRINE;
                    break;
                default:
                    specialBlock = Blocks.BEACON;
            }

            if (specialBlock != null) {
                setBlockSafe(world, pos, specialBlock.getDefaultState());
                System.out.println("[Ruins] 放置特殊方块: " + specialBlock.getRegistryName() + " @ " + pos);
            }
        } catch (Exception e) {
            setBlockSafe(world, pos, Blocks.BEACON.getDefaultState());
        }
    }

    private IBlockState getRandomRuinBlock(Random random) {
        int type = random.nextInt(10);
        switch (type) {
            case 0: return Blocks.STONEBRICK.getStateFromMeta(1);
            case 1: return Blocks.STONEBRICK.getStateFromMeta(2);
            case 2: return Blocks.IRON_BLOCK.getDefaultState();
            case 3: return Blocks.QUARTZ_BLOCK.getDefaultState();
            case 4: return Blocks.CONCRETE.getStateFromMeta(8);
            case 5: return Blocks.CONCRETE.getStateFromMeta(15);
            default: return Blocks.STONEBRICK.getDefaultState();
        }
    }

    private IBlockState getRandomOre(Random random) {
        int type = random.nextInt(10);
        switch (type) {
            case 0: return Blocks.DIAMOND_ORE.getDefaultState();
            case 1:
            case 2: return Blocks.GOLD_ORE.getDefaultState();
            case 3:
            case 4:
            case 5: return Blocks.IRON_ORE.getDefaultState();
            case 6: return Blocks.EMERALD_ORE.getDefaultState();
            case 7: return Blocks.REDSTONE_ORE.getDefaultState();
            default: return Blocks.COAL_ORE.getDefaultState();
        }
    }

    private IBlockState getRandomOreBlock(Random random) {
        int type = random.nextInt(6);
        switch (type) {
            case 0: return Blocks.DIAMOND_BLOCK.getDefaultState();
            case 1: return Blocks.GOLD_BLOCK.getDefaultState();
            case 2: return Blocks.IRON_BLOCK.getDefaultState();
            case 3: return Blocks.EMERALD_BLOCK.getDefaultState();
            case 4: return Blocks.REDSTONE_BLOCK.getDefaultState();
            default: return Blocks.COAL_BLOCK.getDefaultState();
        }
    }

    private ItemStack getRandomMaterial(Random random, int tier) {
        int type = random.nextInt(12 + tier * 3);
        int count = 2 + random.nextInt(tier * 3);

        switch (type) {
            case 0:
            case 1:
            case 2:
                return new ItemStack(Items.IRON_INGOT, count);
            case 3:
            case 4:
                return new ItemStack(Items.GOLD_INGOT, count);
            case 5:
            case 6:
                return new ItemStack(Items.REDSTONE, count * 2);
            case 7:
            case 8:
                return new ItemStack(Items.DIAMOND, 1 + random.nextInt(tier));
            case 9:
                return new ItemStack(Items.EMERALD, 1 + random.nextInt(tier));
            case 10:
                return new ItemStack(Items.ENDER_PEARL, 2 + random.nextInt(4));
            case 11:
                return new ItemStack(Items.NETHER_STAR, 1);
            default:
                try {
                    if (ModItems.ANCIENT_CORE_FRAGMENT != null && random.nextBoolean()) {
                        return new ItemStack(ModItems.ANCIENT_CORE_FRAGMENT, 1 + random.nextInt(2));
                    }
                    if (ModItems.RIFT_CRYSTAL != null) {
                        return new ItemStack(ModItems.RIFT_CRYSTAL, 1 + random.nextInt(2));
                    }
                } catch (Exception ignored) {}
                return new ItemStack(Items.QUARTZ, count);
        }
    }

    private ItemStack createRandomTemplate(Random random) {
        try {
            if (ModItems.PRINT_TEMPLATE != null) {
                java.util.Collection<PrinterRecipe> recipes = PrinterRecipeRegistry.getAllRecipes();
                if (!recipes.isEmpty()) {
                    PrinterRecipe[] recipeArray = recipes.toArray(new PrinterRecipe[0]);
                    PrinterRecipe selectedRecipe = recipeArray[random.nextInt(recipeArray.length)];
                    return ItemPrintTemplate.createTemplate(ModItems.PRINT_TEMPLATE, selectedRecipe.getTemplateId());
                }
            }
        } catch (Exception ignored) {}
        return new ItemStack(Items.PAPER, 1);
    }

    private void setBlockSafe(World world, BlockPos pos, IBlockState state) {
        if (world.isBlockLoaded(pos)) {
            world.setBlockState(pos, state, 2);
        }
    }

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
