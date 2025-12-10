/**
 * 打印机系统 CraftTweaker 示例脚本
 * Printer System CraftTweaker Example Script
 *
 * 打印模板现在完全由CRT控制，没有预定义的配方。
 * Print templates are now fully controlled by CRT, with no predefined recipes.
 */

// ==================== 基础配方 ====================
// Basic Recipes

// 添加基础打印配方
// addRecipe(templateId, output, energyCost, processingTime, materials)
mods.moremod.Printer.addRecipe("diamond_gear",
    <minecraft:diamond> * 2,
    100000,  // 100k RF
    200,     // 10 seconds (200 ticks)
    [<minecraft:iron_ingot> * 4, <minecraft:gold_ingot> * 2]
);

// ==================== 高级配方 ====================
// Advanced Recipes

// 添加带显示名称和稀有度的高级配方
// addRecipeAdvanced(templateId, displayName, rarity, output, energyCost, processingTime, materials)
// rarity: "common", "uncommon", "rare", "epic"
mods.moremod.Printer.addRecipeAdvanced("ancient_artifact",
    "远古神器模板",  // 显示名称
    "epic",          // 稀有度
    <minecraft:nether_star>,
    1000000,         // 1M RF
    600,             // 30 seconds
    [<minecraft:diamond> * 8, <minecraft:emerald> * 4, <minecraft:ender_eye> * 2]
);

// ==================== 创建模板物品用于合成 ====================
// Create Template Items for Crafting

// 创建带有指定templateId的模板物品
val diamondGearTemplate = mods.moremod.Printer.createTemplate("diamond_gear");
val ancientArtifactTemplate = mods.moremod.Printer.createTemplate("ancient_artifact");

// 使用模板物品定义合成配方
recipes.addShaped("diamond_gear_template", diamondGearTemplate,
    [[<minecraft:paper>, <minecraft:diamond>, <minecraft:paper>],
     [<minecraft:diamond>, <moremod:blank_template>, <minecraft:diamond>],
     [<minecraft:paper>, <minecraft:diamond>, <minecraft:paper>]]
);

recipes.addShaped("ancient_artifact_template", ancientArtifactTemplate,
    [[<minecraft:gold_block>, <minecraft:nether_star>, <minecraft:gold_block>],
     [<minecraft:nether_star>, <moremod:blank_template>, <minecraft:nether_star>],
     [<minecraft:gold_block>, <minecraft:nether_star>, <minecraft:gold_block>]]
);

// ==================== 使用现有材料模板物品 ====================
// Using Existing Material Template Items

// 保留的模板物品: ancient_component, antikythera_gear, blank_template, rare_crystal
// Preserved template items: ancient_component, antikythera_gear, blank_template, rare_crystal

// 为这些现有物品添加打印配方
mods.moremod.Printer.addRecipeAdvanced("antikythera_gear",
    "安提基特拉齿轮模板",
    "rare",
    <moremod:antikythera_gear>,
    500000,
    400,
    [<minecraft:iron_ingot> * 16, <minecraft:gold_ingot> * 8, <minecraft:redstone> * 32]
);

mods.moremod.Printer.addRecipeAdvanced("ancient_component",
    "远古组件模板",
    "rare",
    <moremod:ancient_component>,
    300000,
    300,
    [<minecraft:gold_ingot> * 8, <minecraft:diamond> * 2, <minecraft:redstone> * 16]
);

mods.moremod.Printer.addRecipeAdvanced("rare_crystal",
    "稀有水晶模板",
    "uncommon",
    <moremod:rare_crystal>,
    200000,
    200,
    [<minecraft:quartz> * 16, <minecraft:glowstone_dust> * 8, <minecraft:ender_pearl> * 4]
);

// 为这些配方创建模板物品的合成
val antikytheraTemplate = mods.moremod.Printer.createTemplate("antikythera_gear");
val ancientComponentTemplate = mods.moremod.Printer.createTemplate("ancient_component");
val rareCrystalTemplate = mods.moremod.Printer.createTemplate("rare_crystal");

recipes.addShaped("antikythera_template", antikytheraTemplate,
    [[<minecraft:iron_ingot>, <minecraft:clock>, <minecraft:iron_ingot>],
     [<minecraft:clock>, <moremod:blank_template>, <minecraft:clock>],
     [<minecraft:iron_ingot>, <minecraft:clock>, <minecraft:iron_ingot>]]
);

recipes.addShaped("ancient_component_template", ancientComponentTemplate,
    [[<minecraft:gold_ingot>, <minecraft:redstone>, <minecraft:gold_ingot>],
     [<minecraft:redstone>, <moremod:blank_template>, <minecraft:redstone>],
     [<minecraft:gold_ingot>, <minecraft:redstone>, <minecraft:gold_ingot>]]
);

recipes.addShaped("rare_crystal_template", rareCrystalTemplate,
    [[<minecraft:quartz>, <minecraft:glowstone_dust>, <minecraft:quartz>],
     [<minecraft:glowstone_dust>, <moremod:blank_template>, <minecraft:glowstone_dust>],
     [<minecraft:quartz>, <minecraft:glowstone_dust>, <minecraft:quartz>]]
);

// ==================== 工具方法 ====================
// Utility Methods

// 检查配方是否存在
// if (mods.moremod.Printer.hasRecipe("diamond_gear")) {
//     print("Diamond gear recipe exists!");
// }

// 获取已注册配方数量
// print("Total printer recipes: " + mods.moremod.Printer.getRecipeCount());

// 移除配方
// mods.moremod.Printer.removeRecipe("some_recipe");

// 移除所有配方
// mods.moremod.Printer.removeAllRecipes();

print("[MoreMod] Printer recipes loaded successfully!");
