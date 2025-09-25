package com.moremod.dimension;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * 虚空结构生成器 - 时空破碎主题强化版
 * 营造维度裂隙、空间扭曲、时间错位的氛围
 */
public class VoidStructureGenerator {

    private static final Random random = new Random();

    // 结构类型枚举（权重仅相对，自动归一）
    public enum StructureType {
        // 原有结构
        FLOATING_ISLAND("浮空岛", 0.08),
        CRYSTAL_FORMATION("水晶阵", 0.06),
        ANCIENT_PLATFORM("远古平台", 0.05),
        VOID_BRIDGE("虚空桥", 0.04),
        TREASURE_VAULT("宝库", 0.03),
        GARDEN_SPHERE("花园球", 0.05),
        RUINED_TOWER("废弃塔", 0.06),
        ENERGY_CORE("能量核心", 0.04),
        MINING_OUTPOST("采矿前哨", 0.05),
        VOID_FORTRESS("虚空要塞", 0.03),
        CRYSTAL_GARDEN("水晶花园", 0.05),

        // 碎片化主题
        NETHER_SHARD("下界碎片", 0.06),
        END_FRAGMENT("末地碎片", 0.05),
        RUINED_PORTAL_CLUSTER("废弃传送遗址簇", 0.04),
        PRISMARINE_SHARD("海晶碎片", 0.04),
        GRAVITY_RIBBON("重力缎带", 0.02),
        RIFT_SPIRE("裂隙尖塔", 0.02),
        OVERWORLD_CHUNK("浮空原野块", 0.05),

        // 时空破碎主题
        TEMPORAL_FRACTURE("时间裂痕", 0.03),
        DIMENSIONAL_TEAR("维度撕裂点", 0.03),
        VOID_VORTEX("虚空漩涡", 0.02),
        SHATTERED_REALITY("破碎现实", 0.03),
        FROZEN_EXPLOSION("冻结爆炸", 0.02),
        INVERTED_RUINS("倒悬废墟", 0.03),
        REALITY_SPLICE("现实拼接", 0.02),
        CHAOS_NEXUS("混沌枢纽", 0.02),
        TIME_LOOP_FRAGMENT("时间循环残片", 0.02),
        VOID_CORAL("虚空珊瑚", 0.03),
        MIRROR_SHARD("镜像碎片", 0.02),
        QUANTUM_SCAFFOLD("量子脚手架", 0.02);

        public final String name;
        public final double chance;

        StructureType(String name, double chance) {
            this.name = name;
            this.chance = chance;
        }
    }

    /**
     * 在指定位置周围生成随机虚空结构
     */
    public static void generateNearbyStructures(World world, BlockPos centerPos, int radius) {
        if (world.isRemote) return;

        int structureCount = 3 + random.nextInt(5); // 3-7个结构

        int minDist = Math.min(50, Math.max(16, radius / 4));
        int maxDist = Math.max(minDist + 16, radius);

        for (int i = 0; i < structureCount; i++) {
            int distance = minDist + random.nextInt(Math.max(1, maxDist - minDist + 1));
            double angle = random.nextDouble() * Math.PI * 2;

            int x = centerPos.getX() + (int)(Math.cos(angle) * distance);
            int z = centerPos.getZ() + (int)(Math.sin(angle) * distance);
            int y = 96 + random.nextInt(64);

            BlockPos structurePos = new BlockPos(x, y, z);

            // 预加载区块
            world.getChunk(structurePos.getX() >> 4, structurePos.getZ() >> 4);

            StructureType type = selectRandomStructure();
            generateStructure(world, structurePos, type);

            System.out.println("[虚空结构] 生成 " + type.name + " at " + structurePos);
        }
    }

    /**
     * 生成具体结构
     */
    public static void generateStructure(World world, BlockPos pos, StructureType type) {
        switch (type) {
            // 原有结构
            case FLOATING_ISLAND:       generateFloatingIsland(world, pos); break;
            case CRYSTAL_FORMATION:     generateCrystalFormation(world, pos); break;
            case ANCIENT_PLATFORM:      generateAncientPlatform(world, pos); break;
            case VOID_BRIDGE:           generateVoidBridge(world, pos); break;
            case TREASURE_VAULT:        generateTreasureVault(world, pos); break;
            case GARDEN_SPHERE:         generateGardenSphere(world, pos); break;
            case RUINED_TOWER:          generateRuinedTower(world, pos); break;
            case ENERGY_CORE:           generateEnergyCore(world, pos); break;
            case MINING_OUTPOST:        generateMiningOutpost(world, pos); break;
            case VOID_FORTRESS:         generateVoidFortress(world, pos); break;
            case CRYSTAL_GARDEN:        generateCrystalGarden(world, pos); break;

            // 碎片化主题
            case NETHER_SHARD:          generateNetherShard(world, pos); break;
            case END_FRAGMENT:          generateEndFragment(world, pos); break;
            case RUINED_PORTAL_CLUSTER: generateRuinedPortalCluster(world, pos); break;
            case PRISMARINE_SHARD:      generatePrismarineShard(world, pos); break;
            case GRAVITY_RIBBON:        generateGravityRibbon(world, pos); break;
            case RIFT_SPIRE:            generateRiftSpire(world, pos); break;
            case OVERWORLD_CHUNK:       generateOverworldChunk(world, pos); break;

            // 时空破碎主题
            case TEMPORAL_FRACTURE:     generateTemporalFracture(world, pos); break;
            case DIMENSIONAL_TEAR:      generateDimensionalTear(world, pos); break;
            case VOID_VORTEX:           generateVoidVortex(world, pos); break;
            case SHATTERED_REALITY:     generateShatteredReality(world, pos); break;
            case FROZEN_EXPLOSION:      generateFrozenExplosion(world, pos); break;
            case INVERTED_RUINS:        generateInvertedRuins(world, pos); break;
            case REALITY_SPLICE:        generateRealitySplice(world, pos); break;
            case CHAOS_NEXUS:           generateChaosNexus(world, pos); break;
            case TIME_LOOP_FRAGMENT:    generateTimeLoopFragment(world, pos); break;
            case VOID_CORAL:            generateVoidCoral(world, pos); break;
            case MIRROR_SHARD:          generateMirrorShard(world, pos); break;
            case QUANTUM_SCAFFOLD:      generateQuantumScaffold(world, pos); break;
        }
    }

    // ===================== 原有结构 =====================

