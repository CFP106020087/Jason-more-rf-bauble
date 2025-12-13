package com.moremod.compat.crafttweaker;

import com.moremod.integration.jei.MoreModJEIPlugin;
import com.moremod.recipe.SwordUpgradeRegistry;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * CraftTweaker 物品升级配方接口 - 支持任意物品与NBT匹配
 *
 * v2.0 新增功能：
 * - 支持任意物品升级，不限于剑
 * - 完整NBT支持：输入A、材料B、输出C都可以带NBT
 * - addFull(A, B, C, XP) - 完整配方，A+B->C
 * - addFull(A, B, C, XP, copyNBT) - 可选是否复制输入NBT到输出
 *
 * 原有模式（兼容旧版）：
 * 1. NBT精确配方：addRecipe(带NBT的剑, 材料, 输出剑, XP)
 * 2. Item精确配方：addRecipe(不带NBT的剑, 材料, 输出剑, XP)
 * 3. 通用配方：add(材料, 输出剑, XP) - 任意剑适用
 *
 * JEI 集成：配方會自動顯示在 JEI 中（玩家進入世界時自動刷新）
 */
@ZenRegister
@ZenClass("mods.moremod.SwordUpgradematerial")
public class CTSwordUpgradematerial {

    // ==================== 精确配方 API ====================

    /**
     * 添加精确配方（自动检测是否带NBT）
     * 
     * 示例1（带NBT）：
     * // 只有锋利5的铁剑才能升级
     * mods.moremod.SwordUpgradematerial.addRecipe(
     *     <minecraft:iron_sword>.withTag({ench:[{id:16,lvl:5}]}),
     *     <minecraft:diamond>,
     *     <minecraft:diamond_sword>,
     *     30
     * );
     * 
     * 示例2（无NBT）：
     * // 任何铁剑都能升级
     * mods.moremod.SwordUpgradematerial.addRecipe(
     *     <minecraft:iron_sword>,
     *     <minecraft:diamond>,
     *     <minecraft:diamond_sword>,
     *     30
     * );
     */
    @ZenMethod
    public static void addRecipe(IItemStack inputSword, IItemStack material, IItemStack outputSword, int xpCost) {
        final ItemStack input = CraftTweakerMC.getItemStack(inputSword);
        final Item mat = getItem(material, "material");
        final Item output = getItem(outputSword, "outputSword");
        
        if (input.isEmpty()) {
            CraftTweakerAPI.logError("[SwordUpgrade] inputSword is empty!");
            return;
        }
        
        // 验证是否为剑
        if (!(input.getItem() instanceof ItemSword)) {
            CraftTweakerAPI.logWarning("[SwordUpgrade] inputSword is not a sword: " + input.getItem().getRegistryName());
        }
        if (!(output instanceof ItemSword)) {
            CraftTweakerAPI.logWarning("[SwordUpgrade] outputSword is not a sword: " + output.getRegistryName());
        }
        
        final int cost = Math.max(0, xpCost);
        
        // 自动检测：如果input有NBT，使用带NBT的配方
        if (input.hasTagCompound() && !input.getTagCompound().isEmpty()) {
            CraftTweakerAPI.apply(new ActionAddWithNBT(input.copy(), mat, output, cost));
        } else {
            CraftTweakerAPI.apply(new ActionAddExact(input.getItem(), mat, output, cost));
        }
    }

    /**
     * 添加精确配方（无经验消耗）
     */
    @ZenMethod
    public static void addRecipe(IItemStack inputSword, IItemStack material, IItemStack outputSword) {
        addRecipe(inputSword, material, outputSword, 0);
    }

    /**
     * 移除精确配方
     * 
     * 注意：需要与添加时的NBT完全一致才能移除
     */
    @ZenMethod
    public static void removeRecipe(IItemStack inputSword, IItemStack material) {
        final ItemStack input = CraftTweakerMC.getItemStack(inputSword);
        final Item mat = getItem(material, "material");
        
        if (input.isEmpty()) {
            CraftTweakerAPI.logError("[SwordUpgrade] inputSword is empty!");
            return;
        }
        
        if (input.hasTagCompound() && !input.getTagCompound().isEmpty()) {
            CraftTweakerAPI.apply(new ActionRemoveWithNBT(input.copy(), mat));
        } else {
            CraftTweakerAPI.apply(new ActionRemoveExact(input.getItem(), mat));
        }
    }

    // ==================== 通用配方 API ====================

