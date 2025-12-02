package com.moremod.dungeon.schematic;

import com.moremod.schematic.Schematic;
import com.moremod.init.ModBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import javax.swing.text.html.parser.Entity;
import java.util.Random;

/**
 * 增强版房间模板
 * 包含丰富的装饰、多样化的变种、道中Boss房间
 */
public class EnhancedRoomTemplates {

    private static final Random rand = new Random();

    // 标准房间尺寸
    private static final int STANDARD_SIZE = 26;
    private static final int STANDARD_HEIGHT = 8;

    // Boss房间尺寸
    private static final int BOSS_ROOM_SIZE = 36;
    private static final int BOSS_ROOM_HEIGHT = 16;

    // Mini-Boss房间尺寸 (中等大小)
    private static final int MINI_BOSS_SIZE = 32;
    private static final int MINI_BOSS_HEIGHT = 12;

    // 刷怪箱实体列表
    private static final String[] SPAWNER_ENTITIES = {
            "moremod:curse_knight",
            "moremod:weeping_angel"
    };

    // ==================== BOSS 房间 ====================

    public static Schematic bossArena() {
        Schematic s = new Schematic((short) BOSS_ROOM_SIZE, (short) BOSS_ROOM_HEIGHT, (short) BOSS_ROOM_SIZE);

        IBlockState floorBlock = ModBlocks.UNBREAKABLE_BARRIER_ANCHOR.getDefaultState();
        IBlockState decorFloor = Blocks.STONE.getStateFromMeta(6); // 安山岩

        // 棋盘格地板
        for (int x = 0; x < BOSS_ROOM_SIZE; x++) {
            for (int z = 0; z < BOSS_ROOM_SIZE; z++) {
                boolean checker = ((x / 3) + (z / 3)) % 2 == 0;
                s.setBlockState(x, 0, z, checker ? floorBlock : decorFloor);
                s.setBlockState(x, BOSS_ROOM_HEIGHT - 1, z, floorBlock);
            }
        }

        // 四角巨型柱
        createPillar(s, 8, 8);
        createPillar(s, 8, 27);
        createPillar(s, 27, 8);
        createPillar(s, 27, 27);

        // 中央祭坛
        createBossAltar(s, BOSS_ROOM_SIZE / 2, 1, BOSS_ROOM_SIZE / 2);

        // 战利品箱
        placeLootChest(s, BOSS_ROOM_SIZE / 2 - 5, 1, BOSS_ROOM_SIZE / 2, "moremod:dungeon/dungeon_boss");
        placeLootChest(s, BOSS_ROOM_SIZE / 2 + 5, 1, BOSS_ROOM_SIZE / 2, "moremod:dungeon/dungeon_boss");

        // 墙壁装饰
        addWallDecoration(s, BOSS_ROOM_SIZE, BOSS_ROOM_HEIGHT);

        return s;
    }

    // ==================== MINI-BOSS 房间 (道中Boss) ====================

    /**
     * 道中Boss房间 - 召唤两只血量较少的 VoidRipper
     * 刷怪砖会在召唤后自毁
     */
    public static Schematic miniBossArena() {
        Schematic s = new Schematic((short) MINI_BOSS_SIZE, (short) MINI_BOSS_HEIGHT, (short) MINI_BOSS_SIZE);

        IBlockState floorMain = Blocks.STONEBRICK.getStateFromMeta(0);
        IBlockState floorAccent = Blocks.STONEBRICK.getStateFromMeta(3); // 錾制石砖
        IBlockState crackedBrick = Blocks.STONEBRICK.getStateFromMeta(2); // 裂石砖

        // 圆形竞技场地板
        int center = MINI_BOSS_SIZE / 2;
        for (int x = 0; x < MINI_BOSS_SIZE; x++) {
            for (int z = 0; z < MINI_BOSS_SIZE; z++) {
                double dist = Math.sqrt(Math.pow(x - center, 2) + Math.pow(z - center, 2));

                if (dist <= 14) {
                    // 同心圆图案
                    if ((int) dist % 4 == 0) {
                        s.setBlockState(x, 0, z, floorAccent);
                    } else if (rand.nextFloat() < 0.1) {
                        s.setBlockState(x, 0, z, crackedBrick);
                    } else {
                        s.setBlockState(x, 0, z, floorMain);
                    }
                } else {
                    s.setBlockState(x, 0, z, Blocks.STONE.getDefaultState());
                }

                // 天花板
                s.setBlockState(x, MINI_BOSS_HEIGHT - 1, z, Blocks.STONEBRICK.getDefaultState());
            }
        }

        // 四根装饰柱 (较小)
        createSmallPillar(s, 8, 8, MINI_BOSS_HEIGHT);
        createSmallPillar(s, 8, 23, MINI_BOSS_HEIGHT);
        createSmallPillar(s, 23, 8, MINI_BOSS_HEIGHT);
        createSmallPillar(s, 23, 23, MINI_BOSS_HEIGHT);

        // 中央平台
        createMiniBossPlatform(s, center, center);

        // 放置自毁刷怪砖 - 两个位置，各生成一只 VoidRipper
        placeVoidRipperSpawner(s, center - 4, 1, center);
        placeVoidRipperSpawner(s, center + 4, 1, center);

        // 战利品箱 (Mini-Boss奖励)
        placeLootChest(s, center, 1, center + 6, "moremod:dungeon/dungeon_treasure");

        // 火把照明
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            int tx = center + (int) (10 * Math.cos(angle));
            int tz = center + (int) (10 * Math.sin(angle));
            s.setBlockState(tx, 1, tz, Blocks.TORCH.getDefaultState());
        }