    private static void generateFloatingIsland(World world, BlockPos center) {
        int radius = 8 + random.nextInt(7);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -3; y <= 2; y++) {
                    double dist = Math.sqrt(x*x + z*z + y*y*2);
                    if (dist <= radius) {
                        BlockPos p = center.add(x, y, z);
                        if (y < 0) {
                            world.setBlockState(p, Blocks.STONE.getDefaultState(), 2);
                            if (random.nextDouble() < 0.05) world.setBlockState(p, selectRandomOre(), 2);
                        } else if (y == 0) {
                            world.setBlockState(p, Blocks.GRASS.getDefaultState(), 2);
                        }
                    }
                }
            }
        }

        for (int i = 0; i < radius/2; i++) {
            int x = center.getX() + random.nextInt(radius*2) - radius;
            int z = center.getZ() + random.nextInt(radius*2) - radius;
            BlockPos treePos = new BlockPos(x, center.getY() + 1, z);
            if (world.getBlockState(treePos.down()).getBlock() == Blocks.GRASS) {
                generateSmallTree(world, treePos);
            }
        }

        for (int i = 0; i < 3; i++) {
            BlockPos glowPos = center.add(
                    random.nextInt(radius) - radius/2, -2, random.nextInt(radius) - radius/2
            );
            world.setBlockState(glowPos, Blocks.GLOWSTONE.getDefaultState(), 2);
        }
    }

    private static void generateCrystalFormation(World world, BlockPos center) {
        for (int y = 0; y < 8; y++) world.setBlockState(center.up(y), Blocks.SEA_LANTERN.getDefaultState(), 2);

        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI * 2 * i) / 6;
            int x = (int)(Math.cos(angle) * 5);
            int z = (int)(Math.sin(angle) * 5);
            int h = 3 + random.nextInt(3);
            for (int y = 0; y < h; y++) {
                BlockPos p = center.add(x, y, z);
                IBlockState crystal = random.nextBoolean() ? Blocks.QUARTZ_BLOCK.getDefaultState()
                        : Blocks.PACKED_ICE.getDefaultState();
                world.setBlockState(p, crystal, 2);
            }
        }

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (Math.abs(x) + Math.abs(z) <= 4) {
                    world.setBlockState(center.add(x, -1, z), Blocks.PURPUR_BLOCK.getDefaultState(), 2);
                }
            }
        }
        world.setBlockState(center.up(9), Blocks.BEACON.getDefaultState(), 2);
    }

    private static void generateAncientPlatform(World world, BlockPos center) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                if (Math.abs(x) == 5 || Math.abs(z) == 5) {
                    world.setBlockState(center.add(x, 0, z), Blocks.OBSIDIAN.getDefaultState(), 2);
                    if (Math.abs(x) == 5 && Math.abs(z) == 5) {
                        for (int y = 1; y <= 4; y++)
                            world.setBlockState(center.add(x, y, z), Blocks.OBSIDIAN.getDefaultState(), 2);
                        world.setBlockState(center.add(x, 5, z), Blocks.END_ROD.getDefaultState(), 2);
                    }
                } else {
                    world.setBlockState(center.add(x, 0, z), Blocks.STONE.getDefaultState(), 2);
                }
            }
        }
        BlockPos chestPos = center.up(1);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
        TileEntityChest chest = (TileEntityChest) world.getTileEntity(chestPos);
        if (chest != null) addRandomLoot(chest);

        for (int i = 0; i < 4; i++) {
            BlockPos floatPos = center.add(random.nextInt(11) - 5, 3 + random.nextInt(3), random.nextInt(11) - 5);
            world.setBlockState(floatPos, Blocks.GLOWSTONE.getDefaultState(), 2);
        }
    }

    private static void generateVoidBridge(World world, BlockPos center) {
        generateSmallPlatform(world, center, 4);
        BlockPos endPos = center.add(20 + random.nextInt(10), random.nextInt(5) - 2, 0);
        generateSmallPlatform(world, endPos, 4);

        int steps = 20;
        for (int i = 0; i <= steps; i++) {
            float t = (float)i / steps;
            int x = (int)(center.getX() + (endPos.getX() - center.getX()) * t);
            int y = (int)(center.getY() + (endPos.getY() - center.getY()) * t);
            int z = center.getZ();
            BlockPos p = new BlockPos(x, y, z);

            for (int w = -1; w <= 1; w++)
                world.setBlockState(p.add(0, 0, w), Blocks.QUARTZ_BLOCK.getDefaultState(), 2);

            if (i % 4 == 0) {
                world.setBlockState(p.add(0, 1, -2), Blocks.IRON_BARS.getDefaultState(), 2);
                world.setBlockState(p.add(0, 1, 2), Blocks.IRON_BARS.getDefaultState(), 2);
            }
        }
    }

    private static void generateTreasureVault(World world, BlockPos center) {
        for (int x = -4; x <= 4; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = -4; z <= 4; z++) {
                    if (Math.abs(x) == 4 || Math.abs(z) == 4 || y == 0 || y == 4) {
                        world.setBlockState(center.add(x, y, z), Blocks.GOLD_BLOCK.getDefaultState(), 2);
                    } else {
                        world.setBlockState(center.add(x, y, z), Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }
        BlockPos[] chestPositions = {
                center.add(-2, 1, -2), center.add(2, 1, -2),
                center.add(-2, 1, 2),  center.add(2, 1, 2),
                center.add(0, 1, 0)
        };
        for (BlockPos p : chestPositions) {
            world.setBlockState(p, Blocks.CHEST.getDefaultState(), 2);
            TileEntityChest chest = (TileEntityChest) world.getTileEntity(p);
            if (chest != null) addRandomLoot(chest);
        }
        world.setBlockState(center.up(3), Blocks.SEA_LANTERN.getDefaultState(), 2);
    }

    private static void generateGardenSphere(World world, BlockPos center) {
        int r = 6;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    double d = Math.sqrt(x*x + y*y + z*z);
                    BlockPos p = center.add(x, y, z);
                    if (d >= r - 1 && d <= r) {
                        world.setBlockState(p, Blocks.GLASS.getDefaultState(), 2);
                    } else if (d < r - 1 && y < 0) {
                        world.setBlockState(p, Blocks.DIRT.getDefaultState(), 2);
                    } else if (d < r - 1 && y == 0) {
                        world.setBlockState(p, Blocks.GRASS.getDefaultState(), 2);
                    }
                }
            }
        }
        world.setBlockState(center, Blocks.WATER.getDefaultState(), 2);

        for (int i = 0; i < 10; i++) {
            BlockPos flowerPos = center.add(random.nextInt(r*2) - r, 1, random.nextInt(r*2) - r);
            if (world.getBlockState(flowerPos.down()).getBlock() == Blocks.GRASS) {
                world.setBlockState(flowerPos, selectRandomFlower(), 2);
            }
        }
    }

    private static void generateRuinedTower(World world, BlockPos center) {
        int height = 15 + random.nextInt(10);
        int radius = 4;

        for (int y = 0; y < height; y++) {
            double intact = 1.0 - (double)y / height * 0.5;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x*x + z*z <= radius*radius) {
                        if (Math.abs(x) == radius || Math.abs(z) == radius) {
                            if (random.nextDouble() < intact) {
                                IBlockState b = random.nextBoolean() ? Blocks.COBBLESTONE.getDefaultState()
                                        : Blocks.MOSSY_COBBLESTONE.getDefaultState();
                                world.setBlockState(center.add(x, y, z), b, 2);
                            }
                        } else if (y % 5 == 0 && y > 0) {
                            if (random.nextDouble() < intact - 0.2) {
                                world.setBlockState(center.add(x, y, z), Blocks.WOODEN_SLAB.getDefaultState(), 2);
                            }
                        }
                    }
                }
            }
            double angle = (y * Math.PI / 3);
            int sx = (int)(Math.cos(angle) * (radius - 1));
            int sz = (int)(Math.sin(angle) * (radius - 1));
            world.setBlockState(center.add(sx, y, sz), Blocks.STONE_BRICK_STAIRS.getDefaultState(), 2);
        }

        if (random.nextDouble() < 0.7) {
            BlockPos chestPos = center.up(height);
            world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
            TileEntityChest chest = (TileEntityChest) world.getTileEntity(chestPos);
            if (chest != null) addRandomLoot(chest);
        }
    }

    private static void generateEnergyCore(World world, BlockPos center) {
        world.setBlockState(center, Blocks.BEACON.getDefaultState(), 2);

        for (int ring = 0; ring < 3; ring++) {
            int r = 3 + ring * 2;
            int y = ring * 2;
            for (int i = 0; i < 16; i++) {
                double a = (Math.PI * 2 * i) / 16;
                int x = (int)(Math.cos(a) * r);
                int z = (int)(Math.sin(a) * r);

                IBlockState b = ring == 0 ? Blocks.REDSTONE_BLOCK.getDefaultState()
                        : ring == 1 ? Blocks.LAPIS_BLOCK.getDefaultState()
                        : Blocks.EMERALD_BLOCK.getDefaultState();
                world.setBlockState(center.add(x, y, z), b, 2);

                if (i % 4 == 0) {
                    for (int j = 1; j < r; j++) {
                        BlockPos lp = center.add((int)(Math.cos(a) * j), y, (int)(Math.sin(a) * j));
                        world.setBlockState(lp, Blocks.GLOWSTONE.getDefaultState(), 2);
                    }
                }
            }
        }

        for (int i = 0; i < 4; i++) {
            double a = (Math.PI * 2 * i) / 4;
            BlockPos p = center.add((int)(Math.cos(a) * 5), 8, (int)(Math.sin(a) * 5));
            world.setBlockState(p, Blocks.SEA_LANTERN.getDefaultState(), 2);
        }
    }

    private static void generateMiningOutpost(World world, BlockPos center) {
        for (int x = -6; x <= 6; x++)
            for (int z = -6; z <= 6; z++)
                world.setBlockState(center.add(x, 0, z), Blocks.IRON_BLOCK.getDefaultState(), 2);

        for (int x = -6; x <= 6; x += 4)
            for (int z = -6; z <= 6; z += 4)
                for (int y = -5; y < 0; y++)
                    world.setBlockState(center.add(x, y, z), Blocks.IRON_BARS.getDefaultState(), 2);

        world.setBlockState(center.add(0, 1, 0), Blocks.HOPPER.getDefaultState(), 2);
        world.setBlockState(center.add(0, 2, 0), Blocks.DROPPER.getDefaultState(), 2);

        for (int i = 0; i < 5; i++) {
            BlockPos orePos = center.add(random.nextInt(13) - 6, -3 - random.nextInt(3), random.nextInt(13) - 6);
            world.setBlockState(orePos, Blocks.GLOWSTONE.getDefaultState(), 2);
        }

        BlockPos chestPos = center.add(3, 1, 3);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
        TileEntityChest chest = (TileEntityChest) world.getTileEntity(chestPos);
        if (chest != null) addMiningLoot(chest);
    }

    private static void generateVoidFortress(World world, BlockPos center) {
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                for (int y = 0; y <= 8; y++) {
                    if (Math.abs(x) == 8 || Math.abs(z) == 8) {
                        if (y == 0 || y == 8 || (y % 3 == 0 && random.nextBoolean())) {
                            world.setBlockState(center.add(x, y, z), Blocks.OBSIDIAN.getDefaultState(), 2);
                        }
                    }
                }
            }
        }

        for (int x = -8; x <= 8; x += 16) {
            for (int z = -8; z <= 8; z += 16) {
                for (int y = 0; y <= 12; y++)
                    world.setBlockState(center.add(x, y, z), Blocks.NETHER_BRICK.getDefaultState(), 2);
                world.setBlockState(center.add(x, 13, z), Blocks.BEACON.getDefaultState(), 2);
            }
        }

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = 1; y <= 5; y++) {
                    if (Math.abs(x) == 3 || Math.abs(z) == 3 || y == 5) {
                        world.setBlockState(center.add(x, y, z), Blocks.PURPUR_BLOCK.getDefaultState(), 2);
                    }
                }
            }
        }

        world.setBlockState(center.add(0, 1, 0), Blocks.MOB_SPAWNER.getDefaultState(), 2);
        TileEntityMobSpawner spawner = (TileEntityMobSpawner) world.getTileEntity(center.add(0, 1, 0));
        if (spawner != null)
            spawner.getSpawnerBaseLogic().setEntityId(new ResourceLocation("minecraft:blaze"));

        world.setBlockState(center.add(0, 2, 0), Blocks.ENDER_CHEST.getDefaultState(), 2);
    }

    private static void generateCrystalGarden(World world, BlockPos center) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                double d = Math.sqrt(x*x + z*z);
                if (d <= 5) world.setBlockState(center.add(x, 0, z), Blocks.QUARTZ_BLOCK.getDefaultState(), 2);
            }
        }

        IBlockState[] crystals = {
                Blocks.STAINED_GLASS.getStateFromMeta(3),
                Blocks.STAINED_GLASS.getStateFromMeta(5),
                Blocks.STAINED_GLASS.getStateFromMeta(10),
                Blocks.STAINED_GLASS.getStateFromMeta(0),
                Blocks.SEA_LANTERN.getDefaultState()
        };

        for (int i = 0; i < 12; i++) {
            double a = (Math.PI * 2 * i) / 12;
            int r = 3 + random.nextInt(2);
            int x = (int)(Math.cos(a) * r);
            int z = (int)(Math.sin(a) * r);
            int h = 2 + random.nextInt(5);
            IBlockState c = crystals[random.nextInt(crystals.length)];
            for (int y = 1; y <= h; y++) world.setBlockState(center.add(x, y, z), c, 2);
            world.setBlockState(center.add(x, h + 1, z), Blocks.END_ROD.getDefaultState(), 2);
        }

        world.setBlockState(center.add(0, 1, 0), Blocks.WATER.getDefaultState(), 2);
        for (int y = 2; y <= 4; y++) world.setBlockState(center.add(0, y, 0), Blocks.GLASS.getDefaultState(), 2);
        world.setBlockState(center.add(0, 5, 0), Blocks.WATER.getDefaultState(), 2);
    }

    // ===================== 碎片化主题结构 =====================

    /** 下界碎片 */
    private static void generateNetherShard(World world, BlockPos center) {
        int rx = 5 + random.nextInt(4);
        int ry = 3 + random.nextInt(3);
        int rz = 5 + random.nextInt(4);

        for (int x = -rx; x <= rx; x++) {
            for (int y = -ry; y <= ry; y++) {
                for (int z = -rz; z <= rz; z++) {
                    double n = (x*x)/(double)(rx*rx) + (y*y)/(double)(ry*ry) + (z*z)/(double)(rz*rz);
                    if (n <= 1.0) {
                        BlockPos p = center.add(x, y, z);
                        IBlockState b;
                        double r = random.nextDouble();
                        if (r < 0.70) b = Blocks.NETHERRACK.getDefaultState();
                        else if (r < 0.85) b = Blocks.SOUL_SAND.getDefaultState();
                        else b = Blocks.MAGMA.getDefaultState();
                        world.setBlockState(p, b, 2);

                        if (y == ry && r < 0.25) world.setBlockState(p.up(), Blocks.FIRE.getDefaultState(), 2);
                    }
                }
            }
        }

        int spikes = 4 + random.nextInt(4);
        for (int i = 0; i < spikes; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            int r = 2 + random.nextInt(3);
            BlockPos base = center.add((int)(Math.cos(a)*r), ry - 1, (int)(Math.sin(a)*r));
            int h = 3 + random.nextInt(4);
            for (int y = 0; y < h; y++) world.setBlockState(base.up(y), Blocks.NETHER_BRICK.getDefaultState(), 2);
        }
    }

    /** 末地碎片 */
    private static void generateEndFragment(World world, BlockPos center) {
        int r = 6 + random.nextInt(3);

        for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x*x + z*z + (y*y*2) <= r*r) {
                        world.setBlockState(center.add(x, y, z), Blocks.END_STONE.getDefaultState(), 2);
                    }
                }
            }
        }

        for (int i = 0; i < 3; i++) {
            int dx = random.nextInt(r*2+1) - r;
            int dz = random.nextInt(r*2+1) - r;
            BlockPos base = center.add(dx, 1, dz);
            int h = 3 + random.nextInt(3);
            for (int y = 0; y < h; y++) world.setBlockState(base.up(y), Blocks.OBSIDIAN.getDefaultState(), 2);
            world.setBlockState(base.up(h), Blocks.END_ROD.getDefaultState(), 2);
        }

        for (int i = 0; i < 8; i++) {
            BlockPos p = center.add(random.nextInt(r*2+1)-r, 1, random.nextInt(r*2+1)-r);
            if (world.getBlockState(p.down()).getBlock() == Blocks.END_STONE) {
                world.setBlockState(p, Blocks.CHORUS_PLANT.getDefaultState(), 2);
                if (random.nextBoolean()) world.setBlockState(p.up(), Blocks.CHORUS_FLOWER.getDefaultState(), 2);
            }
        }
    }

    /** 废弃传送遗址簇 */
    private static void generateRuinedPortalCluster(World world, BlockPos center) {
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                if (Math.abs(x) + Math.abs(z) <= 8) {
                    IBlockState b = random.nextBoolean() ? Blocks.NETHERRACK.getDefaultState()
                            : Blocks.MAGMA.getDefaultState();
                    world.setBlockState(center.add(x, 0, z), b, 2);
                    if (random.nextDouble() < 0.12) world.setBlockState(center.add(x, 1, z), Blocks.FIRE.getDefaultState(), 2);
                }
            }
        }

        int portals = 1 + random.nextInt(3);
        for (int i = 0; i < portals; i++) {
            int ox = random.nextInt(9) - 4;
            int oz = random.nextInt(9) - 4;
            buildBrokenPortal(world, center.add(ox, 1, oz), 4 + random.nextInt(2), 5 + random.nextInt(2));
        }
    }

    /** 海晶碎片 */
    private static void generatePrismarineShard(World world, BlockPos center) {
        int r = 5;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    double d = Math.sqrt(x*x + y*y + z*z);
                    BlockPos p = center.add(x, y, z);
                    if (d >= r - 0.5 && d <= r + 0.5) world.setBlockState(p, Blocks.GLASS.getDefaultState(), 2);
                    else if (d < r - 1.0) world.setBlockState(p, Blocks.WATER.getDefaultState(), 2);
                }
            }
        }
        for (int x = -2; x <= 2; x++)
            for (int y = -2; y <= 2; y++)
                for (int z = -2; z <= 2; z++)
                    if (x*x + y*y + z*z <= 6)
                        world.setBlockState(center.add(x, y, z),
                                (random.nextDouble() < 0.75 ? Blocks.PRISMARINE : Blocks.SEA_LANTERN).getDefaultState(), 2);
    }

    /** 重力缎带 */
    private static void generateGravityRibbon(World world, BlockPos center) {
        int len = 24 + random.nextInt(16);
        double a = random.nextDouble() * Math.PI * 2;
        double ay = (random.nextDouble() - 0.5) * 0.2;

        for (int i = 0; i < len; i++) {
            double t = i / (double)len;
            int x = center.getX() + (int)(Math.cos(a + t * 2.5) * 6 * Math.sin(t * Math.PI));
            int y = center.getY() + (int)(Math.sin(t * Math.PI) * 4 + i * ay);
            int z = center.getZ() + (int)(Math.sin(a + t * 2.5) * 6 * Math.sin(t * Math.PI));

            BlockPos p = new BlockPos(x, y, z);
            world.setBlockState(p, (i % 3 == 0 ? Blocks.QUARTZ_BLOCK : Blocks.STAINED_GLASS).getDefaultState(), 2);
            if (i % 7 == 0) world.setBlockState(p.up(), Blocks.END_ROD.getDefaultState(), 2);
        }
    }

    /** 裂隙尖塔 */
    private static void generateRiftSpire(World world, BlockPos center) {
        int count = 5 + random.nextInt(4);
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            int r = 2 + random.nextInt(4);
            BlockPos base = center.add((int)(Math.cos(a)*r), 0, (int)(Math.sin(a)*r));
            int h = 6 + random.nextInt(8);
            for (int y = 0; y < h; y++) {
                IBlockState b;
                int mod = y % 3;
                if (mod == 0) b = Blocks.OBSIDIAN.getDefaultState();
                else if (mod == 1) b = Blocks.PURPUR_BLOCK.getDefaultState();
                else b = Blocks.QUARTZ_BLOCK.getDefaultState();
                world.setBlockState(base.up(y), b, 2);
            }
            world.setBlockState(base.up(h), Blocks.END_ROD.getDefaultState(), 2);
        }
    }

    /** 浮空原野块 */
    private static void generateOverworldChunk(World world, BlockPos center) {
        int rx = 6, rz = 6, ry = 3;

        for (int x = -rx; x <= rx; x++) {
            for (int z = -rz; z <= rz; z++) {
                for (int y = -ry; y <= 0; y++) {
                    double e = (x*x)/(double)(rx*rx) + (y*y*1.5)/(double)(ry*ry) + (z*z)/(double)(rz*rz);
                    if (e <= 1.0) {
                        BlockPos p = center.add(x, y, z);
                        if (y == 0) world.setBlockState(p, Blocks.GRASS.getDefaultState(), 2);
                        else world.setBlockState(p, Blocks.STONE.getDefaultState(), 2);
                        if (y < 0 && random.nextDouble() < 0.04) world.setBlockState(p, selectRandomOre(), 2);
                    }
                }
            }
        }

        if (random.nextBoolean()) {
            for (int x = -2; x <= 2; x++)
                for (int z = -2; z <= 2; z++)
                    world.setBlockState(center.add(x, 1, z), Blocks.AIR.getDefaultState(), 2);
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++)
                    world.setBlockState(center.add(x, 0, z), Blocks.WATER.getDefaultState(), 2);
        }

        generateSmallTree(world, center.up(1).add(random.nextInt(5)-2, 0, random.nextInt(5)-2));
        for (int i = 0; i < 6; i++) {
            BlockPos fp = center.add(random.nextInt(11)-5, 1, random.nextInt(11)-5);
            if (world.getBlockState(fp.down()).getBlock() == Blocks.GRASS)
                world.setBlockState(fp, selectRandomFlower(), 2);
        }
    }

    // ===================== 时空破碎主题结构（续） =====================

    /** 时间裂痕 */
    private static void generateTemporalFracture(World world, BlockPos center) {
        world.setBlockState(center, Blocks.REDSTONE_BLOCK.getDefaultState(), 2);

        IBlockState[] timeLayers = {
                Blocks.MOSSY_COBBLESTONE.getDefaultState(),
                Blocks.STONEBRICK.getDefaultState(),
                Blocks.COBBLESTONE.getDefaultState(),
                Blocks.BRICK_BLOCK.getDefaultState(),
                Blocks.IRON_BLOCK.getDefaultState(),
                Blocks.QUARTZ_BLOCK.getDefaultState(),
                Blocks.PURPUR_BLOCK.getDefaultState()
        };

        for (int ring = 0; ring < timeLayers.length; ring++) {
            int r = ring + 2;
            for (int i = 0; i < 20; i++) {
                double angle = (Math.PI * 2 * i) / 20;
                int x = (int)(Math.cos(angle) * r);
                int z = (int)(Math.sin(angle) * r);

                int height = 7 - ring;
                for (int y = -1; y <= height; y++) {
                    if (random.nextDouble() < 0.7) {
                        world.setBlockState(center.add(x, y, z), timeLayers[ring], 2);
                    }
                }

                if (i % 5 == 0) {
                    world.setBlockState(center.add(x, height + 1, z), Blocks.REDSTONE_LAMP.getDefaultState(), 2);
                }
            }
        }

        for (int y = 1; y <= 5; y++) {
            world.setBlockState(center.up(y), Blocks.BEACON.getDefaultState(), 2);
        }
    }

    /** 维度撕裂点 */
    private static void generateDimensionalTear(World world, BlockPos center) {
        for (int y = -2; y <= 2; y++) {
            world.setBlockState(center.up(y), Blocks.OBSIDIAN.getDefaultState(), 2);
        }

        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI * 2 * i) / 6;
            int x = (int)(Math.cos(angle) * 6);
            int z = (int)(Math.sin(angle) * 6);
            BlockPos fragmentPos = center.add(x, 0, z);

            switch (i) {
                case 0:
                    generateMiniOverworld(world, fragmentPos);
                    break;
                case 1:
                    generateMiniNether(world, fragmentPos);
                    break;
                case 2:
                    generateMiniEnd(world, fragmentPos);
                    break;
                case 3:
                    generateMiniOcean(world, fragmentPos);
                    break;
                case 4:
                    generateMiniSky(world, fragmentPos);
                    break;
                case 5:
                    generateMiniVoid(world, fragmentPos);
                    break;
            }

            for (int j = 1; j < 6; j++) {
                BlockPos linePos = center.add((x * j) / 6, 0, (z * j) / 6);
                world.setBlockState(linePos, Blocks.END_ROD.getDefaultState(), 2);
            }
        }
    }

    /** 虚空漩涡 */
    private static void generateVoidVortex(World world, BlockPos center) {
        int levels = 8;
        double spiralRadius = 8;

        for (int level = 0; level < levels; level++) {
            double angle = level * Math.PI / 2;
            double currentRadius = spiralRadius * (1 - level * 0.1);

            for (int i = 0; i < 4; i++) {
                double platformAngle = angle + (Math.PI / 2 * i);
                int x = (int)(Math.cos(platformAngle) * currentRadius);
                int z = (int)(Math.sin(platformAngle) * currentRadius);
                int y = -level * 3;

                BlockPos platformPos = center.add(x, y, z);

                for (int px = -2; px <= 2; px++) {
                    for (int pz = -2; pz <= 2; pz++) {
                        if (random.nextDouble() < 0.7) {
                            IBlockState block = level % 2 == 0 ?
                                    Blocks.OBSIDIAN.getDefaultState() :
                                    Blocks.PURPUR_BLOCK.getDefaultState();
                            world.setBlockState(platformPos.add(px, 0, pz), block, 2);
                        }
                    }
                }

                if (i == 0) {
                    for (int h = 1; h <= 3; h++) {
                        world.setBlockState(platformPos.up(h), Blocks.SEA_LANTERN.getDefaultState(), 2);
                    }
                }
            }
        }

        world.setBlockState(center.down(levels * 3), Blocks.BEACON.getDefaultState(), 2);
    }

    /** 破碎现实 */
    private static void generateShatteredReality(World world, BlockPos center) {
        int fragments = 3 + random.nextInt(3);

        for (int f = 0; f < fragments; f++) {
            int offsetX = random.nextInt(7) - 3;
            int offsetY = random.nextInt(7) - 3;
            int offsetZ = random.nextInt(7) - 3;
            BlockPos fragmentCenter = center.add(offsetX, offsetY, offsetZ);

            int size = 3 + random.nextInt(3);
            for (int x = -size; x <= size; x++) {
                for (int y = -size; y <= size; y++) {
                    for (int z = -size; z <= size; z++) {
                        boolean isEdge = Math.abs(x) == size || Math.abs(y) == size || Math.abs(z) == size;
                        boolean isCorner = (Math.abs(x) == size ? 1 : 0) +
                                (Math.abs(y) == size ? 1 : 0) +
                                (Math.abs(z) == size ? 1 : 0) >= 2;

                        if (isEdge && random.nextDouble() < (isCorner ? 0.9 : 0.4)) {
                            IBlockState block;
                            switch (f % 3) {
                                case 0: block = Blocks.IRON_BARS.getDefaultState(); break;
                                case 1: block = Blocks.GLASS.getDefaultState(); break;
                                default: block = Blocks.STAINED_GLASS.getStateFromMeta(f); break;
                            }
                            world.setBlockState(fragmentCenter.add(x, y, z), block, 2);
                        }
                    }
                }
            }

            world.setBlockState(fragmentCenter, Blocks.GLOWSTONE.getDefaultState(), 2);
        }
    }

    /** 冻结爆炸 */
    private static void generateFrozenExplosion(World world, BlockPos center) {
        world.setBlockState(center, Blocks.TNT.getDefaultState(), 2);

        int rays = 20 + random.nextInt(10);
        for (int i = 0; i < rays; i++) {
            double theta = random.nextDouble() * Math.PI * 2;
            double phi = random.nextDouble() * Math.PI;

            int length = 5 + random.nextInt(8);
            for (int l = 1; l <= length; l++) {
                int x = (int)(Math.sin(phi) * Math.cos(theta) * l);
                int y = (int)(Math.cos(phi) * l);
                int z = (int)(Math.sin(phi) * Math.sin(theta) * l);

                BlockPos rayPos = center.add(x, y, z);

                IBlockState fragment;
                if (l < 3) fragment = Blocks.OBSIDIAN.getDefaultState();
                else if (l < 6) fragment = Blocks.COBBLESTONE.getDefaultState();
                else fragment = Blocks.GRAVEL.getDefaultState();

                if (random.nextDouble() < (1.0 - l / (double)length)) {
                    world.setBlockState(rayPos, fragment, 2);
                }
            }
        }

        for (int r = 3; r <= 6; r++) {
            for (int i = 0; i < r * 6; i++) {
                double angle = (Math.PI * 2 * i) / (r * 6);
                int x = (int)(Math.cos(angle) * r);
                int z = (int)(Math.sin(angle) * r);

                if (random.nextDouble() < 0.3) {
                    world.setBlockState(center.add(x, 0, z), Blocks.GLASS.getDefaultState(), 2);
                }
            }
        }
    }

    /** 倒悬废墟 */
    private static void generateInvertedRuins(World world, BlockPos center) {
        int platformY = 8;
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                if (Math.abs(x) + Math.abs(z) <= 8) {
                    world.setBlockState(center.add(x, platformY, z), Blocks.STONE.getDefaultState(), 2);
                }
            }
        }

        for (int floor = 0; floor < 3; floor++) {
            int y = platformY - 1 - floor * 3;
            int size = 5 - floor;

            for (int x = -size; x <= size; x++) {
                for (int z = -size; z <= size; z++) {
                    if (Math.abs(x) == size || Math.abs(z) == size) {
                        world.setBlockState(center.add(x, y, z), Blocks.STONEBRICK.getDefaultState(), 2);
                    }

                    if ((Math.abs(x) == size && Math.abs(z) == size) ||
                            (x == 0 && z == 0 && floor == 0)) {
                        for (int h = 1; h <= 3; h++) {
                            world.setBlockState(center.add(x, y - h, z),
                                    random.nextBoolean() ? Blocks.COBBLESTONE.getDefaultState() :
                                            Blocks.MOSSY_COBBLESTONE.getDefaultState(), 2);
                        }
                    }
                }
            }

            if (floor == 2) {
                world.setBlockState(center.add(0, y - 4, 0), Blocks.GLOWSTONE.getDefaultState(), 2);
            }
        }

        for (int i = 0; i < 5; i++) {
            BlockPos particlePos = center.add(
                    random.nextInt(13) - 6,
                    random.nextInt(8),
                    random.nextInt(13) - 6
            );
            world.setBlockState(particlePos, Blocks.END_ROD.getDefaultState(), 2);
        }
    }

    /** 现实拼接 */
    private static void generateRealitySplice(World world, BlockPos center) {
        for (int sector = 0; sector < 6; sector++) {
            double startAngle = (Math.PI * 2 * sector) / 6;
            double endAngle = (Math.PI * 2 * (sector + 1)) / 6;

            for (int r = 1; r <= 6; r++) {
                for (double a = startAngle; a < endAngle; a += 0.1) {
                    int x = (int)(Math.cos(a) * r);
                    int z = (int)(Math.sin(a) * r);
                    BlockPos pos = center.add(x, 0, z);

                    switch (sector) {
                        case 0:
                            world.setBlockState(pos, Blocks.SAND.getDefaultState(), 2);
                            if (r == 3 && random.nextDouble() < 0.1) {
                                world.setBlockState(pos.up(), Blocks.CACTUS.getDefaultState(), 2);
                            }
                            break;
                        case 1:
                            world.setBlockState(pos, Blocks.PACKED_ICE.getDefaultState(), 2);
                            if (r == 4 && random.nextDouble() < 0.1) {
                                world.setBlockState(pos.up(), Blocks.SNOW.getDefaultState(), 2);
                            }
                            break;
                        case 2:
                            world.setBlockState(pos, Blocks.GRASS.getDefaultState(), 2);
                            if (r == 5 && random.nextDouble() < 0.1) {
                                generateSmallTree(world, pos.up());
                            }
                            break;
                        case 3:
                            world.setBlockState(pos, Blocks.MYCELIUM.getDefaultState(), 2);
                            if (r == 3 && random.nextDouble() < 0.1) {
                                world.setBlockState(pos.up(),
                                        random.nextBoolean() ? Blocks.RED_MUSHROOM.getDefaultState() :
                                                Blocks.BROWN_MUSHROOM.getDefaultState(), 2);
                            }
                            break;
                        case 4:
                            world.setBlockState(pos, Blocks.NETHERRACK.getDefaultState(), 2);
                            if (r == 2 && random.nextDouble() < 0.05) {
                                world.setBlockState(pos.up(), Blocks.FIRE.getDefaultState(), 2);
                            }
                            break;
                        case 5:
                            world.setBlockState(pos, Blocks.END_STONE.getDefaultState(), 2);
                            if (r == 6 && random.nextDouble() < 0.1) {
                                world.setBlockState(pos.up(), Blocks.CHORUS_PLANT.getDefaultState(), 2);
                            }
                            break;
                    }
                }
            }
        }

        world.setBlockState(center, Blocks.BEACON.getDefaultState(), 2);
        for (int y = 1; y <= 3; y++) {
            world.setBlockState(center.up(y), Blocks.GLASS.getDefaultState(), 2);
        }
    }

    /** 混沌枢纽 */
    private static void generateChaosNexus(World world, BlockPos center) {
        world.setBlockState(center, Blocks.DRAGON_EGG.getDefaultState(), 2);

        IBlockState[] chaosBlocks = {
                Blocks.OBSIDIAN.getDefaultState(),
                Blocks.GLOWSTONE.getDefaultState(),
                Blocks.REDSTONE_BLOCK.getDefaultState(),
                Blocks.LAPIS_BLOCK.getDefaultState(),
                Blocks.EMERALD_BLOCK.getDefaultState(),
                Blocks.QUARTZ_BLOCK.getDefaultState(),
                Blocks.PURPUR_BLOCK.getDefaultState(),
                Blocks.MAGMA.getDefaultState(),
                Blocks.SEA_LANTERN.getDefaultState(),
                Blocks.PACKED_ICE.getDefaultState()
        };

        for (int layer = 0; layer < 3; layer++) {
            int radius = 3 + layer * 2;
            int y = layer * 2;

            for (int i = 0; i < radius * 8; i++) {
                double angle = (Math.PI * 2 * i) / (radius * 8);
                int x = (int)(Math.cos(angle) * radius);
                int z = (int)(Math.sin(angle) * radius);

                IBlockState chaosBlock = chaosBlocks[random.nextInt(chaosBlocks.length)];
                world.setBlockState(center.add(x, y, z), chaosBlock, 2);

                if (i % (radius * 2) == 0) {
                    for (int h = -2; h <= 4; h++) {
                        if (random.nextDouble() < 0.6) {
                            world.setBlockState(center.add(x, h, z),
                                    chaosBlocks[random.nextInt(chaosBlocks.length)], 2);
                        }
                    }
                }
            }
        }

        for (int i = 0; i < 20; i++) {
            BlockPos particlePos = center.add(
                    random.nextInt(15) - 7,
                    random.nextInt(9) - 2,
                    random.nextInt(15) - 7
            );
            if (world.getBlockState(particlePos).getBlock() == Blocks.AIR) {
                world.setBlockState(particlePos, Blocks.END_ROD.getDefaultState(), 2);
            }
        }
    }

    /** 时间循环残片 */
    private static void generateTimeLoopFragment(World world, BlockPos center) {
        for (int loop = 0; loop < 4; loop++) {
            int offset = loop * 4;
            double scale = 1.0 - loop * 0.2;

            int size = (int)(4 * scale);
            for (int x = -size; x <= size; x++) {
                for (int z = -size; z <= size; z++) {
                    if (Math.abs(x) == size || Math.abs(z) == size) {
                        BlockPos pos = center.add(x, offset, z);

                        if (random.nextDouble() < (1.0 - loop * 0.25)) {
                            IBlockState block = loop == 0 ? Blocks.QUARTZ_BLOCK.getDefaultState() :
                                    loop == 1 ? Blocks.STONEBRICK.getDefaultState() :
                                            loop == 2 ? Blocks.COBBLESTONE.getDefaultState() :
                                                    Blocks.GRAVEL.getDefaultState();
                            world.setBlockState(pos, block, 2);
                        }
                    }
                }
            }

            world.setBlockState(center.add(0, offset, 0), Blocks.REDSTONE_LAMP.getDefaultState(), 2);

            if (loop < 3) {
                for (int y = 1; y < 4; y++) {
                    world.setBlockState(center.add(size, offset + y, size),
                            Blocks.IRON_BARS.getDefaultState(), 2);
                }
            }
        }
    }

    /** 虚空珊瑚 */
    private static void generateVoidCoral(World world, BlockPos center) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                world.setBlockState(center.add(x, 0, z), Blocks.PRISMARINE.getDefaultState(), 2);
            }
        }

        int trunkHeight = 5 + random.nextInt(4);
        for (int y = 1; y <= trunkHeight; y++) {
            world.setBlockState(center.up(y), Blocks.SEA_LANTERN.getDefaultState(), 2);
        }

        for (int branch = 0; branch < 6; branch++) {
            double angle = (Math.PI * 2 * branch) / 6;
            int branchHeight = trunkHeight - 2 + random.nextInt(3);

            for (int l = 1; l <= 4; l++) {
                int x = (int)(Math.cos(angle) * l);
                int z = (int)(Math.sin(angle) * l);
                int y = branchHeight + (l / 2);

                world.setBlockState(center.add(x, y, z),
                        l % 2 == 0 ? Blocks.PRISMARINE.getStateFromMeta(1) :
                                Blocks.PRISMARINE.getStateFromMeta(2), 2);

                if (l == 3) {
                    for (int sub = -1; sub <= 1; sub += 2) {
                        double subAngle = angle + (Math.PI / 6 * sub);
                        int sx = (int)(Math.cos(subAngle) * 2);
                        int sz = (int)(Math.sin(subAngle) * 2);
                        world.setBlockState(center.add(x + sx, y + 1, z + sz),
                                Blocks.SEA_LANTERN.getDefaultState(), 2);
                    }
                }
            }

            int tipX = (int)(Math.cos(angle) * 5);
            int tipZ = (int)(Math.sin(angle) * 5);
            world.setBlockState(center.add(tipX, branchHeight + 3, tipZ),
                    Blocks.END_ROD.getDefaultState(), 2);
        }

        for (int i = 0; i < 8; i++) {
            BlockPos particlePos = center.add(
                    random.nextInt(11) - 5,
                    trunkHeight + random.nextInt(5),
                    random.nextInt(11) - 5
            );
            world.setBlockState(particlePos, Blocks.GLOWSTONE.getDefaultState(), 2);
        }
    }

    /** 镜像碎片 */
    private static void generateMirrorShard(World world, BlockPos center) {
        for (int x = 0; x <= 6; x++) {
            for (int y = 0; y <= 6; y++) {
                for (int z = -6; z <= 6; z++) {
                    double placeProbability = 1.0 - (Math.abs(y - 3) * 0.15);

                    if (random.nextDouble() < placeProbability) {
                        if (x == 0 || x == 6 || Math.abs(z) == 6 || y == 0 || y == 6) {
                            IBlockState block = (x + y + Math.abs(z)) % 2 == 0 ?
                                    Blocks.GLASS.getDefaultState() :
                                    Blocks.STAINED_GLASS.getStateFromMeta(3);

                            world.setBlockState(center.add(x, y, z), block, 2);
                            world.setBlockState(center.add(-x, y, z), block, 2);
                        }
                    }
                }
            }
        }

        for (int y = 1; y <= 5; y++) {
            for (int z = -3; z <= 3; z++) {
                world.setBlockState(center.add(0, y, z),
                        Blocks.STAINED_GLASS.getStateFromMeta(0), 2);
            }
        }

        for (int i = 0; i < 10; i++) {
            BlockPos shardPos = center.add(
                    random.nextInt(13) - 6,
                    random.nextInt(7),
                    random.nextInt(13) - 6
            );
            world.setBlockState(shardPos, Blocks.AIR.getDefaultState(), 2);
        }
    }

    /** 量子脚手架 */
    private static void generateQuantumScaffold(World world, BlockPos center) {
        int gridSize = 8;
        int spacing = 3;

        for (int x = -gridSize; x <= gridSize; x += spacing) {
            for (int y = -2; y <= 6; y += spacing) {
                for (int z = -gridSize; z <= gridSize; z += spacing) {
                    BlockPos nodePos = center.add(x, y, z);

                    boolean isMainNode = x == 0 && y == 1 && z == 0;
                    if (isMainNode) {
                        world.setBlockState(nodePos, Blocks.BEACON.getDefaultState(), 2);
                    } else if ((x + y + z) % 2 == 0) {
                        world.setBlockState(nodePos, Blocks.SEA_LANTERN.getDefaultState(), 2);
                    } else {
                        world.setBlockState(nodePos, Blocks.REDSTONE_LAMP.getDefaultState(), 2);
                    }

                    if (x < gridSize && random.nextDouble() < 0.7) {
                        for (int i = 1; i < spacing; i++) {
                            world.setBlockState(nodePos.add(i, 0, 0),
                                    Blocks.END_ROD.getDefaultState(), 2);
                        }
                    }
                    if (z < gridSize && random.nextDouble() < 0.7) {
                        for (int i = 1; i < spacing; i++) {
                            world.setBlockState(nodePos.add(0, 0, i),
                                    Blocks.END_ROD.getDefaultState(), 2);
                        }
                    }
                    if (y < 4 && random.nextDouble() < 0.5) {
                        for (int i = 1; i < spacing; i++) {
                            world.setBlockState(nodePos.add(0, i, 0),
                                    Blocks.IRON_BARS.getDefaultState(), 2);
                        }
                    }
                }
            }
        }

        for (int i = 0; i < 15; i++) {
            BlockPos holePos = center.add(
                    random.nextInt(17) - 8,
                    random.nextInt(9) - 2,
                    random.nextInt(17) - 8
            );

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (random.nextDouble() < 0.6) {
                            world.setBlockState(holePos.add(dx, dy, dz),
                                    Blocks.AIR.getDefaultState(), 2);
                        }
                    }
                }
            }
        }
    }
