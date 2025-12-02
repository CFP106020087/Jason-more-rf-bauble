// 文件: com/moremod/dungeon/DungeonTypes.java
package com.moremod.dungeon;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/**
 * 地牢系统的所有核心类型定义
 * 注意：所有类都在同一个包中，避免循环依赖
 */
public class DungeonTypes {

    public enum RoomType {
        ENTRANCE("entrance", 1.0),
        EXIT("exit", 1.0),
        HUB("hub", 0.8),
        NORMAL("normal", 1.0),
        TREASURE("treasure", 0.6),
        TRAP("trap", 0.4),
        MONSTER("monster", 0.7),
        PUZZLE("puzzle", 0.5),
        MINI_BOSS("mini_boss", 0.3),  // 道中Boss房间
        BOSS("boss", 0.2),
        // 三维地牢楼梯房间
        STAIRCASE_UP("staircase_up", 0.5),      // 只能往上
        STAIRCASE_DOWN("staircase_down", 0.5),  // 只能往下
        STAIRCASE_BOTH("staircase_both", 0.5);  // 可上可下

        public final String name;
        public final double weight;

        RoomType(String name, double weight) {
            this.name = name;
            this.weight = weight;
        }
    }

    public enum Rotation {
        NONE(0),
        CLOCKWISE_90(90),
        CLOCKWISE_180(180),
        COUNTERCLOCKWISE_90(270);

        public final int degrees;

        Rotation(int degrees) {
            this.degrees = degrees;
        }
    }

    public enum ConnectionType {
        NORMAL,
        SECRET,
        LOCKED,
        ONE_WAY
    }

    public static class BoundingBox {
        public final int minX, minY, minZ;
        public final int maxX, maxY, maxZ;
        private final String id;

        public BoundingBox(BlockPos pos1, BlockPos pos2) {
            this(pos1.getX(), pos1.getY(), pos1.getZ(),
                    pos2.getX(), pos2.getY(), pos2.getZ());
        }

        public BoundingBox(int x1, int y1, int z1, int x2, int y2, int z2) {
            this.minX = Math.min(x1, x2);
            this.minY = Math.min(y1, y2);
            this.minZ = Math.min(z1, z2);
            this.maxX = Math.max(x1, x2);
            this.maxY = Math.max(y1, y2);
            this.maxZ = Math.max(z1, z2);
            this.id = UUID.randomUUID().toString();
        }

        public static BoundingBox fromCenter(BlockPos center, int sizeX, int sizeY, int sizeZ) {
            return new BoundingBox(
                    center.getX() - sizeX/2, center.getY(), center.getZ() - sizeZ/2,
                    center.getX() + sizeX/2, center.getY() + sizeY, center.getZ() + sizeZ/2
            );
        }

        public boolean intersects(BoundingBox other) {
            return this.minX <= other.maxX && this.maxX >= other.minX &&
                    this.minY <= other.maxY && this.maxY >= other.minY &&
                    this.minZ <= other.maxZ && this.maxZ >= other.minZ;
        }

        public boolean contains(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX &&
                    pos.getY() >= minY && pos.getY() <= maxY &&
                    pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }

        public BlockPos getCenter() {
            return new BlockPos(
                    (minX + maxX) / 2,
                    (minY + maxY) / 2,
                    (minZ + maxZ) / 2
            );
        }

        public BlockPos getSize() {
            return new BlockPos(
                    maxX - minX + 1,
                    maxY - minY + 1,
                    maxZ - minZ + 1
            );
        }

        public BoundingBox offset(int dx, int dy, int dz) {
            return new BoundingBox(
                    minX + dx, minY + dy, minZ + dz,
                    maxX + dx, maxY + dy, maxZ + dz
            );
        }

        public BoundingBox expand(int amount) {
            return new BoundingBox(
                    minX - amount, minY - amount, minZ - amount,
                    maxX + amount, maxY + amount, maxZ + amount
            );
        }

