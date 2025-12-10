package com.moremod.printer;

import com.moremod.init.ModItems;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.*;

/**
 * 打印机配方注册表
 * 管理所有打印配方，支持CraftTweaker动态添加/移除
 */
public class PrinterRecipeRegistry {

    private static final Map<String, PrinterRecipe> RECIPES = new LinkedHashMap<>();
    private static boolean initialized = false;

    /**
     * 注册一个配方
     */
    public static void registerRecipe(PrinterRecipe recipe) {
        RECIPES.put(recipe.getTemplateId(), recipe);
        System.out.println("[Printer] 注册配方: " + recipe.getTemplateId() + " -> " + recipe.getOutput().getDisplayName());
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
     * 检查模版ID是否有对应配方
     */
    public static boolean hasRecipe(String templateId) {
        return RECIPES.containsKey(templateId);
    }

    /**
     * 初始化默认配方
     */
    public static void initDefaultRecipes() {
        if (initialized) return;
        initialized = true;

        System.out.println("[Printer] 初始化默认打印配方...");

        // 齿轮打印配方 - 消耗铁锭
        registerRecipe(new PrinterRecipe.Builder()
            .setTemplateId("gear_iron")
            .addMaterial(new ItemStack(Items.IRON_INGOT, 4))
            .setEnergyCost(50000)      // 50k RF
            .setProcessingTime(100)    // 5秒
            .setOutput(new ItemStack(Items.IRON_INGOT)) // 临时输出，后续替换为齿轮
            .build());

        // 远古组件打印配方 - 消耗金锭和钻石
        registerRecipe(new PrinterRecipe.Builder()
            .setTemplateId("ancient_component")
            .addMaterial(new ItemStack(Items.GOLD_INGOT, 8))
            .addMaterial(new ItemStack(Items.DIAMOND, 2))
            .addMaterial(new ItemStack(Items.REDSTONE, 16))
            .setEnergyCost(500000)     // 500k RF
            .setProcessingTime(400)    // 20秒
            .setOutput(new ItemStack(Items.NETHER_STAR)) // 临时输出
            .build());

        // 神秘水晶打印配方 - 消耗末影珍珠和萤石粉
        registerRecipe(new PrinterRecipe.Builder()
            .setTemplateId("mystery_crystal")
            .addMaterial(new ItemStack(Items.ENDER_PEARL, 4))
            .addMaterial(new ItemStack(Items.GLOWSTONE_DUST, 8))
            .addMaterial(new ItemStack(Items.QUARTZ, 16))
            .setEnergyCost(200000)     // 200k RF
            .setProcessingTime(200)    // 10秒
            .setOutput(new ItemStack(Blocks.SEA_LANTERN)) // 临时输出
            .build());

        // 时空碎片打印配方 - 高级材料
        registerRecipe(new PrinterRecipe.Builder()
            .setTemplateId("spacetime_shard")
            .addMaterial(new ItemStack(Items.DIAMOND, 4))
            .addMaterial(new ItemStack(Items.EMERALD, 4))
            .addMaterial(new ItemStack(Items.ENDER_EYE, 2))
            .setEnergyCost(1000000)    // 1M RF
            .setProcessingTime(600)    // 30秒
            .setOutput(new ItemStack(Items.NETHER_STAR)) // 临时输出
            .build());

        // 远古核心碎片打印配方
        registerRecipe(new PrinterRecipe.Builder()
            .setTemplateId("ancient_core_fragment")
            .addMaterial(new ItemStack(Items.IRON_INGOT, 16))
            .addMaterial(new ItemStack(Items.GOLD_INGOT, 8))
            .addMaterial(new ItemStack(Items.REDSTONE, 32))
            .setEnergyCost(300000)     // 300k RF
            .setProcessingTime(300)    // 15秒
            .setOutput(new ItemStack(Items.PRISMARINE_CRYSTALS, 4)) // 临时输出
            .build());

        System.out.println("[Printer] 默认配方初始化完成，共 " + RECIPES.size() + " 个配方");
    }

    /**
     * 清除所有配方（用于重新加载）
     */
    public static void clearAllRecipes() {
        RECIPES.clear();
        initialized = false;
        System.out.println("[Printer] 已清除所有配方");
    }
}
