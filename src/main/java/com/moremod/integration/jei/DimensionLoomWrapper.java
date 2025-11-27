package com.moremod.integration.jei;

import com.moremod.recipe.DimensionLoomRecipes;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dimension Loom配方包装器
 */
public class DimensionLoomWrapper implements IRecipeWrapper {

    private final DimensionLoomRecipes.DimensionLoomRecipe recipe;

    public DimensionLoomWrapper(DimensionLoomRecipes.DimensionLoomRecipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        // 准备输入列表
        List<List<ItemStack>> inputs = new ArrayList<>();

        // 添加9个输入槽位
        ItemStack[] recipeInputs = recipe.getInputs();
        for (int i = 0; i < 9; i++) {
            if (recipeInputs[i] != null && !recipeInputs[i].isEmpty()) {
                // 有物品的槽位
                inputs.add(Collections.singletonList(recipeInputs[i].copy()));
            } else {
                // 空槽位
                inputs.add(Collections.emptyList());
            }
        }

        // 设置输入
        ingredients.setInputLists(ItemStack.class, inputs);

        // 设置输出
        ingredients.setOutput(ItemStack.class, recipe.getOutput());
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        FontRenderer font = minecraft.fontRenderer;

        // 可以在这里显示额外信息
        // 例如：配方来源、特殊要求等

        // 显示输出物品名称
        String outputName = recipe.getOutput().getDisplayName();
        int nameX = (recipeWidth - font.getStringWidth(outputName)) / 2;
        font.drawString(outputName, nameX, recipeHeight - 10, 0x555555);

        // 统计输入物品数量
        int inputCount = 0;
        for (ItemStack input : recipe.getInputs()) {
            if (input != null && !input.isEmpty()) {
                inputCount++;
            }
        }

        // 显示需要的物品数
        String countText = "Requires: " + inputCount + " items";
        font.drawString(countText, 5, recipeHeight - 20, 0x808080);
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        // 可以根据鼠标位置返回工具提示
        return null;
    }

    @Override
    public boolean handleClick(Minecraft minecraft, int mouseX, int mouseY, int mouseButton) {
        // 默认处理
        return false;
    }

    /**
     * 获取原始配方（供其他地方使用）
     */
    public DimensionLoomRecipes.DimensionLoomRecipe getRecipe() {
        return recipe;
    }
}