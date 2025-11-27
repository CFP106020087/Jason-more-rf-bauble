package com.moremod.dungeon.schematic;

import com.moremod.schematic.Schematic;
import com.moremod.init.ModBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.Random;

public class EnhancedRoomTemplates {

    private static final Random rand = new Random();
    private static final int BOSS_ROOM_SIZE = 36;
    private static final int BOSS_ROOM_HEIGHT = 16;

    // 刷怪箱實體列表
    private static final String[] SPAWNER_ENTITIES = {
            "moremod:curse_knight",
            "moremod:weeping_angel"
    };

    public static Schematic bossArena() {
        Schematic s = new Schematic((short) BOSS_ROOM_SIZE, (short) BOSS_ROOM_HEIGHT, (short) BOSS_ROOM_SIZE);

        IBlockState floorBlock = ModBlocks.UNBREAKABLE_BARRIER_ANCHOR.getDefaultState();
        IBlockState decorFloor = Blocks.STONE.getStateFromMeta(6);

        for (int x = 0; x < BOSS_ROOM_SIZE; x++) {
            for (int z = 0; z < BOSS_ROOM_SIZE; z++) {
                boolean checker = ((x / 3) + (z / 3)) % 2 == 0;
                s.setBlockState(x, 0, z, checker ? floorBlock : decorFloor);
                s.setBlockState(x, BOSS_ROOM_HEIGHT - 1, z, floorBlock);
            }
        }

        createPillar(s, 8, 8);
        createPillar(s, 8, 27);
        createPillar(s, 27, 8);
        createPillar(s, 27, 27);

        createBossAltar(s, BOSS_ROOM_SIZE / 2, 1, BOSS_ROOM_SIZE / 2);

        placeLootChest(s, BOSS_ROOM_SIZE/2 - 5, 1, BOSS_ROOM_SIZE/2, "moremod:dungeon/dungeon_boss");
        placeLootChest(s, BOSS_ROOM_SIZE/2 + 5, 1, BOSS_ROOM_SIZE/2, "moremod:dungeon/dungeon_boss");

        return s;
    }

    public static Schematic combatRoom() {
        Schematic s = new Schematic((short) 26, (short) 8, (short) 26);

        IBlockState floor = Blocks.STONEBRICK.getDefaultState();
        for (int x = 0; x < 26; x++) {
            for (int z = 0; z < 26; z++) {
                s.setBlockState(x, 0, z, floor);
            }
        }

        createCombatObstacles(s);

        // 隨機放置不同類型的刷怪籠
        placeRandomSpawner(s, 5, 1, 5, 1);
        placeRandomSpawner(s, 20, 1, 5, 1);
        placeRandomSpawner(s, 13, 1, 13, 2);
        placeRandomSpawner(s, 5, 1, 20, 1);
        placeRandomSpawner(s, 20, 1, 20, 1);

        placeLootChest(s, 13, 1, 20, "moremod:dungeon/dungeon_normal");

        return s;
    }

    public static Schematic mazeRoom() {
        Schematic s = new Schematic((short) 26, (short) 8, (short) 26);

        IBlockState floor = Blocks.STONE.getDefaultState();
        IBlockState wall = Blocks.STONEBRICK.getDefaultState();
        IBlockState unbreakableWall = ModBlocks.UNBREAKABLE_BARRIER_ANCHOR.getDefaultState();

        for (int x = 0; x < 26; x++) {
            for (int z = 0; z < 26; z++) {
                s.setBlockState(x, 0, z, floor);
                s.setBlockState(x, 7, z, floor);
            }
        }

        boolean[][] maze = generateMaze(26, 26);
        for (int x = 0; x < 26; x++) {
            for (int z = 0; z < 26; z++) {
                if (maze[x][z]) {
                    for (int y = 1; y <= 4; y++) {
                        boolean isEdge = (x == 0 || x == 25 || z == 0 || z == 25);
                        s.setBlockState(x, y, z, isEdge ? unbreakableWall : wall);
                    }
                }
            }
        }

        placeSpawnersInMaze(s, maze);
        placeLootChest(s, 13, 1, 13, "moremod:dungeon/dungeon_hub");

        return s;
    }

