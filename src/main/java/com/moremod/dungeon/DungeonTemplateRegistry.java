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

    // 设为 true 启用从资源加载用户自定义 schematic 模板
    private static final boolean LOAD_RESOURCE_TEMPLATES = true;
    // 设为 false 可彻底禁用磁盘上的旧 .schematic 模板
    private static final boolean LOAD_FILE_TEMPLATES = false;

    // 用户自定义 schematic 房间类型映射
    // 1=入口, 2=藏宝, 3=普通, 4=怪物, 5=出口, 6-9=陷阱, 10=枢纽
    private static final Map<Integer, DungeonTree.RoomType> SCHEMATIC_ROOM_TYPE_MAP = new HashMap<>();
    static {
        SCHEMATIC_ROOM_TYPE_MAP.put(1, DungeonTree.RoomType.ENTRANCE);
        SCHEMATIC_ROOM_TYPE_MAP.put(2, DungeonTree.RoomType.TREASURE);
        SCHEMATIC_ROOM_TYPE_MAP.put(3, DungeonTree.RoomType.NORMAL);
        SCHEMATIC_ROOM_TYPE_MAP.put(4, DungeonTree.RoomType.MONSTER);
        SCHEMATIC_ROOM_TYPE_MAP.put(5, DungeonTree.RoomType.EXIT);
        SCHEMATIC_ROOM_TYPE_MAP.put(6, DungeonTree.RoomType.TRAP);
        SCHEMATIC_ROOM_TYPE_MAP.put(7, DungeonTree.RoomType.TRAP);
        SCHEMATIC_ROOM_TYPE_MAP.put(8, DungeonTree.RoomType.TRAP);
        SCHEMATIC_ROOM_TYPE_MAP.put(9, DungeonTree.RoomType.TRAP);
        SCHEMATIC_ROOM_TYPE_MAP.put(10, DungeonTree.RoomType.HUB);
    }

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
        if (LOAD_RESOURCE_TEMPLATES) loadFromResources();
        if (LOAD_FILE_TEMPLATES) loadFromFiles();
        ensureAllBuiltins(); // 无论是否读磁盘，都保证箱内模板齐全
    }

    /**
     * 从 mod 资源加载用户自定义 schematic 模板
     * 路径: assets/moremod/schematics/dungeon/1.schematic ~ 10.schematic
     */
    private void loadFromResources() {
        for (Map.Entry<Integer, DungeonTree.RoomType> entry : SCHEMATIC_ROOM_TYPE_MAP.entrySet()) {
            int num = entry.getKey();
            DungeonTree.RoomType type = entry.getValue();
            String resourcePath = "/assets/moremod/schematics/dungeon/" + num + ".schematic";

            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    System.err.println("[DungeonTemplateRegistry] 未找到资源: " + resourcePath);
                    continue;
                }
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(is);
                Schematic schematic = Schematic.loadFromNBT(nbt);
                templatesByType.get(type).add(schematic);
                System.out.println("[DungeonTemplateRegistry] 加载自定义模板: " + num + ".schematic -> " + type);
            } catch (Exception e) {
                System.err.println("[DungeonTemplateRegistry] 无法加载资源模板: " + resourcePath + " - " + e.getMessage());
            }
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
