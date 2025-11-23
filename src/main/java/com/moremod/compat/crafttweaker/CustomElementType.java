package com.moremod.compat.crafttweaker;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义元素类型 - 完整版本（包含所有原有功能）
 *
 * 功能列表：
 * - 动态伤害类型（新增）
 * - 转换率和增伤
 * - 伤害属性（穿甲、魔法、爆炸等）
 * - 效果系统
 */
public class CustomElementType {

    private String id;
    private String displayName;

    // ==========================================
    // ⭐ 新增：动态伤害类型
    // ==========================================
    private String damageType;                // 任意伤害类型

    // ==========================================
    // 转换和增伤
    // ==========================================
    private float conversionRatePerGem;
    private float maxConversionRate;
    private float damageMultiplier;
    private float mixedPenalty;

    // ==========================================
    // 伤害属性
    // ==========================================
    private boolean bypassesArmor;            // 穿透护甲
    private boolean magicDamage;              // 魔法伤害
    private boolean explosion;                // 爆炸伤害
    private boolean absoluteDamage;           // 真实伤害
    private boolean unblockable;              // 无法格挡

    // ==========================================
    // 效果系统
    // ==========================================
    private List<Object> effects;             // 效果列表

    // ==========================================
    // 构造函数
    // ==========================================

    public CustomElementType(String id) {
        this.id = id;
        this.displayName = id;
        this.damageType = "physical";
        this.conversionRatePerGem = 0.0f;
        this.maxConversionRate = 1.0f;
        this.damageMultiplier = 1.0f;
        this.mixedPenalty = 0.0f;
        this.bypassesArmor = false;
        this.magicDamage = false;
        this.explosion = false;
        this.absoluteDamage = false;
        this.unblockable = false;
        this.effects = new ArrayList<>();
    }

    public CustomElementType(
            String id,
            String displayName,
            String damageType,
            float conversionRatePerGem,
            float maxConversionRate,
            float damageMultiplier,
            float mixedPenalty
    ) {
        this.id = id;
        this.displayName = displayName;
        this.damageType = damageType != null ? damageType.toLowerCase() : "physical";
        this.conversionRatePerGem = conversionRatePerGem;
        this.maxConversionRate = maxConversionRate;
        this.damageMultiplier = damageMultiplier;
        this.mixedPenalty = mixedPenalty;
        this.bypassesArmor = false;
        this.magicDamage = false;
        this.explosion = false;
        this.absoluteDamage = false;
        this.unblockable = false;
        this.effects = new ArrayList<>();
    }

    // ==========================================
    // Getter 方法
    // ==========================================

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDamageType() {
        return damageType;
    }

    public float getConversionRatePerGem() {
        return conversionRatePerGem;
    }

    public float getMaxConversionRate() {
        return maxConversionRate;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    public float getMixedPenalty() {
        return mixedPenalty;
    }

    public boolean isBypassesArmor() {
        return bypassesArmor;
    }

    public boolean isMagicDamage() {
        return magicDamage;
    }

    public boolean isExplosion() {
        return explosion;
    }

    public boolean isAbsoluteDamage() {
        return absoluteDamage;
    }

    public boolean isUnblockable() {
        return unblockable;
    }

    public List<Object> getEffects() {
        return effects;
    }

    // ==========================================
    // Setter 方法
    // ==========================================

    public void setId(String id) {
        this.id = id;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * ⭐ 新方法：设置伤害类型
     */
    public void setDamageType(String damageType) {
        this.damageType = damageType != null ? damageType.toLowerCase() : "physical";
    }

    /**
     * 兼容旧代码：设置为火焰伤害
     * @deprecated 推荐使用 setDamageType("fire")
     */
    @Deprecated
    public void setFireDamage(boolean isFire) {
        if (isFire) {
            this.damageType = "fire";
        }
    }

    public void setIceDamage(boolean isIce) {
        if (isIce) {
            this.damageType = "ice";
        }
    }

    public void setLightningDamage(boolean isLightning) {
        if (isLightning) {
            this.damageType = "lightning";
        }
    }

    public void setConversionRatePerGem(float conversionRatePerGem) {
        this.conversionRatePerGem = conversionRatePerGem;
    }

    public void setMaxConversionRate(float maxConversionRate) {
        this.maxConversionRate = maxConversionRate;
    }

    public void setDamageMultiplier(float damageMultiplier) {
        this.damageMultiplier = damageMultiplier;
    }

    public void setMixedPenalty(float mixedPenalty) {
        this.mixedPenalty = mixedPenalty;
    }

    public void setBypassesArmor(boolean bypassesArmor) {
        this.bypassesArmor = bypassesArmor;
    }

    public void setMagicDamage(boolean magicDamage) {
        this.magicDamage = magicDamage;
    }

    public void setExplosion(boolean explosion) {
        this.explosion = explosion;
    }

    public void setAbsoluteDamage(boolean absoluteDamage) {
        this.absoluteDamage = absoluteDamage;
    }

    public void setUnblockable(boolean unblockable) {
        this.unblockable = unblockable;
    }

    // ==========================================
    // ⭐ 效果系统方法
    // ==========================================

    /**
     * 添加效果
     * @param effect 效果对象
     */
    public void addEffect(Object effect) {
        if (effect != null) {
            this.effects.add(effect);
        }
    }

    /**
     * 移除效果
     */
    public void removeEffect(Object effect) {
        this.effects.remove(effect);
    }

    /**
     * 清空所有效果
     */
    public void clearEffects() {
        this.effects.clear();
    }

    /**
     * 是否有效果
     */
    public boolean hasEffects() {
        return !effects.isEmpty();
    }

    // ==========================================
    // 伤害类型判断
    // ==========================================

    public boolean isDamageType(String type) {
        if (type == null) return false;
        return damageType.equalsIgnoreCase(type);
    }

    public boolean isFire() {
        return "fire".equalsIgnoreCase(damageType);
    }

    public boolean isIce() {
        return "ice".equalsIgnoreCase(damageType);
    }

    public boolean isLightning() {
        return "lightning".equalsIgnoreCase(damageType);
    }

    public boolean isPhysical() {
        return "physical".equalsIgnoreCase(damageType);
    }

    public boolean isMagic() {
        return !isPhysical();
    }

    // ==========================================
    // 便捷方法
    // ==========================================

    public boolean providesConversion() {
        return conversionRatePerGem > 0.0f;
    }

    public boolean providesMultiplier() {
        return damageMultiplier > 1.0f;
    }

    @Override
    public String toString() {
        return String.format(
                "CustomElementType[id=%s, name=%s, damageType=%s, conversion=%.0f%%/gem, multiplier=×%.1f/gem, effects=%d]",
                id, displayName, damageType, conversionRatePerGem * 100, damageMultiplier, effects.size()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CustomElementType that = (CustomElementType) obj;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}