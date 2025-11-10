package com.moremod.compat.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * CraftTweaker API - 宝石词条提取/提纯系统
 * 
 * 使用示例：
 * ```zenscript
 * import mods.moremod.GemExtraction;
 * 
 * // 提取词条
 * var refined = GemExtraction.extract(<moremod:gem>, 0);  // 提取第0个词条
 * 
 * // 分解宝石
 * var refined = GemExtraction.decompose(<moremod:gem>);  // 返回所有单词条宝石
 * 
 * // 提纯词条（2个宝石）
 * var upgraded = GemExtraction.purify(<moremod:gem1>, <moremod:gem2>);
 * 
 * // 提纯词条（3个宝石）
 * var upgraded = GemExtraction.purify3(<moremod:gem1>, <moremod:gem2>, <moremod:gem3>);
 * 
 * // 转移词条
 * var result = GemExtraction.transfer(<moremod:refined_gem>, <moremod:target_gem>);
 * 
 * // 重铸词条
 * var reforged = GemExtraction.reforge(<moremod:gem>, 0.5, 3);  // 50%品质下限，roll 3次
 * 
 * // 检查
 * if (GemExtraction.isRefined(<moremod:gem>)) {
 *     print("这是精炼宝石");
 * }
 * ```
 */
@ZenRegister
@ZenClass("mods.moremod.GemExtraction")
public class CTGemExtraction {
    
    // ==========================================
    // ⭐ 词条提取
    // ==========================================
    
    /**
     * 从宝石中提取指定词条
     * 
     * @param gem 源宝石（已鉴定）
     * @param affixIndex 词条索引（0开始）
     * @return 精炼宝石（只包含该词条）
     * 
     * 示例：
     * var refined = GemExtraction.extract(<moremod:gem>, 0);
     */
    @ZenMethod
    public static IItemStack extract(IItemStack gem, int affixIndex) {
        if (gem == null) {
            CraftTweakerAPI.logError("[GemExtraction] 宝石为null");
            return null;
        }
        
        ItemStack mcGem = CraftTweakerMC.getItemStack(gem);
        ItemStack result = GemExtractionHelper.extractAffix(mcGem, affixIndex);
        
        if (result.isEmpty()) {
            CraftTweakerAPI.logWarning("[GemExtraction] 提取失败：索引" + affixIndex);
            return null;
        }
        
        CraftTweakerAPI.logInfo("[GemExtraction] ✓ 提取成功：" + 
                               GemExtractionHelper.getAffixTypeName(result));
        
        return CraftTweakerMC.getIItemStack(result);
    }
    
    /**
     * 分解宝石 - 将多词条宝石分解为多个单词条宝石
     * 
     * @param gem 源宝石
     * @return 分解后的精炼宝石数组
     * 
     * 示例：
     * var refined = GemExtraction.decompose(<moremod:gem>);
     * for r in refined {
     *     print(r.displayName);
     * }
     */
    @ZenMethod
    public static IItemStack[] decompose(IItemStack gem) {
        if (gem == null) {
            CraftTweakerAPI.logError("[GemExtraction] 宝石为null");
            return new IItemStack[0];
        }
        
        ItemStack mcGem = CraftTweakerMC.getItemStack(gem);
        List<ItemStack> results = GemExtractionHelper.decomposeGem(mcGem);
        
        if (results.isEmpty()) {
            CraftTweakerAPI.logWarning("[GemExtraction] 分解失败");
            return new IItemStack[0];
        }
        
        IItemStack[] array = new IItemStack[results.size()];
        for (int i = 0; i < results.size(); i++) {
            array[i] = CraftTweakerMC.getIItemStack(results.get(i));
        }
        
        CraftTweakerAPI.logInfo("[GemExtraction] ✓ 分解成功：" + results.size() + "个精炼宝石");
        
        return array;
    }
    
    // ==========================================
    // ⭐ 词条提纯
    // ==========================================
    
