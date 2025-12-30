package com.moremod.integration.jei.generator;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import java.util.Collections;

/**
 * 石油发电机燃料配方包装器
 */
public class OilGeneratorWrapper implements IRecipeWrapper {

    private final GeneratorFuel fuel;

    public OilGeneratorWrapper(GeneratorFuel fuel) {
        this.fuel = fuel;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputs(ItemStack.class, Collections.singletonList(fuel.getFuel()));
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        int infoX = 50;
        int infoY = 50;

        // 显示产出信息
        String rfText = I18n.format("jei.moremod.generator.total_rf", formatEnergy(fuel.getTotalRF()));
        minecraft.fontRenderer.drawString(rfText, infoX, infoY, 0x22AA22);

        // 显示每tick产出
        String perTickText = I18n.format("jei.moremod.generator.rf_per_tick", fuel.getRFPerTick());
        minecraft.fontRenderer.drawString(perTickText, infoX, infoY + 12, 0x2266CC);

        // 显示燃烧时间
        String timeText = I18n.format("jei.moremod.generator.burn_time", String.format("%.1f", fuel.getBurnTimeSeconds()));
        minecraft.fontRenderer.drawString(timeText, infoX, infoY + 24, 0x666666);
    }

    private String formatEnergy(int energy) {
        if (energy >= 1000000) {
            return String.format("%.2fM", energy / 1000000.0);
        } else if (energy >= 1000) {
            return String.format("%.1fk", energy / 1000.0);
        }
        return String.valueOf(energy);
    }

    public GeneratorFuel getFuel() {
        return fuel;
    }
}
