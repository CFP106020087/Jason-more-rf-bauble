package com.moremod.compat.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * CraftTweaker API - 元素伤害系统
 *
 * 推荐用法：
 * 1. 使用 registerElement(id, name) 注册元素类型
 * 2. 使用 GemAffixes 配置宝石词条数值
 * 
 * 这样可以避免两个系统混淆
 */
@ZenRegister
@ZenClass("mods.moremod.ElementalDamage")
public class CTElementalDamageCustom {

    // ==========================================
    // ⭐ 推荐API：只注册元素类型（不涉及数值）
    // ==========================================
    
    /**
     * 注册元素类型（极简版 - 只需要ID和名称）
     * 
     * ⚠️ 重要：这个方法只注册元素类型，不涉及任何数值效果
     * 所有数值效果（转换率、增伤等）由宝石词条系统控制
     * 
     * @param id 元素ID（例如 "fire", "ice", "lightning"）
     * @param displayName 显示名称（例如 "§c火焰", "§b寒冰"）
     * 
     * 使用示例：
     * ```zenscript
     * // 第一步：注册元素类型（只是声明"有这个元素"）
     * ElementalDamage.registerElement("fire", "§c火焰");
     * ElementalDamage.registerElement("ice", "§b寒冰");
     * ElementalDamage.registerElement("lightning", "§e雷电");
     * 
     * // 第二步：配置宝石词条（控制具体数值）
     * GemAffixes.addConversion("火焰转换", "fire", 5, 35);      // 5-35%转换
     * GemAffixes.addDamageBoost("火焰增伤", "fire", 20, 120);  // 20-120%增伤
     * GemAffixes.addFlatDamage("火焰附加", "fire", 3.0, 15.0); // 3-15附加伤害
     * ```
     * 
     * 这样做的好处：
     * 1. 元素类型注册和数值分离，清晰明了
     * 2. 所有数值由宝石词条统一管理，不会混乱
     * 3. 避免两个系统相互影响
     */
    @ZenMethod
    public static void registerElement(String id, String displayName) {
        try {
            CustomElementType elementType = new CustomElementType(id);
            elementType.setDisplayName(displayName);
            elementType.setDamageType(id);  // 使用ID作为伤害类型
            // 不设置转换率和伤害倍率，由宝石词条系统控制
            
            ElementTypeRegistry.registerElementType(elementType);
            CraftTweakerAPI.logInfo("[ElementalDamage] ✅ 已注册元素类型: " + id + " (" + displayName + ")");
        } catch (Exception e) {
            CraftTweakerAPI.logError("[ElementalDamage] 注册元素失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================
    // 高级API：完整配置（不推荐，会与宝石系统混淆）
    // ==========================================

    /**
     * 注册元素类型（完整版，支持自定义伤害类型）
     *
     * ⚠️ 不推荐使用！推荐使用 registerElement(id, name) + GemAffixes
     * 
     * @param id 元素ID
     * @param displayName 显示名称
     * @param damageType 伤害类型（fire, ice, lightning, poison, holy, shadow等）
     * @param conversionRate 转换率/每个宝石
     * @param maxConversion 最大转换率
     * @param damageMultiplier 伤害倍率
     */
    @ZenMethod
    public static void registerElementWithType(String id, String displayName,
                                               String damageType,
                                               float conversionRate, float maxConversion,
                                               float damageMultiplier) {
        try {
            CustomElementType elementType = new CustomElementType(id);
            elementType.setDisplayName(displayName);
            elementType.setDamageType(damageType);
            elementType.setConversionRatePerGem(conversionRate);
            elementType.setMaxConversionRate(maxConversion);
            elementType.setDamageMultiplier(damageMultiplier);

            ElementTypeRegistry.registerElementType(elementType);
            CraftTweakerAPI.logInfo("[ElementalDamage] ✅ 已注册元素: " + id +
                    " (类型: " + damageType +
                    ", 转换: " + (conversionRate * 100) + "%/个" +
                    ", 增伤: ×" + damageMultiplier + "/个)");
        } catch (Exception e) {
            CraftTweakerAPI.logError("[ElementalDamage] 注册元素失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 简化版：注册元素类型（自动设置最大转换率为1.0）
     *
     * @param id 元素ID
     * @param displayName 显示名称
     * @param damageType 伤害类型
     * @param conversionRate 转换率
     * @param damageMultiplier 伤害倍率
     */
    @ZenMethod
    public static void registerCustomElement(String id, String displayName,
                                             String damageType,
                                             float conversionRate,
                                             float damageMultiplier) {
        registerElementWithType(id, displayName, damageType, conversionRate, 1.0f, damageMultiplier);
    }

    // ==========================================
    // 材料映射（用于材料镶嵌系统）
    // ==========================================

    /**
     * 将材料映射到元素类型
     *
     * @param materialId 材料ID（例如 "minecraft:blaze_rod"）
     * @param elementId 元素ID（例如 "fire"）
     */
    @ZenMethod
    public static void registerMaterial(String materialId, String elementId) {
        try {
            CustomElementType elementType = ElementTypeRegistry.getElementType(elementId);
            if (elementType == null) {
                CraftTweakerAPI.logError("[ElementalDamage] 元素类型未注册: " + elementId);
                return;
            }

            ElementTypeRegistry.registerMaterial(materialId, elementType);
            CraftTweakerAPI.logInfo("[ElementalDamage] ✅ 已映射材料: " + materialId + " → " + elementId);
        } catch (Exception e) {
            CraftTweakerAPI.logError("[ElementalDamage] 映射材料失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================
    // 快速注册方法（各种元素类型）
    // ==========================================

    /**
     * 快速注册：火焰元素
     */
    @ZenMethod
    public static void registerFire(String id, String displayName) {
        registerElement(id, displayName);
    }

    /**
     * 快速注册：寒冰元素
     */
    @ZenMethod
    public static void registerIce(String id, String displayName) {
        registerElement(id, displayName);
    }

    /**
     * 快速注册：雷电元素
     */
    @ZenMethod
    public static void registerLightning(String id, String displayName) {
        registerElement(id, displayName);
    }

    /**
     * 快速注册：剧毒元素
     */
    @ZenMethod
    public static void registerPoison(String id, String displayName) {
        registerElement(id, displayName);
    }

    /**
     * 快速注册：神圣元素
     */
    @ZenMethod
    public static void registerHoly(String id, String displayName) {
        registerElement(id, displayName);
    }

    /**
     * 快速注册：暗影元素
     */
    @ZenMethod
    public static void registerShadow(String id, String displayName) {
        registerElement(id, displayName);
    }

    // ==========================================
    // 工具方法
    // ==========================================

    /**
     * 检查元素类型是否已注册
     */
    @ZenMethod
    public static boolean isRegistered(String id) {
        return ElementTypeRegistry.getElementType(id) != null;
    }

    /**
     * 获取已注册的元素类型数量
     */
    @ZenMethod
    public static int getRegisteredCount() {
        return ElementTypeRegistry.getAllElementTypes().size();
    }

    /**
     * 输出所有已注册的元素类型（调试用）
     */
    @ZenMethod
    public static void printAllElements() {
        CraftTweakerAPI.logInfo("========== 已注册的元素类型 ==========");
        for (CustomElementType type : ElementTypeRegistry.getAllElementTypes()) {
            CraftTweakerAPI.logInfo("  • " + type.getId() + " - " + type.getDisplayName());
        }
        CraftTweakerAPI.logInfo("  总计: " + getRegisteredCount() + " 种元素");
        CraftTweakerAPI.logInfo("===================================");
    }
}