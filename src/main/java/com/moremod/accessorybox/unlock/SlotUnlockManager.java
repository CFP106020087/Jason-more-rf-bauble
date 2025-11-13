package com.moremod.accessorybox.unlock;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.accessorybox.unlock.rules.UnlockRulesConfig;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 槽位解锁管理器（修正版）
 * 支持永久解锁和临时解锁
 */
public class SlotUnlockManager {

    private static final SlotUnlockManager INSTANCE = new SlotUnlockManager();

    // 永久解锁数据: UUID -> Set<SlotID>
    private final Map<UUID, Set<Integer>> permanentUnlocks = new ConcurrentHashMap<>();

    // 临时解锁数据: UUID -> Set<SlotID>（不保存，每次检查）
    private final Map<UUID, Set<Integer>> temporaryUnlocks = new ConcurrentHashMap<>();

    // NBT 键名
    private static final String NBT_UNLOCKED_SLOTS = "UnlockedBaubleSlots";

    private SlotUnlockManager() {}

    public static SlotUnlockManager getInstance() {
        return INSTANCE;
    }

    // ==================== 查询 ====================

    /**
     * 检查槽位是否解锁（永久 OR 临时）
     */
    public boolean isSlotUnlocked(EntityPlayer player, int slotId) {
        return isSlotUnlocked(player.getUniqueID(), slotId);
    }

    public boolean isSlotUnlocked(UUID playerUUID, int slotId) {
        // 0-6 原版位恒可用
        if (slotId < 7) return true;

        // 配置默认解锁
        boolean defaultLocked = UnlockableSlotsConfig.isSlotDefaultLocked(slotId);
        if (!defaultLocked) {
            if (UnlockRulesConfig.debugMode) {
                System.out.println("[SlotUnlock] 槽位 " + slotId + " 配置为默认解锁，直接返回true");
            }
            return true;
        }

        // 永久解锁
        Set<Integer> permanent = permanentUnlocks.get(playerUUID);
        if (permanent != null && permanent.contains(slotId)) {
            if (UnlockRulesConfig.debugMode) {
                System.out.println("[SlotUnlock] 槽位 " + slotId + " 在永久解锁列表中");
            }
            return true;
        }

        // 临时解锁
        Set<Integer> temporary = temporaryUnlocks.get(playerUUID);
        if (temporary != null && temporary.contains(slotId)) {
            if (UnlockRulesConfig.debugMode) {
                System.out.println("[SlotUnlock] 槽位 " + slotId + " 在临时解锁列表中");
            }
            return true;
        }

        if (UnlockRulesConfig.debugMode) {
            System.out.println("[SlotUnlock] 槽位 " + slotId + " 锁定（defaultLocked=" + defaultLocked + ", permanent=null, temporary=null）");
        }
        return false;
    }

    /**
     * 获取所有可用槽位（永久+临时+默认）
     */
    public Set<Integer> getAvailableSlots(UUID playerUUID) {
        Set<Integer> available = new HashSet<>();
        int totalSlots = getTotalSlotCount();

        for (int slotId = 0; slotId < totalSlots; slotId++) {
            if (isSlotUnlocked(playerUUID, slotId)) {
                available.add(slotId);
            }
        }

        return available;
    }

    // ==================== 永久解锁 ====================

    /**
     * 永久解锁槽位
     */
    public boolean unlockSlotPermanent(EntityPlayer player, int slotId) {
        if (slotId < 7 || slotId >= getTotalSlotCount()) return false;
        if (isSlotUnlocked(player.getUniqueID(), slotId)) return false;

        Set<Integer> unlocked = permanentUnlocks.computeIfAbsent(
                player.getUniqueID(), k -> new HashSet<>());
        boolean added = unlocked.add(slotId);

        if (added) {
            saveToPlayer(player);
            if (player instanceof EntityPlayerMP) {
                syncToClient((EntityPlayerMP) player);
            }

            if (UnlockRulesConfig.debugMode) {
                System.out.println("[SlotUnlock] ⚠️ 永久解锁槽位 " + slotId +
                        " for " + player.getName());
                System.out.println("[SlotUnlock] 调用堆栈:");
                StackTraceElement[] trace = Thread.currentThread().getStackTrace();
                for (int i = 2; i < Math.min(10, trace.length); i++) {
                    System.out.println("[SlotUnlock]   " + trace[i]);
                }
            }
        }
        return added;
    }

    /**
     * 兼容旧API（默认永久解锁）
     */
    public boolean unlockSlot(EntityPlayer player, int slotId) {
        return unlockSlotPermanent(player, slotId);
    }

