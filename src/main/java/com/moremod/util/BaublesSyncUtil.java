package com.moremod.util;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 兼容不同 Baubles 版本的安全同步工具 - 高性能版
 * 
 * 优化：
 * 1. 反射方法缓存：只查找一次
 * 2. 减少日志输出：只在首次或debug模式输出
 * 3. 调用频率限制：防止被过度频繁调用
 * 4. 批量优化：智能跳过空槽位
 */
public final class BaublesSyncUtil {
    private BaublesSyncUtil() {}
    
    // ========== 反射缓存 ==========
    
    /** 是否已经尝试过查找 setChanged 方法 */
    private static boolean methodChecked = false;
    
    /** 缓存的 setChanged 方法（可能为null表示不支持）*/
    private static Method cachedSetChangedMethod = null;
    
    /** 同步方法类型 */
    private static SyncMethodType syncMethodType = SyncMethodType.UNKNOWN;
    
    // ========== 日志控制 ==========
    
    /** 是否启用调试日志 */
    private static boolean debugMode = false;
    
    /** 是否已经输出过方法类型日志 */
    private static boolean methodTypeLogged = false;
    
    // ========== 频率限制 ==========
    
    /** 玩家 → 上次同步时间（tick） */
    private static final Map<UUID, Integer> lastSyncTick = new HashMap<>();
    
    /** 最小同步间隔（tick） */
    private static final int MIN_SYNC_INTERVAL = 2; // 0.1秒
    
    /**
     * 同步所有 baubles 槽位（尽量只在服务端调用）
     * 
     * 优化：
     * - 反射方法只查找一次并缓存
     * - 日志只在首次或debug模式输出
     * - 有频率限制，避免过度调用
     */
    public static void safeSyncAll(EntityPlayer player) {
        // 频率限制检查
        if (!checkSyncCooldown(player)) {
            return; // 太频繁，跳过
        }
        
        IBaublesItemHandler h = BaublesApi.getBaublesHandler(player);
        if (h == null) return;
        
        // 初始化反射方法（只在第一次调用时）
        if (!methodChecked) {
            initializeReflectionMethod(h);
            methodChecked = true;
        }
        
        // 根据缓存的方法类型执行同步
        switch (syncMethodType) {
            case SET_CHANGED:
                syncUsingSetChanged(h);
                break;
                
            case FALLBACK:
                syncUsingFallback(h);
                break;
                
            case UNKNOWN:
            default:
                // 理论上不会到这里，但作为保险
                syncUsingFallback(h);
                break;
        }
    }
    
    /**
     * 同步单个槽位
     * 
     * 优化：使用缓存的反射方法
     */
    public static void safeSyncSlot(EntityPlayer player, int slot) {
        IBaublesItemHandler h = BaublesApi.getBaublesHandler(player);
        if (h == null) return;
        
        // 初始化反射方法
        if (!methodChecked) {
            initializeReflectionMethod(h);
            methodChecked = true;
        }
        
        // 根据缓存的方法类型执行同步
        switch (syncMethodType) {
            case SET_CHANGED:
                try {
                    cachedSetChangedMethod.invoke(h, slot, true);
                    if (debugMode) {
                        System.out.println("[BaublesSyncUtil] 同步槽位 " + slot);
                    }
                } catch (Exception e) {
                    if (debugMode) {
                        e.printStackTrace();
                    }
                }
                break;
                
            case FALLBACK:
                ItemStack cur = h.getStackInSlot(slot);
                if (!cur.isEmpty()) {
                    h.setStackInSlot(slot, ItemStack.EMPTY);
                    h.setStackInSlot(slot, cur.copy());
                    if (debugMode) {
                        System.out.println("[BaublesSyncUtil] 同步槽位 " + slot + ": " + cur.getDisplayName());
                    }
                }
                break;
        }
    }
    
    // ========== 内部方法 ==========
    
    /**
     * 初始化反射方法（只调用一次）
     */
    private static void initializeReflectionMethod(IBaublesItemHandler handler) {
        try {
            cachedSetChangedMethod = handler.getClass().getMethod("setChanged", int.class, boolean.class);
            syncMethodType = SyncMethodType.SET_CHANGED;
            
            if (!methodTypeLogged) {
                System.out.println("[BaublesSyncUtil] 使用 setChanged 方法同步");
                methodTypeLogged = true;
            }
        } catch (Throwable e) {
            syncMethodType = SyncMethodType.FALLBACK;
            
            if (!methodTypeLogged) {
                System.out.println("[BaublesSyncUtil] setChanged 方法不可用，使用兜底方案");
                methodTypeLogged = true;
            }
        }
    }
    
    /**
     * 使用 setChanged 方法同步
     */
    private static void syncUsingSetChanged(IBaublesItemHandler handler) {
        if (cachedSetChangedMethod == null) return;
        
        try {
            for (int i = 0; i < handler.getSlots(); i++) {
                cachedSetChangedMethod.invoke(handler, i, true);
            }
            
            if (debugMode) {
                System.out.println("[BaublesSyncUtil] 使用 setChanged 同步了 " + handler.getSlots() + " 个槽位");
            }
        } catch (Exception e) {
            if (debugMode) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 使用兜底方案同步
     */
    private static void syncUsingFallback(IBaublesItemHandler handler) {
        int syncedCount = 0;
        
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack cur = handler.getStackInSlot(i);
            if (!cur.isEmpty()) {
                handler.setStackInSlot(i, ItemStack.EMPTY);
                handler.setStackInSlot(i, cur.copy());
                syncedCount++;
            }
        }
        
        if (debugMode && syncedCount > 0) {
            System.out.println("[BaublesSyncUtil] 使用兜底方案同步了 " + syncedCount + " 个槽位");
        }
    }
    
    /**
     * 检查同步冷却
     * @return true=可以同步, false=冷却中
     */
    private static boolean checkSyncCooldown(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        Integer lastTick = lastSyncTick.get(playerId);
        int currentTick = player.ticksExisted;
        
        if (lastTick == null || (currentTick - lastTick) >= MIN_SYNC_INTERVAL) {
            lastSyncTick.put(playerId, currentTick);
            return true;
        }
        
        return false;
    }
    
    // ========== 同步方法类型枚举 ==========
    
    private enum SyncMethodType {
        UNKNOWN,
        SET_CHANGED,
        FALLBACK
    }
    
    // ========== 公共API ==========
    
    /**
     * 设置调试模式
     */
    public static void setDebugMode(boolean debug) {
        debugMode = debug;
    }
    
    /**
     * 清理玩家数据（玩家退出时调用）
     */
    public static void cleanupPlayer(UUID playerId) {
        lastSyncTick.remove(playerId);
    }
    
    /**
     * 获取统计信息
     */
    public static String getStats() {
        return String.format("方法: %s | 缓存玩家: %d",
                syncMethodType, lastSyncTick.size());
    }
    
    /**
     * 强制刷新方法缓存（用于测试）
     */
    public static void resetMethodCache() {
        methodChecked = false;
        cachedSetChangedMethod = null;
        syncMethodType = SyncMethodType.UNKNOWN;
        methodTypeLogged = false;
    }
}