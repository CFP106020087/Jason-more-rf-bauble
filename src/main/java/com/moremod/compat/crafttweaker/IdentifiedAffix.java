package com.moremod.compat.crafttweaker;

/**
 * 已鉴定的词条实例 - 包含具体的数值
 *
 * 修复版本 - 添加了 getDisplayName() 方法
 */
public class IdentifiedAffix {

    /** 词条定义 */
    private final GemAffix affix;

    /** 实际数值 */
    private final float value;

    /** 词条品质 (0-100) */
    private final int quality;

    public IdentifiedAffix(GemAffix affix, float value) {
        this.affix = affix;
        this.value = value;

        // 计算品质 (数值在范围内的百分位)
        float range = affix.getMaxValue() - affix.getMinValue();
        if (range > 0) {
            float percent = (value - affix.getMinValue()) / range;
            this.quality = Math.round(percent * 100);
        } else {
            this.quality = 100;
        }
    }

    public IdentifiedAffix(GemAffix affix, float value, int quality) {
        this.affix = affix;
        this.value = value;
        this.quality = Math.max(0, Math.min(100, quality));
    }

    // ==================== Getters ====================

    public GemAffix getAffix() {
        return affix;
    }

    public float getValue() {
        return value;
    }

    public int getQuality() {
        return quality;
    }

    /**
     * 获取显示名称（带数值）
     * ✅ 新增方法 - 供GUI使用
     */
    public String getDisplayName() {
        return affix.formatDescription(value);
    }

    /**
     * 获取格式化的词条描述
     */
    public String getFormattedDescription() {
        return affix.formatDescription(value);
    }

    /**
     * 获取带品质的完整描述
     */
    public String getFullDescription() {
        String desc = getFormattedDescription();
        String qualityColor = getQualityColor();
        return qualityColor + desc + " §7(" + quality + "%)";
    }

    /**
     * 根据品质获取颜色代码
     */
    public String getQualityColor() {
        if (quality >= 90) return "§d"; // 粉色 - 传说
        if (quality >= 75) return "§6"; // 金色 - 史诗
        if (quality >= 60) return "§5"; // 紫色 - 稀有
        if (quality >= 40) return "§9"; // 蓝色 - 优秀
        if (quality >= 20) return "§a"; // 绿色 - 良好
        return "§7"; // 灰色 - 普通
    }




    @Override
    public String toString() {
        return String.format("IdentifiedAffix[%s: %.2f (Q:%d%%)]",
                affix.getId(), value, quality);
    }
}