        public AxisAlignedBB toAABB() {
            return new AxisAlignedBB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            BoundingBox that = (BoundingBox) obj;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return String.format("BoundingBox[(%d,%d,%d) to (%d,%d,%d)]",
                    minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    public static class RoomNode {
        public BlockPos position;   // 这里约定：为"外壳左下角(基点)"相对偏移
        public int size;            // 用壳体尺寸逻辑，不用于模板尺寸
        public RoomType type;
        public Rotation rotation;
        public double difficulty;
        public int floorIndex;      // 楼层索引 (0=底层, 1=中层, 2=顶层...)
        public RoomNode linkedStaircase; // 楼梯连接的房间（上层或下层）

        public RoomNode(BlockPos position, int size, RoomType type) {
            this.position = position;
            this.size = size;
            this.type = type;
            this.rotation = Rotation.NONE;
            this.difficulty = 0.5;
            this.floorIndex = 0;
            this.linkedStaircase = null;
        }

        public boolean isStaircase() {
            return type == RoomType.STAIRCASE_UP ||
                   type == RoomType.STAIRCASE_DOWN ||
                   type == RoomType.STAIRCASE_BOTH;
        }
    }

    public static class RoomConnection {
        public RoomNode from;
        public RoomNode to;
        public ConnectionType type;

        public RoomConnection(RoomNode from, RoomNode to, ConnectionType type) {
            this.from = from;
            this.to = to;
            this.type = type;
        }

        public double getDistance() {
            BlockPos a = from.position;
            BlockPos b = to.position;
            double dx = (a.getX() + 15) - (b.getX() + 15);
            double dz = (a.getZ() + 15) - (b.getZ() + 15);
            return Math.sqrt(dx*dx + dz*dz);
        }
    }

    public static class DungeonLayout {
        private final BlockPos center;
        private final int size;
        private List<RoomNode> rooms;
        private List<RoomConnection> connections;
        private int floorCount;           // 楼层数
        private int floorHeight;          // 每层高度间隔
        private List<List<RoomNode>> floorRooms; // 按楼层分组的房间

        public DungeonLayout(BlockPos center, int size) {
            this.center = center;
            this.size = size;
            this.rooms = new ArrayList<>();
            this.connections = new ArrayList<>();
            this.floorCount = 1;
            this.floorHeight = 25;
            this.floorRooms = new ArrayList<>();
        }

        public void setRooms(List<RoomNode> rooms) {
            this.rooms = rooms;
        }

        public void setConnections(List<RoomConnection> connections) {
            this.connections = connections;
        }

        public List<RoomNode> getRooms() {
            return rooms;
        }

        public List<RoomConnection> getConnections() {
            return connections;
        }

        public BlockPos getCenter() {
            return center;
        }

        public int getSize() {
            return size;
        }

        public int getFloorCount() {
            return floorCount;
        }

        public void setFloorCount(int floorCount) {
            this.floorCount = floorCount;
        }

        public int getFloorHeight() {
            return floorHeight;
        }

        public void setFloorHeight(int floorHeight) {
            this.floorHeight = floorHeight;
        }

        public List<List<RoomNode>> getFloorRooms() {
            return floorRooms;
        }

        public void setFloorRooms(List<List<RoomNode>> floorRooms) {
            this.floorRooms = floorRooms;
        }

        public List<RoomNode> getRoomsOnFloor(int floor) {
            if (floor >= 0 && floor < floorRooms.size()) {
                return floorRooms.get(floor);
            }
            return new ArrayList<>();
        }

        public RoomNode getEntranceRoom() {
            return rooms.stream()
                    .filter(r -> r.type == RoomType.ENTRANCE)
                    .findFirst()
                    .orElse(null);
        }

        public RoomNode getExitRoom() {
            return rooms.stream()
                    .filter(r -> r.type == RoomType.EXIT)
                    .findFirst()
                    .orElse(null);
        }

        public List<RoomNode> getStaircaseRooms() {
            List<RoomNode> staircases = new ArrayList<>();
            for (RoomNode room : rooms) {
                if (room.isStaircase()) {
                    staircases.add(room);
                }
            }
            return staircases;
        }
    }

    public static class ConnectionPoint {
        public final BlockPos position;
        public final EnumFacing facing;

        public ConnectionPoint(BlockPos position, EnumFacing facing) {
            this.position = position;
            this.facing = facing;
        }
    }

    public static class DungeonTemplate {
        public final String name;
        public final com.moremod.schematic.Schematic schematic;
        public final RoomType type;
        public final int weight;
        public final List<ConnectionPoint> connectionPoints;

        public DungeonTemplate(String name, com.moremod.schematic.Schematic schematic,
                               RoomType type, int weight) {
            this.name = name;
            this.schematic = schematic;
            this.type = type;
            this.weight = weight;
            this.connectionPoints = new ArrayList<>();
        }

        public void addConnectionPoint(ConnectionPoint point) {
            connectionPoints.add(point);
        }

        public BoundingBox getBoundingBox(BlockPos origin) {
            return new BoundingBox(
                    origin.getX(), origin.getY(), origin.getZ(),
                    origin.getX() + schematic.width - 1,
                    origin.getY() + schematic.height - 1,
                    origin.getZ() + schematic.length - 1
            );
        }
    }

    public static class WeightedTemplate {
        public final String name;
        public final com.moremod.schematic.Schematic schematic;
        public final RoomType type;
        public final int weight;
        public final List<ConnectionPoint> connectionPoints;

        public WeightedTemplate(String name, com.moremod.schematic.Schematic schematic,
                                RoomType type, int weight, List<ConnectionPoint> connectionPoints) {
            this.name = name;
            this.schematic = schematic;
            this.type = type;
            this.weight = weight;
            this.connectionPoints = connectionPoints;
        }
    }
}
