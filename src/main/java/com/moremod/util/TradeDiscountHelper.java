package com.moremod.util;

import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理交易折扣的辅助类
 * 使用基于内容的键而非对象引用，解决村民序列化/反序列化后折扣丢失的问题
 * 支持多种独立的折扣来源（说服器、人性值等）
 */
public class TradeDiscountHelper {

    // 不同的折扣来源类型
    public enum DiscountSource {
        PERSUADER,      // 说服器折扣
        HUMANITY        // 人性值折扣
    }

    // 存储原始价格（未经任何mod修改的价格）
    private static final Map<String, RecipeOriginalPrices> originalPrices = new HashMap<>();

    // 存储玩家折扣信息
    private static final Map<String, PlayerDiscountInfo> playerDiscounts = new HashMap<>();

    // 按来源分别存储折扣信息
    private static final Map<String, DiscountInfo> persuaderDiscounts = new HashMap<>();
    private static final Map<String, DiscountInfo> humanityDiscounts = new HashMap<>();

    /**
     * 生成配方的唯一键（基于配方内容）
     * 这个键在序列化/反序列化后保持一致
     */
    private static String getRecipeKey(MerchantRecipe recipe) {
        StringBuilder sb = new StringBuilder();

        // 第一个购买物品
        ItemStack buy1 = recipe.getItemToBuy();
        if (!buy1.isEmpty()) {
            sb.append(buy1.getItem().getRegistryName());
            sb.append("@").append(buy1.getMetadata());
            // 注意：不包含count，因为count会被折扣修改
        }
        sb.append("|");

        // 第二个购买物品
        ItemStack buy2 = recipe.getSecondItemToBuy();
        if (!buy2.isEmpty()) {
            sb.append(buy2.getItem().getRegistryName());
            sb.append("@").append(buy2.getMetadata());
        }
        sb.append("|");

        // 出售物品
        ItemStack sell = recipe.getItemToSell();
        if (!sell.isEmpty()) {
            sb.append(sell.getItem().getRegistryName());
            sb.append("@").append(sell.getMetadata());
            sb.append("x").append(sell.getCount());
            // 出售物品的NBT也应包含（如附魔书）
            if (sell.hasTagCompound()) {
                sb.append("#").append(sell.getTagCompound().hashCode());
            }
        }

        return sb.toString();
    }

    /**
     * 获取指定来源的折扣Map
     */
    private static Map<String, DiscountInfo> getDiscountMap(DiscountSource source) {
        switch (source) {
            case PERSUADER:
                return persuaderDiscounts;
            case HUMANITY:
                return humanityDiscounts;
            default:
                return persuaderDiscounts;
        }
    }

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
     * 存储折扣信息
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
     * 保存配方的原始价格（首次调用时）
     */
    public static void saveOriginalPrices(MerchantRecipe recipe) {
        String key = getRecipeKey(recipe);
        if (!originalPrices.containsKey(key)) {
            int firstPrice = recipe.getItemToBuy().getCount();
            int secondPrice = recipe.getSecondItemToBuy().isEmpty() ? 0 :
                    recipe.getSecondItemToBuy().getCount();
            originalPrices.put(key, new RecipeOriginalPrices(firstPrice, secondPrice));
        }
    }

    /**
     * 应用折扣到配方（通用方法，默认使用PERSUADER来源）
     */
    public static void applyDiscount(MerchantRecipe recipe, double discountRate) {
        applyDiscount(recipe, discountRate, DiscountSource.PERSUADER);
    }

    /**
     * 应用折扣到配方（指定来源）
     */
    public static void applyDiscount(MerchantRecipe recipe, double discountRate, DiscountSource source) {
        // 先保存原始价格
        saveOriginalPrices(recipe);

        String key = getRecipeKey(recipe);
        RecipeOriginalPrices original = originalPrices.get(key);
        if (original != null) {
            // 计算所有来源的总折扣
            Map<String, DiscountInfo> sourceMap = getDiscountMap(source);
            sourceMap.put(key, new DiscountInfo(discountRate));

            // 应用组合折扣
            applyAllDiscounts(recipe, key, original);
        }
    }

