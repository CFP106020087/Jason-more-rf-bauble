package com.moremod.printer.compat;

import com.moremod.init.ModItems;
import com.moremod.printer.ItemCustomPrintTemplate;
import com.moremod.printer.ItemPrintTemplate;
import com.moremod.printer.PrinterRecipe;
import com.moremod.printer.PrinterRecipeRegistry;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * CraftTweaker 打印机配方接口
 *
 * 使用方法:
 *
 * // ========== 基础配方添加 ==========
 * // 添加基础配方
 * mods.moremod.Printer.addRecipe("my_recipe", <minecraft:diamond>, 100000, 200,
 *     [<minecraft:iron_ingot> * 4, <minecraft:gold_ingot> * 2]);
 *
 * // 添加高级配方（带显示名称和稀有度）
 * mods.moremod.Printer.addRecipeAdvanced("my_recipe", "闪耀钻石模板", "epic",
 *     <minecraft:diamond>, 100000, 200,
 *     [<minecraft:iron_ingot> * 4]);
 *
 * // ========== 模板物品创建 ==========
 * // 获取空白模版（用于合成配方输入）
 * val blankTemplate = mods.moremod.Printer.getBlankTemplate();
 *
 * // 创建带有指定模板ID的模板物品（用于合成配方输出）
 * val definedTemplate = mods.moremod.Printer.createTemplate("my_recipe");
 *
 * // 使用空白模版+材料合成定义好的模版
 * recipes.addShaped("my_template", definedTemplate,
 *     [[<minecraft:paper>, <minecraft:diamond>, <minecraft:paper>],
 *      [<minecraft:redstone>, blankTemplate, <minecraft:redstone>],
 *      [<minecraft:paper>, <minecraft:gold_ingot>, <minecraft:paper>]]);
 *
 * // ========== 配方管理 ==========
 * // 移除配方
 * mods.moremod.Printer.removeRecipe("my_recipe");
 *
 * // 移除所有配方
 * mods.moremod.Printer.removeAllRecipes();
 *
 * // 列出所有配方ID
 * val recipeIds = mods.moremod.Printer.getAllRecipeIds();
 * for id in recipeIds { print(id); }
 */
@ZenClass("mods.moremod.Printer")
@ZenRegister
public class CTPrinterHandler {

    /**
     * 添加基础打印配方
     *
     * @param templateId 模版ID
     * @param output 输出物品
     * @param energyCost 能量消耗 (RF)
     * @param processingTime 处理时间 (ticks)
     * @param materials 所需材料
     */
    @ZenMethod
    public static void addRecipe(String templateId, IItemStack output, int energyCost, int processingTime, IIngredient[] materials) {
        CraftTweakerAPI.apply(new AddRecipeAction(templateId, "", "rare", output, energyCost, processingTime, materials));
    }

    /**
     * 添加高级打印配方（带显示名称和稀有度）
     *
     * @param templateId 模版ID
     * @param displayName 显示名称
     * @param rarity 稀有度 ("common", "uncommon", "rare", "epic")
     * @param output 输出物品
     * @param energyCost 能量消耗 (RF)
     * @param processingTime 处理时间 (ticks)
     * @param materials 所需材料
     */
    @ZenMethod
    public static void addRecipeAdvanced(String templateId, String displayName, String rarity,
                                         IItemStack output, int energyCost, int processingTime, IIngredient[] materials) {
        CraftTweakerAPI.apply(new AddRecipeAction(templateId, displayName, rarity, output, energyCost, processingTime, materials));
    }

    /**
     * 创建带有指定模板ID的模板物品
     * 用于在CraftTweaker中定义模板物品的合成配方
     *
     * @param templateId 模版ID
     * @return 带有指定模板ID的模板物品
     */
    @ZenMethod
    public static IItemStack createTemplate(String templateId) {
        ItemStack stack = new ItemStack(ModItems.PRINT_TEMPLATE);
        ItemPrintTemplate.setTemplateId(stack, templateId);
        return CraftTweakerMC.getIItemStack(stack);
    }

    /**
     * 移除打印配方
     *
     * @param templateId 要移除的模版ID
     */
    @ZenMethod
    public static void removeRecipe(String templateId) {
        CraftTweakerAPI.apply(new RemoveRecipeAction(templateId));
    }

    /**
     * 移除所有打印配方
     */
    @ZenMethod
    public static void removeAllRecipes() {
        CraftTweakerAPI.apply(new RemoveAllRecipesAction());
    }

