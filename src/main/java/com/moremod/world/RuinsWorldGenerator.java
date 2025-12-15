package com.moremod.world;

import com.moremod.init.ModBlocks;
import com.moremod.init.ModItems;
import com.moremod.item.RegisterItem;
import com.moremod.printer.ItemPrintTemplate;
import com.moremod.printer.PrinterRecipe;
import com.moremod.printer.PrinterRecipeRegistry;
import com.moremod.quarry.QuarryRegistry;
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
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * 科技废墟世界生成器 v3.2
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
 *
 * v3.1 改进：
 * - 新增5种详细结构 (数据方尖碑、生态穹顶、聚变反应环、坠毁空降舱、虚空工厂)
 * - 新增辅助方法 (fillCylinder, setBlockWithDecay, decorateRuin, adjustToTerrain)
 * - 结构总数增加至15种
 * - 改进衰减/风化算法
 * - 优化地形适应
 *
 * v3.2 改进：
 * - 大幅降低生成率 (1/3000 区块 ≈ 0.033%)
 * - 大幅增加奖励品质和数量 (补偿稀有度)
 * - 每个结构放置2-5个哭泣天使刷怪笼
 * - 增加功能性方块奖励种类 (20种)
 * - 增加多个奖励箱 (根据结构等级)
 * - 增加稀有物品掉落 (下界之星、龙息、不死图腾、鞘翅)
 */
public class RuinsWorldGenerator implements IWorldGenerator {

    // ============== 生成配置 ==============
    private static final int MIN_Y = 50;
    private static final int SPAWN_CHANCE = 3000;            // 大幅降低生成率 (1/3000 区块 ≈ 0.033%)
    private static final int MIN_DISTANCE_FROM_SPAWN = 600;  // 增加距离要求

    // 稀有方块概率系数 (提高为1.5倍 - 补偿降低的生成率)
    private static final float SPECIAL_BLOCK_CHANCE_MULTIPLIER = 1.5f;

    // Glitch Armor 出现概率 (15% - 提高补偿)
    private static final float GLITCH_ARMOR_CHANCE = 0.15f;

    // 刷怪笼数量 (每个结构)
    private static final int MIN_SPAWNERS = 2;
    private static final int MAX_SPAWNERS = 5;

    // ============== 结构类型枚举 ==============
    public enum RuinType {
        RESEARCH_OUTPOST("研究前哨站", 12, 12, 8, 2),
        MECHANICAL_COMPLEX("机械综合体", 18, 18, 12, 3),
        SIGNAL_TOWER("信号塔", 10, 10, 25, 2),
        UNDERGROUND_VAULT("地下金库", 14, 14, 10, 4),
        CRASHED_TRANSPORT("坠毁运输船", 20, 12, 8, 3),
        FACTORY_RUINS("废弃工厂", 24, 24, 14, 4),
        QUANTUM_QUARRY_SITE("量子采矿场", 16, 16, 10, 5),
        ENERGY_STATION("能量中继站", 14, 14, 16, 4),
        DATA_CENTER("数据中心废墟", 20, 20, 8, 3),
        TEMPORAL_LAB("时间实验室", 16, 16, 12, 5),
        // v3.1 新增结构
        DATA_OBELISK("数据方尖碑", 9, 9, 20, 3),
        BIO_DOME("生态穹顶", 18, 18, 12, 3),
        FUSION_RING("聚变反应环", 20, 20, 8, 5),
        CRASHED_POD("坠毁空降舱", 12, 8, 6, 2),
        VOID_FACTORY("虚空工厂", 26, 20, 16, 5);

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
            // v3.1 新增结构
            case DATA_OBELISK:
                generateDataObelisk(world, pos, random);
                break;
            case BIO_DOME:
                generateBioDome(world, pos, random);
                break;
            case FUSION_RING:
                generateFusionRing(world, pos, random);
                break;
            case CRASHED_POD:
                generateCrashedPod(world, pos, random);
                break;
            case VOID_FACTORY:
                generateVoidFactory(world, pos, random);
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