    /**
     * 提纯2个宝石（相同词条）
     * 
     * @param gem1 宝石1
     * @param gem2 宝石2
     * @return 提纯后的宝石
     * 
     * 示例：
     * var upgraded = GemExtraction.purify(<moremod:gem1>, <moremod:gem2>);
     */
    @ZenMethod
    public static IItemStack purify(IItemStack gem1, IItemStack gem2) {
        if (gem1 == null || gem2 == null) {
            CraftTweakerAPI.logError("[GemExtraction] 宝石为null");
            return null;
        }
        
        ItemStack mc1 = CraftTweakerMC.getItemStack(gem1);
        ItemStack mc2 = CraftTweakerMC.getItemStack(gem2);
        
        ItemStack result = GemExtractionHelper.purifyTwo(mc1, mc2);
        
        if (result.isEmpty()) {
            CraftTweakerAPI.logWarning("[GemExtraction] 提纯失败：词条类型不匹配或不是单词条宝石");
            return null;
        }
        
        CraftTweakerAPI.logInfo("[GemExtraction] ✓ 提纯成功（2个宝石）");
        
        return CraftTweakerMC.getIItemStack(result);
    }
    
    /**
     * 提纯3个宝石（相同词条）
     * 
     * @param gem1 宝石1
     * @param gem2 宝石2
     * @param gem3 宝石3
     * @return 提纯后的宝石
     * 
     * 示例：
     * var upgraded = GemExtraction.purify3(<moremod:gem1>, <moremod:gem2>, <moremod:gem3>);
     */
    @ZenMethod
    public static IItemStack purify3(IItemStack gem1, IItemStack gem2, IItemStack gem3) {
        if (gem1 == null || gem2 == null || gem3 == null) {
            CraftTweakerAPI.logError("[GemExtraction] 宝石为null");
            return null;
        }
        
        ItemStack mc1 = CraftTweakerMC.getItemStack(gem1);
        ItemStack mc2 = CraftTweakerMC.getItemStack(gem2);
        ItemStack mc3 = CraftTweakerMC.getItemStack(gem3);
        
        ItemStack result = GemExtractionHelper.purifyThree(mc1, mc2, mc3);
        
        if (result.isEmpty()) {
            CraftTweakerAPI.logWarning("[GemExtraction] 提纯失败");
            return null;
        }
        
        CraftTweakerAPI.logInfo("[GemExtraction] ✓ 提纯成功（3个宝石）");
        
        return CraftTweakerMC.getIItemStack(result);
    }
    
    /**
     * 高级提纯 - 支持2-5个宝石
     * 
     * @param gems 宝石数组
     * @return 提纯后的宝石
     * 
     * 示例：
     * var upgraded = GemExtraction.purifyAdvanced([gem1, gem2, gem3, gem4]);
     */
    @ZenMethod
    public static IItemStack purifyAdvanced(IItemStack[] gems) {
        if (gems == null || gems.length < 2 || gems.length > 5) {
            CraftTweakerAPI.logError("[GemExtraction] 宝石数量必须在2-5之间");
            return null;
        }
        
        List<ItemStack> mcGems = new ArrayList<>();
        for (IItemStack gem : gems) {
            if (gem != null) {
                mcGems.add(CraftTweakerMC.getItemStack(gem));
            }
        }
        
        ItemStack result = GemExtractionHelper.purifyAffixes(mcGems);
        
        if (result.isEmpty()) {
            CraftTweakerAPI.logWarning("[GemExtraction] 提纯失败");
            return null;
        }
        
        CraftTweakerAPI.logInfo("[GemExtraction] ✓ 提纯成功（" + mcGems.size() + "个宝石）");
        
        return CraftTweakerMC.getIItemStack(result);
    }
    
    // ==========================================
    // ⭐ 词条转移
    // ==========================================
    
