package com.moremod.integration.jei;

import com.moremod.init.ModBlocks;
import com.moremod.integration.jei.generator.*;
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
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
    public static final String OIL_GENERATOR_UID = "moremod.oil_generator";
    public static final String BIO_GENERATOR_UID = "moremod.bio_generator";

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

        // 注册石油发电机类别
        registration.addRecipeCategories(
                new OilGeneratorCategory(guiHelper)
        );

        // 注册生物质发电机类别
        registration.addRecipeCategories(
                new BioGeneratorCategory(guiHelper)
        );

        System.out.println("[MoreMod-JEI] Registered recipe categories (including generators)");
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

        // ========== 石油发电机系统 ==========
        registerOilGenerator(registry);

        // ========== 生物质发电机系统 ==========
        registerBioGenerator(registry);

        System.out.println("[MoreMod-JEI] JEI registration complete!");
    }

    // 用於追蹤是否已經註冊了事件處理器
    private static boolean eventHandlerRegistered = false;

    // 延遲刷新的tick計數器
    private static int pendingRefreshTicks = -1;
    private static final int REFRESH_DELAY_TICKS = 20; // 延遲1秒刷新

    /**
     * JEI 運行時可用時的回調
     * 保存運行時引用以便後續動態添加配方
     */
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        jeiRuntime = runtime;
        recipeRegistry = runtime.getRecipeRegistry();

        System.out.println("[MoreMod-JEI] ========================================");
        System.out.println("[MoreMod-JEI] Runtime available!");
        System.out.println("[MoreMod-JEI] recipeRegistry = " + (recipeRegistry != null ? "OK" : "NULL"));

        // 檢查 CraftTweaker 是否已加載
        boolean ctLoaded = Loader.isModLoaded("crafttweaker");
        System.out.println("[MoreMod-JEI] CraftTweaker loaded = " + ctLoaded);

        // 打印當前配方狀態
        int swordRecipes = SwordUpgradeRegistry.viewAll().size();
        int ritualRecipes = RitualInfusionAPI.RITUAL_RECIPES.size();
        System.out.println("[MoreMod-JEI] Current sword recipes in registry: " + swordRecipes);
        System.out.println("[MoreMod-JEI] Current ritual recipes in registry: " + ritualRecipes);
        System.out.println("[MoreMod-JEI] ========================================");

        // 只註冊一次事件監聽器
        if (!eventHandlerRegistered) {
            MinecraftForge.EVENT_BUS.register(new JEIRefreshHandler());
            eventHandlerRegistered = true;
            System.out.println("[MoreMod-JEI] Event handler registered");
        }

        // 如果已經有配方（可能CRT已經執行完畢），立即嘗試刷新
        if (swordRecipes > 0 || ritualRecipes > registeredRitualRecipes.size()) {
            System.out.println("[MoreMod-JEI] Recipes found, scheduling immediate refresh...");
            pendingRefreshTicks = REFRESH_DELAY_TICKS;
        }
    }

    /**
     * 事件處理器：處理玩家登入和客戶端tick
     */
    public static class JEIRefreshHandler {

        private int tickCounter = 0;
        private boolean firstTickRefreshDone = false;

        @SubscribeEvent
        public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            System.out.println("[MoreMod-JEI] ========================================");
            System.out.println("[MoreMod-JEI] Player logged in: " + event.player.getName());
            System.out.println("[MoreMod-JEI] hasRefreshed=" + hasRefreshed);
            System.out.println("[MoreMod-JEI] recipeRegistry=" + (recipeRegistry != null ? "OK" : "NULL"));
            System.out.println("[MoreMod-JEI] Sword recipes in registry: " + SwordUpgradeRegistry.viewAll().size());
            System.out.println("[MoreMod-JEI] Already registered sword recipes: " + registeredSwordRecipes.size());
            System.out.println("[MoreMod-JEI] ========================================");

            // 總是嘗試刷新（檢查是否有新配方）
            if (recipeRegistry != null) {
                int beforeSword = registeredSwordRecipes.size();
                int beforeRitual = registeredRitualRecipes.size();

                refreshAllRecipes();

                int afterSword = registeredSwordRecipes.size();
                int afterRitual = registeredRitualRecipes.size();

                if (afterSword > beforeSword || afterRitual > beforeRitual) {
                    System.out.println("[MoreMod-JEI] Refresh added: " +
                        (afterSword - beforeSword) + " sword, " +
                        (afterRitual - beforeRitual) + " ritual recipes");
                }
            } else {
                System.out.println("[MoreMod-JEI] WARNING: Cannot refresh - recipeRegistry is null!");
            }
            hasRefreshed = true;
        }

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            // 處理延遲刷新
            if (pendingRefreshTicks > 0) {
                pendingRefreshTicks--;
                if (pendingRefreshTicks == 0) {
                    System.out.println("[MoreMod-JEI] Executing delayed refresh...");
                    if (recipeRegistry != null) {
                        refreshAllRecipes();
                    }
                    pendingRefreshTicks = -1;
                }
            }

            // 每5秒檢查一次是否有新配方（前30秒內）
            tickCounter++;
            if (tickCounter < 600 && tickCounter % 100 == 0) { // 每5秒
                int currentRecipes = SwordUpgradeRegistry.viewAll().size();
                int registeredCount = registeredSwordRecipes.size();

                if (currentRecipes > registeredCount && recipeRegistry != null) {
                    System.out.println("[MoreMod-JEI] Detected " + (currentRecipes - registeredCount) + " new recipes, refreshing...");
                    refreshSwordUpgradeRecipes();
                }
            }
        }
    }

    /**
     * 手動觸發刷新（可從外部調用）
     */
    public static void triggerDelayedRefresh() {
        pendingRefreshTicks = REFRESH_DELAY_TICKS;
        System.out.println("[MoreMod-JEI] Delayed refresh scheduled");
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
        if (recipeRegistry == null) {
            System.out.println("[MoreMod-JEI] Cannot refresh sword recipes - recipeRegistry is null");
            return;
        }

        List<SwordUpgradeRegistry.Recipe> currentRecipes = SwordUpgradeRegistry.viewAll();
        System.out.println("[MoreMod-JEI] Found " + currentRecipes.size() + " sword upgrade recipes in registry");
        int addedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        for (SwordUpgradeRegistry.Recipe recipe : currentRecipes) {
            try {
                // 驗證配方有效性
                if (recipe == null || recipe.targetSword == null) {
                    System.err.println("[MoreMod-JEI] Skipping invalid recipe (null target)");
                    errorCount++;
                    continue;
                }

                // 使用配方的唯一標識符避免重複
                String recipeId = getRecipeId(recipe);
                if (registeredSwordRecipes.contains(recipeId)) {
                    skippedCount++;
                    continue;
                }

                SwordUpgradeWrapper wrapper = new SwordUpgradeWrapper(recipe);
                recipeRegistry.addRecipe(wrapper, SWORD_UPGRADE_UID);
                registeredSwordRecipes.add(recipeId);
                addedCount++;
            } catch (Exception e) {
                System.err.println("[MoreMod-JEI] Failed to add sword upgrade recipe: " + e.getMessage());
                e.printStackTrace();
                errorCount++;
            }
        }

        System.out.println("[MoreMod-JEI] Sword recipes refresh complete: added=" + addedCount +
                          ", skipped=" + skippedCount + ", errors=" + errorCount);
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
        try {
            if (recipe.inputRequirement != null && recipe.inputRequirement.getItem() != null) {
                sb.append(recipe.inputRequirement.getItem().getRegistryName());
            } else {
                sb.append("null_input");
            }
            if (recipe.inputRequirement != null && recipe.inputRequirement.hasTagCompound()) {
                sb.append("_").append(recipe.inputRequirement.getTagCompound().getString("_upgrade_material"));
            }
            sb.append("->");
            if (recipe.targetSword != null) {
                sb.append(recipe.targetSword.getRegistryName());
            } else {
                sb.append("null_output");
            }
        } catch (Exception e) {
            sb.append("error_").append(System.identityHashCode(recipe));
        }
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

    /**
     * 注册石油发电机配方
     */
    private void registerOilGenerator(IModRegistry registry) {
        // 1. 注册配方处理器
        registry.handleRecipes(
                GeneratorFuel.class,
                OilGeneratorWrapper::new,
                OIL_GENERATOR_UID
        );

        // 2. 添加配方
        List<GeneratorFuel> oilFuels = GeneratorRecipes.getOilGeneratorFuels();
        if (!oilFuels.isEmpty()) {
            registry.addRecipes(oilFuels, OIL_GENERATOR_UID);
            System.out.println("[MoreMod-JEI] Added " + oilFuels.size() + " oil generator fuels");
        }

        // 3. 添加催化剂
        if (ModBlocks.OIL_GENERATOR != null) {
            registry.addRecipeCatalyst(
                    new ItemStack(ModBlocks.OIL_GENERATOR),
                    OIL_GENERATOR_UID
            );

            registry.addIngredientInfo(
                    new ItemStack(ModBlocks.OIL_GENERATOR),
                    ItemStack.class,
                    "石油发电机\n" +
                            "使用原油或植物油产生RF能量。\n" +
                            "需要红石信号启动。"
            );
        }
    }

    /**
     * 注册生物质发电机配方
     */
    private void registerBioGenerator(IModRegistry registry) {
        // 1. 注册配方处理器
        registry.handleRecipes(
                GeneratorFuel.class,
                BioGeneratorWrapper::new,
                BIO_GENERATOR_UID
        );

        // 2. 添加配方
        List<GeneratorFuel> bioFuels = GeneratorRecipes.getBioGeneratorFuels();
        if (!bioFuels.isEmpty()) {
            registry.addRecipes(bioFuels, BIO_GENERATOR_UID);
            System.out.println("[MoreMod-JEI] Added " + bioFuels.size() + " bio generator fuels");
        }

        // 3. 添加催化剂
        if (ModBlocks.BIO_GENERATOR != null) {
            registry.addRecipeCatalyst(
                    new ItemStack(ModBlocks.BIO_GENERATOR),
                    BIO_GENERATOR_UID
            );

            registry.addIngredientInfo(
                    new ItemStack(ModBlocks.BIO_GENERATOR),
                    ItemStack.class,
                    "生物质发电机\n" +
                            "使用植物、种子、作物等生物质产生RF能量。\n" +
                            "需要红石信号启动。"
            );
        }
    }
}
