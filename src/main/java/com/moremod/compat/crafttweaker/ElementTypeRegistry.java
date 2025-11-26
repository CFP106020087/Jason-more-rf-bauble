package com.moremod.compat.crafttweaker;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.*;

/**
 * 元素类型注册中心 - 简化版
 *
 * 修复：
 * 1. 添加 registerMaterial() 方法
 * 2. 添加 getAllElementTypes() 返回 Collection
 * 3. 支持宝石词条系统
 */
public class ElementTypeRegistry {

    private static final Map<String, CustomElementType> ELEMENT_TYPES = new HashMap<>();
    private static final Map<String, String> MATERIAL_TO_ELEMENT = new HashMap<>();
    private static boolean debugMode = false;

    // ==========================================
    // 元素类型注册
    // ==========================================

    public static void registerElementType(CustomElementType elementType) {
        if (elementType == null || elementType.getId() == null) {
            throw new IllegalArgumentException("元素类型或ID不能为空");
        }

        String id = elementType.getId().toLowerCase();
        ELEMENT_TYPES.put(id, elementType);

        if (debugMode) {
            System.out.println("[ElementTypeRegistry] 注册元素类型: " + id +
                    " (" + elementType.getDisplayName() + ")");
        }
    }

    public static CustomElementType getElementType(String id) {
        if (id == null) return null;
        return ELEMENT_TYPES.get(id.toLowerCase());
    }

    public static boolean hasElementType(String id) {
        if (id == null) return false;
        return ELEMENT_TYPES.containsKey(id.toLowerCase());
    }

    /**
     * 获取所有已注册的元素类型（修复：返回Collection用于foreach）
     */
    public static Collection<CustomElementType> getAllElementTypes() {
        return ELEMENT_TYPES.values();
    }

    // ==========================================
    // 材料映射（向后兼容）
    // ==========================================

    /**
     * 注册材料到元素的映射（修复：添加别名方法）
     */
    public static void registerMaterial(String materialId, CustomElementType elementType) {
        if (elementType != null) {
            registerMaterialElement(materialId, elementType.getId());
        }
    }

    public static void registerMaterialElement(String materialId, String elementId) {
        if (materialId == null || elementId == null) return;

        String normalizedElementId = elementId.toLowerCase();
        if (!hasElementType(normalizedElementId)) {
            System.err.println("[ElementTypeRegistry] 警告: 元素类型 " + elementId + " 不存在");
            return;
        }

        String[] formats = getAllFormats(materialId);
        for (String format : formats) {
            MATERIAL_TO_ELEMENT.put(format, normalizedElementId);
        }

        if (debugMode) {
            System.out.println("[ElementTypeRegistry] 映射材料: " + materialId + " → " + elementId);
        }
    }

    private static String[] getAllFormats(String materialId) {
        String lower = materialId.toLowerCase();
        return new String[] {
                lower,
                lower.replace(":", "_"),
                materialId,
                materialId.replace(":", "_")
        };
    }

    public static CustomElementType getElementTypeByMaterial(String materialId) {
        if (materialId == null) return null;

        String[] formats = getAllFormats(materialId);
        for (String format : formats) {
            String elementId = MATERIAL_TO_ELEMENT.get(format);
            if (elementId != null) {
                return getElementType(elementId);
            }
        }
        return null;
    }

    // ==========================================
    // 宝石词条系统（核心）
    // ==========================================

