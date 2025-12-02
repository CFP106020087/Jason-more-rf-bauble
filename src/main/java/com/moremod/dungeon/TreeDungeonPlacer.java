package com.moremod.dungeon;

import com.moremod.dungeon.DungeonTypes.*;
import com.moremod.dungeon.tree.DungeonTree;
import com.moremod.dungeon.crystal.ImprovedCrystalLinker;
import com.moremod.dungeon.loot.DungeonLootManager;
import com.moremod.util.ReflectionHelper;
import com.moremod.schematic.Schematic;
import com.moremod.init.ModBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class TreeDungeonPlacer {

    // 標準房間參數
    private static final int STANDARD_SHELL_SIZE   = 30;
    private static final int STANDARD_SHELL_HEIGHT = 12;

    // Mini-Boss房間參數（中等大小）
    private static final int MINI_BOSS_SHELL_SIZE   = 36;
    private static final int MINI_BOSS_SHELL_HEIGHT = 16;

    // Boss房間參數（更大）
    private static final int BOSS_SHELL_SIZE   = 40;
    private static final int BOSS_SHELL_HEIGHT = 18;

    private static final int THICK        = 2;
    private static final int INNER_Y      = 2;

    private final World world;
    private final ImprovedCrystalLinker crystalLinker;
    private final DungeonLootManager lootManager;
    private final Map<RoomNode, BlockPos> roomBases = new HashMap<>();
    private final Random random = new Random();

    // 刷怪箱實體列表
    private static final String[] SPAWNER_ENTITIES = {
            "moremod:curse_knight",
            "moremod:weeping_angel"
    };

    public TreeDungeonPlacer(World world) {
        this.world = world;
        this.crystalLinker = new ImprovedCrystalLinker(world);
        this.lootManager = DungeonLootManager.getInstance();
    }

    /**
     * 放置完整的地牢
     */
    public void placeDungeon(DungeonLayout layout) {
        System.out.println("[地牢生成] 開始放置地牢，房間數: " + layout.getRooms().size());

        // 第一步：放置所有房間
        for (RoomNode room : layout.getRooms()) {
            BlockPos base = layout.getCenter().add(room.position);
            roomBases.put(room, base);
            placeRoom(base, room);
        }

        // 第二步：使用改進的水晶連接系統
        crystalLinker.placeAndLinkCrystals(layout, roomBases);

        // 第三步：添加特殊連接（如秘密通道）
        addSpecialConnections(layout);

        System.out.println("[地牢生成] 地牢放置完成");
    }

    private RoomDimensions getRoomDimensions(RoomNode room) {
        if (room.type == RoomType.BOSS) {
            return new RoomDimensions(BOSS_SHELL_SIZE, BOSS_SHELL_HEIGHT);
        } else if (room.type == RoomType.MINI_BOSS) {
            return new RoomDimensions(MINI_BOSS_SHELL_SIZE, MINI_BOSS_SHELL_HEIGHT);
        } else {
            return new RoomDimensions(STANDARD_SHELL_SIZE, STANDARD_SHELL_HEIGHT);
        }
    }

    private static class RoomDimensions {
        final int size;
        final int height;

        RoomDimensions(int size, int height) {
            this.size = size;
            this.height = height;
        }
    }

    private void placeRoom(BlockPos base, RoomNode room) {
        RoomDimensions dims = getRoomDimensions(room);

        // 1) 生成房間外殼
        generateRoomShell(base, dims.size, dims.height);

        // 2) 放置房間模板
        Schematic template = DungeonTemplateRegistry.getInstance().getRandomTemplate(convert(room.type));
        template.place(world, base.getX() + THICK, base.getY() + INNER_Y, base.getZ() + THICK);

        // 3) 根據房間類型添加特定內容
        enhanceRoomContent(base, room, dims);

        // 4) 處理門洞（入口和出口）
        if (room.type == RoomType.ENTRANCE) {
            carveDoorway(base, dims.size, EntranceDirection.WEST);
        }
        if (room.type == RoomType.EXIT) {
            carveDoorway(base, dims.size, EntranceDirection.EAST);
        }
    }

    private void generateRoomShell(BlockPos base, int shellSize, int shellHeight) {
        IBlockState wallBlock = ModBlocks.UNBREAKABLE_BARRIER_ANCHOR.getDefaultState();

        for (int x = 0; x < shellSize; x++) {
            for (int y = 0; y < shellHeight; y++) {
                for (int z = 0; z < shellSize; z++) {
                    boolean isWall = x < THICK || x >= shellSize - THICK ||
                            y < THICK || y >= shellHeight - THICK ||
                            z < THICK || z >= shellSize - THICK;

                    IBlockState state = isWall ? wallBlock : Blocks.AIR.getDefaultState();
                    world.setBlockState(base.add(x, y, z), state, 2);
                }
            }
        }
    }

    private void enhanceRoomContent(BlockPos base, RoomNode room, RoomDimensions dims) {
        BlockPos innerBase = base.add(THICK, INNER_Y, THICK);
        int innerSize = dims.size - THICK * 2;

        switch (room.type) {
            case BOSS:
                // Boss房間不需要額外處理，模板已經包含箱子
                System.out.println("[Boss房間] 使用模板預設配置");
                break;

            case MINI_BOSS:
                // 道中Boss房間 - 模板已包含VoidRipper刷怪砖，無需額外處理
                System.out.println("[道中Boss房間] 使用模板預設配置 (VoidRipper刷怪砖)");
                break;

            case TREASURE:
                // 只添加守衛刷怪籠
                setupTreasureGuards(innerBase, innerSize);
                break;

            case TRAP:
                // 只設置陷阱機制
                setupTrapMechanics(innerBase, innerSize);
                break;

            case NORMAL:
            case MONSTER:
                // 只添加刷怪籠
                setupCombatSpawners(innerBase, innerSize);
                break;

            case ENTRANCE:
            case EXIT:
            case HUB:
                // 這些房間完全依賴模板
                break;
        }
    }

    private void setupTreasureGuards(BlockPos origin, int innerSize) {
        int center = innerSize / 2;
        placeRandomSpawner(origin.add(center, 0, 5), 1);
        placeRandomSpawner(origin.add(center, 0, innerSize - 6), 1);
    }

    private void setupTrapMechanics(BlockPos origin, int innerSize) {
        int center = innerSize / 2;
        BlockPos[] hiddenSpawners = {
                origin.add(center - 3, 0, center - 2),
                origin.add(center + 3, 0, center - 2)
        };

        for (BlockPos pos : hiddenSpawners) {
            placeRandomSpawner(pos, 1);
            world.setBlockState(pos.up(), Blocks.STONEBRICK.getDefaultState(), 2);
        }
    }

    private void setupCombatSpawners(BlockPos origin, int innerSize) {
        BlockPos[] spawnerPositions = {
                origin.add(5, 1, 5),
                origin.add(innerSize - 6, 1, 5),
                origin.add(innerSize / 2, 1, innerSize / 2)
        };

        for (BlockPos pos : spawnerPositions) {
            placeRandomSpawner(pos, 1);
        }
    }

    /**
     * 隨機放置刷怪箱（curse_knight 或 weeping_angel）
     */
    private void placeRandomSpawner(BlockPos pos, int tier) {
        // 隨機選擇實體
        String entityId = SPAWNER_ENTITIES[random.nextInt(SPAWNER_ENTITIES.length)];
        placeSpawner(pos, entityId, tier);
    }

    /**
     * 放置指定類型的刷怪箱
     */
    private void placeSpawner(BlockPos pos, String entityId, int tier) {
        world.setBlockState(pos, Blocks.MOB_SPAWNER.getDefaultState(), 2);

        TileEntityMobSpawner spawner = (TileEntityMobSpawner) world.getTileEntity(pos);
        if (spawner != null) {
            MobSpawnerBaseLogic logic = spawner.getSpawnerBaseLogic();
            logic.setEntityId(new ResourceLocation(entityId));

            // 使用反射輔助類設置參數
            ReflectionHelper.SpawnerConfig config;

            switch (tier) {
                case 1:
                    config = ReflectionHelper.SpawnerConfig.normal();
                    break;
                case 2:
                    config = ReflectionHelper.SpawnerConfig.hard();
                    break;
                case 3:
                    config = ReflectionHelper.SpawnerConfig.extreme();
                    break;
                case 4:
                    config = ReflectionHelper.SpawnerConfig.boss();
                    break;
                default:
                    config = ReflectionHelper.SpawnerConfig.normal();
            }

            ReflectionHelper.setSpawnerByNBT(logic, config);
            System.out.println("[刷怪箱] 放置 " + entityId + " 於 " + pos);
        }
    }

    private void carveDoorway(BlockPos base, int shellSize, EntranceDirection direction) {
        int midZ = shellSize / 2;

        for (int y = INNER_Y; y < INNER_Y + 4; y++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int t = 0; t < THICK; t++) {
                    BlockPos doorPos = null;

                    switch (direction) {
                        case WEST:
                            doorPos = base.add(t, y, midZ + dz);
                            break;
                        case EAST:
                            doorPos = base.add(shellSize - 1 - t, y, midZ + dz);
                            break;
                        case NORTH:
                            doorPos = base.add(midZ + dz, y, t);
                            break;
                        case SOUTH:
                            doorPos = base.add(midZ + dz, y, shellSize - 1 - t);
                            break;
                    }

                    if (doorPos != null) {
                        world.setBlockState(doorPos, Blocks.AIR.getDefaultState(), 3);
                    }
                }
            }
        }
    }

    private void addSpecialConnections(DungeonLayout layout) {
        for (RoomNode room : layout.getRooms()) {
            if (room.type == RoomType.TREASURE) {
                RoomNode nearest = findNearestRoomOfType(layout, room, RoomType.NORMAL);
                if (nearest != null) {
                    BlockPos treasureBase = roomBases.get(room);
                    BlockPos normalBase = roomBases.get(nearest);

                    if (treasureBase != null && normalBase != null) {
                        RoomDimensions normalDims = getRoomDimensions(nearest);

                        BlockPos secretCrystal1 = treasureBase.add(THICK + 1, INNER_Y + 1, THICK + 1);
                        BlockPos secretCrystal2 = normalBase.add(
                                normalDims.size - THICK - 2,
                                INNER_Y + 1,
                                normalDims.size - THICK - 2
                        );

                        crystalLinker.addSpecialConnection(secretCrystal1, secretCrystal2, "秘密通道");
                    }
                }
            }
        }
    }

    private RoomNode findNearestRoomOfType(DungeonLayout layout, RoomNode from, RoomType type) {
        RoomNode nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (RoomNode room : layout.getRooms()) {
            if (room != from && room.type == type) {
                double dist = from.position.getDistance(
                        room.position.getX(),
                        room.position.getY(),
                        room.position.getZ()
                );

                if (dist < minDistance) {
                    minDistance = dist;
                    nearest = room;
                }
            }
        }

        return nearest;
    }

    private DungeonTree.RoomType convert(RoomType type) {
        switch (type) {
            case ENTRANCE:  return DungeonTree.RoomType.ENTRANCE;
            case TREASURE:  return DungeonTree.RoomType.TREASURE;
            case TRAP:      return DungeonTree.RoomType.TRAP;
            case BOSS:      return DungeonTree.RoomType.BOSS;
            case MINI_BOSS: return DungeonTree.RoomType.MINI_BOSS;
            case HUB:       return DungeonTree.RoomType.HUB;
            default:        return DungeonTree.RoomType.NORMAL;
        }
    }

    private enum EntranceDirection {
        NORTH, SOUTH, EAST, WEST
    }
}