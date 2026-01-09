package com.moremod.compat.crafttweaker;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * 宝石词条提取系统
 * 
 * 功能：
 * 1. 提取 - 从宝石中提取单个词条生成精炼宝石
 * 2. 提纯 - 合成多个相同词条提升品质
 * 3. 转移 - 将词条从一个宝石转移到另一个
 * 4. 重铸 - 重新随机数值（保持类型）
 * 5. 分解 - 将多词条宝石分解为多个单词条宝石
 */
public class GemExtractionHelper {
    
    private static final String TAG_REFINED = "Refined";        // 标记精炼宝石
    private static final String TAG_EXTRACT_COUNT = "ExtractCount";  // 提取次数
    
    // ==========================================
    // 词条提取
    // ==========================================
    
    /**
     * 从宝石中提取指定词条，生成精炼宝石
     * 
     * @param sourceGem 源宝石（已鉴定）
     * @param affixIndex 词条索引（0开始）
     * @return 精炼宝石（只包含该词条）
     */
    public static ItemStack extractAffix(ItemStack sourceGem, int affixIndex) {
        if (!GemNBTHelper.isIdentified(sourceGem)) {
            return ItemStack.EMPTY;
        }
        
        List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(sourceGem);
        
        if (affixIndex < 0 || affixIndex >= affixes.size()) {
            return ItemStack.EMPTY;
        }
        
        IdentifiedAffix targetAffix = affixes.get(affixIndex);
        int gemLevel = GemNBTHelper.getGemLevel(sourceGem);
        
        // 创建只包含该词条的精炼宝石
        List<IdentifiedAffix> refinedAffixes = new ArrayList<>();
        refinedAffixes.add(targetAffix);
        
        ItemStack refinedGem = GemNBTHelper.createIdentifiedGem(
            sourceGem.copy(),
            refinedAffixes,
            gemLevel
        );
        
        // 标记为精炼宝石
        markAsRefined(refinedGem, targetAffix);
        
        return refinedGem;
    }
    
    /**
     * 分解宝石 - 将多词条宝石分解为多个单词条宝石
     * 
     * @param sourceGem 源宝石
     * @return 分解后的精炼宝石列表
     */
    public static List<ItemStack> decomposeGem(ItemStack sourceGem) {
        List<ItemStack> results = new ArrayList<>();
        
        if (!GemNBTHelper.isIdentified(sourceGem)) {
            return results;
        }
        
        List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(sourceGem);
        int gemLevel = GemNBTHelper.getGemLevel(sourceGem);
        
        for (IdentifiedAffix affix : affixes) {
            List<IdentifiedAffix> singleAffix = new ArrayList<>();
            singleAffix.add(affix);
            
            ItemStack refinedGem = GemNBTHelper.createIdentifiedGem(
                sourceGem.copy(),
                singleAffix,
                gemLevel
            );
            
            markAsRefined(refinedGem, affix);
            results.add(refinedGem);
        }
        
        return results;
    }
    
    /**
     * 标记为精炼宝石
     */
    private static void markAsRefined(ItemStack gem, IdentifiedAffix affix) {
        NBTTagCompound nbt = gem.getOrCreateSubCompound("GemData");
        nbt.setBoolean(TAG_REFINED, true);
        nbt.setInteger(TAG_EXTRACT_COUNT, 1);
        
        // 更新显示名称
        String qualityColor = affix.getQualityColor();
        gem.setStackDisplayName(qualityColor + "§l精炼宝石 §7[" + affix.getAffix().getDisplayName() + "]");
    }
    
    /**
     * 检查是否为精炼宝石
     */
    public static boolean isRefined(ItemStack gem) {
        if (!gem.hasTagCompound()) return false;
        NBTTagCompound nbt = gem.getTagCompound();
        if (!nbt.hasKey("GemData")) return false;
        return nbt.getCompoundTag("GemData").getBoolean(TAG_REFINED);
    }
    
    // ==========================================
    // 词条提纯
    // ==========================================
    
