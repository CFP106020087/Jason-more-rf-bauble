package com.moremod.accessorybox.unlock.rules.progress;

import com.moremod.accessorybox.unlock.rules.UnlockRulesConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家进度追踪器
 * 记录玩家的各种行为计数(消耗物品、击杀实体等)
 */
public class ProgressTracker {

    // 玩家数据: UUID -> 数据类
    private static final Map<UUID, PlayerProgressData> playerData = new ConcurrentHashMap<>();

    // NBT键名
    private static final String NBT_PROGRESS = "UnlockRuleProgress";

    // ==================== 数据获取 ====================

    /**
     * 获取物品消耗次数
     */
    public static int getItemConsumeCount(EntityPlayer player, String itemId) {
        return getData(player).getCount("consume", itemId);
    }

    /**
     * 获取物品使用次数
     */
    public static int getItemUseCount(EntityPlayer player, String itemId) {
        return getData(player).getCount("use", itemId);
    }

    /**
     * 获取物品拾取次数
     */
    public static int getItemPickupCount(EntityPlayer player, String itemId) {
        return getData(player).getCount("pickup", itemId);
    }

    /**
     * 获取物品合成次数
     */
    public static int getItemCraftCount(EntityPlayer player, String itemId) {
        return getData(player).getCount("craft", itemId);
    }

    /**
     * 获取实体击杀次数
     */
    public static int getEntityKillCount(EntityPlayer player, String entityId) {
        return getData(player).getCount("kill", entityId);
    }

    // ==================== 数据记录 ====================

    /**
     * 记录物品消耗
     */
    public static void recordItemConsume(EntityPlayer player, String itemId, int count) {
        getData(player).addCount("consume", itemId, count);
        save(player);
    }

    /**
     * 记录物品使用
     */
    public static void recordItemUse(EntityPlayer player, String itemId) {
        getData(player).addCount("use", itemId, 1);
        save(player);
    }

    /**
     * 记录物品拾取
     */
    public static void recordItemPickup(EntityPlayer player, String itemId, int count) {
        getData(player).addCount("pickup", itemId, count);
        save(player);
    }

    /**
     * 记录物品合成
     */
    public static void recordItemCraft(EntityPlayer player, String itemId, int count) {
        getData(player).addCount("craft", itemId, count);
        save(player);
    }

    /**
     * 记录实体击杀
     */
    public static void recordEntityKill(EntityPlayer player, String entityId) {
        getData(player).addCount("kill", entityId, 1);
        save(player);
    }

    // ==================== 数据管理 ====================

    /**
     * 获取玩家数据
     */
    private static PlayerProgressData getData(EntityPlayer player) {
        return playerData.computeIfAbsent(player.getUniqueID(), 
            k -> new PlayerProgressData());
    }

    /**
     * 从NBT加载
     */
    public static void loadFromPlayer(EntityPlayer player) {
        NBTTagCompound entityData = player.getEntityData();
        
        if (entityData.hasKey(NBT_PROGRESS)) {
            NBTTagCompound progressNBT = entityData.getCompoundTag(NBT_PROGRESS);
            PlayerProgressData data = new PlayerProgressData();
            data.deserialize(progressNBT);
            playerData.put(player.getUniqueID(), data);
            
            if (UnlockRulesConfig.debugMode) {
                System.out.println("[ProgressTracker] 加载玩家进度: " + player.getName());
            }
        }
    }

    /**
     * 保存到NBT
     */
    private static void save(EntityPlayer player) {
        PlayerProgressData data = playerData.get(player.getUniqueID());
        if (data == null) return;

        NBTTagCompound entityData = player.getEntityData();
        NBTTagCompound progressNBT = data.serialize();
        entityData.setTag(NBT_PROGRESS, progressNBT);
    }

    /**
     * 玩家登出时清理内存
     */
    public static void onPlayerLogout(UUID playerUUID) {
        playerData.remove(playerUUID);
    }

    /**
     * 清空所有数据
     */
    public static void clearAll() {
        playerData.clear();
    }

    /**
     * 重置玩家进度
     */
    public static void resetPlayer(EntityPlayer player) {
        playerData.remove(player.getUniqueID());
        NBTTagCompound entityData = player.getEntityData();
        entityData.removeTag(NBT_PROGRESS);
        
        if (UnlockRulesConfig.debugMode) {
            System.out.println("[ProgressTracker] 重置玩家进度: " + player.getName());
        }
    }

    /**
     * 调试输出
     */
    public static void debugPrint(EntityPlayer player) {
        PlayerProgressData data = playerData.get(player.getUniqueID());
        if (data == null) {
            System.out.println("[ProgressTracker] 玩家 " + player.getName() + " 无进度数据");
            return;
        }

        System.out.println("========== 玩家进度 ==========");
        System.out.println("玩家: " + player.getName());
        data.debugPrint();
        System.out.println("==============================");
    }

    // ==================== 玩家数据类 ====================

    /**
     * 玩家进度数据
     * 使用嵌套Map存储: 类型 -> (ID -> 计数)
     */
    private static class PlayerProgressData {
        // 类型 -> (ID -> 计数)
        private final Map<String, Map<String, Integer>> counters = new HashMap<>();

        /**
         * 获取计数
         */
        public int getCount(String type, String id) {
            Map<String, Integer> typeCounters = counters.get(type);
            if (typeCounters == null) return 0;
            return typeCounters.getOrDefault(id, 0);
        }

        /**
         * 增加计数
         */
        public void addCount(String type, String id, int amount) {
            Map<String, Integer> typeCounters = counters.computeIfAbsent(type, 
                k -> new HashMap<>());
            int current = typeCounters.getOrDefault(id, 0);
            typeCounters.put(id, current + amount);
        }

        /**
         * 序列化到NBT
         */
        public NBTTagCompound serialize() {
            NBTTagCompound nbt = new NBTTagCompound();
            
            for (Map.Entry<String, Map<String, Integer>> typeEntry : counters.entrySet()) {
                String type = typeEntry.getKey();
                Map<String, Integer> typeCounters = typeEntry.getValue();
                
                NBTTagCompound typeNBT = new NBTTagCompound();
                for (Map.Entry<String, Integer> entry : typeCounters.entrySet()) {
                    typeNBT.setInteger(entry.getKey(), entry.getValue());
                }
                
                nbt.setTag(type, typeNBT);
            }
            
            return nbt;
        }

        /**
         * 从NBT反序列化
         */
        public void deserialize(NBTTagCompound nbt) {
            counters.clear();
            
            for (String type : nbt.getKeySet()) {
                NBTTagCompound typeNBT = nbt.getCompoundTag(type);
                Map<String, Integer> typeCounters = new HashMap<>();
                
                for (String id : typeNBT.getKeySet()) {
                    typeCounters.put(id, typeNBT.getInteger(id));
                }
                
                counters.put(type, typeCounters);
            }
        }

        /**
         * 调试输出
         */
        public void debugPrint() {
            if (counters.isEmpty()) {
                System.out.println("  (空)");
                return;
            }
            
            for (Map.Entry<String, Map<String, Integer>> typeEntry : counters.entrySet()) {
                System.out.println("  " + typeEntry.getKey() + ":");
                for (Map.Entry<String, Integer> entry : typeEntry.getValue().entrySet()) {
                    System.out.println("    " + entry.getKey() + ": " + entry.getValue());
                }
            }
        }
    }
}
