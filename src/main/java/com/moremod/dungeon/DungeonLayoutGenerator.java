package com.moremod.dungeon;

import com.moremod.dungeon.DungeonTypes.*;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * 严格树状布局生成器（无环、不重叠）
 * - 每个房间使用统一 30x12x30 壳体（大小逻辑按"壳中心点"计算间距）
 * - 只输出一棵树：connections.size() == rooms.size() - 1
 * - 支持多层三维地牢结构
 */
public class DungeonLayoutGenerator {

    // 壳体参数（需与放置器一致）
    // 使用最大房间尺寸(BOSS=40)作为基准，确保任何房间都不会重叠
    private static final int SHELL_SIZE = 30;
    private static final int MAX_SHELL_SIZE = 40; // BOSS房间尺寸
    private static final int GAP = 12; // 增加间距确保大房间不重叠
    private static final int MIN_ROOMS = 14;
    private static final int MAX_ROOMS = 24;

    // 三维地牢参数
    private static final int DEFAULT_FLOOR_COUNT = 3;    // 默认楼层数
    private static final int FLOOR_HEIGHT = 25;          // 楼层间隔高度
    private static final int MIN_ROOMS_PER_FLOOR = 5;    // 每层最少房间
    private static final int MAX_ROOMS_PER_FLOOR = 8;    // 每层最多房间

    private static final double TRI_MAX_DIST = SHELL_SIZE * 4.0; // 三角化近邻阈值

    /**
     * 生成传统单层地牢（向下兼容）
     */
    public DungeonLayout generateLayout(BlockPos center, int dungeonSize, long seed) {
        Random random = new Random(seed);
        DungeonLayout layout = new DungeonLayout(center, dungeonSize);

        // 1) 生成房间（初始基点 = 壳左下角基点相对偏移）
        List<RoomNode> rooms = generateRooms(dungeonSize, random);

        // 2) 物理分离（基于壳“中心点”的二维距离）
        separateRooms(rooms, dungeonSize);

        // 3) 近邻图（简化三角剖分：距离阈值内连边）
        List<RoomConnection> nearGraph = buildNeighborhood(rooms);

        // 4) 生成 MST（严格树）
        List<RoomConnection> tree = generateMinimumSpanningTree(rooms, nearGraph);

        // 5) 分配类型（入口=最靠近原点；出口=树中离入口最远的叶子；BOSS=次远叶子）
        assignRoomTypesStrictTree(rooms, tree);

        layout.setRooms(rooms);
        layout.setConnections(tree);
        return layout;
    }

    // ---------------- 生成房间 ----------------

    private List<RoomNode> generateRooms(int dungeonSize, Random random) {
        List<RoomNode> rooms = new ArrayList<>();
        int roomCount = MIN_ROOMS + random.nextInt(MAX_ROOMS - MIN_ROOMS + 1);

        // 在圆形区域内随机“放置壳的中心点”，再转换成“壳基点=中心-15”
        double radius = dungeonSize * 0.35;

        for (int i = 0; i < roomCount; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double r = Math.sqrt(random.nextDouble()) * radius;

            int cx = (int) Math.round(r * Math.cos(angle));
            int cz = (int) Math.round(r * Math.sin(angle));
            int cy = -2 + random.nextInt(5); // 高度微扰

            // 基点 = 中心 - 15
            BlockPos base = new BlockPos(cx - SHELL_SIZE / 2, cy, cz - SHELL_SIZE / 2);
            rooms.add(new RoomNode(base, SHELL_SIZE, RoomType.NORMAL));
        }
        return rooms;
    }

    // ---------------- 分离重叠 ----------------

