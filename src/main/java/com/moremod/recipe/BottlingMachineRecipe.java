package com.moremod.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BottlingMachineRecipe {

    public static class IngredientStack {
        public final ItemStack stack;
        public final int amount;
        public final String oreDictName;

        public IngredientStack(ItemStack stack, int amount) {
            this.stack = stack;
            this.amount = amount;
            this.oreDictName = null;
        }

        public IngredientStack(String oreDictName, int amount) {
            this.stack = ItemStack.EMPTY;
            this.amount = amount;
            this.oreDictName = oreDictName;
        }

        public boolean matches(ItemStack input) {
            if (input.isEmpty()) return false;

            if (oreDictName != null) {
                // 矿物词典匹配
                int[] oreIds = OreDictionary.getOreIDs(input);
                int targetId = OreDictionary.getOreID(oreDictName);
                for (int id : oreIds) {
                    if (id == targetId) {
                        return input.getCount() >= amount;
                    }
                }
                return false;
            } else {
                // 直接物品匹配
                return ItemStack.areItemsEqual(stack, input) &&
                        ItemStack.areItemStackTagsEqual(stack, input) &&
                        input.getCount() >= amount;
            }
        }

        public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            if (oreDictName != null) {
                nbt.setString("ore", oreDictName);
            } else {
                nbt.setTag("stack", stack.writeToNBT(new NBTTagCompound()));
            }
            nbt.setInteger("amount", amount);
            return nbt;
        }

        public static IngredientStack readFromNBT(NBTTagCompound nbt) {
            int amount = nbt.getInteger("amount");
            if (nbt.hasKey("ore")) {
                return new IngredientStack(nbt.getString("ore"), amount);
            } else {
                ItemStack stack = new ItemStack(nbt.getCompoundTag("stack"));
                return new IngredientStack(stack, amount);
            }
        }
    }

    public final IngredientStack input;
    public final FluidStack fluidInput;
    public final ItemStack output;

    private static ArrayList<BottlingMachineRecipe> recipeList = new ArrayList<>();

    public BottlingMachineRecipe(ItemStack output, IngredientStack input, FluidStack fluidInput) {
        this.output = output;
        this.input = input;
        this.fluidInput = fluidInput;
    }

    // 添加配方的便捷方法
    public static void addRecipe(ItemStack output, ItemStack input, int inputAmount, FluidStack fluidInput) {
        IngredientStack ingredient = new IngredientStack(input, inputAmount);
        BottlingMachineRecipe recipe = new BottlingMachineRecipe(output, ingredient, fluidInput);
        recipeList.add(recipe);
    }

    public static void addRecipe(ItemStack output, String oreDictInput, int inputAmount, FluidStack fluidInput) {
        IngredientStack ingredient = new IngredientStack(oreDictInput, inputAmount);
        BottlingMachineRecipe recipe = new BottlingMachineRecipe(output, ingredient, fluidInput);
        recipeList.add(recipe);
    }

    // 查找配方
    public static BottlingMachineRecipe findRecipe(ItemStack input, FluidStack fluid) {
        if (input.isEmpty() || fluid == null) return null;

        for (BottlingMachineRecipe recipe : recipeList) {
            if (recipe.input.matches(input) && fluid.containsFluid(recipe.fluidInput)) {
                return recipe;
            }
        }
        return null;
    }

    // 移除配方
    public static List<BottlingMachineRecipe> removeRecipes(ItemStack output) {
        List<BottlingMachineRecipe> removedRecipes = new ArrayList<>();
        Iterator<BottlingMachineRecipe> it = recipeList.iterator();

        while (it.hasNext()) {
            BottlingMachineRecipe recipe = it.next();
            if (ItemStack.areItemsEqual(recipe.output, output) &&
                    ItemStack.areItemStackTagsEqual(recipe.output, output)) {
                removedRecipes.add(recipe);
                it.remove();
            }
        }
        return removedRecipes;
    }

    // 获取所有配方
    public static List<BottlingMachineRecipe> getAllRecipes() {
        return new ArrayList<>(recipeList);
    }

    // 清空所有配方
    public static void clearAllRecipes() {
        recipeList.clear();
    }

    // NBT序列化
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setTag("input", input.writeToNBT(new NBTTagCompound()));
        nbt.setTag("fluidInput", fluidInput.writeToNBT(new NBTTagCompound()));
        nbt.setTag("output", output.writeToNBT(new NBTTagCompound()));
        return nbt;
    }

    public static BottlingMachineRecipe loadFromNBT(NBTTagCompound nbt) {
        IngredientStack input = IngredientStack.readFromNBT(nbt.getCompoundTag("input"));
        FluidStack fluidInput = FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("fluidInput"));
        ItemStack output = new ItemStack(nbt.getCompoundTag("output"));

        // 尝试从已注册的配方中找到匹配的
        for (BottlingMachineRecipe recipe : recipeList) {
            if (recipe.input.amount == input.amount &&
                    recipe.fluidInput.isFluidEqual(fluidInput) &&
                    ItemStack.areItemStacksEqual(recipe.output, output)) {
                return recipe;
            }
        }

        // 如果找不到，创建一个新的（临时的）
        return new BottlingMachineRecipe(output, input, fluidInput);
    }
}