    /**
     * 获取已注册的配方数量
     */
    @ZenMethod
    public static int getRecipeCount() {
        return PrinterRecipeRegistry.getRecipeCount();
    }

    /**
     * 检查配方是否存在
     */
    @ZenMethod
    public static boolean hasRecipe(String templateId) {
        return PrinterRecipeRegistry.hasRecipe(templateId);
    }

    /**
     * 获取空白模版物品
     * 用于在CraftTweaker中作为合成配方的输入材料
     *
     * @return 空白模版物品
     */
    @ZenMethod
    public static IItemStack getBlankTemplate() {
        ItemStack stack = new ItemStack(ModItems.BLANK_PRINT_TEMPLATE);
        return CraftTweakerMC.getIItemStack(stack);
    }

    /**
     * 获取多个空白模版
     *
     * @param count 数量
     * @return 空白模版物品
     */
    @ZenMethod
    public static IItemStack getBlankTemplate(int count) {
        ItemStack stack = new ItemStack(ModItems.BLANK_PRINT_TEMPLATE, count);
        return CraftTweakerMC.getIItemStack(stack);
    }

    /**
     * 获取所有已注册的配方ID列表
     *
     * @return 配方ID数组
     */
    @ZenMethod
    public static String[] getAllRecipeIds() {
        java.util.Collection<PrinterRecipe> recipes = PrinterRecipeRegistry.getAllRecipes();
        String[] ids = new String[recipes.size()];
        int i = 0;
        for (PrinterRecipe recipe : recipes) {
            ids[i++] = recipe.getTemplateId();
        }
        return ids;
    }

    /**
     * 获取配方的能量消耗
     *
     * @param templateId 模版ID
     * @return 能量消耗，如果配方不存在返回-1
     */
    @ZenMethod
    public static int getEnergyCost(String templateId) {
        PrinterRecipe recipe = PrinterRecipeRegistry.getRecipe(templateId);
        return recipe != null ? recipe.getEnergyCost() : -1;
    }

    /**
     * 获取配方的处理时间
     *
     * @param templateId 模版ID
     * @return 处理时间(ticks)，如果配方不存在返回-1
     */
    @ZenMethod
    public static int getProcessingTime(String templateId) {
        PrinterRecipe recipe = PrinterRecipeRegistry.getRecipe(templateId);
        return recipe != null ? recipe.getProcessingTime() : -1;
    }

    /**
     * 获取配方的输出物品
     *
     * @param templateId 模版ID
     * @return 输出物品，如果配方不存在返回null
     */
    @ZenMethod
    public static IItemStack getRecipeOutput(String templateId) {
        PrinterRecipe recipe = PrinterRecipeRegistry.getRecipe(templateId);
        if (recipe != null) {
            return CraftTweakerMC.getIItemStack(recipe.getOutput());
        }
        return null;
    }

    // ==================== 自定义模版相关方法 ====================

    /**
     * 创建自定义打印模版（配方数据存储在NBT中）
     *
     * 此模版无需预先注册配方，所有配方数据直接存储在物品NBT中
     * 适用于战利品、一次性物品或动态生成的模版
     *
     * @param displayName 显示名称
     * @param output 输出物品
     * @param energyCost 能量消耗 (RF)
     * @param processingTime 处理时间 (ticks)
     * @param materials 所需材料
     * @return 自定义模版物品
     */
    @ZenMethod
    public static IItemStack createCustomTemplate(String displayName, IItemStack output,
                                                   int energyCost, int processingTime, IIngredient[] materials) {
        ItemStack stack = new ItemStack(ModItems.CUSTOM_PRINT_TEMPLATE);
        ItemCustomPrintTemplate.setDisplayName(stack, displayName);
        ItemCustomPrintTemplate.setOutput(stack, CraftTweakerMC.getItemStack(output));
        ItemCustomPrintTemplate.setEnergyCost(stack, energyCost);
        ItemCustomPrintTemplate.setProcessingTime(stack, processingTime);

        java.util.List<ItemStack> materialList = new java.util.ArrayList<>();
        for (IIngredient material : materials) {
            if (material instanceof IItemStack) {
                materialList.add(CraftTweakerMC.getItemStack((IItemStack) material));
            }
        }
        ItemCustomPrintTemplate.setMaterials(stack, materialList);

        return CraftTweakerMC.getIItemStack(stack);
    }

