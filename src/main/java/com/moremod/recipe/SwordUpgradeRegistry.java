package com.moremod.recipe;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.util.*;

/**
 * 物品升级配方注册表 - 支持任意物品与NBT匹配
 *
 * 配方类型：
 * 1. 精确配方（带NBT）：特定物品A（含NBT） + 材料B -> 输出物品C
 * 2. 精确配方（无NBT）：特定物品类型A + 材料B -> 输出物品C
 * 3. 通用配方：任意物品 + 材料B -> 输出物品C
 *
 * 查询优先级：带NBT精确 > 无NBT精确 > 通用
 *
 * v2.0 更新：
 * - 支持任意物品，不限于剑
 * - 输出物品支持完整NBT（ItemStack）
 * - 材料支持NBT匹配
 */
public final class SwordUpgradeRegistry {

    /** 升级配方 */
    public static final class Recipe {
        public final ItemStack inputRequirement;  // 输入要求（可能包含NBT）
        public final boolean requireNBT;          // 是否要求NBT匹配
        public final Item targetSword;            // 输出物品（兼容旧版）
        public final ItemStack outputStack;       // 输出物品（新版，支持NBT）
        public final ItemStack materialStack;     // 材料物品（新版，支持NBT匹配）
        public final int xpCost;                  // 经验消耗
        public final boolean copyInputNBT;        // 是否复制输入物品的NBT到输出

        // 兼容旧版构造器
        private Recipe(ItemStack inputReq, boolean requireNBT, Item targetSword, int xpCost) {
            this.inputRequirement = inputReq;
            this.requireNBT = requireNBT;
            this.targetSword = targetSword;
            this.outputStack = targetSword != null ? new ItemStack(targetSword) : ItemStack.EMPTY;
            this.materialStack = ItemStack.EMPTY;
            this.xpCost = Math.max(0, xpCost);
            this.copyInputNBT = true; // 旧版默认复制NBT
        }

