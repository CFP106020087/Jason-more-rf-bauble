package com.moremod.dungeon.tree;

import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.util.*;

public class DungeonTree {

    public static class DungeonNode {
        public final String nodeId;
        public final BlockPos position;
        public RoomType type;
        public final DungeonNode parent;
        public final List<DungeonNode> children = new ArrayList<>();
        public final Map<String, BlockPos> portalPositions = new HashMap<>();

        public DungeonNode(String id, BlockPos pos, RoomType type, DungeonNode parent) {
            this.nodeId = id;
            this.position = pos;
            this.type = type;
            this.parent = parent;
        }

        public NBTTagCompound serialize() {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setString("id", nodeId);
            nbt.setLong("pos", position.toLong());
            nbt.setString("type", type.name());

            NBTTagList childList = new NBTTagList();
            for (DungeonNode child : children) {
                childList.appendTag(child.serialize());
            }
            nbt.setTag("children", childList);

            NBTTagCompound portals = new NBTTagCompound();
            for (Map.Entry<String, BlockPos> entry : portalPositions.entrySet()) {
                portals.setLong(entry.getKey(), entry.getValue().toLong());
            }
            nbt.setTag("portals", portals);

            return nbt;
        }

        public static DungeonNode deserialize(NBTTagCompound nbt, DungeonNode parent) {
            String id = nbt.getString("id");
            BlockPos pos = BlockPos.fromLong(nbt.getLong("pos"));
            RoomType type = RoomType.valueOf(nbt.getString("type"));

            DungeonNode node = new DungeonNode(id, pos, type, parent);

            NBTTagList childList = nbt.getTagList("children", 10);
            for (int i = 0; i < childList.tagCount(); i++) {
                node.children.add(deserialize(childList.getCompoundTagAt(i), node));
            }

            NBTTagCompound portals = nbt.getCompoundTag("portals");
            for (String key : portals.getKeySet()) {
                node.portalPositions.put(key, BlockPos.fromLong(portals.getLong(key)));
            }

            return node;
        }
    }

    public enum RoomType {
        ENTRANCE(1.0f),
        EXIT(1.0f),              // 出口房间
        NORMAL(0.5f),
        TREASURE(0.15f),
        TRAP(0.2f),
        MINI_BOSS(0.1f),  // 道中Boss
        BOSS(0.05f),
        HUB(0.1f),
        MONSTER(0.3f),           // 怪物房间
        // 三维地牢楼梯房间
        STAIRCASE_UP(0.3f),      // 只能往上
        STAIRCASE_DOWN(0.3f),    // 只能往下
        STAIRCASE_BOTH(0.3f);    // 可上可下

        public final float weight;
        RoomType(float weight) { this.weight = weight; }
    }

    public DungeonNode root;
    public final String dungeonId;
    public final long seed;

    public DungeonTree(String id, long seed) {
        this.dungeonId = id;
        this.seed = seed;
    }
}