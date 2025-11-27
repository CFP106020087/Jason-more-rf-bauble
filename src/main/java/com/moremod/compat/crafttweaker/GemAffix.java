package com.moremod.compat.crafttweaker;

import java.util.HashMap;
import java.util.Map;

/**
 * 宝石词条 - 智能格式化完整版
 *
 * 特性：
 * 1. 根据effectType智能选择显示单位
 * 2. 避免百分比误伤非概率类效果
 * 3. 支持所有Ultimate效果类型
 */
public class GemAffix {

    private final String id;
    private String displayName;
    private AffixType type;
    private int weight;
    private int levelRequirement;
    private float minValue;
    private float maxValue;
    private final Map<String, Object> parameters;
    private boolean enabled;

    public GemAffix(String id) {
        this.id = id;
        this.displayName = id;
        this.type = AffixType.DAMAGE_MULTIPLIER;
        this.weight = 100;
        this.levelRequirement = 1;
        this.minValue = 1.0f;
        this.maxValue = 2.0f;
        this.parameters = new HashMap<>();
        this.enabled = true;
    }

    // ==========================================
    // Getters
    // ==========================================

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescriptionPattern() { return displayName; }
    public AffixType getType() { return type; }
    public int getWeight() { return weight; }
    public int getLevelRequirement() { return levelRequirement; }
    public float getMinValue() { return minValue; }
    public float getMaxValue() { return maxValue; }
    public Map<String, Object> getParameters() { return parameters; }
    public boolean isEnabled() { return enabled; }
    public Object getParameter(String key) { return parameters.get(key); }
    public boolean hasParameter(String key) { return parameters.containsKey(key); }

    // ==========================================
    // Setters (Builder Pattern)
    // ==========================================

    public GemAffix setDisplayName(String name) {
        this.displayName = name;
        return this;
    }

    public GemAffix setType(AffixType type) {
        this.type = type;
        return this;
    }

    public GemAffix setWeight(int weight) {
        this.weight = Math.max(1, weight);
        return this;
    }

    public GemAffix setLevelRequirement(int level) {
        this.levelRequirement = Math.max(1, level);
        return this;
    }

    public GemAffix setValueRange(float min, float max) {
        this.minValue = min;
        this.maxValue = max;
        return this;
    }

    public GemAffix setParameter(String key, Object value) {
        this.parameters.put(key, value);
        return this;
    }

    public GemAffix setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    // ==========================================
    // 业务方法
    // ==========================================

    public float rollValue() {
        if (minValue >= maxValue) return minValue;
        return minValue + (float)(Math.random() * (maxValue - minValue));
    }

    /**
     * ✅ 智能格式化 - 根据数值类型和大小自动调整精度
     */
    public String formatDescription(float rolledValue) {
        String valueStr;

        switch (type) {
            case DAMAGE_CONVERSION:
                // 伤害转换：0.0-1.0 → 0%-100%
                valueStr = formatPercentage(rolledValue);
                break;

            case DAMAGE_MULTIPLIER:
                // 伤害倍率：1.5 → +50%
                valueStr = formatMultiplier(rolledValue);
                break;

            case FLAT_DAMAGE:
                // 固定伤害：智能精度
                valueStr = formatNumber(rolledValue);
                break;

            case ATTACK_SPEED:
                // 攻击速度：+0.25
                valueStr = formatSpeed(rolledValue);
                break;

            case ATTRIBUTE_BONUS:
                // 属性加成：智能精度
                valueStr = formatBonus(rolledValue);
                break;

            case SPECIAL_EFFECT:
                // ✅ 修复：根据effectType智能选择格式化方式
                valueStr = formatSpecialEffect(rolledValue);
                break;

            default:
                valueStr = formatNumber(rolledValue);
        }

        return displayName.replace("{value}", valueStr);
    }

