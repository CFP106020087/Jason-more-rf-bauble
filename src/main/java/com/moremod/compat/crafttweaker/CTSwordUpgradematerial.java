package com.moremod.compat.crafttweaker;

import com.moremod.recipe.SwordUpgradeRegistry;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * CraftTweaker: 配置“材料 -> 目标剑 (+XP点数)”
 */
@ZenRegister
@ZenClass("mods.moremod.SwordUpgradematerial")
public class CTSwordUpgradematerial {

    @ZenMethod
    public static void add(IItemStack material, IItemStack targetSword) {
        add(material, targetSword, 0);
    }

    @ZenMethod
    public static void add(IItemStack material, IItemStack targetSword, int xpCost) {
        final Item mat = getItem(material, "material");
        final Item target = getItem(targetSword, "targetSword");
        if (!(target instanceof ItemSword)) {
            CraftTweakerAPI.logWarning("[MoreMod][SwordUpgradematerial] target is not a sword: " + target.getRegistryName());
        }
        final int cost = Math.max(0, xpCost);
        CraftTweakerAPI.apply(new ActionAdd(mat, target, cost));
    }

    @ZenMethod
    public static void remove(IItemStack material) {
        final Item mat = getItem(material, "material");
        CraftTweakerAPI.apply(new ActionRemove(mat));
    }

    @ZenMethod
    public static void clear() { CraftTweakerAPI.apply(new ActionClear()); }

    @ZenMethod
    public static void dump() { CraftTweakerAPI.apply(new ActionDump()); }

    private static Item getItem(IItemStack stack, String role) {
        ItemStack is = CraftTweakerMC.getItemStack(stack);
        if (is.isEmpty()) throw new IllegalArgumentException("[MoreMod][SwordUpgradematerial] " + role + " IItemStack is empty!");
        return is.getItem();
    }

    private static final class ActionAdd implements IAction {
        private final Item material, target; private final int xp;
        private ActionAdd(Item material, Item target, int xp) { this.material = material; this.target = target; this.xp = xp; }
        @Override public void apply() { SwordUpgradeRegistry.register(material, target, xp); }
        @Override public String describe() {
            return "[MoreMod][SwordUpgradematerial] add: " + material.getRegistryName() + " -> " + target.getRegistryName() + " (xp=" + xp + ")";
        }
    }
    private static final class ActionRemove implements IAction {
        private final Item material; private ActionRemove(Item material) { this.material = material; }
        @Override public void apply() { SwordUpgradeRegistry.remove(material); }
        @Override public String describe() { return "[MoreMod][SwordUpgradematerial] remove: " + material.getRegistryName(); }
    }
    private static final class ActionClear implements IAction {
        @Override public void apply() { SwordUpgradeRegistry.clear(); }
        @Override public String describe() { return "[MoreMod][SwordUpgradematerial] clear all"; }
    }
    private static final class ActionDump implements IAction {
        @Override public void apply() {
            java.util.Map<Item, SwordUpgradeRegistry.Recipe> map = SwordUpgradeRegistry.viewAll();
            CraftTweakerAPI.logInfo("[MoreMod][SwordUpgradematerial] ---- dump begin ----");
            for (java.util.Map.Entry<Item, SwordUpgradeRegistry.Recipe> e : map.entrySet()) {
                String k = (e.getKey() == null) ? "null" : e.getKey().getRegistryName().toString();
                String v = (e.getValue() == null || e.getValue().target == null) ? "null" : e.getValue().target.getRegistryName().toString();
                int xp = (e.getValue() == null) ? -1 : e.getValue().xpCost;
                CraftTweakerAPI.logInfo("  " + k + " -> " + v + "  xp=" + xp);
            }
            CraftTweakerAPI.logInfo("[MoreMod][SwordUpgradematerial] ---- dump end ----");
        }
        @Override public String describe() { return "[MoreMod][SwordUpgradematerial] dump mappings"; }
    }
}