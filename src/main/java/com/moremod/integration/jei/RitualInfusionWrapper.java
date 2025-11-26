package com.moremod.integration.jei;

import com.moremod.ritual.RitualInfusionRecipe;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RitualInfusionWrapper implements IRecipeWrapper {

    private final RitualInfusionRecipe recipe;

    public RitualInfusionWrapper(RitualInfusionRecipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        List<List<ItemStack>> inputs = new ArrayList<>();

        // 添加核心物品
        inputs.add(Arrays.asList(recipe.getCore().getMatchingStacks()));

        // 添加基座物品
        for (Ingredient pedestalItem : recipe.getPedestalItems()) {
            inputs.add(Arrays.asList(pedestalItem.getMatchingStacks()));
        }

        // 设置输入
        ingredients.setInputLists(VanillaTypes.ITEM, inputs);

        // 设置输出
        ingredients.setOutput(VanillaTypes.ITEM, recipe.getOutput());
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        FontRenderer fontRenderer = minecraft.fontRenderer;

        int y = recipeHeight - 25;

        // 显示时间（转换为秒）
        String timeText = String.format("时间: %ds", recipe.getTime() / 20);
        fontRenderer.drawString(timeText, 5, y, Color.GRAY.getRGB());

        // 显示总能量需求
        int totalEnergy = recipe.getEnergyPerPedestal() * recipe.getPedestalCount();
        String energyText = String.format("能量: %d RF", totalEnergy);
        fontRenderer.drawString(energyText, 5, y + 10, Color.GRAY.getRGB());

        // 显示失败概率（如果有）
        if (recipe.getFailChance() > 0) {
            String failText = String.format("失败: %.0f%%", recipe.getFailChance() * 100);
            fontRenderer.drawString(failText, recipeWidth - 45, y, Color.RED.getRGB());
        }
    }
}