    /**
     * ✅ 智能格式化特殊效果
     */
    private String formatSpecialEffect(float value) {
        String effectType = (String) parameters.get("effectType");
        
        if (effectType == null) {
            return formatNumber(value);
        }
        
        // 根据effectType决定格式化方式
        switch (effectType) {
            // ===== 概率类（需要×100显示为百分比） =====
            case "crit_chance":
            case "dodge":
            case "dodge_chance":
            case "block":
            case "block_chance":
            case "lifesteal":
            case "iframe_penetration":
            case "percent_max_hp":
            case "percent_current_hp":
                return formatPercentage(value);  // 0.15 → 15%
                
            // ===== 倍率类（直接显示倍数） =====
            case "crit_damage":
            case "damage_multiplier":
            case "knockback":
            case "knockup":
                return formatMultiplierDirect(value);  // 2.5 → ×2.5
                
            // ===== 伤害类（显示数值+单位） =====
            case "bonus_damage":
            case "custom_damage":
            case "aoe_damage":
            case "chain_damage":
            case "thorns":
                return formatDamage(value);  // 15.5 → 15.5 伤害
                
            // ===== 生命值类 =====
            case "heal_on_kill":
            case "lifesteal_onkill":
            case "absorb_steal":
                return formatHP(value);  // 20 → 20 HP
                
            // ===== 时间类（tick需要转换为秒） =====
            case "stun":
            case "freeze":
            case "ignite":
            case "duration":
                return formatTime(value);  // 40 → 2秒
                
            // ===== 距离类（方块） =====
            case "radius":
            case "teleport_forward":
            case "teleport_behind":
            case "teleport_random":
            case "blink":
            case "pull":
            case "animal_love":  // 繁殖范围
            case "villager_mate":
                return formatDistance(value);  // 15 → 15格
                
            // ===== Tick类 =====
            case "reduce_iframes":
                return formatTicks(value);  // 10 → 10 ticks
                
            // ===== 布尔类 =====
            case "ignore_iframes":
                return value > 0 ? "是" : "否";
                
            // ===== 计数类 =====
            case "sword_beam":
            case "multi_beam":
            case "circle_beam":
            case "summon_entity":
            case "summon_ally":
                return formatCount(value);  // 3 → 3个
                
            // ===== 爆炸威力 =====
            case "explosion":
                return formatPower(value);  // 4.0 → 威力 4.0
                
            // ===== 药水等级 =====
            case "potion":
            case "potion_self":
            case "random_potion":
                return formatLevel(value);  // 2 → Lv.2
                
            // ===== 默认：直接显示数值 =====
            default:
                return formatNumber(value);
        }
    }

    // ==========================================
    // 格式化方法
    // ==========================================

    /**
     * 格式化百分比（0.0-1.0 → 0%-100%）
     */
    private String formatPercentage(float value) {
        double percent = value * 100.0;

        if (percent >= 100) {
            return String.format("%.0f%%", percent);
        } else if (percent >= 10) {
            return String.format("%.1f%%", percent);
        } else if (percent >= 1) {
            return String.format("%.1f%%", percent);
        } else if (percent >= 0.1) {
            return String.format("%.2f%%", percent);
        } else {
            return String.format("%.3f%%", percent);
        }
    }

    /**
     * 格式化倍率（1.5 → +50%）用于DAMAGE_MULTIPLIER类型
     */
    private String formatMultiplier(float value) {
        double percent = (value - 1.0) * 100.0;
        String sign = percent >= 0 ? "+" : "";

        if (Math.abs(percent) >= 100) {
            return String.format("%s%.0f%%", sign, percent);
        } else if (Math.abs(percent) >= 10) {
            return String.format("%s%.1f%%", sign, percent);
        } else {
            return String.format("%s%.1f%%", sign, percent);
        }
    }

    /**
     * 格式化倍率（直接显示）用于特殊效果
     */
    private String formatMultiplierDirect(float value) {
        if (value >= 10) {
            return String.format("×%.0f", value);
        } else {
            return String.format("×%.1f", value);
        }
    }

