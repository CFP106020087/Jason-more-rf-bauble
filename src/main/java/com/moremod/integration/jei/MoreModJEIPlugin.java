package com.moremod.integration.jei;

import com.moremod.init.ModBlocks;
import com.moremod.moremod;
import com.moremod.recipe.DimensionLoomRecipes;
import com.moremod.ritual.RitualInfusionAPI;
import com.moremod.ritual.RitualInfusionRecipe;
import mezz.jei.api.*;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.IRecipeWrapperFactory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.List;

/**
 * JEI集成插件 - 支持多种配方系统
 */
@JEIPlugin
public class MoreModJEIPlugin implements IModPlugin {

    // 配方类别ID
    public static final String RITUAL_INFUSION_UID = "moremod.ritual_infusion";
    public static final String DIMENSION_LOOM_UID = "moremod.dimension_loom";


    public ResourceLocation getPluginUid() {
        return new ResourceLocation("moremod", "jei_plugin");
    }

    /**
     * 注册配方类别
     */
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers jeiHelpers = registration.getJeiHelpers();
        IGuiHelper guiHelper = jeiHelpers.getGuiHelper();

        // 注册仪式注入类别
        registration.addRecipeCategories(
                new RitualInfusionCategory(guiHelper)
        );

        // 注册维度织机类别
        registration.addRecipeCategories(
                new DimensionLoomCategory(guiHelper)
        );

        System.out.println("[MoreMod-JEI] Registered recipe categories");
    }

    /**
     * 注册配方和催化剂
     */
    @Override
    public void register(IModRegistry registry) {
        System.out.println("[MoreMod-JEI] Starting JEI registration...");

        // ========== 仪式注入系统 ==========
        registerRitualInfusion(registry);

        // ========== 维度织机系统 ==========
        registerDimensionLoom(registry);

        System.out.println("[MoreMod-JEI] JEI registration complete!");
    }

    /**
     * 注册仪式注入配方
     */
    private void registerRitualInfusion(IModRegistry registry) {
        // 1. 注册配方处理器
        registry.handleRecipes(
                RitualInfusionRecipe.class,
                new IRecipeWrapperFactory<RitualInfusionRecipe>() {
                    @Override
                    public IRecipeWrapper getRecipeWrapper(RitualInfusionRecipe recipe) {
                        return new RitualInfusionWrapper(recipe);
                    }
                },
                RITUAL_INFUSION_UID
        );

        // 2. 添加配方
        List<RitualInfusionRecipe> ritualRecipes = RitualInfusionAPI.RITUAL_RECIPES;
        if (!ritualRecipes.isEmpty()) {
            registry.addRecipes(ritualRecipes, RITUAL_INFUSION_UID);
            System.out.println("[MoreMod-JEI] Added " + ritualRecipes.size() + " ritual infusion recipes");
        }

        // 3. 添加催化剂
        if (moremod.RITUAL_CORE_BLOCK != null) {
            registry.addRecipeCatalyst(
                    new ItemStack(moremod.RITUAL_CORE_BLOCK),
                    RITUAL_INFUSION_UID
            );
        }

        if (moremod.RITUAL_PEDESTAL_BLOCK != null) {
            registry.addRecipeCatalyst(
                    new ItemStack(moremod.RITUAL_PEDESTAL_BLOCK),
                    RITUAL_INFUSION_UID
            );
        }
    }

    /**
     * 注册维度织机配方
     */
    private void registerDimensionLoom(IModRegistry registry) {
        // 1. 注册配方处理器
        registry.handleRecipes(
                DimensionLoomRecipes.DimensionLoomRecipe.class,
                new IRecipeWrapperFactory<DimensionLoomRecipes.DimensionLoomRecipe>() {
                    @Override
                    public IRecipeWrapper getRecipeWrapper(DimensionLoomRecipes.DimensionLoomRecipe recipe) {
                        return new DimensionLoomWrapper(recipe);
                    }
                },
                DIMENSION_LOOM_UID
        );

        // 2. 添加配方
        List<DimensionLoomRecipes.DimensionLoomRecipe> loomRecipes = DimensionLoomRecipes.getAllRecipes();
        if (!loomRecipes.isEmpty()) {
            registry.addRecipes(loomRecipes, DIMENSION_LOOM_UID);
            System.out.println("[MoreMod-JEI] Added " + loomRecipes.size() + " dimension loom recipes");
        }

        // 3. 添加催化剂（如果你有维度织机方块）
        if (ModBlocks.dimensionLoom != null) {
            registry.addRecipeCatalyst(
                    new ItemStack(ModBlocks.dimensionLoom),
                    DIMENSION_LOOM_UID
            );

            // 添加物品描述
            registry.addIngredientInfo(
                    new ItemStack(ModBlocks.dimensionLoom),
                    ItemStack.class,
                    "The Dimension Loom weaves items across dimensional boundaries.",
                    "Place items in a 3x3 pattern to create powerful artifacts."
            );
        }

        // 如果有GUI，添加点击区域
        // registry.addRecipeClickArea(
        //     GuiDimensionLoom.class,
        //     88, 32,  // 箭头位置
        //     28, 23,  // 箭头大小
        //     DIMENSION_LOOM_UID
        // );
    }
}