    /**
     * 批量解锁槽位
     */
    public int unlockSlots(EntityPlayer player, int... slotIds) {
        int count = 0;
        for (int slotId : slotIds) {
            if (unlockSlotPermanent(player, slotId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 解锁指定类型的所有槽位
     */
    public int unlockAllSlotsOfType(EntityPlayer player, String type) {
        int[] slotIds = com.moremod.accessorybox.SlotLayoutHelper.getSlotIdsForType(type);
        return unlockSlots(player, slotIds);
    }

    // ==================== 临时解锁 ====================

    /**
     * 更新临时解锁状态
     * @param tempUnlocks 当前应该临时解锁的槽位集合
     */
    public void updateTemporaryUnlocks(EntityPlayer player, Set<Integer> tempUnlocks) {
        UUID uuid = player.getUniqueID();
        Set<Integer> previous = temporaryUnlocks.getOrDefault(uuid, Collections.emptySet());

        if (UnlockRulesConfig.debugMode) {
            System.out.println("[SlotUnlock] 更新臨時解鎖 - 玩家: " + player.getName());
            System.out.println("[SlotUnlock]   之前臨時解鎖: " + previous);
            System.out.println("[SlotUnlock]   現在臨時解鎖: " + tempUnlocks);
        }

        // 找出失效的临时槽位
        Set<Integer> lost = new HashSet<>(previous);
        lost.removeAll(tempUnlocks);

        // 找出新增的临时槽位
        Set<Integer> gained = new HashSet<>(tempUnlocks);
        gained.removeAll(previous);

        if (UnlockRulesConfig.debugMode && (!lost.isEmpty() || !gained.isEmpty())) {
            System.out.println("[SlotUnlock]   失效槽位: " + lost);
            System.out.println("[SlotUnlock]   新增槽位: " + gained);
        }

        // 处理失效的临时槽位
        boolean hasLostSlots = false;
        for (int slotId : lost) {
            handleTemporarySlotLost(player, slotId);
            hasLostSlots = true;
        }

        // 更新临时解锁记录
        if (tempUnlocks.isEmpty()) {
            temporaryUnlocks.remove(uuid);
        } else {
            temporaryUnlocks.put(uuid, new HashSet<>(tempUnlocks));
        }

        // 同步到客户端
        if (player instanceof EntityPlayerMP) {
            if (UnlockRulesConfig.debugMode) {
                System.out.println("[SlotUnlock] 正在同步到客戶端...");
            }
            syncToClient((EntityPlayerMP) player);
        }

        // ⭐ 如果配置启用且有槽位失效，关闭玩家当前的容器
        if (hasLostSlots && UnlockRulesConfig.closeContainerOnTempLoss) {
            player.closeScreen();

            if (UnlockRulesConfig.debugMode) {
                System.out.println("[SlotUnlock] 临时槽位失效，已关闭玩家容器: " + player.getName());
            }
        }
    }

    /**
     * 处理临时槽位失效
     */
    private void handleTemporarySlotLost(EntityPlayer player, int slotId) {
        if (player.world.isRemote) return;

        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) return;

        ItemStack stack = handler.getStackInSlot(slotId);
        if (stack.isEmpty()) return;

        if (UnlockRulesConfig.debugMode) {
            System.out.println("[SlotUnlock] 临时槽位 " + slotId + " 失效，处理物品: " +
                    stack.getDisplayName());
        }

        // 根据配置处理物品
        switch (UnlockRulesConfig.temporarySlotBehavior.toLowerCase()) {
            case "drop":
                // 掉落物品
                dropItem(player, stack);
                handler.setStackInSlot(slotId, ItemStack.EMPTY);
                break;

            case "inventory":
                // 尝试放入背包
                if (player.inventory.addItemStackToInventory(stack.copy())) {
                    handler.setStackInSlot(slotId, ItemStack.EMPTY);
                } else {
                    // 背包满则掉落
                    dropItem(player, stack);
                    handler.setStackInSlot(slotId, ItemStack.EMPTY);
                }
                break;

            case "keep":
            default:
                // 保持在槽位（下次打开GUI无法访问）
                break;
        }
    }

    /**
     * 掉落物品到玩家位置
     */
    private void dropItem(EntityPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;

        EntityItem entityItem = new EntityItem(
                player.world,
                player.posX,
                player.posY + 0.5,
                player.posZ,
                stack.copy()
        );
        entityItem.setNoPickupDelay();
        player.world.spawnEntity(entityItem);
    }

    // ==================== 锁定 ====================

    /**
     * 重新锁定槽位（慎用！）
     */
    public boolean lockSlot(EntityPlayer player, int slotId) {
        UUID id = player.getUniqueID();

        // 移除永久解锁
        Set<Integer> permanent = permanentUnlocks.get(id);
        boolean removed = false;
        if (permanent != null) {
            removed = permanent.remove(slotId);
        }

        if (removed) {
            saveToPlayer(player);
            if (player instanceof EntityPlayerMP) {
                syncToClient((EntityPlayerMP) player);
            }

            if (UnlockRulesConfig.debugMode) {
                System.out.println("[SlotUnlock] 锁定槽位 " + slotId + " for " + player.getName());
            }
        }
        return removed;
    }

    /**
     * 重置玩家所有解锁状态
     */
    public void resetPlayer(EntityPlayer player) {
        UUID id = player.getUniqueID();
        permanentUnlocks.remove(id);
        temporaryUnlocks.remove(id);
        saveToPlayer(player);

        if (player instanceof EntityPlayerMP) {
            syncToClient((EntityPlayerMP) player);
        }

        if (UnlockRulesConfig.debugMode) {
            System.out.println("[SlotUnlock] 重置玩家 " + id + " 的解锁状态");
        }
    }

    // ==================== 持久化 ====================

    public void loadFromPlayer(EntityPlayer player) {
        UUID id = player.getUniqueID();
        NBTTagCompound data = player.getEntityData();

        if (data.hasKey(NBT_UNLOCKED_SLOTS)) {
            int[] arr = data.getIntArray(NBT_UNLOCKED_SLOTS);
            Set<Integer> set = new HashSet<>();
            for (int s : arr) set.add(s);
            permanentUnlocks.put(id, set);

            if (UnlockRulesConfig.debugMode) {
                System.out.println("[SlotUnlock] 加载玩家 " + id + " 的解锁数据: " +
                        set.size() + " 个槽位");
            }
        } else {
            permanentUnlocks.remove(id);
        }

        // 临时解锁不需要加载
        temporaryUnlocks.remove(id);
    }

    public void saveToPlayer(EntityPlayer player) {
        UUID id = player.getUniqueID();
        Set<Integer> set = permanentUnlocks.get(id);

        NBTTagCompound data = player.getEntityData();
        if (set == null || set.isEmpty()) {
            data.removeTag(NBT_UNLOCKED_SLOTS);
        } else {
            int[] arr = set.stream().mapToInt(Integer::intValue).toArray();
            data.setIntArray(NBT_UNLOCKED_SLOTS, arr);
        }
    }

    public void onPlayerLogout(UUID playerUUID) {
        permanentUnlocks.remove(playerUUID);
        temporaryUnlocks.remove(playerUUID);
    }

    // ==================== 同步 ====================

    public void syncToClient(EntityPlayerMP player) {
        Set<Integer> allUnlocked = getAvailableSlots(player.getUniqueID());

        if (UnlockRulesConfig.debugMode) {
            System.out.println("[SlotUnlock] [服務器] 同步到客戶端: " + player.getName());
            System.out.println("[SlotUnlock] [服務器]   發送槽位: " + allUnlocked);
        }

        PacketSyncUnlockedSlots packet = new PacketSyncUnlockedSlots(allUnlocked);
        ModNetworkHandler.INSTANCE.sendTo(packet, player);
    }

    public void receiveSync(UUID playerUUID, Set<Integer> unlockedSlotIds) {
        // 客户端接收同步数据（包含永久+临时）
        // 注意：客户端不区分永久/临时，统一存储
        Set<Integer> oldSlots = permanentUnlocks.get(playerUUID);

        // ⚠️ 关键：完全替换数据，清除旧的临时解锁
        permanentUnlocks.put(playerUUID, new HashSet<>(unlockedSlotIds));

        // 同时清理客户端的临时解锁列表（防止污染）
        temporaryUnlocks.remove(playerUUID);

        if (UnlockRulesConfig.debugMode) {
            System.out.println("[SlotUnlock] [客戶端] 接收同步數據");
            System.out.println("[SlotUnlock] [客戶端]   舊槽位: " + oldSlots);
            System.out.println("[SlotUnlock] [客戶端]   新槽位: " + unlockedSlotIds);
        }
    }

    // ==================== 辅助 ====================

    private int getTotalSlotCount() {
        int max = -1;
        max = Math.max(max, maxIn(com.moremod.accessorybox.SlotLayoutHelper.getSlotIdsForType("AMULET")));
        max = Math.max(max, maxIn(com.moremod.accessorybox.SlotLayoutHelper.getSlotIdsForType("RING")));
        max = Math.max(max, maxIn(com.moremod.accessorybox.SlotLayoutHelper.getSlotIdsForType("BELT")));
        max = Math.max(max, maxIn(com.moremod.accessorybox.SlotLayoutHelper.getSlotIdsForType("HEAD")));
        max = Math.max(max, maxIn(com.moremod.accessorybox.SlotLayoutHelper.getSlotIdsForType("BODY")));
        max = Math.max(max, maxIn(com.moremod.accessorybox.SlotLayoutHelper.getSlotIdsForType("CHARM")));
        max = Math.max(max, maxIn(com.moremod.accessorybox.SlotLayoutHelper.getSlotIdsForType("TRINKET")));
        return max + 1;
    }

    private static int maxIn(int[] arr) {
        if (arr == null || arr.length == 0) return -1;
        int m = -1;
        for (int v : arr) if (v > m) m = v;
        return m;
    }

    public void clearAll() {
        permanentUnlocks.clear();
        temporaryUnlocks.clear();
    }

    public void debugPrint(UUID playerUUID) {
        System.out.println("========== 槽位解锁状态 ==========");
        System.out.println("玩家: " + playerUUID);

        Set<Integer> permanent = permanentUnlocks.get(playerUUID);
        Set<Integer> temporary = temporaryUnlocks.get(playerUUID);
        Set<Integer> available = getAvailableSlots(playerUUID);

        System.out.println("永久解锁: " + (permanent != null ? permanent : "无"));
        System.out.println("临时解锁: " + (temporary != null ? temporary : "无"));
        System.out.println("总可用槽位: " + available);
        System.out.println("==================================");
    }
}