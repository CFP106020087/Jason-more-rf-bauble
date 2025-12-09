package com.moremod.integration.jei;

import com.moremod.recipe.SwordUpgradeRegistry;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 材質變化台配方包裝器
 * 將 SwordUpgradeRegistry.Recipe 轉換為 JEI 可顯示的格式
 */
public class SwordUpgradeWrapper implements IRecipeWrapper {

    private final SwordUpgradeRegistry.Recipe recipe;
    private final ItemStack inputSword;
    private final ItemStack material;
    private final ItemStack output;
    private final boolean isAnySword;

    public SwordUpgradeWrapper(SwordUpgradeRegistry.Recipe recipe) {
        this.recipe = recipe;

        // 解析配方數據
        NBTTagCompound reqTag = recipe.inputRequirement.hasTagCompound()
            ? recipe.inputRequirement.getTagCompound()
            : new NBTTagCompound();

        // 檢查是否為通用配方（任意劍）
        this.isAnySword = reqTag.getBoolean("_any_sword");

        // 獲取材料
        String materialName = reqTag.getString("_upgrade_material");
        Item materialItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(materialName));
        this.material = materialItem != null ? new ItemStack(materialItem) : ItemStack.EMPTY;

        // 獲取輸入劍
        if (isAnySword) {
            // 通用配方：顯示鐵劍作為示例
            this.inputSword = new ItemStack(net.minecraft.init.Items.IRON_SWORD);
        } else {
            // 精確配方：使用指定的劍
            ItemStack copy = recipe.inputRequirement.copy();
            if (copy.hasTagCompound()) {
                // 移除內部標記
                copy.getTagCompound().removeTag("_upgrade_material");
                copy.getTagCompound().removeTag("_any_sword");
                if (copy.getTagCompound().isEmpty()) {
                    copy.setTagCompound(null);
                }
            }
            this.inputSword = copy;
        }

        // 獲取輸出劍
        this.output = new ItemStack(recipe.targetSword);
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        List<List<ItemStack>> inputs = new ArrayList<>();

        // 輸入劍
        if (isAnySword) {
            // 通用配方：顯示多種劍作為示例
            List<ItemStack> swordExamples = new ArrayList<>();
            swordExamples.add(new ItemStack(net.minecraft.init.Items.WOODEN_SWORD));
            swordExamples.add(new ItemStack(net.minecraft.init.Items.STONE_SWORD));
            swordExamples.add(new ItemStack(net.minecraft.init.Items.IRON_SWORD));
            swordExamples.add(new ItemStack(net.minecraft.init.Items.GOLDEN_SWORD));
            swordExamples.add(new ItemStack(net.minecraft.init.Items.DIAMOND_SWORD));
            inputs.add(swordExamples);
        } else {
            inputs.add(Collections.singletonList(inputSword));
        }

        // 材料
        inputs.add(Collections.singletonList(material));

        ingredients.setInputLists(ItemStack.class, inputs);
        ingredients.setOutput(ItemStack.class, output);
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        // 顯示經驗消耗
        if (recipe.xpCost > 0) {
            String xpText = "經驗: " + recipe.xpCost + " 級";
            minecraft.fontRenderer.drawString(xpText, 45, 45, 0x80FF00);
        }

        // 通用配方提示
        if (isAnySword) {
            minecraft.fontRenderer.drawString("任意劍", 6, 45, 0x4169E1);
        }

        // NBT 要求提示
        if (recipe.requireNBT) {
            minecraft.fontRenderer.drawString("需要NBT", 6, 45, 0xFF6600);
        }
    }

    public SwordUpgradeRegistry.Recipe getRecipe() {
        return recipe;
    }

    public boolean isAnySword() {
        return isAnySword;
    }

    public int getXpCost() {
        return recipe.xpCost;
    }
}
