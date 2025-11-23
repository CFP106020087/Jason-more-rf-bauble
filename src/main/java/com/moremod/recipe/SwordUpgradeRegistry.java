package com.moremod.recipe;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.util.*;

/**
 * 剑升级配方注册表 - 支持NBT子集匹配
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
        
        // ✅ 修复：使用 AIR 创建新的 ItemStack，避免 EMPTY.copy() 问题
        ItemStack requirement = new ItemStack(Items.AIR);
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

    /**
     * ✅ 新增：检查材料是否在任意配方中存在（用于槽位验证）
     * 不管是精确配方还是通用配方，只要材料被注册过就返回true
     */
    public static boolean isValidMaterial(Item material) {
        if (material == null) return false;
        String materialName = material.getRegistryName().toString();
        
        for (Recipe recipe : RECIPES) {
            if (!recipe.inputRequirement.hasTagCompound()) continue;
            String recipeMaterial = recipe.inputRequirement.getTagCompound().getString("_upgrade_material");
            if (materialName.equals(recipeMaterial)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 工具方法 ====================
    
    /**
     * ✅ 修复：检查NBT是否匹配 - 使用子集匹配而不是完全相等
     * 玩家的剑只要包含配方要求的所有NBT即可，允许有额外的NBT
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
        
        // 检查输入是否包含所有要求的NBT（子集匹配）
        if (!input.hasTagCompound()) return false;
        
        return containsAllNBT(input.getTagCompound(), cleanRequirement);
    }

    /**
     * 递归检查 actual 是否包含 required 的所有键值对
     * 允许 actual 有额外的键
     */
    private static boolean containsAllNBT(NBTTagCompound actual, NBTTagCompound required) {
        for (String key : required.getKeySet()) {
            if (!actual.hasKey(key)) return false;
            
            NBTBase actualTag = actual.getTag(key);
            NBTBase requiredTag = required.getTag(key);
            
            // 递归检查复合标签
            if (requiredTag instanceof NBTTagCompound) {
                if (!(actualTag instanceof NBTTagCompound)) return false;
                if (!containsAllNBT((NBTTagCompound)actualTag, (NBTTagCompound)requiredTag)) {
                    return false;
                }
            } 
            // 列表需要精确匹配（或者你可以实现更宽松的逻辑）
            else if (requiredTag instanceof NBTTagList) {
                if (!(actualTag instanceof NBTTagList)) return false;
                // 对于附魔等列表，通常需要精确匹配
                if (!actualTag.equals(requiredTag)) return false;
            } 
            // 基本类型：直接比较
            else {
                if (!actualTag.equals(requiredTag)) return false;
            }
        }
        return true;
    }
}