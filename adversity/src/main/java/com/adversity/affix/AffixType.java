package com.adversity.affix;

/**
 * 词条类型枚举
 */
public enum AffixType {

    /**
     * 攻击型词条 - 增强怪物的攻击能力
     */
    OFFENSIVE("offensive", 0xFF5555),

    /**
     * 防御型词条 - 增强怪物的防御能力
     */
    DEFENSIVE("defensive", 0x5555FF),

    /**
     * 功能型词条 - 提供特殊能力
     */
    UTILITY("utility", 0x55FF55),

    /**
     * 特殊型词条 - 稀有的强力词条
     */
    SPECIAL("special", 0xFFAA00);

    private final String name;
    private final int color;

    AffixType(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    /**
     * 获取该类型词条的显示颜色
     */
    public int getColor() {
        return color;
    }

    public String getTranslationKey() {
        return "adversity.affix.type." + name;
    }
}