    /**
     * 将词条从源宝石转移到目标宝石
     * 
     * @param sourceGem 源宝石（必须是精炼宝石/单词条）
     * @param targetGem 目标宝石
     * @return 转移后的目标宝石
     * 
     * 示例：
     * var result = GemExtraction.transfer(<moremod:refined_gem>, <moremod:target_gem>);
     */
    @ZenMethod
    public static IItemStack transfer(IItemStack sourceGem, IItemStack targetGem) {
        if (sourceGem == null || targetGem == null) {
            CraftTweakerAPI.logError("[GemExtraction] 宝石为null");
            return null;
        }
        
        ItemStack mcSource = CraftTweakerMC.getItemStack(sourceGem);
        ItemStack mcTarget = CraftTweakerMC.getItemStack(targetGem);
        
        ItemStack result = GemExtractionHelper.transferAffix(mcSource, mcTarget);
        
        if (result.isEmpty()) {
            CraftTweakerAPI.logWarning("[GemExtraction] 转移失败：源宝石不是单词条，或目标已有相同类型词条");
            return null;
        }
        
        CraftTweakerAPI.logInfo("[GemExtraction] ✓ 转移成功");
        
        return CraftTweakerMC.getIItemStack(result);
    }
    
    // ==========================================
    // ⭐ 词条重铸
    // ==========================================
    
    /**
     * 重铸词条 - 重新随机数值（保持类型）
     * 
     * @param gem 要重铸的宝石（单词条）
     * @param minQuality 品质下限（0.0-1.0）
     * @param rerollCount 重roll次数
     * @return 重铸后的宝石
     * 
     * 示例：
     * var reforged = GemExtraction.reforge(<moremod:gem>, 0.5, 3);
     */
    @ZenMethod
    public static IItemStack reforge(IItemStack gem, double minQuality, int rerollCount) {
        if (gem == null) {
            CraftTweakerAPI.logError("[GemExtraction] 宝石为null");
            return null;
        }
        
        ItemStack mcGem = CraftTweakerMC.getItemStack(gem);
        ItemStack result = GemExtractionHelper.reforgeAffix(
            mcGem,
            (float)minQuality,
            rerollCount
        );
        
        if (result.isEmpty()) {
            CraftTweakerAPI.logWarning("[GemExtraction] 重铸失败：不是单词条宝石");
            return null;
        }
        
        CraftTweakerAPI.logInfo("[GemExtraction] ✓ 重铸成功");
        
        return CraftTweakerMC.getIItemStack(result);
    }
    
    /**
     * 简化重铸 - 使用默认参数
     * 
     * 示例：
     * var reforged = GemExtraction.reforgeSimple(<moremod:gem>);
     */
    @ZenMethod
    public static IItemStack reforgeSimple(IItemStack gem) {
        return reforge(gem, 0.0, 1);
    }
    
    // ==========================================
    // 🔍 查询方法
    // ==========================================
    
    /**
     * 检查是否为精炼宝石
     * 
     * 示例：
     * if (GemExtraction.isRefined(<moremod:gem>)) {
     *     print("这是精炼宝石");
     * }
     */
    @ZenMethod
    public static boolean isRefined(IItemStack gem) {
        if (gem == null) return false;
        ItemStack mcGem = CraftTweakerMC.getItemStack(gem);
        return GemExtractionHelper.isRefined(mcGem);
    }
    
    /**
     * 检查两个宝石是否为相同词条类型
     * 
     * 示例：
     * if (GemExtraction.isSameType(<moremod:gem1>, <moremod:gem2>)) {
     *     print("可以提纯");
     * }
     */
    @ZenMethod
    public static boolean isSameType(IItemStack gem1, IItemStack gem2) {
        if (gem1 == null || gem2 == null) return false;
        
        ItemStack mc1 = CraftTweakerMC.getItemStack(gem1);
        ItemStack mc2 = CraftTweakerMC.getItemStack(gem2);
        
        return GemExtractionHelper.isSameAffixType(mc1, mc2);
    }
    
