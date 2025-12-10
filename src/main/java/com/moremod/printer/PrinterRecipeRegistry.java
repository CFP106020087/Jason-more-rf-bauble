package com.moremod.printer;

import com.moremod.init.ModItems;
import com.moremod.item.ModMaterialItems;
import com.moremod.item.RegisterItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;

import java.util.*;

/**
 * 打印机配方注册表
 * 所有配方完全由CraftTweaker脚本控制
 *
 * 使用示例 (CraftTweaker脚本):
 *
 * // 添加配方
 * mods.moremod.Printer.addRecipe("my_recipe", <minecraft:diamond>, 100000, 200,
 *     [<minecraft:iron_ingot> * 4, <minecraft:gold_ingot> * 2]);
 *
 * // 添加带显示名称和稀有度的配方
 * mods.moremod.Printer.addRecipeAdvanced("my_recipe", "我的配方", "epic",
 *     <minecraft:diamond>, 100000, 200,
 *     [<minecraft:iron_ingot> * 4]);
 *
 * // 创建模板物品用于合成配方
 * val template = mods.moremod.Printer.createTemplate("my_recipe");
 */
public class PrinterRecipeRegistry {

    private static final Map<String, PrinterRecipe> RECIPES = new LinkedHashMap<>();
    private static boolean defaultRecipesRegistered = false;

    /**
     * 注册一个配方
     */
    public static void registerRecipe(PrinterRecipe recipe) {
        RECIPES.put(recipe.getTemplateId(), recipe);
        System.out.println("[Printer] 注册配方: " + recipe.getTemplateId() +
            (recipe.getDisplayName().isEmpty() ? "" : " (" + recipe.getDisplayName() + ")") +
            " -> " + recipe.getOutput().getDisplayName());
    }

    /**
     * 移除一个配方（用于CraftTweaker）
     */
    public static void removeRecipe(String templateId) {
        if (RECIPES.remove(templateId) != null) {
            System.out.println("[Printer] 移除配方: " + templateId);
        }
    }

    /**
     * 根据模版ID获取配方
     */
    public static PrinterRecipe getRecipe(String templateId) {
        return RECIPES.get(templateId);
    }

    /**
     * 获取所有配方
     */
    public static Collection<PrinterRecipe> getAllRecipes() {
        return Collections.unmodifiableCollection(RECIPES.values());
    }

    /**
     * 获取配方数量
     */
    public static int getRecipeCount() {
        return RECIPES.size();
    }

    /**
     * 检查模版ID是否有对应配方
     */
    public static boolean hasRecipe(String templateId) {
        return RECIPES.containsKey(templateId);
    }

    /**
     * 清除所有配方（用于重新加载）
     */
    public static void clearAllRecipes() {
        RECIPES.clear();
        System.out.println("[Printer] 已清除所有配方");
    }

    /**
     * 注册默认预设配方
     * 包含模组内置的材料打印配方
     */
    public static void registerDefaultRecipes() {
        if (defaultRecipesRegistered) {
            return;
        }
        defaultRecipesRegistered = true;

        System.out.println("[Printer] 正在注册预设配方...");

        // ========== 1. 安提基特拉齿轮 (Antikythera Gear) ==========
        // 稀有的机械材料，用于高级机械制作
        if (RegisterItem.ANTIKYTHERA_GEAR != null) {
            try {
                PrinterRecipe gearRecipe = new PrinterRecipe.Builder()
                    .setTemplateId("antikythera_gear")
                    .setDisplayName("安提基特拉齿轮模板")
                    .setOutput(new ItemStack(RegisterItem.ANTIKYTHERA_GEAR, 2))
                    .addMaterial(new ItemStack(Items.IRON_INGOT, 8))
                    .addMaterial(new ItemStack(Items.GOLD_INGOT, 4))
                    .addMaterial(new ItemStack(Items.REDSTONE, 16))
                    .setEnergyCost(150000)  // 150k RF
                    .setProcessingTime(300) // 15秒
                    .setRarity(EnumRarity.UNCOMMON)
                    .build();
                registerRecipe(gearRecipe);
            } catch (Exception e) {
                System.err.println("[Printer] 注册齿轮配方失败: " + e.getMessage());
            }
        }

        // ========== 2. 远古核心碎片 (Ancient Core Fragment) ==========
        // 远古科技的核心组件
        if (ModItems.ANCIENT_CORE_FRAGMENT != null) {
            try {
                PrinterRecipe coreRecipe = new PrinterRecipe.Builder()
                    .setTemplateId("ancient_core_fragment")
                    .setDisplayName("远古核心碎片模板")
                    .setOutput(new ItemStack(ModItems.ANCIENT_CORE_FRAGMENT, 1))
                    .addMaterial(new ItemStack(Items.DIAMOND, 2))
                    .addMaterial(new ItemStack(Items.EMERALD, 2))
                    .addMaterial(new ItemStack(Items.ENDER_PEARL, 4))
                    .addMaterial(new ItemStack(Items.BLAZE_POWDER, 8))
                    .setEnergyCost(250000)  // 250k RF
                    .setProcessingTime(400) // 20秒
                    .setRarity(EnumRarity.RARE)
                    .build();
                registerRecipe(coreRecipe);
            } catch (Exception e) {
                System.err.println("[Printer] 注册远古核心配方失败: " + e.getMessage());
            }
        }

        // ========== 3. 稀有水晶 (Rare Crystal) ==========
        // 蕴含能量的稀有水晶
        if (ModMaterialItems.RARE_CRYSTAL != null) {
            try {
                PrinterRecipe crystalRecipe = new PrinterRecipe.Builder()
                    .setTemplateId("rare_crystal")
                    .setDisplayName("稀有水晶模板")
                    .setOutput(new ItemStack(ModMaterialItems.RARE_CRYSTAL, 2))
                    .addMaterial(new ItemStack(Items.QUARTZ, 16))
                    .addMaterial(new ItemStack(Items.GLOWSTONE_DUST, 8))
                    .addMaterial(new ItemStack(Items.PRISMARINE_CRYSTALS, 4))
                    .setEnergyCost(120000)  // 120k RF
                    .setProcessingTime(240) // 12秒
                    .setRarity(EnumRarity.UNCOMMON)
                    .build();
                registerRecipe(crystalRecipe);
            } catch (Exception e) {
                System.err.println("[Printer] 注册稀有水晶配方失败: " + e.getMessage());
            }
        }

        // ========== 4. 裂隙水晶 (Rift Crystal) ==========
        // 次元能量的结晶
        if (ModItems.RIFT_CRYSTAL != null) {
            try {
                PrinterRecipe riftRecipe = new PrinterRecipe.Builder()
                    .setTemplateId("rift_crystal")
                    .setDisplayName("裂隙水晶模板")
                    .setOutput(new ItemStack(ModItems.RIFT_CRYSTAL, 1))
                    .addMaterial(new ItemStack(Items.ENDER_PEARL, 8))
                    .addMaterial(new ItemStack(Items.NETHER_STAR, 1))
                    .addMaterial(new ItemStack(Blocks.OBSIDIAN, 4))
                    .setEnergyCost(500000)  // 500k RF
                    .setProcessingTime(600) // 30秒
                    .setRarity(EnumRarity.EPIC)
                    .build();
                registerRecipe(riftRecipe);
            } catch (Exception e) {
                System.err.println("[Printer] 注册裂隙水晶配方失败: " + e.getMessage());
            }
        }

        System.out.println("[Printer] 预设配方注册完成，共 " + RECIPES.size() + " 个配方");
    }
}