        // 新版构造器：支持完整ItemStack
        private Recipe(ItemStack inputReq, boolean requireNBT, ItemStack output, ItemStack material, int xpCost, boolean copyInputNBT) {
            this.inputRequirement = inputReq;
            this.requireNBT = requireNBT;
            this.targetSword = output.isEmpty() ? null : output.getItem();
            this.outputStack = output.copy();
            this.materialStack = material.copy();
            this.xpCost = Math.max(0, xpCost);
            this.copyInputNBT = copyInputNBT;
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

    // ==================== 新版 API (v2.0) - 支持任意物品与NBT ====================

    /**
     * 注册完整配方：输入物品A + 材料B -> 输出物品C
     * 支持完整的NBT匹配和输出
     *
     * @param inputStack 输入物品A（可带NBT要求）
     * @param materialStack 材料物品B（可带NBT要求）
     * @param outputStack 输出物品C（可带NBT）
     * @param xpCost 经验消耗
     * @param copyInputNBT 是否将输入物品的NBT复制到输出（true=合并，false=仅使用outputStack的NBT）
     */
    public static void registerFull(ItemStack inputStack, ItemStack materialStack, ItemStack outputStack, int xpCost, boolean copyInputNBT) {
        if (inputStack.isEmpty() || materialStack.isEmpty() || outputStack.isEmpty()) {
            System.err.println("[ItemUpgrade] registerFull: 参数不能为空！");
            return;
        }

        // 检查是否需要NBT匹配
        boolean requireInputNBT = inputStack.hasTagCompound() && !inputStack.getTagCompound().isEmpty();

        // 创建配方标识
        ItemStack requirement = inputStack.copy();
        if (!requirement.hasTagCompound()) {
            requirement.setTagCompound(new NBTTagCompound());
        }
        // 存储材料信息
        requirement.getTagCompound().setString("_upgrade_material", materialStack.getItem().getRegistryName().toString());
        if (materialStack.hasTagCompound()) {
            requirement.getTagCompound().setTag("_upgrade_material_nbt", materialStack.getTagCompound().copy());
        }

        RECIPES.add(new Recipe(requirement, requireInputNBT, outputStack, materialStack, xpCost, copyInputNBT));

        System.out.println("[ItemUpgrade] 注册配方: " +
            inputStack.getItem().getRegistryName() + " + " +
            materialStack.getItem().getRegistryName() + " -> " +
            outputStack.getItem().getRegistryName() + " (XP=" + xpCost + ", copyNBT=" + copyInputNBT + ")");
    }

    /**
     * 注册完整配方（默认复制输入NBT）
     */
    public static void registerFull(ItemStack inputStack, ItemStack materialStack, ItemStack outputStack, int xpCost) {
        registerFull(inputStack, materialStack, outputStack, xpCost, true);
    }

    /**
     * 注册通配符配方：任意物品 + 材料B -> 输出物品C
     * 输入物品的NBT会被复制到输出
     *
     * @param materialStack 材料物品B（可带NBT要求）
     * @param outputStack 输出物品C（可带NBT）
     * @param xpCost 经验消耗
     */
    public static void registerWildcard(ItemStack materialStack, ItemStack outputStack, int xpCost) {
        if (materialStack.isEmpty() || outputStack.isEmpty()) {
            System.err.println("[ItemUpgrade] registerWildcard: 参数不能为空！");
            return;
        }

        ItemStack requirement = new ItemStack(Items.AIR);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("_upgrade_material", materialStack.getItem().getRegistryName().toString());
        tag.setBoolean("_any_item", true);
        if (materialStack.hasTagCompound()) {
            tag.setTag("_upgrade_material_nbt", materialStack.getTagCompound().copy());
        }
        requirement.setTagCompound(tag);

        RECIPES.add(new Recipe(requirement, false, outputStack, materialStack, xpCost, true));

        System.out.println("[ItemUpgrade] 注册通配配方: ANY + " +
            materialStack.getItem().getRegistryName() + " -> " +
            outputStack.getItem().getRegistryName() + " (XP=" + xpCost + ")");
    }

    /**
     * 移除完整配方
     */
    public static void removeFull(ItemStack inputStack, ItemStack materialStack) {
        if (inputStack.isEmpty() || materialStack.isEmpty()) return;

        String materialName = materialStack.getItem().getRegistryName().toString();
        NBTTagCompound materialNBT = materialStack.hasTagCompound() ? materialStack.getTagCompound() : null;

        RECIPES.removeIf(recipe -> {
            if (!recipe.inputRequirement.hasTagCompound()) return false;
            NBTTagCompound reqTag = recipe.inputRequirement.getTagCompound();

            String recipeMaterial = reqTag.getString("_upgrade_material");
            if (!materialName.equals(recipeMaterial)) return false;

            // 检查材料NBT是否匹配
            if (materialNBT != null) {
                if (!reqTag.hasKey("_upgrade_material_nbt")) return false;
                if (!materialNBT.equals(reqTag.getCompoundTag("_upgrade_material_nbt"))) return false;
            }

            if (recipe.inputRequirement.getItem() != inputStack.getItem()) return false;

            // 检查输入NBT是否匹配
            if (recipe.requireNBT) {
                return nbtMatches(inputStack, recipe.inputRequirement);
            }
            return true;
        });
    }

    // ==================== 查询 API ====================

    /**
     * 根据输入物品和材料查找配方（新版，支持材料NBT）
     * 自动按优先级匹配：NBT精确 > Item精确 > 通用
     */
    public static Recipe getRecipe(ItemStack inputStack, ItemStack materialStack) {
        if (inputStack.isEmpty() || materialStack.isEmpty()) return null;

        String materialName = materialStack.getItem().getRegistryName().toString();
        Recipe bestMatch = null;
        int bestPriority = -1;  // 优先级：2=NBT精确, 1=Item精确, 0=通用

        for (Recipe recipe : RECIPES) {
            if (!recipe.inputRequirement.hasTagCompound()) continue;

            NBTTagCompound reqTag = recipe.inputRequirement.getTagCompound();
            String recipeMaterial = reqTag.getString("_upgrade_material");
            if (!materialName.equals(recipeMaterial)) continue;

            // 检查材料NBT是否匹配（如果配方要求）
            if (reqTag.hasKey("_upgrade_material_nbt")) {
                NBTTagCompound requiredMatNBT = reqTag.getCompoundTag("_upgrade_material_nbt");
                if (!materialStack.hasTagCompound()) continue;
                if (!containsAllNBT(materialStack.getTagCompound(), requiredMatNBT)) continue;
            }

            // 检查是否为通用配方（_any_sword 或 _any_item）
            if (reqTag.getBoolean("_any_sword") || reqTag.getBoolean("_any_item")) {
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
     * 根据输入物品和材料Item查找配方（兼容旧版）
     * 自动按优先级匹配：NBT精确 > Item精确 > 通用
     */
    public static Recipe getRecipe(ItemStack inputStack, Item material) {
        if (inputStack.isEmpty() || material == null) return null;
        return getRecipe(inputStack, new ItemStack(material));
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
     * 检查材料是否在任意配方中存在（用于槽位验证）
     * 不管是精确配方还是通用配方，只要材料被注册过就返回true
     */
    public static boolean isValidMaterial(Item material) {
        if (material == null) return false;
        return isValidMaterial(new ItemStack(material));
    }

    /**
     * 检查材料ItemStack是否在任意配方中存在（支持NBT匹配）
     */
    public static boolean isValidMaterial(ItemStack materialStack) {
        if (materialStack.isEmpty()) return false;
        String materialName = materialStack.getItem().getRegistryName().toString();

        for (Recipe recipe : RECIPES) {
            if (!recipe.inputRequirement.hasTagCompound()) continue;
            NBTTagCompound reqTag = recipe.inputRequirement.getTagCompound();
            String recipeMaterial = reqTag.getString("_upgrade_material");

            if (materialName.equals(recipeMaterial)) {
                // 如果配方要求材料NBT，也需要检查
                if (reqTag.hasKey("_upgrade_material_nbt")) {
                    NBTTagCompound requiredMatNBT = reqTag.getCompoundTag("_upgrade_material_nbt");
                    if (materialStack.hasTagCompound() && containsAllNBT(materialStack.getTagCompound(), requiredMatNBT)) {
                        return true;
                    }
                    // NBT不匹配，继续检查其他配方
                } else {
                    // 配方不要求材料NBT，只要Item匹配即可
                    return true;
                }
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