package com.moremod.integration.jei;

import com.moremod.init.ModBlocks;
import com.moremod.moremod;
import com.moremod.recipe.DimensionLoomRecipes;
import com.moremod.recipe.SwordUpgradeRegistry;
import com.moremod.ritual.RitualInfusionAPI;
import com.moremod.ritual.RitualInfusionRecipe;
import mezz.jei.api.*;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.IRecipeWrapperFactory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JEI集成插件 - 支持多种配方系统
 *
 * 支持 CraftTweaker 動態配方：
 * - 在遊戲加載完成後自動刷新 JEI 配方
 * - 支持運行時添加/移除配方
 */
@JEIPlugin
public class MoreModJEIPlugin implements IModPlugin {

    // 配方类别ID
    public static final String RITUAL_INFUSION_UID = "moremod.ritual_infusion";
    public static final String DIMENSION_LOOM_UID = "moremod.dimension_loom";
    public static final String SWORD_UPGRADE_UID = "moremod.sword_upgrade_material";

    // JEI 運行時引用（用於動態配方註冊）
    private static IRecipeRegistry recipeRegistry;
    private static IJeiRuntime jeiRuntime;

    // 追踪已註冊的配方（用於避免重複）
    private static final Set<Object> registeredSwordRecipes = new HashSet<>();
    private static final Set<Object> registeredRitualRecipes = new HashSet<>();

    // 是否已完成首次刷新
    private static boolean hasRefreshed = false;

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

        // 注册材質變化台类别
        registration.addRecipeCategories(
                new SwordUpgradeCategory(guiHelper)
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

        // ========== 材質變化台系统 (CraftTweaker) ==========
        registerSwordUpgrade(registry);

        System.out.println("[MoreMod-JEI] JEI registration complete!");
    }

    /**
     * JEI 運行時可用時的回調
     * 保存運行時引用以便後續動態添加配方
     */
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        jeiRuntime = runtime;
        recipeRegistry = runtime.getRecipeRegistry();

        // 註冊事件監聽器用於刷新配方
        MinecraftForge.EVENT_BUS.register(new JEIRefreshHandler());

