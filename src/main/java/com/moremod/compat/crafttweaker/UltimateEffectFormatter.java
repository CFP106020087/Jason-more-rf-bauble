package com.moremod.compat.crafttweaker;

import java.util.HashMap;
import java.util.Map;

/**
 * Ultimate效果格式化器
 * 根据effectType智能选择显示单位
 */
public class UltimateEffectFormatter {
    
    // 效果类型到格式化规则的映射
    private static final Map<String, FormatRule> FORMAT_RULES = new HashMap<>();
    
    static {
        // ========== 概率类（0.0-1.0 → 百分比） ==========
        FORMAT_RULES.put("crit_chance", FormatRule.PERCENTAGE);
        FORMAT_RULES.put("dodge", FormatRule.PERCENTAGE);
        FORMAT_RULES.put("block", FormatRule.PERCENTAGE);
        FORMAT_RULES.put("lifesteal", FormatRule.PERCENTAGE);
        FORMAT_RULES.put("iframe_penetration", FormatRule.PERCENTAGE);
        FORMAT_RULES.put("dodge_chance", FormatRule.PERCENTAGE);
        FORMAT_RULES.put("block_chance", FormatRule.PERCENTAGE);
        FORMAT_RULES.put("percent_max_hp", FormatRule.PERCENTAGE);
        FORMAT_RULES.put("percent_current_hp", FormatRule.PERCENTAGE);
        
        // ========== 倍率类（显示为×倍） ==========
        FORMAT_RULES.put("crit_damage", FormatRule.MULTIPLIER);
        FORMAT_RULES.put("damage_multiplier", FormatRule.MULTIPLIER);
        FORMAT_RULES.put("knockback", FormatRule.MULTIPLIER);
        FORMAT_RULES.put("knockup", FormatRule.MULTIPLIER);
        
        // ========== 固定伤害（显示数值） ==========
        FORMAT_RULES.put("bonus_damage", FormatRule.DAMAGE);
        FORMAT_RULES.put("custom_damage", FormatRule.DAMAGE);
        FORMAT_RULES.put("aoe_damage", FormatRule.DAMAGE);
        FORMAT_RULES.put("chain_damage", FormatRule.DAMAGE);
        FORMAT_RULES.put("thorns", FormatRule.DAMAGE);
        
        // ========== 生命值类 ==========
        FORMAT_RULES.put("heal_on_kill", FormatRule.HP);
        FORMAT_RULES.put("lifesteal_onkill", FormatRule.HP);
        FORMAT_RULES.put("absorb_steal", FormatRule.HP);
        
        // ========== 时间类（tick → 秒） ==========
        FORMAT_RULES.put("duration", FormatRule.SECONDS);
        FORMAT_RULES.put("stun", FormatRule.SECONDS);
        FORMAT_RULES.put("freeze", FormatRule.SECONDS);
        FORMAT_RULES.put("ignite", FormatRule.SECONDS);
        
        // ========== 无敌帧类（显示tick） ==========
        FORMAT_RULES.put("reduce_iframes", FormatRule.TICKS);
        FORMAT_RULES.put("ignore_iframes", FormatRule.BOOLEAN);  // 特殊：是/否
        
        // ========== 距离类 ==========
        FORMAT_RULES.put("radius", FormatRule.BLOCKS);
        FORMAT_RULES.put("teleport_forward", FormatRule.BLOCKS);
        FORMAT_RULES.put("teleport_behind", FormatRule.BLOCKS);
        FORMAT_RULES.put("teleport_random", FormatRule.BLOCKS);
        FORMAT_RULES.put("blink", FormatRule.BLOCKS);
        FORMAT_RULES.put("pull", FormatRule.BLOCKS);
        
        // ========== 计数类（整数） ==========
        FORMAT_RULES.put("sword_beam", FormatRule.COUNT);
        FORMAT_RULES.put("multi_beam", FormatRule.COUNT);
        FORMAT_RULES.put("circle_beam", FormatRule.COUNT);
        FORMAT_RULES.put("summon_entity", FormatRule.COUNT);
        FORMAT_RULES.put("summon_ally", FormatRule.COUNT);
        
        // ========== 特殊类 ==========
        FORMAT_RULES.put("explosion", FormatRule.POWER);  // 爆炸威力
        FORMAT_RULES.put("potion", FormatRule.LEVEL);     // 药水等级
        FORMAT_RULES.put("random_potion", FormatRule.LEVEL);
    }
    
