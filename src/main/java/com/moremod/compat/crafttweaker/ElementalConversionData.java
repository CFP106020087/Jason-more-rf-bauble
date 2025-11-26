package com.moremod.compat.crafttweaker;

/**
 * 元素转换数据（支持转换率+增伤分离）
 *
 * 新设计：
 * - conversionRatio：转换率（0.0-1.0），决定多少伤害转为元素
 * - damageMultiplier：增伤倍率（≥1.0），决定元素伤害的倍率
 *
 * 计算公式：
 * 元素伤害 = 原始伤害 × 转换率 × 增伤倍率
 * 物理伤害 = 原始伤害 × (1 - 转换率)
 * 最终伤害 = 元素伤害 + 物理伤害
 */
public class ElementalConversionData {

    /** 主导元素类型 */
    public final CustomElementType dominantType;

    /** 宝石总数量 */
    public final int totalGemCount;

    /** 转换率 (0.0 - 1.0) */
    public final float conversionRatio;

    /** 增伤倍率 (≥1.0) */
    public final float damageMultiplier;

    /** 是否混合元素 */
    public final boolean isMixed;

    /**
     * 构造函数
     *
     * @param dominantType 主导元素类型
     * @param totalGemCount 宝石总数
     * @param conversionRatio 转换率 (0.0-1.0)
     * @param damageMultiplier 增伤倍率 (≥1.0)
     * @param isMixed 是否混合
     */
    public ElementalConversionData(
            CustomElementType dominantType,
            int totalGemCount,
            float conversionRatio,
            float damageMultiplier,
            boolean isMixed
    ) {
        this.dominantType = dominantType;
        this.totalGemCount = totalGemCount;
        this.conversionRatio = Math.max(0.0f, Math.min(1.0f, conversionRatio));
        this.damageMultiplier = Math.max(1.0f, damageMultiplier);
        this.isMixed = isMixed;
    }

    /**
     * 计算最终伤害
     *
     * @param originalDamage 原始伤害
     * @return 最终伤害
     */
    public float calculateFinalDamage(float originalDamage) {
        // 元素伤害 = 原始 × 转换率 × 增伤
        float elementalDamage = originalDamage * conversionRatio * damageMultiplier;

        // 物理伤害 = 原始 × (1 - 转换率)
        float physicalDamage = originalDamage * (1.0f - conversionRatio);

        // 总伤害
        return elementalDamage + physicalDamage;
    }

    /**
     * 获取元素伤害部分
     */
    public float getElementalDamage(float originalDamage) {
        return originalDamage * conversionRatio * damageMultiplier;
    }

    /**
     * 获取物理伤害部分
     */
    public float getPhysicalDamage(float originalDamage) {
        return originalDamage * (1.0f - conversionRatio);
    }

    @Override
    public String toString() {
        return String.format(
                "ElementalConversion[type=%s, gems=%d, conversion=%.0f%%, multiplier=×%.1f, mixed=%s]",
                dominantType != null ? dominantType.getDisplayName() : "null",
                totalGemCount,
                conversionRatio * 100,
                damageMultiplier,
                isMixed
        );
    }
}