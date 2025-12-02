package com.moremod.dungeon;

import com.moremod.dungeon.DungeonTypes.*;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * 严格树状布局生成器（无环、不重叠）
 * - 每个房间使用统一 30x12x30 壳体（大小逻辑按“壳中心点”计算间距）
 * - 只输出一棵树：connections.size() == rooms.size() - 1
 */
public class DungeonLayoutGenerator {

    // 壳体参数（需与放置器一致）
    private static final int SHELL_SIZE = 30;
    private static final int GAP = 8; // 壳与壳之间的最小净距（中心间距 - SHELL_SIZE）
    private static final int MIN_ROOMS = 14;
    private static final int MAX_ROOMS = 24;

    private static final double TRI_MAX_DIST = SHELL_SIZE * 4.0; // 三角化近邻阈值

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
        int iterations = 0, maxIter = 400;
        boolean ok = false;
        int half = dungeonSize / 2;
        int minX = -half, maxX = half - SHELL_SIZE;
        int minZ = -half, maxZ = half - SHELL_SIZE;

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
                        int push = 2; // 每次推2格
                        a.position = clampBase(a.position.add((int) Math.round(dir[0] * push), 0, (int) Math.round(dir[1] * push)),
                                minX, maxX, minZ, maxZ);
                        b.position = clampBase(b.position.add((int) Math.round(-dir[0] * push), 0, (int) Math.round(-dir[1] * push)),
                                minX, maxX, minZ, maxZ);
                    }
                }
            }
        }
    }

    private boolean roomsOverlap(RoomNode a, RoomNode b) {
        BlockPos ca = centerOf(a);
        BlockPos cb = centerOf(b);
        double dx = ca.getX() - cb.getX();
        double dz = ca.getZ() - cb.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        return dist < (SHELL_SIZE + GAP);
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
}
