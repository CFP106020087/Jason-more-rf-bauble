package com.moremod.item.chengyue;

import net.minecraft.util.text.TextFormatting;

/**
 * 澄月 - 八相形态枚举
 */
public enum ChengYueMoonForm {
    
    FULL_MOON(
        "满月",
        "攻击型",
        TextFormatting.GOLD,
        1.5f,   // 攻击倍率
        1.0f,   // 攻速倍率
        1.0f,   // 暴击率倍率
        1.0f    // 暴击伤害倍率
    ),
    
    WANING_GIBBOUS(
        "亏凸月",
        "均衡型",
        TextFormatting.YELLOW,
        1.2f,
        1.2f,
        1.1f,
        1.1f
    ),
    
    LAST_QUARTER(
        "下弦月",
        "防御型",
        TextFormatting.AQUA,
        0.8f,
        1.0f,
        1.0f,
        1.0f
    ),
    
    WANING_CRESCENT(
        "残月",
        "闪避型",
        TextFormatting.BLUE,
        0.9f,
        1.3f,
        1.2f,
        1.0f
    ),
    
    NEW_MOON(
        "新月",
        "暴击型",
        TextFormatting.DARK_PURPLE,
        1.0f,
        1.0f,
        1.5f,
        3.0f
    ),
    
    WAXING_CRESCENT(
        "娥眉月",
        "速度型",
        TextFormatting.LIGHT_PURPLE,
        1.1f,
        1.8f,
        1.0f,
        1.0f
    ),
    
    FIRST_QUARTER(
        "上弦月",
        "技能型",
        TextFormatting.GREEN,
        1.0f,
        1.1f,
        1.0f,
        1.0f
    ),
    
    WAXING_GIBBOUS(
        "盈凸月",
        "成长型",
        TextFormatting.WHITE,
        1.3f,
        1.0f,
        1.0f,
        1.2f
    );
    
    private final String name;
    private final String type;
    private final TextFormatting color;
    private final float damageMultiplier;
    private final float attackSpeedMultiplier;
    private final float critChanceMultiplier;
    private final float critDamageMultiplier;
    
    ChengYueMoonForm(String name, String type, TextFormatting color,
                     float damageMultiplier, float attackSpeedMultiplier,
                     float critChanceMultiplier, float critDamageMultiplier) {
        this.name = name;
        this.type = type;
        this.color = color;
        this.damageMultiplier = damageMultiplier;
        this.attackSpeedMultiplier = attackSpeedMultiplier;
        this.critChanceMultiplier = critChanceMultiplier;
        this.critDamageMultiplier = critDamageMultiplier;
    }
    
    // ==================== Getter方法 ====================
    
    public String getName() {
        return name;
    }
    
    public String getType() {
        return type;
    }
    
    public TextFormatting getColor() {
        return color;
    }
    
    public float getDamageMultiplier() {
        return damageMultiplier;
    }
    
    public float getAttackSpeedMultiplier() {
        return attackSpeedMultiplier;
    }
    
    public float getCritChanceMultiplier() {
        return critChanceMultiplier;
    }
    
    public float getCritDamageMultiplier() {
        return critDamageMultiplier;
    }
    
    // ==================== 显示方法 ====================
    
    /**
     * 获取完整显示名称（带颜色）
     */
    public String getFullDisplayName() {
        return color + "【" + name + "·" + type + "】";
    }
    
    /**
     * 获取描述
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        
        // 攻击力
        if (damageMultiplier != 1.0f) {
            String prefix = damageMultiplier > 1.0f ? "+" : "";
            sb.append("攻击力 ").append(prefix).append(String.format("%.0f%%", (damageMultiplier - 1.0f) * 100));
            sb.append(" ");
        }
        
        // 攻速
        if (attackSpeedMultiplier != 1.0f) {
            String prefix = attackSpeedMultiplier > 1.0f ? "+" : "";
            sb.append("攻速 ").append(prefix).append(String.format("%.0f%%", (attackSpeedMultiplier - 1.0f) * 100));
            sb.append(" ");
        }
        
        // 暴击率
        if (critChanceMultiplier != 1.0f) {
            String prefix = critChanceMultiplier > 1.0f ? "+" : "";
            sb.append("暴击率 ").append(prefix).append(String.format("%.0f%%", (critChanceMultiplier - 1.0f) * 100));
            sb.append(" ");
        }
        
        // 暴击伤害
        if (critDamageMultiplier != 1.0f) {
            String prefix = critDamageMultiplier > 1.0f ? "+" : "";
            sb.append("暴击伤害 ").append(prefix).append(String.format("%.0f%%", (critDamageMultiplier - 1.0f) * 100));
        }
        
        String result = sb.toString().trim();
        return result.isEmpty() ? "标准属性" : result;
    }
    
    /**
     * 获取详细描述（多行）
     */
    public String getDetailedDescription() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(color).append("【").append(name).append("·").append(type).append("】\n");
        sb.append(TextFormatting.GRAY);
        
        if (damageMultiplier != 1.0f) {
            sb.append("攻击力: ").append(TextFormatting.WHITE)
              .append(String.format("×%.2f", damageMultiplier))
              .append(TextFormatting.GRAY).append("\n");
        }
        
        if (attackSpeedMultiplier != 1.0f) {
            sb.append("攻击速度: ").append(TextFormatting.WHITE)
              .append(String.format("×%.2f", attackSpeedMultiplier))
              .append(TextFormatting.GRAY).append("\n");
        }
        
        if (critChanceMultiplier != 1.0f) {
            sb.append("暴击率: ").append(TextFormatting.WHITE)
              .append(String.format("×%.2f", critChanceMultiplier))
              .append(TextFormatting.GRAY).append("\n");
        }
        
        if (critDamageMultiplier != 1.0f) {
            sb.append("暴击伤害: ").append(TextFormatting.WHITE)
              .append(String.format("×%.2f", critDamageMultiplier));
        }
        
        return sb.toString();
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 根据月相索引获取形态
     */
    public static ChengYueMoonForm getFormByMoonPhase(int moonPhase) {
        moonPhase = moonPhase % 8;
        return values()[moonPhase];
    }
    
    /**
     * 根据名称获取形态
     */
    public static ChengYueMoonForm getFormByName(String name) {
        for (ChengYueMoonForm form : values()) {
            if (form.getName().equals(name)) {
                return form;
            }
        }
        return FULL_MOON; // 默认返回满月
    }
    
    /**
     * 获取下一个形态
     */
    public ChengYueMoonForm next() {
        int nextIndex = (this.ordinal() + 1) % 8;
        return values()[nextIndex];
    }
    
    /**
     * 获取上一个形态
     */
    public ChengYueMoonForm previous() {
        int prevIndex = (this.ordinal() - 1 + 8) % 8;
        return values()[prevIndex];
    }
}