    /**
     * 添加通用配方：任意剑 + 材料 -> 输出剑
     * 
     * 示例：
     * // 任意剑 + 下界之星 -> 神剑（消耗50点经验）
     * mods.moremod.SwordUpgradematerial.add(
     *     <minecraft:nether_star>,
     *     <moremod:legendary_sword>,
     *     50
     * );
     */
    @ZenMethod
    public static void add(IItemStack material, IItemStack outputSword, int xpCost) {
        final Item mat = getItem(material, "material");
        final Item output = getItem(outputSword, "outputSword");
        
        if (!(output instanceof ItemSword)) {
            CraftTweakerAPI.logWarning("[SwordUpgrade] outputSword is not a sword: " + output.getRegistryName());
        }
        
        final int cost = Math.max(0, xpCost);
        CraftTweakerAPI.apply(new ActionAdd(mat, output, cost));
    }

    /**
     * 添加通用配方（无经验消耗）
     */
    @ZenMethod
    public static void add(IItemStack material, IItemStack outputSword) {
        add(material, outputSword, 0);
    }

    /**
     * 移除通用配方
     */
    @ZenMethod
    public static void remove(IItemStack material) {
        final Item mat = getItem(material, "material");
        CraftTweakerAPI.apply(new ActionRemove(mat));
    }

    // ==================== 新版 API (v2.0) - 任意物品升级 ====================

    /**
     * 添加完整配方：物品A + 材料B -> 输出物品C
     * 支持任意物品（不限于剑），支持完整NBT匹配和输出
     *
     * 示例1（基础用法）：
     * // 铁锭 + 钻石 -> 钻石锭
     * mods.moremod.SwordUpgradematerial.addFull(
     *     <minecraft:iron_ingot>,
     *     <minecraft:diamond>,
     *     <minecraft:diamond> * 2,
     *     10
     * );
     *
     * 示例2（带NBT输入）：
     * // 锋利5的铁剑 + 下界之星 -> 锋利5的钻石剑
     * mods.moremod.SwordUpgradematerial.addFull(
     *     <minecraft:iron_sword>.withTag({ench:[{id:16,lvl:5}]}),
     *     <minecraft:nether_star>,
     *     <minecraft:diamond_sword>,
     *     50
     * );
     *
     * 示例3（带NBT材料）：
     * // 任意剑 + 特定NBT的书 -> 附魔剑
     * mods.moremod.SwordUpgradematerial.addFull(
     *     <minecraft:iron_sword>,
     *     <minecraft:enchanted_book>.withTag({StoredEnchantments:[{id:16,lvl:5}]}),
     *     <minecraft:diamond_sword>.withTag({ench:[{id:16,lvl:5}]}),
     *     30
     * );
     *
     * 示例4（带NBT输出，不复制输入NBT）：
     * mods.moremod.SwordUpgradematerial.addFull(
     *     <minecraft:iron_sword>,
     *     <minecraft:diamond>,
     *     <minecraft:diamond_sword>.withTag({display:{Name:"神剑"}}),
     *     20,
     *     false  // 不复制输入NBT，仅使用输出指定的NBT
     * );
     */
    @ZenMethod
    public static void addFull(IItemStack inputItem, IItemStack materialItem, IItemStack outputItem, int xpCost, boolean copyInputNBT) {
        final ItemStack input = CraftTweakerMC.getItemStack(inputItem);
        final ItemStack material = CraftTweakerMC.getItemStack(materialItem);
        final ItemStack output = CraftTweakerMC.getItemStack(outputItem);

        if (input.isEmpty()) {
            CraftTweakerAPI.logError("[ItemUpgrade] inputItem is empty!");
            return;
        }
        if (material.isEmpty()) {
            CraftTweakerAPI.logError("[ItemUpgrade] materialItem is empty!");
            return;
        }
        if (output.isEmpty()) {
            CraftTweakerAPI.logError("[ItemUpgrade] outputItem is empty!");
            return;
        }

        final int cost = Math.max(0, xpCost);
        CraftTweakerAPI.apply(new ActionAddFull(input.copy(), material.copy(), output.copy(), cost, copyInputNBT));
    }

    /**
     * 添加完整配方（默认复制输入NBT）
     */
    @ZenMethod
    public static void addFull(IItemStack inputItem, IItemStack materialItem, IItemStack outputItem, int xpCost) {
        addFull(inputItem, materialItem, outputItem, xpCost, true);
    }

    /**
     * 添加完整配方（无经验消耗，复制输入NBT）
     */
    @ZenMethod
    public static void addFull(IItemStack inputItem, IItemStack materialItem, IItemStack outputItem) {
        addFull(inputItem, materialItem, outputItem, 0, true);
    }