    /**
     * 提纯词条 - 合成多个相同词条，提升品质
     * 
     * 规则：
     * 1. 所有宝石必须是相同的词条类型
     * 2. 新品质 = max(所有品质) + 随机(5-15)
     * 3. 新数值 = 基于新品质重新计算
     * 4. 最多提升到100%品质
     * 
     * @param gems 要提纯的宝石列表（2-5个）
     * @return 提纯后的宝石
     */
    public static ItemStack purifyAffixes(List<ItemStack> gems) {
        if (gems.size() < 2 || gems.size() > 5) {
            return ItemStack.EMPTY;
        }
        
        // 检查所有宝石是否为相同词条
        IdentifiedAffix firstAffix = null;
        int maxQuality = 0;
        int totalQuality = 0;
        int maxLevel = 0;
        
        for (ItemStack gem : gems) {
            if (!GemNBTHelper.isIdentified(gem)) {
                return ItemStack.EMPTY;
            }
            
            List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
            if (affixes.size() != 1) {
                return ItemStack.EMPTY;  // 必须是单词条宝石
            }
            
            IdentifiedAffix affix = affixes.get(0);
            
            if (firstAffix == null) {
                firstAffix = affix;
            } else {
                // 检查词条ID是否相同
                if (!firstAffix.getAffix().getId().equals(affix.getAffix().getId())) {
                    return ItemStack.EMPTY;  // 词条类型不同
                }
            }
            
            maxQuality = Math.max(maxQuality, affix.getQuality());
            totalQuality += affix.getQuality();
            maxLevel = Math.max(maxLevel, GemNBTHelper.getGemLevel(gem));
        }
        
        // 计算新品质
        int qualityBonus = 5 + (int)(Math.random() * 11);  // 5-15
        int baseBonus = gems.size() * 2;  // 每个宝石+2%
        int newQuality = Math.min(100, maxQuality + qualityBonus + baseBonus);
        
        // 根据新品质计算新数值
        GemAffix affixDef = firstAffix.getAffix();
        float range = affixDef.getMaxValue() - affixDef.getMinValue();
        float newValue = affixDef.getMinValue() + (range * newQuality / 100.0f);
        
        // 创建新词条
        IdentifiedAffix purifiedAffix = new IdentifiedAffix(
            affixDef,
            newValue,
            newQuality
        );
        
        // 创建新宝石
        List<IdentifiedAffix> newAffixes = new ArrayList<>();
        newAffixes.add(purifiedAffix);
        
        ItemStack purifiedGem = GemNBTHelper.createIdentifiedGem(
            gems.get(0).copy(),
            newAffixes,
            maxLevel
        );
        
        // 标记为精炼+提纯
        NBTTagCompound nbt = purifiedGem.getOrCreateSubCompound("GemData");
        nbt.setBoolean(TAG_REFINED, true);
        nbt.setInteger(TAG_EXTRACT_COUNT, gems.size());
        
        // 更新显示名称
        String qualityColor = purifiedAffix.getQualityColor();
        purifiedGem.setStackDisplayName(
            qualityColor + "§l★精炼宝石 §7[" + affixDef.getDisplayName() + "] §6+" + (newQuality - maxQuality) + "%"
        );
        
        return purifiedGem;
    }
    
    /**
     * 简化版：提纯2个宝石
     */
    public static ItemStack purifyTwo(ItemStack gem1, ItemStack gem2) {
        List<ItemStack> gems = new ArrayList<>();
        gems.add(gem1);
        gems.add(gem2);
        return purifyAffixes(gems);
    }
    
    /**
     * 简化版：提纯3个宝石
     */
    public static ItemStack purifyThree(ItemStack gem1, ItemStack gem2, ItemStack gem3) {
        List<ItemStack> gems = new ArrayList<>();
        gems.add(gem1);
        gems.add(gem2);
        gems.add(gem3);
        return purifyAffixes(gems);
    }
    
    // ==========================================
    // 词条转移
    // ==========================================
    
