package com.moremod.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.village.MerchantRecipe;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 管理交易折扣的辅助类
 */
public class TradeDiscountHelper {

    // 存储原始价格的映射
    private static final Map<MerchantRecipe, RecipeOriginalPrices> originalPrices = new HashMap<>();

    // 存储玩家折扣信息
    private static final Map<String, PlayerDiscountInfo> playerDiscounts = new HashMap<>();

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

            // 在NBT中标记已应用折扣
            markAsDiscounted(recipe, discountRate);
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

            // 清除NBT标记
            clearDiscountMark(recipe);
        }
    }

    /**
     * 检查配方是否有折扣
     */
    public static boolean hasDiscount(MerchantRecipe recipe) {
        return originalPrices.containsKey(recipe) && isMarkedAsDiscounted(recipe);
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
    }

    /**
     * 在NBT中标记配方已应用折扣
     */
    private static void markAsDiscounted(MerchantRecipe recipe, double discountRate) {
        ItemStack item = recipe.getItemToSell();
        if (!item.isEmpty()) {
            NBTTagCompound nbt = item.getTagCompound();
            if (nbt == null) {
                nbt = new NBTTagCompound();
                item.setTagCompound(nbt);
            }
            nbt.setBoolean("Persuader_Discounted", true);
            nbt.setDouble("Persuader_DiscountRate", discountRate);
        }
    }

    /**
     * 清除NBT中的折扣标记
     */
    private static void clearDiscountMark(MerchantRecipe recipe) {
        ItemStack item = recipe.getItemToSell();
        if (!item.isEmpty() && item.hasTagCompound()) {
            NBTTagCompound nbt = item.getTagCompound();
            nbt.removeTag("Persuader_Discounted");
            nbt.removeTag("Persuader_DiscountRate");
        }
    }

    /**
     * 检查配方是否被标记为已折扣
     */
    private static boolean isMarkedAsDiscounted(MerchantRecipe recipe) {
        ItemStack item = recipe.getItemToSell();
        if (!item.isEmpty() && item.hasTagCompound()) {
            return item.getTagCompound().getBoolean("Persuader_Discounted");
        }
        return false;
    }
}