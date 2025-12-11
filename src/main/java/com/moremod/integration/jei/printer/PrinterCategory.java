package com.moremod.integration.jei.printer;

import com.moremod.init.ModBlocks;
import com.moremod.printer.PrinterRecipe;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * 打印机JEI配方类别
 */
public class PrinterCategory implements IRecipeCategory<PrinterWrapper> {

    public static final String UID = "moremod.printer";

    private final IDrawable background;
    private final IDrawable icon;
    private final String title;

    // GUI尺寸
    private static final int GUI_WIDTH = 160;
    private static final int GUI_HEIGHT = 80;

    // 材料槽位置 (2行3列)
    private static final int MATERIAL_START_X = 5;
    private static final int MATERIAL_START_Y = 15;
    private static final int SLOT_SIZE = 18;

    // 输出槽位置
    private static final int OUTPUT_X = 130;
    private static final int OUTPUT_Y = 25;

    public PrinterCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(GUI_WIDTH, GUI_HEIGHT);
        // 使用打印机方块作为图标
        ItemStack iconStack = ModBlocks.PRINTER != null
                ? new ItemStack(ModBlocks.PRINTER)
                : new ItemStack(Blocks.IRON_BLOCK);
        this.icon = guiHelper.createDrawableIngredient(iconStack);
        this.title = I18n.format("jei.moremod.printer");
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return title.isEmpty() ? "远古打印机" : title;
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
    public void setRecipe(IRecipeLayout recipeLayout, PrinterWrapper recipeWrapper, IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();

        PrinterRecipe recipe = recipeWrapper.getRecipe();
        List<ItemStack> materials = recipe.getMaterials();

        // 添加材料槽 (最多6个，2行3列)
        int slotIndex = 0;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int x = MATERIAL_START_X + col * SLOT_SIZE;
                int y = MATERIAL_START_Y + row * SLOT_SIZE;
                itemStacks.init(slotIndex, true, x, y);
                slotIndex++;
            }
        }

        // 添加输出槽
        itemStacks.init(6, false, OUTPUT_X, OUTPUT_Y);

        // 设置材料
        for (int i = 0; i < materials.size() && i < 6; i++) {
            itemStacks.set(i, materials.get(i));
        }

        // 设置输出
        itemStacks.set(6, recipe.getOutput());
    }
}