        return s;
    }

    /**
     * 道中Boss房间变种2 - 黑暗竞技场
     */
    public static Schematic miniBossArenaDark() {
        Schematic s = new Schematic((short) MINI_BOSS_SIZE, (short) MINI_BOSS_HEIGHT, (short) MINI_BOSS_SIZE);

        IBlockState floorMain = Blocks.NETHER_BRICK.getDefaultState();
        IBlockState floorAccent = Blocks.MAGMA.getDefaultState();

        int center = MINI_BOSS_SIZE / 2;

        // 地板 - 岩浆裂痕图案
        for (int x = 0; x < MINI_BOSS_SIZE; x++) {
            for (int z = 0; z < MINI_BOSS_SIZE; z++) {
                double dist = Math.sqrt(Math.pow(x - center, 2) + Math.pow(z - center, 2));

                if (dist <= 13) {
                    // 随机岩浆裂痕
                    if (rand.nextFloat() < 0.08 && dist > 3) {
                        s.setBlockState(x, 0, z, floorAccent);
                    } else {
                        s.setBlockState(x, 0, z, floorMain);
                    }
                } else {
                    s.setBlockState(x, 0, z, Blocks.OBSIDIAN.getDefaultState());
                }

                s.setBlockState(x, MINI_BOSS_HEIGHT - 1, z, Blocks.NETHER_BRICK.getDefaultState());
            }
        }

        // 四角火焰柱
        createNetherPillar(s, 7, 7, MINI_BOSS_HEIGHT);
        createNetherPillar(s, 7, 24, MINI_BOSS_HEIGHT);
        createNetherPillar(s, 24, 7, MINI_BOSS_HEIGHT);
        createNetherPillar(s, 24, 24, MINI_BOSS_HEIGHT);

        // VoidRipper 刷怪砖
        placeVoidRipperSpawner(s, center - 5, 1, center - 3);
        placeVoidRipperSpawner(s, center + 5, 1, center + 3);

        // 战利品
        placeLootChest(s, center, 1, center, "moremod:dungeon/dungeon_treasure");

        return s;
    }

    // ==================== 战斗房间 ====================

    public static Schematic combatRoom() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.STONEBRICK.getDefaultState();
        IBlockState crackedFloor = Blocks.STONEBRICK.getStateFromMeta(2);

        // 地板 - 带战斗痕迹
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                if (rand.nextFloat() < 0.15) {
                    s.setBlockState(x, 0, z, crackedFloor);
                } else {
                    s.setBlockState(x, 0, z, floor);
                }
            }
        }

        // 战斗障碍物
        createCombatObstacles(s);

        // 刷怪笼 - 四角 + 中央
        placeRandomSpawner(s, 5, 1, 5, 1);
        placeRandomSpawner(s, 20, 1, 5, 1);
        placeRandomSpawner(s, 13, 1, 13, 2);
        placeRandomSpawner(s, 5, 1, 20, 1);
        placeRandomSpawner(s, 20, 1, 20, 1);

        // 战利品
        placeLootChest(s, 13, 1, 20, "moremod:dungeon/dungeon_normal");

        // 角落火把
        s.setBlockState(2, 1, 2, Blocks.TORCH.getDefaultState());
        s.setBlockState(23, 1, 2, Blocks.TORCH.getDefaultState());
        s.setBlockState(2, 1, 23, Blocks.TORCH.getDefaultState());
        s.setBlockState(23, 1, 23, Blocks.TORCH.getDefaultState());

        return s;
    }

    /**
     * 战斗房间变种 - 训练场
     */
    public static Schematic combatRoomTrainingGround() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        // 木质地板 (训练场风格)
        IBlockState woodFloor = Blocks.PLANKS.getStateFromMeta(1); // 云杉木板
        IBlockState logBorder = Blocks.LOG.getStateFromMeta(1);    // 云杉原木

        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                boolean isBorder = x < 2 || x >= 24 || z < 2 || z >= 24;
                s.setBlockState(x, 0, z, isBorder ? logBorder : woodFloor);
            }
        }

        // 训练假人 (干草块)
        createTrainingDummy(s, 8, 1, 13);
        createTrainingDummy(s, 17, 1, 13);

        // 掩体
        createWoodenBarricade(s, 13, 1, 8);
        createWoodenBarricade(s, 13, 1, 18);

        // 刷怪笼 (较少)
        placeRandomSpawner(s, 13, 1, 4, 1);
        placeRandomSpawner(s, 13, 1, 21, 1);

        // 战利品
        placeLootChest(s, 13, 1, 13, "moremod:dungeon/dungeon_normal");

        // 火把
        s.setBlockState(4, 2, 4, Blocks.TORCH.getDefaultState());
        s.setBlockState(21, 2, 4, Blocks.TORCH.getDefaultState());
        s.setBlockState(4, 2, 21, Blocks.TORCH.getDefaultState());
        s.setBlockState(21, 2, 21, Blocks.TORCH.getDefaultState());

        return s;
    }

    /**
     * 战斗房间变种 - 角斗场
     */
    public static Schematic combatRoomColosseum() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        int center = STANDARD_SIZE / 2;

        // 圆形沙地
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                double dist = Math.sqrt(Math.pow(x - center, 2) + Math.pow(z - center, 2));

                if (dist <= 10) {
                    s.setBlockState(x, 0, z, Blocks.SAND.getDefaultState());
                } else if (dist <= 12) {
                    // 阶梯座位
                    s.setBlockState(x, 0, z, Blocks.STONE_BRICK_STAIRS.getDefaultState());
                    s.setBlockState(x, 1, z, Blocks.STONEBRICK.getDefaultState());
                } else {
                    s.setBlockState(x, 0, z, Blocks.STONEBRICK.getDefaultState());
                    s.setBlockState(x, 1, z, Blocks.STONEBRICK.getDefaultState());
                    s.setBlockState(x, 2, z, Blocks.STONEBRICK.getDefaultState());
                }
            }
        }

        // 中央柱
        for (int y = 1; y <= 4; y++) {
            s.setBlockState(center, y, center, Blocks.IRON_BLOCK.getDefaultState());
        }

        // 刷怪笼 (环绕)
        placeRandomSpawner(s, center - 6, 1, center, 2);
        placeRandomSpawner(s, center + 6, 1, center, 2);
        placeRandomSpawner(s, center, 1, center - 6, 2);
        placeRandomSpawner(s, center, 1, center + 6, 2);

        // 战利品
        placeLootChest(s, center - 3, 1, center - 3, "moremod:dungeon/dungeon_normal");
        placeLootChest(s, center + 3, 1, center + 3, "moremod:dungeon/dungeon_normal");

        return s;
    }

    // ==================== 迷宫房间 ====================

    public static Schematic mazeRoom() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.STONE.getDefaultState();
        IBlockState wall = Blocks.STONEBRICK.getDefaultState();
        IBlockState unbreakableWall = ModBlocks.UNBREAKABLE_BARRIER_ANCHOR.getDefaultState();

        // 地板和天花板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, floor);
                s.setBlockState(x, 7, z, floor);
            }
        }

        // 生成迷宫
        boolean[][] maze = generateMaze(STANDARD_SIZE, STANDARD_SIZE);
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                if (maze[x][z]) {
                    for (int y = 1; y <= 4; y++) {
                        boolean isEdge = (x == 0 || x == 25 || z == 0 || z == 25);
                        s.setBlockState(x, y, z, isEdge ? unbreakableWall : wall);
                    }
                }
            }
        }

        // 迷宫内刷怪笼
        placeSpawnersInMaze(s, maze);

        // 中央宝箱
        placeLootChest(s, 13, 1, 13, "moremod:dungeon/dungeon_hub");

        return s;
    }

    /**
     * 迷宫变种 - 花园迷宫
     */
    public static Schematic mazeRoomGarden() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        // 草地地板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, Blocks.GRASS.getDefaultState());
            }
        }

        // 树篱迷宫
        boolean[][] maze = generateMaze(STANDARD_SIZE, STANDARD_SIZE);
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                if (maze[x][z]) {
                    for (int y = 1; y <= 3; y++) {
                        s.setBlockState(x, y, z, Blocks.LEAVES.getDefaultState());
                    }
                }
            }
        }

        // 花朵装饰
        for (int i = 0; i < 15; i++) {
            int fx = 2 + rand.nextInt(22);
            int fz = 2 + rand.nextInt(22);
            if (!maze[fx][fz]) {
                s.setBlockState(fx, 1, fz, rand.nextBoolean() ?
                        Blocks.RED_FLOWER.getDefaultState() :
                        Blocks.YELLOW_FLOWER.getDefaultState());
            }
        }

        // 刷怪笼 (隐藏在树篱中)
        placeRandomSpawner(s, 6, 0, 6, 1);
        placeRandomSpawner(s, 19, 0, 19, 1);

        placeLootChest(s, 13, 1, 13, "moremod:dungeon/dungeon_hub");

        return s;
    }

    // ==================== 陷阱房间 ====================

    public static Schematic trapRoom() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.STONEBRICK.getDefaultState();
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, floor);
            }
        }

        // 陷阱走廊
        createTrapCorridor(s, 8, 18, 13, true);
        createTrapCorridor(s, 13, 13, 8, false);

        // 隐藏刷怪房
        createHiddenSpawnerRoom(s, 3, 3);
        createHiddenSpawnerRoom(s, 20, 3);
        createHiddenSpawnerRoom(s, 3, 20);
        createHiddenSpawnerRoom(s, 20, 20);

        // 中央安全区 (荧石)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                s.setBlockState(13 + dx, 0, 13 + dz, Blocks.GLOWSTONE.getDefaultState());
            }
        }

        placeLootChest(s, 13, 1, 13, "moremod:dungeon/dungeon_trap");

        return s;
    }

    /**
     * 陷阱房间变种 - 箭矢走廊
     */
    public static Schematic trapRoomArrowCorridor() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.STONEBRICK.getDefaultState();
        IBlockState crackedFloor = Blocks.STONEBRICK.getStateFromMeta(2);

        // 地板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, rand.nextFloat() < 0.2 ? crackedFloor : floor);
            }
        }

        // 走廊墙壁 + 发射器
        for (int z = 4; z <= 21; z++) {
            // 左墙
            for (int y = 1; y <= 3; y++) {
                s.setBlockState(4, y, z, Blocks.STONEBRICK.getDefaultState());
            }
            // 右墙
            for (int y = 1; y <= 3; y++) {
                s.setBlockState(21, y, z, Blocks.STONEBRICK.getDefaultState());
            }

            // 发射器 (交替)
            if (z % 3 == 0) {
                s.setBlockState(4, 2, z, Blocks.DISPENSER.getDefaultState());
                s.setBlockState(21, 2, z, Blocks.DISPENSER.getDefaultState());
            }
        }

        // 压力板触发
        for (int z = 5; z <= 20; z += 2) {
            s.setBlockState(13, 1, z, Blocks.STONE_PRESSURE_PLATE.getDefaultState());
        }

        // 两侧刷怪笼
        placeRandomSpawner(s, 2, 1, 8, 1);
        placeRandomSpawner(s, 23, 1, 8, 1);
        placeRandomSpawner(s, 2, 1, 17, 1);
        placeRandomSpawner(s, 23, 1, 17, 1);

        // 终点奖励
        placeLootChest(s, 13, 1, 22, "moremod:dungeon/dungeon_trap");

        return s;
    }

    /**
     * 陷阱房间变种 - 落穴陷阱
     */
    public static Schematic trapRoomPitfall() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.STONEBRICK.getDefaultState();
        IBlockState mossyFloor = Blocks.STONEBRICK.getStateFromMeta(1);

        // 地板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, rand.nextFloat() < 0.3 ? mossyFloor : floor);
            }
        }

        // 创建落穴区域
        int[][] pitAreas = {{6, 6}, {6, 18}, {18, 6}, {18, 18}};
        for (int[] pit : pitAreas) {
            int px = pit[0], pz = pit[1];
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    // 假地板 (蜘蛛网)
                    s.setBlockState(px + dx, 0, pz + dz, Blocks.WEB.getDefaultState());
                }
            }
            // 落穴底部刷怪笼
            placeRandomSpawner(s, px, 1, pz, 1);
        }

        // 安全路径标记 (红石火把)
        s.setBlockState(13, 1, 4, Blocks.REDSTONE_TORCH.getDefaultState());
        s.setBlockState(13, 1, 21, Blocks.REDSTONE_TORCH.getDefaultState());
        s.setBlockState(4, 1, 13, Blocks.REDSTONE_TORCH.getDefaultState());
        s.setBlockState(21, 1, 13, Blocks.REDSTONE_TORCH.getDefaultState());

        // 中央奖励
        placeLootChest(s, 13, 1, 13, "moremod:dungeon/dungeon_trap");

        return s;
    }

    // ==================== 宝藏房间 ====================

    public static Schematic treasureRoom() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        // 豪华地板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                boolean edge = (x < 2 || x >= 24 || z < 2 || z >= 24);
                s.setBlockState(x, 0, z, edge ?
                        Blocks.GOLD_BLOCK.getDefaultState() :
                        Blocks.STONE.getDefaultState());
            }
        }

        // 中央展示台
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    s.setBlockState(13 + dx, 1, 13 + dz, Blocks.QUARTZ_BLOCK.getDefaultState());
                }
            }
        }

        // 三个宝箱
        placeLootChest(s, 13, 2, 13, "moremod:dungeon/dungeon_treasure");
        placeLootChest(s, 11, 1, 13, "moremod:dungeon/dungeon_treasure");
        placeLootChest(s, 15, 1, 13, "moremod:dungeon/dungeon_treasure");

        // 守卫刷怪笼
        placeRandomSpawner(s, 13, 0, 5, 2);
        placeRandomSpawner(s, 13, 0, 20, 2);

        // 金苹果装饰柱
        createGoldPillar(s, 5, 5);
        createGoldPillar(s, 20, 5);
        createGoldPillar(s, 5, 20);
        createGoldPillar(s, 20, 20);

        return s;
    }

    /**
     * 宝藏房间变种 - 地下金库
     */
    public static Schematic treasureRoomVault() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState ironFloor = Blocks.IRON_BLOCK.getDefaultState();
        IBlockState stoneFloor = Blocks.STONE.getDefaultState();

        // 铁块 + 石头交替地板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                boolean ironTile = (x + z) % 4 < 2;
                s.setBlockState(x, 0, z, ironTile ? ironFloor : stoneFloor);
            }
        }

        // 多排保险箱 (铁块 + 箱子)
        for (int row = 0; row < 3; row++) {
            int rz = 6 + row * 6;
            for (int col = 0; col < 4; col++) {
                int cx = 5 + col * 5;
                // 铁块底座
                s.setBlockState(cx, 1, rz, Blocks.IRON_BLOCK.getDefaultState());
                // 宝箱
                placeLootChest(s, cx, 2, rz, "moremod:dungeon/dungeon_treasure");
            }
        }

        // 守卫
        placeRandomSpawner(s, 3, 1, 13, 2);
        placeRandomSpawner(s, 22, 1, 13, 2);

        // 红石灯照明
        s.setBlockState(4, 1, 4, Blocks.REDSTONE_LAMP.getDefaultState());
        s.setBlockState(21, 1, 4, Blocks.REDSTONE_LAMP.getDefaultState());
        s.setBlockState(4, 1, 21, Blocks.REDSTONE_LAMP.getDefaultState());
        s.setBlockState(21, 1, 21, Blocks.REDSTONE_LAMP.getDefaultState());

        return s;
    }

    /**
     * 宝藏房间变种 - 皇家宝库
     */
    public static Schematic treasureRoomRoyal() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        // 红地毯 + 石砖
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, Blocks.STONEBRICK.getDefaultState());
            }
        }

        // 红地毯走道
        for (int z = 2; z < 24; z++) {
            s.setBlockState(12, 1, z, Blocks.CARPET.getStateFromMeta(14)); // 红色
            s.setBlockState(13, 1, z, Blocks.CARPET.getStateFromMeta(14));
        }

        // 王座 (末端)
        s.setBlockState(13, 1, 21, Blocks.QUARTZ_STAIRS.getDefaultState());
        s.setBlockState(13, 2, 22, Blocks.GOLD_BLOCK.getDefaultState());
        s.setBlockState(13, 3, 22, Blocks.GOLD_BLOCK.getDefaultState());

        // 王座两侧宝箱
        placeLootChest(s, 10, 1, 20, "moremod:dungeon/dungeon_treasure");
        placeLootChest(s, 16, 1, 20, "moremod:dungeon/dungeon_treasure");

        // 柱子
        createQuartzPillar(s, 5, 8);
        createQuartzPillar(s, 20, 8);
        createQuartzPillar(s, 5, 16);
        createQuartzPillar(s, 20, 16);

        // 守卫
        placeRandomSpawner(s, 7, 1, 4, 2);
        placeRandomSpawner(s, 18, 1, 4, 2);

        return s;
    }

    // ==================== 入口房间 ====================

    /**
     * 入口房间 - 带醒目标志的引导版
     */
    public static Schematic entranceRoom() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.STONE.getDefaultState();
        IBlockState pillar = Blocks.STONEBRICK.getDefaultState();
        IBlockState goldFloor = Blocks.GOLD_BLOCK.getDefaultState();
        IBlockState lapisFloor = Blocks.LAPIS_BLOCK.getDefaultState();

        // 地板 - 带指引图案
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, floor);
            }
        }

        // 中央金色標誌 - "START" 圖案
        int center = STANDARD_SIZE / 2;
        // 圓形金色區域
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz <= 9) {
                    s.setBlockState(center + dx, 0, center + dz, goldFloor);
                }
            }
        }
        // 十字青金石指引
        for (int i = 4; i <= 8; i++) {
            s.setBlockState(center + i, 0, center, lapisFloor); // 東
            s.setBlockState(center - i, 0, center, lapisFloor); // 西
            s.setBlockState(center, 0, center + i, lapisFloor); // 南
            s.setBlockState(center, 0, center - i, lapisFloor); // 北
        }

        // 中央入口標誌塔
        createEntranceBeacon(s, center, 1, center);

        // 四角柱 - 更華麗
        createEntrancePillar(s, 5, 5);
        createEntrancePillar(s, 20, 5);
        createEntrancePillar(s, 5, 20);
        createEntrancePillar(s, 20, 20);

        // 明亮照明
        s.setBlockState(center, 2, 2, Blocks.TORCH.getDefaultState());
        s.setBlockState(center, 2, 23, Blocks.TORCH.getDefaultState());
        s.setBlockState(2, 2, center, Blocks.TORCH.getDefaultState());
        s.setBlockState(23, 2, center, Blocks.TORCH.getDefaultState());

        // 角落海晶燈
        s.setBlockState(3, 1, 3, Blocks.SEA_LANTERN.getDefaultState());
        s.setBlockState(22, 1, 3, Blocks.SEA_LANTERN.getDefaultState());
        s.setBlockState(3, 1, 22, Blocks.SEA_LANTERN.getDefaultState());
        s.setBlockState(22, 1, 22, Blocks.SEA_LANTERN.getDefaultState());

        // 初始补给箱
        placeLootChest(s, center - 3, 1, center, "moremod:dungeon/dungeon_entrance");
        placeLootChest(s, center + 3, 1, center, "moremod:dungeon/dungeon_entrance");

        return s;
    }

    /**
     * 創建入口標誌燈塔
     */
    private static void createEntranceBeacon(Schematic s, int x, int y, int z) {
        // 底座
        s.setBlockState(x, y, z, Blocks.BEACON.getDefaultState());

        // 玻璃罩
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    s.setBlockState(x + dx, y, z + dz, Blocks.STAINED_GLASS.getStateFromMeta(4)); // 黃色
                    s.setBlockState(x + dx, y + 1, z + dz, Blocks.STAINED_GLASS.getStateFromMeta(4));
                }
            }
        }
        s.setBlockState(x, y + 1, z, Blocks.GLOWSTONE.getDefaultState());

        // 頂部裝飾
        s.setBlockState(x, y + 2, z, Blocks.GOLD_BLOCK.getDefaultState());
        s.setBlockState(x, y + 3, z, Blocks.TORCH.getDefaultState());
    }

    /**
     * 創建入口房間華麗柱子
     */
    private static void createEntrancePillar(Schematic s, int x, int z) {
        for (int y = 1; y <= 5; y++) {
            if (y == 1 || y == 5) {
                s.setBlockState(x, y, z, Blocks.STONEBRICK.getStateFromMeta(3)); // 錾制石磚
            } else {
                s.setBlockState(x, y, z, Blocks.STONEBRICK.getDefaultState());
            }
        }
        s.setBlockState(x, 6, z, Blocks.TORCH.getDefaultState());
    }

    /**
     * 入口房间变种 - 遗迹入口
     */
    public static Schematic entranceRoomRuins() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.STONEBRICK.getStateFromMeta(2); // 裂石砖
        IBlockState mossyBrick = Blocks.STONEBRICK.getStateFromMeta(1); // 苔石砖

        // 破碎地板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, rand.nextFloat() < 0.4 ? mossyBrick : floor);
            }
        }

        // 倒塌的柱子
        createRuinedPillar(s, 6, 6);
        createRuinedPillar(s, 19, 6);
        createRuinedPillar(s, 6, 19);
        // 一根完整的柱子
        for (int y = 1; y <= 5; y++) {
            s.setBlockState(19, y, 19, Blocks.STONEBRICK.getDefaultState());
        }

        // 藤蔓装饰
        for (int i = 0; i < 8; i++) {
            int vx = 1 + rand.nextInt(24);
            int vz = 1 + rand.nextInt(24);
            s.setBlockState(vx, 1, vz, Blocks.VINE.getDefaultState());
        }

        // 入口补给
        placeLootChest(s, 13, 1, 13, "moremod:dungeon/dungeon_entrance");

        // 火把 (稀少)
        s.setBlockState(13, 2, 5, Blocks.TORCH.getDefaultState());
        s.setBlockState(13, 2, 20, Blocks.TORCH.getDefaultState());

        return s;
    }

    /**
     * 入口房间变种 - 神殿入口
     */
    public static Schematic entranceRoomTemple() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState sandstoneFloor = Blocks.SANDSTONE.getStateFromMeta(2); // 平滑砂岩
        IBlockState chiseledSandstone = Blocks.SANDSTONE.getStateFromMeta(1); // 錾制砂岩

        // 砂岩地板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                boolean pattern = (x + z) % 5 == 0;
                s.setBlockState(x, 0, z, pattern ? chiseledSandstone : sandstoneFloor);
            }
        }

        // 砂岩柱
        createSandstonePillar(s, 6, 6);
        createSandstonePillar(s, 19, 6);
        createSandstonePillar(s, 6, 19);
        createSandstonePillar(s, 19, 19);

        // 入口拱门装饰
        s.setBlockState(13, 4, 2, Blocks.SANDSTONE.getStateFromMeta(1));
        s.setBlockState(12, 4, 2, Blocks.SANDSTONE_STAIRS.getDefaultState());
        s.setBlockState(14, 4, 2, Blocks.SANDSTONE_STAIRS.getDefaultState());

        // 补给
        placeLootChest(s, 13, 1, 13, "moremod:dungeon/dungeon_entrance");

        // 红石火把 (神秘感)
        s.setBlockState(6, 3, 6, Blocks.REDSTONE_TORCH.getDefaultState());
        s.setBlockState(19, 3, 6, Blocks.REDSTONE_TORCH.getDefaultState());
        s.setBlockState(6, 3, 19, Blocks.REDSTONE_TORCH.getDefaultState());
        s.setBlockState(19, 3, 19, Blocks.REDSTONE_TORCH.getDefaultState());

        return s;
    }

    // ==================== 枢纽/喷泉房间 ====================

    /**
     * 枢纽房间 - 喷泉
     */
    public static Schematic fountainRoom() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        // 石头地板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, Blocks.STONE.getDefaultState());
            }
        }

        // 喷泉池
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int dist = Math.max(Math.abs(dx), Math.abs(dz));
                if (dist == 3) {
                    s.setBlockState(13 + dx, 1, 13 + dz, Blocks.STONE_BRICK_STAIRS.getDefaultState());
                } else if (dist >= 1) {
                    s.setBlockState(13 + dx, 0, 13 + dz, Blocks.WATER.getDefaultState());
                }
            }
        }

        // 喷泉柱
        for (int y = 1; y <= 3; y++) {
            s.setBlockState(13, y, 13, Blocks.QUARTZ_BLOCK.getDefaultState());
        }
        s.setBlockState(13, 4, 13, Blocks.WATER.getDefaultState());

        // 四角装饰
        s.setBlockState(5, 1, 5, Blocks.FLOWER_POT.getDefaultState());
        s.setBlockState(20, 1, 5, Blocks.FLOWER_POT.getDefaultState());
        s.setBlockState(5, 1, 20, Blocks.FLOWER_POT.getDefaultState());
        s.setBlockState(20, 1, 20, Blocks.FLOWER_POT.getDefaultState());

        // 补给
        placeLootChest(s, 5, 1, 13, "moremod:dungeon/dungeon_hub");
        placeLootChest(s, 20, 1, 13, "moremod:dungeon/dungeon_hub");

        return s;
    }

    /**
     * 枢纽房间变种 - 休息营地
     */
    public static Schematic hubRoomCamp() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        // 泥土/草地板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, rand.nextFloat() < 0.3 ?
                        Blocks.GRASS.getDefaultState() :
                        Blocks.DIRT.getDefaultState());
            }
        }

        // 中央篝火
        s.setBlockState(13, 0, 13, Blocks.NETHERRACK.getDefaultState());
        s.setBlockState(13, 1, 13, Blocks.FIRE.getDefaultState());

        // 围绕篝火的原木座位
        s.setBlockState(11, 1, 11, Blocks.LOG.getDefaultState());
        s.setBlockState(15, 1, 11, Blocks.LOG.getDefaultState());
        s.setBlockState(11, 1, 15, Blocks.LOG.getDefaultState());
        s.setBlockState(15, 1, 15, Blocks.LOG.getDefaultState());

        // 帐篷 (羊毛)
        createTent(s, 6, 6);
        createTent(s, 18, 18);

        // 补给箱
        placeLootChest(s, 7, 1, 7, "moremod:dungeon/dungeon_hub");
        placeLootChest(s, 19, 1, 19, "moremod:dungeon/dungeon_hub");

        return s;
    }

    /**
     * 枢纽房间变种 - 图书馆
     */
    public static Schematic hubRoomLibrary() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.PLANKS.getDefaultState();
        IBlockState bookshelf = Blocks.BOOKSHELF.getDefaultState();

        // 木地板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, floor);
            }
        }

        // 书架排列
        for (int row = 0; row < 4; row++) {
            int rz = 4 + row * 5;
            for (int x = 3; x <= 22; x++) {
                if (x != 12 && x != 13 && x != 14) { // 留出走道
                    for (int y = 1; y <= 3; y++) {
                        s.setBlockState(x, y, rz, bookshelf);
                    }
                }
            }
        }

        // 中央附魔台
        s.setBlockState(13, 1, 13, Blocks.ENCHANTING_TABLE.getDefaultState());

        // 阅读桌
        s.setBlockState(10, 1, 13, Blocks.OAK_FENCE.getDefaultState());
        s.setBlockState(10, 2, 13, Blocks.WOODEN_PRESSURE_PLATE.getDefaultState());
        s.setBlockState(16, 1, 13, Blocks.OAK_FENCE.getDefaultState());
        s.setBlockState(16, 2, 13, Blocks.WOODEN_PRESSURE_PLATE.getDefaultState());

        // 烛台照明
        s.setBlockState(6, 3, 2, Blocks.TORCH.getDefaultState());
        s.setBlockState(19, 3, 2, Blocks.TORCH.getDefaultState());
        s.setBlockState(6, 3, 23, Blocks.TORCH.getDefaultState());
        s.setBlockState(19, 3, 23, Blocks.TORCH.getDefaultState());

        // 补给
        placeLootChest(s, 3, 1, 13, "moremod:dungeon/dungeon_hub");

        return s;
    }

    // ==================== 普通房间变种 ====================

    /**
     * 普通房间 - 炼金室
     */
    public static Schematic normalRoomAlchemy() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, Blocks.STONEBRICK.getDefaultState());
            }
        }

        // 四组酿造台
        int[][] brewingPositions = {{8, 8}, {17, 8}, {8, 17}, {17, 17}};
        for (int[] pos : brewingPositions) {
            s.setBlockState(pos[0], 1, pos[1], Blocks.BREWING_STAND.getDefaultState());
            s.setBlockState(pos[0], 1, pos[1] + 1, Blocks.CAULDRON.getDefaultState());
        }

        // 中央工作台
        s.setBlockState(13, 1, 13, Blocks.CRAFTING_TABLE.getDefaultState());

        // 刷怪笼
        placeRandomSpawner(s, 13, 0, 4, 1);
        placeRandomSpawner(s, 13, 0, 21, 1);

        placeLootChest(s, 13, 1, 8, "moremod:dungeon/dungeon_normal");

        // 火把
        s.setBlockState(4, 2, 4, Blocks.TORCH.getDefaultState());
        s.setBlockState(21, 2, 4, Blocks.TORCH.getDefaultState());
        s.setBlockState(4, 2, 21, Blocks.TORCH.getDefaultState());
        s.setBlockState(21, 2, 21, Blocks.TORCH.getDefaultState());

        return s;
    }

    /**
     * 普通房间 - 温室
     */
    public static Schematic normalRoomGreenhouse() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        // 草地 + 农田
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, Blocks.GRASS.getDefaultState());
            }
        }

        // 农田区域
        for (int x = 4; x < 22; x += 3) {
            for (int z = 4; z < 22; z += 3) {
                s.setBlockState(x, 0, z, Blocks.FARMLAND.getDefaultState());
                s.setBlockState(x, 1, z, Blocks.WHEAT.getStateFromMeta(7)); // 成熟小麦
            }
        }

        // 中央水源
        s.setBlockState(13, 0, 13, Blocks.WATER.getDefaultState());

        // 刷怪笼 (边缘)
        placeRandomSpawner(s, 2, 1, 13, 1);
        placeRandomSpawner(s, 23, 1, 13, 1);

        placeLootChest(s, 13, 1, 4, "moremod:dungeon/dungeon_normal");

        return s;
    }

    /**
     * 普通房间 - 矿井
     */
    public static Schematic normalRoomMine() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState stone = Blocks.STONE.getDefaultState();
        IBlockState gravel = Blocks.GRAVEL.getDefaultState();
        IBlockState coalOre = Blocks.COAL_ORE.getDefaultState();
        IBlockState ironOre = Blocks.IRON_ORE.getDefaultState();

        // 石头地板 + 矿石点缀
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                float r = rand.nextFloat();
                if (r < 0.1) {
                    s.setBlockState(x, 0, z, gravel);
                } else if (r < 0.15) {
                    s.setBlockState(x, 0, z, coalOre);
                } else if (r < 0.18) {
                    s.setBlockState(x, 0, z, ironOre);
                } else {
                    s.setBlockState(x, 0, z, stone);
                }
            }
        }

        // 矿车轨道
        for (int z = 4; z <= 21; z++) {
            s.setBlockState(13, 1, z, Blocks.RAIL.getDefaultState());
        }

        // 支撑木柱
        for (int pz = 6; pz <= 19; pz += 6) {
            for (int y = 1; y <= 4; y++) {
                s.setBlockState(6, y, pz, Blocks.LOG.getDefaultState());
                s.setBlockState(19, y, pz, Blocks.LOG.getDefaultState());
            }
            // 横梁
            for (int px = 6; px <= 19; px++) {
                s.setBlockState(px, 5, pz, Blocks.PLANKS.getDefaultState());
            }
        }

        placeLootChest(s, 13, 1, 8, "moremod:dungeon/dungeon_normal");

        // 刷怪笼 (矿洞深处)
        placeRandomSpawner(s, 4, 1, 4, 1);
        placeRandomSpawner(s, 21, 1, 21, 1);

        return s;
    }

    /**
     * 普通房间 - 储藏室
     */
    public static Schematic normalRoomStorage() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.PLANKS.getStateFromMeta(5); // 深色橡木

        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, floor);
            }
        }

        // 货架排列
        for (int row = 0; row < 3; row++) {
            int rz = 5 + row * 7;
            for (int x = 4; x <= 21; x += 4) {
                // 木箱堆叠
                s.setBlockState(x, 1, rz, Blocks.CHEST.getDefaultState());
                if (rand.nextFloat() < 0.5) {
                    s.setBlockState(x, 2, rz, Blocks.CHEST.getDefaultState());
                }
            }
        }

        // 刷怪笼 (角落)
        placeRandomSpawner(s, 2, 1, 2, 1);
        placeRandomSpawner(s, 23, 1, 23, 1);

        // 主要战利品
        placeLootChest(s, 13, 1, 13, "moremod:dungeon/dungeon_normal");

        return s;
    }

    // ==================== 辅助方法 ====================

    /**
     * 放置 VoidRipper 自毁刷怪砖
     * 生成2只血量较少的VoidRipper，VoidRipper生成后会自动销毁刷怪砖
     */
    private static void placeVoidRipperSpawner(Schematic s, int x, int y, int z) {
        s.setBlockState(x, y, z, Blocks.MOB_SPAWNER.getDefaultState());

        NBTTagCompound te = new NBTTagCompound();
        te.setString("id", "minecraft:mob_spawner");
        te.setInteger("x", x);
        te.setInteger("y", y);
        te.setInteger("z", z);

        // 设置生成参数
        te.setShort("Delay", (short) 20);           // 1秒后生成
        te.setShort("MinSpawnDelay", (short) 20);
        te.setShort("MaxSpawnDelay", (short) 40);
        te.setShort("SpawnCount", (short) 2);       // 每次生成2只
        te.setShort("MaxNearbyEntities", (short) 6);// 允许较多实体（VoidRipper会自己销毁spawner）
        te.setShort("RequiredPlayerRange", (short) 16);
        te.setShort("SpawnRange", (short) 3);

        // 设置生成的实体 - VoidRipper (带自定义NBT)
        NBTTagCompound spawnData = new NBTTagCompound();
        spawnData.setString("id", "moremod:void_ripper");

        // 设置较低的血量 (通过Attributes)
        NBTTagList attributes = new NBTTagList();
        NBTTagCompound healthAttr = new NBTTagCompound();
        healthAttr.setString("Name", "generic.maxHealth");
        healthAttr.setDouble("Base", 60.0); // 较低血量
        attributes.appendTag(healthAttr);
        spawnData.setTag("Attributes", attributes);
        spawnData.setFloat("Health", 60.0f);

        // 关键标记：让VoidRipper知道这是Mini-Boss刷怪砖生成的，需要销毁spawner
        spawnData.setBoolean("MiniBossSpawn", true);

        te.setTag("SpawnData", spawnData);

        // SpawnPotentials
        NBTTagList potentials = new NBTTagList();
        NBTTagCompound entry = new NBTTagCompound();
        entry.setInteger("Weight", 1);
        NBTTagCompound entity = new NBTTagCompound();
        entity.setString("id", "moremod:void_ripper");
        entity.setTag("Attributes", attributes.copy());
        entity.setFloat("Health", 60.0f);
        entity.setBoolean("MiniBossSpawn", true);  // 也在SpawnPotentials中设置
        entry.setTag("Entity", entity);
        potentials.appendTag(entry);
        te.setTag("SpawnPotentials", potentials);

        s.tileEntities.add(te);
    }

    private static void placeRandomSpawner(Schematic s, int x, int y, int z, int difficulty) {
        String entityId = SPAWNER_ENTITIES[rand.nextInt(SPAWNER_ENTITIES.length)];
        placeSpawner(s, x, y, z, entityId, difficulty);
    }

    private static void placeSpawner(Schematic s, int x, int y, int z, String entityId, int difficulty) {
        s.setBlockState(x, y, z, Blocks.MOB_SPAWNER.getDefaultState());

        NBTTagCompound te = new NBTTagCompound();
        te.setString("id", "minecraft:mob_spawner");
        te.setInteger("x", x);
        te.setInteger("y", y);
        te.setInteger("z", z);
        te.setShort("Delay", (short) 0);
        te.setShort("MinSpawnDelay", (short) Math.max(60, 200 - difficulty * 20));
        te.setShort("MaxSpawnDelay", (short) Math.max(120, 400 - difficulty * 40));
        te.setShort("SpawnCount", (short) (1 + difficulty));
        te.setShort("MaxNearbyEntities", (short) (3 + difficulty));
        te.setShort("RequiredPlayerRange", (short) 16);
        te.setShort("SpawnRange", (short) 4);

        NBTTagCompound spawnData = new NBTTagCompound();
        spawnData.setString("id", entityId);
        te.setTag("SpawnData", spawnData);

        NBTTagList potentials = new NBTTagList();
        NBTTagCompound entry = new NBTTagCompound();
        entry.setInteger("Weight", 1);
        NBTTagCompound entity = new NBTTagCompound();
        entity.setString("id", entityId);
        entry.setTag("Entity", entity);
        potentials.appendTag(entry);
        te.setTag("SpawnPotentials", potentials);

        s.tileEntities.add(te);
    }

    private static void placeLootChest(Schematic s, int x, int y, int z, String lootTable) {
        s.setBlockState(x, y, z, Blocks.CHEST.getDefaultState());

        NBTTagCompound chestTag = new NBTTagCompound();
        chestTag.setString("id", "minecraft:chest");
        chestTag.setInteger("x", x);
        chestTag.setInteger("y", y);
        chestTag.setInteger("z", z);
        chestTag.setString("LootTable", lootTable);
        chestTag.setLong("LootTableSeed", rand.nextLong());

        s.tileEntities.add(chestTag);
    }

    // ===== 柱子创建方法 =====

    private static void createPillar(Schematic s, int x, int z) {
        IBlockState pillarBlock = ModBlocks.UNBREAKABLE_BARRIER_ANCHOR.getDefaultState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = 1; y < BOSS_ROOM_HEIGHT - 2; y++) {
                    if (Math.abs(dx) + Math.abs(dz) <= 1) {
                        s.setBlockState(x + dx, y, z + dz, pillarBlock);
                    }
                }
            }
        }
        s.setBlockState(x, BOSS_ROOM_HEIGHT - 2, z, Blocks.GLOWSTONE.getDefaultState());
    }

    private static void createSmallPillar(Schematic s, int x, int z, int height) {
        for (int y = 1; y < height - 1; y++) {
            s.setBlockState(x, y, z, Blocks.STONEBRICK.getDefaultState());
        }
        s.setBlockState(x, height - 2, z, Blocks.TORCH.getDefaultState());
    }

    private static void createNetherPillar(Schematic s, int x, int z, int height) {
        for (int y = 1; y < height - 1; y++) {
            s.setBlockState(x, y, z, Blocks.NETHER_BRICK.getDefaultState());
        }
        s.setBlockState(x, 0, z, Blocks.NETHERRACK.getDefaultState());
        s.setBlockState(x, height - 2, z, Blocks.FIRE.getDefaultState());
    }

    private static void createGoldPillar(Schematic s, int x, int z) {
        for (int y = 1; y <= 3; y++) {
            s.setBlockState(x, y, z, Blocks.GOLD_BLOCK.getDefaultState());
        }
        s.setBlockState(x, 4, z, Blocks.TORCH.getDefaultState());
    }

    private static void createQuartzPillar(Schematic s, int x, int z) {
        for (int y = 1; y <= 4; y++) {
            s.setBlockState(x, y, z, Blocks.QUARTZ_BLOCK.getStateFromMeta(2)); // 柱状石英
        }
        s.setBlockState(x, 5, z, Blocks.TORCH.getDefaultState());
    }

    private static void createSandstonePillar(Schematic s, int x, int z) {
        for (int y = 1; y <= 4; y++) {
            s.setBlockState(x, y, z, Blocks.SANDSTONE.getStateFromMeta(2)); // 平滑砂岩
        }
        s.setBlockState(x, 5, z, Blocks.TORCH.getDefaultState());
    }

    private static void createRuinedPillar(Schematic s, int x, int z) {
        int height = 1 + rand.nextInt(3);
        for (int y = 1; y <= height; y++) {
            s.setBlockState(x, y, z, Blocks.STONEBRICK.getStateFromMeta(2)); // 裂石砖
        }
        // 倒塌的碎片
        if (rand.nextBoolean()) {
            s.setBlockState(x + 1, 1, z, Blocks.STONEBRICK.getStateFromMeta(2));
        }
        if (rand.nextBoolean()) {
            s.setBlockState(x, 1, z + 1, Blocks.STONEBRICK.getStateFromMeta(2));
        }
    }

    // ===== 装饰创建方法 =====

    private static void createBossAltar(Schematic s, int x, int y, int z) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int dist = Math.max(Math.abs(dx), Math.abs(dz));
                if (dist <= 3) {
                    IBlockState block = dist == 3 ? Blocks.OBSIDIAN.getDefaultState() :
                            dist == 2 ? Blocks.PURPUR_BLOCK.getDefaultState() :
                                    Blocks.END_STONE.getDefaultState();
                    s.setBlockState(x + dx, y, z + dz, block);
                }
            }
        }
        // 使用时间锁定方块作为祭坛核心（更美观）
        s.setBlockState(x, y + 1, z, ModBlocks.UNBREAKABLE_BARRIER_TEMPORAL.getDefaultState());
    }

    private static void createMiniBossPlatform(Schematic s, int x, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int dist = Math.max(Math.abs(dx), Math.abs(dz));
                if (dist <= 2) {
                    s.setBlockState(x + dx, 1, z + dz,
                            dist == 2 ? Blocks.STONEBRICK.getStateFromMeta(3) :
                                    Blocks.STONEBRICK.getDefaultState());
                }
            }
        }
    }

    private static void addWallDecoration(Schematic s, int size, int height) {
        // 四面墙上的红石火把
        for (int i = 8; i < size - 8; i += 6) {
            s.setBlockState(i, height / 2, 2, Blocks.REDSTONE_TORCH.getDefaultState());
            s.setBlockState(i, height / 2, size - 3, Blocks.REDSTONE_TORCH.getDefaultState());
            s.setBlockState(2, height / 2, i, Blocks.REDSTONE_TORCH.getDefaultState());
            s.setBlockState(size - 3, height / 2, i, Blocks.REDSTONE_TORCH.getDefaultState());
        }
    }

    private static void createCombatObstacles(Schematic s) {
        IBlockState cover = Blocks.COBBLESTONE_WALL.getDefaultState();
        IBlockState fullCover = Blocks.COBBLESTONE.getDefaultState();

        for (int i = 0; i < 12; i++) {
            int x = 3 + rand.nextInt(20);
            int z = 3 + rand.nextInt(20);
            int type = rand.nextInt(3);

            switch (type) {
                case 0: // L形掩体
                    s.setBlockState(x, 1, z, cover);
                    s.setBlockState(x + 1, 1, z, cover);
                    s.setBlockState(x, 1, z + 1, cover);
                    break;
                case 1: // 高掩体
                    s.setBlockState(x, 1, z, fullCover);
                    s.setBlockState(x, 2, z, fullCover);
                    break;
                case 2: // 一字形
                    s.setBlockState(x, 1, z, cover);
                    s.setBlockState(x - 1, 1, z, cover);
                    s.setBlockState(x + 1, 1, z, cover);
                    break;
            }
        }
    }

    private static void createTrainingDummy(Schematic s, int x, int y, int z) {
        s.setBlockState(x, y, z, Blocks.HAY_BLOCK.getDefaultState());
        s.setBlockState(x, y + 1, z, Blocks.HAY_BLOCK.getDefaultState());
        s.setBlockState(x, y + 2, z, Blocks.PUMPKIN.getDefaultState());
    }

    private static void createWoodenBarricade(Schematic s, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            s.setBlockState(x + dx, y, z, Blocks.OAK_FENCE.getDefaultState());
            s.setBlockState(x + dx, y + 1, z, Blocks.OAK_FENCE.getDefaultState());
        }
    }

    private static void createTent(Schematic s, int x, int z) {
        // 简单帐篷 (羊毛)
        IBlockState wool = Blocks.WOOL.getStateFromMeta(0); // 白色

        s.setBlockState(x, 1, z, wool);
        s.setBlockState(x + 1, 1, z, wool);
        s.setBlockState(x, 1, z + 1, wool);
        s.setBlockState(x + 1, 1, z + 1, wool);

        s.setBlockState(x, 2, z, wool);
        s.setBlockState(x + 1, 2, z, wool);

        s.setBlockState(x, 3, z, wool);
    }

    // ===== 迷宫生成 =====

    private static boolean[][] generateMaze(int width, int height) {
        boolean[][] maze = new boolean[width][height];

        // 边界
        for (int x = 0; x < width; x++) {
            maze[x][0] = true;
            maze[x][height - 1] = true;
        }
        for (int z = 0; z < height; z++) {
            maze[0][z] = true;
            maze[width - 1][z] = true;
        }

        // 递归分割
        divideMaze(maze, 1, 1, width - 2, height - 2, rand);

        // 中央清空
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                maze[13 + dx][13 + dz] = false;
            }
        }

        return maze;
    }

    private static void divideMaze(boolean[][] maze, int x, int y, int w, int h, Random rand) {
        if (w < 4 || h < 4) return;

        boolean horizontal = w < h;

        if (horizontal) {
            int wallY = y + 2 + rand.nextInt(Math.max(1, h - 3));
            for (int i = x; i < x + w; i++) {
                maze[i][wallY] = true;
            }
            int doorX = x + rand.nextInt(w);
            maze[doorX][wallY] = false;

            divideMaze(maze, x, y, w, wallY - y, rand);
            divideMaze(maze, x, wallY + 1, w, h - (wallY - y) - 1, rand);
        } else {
            int wallX = x + 2 + rand.nextInt(Math.max(1, w - 3));
            for (int i = y; i < y + h; i++) {
                maze[wallX][i] = true;
            }
            int doorY = y + rand.nextInt(h);
            maze[wallX][doorY] = false;

            divideMaze(maze, x, y, wallX - x, h, rand);
            divideMaze(maze, wallX + 1, y, w - (wallX - x) - 1, h, rand);
        }
    }

    private static void placeSpawnersInMaze(Schematic s, boolean[][] maze) {
        int[][] positions = {{7, 7}, {18, 7}, {7, 18}, {18, 18}};
        for (int[] pos : positions) {
            if (!maze[pos[0]][pos[1]]) {
                placeRandomSpawner(s, pos[0], 1, pos[1], 1);
            }
        }
    }

    // ===== 陷阱走廊 =====

    private static void createTrapCorridor(Schematic s, int startX, int endX, int z, boolean horizontal) {
        if (horizontal) {
            for (int x = startX; x <= endX; x++) {
                s.setBlockState(x, 1, z, Blocks.STONE_PRESSURE_PLATE.getDefaultState());
                if ((x - startX) % 3 == 0) {
                    placeRandomSpawner(s, x, 0, z - 2, 1);
                    placeRandomSpawner(s, x, 0, z + 2, 1);
                }
            }
        } else {
            for (int currentZ = startX; currentZ <= endX; currentZ++) {
                s.setBlockState(z, 1, currentZ, Blocks.STONE_PRESSURE_PLATE.getDefaultState());
                if ((currentZ - startX) % 3 == 0) {
                    placeRandomSpawner(s, z - 2, 0, currentZ, 1);
                    placeRandomSpawner(s, z + 2, 0, currentZ, 1);
                }
            }
        }
    }

    private static void createHiddenSpawnerRoom(Schematic s, int x, int z) {
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                for (int y = 1; y <= 3; y++) {
                    s.setBlockState(x + dx, y, z + dz, Blocks.AIR.getDefaultState());
                }
            }
        }
        placeRandomSpawner(s, x + 1, 1, z + 1, 2);
    }

    // ==================== 楼梯房间 (三维地牢) ====================

    /**
     * 楼梯房间 - 向上传送
     */
    public static Schematic staircaseRoomUp() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.QUARTZ_BLOCK.getDefaultState();
        IBlockState accent = Blocks.LAPIS_BLOCK.getDefaultState(); // 蓝色=向上

        // 石英地板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, floor);
            }
        }

        int center = STANDARD_SIZE / 2;

        // 中央传送平台 - 向上箭头图案
        createUpArrowPattern(s, center, accent);

        // 传送台阶 (向上方向)
        for (int step = 0; step < 4; step++) {
            int y = step;
            int startZ = center - 2 - step;
            for (int dx = -2; dx <= 2; dx++) {
                s.setBlockState(center + dx, y, startZ, Blocks.QUARTZ_STAIRS.getDefaultState());
            }
        }

        // 四角海晶燈柱
        createSeaLanternPillar(s, 5, 5);
        createSeaLanternPillar(s, 20, 5);
        createSeaLanternPillar(s, 5, 20);
        createSeaLanternPillar(s, 20, 20);

        // 发光指引
        s.setBlockState(center, 1, center + 5, Blocks.SEA_LANTERN.getDefaultState());
        s.setBlockState(center, 1, center - 5, Blocks.SEA_LANTERN.getDefaultState());

        return s;
    }

    /**
     * 楼梯房间 - 向下传送
     */
    public static Schematic staircaseRoomDown() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.NETHER_BRICK.getDefaultState();
        IBlockState accent = Blocks.REDSTONE_BLOCK.getDefaultState(); // 红色=向下

        // 地狱砖地板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, floor);
            }
        }

        int center = STANDARD_SIZE / 2;

        // 中央传送平台 - 向下箭头图案
        createDownArrowPattern(s, center, accent);

        // 下沉平台 (向下方向) - 使用不同材质标记，不能用负Y
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 4) {
                    // 使用黑曜石作为下沉区域标记
                    s.setBlockState(center + dx, 0, center + dz, Blocks.OBSIDIAN.getDefaultState());
                }
            }
        }
        // 中央下沉一格的视觉效果
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                s.setBlockState(center + dx, 0, center + dz, Blocks.MAGMA.getDefaultState());
            }
        }

        // 四角火焰柱
        createNetherPillar(s, 5, 5, STANDARD_HEIGHT);
        createNetherPillar(s, 20, 5, STANDARD_HEIGHT);
        createNetherPillar(s, 5, 20, STANDARD_HEIGHT);
        createNetherPillar(s, 20, 20, STANDARD_HEIGHT);

        // 红石灯照明
        s.setBlockState(center - 5, 1, center, Blocks.REDSTONE_LAMP.getDefaultState());
        s.setBlockState(center + 5, 1, center, Blocks.REDSTONE_LAMP.getDefaultState());

        return s;
    }

    /**
     * 楼梯房间 - 双向传送
     */
    public static Schematic staircaseRoomBoth() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.END_BRICKS.getDefaultState();
        IBlockState upAccent = Blocks.LAPIS_BLOCK.getDefaultState();
        IBlockState downAccent = Blocks.REDSTONE_BLOCK.getDefaultState();

        // 末地砖地板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, floor);
            }
        }

        int center = STANDARD_SIZE / 2;

        // 左半区 - 向上 (蓝色)
        for (int x = 3; x < center - 1; x++) {
            for (int z = 3; z < STANDARD_SIZE - 3; z++) {
                s.setBlockState(x, 0, z, Blocks.QUARTZ_BLOCK.getDefaultState());
            }
        }
        createUpArrowPattern(s, 8, upAccent);

        // 右半区 - 向下 (红色)
        for (int x = center + 1; x < STANDARD_SIZE - 3; x++) {
            for (int z = 3; z < STANDARD_SIZE - 3; z++) {
                s.setBlockState(x, 0, z, Blocks.NETHER_BRICK.getDefaultState());
            }
        }
        createDownArrowPattern(s, 17, downAccent);

        // 中央分隔带
        for (int z = 0; z < STANDARD_SIZE; z++) {
            s.setBlockState(center, 0, z, Blocks.PURPUR_BLOCK.getDefaultState());
            s.setBlockState(center, 1, z, Blocks.PURPUR_PILLAR.getDefaultState());
        }

        // 四角装饰柱
        createEndPillar(s, 5, 5);
        createEndPillar(s, 20, 5);
        createEndPillar(s, 5, 20);
        createEndPillar(s, 20, 20);

        // 照明
        s.setBlockState(8, 1, center, Blocks.SEA_LANTERN.getDefaultState());
        s.setBlockState(17, 1, center, Blocks.REDSTONE_LAMP.getDefaultState());

        return s;
    }

    // ===== 楼梯房间辅助方法 =====

    private static void createUpArrowPattern(Schematic s, int centerX, IBlockState accent) {
        int center = STANDARD_SIZE / 2;
        // 向上箭头 ↑
        s.setBlockState(centerX, 0, center, accent);
        s.setBlockState(centerX, 0, center + 1, accent);
        s.setBlockState(centerX, 0, center + 2, accent);
        s.setBlockState(centerX - 1, 0, center - 1, accent);
        s.setBlockState(centerX + 1, 0, center - 1, accent);
        s.setBlockState(centerX - 2, 0, center, accent);
        s.setBlockState(centerX + 2, 0, center, accent);
    }

    private static void createDownArrowPattern(Schematic s, int centerX, IBlockState accent) {
        int center = STANDARD_SIZE / 2;
        // 向下箭头 ↓
        s.setBlockState(centerX, 0, center, accent);
        s.setBlockState(centerX, 0, center - 1, accent);
        s.setBlockState(centerX, 0, center - 2, accent);
        s.setBlockState(centerX - 1, 0, center + 1, accent);
        s.setBlockState(centerX + 1, 0, center + 1, accent);
        s.setBlockState(centerX - 2, 0, center, accent);
        s.setBlockState(centerX + 2, 0, center, accent);
    }

    private static void createSeaLanternPillar(Schematic s, int x, int z) {
        for (int y = 1; y <= 4; y++) {
            s.setBlockState(x, y, z, Blocks.QUARTZ_BLOCK.getStateFromMeta(2)); // 柱状石英
        }
        s.setBlockState(x, 5, z, Blocks.SEA_LANTERN.getDefaultState());
    }

    private static void createEndPillar(Schematic s, int x, int z) {
        for (int y = 1; y <= 4; y++) {
            s.setBlockState(x, y, z, Blocks.PURPUR_PILLAR.getDefaultState());
        }
        s.setBlockState(x, 5, z, Blocks.END_ROD.getDefaultState());
    }

    // ==================== 更多房间变种 ====================

    /**
     * 战斗房间变种 - 地下竞技场
     */
    public static Schematic combatRoomArena() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.COBBLESTONE.getDefaultState();
        IBlockState accent = Blocks.STONEBRICK.getStateFromMeta(2); // 裂石砖

        // 圆形竞技场
        int center = STANDARD_SIZE / 2;
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                double dist = Math.sqrt(Math.pow(x - center, 2) + Math.pow(z - center, 2));
                if (dist <= 11) {
                    s.setBlockState(x, 0, z, dist <= 8 ? floor : accent);
                } else {
                    // 阶梯看台
                    int height = (int) (dist - 10);
                    for (int y = 0; y <= Math.min(height, 3); y++) {
                        s.setBlockState(x, y, z, Blocks.STONEBRICK.getDefaultState());
                    }
                }
            }
        }

        // 中央武器架
        s.setBlockState(center, 1, center, Blocks.STANDING_BANNER.getDefaultState());

        // 四个刷怪笼入口
        placeRandomSpawner(s, center, 0, 3, 2);
        placeRandomSpawner(s, center, 0, 22, 2);
        placeRandomSpawner(s, 3, 0, center, 2);
        placeRandomSpawner(s, 22, 0, center, 2);

        placeLootChest(s, center - 3, 1, center - 3, "moremod:dungeon/dungeon_normal");
        placeLootChest(s, center + 3, 1, center + 3, "moremod:dungeon/dungeon_normal");

        return s;
    }

    /**
     * 谜题房间 - 迷宫挑战
     */
    public static Schematic puzzleRoomMaze() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        // 地板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, Blocks.STONEBRICK.getDefaultState());
            }
        }

        // 简化迷宫墙壁
        IBlockState wall = Blocks.STONEBRICK.getStateFromMeta(1); // 苔石砖
        // 外墙
        for (int i = 0; i < STANDARD_SIZE; i++) {
            for (int y = 1; y <= 3; y++) {
                s.setBlockState(i, y, 0, wall);
                s.setBlockState(i, y, 25, wall);
                s.setBlockState(0, y, i, wall);
                s.setBlockState(25, y, i, wall);
            }
        }

        // 内部墙壁（十字形）
        for (int i = 5; i < 21; i++) {
            if (i < 10 || i > 16) {
                for (int y = 1; y <= 2; y++) {
                    s.setBlockState(i, y, 13, wall);
                    s.setBlockState(13, y, i, wall);
                }
            }
        }

        // 角落刷怪笼
        placeRandomSpawner(s, 4, 1, 4, 1);
        placeRandomSpawner(s, 21, 1, 21, 1);

        // 中央宝箱
        placeLootChest(s, 13, 1, 13, "moremod:dungeon/dungeon_puzzle");

        // 火把照明
        s.setBlockState(6, 2, 6, Blocks.TORCH.getDefaultState());
        s.setBlockState(19, 2, 6, Blocks.TORCH.getDefaultState());
        s.setBlockState(6, 2, 19, Blocks.TORCH.getDefaultState());
        s.setBlockState(19, 2, 19, Blocks.TORCH.getDefaultState());

        return s;
    }

    /**
     * 隐藏宝藏房间 - 海底神殿风格
     */
    public static Schematic treasureRoomOcean() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.PRISMARINE.getDefaultState();
        IBlockState darkFloor = Blocks.PRISMARINE.getStateFromMeta(1); // 暗海晶石

        // 海晶石地板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                boolean dark = (x + z) % 3 == 0;
                s.setBlockState(x, 0, z, dark ? darkFloor : floor);
            }
        }

        int center = STANDARD_SIZE / 2;

        // 中央水池
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx * dx + dz * dz <= 16) {
                    s.setBlockState(center + dx, 0, center + dz, Blocks.WATER.getDefaultState());
                }
            }
        }

        // 海晶灯柱
        for (int corner = 0; corner < 4; corner++) {
            int px = corner < 2 ? 5 : 20;
            int pz = corner % 2 == 0 ? 5 : 20;
            for (int y = 1; y <= 4; y++) {
                s.setBlockState(px, y, pz, Blocks.PRISMARINE.getStateFromMeta(2)); // 海晶石砖
            }
            s.setBlockState(px, 5, pz, Blocks.SEA_LANTERN.getDefaultState());
        }

        // 宝箱在水池周围
        placeLootChest(s, center - 6, 1, center, "moremod:dungeon/dungeon_treasure");
        placeLootChest(s, center + 6, 1, center, "moremod:dungeon/dungeon_treasure");
        placeLootChest(s, center, 1, center - 6, "moremod:dungeon/dungeon_treasure");
        placeLootChest(s, center, 1, center + 6, "moremod:dungeon/dungeon_treasure");

        // 守卫
        placeRandomSpawner(s, 3, 1, 13, 2);
        placeRandomSpawner(s, 22, 1, 13, 2);

        return s;
    }
}
