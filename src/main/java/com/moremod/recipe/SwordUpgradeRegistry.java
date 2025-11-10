
package com.moremod.recipe;

import net.minecraft.item.Item;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 材料物品 -> 升级配方（目标剑 + XP消耗） */
public final class SwordUpgradeRegistry {

    /** 升级配方 */
    public static final class Recipe {
        public final Item target;
        /** 经验消耗（单位：XP点，不是等级）。 */
        public final int xpCost;

        public Recipe(Item target, int xpCost) {
            this.target = target;
            this.xpCost = Math.max(0, xpCost);
        }
    }

    private static final Map<Item, Recipe> MATERIAL_TO_RECIPE = new HashMap<>();

    private SwordUpgradeRegistry() {}

    /** 注册：材料 -> 目标剑（默认不消耗经验） */
    public static void register(Item material, Item targetSword) {
        register(material, targetSword, 0);
    }

    /** 注册：材料 -> 目标剑 + XP消耗（单位：点） */
    public static void register(Item material, Item targetSword, int xpCost) {
        if (material == null || targetSword == null) return;
        MATERIAL_TO_RECIPE.put(material, new Recipe(targetSword, xpCost));
    }

    public static Recipe getRecipe(Item material) {
        return MATERIAL_TO_RECIPE.get(material);
    }

    public static void remove(Item material) {
        MATERIAL_TO_RECIPE.remove(material);
    }

    public static void clear() {
        MATERIAL_TO_RECIPE.clear();
    }

    public static Map<Item, Recipe> viewAll() {
        return Collections.unmodifiableMap(MATERIAL_TO_RECIPE);
    }
}
