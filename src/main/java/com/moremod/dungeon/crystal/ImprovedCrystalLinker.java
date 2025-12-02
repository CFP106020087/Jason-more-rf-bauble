package com.moremod.dungeon.crystal;

import com.moremod.dungeon.DungeonTypes.*;
import com.moremod.dungeon.portal.PortalManager;
import com.moremod.init.ModBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * 改进的水晶连接系统
 * 确保所有房间双向连通
 */
public class ImprovedCrystalLinker {

    private static final int SHELL_SIZE = 30;
    private static final int THICK = 2;
    private static final int INNER_Y = 2;

    private final World world;
    private final Map<RoomNode, List<BlockPos>> roomCrystals = new HashMap<>();
    private final Map<RoomConnection, CrystalPair> connectionCrystals = new HashMap<>();

    public ImprovedCrystalLinker(World world) {
        this.world = world;
    }

    /**
     * 水晶对 - 记录连接的两个水晶位置
     */
    private static class CrystalPair {
        public final BlockPos crystal1;
        public final BlockPos crystal2;

        public CrystalPair(BlockPos c1, BlockPos c2) {
            this.crystal1 = c1;
            this.crystal2 = c2;
        }
    }

    /**
     * 放置并链接所有水晶
     */
    public void placeAndLinkCrystals(DungeonLayout layout, Map<RoomNode, BlockPos> roomBases) {
        // 第一步：为每个连接计算并放置水晶对
        for (RoomConnection conn : layout.getConnections()) {
            BlockPos baseA = roomBases.get(conn.from);
            BlockPos baseB = roomBases.get(conn.to);

            if (baseA == null || baseB == null) continue;

            // 计算两个房间之间的水晶位置
            CrystalPair pair = calculateCrystalPair(baseA, baseB);

            // 放置水晶
            placeCrystal(pair.crystal1);
            placeCrystal(pair.crystal2);

            // 记录水晶对
            connectionCrystals.put(conn, pair);

            // 记录每个房间的水晶
            roomCrystals.computeIfAbsent(conn.from, k -> new ArrayList<>()).add(pair.crystal1);
            roomCrystals.computeIfAbsent(conn.to, k -> new ArrayList<>()).add(pair.crystal2);
        }

        // 第二步：建立双向传送链接
        for (CrystalPair pair : connectionCrystals.values()) {
            PortalManager.registerLink(world, pair.crystal1, pair.crystal2);
            System.out.println("[水晶链接] 建立双向连接: " + pair.crystal1 + " <-> " + pair.crystal2);
        }

        // 第三步：验证连通性
        verifyConnectivity(layout);
    }

    /**
     * 计算两个房间之间的水晶对位置
     */
    private CrystalPair calculateCrystalPair(BlockPos roomA, BlockPos roomB) {
        // 计算房间中心
        BlockPos centerA = roomA.add(SHELL_SIZE / 2, INNER_Y, SHELL_SIZE / 2);
        BlockPos centerB = roomB.add(SHELL_SIZE / 2, INNER_Y, SHELL_SIZE / 2);

        // 计算方向
        int dx = centerB.getX() - centerA.getX();
        int dz = centerB.getZ() - centerA.getZ();

        // 确定水晶放置的边
        EnumFacing facingA = getFacingDirection(dx, dz);
        EnumFacing facingB = facingA.getOpposite();

        // 计算精确的水晶位置
        BlockPos crystalA = getCrystalPosition(roomA, facingA);
        BlockPos crystalB = getCrystalPosition(roomB, facingB);

        return new CrystalPair(crystalA, crystalB);
    }

    /**
     * 根据方向获取水晶位置
     */
    private BlockPos getCrystalPosition(BlockPos roomBase, EnumFacing facing) {
        int x, z;
        int y = roomBase.getY() + INNER_Y + 1; // 地面上方一格

        switch (facing) {
            case NORTH:
                x = roomBase.getX() + SHELL_SIZE / 2;
                z = roomBase.getZ() + THICK + 1; // 北墙内侧
                break;
            case SOUTH:
                x = roomBase.getX() + SHELL_SIZE / 2;
                z = roomBase.getZ() + SHELL_SIZE - THICK - 2; // 南墙内侧
                break;
            case EAST:
                x = roomBase.getX() + SHELL_SIZE - THICK - 2; // 东墙内侧
                z = roomBase.getZ() + SHELL_SIZE / 2;
                break;
            case WEST:
                x = roomBase.getX() + THICK + 1; // 西墙内侧
                z = roomBase.getZ() + SHELL_SIZE / 2;
                break;
            default:
                // 默认中心位置
                x = roomBase.getX() + SHELL_SIZE / 2;
                z = roomBase.getZ() + SHELL_SIZE / 2;
        }

        return new BlockPos(x, y, z);
    }