    /**
     * 格式化规则枚举
     */
    public enum FormatRule {
        // 百分比（0.15 → 15%）
        PERCENTAGE {
            @Override
            public String format(float value) {
                return formatPercentage(value * 100);
            }
        },
        
        // 倍率（2.5 → ×2.5）
        MULTIPLIER {
            @Override
            public String format(float value) {
                if (value >= 10) {
                    return String.format("×%.0f", value);
                } else {
                    return String.format("×%.1f", value);
                }
            }
        },
        
        // 伤害值
        DAMAGE {
            @Override
            public String format(float value) {
                if (value >= 100) {
                    return String.format("%.0f 伤害", value);
                } else if (value >= 10) {
                    return String.format("%.1f 伤害", value);
                } else {
                    return String.format("%.2f 伤害", value);
                }
            }
        },
        
        // 生命值
        HP {
            @Override
            public String format(float value) {
                if (value >= 100) {
                    return String.format("%.0f HP", value);
                } else if (value >= 10) {
                    return String.format("%.1f HP", value);
                } else {
                    return String.format("%.2f HP", value);
                }
            }
        },
        
        // 秒（20 ticks → 1秒）
        SECONDS {
            @Override
            public String format(float value) {
                float seconds = value / 20f;  // MC中20 tick = 1秒
                if (seconds >= 10) {
                    return String.format("%.0f秒", seconds);
                } else {
                    return String.format("%.1f秒", seconds);
                }
            }
        },
        
        // Tick
        TICKS {
            @Override
            public String format(float value) {
                return String.format("%d tick", (int)value);
            }
        },
        
        // 方块距离
        BLOCKS {
            @Override
            public String format(float value) {
                if (value >= 10) {
                    return String.format("%.0f格", value);
                } else {
                    return String.format("%.1f格", value);
                }
            }
        },
        
        // 计数（整数）
        COUNT {
            @Override
            public String format(float value) {
                return String.format("%d个", (int)value);
            }
        },
        
        // 爆炸威力
        POWER {
            @Override
            public String format(float value) {
                return String.format("威力 %.1f", value);
            }
        },
        
        // 等级
        LEVEL {
            @Override
            public String format(float value) {
                return String.format("Lv.%d", (int)value);
            }
        },
        
        // 布尔值
        BOOLEAN {
            @Override
            public String format(float value) {
                return value > 0 ? "是" : "否";
            }
        },
        
        // 默认（无单位）
        DEFAULT {
            @Override
            public String format(float value) {
                if (value >= 1000) {
                    return String.format("%.0f", value);
                } else if (value >= 100) {
                    return String.format("%.1f", value);
                } else {
                    return String.format("%.2f", value);
                }
            }
        };
        
        public abstract String format(float value);
        
        // 辅助方法：格式化百分比
        protected static String formatPercentage(float percent) {
            if (percent >= 100) {
                return String.format("%.0f%%", percent);
            } else if (percent >= 10) {
                return String.format("%.1f%%", percent);
            } else {
                return String.format("%.2f%%", percent);
            }
        }
    }
    
    /**
     * 格式化效果值
     * 
     * @param effectType 效果类型
     * @param value 数值
     * @return 格式化后的字符串
     */
    public static String formatEffect(String effectType, float value) {
        FormatRule rule = FORMAT_RULES.getOrDefault(effectType, FormatRule.DEFAULT);
        return rule.format(value);
    }
    
    /**
     * 格式化带描述的效果
     * 
     * @param effectType 效果类型
     * @param displayName 显示名称（如"火焰暴击率"）
     * @param value 数值
     * @return 完整的格式化字符串
     */
    public static String formatEffectWithName(String effectType, String displayName, float value) {
        String formattedValue = formatEffect(effectType, value);
        
        // 处理特殊的显示名称模板
        if (displayName.contains("{value}")) {
            return displayName.replace("{value}", formattedValue);
        }
        
        // 默认格式：名称 + 值
        return displayName + ": " + formattedValue;
    }
}