    /**
     * 应用所有来源的组合折扣
     */
    private static void applyAllDiscounts(MerchantRecipe recipe, String key, RecipeOriginalPrices original) {
        // 计算总折扣率（乘法叠加）
        double combinedMultiplier = 1.0;

        DiscountInfo persuader = persuaderDiscounts.get(key);
        if (persuader != null) {
            combinedMultiplier *= (1.0 - persuader.discountRate);
        }

        DiscountInfo humanity = humanityDiscounts.get(key);
        if (humanity != null) {
            combinedMultiplier *= (1.0 - humanity.discountRate);
        }

        // 应用组合折扣到第一个物品
        int discountedPrice1 = Math.max(1, (int)(original.firstItemPrice * combinedMultiplier));
        recipe.getItemToBuy().setCount(discountedPrice1);

        // 应用折扣到第二个物品（如果存在）
        if (!recipe.getSecondItemToBuy().isEmpty() && original.secondItemPrice > 0) {
            int discountedPrice2 = Math.max(1, (int)(original.secondItemPrice * combinedMultiplier));
            recipe.getSecondItemToBuy().setCount(discountedPrice2);
        }
    }

    /**
     * 移除配方的折扣（通用方法，默认使用PERSUADER来源）
     */
    public static void removeDiscount(MerchantRecipe recipe) {
        removeDiscount(recipe, DiscountSource.PERSUADER);
    }

    /**
     * 移除配方的折扣（指定来源）
     */
    public static void removeDiscount(MerchantRecipe recipe, DiscountSource source) {
        String key = getRecipeKey(recipe);
        RecipeOriginalPrices original = originalPrices.get(key);

        // 移除指定来源的折扣
        Map<String, DiscountInfo> sourceMap = getDiscountMap(source);
        sourceMap.remove(key);

        if (original != null) {
            // 检查是否还有其他折扣
            boolean hasOtherDiscounts = persuaderDiscounts.containsKey(key) ||
                                        humanityDiscounts.containsKey(key);

            if (hasOtherDiscounts) {
                // 重新计算并应用剩余折扣
                applyAllDiscounts(recipe, key, original);
            } else {
                // 没有任何折扣了，恢复原始价格
                recipe.getItemToBuy().setCount(original.firstItemPrice);
                if (!recipe.getSecondItemToBuy().isEmpty()) {
                    recipe.getSecondItemToBuy().setCount(original.secondItemPrice);
                }
            }
        }
    }

    /**
     * 检查配方是否有折扣（任何来源）
     */
    public static boolean hasDiscount(MerchantRecipe recipe) {
        String key = getRecipeKey(recipe);
        return persuaderDiscounts.containsKey(key) || humanityDiscounts.containsKey(key);
    }

    /**
     * 检查配方是否有指定来源的折扣
     */
    public static boolean hasDiscount(MerchantRecipe recipe, DiscountSource source) {
        String key = getRecipeKey(recipe);
        return getDiscountMap(source).containsKey(key);
    }

    /**
     * 获取配方的总折扣率
     */
    public static double getDiscountRate(MerchantRecipe recipe) {
        String key = getRecipeKey(recipe);
        double totalDiscount = 0.0;

        DiscountInfo persuader = persuaderDiscounts.get(key);
        if (persuader != null) {
            totalDiscount += persuader.discountRate;
        }

        DiscountInfo humanity = humanityDiscounts.get(key);
        if (humanity != null) {
            totalDiscount += humanity.discountRate;
        }

        return totalDiscount;
    }

    /**
     * 获取第一个物品的原始价格
     */
    public static int getOriginalPrice(MerchantRecipe recipe) {
        String key = getRecipeKey(recipe);
        RecipeOriginalPrices original = originalPrices.get(key);
        return original != null ? original.firstItemPrice : recipe.getItemToBuy().getCount();
    }

    /**
     * 获取第二个物品的原始价格
     */
    public static int getSecondOriginalPrice(MerchantRecipe recipe) {
        String key = getRecipeKey(recipe);
        RecipeOriginalPrices original = originalPrices.get(key);
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

        // 清理过期的折扣标记
        persuaderDiscounts.entrySet().removeIf(entry ->
                currentTime - entry.getValue().timestamp > expirationTime);
        humanityDiscounts.entrySet().removeIf(entry ->
                currentTime - entry.getValue().timestamp > expirationTime);
    }

    /**
     * 清除所有折扣数据（用于调试或重置）
     */
    public static void clearAll() {
        originalPrices.clear();
        playerDiscounts.clear();
        persuaderDiscounts.clear();
        humanityDiscounts.clear();
    }
}
