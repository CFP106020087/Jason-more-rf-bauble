package com.moremod.integration.jei;

import com.moremod.moremod;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.*;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import java.util.List;

import static com.moremod.integration.jei.MoreModJEIPlugin.RITUAL_INFUSION_UID;

public class RitualInfusionCategory implements IRecipeCategory<RitualInfusionWrapper> {

    private final IDrawable background;
    private final IDrawable icon;
    private final String title;

    // GUI布局
    private static final int GUI_WIDTH = 166;
    private static final int GUI_HEIGHT = 125;

    // 核心物品位置（中心）
    private static final int CORE_X = 74;
    private static final int CORE_Y = 50;

    // 输出位置
    private static final int OUTPUT_X = 130;
    private static final int OUTPUT_Y = 50;

    // 基座半径
    private static final int PEDESTAL_RADIUS = 36;

    public RitualInfusionCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(GUI_WIDTH, GUI_HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(new ItemStack(moremod.RITUAL_CORE_BLOCK));
        this.title = I18n.format("能量注魔");
    }

    @Override
    public String getUid() {
        return MoreModJEIPlugin.RITUAL_INFUSION_UID;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getModName() {
        return "MoreMod";
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, RitualInfusionWrapper recipeWrapper, IIngredients ingredients) {
        IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();

        int slotIndex = 0;

        // 核心物品槽位
        guiItemStacks.init(slotIndex++, true, CORE_X, CORE_Y);

        // 基座物品 - 环形排列
        List<List<ItemStack>> inputs = ingredients.getInputs(ItemStack.class);
        int pedestalCount = inputs.size() - 1; // 减去核心

        if (pedestalCount > 0) {
            double angleStep = 360.0 / pedestalCount;
            for (int i = 0; i < pedestalCount; i++) {
                double angle = Math.toRadians(angleStep * i - 90);
                int x = (int)(CORE_X + Math.cos(angle) * PEDESTAL_RADIUS);
                int y = (int)(CORE_Y + Math.sin(angle) * PEDESTAL_RADIUS);
                guiItemStacks.init(slotIndex++, true, x, y);
            }
        }

        // 输出槽位
        guiItemStacks.init(slotIndex, false, OUTPUT_X, OUTPUT_Y);

        // 设置物品
        guiItemStacks.set(ingredients);
    }

    @Override
    public void drawExtras(Minecraft minecraft) {
        // 绘制标题
        String ritualText = "能量注魔";
        minecraft.fontRenderer.drawString(ritualText,
                (GUI_WIDTH - minecraft.fontRenderer.getStringWidth(ritualText)) / 2, 5, 0x404040);

        // 绘制箭头
        minecraft.fontRenderer.drawString("→", CORE_X + 30, CORE_Y + 4, 0x404040);

        // 绘制装饰圆圈（可选）
        drawRitualCircle(minecraft);
    }

    private void drawRitualCircle(Minecraft minecraft) {
        // 简单的装饰性圆圈点
        int segments = 16;
        for (int i = 0; i < segments; i++) {
            double angle = (2 * Math.PI * i) / segments;
            int x = (int)(CORE_X + 8 + Math.cos(angle) * PEDESTAL_RADIUS);
            int y = (int)(CORE_Y + 8 + Math.sin(angle) * PEDESTAL_RADIUS);
            minecraft.fontRenderer.drawString("·", x, y, 0x805080);
        }
    }
}