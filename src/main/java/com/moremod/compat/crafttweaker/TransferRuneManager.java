package com.moremod.compat.crafttweaker;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import java.util.HashMap;
import java.util.Map;

public class TransferRuneManager {

    private static Map<ItemStack, RuneData> runeRegistry = new HashMap<>();
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
     * 添加符文
     */
    public static void addRune(ItemStack item, float successRate, int xpCost) {
        if (item.isEmpty()) {
            System.out.println("[TransferRunes] 警告：尝试添加空物品作为符文");
            return;
        }
        RuneData data = new RuneData(successRate, xpCost);
        runeRegistry.put(item.copy(), data);  // 使用副本避免外部修改
        System.out.println("[TransferRunes] 注册符文: " + item.getDisplayName() +
                " (Item: " + item.getItem().getRegistryName() + ", meta: " + item.getMetadata() + ")");
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

        for (Map.Entry<ItemStack, RuneData> entry : runeRegistry.entrySet()) {
            if (ItemStack.areItemsEqual(stack, entry.getKey())) {
                return true;
            }
        }

        // 调试日志
        System.out.println("[TransferRunes] 检查符文失败: " + stack.getDisplayName() +
                " (registry size: " + runeRegistry.size() + ")");
        return false;
    }
    
    /**
     * 获取符文数据
     */
    public static RuneData getRuneData(ItemStack stack) {
        for (Map.Entry<ItemStack, RuneData> entry : runeRegistry.entrySet()) {
            if (ItemStack.areItemsEqual(stack, entry.getKey())) {
                return entry.getValue();
            }
        }
        // 默认数据
        return new RuneData(1.0f, 0);
    }
    
    // Setter方法
    public static void setSuccessModifier(ItemStack item, float modifier) {
        RuneData data = getRuneData(item);
        if (data != null) {
            data.successRate = modifier;
        }
    }
    
    public static void setXpCost(ItemStack item, int cost) {
        RuneData data = getRuneData(item);
        if (data != null) {
            data.xpCost = cost;
        }
    }
    
    public static void setMaxAffixLimit(ItemStack item, int limit) {
        RuneData data = getRuneData(item);
        if (data != null) {
            data.maxAffixes = limit;
        }
    }
    
    public static void clearRunes() {
        runeRegistry.clear();
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
        runeRegistry.entrySet().removeIf(entry -> 
            ItemStack.areItemsEqual(entry.getKey(), item)
        );
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
}