        System.out.println("[MoreMod-JEI] Runtime available, dynamic recipe support enabled");
    }

    /**
     * 事件處理器：在玩家進入世界時刷新 JEI 配方
     * 這確保 CraftTweaker 腳本已經執行完畢
     */
    public static class JEIRefreshHandler {
        @SubscribeEvent
        public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (!hasRefreshed) {
                hasRefreshed = true;
                refreshAllRecipes();
            }
        }
    }

    /**
     * 刷新所有動態配方（CraftTweaker 配方）
     * 可從外部調用以強制刷新
     */
    public static void refreshAllRecipes() {
        if (recipeRegistry == null) {
            System.out.println("[MoreMod-JEI] Cannot refresh recipes - JEI runtime not available");
            return;
        }

        System.out.println("[MoreMod-JEI] Refreshing dynamic recipes...");

        // 刷新材質變化台配方
        refreshSwordUpgradeRecipes();

        // 刷新儀式配方
        refreshRitualRecipes();

        System.out.println("[MoreMod-JEI] Dynamic recipe refresh complete!");
    }

    /**
     * 刷新材質變化台配方
     */
    public static void refreshSwordUpgradeRecipes() {
        if (recipeRegistry == null) return;

        List<SwordUpgradeRegistry.Recipe> currentRecipes = SwordUpgradeRegistry.viewAll();
        int addedCount = 0;

        for (SwordUpgradeRegistry.Recipe recipe : currentRecipes) {
            // 使用配方的唯一標識符避免重複
            String recipeId = getRecipeId(recipe);
            if (!registeredSwordRecipes.contains(recipeId)) {
                try {
                    recipeRegistry.addRecipe(new SwordUpgradeWrapper(recipe), SWORD_UPGRADE_UID);
                    registeredSwordRecipes.add(recipeId);
                    addedCount++;
                } catch (Exception e) {
                    System.err.println("[MoreMod-JEI] Failed to add sword upgrade recipe: " + e.getMessage());
                }
            }
        }

        if (addedCount > 0) {
            System.out.println("[MoreMod-JEI] Added " + addedCount + " new sword upgrade recipes to JEI");
        }
    }

    /**
     * 刷新儀式配方
     */
    public static void refreshRitualRecipes() {
        if (recipeRegistry == null) return;

        List<RitualInfusionRecipe> currentRecipes = RitualInfusionAPI.RITUAL_RECIPES;
        int addedCount = 0;

        for (RitualInfusionRecipe recipe : currentRecipes) {
            // 使用輸出物品作為簡單的唯一標識
            String recipeId = recipe.output.toString();
            if (!registeredRitualRecipes.contains(recipeId)) {
                try {
                    recipeRegistry.addRecipe(new RitualInfusionWrapper(recipe), RITUAL_INFUSION_UID);
                    registeredRitualRecipes.add(recipeId);
                    addedCount++;
                } catch (Exception e) {
                    System.err.println("[MoreMod-JEI] Failed to add ritual recipe: " + e.getMessage());
                }
            }
        }

        if (addedCount > 0) {
            System.out.println("[MoreMod-JEI] Added " + addedCount + " new ritual recipes to JEI");
        }
    }

    /**
     * 生成配方的唯一標識符
     */
    private static String getRecipeId(SwordUpgradeRegistry.Recipe recipe) {
        StringBuilder sb = new StringBuilder();
        sb.append(recipe.inputRequirement.getItem().getRegistryName());
        if (recipe.inputRequirement.hasTagCompound()) {
            sb.append(recipe.inputRequirement.getTagCompound().getString("_upgrade_material"));
        }
        sb.append("->");
        sb.append(recipe.targetSword.getRegistryName());
        return sb.toString();
    }

    /**
     * 获取 JEI 運行時（供外部使用）
     */
    public static IJeiRuntime getJeiRuntime() {
        return jeiRuntime;
    }

    /**
     * 获取配方註冊表（供外部使用）
     */
    public static IRecipeRegistry getRecipeRegistry() {
        return recipeRegistry;
    }

    /**
     * 强制重置刷新状态（用于重新加载）
     */
    public static void resetRefreshState() {
        hasRefreshed = false;
        registeredSwordRecipes.clear();
        registeredRitualRecipes.clear();
    }

    // ==================== 原有註冊方法 ====================

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

        // 2. 添加配方（初始配方，CRT 配方會在運行時添加）
        List<RitualInfusionRecipe> ritualRecipes = RitualInfusionAPI.RITUAL_RECIPES;
        if (!ritualRecipes.isEmpty()) {
            registry.addRecipes(ritualRecipes, RITUAL_INFUSION_UID);
            // 標記為已註冊
            for (RitualInfusionRecipe recipe : ritualRecipes) {
                registeredRitualRecipes.add(recipe.output.toString());
            }
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
                    "维度织机，可编织维度的碎片。\n" +
                            "将物品按 3x3 排列以制作道具。"
            );
            registry.addIngredientInfo(
                    new ItemStack(moremod.RITUAL_CORE_BLOCK),
                    ItemStack.class,
                    "注能核心\n" +
                            "在周围放上注能台，提供注能台能量，以为核心道具注入其他物品。"
            );
        }
    }

    /**
     * 注册材質變化台配方 (CraftTweaker 定義)
     */
    private void registerSwordUpgrade(IModRegistry registry) {
        // 1. 注册配方处理器
        registry.handleRecipes(
                SwordUpgradeRegistry.Recipe.class,
                SwordUpgradeWrapper::new,
                SWORD_UPGRADE_UID
        );

        // 2. 获取所有配方并添加（初始配方）
        List<SwordUpgradeRegistry.Recipe> recipes = SwordUpgradeRegistry.viewAll();
        if (!recipes.isEmpty()) {
            List<SwordUpgradeRegistry.Recipe> recipeList = new ArrayList<>(recipes);
            registry.addRecipes(recipeList, SWORD_UPGRADE_UID);
            // 標記為已註冊
            for (SwordUpgradeRegistry.Recipe recipe : recipeList) {
                registeredSwordRecipes.add(getRecipeId(recipe));
            }
            System.out.println("[MoreMod-JEI] Added " + recipeList.size() + " sword upgrade recipes");
        } else {
            System.out.println("[MoreMod-JEI] No sword upgrade recipes found (will refresh after CraftTweaker loads)");
        }

        // 3. 添加催化剂
        if (ModBlocks.SWORD_UPGRADE_STATION != null) {
            registry.addRecipeCatalyst(
                    new ItemStack(ModBlocks.SWORD_UPGRADE_STATION),
                    SWORD_UPGRADE_UID
            );
        }

        if (ModBlocks.SWORD_UPGRADE_STATION_MATERIAL != null) {
            registry.addRecipeCatalyst(
                    new ItemStack(ModBlocks.SWORD_UPGRADE_STATION_MATERIAL),
                    SWORD_UPGRADE_UID
            );

            registry.addIngredientInfo(
                    new ItemStack(ModBlocks.SWORD_UPGRADE_STATION_MATERIAL),
                    ItemStack.class,
                    "材質變化台\n" +
                            "使用 CraftTweaker 定義配方。\n" +
                            "將劍放入左側，材料放入中間，即可變化劍的材質。"
            );
        }
    }
}
