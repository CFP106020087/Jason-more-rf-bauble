package com.moremod.integration.crafttweaker;

import com.moremod.recipe.BottlingMachineRecipe;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.liquid.ILiquidStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.oredict.IOreDictEntry;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * CraftTweaker集成 - 装瓶机配方
 * 使用方法：
 * import mods.moremod.BottlingMachine;
 *
 * // 添加配方
 * BottlingMachine.addRecipe(<minecraft:potion>, <minecraft:glass_bottle>, <liquid:water> * 250);
 *
 * // 使用矿物词典
 * BottlingMachine.addRecipe(<minecraft:water_bucket>, <ore:bucketEmpty>, <liquid:water> * 1000);
 *
 * // 移除配方
 * BottlingMachine.removeRecipe(<minecraft:water_bucket>);
 * BottlingMachine.removeRecipeByFluid(<liquid:lava>);
 */
@ZenRegister
@ZenClass("mods.moremod.BottlingMachine")
public class BottlingMachineCT {

    /**
     * 添加装瓶机配方
     * @param output 输出物品
     * @param input 输入物品（空容器）
     * @param fluid 输入流体
     * @param inputAmount 输入物品数量（可选，默认1）
     */
    @ZenMethod
    public static void addRecipe(IItemStack output, IIngredient input, ILiquidStack fluid, @Optional int inputAmount) {
        if (output == null || input == null || fluid == null) {
            CraftTweakerAPI.logError("装瓶机配方不能有null参数");
            return;
        }

        int amount = inputAmount > 0 ? inputAmount : 1;

        // 延迟添加，确保在正确的时机执行
        CraftTweakerAPI.apply(new AddRecipeAction(output, input, fluid, amount));
    }

    /**
     * 简化版添加配方（输入数量默认为1）
     */
    @ZenMethod
    public static void addRecipe(IItemStack output, IIngredient input, ILiquidStack fluid) {
        addRecipe(output, input, fluid, 1);
    }

    /**
     * 根据输出物品移除配方
     * @param output 要移除的配方的输出物品
     */
    @ZenMethod
    public static void removeRecipe(IItemStack output) {
        if (output == null) {
            CraftTweakerAPI.logError("移除配方的输出物品不能为null");
            return;
        }

        CraftTweakerAPI.apply(new RemoveRecipeAction(CraftTweakerMC.getItemStack(output)));
    }

    /**
     * 根据流体移除所有相关配方
     * @param fluid 流体
     */
    @ZenMethod
    public static void removeRecipeByFluid(ILiquidStack fluid) {
        if (fluid == null) {
            CraftTweakerAPI.logError("移除配方的流体不能为null");
            return;
        }

        CraftTweakerAPI.apply(new RemoveRecipeByFluidAction(CraftTweakerMC.getLiquidStack(fluid)));
    }

    /**
     * 移除所有配方
     */
    @ZenMethod
    public static void removeAll() {
        CraftTweakerAPI.apply(new RemoveAllRecipesAction());
    }

    // ========== Action类 ==========

    /**
     * 添加配方的Action
     */
    private static class AddRecipeAction implements crafttweaker.IAction {
        private final ItemStack output;
        private final IIngredient input;
        private final FluidStack fluid;
        private final int inputAmount;

        public AddRecipeAction(IItemStack output, IIngredient input, ILiquidStack fluid, int inputAmount) {
            this.output = CraftTweakerMC.getItemStack(output);
            this.input = input;
            this.fluid = CraftTweakerMC.getLiquidStack(fluid);
            this.inputAmount = inputAmount;
        }

        @Override
        public void apply() {
            // 处理不同类型的输入
            if (input instanceof IOreDictEntry) {
                // 矿物词典
                String oreName = ((IOreDictEntry) input).getName();
                BottlingMachineRecipe.addRecipe(output, oreName, inputAmount, fluid);
                CraftTweakerAPI.logInfo("添加装瓶机配方（矿物词典）: " + output.getDisplayName() +
                        " <- " + oreName + " x" + inputAmount + " + " + fluid.getLocalizedName());
            } else if (input instanceof IItemStack) {
                // 普通物品
                ItemStack inputStack = CraftTweakerMC.getItemStack((IItemStack) input);
                BottlingMachineRecipe.addRecipe(output, inputStack, inputAmount, fluid);
                CraftTweakerAPI.logInfo("添加装瓶机配方: " + output.getDisplayName() +
                        " <- " + inputStack.getDisplayName() + " x" + inputAmount + " + " + fluid.getLocalizedName());
            } else {
                CraftTweakerAPI.logError("不支持的输入类型: " + input.getClass().getName());
            }
        }

