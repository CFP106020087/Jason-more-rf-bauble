/**
 * MoreMod - 材質變化台 CraftTweaker 配方示例
 *
 * 此文件應放置在 minecraft/scripts/ 目錄中
 *
 * 使用方法:
 * 1. 精確配方 (指定輸入劍): addRecipe(輸入劍, 材料, 輸出劍, 經驗消耗)
 * 2. 通用配方 (任意劍): add(材料, 輸出劍, 經驗消耗)
 * 3. 調試命令: dump() - 打印所有配方到日誌
 * 4. 刷新JEI: refreshJEI() - 強制刷新JEI配方顯示
 */

// ========== 通用配方 (任意劍 + 材料 -> 輸出劍) ==========

// 任意劍 + 鑽石 -> 鑽石劍 (消耗10經驗等級)
mods.moremod.SwordUpgradematerial.add(<minecraft:diamond>, <minecraft:diamond_sword>, 10);

// 任意劍 + 下界之星 -> 鑽石劍 (消耗50經驗等級)
mods.moremod.SwordUpgradematerial.add(<minecraft:nether_star>, <minecraft:diamond_sword>, 50);


// ========== 精確配方 (指定輸入劍) ==========

// 鐵劍 + 金錠 -> 金劍 (消耗5經驗等級)
mods.moremod.SwordUpgradematerial.addRecipe(
    <minecraft:iron_sword>,
    <minecraft:gold_ingot>,
    <minecraft:golden_sword>,
    5
);

// 木劍 + 圓石 -> 石劍 (無經驗消耗)
mods.moremod.SwordUpgradematerial.addRecipe(
    <minecraft:wooden_sword>,
    <minecraft:cobblestone>,
    <minecraft:stone_sword>,
    0
);


// ========== 帶NBT的精確配方 ==========

// 只有帶鋒利5附魔的鐵劍才能升級
// mods.moremod.SwordUpgradematerial.addRecipe(
//     <minecraft:iron_sword>.withTag({ench:[{id:16 as short,lvl:5 as short}]}),
//     <minecraft:emerald>,
//     <minecraft:diamond_sword>,
//     30
// );


// ========== 調試和管理 ==========

// 打印所有已註冊的配方 (查看 crafttweaker.log)
mods.moremod.SwordUpgradematerial.dump();

// 獲取配方數量
print("[SwordUpgrade] Total recipes: " + mods.moremod.SwordUpgradematerial.getRecipeCount());

// 注意: 配方會在玩家進入世界時自動顯示在JEI中
// 如果需要手動刷新JEI，可以在遊戲內使用 /ct reload 後調用:
// mods.moremod.SwordUpgradematerial.refreshJEI();