    /**
     * 将词条从源宝石转移到目标宝石
     * 
     * 规则：
     * 1. 源宝石必须是精炼宝石（单词条）
     * 2. 目标宝石词条数不能超过最大值
     * 3. 目标宝石不能已有相同类型的词条
     * 
     * @param sourceGem 源宝石（精炼宝石）
     * @param targetGem 目标宝石
     * @return 转移后的目标宝石
     */
    public static ItemStack transferAffix(ItemStack sourceGem, ItemStack targetGem) {
        if (!GemNBTHelper.isIdentified(sourceGem) || !GemNBTHelper.isIdentified(targetGem)) {
            return ItemStack.EMPTY;
        }
        
        // 源宝石必须是单词条
        List<IdentifiedAffix> sourceAffixes = GemNBTHelper.getAffixes(sourceGem);
        if (sourceAffixes.size() != 1) {
            return ItemStack.EMPTY;
        }
        
        IdentifiedAffix transferAffix = sourceAffixes.get(0);
        
        // 检查目标宝石
        List<IdentifiedAffix> targetAffixes = GemNBTHelper.getAffixes(targetGem);
        
        // 检查是否已有相同类型词条
        for (IdentifiedAffix affix : targetAffixes) {
            if (affix.getAffix().getId().equals(transferAffix.getAffix().getId())) {
                return ItemStack.EMPTY;  // 已有相同类型
            }
        }
        
        // 检查词条数量限制
        if (targetAffixes.size() >= GemLootRuleManager.getMaxAffixes()) {
            return ItemStack.EMPTY;
        }
        
        // 添加新词条
        List<IdentifiedAffix> newAffixes = new ArrayList<>(targetAffixes);
        newAffixes.add(transferAffix);
        
        // 创建新宝石
        int gemLevel = GemNBTHelper.getGemLevel(targetGem);
        ItemStack newGem = GemNBTHelper.createIdentifiedGem(
            targetGem.copy(),
            newAffixes,
            gemLevel
        );
        
        return newGem;
    }
    
    // ==========================================
    // 词条重铸
    // ==========================================
    
    /**
     * 重铸词条 - 重新随机数值，但保持类型和品质范围
     * 
     * @param gem 要重铸的宝石（精炼宝石）
     * @param minQuality 品质下限（0.0-1.0）
     * @param rerollCount 重roll次数
     * @return 重铸后的宝石
     */
    public static ItemStack reforgeAffix(ItemStack gem, float minQuality, int rerollCount) {
        if (!GemNBTHelper.isIdentified(gem)) {
            return ItemStack.EMPTY;
        }
        
        List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
        if (affixes.size() != 1) {
            return ItemStack.EMPTY;  // 只能重铸单词条宝石
        }
        
        IdentifiedAffix oldAffix = affixes.get(0);
        GemAffix affixDef = oldAffix.getAffix();
        
        // 重新roll数值
        float min = affixDef.getMinValue();
        float max = affixDef.getMaxValue();
        float range = max - min;
        float adjustedMin = min + (range * minQuality);
        
        float bestValue = adjustedMin;
        for (int i = 0; i < rerollCount; i++) {
            float rolled = adjustedMin + (float)(Math.random() * (max - adjustedMin));
            if (rolled > bestValue) {
                bestValue = rolled;
            }
        }
        
        // 创建新词条
        IdentifiedAffix newAffix = new IdentifiedAffix(affixDef, bestValue);
        
        List<IdentifiedAffix> newAffixes = new ArrayList<>();
        newAffixes.add(newAffix);
        
        int gemLevel = GemNBTHelper.getGemLevel(gem);
        ItemStack newGem = GemNBTHelper.createIdentifiedGem(
            gem.copy(),
            newAffixes,
            gemLevel
        );
        
        markAsRefined(newGem, newAffix);
        
        return newGem;
    }
    
    // ==========================================
    // 批量操作
    // ==========================================
    
    /**
     * 批量提取 - 提取武器上所有镶嵌宝石的所有词条
     * 
     * @param weapon 武器
     * @return 提取出的所有精炼宝石
     */
    public static List<ItemStack> extractAllFromWeapon(ItemStack weapon) {
        List<ItemStack> results = new ArrayList<>();
        
        if (!GemSocketHelper.hasSocketedGems(weapon)) {
            return results;
        }
        
        ItemStack[] gems = GemSocketHelper.getAllSocketedGems(weapon);
        
        for (ItemStack gem : gems) {
            if (GemNBTHelper.isIdentified(gem)) {
                List<ItemStack> decomposed = decomposeGem(gem);
                results.addAll(decomposed);
            }
        }
        
        return results;
    }
    