    /**
     * 获取词条类型名称
     * 
     * 示例：
     * var name = GemExtraction.getAffixName(<moremod:gem>);
     */
    @ZenMethod
    public static String getAffixName(IItemStack gem) {
        if (gem == null) return "未知";
        ItemStack mcGem = CraftTweakerMC.getItemStack(gem);
        return GemExtractionHelper.getAffixTypeName(mcGem);
    }
    
    /**
     * 获取提取次数
     * 
     * 示例：
     * var count = GemExtraction.getExtractCount(<moremod:gem>);
     */
    @ZenMethod
    public static int getExtractCount(IItemStack gem) {
        if (gem == null) return 0;
        ItemStack mcGem = CraftTweakerMC.getItemStack(gem);
        return GemExtractionHelper.getExtractCount(mcGem);
    }
    
    /**
     * 预测提纯结果品质
     * 
     * 示例：
     * var quality = GemExtraction.predictQuality([gem1, gem2, gem3]);
     */
    @ZenMethod
    public static int predictQuality(IItemStack[] gems) {
        if (gems == null || gems.length == 0) return 0;
        
        List<ItemStack> mcGems = new ArrayList<>();
        for (IItemStack gem : gems) {
            if (gem != null) {
                mcGems.add(CraftTweakerMC.getItemStack(gem));
            }
        }
        
        return GemExtractionHelper.predictPurifyQuality(mcGems);
    }
    
    // ==========================================
    // 🔧 批量操作
    // ==========================================
    
    /**
     * 从武器提取所有词条
     * 
     * @param weapon 武器
     * @return 所有提取的精炼宝石
     * 
     * 示例：
     * var refined = GemExtraction.extractFromWeapon(<minecraft:diamond_sword>);
     */
    @ZenMethod
    public static IItemStack[] extractFromWeapon(IItemStack weapon) {
        if (weapon == null) {
            CraftTweakerAPI.logError("[GemExtraction] 武器为null");
            return new IItemStack[0];
        }
        
        ItemStack mcWeapon = CraftTweakerMC.getItemStack(weapon);
        List<ItemStack> results = GemExtractionHelper.extractAllFromWeapon(mcWeapon);
        
        if (results.isEmpty()) {
            CraftTweakerAPI.logWarning("[GemExtraction] 武器上没有镶嵌宝石");
            return new IItemStack[0];
        }
        
        IItemStack[] array = new IItemStack[results.size()];
        for (int i = 0; i < results.size(); i++) {
            array[i] = CraftTweakerMC.getIItemStack(results.get(i));
        }
        
        CraftTweakerAPI.logInfo("[GemExtraction] ✓ 提取成功：" + results.size() + "个精炼宝石");
        
        return array;
    }
    
    // ==========================================
    // 📊 调试工具
    // ==========================================
    
    /**
     * 输出宝石详细信息
     * 
     * 示例：
     * GemExtraction.debug(<moremod:gem>);
     */
    @ZenMethod
    public static void debug(IItemStack gem) {
        if (gem == null) {
            CraftTweakerAPI.logInfo("[GemExtraction] 宝石为null");
            return;
        }
        
        ItemStack mcGem = CraftTweakerMC.getItemStack(gem);
        
        CraftTweakerAPI.logInfo("========== 宝石信息 ==========");
        CraftTweakerAPI.logInfo("名称: " + gem.getDisplayName());
        CraftTweakerAPI.logInfo("是否精炼: " + GemExtractionHelper.isRefined(mcGem));
        CraftTweakerAPI.logInfo("提取次数: " + GemExtractionHelper.getExtractCount(mcGem));
        CraftTweakerAPI.logInfo("词条类型: " + GemExtractionHelper.getAffixTypeName(mcGem));
        CraftTweakerAPI.logInfo("===============================");
    }
}