    /**
     * 从宝石的词条数据中检查是否有元素宝石
     */
    public static boolean hasElementalGems(ItemStack weapon) {
        if (weapon.isEmpty() || !weapon.hasTagCompound()) {
            return false;
        }

        NBTTagCompound weaponTag = weapon.getTagCompound();
        if (!weaponTag.hasKey("SocketedGems")) {
            return false;
        }

        NBTTagList gemList = weaponTag.getTagList("SocketedGems", Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < gemList.tagCount(); i++) {
            NBTTagCompound gemItemTag = gemList.getCompoundTagAt(i);

            if (!gemItemTag.hasKey("tag")) continue;
            NBTTagCompound gemTag = gemItemTag.getCompoundTag("tag");

            if (!gemTag.hasKey("GemData")) continue;
            NBTTagCompound gemData = gemTag.getCompoundTag("GemData");

            if (!gemData.hasKey("affixes")) continue;
            NBTTagList affixList = gemData.getTagList("affixes", Constants.NBT.TAG_COMPOUND);

            for (int j = 0; j < affixList.tagCount(); j++) {
                NBTTagCompound affix = affixList.getCompoundTagAt(j);
                String affixId = affix.getString("affixId");

                // 检查是否是元素词条
                if (affixId.contains("conversion_") || affixId.contains("boost_")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 计算元素转换和增伤（从宝石词条）
     */
    public static ElementalConversionData calculateConversion(ItemStack weapon) {
        if (!hasElementalGems(weapon)) {
            return null;
        }

        Map<String, Float> conversionRates = new HashMap<>();
        Map<String, Float> damageMultipliers = new HashMap<>();
        Map<String, Integer> gemCounts = new HashMap<>();

        NBTTagCompound weaponTag = weapon.getTagCompound();
        NBTTagList gemList = weaponTag.getTagList("SocketedGems", Constants.NBT.TAG_COMPOUND);

        // 遍历所有镶嵌的宝石
        for (int i = 0; i < gemList.tagCount(); i++) {
            NBTTagCompound gemItemTag = gemList.getCompoundTagAt(i);

            if (!gemItemTag.hasKey("tag")) continue;
            NBTTagCompound gemTag = gemItemTag.getCompoundTag("tag");

            if (!gemTag.hasKey("GemData")) continue;
            NBTTagCompound gemData = gemTag.getCompoundTag("GemData");

            if (!gemData.hasKey("affixes")) continue;
            NBTTagList affixList = gemData.getTagList("affixes", Constants.NBT.TAG_COMPOUND);

            // 遍历宝石的所有词条
            for (int j = 0; j < affixList.tagCount(); j++) {
                NBTTagCompound affix = affixList.getCompoundTagAt(j);
                String affixId = affix.getString("affixId");
                float value = affix.getFloat("value");

                // 解析元素类型（从词条ID中提取）
                String elementType = extractElementType(affixId);
                if (elementType == null) continue;

                // 统计宝石数量
                gemCounts.merge(elementType, 1, Integer::sum);

                // 处理转换率词条
                if (affixId.startsWith("conversion_")) {
                    float currentConversion = conversionRates.getOrDefault(elementType, 0f);
                    // ✅ 修复：value已经是小数形式，不需要除以100
                    conversionRates.put(elementType, currentConversion + value);
                }

                // 处理增伤词条
                else if (affixId.startsWith("boost_")) {
                    float currentMultiplier = damageMultipliers.getOrDefault(elementType, 1f);
                    // ✅ 修复：value已经是倍率形式，直接乘算
                    damageMultipliers.put(elementType, currentMultiplier * value);
                }
            }
        }

        if (conversionRates.isEmpty() && damageMultipliers.isEmpty()) {
            return null;
        }

        // 找出主导元素（转换率最高的）
        String dominantElement = findDominantElement(conversionRates, gemCounts);
        if (dominantElement == null) {
            dominantElement = conversionRates.keySet().iterator().next();
        }

        CustomElementType dominantType = getElementType(dominantElement);
        if (dominantType == null) {
            dominantType = createDefaultElementType(dominantElement);
        }

        // 计算总转换率和增伤
        float totalConversion = Math.min(1.0f, conversionRates.getOrDefault(dominantElement, 0f));
        float totalMultiplier = damageMultipliers.getOrDefault(dominantElement, 1f);
        int totalGems = gemCounts.getOrDefault(dominantElement, 0);

        boolean isMixed = gemCounts.size() > 1;

        // 注意：构造函数参数顺序是 (type, int gemCount, float conversion, float multiplier, boolean mixed)
        return new ElementalConversionData(
                dominantType,
                totalGems,           // int: 宝石数量
                totalConversion,     // float: 转换率
                totalMultiplier,     // float: 增伤倍率
                isMixed              // boolean: 是否混合
        );
    }

    /**
     * 从词条ID中提取元素类型
     */
    private static String extractElementType(String affixId) {
        // conversion_fire → fire
        // boost_ice → ice
        if (affixId.startsWith("conversion_")) {
            return affixId.substring("conversion_".length());
        } else if (affixId.startsWith("boost_")) {
            return affixId.substring("boost_".length());
        }
        return null;
    }

    /**
     * 找出主导元素（转换率最高或宝石数量最多）
     */
    private static String findDominantElement(Map<String, Float> conversionRates,
                                              Map<String, Integer> gemCounts) {
        String dominant = null;
        float maxConversion = 0f;
        int maxCount = 0;

        for (Map.Entry<String, Float> entry : conversionRates.entrySet()) {
            String element = entry.getKey();
            float conversion = entry.getValue();
            int count = gemCounts.getOrDefault(element, 0);

            if (conversion > maxConversion || (conversion == maxConversion && count > maxCount)) {
                dominant = element;
                maxConversion = conversion;
                maxCount = count;
            }
        }

        return dominant;
    }

    /**
     * 创建默认元素类型（如果未注册）
     */
    private static CustomElementType createDefaultElementType(String elementId) {
        CustomElementType type = new CustomElementType(elementId);
        type.setDisplayName("§7" + elementId);
        type.setDamageType(elementId);
        return type;
    }

    /**
     * 统计每种元素的宝石数量
     */
    public static Map<String, Integer> countGemsByElement(ItemStack weapon) {
        Map<String, Integer> counts = new HashMap<>();

        if (weapon.isEmpty() || !weapon.hasTagCompound()) {
            return counts;
        }

        NBTTagCompound weaponTag = weapon.getTagCompound();
        if (!weaponTag.hasKey("SocketedGems")) {
            return counts;
        }

        NBTTagList gemList = weaponTag.getTagList("SocketedGems", Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < gemList.tagCount(); i++) {
            NBTTagCompound gemItemTag = gemList.getCompoundTagAt(i);

            if (!gemItemTag.hasKey("tag")) continue;
            NBTTagCompound gemTag = gemItemTag.getCompoundTag("tag");

            if (!gemTag.hasKey("GemData")) continue;
            NBTTagCompound gemData = gemTag.getCompoundTag("GemData");

            if (!gemData.hasKey("affixes")) continue;
            NBTTagList affixList = gemData.getTagList("affixes", Constants.NBT.TAG_COMPOUND);

            Set<String> gemElements = new HashSet<>();

            for (int j = 0; j < affixList.tagCount(); j++) {
                NBTTagCompound affix = affixList.getCompoundTagAt(j);
                String affixId = affix.getString("affixId");

                String elementType = extractElementType(affixId);
                if (elementType != null) {
                    gemElements.add(elementType);
                }
            }

            // 一个宝石可能有多个元素词条，但只计数一次
            for (String element : gemElements) {
                counts.merge(element, 1, Integer::sum);
            }
        }

        return counts;
    }

    // ==========================================
    // 调试和工具方法
    // ==========================================

    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
    }

    public static void clearAll() {
        ELEMENT_TYPES.clear();
        MATERIAL_TO_ELEMENT.clear();
    }

    public static int getRegisteredCount() {
        return ELEMENT_TYPES.size();
    }

    // ==========================================
    // 多元素数据结构（用于伤害处理器）
    // ==========================================

    /**
     * 多元素数据 - 包含所有元素的转换率和增伤倍率
     */
    public static class MultiElementData {
        public final Map<String, Float> conversionRates;
        public final Map<String, Float> damageMultipliers;
        public final Map<String, Integer> gemCounts;

        public MultiElementData(Map<String, Float> conversionRates,
                                Map<String, Float> damageMultipliers,
                                Map<String, Integer> gemCounts) {
            this.conversionRates = conversionRates;
            this.damageMultipliers = damageMultipliers;
            this.gemCounts = gemCounts;
        }

        /**
         * 获取总转换率（所有元素的转换率之和，上限100%）
         */
        public float getTotalConversionRate() {
            float total = 0f;
            for (float rate : conversionRates.values()) {
                total += rate;
            }
            return Math.min(1.0f, total);
        }

        /**
         * 检查是否为混合元素
         */
        public boolean isMixed() {
            return conversionRates.size() > 1;
        }

        /**
         * 获取主导元素（转换率最高的）
         */
        public String getDominantElement() {
            String dominant = null;
            float maxRate = 0f;

            for (Map.Entry<String, Float> entry : conversionRates.entrySet()) {
                if (entry.getValue() > maxRate) {
                    dominant = entry.getKey();
                    maxRate = entry.getValue();
                }
            }

            return dominant;
        }
    }

    /**
     * 解析宝石词条数据（用于伤害处理器）
     *
     * 返回所有元素的转换率、增伤倍率和宝石数量
     */
    public static MultiElementData parseGemAffixes(ItemStack weapon) {
        Map<String, Float> conversionRates = new HashMap<>();
        Map<String, Float> damageMultipliers = new HashMap<>();
        Map<String, Integer> gemCounts = new HashMap<>();

        if (weapon.isEmpty() || !weapon.hasTagCompound()) {
            return new MultiElementData(conversionRates, damageMultipliers, gemCounts);
        }

        NBTTagCompound weaponTag = weapon.getTagCompound();
        if (!weaponTag.hasKey("SocketedGems")) {
            return new MultiElementData(conversionRates, damageMultipliers, gemCounts);
        }

        NBTTagList gemList = weaponTag.getTagList("SocketedGems", Constants.NBT.TAG_COMPOUND);

        // 遍历所有镶嵌的宝石
        for (int i = 0; i < gemList.tagCount(); i++) {
            NBTTagCompound gemItemTag = gemList.getCompoundTagAt(i);

            if (!gemItemTag.hasKey("tag")) continue;
            NBTTagCompound gemTag = gemItemTag.getCompoundTag("tag");

            if (!gemTag.hasKey("GemData")) continue;
            NBTTagCompound gemData = gemTag.getCompoundTag("GemData");

            if (!gemData.hasKey("affixes")) continue;
            NBTTagList affixList = gemData.getTagList("affixes", Constants.NBT.TAG_COMPOUND);

            Set<String> gemElements = new HashSet<>();

            // 遍历宝石的所有词条
            for (int j = 0; j < affixList.tagCount(); j++) {
                NBTTagCompound affix = affixList.getCompoundTagAt(j);
                String affixId = affix.getString("affixId");
                float value = affix.getFloat("value");

                // 解析元素类型
                String elementType = extractElementType(affixId);
                if (elementType == null) continue;

                gemElements.add(elementType);

                // 处理转换率词条
                if (affixId.startsWith("conversion_")) {
                    float currentConversion = conversionRates.getOrDefault(elementType, 0f);
                    // ✅ 修复：value已经是小数形式（0.28 = 28%），不需要除以100
                    conversionRates.put(elementType, currentConversion + value);
                }

                // 处理增伤词条
                else if (affixId.startsWith("boost_")) {
                    float currentMultiplier = damageMultipliers.getOrDefault(elementType, 1f);
                    // ✅ 修复：value已经是倍率形式（1.5 = +50%），直接乘算
                    // 例如：1.5 × 1.8 = 2.7 倍
                    damageMultipliers.put(elementType, currentMultiplier * value);
                }
            }

            // 统计宝石数量
            for (String element : gemElements) {
                gemCounts.merge(element, 1, Integer::sum);
            }
        }

        return new MultiElementData(conversionRates, damageMultipliers, gemCounts);
    }
}