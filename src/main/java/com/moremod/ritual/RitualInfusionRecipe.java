package com.moremod.ritual;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import java.util.ArrayList;
import java.util.List;

public class RitualInfusionRecipe {
     public   Ingredient core;
     public   ItemStack output;
     public   int time;
     public   int energyPerPedestal;
     public   float failChance;
     public   List<Ingredient> pedestalItems;

    public RitualInfusionRecipe(Ingredient core, List<Ingredient> pedestalItems, ItemStack output,
                                int time, int energyPerPedestal, float failChance) {
        this.core = core;
        this.pedestalItems = pedestalItems;
        this.output = output;
        this.time = time;
        this.energyPerPedestal = energyPerPedestal;
        this.failChance = failChance;
    }

    public boolean matchPedestalStacks(List<ItemStack> stacks) {
        if (stacks.size() < pedestalItems.size()) return false;

        List<ItemStack> available = new ArrayList<>(stacks);
        for (Ingredient required : pedestalItems) {
            boolean found = false;
            for (int i = 0; i < available.size(); i++) {
                if (required.apply(available.get(i))) {
                    available.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public Ingredient getCore() { return core; }
    public ItemStack getOutput() { return output.copy(); }
    public int getTime() { return time; }
    public int getEnergyPerPedestal() { return energyPerPedestal; }
    public float getFailChance() { return failChance; }
    public int getPedestalCount() { return pedestalItems.size(); }
    public List<Ingredient> getPedestalItems() { return pedestalItems; }  // 添加这个方法
}