    /**
     * 创建自定义打印模版（带稀有度）
     *
     * @param displayName 显示名称
     * @param rarity 稀有度 ("common", "uncommon", "rare", "epic")
     * @param output 输出物品
     * @param energyCost 能量消耗 (RF)
     * @param processingTime 处理时间 (ticks)
     * @param materials 所需材料
     * @return 自定义模版物品
     */
    @ZenMethod
    public static IItemStack createCustomTemplateAdvanced(String displayName, String rarity,
                                                           IItemStack output, int energyCost,
                                                           int processingTime, IIngredient[] materials) {
        ItemStack stack = new ItemStack(ModItems.CUSTOM_PRINT_TEMPLATE);
        ItemCustomPrintTemplate.setDisplayName(stack, displayName);
        ItemCustomPrintTemplate.setRarityString(stack, rarity);
        ItemCustomPrintTemplate.setOutput(stack, CraftTweakerMC.getItemStack(output));
        ItemCustomPrintTemplate.setEnergyCost(stack, energyCost);
        ItemCustomPrintTemplate.setProcessingTime(stack, processingTime);

        java.util.List<ItemStack> materialList = new java.util.ArrayList<>();
        for (IIngredient material : materials) {
            if (material instanceof IItemStack) {
                materialList.add(CraftTweakerMC.getItemStack((IItemStack) material));
            }
        }
        ItemCustomPrintTemplate.setMaterials(stack, materialList);

        return CraftTweakerMC.getIItemStack(stack);
    }

    /**
     * 获取空白自定义模版
     *
     * @return 空白自定义模版物品
     */
    @ZenMethod
    public static IItemStack getCustomTemplate() {
        return CraftTweakerMC.getIItemStack(new ItemStack(ModItems.CUSTOM_PRINT_TEMPLATE));
    }

    /**
     * 检查物品是否是有效的自定义模版
     *
     * @param stack 物品
     * @return 是否有效
     */
    @ZenMethod
    public static boolean isValidCustomTemplate(IItemStack stack) {
        return ItemCustomPrintTemplate.isValidTemplate(CraftTweakerMC.getItemStack(stack));
    }

    /**
     * 添加配方动作
     */
    private static class AddRecipeAction implements IAction {
        private final String templateId;
        private final String displayName;
        private final String rarity;
        private final IItemStack output;
        private final int energyCost;
        private final int processingTime;
        private final IIngredient[] materials;

        public AddRecipeAction(String templateId, String displayName, String rarity,
                               IItemStack output, int energyCost, int processingTime, IIngredient[] materials) {
            this.templateId = templateId;
            this.displayName = displayName;
            this.rarity = rarity;
            this.output = output;
            this.energyCost = energyCost;
            this.processingTime = processingTime;
            this.materials = materials;
        }

        @Override
        public void apply() {
            ItemStack outputStack = CraftTweakerMC.getItemStack(output);

            PrinterRecipe.Builder builder = new PrinterRecipe.Builder()
                .setTemplateId(templateId)
                .setDisplayName(displayName)
                .setRarity(rarity)
                .setOutput(outputStack)
                .setEnergyCost(energyCost)
                .setProcessingTime(processingTime);

            for (IIngredient material : materials) {
                if (material instanceof IItemStack) {
                    ItemStack materialStack = CraftTweakerMC.getItemStack((IItemStack) material);
                    builder.addMaterial(materialStack);
                }
            }

            PrinterRecipeRegistry.registerRecipe(builder.build());
        }

        @Override
        public String describe() {
            String name = displayName.isEmpty() ? templateId : displayName;
            return "添加打印机配方: " + name + " -> " + output.getDisplayName();
        }
    }

    /**
     * 移除配方动作
     */
    private static class RemoveRecipeAction implements IAction {
        private final String templateId;

        public RemoveRecipeAction(String templateId) {
            this.templateId = templateId;
        }

        @Override
        public void apply() {
            PrinterRecipeRegistry.removeRecipe(templateId);
        }

        @Override
        public String describe() {
            return "移除打印机配方: " + templateId;
        }
    }

    /**
     * 移除所有配方动作
     */
    private static class RemoveAllRecipesAction implements IAction {
        @Override
        public void apply() {
            PrinterRecipeRegistry.clearAllRecipes();
        }

        @Override
        public String describe() {
            return "移除所有打印机配方";
        }
    }
}
