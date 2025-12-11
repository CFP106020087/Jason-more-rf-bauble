package com.moremod.integration.jei.printer;

import com.moremod.printer.PrinterRecipe;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 打印机配方JEI包装器
 */
public class PrinterWrapper implements IRecipeWrapper {

    private final PrinterRecipe recipe;

    public PrinterWrapper(PrinterRecipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        // 输入材料
        List<List<ItemStack>> inputs = new ArrayList<>();
        for (ItemStack material : recipe.getMaterials()) {
            inputs.add(Collections.singletonList(material));
        }
        ingredients.setInputLists(ItemStack.class, inputs);

        // 输出物品
        ingredients.setOutput(ItemStack.class, recipe.getOutput());
    }

    public PrinterRecipe getRecipe() {
        return recipe;
    }

    public String getTemplateId() {
        return recipe.getTemplateId();
    }

    public int getEnergyCost() {
        return recipe.getEnergyCost();
    }

    public int getProcessingTime() {
        return recipe.getProcessingTime();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        // 绘制能量消耗
        String energyText = TextFormatting.RED + "⚡ " + formatEnergy(recipe.getEnergyCost()) + " RF";
        minecraft.fontRenderer.drawString(energyText, 5, recipeHeight - 22, 0xFFFFFF);

        // 绘制处理时间
        String timeText = TextFormatting.AQUA + "⏱ " + (recipe.getProcessingTime() / 20.0f) + "s";
        minecraft.fontRenderer.drawString(timeText, 5, recipeHeight - 11, 0xFFFFFF);

        // 绘制配方名称（如果有）
        if (!recipe.getDisplayName().isEmpty()) {
            String nameText = TextFormatting.GOLD + recipe.getDisplayName();
            minecraft.fontRenderer.drawString(nameText, 5, 0, 0xFFFFFF);
        }
    }

    private String formatEnergy(int energy) {
        if (energy >= 1000000) {
            return String.format("%.1fM", energy / 1000000.0);
        } else if (energy >= 1000) {
            return String.format("%.1fk", energy / 1000.0);
        }
        return String.valueOf(energy);
    }
}
