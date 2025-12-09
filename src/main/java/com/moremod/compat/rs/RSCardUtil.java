package com.moremod.compat.rs;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * RS 卡片检测工具类
 * 使用延迟初始化避免在 ASM 阶段加载 RS 类
 * 
 * ⭐ 注意：ASM 调用的方法使用 Object 参数，避免类型解析问题
 */
public class RSCardUtil {
    
    private static Item infinityCard = null;
    private static Item dimensionCard = null;
    private static boolean initialized = false;
    
    private static void init() {
        if (initialized) return;
        initialized = true;
        
        try {
            infinityCard = Item.getByNameOrId("moremod:infinity_card");
            dimensionCard = Item.getByNameOrId("moremod:dimension_card");
            
            if (infinityCard != null) {
                System.out.println("[RSCardUtil] Found infinity_card");
            }
            if (dimensionCard != null) {
                System.out.println("[RSCardUtil] Found dimension_card");
            }
        } catch (Exception e) {
            System.err.println("[RSCardUtil] Failed to init: " + e.getMessage());
        }
    }
    
    /**
     * 检查升级槽中是否有无限卡
     * ⭐ ASM 调用入口 - 使用 Object 避免类型解析问题
     * @param upgrades 升级物品处理器 (Object，实际是 IItemHandler)
     */
    public static boolean isInfinityCard(Object upgrades) {
        init();
        if (infinityCard == null || upgrades == null) return false;
        if (upgrades instanceof IItemHandler) {
            return containsItem((IItemHandler) upgrades, infinityCard);
        }
        return false;
    }
    
    /**
     * 检查升级槽中是否有维度卡
     * ⭐ ASM 调用入口 - 使用 Object 避免类型解析问题
     */
    public static boolean isDimensionCard(Object upgrades) {
        init();
        if (dimensionCard == null || upgrades == null) return false;
        if (upgrades instanceof IItemHandler) {
            return containsItem((IItemHandler) upgrades, dimensionCard);
        }
        return false;
    }
    
    /**
     * 检查是否同时有两张卡
     * ⭐ ASM 调用入口 - 使用 Object 避免类型解析问题
     */
    public static boolean isBothCards(Object upgrades) {
        return isInfinityCard(upgrades) && isDimensionCard(upgrades);
    }
    
    /**
     * 检查是否是我们的卡片（用于升级槽验证）
     */
    public static boolean isOurCard(ItemStack stack) {
        init();
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == infinityCard || item == dimensionCard;
    }
    
    private static boolean containsItem(IItemHandler handler, Item target) {
        try {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() == target) {
                    return true;
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        return false;
    }
}