// 将这些方法添加到 VoidStructureGenerator 类的末尾，在最后一个 } 之前

// ===================== 核心辅助方法 =====================

/** 随机选择结构类型 */
// 将这些方法添加到 VoidStructureGenerator 类的末尾，在最后一个 } 之前

    // ===================== 核心辅助方法 =====================

    /** 随机选择结构类型 */
    public static StructureType selectRandomStructure() {
        double total = 0.0;
        for (StructureType t : StructureType.values()) {
            total += t.chance;
        }
        double roll = random.nextDouble() * total;
        double acc = 0.0;
        for (StructureType t : StructureType.values()) {
            acc += t.chance;
            if (roll <= acc) return t;
        }
        return StructureType.FLOATING_ISLAND; // 兜底
    }

    /** 选择随机矿石 */
    private static IBlockState selectRandomOre() {
        double r = random.nextDouble();
        if (r < 0.30) return Blocks.COAL_ORE.getDefaultState();
        if (r < 0.55) return Blocks.IRON_ORE.getDefaultState();
        if (r < 0.70) return Blocks.GOLD_ORE.getDefaultState();
        if (r < 0.85) return Blocks.REDSTONE_ORE.getDefaultState();
        if (r < 0.95) return Blocks.LAPIS_ORE.getDefaultState();
        return Blocks.DIAMOND_ORE.getDefaultState();
    }

    /** 生成小树 */
    private static void generateSmallTree(World world, BlockPos pos) {
        // 树干
        for (int y = 0; y < 4; y++) {
            world.setBlockState(pos.up(y), Blocks.LOG.getDefaultState(), 2);
        }
        // 树叶
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 3; y <= 5; y++) {
                    if (Math.abs(x) + Math.abs(z) <= 3 - (y - 3)) {
                        BlockPos leaf = pos.add(x, y, z);
                        if (world.getBlockState(leaf).getBlock() == Blocks.AIR) {
                            world.setBlockState(leaf, Blocks.LEAVES.getDefaultState(), 2);
                        }
                    }
                }
            }
        }
    }

    /** 添加随机战利品到箱子 */
    private static void addRandomLoot(TileEntityChest chest) {
        for (int i = 0; i < 3 + random.nextInt(5); i++) {
            ItemStack loot = selectRandomLoot();
            chest.setInventorySlotContents(i, loot);
        }
    }

    /** 选择随机战利品 */
    private static ItemStack selectRandomLoot() {
        ItemStack[] loot = {
                new ItemStack(Items.DIAMOND, 1 + random.nextInt(3)),
                new ItemStack(Items.EMERALD, 2 + random.nextInt(5)),
                new ItemStack(Items.GOLDEN_APPLE, 1),
                new ItemStack(Items.ENDER_PEARL, 4 + random.nextInt(12)),
                new ItemStack(Items.EXPERIENCE_BOTTLE, 8 + random.nextInt(16)),
                new ItemStack(Items.ENCHANTED_BOOK, 1),
                new ItemStack(Items.TOTEM_OF_UNDYING, 1),
                new ItemStack(Blocks.BEACON, 1),
                new ItemStack(Items.ELYTRA, 1)
        };
        return loot[random.nextInt(loot.length)];
    }

    /** 添加采矿战利品 */
    private static void addMiningLoot(TileEntityChest chest) {
        ItemStack[] loot = {
                new ItemStack(Items.DIAMOND_PICKAXE, 1),
                new ItemStack(Items.IRON_INGOT, 16 + random.nextInt(16)),
                new ItemStack(Items.GOLD_INGOT, 8 + random.nextInt(8)),
                new ItemStack(Items.REDSTONE, 32 + random.nextInt(32)),
                new ItemStack(Blocks.TNT, 4 + random.nextInt(4)),
                new ItemStack(Items.ENDER_PEARL, 2 + random.nextInt(4)),
                new ItemStack(Items.NETHER_STAR, 1)
        };
        for (int i = 0; i < 3 + random.nextInt(3); i++) {
            chest.setInventorySlotContents(i, loot[random.nextInt(loot.length)]);
        }
    }

    /** 选择随机花朵 */
    private static IBlockState selectRandomFlower() {
        IBlockState[] flowers = {
                Blocks.YELLOW_FLOWER.getDefaultState(),
                Blocks.RED_FLOWER.getDefaultState()
        };
        return flowers[random.nextInt(flowers.length)];
    }

    /** 生成小平台 */
    private static void generateSmallPlatform(World world, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (Math.abs(x) + Math.abs(z) <= radius) {
                    world.setBlockState(center.add(x, 0, z), Blocks.STONEBRICK.getDefaultState(), 2);
                }
            }
        }
    }

    /** 建造破损传送门 */
    private static void buildBrokenPortal(World world, BlockPos base, int innerW, int innerH) {
        int w = innerW + 2;
        int h = innerH + 2;
        // 门框
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean frame = (x == 0 || x == w-1 || y == 0 || y == h-1);
                BlockPos p = base.add(x, y, 0);
                if (frame && random.nextDouble() > 0.25) {
                    world.setBlockState(p, Blocks.OBSIDIAN.getDefaultState(), 2);
                } else {
                    world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                }
            }
        }
        // 地面装饰
        for (int dx = -1; dx <= w; dx++) {
            if (random.nextBoolean()) {
                world.setBlockState(base.add(dx, -1, 0), Blocks.NETHER_BRICK.getDefaultState(), 2);
            }
            if (random.nextDouble() < 0.3) {
                world.setBlockState(base.add(dx, 0, 0), Blocks.FIRE.getDefaultState(), 2);
            }
        }
    }

    // ===================== 迷你维度生成方法 =====================

    /** 生成迷你主世界 */
    private static void generateMiniOverworld(World world, BlockPos pos) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                world.setBlockState(pos.add(x, 0, z), Blocks.GRASS.getDefaultState(), 2);
                if (x == 0 && z == 0) {
                    generateSmallTree(world, pos.up());
                }
            }
        }
    }

    /** 生成迷你下界 */
    private static void generateMiniNether(World world, BlockPos pos) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                world.setBlockState(pos.add(x, 0, z), Blocks.NETHERRACK.getDefaultState(), 2);
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    if (random.nextDouble() < 0.3) {
                        world.setBlockState(pos.add(x, 1, z), Blocks.FIRE.getDefaultState(), 2);
                    }
                }
            }
        }
        world.setBlockState(pos, Blocks.MAGMA.getDefaultState(), 2);
    }

    /** 生成迷你末地 */
    private static void generateMiniEnd(World world, BlockPos pos) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                world.setBlockState(pos.add(x, 0, z), Blocks.END_STONE.getDefaultState(), 2);
            }
        }
        world.setBlockState(pos.up(), Blocks.CHORUS_PLANT.getDefaultState(), 2);
        world.setBlockState(pos.up(2), Blocks.CHORUS_FLOWER.getDefaultState(), 2);
    }

    /** 生成迷你海洋 */
    private static void generateMiniOcean(World world, BlockPos pos) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                world.setBlockState(pos.add(x, 0, z), Blocks.PRISMARINE.getDefaultState(), 2);
                if (x >= -1 && x <= 1 && z >= -1 && z <= 1) {
                    world.setBlockState(pos.add(x, 1, z), Blocks.WATER.getDefaultState(), 2);
                }
            }
        }
    }

    /** 生成迷你天空 */
    private static void generateMiniSky(World world, BlockPos pos) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) + Math.abs(z) <= 3) {
                    world.setBlockState(pos.add(x, 0, z), Blocks.GLASS.getDefaultState(), 2);
                }
            }
        }
        world.setBlockState(pos.up(), Blocks.GLOWSTONE.getDefaultState(), 2);
    }

    /** 生成迷你虚空 */
    private static void generateMiniVoid(World world, BlockPos pos) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    world.setBlockState(pos.add(x, 0, z), Blocks.OBSIDIAN.getDefaultState(), 2);
                }
            }
        }
        world.setBlockState(pos, Blocks.BEACON.getDefaultState(), 2);
    }}