    private void separateRooms(List<RoomNode> rooms, int dungeonSize) {
        int iterations = 0, maxIter = 800; // 增加迭代次数
        boolean ok = false;
        int half = dungeonSize / 2;
        int minX = -half, maxX = half - MAX_SHELL_SIZE;
        int minZ = -half, maxZ = half - MAX_SHELL_SIZE;

        while (!ok && iterations++ < maxIter) {
            ok = true;
            for (int i = 0; i < rooms.size(); i++) {
                for (int j = i + 1; j < rooms.size(); j++) {
                    RoomNode a = rooms.get(i);
                    RoomNode b = rooms.get(j);
                    if (roomsOverlap(a, b)) {
                        ok = false;
                        // 以中心点为矢量推开
                        double[] dir = normDir(centerOf(b), centerOf(a));
                        int push = 4; // 每次推4格（加快分离速度）
                        a.position = clampBase(a.position.add((int) Math.round(dir[0] * push), 0, (int) Math.round(dir[1] * push)),
                                minX, maxX, minZ, maxZ);
                        b.position = clampBase(b.position.add((int) Math.round(-dir[0] * push), 0, (int) Math.round(-dir[1] * push)),
                                minX, maxX, minZ, maxZ);
                    }
                }
            }
        }
        if (!ok) {
            System.out.println("[地牢布局] 警告: 房间分离未完全完成，可能存在轻微重叠");
        }
    }

    private boolean roomsOverlap(RoomNode a, RoomNode b) {
        BlockPos ca = centerOf(a);
        BlockPos cb = centerOf(b);
        double dx = ca.getX() - cb.getX();
        double dz = ca.getZ() - cb.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        // 使用最大房间尺寸确保任何类型房间都不会重叠
        return dist < (MAX_SHELL_SIZE + GAP);
    }

    private BlockPos centerOf(RoomNode r) {
        return r.position.add(SHELL_SIZE / 2, 0, SHELL_SIZE / 2);
    }

    private double[] normDir(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double len = Math.max(1e-6, Math.sqrt(dx * dx + dz * dz));
        return new double[]{dx / len, dz / len};
    }

    private BlockPos clampBase(BlockPos base, int minX, int maxX, int minZ, int maxZ) {
        int x = Math.max(minX, Math.min(maxX, base.getX()));
        int z = Math.max(minZ, Math.min(maxZ, base.getZ()));
        return new BlockPos(x, base.getY(), z);
    }

    // ---------------- 近邻图（简化三角化） ----------------

    private List<RoomConnection> buildNeighborhood(List<RoomNode> rooms) {
        List<RoomConnection> edges = new ArrayList<>();
        for (int i = 0; i < rooms.size(); i++) {
            for (int j = i + 1; j < rooms.size(); j++) {
                RoomNode a = rooms.get(i), b = rooms.get(j);
                double d = dist2D(centerOf(a), centerOf(b));
                if (d < TRI_MAX_DIST) {
                    edges.add(new RoomConnection(a, b, ConnectionType.NORMAL));
                }
            }
        }
        // 确保至少连得上：若近邻为空，则强制连最近的一个
        if (edges.isEmpty() && rooms.size() > 1) {
            RoomNode a = rooms.get(0);
            RoomNode best = null;
            double bestD = 1e18;
            for (int i = 1; i < rooms.size(); i++) {
                double d = dist2D(centerOf(a), centerOf(rooms.get(i)));
                if (d < bestD) { bestD = d; best = rooms.get(i); }
            }
            if (best != null) edges.add(new RoomConnection(a, best, ConnectionType.NORMAL));
        }
        return edges;
    }

