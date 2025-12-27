package com.moremod.compat.crafttweaker;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import java.util.HashMap;
import java.util.Map;

public class TransferRuneManager {

    // 使用 String key (registryName:meta) 替代 ItemStack，避免 HashMap 比较问题
    private static Map<String, RuneData> runeRegistry = new HashMap<>();
    private static Map<String, ItemStack> runeItems = new HashMap<>();  // 保存原始 ItemStack 用于显示
    private static boolean allowAnyItem = false;
    private static int baseXpCost = 5;
    private static boolean destroyOnFail = false;
    private static boolean defaultsLoaded = false;
    
    public static class RuneData {
        public float successRate;
        public int xpCost;
        public int maxAffixes = 6;
        
        public RuneData(float successRate, int xpCost) {
            this.successRate = Math.max(0, Math.min(1, successRate));
            this.xpCost = Math.max(0, xpCost);
        }
    }
    
    /**
     * 生成物品的唯一 key (registryName:meta)
     */
    private static String getItemKey(ItemStack stack) {
        if (stack.isEmpty()) return "";
        return stack.getItem().getRegistryName() + ":" + stack.getMetadata();
    }

    /**
     * 添加符文
     */
    public static void addRune(ItemStack item, float successRate, int xpCost) {
        if (item.isEmpty()) {
            System.out.println("[TransferRunes] 警告：尝试添加空物品作为符文");
            return;
        }
        String key = getItemKey(item);
        RuneData data = new RuneData(successRate, xpCost);
        runeRegistry.put(key, data);
        runeItems.put(key, item.copy());  // 保存副本用于显示
        System.out.println("[TransferRunes] 注册符文: " + item.getDisplayName() +
                " (Key: " + key + ")");
    }
    
    /**
     * 检查物品是否是有效符文
     */
    public static boolean isValidRune(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (allowAnyItem) return true;

        // 确保默认符文已加载
        if (!defaultsLoaded) {
            loadDefaultRunes();
        }

        String key = getItemKey(stack);
        boolean valid = runeRegistry.containsKey(key);

        if (!valid) {
            // 调试日志
            System.out.println("[TransferRunes] 检查符文失败: " + stack.getDisplayName() +
                    " (Key: " + key + ", registry size: " + runeRegistry.size() + ")");
            // 打印已注册的符文列表帮助调试
            if (!runeRegistry.isEmpty()) {
                System.out.println("[TransferRunes] 已注册符文: " + runeRegistry.keySet());
            }
        }

        return valid;
    }
    
    /**
     * 获取符文数据
     */
    public static RuneData getRuneData(ItemStack stack) {
        String key = getItemKey(stack);
        RuneData data = runeRegistry.get(key);
        if (data != null) {
            return data;
        }
        // 默认数据
        return new RuneData(1.0f, 0);
    }
    
    // Setter方法
    public static void setSuccessModifier(ItemStack item, float modifier) {
        String key = getItemKey(item);
        RuneData data = runeRegistry.get(key);
        if (data != null) {
            data.successRate = Math.max(0, Math.min(1, modifier));
        } else {
            System.out.println("[TransferRunes] 警告：尝试修改未注册的符文: " + key);
        }
    }

    public static void setXpCost(ItemStack item, int cost) {
        String key = getItemKey(item);
        RuneData data = runeRegistry.get(key);
        if (data != null) {
            data.xpCost = Math.max(0, cost);
        } else {
            System.out.println("[TransferRunes] 警告：尝试修改未注册的符文: " + key);
        }
    }

    public static void setMaxAffixLimit(ItemStack item, int limit) {
        String key = getItemKey(item);
        RuneData data = runeRegistry.get(key);
        if (data != null) {
            data.maxAffixes = limit;
        } else {
            System.out.println("[TransferRunes] 警告：尝试修改未注册的符文: " + key);
        }
    }
    
    public static void clearRunes() {
        runeRegistry.clear();
        runeItems.clear();
        // 重置 defaultsLoaded，但不自动重新加载默认值
        // 这样 CRT 可以完全控制符文列表
        defaultsLoaded = true;  // 防止默认符文自动加载
        System.out.println("[TransferRunes] 已清空所有符文配置");
    }
    
    public static void setAllowAnyItem(boolean allow) {
        allowAnyItem = allow;
    }
    
    public static void setBaseXpCost(int cost) {
        baseXpCost = cost;
    }
    
    public static void setDestroyOnFail(boolean destroy) {
        destroyOnFail = destroy;
    }
    
    public static void removeRune(ItemStack item) {
        String key = getItemKey(item);
        runeRegistry.remove(key);
        runeItems.remove(key);
        System.out.println("[TransferRunes] 移除符文: " + key);
    }
    
    // Getter方法
    public static int getBaseXpCost() {
        return baseXpCost;
    }

    public static boolean isDestroyOnFail() {
        return destroyOnFail;
    }

    /**
     * 加载默认符文配置
     * 在模组初始化时调用，确保转移台有可用的符文
     */
    public static void loadDefaultRunes() {
        if (defaultsLoaded) return;
        defaultsLoaded = true;

        System.out.println("[TransferRunes] 加载默认符文配置...");

        // 默认符文：龙息 - 100% 成功率，1级经验
        addRune(new ItemStack(Items.DRAGON_BREATH), 1.0f, 1);

        System.out.println("[TransferRunes] 默认符文配置加载完成，共 " + runeRegistry.size() + " 种符文");
    }

    /**
     * 检查默认配置是否已加载
     */
    public static boolean isDefaultsLoaded() {
        return defaultsLoaded;
    }

    /**
     * 强制重新加载默认符文
     * 用于 CRT 在 clear() 后想要恢复默认配置
     */
    public static void reloadDefaults() {
        defaultsLoaded = false;
        loadDefaultRunes();
    }

    /**
     * 打印所有已注册的符文（调试用）
     */
    public static void printAllRunes() {
        System.out.println("[TransferRunes] ===== 已注册符文列表 =====");
        if (runeRegistry.isEmpty()) {
            System.out.println("[TransferRunes] (空)");
        } else {
            for (Map.Entry<String, RuneData> entry : runeRegistry.entrySet()) {
                RuneData data = entry.getValue();
                ItemStack stack = runeItems.get(entry.getKey());
                String displayName = stack != null ? stack.getDisplayName() : entry.getKey();
                System.out.println(String.format("[TransferRunes]   %s: 成功率=%.0f%%, 经验=%d, 词条上限=%d",
                        displayName, data.successRate * 100, data.xpCost, data.maxAffixes));
            }
        }
        System.out.println("[TransferRunes] =============================");
    }

    /**
     * 获取已注册符文数量
     */
    public static int getRuneCount() {
        return runeRegistry.size();
    }
}