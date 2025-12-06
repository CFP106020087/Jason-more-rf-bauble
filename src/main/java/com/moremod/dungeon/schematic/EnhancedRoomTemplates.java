package com.moremod.dungeon.schematic;

import com.moremod.schematic.Schematic;
import com.moremod.init.ModBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;

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
    private static final int STAIRCASE_ROOM_HEIGHT = 10;
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
        // 使用量子领域方块作为祭坛核心（与 DungeonBossSpawner 匹配）
        s.setBlockState(x, y + 1, z, ModBlocks.UNBREAKABLE_BARRIER_QUANTUM.getDefaultState());
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


    /**
     * 楼梯房间 - 向下传送
     */
    public static Schematic staircaseRoomUp() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STAIRCASE_ROOM_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.QUARTZ_BLOCK.getDefaultState();
        IBlockState accent = Blocks.LAPIS_BLOCK.getDefaultState(); // 蓝色=向上
        IBlockState pillarMat = Blocks.QUARTZ_BLOCK.getStateFromMeta(2); // 柱状石英
        // 使用完整方块作为阶梯，以保证生成稳定性和可行走性
        IBlockState stairMat = Blocks.QUARTZ_BLOCK.getDefaultState();

        // 基础地板和天花板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, floor);
                s.setBlockState(x, STAIRCASE_ROOM_HEIGHT - 1, z, floor);
            }
        }

        int center = STANDARD_SIZE / 2;
        int platformHeight = 4;

        // 1. 中央高架传送平台 (9x9)
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                // 使用青金石标记核心传送区域 (3x3)
                boolean isCore = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                s.setBlockState(center + dx, platformHeight, center + dz, isCore ? accent : floor);
            }
        }

        // 2. 四方宏伟阶梯 (宽度 5)
        int stairWidth = 2; // 半径 (2*2+1 = 5格宽)
        createGrandStairs(s, center, center, platformHeight, EnumFacing.NORTH, stairWidth, stairMat);
        createGrandStairs(s, center, center, platformHeight, EnumFacing.SOUTH, stairWidth, stairMat);
        createGrandStairs(s, center, center, platformHeight, EnumFacing.EAST, stairWidth, stairMat);
        createGrandStairs(s, center, center, platformHeight, EnumFacing.WEST, stairWidth, stairMat);


        // 3. 支撑柱和装饰
        int[][] pillarPos = {{center-6, center-6}, {center+6, center-6}, {center-6, center+6}, {center+6, center+6}};
        for (int[] pos : pillarPos) {
            for (int y = 1; y < platformHeight; y++) {
                s.setBlockState(pos[0], y, pos[1], pillarMat);
            }
            // 柱顶照明
            s.setBlockState(pos[0], platformHeight, pos[1], Blocks.SEA_LANTERN.getDefaultState());
        }

        // 4. 中央光束 (视觉效果)
        for (int y = platformHeight + 1; y < STAIRCASE_ROOM_HEIGHT - 1; y++) {
            // 使用末地烛模拟光束
            s.setBlockState(center, y, center, Blocks.END_ROD.getDefaultState());
        }
        s.setBlockState(center, STAIRCASE_ROOM_HEIGHT - 1, center, Blocks.SEA_LANTERN.getDefaultState());

        return s;
    }

    /**
     * 楼梯房间 - 向下：深渊螺旋 (The Descent)
     * 主题：深入、黑暗。结构：环绕中央深坑的螺旋楼梯，通向底部。
     */
    public static Schematic staircaseRoomDown() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STAIRCASE_ROOM_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.NETHER_BRICK.getDefaultState();
        IBlockState accent = Blocks.REDSTONE_BLOCK.getDefaultState(); // 红色=向下
        IBlockState wall = Blocks.OBSIDIAN.getDefaultState();
        IBlockState stairMat = Blocks.NETHER_BRICK.getDefaultState();

        int center = STANDARD_SIZE / 2;
        int upperLevelHeight = 6;

        // 1. 搭建整体结构
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                // 天花板
                s.setBlockState(x, STAIRCASE_ROOM_HEIGHT - 1, z, wall);
                // 底部保护层 Y=0
                s.setBlockState(x, 0, z, wall);
            }
        }

        // 2. 创建上层平台 (Y=6)
        for (int x = 1; x < STANDARD_SIZE - 1; x++) {
            for (int z = 1; z < STANDARD_SIZE - 1; z++) {
                s.setBlockState(x, upperLevelHeight, z, floor);
            }
        }

        // 3. 中央深坑 (9x9)
        int pitSize = 4; // 半径
        for (int dx = -pitSize; dx <= pitSize; dx++) {
            for (int dz = -pitSize; dz <= pitSize; dz++) {
                // 清空上层平台中央
                s.setBlockState(center + dx, upperLevelHeight, center + dz, Blocks.AIR.getDefaultState());

                // 底部传送核心 (Y=1)
                boolean isCore = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                // 底部岩浆和红石块
                s.setBlockState(center + dx, 1, center + dz, isCore ? accent : Blocks.MAGMA.getDefaultState());
            }
        }

        // 4. 螺旋下降楼梯 (从 Y=6 到 Y=2)
        createSpiralDescent(s, center, upperLevelHeight, stairMat);

        // 5. 装饰和氛围
        // 悬挂锁链 (铁栅栏)
        for (int dx = -6; dx <= 6; dx+=12) {
            for (int dz = -6; dz <= 6; dz+=12) {
                for (int y = upperLevelHeight + 1; y < STAIRCASE_ROOM_HEIGHT - 1; y++) {
                    s.setBlockState(center + dx, y, center + dz, Blocks.IRON_BARS.getDefaultState());
                }
                s.setBlockState(center + dx, upperLevelHeight, center + dz, Blocks.REDSTONE_LAMP.getDefaultState());
            }
        }

        // 6. 深坑边缘栏杆 (注意避开楼梯起点)
        IBlockState railing = Blocks.NETHER_BRICK_FENCE.getDefaultState();
        for (int i = -pitSize; i <= pitSize; i++) {
            // 东西两侧
            s.setBlockState(center - pitSize - 1, upperLevelHeight + 1, center + i, railing);
            s.setBlockState(center + pitSize + 1, upperLevelHeight + 1, center + i, railing);

            // 南北两侧 (避开北侧 Z- 的楼梯起点 -1, 0, 1)
            if (i < -1 || i > 1) {
                s.setBlockState(center + i, upperLevelHeight + 1, center - pitSize - 1, railing);
            }
            s.setBlockState(center + i, upperLevelHeight + 1, center + pitSize + 1, railing);
        }

        return s;
    }

    /**
     * 楼梯房间 - 双向：时空枢纽 (Dimensional Nexus)
     * 主题：平衡、能量。结构：分离的上升平台和下降平台。
     */
    public static Schematic staircaseRoomBoth() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STAIRCASE_ROOM_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floor = Blocks.END_BRICKS.getDefaultState();
        IBlockState upAccent = Blocks.LAPIS_BLOCK.getDefaultState();
        IBlockState downAccent = Blocks.REDSTONE_BLOCK.getDefaultState();
        IBlockState core = Blocks.BEACON.getDefaultState();
        IBlockState stairs = Blocks.PURPUR_BLOCK.getDefaultState(); // 使用完整方块

        // 基础地板和天花板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, floor);
                s.setBlockState(x, STAIRCASE_ROOM_HEIGHT - 1, z, floor);
            }
        }

        int center = STANDARD_SIZE / 2;

        // 1. 中央能量核心
        s.setBlockState(center, 1, center, core);
        for (int y = 2; y < STAIRCASE_ROOM_HEIGHT - 1; y++) {
            // 使用紫色玻璃模拟能量光束
            s.setBlockState(center, y, center, Blocks.STAINED_GLASS.getStateFromMeta(10));
        }

        // 2. 上升平台 (北侧 Z-, Y=4)
        int upHeight = 4;
        for (int x = center - 5; x <= center + 5; x++) {
            for (int z = 3; z <= center - 3; z++) {
                s.setBlockState(x, upHeight, z, floor);
            }
        }
        // 上升核心标记
        s.setBlockState(center, upHeight, 6, upAccent);
        s.setBlockState(center, upHeight+1, 6, Blocks.SEA_LANTERN.getDefaultState());


        // 通往上升平台的楼梯 (宽度 3)
        for (int dx = -1; dx <= 1; dx++) {
            // Step 1 (Y=1)
            s.setBlockState(center + dx, 1, center - 2, stairs);
            // Step 2 (Y=2)
            s.setBlockState(center + dx, 2, center - 3, stairs);
            s.setBlockState(center + dx, 1, center - 3, stairs); // 填充
            // Step 3 (Y=3)
            s.setBlockState(center + dx, 3, center - 4, stairs);
            s.setBlockState(center + dx, 1, center - 4, stairs); // 填充
            s.setBlockState(center + dx, 2, center - 4, stairs); // 填充
        }


        // 3. 下降平台 (南侧 Z+, Y=0)
        // 下降平台保持在 Y=0，但使用不同材质区分
        for (int x = center - 5; x <= center + 5; x++) {
            for (int z = center + 3; z <= STANDARD_SIZE - 4; z++) {
                s.setBlockState(x, 0, z, Blocks.OBSIDIAN.getDefaultState());
            }
        }
        // 下降核心标记
        s.setBlockState(center, 0, STANDARD_SIZE - 7, downAccent);
        s.setBlockState(center, 1, STANDARD_SIZE - 7, Blocks.MAGMA.getDefaultState());


        // 4. 装饰柱 (使用原有的辅助函数)
        createEndPillar(s, 5, 5);
        createEndPillar(s, 20, 5);
        createEndPillar(s, 5, 20);
        createEndPillar(s, 20, 20);

        return s;
    }

    // ===== 楼梯房间新增辅助方法 (必须添加) =====

    /**
     * 【新增辅助函数】创建宏伟阶梯 (使用完整方块)
     */
    private static void createGrandStairs(Schematic s, int centerX, int centerZ, int platformHeight, EnumFacing facing, int width, IBlockState material) {
        int platformDepth = 4; // 平台边缘距离中心的距离 (对应 9x9 平台)

        for (int h = 1; h <= platformHeight; h++) {
            // 每一级台阶向外延伸一格
            int offset = platformHeight + 1 - h;

            for (int w = -width; w <= width; w++) {
                int x = centerX;
                int z = centerZ;

                // 根据方向确定坐标
                switch (facing) {
                    case NORTH: // Z-
                        x += w;
                        z -= (platformDepth + offset);
                        break;
                    case SOUTH: // Z+
                        x += w;
                        z += (platformDepth + offset);
                        break;
                    case EAST: // X+
                        x += (platformDepth + offset);
                        z += w;
                        break;
                    case WEST: // X-
                        x -= (platformDepth + offset);
                        z += w;
                        break;
                }

                // 边界检查
                if (x >= 0 && x < STANDARD_SIZE && z >= 0 && z < STANDARD_SIZE && h < STAIRCASE_ROOM_HEIGHT) {
                    s.setBlockState(x, h, z, material);
                    // 填充楼梯下方的空间
                    for (int y = 1; y < h; y++) {
                        // 检查是否为空，避免覆盖其他方向的楼梯
                        if (s.getBlockState(x, y, z).getBlock() == Blocks.AIR) {
                            s.setBlockState(x, y, z, material);
                        }
                    }
                }
            }
        }
    }

    /**
     * 【新增辅助函数】创建螺旋下降楼梯 (使用预定义路径确保平滑)
     */
    private static void createSpiralDescent(Schematic s, int center, int startY, IBlockState material) {
        // 螺旋路径坐标 (围绕 9x9 的坑，位于半径 5 的区域内)
        // 路径定义了楼梯的形状，顺时针下降
        int[][] path = {
                // 从北侧 (Z-) 开始
                {-1, -5}, {0, -5}, {1, -5},
                // 向东移动 (X+)
                {2, -5}, {3, -4}, {4, -3}, {5, -2},
                {5, -1}, {5, 0}, {5, 1},
                // 向南移动 (Z+)
                {5, 2}, {4, 3}, {3, 4}, {2, 5},
                {1, 5}, {0, 5}, {-1, 5},
                // 向西移动 (X-)
                {-2, 5}, {-3, 4}, {-4, 3}, {-5, 2},
                {-5, 1}, {-5, 0}, {-5, -1},
                // 向北移动 (Z-)
                {-5, -2}, {-4, -3}, {-3, -4}, {-2, -5}
                // 回到起点附近
        };

        int currentY = startY;
        int stepCounter = 0;

        // 沿着路径放置楼梯
        for (int[] coords : path) {
            int x = center + coords[0];
            int z = center + coords[1];

            // 每走 6 步下降 1 格 (更平缓)
            if (stepCounter > 0 && stepCounter % 6 == 0 && currentY > 1) {
                currentY--;
            }

            // 放置楼梯方块
            if (currentY < STAIRCASE_ROOM_HEIGHT) {
                s.setBlockState(x, currentY, z, material);

                // 清空楼梯上方的空间
                for (int y = currentY + 1; y <= startY + 3; y++) {
                    if (y < STAIRCASE_ROOM_HEIGHT) {
                        // 只有当方块不是空气时才清除（避免清除栏杆或装饰）
                        if (s.getBlockState(x, y, z) != Blocks.AIR.getDefaultState()) {
                            s.setBlockState(x, y, z, Blocks.AIR.getDefaultState());
                        }
                    }
                }
            }

            stepCounter++;
        }
    }
