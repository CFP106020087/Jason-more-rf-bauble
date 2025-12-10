package com.moremod.printer.compat;

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

import java.util.ArrayList;
import java.util.List;

/**
 * CraftTweaker 打印机配方接口
 *
 * 使用方法:
 *
 * // 添加配方
 * mods.moremod.Printer.addRecipe("custom_recipe", <minecraft:diamond>, 100000, 200,
 *     [<minecraft:iron_ingot> * 4, <minecraft:gold_ingot> * 2]);
 *
 * // 移除配方
 * mods.moremod.Printer.removeRecipe("gear_iron");
 *
 * // 移除所有配方
 * mods.moremod.Printer.removeAllRecipes();
 */
@ZenClass("mods.moremod.Printer")
@ZenRegister
public class CTPrinterHandler {

    /**
     * 添加打印配方
     *
     * @param templateId 模版ID
     * @param output 输出物品
     * @param energyCost 能量消耗 (RF)
     * @param processingTime 处理时间 (ticks)
     * @param materials 所需材料
     */
    @ZenMethod
    public static void addRecipe(String templateId, IItemStack output, int energyCost, int processingTime, IIngredient[] materials) {
        CraftTweakerAPI.apply(new AddRecipeAction(templateId, output, energyCost, processingTime, materials));
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
     * 添加配方动作
     */
    private static class AddRecipeAction implements IAction {
        private final String templateId;
        private final IItemStack output;
        private final int energyCost;
        private final int processingTime;
        private final IIngredient[] materials;

        public AddRecipeAction(String templateId, IItemStack output, int energyCost, int processingTime, IIngredient[] materials) {
            this.templateId = templateId;
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
            return "添加打印机配方: " + templateId + " -> " + output.getDisplayName();
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
