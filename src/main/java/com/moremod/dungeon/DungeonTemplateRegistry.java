package com.moremod.dungeon;

import com.moremod.dungeon.schematic.BoxRoomTemplates;
import com.moremod.dungeon.tree.DungeonTree;
import com.moremod.schematic.Schematic;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import java.io.File;
import java.io.FileInputStream;
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
        if (templates.isEmpty()) {
            ensureBuiltinsFor(type); // 兜底：箱内模板
            templates = templatesByType.get(type);
        }
        int i = random.nextInt(templates.size());
        return templates.get(i);
    }

    private void loadTemplates() {
        if (LOAD_FILE_TEMPLATES) loadFromFiles();
        ensureAllBuiltins(); // 无论是否读磁盘，都保证箱内模板齐全
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
                list.add(BoxRoomTemplates.entrance());
                break;
            case TREASURE:
                list.add(BoxRoomTemplates.treasure());
                break;
            case TRAP:
                list.add(BoxRoomTemplates.miniMaze());
                list.add(BoxRoomTemplates.trapRoom()); // 新增陷阱房间变体
                break;
            case BOSS:
                list.add(BoxRoomTemplates.arena());
                break;
            case HUB:
                list.add(BoxRoomTemplates.fountain());
                break;
            case NORMAL:
            default:
                list.add(BoxRoomTemplates.library());
                list.add(BoxRoomTemplates.alchemy());
                list.add(BoxRoomTemplates.greenhouse());
                list.add(BoxRoomTemplates.combatRoom()); // 新增战斗房间
                break;
        }
    }
}
