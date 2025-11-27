package com.moremod.compat.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * CraftTweaker API - 转移符文配置
 */
@ZenRegister
@ZenClass("mods.moremod.TransferRunes")
public class CTTransferRunes {
    
    /**
     * 添加可用作转移符文的物品
     */
    @ZenMethod
    public static void addRune(IItemStack item, double successRate, int xpCost) {
        ItemStack stack = toItemStack(item);
        if (stack.isEmpty()) {
            CraftTweakerAPI.logError("[TransferRunes] 无效的物品");
            return;
        }
        
        TransferRuneManager.addRune(stack, (float)successRate, xpCost);
        CraftTweakerAPI.logInfo(String.format(
            "[TransferRunes] 添加符文: %s (成功率: %.0f%%, 经验: %d)",
            stack.getDisplayName(), successRate * 100, xpCost
        ));
    }
    
    /**
     * 批量添加符文配置
     */
    @ZenMethod
    public static void addRuneSet(String tier, IItemStack[] items, double successRate, int xpCost) {
        for (IItemStack item : items) {
            addRune(item, successRate, xpCost);
        }
        CraftTweakerAPI.logInfo(String.format(
            "[TransferRunes] 添加 %s 级符文组: %d个物品",
            tier, items.length
        ));
    }
    
    /**
     * 移除符文配置
     */
    @ZenMethod
    public static void removeRune(IItemStack item) {
        ItemStack stack = toItemStack(item);
        if (stack.isEmpty()) {
            CraftTweakerAPI.logError("[TransferRunes] 无效的物品");
            return;
        }
        
        TransferRuneManager.removeRune(stack);
        CraftTweakerAPI.logInfo("[TransferRunes] 移除符文: " + stack.getDisplayName());
    }
    
    /**
     * 设置成功率修正
     */
    @ZenMethod
    public static void setSuccessModifier(IItemStack item, double modifier) {
        ItemStack stack = toItemStack(item);
        if (stack.isEmpty()) {
            CraftTweakerAPI.logError("[TransferRunes] 无效的物品");
            return;
        }
        
        TransferRuneManager.setSuccessModifier(stack, (float)modifier);
    }
    
    /**
     * 设置经验消耗
     */
    @ZenMethod
    public static void setXpCost(IItemStack item, int cost) {
        ItemStack stack = toItemStack(item);
        if (stack.isEmpty()) {
            CraftTweakerAPI.logError("[TransferRunes] 无效的物品");
            return;
        }
        
        TransferRuneManager.setXpCost(stack, cost);
    }
    
    /**
     * 设置最大词条数限制
     */
    @ZenMethod
    public static void setMaxAffixLimit(IItemStack item, int maxAffixes) {
        ItemStack stack = toItemStack(item);
        if (stack.isEmpty()) {
            CraftTweakerAPI.logError("[TransferRunes] 无效的物品");
            return;
        }
        
        TransferRuneManager.setMaxAffixLimit(stack, maxAffixes);
    }
    
    /**
     * 清空所有符文配置
     */
    @ZenMethod
    public static void clear() {
        TransferRuneManager.clearRunes();
        CraftTweakerAPI.logInfo("[TransferRunes] 清空所有符文配置");
    }
    
    /**
     * 预设配置
     */
    @ZenMethod
    public static void loadPresets() {
        // 基础级
        ItemStack enderPearl = new ItemStack(net.minecraft.init.Items.ENDER_PEARL);
        ItemStack blazePowder = new ItemStack(net.minecraft.init.Items.BLAZE_POWDER);
        
        TransferRuneManager.addRune(enderPearl, 1.0f, 3);
        TransferRuneManager.addRune(blazePowder, 1.0f, 3);
        
        // 进阶级
        ItemStack enderEye = new ItemStack(net.minecraft.init.Items.ENDER_EYE);
        ItemStack ghastTear = new ItemStack(net.minecraft.init.Items.GHAST_TEAR);
        
        TransferRuneManager.addRune(enderEye, 0.95f, 5);
        TransferRuneManager.addRune(ghastTear, 0.95f, 5);
        
        // 大师级
        ItemStack netherStar = new ItemStack(net.minecraft.init.Items.NETHER_STAR);
        ItemStack dragonBreath = new ItemStack(net.minecraft.init.Items.DRAGON_BREATH);
        
        TransferRuneManager.addRune(netherStar, 0.9f, 10);
        TransferRuneManager.addRune(dragonBreath, 0.9f, 10);
        
        CraftTweakerAPI.logInfo("[TransferRunes] 加载预设符文配置");
    }
    
    /**
     * 设置基础经验消耗
     */
    @ZenMethod
    public static void setBaseXpCost(int cost) {
        TransferRuneManager.setBaseXpCost(cost);
        CraftTweakerAPI.logInfo("[TransferRunes] 设置基础经验消耗: " + cost);
    }
    
    /**
     * 设置失败是否销毁
     */
    @ZenMethod
    public static void setDestroyOnFail(boolean destroy) {
        TransferRuneManager.setDestroyOnFail(destroy);
        CraftTweakerAPI.logInfo("[TransferRunes] 失败销毁: " + destroy);
    }
    
    /**
     * 允许任何物品作为符文（测试用）
     */
    @ZenMethod
    public static void setAllowAnyItem(boolean allow) {
        TransferRuneManager.setAllowAnyItem(allow);
        CraftTweakerAPI.logInfo("[TransferRunes] 允许任何物品: " + allow);
    }
    
    /**
     * 辅助方法：将IItemStack转换为ItemStack
     */
    private static ItemStack toItemStack(IItemStack istack) {
        if (istack == null) {
            return ItemStack.EMPTY;
        }
        
        Object internal = istack.getInternal();
        if (internal instanceof ItemStack) {
            return ((ItemStack) internal).copy();
        }
        
        // 如果不是ItemStack，尝试其他方式构造
        try {
            // 尝试从IItemStack获取物品信息
            net.minecraft.item.Item item = (net.minecraft.item.Item) istack.getDefinition().getInternal();
            int meta = istack.getMetadata();
            int amount = istack.getAmount();
            return new ItemStack(item, amount, meta);
        } catch (Exception e) {
            CraftTweakerAPI.logError("[TransferRunes] 无法转换IItemStack: " + e.getMessage());
            return ItemStack.EMPTY;
        }
    }
}