    public static Schematic trapRoom() {
        Schematic s = new Schematic((short) 26, (short) 8, (short) 26);

        IBlockState floor = Blocks.STONEBRICK.getDefaultState();
        for (int x = 0; x < 26; x++) {
            for (int z = 0; z < 26; z++) {
                s.setBlockState(x, 0, z, floor);
            }
        }

        createTrapCorridor(s, 8, 18, 13, true);
        createTrapCorridor(s, 13, 13, 8, false);

        createHiddenSpawnerRoom(s, 3, 3);
        createHiddenSpawnerRoom(s, 20, 3);
        createHiddenSpawnerRoom(s, 3, 20);
        createHiddenSpawnerRoom(s, 20, 20);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                s.setBlockState(13 + dx, 0, 13 + dz, Blocks.GLOWSTONE.getDefaultState());
            }
        }

        placeLootChest(s, 13, 1, 13, "moremod:dungeon/dungeon_trap");

        return s;
    }

    public static Schematic treasureRoom() {
        Schematic s = new Schematic((short) 26, (short) 8, (short) 26);

        for (int x = 0; x < 26; x++) {
            for (int z = 0; z < 26; z++) {
                boolean edge = (x < 2 || x >= 24 || z < 2 || z >= 24);
                s.setBlockState(x, 0, z, edge ?
                        Blocks.GOLD_BLOCK.getDefaultState() :
                        Blocks.STONE.getDefaultState());
            }
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    s.setBlockState(13 + dx, 1, 13 + dz, Blocks.QUARTZ_BLOCK.getDefaultState());
                }
            }
        }

        placeLootChest(s, 13, 2, 13, "moremod:dungeon/dungeon_treasure");
        placeLootChest(s, 11, 1, 13, "moremod:dungeon/dungeon_treasure");
        placeLootChest(s, 15, 1, 13, "moremod:dungeon/dungeon_treasure");

        // 寶藏房間守衛
        placeRandomSpawner(s, 13, 0, 5, 2);
        placeRandomSpawner(s, 13, 0, 20, 2);

        return s;
    }

    /**
     * 隨機放置刷怪籠（curse_knight 或 weeping_angel）
     */
    private static void placeRandomSpawner(Schematic s, int x, int y, int z, int difficulty) {
        String entityId = SPAWNER_ENTITIES[rand.nextInt(SPAWNER_ENTITIES.length)];
        placeSpawner(s, x, y, z, entityId, difficulty);
    }

    /**
     * 放置指定類型的刷怪籠
     */
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

    // 輔助方法保持不變
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
        s.setBlockState(x, y + 1, z, Blocks.BEACON.getDefaultState());
    }

    private static void createCombatObstacles(Schematic s) {
        IBlockState cover = Blocks.COBBLESTONE_WALL.getDefaultState();
        IBlockState fullCover = Blocks.COBBLESTONE.getDefaultState();

        for (int i = 0; i < 12; i++) {
            int x = 3 + rand.nextInt(20);
            int z = 3 + rand.nextInt(20);
            int type = rand.nextInt(3);

            switch (type) {
                case 0:
                    s.setBlockState(x, 1, z, cover);
                    s.setBlockState(x + 1, 1, z, cover);
                    s.setBlockState(x, 1, z + 1, cover);
                    break;
                case 1:
                    s.setBlockState(x, 1, z, fullCover);
                    s.setBlockState(x, 2, z, fullCover);
                    break;
                case 2:
                    s.setBlockState(x, 1, z, cover);
                    s.setBlockState(x - 1, 1, z, cover);
                    s.setBlockState(x + 1, 1, z, cover);
                    break;
            }
        }
    }

    private static boolean[][] generateMaze(int width, int height) {
        boolean[][] maze = new boolean[width][height];

        for (int x = 0; x < width; x++) {
            maze[x][0] = true;
            maze[x][height - 1] = true;
        }
        for (int z = 0; z < height; z++) {
            maze[0][z] = true;
            maze[width - 1][z] = true;
        }

        divideMaze(maze, 1, 1, width - 2, height - 2, rand);

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
        // 迷宮中也隨機放置不同類型的刷怪籠
        if (!maze[7][7]) placeRandomSpawner(s, 7, 1, 7, 1);
        if (!maze[18][7]) placeRandomSpawner(s, 18, 1, 7, 1);
        if (!maze[7][18]) placeRandomSpawner(s, 7, 1, 18, 1);
        if (!maze[18][18]) placeRandomSpawner(s, 18, 1, 18, 1);
    }

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
}