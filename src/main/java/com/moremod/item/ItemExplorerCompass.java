package com.moremod.item;

import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.BaubleType;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 探险者罗盘 - Waystones 支持版
 *
 * 功能:
 * 1. Shift + 左键: 循环切换目标类型并定位
 *    - 原版结构: 村庄、要塞、林地府邸等
 *    - 特殊目标: 箱子、地牢、传送石碑 (Waystones)
 * 2. Shift + 右键: 生成粒子导航路径
 * 3. 支持 Baubles 饰品佩戴
 * 4. 箱子高亮功能
 *
 * Waystones 支持:
 * - 使用反射实现，无需硬依赖
 * - 自动检测 Waystones mod 是否存在
 * - 查找最近的传送石碑
 * - 显示石碑名称
 */
@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ItemExplorerCompass extends Item implements IBauble {

    // ===== NBT 键名 =====
    private static final String NBT_TARGET_X = "TargetX";
    private static final String NBT_TARGET_Y = "TargetY";
    private static final String NBT_TARGET_Z = "TargetZ";
    private static final String NBT_HAS_TARGET = "HasTarget";
    private static final String NBT_TARGET_TYPE = "TargetType";
    private static final String NBT_TARGET_NAME = "TargetName"; // 用于存储石碑名称
    private static final String NBT_CHEST_HIGHLIGHT = "ChestHighlight";

    // ===== 搜索配置 =====
    private static final int STRUCTURE_SEARCH_RADIUS = 2000;
    private static final int CHEST_SEARCH_RADIUS = 64;
    private static final int DUNGEON_SEARCH_RADIUS = 128;
    private static final int WAYSTONE_SEARCH_RADIUS = 128;

    // ===== Waystones 反射缓存 =====
    private static Boolean WAYSTONES_LOADED = null;
    private static Class<?> CLASS_BLOCK_WAYSTONE = null;
    private static Class<?> CLASS_TILE_WAYSTONE = null;
    private static Method METHOD_GET_TILE_WAYSTONE = null;
    private static Method METHOD_GET_WAYSTONE_NAME = null;

    // ===== 目标类型枚举 =====
    public enum TargetType {
        // 原版结构
        VILLAGE("Village", "村庄", true),
        STRONGHOLD("Stronghold", "要塞", true),
        MANSION("Mansion", "林地府邸", true),
        MONUMENT("Monument", "海底神殿", true),
        TEMPLE("Temple", "神殿", true),
        MINESHAFT("Mineshaft", "废弃矿井", true),
        FORTRESS("Fortress", "下界要塞", true),

        // 特殊目标
        CHEST("Chest", "箱子", false),
        DUNGEON("Dungeon", "地牢", false),
        WAYSTONE("Waystone", "传送石碑", false); // Waystones mod

        public final String id;
        public final String displayName;
        public final boolean isStructure;

        TargetType(String id, String displayName, boolean isStructure) {
            this.id = id;
            this.displayName = displayName;
            this.isStructure = isStructure;
        }
    }

    public ItemExplorerCompass() {
        setMaxStackSize(1);
        setTranslationKey("explorer_compass");
        setRegistryName("explorer_compass");
    }

    // ===== Baubles 接口实现 =====

    @Override
    @Optional.Method(modid = "baubles")
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.TRINKET;
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (!player.world.isRemote && player instanceof EntityPlayer) {
            ((EntityPlayer) player).sendMessage(new TextComponentString("§a探险者罗盘已装备"));
        }
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (!player.world.isRemote && player instanceof EntityPlayer) {
            ((EntityPlayer) player).sendMessage(new TextComponentString("§c探险者罗盘已卸下"));
        }
    }

    // ===== 获取装备的罗盘 =====

    public static ItemStack getEquippedCompass(EntityPlayer player) {
        if (Loader.isModLoaded("baubles")) {
            ItemStack baubleCompass = getCompassFromBaubles(player);
            if (!baubleCompass.isEmpty()) {
                return baubleCompass;
            }
        }

        ItemStack mainHand = player.getHeldItemMainhand();
        if (mainHand.getItem() instanceof ItemExplorerCompass) {
            return mainHand;
        }

        ItemStack offHand = player.getHeldItemOffhand();
        if (offHand.getItem() instanceof ItemExplorerCompass) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    @Optional.Method(modid = "baubles")
    private static ItemStack getCompassFromBaubles(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (stack.getItem() instanceof ItemExplorerCompass) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean isEquipped(EntityPlayer player) {
        return !getEquippedCompass(player).isEmpty();
    }

    // ===== Waystones 反射初始化 =====

    /**
     * 初始化 Waystones 反射
     */
    private static void initWaystonesReflection() {
        if (WAYSTONES_LOADED != null) {
            return; // 已经初始化过
        }

        WAYSTONES_LOADED = Loader.isModLoaded("waystones");

        if (!WAYSTONES_LOADED) {
            System.out.println("[MoreMod] Waystones mod 未安装，石碑搜索功能不可用");
            return;
        }

        try {
            // 加载 BlockWaystone 类
            CLASS_BLOCK_WAYSTONE = Class.forName("net.blay09.mods.waystones.block.BlockWaystone");

            // 加载 TileWaystone 类
            CLASS_TILE_WAYSTONE = Class.forName("net.blay09.mods.waystones.block.TileWaystone");

            // 获取 getTileWaystone 方法
            METHOD_GET_TILE_WAYSTONE = CLASS_BLOCK_WAYSTONE.getDeclaredMethod(
                    "getTileWaystone",
                    World.class,
                    BlockPos.class
            );
            METHOD_GET_TILE_WAYSTONE.setAccessible(true);

            // 获取 getWaystoneName 方法
            METHOD_GET_WAYSTONE_NAME = CLASS_TILE_WAYSTONE.getDeclaredMethod("getWaystoneName");
            METHOD_GET_WAYSTONE_NAME.setAccessible(true);

            System.out.println("[MoreMod] Waystones 反射初始化成功");

        } catch (Exception e) {
            System.err.println("[MoreMod] Waystones 反射初始化失败: " + e.getMessage());
            e.printStackTrace();
            WAYSTONES_LOADED = false;
        }
    }

    /**
     * 检查是否支持 Waystones
     */
    private static boolean isWaystonesAvailable() {
        initWaystonesReflection();
        return WAYSTONES_LOADED != null && WAYSTONES_LOADED;
    }

    // ===== 左键处理 - 循环切换目标类型并定位 =====

    public static void handleLeftClick(EntityPlayerMP player) {
        ItemStack compass = getEquippedCompass(player);
        if (compass.isEmpty()) {
            player.sendMessage(new TextComponentString("§c请先装备探险者罗盘!"));
            return;
        }

        TargetType currentType = getCurrentTargetType(compass);
        TargetType nextType = getNextTargetType(currentType, player);

        // 如果是传送石碑但 Waystones 不可用，跳过
        if (nextType == TargetType.WAYSTONE && !isWaystonesAvailable()) {
            player.sendMessage(new TextComponentString("§c需要安装 Waystones mod 才能使用石碑搜索!"));
            // 继续切换到下一个类型
            nextType = getNextTargetType(nextType, player);
        }

        // 根据类型查找目标
        WaystoneSearchResult result = findTarget(player, nextType);

        if (result == null || result.pos == null) {
            int radius = nextType.isStructure ? STRUCTURE_SEARCH_RADIUS :
                    (nextType == TargetType.CHEST ? CHEST_SEARCH_RADIUS :
                            (nextType == TargetType.WAYSTONE ? WAYSTONE_SEARCH_RADIUS : DUNGEON_SEARCH_RADIUS));
            player.sendMessage(new TextComponentString(
                    String.format("§c附近 %d 格内没有找到 %s!", radius, nextType.displayName)
            ));
            return;
        }

        // 保存目标
        setTargetPosition(compass, result.pos, nextType, result.name);

        double distance = player.getPosition().getDistance(
                result.pos.getX(),
                result.pos.getY(),
                result.pos.getZ()
        );

        String message = result.name != null ?
                String.format("§a已定位 %s §7[§e%s§7] §7[%d, %d, %d] §e距离: %.0f格",
                        nextType.displayName,
                        result.name,
                        result.pos.getX(),
                        result.pos.getY(),
                        result.pos.getZ(),
                        distance) :
                String.format("§a已定位 %s §7[%d, %d, %d] §e距离: %.0f格",
                        nextType.displayName,
                        result.pos.getX(),
                        result.pos.getY(),
                        result.pos.getZ(),
                        distance);

        player.sendMessage(new TextComponentString(message));

        // 播放音效
        player.world.playSound(
                null,
                player.getPosition(),
                net.minecraft.init.SoundEvents.BLOCK_NOTE_PLING,
                net.minecraft.util.SoundCategory.PLAYERS,
                0.5F,
                1.0F
        );
    }

    /**
     * 传送石碑搜索结果
     */
    private static class WaystoneSearchResult {
        BlockPos pos;
        String name;

        WaystoneSearchResult(BlockPos pos, String name) {
            this.pos = pos;
            this.name = name;
        }
    }

    /**
     * 获取下一个目标类型
     */
    private static TargetType getNextTargetType(TargetType current, EntityPlayer player) {
        int dimension = player.world.provider.getDimension();

        if (dimension == 0) {
            // 主世界 - 包含所有类型
            List<TargetType> types = new ArrayList<>();
            types.add(TargetType.VILLAGE);
            types.add(TargetType.STRONGHOLD);
            types.add(TargetType.MANSION);
            types.add(TargetType.MONUMENT);
            types.add(TargetType.TEMPLE);
            types.add(TargetType.MINESHAFT);
            types.add(TargetType.CHEST);
            types.add(TargetType.DUNGEON);

            // 只有在 Waystones 可用时才添加
            if (isWaystonesAvailable()) {
                types.add(TargetType.WAYSTONE);
            }

            for (int i = 0; i < types.size(); i++) {
                if (types.get(i) == current) {
                    return types.get((i + 1) % types.size());
                }
            }
            return types.get(0);

        } else if (dimension == -1) {
            // 下界
            TargetType[] types = {
                    TargetType.FORTRESS,
                    TargetType.CHEST
            };

            for (int i = 0; i < types.length; i++) {
                if (types[i] == current) {
                    return types[(i + 1) % types.length];
                }
            }
            return types[0];
        } else {
            // 末地或其他维度
            return TargetType.CHEST;
        }
    }

    /**
     * 根据类型查找目标
     */
    private static WaystoneSearchResult findTarget(EntityPlayerMP player, TargetType type) {
        if (type.isStructure) {
            BlockPos pos = findNearestStructure(player, type);
            return new WaystoneSearchResult(pos, null);
        } else {
            switch (type) {
                case CHEST:
                    return new WaystoneSearchResult(findNearestChest(player), null);
                case DUNGEON:
                    return new WaystoneSearchResult(findNearestDungeon(player), null);
                case WAYSTONE:
                    return findNearestWaystone(player);
                default:
                    return null;
            }
        }
    }

    /**
     * 查找最近的原版结构
     */
    private static BlockPos findNearestStructure(EntityPlayerMP player, TargetType type) {
        World world = player.getServerWorld();
        BlockPos playerPos = player.getPosition();

        try {
            return world.findNearestStructure(type.id, playerPos, false);
        } catch (Exception e) {
            System.out.println("[MoreMod] 查找结构时出错: " + type.id + " - " + e.getMessage());
        }

        return null;
    }

    /**
     * 查找最近的箱子
     */
    private static BlockPos findNearestChest(EntityPlayerMP player) {
        World world = player.getServerWorld();
        BlockPos center = player.getPosition();

        double minDistance = Double.MAX_VALUE;
        BlockPos nearestChest = null;

        for (int x = -CHEST_SEARCH_RADIUS; x <= CHEST_SEARCH_RADIUS; x++) {
            for (int y = -CHEST_SEARCH_RADIUS; y <= CHEST_SEARCH_RADIUS; y++) {
                for (int z = -CHEST_SEARCH_RADIUS; z <= CHEST_SEARCH_RADIUS; z++) {
                    BlockPos pos = center.add(x, y, z);

                    double distance = pos.getDistance(center.getX(), center.getY(), center.getZ());
                    if (distance > CHEST_SEARCH_RADIUS) continue;

                    Block block = world.getBlockState(pos).getBlock();

                    if (block instanceof BlockChest ||
                            block == Blocks.ENDER_CHEST ||
                            block == Blocks.TRAPPED_CHEST) {

                        if (distance < minDistance) {
                            minDistance = distance;
                            nearestChest = pos;
                        }
                    }
                }
            }
        }

        return nearestChest;
    }

    /**
     * 查找最近的地牢
     */
    private static BlockPos findNearestDungeon(EntityPlayerMP player) {
        World world = player.getServerWorld();
        BlockPos center = player.getPosition();

        double minDistance = Double.MAX_VALUE;
        BlockPos nearestDungeon = null;

        for (int x = -DUNGEON_SEARCH_RADIUS; x <= DUNGEON_SEARCH_RADIUS; x++) {
            for (int y = -DUNGEON_SEARCH_RADIUS; y <= DUNGEON_SEARCH_RADIUS; y++) {
                for (int z = -DUNGEON_SEARCH_RADIUS; z <= DUNGEON_SEARCH_RADIUS; z++) {
                    BlockPos pos = center.add(x, y, z);

                    double distance = pos.getDistance(center.getX(), center.getY(), center.getZ());
                    if (distance > DUNGEON_SEARCH_RADIUS) continue;

                    Block block = world.getBlockState(pos).getBlock();

                    if (block == Blocks.MOB_SPAWNER && isDungeonSpawner(world, pos)) {
                        if (distance < minDistance) {
                            minDistance = distance;
                            nearestDungeon = pos;
                        }
                    }
                }
            }
        }

        return nearestDungeon;
    }

    private static boolean isDungeonSpawner(World world, BlockPos spawnerPos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block block = world.getBlockState(spawnerPos.add(dx, dy, dz)).getBlock();
                    if (block == Blocks.MOSSY_COBBLESTONE || block == Blocks.COBBLESTONE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 使用反射查找最近的传送石碑 (Waystones)
     */
    private static WaystoneSearchResult findNearestWaystone(EntityPlayerMP player) {
        if (!isWaystonesAvailable()) {
            return null;
        }

        World world = player.getServerWorld();
        BlockPos center = player.getPosition();

        double minDistance = Double.MAX_VALUE;
        BlockPos nearestWaystone = null;
        String waystoneName = null;

        try {
            for (int x = -WAYSTONE_SEARCH_RADIUS; x <= WAYSTONE_SEARCH_RADIUS; x++) {
                for (int y = -WAYSTONE_SEARCH_RADIUS; y <= WAYSTONE_SEARCH_RADIUS; y++) {
                    for (int z = -WAYSTONE_SEARCH_RADIUS; z <= WAYSTONE_SEARCH_RADIUS; z++) {
                        BlockPos pos = center.add(x, y, z);

                        double distance = pos.getDistance(center.getX(), center.getY(), center.getZ());
                        if (distance > WAYSTONE_SEARCH_RADIUS) continue;

                        Block block = world.getBlockState(pos).getBlock();

                        // 使用反射检查是否是 BlockWaystone
                        if (CLASS_BLOCK_WAYSTONE.isInstance(block)) {
                            try {
                                // 调用 getTileWaystone(world, pos)
                                Object tileWaystone = METHOD_GET_TILE_WAYSTONE.invoke(block, world, pos);

                                if (tileWaystone != null && distance < minDistance) {
                                    // 调用 getWaystoneName()
                                    String name = (String) METHOD_GET_WAYSTONE_NAME.invoke(tileWaystone);

                                    minDistance = distance;
                                    nearestWaystone = pos;
                                    waystoneName = name != null ? name : "未命名石碑";
                                }
                            } catch (Exception e) {
                                // 单个石碑获取失败，继续搜索
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[MoreMod] 搜索传送石碑时出错: " + e.getMessage());
            e.printStackTrace();
        }

        return new WaystoneSearchResult(nearestWaystone, waystoneName);
    }

    // ===== 右键处理 - 粒子导航 =====

    public static void handleRightClick(EntityPlayerMP player) {
        ItemStack compass = getEquippedCompass(player);
        if (compass.isEmpty()) {
            player.sendMessage(new TextComponentString("§c请先装备探险者罗盘!"));
            return;
        }

        BlockPos targetPos = getTargetPosition(compass);
        if (targetPos == null) {
            player.sendMessage(new TextComponentString("§c请先使用 Shift+左键 定位一个目标!"));
            return;
        }

        TargetType targetType = getCurrentTargetType(compass);
        String targetName = getTargetName(compass);

        createParticlePath(player, targetPos);

        String message = targetName != null ?
                String.format("§a粒子导航已启动! 目标: §e%s [%s]", targetType.displayName, targetName) :
                String.format("§a粒子导航已启动! 目标: §e%s", targetType.displayName);

        player.sendMessage(new TextComponentString(message));
    }

    private static void createParticlePath(EntityPlayerMP player, BlockPos target) {
        World world = player.getServerWorld();

        Vec3d start = player.getPositionVector().add(0, player.getEyeHeight(), 0);
        Vec3d end = new Vec3d(target.getX() + 0.5, target.getY() + 1.5, target.getZ() + 0.5);

        double distance = start.distanceTo(end);
        int steps = Math.min((int) distance, 150);

        double arcHeight = Math.min(distance * 0.2, 20);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;

            double x = start.x + (end.x - start.x) * t;
            double y = start.y + (end.y - start.y) * t;
            double z = start.z + (end.z - start.z) * t;

            double arc = arcHeight * Math.sin(t * Math.PI);
            y += arc;

            if (i % 3 == 0) {
                world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, x, y, z, 0.0, 0.0, 0.0);
            }

            if (i % 5 == 0) {
                world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z,
                        (Math.random() - 0.5) * 0.1,
                        (Math.random() - 0.5) * 0.1,
                        (Math.random() - 0.5) * 0.1);
            }
        }

        for (int i = 0; i < 20; i++) {
            world.spawnParticle(EnumParticleTypes.FLAME,
                    target.getX() + 0.5 + (Math.random() - 0.5),
                    target.getY() + 1.0 + Math.random(),
                    target.getZ() + 0.5 + (Math.random() - 0.5),
                    0.0, 0.1, 0.0);
        }

        world.playSound(null, player.getPosition(),
                net.minecraft.init.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                net.minecraft.util.SoundCategory.PLAYERS, 0.3F, 1.5F);
    }

    // ===== NBT 数据管理 =====

    public static void setTargetPosition(ItemStack stack, BlockPos pos, TargetType type, String name) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }

        nbt.setInteger(NBT_TARGET_X, pos.getX());
        nbt.setInteger(NBT_TARGET_Y, pos.getY());
        nbt.setInteger(NBT_TARGET_Z, pos.getZ());
        nbt.setBoolean(NBT_HAS_TARGET, true);
        nbt.setString(NBT_TARGET_TYPE, type.name());

        if (name != null) {
            nbt.setString(NBT_TARGET_NAME, name);
        } else {
            nbt.removeTag(NBT_TARGET_NAME);
        }
    }

    public static BlockPos getTargetPosition(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null || !nbt.getBoolean(NBT_HAS_TARGET)) {
            return null;
        }

        return new BlockPos(
                nbt.getInteger(NBT_TARGET_X),
                nbt.getInteger(NBT_TARGET_Y),
                nbt.getInteger(NBT_TARGET_Z)
        );
    }

    private static TargetType getCurrentTargetType(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null || !nbt.hasKey(NBT_TARGET_TYPE)) {
            return TargetType.VILLAGE;
        }

        try {
            return TargetType.valueOf(nbt.getString(NBT_TARGET_TYPE));
        } catch (IllegalArgumentException e) {
            return TargetType.VILLAGE;
        }
    }

    private static String getTargetName(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null || !nbt.hasKey(NBT_TARGET_NAME)) {
            return null;
        }
        return nbt.getString(NBT_TARGET_NAME);
    }

    public static boolean getHasChestHighlight(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.getBoolean(NBT_CHEST_HIGHLIGHT);
    }

    public static void setChestHighlight(ItemStack stack, boolean enabled) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        nbt.setBoolean(NBT_CHEST_HIGHLIGHT, enabled);
    }

    // ===== 箱子扫描 =====

    public static List<BlockPos> getNearbyChests(EntityPlayer player, int radius) {
        List<BlockPos> chests = new ArrayList<>();
        World world = player.world;
        BlockPos center = player.getPosition();

        int scanRadius = Math.min(radius, 48);

        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos pos = center.add(x, y, z);

                    if (pos.getDistance(center.getX(), center.getY(), center.getZ()) > scanRadius) {
                        continue;
                    }

                    Block block = world.getBlockState(pos).getBlock();

                    if (block instanceof BlockChest ||
                            block == Blocks.ENDER_CHEST ||
                            block == Blocks.TRAPPED_CHEST) {
                        chests.add(pos);
                    }
                }
            }
        }

        return chests;
    }
}