// 在 EnhancedRoomTemplates.java 中添加以下新方法和辅助函数：

    // ==================== 新增精致房间模板 ====================

    // -------------------- 1. 虚空观测室 (Void Observatory) --------------------

    /**
     * 宝藏/枢纽房间 - 虚空观测室 (Void Observatory)
     * 特色：算法生成的星空穹顶，中央望远镜，虚空主题。
     */
    public static Schematic voidObservatory() {
        // 观测室需要更高的高度来容纳穹顶
        int height = 14;
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) height, (short) STANDARD_SIZE);

        IBlockState floorMain = Blocks.PURPUR_BLOCK.getDefaultState();
        IBlockState floorAccent = Blocks.END_BRICKS.getDefaultState();
        IBlockState domeMaterial = Blocks.OBSIDIAN.getDefaultState();
        IBlockState domeGlass = Blocks.STAINED_GLASS.getStateFromMeta(15); // 黑色玻璃

        int center = STANDARD_SIZE / 2;

        // 地板 - 紫珀块和末地砖同心圆
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                // 使用中心偏移 0.5 来确保图案完美居中
                double dist = Math.sqrt(Math.pow(x - center + 0.5, 2) + Math.pow(z - center + 0.5, 2));
                s.setBlockState(x, 0, z, ((int)dist) % 4 == 0 ? floorAccent : floorMain);
            }
        }

        // 创建星空穹顶 (从 Y=3 开始，半径 12)
        createObservatoryDome(s, center, 3, center, 12, domeMaterial, domeGlass, height);

        // 中央望远镜结构
        createTelescope(s, center, 1, center);

        // 宝箱
        placeLootChest(s, center - 3, 1, center, "moremod:dungeon/dungeon_treasure_rare");
        placeLootChest(s, center + 3, 1, center, "moremod:dungeon/dungeon_treasure_rare");

        // 虚空主题守卫 (使用末影人或自定义虚空怪)
        placeSpawner(s, 4, 1, 4, "minecraft:enderman", 1);
        placeSpawner(s, 21, 1, 21, "minecraft:enderman", 1);

        return s;
    }

    // -------------------- 2. 水晶洞穴 (Crystal Cave) --------------------

    /**
     * 普通/宝藏房间 - 水晶洞穴 (Crystal Cave)
     * 特色：使用噪声函数生成有机地形，打破方正结构。
     */
    public static Schematic normalRoomCrystalCave() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState stone = Blocks.STONE.getDefaultState();
        IBlockState gravel = Blocks.GRAVEL.getDefaultState();
        IBlockState water = Blocks.WATER.getDefaultState();
        IBlockState crystal = Blocks.SEA_LANTERN.getDefaultState(); // 使用海晶灯模拟水晶

        // 1. 噪声生成起伏地形
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                // 使用简单的三角函数叠加模拟地形高度 (0-3)
                double noise = (Math.sin(x * 0.4) + Math.cos(z * 0.4)) * 0.8 + 1.5;
                int height = Math.max(0, Math.min(3, (int) noise));

                for (int y = 0; y <= height; y++) {
                    if (y == height) {
                        // 地表材质变化
                        if (rand.nextFloat() < 0.1) {
                            s.setBlockState(x, y, z, gravel);
                        } else if (height <= 1 && rand.nextFloat() < 0.15) {
                            // 低洼处积水
                            s.setBlockState(x, y, z, water);
                        } else {
                            s.setBlockState(x, y, z, stone);
                        }
                    } else {
                        s.setBlockState(x, y, z, stone);
                    }
                }
            }
        }

        // 2. 点缀水晶簇
        for (int i = 0; i < 6; i++) {
            int cx = 4 + rand.nextInt(18);
            int cz = 4 + rand.nextInt(18);
            // 找到地形高度并放置在上方
            int cy = getSurfaceHeight(s, cx, cz) + 1;
            createCrystalCluster(s, cx, cy, cz, crystal);
        }

        // 3. 钟乳石 (从天花板垂下)
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, STANDARD_HEIGHT - 1, z, stone); // 确保天花板是石头
                if (rand.nextFloat() < 0.05) {
                    int length = 1 + rand.nextInt(3);
                    for (int y = 0; y < length; y++) {
                        // 使用圆石墙模拟钟乳石
                        s.setBlockState(x, STANDARD_HEIGHT - 2 - y, z, Blocks.COBBLESTONE_WALL.getDefaultState());
                    }
                }
            }
        }

        // 4. 放置刷怪笼和宝箱 (根据地形高度调整，放置在地表上方)
        int spawnerX = 6, spawnerZ = 6;
        placeRandomSpawner(s, spawnerX, getSurfaceHeight(s, spawnerX, spawnerZ) + 1, spawnerZ, 1);
        spawnerX = 19; spawnerZ = 19;
        placeRandomSpawner(s, spawnerX, getSurfaceHeight(s, spawnerX, spawnerZ) + 1, spawnerZ, 1);

        int chestX = 13, chestZ = 13;
        placeLootChest(s, chestX, getSurfaceHeight(s, chestX, chestZ) + 1, chestZ, "moremod:dungeon/dungeon_treasure_rare");

        return s;
    }

    // -------------------- 3. 地狱裂隙 (Nether Breach) --------------------

    /**
     * 枢纽/普通房间 - 地狱裂隙 (Nether Breach)
     * 特色：使用距离衰减算法实现材质渐变混合，展现下界入侵效果。
     */
    public static Schematic netherBreach() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        int center = STANDARD_SIZE / 2;

        // 根据距离计算腐化程度
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                double dist = Math.sqrt(Math.pow(x - center + 0.5, 2) + Math.pow(z - center + 0.5, 2));
                // 腐化率随距离衰减 (最大半径 12)
                float corruption = (float) Math.max(0, 1.0 - dist / 12.0);

                // 地板材质过渡
                s.setBlockState(x, 0, z, getCorruptedBlock(corruption, rand.nextFloat()));

                // 天花板材质过渡
                s.setBlockState(x, STANDARD_HEIGHT - 1, z, getCorruptedBlock(corruption, rand.nextFloat()));

                // 随机火焰和岩浆 (靠近中心)
                if (corruption > 0.7 && rand.nextFloat() < 0.1) {
                    if (rand.nextFloat() < 0.3) {
                        s.setBlockState(x, 0, z, Blocks.LAVA.getDefaultState());
                    }
                    // 确保上方是空气再放置火焰
                    if (s.getBlockState(x, 1, z).getBlock() == Blocks.AIR) {
                        s.setBlockState(x, 1, z, Blocks.FIRE.getDefaultState());
                    }
                }
            }
        }

        // 中央损毁的传送门
        createRuinedPortal(s, center, 1, center);

        // 刷怪笼 (使用下界生物)
        placeSpawner(s, 4, 1, 4, "minecraft:zombie_pigman", 1);
        placeSpawner(s, 21, 1, 21, "minecraft:zombie_pigman", 1);
        placeSpawner(s, center - 3, 1, center, "minecraft:blaze", 1);
        placeSpawner(s, center + 3, 1, center, "minecraft:blaze", 1);

        placeLootChest(s, center, 1, center + 5, "moremod:dungeon/dungeon_hub");

        return s;
    }

    // -------------------- 4. 齿轮工坊 (Clockwork Workshop) --------------------

    /**
     * 普通/谜题房间 - 齿轮工坊 (Clockwork Workshop)
     * 特色：蒸汽朋克主题，使用几何算法生成齿轮图案。
     */
    public static Schematic clockworkWorkshop() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floorBase = Blocks.STONE.getStateFromMeta(5); // 磨制安山岩
        IBlockState gearStone = Blocks.COBBLESTONE.getDefaultState();
        IBlockState gearMetal = Blocks.IRON_BLOCK.getDefaultState();

        // 基础地板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                s.setBlockState(x, 0, z, floorBase);
            }
        }

        // 地板上的巨型齿轮
        createGear(s, 8, 0, 8, 5, gearStone);
        createGear(s, 18, 0, 18, 5, gearStone);
        createGear(s, 13, 0, 13, 3, gearMetal); // 中央金属齿轮

        // 墙壁上的装饰齿轮 (模拟垂直放置)
        // 检查边界，确保齿轮在房间内
        if (STANDARD_HEIGHT > 6) {
            createGear(s, 1, 4, 8, 2, gearMetal);
            createGear(s, 24, 4, 18, 2, gearMetal);
        }

        // 机械工作站
        s.setBlockState(6, 1, 20, Blocks.ANVIL.getDefaultState());
        s.setBlockState(7, 1, 20, Blocks.CRAFTING_TABLE.getDefaultState());
        s.setBlockState(8, 1, 20, Blocks.FURNACE.getDefaultState());

        // 活塞装饰
        s.setBlockState(20, 1, 6, Blocks.PISTON.getDefaultState());
        s.setBlockState(20, 2, 6, Blocks.PISTON.getDefaultState());
        s.setBlockState(20, 3, 6, Blocks.REDSTONE_BLOCK.getDefaultState());

        // 刷怪笼
        placeRandomSpawner(s, 13, 1, 5, 1);
        placeRandomSpawner(s, 13, 1, 20, 1);

        placeLootChest(s, 20, 1, 8, "moremod:dungeon/dungeon_normal");

        // 红石火把照明 (工业感)
        s.setBlockState(4, 3, 4, Blocks.REDSTONE_TORCH.getDefaultState());
        s.setBlockState(21, 3, 4, Blocks.REDSTONE_TORCH.getDefaultState());
        s.setBlockState(4, 3, 21, Blocks.REDSTONE_TORCH.getDefaultState());
        s.setBlockState(21, 3, 21, Blocks.REDSTONE_TORCH.getDefaultState());

        return s;
    }

    // -------------------- 5. 黑暗祭祀场 (Dark Ritual Chamber) --------------------

    /**
     * 宝藏/陷阱房间 - 黑暗祭祀场 (Dark Ritual Chamber)
     * 特色：使用算法在地板上精确绘制红石符号，邪恶氛围。
     */
    public static Schematic treasureRoomRitualChamber() {
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) STANDARD_HEIGHT, (short) STANDARD_SIZE);

        IBlockState floorMain = Blocks.OBSIDIAN.getDefaultState();
        IBlockState floorAccent = Blocks.NETHER_BRICK.getDefaultState();
        IBlockState ritualMarking = Blocks.REDSTONE_WIRE.getDefaultState(); // 红石粉末作为符号

        int center = STANDARD_SIZE / 2;

        // 地板和天花板
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                // 交错材质
                s.setBlockState(x, 0, z, ((x/2) + (z/2)) % 3 == 0 ? floorAccent : floorMain);
                s.setBlockState(x, STANDARD_HEIGHT - 1, z, floorMain);
            }
        }

        // 地板上的祭祀符号 (五角星)
        // 计算半径为 8 的五角星顶点坐标 (使用三角函数确保完美对称)
        int radius = 8;
        int[][] starPoints = new int[5][2];
        for (int i = 0; i < 5; i++) {
            // 角度计算，确保图案正对 (从正上方开始)
            double angle = Math.PI / 2 + (2 * Math.PI * i / 5);
            starPoints[i][0] = center + (int)(radius * Math.cos(angle));
            starPoints[i][1] = center + (int)(radius * Math.sin(angle));
        }

        // 绘制五角星的连线
        for (int i = 0; i < 5; i++) {
            int[] p1 = starPoints[i];
            // 五角星连线是连接 i 和 (i+2) % 5
            int[] p2 = starPoints[(i + 2) % 5];
            // 使用辅助函数绘制直线 (Bresenham's algorithm)
            drawLine(s, p1[0], p1[1], p2[0], p2[1], ritualMarking);
        }

        // 中央祭坛
        createDarkAltar(s, center, 1, center);

        // 祭坛上的宝藏 (高价值)
        placeLootChest(s, center, 3, center, "moremod:dungeon/dungeon_treasure_rare");

        // 四角火焰柱 (使用已有的辅助方法)
        createNetherPillar(s, 5, 5, STANDARD_HEIGHT);
        createNetherPillar(s, 20, 5, STANDARD_HEIGHT);
        createNetherPillar(s, 5, 20, STANDARD_HEIGHT);
        createNetherPillar(s, 20, 20, STANDARD_HEIGHT);

        // 隐藏的强力守卫
        placeRandomSpawner(s, center-6, 1, center, 3);
        placeRandomSpawner(s, center+6, 1, center, 3);

        // 氛围照明
        s.setBlockState(4, 3, center, Blocks.REDSTONE_TORCH.getDefaultState());
        s.setBlockState(21, 3, center, Blocks.REDSTONE_TORCH.getDefaultState());

        return s;
    }

    // -------------------- 6. 宏伟门厅 (Grand Foyer) --------------------

    /**
     * 枢纽房间 - 宏伟门厅 (Grand Foyer)
     * 特色：华丽的双层结构，带有环绕阳台和大型吊灯。
     */
    public static Schematic hubRoomGrandFoyer() {
        // 门厅需要更高的高度
        int height = 12;
        Schematic s = new Schematic((short) STANDARD_SIZE, (short) height, (short) STANDARD_SIZE);

        IBlockState floorMain = Blocks.QUARTZ_BLOCK.getDefaultState();
        IBlockState floorPattern = Blocks.QUARTZ_BLOCK.getStateFromMeta(1); // 錾制石英
        IBlockState pillar = Blocks.QUARTZ_BLOCK.getStateFromMeta(2); // 柱状石英
        IBlockState railing = Blocks.STAINED_GLASS_PANE.getStateFromMeta(11); // 蓝色玻璃板

        int center = STANDARD_SIZE / 2;

        // 华丽地板 (Y=0) 和 天花板 (Y=height-1)
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                // 使用距离函数创建菱形图案
                boolean isPattern = (Math.abs(x - center + 0.5) + Math.abs(z - center + 0.5)) % 5 == 0;
                s.setBlockState(x, 0, z, isPattern ? floorPattern : floorMain);
                s.setBlockState(x, height - 1, z, floorMain);
            }
        }

        // 二层阳台 (Y=5)
        int balconyHeight = 5;
        int balconyInnerEdge = 6;
        for (int x = 0; x < STANDARD_SIZE; x++) {
            for (int z = 0; z < STANDARD_SIZE; z++) {
                boolean isBalconyArea = (x < balconyInnerEdge || x >= STANDARD_SIZE - balconyInnerEdge ||
                        z < balconyInnerEdge || z >= STANDARD_SIZE - balconyInnerEdge);

                if (isBalconyArea) {
                    s.setBlockState(x, balconyHeight, z, floorMain);

                    // 阳台栏杆 (Y=6)
                    boolean isEdge = (x == balconyInnerEdge - 1 || x == STANDARD_SIZE - balconyInnerEdge ||
                            z == balconyInnerEdge - 1 || z == STANDARD_SIZE - balconyInnerEdge);

                    if (isEdge && x > 0 && x < STANDARD_SIZE -1 && z > 0 && z < STANDARD_SIZE -1) {
                        // 留出潜在连接点空隙
                        if (Math.abs(x-center+0.5) > 2 && Math.abs(z-center+0.5) > 2) {
                            s.setBlockState(x, balconyHeight + 1, z, railing);
                        }
                    }
                }
            }
        }

        // 支撑柱
        int[][] pillarPos = {{balconyInnerEdge-2, balconyInnerEdge-2},
                {STANDARD_SIZE-balconyInnerEdge+1, balconyInnerEdge-2},
                {balconyInnerEdge-2, STANDARD_SIZE-balconyInnerEdge+1},
                {STANDARD_SIZE-balconyInnerEdge+1, STANDARD_SIZE-balconyInnerEdge+1}};
        for (int[] pos : pillarPos) {
            for (int y = 1; y < balconyHeight; y++) {
                s.setBlockState(pos[0], y, pos[1], pillar);
            }
        }

        // 大型吊灯
        createChandelier(s, center, height - 2, center);

        // 补给
        placeLootChest(s, center - 5, 1, center, "moremod:dungeon/dungeon_hub");
        placeLootChest(s, center + 5, 1, center, "moremod:dungeon/dungeon_hub");

        return s;
    }


    // ==================== 新增辅助函数 (必须添加) ====================

    /**
     * 辅助方法：Bresenham 直线绘制算法 (用于祭祀符号)
     * 在 Y=1 绘制。
     */
    private static void drawLine(Schematic s, int x1, int z1, int x2, int z2, IBlockState state) {
        int dx = Math.abs(x2 - x1);
        int dz = Math.abs(z2 - z1);
        int sx = x1 < x2 ? 1 : -1;
        int sz = z1 < z2 ? 1 : -1;
        int err = dx - dz;

        // 确保起始坐标在范围内
        x1 = Math.max(0, Math.min(STANDARD_SIZE - 1, x1));
        z1 = Math.max(0, Math.min(STANDARD_SIZE - 1, z1));

        while (true) {
            // 检查 Y=1 是否是空气，防止覆盖其他方块
            if (s.getBlockState(x1, 1, z1).getBlock() == Blocks.AIR) {
                s.setBlockState(x1, 1, z1, state);
            }

            if (x1 == x2 && z1 == z2) break;
            int e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                z1 += sz;
            }

            // 边界检查
            if (x1 < 0 || x1 >= STANDARD_SIZE || z1 < 0 || z1 >= STANDARD_SIZE) break;
        }
    }

    /**
     * 辅助方法：创建黑暗祭坛
     */
    private static void createDarkAltar(Schematic s, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                s.setBlockState(x+dx, y, z+dz, Blocks.NETHER_BRICK.getDefaultState());
            }
        }
        s.setBlockState(x, y+1, z, Blocks.NETHER_BRICK_FENCE.getDefaultState());
        s.setBlockState(x, y+2, z, Blocks.NETHER_BRICK_FENCE.getDefaultState());
    }

    /**
     * 辅助方法：创建观测室穹顶 (球体生成算法)
     */
    private static void createObservatoryDome(Schematic s, int cx, int cy, int cz, int radius, IBlockState material, IBlockState glass, int maxHeight) {
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                // 只生成上半球
                for (int y = cy; y <= cy + radius; y++) {
                    // 检查坐标是否在 Schematic 范围内
                    if (x >= 0 && x < s.width && z >= 0 && z < s.length && y >= 0 && y < maxHeight) {
                        double distSq = Math.pow(x - cx, 2) + Math.pow(y - cy, 2) + Math.pow(z - cz, 2);

                        // 形成球壳 (距离在 radius-1.5 和 radius 之间)
                        if (distSq >= Math.pow(radius - 1.5, 2) && distSq <= Math.pow(radius, 2)) {
                            // 随机使用玻璃和实体方块，模拟星空纹理
                            if (rand.nextFloat() < 0.4) {
                                s.setBlockState(x, y, z, glass);
                            } else {
                                s.setBlockState(x, y, z, material);
                            }

                            // 随机放置“星星” (末地烛)
                            if (rand.nextFloat() < 0.08 && y > cy + 2) {
                                if (y > 0 && s.getBlockState(x, y-1, z).getBlock() == Blocks.AIR) {
                                    // 末地烛会自动朝下放置
                                    s.setBlockState(x, y-1, z, Blocks.END_ROD.getDefaultState());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 辅助方法：创建望远镜结构
     */
    private static void createTelescope(Schematic s, int x, int y, int z) {
        // 底座 (3x3 石英)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                s.setBlockState(x + dx, y, z + dz, Blocks.QUARTZ_BLOCK.getDefaultState());
            }
        }
        // 支架
        s.setBlockState(x, y + 1, z, Blocks.QUARTZ_BLOCK.getStateFromMeta(2));
        s.setBlockState(x, y + 2, z, Blocks.QUARTZ_BLOCK.getStateFromMeta(2));

        // 镜筒 (铁块，倾斜放置)
        for (int i = 0; i <= 4; i++) {
            // 向上倾斜，朝向 Z+ 方向
            s.setBlockState(x, y + 3 + (i/2), z + i, Blocks.IRON_BLOCK.getDefaultState());
        }
        s.setBlockState(x, y + 2, z - 1, Blocks.GLASS_PANE.getDefaultState()); // 目镜
        s.setBlockState(x, y + 5, z + 5, Blocks.GLASS.getDefaultState()); // 物镜
    }

    // 辅助方法：获取地表高度 (Y坐标)
    private static int getSurfaceHeight(Schematic s, int x, int z) {
        // 检查边界
        if (x < 0 || x >= s.width || z < 0 || z >= s.length) return 0;

        for (int y = STANDARD_HEIGHT - 1; y >= 0; y--) {
            IBlockState state = s.getBlockState(x, y, z);
            // 检查方块材质是否是固体或液体
            if (state.getMaterial().isSolid() || state.getMaterial().isLiquid()) {
                return y;
            }
        }
        return 0;
    }

    // 辅助方法：创建水晶簇
    private static void createCrystalCluster(Schematic s, int x, int y, int z, IBlockState crystal) {
        if (y < STANDARD_HEIGHT) s.setBlockState(x, y, z, crystal);
        if (rand.nextBoolean() && y + 1 < STANDARD_HEIGHT) s.setBlockState(x, y + 1, z, crystal);
        if (rand.nextBoolean() && x + 1 < STANDARD_SIZE) s.setBlockState(x + 1, y, z, crystal);
        if (rand.nextBoolean() && z + 1 < STANDARD_SIZE) s.setBlockState(x, y, z + 1, crystal);
    }

    /**
     * 辅助方法：根据腐化程度获取方块 (材质渐变混合)
     */
    private static IBlockState getCorruptedBlock(float corruption, float randomFactor) {
        if (randomFactor < corruption) {
            // 高度腐化区域
            if (corruption > 0.8) {
                return rand.nextFloat() < 0.2 ? Blocks.SOUL_SAND.getDefaultState() : Blocks.NETHERRACK.getDefaultState();
            } else if (corruption > 0.5) {
                return rand.nextFloat() < 0.5 ? Blocks.NETHER_BRICK.getDefaultState() : Blocks.NETHERRACK.getDefaultState();
            } else {
                return Blocks.NETHERRACK.getDefaultState();
            }
        } else {
            // 未腐化或轻微腐化区域
            if (randomFactor < corruption + 0.2) {
                return Blocks.STONEBRICK.getStateFromMeta(2); // 裂石砖
            } else if (randomFactor < corruption + 0.4) {
                return Blocks.COBBLESTONE.getDefaultState();
            } else {
                return Blocks.STONEBRICK.getDefaultState();
            }
        }
    }

    /**
     * 辅助方法：创建损毁的传送门
     */
    private static void createRuinedPortal(Schematic s, int x, int y, int z) {
        // 6x5 的大型传送门框架
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                boolean isFrame = (Math.abs(dx) == 3 || dy == 0 || dy == 5);
                if (isFrame) {
                    // 随机移除一些黑曜石，模拟损毁 (不移除底座)
                    if (rand.nextFloat() > 0.7 && (dy != 0)) continue;
                    s.setBlockState(x + dx, y + dy, z, Blocks.OBSIDIAN.getDefaultState());
                }
            }
        }
        // 哭泣的黑曜石 (如果模组中有的话可以添加，这里用普通黑曜石代替)
        s.setBlockState(x, y+1, z, Blocks.OBSIDIAN.getDefaultState());
    }

    /**
     * 辅助方法：创建齿轮图案 (2D 几何生成)
     */
    private static void createGear(Schematic s, int cx, int y, int cz, int radius, IBlockState material) {
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                // 检查边界
                if (x >= 0 && x < s.width && z >= 0 && z < s.length && y >= 0 && y < s.height) {
                    double dist = Math.sqrt(Math.pow(x - cx, 2) + Math.pow(z - cz, 2));

                    // 内圈
                    if (dist <= radius - 2) {
                        s.setBlockState(x, y, z, material);
                    }
                    // 外圈和齿
                    else if (dist <= radius) {
                        // 计算角度 (使用 atan2)，用于生成齿
                        double angle = Math.atan2(z - cz, x - cx);
                        int numTeeth = radius * 2;
                        // 使用模运算判断当前角度是否在齿的范围内
                        boolean isTooth = (int)(angle * numTeeth / Math.PI) % 2 == 0;

                        if (isTooth || dist <= radius - 1) {
                            s.setBlockState(x, y, z, material);
                        }
                    }
                }
            }
        }
    }

    /**
     * 辅助方法：创建大型吊灯
     */
    private static void createChandelier(Schematic s, int x, int y, int z) {
        IBlockState chain = Blocks.OAK_FENCE.getDefaultState(); // 栅栏作为锁链
        IBlockState light = Blocks.GLOWSTONE.getDefaultState();

        // 中央吊挂
        s.setBlockState(x, y, z, chain);
        s.setBlockState(x, y-1, z, chain);
        s.setBlockState(x, y-2, z, light);

        // 第一层分支 (十字形)
        for (int i = 0; i < 4; i++) {
            int dx = (i % 2 == 0) ? (i == 0 ? 1 : -1) : 0;
            int dz = (i % 2 != 0) ? (i == 1 ? 1 : -1) : 0;

            s.setBlockState(x + dx, y-2, z + dz, chain);
            s.setBlockState(x + 2*dx, y-2, z + 2*dz, chain);
            s.setBlockState(x + 2*dx, y-3, z + 2*dz, light);
        }

        // 第二层分支 (对角线)
        for (int dx = -1; dx <= 1; dx+=2) {
            for (int dz = -1; dz <= 1; dz+=2) {
                s.setBlockState(x + dx, y-3, z + dz, chain);
                s.setBlockState(x + 2*dx, y-4, z + 2*dz, light);
            }
        }
    }
    /**
     * 楼梯房间 - 双向传送
     */


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

        // 四个刷怪笼入口 (Y=1 放置在地板上方)
        placeRandomSpawner(s, center, 1, 3, 2);
        placeRandomSpawner(s, center, 1, 22, 2);
        placeRandomSpawner(s, 3, 1, center, 2);
        placeRandomSpawner(s, 22, 1, center, 2);

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