    /**
     * 移除完整配方
     */
    @ZenMethod
    public static void removeFull(IItemStack inputItem, IItemStack materialItem) {
        final ItemStack input = CraftTweakerMC.getItemStack(inputItem);
        final ItemStack material = CraftTweakerMC.getItemStack(materialItem);

        if (input.isEmpty() || material.isEmpty()) {
            CraftTweakerAPI.logError("[ItemUpgrade] removeFull: input or material is empty!");
            return;
        }

        CraftTweakerAPI.apply(new ActionRemoveFull(input.copy(), material.copy()));
    }

    /**
     * 添加通配配方：任意物品 + 材料B -> 输出物品C
     * 输入物品的NBT会被复制到输出
     *
     * 示例：
     * // 任意物品 + 魔法水晶 -> 魔法版物品
     * mods.moremod.SwordUpgradematerial.addWildcard(
     *     <moremod:magic_crystal>,
     *     <moremod:magic_item>,
     *     25
     * );
     */
    @ZenMethod
    public static void addWildcard(IItemStack materialItem, IItemStack outputItem, int xpCost) {
        final ItemStack material = CraftTweakerMC.getItemStack(materialItem);
        final ItemStack output = CraftTweakerMC.getItemStack(outputItem);

        if (material.isEmpty()) {
            CraftTweakerAPI.logError("[ItemUpgrade] materialItem is empty!");
            return;
        }
        if (output.isEmpty()) {
            CraftTweakerAPI.logError("[ItemUpgrade] outputItem is empty!");
            return;
        }

        final int cost = Math.max(0, xpCost);
        CraftTweakerAPI.apply(new ActionAddWildcard(material.copy(), output.copy(), cost));
    }

    /**
     * 添加通配配方（无经验消耗）
     */
    @ZenMethod
    public static void addWildcard(IItemStack materialItem, IItemStack outputItem) {
        addWildcard(materialItem, outputItem, 0);
    }

    // ==================== 管理 API ====================

    /**
     * 清空所有配方
     */
    @ZenMethod
    public static void clear() {
        CraftTweakerAPI.apply(new ActionClear());
    }

    /**
     * 打印所有配方（用于调试）
     */
    @ZenMethod
    public static void dump() {
        CraftTweakerAPI.apply(new ActionDump());
    }

    // ==================== 工具方法 ====================

    private static Item getItem(IItemStack stack, String role) {
        ItemStack is = CraftTweakerMC.getItemStack(stack);
        if (is.isEmpty()) {
            throw new IllegalArgumentException("[SwordUpgrade] " + role + " is empty!");
        }
        return is.getItem();
    }

    // ==================== Actions ====================

    /** 添加精确配方（带NBT） */
    private static final class ActionAddWithNBT implements IAction {
        private final ItemStack input;
        private final Item material, output;
        private final int xp;

        private ActionAddWithNBT(ItemStack input, Item material, Item output, int xp) {
            this.input = input;
            this.material = material;
            this.output = output;
            this.xp = xp;
        }

        @Override
        public void apply() {
            SwordUpgradeRegistry.registerExact(input, material, output, xp);
            // 觸發 JEI 刷新
            scheduleJEIRefresh();
        }

        @Override
        public String describe() {
            String nbtStr = input.hasTagCompound() ? input.getTagCompound().toString() : "{}";
            return String.format("[SwordUpgrade] NBT-Exact: %s%s + %s -> %s (xp=%d)",
                    input.getItem().getRegistryName(), nbtStr,
                    material.getRegistryName(), output.getRegistryName(), xp);
        }
    }

    /** 添加精确配方（无NBT） */
    private static final class ActionAddExact implements IAction {
        private final Item input, material, output;
        private final int xp;

        private ActionAddExact(Item input, Item material, Item output, int xp) {
            this.input = input;
            this.material = material;
            this.output = output;
            this.xp = xp;
        }

        @Override
        public void apply() {
            SwordUpgradeRegistry.registerExact(input, material, output, xp);
            // 觸發 JEI 刷新
            scheduleJEIRefresh();
        }

        @Override
        public String describe() {
            return String.format("[SwordUpgrade] Item-Exact: %s + %s -> %s (xp=%d)",
                    input.getRegistryName(), material.getRegistryName(),
                    output.getRegistryName(), xp);
        }
    }

    /** 添加通用配方 */
    private static final class ActionAdd implements IAction {
        private final Item material, output;
        private final int xp;

        private ActionAdd(Item material, Item output, int xp) {
            this.material = material;
            this.output = output;
            this.xp = xp;
        }

