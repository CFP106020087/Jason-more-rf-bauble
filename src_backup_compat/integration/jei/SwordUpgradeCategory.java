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
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * 材質變化台 JEI 配方類別
 * 顯示 CraftTweaker 定義的劍升級配方
 */
public class SwordUpgradeCategory implements IRecipeCategory<SwordUpgradeWrapper> {

    public static final String UID = "moremod.sword_upgrade_material";

    private final IDrawable background;
    private final IDrawable icon;
    private final String title;

    // GUI 佈局
    private static final int GUI_WIDTH = 150;
    private static final int GUI_HEIGHT = 60;

    // 槽位位置
    private static final int INPUT_SWORD_X = 10;
    private static final int INPUT_SWORD_Y = 20;
    private static final int MATERIAL_X = 45;
    private static final int MATERIAL_Y = 20;
    private static final int OUTPUT_X = 110;
    private static final int OUTPUT_Y = 20;

    public SwordUpgradeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(GUI_WIDTH, GUI_HEIGHT);

        // 使用劍作為圖標
        if (ModBlocks.SWORD_UPGRADE_STATION != null) {
            this.icon = guiHelper.createDrawableIngredient(new ItemStack(ModBlocks.SWORD_UPGRADE_STATION));
        } else {
            this.icon = guiHelper.createDrawableIngredient(new ItemStack(Items.IRON_SWORD));
        }

        this.title = I18n.format("jei.moremod.sword_upgrade_material");
    }

    @Override
    public String getUid() {
        return UID;
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
    public void setRecipe(IRecipeLayout recipeLayout, SwordUpgradeWrapper recipeWrapper, IIngredients ingredients) {
        IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();

        List<List<ItemStack>> inputs = ingredients.getInputs(ItemStack.class);
        List<List<ItemStack>> outputs = ingredients.getOutputs(ItemStack.class);

        // 輸入劍槽位 (slot 0)
        guiItemStacks.init(0, true, INPUT_SWORD_X, INPUT_SWORD_Y);
        if (!inputs.isEmpty() && !inputs.get(0).isEmpty()) {
            guiItemStacks.set(0, inputs.get(0));
        }

        // 材料槽位 (slot 1)
        guiItemStacks.init(1, true, MATERIAL_X, MATERIAL_Y);
        if (inputs.size() > 1 && !inputs.get(1).isEmpty()) {
            guiItemStacks.set(1, inputs.get(1));
        }

        // 輸出槽位 (slot 2)
        guiItemStacks.init(2, false, OUTPUT_X, OUTPUT_Y);
        if (!outputs.isEmpty() && !outputs.get(0).isEmpty()) {
            guiItemStacks.set(2, outputs.get(0));
        }
    }

    @Override
    public void drawExtras(Minecraft minecraft) {
        // 標題
        String titleText = "材質變化台";
        int titleX = (GUI_WIDTH - minecraft.fontRenderer.getStringWidth(titleText)) / 2;
        minecraft.fontRenderer.drawString(titleText, titleX, 3, 0x404040);

        // 繪製槽位邊框
        drawSlotBorder(minecraft, INPUT_SWORD_X - 1, INPUT_SWORD_Y - 1, 0x8B4513);  // 棕色 - 劍
        drawSlotBorder(minecraft, MATERIAL_X - 1, MATERIAL_Y - 1, 0x4169E1);       // 藍色 - 材料
        drawSlotBorder(minecraft, OUTPUT_X - 1, OUTPUT_Y - 1, 0x228B22);           // 綠色 - 輸出

        // 繪製 + 號
        minecraft.fontRenderer.drawString("+", INPUT_SWORD_X + 24, INPUT_SWORD_Y + 4, 0x606060);

        // 繪製箭頭
        minecraft.fontRenderer.drawString(">>>", MATERIAL_X + 24, MATERIAL_Y + 4, 0x606060);
        minecraft.fontRenderer.drawString("→", MATERIAL_X + 40, MATERIAL_Y + 4, 0x9966CC);

        // 槽位標籤
        minecraft.fontRenderer.drawString("劍", INPUT_SWORD_X + 4, INPUT_SWORD_Y + 22, 0x808080);
        minecraft.fontRenderer.drawString("材料", MATERIAL_X, MATERIAL_Y + 22, 0x808080);
        minecraft.fontRenderer.drawString("結果", OUTPUT_X + 2, OUTPUT_Y + 22, 0x808080);
    }

    private void drawSlotBorder(Minecraft mc, int x, int y, int color) {
        int size = 18;
        // 使用帶透明度的顏色
        int colorWithAlpha = (0x80 << 24) | (color & 0xFFFFFF);

        Gui.drawRect(x, y, x + size, y + 1, colorWithAlpha);
        Gui.drawRect(x, y + size - 1, x + size, y + size, colorWithAlpha);
        Gui.drawRect(x, y, x + 1, y + size, colorWithAlpha);
        Gui.drawRect(x + size - 1, y, x + size, y + size, colorWithAlpha);
    }
}
