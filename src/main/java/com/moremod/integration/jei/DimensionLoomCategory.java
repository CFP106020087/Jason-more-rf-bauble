package com.moremod.integration.jei;

import com.moremod.init.ModBlocks;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import java.util.List;

/**
 * 维度织机配方显示类别 - 带图标版
 */
public class DimensionLoomCategory implements IRecipeCategory<DimensionLoomWrapper> {

    private final IDrawable background;
    private final IDrawable icon;
    private final String title;

    // GUI布局
    private static final int GUI_WIDTH = 160;
    private static final int GUI_HEIGHT = 100;
    private static final int INPUT_START_X = 16;
    private static final int INPUT_START_Y = 25;
    private static final int SLOT_SIZE = 18;
    private static final int OUTPUT_X = 124;
    private static final int OUTPUT_Y = 43;
    private static final int ARROW_X = 86;
    private static final int ARROW_Y = 43;

    public DimensionLoomCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(GUI_WIDTH, GUI_HEIGHT);

        // ========== 设置图标 ==========
        // 优先使用dimension_loom作为图标
        if (ModBlocks.dimensionLoom != null) {
            this.icon = guiHelper.createDrawableIngredient(new ItemStack(ModBlocks.dimensionLoom));
        }
        // 备选：使用工作台
        else {
            this.icon = guiHelper.createDrawableIngredient(new ItemStack(Blocks.CRAFTING_TABLE));
        }

        this.title = I18n.format("jei.moremod.dimension_loom");
    }

    @Override
    public String getUid() {
        return MoreModJEIPlugin.DIMENSION_LOOM_UID;
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
    public void setRecipe(IRecipeLayout recipeLayout, DimensionLoomWrapper recipeWrapper, IIngredients ingredients) {
        IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();

        List<List<ItemStack>> inputs = ingredients.getInputs(ItemStack.class);
        List<ItemStack> outputs = ingredients.getOutputs(ItemStack.class).get(0);

        // 初始化3x3输入槽位
        int slotIndex = 0;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int posX = INPUT_START_X + x * SLOT_SIZE;
                int posY = INPUT_START_Y + y * SLOT_SIZE;

                guiItemStacks.init(slotIndex, true, posX, posY);

                if (slotIndex < inputs.size()) {
                    guiItemStacks.set(slotIndex, inputs.get(slotIndex));
                }

                slotIndex++;
            }
        }

        // 输出槽位
        guiItemStacks.init(9, false, OUTPUT_X, OUTPUT_Y);
        guiItemStacks.set(9, outputs);
    }

    @Override
    public void drawExtras(Minecraft minecraft) {
        // 标题
        String titleText = "Dimension Loom";
        int titleX = (GUI_WIDTH - minecraft.fontRenderer.getStringWidth(titleText)) / 2;
        minecraft.fontRenderer.drawString(titleText, titleX, 5, 0x404040);

        // 绘制3x3网格框架
        drawCraftingGrid(minecraft);

        // 绘制合成箭头
        drawCraftingArrow(minecraft);
    }

    private void drawCraftingGrid(Minecraft mc) {
        int gridWidth = 3 * SLOT_SIZE + 2;
        int gridHeight = 3 * SLOT_SIZE + 2;

        // 外框
        drawHorizontalLine(mc, INPUT_START_X - 1, INPUT_START_Y - 1, gridWidth, 0x805080);
        drawHorizontalLine(mc, INPUT_START_X - 1, INPUT_START_Y + gridHeight - 2, gridWidth, 0x805080);
        drawVerticalLine(mc, INPUT_START_X - 1, INPUT_START_Y - 1, gridHeight, 0x805080);
        drawVerticalLine(mc, INPUT_START_X + gridWidth - 2, INPUT_START_Y - 1, gridHeight, 0x805080);
    }

    private void drawCraftingArrow(Minecraft mc) {
        // 多层箭头表示合成过程
        mc.fontRenderer.drawString(">>>", ARROW_X, ARROW_Y + 4, 0x606060);
        mc.fontRenderer.drawString("→", ARROW_X + 8, ARROW_Y + 4, 0x9966CC);
    }

    private void drawHorizontalLine(Minecraft mc, int x, int y, int width, int color) {
        Gui.drawRect(x, y, x + width, y + 1, color);
    }

    private void drawVerticalLine(Minecraft mc, int x, int y, int height, int color) {
        Gui.drawRect(x, y, x + 1, y + height, color);
    }
}