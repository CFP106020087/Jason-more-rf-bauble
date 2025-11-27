package com.moremod.compat.crafttweaker;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import java.util.*;

/**
 * 剑升级配方注册表 - 支持严格NBT匹配（方案1）
 * 
 * 配方类型：
 * 1. 精确配方（带NBT）：特定剑（含NBT） + 材料 -> 输出剑
 * 2. 精确配方（无NBT）：特定剑类型 + 材料 -> 输出剑
 * 3. 通用配方：任意剑 + 材料 -> 输出剑
 * 
 * 查询优先级：带NBT精确 > 无NBT精确 > 通用
 */
public final class SwordUpgradeRegistry {

    /** 升级配方 */
    public static final class Recipe {
        public final ItemStack inputRequirement;  // 输入要求（可能包含NBT）
        public final boolean requireNBT;          // 是否要求NBT匹配
        public final Item targetSword;            // 输出剑
        public final int xpCost;                  // 经验消耗

        private Recipe(ItemStack inputReq, boolean requireNBT, Item targetSword, int xpCost) {
            this.inputRequirement = inputReq;
            this.requireNBT = requireNBT;
            this.targetSword = targetSword;
            this.xpCost = Math.max(0, xpCost);
        }
    }

    // 所有配方存储（按优先级排序）
    private static final List<Recipe> RECIPES = new ArrayList<>();

    private SwordUpgradeRegistry() {}

    // ==================== 注册 API ====================
    
    /**
     * 注册精确配方（带NBT要求）
     * 
     * @param inputStack 输入剑（包含NBT要求）
     * @param material 升级材料
     * @param targetSword 输出剑
     * @param xpCost 经验消耗
     */
    public static void registerExact(ItemStack inputStack, Item material, Item targetSword, int xpCost) {
        if (inputStack.isEmpty() || material == null || targetSword == null) return;
        
        // 创建一个包含材料信息的特殊ItemStack作为标识
        ItemStack requirement = inputStack.copy();
        if (!requirement.hasTagCompound()) {
            requirement.setTagCompound(new NBTTagCompound());
        }
        requirement.getTagCompound().setString("_upgrade_material", material.getRegistryName().toString());
        
        boolean hasNBT = inputStack.hasTagCompound() && !inputStack.getTagCompound().isEmpty();
        RECIPES.add(new Recipe(requirement, hasNBT, targetSword, xpCost));
    }

    /**
     * 注册精确配方（忽略NBT）
     */
    public static void registerExact(Item inputSword, Item material, Item targetSword, int xpCost) {
        if (inputSword == null || material == null || targetSword == null) return;
        
        ItemStack requirement = new ItemStack(inputSword);
        requirement.setTagCompound(new NBTTagCompound());
        requirement.getTagCompound().setString("_upgrade_material", material.getRegistryName().toString());
        
        RECIPES.add(new Recipe(requirement, false, targetSword, xpCost));
    }

    /**
     * 注册通用配方：任意剑 + 材料 -> 输出剑
     */
    public static void register(Item material, Item targetSword, int xpCost) {
        if (material == null || targetSword == null) return;
        
        ItemStack requirement = ItemStack.EMPTY.copy();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("_upgrade_material", material.getRegistryName().toString());
        tag.setBoolean("_any_sword", true);
        requirement.setTagCompound(tag);
        
        RECIPES.add(new Recipe(requirement, false, targetSword, xpCost));
    }

    public static void register(Item material, Item targetSword) {
        register(material, targetSword, 0);
    }

    // ==================== 查询 API ====================
    
    /**
     * 根据输入剑ItemStack和材料查找配方
     * 自动按优先级匹配：NBT精确 > Item精确 > 通用
     */
    public static Recipe getRecipe(ItemStack inputStack, Item material) {
        if (inputStack.isEmpty() || material == null) return null;
        
        String materialName = material.getRegistryName().toString();
        Recipe bestMatch = null;
        int bestPriority = -1;  // 优先级：2=NBT精确, 1=Item精确, 0=通用
        
        for (Recipe recipe : RECIPES) {
            if (!recipe.inputRequirement.hasTagCompound()) continue;
            
            String recipeMaterial = recipe.inputRequirement.getTagCompound().getString("_upgrade_material");
            if (!materialName.equals(recipeMaterial)) continue;
            
            // 检查是否为通用配方
            if (recipe.inputRequirement.getTagCompound().getBoolean("_any_sword")) {
                if (bestPriority < 0) {
                    bestMatch = recipe;
                    bestPriority = 0;
                }
                continue;
            }
            
            // 检查Item类型是否匹配
            if (recipe.inputRequirement.getItem() != inputStack.getItem()) continue;
            
            // 如果配方要求NBT匹配
            if (recipe.requireNBT) {
                if (nbtMatches(inputStack, recipe.inputRequirement)) {
                    if (bestPriority < 2) {
                        bestMatch = recipe;
                        bestPriority = 2;
                    }
                }
            } else {
                // 配方不要求NBT，只要Item匹配即可
                if (bestPriority < 1) {
                    bestMatch = recipe;
                    bestPriority = 1;
                }
            }
        }
        
        return bestMatch;
    }

