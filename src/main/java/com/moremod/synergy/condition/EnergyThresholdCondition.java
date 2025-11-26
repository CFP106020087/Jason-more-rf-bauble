package com.moremod.synergy.condition;

import com.moremod.synergy.api.ISynergyCondition;
import com.moremod.synergy.bridge.ExistingModuleBridge;
import com.moremod.synergy.core.SynergyContext;

/**
 * 能量阈值条件
 *
 * 检查玩家能量是否满足指定的阈值条件。
 */
public class EnergyThresholdCondition implements ISynergyCondition {

    public enum CompareType {
        GREATER_THAN,
        GREATER_OR_EQUAL,
        LESS_THAN,
        LESS_OR_EQUAL,
        EQUAL,
        BETWEEN
    }

    private final CompareType compareType;
    private final float threshold;  // 百分比 (0-100)
    private final float upperThreshold;  // 用于 BETWEEN

    public EnergyThresholdCondition(CompareType compareType, float threshold) {
        this.compareType = compareType;
        this.threshold = threshold;
        this.upperThreshold = 100f;
    }

    public EnergyThresholdCondition(float lowerThreshold, float upperThreshold) {
        this.compareType = CompareType.BETWEEN;
        this.threshold = lowerThreshold;
        this.upperThreshold = upperThreshold;
    }

    @Override
    public boolean test(SynergyContext context) {
        ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();
        float currentPercent = bridge.getEnergyPercent(context.getPlayer());

        switch (compareType) {
            case GREATER_THAN:
                return currentPercent > threshold;
            case GREATER_OR_EQUAL:
                return currentPercent >= threshold;
            case LESS_THAN:
                return currentPercent < threshold;
            case LESS_OR_EQUAL:
                return currentPercent <= threshold;
            case EQUAL:
                return Math.abs(currentPercent - threshold) < 0.01f;
            case BETWEEN:
                return currentPercent >= threshold && currentPercent <= upperThreshold;
            default:
                return false;
        }
    }

    @Override
    public String getDescription() {
        switch (compareType) {
            case GREATER_THAN:
                return "Energy > " + threshold + "%";
            case GREATER_OR_EQUAL:
                return "Energy >= " + threshold + "%";
            case LESS_THAN:
                return "Energy < " + threshold + "%";
            case LESS_OR_EQUAL:
                return "Energy <= " + threshold + "%";
            case EQUAL:
                return "Energy = " + threshold + "%";
            case BETWEEN:
                return "Energy " + threshold + "% ~ " + upperThreshold + "%";
            default:
                return "Energy check";
        }
    }

    // ==================== 静态工厂方法 ====================

    public static EnergyThresholdCondition above(float percent) {
        return new EnergyThresholdCondition(CompareType.GREATER_THAN, percent);
    }

    public static EnergyThresholdCondition atLeast(float percent) {
        return new EnergyThresholdCondition(CompareType.GREATER_OR_EQUAL, percent);
    }

    public static EnergyThresholdCondition below(float percent) {
        return new EnergyThresholdCondition(CompareType.LESS_THAN, percent);
    }

    public static EnergyThresholdCondition atMost(float percent) {
        return new EnergyThresholdCondition(CompareType.LESS_OR_EQUAL, percent);
    }

    public static EnergyThresholdCondition full() {
        return new EnergyThresholdCondition(CompareType.GREATER_OR_EQUAL, 100f);
    }

    public static EnergyThresholdCondition empty() {
        return new EnergyThresholdCondition(CompareType.LESS_OR_EQUAL, 0f);
    }

    public static EnergyThresholdCondition between(float lower, float upper) {
        return new EnergyThresholdCondition(lower, upper);
    }
}