    /**
     * 根据偏移量确定朝向
     */
    private EnumFacing getFacingDirection(int dx, int dz) {
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? EnumFacing.EAST : EnumFacing.WEST;
        } else {
            return dz > 0 ? EnumFacing.SOUTH : EnumFacing.NORTH;
        }
    }

    /**
     * 放置水晶方块
     */
    private void placeCrystal(BlockPos pos) {
        IBlockState crystalState = ModBlocks.UNBREAKABLE_BARRIER_VOID.getDefaultState();
        IBlockState glowstoneState = Blocks.GLOWSTONE.getDefaultState();

        // 放置水晶
        world.setBlockState(pos, crystalState, 3);

        // 在水晶下方放置光源
        world.setBlockState(pos.down(), glowstoneState, 3);

        // 装饰性粒子效果标记
        world.setBlockState(pos.up(), Blocks.GLASS.getDefaultState(), 3);
    }

    /**
     * 验证连通性
     */
    private void verifyConnectivity(DungeonLayout layout) {
        Set<RoomNode> visited = new HashSet<>();
        Queue<RoomNode> queue = new LinkedList<>();

        // 从入口开始BFS
        RoomNode entrance = layout.getEntranceRoom();
        if (entrance == null && !layout.getRooms().isEmpty()) {
            entrance = layout.getRooms().get(0);
        }

        if (entrance != null) {
            queue.offer(entrance);
            visited.add(entrance);

            while (!queue.isEmpty()) {
                RoomNode current = queue.poll();

                // 检查所有连接
                for (RoomConnection conn : layout.getConnections()) {
                    RoomNode next = null;
                    if (conn.from == current) next = conn.to;
                    else if (conn.to == current) next = conn.from;

                    if (next != null && !visited.contains(next)) {
                        visited.add(next);
                        queue.offer(next);
                    }
                }
            }
        }

        // 报告连通性
        int totalRooms = layout.getRooms().size();
        int connectedRooms = visited.size();

        if (connectedRooms == totalRooms) {
            System.out.println("[水晶连接] ✓ 所有房间均已连通 (" + connectedRooms + "/" + totalRooms + ")");
        } else {
            System.err.println("[水晶连接] ✗ 警告：有房间未连通 (" + connectedRooms + "/" + totalRooms + ")");

            // 识别未连通的房间
            for (RoomNode room : layout.getRooms()) {
                if (!visited.contains(room)) {
                    System.err.println("  - 未连通房间: " + room.type + " at " + room.position);
                }
            }
        }
    }

    /**
     * 获取房间的所有水晶位置
     */
    public List<BlockPos> getRoomCrystals(RoomNode room) {
        return roomCrystals.getOrDefault(room, Collections.emptyList());
    }

    /**
     * 添加额外的水晶连接（用于特殊房间）
     */
    public void addSpecialConnection(BlockPos crystal1, BlockPos crystal2, String label) {
        placeCrystal(crystal1);
        placeCrystal(crystal2);
        PortalManager.registerLink(world, crystal1, crystal2);
        System.out.println("[水晶链接] 添加特殊连接 (" + label + "): " + crystal1 + " <-> " + crystal2);
    }

    // 记录楼梯水晶位置，用于后续跨层连接
    private final Map<RoomNode, BlockPos> staircaseUpCrystals = new HashMap<>();
    private final Map<RoomNode, BlockPos> staircaseDownCrystals = new HashMap<>();

    /**
     * 放置楼梯房间的传送水晶
     * @param pos 水晶位置
     * @param room 楼梯房间节点
     * @param isUpward true=向上传送, false=向下传送
     */
    public void placeStaircaseCrystal(BlockPos pos, RoomNode room, boolean isUpward) {
        // 放置特殊颜色的水晶
        IBlockState crystalState = isUpward ?
                ModBlocks.UNBREAKABLE_BARRIER_VOID.getDefaultState() :  // 向上用默认水晶
                ModBlocks.UNBREAKABLE_BARRIER_ANCHOR.getDefaultState(); // 向下用锚点水晶

        world.setBlockState(pos, crystalState, 3);
        world.setBlockState(pos.down(), Blocks.SEA_LANTERN.getDefaultState(), 3);

        // 记录位置
        if (isUpward) {
            staircaseUpCrystals.put(room, pos);
        } else {
            staircaseDownCrystals.put(room, pos);
        }

        // 如果链接的楼梯房间已经有水晶，建立连接
        RoomNode linkedRoom = room.linkedStaircase;
        if (linkedRoom != null) {
            BlockPos linkedCrystal = isUpward ?
                    staircaseDownCrystals.get(linkedRoom) :
                    staircaseUpCrystals.get(linkedRoom);

            if (linkedCrystal != null) {
                PortalManager.registerLink(world, pos, linkedCrystal);
                String direction = isUpward ? "↑上层" : "↓下层";
                System.out.println("[水晶链接] 楼梯连接 " + direction + ": " + pos + " <-> " + linkedCrystal);
            }
        }
    }

    /**
     * 完成所有楼梯水晶的连接（在所有房间放置完成后调用）
     */
    public void finalizeStaircaseConnections() {
        for (Map.Entry<RoomNode, BlockPos> entry : staircaseUpCrystals.entrySet()) {
            RoomNode room = entry.getKey();
            BlockPos upCrystal = entry.getValue();

            if (room.linkedStaircase != null) {
                BlockPos downCrystal = staircaseDownCrystals.get(room.linkedStaircase);
                if (downCrystal != null) {
                    PortalManager.registerLink(world, upCrystal, downCrystal);
                    System.out.println("[水晶链接] 最终楼梯连接: " + upCrystal + " <-> " + downCrystal);
                }
            }
        }
    }
}