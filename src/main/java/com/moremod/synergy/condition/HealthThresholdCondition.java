package com.moremod.synergy.condition;

import com.moremod.synergy.api.ISynergyCondition;
import com.moremod.synergy.core.SynergyContext;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 生命值阈值条件
 *
 * 检查玩家生命值是否满足指定的阈值条件。
 */
public class HealthThresholdCondition implements ISynergyCondition {

    public enum CompareType {
        GREATER_THAN,
        GREATER_OR_EQUAL,
        LESS_THAN,
        LESS_OR_EQUAL,
        BETWEEN
    }

    private final CompareType compareType;
    private final float threshold;  // 百分比 (0-100)
    private final float upperThreshold;

    public HealthThresholdCondition(CompareType compareType, float threshold) {
        this.compareType = compareType;
        this.threshold = threshold;
        this.upperThreshold = 100f;
    }

    public HealthThresholdCondition(float lowerThreshold, float upperThreshold) {
        this.compareType = CompareType.BETWEEN;
        this.threshold = lowerThreshold;
        this.upperThreshold = upperThreshold;
    }

    @Override
    public boolean test(SynergyContext context) {
        EntityPlayer player = context.getPlayer();
        float maxHealth = player.getMaxHealth();
        float currentHealth = player.getHealth();
        float healthPercent = (currentHealth / maxHealth) * 100f;

        switch (compareType) {
            case GREATER_THAN:
                return healthPercent > threshold;
            case GREATER_OR_EQUAL:
                return healthPercent >= threshold;
            case LESS_THAN:
                return healthPercent < threshold;
            case LESS_OR_EQUAL:
                return healthPercent <= threshold;
            case BETWEEN:
                return healthPercent >= threshold && healthPercent <= upperThreshold;
            default:
                return false;
        }
    }

    @Override
    public String getDescription() {
        switch (compareType) {
            case GREATER_THAN:
                return "HP > " + threshold + "%";
            case GREATER_OR_EQUAL:
                return "HP >= " + threshold + "%";
            case LESS_THAN:
                return "HP < " + threshold + "%";
            case LESS_OR_EQUAL:
                return "HP <= " + threshold + "%";
            case BETWEEN:
                return "HP " + threshold + "% ~ " + upperThreshold + "%";
            default:
                return "HP check";
        }
    }

    // ==================== 静态工厂方法 ====================

    public static HealthThresholdCondition above(float percent) {
        return new HealthThresholdCondition(CompareType.GREATER_THAN, percent);
    }

    public static HealthThresholdCondition atLeast(float percent) {
        return new HealthThresholdCondition(CompareType.GREATER_OR_EQUAL, percent);
    }

    public static HealthThresholdCondition below(float percent) {
        return new HealthThresholdCondition(CompareType.LESS_THAN, percent);
    }

    public static HealthThresholdCondition atMost(float percent) {
        return new HealthThresholdCondition(CompareType.LESS_OR_EQUAL, percent);
    }

    public static HealthThresholdCondition critical() {
        return below(30f);
    }

    public static HealthThresholdCondition low() {
        return below(50f);
    }

    public static HealthThresholdCondition between(float lower, float upper) {
        return new HealthThresholdCondition(lower, upper);
    }
}
