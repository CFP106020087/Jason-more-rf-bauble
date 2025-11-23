package com.moremod.ritual;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RitualInfusionAPI {
    public static final List<RitualInfusionRecipe> RITUAL_RECIPES = new ArrayList<>();

    // 修改参数顺序以匹配新的构造函数
    public static void addRitual(Ingredient core, ItemStack output, int time,
                                 int energyPerPedestal, float failChance, Ingredient... pedestalItems) {
        List<Ingredient> pedestalList = Arrays.asList(pedestalItems);
        // 使用新的参数顺序
        RITUAL_RECIPES.add(new RitualInfusionRecipe(core, pedestalList, output, time, energyPerPedestal, failChance));
    }

    // 添加一个接受List的重载方法
    public static void addRitual(Ingredient core, List<Ingredient> pedestalItems, ItemStack output,
                                 int time, int energyPerPedestal, float failChance) {
        RITUAL_RECIPES.add(new RitualInfusionRecipe(core, pedestalItems, output, time, energyPerPedestal, failChance));
    }
}