    private double dist2D(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    // ---------------- MST（严格树） ----------------

    private List<RoomConnection> generateMinimumSpanningTree(List<RoomNode> rooms, List<RoomConnection> edges) {
        List<RoomConnection> mst = new ArrayList<>();
        if (rooms.size() <= 1) return mst;

        edges.sort(Comparator.comparingDouble(RoomConnection::getDistance));
        Map<RoomNode, RoomNode> parent = new HashMap<>();
        for (RoomNode r : rooms) parent.put(r, r);

        for (RoomConnection e : edges) {
            RoomNode ra = find(parent, e.from);
            RoomNode rb = find(parent, e.to);
            if (ra != rb) {
                mst.add(e);
                parent.put(ra, rb);
                if (mst.size() == rooms.size() - 1) break;
            }
        }

        // 若图不连通，补“最近边”直到连通
        if (mst.size() < rooms.size() - 1) {
            Set<RoomNode> seenRoots = new HashSet<>();
            for (RoomNode r : rooms) seenRoots.add(find(parent, r));
            while (seenRoots.size() > 1) {
                double best = 1e18; RoomConnection bestEdge = null;
                for (RoomNode a : rooms) for (RoomNode b : rooms) {
                    if (a == b) continue;
                    if (find(parent, a) == find(parent, b)) continue;
                    double d = dist2D(centerOf(a), centerOf(b));
                    if (d < best) { best = d; bestEdge = new RoomConnection(a, b, ConnectionType.NORMAL); }
                }
                if (bestEdge == null) break;
                mst.add(bestEdge);
                RoomNode ra = find(parent, bestEdge.from);
                RoomNode rb = find(parent, bestEdge.to);
                parent.put(ra, rb);
                seenRoots.clear();
                for (RoomNode r : rooms) seenRoots.add(find(parent, r));
            }
        }
        return mst;
    }

    private RoomNode find(Map<RoomNode, RoomNode> parent, RoomNode x) {
        RoomNode p = parent.get(x);
        if (p != x) parent.put(x, find(parent, p));
        return parent.get(x);
    }

    // ---------------- 类型分配（严格树） ----------------

    private void assignRoomTypesStrictTree(List<RoomNode> rooms, List<RoomConnection> tree) {
        if (rooms.isEmpty()) return;

        // 建邻接
        Map<RoomNode, List<RoomNode>> g = new HashMap<>();
        for (RoomNode r : rooms) g.put(r, new ArrayList<>());
        for (RoomConnection e : tree) { g.get(e.from).add(e.to); g.get(e.to).add(e.from); }

        // 入口 = 中心最近
        RoomNode entrance = rooms.get(0);
        double best = 1e18;
        for (RoomNode r : rooms) {
            double d = dist2D(centerOf(r), new BlockPos(0,0,0));
            if (d < best) { best = d; entrance = r; }
        }
        entrance.type = RoomType.ENTRANCE;

        // BFS 求最远叶子作为出口
        Map<RoomNode, RoomNode> parent = new HashMap<>();
        List<RoomNode> order = bfsOrder(g, entrance, parent);
        RoomNode exit = entrance; double far = -1;
        for (RoomNode r : order) {
            if (g.get(r).size() == 1 || r == entrance) {
                int dist = pathLen(parent, entrance, r);
                if (dist > far) { far = dist; exit = r; }
            }
        }
        exit.type = RoomType.EXIT;

        // BOSS = 与入口路径长的另一叶子（排除出口）中距离第二远的
        RoomNode boss = null; int bestLen = -1;
        for (RoomNode r : order) {
            if (r == entrance || r == exit) continue;
            if (g.get(r).size() == 1) {
                int len = pathLen(parent, entrance, r);
                if (len > bestLen) { bestLen = len; boss = r; }
            }
        }
        if (boss != null) boss.type = RoomType.BOSS;

        // MINI_BOSS = 在入口到Boss/出口路径中间的房间（度>=2）
        // 选择路径长度在 40%-60% 范围内的房间
        int maxPathLen = Math.max(pathLen(parent, entrance, exit), bestLen);
        RoomNode miniBoss = null;
        int targetLen = maxPathLen / 2;
        int minDiff = Integer.MAX_VALUE;
        for (RoomNode r : order) {
            if (r == entrance || r == exit || r == boss) continue;
            int len = pathLen(parent, entrance, r);
            int diff = Math.abs(len - targetLen);
            // 选择路径中间、度>=2的房间作为道中Boss
            if (diff < minDiff && g.get(r).size() >= 2 && len >= 2) {
                minDiff = diff;
                miniBoss = r;
            }
        }
        if (miniBoss != null && maxPathLen >= 5) {
            miniBoss.type = RoomType.MINI_BOSS;
        }

        // 其它按度分：度=1 → TREASURE/TRAP；度>=4 → HUB；否则 NORMAL/MONSTER/PUZZLE
        Random rnd = new Random(rooms.hashCode() ^ tree.hashCode());
        for (RoomNode r : rooms) {
            if (r.type == RoomType.ENTRANCE || r.type == RoomType.EXIT ||
                r.type == RoomType.BOSS || r.type == RoomType.MINI_BOSS) continue;
            int deg = g.get(r).size();
            if (deg == 1) r.type = rnd.nextDouble() < 0.65 ? RoomType.TREASURE : RoomType.TRAP;
            else if (deg >= 4) r.type = RoomType.HUB;
            else {
                double p = rnd.nextDouble();
                if (p < 0.33) r.type = RoomType.MONSTER;
                else if (p < 0.55) r.type = RoomType.PUZZLE;
                else r.type = RoomType.NORMAL;
            }
        }
    }

    private List<RoomNode> bfsOrder(Map<RoomNode, List<RoomNode>> g, RoomNode start, Map<RoomNode, RoomNode> parent) {
        List<RoomNode> order = new ArrayList<>();
        Queue<RoomNode> q = new ArrayDeque<>();
        Set<RoomNode> vis = new HashSet<>();
        q.add(start); vis.add(start); parent.put(start, null);
        while (!q.isEmpty()) {
            RoomNode u = q.poll(); order.add(u);
            for (RoomNode v : g.get(u)) if (!vis.contains(v)) {
                vis.add(v); parent.put(v, u); q.add(v);
            }
        }
        return order;
    }

    private int pathLen(Map<RoomNode, RoomNode> parent, RoomNode a, RoomNode b) {
        int len = 0; RoomNode cur = b;
        while (cur != null && cur != a) { cur = parent.get(cur); len++; }
        return len;
    }

    // ==================== 三维多层地牢生成 ====================

    /**
     * 生成三维多层地牢
     * @param center 地牢中心点
     * @param dungeonSize 地牢水平尺寸
     * @param seed 随机种子
     * @param floorCount 楼层数 (建议3层)
     * @return 多层地牢布局
     */
    public DungeonLayout generateMultiFloorLayout(BlockPos center, int dungeonSize, long seed, int floorCount) {
        Random random = new Random(seed);
        DungeonLayout layout = new DungeonLayout(center, dungeonSize);
        layout.setFloorCount(floorCount);
        layout.setFloorHeight(FLOOR_HEIGHT);

        List<List<RoomNode>> allFloorRooms = new ArrayList<>();
        List<RoomNode> allRooms = new ArrayList<>();
        List<RoomConnection> allConnections = new ArrayList<>();

        // 为每层生成房间
        for (int floor = 0; floor < floorCount; floor++) {
            List<RoomNode> floorRooms = generateFloorRooms(floor, floorCount, dungeonSize, random);
            separateRooms(floorRooms, dungeonSize);

            // 设置楼层索引
            for (RoomNode room : floorRooms) {
                room.floorIndex = floor;
            }

            allFloorRooms.add(floorRooms);
            allRooms.addAll(floorRooms);
        }

        // 为每层生成MST连接
        for (int floor = 0; floor < floorCount; floor++) {
            List<RoomNode> floorRooms = allFloorRooms.get(floor);
            List<RoomConnection> nearGraph = buildNeighborhood(floorRooms);
            List<RoomConnection> floorTree = generateMinimumSpanningTree(floorRooms, nearGraph);
            allConnections.addAll(floorTree);
        }

        // 分配楼梯房间并建立跨层连接
        assignStaircaseRooms(allFloorRooms, allConnections, random);

        // 为每层分配房间类型
        for (int floor = 0; floor < floorCount; floor++) {
            List<RoomNode> floorRooms = allFloorRooms.get(floor);
            List<RoomConnection> floorConnections = getConnectionsForFloor(allConnections, floor);
            assignMultiFloorRoomTypes(floorRooms, floorConnections, floor, floorCount, random);
        }

        layout.setRooms(allRooms);
        layout.setConnections(allConnections);
        layout.setFloorRooms(allFloorRooms);

        System.out.println("[三维地牢] 生成完成: " + floorCount + "层, 共" + allRooms.size() + "个房间");

        return layout;
    }

    /**
     * 生成单层房间
     */
    private List<RoomNode> generateFloorRooms(int floor, int totalFloors, int dungeonSize, Random random) {
        List<RoomNode> rooms = new ArrayList<>();

        // 根据楼层调整房间数量
        int roomCount;
        if (floor == 0) {
            // 入口层：较少房间
            roomCount = MIN_ROOMS_PER_FLOOR + random.nextInt(2);
        } else if (floor == totalFloors - 1) {
            // Boss层：较少房间
            roomCount = MIN_ROOMS_PER_FLOOR + random.nextInt(2);
        } else {
            // 中间层：更多房间
            roomCount = MIN_ROOMS_PER_FLOOR + random.nextInt(MAX_ROOMS_PER_FLOOR - MIN_ROOMS_PER_FLOOR + 1);
        }

        double radius = dungeonSize * 0.30; // 略小的分布半径

        for (int i = 0; i < roomCount; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double r = Math.sqrt(random.nextDouble()) * radius;

            int cx = (int) Math.round(r * Math.cos(angle));
            int cz = (int) Math.round(r * Math.sin(angle));
            // Y坐标由楼层决定
            int cy = floor * FLOOR_HEIGHT;

            BlockPos base = new BlockPos(cx - SHELL_SIZE / 2, cy, cz - SHELL_SIZE / 2);
            rooms.add(new RoomNode(base, SHELL_SIZE, RoomType.NORMAL));
        }

        return rooms;
    }

    /**
     * 分配楼梯房间
     */
    private void assignStaircaseRooms(List<List<RoomNode>> allFloorRooms, List<RoomConnection> connections, Random random) {
        int floorCount = allFloorRooms.size();

        for (int floor = 0; floor < floorCount - 1; floor++) {
            List<RoomNode> currentFloor = allFloorRooms.get(floor);
            List<RoomNode> nextFloor = allFloorRooms.get(floor + 1);

            // 选择当前层的一个房间作为楼梯
            RoomNode staircaseDown = selectStaircaseRoom(currentFloor, random);
            // 选择上层最近的房间作为连接点
            RoomNode staircaseUp = findNearestRoom(staircaseDown, nextFloor);

            // 设置楼梯类型
            if (floor == 0) {
                staircaseDown.type = RoomType.STAIRCASE_UP;
            } else {
                staircaseDown.type = RoomType.STAIRCASE_BOTH;
            }

            if (floor == floorCount - 2) {
                staircaseUp.type = RoomType.STAIRCASE_DOWN;
            } else {
                staircaseUp.type = RoomType.STAIRCASE_BOTH;
            }

            // 建立双向链接
            staircaseDown.linkedStaircase = staircaseUp;
            staircaseUp.linkedStaircase = staircaseDown;

            // 添加跨层连接
            connections.add(new RoomConnection(staircaseDown, staircaseUp, ConnectionType.NORMAL));

            System.out.println("[三维地牢] 楼梯连接: 层" + floor + " -> 层" + (floor + 1));
        }
    }

    /**
     * 选择适合作为楼梯的房间（度较高的房间）
     */
    private RoomNode selectStaircaseRoom(List<RoomNode> rooms, Random random) {
        // 优先选择不是特殊房间的普通房间
        List<RoomNode> candidates = new ArrayList<>();
        for (RoomNode room : rooms) {
            if (room.type == RoomType.NORMAL || room.type == RoomType.HUB) {
                candidates.add(room);
            }
        }

        if (candidates.isEmpty()) {
            candidates = new ArrayList<>(rooms);
        }

        // 随机选择一个
        return candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * 找到距离目标房间最近的房间
     */
    private RoomNode findNearestRoom(RoomNode target, List<RoomNode> rooms) {
        RoomNode nearest = null;
        double minDist = Double.MAX_VALUE;

        BlockPos targetCenter = centerOf(target);

        for (RoomNode room : rooms) {
            BlockPos roomCenter = centerOf(room);
            // 只比较XZ平面距离
            double dx = targetCenter.getX() - roomCenter.getX();
            double dz = targetCenter.getZ() - roomCenter.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist < minDist) {
                minDist = dist;
                nearest = room;
            }
        }

        return nearest;
    }

    /**
     * 获取指定楼层的连接
     */
    private List<RoomConnection> getConnectionsForFloor(List<RoomConnection> allConnections, int floor) {
        List<RoomConnection> floorConnections = new ArrayList<>();
        for (RoomConnection conn : allConnections) {
            if (conn.from.floorIndex == floor && conn.to.floorIndex == floor) {
                floorConnections.add(conn);
            }
        }
        return floorConnections;
    }

    /**
     * 为多层地牢分配房间类型
     */
    private void assignMultiFloorRoomTypes(List<RoomNode> rooms, List<RoomConnection> connections,
                                           int floor, int totalFloors, Random random) {
        if (rooms.isEmpty()) return;

        // 建邻接表
        Map<RoomNode, List<RoomNode>> g = new HashMap<>();
        for (RoomNode r : rooms) g.put(r, new ArrayList<>());
        for (RoomConnection e : connections) {
            if (g.containsKey(e.from) && g.containsKey(e.to)) {
                g.get(e.from).add(e.to);
                g.get(e.to).add(e.from);
            }
        }

        // 根据楼层分配特殊房间
        if (floor == 0) {
            // 底层：入口 + 楼梯
            RoomNode entrance = rooms.stream()
                    .filter(r -> !r.isStaircase())
                    .min((a, b) -> Double.compare(
                            dist2D(centerOf(a), new BlockPos(0, 0, 0)),
                            dist2D(centerOf(b), new BlockPos(0, 0, 0))))
                    .orElse(rooms.get(0));
            entrance.type = RoomType.ENTRANCE;
        }

        if (floor == totalFloors - 1) {
            // 顶层：Boss + 出口
            RoomNode boss = rooms.stream()
                    .filter(r -> !r.isStaircase())
                    .filter(r -> g.containsKey(r) && g.get(r).size() == 1)
                    .findFirst()
                    .orElse(rooms.stream().filter(r -> !r.isStaircase()).findFirst().orElse(null));
            if (boss != null) {
                boss.type = RoomType.BOSS;
            }

            // 出口房间
            RoomNode exit = rooms.stream()
                    .filter(r -> !r.isStaircase() && r.type != RoomType.BOSS)
                    .filter(r -> g.containsKey(r) && g.get(r).size() == 1)
                    .findFirst()
                    .orElse(null);
            if (exit != null) {
                exit.type = RoomType.EXIT;
            }
        }

        if (floor == totalFloors / 2 || (totalFloors > 2 && floor == 1)) {
            // 中间层：MINI_BOSS
            RoomNode miniBoss = rooms.stream()
                    .filter(r -> !r.isStaircase() && r.type == RoomType.NORMAL)
                    .filter(r -> g.containsKey(r) && g.get(r).size() >= 2)
                    .findFirst()
                    .orElse(null);
            if (miniBoss != null) {
                miniBoss.type = RoomType.MINI_BOSS;
            }
        }

        // 其他房间类型分配
        for (RoomNode r : rooms) {
            if (r.type == RoomType.ENTRANCE || r.type == RoomType.EXIT ||
                r.type == RoomType.BOSS || r.type == RoomType.MINI_BOSS ||
                r.isStaircase()) continue;

            int deg = g.containsKey(r) ? g.get(r).size() : 0;

            if (deg == 1) {
                r.type = random.nextDouble() < 0.65 ? RoomType.TREASURE : RoomType.TRAP;
            } else if (deg >= 3) {
                r.type = RoomType.HUB;
            } else {
                double p = random.nextDouble();
                if (p < 0.33) r.type = RoomType.MONSTER;
                else if (p < 0.55) r.type = RoomType.PUZZLE;
                else r.type = RoomType.NORMAL;
            }
        }
    }
}
