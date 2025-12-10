package com.moremod.dungeon;

import com.moremod.dungeon.schematic.BoxRoomTemplates;
import com.moremod.dungeon.schematic.EnhancedRoomTemplates;
import com.moremod.dungeon.tree.DungeonTree;
import com.moremod.schematic.Schematic;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class DungeonTemplateRegistry {

    private static DungeonTemplateRegistry INSTANCE;
    private final Map<DungeonTree.RoomType, List<Schematic>> templatesByType = new HashMap<>();
    private final Random random = new Random();

    // 设为 false 可彻底禁用磁盘上的旧 .schematic 模板
    private static final boolean LOAD_FILE_TEMPLATES = false;

    private DungeonTemplateRegistry() {
        for (DungeonTree.RoomType type : DungeonTree.RoomType.values()) {
            templatesByType.put(type, new ArrayList<>());
        }
        loadTemplates();
    }

    public static DungeonTemplateRegistry getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DungeonTemplateRegistry();
        }
        return INSTANCE;
    }

    /** 开发期热重载 */
    public static void reloadTemplates() {
        if (INSTANCE != null) {
            for (List<Schematic> list : INSTANCE.templatesByType.values()) list.clear();
            INSTANCE.loadTemplates();
            System.out.println("[DungeonTemplateRegistry] Reloaded templates.");
        }
    }

    public Schematic getRandomTemplate(DungeonTree.RoomType type) {
        List<Schematic> templates = templatesByType.get(type);
        if (templates == null || templates.isEmpty()) {
            ensureBuiltinsFor(type); // 兜底：箱内模板
            templates = templatesByType.get(type);
        }
        if (templates == null || templates.isEmpty()) {
            System.err.println("[DungeonTemplateRegistry] 警告: 类型 " + type + " 没有可用模板，使用NORMAL替代");
            templates = templatesByType.get(DungeonTree.RoomType.NORMAL);
            if (templates == null || templates.isEmpty()) {
                ensureBuiltinsFor(DungeonTree.RoomType.NORMAL);
                templates = templatesByType.get(DungeonTree.RoomType.NORMAL);
            }
        }
        int i = random.nextInt(templates.size());
        return templates.get(i);
    }

    private void loadTemplates() {
        if (LOAD_FILE_TEMPLATES) loadFromFiles();
        loadFromResources(); // 从 resources 加载 .schem 文件
        ensureAllBuiltins(); // 无论是否读磁盘，都保证箱内模板齐全
    }

    /**
     * 从 mod resources 目录加载 .schem 文件
     * 路径: assets/moremod/schematics/
     *
     * dungeon_room.schem 是一个包含多个房间的大型 schematic (186x16x71)
     * 需要分割成 10 个独立的房间模板
     */
    private void loadFromResources() {
        try {
            String resourcePath = "/assets/moremod/schematics/dungeon_room.schem";
            InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is != null) {
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(is);
                Schematic fullSchematic = Schematic.loadFromNBT(nbt);
                is.close();

                System.out.println("[DungeonTemplateRegistry] 加载大型 schematic: " + fullSchematic.width + "x" + fullSchematic.height + "x" + fullSchematic.length);

                // 定义房间布局: {startX, startZ, width, length, roomType}
                // 用户自定义房间: HUB, ENTRANCE, NORMAL, TREASURE, TRAP×4, MONSTER, EXIT
                // 房间沿 X 轴排列，分两排
                int[][] roomDefinitions = {
                    // 上排房间 (Z = 0-30) - 从左到右: HUB, ENTRANCE, NORMAL, TREASURE, TRAP
                    {0, 0, 30, 30, 0},      // HUB
                    {30, 0, 30, 30, 1},     // ENTRANCE
                    {60, 0, 30, 30, 2},     // NORMAL
                    {90, 0, 30, 30, 3},     // TREASURE
                    {120, 0, 30, 30, 4},    // TRAP #1
                    // 下排房间 (Z = 35-65) - 从左到右: TRAP, TRAP, TRAP, MONSTER, EXIT
                    {0, 35, 30, 30, 4},     // TRAP #2
                    {30, 35, 30, 30, 4},    // TRAP #3
                    {60, 35, 30, 30, 4},    // TRAP #4
                    {90, 35, 30, 30, 5},    // MONSTER
                    {120, 35, 30, 30, 6},   // EXIT
                };

                // 房间类型映射 (索引对应 roomDefinitions 中的 typeIdx)
                // 0=HUB, 1=ENTRANCE, 2=NORMAL, 3=TREASURE, 4=TRAP, 5=MONSTER, 6=EXIT
                DungeonTree.RoomType[] types = {
                    DungeonTree.RoomType.HUB,        // 0
                    DungeonTree.RoomType.ENTRANCE,   // 1
                    DungeonTree.RoomType.NORMAL,     // 2
                    DungeonTree.RoomType.TREASURE,   // 3
                    DungeonTree.RoomType.TRAP,       // 4
                    DungeonTree.RoomType.MONSTER,    // 5
                    DungeonTree.RoomType.EXIT,       // 6
                };

                // 提取每个房间
                for (int[] def : roomDefinitions) {
                    int startX = def[0];
                    int startZ = def[1];
                    int roomWidth = def[2];
                    int roomLength = def[3];
                    int typeIdx = def[4];

                    // 确保不越界
                    if (startX + roomWidth > fullSchematic.width) {
                        roomWidth = fullSchematic.width - startX;
                    }
                    if (startZ + roomLength > fullSchematic.length) {
                        roomLength = fullSchematic.length - startZ;
                    }

                    if (roomWidth <= 0 || roomLength <= 0) continue;

                    Schematic roomSchematic = fullSchematic.extractRegion(
                        startX, 0, startZ,
                        roomWidth, fullSchematic.height, roomLength
                    );

                    if (typeIdx < types.length) {
                        DungeonTree.RoomType roomType = types[typeIdx];
                        templatesByType.get(roomType).add(roomSchematic);
                        System.out.println("[DungeonTemplateRegistry] 提取房间 [" + startX + "," + startZ + "] "
                            + roomWidth + "x" + fullSchematic.height + "x" + roomLength + " -> " + roomType);
                    }
                }
            } else {
                System.out.println("[DungeonTemplateRegistry] 未找到 resources 模板: " + resourcePath);
            }
        } catch (Exception e) {
            System.err.println("[DungeonTemplateRegistry] 加载 resources 模板失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadFromFiles() {
        File dir = new File("config/moremod/dungeons/");
        if (!dir.exists()) { dir.mkdirs(); return; }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".schematic"));
        if (files == null) return;

        for (File file : files) {
            try {
                String fileName = file.getName().replace(".schematic", "");
                DungeonTree.RoomType type = parseRoomType(fileName);
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(new FileInputStream(file));
                Schematic schematic = Schematic.loadFromNBT(nbt);
                templatesByType.get(type).add(schematic);
                System.out.println("加载地牢模板: " + fileName + " -> " + type);
            } catch (Exception e) {
                System.err.println("无法加载模板: " + file.getName());
            }
        }
    }

    private DungeonTree.RoomType parseRoomType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.contains("entrance")) return DungeonTree.RoomType.ENTRANCE;
        if (lower.contains("treasure")) return DungeonTree.RoomType.TREASURE;
        if (lower.contains("trap"))     return DungeonTree.RoomType.TRAP;
        if (lower.contains("boss"))     return DungeonTree.RoomType.BOSS;
        if (lower.contains("hub"))      return DungeonTree.RoomType.HUB;
        return DungeonTree.RoomType.NORMAL;
    }

    private void ensureAllBuiltins() {
        for (DungeonTree.RoomType t : DungeonTree.RoomType.values()) ensureBuiltinsFor(t);
    }

    private void ensureBuiltinsFor(DungeonTree.RoomType type) {
        List<Schematic> list = templatesByType.get(type);
        if (list == null || !list.isEmpty()) return;

        switch (type) {
            case ENTRANCE:
                list.add(EnhancedRoomTemplates.entranceRoom());
                list.add(EnhancedRoomTemplates.entranceRoomRuins());
                list.add(EnhancedRoomTemplates.entranceRoomTemple());
                break;
            case TREASURE:
                list.add(EnhancedRoomTemplates.treasureRoom());
                list.add(EnhancedRoomTemplates.treasureRoomVault());
                list.add(EnhancedRoomTemplates.treasureRoomRoyal());
                list.add(EnhancedRoomTemplates.treasureRoomOcean());
                list.add(EnhancedRoomTemplates.treasureRoomRitualChamber()); // 黑暗祭祀场
                list.add(EnhancedRoomTemplates.voidObservatory()); // 虚空观测室
                list.add(EnhancedRoomTemplates.normalRoomCrystalCave()); // 水晶洞穴 (稀有)
                break;
            case TRAP:
                list.add(EnhancedRoomTemplates.trapRoom());
                list.add(EnhancedRoomTemplates.trapRoomArrowCorridor());
                list.add(EnhancedRoomTemplates.trapRoomPitfall());
                list.add(EnhancedRoomTemplates.mazeRoom());
                list.add(EnhancedRoomTemplates.mazeRoomGarden());
                list.add(EnhancedRoomTemplates.treasureRoomRitualChamber()); // 黑暗祭祀场 (陷阱变种)
                break;
            case BOSS:
                list.add(EnhancedRoomTemplates.bossArena());
                break;
            case MINI_BOSS:
                // 道中Boss房间 - 召唤两只血量较低的VoidRipper
                list.add(EnhancedRoomTemplates.miniBossArena());
                list.add(EnhancedRoomTemplates.miniBossArenaDark());
                break;
            case HUB:
                list.add(EnhancedRoomTemplates.fountainRoom());
                list.add(EnhancedRoomTemplates.hubRoomCamp());
                list.add(EnhancedRoomTemplates.hubRoomLibrary());
                list.add(EnhancedRoomTemplates.netherBreach()); // 地狱裂隙
                list.add(EnhancedRoomTemplates.hubRoomGrandFoyer()); // 宏伟门厅
                list.add(EnhancedRoomTemplates.voidObservatory()); // 虚空观测室 (枢纽变种)
                break;
            case EXIT:
                // 出口房间 - 使用入口房间模板作为后备
                list.add(EnhancedRoomTemplates.entranceRoom());
                break;
            case MONSTER:
                // 怪物房间 - 使用战斗房间模板
                list.add(EnhancedRoomTemplates.combatRoom());
                list.add(EnhancedRoomTemplates.combatRoomArena());
                break;
            case NORMAL:
            default:
                list.add(EnhancedRoomTemplates.normalRoomAlchemy());
                list.add(EnhancedRoomTemplates.normalRoomGreenhouse());
                list.add(EnhancedRoomTemplates.normalRoomMine());
                list.add(EnhancedRoomTemplates.normalRoomStorage());
                list.add(EnhancedRoomTemplates.combatRoom());
                list.add(EnhancedRoomTemplates.combatRoomTrainingGround());
                list.add(EnhancedRoomTemplates.combatRoomColosseum());
                list.add(EnhancedRoomTemplates.combatRoomArena());
                list.add(EnhancedRoomTemplates.puzzleRoomMaze());
                list.add(EnhancedRoomTemplates.normalRoomCrystalCave()); // 水晶洞穴
                list.add(EnhancedRoomTemplates.clockworkWorkshop()); // 齿轮工坊
                list.add(EnhancedRoomTemplates.netherBreach()); // 地狱裂隙
                break;

            // 三维地牢楼梯房间
            case STAIRCASE_UP:
                list.add(EnhancedRoomTemplates.staircaseRoomUp());
                break;
            case STAIRCASE_DOWN:
                list.add(EnhancedRoomTemplates.staircaseRoomDown());
                break;
            case STAIRCASE_BOTH:
                list.add(EnhancedRoomTemplates.staircaseRoomBoth());
                break;
        }
    }
}