        @Override
        public String describe() {
            return "添加装瓶机配方: " + output.getDisplayName();
        }
    }

    /**
     * 移除配方的Action
     */
    private static class RemoveRecipeAction implements crafttweaker.IAction {
        private final ItemStack output;
        private int removedCount = 0;

        public RemoveRecipeAction(ItemStack output) {
            this.output = output;
        }

        @Override
        public void apply() {
            List<BottlingMachineRecipe> removed = BottlingMachineRecipe.removeRecipes(output);
            removedCount = removed.size();
            if (removedCount > 0) {
                CraftTweakerAPI.logInfo("移除了 " + removedCount + " 个装瓶机配方（输出: " +
                        output.getDisplayName() + "）");
            } else {
                CraftTweakerAPI.logWarning("未找到要移除的配方（输出: " + output.getDisplayName() + "）");
            }
        }

        @Override
        public String describe() {
            return "移除装瓶机配方（输出: " + output.getDisplayName() + "）";
        }
    }

    /**
     * 根据流体移除配方的Action
     */
    private static class RemoveRecipeByFluidAction implements crafttweaker.IAction {
        private final FluidStack fluid;
        private int removedCount = 0;

        public RemoveRecipeByFluidAction(FluidStack fluid) {
            this.fluid = fluid;
        }

        @Override
        public void apply() {
            List<BottlingMachineRecipe> toRemove = new ArrayList<>();
            for (BottlingMachineRecipe recipe : BottlingMachineRecipe.getAllRecipes()) {
                if (recipe.fluidInput.isFluidEqual(fluid)) {
                    toRemove.add(recipe);
                }
            }

            for (BottlingMachineRecipe recipe : toRemove) {
                BottlingMachineRecipe.removeRecipes(recipe.output);
                removedCount++;
            }

            if (removedCount > 0) {
                CraftTweakerAPI.logInfo("移除了 " + removedCount + " 个使用 " +
                        fluid.getLocalizedName() + " 的装瓶机配方");
            } else {
                CraftTweakerAPI.logWarning("未找到使用 " + fluid.getLocalizedName() + " 的配方");
            }
        }

        @Override
        public String describe() {
            return "移除使用 " + fluid.getLocalizedName() + " 的装瓶机配方";
        }
    }

    /**
     * 移除所有配方的Action
     */
    private static class RemoveAllRecipesAction implements crafttweaker.IAction {
        private int originalCount = 0;

        @Override
        public void apply() {
            originalCount = BottlingMachineRecipe.getAllRecipes().size();
            BottlingMachineRecipe.clearAllRecipes();
            CraftTweakerAPI.logInfo("移除了所有 " + originalCount + " 个装瓶机配方");
        }

        @Override
        public String describe() {
            return "移除所有装瓶机配方";
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 获取所有配方（用于调试）
     */
    @ZenMethod
    public static void listAllRecipes() {
        List<BottlingMachineRecipe> recipes = BottlingMachineRecipe.getAllRecipes();
        CraftTweakerAPI.logInfo("========== 装瓶机配方列表 ==========");
        CraftTweakerAPI.logInfo("共 " + recipes.size() + " 个配方:");
        for (BottlingMachineRecipe recipe : recipes) {
            String inputDesc = recipe.input.oreDictName != null ?
                    "ore:" + recipe.input.oreDictName :
                    recipe.input.stack.getDisplayName();
            CraftTweakerAPI.logInfo("  " + recipe.output.getDisplayName() + " <- " +
                    inputDesc + " x" + recipe.input.amount + " + " +
                    recipe.fluidInput.getLocalizedName() + " x" + recipe.fluidInput.amount + "mB");
        }
        CraftTweakerAPI.logInfo("====================================");
    }
}