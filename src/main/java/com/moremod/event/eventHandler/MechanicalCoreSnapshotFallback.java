package com.moremod.event.eventHandler;

import com.moremod.item.ItemMechanicalCore;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 机械核心快照回退系统
 * 防止网络抖动（内网穿透）导致的机械核心意外丢失
 *
 * 工作原理：
 * 1. 每秒记录佩戴机械核心玩家的核心NBT快照
 * 2. 检测头部饰品槽位(slot 0)的机械核心
 * 3. 如果核心意外消失，从快照恢复
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class MechanicalCoreSnapshotFallback {

    // 玩家UUID -> 快照数据
    private static final Map<UUID, CoreSnapshot> snapshots = new ConcurrentHashMap<>();

    // 配置
    private static final int CHECK_INTERVAL_TICKS = 20; // 每秒检查
    private static final int SNAPSHOT_HISTORY_SIZE = 3;  // 保留3个历史快照
    private static final int GRACE_PERIOD_TICKS = 60;    // 3秒宽限期（防止正常卸下触发）

    // 调试模式
    public static boolean debugMode = false;

    /**
     * 核心快照数据
     */
    private static class CoreSnapshot {
        final NBTTagCompound[] history;
        int currentIndex = 0;
        int validCount = 0;
        long lastSeenTick = 0;
        boolean hadCore = false;

        CoreSnapshot() {
            history = new NBTTagCompound[SNAPSHOT_HISTORY_SIZE];
        }

        void addSnapshot(NBTTagCompound nbt, long tick) {
            history[currentIndex] = nbt;
            currentIndex = (currentIndex + 1) % SNAPSHOT_HISTORY_SIZE;
            if (validCount < SNAPSHOT_HISTORY_SIZE) validCount++;
            lastSeenTick = tick;
            hadCore = true;
        }

        NBTTagCompound getLatestSnapshot() {
            if (validCount == 0) return null;
            int idx = (currentIndex - 1 + SNAPSHOT_HISTORY_SIZE) % SNAPSHOT_HISTORY_SIZE;
            return history[idx];
        }

        void clear() {
            for (int i = 0; i < SNAPSHOT_HISTORY_SIZE; i++) {
                history[i] = null;
            }
            validCount = 0;
            currentIndex = 0;
            hadCore = false;
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;

        // 每秒检查一次
        if (player.ticksExisted % CHECK_INTERVAL_TICKS != 0) return;

        UUID uuid = player.getUniqueID();
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) return;

        // 检查头部饰品槽 (slot 0) 的机械核心
        ItemStack headSlotStack = handler.getStackInSlot(0);
        boolean hasCore = !headSlotStack.isEmpty() && ItemMechanicalCore.isMechanicalCore(headSlotStack);

        CoreSnapshot snapshot = snapshots.get(uuid);

        if (hasCore) {
            // 玩家佩戴了机械核心，记录快照
            if (snapshot == null) {
                snapshot = new CoreSnapshot();
                snapshots.put(uuid, snapshot);
            }

            NBTTagCompound nbt = new NBTTagCompound();
            headSlotStack.writeToNBT(nbt);
            snapshot.addSnapshot(nbt, player.world.getTotalWorldTime());

            if (debugMode) {
                System.out.println("[CoreSnapshot] 记录快照 - 玩家: " + player.getName() +
                        ", NBT大小: " + nbt.getSize() + " bytes");
            }

        } else {
            // 玩家没有佩戴机械核心
            if (snapshot != null && snapshot.hadCore) {
                long currentTick = player.world.getTotalWorldTime();
                long ticksSinceLastSeen = currentTick - snapshot.lastSeenTick;

                // 检查是否在宽限期内（防止正常卸下操作触发恢复）
                if (ticksSinceLastSeen <= GRACE_PERIOD_TICKS) {

                    // ⚠ 安全检查1：玩家是否打开了容器（箱子/工作台等）
                    // 如果打开了容器，核心可能被放入容器，不应该恢复（防止复制）
                    if (isPlayerUsingContainer(player)) {
                        if (debugMode) {
                            System.out.println("[CoreSnapshot] 玩家正在使用容器，清除快照防止复制 - 玩家: " + player.getName());
                        }
                        snapshot.clear();
                        return;
                    }

                    // ⚠ 安全检查2：核心是否在其他饰品槽位
                    if (findCoreInBaubles(handler) != -1) {
                        if (debugMode) {
                            System.out.println("[CoreSnapshot] 机械核心在其他饰品槽，清除快照 - 玩家: " + player.getName());
                        }
                        snapshot.clear();
                        return;
                    }

                    // 检查机械核心是否在背包中（正常卸下）
                    boolean coreInInventory = findCoreInInventory(player) != null;

                    if (!coreInInventory) {
                        // 核心不在背包中，可能是网络抖动导致丢失
                        NBTTagCompound savedNbt = snapshot.getLatestSnapshot();
                        if (savedNbt != null) {
                            restoreCoreFromSnapshot(player, handler, savedNbt);
                        }
                    } else {
                        // 核心在背包中，是正常卸下，清除快照
                        if (debugMode) {
                            System.out.println("[CoreSnapshot] 机械核心已移至背包，清除快照 - 玩家: " + player.getName());
                        }
                        snapshot.clear();
                    }
                } else {
                    // 超过宽限期，清除快照
                    snapshot.clear();
                }
            }
        }
    }

    /**
     * 检查玩家是否正在使用容器（箱子/工作台等）
     * 用于防止核心放入容器时误判为丢失
     */
    private static boolean isPlayerUsingContainer(EntityPlayer player) {
        // inventoryContainer 是玩家默认的背包容器
        // 如果 openContainer 不等于 inventoryContainer，说明打开了其他容器
        return player.openContainer != null &&
               player.openContainer != player.inventoryContainer;
    }

    /**
     * 在饰品槽中查找机械核心
     * @return 找到的槽位ID，未找到返回-1
     */
    private static int findCoreInBaubles(IBaublesItemHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && ItemMechanicalCore.isMechanicalCore(stack)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 在背包中查找机械核心
     */
    private static ItemStack findCoreInInventory(EntityPlayer player) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && ItemMechanicalCore.isMechanicalCore(stack)) {
                return stack;
            }
        }
        return null;
    }

    /**
     * 从快照恢复机械核心
     */
    private static void restoreCoreFromSnapshot(EntityPlayer player, IBaublesItemHandler handler, NBTTagCompound savedNbt) {
        ItemStack restoredCore = new ItemStack(savedNbt);

        if (restoredCore.isEmpty()) {
            System.out.println("[CoreSnapshot] 警告：快照恢复失败，NBT数据无效 - 玩家: " + player.getName());
            return;
        }

        // 检查槽位是否为空
        ItemStack currentSlot = handler.getStackInSlot(0);
        if (!currentSlot.isEmpty()) {
            // 槽位不为空，不覆盖
            if (debugMode) {
                System.out.println("[CoreSnapshot] 槽位非空，跳过恢复 - 玩家: " + player.getName());
            }
            return;
        }

        // 恢复核心到槽位
        handler.setStackInSlot(0, restoredCore);

        // 清除快照（已恢复）
        CoreSnapshot snapshot = snapshots.get(player.getUniqueID());
        if (snapshot != null) {
            snapshot.clear();
        }

        // 通知玩家
        player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "⚠ " + TextFormatting.GRAY +
                "检测到机械核心意外丢失，已从快照恢复。"
        ));

        System.out.println("[CoreSnapshot] 机械核心已从快照恢复 - 玩家: " + player.getName());
    }

    /**
     * 玩家登出时清理快照
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        if (event.player != null) {
            UUID uuid = event.player.getUniqueID();
            snapshots.remove(uuid);

            if (debugMode) {
                System.out.println("[CoreSnapshot] 玩家登出，清理快照 - " + event.player.getName());
            }
        }
    }

    /**
     * 手动清理快照（供外部调用）
     */
    public static void clearSnapshotForPlayer(UUID playerUUID) {
        snapshots.remove(playerUUID);
    }

    /**
     * 手动清除玩家快照（用于正常卸下核心时调用）
     */
    public static void clearSnapshot(UUID playerUUID) {
        CoreSnapshot snapshot = snapshots.get(playerUUID);
        if (snapshot != null) {
            snapshot.clear();
        }
    }

    /**
     * 调试：打印快照状态
     */
    public static void debugPrint(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        CoreSnapshot snapshot = snapshots.get(uuid);

        System.out.println("========== 机械核心快照状态 ==========");
        System.out.println("玩家: " + player.getName());

        if (snapshot == null) {
            System.out.println("无快照数据");
        } else {
            System.out.println("有效快照数: " + snapshot.validCount);
            System.out.println("上次记录tick: " + snapshot.lastSeenTick);
            System.out.println("曾佩戴核心: " + snapshot.hadCore);

            NBTTagCompound latest = snapshot.getLatestSnapshot();
            if (latest != null) {
                System.out.println("最新快照NBT大小: " + latest.getSize() + " bytes");
            }
        }
        System.out.println("=====================================");
    }
}
