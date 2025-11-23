package com.moremod.compat.crafttweaker;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 宝石NBT数据管理器（支持品质保底 + 等级加成）
 *
 * NBT结构:
 * {
 *   GemData: {
 *     identified: 0b/1b,
 *     gemLevel: 50,
 *     affixCount: 3,
 *     minQuality: 0.4,         // 品质下限
 *     rerollCount: 2,          // 重roll次数
 *     affixes: [...]
 *   }
 * }
 *
 * ✅ 新特性：宝石等级影响词条品质
 * - 高等级宝石自动提升品质下限
 * - 等级加成与保底叠加
 */
public class GemNBTHelper {

    private static final String TAG_GEM_DATA = "GemData";
    private static final String TAG_IDENTIFIED = "identified";
    private static final String TAG_GEM_LEVEL = "gemLevel";
    private static final String TAG_AFFIX_COUNT = "affixCount";
    private static final String TAG_MIN_QUALITY = "minQuality";
    private static final String TAG_REROLL_COUNT = "rerollCount";
    private static final String TAG_AFFIXES = "affixes";
    private static final String TAG_AFFIX_ID = "id";
    private static final String TAG_AFFIX_VALUE = "value";
    private static final String TAG_AFFIX_QUALITY = "quality";

    private static final Random RANDOM = new Random();

    // ==========================================
    // 创建宝石（新版，支持品质保底）
    // ==========================================

    /**
     * ✅ 新方法：创建带品质保底的未鉴定宝石
     */
    public static ItemStack createUnidentifiedGemWithQuality(
            ItemStack baseGem,
            int gemLevel,
            int affixCount,
            float minQuality,
            int rerollCount
    ) {
        ItemStack gem = baseGem.copy();
        NBTTagCompound nbt = gem.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            gem.setTagCompound(nbt);
        }

        NBTTagCompound gemData = new NBTTagCompound();
        gemData.setBoolean(TAG_IDENTIFIED, false);
        gemData.setInteger(TAG_GEM_LEVEL, gemLevel);
        gemData.setInteger(TAG_AFFIX_COUNT, affixCount);
        gemData.setFloat(TAG_MIN_QUALITY, minQuality);
        gemData.setInteger(TAG_REROLL_COUNT, rerollCount);

        nbt.setTag(TAG_GEM_DATA, gemData);

        // 更新显示名称
        gem.setStackDisplayName(String.format(
                "§6未鉴定的宝石 §7[等级 %d] [%d词条]",
                gemLevel,
                affixCount
        ));

