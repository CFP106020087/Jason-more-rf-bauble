package com.moremod.event.eventHandler;

import com.moremod.item.ItemMechanicalCore;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 机械核心快照回退系统 - 增强版
 *
 * 多层保护机制：
 * 1. 内存快照：每秒记录，保留3个历史版本（快速恢复）
 * 2. NBT持久化：每30秒保存到玩家NBT（跨重启保护）
 * 3. 紧急恢复：登录时检查，如果核心丢失但有备份则恢复
 * 4. 全槽位扫描：检查所有饰品槽位，不仅仅是头部
 *
 * 触发恢复的条件：
 * - 玩家曾佩戴过核心（hadCore = true）
 * - 当前没有佩戴核心
 * - 背包中也没有核心
 * - 有有效的快照数据
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class MechanicalCoreSnapshotFallback {

    // ========== NBT键 ==========
    private static final String NBT_SNAPSHOT_KEY = "moremod_core_snapshot";
    private static final String NBT_SNAPSHOT_SLOT = "SnapshotSlot";
    private static final String NBT_SNAPSHOT_DATA = "SnapshotData";
    private static final String NBT_SNAPSHOT_TIME = "SnapshotTime";
    private static final String NBT_SNAPSHOT_HISTORY = "SnapshotHistory";
    private static final String NBT_HAD_CORE = "HadCore";

    // ========== 内存缓存 ==========
    private static final Map<UUID, CoreSnapshot> snapshots = new ConcurrentHashMap<>();

    // ========== 配置 ==========
    private static final int CHECK_INTERVAL_TICKS = 20;     // 每秒检查
    private static final int PERSIST_INTERVAL_TICKS = 600;  // 每30秒持久化
    private static final int SNAPSHOT_HISTORY_SIZE = 5;     // 保留5个历史快照
    private static final int MAX_RESTORE_ATTEMPTS = 3;      // 最大恢复尝试次数

    // ========== 开关 ==========
    public static boolean enabled = true;
    public static boolean debugMode = false;

    /**
     * 核心快照数据 - 增强版
     */
    private static class CoreSnapshot {
        final NBTTagCompound[] history;
        final int[] slotHistory;  // 记录每个快照对应的槽位
        int currentIndex = 0;
        int validCount = 0;
        boolean hadCore = false;
        long lastPersistTime = 0;
        int restoreAttempts = 0;

        CoreSnapshot() {
            history = new NBTTagCompound[SNAPSHOT_HISTORY_SIZE];
            slotHistory = new int[SNAPSHOT_HISTORY_SIZE];
        }

        void addSnapshot(NBTTagCompound nbt, int slot) {
            history[currentIndex] = nbt.copy();  // 使用副本，避免引用问题
            slotHistory[currentIndex] = slot;
            currentIndex = (currentIndex + 1) % SNAPSHOT_HISTORY_SIZE;
            if (validCount < SNAPSHOT_HISTORY_SIZE) validCount++;
            hadCore = true;
            restoreAttempts = 0;  // 有新快照时重置恢复计数
        }

        NBTTagCompound getLatestSnapshot() {
            if (validCount == 0) return null;
            int idx = (currentIndex - 1 + SNAPSHOT_HISTORY_SIZE) % SNAPSHOT_HISTORY_SIZE;
            return history[idx];
        }

        int getLatestSlot() {
            if (validCount == 0) return 0;
            int idx = (currentIndex - 1 + SNAPSHOT_HISTORY_SIZE) % SNAPSHOT_HISTORY_SIZE;
            return slotHistory[idx];
        }

        /**
         * 获取指定历史索引的快照（0=最新，1=上一个...）
         */
        NBTTagCompound getHistorySnapshot(int historyIndex) {
            if (historyIndex >= validCount) return null;
            int idx = (currentIndex - 1 - historyIndex + SNAPSHOT_HISTORY_SIZE * 2) % SNAPSHOT_HISTORY_SIZE;
            return history[idx];
        }

        void clear() {
            for (int i = 0; i < SNAPSHOT_HISTORY_SIZE; i++) {
                history[i] = null;
                slotHistory[i] = 0;
            }
            validCount = 0;
            currentIndex = 0;
            hadCore = false;
            restoreAttempts = 0;
        }

        boolean canAttemptRestore() {
            return restoreAttempts < MAX_RESTORE_ATTEMPTS;
        }

        void incrementRestoreAttempts() {
            restoreAttempts++;
        }
    }

    // ========== 主要事件处理 ==========

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!enabled) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        int tick = player.ticksExisted;

        // 每秒检查一次
        if (tick % CHECK_INTERVAL_TICKS != 0) return;

        UUID uuid = player.getUniqueID();
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) return;

        // 查找机械核心（扫描所有槽位）
        int coreSlot = findMechanicalCoreSlot(handler);
        boolean hasCore = coreSlot >= 0;

        CoreSnapshot snapshot = snapshots.computeIfAbsent(uuid, k -> new CoreSnapshot());

        if (hasCore) {
            // 玩家佩戴了机械核心，记录快照
            ItemStack coreStack = handler.getStackInSlot(coreSlot);
            NBTTagCompound nbt = new NBTTagCompound();
            coreStack.writeToNBT(nbt);
            snapshot.addSnapshot(nbt, coreSlot);

            // 定期持久化到NBT
            if (tick % PERSIST_INTERVAL_TICKS == 0) {
                persistSnapshotToNBT(player, snapshot, coreSlot);
            }

            if (debugMode && tick % 100 == 0) {
                System.out.println("[CoreSnapshot] 快照更新 - 玩家: " + player.getName() +
                        ", 槽位: " + coreSlot + ", 历史数: " + snapshot.validCount);
            }
        } else {
            // 玩家没有佩戴机械核心，检查是否需要恢复
            if (snapshot.hadCore && snapshot.canAttemptRestore()) {
                // 先检查背包中是否有核心（可能是正常卸下）
                if (!hasCoreinInventory(player)) {
                    attemptRestore(player, handler, snapshot);
                }
            }
        }
    }

    /**
     * 玩家登录时检查是否需要恢复
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (!enabled) return;
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        UUID uuid = player.getUniqueID();

        // 从NBT加载快照
        CoreSnapshot snapshot = loadSnapshotFromNBT(player);
        if (snapshot != null && snapshot.hadCore) {
            snapshots.put(uuid, snapshot);

            // 检查当前是否有核心
            IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
            if (handler != null) {
                int coreSlot = findMechanicalCoreSlot(handler);
                if (coreSlot < 0 && !hasCoreinInventory(player)) {
                    // 没有核心，尝试恢复
                    player.getServer().addScheduledTask(() -> {
                        attemptRestore(player, handler, snapshot);
                    });
                }
            }
        }

        if (debugMode) {
            System.out.println("[CoreSnapshot] 玩家登录 - " + player.getName() +
                    ", 有快照: " + (snapshot != null && snapshot.hadCore));
        }
    }

    /**
     * 玩家死亡/维度切换时保留快照
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!enabled) return;
        EntityPlayer oldPlayer = event.getOriginal();
        EntityPlayer newPlayer = event.getEntityPlayer();
        UUID uuid = oldPlayer.getUniqueID();

        // 从旧玩家获取快照
        CoreSnapshot snapshot = snapshots.get(uuid);
        if (snapshot == null) {
            snapshot = loadSnapshotFromNBT(oldPlayer);
        }

        if (snapshot != null && snapshot.hadCore) {
            // 持久化到新玩家的NBT
            persistSnapshotToNBT(newPlayer, snapshot, snapshot.getLatestSlot());
            snapshots.put(uuid, snapshot);

            if (debugMode) {
                System.out.println("[CoreSnapshot] 玩家Clone - " + newPlayer.getName() +
                        ", 保留快照, 历史数: " + snapshot.validCount);
            }
        }
    }

    /**
     * 玩家登出时持久化快照
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        if (event.player == null) return;
        EntityPlayer player = event.player;
        UUID uuid = player.getUniqueID();

        CoreSnapshot snapshot = snapshots.get(uuid);
        if (snapshot != null && snapshot.hadCore) {
            // 登出前持久化
            persistSnapshotToNBT(player, snapshot, snapshot.getLatestSlot());
        }

        // 不清除内存快照，保留一段时间防止快速重连

        if (debugMode) {
            System.out.println("[CoreSnapshot] 玩家登出 - " + player.getName());
        }
    }

    // ========== 核心功能方法 ==========

    /**
     * 尝试恢复机械核心
     */
    private static void attemptRestore(EntityPlayer player, IBaublesItemHandler handler, CoreSnapshot snapshot) {
        if (!snapshot.canAttemptRestore()) {
            if (debugMode) {
                System.out.println("[CoreSnapshot] 恢复次数已达上限 - 玩家: " + player.getName());
            }
            return;
        }

        snapshot.incrementRestoreAttempts();

        // 尝试从历史快照恢复（从最新到最旧）
        for (int i = 0; i < snapshot.validCount; i++) {
            NBTTagCompound savedNbt = snapshot.getHistorySnapshot(i);
            if (savedNbt == null) continue;

            ItemStack restoredCore = new ItemStack(savedNbt);
            if (restoredCore.isEmpty() || !ItemMechanicalCore.isMechanicalCore(restoredCore)) {
                continue;
            }

            // 找到合适的槽位
            int targetSlot = snapshot.getLatestSlot();
            if (!handler.getStackInSlot(targetSlot).isEmpty()) {
                // 原槽位被占用，尝试找其他合适的槽位
                targetSlot = findEmptyValidSlot(handler, restoredCore, player);
            }

            if (targetSlot >= 0 && handler.getStackInSlot(targetSlot).isEmpty()) {
                // 恢复核心
                handler.setStackInSlot(targetSlot, restoredCore);

                // 验证恢复成功
                ItemStack verify = handler.getStackInSlot(targetSlot);
                if (!verify.isEmpty() && ItemMechanicalCore.isMechanicalCore(verify)) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "✓ " + TextFormatting.GRAY +
                                    "检测到机械核心异常丢失，已从快照恢复！"
                    ));
                    player.sendMessage(new TextComponentString(
                            TextFormatting.DARK_GRAY + "（快照系统保护了你的数据）"
                    ));

                    System.out.println("[CoreSnapshot] ✓ 机械核心已恢复 - 玩家: " + player.getName() +
                            ", 槽位: " + targetSlot + ", 尝试次数: " + snapshot.restoreAttempts);

                    // 恢复成功，重置计数
                    snapshot.restoreAttempts = 0;
                    return;
                }
            }
        }

        // 所有尝试都失败了
        System.err.println("[CoreSnapshot] ✗ 机械核心恢复失败 - 玩家: " + player.getName() +
                ", 尝试次数: " + snapshot.restoreAttempts);

        if (snapshot.restoreAttempts >= MAX_RESTORE_ATTEMPTS) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "⚠ 机械核心恢复失败，请联系管理员！"
            ));
        }
    }

    /**
     * 持久化快照到玩家NBT
     */
    private static void persistSnapshotToNBT(EntityPlayer player, CoreSnapshot snapshot, int slot) {
        try {
            NBTTagCompound persistent = getPlayerPersistentData(player);

            NBTTagCompound snapshotNbt = new NBTTagCompound();
            snapshotNbt.setBoolean(NBT_HAD_CORE, snapshot.hadCore);
            snapshotNbt.setInteger(NBT_SNAPSHOT_SLOT, slot);
            snapshotNbt.setLong(NBT_SNAPSHOT_TIME, System.currentTimeMillis());

            // 保存最新的快照数据
            NBTTagCompound latestData = snapshot.getLatestSnapshot();
            if (latestData != null) {
                snapshotNbt.setTag(NBT_SNAPSHOT_DATA, latestData.copy());
            }

            // 保存历史快照
            NBTTagList historyList = new NBTTagList();
            for (int i = 0; i < snapshot.validCount; i++) {
                NBTTagCompound histData = snapshot.getHistorySnapshot(i);
                if (histData != null) {
                    NBTTagCompound entry = new NBTTagCompound();
                    entry.setTag("data", histData.copy());
                    entry.setInteger("slot", snapshot.slotHistory[
                            (snapshot.currentIndex - 1 - i + SNAPSHOT_HISTORY_SIZE * 2) % SNAPSHOT_HISTORY_SIZE]);
                    historyList.appendTag(entry);
                }
            }
            snapshotNbt.setTag(NBT_SNAPSHOT_HISTORY, historyList);

            persistent.setTag(NBT_SNAPSHOT_KEY, snapshotNbt);
            snapshot.lastPersistTime = System.currentTimeMillis();

            if (debugMode) {
                System.out.println("[CoreSnapshot] 快照已持久化 - 玩家: " + player.getName() +
                        ", 历史数: " + historyList.tagCount());
            }
        } catch (Exception e) {
            System.err.println("[CoreSnapshot] 持久化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从玩家NBT加载快照
     */
    private static CoreSnapshot loadSnapshotFromNBT(EntityPlayer player) {
        try {
            NBTTagCompound persistent = getPlayerPersistentData(player);
            if (!persistent.hasKey(NBT_SNAPSHOT_KEY)) return null;

            NBTTagCompound snapshotNbt = persistent.getCompoundTag(NBT_SNAPSHOT_KEY);
            if (!snapshotNbt.getBoolean(NBT_HAD_CORE)) return null;

            CoreSnapshot snapshot = new CoreSnapshot();
            snapshot.hadCore = true;

            // 加载历史快照
            if (snapshotNbt.hasKey(NBT_SNAPSHOT_HISTORY)) {
                NBTTagList historyList = snapshotNbt.getTagList(NBT_SNAPSHOT_HISTORY, Constants.NBT.TAG_COMPOUND);
                for (int i = historyList.tagCount() - 1; i >= 0; i--) {
                    NBTTagCompound entry = historyList.getCompoundTagAt(i);
                    if (entry.hasKey("data")) {
                        snapshot.addSnapshot(entry.getCompoundTag("data"), entry.getInteger("slot"));
                    }
                }
            } else if (snapshotNbt.hasKey(NBT_SNAPSHOT_DATA)) {
                // 兼容旧格式
                snapshot.addSnapshot(snapshotNbt.getCompoundTag(NBT_SNAPSHOT_DATA),
                        snapshotNbt.getInteger(NBT_SNAPSHOT_SLOT));
            }

            if (debugMode) {
                System.out.println("[CoreSnapshot] 从NBT加载快照 - 玩家: " + player.getName() +
                        ", 历史数: " + snapshot.validCount);
            }

            return snapshot;
        } catch (Exception e) {
            System.err.println("[CoreSnapshot] 加载快照失败: " + e.getMessage());
            return null;
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 查找机械核心所在的槽位
     * @return 槽位索引，-1表示没找到
     */
    private static int findMechanicalCoreSlot(IBaublesItemHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && ItemMechanicalCore.isMechanicalCore(stack)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 检查背包中是否有机械核心
     */
    private static boolean hasCoreinInventory(EntityPlayer player) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && ItemMechanicalCore.isMechanicalCore(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 找到一个空的、可以放置核心的槽位
     */
    private static int findEmptyValidSlot(IBaublesItemHandler handler, ItemStack core, EntityPlayer player) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).isEmpty() && handler.isItemValidForSlot(i, core, player)) {
                return i;
            }
        }
        return -1;
    }

    private static NBTTagCompound getPlayerPersistentData(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    // ========== 公共API ==========

    /**
     * 手动触发快照保存（供外部调用）
     */
    public static void forceSnapshot(EntityPlayer player) {
        if (player.world.isRemote) return;

        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) return;

        int coreSlot = findMechanicalCoreSlot(handler);
        if (coreSlot >= 0) {
            UUID uuid = player.getUniqueID();
            CoreSnapshot snapshot = snapshots.computeIfAbsent(uuid, k -> new CoreSnapshot());

            ItemStack coreStack = handler.getStackInSlot(coreSlot);
            NBTTagCompound nbt = new NBTTagCompound();
            coreStack.writeToNBT(nbt);
            snapshot.addSnapshot(nbt, coreSlot);
            persistSnapshotToNBT(player, snapshot, coreSlot);

            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 机械核心快照已保存"
            ));
        }
    }

    /**
     * 手动触发恢复（供管理员命令调用）
     */
    public static boolean forceRestore(EntityPlayer player) {
        if (player.world.isRemote) return false;

        UUID uuid = player.getUniqueID();
        CoreSnapshot snapshot = snapshots.get(uuid);
        if (snapshot == null) {
            snapshot = loadSnapshotFromNBT(player);
        }

        if (snapshot == null || !snapshot.hadCore) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 没有可用的快照数据"
            ));
            return false;
        }

        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) return false;

        // 重置恢复计数，强制恢复
        snapshot.restoreAttempts = 0;
        attemptRestore(player, handler, snapshot);
        return true;
    }

    /**
     * 清除玩家的快照数据
     */
    public static void clearSnapshot(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        snapshots.remove(uuid);

        NBTTagCompound persistent = getPlayerPersistentData(player);
        persistent.removeTag(NBT_SNAPSHOT_KEY);

        player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "⚠ 机械核心快照已清除"
        ));
    }

    /**
     * 调试：打印快照状态
     */
    public static void debugPrint(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        CoreSnapshot snapshot = snapshots.get(uuid);

        System.out.println("========== 机械核心快照状态（增强版）==========");
        System.out.println("玩家: " + player.getName());
        System.out.println("系统开关: " + (enabled ? "开启" : "关闭"));

        if (snapshot == null) {
            System.out.println("内存快照: 无");
        } else {
            System.out.println("内存快照: 有");
            System.out.println("  - 有效历史数: " + snapshot.validCount);
            System.out.println("  - 曾佩戴核心: " + snapshot.hadCore);
            System.out.println("  - 恢复尝试次数: " + snapshot.restoreAttempts);

            NBTTagCompound latest = snapshot.getLatestSnapshot();
            if (latest != null) {
                System.out.println("  - 最新快照大小: " + latest.getSize() + " bytes");
            }
        }

        // 检查NBT中的快照
        NBTTagCompound persistent = getPlayerPersistentData(player);
        if (persistent.hasKey(NBT_SNAPSHOT_KEY)) {
            NBTTagCompound nbtSnapshot = persistent.getCompoundTag(NBT_SNAPSHOT_KEY);
            System.out.println("NBT快照: 有");
            System.out.println("  - 保存时间: " + nbtSnapshot.getLong(NBT_SNAPSHOT_TIME));
            if (nbtSnapshot.hasKey(NBT_SNAPSHOT_HISTORY)) {
                System.out.println("  - 历史数: " +
                        nbtSnapshot.getTagList(NBT_SNAPSHOT_HISTORY, Constants.NBT.TAG_COMPOUND).tagCount());
            }
        } else {
            System.out.println("NBT快照: 无");
        }

        System.out.println("==============================================");
    }
}
