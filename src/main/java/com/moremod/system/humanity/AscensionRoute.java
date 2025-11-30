package com.moremod.system.humanity;

/**
 * 升格路线枚举
 * Ascension Route Enum
 *
 * 两条对偶路线：
 * - 破碎之神 (Broken God) - 绝对的矛，人性归零，全攻无守
 * - 香巴拉 (Shambhala) - 绝对的盾，人性圆满，全守无攻
 */
public enum AscensionRoute {
    /** 未升格 */
    NONE("none", "未升格", "Not Ascended"),

    /**
     * 破碎之神
     * 抛弃人性的机械升格
     * - 药水完全免疫
     * - 齿轮共鸣（机械增幅）
     * - 维度几何扭曲
     * - 存在干扰
     * - 失去与人类世界的交互能力
     */
    BROKEN_GOD("broken_god", "破碎之神", "Broken God"),

    /**
     * 香巴拉 - 永恒�的轮圣化身
     * Avatar of Eternal Gearwork Shambhala
     * 人性圆满的机械升格
     * - 绝对防御
     * - 高倍率反伤
     * - 免疫负面效果
     * - 代价：伤害削弱，防御机制消耗大量能量
     * - 只要有能量就不会倒下
     */
    SHAMBHALA("shambhala", "香巴拉", "Shambhala");

    private final String id;
    private final String displayNameCN;
    private final String displayNameEN;

    AscensionRoute(String id, String displayNameCN, String displayNameEN) {
        this.id = id;
        this.displayNameCN = displayNameCN;
        this.displayNameEN = displayNameEN;
    }

    public String getId() {
        return id;
    }

    public String getDisplayNameCN() {
        return displayNameCN;
    }

    public String getDisplayNameEN() {
        return displayNameEN;
    }

    /**
     * 从 ID 获取枚举
     */
    public static AscensionRoute fromId(String id) {
        if (id == null || id.isEmpty()) return NONE;

        for (AscensionRoute route : values()) {
            if (route.id.equals(id)) {
                return route;
            }
        }
        return NONE;
    }

    /**
     * 检查是否已升格
     */
    public boolean isAscended() {
        return this != NONE;
    }

    /**
     * 检查是否是香巴拉路线
     */
    public boolean isShambhala() {
        return this == SHAMBHALA;
    }

    /**
     * 检查是否是破碎之神路线
     */
    public boolean isBrokenGod() {
        return this == BROKEN_GOD;
    }
}