        @Override
        public void apply() {
            SwordUpgradeRegistry.register(material, output, xp);
            // 觸發 JEI 刷新
            scheduleJEIRefresh();
        }

        @Override
        public String describe() {
            return String.format("[SwordUpgrade] Any: ANY_SWORD + %s -> %s (xp=%d)",
                    material.getRegistryName(), output.getRegistryName(), xp);
        }
    }

    // JEI 刷新標記（避免多次刷新）
    private static boolean jeiRefreshScheduled = false;
    private static int recipeAddedCount = 0;

    /**
     * 安排 JEI 刷新（延遲執行以批量處理多個配方）
     */
    private static void scheduleJEIRefresh() {
        recipeAddedCount++;

        if (!jeiRefreshScheduled) {
            jeiRefreshScheduled = true;
            System.out.println("[SwordUpgrade] JEI refresh scheduled");
        }

        // 嘗試觸發延遲刷新
        try {
            MoreModJEIPlugin.triggerDelayedRefresh();
        } catch (Exception e) {
            // JEI 可能尚未初始化，忽略
        }

        // 每10個配方打印一次狀態
        if (recipeAddedCount % 10 == 0) {
            System.out.println("[SwordUpgrade] " + recipeAddedCount + " recipes added so far, " +
                              SwordUpgradeRegistry.getRecipeCount() + " total in registry");
        }
    }

