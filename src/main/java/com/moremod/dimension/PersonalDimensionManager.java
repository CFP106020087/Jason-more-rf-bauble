package com.moremod.dimension;

import com.moremod.init.ModBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 私人维度管理系统 - 修复版
 * 修复了只能在第一个存档创建维度的问题
 */
public class PersonalDimensionManager {

    // 私人维度ID
    public static final int PERSONAL_DIM_ID = 100;

    // 空间配置
    private static final int SPACE_WIDTH = 30;
    private static final int SPACE_HEIGHT = 15;
    private static final int SPACE_DEPTH = 30;
    private static final int WALL_THICKNESS = 1;
    private static final int SPACE_PADDING = 20;

    // 玩家空间分配 - 使用线程安全的集合
    private static final Map<UUID, PersonalSpace> playerSpaces = new ConcurrentHashMap<>();
    private static final Map<BlockPos, UUID> spaceOwners = new ConcurrentHashMap<>();
    private static int nextSpaceIndex = 0;

    // 墙壁恢复任务队列
    private static final Map<UUID, WallRestoreTask> wallRestoreTasks = new ConcurrentHashMap<>();

    // 记录已初始化的玩家
    private static final Set<UUID> initializedPlayers = Collections.synchronizedSet(new HashSet<>());

    // 待生成队列（用于延迟生成）
    private static final Map<UUID, Long> pendingGenerations = new ConcurrentHashMap<>();

    // 标记维度是否已经初始化
    private static boolean isDimensionInitialized = false;

    // 标记数据是否已加载
    private static boolean isDataLoaded = false;

    /**
     * 墙壁恢复任务
     */
    private static class WallRestoreTask {
        public final UUID playerId;
        public final long restoreTime;

        public WallRestoreTask(UUID playerId, long restoreTime) {
            this.playerId = playerId;
            this.restoreTime = restoreTime;
        }
    }

    /**
     * 玩家的私人空间数据
     */
    public static class PersonalSpace {
        public final UUID playerId;
        public final String playerName;
        public final BlockPos centerPos;
        public final BlockPos innerMinPos;
        public final BlockPos innerMaxPos;
        public final BlockPos outerMinPos;
        public final BlockPos outerMaxPos;
        public final long createdTime;
        public boolean isActive;
        public boolean isGenerated;
        public boolean hasVoidStructures;  // 新增：是否已生成虚空结构
        public final int index;

        public PersonalSpace(UUID playerId, String playerName, int index) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.index = index;
            this.createdTime = System.currentTimeMillis();
            this.isActive = true;
            this.isGenerated = false;
            this.hasVoidStructures = false;  // 初始化为false

            // 计算空间位置
            int gridSize = 10;
            int gridX = index % gridSize;
            int gridZ = index / gridSize;

            int totalSpacing = SPACE_WIDTH + WALL_THICKNESS * 2 + SPACE_PADDING;
            int centerX = gridX * totalSpacing;
            int centerZ = gridZ * totalSpacing;
            int centerY = 128;

            this.centerPos = new BlockPos(centerX, centerY, centerZ);

            // 内部空间边界
            this.innerMinPos = new BlockPos(
                    centerX - SPACE_WIDTH/2,
                    centerY - SPACE_HEIGHT/2,
                    centerZ - SPACE_DEPTH/2
            );
            this.innerMaxPos = new BlockPos(
                    centerX + SPACE_WIDTH/2,
                    centerY + SPACE_HEIGHT/2,
                    centerZ + SPACE_DEPTH/2
            );

            // 外墙边界
            this.outerMinPos = new BlockPos(
                    innerMinPos.getX() - WALL_THICKNESS,
                    innerMinPos.getY() - WALL_THICKNESS,
                    innerMinPos.getZ() - WALL_THICKNESS
            );
            this.outerMaxPos = new BlockPos(
                    innerMaxPos.getX() + WALL_THICKNESS,
                    innerMaxPos.getY() + WALL_THICKNESS,
                    innerMaxPos.getZ() + WALL_THICKNESS
            );
        }

        public boolean isInInnerSpace(BlockPos pos) {
            return pos.getX() >= innerMinPos.getX() && pos.getX() <= innerMaxPos.getX() &&
                    pos.getY() >= innerMinPos.getY() && pos.getY() <= innerMaxPos.getY() &&
                    pos.getZ() >= innerMinPos.getZ() && pos.getZ() <= innerMaxPos.getZ();
        }

