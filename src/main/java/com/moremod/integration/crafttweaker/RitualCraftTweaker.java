package com.moremod.integration.crafttweaker;

import com.moremod.ritual.RitualInfusionAPI;
import com.moremod.ritual.RitualInfusionRecipe;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.ArrayList;
import java.util.List;

@ModOnly("crafttweaker")
@ZenClass("mods.moremod.Ritual")
@ZenRegister
public class RitualCraftTweaker {

    /**
     * 添加仪式配方（完整参数版，包含阶层要求）
     * @param output 输出物品
     * @param core 核心物品
     * @param time 时间（tick）
     * @param energy 每基座能量消耗
     * @param failChance 失败率 (0.0-1.0)
     * @param requiredTier 所需祭坛阶层 (1-3)
     * @param pedestals 基座物品数组
     */
    @ZenMethod
    public static void addRecipe(IItemStack output, IIngredient core, int time, int energy,
                                 float failChance, int requiredTier, IIngredient[] pedestals) {
        if (pedestals.length < 1 || pedestals.length > 8) {
            CraftTweakerAPI.logError("Ritual recipe must have 1-8 pedestal items");
            return;
        }
        if (requiredTier < 1 || requiredTier > 3) {
            CraftTweakerAPI.logError("Ritual tier must be 1-3, got: " + requiredTier);
            return;
        }

        CraftTweakerAPI.apply(new AddRitualAction(output, core, time, energy, failChance, requiredTier, pedestals));
    }

    /**
     * 添加仪式配方（带失败率，默认一阶）
     */
    @ZenMethod
    public static void addRecipe(IItemStack output, IIngredient core, int time, int energy,
                                 float failChance, IIngredient[] pedestals) {
        addRecipe(output, core, time, energy, failChance, 1, pedestals);
    }

    /**
     * 添加仪式配方（简化版，无失败率，默认一阶）
     */
    @ZenMethod
    public static void addRecipe(IItemStack output, IIngredient core, int time, int energy, IIngredient[] pedestals) {
        addRecipe(output, core, time, energy, 0.0f, 1, pedestals);
    }

    /**
     * 添加仅限特定阶层的仪式配方
     * @param tier 1=基础祭坛, 2=进阶祭坛, 3=大师祭坛
     */
    @ZenMethod
    public static void addTieredRecipe(int tier, IItemStack output, IIngredient core, int time, int energy,
                                       float failChance, IIngredient[] pedestals) {
        addRecipe(output, core, time, energy, failChance, tier, pedestals);
    }

    @ZenMethod
    public static void removeRecipe(IItemStack output) {
        CraftTweakerAPI.apply(new RemoveRitualAction(output));
    }

    @ZenMethod
    public static void removeRecipeByCore(IIngredient core) {
        CraftTweakerAPI.apply(new RemoveRitualByCoreAction(core));
    }

    @ZenMethod
    public static void removeAll() {
        CraftTweakerAPI.apply(new RemoveAllRitualsAction());
    }

    private static class AddRitualAction implements IAction {
        private final ItemStack output;
        private final Ingredient core;
        private final int time;
        private final int energy;
        private final float failChance;
        private final int requiredTier;
        private final List<Ingredient> pedestalItems;

        public AddRitualAction(IItemStack output, IIngredient core, int time, int energy,
                               float failChance, int requiredTier, IIngredient[] pedestals) {
            this.output = CraftTweakerMC.getItemStack(output);
            this.core = CraftTweakerMC.getIngredient(core);
            this.time = time;
            this.energy = energy;
            this.failChance = failChance;
            this.requiredTier = requiredTier;
            this.pedestalItems = new ArrayList<>();
            for (IIngredient pedestal : pedestals) {
                this.pedestalItems.add(CraftTweakerMC.getIngredient(pedestal));
            }
        }

        @Override
        public void apply() {
            RitualInfusionRecipe recipe = new RitualInfusionRecipe(
                    core, pedestalItems, output, time, energy, failChance, requiredTier
            );
            RitualInfusionAPI.RITUAL_RECIPES.add(recipe);
        }

        @Override
        public String describe() {
            String tierName = requiredTier == 1 ? "基础" : (requiredTier == 2 ? "进阶" : "大师");
            return "Adding Ritual recipe for " + output.getDisplayName() + " (requires " + tierName + " altar)";
        }
    }

    private static class RemoveRitualAction implements IAction {
        private final ItemStack output;

        public RemoveRitualAction(IItemStack output) {
            this.output = CraftTweakerMC.getItemStack(output);
        }

        @Override
        public void apply() {
            RitualInfusionAPI.RITUAL_RECIPES.removeIf(recipe ->
                    ItemStack.areItemStacksEqual(recipe.getOutput(), output)
            );
        }

        @Override
        public String describe() {
            return "Removing Ritual recipe for " + output.getDisplayName();
        }
    }

    private static class RemoveRitualByCoreAction implements IAction {
        private final Ingredient core;

        public RemoveRitualByCoreAction(IIngredient core) {
            this.core = CraftTweakerMC.getIngredient(core);
        }

        @Override
        public void apply() {
            RitualInfusionAPI.RITUAL_RECIPES.removeIf(recipe ->
                    recipe.getCore().apply(core.getMatchingStacks()[0])
            );
        }

        @Override
        public String describe() {
            return "Removing Ritual recipes with core: " + core.toString();
        }
    }

    private static class RemoveAllRitualsAction implements IAction {
        @Override
        public void apply() {
            RitualInfusionAPI.RITUAL_RECIPES.clear();
        }

        @Override
        public String describe() {
            return "Removing all Ritual recipes";
        }
    }
}