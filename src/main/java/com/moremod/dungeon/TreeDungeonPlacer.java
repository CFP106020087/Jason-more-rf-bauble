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
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.*;

public class TreeDungeonPlacer {

    // 標準房間參數 (加大版: 模板+5)
    private static final int STANDARD_SHELL_SIZE   = 35;
    private static final int STANDARD_SHELL_HEIGHT = 20;

    // Mini-Boss房間參數 (加大版)
    private static final int MINI_BOSS_SHELL_SIZE   = 41;
    private static final int MINI_BOSS_SHELL_HEIGHT = 24;

    // Boss房間參數 (加大版)
    private static final int BOSS_SHELL_SIZE   = 45;
    private static final int BOSS_SHELL_HEIGHT = 28;

    // 樓梯房間參數 (加大版)
    private static final int STAIRCASE_SHELL_SIZE   = 35;
    private static final int STAIRCASE_SHELL_HEIGHT = 22;

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
        } else if (room.isStaircase()) {
            return new RoomDimensions(STAIRCASE_SHELL_SIZE, STAIRCASE_SHELL_HEIGHT);
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
        try {
            RoomDimensions dims = getRoomDimensions(room);
            System.out.println("[房间放置] 类型=" + room.type + " 位置=" + base + " 尺寸=" + dims.size + "x" + dims.height);

            // 1) 生成房間外殼（先清空区域）
            generateRoomShell(base, dims.size, dims.height);

            // 2) 放置房間模板
            Schematic template = DungeonTemplateRegistry.getInstance().getRandomTemplate(convert(room.type));
            if (template == null) {
                System.err.println("[房间放置] 警告: 模板为null，跳过放置");
                return;
            }
            System.out.println("[房间放置] 模板尺寸=" + template.width + "x" + template.height + "x" + template.length);
            template.place(world, base.getX() + THICK, base.getY() + INNER_Y, base.getZ() + THICK);

            // 3) ⚠️ 重新密封墙壁（防止模板覆盖）
            sealRoomWalls(base, dims.size, dims.height);

            // 4) 根據房間類型添加特定內容
            enhanceRoomContent(base, room, dims);

            // 5) 處理門洞（入口和出口）
            if (room.type == RoomType.ENTRANCE) {
                carveDoorway(base, dims.size, EntranceDirection.WEST);
                // 6) 入口外部引導標識
                createEntranceMarkers(base, dims.size);
            }
            if (room.type == RoomType.EXIT) {
                carveDoorway(base, dims.size, EntranceDirection.EAST);
            }
            System.out.println("[房间放置] 完成: " + room.type);
        } catch (Exception e) {
            System.err.println("[房间放置] 失败: " + room.type + " - " + e.getMessage());
            e.printStackTrace();
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

    /**
     * 重新密封房间墙壁（在模板放置后调用）
     * 只覆盖边缘区域，不影响内部
     */
    private void sealRoomWalls(BlockPos base, int shellSize, int shellHeight) {
        IBlockState wallBlock = ModBlocks.UNBREAKABLE_BARRIER_ANCHOR.getDefaultState();

        for (int x = 0; x < shellSize; x++) {
            for (int y = 0; y < shellHeight; y++) {
                for (int z = 0; z < shellSize; z++) {
                    // 只处理边缘位置（墙壁区域）
                    boolean isWall = x < THICK || x >= shellSize - THICK ||
                            y < THICK || y >= shellHeight - THICK ||
                            z < THICK || z >= shellSize - THICK;

                    if (isWall) {
                        world.setBlockState(base.add(x, y, z), wallBlock, 2);
                    }
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
                // 添加守衛刷怪籠 + 戰利品箱
                setupTreasureGuards(innerBase, innerSize);
                setupTreasureLoot(innerBase, innerSize);
                break;

            case TRAP:
                // 設置陷阱機制 + 少量補償箱
                setupTrapMechanics(innerBase, innerSize);
                setupTrapLoot(innerBase, innerSize);
                break;

            case NORMAL:
                // 添加刷怪籠 + 普通戰利品箱
                setupCombatSpawners(innerBase, innerSize);
                setupNormalLoot(innerBase, innerSize);
                break;

            case MONSTER:
                // 添加更多刷怪籠 + 戰利品箱
                setupCombatSpawners(innerBase, innerSize);
                setupMonsterLoot(innerBase, innerSize);
                break;

            case ENTRANCE:
                // 入口房間 - 添加基礎補給箱
                setupEntranceLoot(innerBase, innerSize);
                break;

            case EXIT:
                // 出口房間 - 添加完成獎勵箱
                setupExitLoot(innerBase, innerSize);
                break;

            case HUB:
                // 樞紐房間 - 添加探索輔助箱
                setupHubLoot(innerBase, innerSize);
                break;

            case STAIRCASE_UP:
            case STAIRCASE_DOWN:
            case STAIRCASE_BOTH:
                // 樓梯房間 - 建立垂直傳送連接
                setupStaircaseRoom(innerBase, innerSize, room);
                break;
        }
    }

    /**
     * 設置樓梯房間的垂直傳送裝置
     */
    private void setupStaircaseRoom(BlockPos origin, int innerSize, RoomNode room) {
        int center = innerSize / 2;

        // 中央樓梯平台
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlockState(origin.add(center + dx, 0, center + dz),
                        Blocks.QUARTZ_BLOCK.getDefaultState(), 2);
            }
        }

        // 根據樓梯類型放置傳送水晶
        if (room.type == RoomType.STAIRCASE_UP || room.type == RoomType.STAIRCASE_BOTH) {
            // 向上的傳送點 (藍色標記)
            world.setBlockState(origin.add(center, 1, center - 2),
                    Blocks.LAPIS_BLOCK.getDefaultState(), 2);
            world.setBlockState(origin.add(center, 2, center - 2),
                    Blocks.TORCH.getDefaultState(), 2);

            // 放置連接水晶 - 由 CrystalLinker 處理跨層連接
            if (room.linkedStaircase != null) {
                BlockPos crystalPos = origin.add(center, 1, center - 1);
                crystalLinker.placeStaircaseCrystal(crystalPos, room, true);
            }
        }

        if (room.type == RoomType.STAIRCASE_DOWN || room.type == RoomType.STAIRCASE_BOTH) {
            // 向下的傳送點 (紅色標記)
            world.setBlockState(origin.add(center, 1, center + 2),
                    Blocks.REDSTONE_BLOCK.getDefaultState(), 2);
            world.setBlockState(origin.add(center, 2, center + 2),
                    Blocks.TORCH.getDefaultState(), 2);

            // 放置連接水晶
            if (room.linkedStaircase != null) {
                BlockPos crystalPos = origin.add(center, 1, center + 1);
                crystalLinker.placeStaircaseCrystal(crystalPos, room, false);
            }
        }

        // 四角裝飾柱
        placeStaircasePillar(origin.add(3, 0, 3));
        placeStaircasePillar(origin.add(innerSize - 4, 0, 3));
        placeStaircasePillar(origin.add(3, 0, innerSize - 4));
        placeStaircasePillar(origin.add(innerSize - 4, 0, innerSize - 4));

        // 樓層標識告示牌
        BlockPos signPos = origin.add(center, 2, center);
        world.setBlockState(signPos, Blocks.STANDING_SIGN.getDefaultState(), 2);

        // 設置告示牌文字
        TileEntitySign sign = (TileEntitySign) world.getTileEntity(signPos);
        if (sign != null) {
            sign.signText[0] = new TextComponentString("");
            sign.signText[1] = new TextComponentString("上下层中转站");
            sign.signText[2] = new TextComponentString("");
            sign.signText[3] = new TextComponentString("");
            sign.markDirty();
        }
    }

    private void placeStaircasePillar(BlockPos pos) {
        for (int y = 1; y <= 4; y++) {
            world.setBlockState(pos.up(y), Blocks.QUARTZ_BLOCK.getStateFromMeta(2), 2); // 柱狀石英
        }
        world.setBlockState(pos.up(5), Blocks.SEA_LANTERN.getDefaultState(), 2);
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

    // ========== 戰利品箱設置方法 ==========

    /**
     * 藏寶房 - 放置多個高級戰利品箱
     */
    private void setupTreasureLoot(BlockPos origin, int innerSize) {
        int center = innerSize / 2;
        // 中央主箱 (陷阱箱)
        lootManager.createSpecialChest(world, origin.add(center, 1, center), RoomType.TREASURE, true);
        // 四角輔助箱
        lootManager.createSpecialChest(world, origin.add(4, 1, 4), RoomType.TREASURE, false);
        lootManager.createSpecialChest(world, origin.add(innerSize - 5, 1, 4), RoomType.TREASURE, false);
        lootManager.createSpecialChest(world, origin.add(4, 1, innerSize - 5), RoomType.TREASURE, false);
        lootManager.createSpecialChest(world, origin.add(innerSize - 5, 1, innerSize - 5), RoomType.TREASURE, false);
        System.out.println("[藏寶房] 放置5個戰利品箱");
    }

    /**
     * 陷阱房 - 放置少量補償箱 (部分為陷阱箱)
     */
    private void setupTrapLoot(BlockPos origin, int innerSize) {
        int center = innerSize / 2;
        // 房間盡頭的補償箱 (50%概率是陷阱)
        boolean trapped = random.nextBoolean();
        lootManager.createSpecialChest(world, origin.add(center, 1, innerSize - 4), RoomType.TRAP, trapped);
        System.out.println("[陷阱房] 放置1個補償箱 (陷阱=" + trapped + ")");
    }

    /**
     * 普通房 - 放置1-2個普通戰利品箱
     */
    private void setupNormalLoot(BlockPos origin, int innerSize) {
        // 隨機位置放置1-2個箱子
        if (random.nextBoolean()) {
            lootManager.createSpecialChest(world, origin.add(5, 1, innerSize - 5), RoomType.NORMAL, false);
        }
        lootManager.createSpecialChest(world, origin.add(innerSize - 5, 1, 5), RoomType.NORMAL, false);
        System.out.println("[普通房] 放置戰利品箱");
    }

    /**
     * 怪物房 - 放置戰利品箱作為擊殺獎勵
     */
    private void setupMonsterLoot(BlockPos origin, int innerSize) {
        int center = innerSize / 2;
        // 房間中央放置獎勵箱
        lootManager.createSpecialChest(world, origin.add(center, 1, center - 3), RoomType.NORMAL, false);
        lootManager.createSpecialChest(world, origin.add(center, 1, center + 3), RoomType.NORMAL, false);
        System.out.println("[怪物房] 放置2個戰利品箱");
    }

    /**
     * 入口房 - 放置基礎補給箱
     */
    private void setupEntranceLoot(BlockPos origin, int innerSize) {
        int center = innerSize / 2;
        // 入口處放置起始補給
        lootManager.createSpecialChest(world, origin.add(center + 3, 1, center), RoomType.ENTRANCE, false);
        System.out.println("[入口房] 放置1個起始補給箱");
    }

    /**
     * 出口房 - 放置完成獎勵箱
     */
    private void setupExitLoot(BlockPos origin, int innerSize) {
        int center = innerSize / 2;
        // 出口處放置完成獎勵
        lootManager.createSpecialChest(world, origin.add(center, 1, center), RoomType.EXIT, false);
        lootManager.createSpecialChest(world, origin.add(center - 2, 1, center), RoomType.EXIT, false);
        lootManager.createSpecialChest(world, origin.add(center + 2, 1, center), RoomType.EXIT, false);
        System.out.println("[出口房] 放置3個完成獎勵箱");
    }

    /**
     * 樞紐房 - 放置探索輔助箱
     */
    private void setupHubLoot(BlockPos origin, int innerSize) {
        int center = innerSize / 2;
        // 樞紐中央放置補給箱
        lootManager.createSpecialChest(world, origin.add(center, 1, center - 4), RoomType.HUB, false);
        lootManager.createSpecialChest(world, origin.add(center, 1, center + 4), RoomType.HUB, false);
        System.out.println("[樞紐房] 放置2個探索輔助箱");
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

    /**
     * 在入口房間外部創建引導標識
     * 包含：信標柱、地面指示、光源
     */
    private void createEntranceMarkers(BlockPos base, int shellSize) {
        int midZ = shellSize / 2;

        // 入口在西側 (x = 0 方向)
        BlockPos entrancePos = base.add(-1, INNER_Y, midZ);

        // 創建引導路徑 (向西延伸)
        for (int i = 1; i <= 10; i++) {
            BlockPos pathPos = entrancePos.add(-i, 0, 0);
            // 地面標記 - 金塊路徑
            world.setBlockState(pathPos.down(), Blocks.GOLD_BLOCK.getDefaultState(), 2);
            // 兩側紅石燈
            if (i % 3 == 0) {
                world.setBlockState(pathPos.add(0, 0, -2), Blocks.GLOWSTONE.getDefaultState(), 2);
                world.setBlockState(pathPos.add(0, 0, 2), Blocks.GLOWSTONE.getDefaultState(), 2);
            }
        }

        // 入口標識塔 (距離入口10格)
        BlockPos towerBase = entrancePos.add(-10, -1, 0);

        // 塔基座 (3x3 黑曜石)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(towerBase.add(dx, 0, dz), Blocks.OBSIDIAN.getDefaultState(), 2);
            }
        }

        // 塔身 (海晶燈柱)
        for (int y = 1; y <= 8; y++) {
            world.setBlockState(towerBase.add(0, y, 0), Blocks.SEA_LANTERN.getDefaultState(), 2);
        }

        // 塔頂信標效果 (金塊 + 信標)
        world.setBlockState(towerBase.add(0, 9, 0), Blocks.GOLD_BLOCK.getDefaultState(), 2);
        world.setBlockState(towerBase.add(0, 10, 0), Blocks.BEACON.getDefaultState(), 2);

        // 四角裝飾火把
        world.setBlockState(towerBase.add(-2, 1, -2), Blocks.TORCH.getDefaultState(), 2);
        world.setBlockState(towerBase.add(-2, 1, 2), Blocks.TORCH.getDefaultState(), 2);
        world.setBlockState(towerBase.add(2, 1, -2), Blocks.TORCH.getDefaultState(), 2);
        world.setBlockState(towerBase.add(2, 1, 2), Blocks.TORCH.getDefaultState(), 2);

        System.out.println("[入口標識] 已創建入口引導標識於 " + towerBase);
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
            case ENTRANCE:       return DungeonTree.RoomType.ENTRANCE;
            case EXIT:           return DungeonTree.RoomType.EXIT;
            case TREASURE:       return DungeonTree.RoomType.TREASURE;
            case TRAP:           return DungeonTree.RoomType.TRAP;
            case BOSS:           return DungeonTree.RoomType.BOSS;
            case MINI_BOSS:      return DungeonTree.RoomType.MINI_BOSS;
            case HUB:            return DungeonTree.RoomType.HUB;
            case MONSTER:        return DungeonTree.RoomType.MONSTER;
            case STAIRCASE_UP:   return DungeonTree.RoomType.STAIRCASE_UP;
            case STAIRCASE_DOWN: return DungeonTree.RoomType.STAIRCASE_DOWN;
            case STAIRCASE_BOTH: return DungeonTree.RoomType.STAIRCASE_BOTH;
            default:             return DungeonTree.RoomType.NORMAL;
        }
    }

    private enum EntranceDirection {
        NORTH, SOUTH, EAST, WEST
    }
}