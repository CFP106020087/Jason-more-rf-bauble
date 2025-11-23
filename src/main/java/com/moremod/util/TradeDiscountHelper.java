package com.moremod.util;

import net.minecraft.village.MerchantRecipe;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理交易折扣的辅助类
 * 修改版本: 不再给交易产出物品添加 NBT,改用 Map 存储折扣信息
 */
public class TradeDiscountHelper {

    // 存储原始价格的映射
    private static final Map<MerchantRecipe, RecipeOriginalPrices> originalPrices = new HashMap<>();

    // 存储玩家折扣信息
    private static final Map<String, PlayerDiscountInfo> playerDiscounts = new HashMap<>();

    // ✅ 新增: 存储已应用折扣的配方信息
    private static final Map<MerchantRecipe, DiscountInfo> discountedRecipes = new HashMap<>();

    /**
     * 存储配方的原始价格
     */
    private static class RecipeOriginalPrices {
        public final int firstItemPrice;
        public final int secondItemPrice;
        public final long timestamp;

        public RecipeOriginalPrices(int first, int second) {
            this.firstItemPrice = first;
            this.secondItemPrice = second;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 存储玩家的折扣信息
     */
    private static class PlayerDiscountInfo {
        public final double discountRate;
        public final int energy;
        public final long timestamp;

        public PlayerDiscountInfo(double rate, int energy) {
            this.discountRate = rate;
            this.energy = energy;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * ✅ 新增: 存储折扣信息
     */
    private static class DiscountInfo {
        public final double discountRate;
        public final long timestamp;

        public DiscountInfo(double rate) {
            this.discountRate = rate;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 保存配方的原始价格
     */
    public static void saveOriginalPrices(MerchantRecipe recipe) {
        if (!originalPrices.containsKey(recipe)) {
            int firstPrice = recipe.getItemToBuy().getCount();
            int secondPrice = recipe.getSecondItemToBuy().isEmpty() ? 0 :
                    recipe.getSecondItemToBuy().getCount();
            originalPrices.put(recipe, new RecipeOriginalPrices(firstPrice, secondPrice));
        }
    }

    /**
     * 应用折扣到配方
     */
    public static void applyDiscount(MerchantRecipe recipe, double discountRate) {
        // 先保存原始价格
        saveOriginalPrices(recipe);

        RecipeOriginalPrices original = originalPrices.get(recipe);
        if (original != null) {
            // 应用折扣到第一个物品
            int discountedPrice1 = Math.max(1, (int)(original.firstItemPrice * (1.0 - discountRate)));
            recipe.getItemToBuy().setCount(discountedPrice1);

            // 应用折扣到第二个物品（如果存在）
            if (!recipe.getSecondItemToBuy().isEmpty() && original.secondItemPrice > 0) {
                int discountedPrice2 = Math.max(1, (int)(original.secondItemPrice * (1.0 - discountRate)));
                recipe.getSecondItemToBuy().setCount(discountedPrice2);
            }

            // ✅ 改为在 Map 中标记,而不是修改产出物品的 NBT
            discountedRecipes.put(recipe, new DiscountInfo(discountRate));
        }
    }

    /**
     * 移除配方的折扣
     */
    public static void removeDiscount(MerchantRecipe recipe) {
        RecipeOriginalPrices original = originalPrices.get(recipe);
        if (original != null) {
            // 恢复原始价格
            recipe.getItemToBuy().setCount(original.firstItemPrice);
            if (!recipe.getSecondItemToBuy().isEmpty()) {
                recipe.getSecondItemToBuy().setCount(original.secondItemPrice);
            }

            // ✅ 从 Map 中移除折扣标记,而不是清除物品 NBT
            discountedRecipes.remove(recipe);
        }
    }

    /**
     * 检查配方是否有折扣
     */
    public static boolean hasDiscount(MerchantRecipe recipe) {
        // ✅ 从 Map 中检查,而不是检查物品 NBT
        return discountedRecipes.containsKey(recipe);
    }

    /**
     * ✅ 新增: 获取配方的折扣率
     */
    public static double getDiscountRate(MerchantRecipe recipe) {
        DiscountInfo info = discountedRecipes.get(recipe);
        return info != null ? info.discountRate : 0.0;
    }

    /**
     * 获取第一个物品的原始价格
     */
    public static int getOriginalPrice(MerchantRecipe recipe) {
        RecipeOriginalPrices original = originalPrices.get(recipe);
        return original != null ? original.firstItemPrice : recipe.getItemToBuy().getCount();
    }

    /**
     * 获取第二个物品的原始价格
     */
    public static int getSecondOriginalPrice(MerchantRecipe recipe) {
        RecipeOriginalPrices original = originalPrices.get(recipe);
        return original != null ? original.secondItemPrice :
                (recipe.getSecondItemToBuy().isEmpty() ? 0 : recipe.getSecondItemToBuy().getCount());
    }

    /**
     * 保存玩家的折扣信息
     */
    public static void savePlayerDiscount(String playerUUID, double discountRate, int energy) {
        playerDiscounts.put(playerUUID, new PlayerDiscountInfo(discountRate, energy));
    }

    /**
     * 获取玩家的折扣率
     */
    public static double getPlayerDiscountRate(String playerUUID) {
        PlayerDiscountInfo info = playerDiscounts.get(playerUUID);
        return info != null ? info.discountRate : 0.0;
    }

    /**
     * 清理过期的数据
     */
    public static void cleanupExpiredData() {
        long currentTime = System.currentTimeMillis();
        long expirationTime = 30 * 60 * 1000; // 30分钟

        // 清理过期的配方原始价格
        originalPrices.entrySet().removeIf(entry ->
                currentTime - entry.getValue().timestamp > expirationTime);

        // 清理过期的玩家折扣信息
        playerDiscounts.entrySet().removeIf(entry ->
                currentTime - entry.getValue().timestamp > expirationTime);

        // ✅ 清理过期的折扣标记
        discountedRecipes.entrySet().removeIf(entry ->
                currentTime - entry.getValue().timestamp > expirationTime);
    }

    /**
     * ✅ 新增: 清除所有折扣数据（用于调试或重置）
     */
    public static void clearAll() {
        originalPrices.clear();
        playerDiscounts.clear();
        discountedRecipes.clear();
    }

    // ❌ 已删除的方法（不再需要）:
    // - markAsDiscounted() - 不再给物品添加 NBT
    // - clearDiscountMark() - 不再清除物品 NBT
    // - isMarkedAsDiscounted() - 不再检查物品 NBT
}