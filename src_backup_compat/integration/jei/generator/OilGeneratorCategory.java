package com.moremod.integration.jei.generator;

import com.moremod.init.ModBlocks;
import com.moremod.integration.jei.MoreModJEIPlugin;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * 石油发电机 JEI 类别
 */
public class OilGeneratorCategory implements IRecipeCategory<OilGeneratorWrapper> {

    private final IDrawable background;
    private final IDrawable icon;
    private final String title;

    private static final int GUI_WIDTH = 160;
    private static final int GUI_HEIGHT = 80;

    public OilGeneratorCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(GUI_WIDTH, GUI_HEIGHT);

        if (ModBlocks.OIL_GENERATOR != null) {
            this.icon = guiHelper.createDrawableIngredient(new ItemStack(ModBlocks.OIL_GENERATOR));
        } else {
            this.icon = guiHelper.createBlankDrawable(16, 16);
        }

        this.title = I18n.format("jei.moremod.oil_generator");
    }

    @Override
    public String getUid() {
        return MoreModJEIPlugin.OIL_GENERATOR_UID;
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
    public void setRecipe(IRecipeLayout recipeLayout, OilGeneratorWrapper wrapper, IIngredients ingredients) {
        IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();

        // 燃料输入槽
        guiItemStacks.init(0, true, 20, 30);
        List<List<ItemStack>> inputs = ingredients.getInputs(ItemStack.class);
        if (!inputs.isEmpty() && !inputs.get(0).isEmpty()) {
            guiItemStacks.set(0, inputs.get(0));
        }
    }

    @Override
    public void drawExtras(Minecraft minecraft) {
        // 标题
        String titleText = I18n.format("jei.moremod.oil_generator");
        int titleX = (GUI_WIDTH - minecraft.fontRenderer.getStringWidth(titleText)) / 2;
        minecraft.fontRenderer.drawString(titleText, titleX, 5, 0x404040);

        // 绘制槽位框
        drawSlotBorder(minecraft, 20, 30);

        // 绘制箭头
        minecraft.fontRenderer.drawString(">>>", 50, 34, 0x606060);

        // 绘制火焰图标区域
        drawFlameIcon(minecraft, 90, 28);
    }

    private void drawSlotBorder(Minecraft mc, int x, int y) {
        int color = 0xFF8B8B8B;
        Gui.drawRect(x - 1, y - 1, x + 17, y, color);
        Gui.drawRect(x - 1, y + 16, x + 17, y + 17, color);
        Gui.drawRect(x - 1, y - 1, x, y + 17, color);
        Gui.drawRect(x + 16, y - 1, x + 17, y + 17, color);
    }

    private void drawFlameIcon(Minecraft mc, int x, int y) {
        // 简单火焰符号
        mc.fontRenderer.drawString("\u2668", x, y, 0xFFAA00); // ♨ 火焰符号
        mc.fontRenderer.drawString("RF", x + 12, y + 4, 0xFF6600);
    }
}