        public boolean isInOuterSpace(BlockPos pos) {
            return pos.getX() >= outerMinPos.getX() && pos.getX() <= outerMaxPos.getX() &&
                    pos.getY() >= outerMinPos.getY() && pos.getY() <= outerMaxPos.getY() &&
                    pos.getZ() >= outerMinPos.getZ() && pos.getZ() <= outerMaxPos.getZ();
        }

        public boolean isWall(BlockPos pos) {
            return isInOuterSpace(pos) && !isInInnerSpace(pos);
        }
    }

    /**
     * 初始化私人维度管理器 - 改进版
     */
    public static void init() {
        System.out.println("[口袋空间] 系统初始化开始...");

        // 只加载数据，不重置（避免清空已有数据）
        if (!isDataLoaded) {
            loadPlayerSpaces();
            isDataLoaded = true;
        }

        System.out.println("[口袋空间] 系统初始化完成");
    }

    /**
     * 完全初始化（包括重置）
     */
    public static void fullInit() {
        System.out.println("[口袋空间] 完全初始化开始...");

        // 重置状态
        reset();

        // 加载数据
        loadPlayerSpaces();
        isDataLoaded = true;

        // 确保维度被加载
        ensureDimensionLoaded();

        isDimensionInitialized = true;
        System.out.println("[口袋空间] 完全初始化完成");
    }

    /**
     * 重置所有静态数据（用于世界切换）
     */
    public static void reset() {
        System.out.println("[口袋空间] 重置管理器状态");
        playerSpaces.clear();
        spaceOwners.clear();
        wallRestoreTasks.clear();
        initializedPlayers.clear();
        pendingGenerations.clear();
        nextSpaceIndex = 0;
        isDimensionInitialized = false;
        isDataLoaded = false;
    }

    /**
     * 确保维度被加载 - 改进版
     */
    private static void ensureDimensionLoaded() {
        // 如果已经初始化，直接返回
        if (isDimensionInitialized) {
            return;
        }

        // 先检查主世界是否已加载
        WorldServer overworld = DimensionManager.getWorld(0);
        if (overworld == null) {
            System.out.println("[口袋空间] 主世界未加载，跳过维度初始化");
            return;
        }

        // 先检查维度是否注册
        if (!DimensionManager.isDimensionRegistered(PERSONAL_DIM_ID)) {
            System.out.println("[口袋空间] 警告：维度未注册！尝试注册维度...");
            PersonalDimensionType.registerDimension();

            // 再次检查
            if (!DimensionManager.isDimensionRegistered(PERSONAL_DIM_ID)) {
                System.err.println("[口袋空间] 错误：无法注册维度！");
                return;
            }
        }

        // 标记为保持加载，但不立即初始化
        DimensionManager.keepDimensionLoaded(PERSONAL_DIM_ID, true);
        System.out.println("[口袋空间] 维度已标记为保持加载");
    }

    /**
     * 获取私人维度世界（确保已加载）
     */
    private static WorldServer getPersonalDimensionWorld() {
        // 先检查主世界是否已加载
        WorldServer overworld = DimensionManager.getWorld(0);
        if (overworld == null) {
            System.out.println("[口袋空间] 主世界未加载，无法获取私人维度");
            return null;
        }

        // 直接尝试获取维度世界
        WorldServer world = DimensionManager.getWorld(PERSONAL_DIM_ID);
        if (world != null) {
            return world;
        }

        // 只在维度不存在时才初始化
        if (!isDimensionInitialized) {
            System.out.println("[口袋空间] 初始化维度世界...");

            try {
                // 确保维度已注册
                if (!DimensionManager.isDimensionRegistered(PERSONAL_DIM_ID)) {
                    System.err.println("[口袋空间] 维度未注册！");
                    return null;
                }

                // 保持维度加载
                DimensionManager.keepDimensionLoaded(PERSONAL_DIM_ID, true);

                // 初始化维度
                DimensionManager.initDimension(PERSONAL_DIM_ID);
                world = DimensionManager.getWorld(PERSONAL_DIM_ID);

                if (world != null) {
                    isDimensionInitialized = true;
                    System.out.println("[口袋空间] 维度初始化成功");
                } else {
                    System.err.println("[口袋空间] 维度初始化失败");
                }
            } catch (Exception e) {
                System.err.println("[口袋空间] 维度初始化异常: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return world;
    }

    /**
     * 世界加载事件 - 改进版
     */
    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld().isRemote) return;

        // 如果是主世界被加载
        if (event.getWorld().provider.getDimension() == 0) {
            System.out.println("[口袋空间] 主世界加载");

            // 如果数据还未加载，立即加载
            if (!isDataLoaded) {
                loadPlayerSpaces();
                isDataLoaded = true;
            }
        }
        // 如果是私人维度被加载
        else if (event.getWorld().provider.getDimension() == PERSONAL_DIM_ID) {
            System.out.println("[口袋空间] 私人维度世界加载");

            // 标记维度已初始化
            isDimensionInitialized = true;

            // 保持维度加载
            DimensionManager.keepDimensionLoaded(PERSONAL_DIM_ID, true);

            // 不在这里立即生成空间，等待玩家实际需要时再生成
            System.out.println("[口袋空间] 维度已准备就绪");
        }
    }

    /**
     * 世界卸载事件
     */
    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) return;

        // 如果是服务器关闭，保存数据
        if (event.getWorld().provider.getDimension() == 0) {
            System.out.println("[口袋空间] 主世界卸载，保存数据");
            savePlayerSpacesInternal();
        }
    }

    /**
     * 服务器Tick事件（定期保存 & 延迟空间生成 & 墙体恢复）
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // 处理待生成队列 - 简化逻辑，避免频繁获取维度
        if (!pendingGenerations.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            Iterator<Map.Entry<UUID, Long>> it = pendingGenerations.entrySet().iterator();

            // 每tick只处理一个生成任务
            while (it.hasNext()) {
                Map.Entry<UUID, Long> entry = it.next();
                if (currentTime >= entry.getValue()) {
                    PersonalSpace space = playerSpaces.get(entry.getKey());
                    if (space != null && !space.isGenerated) {
                        // 只在真正需要生成时才获取世界
                        WorldServer world = DimensionManager.getWorld(PERSONAL_DIM_ID);
                        if (world != null) {
                            System.out.println("[口袋空间] 生成空间: " + space.playerName);
                            generateCompleteSpace(world, space);
                        }
                    }
                    it.remove();
                    break; // 每tick只处理一个
                }
            }
        }

        // 处理墙壁恢复任务
        if (!wallRestoreTasks.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            Iterator<Map.Entry<UUID, WallRestoreTask>> it = wallRestoreTasks.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<UUID, WallRestoreTask> entry = it.next();
                WallRestoreTask task = entry.getValue();

                if (currentTime >= task.restoreTime) {
                    PersonalSpace space = playerSpaces.get(task.playerId);
                    if (space != null && space.isGenerated) {
                        WorldServer world = DimensionManager.getWorld(PERSONAL_DIM_ID);
                        if (world != null) {
                            restoreAnchorWalls(world, space);
                        }
                    }
                    it.remove();
                }
            }
        }

        // 定期保存数据（约每5分钟一次）
        if (event.side.isServer() && System.currentTimeMillis() % 6000 == 0) {
            savePlayerSpaces();
        }
    }

    /**
     * 玩家加入游戏事件 - 修复版
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        UUID playerId = player.getUniqueID();

        // 确保数据已加载
        if (!isDataLoaded) {
            loadPlayerSpaces();
            isDataLoaded = true;
        }

        PersonalSpace existingSpace = playerSpaces.get(playerId);

        if (existingSpace == null) {
            System.out.println("[口袋空间] 为玩家 " + player.getName() + " 创建新空间");

            // 创建空间数据但延迟生成
            PersonalSpace space = new PersonalSpace(playerId, player.getName(), nextSpaceIndex++);
            playerSpaces.put(playerId, space);

            // 注册空间所有权（按区块粗粒度）
            for (int x = space.outerMinPos.getX(); x <= space.outerMaxPos.getX(); x += 16) {
                for (int z = space.outerMinPos.getZ(); z <= space.outerMaxPos.getZ(); z += 16) {
                    spaceOwners.put(new BlockPos(x, 0, z), playerId);
                }
            }

            // 延迟3秒生成
            pendingGenerations.put(playerId, System.currentTimeMillis() + 3000);

            savePlayerSpaces();
        } else {
            System.out.println("[口袋空间] 玩家 " + player.getName() + " 已有空间");

            // 如果空间未生成，安排生成
            if (!existingSpace.isGenerated) {
                pendingGenerations.put(playerId, System.currentTimeMillis() + 3000);
            }
        }
    }

    /**
     * 玩家退出时清理
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player.world.isRemote) return;

        UUID playerId = event.player.getUniqueID();
        initializedPlayers.remove(playerId);
        pendingGenerations.remove(playerId);

        // 保存当前数据
        savePlayerSpaces();
    }

    /**
     * 传送玩家到私人空间 - 修复版
     */
    public static void teleportToPersonalSpace(EntityPlayer player) {
        if (player.world.isRemote) return;

        UUID playerId = player.getUniqueID();

        // 确保数据已加载
        if (!isDataLoaded) {
            loadPlayerSpaces();
            isDataLoaded = true;
        }

        PersonalSpace space = playerSpaces.get(playerId);

        if (space == null) {
            System.out.println("[口袋空间] 传送时创建新空间: " + player.getName());

            space = new PersonalSpace(playerId, player.getName(), nextSpaceIndex++);
            playerSpaces.put(playerId, space);

            for (int x = space.outerMinPos.getX(); x <= space.outerMaxPos.getX(); x += 16) {
                for (int z = space.outerMinPos.getZ(); z <= space.outerMaxPos.getZ(); z += 16) {
                    spaceOwners.put(new BlockPos(x, 0, z), playerId);
                }
            }

            savePlayerSpaces();
        }

        WorldServer personalWorld = getPersonalDimensionWorld();
        if (personalWorld == null) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "无法加载私人维度！请稍后再试。"
            ), true);
            return;
        }

        // 确保空间已生成
        if (!space.isGenerated) {
            System.out.println("[口袋空间] 立即生成空间: " + space.centerPos);
            generateCompleteSpace(personalWorld, space);
        }

        // 老空间补一次结构（打补丁前已生成的存档）
        if (!space.hasVoidStructures) {
            maybeGenerateVoidStructures(personalWorld, space);
        }

        // 传送玩家
        if (player.dimension != PERSONAL_DIM_ID) {
            player.changeDimension(PERSONAL_DIM_ID, new PersonalTeleporter(personalWorld, space.centerPos));
        } else {
            ((EntityPlayerMP)player).connection.setPlayerLocation(
                    space.centerPos.getX() + 0.5,
                    space.centerPos.getY(),
                    space.centerPos.getZ() + 0.5,
                    player.rotationYaw,
                    player.rotationPitch
            );
        }

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.GREEN + "已传送到你的私人空间"
        ), true);
    }

    // ==================== 空间生成相关方法 ====================

    /**
     * 一次性生成完整空间
     */
    private static void generateCompleteSpace(World world, PersonalSpace space) {
        if (space.isGenerated) {
            System.out.println("[口袋空间] 空间已生成，跳过: " + space.playerName);
            return;
        }

        // 强制加载自身区块
        for (int cx = (space.outerMinPos.getX() >> 4); cx <= (space.outerMaxPos.getX() >> 4); cx++) {
            for (int cz = (space.outerMinPos.getZ() >> 4); cz <= (space.outerMaxPos.getZ() >> 4); cz++) {
                world.getChunk(cx, cz);
            }
        }

        long startTime = System.currentTimeMillis();
        System.out.println("[口袋空间] 开始生成空间: " + space.centerPos);

        IBlockState wallBlock;
        try {
            wallBlock = ModBlocks.UNBREAKABLE_BARRIER_ANCHOR.getDefaultState();
            System.out.println("[口袋空间] 使用维度锚定屏障");
        } catch (Exception e) {
            wallBlock = Blocks.BEDROCK.getDefaultState();
            System.out.println("[口袋空间] 使用基岩作为墙壁");
        }

        generateWalls(world, space, wallBlock);
        clearInterior(world, space);
        generateFloor(world, space);
        addInfrastructure(world, space);

        space.isGenerated = true;

        // === 新增：生成一次虚空结构 ===
        if (world instanceof WorldServer) {
            maybeGenerateVoidStructures((WorldServer) world, space);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("[口袋空间] 空间生成完成，耗时: " + (endTime - startTime) + "ms");

        savePlayerSpaces();
    }

    /**
     * 在玩家空间周边生成一次虚空结构（只执行一次）
     */
    private static void maybeGenerateVoidStructures(WorldServer world, PersonalSpace space) {
        if (space.hasVoidStructures) {
            return;
        }

        final int maxDistance = 200; // 与 VoidStructureGenerator 的最远生成距离保持一致
        final int minChunkX = (space.centerPos.getX() - maxDistance) >> 4;
        final int maxChunkX = (space.centerPos.getX() + maxDistance) >> 4;
        final int minChunkZ = (space.centerPos.getZ() - maxDistance) >> 4;
        final int maxChunkZ = (space.centerPos.getZ() + maxDistance) >> 4;

        // 预加载外围区块，避免 setBlockState 引发同步加载卡顿
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                world.getChunk(cx, cz);
            }
        }

        System.out.println("[口袋空间] 为 " + space.playerName + " 生成虚空结构（<=200格）...");
        VoidStructureGenerator.generateNearbyStructures(world, space.centerPos, maxDistance);
        space.hasVoidStructures = true;
        savePlayerSpaces();
    }

    /**
     * 生成所有墙壁
     */
    private static void generateWalls(World world, PersonalSpace space, IBlockState wallBlock) {
        // 底面
        int y = space.outerMinPos.getY();
        for (int x = space.outerMinPos.getX(); x <= space.outerMaxPos.getX(); x++) {
            for (int z = space.outerMinPos.getZ(); z <= space.outerMaxPos.getZ(); z++) {
                world.setBlockState(new BlockPos(x, y, z), wallBlock, 2);
            }
        }

        // 顶面
        y = space.outerMaxPos.getY();
        for (int x = space.outerMinPos.getX(); x <= space.outerMaxPos.getX(); x++) {
            for (int z = space.outerMinPos.getZ(); z <= space.outerMaxPos.getZ(); z++) {
                world.setBlockState(new BlockPos(x, y, z), wallBlock, 2);
            }
        }

        // 四个侧面
        for (y = space.outerMinPos.getY() + 1; y < space.outerMaxPos.getY(); y++) {
            // 前后墙
            for (int x = space.outerMinPos.getX(); x <= space.outerMaxPos.getX(); x++) {
                world.setBlockState(new BlockPos(x, y, space.outerMinPos.getZ()), wallBlock, 2);
                world.setBlockState(new BlockPos(x, y, space.outerMaxPos.getZ()), wallBlock, 2);
            }
            // 左右墙
            for (int z = space.outerMinPos.getZ() + 1; z < space.outerMaxPos.getZ(); z++) {
                world.setBlockState(new BlockPos(space.outerMinPos.getX(), y, z), wallBlock, 2);
                world.setBlockState(new BlockPos(space.outerMaxPos.getX(), y, z), wallBlock, 2);
            }
        }
    }

    /**
     * 清空内部空间
     */
    private static void clearInterior(World world, PersonalSpace space) {
        IBlockState air = Blocks.AIR.getDefaultState();

        for (int x = space.innerMinPos.getX(); x <= space.innerMaxPos.getX(); x++) {
            for (int y = space.innerMinPos.getY(); y <= space.innerMaxPos.getY(); y++) {
                for (int z = space.innerMinPos.getZ(); z <= space.innerMaxPos.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (world.getBlockState(pos).getBlock() != Blocks.AIR) {
                        world.setBlockState(pos, air, 2);
                    }
                }
            }
        }
    }

    /**
     * 生成地板
     */
    private static void generateFloor(World world, PersonalSpace space) {
        int floorY = space.innerMinPos.getY();

        for (int x = space.innerMinPos.getX(); x <= space.innerMaxPos.getX(); x++) {
            for (int z = space.innerMinPos.getZ(); z <= space.innerMaxPos.getZ(); z++) {
                BlockPos pos = new BlockPos(x, floorY, z);

                int dx = Math.abs(x - space.centerPos.getX());
                int dz = Math.abs(z - space.centerPos.getZ());

                if (dx <= 1 && dz <= 1) {
                    world.setBlockState(pos, Blocks.SEA_LANTERN.getDefaultState(), 2);
                } else if (dx <= 2 && dz <= 2) {
                    world.setBlockState(pos, Blocks.QUARTZ_BLOCK.getDefaultState(), 2);
                } else {
                    world.setBlockState(pos, Blocks.STONE.getDefaultState(), 2);
                }
            }
        }
    }

    /**
     * 添加基础设施
     */
    private static void addInfrastructure(World world, PersonalSpace space) {
        int floorY = space.innerMinPos.getY() + 1;

        BlockPos[] corners = {
                new BlockPos(space.innerMinPos.getX() + 2, floorY, space.innerMinPos.getZ() + 2),
                new BlockPos(space.innerMaxPos.getX() - 2, floorY, space.innerMinPos.getZ() + 2),
                new BlockPos(space.innerMinPos.getX() + 2, floorY, space.innerMaxPos.getZ() - 2),
                new BlockPos(space.innerMaxPos.getX() - 2, floorY, space.innerMaxPos.getZ() - 2)
        };

        for (BlockPos corner : corners) {
            world.setBlockState(corner, Blocks.GLOWSTONE.getDefaultState(), 2);
            world.setBlockState(corner.up(), Blocks.END_ROD.getDefaultState(), 2);
        }

        int lightY = space.innerMinPos.getY() + 4;
        int spacing = 10;

        for (int i = space.innerMinPos.getX() + spacing; i < space.innerMaxPos.getX(); i += spacing) {
            world.setBlockState(new BlockPos(i, lightY, space.innerMinPos.getZ() + 1),
                    Blocks.GLOWSTONE.getDefaultState(), 2);
            world.setBlockState(new BlockPos(i, lightY, space.innerMaxPos.getZ() - 1),
                    Blocks.GLOWSTONE.getDefaultState(), 2);
        }
    }

    /**
     * 恢复空间的维度锚定墙壁
     */
    private static void restoreAnchorWalls(World world, PersonalSpace space) {
        IBlockState wallBlock;
        try {
            wallBlock = ModBlocks.UNBREAKABLE_BARRIER_ANCHOR.getDefaultState();
        } catch (Exception e) {
            wallBlock = Blocks.BEDROCK.getDefaultState();
            System.out.println("[口袋空间] 使用基岩恢复墙壁");
        }

        System.out.println("[口袋空间] 开始恢复维度锚定墙壁: " + space.playerName);

        // 恢复底面
        int y = space.outerMinPos.getY();
        for (int x = space.outerMinPos.getX(); x <= space.outerMaxPos.getX(); x++) {
            for (int z = space.outerMinPos.getZ(); z <= space.outerMaxPos.getZ(); z++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (world.getBlockState(pos).getBlock() == Blocks.AIR) {
                    world.setBlockState(pos, wallBlock, 3);
                    spawnRestoreParticles(world, pos);
                }
            }
        }

        // 恢复顶面
        y = space.outerMaxPos.getY();
        for (int x = space.outerMinPos.getX(); x <= space.outerMaxPos.getX(); x++) {
            for (int z = space.outerMinPos.getZ(); z <= space.outerMaxPos.getZ(); z++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (world.getBlockState(pos).getBlock() == Blocks.AIR) {
                    world.setBlockState(pos, wallBlock, 3);
                    spawnRestoreParticles(world, pos);
                }
            }
        }

        // 恢复四个侧面
        for (y = space.outerMinPos.getY() + 1; y < space.outerMaxPos.getY(); y++) {
            // 前后墙
            for (int x = space.outerMinPos.getX(); x <= space.outerMaxPos.getX(); x++) {
                BlockPos frontPos = new BlockPos(x, y, space.outerMinPos.getZ());
                BlockPos backPos = new BlockPos(x, y, space.outerMaxPos.getZ());

                if (world.getBlockState(frontPos).getBlock() == Blocks.AIR) {
                    world.setBlockState(frontPos, wallBlock, 3);
                    spawnRestoreParticles(world, frontPos);
                }
                if (world.getBlockState(backPos).getBlock() == Blocks.AIR) {
                    world.setBlockState(backPos, wallBlock, 3);
                    spawnRestoreParticles(world, backPos);
                }
            }

            // 左右墙
            for (int z = space.outerMinPos.getZ() + 1; z < space.outerMaxPos.getZ(); z++) {
                BlockPos leftPos = new BlockPos(space.outerMinPos.getX(), y, z);
                BlockPos rightPos = new BlockPos(space.outerMaxPos.getX(), y, z);

                if (world.getBlockState(leftPos).getBlock() == Blocks.AIR) {
                    world.setBlockState(leftPos, wallBlock, 3);
                    spawnRestoreParticles(world, leftPos);
                }
                if (world.getBlockState(rightPos).getBlock() == Blocks.AIR) {
                    world.setBlockState(rightPos, wallBlock, 3);
                    spawnRestoreParticles(world, rightPos);
                }
            }
        }

        System.out.println("[口袋空间] 维度锚定墙壁恢复完成");
    }

    /**
     * 生成恢复粒子效果
     */
    private static void spawnRestoreParticles(World world, BlockPos pos) {
        for (int i = 0; i < 3; i++) {
            double x = pos.getX() + 0.5 + (world.rand.nextDouble() - 0.5) * 0.5;
            double y = pos.getY() + 0.5 + (world.rand.nextDouble() - 0.5) * 0.5;
            double z = pos.getZ() + 0.5 + (world.rand.nextDouble() - 0.5) * 0.5;
            world.spawnParticle(EnumParticleTypes.SPELL_WITCH, x, y, z, 0, 0.1, 0);
        }
    }

    // ==================== 事件处理方法 ====================

    /**
     * 方块破坏事件 - 只保护维度锚定墙壁
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().provider.getDimension() != PERSONAL_DIM_ID) return;

        EntityPlayer player = event.getPlayer();
        BlockPos pos = event.getPos();
        IBlockState state = event.getState();

        PersonalSpace space = findSpaceByPos(pos);
        if (space == null) {
            return;
        }

        // 检查是否是维度锚定墙壁
        if (state.getBlock().getRegistryName() != null) {
            String blockName = state.getBlock().getRegistryName().toString();

            // 只保护维度锚定屏障墙壁
            if (blockName.contains("unbreakable_barrier") && space.isWall(pos)) {
                event.setCanceled(true);
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 维度锚定墙壁不可破坏！"
                ), true);
                return;
            }
        }

        // 检查是否在其他玩家的空间内
        if (!space.playerId.equals(player.getUniqueID()) && space.isInInnerSpace(pos)) {
            event.setCanceled(true);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "你不能破坏其他玩家的私人空间！"
            ), true);
        }
    }

    /**
     * 方块放置事件 - 限制在维度锚定墙壁上放置
     */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.PlaceEvent event) {
        if (event.getWorld().provider.getDimension() != PERSONAL_DIM_ID) return;

        EntityPlayer player = event.getPlayer();
        BlockPos pos = event.getPos();

        PersonalSpace space = findSpaceByPos(pos);
        if (space == null) {
            return;
        }

        // 检查是否试图在维度锚定墙壁位置放置方块
        if (space.isWall(pos)) {
            IBlockState existingState = event.getWorld().getBlockState(pos);
            if (existingState.getBlock().getRegistryName() != null) {
                String blockName = existingState.getBlock().getRegistryName().toString();

                // 只有在维度锚定墙壁位置才阻止放置
                if (blockName.contains("unbreakable_barrier")) {
                    event.setCanceled(true);
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.RED + "不能在维度锚定墙壁上放置方块！"
                    ), true);
                    return;
                }
            }
        }

        // 检查是否在其他玩家的空间内
        if (!space.playerId.equals(player.getUniqueID()) && space.isInInnerSpace(pos)) {
            event.setCanceled(true);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "你不能在其他玩家的私人空间放置方块！"
            ), true);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 安排墙壁恢复任务
     */
    public static void scheduleWallRestore(UUID playerId, int ticksDelay) {
        long restoreTime = System.currentTimeMillis() + (ticksDelay * 50L);
        WallRestoreTask task = new WallRestoreTask(playerId, restoreTime);
        wallRestoreTasks.put(playerId, task);

        System.out.println("[口袋空间] 已安排墙壁恢复，将在 " + (ticksDelay/20) + " 秒后执行");
    }

    private static PersonalSpace findSpaceByPos(BlockPos pos) {
        for (PersonalSpace space : playerSpaces.values()) {
            if (space.isInOuterSpace(pos)) {
                return space;
            }
        }
        return null;
    }

    public static PersonalSpace getPlayerSpace(UUID playerId) {
        return playerSpaces.get(playerId);
    }

    public static boolean hasPersonalSpace(UUID playerId) {
        return playerSpaces.containsKey(playerId);
    }

    public static Collection<PersonalSpace> getAllSpaces() {
        return playerSpaces.values();
    }

    public static PersonalSpace getOrCreateSpace(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        PersonalSpace space = playerSpaces.get(playerId);

        if (space == null) {
            space = new PersonalSpace(playerId, player.getName(), nextSpaceIndex++);
            playerSpaces.put(playerId, space);

            for (int x = space.outerMinPos.getX(); x <= space.outerMaxPos.getX(); x += 16) {
                for (int z = space.outerMinPos.getZ(); z <= space.outerMaxPos.getZ(); z += 16) {
                    spaceOwners.put(new BlockPos(x, 0, z), playerId);
                }
            }

            WorldServer world = getPersonalDimensionWorld();
            if (world != null && !space.isGenerated) {
                generateCompleteSpace(world, space);
            }

            savePlayerSpaces();
        }

        return space;
    }

    // ==================== 数据保存和加载 ====================

    /**
     * 公共的保存方法
     */
    public static void savePlayerSpaces() {
        savePlayerSpacesInternal();
    }

    private static void savePlayerSpacesInternal() {
        try {
            File saveFile = getSaveFile();
            NBTTagCompound compound = new NBTTagCompound();

            compound.setInteger("nextIndex", nextSpaceIndex);

            NBTTagCompound spaces = new NBTTagCompound();
            for (Map.Entry<UUID, PersonalSpace> entry : playerSpaces.entrySet()) {
                NBTTagCompound spaceData = new NBTTagCompound();
                PersonalSpace space = entry.getValue();

                spaceData.setString("playerName", space.playerName);
                spaceData.setInteger("index", space.index);
                spaceData.setBoolean("isGenerated", space.isGenerated);
                spaceData.setBoolean("hasVoidStructures", space.hasVoidStructures);
                spaceData.setLong("createdTime", space.createdTime);
                spaceData.setBoolean("isActive", space.isActive);

                spaces.setTag(entry.getKey().toString(), spaceData);
            }
            compound.setTag("spaces", spaces);

            if (!saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(saveFile);
                 DataOutputStream dos = new DataOutputStream(fos)) {
                net.minecraft.nbt.CompressedStreamTools.writeCompressed(compound, dos);
            }

            System.out.println("[口袋空间] 已保存 " + playerSpaces.size() + " 个玩家空间");
        } catch (Exception e) {
            System.err.println("[口袋空间] 保存数据失败！");
            e.printStackTrace();
        }
    }

    private static void loadPlayerSpaces() {
        try {
            File saveFile = getSaveFile();
            if (!saveFile.exists()) {
                System.out.println("[口袋空间] 没有找到保存文件，使用空数据");
                return;
            }

            NBTTagCompound compound;
            try (FileInputStream fis = new FileInputStream(saveFile);
                 DataInputStream dis = new DataInputStream(fis)) {
                compound = net.minecraft.nbt.CompressedStreamTools.readCompressed(dis);
            }

            playerSpaces.clear();
            spaceOwners.clear();

            nextSpaceIndex = compound.getInteger("nextIndex");

            NBTTagCompound spaces = compound.getCompoundTag("spaces");
            for (String key : spaces.getKeySet()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    NBTTagCompound spaceData = spaces.getCompoundTag(key);

                    String playerName = spaceData.getString("playerName");
                    int index = spaceData.getInteger("index");

                    PersonalSpace space = new PersonalSpace(playerId, playerName, index);
                    space.isActive = spaceData.getBoolean("isActive");
                    space.isGenerated = spaceData.getBoolean("isGenerated");
                    space.hasVoidStructures = spaceData.hasKey("hasVoidStructures") ?
                            spaceData.getBoolean("hasVoidStructures") : false;

                    playerSpaces.put(playerId, space);

                    // 重建空间所有权映射
                    for (int x = space.outerMinPos.getX(); x <= space.outerMaxPos.getX(); x += 16) {
                        for (int z = space.outerMinPos.getZ(); z <= space.outerMaxPos.getZ(); z += 16) {
                            spaceOwners.put(new BlockPos(x, 0, z), playerId);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[口袋空间] 加载空间数据失败: " + key);
                    e.printStackTrace();
                }
            }

            isDataLoaded = true;
            System.out.println("[口袋空间] 已加载 " + playerSpaces.size() + " 个玩家空间");
        } catch (Exception e) {
            System.err.println("[口袋空间] 加载数据失败！");
            e.printStackTrace();
        }
    }

    /**
     * 获取保存文件路径
     */
    private static File getSaveFile() {
        // 使用世界相关的保存路径
        WorldServer overworld = DimensionManager.getWorld(0);
        if (overworld != null) {
            File worldDir = overworld.getSaveHandler().getWorldDirectory();
            return new File(worldDir, "data/personal_dimensions.dat");
        } else {
            // 备用路径
            return new File("config/personal_dimensions.dat");
        }
    }
}
