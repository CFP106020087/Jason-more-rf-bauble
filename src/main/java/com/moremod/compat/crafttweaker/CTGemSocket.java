package com.moremod.compat.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import net.minecraft.item.ItemStack;

/**
 * CraftTweaker API - 宝石镶嵌
 * 
 * 使用示例：
 * ```zenscript
 * import mods.moremod.GemSocket;
 * 
 * // 将宝石镶嵌到武器上
 * GemSocket.socket(<minecraft:diamond_sword>, <moremod:gem>);
 * 
 * // 查询镶嵌数量
 * var count = GemSocket.getCount(<minecraft:diamond_sword>);
 * 
 * // 设置最大镶嵌数
 * GemSocket.setMaxSockets(7);
 * ```
 */
@ZenRegister
@ZenClass("mods.moremod.GemSocket")
public class CTGemSocket {
    
    /**
     * 将宝石镶嵌到物品上
     * 
     * @param item 要镶嵌的物品（武器/装备）
     * @param gem 宝石（必须是已鉴定的）
     * @return 是否成功
     * 
     * 示例：
     * GemSocket.socket(<minecraft:diamond_sword>, <moremod:gem>);
     */
    @ZenMethod
    public static boolean socket(IItemStack item, IItemStack gem) {
        if (item == null || gem == null) {
            CraftTweakerAPI.logError("[GemSocket] 物品或宝石为null");
            return false;
        }
        
        ItemStack itemStack = (ItemStack) item.getInternal();
        ItemStack gemStack = (ItemStack) gem.getInternal();
        
        boolean success = GemSocketHelper.socketGem(itemStack, gemStack);
        
        if (success) {
            CraftTweakerAPI.logInfo("[GemSocket] ✓ 镶嵌成功");
        } else {
            CraftTweakerAPI.logWarning("[GemSocket] ✗ 镶嵌失败（可能：未鉴定/已满/不是宝石）");
        }
        
        return success;
    }
    
    /**
     * 移除指定位置的宝石
     * 
     * @param item 物品
     * @param index 索引（0开始）
     * @return 被移除的宝石
     * 
     * 示例：
     * var gem = GemSocket.remove(<minecraft:diamond_sword>, 0);
     */
    @ZenMethod
    public static IItemStack remove(IItemStack item, int index) {
        if (item == null) {
            return null;
        }
        
        ItemStack itemStack = (ItemStack) item.getInternal();
        ItemStack removed = GemSocketHelper.removeGem(itemStack, index);
        
        if (!removed.isEmpty()) {
            CraftTweakerAPI.logInfo("[GemSocket] ✓ 移除成功：索引 " + index);
            return crafttweaker.api.minecraft.CraftTweakerMC.getIItemStack(removed);
        }
        
        return null;
    }
    
    /**
     * 移除所有宝石
     * 
     * 示例：
     * GemSocket.removeAll(<minecraft:diamond_sword>);
     */
    @ZenMethod
    public static void removeAll(IItemStack item) {
        if (item == null) {
            return;
        }
        
        ItemStack itemStack = (ItemStack) item.getInternal();
        ItemStack[] removed = GemSocketHelper.removeAllGems(itemStack);
        
        CraftTweakerAPI.logInfo("[GemSocket] ✓ 移除了 " + removed.length + " 个宝石");
    }
    
    /**
     * 获取已镶嵌的宝石数量
     * 
     * 示例：
     * var count = GemSocket.getCount(<minecraft:diamond_sword>);
     */
    @ZenMethod
    public static int getCount(IItemStack item) {
        if (item == null) {
            return 0;
        }
        
        ItemStack itemStack = (ItemStack) item.getInternal();
        return GemSocketHelper.getSocketedGemCount(itemStack);
    }
    
    /**
     * 获取剩余可镶嵌数量
     * 
     * 示例：
     * var remaining = GemSocket.getRemaining(<minecraft:diamond_sword>);
     */
    @ZenMethod
    public static int getRemaining(IItemStack item) {
        if (item == null) {
            return 0;
        }
        
        ItemStack itemStack = (ItemStack) item.getInternal();
        return GemSocketHelper.getRemainingSocketCount(itemStack);
    }
    
    /**
     * 检查是否可以镶嵌更多宝石
     * 
     * 示例：
     * if (GemSocket.canSocketMore(<minecraft:diamond_sword>)) {
     *     print("还能镶嵌");
     * }
     */
    @ZenMethod
    public static boolean canSocketMore(IItemStack item) {
        if (item == null) {
            return false;
        }
        
        ItemStack itemStack = (ItemStack) item.getInternal();
        return GemSocketHelper.canSocketMore(itemStack);
    }
    
    /**
     * 获取平均宝石品质
     * 
     * 示例：
     * var avgQuality = GemSocket.getAverageQuality(<minecraft:diamond_sword>);
     */
    @ZenMethod
    public static int getAverageQuality(IItemStack item) {
        if (item == null) {
            return 0;
        }
        
        ItemStack itemStack = (ItemStack) item.getInternal();
        return GemSocketHelper.getAverageGemQuality(itemStack);
    }
    
    /**
     * 获取总词条数
     * 
     * 示例：
     * var totalAffixes = GemSocket.getTotalAffixes(<minecraft:diamond_sword>);
     */
    @ZenMethod
    public static int getTotalAffixes(IItemStack item) {
        if (item == null) {
            return 0;
        }
        
        ItemStack itemStack = (ItemStack) item.getInternal();
        return GemSocketHelper.getTotalAffixCount(itemStack);
    }
    
    /**
     * 设置最大镶嵌数量
     * 
     * @param max 最大数量（1-10）
     * 
     * 示例：
     * GemSocket.setMaxSockets(7);  // 允许镶嵌7个宝石
     */
    @ZenMethod
    public static void setMaxSockets(int max) {
        if (max < 1 || max > 10) {
            CraftTweakerAPI.logError("[GemSocket] 最大镶嵌数必须在1-10之间，当前值: " + max);
            return;
        }
        
        GemSocketHelper.setMaxSockets(max);
        CraftTweakerAPI.logInfo("[GemSocket] ✓ 最大镶嵌数已设置为: " + max);
    }
    
    /**
     * 获取最大镶嵌数量
     * 
     * 示例：
     * var max = GemSocket.getMaxSockets();
     */
    @ZenMethod
    public static int getMaxSockets() {
        return GemSocketHelper.getMaxSockets();
    }
    
    /**
     * 打印物品的镶嵌信息（调试用）
     * 
     * 示例：
     * GemSocket.debug(<minecraft:diamond_sword>);
     */
    @ZenMethod
    public static void debug(IItemStack item) {
        if (item == null) {
            CraftTweakerAPI.logInfo("[GemSocket] 物品为null");
            return;
        }
        
        ItemStack itemStack = (ItemStack) item.getInternal();
        
        CraftTweakerAPI.logInfo("========== 宝石镶嵌信息 ==========");
        CraftTweakerAPI.logInfo("物品: " + item.getDisplayName());
        CraftTweakerAPI.logInfo("已镶嵌: " + GemSocketHelper.getSocketedGemCount(itemStack) + "/" + 
                               GemSocketHelper.getMaxSockets());
        CraftTweakerAPI.logInfo("平均品质: " + GemSocketHelper.getAverageGemQuality(itemStack) + "%");
        CraftTweakerAPI.logInfo("总词条数: " + GemSocketHelper.getTotalAffixCount(itemStack));
        CraftTweakerAPI.logInfo("===================================");
    }
}