        return gem;
    }

    /**
     * 创建未鉴定的宝石（旧版兼容）
     */
    public static ItemStack createUnidentifiedGem(ItemStack baseGem, int gemLevel, int affixCount) {
        // 默认无品质保底、roll 1次
        return createUnidentifiedGemWithQuality(baseGem, gemLevel, affixCount, 0.0f, 1);
    }

    /**
     * 创建已鉴定的宝石
     */
    public static ItemStack createIdentifiedGem(ItemStack baseGem,
                                                List<IdentifiedAffix> affixes,
                                                int gemLevel) {
        ItemStack gem = baseGem.copy();
        NBTTagCompound nbt = gem.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            gem.setTagCompound(nbt);
        }

        NBTTagCompound gemData = new NBTTagCompound();
        gemData.setBoolean(TAG_IDENTIFIED, true);
        gemData.setInteger(TAG_GEM_LEVEL, gemLevel);

        // 保存词条
        NBTTagList affixList = new NBTTagList();
        for (IdentifiedAffix affix : affixes) {
            NBTTagCompound affixTag = new NBTTagCompound();
            affixTag.setString(TAG_AFFIX_ID, affix.getAffix().getId());
            affixTag.setFloat(TAG_AFFIX_VALUE, affix.getValue());
            affixTag.setInteger(TAG_AFFIX_QUALITY, affix.getQuality());
            affixList.appendTag(affixTag);
        }
        gemData.setTag(TAG_AFFIXES, affixList);

        nbt.setTag(TAG_GEM_DATA, gemData);

        // 更新显示名称和Lore
        updateGemDisplay(gem, affixes, gemLevel);

        return gem;
    }

    // ==========================================
    // 鉴定功能（新版，支持品质保底 + 等级加成）
    // ==========================================

    /**
     * ✅ 鉴定宝石（带品质保底、重roll和等级加成）
     */
    public static ItemStack identifyGem(ItemStack unidentifiedGem) {
        if (!isUnidentified(unidentifiedGem)) {
            return unidentifiedGem;
        }

        NBTTagCompound gemData = getGemData(unidentifiedGem);
        int gemLevel = gemData.getInteger(TAG_GEM_LEVEL);
        int affixCount = gemData.getInteger(TAG_AFFIX_COUNT);
        float minQuality = gemData.getFloat(TAG_MIN_QUALITY);
        int rerollCount = gemData.getInteger(TAG_REROLL_COUNT);

        // 生成词条（带品质保底 + 等级加成）
        List<IdentifiedAffix> affixes = rollAffixesWithQualityFloor(
                affixCount,
                gemLevel,
                minQuality,
                rerollCount
        );

        return createIdentifiedGem(unidentifiedGem, affixes, gemLevel);
    }

    /**
     * ✅ 核心方法：抽取词条并应用品质保底 + 等级加成
     */
    private static List<IdentifiedAffix> rollAffixesWithQualityFloor(
            int count,
            int gemLevel,
            float minQuality,
            int rerollCount
    ) {
        List<IdentifiedAffix> result = new ArrayList<>();

        // 获取可用词条池
        List<GemAffix> availableAffixes = AffixPoolRegistry.getAffixesByLevel(gemLevel);
        if (availableAffixes.isEmpty()) {
            System.err.println("[GemNBT] 警告：等级 " + gemLevel + " 没有可用词条！");
            return result;
        }

        // 防止词条重复
        List<GemAffix> selectedPool = new ArrayList<>(availableAffixes);

        for (int i = 0; i < count && !selectedPool.isEmpty(); i++) {
            // 1. 随机选择词条定义
            GemAffix affix = weightedRandomSelect(selectedPool);
            if (affix == null) break;

            // 2. 应用重roll机制 + 等级加成生成数值
            float rolledValue = rollValueWithReroll(affix, minQuality, rerollCount, gemLevel);

            // 3. 创建已鉴定词条
            IdentifiedAffix identified = new IdentifiedAffix(affix, rolledValue);
            result.add(identified);

            // 4. 移除已选择的词条，防止重复
            selectedPool.remove(affix);

            if (AffixPoolRegistry.isDebugMode()) {
                float levelBonus = calculateLevelBonus(gemLevel);
                System.out.println(String.format(
                        "[GemNBT] 鉴定词条: %s = %.2f (品质: %d%%, 保底: %.0f%%, 等级加成: %.1f%%, roll×%d, Lv.%d)",
                        affix.getId(),
                        rolledValue,
                        identified.getQuality(),
                        minQuality * 100,
                        levelBonus * 100,
                        rerollCount,
                        gemLevel
                ));
            }
        }

        return result;
    }

    /**
     * ✅ 重roll机制 + 等级加成：roll多次取最高值
     */
    /**
     * ✅ 重roll机制 + 等级加成 + Tier上下限限制
     */
    private static float rollValueWithReroll(GemAffix affix, float minQuality, int rerollCount, int gemLevel) {
        float min = affix.getMinValue();
        float max = affix.getMaxValue();
        float range = max - min;

        // 计算等级加成（保持原有逻辑）
        float levelBonus = calculateLevelBonus(gemLevel);

        // 应用品质下限 + 等级加成
        float effectiveMinQuality = Math.min(0.95f, minQuality + levelBonus);

        // ⭐⭐⭐ 新增：应用Tier上下限限制 ⭐⭐⭐
        float[] tierRange = AffixTierMapper.calculateQualityRange(gemLevel);
        effectiveMinQuality = Math.max(effectiveMinQuality, tierRange[0]); // 下限
        float effectiveMaxQuality = Math.min(1.0f, tierRange[1]);          // 上限
        // ⭐⭐⭐ 仅新增4行！ ⭐⭐⭐

        // 计算实际roll范围
        float adjustedMin = min + (range * effectiveMinQuality);
        float adjustedMax = min + (range * effectiveMaxQuality); // ⭐ 使用上限

        if (rerollCount <= 1) {
            // 单次roll（在上下限之间均匀分布）
            return adjustedMin + RANDOM.nextFloat() * (adjustedMax - adjustedMin);
        }

        // 多次roll，取最高值
        float bestValue = adjustedMin;
        for (int i = 0; i < rerollCount; i++) {
            float rolled = adjustedMin + RANDOM.nextFloat() * (adjustedMax - adjustedMin);
            if (rolled > bestValue) {
                bestValue = rolled;
            }
        }

        return bestValue;
    }

    /**
     * ✅ 等级加成计算公式
     *
     * 默认：线性增长，每级 +0.3%
     * - 1级宝石：+0.3%
     * - 50级宝石：+15%
     * - 100级宝石：+30%
     *
     * 可以根据游戏平衡需求调整公式
     */
    private static float calculateLevelBonus(int gemLevel) {
        // ===== 方案1：线性增长（简单直接） =====
        return gemLevel * 0.003f;  // 每级 +0.3%

        // ===== 方案2：分段增长（更平衡，可选） =====
        /*
        if (gemLevel <= 20) {
            return 0.0f;  // 1-20级：无加成
        } else if (gemLevel <= 50) {
            return (gemLevel - 20) * 0.005f;  // 21-50级：0-15%
        } else if (gemLevel <= 80) {
            return 0.15f + (gemLevel - 50) * 0.007f;  // 51-80级：15-36%
        } else {
            return 0.36f + (gemLevel - 80) * 0.003f;  // 81-100级：36-42%
        }
        */

        // ===== 方案3：对数增长（前期快，后期慢） =====
        /*
        return (float)(Math.log(gemLevel + 1) / Math.log(101) * 0.35);  // 最高35%
        */

        // ===== 方案4：幂函数增长（前期慢，后期快） =====
        /*
        return (float)(Math.pow(gemLevel / 100.0, 1.5) * 0.4);  // 最高40%
        */
    }

    /**
     * 加权随机选择词条
     */
    private static GemAffix weightedRandomSelect(List<GemAffix> affixes) {
        if (affixes.isEmpty()) return null;

        int totalWeight = affixes.stream()
                .mapToInt(GemAffix::getWeight)
                .sum();

        if (totalWeight <= 0) return affixes.get(RANDOM.nextInt(affixes.size()));

        int roll = RANDOM.nextInt(totalWeight);
        int current = 0;

        for (GemAffix affix : affixes) {
            current += affix.getWeight();
            if (roll < current) {
                return affix;
            }
        }

        return affixes.get(affixes.size() - 1);
    }

    /**
     * 批量鉴定
     */
    public static List<ItemStack> identifyGems(List<ItemStack> unidentifiedGems) {
        List<ItemStack> identified = new ArrayList<>();
        for (ItemStack gem : unidentifiedGems) {
            identified.add(identifyGem(gem));
        }
        return identified;
    }

    // ==========================================
    // 查询方法
    // ==========================================

    public static boolean isGem(ItemStack stack) {
        return stack != null && !stack.isEmpty() &&
                stack.hasTagCompound() &&
                stack.getTagCompound().hasKey(TAG_GEM_DATA);
    }

    public static boolean isUnidentified(ItemStack stack) {
        if (!isGem(stack)) return false;
        NBTTagCompound gemData = getGemData(stack);
        return !gemData.getBoolean(TAG_IDENTIFIED);
    }

    public static boolean isIdentified(ItemStack stack) {
        if (!isGem(stack)) return false;
        NBTTagCompound gemData = getGemData(stack);
        return gemData.getBoolean(TAG_IDENTIFIED);
    }

    public static int getGemLevel(ItemStack stack) {
        if (!isGem(stack)) return 1;
        return getGemData(stack).getInteger(TAG_GEM_LEVEL);
    }

    public static int getAffixCount(ItemStack stack) {
        if (!isGem(stack)) return 0;
        NBTTagCompound gemData = getGemData(stack);

        if (isUnidentified(stack)) {
            return gemData.getInteger(TAG_AFFIX_COUNT);
        } else {
            return getAffixes(stack).size();
        }
    }

    public static List<IdentifiedAffix> getAffixes(ItemStack stack) {
        List<IdentifiedAffix> affixes = new ArrayList<>();

        if (!isIdentified(stack)) return affixes;

        NBTTagCompound gemData = getGemData(stack);
        NBTTagList affixList = gemData.getTagList(TAG_AFFIXES, Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < affixList.tagCount(); i++) {
            NBTTagCompound affixTag = affixList.getCompoundTagAt(i);
            String id = affixTag.getString(TAG_AFFIX_ID);
            float value = affixTag.getFloat(TAG_AFFIX_VALUE);
            int quality = affixTag.getInteger(TAG_AFFIX_QUALITY);

            GemAffix affix = AffixPoolRegistry.getAffix(id);
            if (affix != null) {
                affixes.add(new IdentifiedAffix(affix, value, quality));
            }
        }

        return affixes;
    }

    private static NBTTagCompound getGemData(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return new NBTTagCompound();
        }
        return stack.getTagCompound().getCompoundTag(TAG_GEM_DATA);
    }

    // ==========================================
    // 显示更新
    // ==========================================

    private static void updateGemDisplay(ItemStack gem, List<IdentifiedAffix> affixes, int gemLevel) {
        // 计算品质
        int avgQuality = affixes.stream()
                .mapToInt(IdentifiedAffix::getQuality)
                .sum() / Math.max(1, affixes.size());

        String qualityColor = getQualityColor(avgQuality);

        // 设置名称
        gem.setStackDisplayName(qualityColor + "宝石 §7[" + affixes.size() + "词条]");

        // 设置Lore
        NBTTagCompound display = gem.getOrCreateSubCompound("display");
        NBTTagList lore = new NBTTagList();

        // 等级信息
        lore.appendTag(new net.minecraft.nbt.NBTTagString("§7等级: §f" + gemLevel));
        lore.appendTag(new net.minecraft.nbt.NBTTagString("§7平均品质: " + qualityColor + avgQuality + "%"));
        lore.appendTag(new net.minecraft.nbt.NBTTagString("")); // 空行

        // 词条列表
        lore.appendTag(new net.minecraft.nbt.NBTTagString("§e▌ 词条效果:"));
        for (IdentifiedAffix affix : affixes) {
            lore.appendTag(new net.minecraft.nbt.NBTTagString("  " + affix.getFullDescription()));
        }

        display.setTag("Lore", lore);
    }

    private static String getQualityColor(int quality) {
        if (quality >= 90) return "§6"; // 金色 - 完美
        if (quality >= 70) return "§d"; // 紫色 - 优秀
        if (quality >= 50) return "§9"; // 蓝色 - 良好
        if (quality >= 30) return "§a"; // 绿色 - 普通
        return "§7"; // 灰色 - 较差
    }
}
