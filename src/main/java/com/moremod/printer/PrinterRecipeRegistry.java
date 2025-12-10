package com.moremod.printer;

import java.util.*;

/**
 * 打印机配方注册表
 * 所有配方完全由CraftTweaker脚本控制
 *
 * 使用示例 (CraftTweaker脚本):
 *
 * // 添加配方
 * mods.moremod.Printer.addRecipe("my_recipe", <minecraft:diamond>, 100000, 200,
 *     [<minecraft:iron_ingot> * 4, <minecraft:gold_ingot> * 2]);
 *
 * // 添加带显示名称和稀有度的配方
 * mods.moremod.Printer.addRecipeAdvanced("my_recipe", "我的配方", "epic",
 *     <minecraft:diamond>, 100000, 200,
 *     [<minecraft:iron_ingot> * 4]);
 *
 * // 创建模板物品用于合成配方
 * val template = mods.moremod.Printer.createTemplate("my_recipe");
 */
public class PrinterRecipeRegistry {

    private static final Map<String, PrinterRecipe> RECIPES = new LinkedHashMap<>();

    /**
     * 注册一个配方
     */
    public static void registerRecipe(PrinterRecipe recipe) {
        RECIPES.put(recipe.getTemplateId(), recipe);
        System.out.println("[Printer] 注册配方: " + recipe.getTemplateId() +
            (recipe.getDisplayName().isEmpty() ? "" : " (" + recipe.getDisplayName() + ")") +
            " -> " + recipe.getOutput().getDisplayName());
    }

    /**
     * 移除一个配方（用于CraftTweaker）
     */
    public static void removeRecipe(String templateId) {
        if (RECIPES.remove(templateId) != null) {
            System.out.println("[Printer] 移除配方: " + templateId);
        }
    }

    /**
     * 根据模版ID获取配方
     */
    public static PrinterRecipe getRecipe(String templateId) {
        return RECIPES.get(templateId);
    }

    /**
     * 获取所有配方
     */
    public static Collection<PrinterRecipe> getAllRecipes() {
        return Collections.unmodifiableCollection(RECIPES.values());
    }

    /**
     * 获取配方数量
     */
    public static int getRecipeCount() {
        return RECIPES.size();
    }

    /**
     * 检查模版ID是否有对应配方
     */
    public static boolean hasRecipe(String templateId) {
        return RECIPES.containsKey(templateId);
    }

    /**
     * 清除所有配方（用于重新加载）
     */
    public static void clearAllRecipes() {
        RECIPES.clear();
        System.out.println("[Printer] 已清除所有配方");
    }
}