    /**
     * 強制立即刷新 JEI
     * 可從 ZenScript 調用：mods.moremod.SwordUpgradematerial.refreshJEI()
     */
    @ZenMethod
    public static void refreshJEI() {
        System.out.println("[SwordUpgrade] Manual JEI refresh requested");
        System.out.println("[SwordUpgrade] Current recipes in registry: " + SwordUpgradeRegistry.getRecipeCount());

        try {
            MoreModJEIPlugin.refreshSwordUpgradeRecipes();
            System.out.println("[SwordUpgrade] JEI recipes refreshed successfully");
        } catch (Exception e) {
            System.err.println("[SwordUpgrade] Failed to refresh JEI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 獲取當前配方數量（用於調試）
     * 可從 ZenScript 調用：mods.moremod.SwordUpgradematerial.getRecipeCount()
     */
    @ZenMethod
    public static int getRecipeCount() {
        return SwordUpgradeRegistry.getRecipeCount();
    }

    /** 移除精确配方（带NBT） */
    private static final class ActionRemoveWithNBT implements IAction {
        private final ItemStack input;
        private final Item material;

        private ActionRemoveWithNBT(ItemStack input, Item material) {
            this.input = input;
            this.material = material;
        }

        @Override
        public void apply() {
            SwordUpgradeRegistry.removeExact(input, material);
        }

        @Override
        public String describe() {
            String nbtStr = input.hasTagCompound() ? input.getTagCompound().toString() : "{}";
            return String.format("[SwordUpgrade] Remove NBT-exact: %s%s + %s",
                    input.getItem().getRegistryName(), nbtStr, material.getRegistryName());
        }
    }

    /** 移除精确配方（无NBT） */
    private static final class ActionRemoveExact implements IAction {
        private final Item input, material;

        private ActionRemoveExact(Item input, Item material) {
            this.input = input;
            this.material = material;
        }

        @Override
        public void apply() {
            SwordUpgradeRegistry.removeExact(input, material);
        }

        @Override
        public String describe() {
            return String.format("[SwordUpgrade] Remove item-exact: %s + %s",
                    input.getRegistryName(), material.getRegistryName());
        }
    }

    /** 移除通用配方 */
    private static final class ActionRemove implements IAction {
        private final Item material;

        private ActionRemove(Item material) {
            this.material = material;
        }

        @Override
        public void apply() {
            SwordUpgradeRegistry.remove(material);
        }

        @Override
        public String describe() {
            return "[SwordUpgrade] Remove any: " + material.getRegistryName();
        }
    }

    /** 清空所有配方 */
    private static final class ActionClear implements IAction {
        @Override
        public void apply() {
            SwordUpgradeRegistry.clear();
        }

        @Override
        public String describe() {
            return "[SwordUpgrade] Clear all recipes";
        }
    }

    /** 打印所有配方 */
    private static final class ActionDump implements IAction {
        @Override
        public void apply() {
            java.util.List<SwordUpgradeRegistry.Recipe> recipes = SwordUpgradeRegistry.viewAll();
            
            CraftTweakerAPI.logInfo("========== [SwordUpgrade] Recipe Dump ==========");
            CraftTweakerAPI.logInfo("Total recipes: " + SwordUpgradeRegistry.getRecipeCount());
            
            int nbtCount = 0, itemCount = 0, anyCount = 0;
            
            for (SwordUpgradeRegistry.Recipe r : recipes) {
                if (!r.inputRequirement.hasTagCompound()) continue;
                
                boolean isAny = r.inputRequirement.getTagCompound().getBoolean("_any_sword");
                String mat = r.inputRequirement.getTagCompound().getString("_upgrade_material");
                String out = (r.targetSword == null) ? "null" : r.targetSword.getRegistryName().toString();
                
                if (isAny) {
                    CraftTweakerAPI.logInfo(String.format("  [Any] ANY_SWORD + %s -> %s (xp=%d)", mat, out, r.xpCost));
                    anyCount++;
                } else if (r.requireNBT) {
                    // ✅ 修复：打印NBT时移除内部标记
                    NBTTagCompound cleanTag = r.inputRequirement.getTagCompound().copy();
                    cleanTag.removeTag("_upgrade_material");
                    cleanTag.removeTag("_any_sword");
                    String nbtStr = cleanTag.toString();
                    
                    CraftTweakerAPI.logInfo(String.format("  [NBT] %s%s + %s -> %s (xp=%d)", 
                            r.inputRequirement.getItem().getRegistryName(), nbtStr, mat, out, r.xpCost));
                    nbtCount++;
                } else {
                    CraftTweakerAPI.logInfo(String.format("  [Item] %s + %s -> %s (xp=%d)", 
                            r.inputRequirement.getItem().getRegistryName(), mat, out, r.xpCost));
                    itemCount++;
                }
            }
            
            CraftTweakerAPI.logInfo(String.format("========== NBT:%d | Item:%d | Any:%d ==========", 
                    nbtCount, itemCount, anyCount));
        }

        @Override
        public String describe() {
            return "[SwordUpgrade] Dump all recipes";
        }
    }

    // ==================== 新版 Actions (v2.0) ====================

    /** 添加完整配方 */
    private static final class ActionAddFull implements IAction {
        private final ItemStack input, material, output;
        private final int xp;
        private final boolean copyNBT;

        private ActionAddFull(ItemStack input, ItemStack material, ItemStack output, int xp, boolean copyNBT) {
            this.input = input;
            this.material = material;
            this.output = output;
            this.xp = xp;
            this.copyNBT = copyNBT;
        }

        @Override
        public void apply() {
            SwordUpgradeRegistry.registerFull(input, material, output, xp, copyNBT);
            scheduleJEIRefresh();
        }

        @Override
        public String describe() {
            String inputNBT = input.hasTagCompound() ? input.getTagCompound().toString() : "";
            String matNBT = material.hasTagCompound() ? material.getTagCompound().toString() : "";
            String outNBT = output.hasTagCompound() ? output.getTagCompound().toString() : "";
            return String.format("[ItemUpgrade] Full: %s%s + %s%s -> %s%s (xp=%d, copyNBT=%b)",
                    input.getItem().getRegistryName(), inputNBT,
                    material.getItem().getRegistryName(), matNBT,
                    output.getItem().getRegistryName(), outNBT,
                    xp, copyNBT);
        }
    }

    /** 移除完整配方 */
    private static final class ActionRemoveFull implements IAction {
        private final ItemStack input, material;

        private ActionRemoveFull(ItemStack input, ItemStack material) {
            this.input = input;
            this.material = material;
        }

        @Override
        public void apply() {
            SwordUpgradeRegistry.removeFull(input, material);
        }

        @Override
        public String describe() {
            return String.format("[ItemUpgrade] Remove Full: %s + %s",
                    input.getItem().getRegistryName(), material.getItem().getRegistryName());
        }
    }

    /** 添加通配配方 */
    private static final class ActionAddWildcard implements IAction {
        private final ItemStack material, output;
        private final int xp;

        private ActionAddWildcard(ItemStack material, ItemStack output, int xp) {
            this.material = material;
            this.output = output;
            this.xp = xp;
        }

        @Override
        public void apply() {
            SwordUpgradeRegistry.registerWildcard(material, output, xp);
            scheduleJEIRefresh();
        }

        @Override
        public String describe() {
            String matNBT = material.hasTagCompound() ? material.getTagCompound().toString() : "";
            String outNBT = output.hasTagCompound() ? output.getTagCompound().toString() : "";
            return String.format("[ItemUpgrade] Wildcard: ANY + %s%s -> %s%s (xp=%d)",
                    material.getItem().getRegistryName(), matNBT,
                    output.getItem().getRegistryName(), outNBT, xp);
        }
    }
}