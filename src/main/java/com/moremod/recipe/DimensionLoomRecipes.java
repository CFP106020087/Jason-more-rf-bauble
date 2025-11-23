package com.moremod.recipe;

import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class DimensionLoomRecipes {

    private static final List<DimensionLoomRecipe> recipes = new ArrayList<>();

    /**
     * 添加配方
     */
    public static void addRecipe(ItemStack output, ItemStack... inputs) {
        if (inputs.length != 9) {
            throw new IllegalArgumentException("Dimension Loom recipe must have exactly 9 inputs");
        }
        recipes.add(new DimensionLoomRecipe(output, inputs));
    }

    /**
     * 获取配方结果
     */
    public static ItemStack getRecipeResult(ItemStack[] inputs) {
        if (inputs.length != 9) {
            return ItemStack.EMPTY;
        }

        for (DimensionLoomRecipe recipe : recipes) {
            if (recipe.matches(inputs)) {
                return recipe.getOutput().copy();
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * 获取配方数量
     */
    public static int getRecipeCount() {
        return recipes.size();
    }

    /**
     * 清空所有配方
     */
    public static void clearRecipes() {
        recipes.clear();
    }

    /**
     * 获取所有配方（用于JEI等集成）
     */
    public static List<DimensionLoomRecipe> getAllRecipes() {
        return new ArrayList<>(recipes);
    }

    /**
     * TileEntity使用的方法
     */
    public static ItemStack getResultForTileEntity(ItemStack[] inputs) {
        return getRecipeResult(inputs);
    }

    /**
     * 配方类
     */
    public static class DimensionLoomRecipe {
        private final ItemStack output;
        private final ItemStack[] inputs;

        public DimensionLoomRecipe(ItemStack output, ItemStack[] inputs) {
            this.output = output;
            this.inputs = inputs;
        }

        public boolean matches(ItemStack[] input) {
            for (int i = 0; i < 9; i++) {
                // 两个都是空的
                if (inputs[i].isEmpty() && input[i].isEmpty()) {
                    continue;
                }
                // 只有一个是空的
                if (inputs[i].isEmpty() != input[i].isEmpty()) {
                    return false;
                }
                // 物品不匹配
                if (!ItemStack.areItemsEqual(inputs[i], input[i])) {
                    return false;
                }
                // 数量不足
                if (input[i].getCount() < inputs[i].getCount()) {
                    return false;
                }
            }
            return true;
        }

        public ItemStack getOutput() {
            return output;
        }

        public ItemStack[] getInputs() {
            return inputs;
        }
    }
}
