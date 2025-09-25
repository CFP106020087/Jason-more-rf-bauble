package com.moremod.integration.jei;

import com.moremod.init.ModBlocks;
import com.moremod.moremod;
import com.moremod.ritual.RitualInfusionAPI;
import com.moremod.ritual.RitualInfusionRecipe;
import mezz.jei.api.*;
import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * 使用你展示的IModRegistry接口的实际JEI插件
 */
@JEIPlugin
public class RitualInfusionJEIPlugin implements IModPlugin {

    public static final String RITUAL_INFUSION_UID = "moremod.ritual_infusion";

    public ResourceLocation getPluginUid() {
        return new ResourceLocation("moremod", "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers jeiHelpers = registration.getJeiHelpers();
        IGuiHelper guiHelper = jeiHelpers.getGuiHelper();

        // 注册配方类别
        registration.addRecipeCategories(
                new RitualInfusionCategory(guiHelper)
        );
    }

    @Override
    public void register(IModRegistry registry) {
        System.out.println("[MoreMod] Registering JEI integration...");

        // 1. 使用 handleRecipes - 注册配方处理器
        registry.handleRecipes(
                RitualInfusionRecipe.class,                    // 配方类
                recipe -> new RitualInfusionWrapper(recipe),   // 包装器工厂
                RITUAL_INFUSION_UID                            // 类别ID
        );

        // 2. 使用 addRecipes - 添加所有配方
        if (!RitualInfusionAPI.RITUAL_RECIPES.isEmpty()) {
            registry.addRecipes(
                    RitualInfusionAPI.RITUAL_RECIPES,  // 配方集合
                    RITUAL_INFUSION_UID                // 类别ID
            );
            System.out.println("[MoreMod] Added " + RitualInfusionAPI.RITUAL_RECIPES.size() + " recipes to JEI");
        }

        // 3. 使用 addRecipeCatalyst - 添加催化剂（右键这些物品查看配方）
        if (moremod.RITUAL_CORE_BLOCK != null) {
            ItemStack coreStack = new ItemStack(moremod.RITUAL_CORE_BLOCK);
            registry.addRecipeCatalyst(coreStack, RITUAL_INFUSION_UID);
        }

        if (moremod.RITUAL_PEDESTAL_BLOCK != null) {
            ItemStack pedestalStack = new ItemStack(moremod.RITUAL_PEDESTAL_BLOCK);
            registry.addRecipeCatalyst(pedestalStack, RITUAL_INFUSION_UID);
        }

        // 4. 使用 addIngredientInfo - 添加物品描述信息
        if (moremod.RITUAL_CORE_BLOCK != null) {
            registry.addIngredientInfo(
                    new ItemStack(moremod.RITUAL_CORE_BLOCK),
                    ItemStack.class,
                    "Place this in the center of your ritual setup.",
                    "Surround with up to 8 pedestals for complex rituals."
            );
        }

        // 5. 使用 addRecipeClickArea - 如果你有GUI，添加点击区域
        // registry.addRecipeClickArea(
        //     GuiRitualAltar.class,  // 你的GUI类
        //     79, 35,                // x, y 坐标
        //     22, 15,                // 宽度, 高度
        //     RITUAL_INFUSION_UID    // 类别ID
        // );

        // 6. 获取其他有用的注册器
        IIngredientRegistry ingredientRegistry = registry.getIngredientRegistry();
        IJeiHelpers jeiHelpers = registry.getJeiHelpers();

        System.out.println("[MoreMod] JEI integration complete!");
    }
}