    /**
     * 根据Item查找（兼容旧版，忽略NBT）
     */
    public static Recipe getRecipe(Item inputSword, Item material) {
        return getRecipe(new ItemStack(inputSword), material);
    }

    /**
     * 只根据材料查找（兼容旧版）
     */
    public static Recipe getRecipe(Item material) {
        if (material == null) return null;
        String materialName = material.getRegistryName().toString();
        
        for (Recipe recipe : RECIPES) {
            if (!recipe.inputRequirement.hasTagCompound()) continue;
            String recipeMaterial = recipe.inputRequirement.getTagCompound().getString("_upgrade_material");
            if (materialName.equals(recipeMaterial) && 
                recipe.inputRequirement.getTagCompound().getBoolean("_any_sword")) {
                return recipe;
            }
        }
        return null;
    }

    // ==================== 移除 API ====================
    
    /**
     * 移除精确配方（带NBT）
     */
    public static void removeExact(ItemStack inputStack, Item material) {
        if (inputStack.isEmpty() || material == null) return;
        String materialName = material.getRegistryName().toString();
        
        RECIPES.removeIf(recipe -> {
            if (!recipe.inputRequirement.hasTagCompound()) return false;
            String recipeMaterial = recipe.inputRequirement.getTagCompound().getString("_upgrade_material");
            if (!materialName.equals(recipeMaterial)) return false;
            if (recipe.inputRequirement.getItem() != inputStack.getItem()) return false;
            return recipe.requireNBT && nbtMatches(inputStack, recipe.inputRequirement);
        });
    }

    /**
     * 移除精确配方（忽略NBT）
     */
    public static void removeExact(Item inputSword, Item material) {
        if (inputSword == null || material == null) return;
        String materialName = material.getRegistryName().toString();
        
        RECIPES.removeIf(recipe -> {
            if (!recipe.inputRequirement.hasTagCompound()) return false;
            String recipeMaterial = recipe.inputRequirement.getTagCompound().getString("_upgrade_material");
            if (!materialName.equals(recipeMaterial)) return false;
            return recipe.inputRequirement.getItem() == inputSword && !recipe.requireNBT;
        });
    }

    /**
     * 移除通用配方
     */
    public static void remove(Item material) {
        if (material == null) return;
        String materialName = material.getRegistryName().toString();
        
        RECIPES.removeIf(recipe -> {
            if (!recipe.inputRequirement.hasTagCompound()) return false;
            String recipeMaterial = recipe.inputRequirement.getTagCompound().getString("_upgrade_material");
            return materialName.equals(recipeMaterial) && 
                   recipe.inputRequirement.getTagCompound().getBoolean("_any_sword");
        });
    }

    /**
     * 清空所有配方
     */
    public static void clear() {
        RECIPES.clear();
    }

    // ==================== 调试 API ====================
    
    public static List<Recipe> viewAll() {
        return Collections.unmodifiableList(RECIPES);
    }

    public static int getRecipeCount() {
        return RECIPES.size();
    }

    // ==================== 工具方法 ====================
    
    /**
     * 检查NBT是否匹配（排除内部标记）
     */
    private static boolean nbtMatches(ItemStack input, ItemStack requirement) {
        NBTTagCompound requirementTag = requirement.getTagCompound();
        if (requirementTag == null) return !input.hasTagCompound();
        
        // 复制并移除内部标记
        NBTTagCompound cleanRequirement = requirementTag.copy();
        cleanRequirement.removeTag("_upgrade_material");
        cleanRequirement.removeTag("_any_sword");
        
        // 如果配方没有实际NBT要求，则不检查
        if (cleanRequirement.isEmpty()) return true;
        
        // 严格匹配NBT
        if (!input.hasTagCompound()) return false;
        
        return cleanRequirement.equals(input.getTagCompound());
    }
}
