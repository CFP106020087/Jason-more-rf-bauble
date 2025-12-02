package com.moremod.item;

import net.minecraft.util.text.TextFormatting;

/**
 * 統一的升級類型枚舉
 * 包含所有升級類型定義
 */
public enum UpgradeType {

    // ===== 基礎升級類型 (來自 ItemMechanicalCore) =====
    ENERGY_CAPACITY("能量容量", TextFormatting.GOLD, UpgradeCategory.BASIC),
    ENERGY_EFFICIENCY("能量效率", TextFormatting.GREEN, UpgradeCategory.BASIC),
    ARMOR_ENHANCEMENT("护甲强化", TextFormatting.BLUE, UpgradeCategory.BASIC),
    SPEED_BOOST("速度提升", TextFormatting.AQUA, UpgradeCategory.BASIC),
    REGENERATION("生命恢复药水", TextFormatting.RED, UpgradeCategory.BASIC),
    SHIELD_GENERATOR("护盾生成", TextFormatting.YELLOW, UpgradeCategory.BASIC),
    FLIGHT_MODULE("飞行模块", TextFormatting.LIGHT_PURPLE, UpgradeCategory.AUXILIARY),
    TEMPERATURE_CONTROL("温度调节", TextFormatting.DARK_AQUA, UpgradeCategory.SURVIVAL),
    MAGIC_ABSORB("魔力熔炉", TextFormatting.AQUA, UpgradeCategory.COMBAT),
    // ===== 生存類升級 =====
    YELLOW_SHIELD("黄条护盾", TextFormatting.YELLOW, UpgradeCategory.SURVIVAL),
    HEALTH_REGEN("生命恢复", TextFormatting.RED, UpgradeCategory.SURVIVAL),
    HUNGER_THIRST("饥饿与口渴管理", TextFormatting.GREEN, UpgradeCategory.SURVIVAL),
    THORNS("反伤荆棘", TextFormatting.DARK_RED, UpgradeCategory.SURVIVAL),
    FIRE_EXTINGUISH("自动灭火", TextFormatting.BLUE, UpgradeCategory.SURVIVAL),

    // ===== 辅助類升級 =====
    WATERPROOF_MODULE("防水模块", TextFormatting.AQUA, UpgradeCategory.AUXILIARY),
    ORE_VISION("矿物透视", TextFormatting.GOLD, UpgradeCategory.AUXILIARY),
    MOVEMENT_SPEED("移动加速", TextFormatting.AQUA, UpgradeCategory.AUXILIARY),
    STEALTH("隐身潜行", TextFormatting.DARK_GRAY, UpgradeCategory.AUXILIARY),
    EXP_AMPLIFIER("经验增幅", TextFormatting.GREEN, UpgradeCategory.AUXILIARY),

    // ===== 战斗類升級 =====
    DAMAGE_BOOST("伤害提升", TextFormatting.DARK_RED, UpgradeCategory.COMBAT),  // 修正：移除"套装"
    ATTACK_SPEED("攻击速度", TextFormatting.YELLOW, UpgradeCategory.COMBAT),
    RANGE_EXTENSION("范围拓展", TextFormatting.BLUE, UpgradeCategory.COMBAT),
    PURSUIT("追击打击", TextFormatting.LIGHT_PURPLE, UpgradeCategory.COMBAT),
    RANGED_DAMAGE_BOOST("远程伤害增幅", TextFormatting.GOLD, UpgradeCategory.COMBAT),

    // ===== 能源類升級 =====
    KINETIC_GENERATOR("动能发电", TextFormatting.GRAY, UpgradeCategory.ENERGY),
    SOLAR_GENERATOR("太阳能发电", TextFormatting.YELLOW, UpgradeCategory.ENERGY),
    VOID_ENERGY("虚空能量", TextFormatting.DARK_PURPLE, UpgradeCategory.ENERGY),
    COMBAT_CHARGER("战斗充能", TextFormatting.RED, UpgradeCategory.ENERGY),

    // ===== 🆕 特殊組合套裝 =====
    SURVIVAL_PACKAGE("生存强化套装", TextFormatting.DARK_GREEN, UpgradeCategory.PACKAGE),  // 改为套装类别
    COMBAT_PACKAGE("战斗强化套装", TextFormatting.DARK_RED, UpgradeCategory.PACKAGE),
    OMNIPOTENT_PACKAGE("全能套装", TextFormatting.LIGHT_PURPLE, UpgradeCategory.PACKAGE);

    // 新增战斗套装
    // 在 UpgradeType 枚举中添加
    private final String displayName;
    private final TextFormatting color;
    private final UpgradeCategory category;

    UpgradeType(String displayName, TextFormatting color, UpgradeCategory category) {
        this.displayName = displayName;
        this.color = color;
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TextFormatting getColor() {
        return color;
    }

    public UpgradeCategory getCategory() {
        return category;
    }

    /**
     * 🆕 判断是否为组合套装
     */
    public boolean isPackage() {
        return this.category == UpgradeCategory.PACKAGE;
    }

    /**
     * 根據字符串獲取升級類型
     */
    public static UpgradeType fromString(String name) {
        // 尝试直接匹配
        for (UpgradeType type : values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }

        // 尝试忽略大小写匹配
        for (UpgradeType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return null;
    }

    /**
     * 升級類別
     */
    public enum UpgradeCategory {
        BASIC("基础", TextFormatting.WHITE),
        SURVIVAL("生存", TextFormatting.GREEN),
        AUXILIARY("辅助", TextFormatting.AQUA),
        COMBAT("战斗", TextFormatting.RED),
        ENERGY("能源", TextFormatting.YELLOW),
        PACKAGE("套装", TextFormatting.LIGHT_PURPLE);  // 🆕 新增套装类别

        private final String name;
        private final TextFormatting color;

        UpgradeCategory(String name, TextFormatting color) {
            this.name = name;
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public TextFormatting getColor() {
            return color;
        }
    }
}