    /**
     * 格式化数字（智能精度）
     */
    private String formatNumber(float value) {
        double absValue = Math.abs(value);

        if (absValue >= 1000) {
            return String.format("%.0f", value);
        } else if (absValue >= 100) {
            return String.format("%.0f", value);
        } else if (absValue >= 10) {
            return String.format("%.1f", value);
        } else if (absValue >= 1) {
            return String.format("%.1f", value);
        } else if (absValue >= 0.1) {
            return String.format("%.2f", value);
        } else {
            return String.format("%.3f", value);
        }
    }

    /**
     * 格式化加成（带正负号）
     */
    private String formatBonus(float value) {
        double absValue = Math.abs(value);
        String sign = value >= 0 ? "+" : "";

        if (absValue >= 100) {
            return String.format("%s%.0f", sign, value);
        } else if (absValue >= 10) {
            return String.format("%s%.1f", sign, value);
        } else if (absValue >= 1) {
            return String.format("%s%.1f", sign, value);
        } else {
            return String.format("%s%.2f", sign, value);
        }
    }

    /**
     * 格式化速度（固定2位小数）
     */
    private String formatSpeed(float value) {
        return String.format("%+.2f", value);
    }

    /**
     * 格式化伤害值
     */
    private String formatDamage(float value) {
        if (value >= 100) {
            return String.format("%.0f 伤害", value);
        } else if (value >= 10) {
            return String.format("%.1f 伤害", value);
        } else {
            return String.format("%.2f 伤害", value);
        }
    }

    /**
     * 格式化生命值
     */
    private String formatHP(float value) {
        if (value >= 100) {
            return String.format("%.0f HP", value);
        } else if (value >= 10) {
            return String.format("%.1f HP", value);
        } else {
            return String.format("%.2f HP", value);
        }
    }

    /**
     * 格式化时间（tick转秒）
     */
    private String formatTime(float ticks) {
        float seconds = ticks / 20f;  // MC中20 tick = 1秒
        if (seconds >= 10) {
            return String.format("%.0f秒", seconds);
        } else if (seconds >= 1) {
            return String.format("%.1f秒", seconds);
        } else {
            return String.format("%.2f秒", seconds);
        }
    }

    /**
     * 格式化距离
     */
    private String formatDistance(float blocks) {
        if (blocks >= 100) {
            return String.format("%.0f格", blocks);
        } else if (blocks >= 10) {
            return String.format("%.1f格", blocks);
        } else {
            return String.format("%.2f格", blocks);
        }
    }

    /**
     * 格式化Tick数
     */
    private String formatTicks(float value) {
        return String.format("%d ticks", (int)value);
    }

    /**
     * 格式化计数
     */
    private String formatCount(float value) {
        return String.format("%d个", (int)value);
    }

    /**
     * 格式化爆炸威力
     */
    private String formatPower(float value) {
        if (value >= 10) {
            return String.format("威力 %.0f", value);
        } else {
            return String.format("威力 %.1f", value);
        }
    }

    /**
     * 格式化等级
     */
    private String formatLevel(float value) {
        return String.format("Lv.%d", (int)value);
    }

    @Override
    public String toString() {
        return String.format("GemAffix[id=%s, type=%s, weight=%d, range=[%.2f-%.2f]]",
                id, type, weight, minValue, maxValue);
    }

    // ==========================================
    // 词条类型枚举
    // ==========================================

    public enum AffixType {
        /** 伤害转换 (0.0-1.0) */
        DAMAGE_CONVERSION("damage_conversion"),

        /** 伤害倍率 (≥1.0) */
        DAMAGE_MULTIPLIER("damage_multiplier"),

        /** 固定伤害 */
        FLAT_DAMAGE("flat_damage"),

        /** 攻击速度 */
        ATTACK_SPEED("attack_speed"),

        /** 属性加成 (力量/敏捷/智力等) */
        ATTRIBUTE_BONUS("attribute_bonus"),

        /** 特殊效果 (吸血/暴击等) */
        SPECIAL_EFFECT("special_effect"),

        /** 自定义类型 */
        CUSTOM("custom");

        private final String id;

        AffixType(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static AffixType fromString(String id) {
            for (AffixType type : values()) {
                if (type.id.equalsIgnoreCase(id)) {
                    return type;
                }
            }
            return CUSTOM;
        }
    }
}