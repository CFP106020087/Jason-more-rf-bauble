package com.moremod.system.humanity;

/**
 * 升格路线枚举
 * Ascension Route Enum
 *
 * 当前只有破碎之神路线，高人性路线待实现
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
    BROKEN_GOD("broken_god", "破碎之神", "Broken God");

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
     * 检查是否是高人性路线（待实现）
     * @deprecated 高人性升格路线尚未实现
     */
    @Deprecated
    public boolean isMekhane() {
        return false;
    }

    /**
     * 检查是否是破碎之神路线
     */
    public boolean isBrokenGod() {
        return this == BROKEN_GOD;
    }
}
