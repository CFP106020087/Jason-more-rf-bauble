package com.moremod.synergy.condition;

import com.moremod.synergy.api.ISynergyCondition;
import com.moremod.synergy.core.SynergyContext;

import java.util.Random;

/**
 * 随机概率条件
 *
 * 以指定概率返回 true。
 */
public class RandomChanceCondition implements ISynergyCondition {

    private static final Random RANDOM = new Random();

    private final float chance;

    /**
     * @param chance 触发概率 (0.0 ~ 1.0)
     */
    public RandomChanceCondition(float chance) {
        this.chance = Math.max(0f, Math.min(1f, chance));
    }

    @Override
    public boolean test(SynergyContext context) {
        return RANDOM.nextFloat() < chance;
    }

    @Override
    public String getDescription() {
        return String.format("%.0f%% chance", chance * 100);
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 百分比概率
     *
     * @param percent 百分比 (0 ~ 100)
     */
    public static RandomChanceCondition percent(float percent) {
        return new RandomChanceCondition(percent / 100f);
    }

    /**
     * 小数概率
     *
     * @param chance 概率 (0.0 ~ 1.0)
     */
    public static RandomChanceCondition of(float chance) {
        return new RandomChanceCondition(chance);
    }
}