    // 放置破损的量子采矿机多方块结构
    private void placeQuantumQuarry(World world, BlockPos pos, Random random) {
        try {
            Block quarryCore = QuarryRegistry.blockQuantumQuarry;
            Block quarryActuator = com.moremod.quarry.QuarryRegistry.blockQuarryActuator;

            if (quarryCore != null && quarryActuator != null) {
                // 放置核心
                setBlockSafe(world, pos, quarryCore.getDefaultState());

                // 六面驱动器 (破损 - 随机缺失1-3个)
                net.minecraft.util.EnumFacing[] faces = net.minecraft.util.EnumFacing.values();
                int missingCount = 1 + random.nextInt(3);  // 缺失1-3个
                java.util.Set<net.minecraft.util.EnumFacing> missingFaces = new java.util.HashSet<>();

                // 随机选择缺失的面
                while (missingFaces.size() < missingCount) {
                    missingFaces.add(faces[random.nextInt(6)]);
                }

                for (net.minecraft.util.EnumFacing face : faces) {
                    BlockPos actuatorPos = pos.offset(face);
                    if (missingFaces.contains(face)) {
                        // 缺失的驱动器 - 放置破损零件
                        if (random.nextFloat() < 0.5f) {
                            setBlockSafe(world, actuatorPos, Blocks.IRON_BLOCK.getDefaultState());
                        } else if (random.nextFloat() < 0.3f) {
                            setBlockSafe(world, actuatorPos, Blocks.REDSTONE_BLOCK.getDefaultState());
                        }
                        // 否则留空
                    } else {
                        // 放置驱动器
                        setBlockSafe(world, actuatorPos, quarryActuator.getDefaultState());
                    }
                }

                System.out.println("[Ruins] 放置了破损的量子采矿机多方块结构于 " + pos + " (缺失" + missingCount + "个驱动器)");
            } else {
                // 备用: 用铁块+红石块模拟
                placeFallbackQuarry(world, pos, random);
            }
        } catch (Exception e) {
            placeFallbackQuarry(world, pos, random);
        }

        // 散落的零件和线缆
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) continue;  // 跳过核心区域
                if (random.nextFloat() < 0.25f) {
                    BlockPos debrisPos = pos.add(dx, 0, dz);
                    if (world.isAirBlock(debrisPos)) {
                        int debrisType = random.nextInt(5);
                        switch (debrisType) {
                            case 0: setBlockSafe(world, debrisPos, Blocks.IRON_BLOCK.getDefaultState()); break;
                            case 1: setBlockSafe(world, debrisPos, Blocks.QUARTZ_BLOCK.getDefaultState()); break;
                            case 2: setBlockSafe(world, debrisPos, Blocks.REDSTONE_BLOCK.getDefaultState()); break;
                            case 3: setBlockSafe(world, debrisPos, Blocks.END_ROD.getDefaultState()); break;
                            default: setBlockSafe(world, debrisPos, Blocks.IRON_BARS.getDefaultState()); break;
                        }
                    }
                }
            }
        }
    }

    // 备用量子采矿机 (方块不存在时)
    private void placeFallbackQuarry(World world, BlockPos pos, Random random) {
        // 核心
        setBlockSafe(world, pos, Blocks.IRON_BLOCK.getDefaultState());
        setBlockSafe(world, pos.up(), Blocks.REDSTONE_BLOCK.getDefaultState());
        setBlockSafe(world, pos.down(), Blocks.OBSIDIAN.getDefaultState());

        // 模拟驱动器
        for (net.minecraft.util.EnumFacing face : net.minecraft.util.EnumFacing.HORIZONTALS) {
            if (random.nextFloat() > 0.3f) {
                setBlockSafe(world, pos.offset(face), Blocks.IRON_BLOCK.getDefaultState());
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

        // 额外特殊方块 (降低概率)
        if (random.nextFloat() < 0.2f) {
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

        // 特殊方块 (降低概率)
        if (random.nextFloat() < 0.3f) {
            placeSpecialBlock(world, pos.add(0, 6, 0), random);
        }
    }

    // ============== v3.1 新增结构 ==============

    // 数据方尖碑 (9x9x20) - 高耸的数据存储塔
    private void generateDataObelisk(World world, BlockPos pos, Random random) {
        clearBuildingArea(world, pos, 5, 5, 22);
        levelGround(world, pos, 5, 5, 3);

        // 基座 (交错图案)
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                IBlockState base = ((x + z) % 2 == 0) ?
                    Blocks.CONCRETE.getStateFromMeta(15) : // 黑色混凝土
                    Blocks.CONCRETE.getStateFromMeta(0);   // 白色混凝土
                setBlockSafe(world, pos.add(x, -1, z), base);
            }
        }

        // 主塔身 (从大到小)
        for (int y = 0; y <= 16; y++) {
            int radius = y < 4 ? 3 : (y < 8 ? 2 : (y < 14 ? 1 : 0));
            float decayChance = 0.05f + (y * 0.015f);

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) == radius || Math.abs(z) == radius) {
                        if (random.nextFloat() > decayChance) {
                            IBlockState block;
                            if (y % 4 == 0) {
                                block = Blocks.SEA_LANTERN.getDefaultState();
                            } else if (y % 2 == 0) {
                                block = Blocks.QUARTZ_BLOCK.getStateFromMeta(2); // 柱子石英
                            } else {
                                block = Blocks.CONCRETE.getStateFromMeta(9); // 青色混凝土
                            }
                            setBlockWithDecay(world, pos.add(x, y, z), block, random, decayChance);
                        }
                    }
                }
            }

            // 中央数据核心
            if (y >= 2 && y <= 12 && y % 2 == 0) {
                setBlockSafe(world, pos.add(0, y, 0), Blocks.REDSTONE_LAMP.getDefaultState());
            }
        }

        // 顶部水晶尖端
        for (int y = 17; y <= 19; y++) {
            setBlockSafe(world, pos.add(0, y, 0), Blocks.END_ROD.getDefaultState());
        }

        // 四角数据柱
        for (int[] corner : new int[][]{{-3, -3}, {-3, 3}, {3, -3}, {3, 3}}) {
            for (int y = 0; y <= 6; y++) {
                if (random.nextFloat() > 0.2f) {
                    IBlockState pillar = (y % 2 == 0) ?
                        Blocks.IRON_BLOCK.getDefaultState() :
                        Blocks.END_ROD.getDefaultState();
                    setBlockSafe(world, pos.add(corner[0], y, corner[1]), pillar);
                }
            }
        }

        // 散落的数据碎片
        decorateRuin(world, pos, random, 6, 4);

        placeRuinContents(world, pos.add(2, 0, 0), random, RuinType.DATA_OBELISK.lootTier);
        placeSpecialBlockGuaranteed(world, pos.add(0, 1, 0), random, 0.25f);
    }

    // 生态穹顶 (18x18x12) - 废弃的生物圈实验室
    private void generateBioDome(World world, BlockPos pos, Random random) {
        clearBuildingArea(world, pos, 10, 10, 14);
        levelGround(world, pos, 10, 10, 3);

        // 穹顶基座
        fillCylinder(world, pos.down(), 8, 2, Blocks.STONEBRICK.getDefaultState(), random, 0.1f);

        // 玻璃穹顶 (球形)
        int radius = 7;
        for (int y = 0; y <= radius; y++) {
            int ringRadius = (int) Math.sqrt(radius * radius - y * y);
            float decayChance = 0.15f + (y * 0.03f);

            for (int x = -ringRadius; x <= ringRadius; x++) {
                for (int z = -ringRadius; z <= ringRadius; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist >= ringRadius - 1 && dist <= ringRadius) {
                        IBlockState glass;
                        if (random.nextFloat() < 0.3f) {
                            glass = Blocks.STAINED_GLASS.getStateFromMeta(9); // 青色玻璃
                        } else {
                            glass = Blocks.GLASS.getDefaultState();
                        }
                        setBlockWithDecay(world, pos.add(x, y, z), glass, random, decayChance);
                    }
                }
            }
        }

        // 内部植被
        for (int i = 0; i < 12; i++) {
            int px = random.nextInt(10) - 5;
            int pz = random.nextInt(10) - 5;
            BlockPos plantPos = pos.add(px, 0, pz);
            if (world.isAirBlock(plantPos)) {
                int plantType = random.nextInt(5);
                switch (plantType) {
                    case 0:
                        setBlockSafe(world, plantPos, Blocks.TALLGRASS.getStateFromMeta(1));
                        break;
                    case 1:
                        setBlockSafe(world, plantPos, Blocks.SAPLING.getStateFromMeta(random.nextInt(6)));
                        break;
                    case 2:
                        setBlockSafe(world, plantPos, Blocks.RED_FLOWER.getStateFromMeta(random.nextInt(9)));
                        break;
                    case 3:
                        setBlockSafe(world, plantPos, Blocks.YELLOW_FLOWER.getDefaultState());
                        break;
                    default:
                        // 放泥土块生草
                        setBlockSafe(world, plantPos.down(), Blocks.GRASS.getDefaultState());
                }
            }
        }

        // 中央培养槽
        setBlockSafe(world, pos, Blocks.WATER.getDefaultState());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                setBlockSafe(world, pos.add(dx, 0, dz), Blocks.GLASS.getDefaultState());
            }
        }

        // 控制室 (一侧)
        for (int x = 5; x <= 7; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 2; y++) {
                    if (y == 0 || y == 2 || z == -2 || z == 2) {
                        setBlockWithDecay(world, pos.add(x, y, z),
                            Blocks.CONCRETE.getStateFromMeta(0), random, 0.2f);
                    }
                }
            }
        }
        setBlockSafe(world, pos.add(6, 1, 0), Blocks.AIR.getDefaultState());

        decorateRuin(world, pos, random, 10, 5);
        placeRuinContents(world, pos.add(6, 0, 0), random, RuinType.BIO_DOME.lootTier);
        placeSpecialBlockGuaranteed(world, pos.add(-3, 0, 3), random, 0.15f);
    }

    // 聚变反应环 (20x20x8) - 环形聚变发电设施
    private void generateFusionRing(World world, BlockPos pos, Random random) {
        clearBuildingArea(world, pos, 11, 11, 10);
        levelGround(world, pos, 11, 11, 3);

        // 基座平台
        fillCylinder(world, pos.down(), 9, 2, Blocks.CONCRETE.getStateFromMeta(8), random, 0.1f);

        // 外环结构
        int outerRadius = 8;
        int innerRadius = 5;

        for (int y = 0; y <= 5; y++) {
            float decayChance = 0.1f + (y * 0.02f);

            for (int angle = 0; angle < 360; angle += 10) {
                double rad = Math.toRadians(angle);

                // 外环
                int ox = (int) Math.round(outerRadius * Math.cos(rad));
                int oz = (int) Math.round(outerRadius * Math.sin(rad));
                IBlockState outerBlock = (y % 2 == 0) ?
                    Blocks.IRON_BLOCK.getDefaultState() :
                    Blocks.QUARTZ_BLOCK.getDefaultState();
                setBlockWithDecay(world, pos.add(ox, y, oz), outerBlock, random, decayChance);

                // 内环
                int ix = (int) Math.round(innerRadius * Math.cos(rad));
                int iz = (int) Math.round(innerRadius * Math.sin(rad));
                IBlockState innerBlock = (y == 2 || y == 3) ?
                    Blocks.SEA_LANTERN.getDefaultState() :
                    Blocks.PURPUR_BLOCK.getDefaultState();
                setBlockWithDecay(world, pos.add(ix, y, iz), innerBlock, random, decayChance);
            }
        }

        // 连接臂 (4条)
        for (int dir = 0; dir < 4; dir++) {
            double rad = Math.toRadians(dir * 90);
            for (int r = innerRadius + 1; r < outerRadius; r++) {
                int ax = (int) Math.round(r * Math.cos(rad));
                int az = (int) Math.round(r * Math.sin(rad));
                for (int y = 0; y <= 4; y++) {
                    if (y == 0 || y == 4) {
                        setBlockWithDecay(world, pos.add(ax, y, az),
                            Blocks.END_ROD.getDefaultState(), random, 0.2f);
                    } else {
                        setBlockWithDecay(world, pos.add(ax, y, az),
                            Blocks.IRON_BARS.getDefaultState(), random, 0.15f);
                    }
                }
            }
        }

        // 中央核心 (等离子体)
        for (int y = 1; y <= 4; y++) {
            setBlockSafe(world, pos.add(0, y, 0), Blocks.BEACON.getDefaultState());
        }
        // 核心底座
        fillCylinder(world, pos, 2, 1, Blocks.OBSIDIAN.getDefaultState(), random, 0.0f);

        // 控制塔
        for (int y = 0; y <= 6; y++) {
            if (random.nextFloat() > 0.15f) {
                setBlockSafe(world, pos.add(outerRadius + 2, y, 0), Blocks.IRON_BLOCK.getDefaultState());
            }
        }
        setBlockSafe(world, pos.add(outerRadius + 2, 3, 0), Blocks.REDSTONE_LAMP.getDefaultState());

        decorateRuin(world, pos, random, 12, 8);
        placeRuinContents(world, pos.add(outerRadius + 1, 0, 2), random, RuinType.FUSION_RING.lootTier);
        placeRuinContents(world, pos.add(-outerRadius - 1, 0, -2), random, RuinType.FUSION_RING.lootTier);
        placeSpecialBlockGuaranteed(world, pos.add(3, 0, 3), random, 0.3f);
    }

    // 坠毁空降舱 (12x8x6) - 小型坠毁逃生舱
    private void generateCrashedPod(World world, BlockPos pos, Random random) {
        // 撞击坑
        int craterRadius = 4;
        for (int x = -craterRadius; x <= craterRadius; x++) {
            for (int z = -craterRadius; z <= craterRadius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist <= craterRadius) {
                    int depth = (int) (3 * (1 - dist / craterRadius));
                    for (int y = 0; y >= -depth; y--) {
                        setBlockSafe(world, pos.add(x, y, z), Blocks.AIR.getDefaultState());
                    }
                    // 焦土
                    if (random.nextFloat() < 0.4f) {
                        setBlockSafe(world, pos.add(x, -depth - 1, z), Blocks.SOUL_SAND.getDefaultState());
                    } else {
                        setBlockSafe(world, pos.add(x, -depth - 1, z), Blocks.DIRT.getDefaultState());
                    }
                }
            }
        }

        // 倾斜的舱体
        int tiltAngle = random.nextInt(30) - 15;
        for (int l = -3; l <= 3; l++) {
            int tiltY = l / 2;
            for (int w = -2; w <= 2; w++) {
                // 舱壁
                if (Math.abs(w) == 2) {
                    if (random.nextFloat() > 0.25f) {
                        setBlockSafe(world, pos.add(l, tiltY, w), Blocks.IRON_BLOCK.getDefaultState());
                        if (random.nextFloat() > 0.4f) {
                            setBlockSafe(world, pos.add(l, tiltY + 1, w), Blocks.IRON_BARS.getDefaultState());
                        }
                    }
                }
                // 舱顶
                if (l >= -2 && l <= 2 && random.nextFloat() > 0.3f) {
                    setBlockSafe(world, pos.add(l, tiltY + 2, w), Blocks.IRON_BLOCK.getDefaultState());
                }
                // 舱底
                if (l >= -2 && l <= 2) {
                    setBlockSafe(world, pos.add(l, tiltY - 1, w), Blocks.IRON_BLOCK.getDefaultState());
                }
            }
        }

        // 窗户
        setBlockSafe(world, pos.add(0, 1, 2), Blocks.GLASS_PANE.getDefaultState());
        setBlockSafe(world, pos.add(0, 1, -2), Blocks.GLASS_PANE.getDefaultState());

        // 舱内设施
        setBlockSafe(world, pos.add(-1, 0, 0), Blocks.REDSTONE_LAMP.getDefaultState());
        setBlockSafe(world, pos.add(1, 0, 0), Blocks.LEVER.getDefaultState());

        // 散落的货物和碎片
        for (int i = 0; i < 6; i++) {
            int dx = random.nextInt(8) - 4;
            int dz = random.nextInt(8) - 4;
            BlockPos debrisPos = pos.add(dx, 0, dz);
            // 调整到地面
            debrisPos = adjustToTerrain(world, debrisPos);
            if (world.isAirBlock(debrisPos)) {
                int debrisType = random.nextInt(4);
                switch (debrisType) {
                    case 0:
                        setBlockSafe(world, debrisPos, Blocks.IRON_BLOCK.getDefaultState());
                        break;
                    case 1:
                        setBlockSafe(world, debrisPos, Blocks.QUARTZ_BLOCK.getDefaultState());
                        break;
                    case 2:
                        setBlockSafe(world, debrisPos, Blocks.CHEST.getDefaultState());
                        break;
                    default:
                        setBlockSafe(world, debrisPos, Blocks.IRON_BARS.getDefaultState());
                }
            }
        }

        placeRuinContents(world, pos, random, RuinType.CRASHED_POD.lootTier);
        placeSpecialBlockGuaranteed(world, pos.add(0, 0, 0), random, 0.12f);
    }

    // 虚空工厂 (26x20x16) - 大型工业废墟
    private void generateVoidFactory(World world, BlockPos pos, Random random) {
        clearBuildingArea(world, pos, 14, 11, 18);
        levelGround(world, pos, 14, 11, 5);

        // 工厂地基
        fillArea(world, pos.add(-13, -2, -10), pos.add(13, -1, 10), Blocks.STONEBRICK.getDefaultState());

        // 主厂房框架
        for (int y = 0; y <= 14; y++) {
            float decayChance = 0.1f + (y * 0.015f);

            // 支撑柱
            for (int[] col : new int[][]{{-12, -9}, {-12, 9}, {12, -9}, {12, 9},
                                         {-12, 0}, {12, 0}, {0, -9}, {0, 9}}) {
                if (random.nextFloat() > decayChance * 0.5f) {
                    setBlockSafe(world, pos.add(col[0], y, col[1]), Blocks.IRON_BLOCK.getDefaultState());
                }
            }

            // 墙壁 (底部实心, 上部镂空)
            if (y <= 4) {
                for (int x = -12; x <= 12; x++) {
                    setBlockWithDecay(world, pos.add(x, y, -9), getRandomRuinBlock(random), random, decayChance);
                    setBlockWithDecay(world, pos.add(x, y, 9), getRandomRuinBlock(random), random, decayChance);
                }
                for (int z = -9; z <= 9; z++) {
                    setBlockWithDecay(world, pos.add(-12, y, z), getRandomRuinBlock(random), random, decayChance);
                    setBlockWithDecay(world, pos.add(12, y, z), getRandomRuinBlock(random), random, decayChance);
                }
            } else if (y >= 5 && y <= 8) {
                // 窗户层
                for (int x = -11; x <= 11; x += 4) {
                    setBlockWithDecay(world, pos.add(x, y, -9), Blocks.IRON_BARS.getDefaultState(), random, decayChance);
                    setBlockWithDecay(world, pos.add(x, y, 9), Blocks.IRON_BARS.getDefaultState(), random, decayChance);
                }
            }

            // 屋顶桁架 (顶层)
            if (y >= 12 && y <= 14) {
                for (int x = -10; x <= 10; x += 2) {
                    setBlockWithDecay(world, pos.add(x, y, 0), Blocks.IRON_BARS.getDefaultState(), random, decayChance);
                }
            }
        }

        // 生产线设备
        for (int z = -6; z <= 6; z += 3) {
            // 熔炉阵列
            setBlockSafe(world, pos.add(-8, 0, z), Blocks.FURNACE.getDefaultState());
            setBlockSafe(world, pos.add(-8, 1, z), Blocks.HOPPER.getDefaultState());

            // 活塞加工台
            setBlockSafe(world, pos.add(-4, 0, z), Blocks.PISTON.getDefaultState());
            setBlockSafe(world, pos.add(-4, 1, z), Blocks.STICKY_PISTON.getDefaultState());

            // 传送带 (用铁块模拟)
            for (int x = -2; x <= 4; x++) {
                setBlockWithDecay(world, pos.add(x, 0, z),
                    Blocks.IRON_BLOCK.getDefaultState(), random, 0.15f);
            }

            // 储存箱
            setBlockSafe(world, pos.add(6, 0, z), Blocks.CHEST.getDefaultState());
            setBlockSafe(world, pos.add(8, 0, z), Blocks.CHEST.getDefaultState());
        }

        // 中央控制平台
        fillArea(world, pos.add(-3, 0, -3), pos.add(3, 0, 3), Blocks.QUARTZ_BLOCK.getDefaultState());
        setBlockSafe(world, pos.add(0, 1, 0), Blocks.ANVIL.getDefaultState());
        setBlockSafe(world, pos.add(-2, 1, -2), Blocks.REDSTONE_LAMP.getDefaultState());
        setBlockSafe(world, pos.add(2, 1, 2), Blocks.REDSTONE_LAMP.getDefaultState());
        setBlockSafe(world, pos.add(-2, 1, 2), Blocks.DAYLIGHT_DETECTOR.getDefaultState());
        setBlockSafe(world, pos.add(2, 1, -2), Blocks.LEVER.getDefaultState());

        // 烟囱群
        for (int[] chimney : new int[][]{{10, -7}, {10, 7}}) {
            for (int y = 0; y <= 20; y++) {
                float decay = 0.05f + (y * 0.025f);
                if (random.nextFloat() > decay) {
                    for (int dx = 0; dx <= 1; dx++) {
                        for (int dz = 0; dz <= 1; dz++) {
                            setBlockSafe(world, pos.add(chimney[0] + dx, y, chimney[1] + dz),
                                Blocks.BRICK_BLOCK.getDefaultState());
                        }
                    }
                }
            }
        }

        // 管道网络
        for (int x = -10; x <= 10; x += 5) {
            for (int y = 6; y <= 8; y++) {
                setBlockWithDecay(world, pos.add(x, y, 0),
                    Blocks.END_ROD.getDefaultState(), random, 0.2f);
            }
        }

        // 货物堆
        for (int i = 0; i < 8; i++) {
            int gx = random.nextInt(20) - 10;
            int gz = random.nextInt(14) - 7;
            BlockPos cargoPos = pos.add(gx, 0, gz);
            if (world.isAirBlock(cargoPos)) {
                int cargoType = random.nextInt(3);
                switch (cargoType) {
                    case 0:
                        setBlockSafe(world, cargoPos, Blocks.CHEST.getDefaultState());
                        break;
                    case 1:
                        setBlockSafe(world, cargoPos, Blocks.IRON_BLOCK.getDefaultState());
                        break;
                    default:
                        setBlockSafe(world, cargoPos, Blocks.QUARTZ_BLOCK.getDefaultState());
                }
            }
        }

        decorateRuin(world, pos, random, 14, 10);

        // 多个战利品点
        placeRuinContents(world, pos.add(-8, 0, 0), random, RuinType.VOID_FACTORY.lootTier);
        placeRuinContents(world, pos.add(8, 0, 0), random, RuinType.VOID_FACTORY.lootTier);
        placeRuinContents(world, pos.add(0, 0, 6), random, RuinType.VOID_FACTORY.lootTier);

        // 特殊方块 (降低概率)
        placeSpecialBlockGuaranteed(world, pos.add(0, 1, 0), random, 0.35f);
        placeSpecialBlockGuaranteed(world, pos.add(-6, 0, -5), random, 0.2f);
    }

    // ============== v3.1 新增辅助方法 ==============

    // 填充圆柱体
    private void fillCylinder(World world, BlockPos center, int radius, int height,
                              IBlockState state, Random random, float decayChance) {
        for (int y = 0; y < height; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        setBlockWithDecay(world, center.add(x, y, z), state, random, decayChance);
                    }
                }
            }
        }
    }

    // 带衰减的方块放置
    private void setBlockWithDecay(World world, BlockPos pos, IBlockState state,
                                   Random random, float decayChance) {
        if (random.nextFloat() > decayChance) {
            setBlockSafe(world, pos, state);
        }
    }

    // 装饰废墟 (散落碎片)
    private void decorateRuin(World world, BlockPos center, Random random, int radius, int count) {
        for (int i = 0; i < count; i++) {
            int dx = random.nextInt(radius * 2) - radius;
            int dz = random.nextInt(radius * 2) - radius;
            BlockPos debrisPos = center.add(dx, 0, dz);

            // 寻找地面
            for (int y = 3; y >= -3; y--) {
                BlockPos checkPos = debrisPos.add(0, y, 0);
                if (world.isAirBlock(checkPos) && !world.isAirBlock(checkPos.down())) {
                    debrisPos = checkPos;
                    break;
                }
            }

            if (world.isAirBlock(debrisPos)) {
                int debrisType = random.nextInt(6);
                switch (debrisType) {
                    case 0:
                        setBlockSafe(world, debrisPos, Blocks.COBBLESTONE.getDefaultState());
                        break;
                    case 1:
                        setBlockSafe(world, debrisPos, Blocks.IRON_BARS.getDefaultState());
                        break;
                    case 2:
                        setBlockSafe(world, debrisPos, Blocks.STONE_SLAB.getDefaultState());
                        break;
                    case 3:
                        setBlockSafe(world, debrisPos, Blocks.END_ROD.getDefaultState());
                        break;
                    case 4:
                        setBlockSafe(world, debrisPos, Blocks.GRAVEL.getDefaultState());
                        break;
                    default:
                        // 不放置
                        break;
                }
            }
        }
    }

    // 保证放置特殊方块 (带概率)
    private void placeSpecialBlockGuaranteed(World world, BlockPos pos, Random random, float chance) {
        if (random.nextFloat() < chance * SPECIAL_BLOCK_CHANCE_MULTIPLIER) {
            placeSpecialBlock(world, pos, random);
        }
    }

    // 调整到地形高度
    private BlockPos adjustToTerrain(World world, BlockPos pos) {
        // 向上搜索
        for (int y = 0; y <= 5; y++) {
            BlockPos checkUp = pos.up(y);
            if (world.isAirBlock(checkUp) && !world.isAirBlock(checkUp.down())) {
                return checkUp;
            }
        }
        // 向下搜索
        for (int y = 0; y <= 5; y++) {
            BlockPos checkDown = pos.down(y);
            if (world.isAirBlock(checkDown) && !world.isAirBlock(checkDown.down())) {
                return checkDown;
            }
        }
        return pos;
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
        if (random.nextFloat() < 0.15f) {
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

        if (random.nextFloat() < 0.25f) {
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

        if (random.nextFloat() < 0.2f) {
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
        // ★ 放置多个奖励箱 (根据等级) ★
        int chestCount = 1 + (lootTier / 2);
        for (int i = 0; i < chestCount; i++) {
            BlockPos chestPos = center.add(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            // 寻找合适位置
            for (int attempt = 0; attempt < 5; attempt++) {
                if (world.isAirBlock(chestPos) || world.getBlockState(chestPos).getBlock() == Blocks.TALLGRASS) {
                    setBlockSafe(world, chestPos, Blocks.CHEST.getDefaultState());
                    TileEntity te = world.getTileEntity(chestPos);
                    if (te instanceof TileEntityChest) {
                        fillChestWithLoot((TileEntityChest) te, random, lootTier);
                    }
                    break;
                }
                chestPos = center.add(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            }
        }

        // ★ 放置多个功能性方块 ★
        int specialBlockCount = 1 + random.nextInt(lootTier);
        for (int i = 0; i < specialBlockCount; i++) {
            float chance = 0.4f * SPECIAL_BLOCK_CHANCE_MULTIPLIER;
            if (random.nextFloat() < chance) {
                BlockPos specialPos = center.add(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
                placeSpecialBlock(world, specialPos, random);
            }
        }

        // ★ 放置哭泣天使刷怪笼 ★
        int spawnerCount = MIN_SPAWNERS + random.nextInt(MAX_SPAWNERS - MIN_SPAWNERS + 1);
        for (int i = 0; i < spawnerCount; i++) {
            BlockPos spawnerPos = center.add(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
            placeWeepingAngelSpawner(world, spawnerPos, random);
        }
    }

    // ★★★ 放置哭泣天使刷怪笼 ★★★
    private void placeWeepingAngelSpawner(World world, BlockPos pos, Random random) {
        // 寻找合适的位置
        BlockPos spawnerPos = pos;
        for (int attempt = 0; attempt < 10; attempt++) {
            if (world.isAirBlock(spawnerPos) && !world.isAirBlock(spawnerPos.down())) {
                break;
            }
            spawnerPos = pos.add(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
        }

        // 放置刷怪笼
        setBlockSafe(world, spawnerPos, Blocks.MOB_SPAWNER.getDefaultState());
        TileEntity te = world.getTileEntity(spawnerPos);
        if (te instanceof TileEntityMobSpawner) {
            TileEntityMobSpawner spawner = (TileEntityMobSpawner) te;
            // 设置为哭泣天使
            spawner.getSpawnerBaseLogic().setEntityId(new ResourceLocation("moremod", "weeping_angel"));
            // 调整刷怪笼参数
            NBTTagCompound nbt = new NBTTagCompound();
            spawner.getSpawnerBaseLogic().writeToNBT(nbt);
            nbt.setShort("MinSpawnDelay", (short) 400);   // 最小延迟 20秒
            nbt.setShort("MaxSpawnDelay", (short) 800);   // 最大延迟 40秒
            nbt.setShort("SpawnCount", (short) 2);        // 每次刷出2只
            nbt.setShort("MaxNearbyEntities", (short) 4); // 附近最多4只
            nbt.setShort("RequiredPlayerRange", (short) 16); // 玩家范围16格
            spawner.getSpawnerBaseLogic().readFromNBT(nbt);
            System.out.println("[Ruins] 放置哭泣天使刷怪笼 @ " + spawnerPos);
        }
    }

    private void fillChestWithLoot(TileEntityChest chest, Random random, int tier) {
        // ★ 增加材料数量 (补偿稀有生成率) ★
        int materialCount = 4 + random.nextInt(4) + tier;
        for (int i = 0; i < materialCount; i++) {
            int slot = random.nextInt(27);
            ItemStack material = getRandomMaterial(random, tier);
            chest.setInventorySlotContents(slot, material);
        }

        // ★ 打印模版概率提高 ★
        if (random.nextFloat() < 0.25f * tier) {
            int slot = random.nextInt(27);
            ItemStack template = createRandomTemplate(random);
            chest.setInventorySlotContents(slot, template);
        }

        // ★ Glitch Armor (提高概率) ★
        if (random.nextFloat() < GLITCH_ARMOR_CHANCE) {
            int slot = random.nextInt(27);
            ItemStack glitchGear = createGlitchArmorPiece(random);
            if (!glitchGear.isEmpty()) {
                chest.setInventorySlotContents(slot, glitchGear);
            }
        }

        // ★ 故障装备 (额外奖励) ★
        if (random.nextFloat() < 0.3f * tier) {
            int slot = random.nextInt(27);
            ItemStack faultyGear = createFaultyGear(random, tier);
            chest.setInventorySlotContents(slot, faultyGear);
        }

        // ★ 稀有物品 (下界之星、龙息等) ★
        if (tier >= 3 && random.nextFloat() < 0.15f) {
            int slot = random.nextInt(27);
            int rareType = random.nextInt(4);
            switch (rareType) {
                case 0:
                    chest.setInventorySlotContents(slot, new ItemStack(Items.NETHER_STAR, 1));
                    break;
                case 1:
                    chest.setInventorySlotContents(slot, new ItemStack(Items.DRAGON_BREATH, 2 + random.nextInt(3)));
                    break;
                case 2:
                    chest.setInventorySlotContents(slot, new ItemStack(Items.TOTEM_OF_UNDYING, 1));
                    break;
                default:
                    chest.setInventorySlotContents(slot, new ItemStack(Items.ELYTRA, 1));
            }
        }

        // ★ 模组特殊物品 ★
        if (random.nextFloat() < 0.2f * tier) {
            try {
                int slot = random.nextInt(27);
                int modItemType = random.nextInt(6);
                switch (modItemType) {
                    case 0:
                        if (ModItems.ANCIENT_CORE_FRAGMENT != null) {
                            chest.setInventorySlotContents(slot, new ItemStack(ModItems.ANCIENT_CORE_FRAGMENT, 1 + random.nextInt(3)));
                        }
                        break;
                    case 1:
                        if (ModItems.RIFT_CRYSTAL != null) {
                            chest.setInventorySlotContents(slot, new ItemStack(ModItems.RIFT_CRYSTAL, 1 + random.nextInt(2)));
                        }
                        break;
                    case 2:
                        if (ModItems.ETHEREAL_SHARD != null) {
                            chest.setInventorySlotContents(slot, new ItemStack(ModItems.ETHEREAL_SHARD, 1));
                        }
                        break;
                    case 3:
                        if (ModItems.VOID_ICHOR != null) {
                            chest.setInventorySlotContents(slot, new ItemStack(ModItems.VOID_ICHOR, 1));
                        }
                        break;
                    case 4:
                        if (ModItems.DIMENSIONAL_WEAVER_CORE != null) {
                            chest.setInventorySlotContents(slot, new ItemStack(ModItems.DIMENSIONAL_WEAVER_CORE, 1));
                        }
                        break;
                    default:
                        if (ModItems.SPACETIME_FABRIC != null) {
                            chest.setInventorySlotContents(slot, new ItemStack(ModItems.SPACETIME_FABRIC, 1 + random.nextInt(2)));
                        }
                }
            } catch (Exception ignored) {}
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

    // ★★★ 创建Glitch Armor ★★★
    private ItemStack createGlitchArmorPiece(Random random) {
        try {
            int type = random.nextInt(4);
            ItemStack armor;

            switch (type) {
                case 0:
                    if (RegisterItem.GLITCH_HELMET != null) {
                        armor = new ItemStack(RegisterItem.GLITCH_HELMET);
                    } else return ItemStack.EMPTY;
                    break;
                case 1:
                    if (RegisterItem.GLITCH_CHESTPLATE != null) {
                        armor = new ItemStack(RegisterItem.GLITCH_CHESTPLATE);
                    } else return ItemStack.EMPTY;
                    break;
                case 2:
                    if (RegisterItem.GLITCH_LEGGINGS != null) {
                        armor = new ItemStack(RegisterItem.GLITCH_LEGGINGS);
                    } else return ItemStack.EMPTY;
                    break;
                case 3:
                    if (RegisterItem.GLITCH_BOOTS != null) {
                        armor = new ItemStack(RegisterItem.GLITCH_BOOTS);
                    } else return ItemStack.EMPTY;
                    break;
                default:
                    return ItemStack.EMPTY;
            }

            // 添加Lore说明来源
            NBTTagCompound nbt = armor.getTagCompound();
            if (nbt == null) nbt = new NBTTagCompound();

            NBTTagCompound display = nbt.getCompoundTag("display");
            NBTTagList lore = new NBTTagList();
            lore.appendTag(new net.minecraft.nbt.NBTTagString("§7來自遠古科技廢墟的遺物"));
            lore.appendTag(new net.minecraft.nbt.NBTTagString("§d§o數據異常..."));
            display.setTag("Lore", lore);
            nbt.setTag("display", display);
            armor.setTagCompound(nbt);

            System.out.println("[Ruins] 生成了稀有的 Glitch Armor: " + armor.getDisplayName());
            return armor;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private void placeSpecialBlock(World world, BlockPos pos, Random random) {
        Block specialBlock = null;
        int type = random.nextInt(20);  // 扩大选择范围

        try {
            switch (type) {
                case 0:
                case 1:
                    specialBlock = ModBlocks.TEMPORAL_ACCELERATOR;  // 时间加速器
                    break;
                case 2:
                case 3:
                    specialBlock = ModBlocks.PROTECTION_FIELD_GENERATOR;  // 保护力场
                    break;
                case 4:
                    specialBlock = ModBlocks.RESPAWN_CHAMBER_CORE;  // 重生仓核心
                    break;
                case 5:
                    specialBlock = ModBlocks.dimensionLoom;  // 维度织布机
                    break;
                case 6:
                case 7:
                    specialBlock = ModBlocks.PRINTER;  // 打印机
                    break;
                case 8:
                    specialBlock = QuarryRegistry.blockQuantumQuarry;  // 量子采矿机
                    break;
                case 9:
                    specialBlock = ModBlocks.UPGRADE_CHAMBER_CORE;  // 升级仓核心
                    break;
                case 10:
                    specialBlock = ModBlocks.SIMPLE_WISDOM_SHRINE;  // 智慧祭坛
                    break;
                case 11:
                    specialBlock = ModBlocks.OIL_GENERATOR;  // 石油发电机
                    break;
                case 12:
                    specialBlock = ModBlocks.CHARGING_STATION;  // 充能站
                    break;
                case 13:
                    specialBlock = ModBlocks.ENERGY_LINK;  // 能量链接器
                    break;
                case 14:
                    specialBlock = ModBlocks.BIO_GENERATOR;  // 生物质发电机
                    break;
                case 15:
                    specialBlock = ModBlocks.TRADING_STATION;  // 交易站
                    break;
                case 16:
                    specialBlock = ModBlocks.RITUAL_CORE;  // 仪式核心
                    break;
                case 17:
                    specialBlock = ModBlocks.FAKE_PLAYER_ACTIVATOR;  // 假玩家激活器
                    break;
                case 18:
                    specialBlock = Blocks.BEACON;  // 信标
                    break;
                default:
                    specialBlock = Blocks.ENCHANTING_TABLE;  // 附魔台
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
        // 减少奖励数量 (原: 2 + tier*3, 现: 1 + tier)
        int type = random.nextInt(15 + tier);
        int count = 1 + random.nextInt(Math.max(1, tier));

        switch (type) {
            case 0:
            case 1:
            case 2:
            case 3:
                return new ItemStack(Items.IRON_INGOT, count);
            case 4:
            case 5:
                return new ItemStack(Items.GOLD_INGOT, Math.max(1, count - 1));
            case 6:
            case 7:
                return new ItemStack(Items.REDSTONE, count + 1);
            case 8:
            case 9:
                return new ItemStack(Items.COAL, count + 2);
            case 10:
                // 钻石更稀有
                return new ItemStack(Items.DIAMOND, 1);
            case 11:
                return new ItemStack(Items.EMERALD, 1);
            case 12:
                return new ItemStack(Items.ENDER_PEARL, 1 + random.nextInt(2));
            case 13:
                // 下界之星极为稀有 (仅tier 4+且4%概率)
                if (tier >= 4 && random.nextFloat() < 0.04f) {
                    return new ItemStack(Items.NETHER_STAR, 1);
                }
                return new ItemStack(Items.BLAZE_POWDER, 1 + random.nextInt(2));
            default:
                try {
                    if (ModItems.ANCIENT_CORE_FRAGMENT != null && random.nextFloat() < 0.3f) {
                        return new ItemStack(ModItems.ANCIENT_CORE_FRAGMENT, 1);
                    }
                    if (ModItems.RIFT_CRYSTAL != null && random.nextFloat() < 0.3f) {
                        return new ItemStack(ModItems.RIFT_CRYSTAL, 1);
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
