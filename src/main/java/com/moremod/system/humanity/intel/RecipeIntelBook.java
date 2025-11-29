package com.moremod.system.humanity.intel;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nonnull;

/**
 * 情报书合成配方
 * Intel Book Crafting Recipe
 *
 * 配方：生物样本 + 书 + 经验瓶 = 情报书
 * 情报书会继承样本中的生物信息
 */
public class RecipeIntelBook extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    public RecipeIntelBook() {
        setRegistryName(new ResourceLocation("moremod", "intel_book"));
    }

    @Override
    public boolean matches(@Nonnull InventoryCrafting inv, @Nonnull World worldIn) {
        boolean hasSample = false;
        boolean hasBook = false;
        boolean hasExpBottle = false;
        int itemCount = 0;
        ItemStack sampleStack = ItemStack.EMPTY;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            itemCount++;

            // 检查生物样本
            if (stack.getItem() instanceof ItemBiologicalSample) {
                if (hasSample) return false; // 只能有一个样本
                // 必须是有效样本（包含生物信息）
                if (ItemBiologicalSample.getEntityId(stack) == null) return false;
                hasSample = true;
                sampleStack = stack;
            }
            // 检查书
            else if (stack.getItem() == Items.BOOK) {
                if (hasBook) return false; // 只能有一本书
                hasBook = true;
            }
            // 检查经验瓶
            else if (stack.getItem() == Items.EXPERIENCE_BOTTLE) {
                if (hasExpBottle) return false; // 只能有一个经验瓶
                hasExpBottle = true;
            }
            // 其他物品不匹配
            else {
                return false;
            }
        }

        // 必须恰好有这三种物品
        return hasSample && hasBook && hasExpBottle && itemCount == 3;
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult(@Nonnull InventoryCrafting inv) {
        // 找到样本
        ItemStack sampleStack = ItemStack.EMPTY;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemBiologicalSample) {
                sampleStack = stack;
                break;
            }
        }

        if (sampleStack.isEmpty()) return ItemStack.EMPTY;

        // 从样本创建情报书
        return ItemIntelBook.createFromSample(sampleStack);
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 3;
    }

    @Nonnull
    @Override
    public ItemStack getRecipeOutput() {
        // 返回一个空的情报书作为示例
        return new ItemStack(IntelSystemItems.INTEL_BOOK);
    }

    @Override
    public boolean isDynamic() {
        // 动态配方，因为输出取决于输入的样本
        return true;
    }

    @Nonnull
    @Override
    public NonNullList<ItemStack> getRemainingItems(@Nonnull InventoryCrafting inv) {
        // 默认行为：消耗所有材料
        NonNullList<ItemStack> ret = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
        return ret;
    }
}