    /**
     * 获取提取次数
     */
    public static int getExtractCount(ItemStack gem) {
        if (!gem.hasTagCompound()) return 0;
        NBTTagCompound nbt = gem.getTagCompound();
        if (!nbt.hasKey("GemData")) return 0;
        return nbt.getCompoundTag("GemData").getInteger(TAG_EXTRACT_COUNT);
    }
    
    // ==========================================
    // 工具方法
    // ==========================================
    
    /**
     * 检查两个词条是否相同类型
     */
    public static boolean isSameAffixType(ItemStack gem1, ItemStack gem2) {
        if (!GemNBTHelper.isIdentified(gem1) || !GemNBTHelper.isIdentified(gem2)) {
            return false;
        }
        
        List<IdentifiedAffix> affixes1 = GemNBTHelper.getAffixes(gem1);
        List<IdentifiedAffix> affixes2 = GemNBTHelper.getAffixes(gem2);
        
        if (affixes1.size() != 1 || affixes2.size() != 1) {
            return false;
        }
        
        return affixes1.get(0).getAffix().getId().equals(affixes2.get(0).getAffix().getId());
    }
    
    /**
     * 获取词条类型名称
     */
    public static String getAffixTypeName(ItemStack gem) {
        if (!GemNBTHelper.isIdentified(gem)) {
            return "未知";
        }
        
        List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
        if (affixes.isEmpty()) {
            return "无词条";
        }
        
        if (affixes.size() == 1) {
            return affixes.get(0).getAffix().getDisplayName();
        }
        
        return "混合词条 ×" + affixes.size();
    }
    
    /**
     * 检查宝石是否已有指定类型的词条（通过词条ID检查）
     * 
     * @param gem 要检查的宝石
     * @param affixId 词条ID
     * @return 是否已有该类型词条
     */
    public static boolean hasAffixById(ItemStack gem, String affixId) {
        if (!GemNBTHelper.isIdentified(gem)) {
            return false;
        }
        
        List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
        for (IdentifiedAffix affix : affixes) {
            if (affix.getAffix().getId().equals(affixId)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查宝石是否已有指定类型的词条（通过词条显示名称检查）
     * 
     * @param gem 要检查的宝石
     * @param affixType 词条类型名称（显示名称）
     * @return 是否已有该类型词条
     */
    public static boolean hasAffixType(ItemStack gem, String affixType) {
        if (!GemNBTHelper.isIdentified(gem)) {
            return false;
        }
        
        if (affixType == null || affixType.isEmpty()) {
            return false;
        }
        
        List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
        for (IdentifiedAffix affix : affixes) {
            String displayName = affix.getAffix().getDisplayName();
            // 支持完全匹配或包含匹配
            if (displayName.equals(affixType) || displayName.contains(affixType)) {
                return true;
            }
            // 也检查ID匹配
            if (affix.getAffix().getId().equals(affixType)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取宝石中指定类型的词条
     * 
     * @param gem 宝石
     * @param affixId 词条ID
     * @return 词条对象，如果不存在返回null
     */
    public static IdentifiedAffix getAffixById(ItemStack gem, String affixId) {
        if (!GemNBTHelper.isIdentified(gem)) {
            return null;
        }
        
        List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
        for (IdentifiedAffix affix : affixes) {
            if (affix.getAffix().getId().equals(affixId)) {
                return affix;
            }
        }
        
        return null;
    }
    
    /**
     * 预测提纯结果品质
     */
    public static int predictPurifyQuality(List<ItemStack> gems) {
        if (gems.isEmpty()) return 0;
        
        int maxQuality = 0;
        for (ItemStack gem : gems) {
            if (GemNBTHelper.isIdentified(gem)) {
                List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
                if (!affixes.isEmpty()) {
                    maxQuality = Math.max(maxQuality, affixes.get(0).getQuality());
                }
            }
        }
        
        int bonus = 5 + 10 + (gems.size() * 2);  // 平均15 + 每个宝石2%
        return Math.min(100, maxQuality + bonus);
    }
}