package com.moremod.init;

import com.moremod.recipe.BottlingMachineRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.*;

/**
 * 精简版反向配方推导系统
 * 通过检测物品的流体Capability自动生成装瓶配方
 */
public class SimpleReverseDeducer {

    // 缓存已处理的配方，避免重复
    private static final Set<String> processedRecipes = new HashSet<>();

    /**
     * 执行反向推导
     * @return 生成的配方数量
     */
    public static int deduceRecipes() {
        System.out.println("[装瓶机] 开始反向配方推导...");

        int recipesCreated = 0;
        int itemsScanned = 0;

        // 遍历所有注册的物品
        for (Item item : ForgeRegistries.ITEMS) {
            try {
                // 获取物品的所有子类型
                NonNullList<ItemStack> subItems = NonNullList.create();
                if (item.getCreativeTab() != null) {
                    item.getSubItems(item.getCreativeTab(), subItems);
                } else {
                    subItems.add(new ItemStack(item));
                }

                for (ItemStack stack : subItems) {
                    if (stack.isEmpty()) continue;
                    itemsScanned++;

                    // 检查是否有流体处理能力
                    if (!stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
                        continue;
                    }

                    // 获取流体处理器
                    IFluidHandlerItem handler = stack.getCapability(
                            CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);

                    if (handler == null) continue;

                    // 检查容器中的液体
                    FluidStack contained = handler.drain(Integer.MAX_VALUE, false);
                    if (contained == null || contained.amount <= 0) {
                        continue;  // 空容器，跳过
                    }

                    // 找到了装有液体的容器，尝试获取空容器
                    ItemStack emptyContainer = findEmptyContainer(stack.copy(), handler);

                    if (!emptyContainer.isEmpty()) {
                        // 创建配方ID用于去重
                        String recipeKey = createRecipeKey(emptyContainer, contained);

                        if (!processedRecipes.contains(recipeKey)) {
                            processedRecipes.add(recipeKey);

                            // 注册配方
                            BottlingMachineRecipe.addRecipe(
                                    stack.copy(),
                                    emptyContainer.copy(),
                                    1,
                                    contained.copy()
                            );

                            recipesCreated++;

                            System.out.println("[装瓶机] 生成配方: " +
                                    emptyContainer.getDisplayName() + " + " +
                                    contained.amount + "mB " +
                                    contained.getLocalizedName() + " → " +
                                    stack.getDisplayName());
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略有问题的物品，继续处理其他物品
            }
        }

        System.out.println("[装瓶机] 反向推导完成：扫描 " + itemsScanned +
                " 个物品，生成 " + recipesCreated + " 个配方");

        return recipesCreated;
    }

    /**
     * 尝试找出对应的空容器
     */
    private static ItemStack findEmptyContainer(ItemStack filledStack, IFluidHandlerItem handler) {
        // 方法1：尝试排空容器
        ItemStack testStack = filledStack.copy();
        IFluidHandlerItem testHandler = testStack.getCapability(
                CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);

        if (testHandler != null) {
            // 排空所有液体
            testHandler.drain(Integer.MAX_VALUE, true);
            ItemStack emptyContainer = testHandler.getContainer();

            // 验证是否真的空了
            if (isContainerEmpty(emptyContainer)) {
                return emptyContainer;
            }
        }

        // 方法2：基于常见命名模式查找
        ItemStack guessedEmpty = guessEmptyContainer(filledStack);
        if (!guessedEmpty.isEmpty() && isValidContainer(guessedEmpty)) {
            return guessedEmpty;
        }

        return ItemStack.EMPTY;
    }

    /**
     * 检查容器是否为空
     */
    private static boolean isContainerEmpty(ItemStack container) {
        if (container.isEmpty()) return false;

        if (!container.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
            // 没有流体能力，可能是基础空容器（如玻璃瓶）
            return true;
        }

        IFluidHandlerItem handler = container.getCapability(
                CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);

        if (handler != null) {
            FluidStack fluid = handler.drain(Integer.MAX_VALUE, false);
            return fluid == null || fluid.amount == 0;
        }

        return false;
    }

    /**
     * 验证物品是否为有效容器
     */
    private static boolean isValidContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // 检查是否能装液体
        if (stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
            return true;
        }

        // 一些基础容器（玻璃瓶、桶等）可能没有Capability但仍然有效
        String name = stack.getTranslationKey().toLowerCase();
        return name.contains("bottle") || name.contains("bucket") ||
                name.contains("bowl") || name.contains("cell") ||
                name.contains("tank") || name.contains("canister");
    }

    /**
     * 基于命名规则猜测空容器
     */
    private static ItemStack guessEmptyContainer(ItemStack filledStack) {
        String registryName = filledStack.getItem().getRegistryName().toString();
        String modId = filledStack.getItem().getRegistryName().getNamespace();
        String path = filledStack.getItem().getRegistryName().getNamespace();

        // 尝试常见的命名模式
        String[] patterns = {
                // 移除液体名后缀
                path.replaceAll("_[a-z]+$", ""),
                // filled -> empty
                path.replace("filled_", "empty_"),
                path.replace("filled", "empty"),
                // full -> empty
                path.replace("full_", "empty_"),
                path.replace("full", "empty"),
                // 添加empty前缀
                "empty_" + path,
                // 只保留容器类型
                path.replaceAll("^[a-z]+_", "")
        };

        for (String pattern : patterns) {
            Item item = Item.getByNameOrId(modId + ":" + pattern);
            if (item != null) {
                ItemStack empty = new ItemStack(item);
                if (isValidContainer(empty) && isContainerEmpty(empty)) {
                    return empty;
                }
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * 创建配方唯一标识
     */
    private static String createRecipeKey(ItemStack container, FluidStack fluid) {
        return container.getItem().getRegistryName() + "@" +
                container.getMetadata() + "+" +
                fluid.getFluid().getName() + "@" +
                fluid.amount;
    }

    /**
     * 清空缓存（用于重新加载）
     */
    public static void clearCache() {
        processedRecipes.clear();
    }
}