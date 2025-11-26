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

    @ZenMethod
    public static void addRecipe(IItemStack output, IIngredient core, int time, int energy,
                                 float failChance, IIngredient[] pedestals) {
        if (pedestals.length < 1 || pedestals.length > 8) {
            CraftTweakerAPI.logError("Ritual recipe must have 1-8 pedestal items");
            return;
        }

        CraftTweakerAPI.apply(new AddRitualAction(output, core, time, energy, failChance, pedestals));
    }

    @ZenMethod
    public static void addRecipe(IItemStack output, IIngredient core, int time, int energy, IIngredient[] pedestals) {
        addRecipe(output, core, time, energy, 0.0f, pedestals);
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
        private final List<Ingredient> pedestalItems;

        public AddRitualAction(IItemStack output, IIngredient core, int time, int energy,
                               float failChance, IIngredient[] pedestals) {
            this.output = CraftTweakerMC.getItemStack(output);
            this.core = CraftTweakerMC.getIngredient(core);
            this.time = time;
            this.energy = energy;
            this.failChance = failChance;
            this.pedestalItems = new ArrayList<>();
            for (IIngredient pedestal : pedestals) {
                this.pedestalItems.add(CraftTweakerMC.getIngredient(pedestal));
            }
        }

        @Override
        public void apply() {
            RitualInfusionRecipe recipe = new RitualInfusionRecipe(
                    core, pedestalItems, output, time, energy, failChance
            );
            RitualInfusionAPI.RITUAL_RECIPES.add(recipe);
        }

        @Override
        public String describe() {
            return "Adding Ritual recipe for " + output.getDisplayName();
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