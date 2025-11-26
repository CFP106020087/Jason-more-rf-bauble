package com.moremod.compat.crafttweaker;

import java.util.*;

/**
 * 词条Tier映射器 - 最小入侵式设计(修复版)
 *
 * 功能:根据宝石等级限制词条的roll范围(上下限)
 * 特点:
 * - 不修改GemAffix类
 * - 不修改词条注册逻辑
 * - 完全向后兼容
 * - 可选启用/禁用
 *
 * ✅ 修复:现在会同时限制上限和下限,防止低级宝石出神装
 */
public class AffixTierMapper {

    private static boolean ENABLED = true;
    private static TierMode MODE = TierMode.BREAKPOINT; // 默认POE分段模式
    private static boolean DEBUG_ENABLED = false; // Debug输出开关

    /**
     * Tier计算模式
     */
    public enum TierMode {
        /** 线性:每级固定提升 */
        LINEAR,

        /** 平方根:前期快,后期慢 */
        SQRT,

        /** 指数:前期慢,后期快 */
        EXPONENTIAL,

        /** 分段:明确的等级断点(POE风格,推荐) */
        BREAKPOINT
    }

    // ==========================================
    // 配置方法
    // ==========================================

    /**
     * 启用/禁用Tier系统
     */
    public static void setEnabled(boolean enabled) {
        ENABLED = enabled;
    }

    /**
     * 设置计算模式
     */
    public static void setMode(TierMode mode) {
        MODE = mode;
    }

    /**
     * 启用/禁用Debug输出
     */
    public static void setDebugEnabled(boolean enabled) {
        DEBUG_ENABLED = enabled;
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static TierMode getMode() {
        return MODE;
    }

    public static boolean isDebugEnabled() {
        return DEBUG_ENABLED;
    }

    // ==========================================
    // 核心方法:计算品质范围(上下限)
    // ==========================================

    /**
     * ✅ 修复:根据宝石等级计算品质范围(上下限)
     *
     * @param gemLevel 宝石等级 (1-100)
     * @return [最小品质, 最大品质] 数组(0.0-1.0范围)
     */
    public static float[] calculateQualityRange(int gemLevel) {
        if (!ENABLED) {
            return new float[]{0.0f, 1.0f}; // 禁用时无限制
        }

        // 根据不同模式计算上下限
        if (MODE == TierMode.LINEAR) {
            return calculateLinearRange(gemLevel);
        } else if (MODE == TierMode.SQRT) {
            return calculateSqrtRange(gemLevel);
        } else if (MODE == TierMode.EXPONENTIAL) {
            return calculateExponentialRange(gemLevel);
        } else if (MODE == TierMode.BREAKPOINT) {
            return calculateBreakpointRange(gemLevel);
        } else {
            return calculateBreakpointRange(gemLevel); // 默认POE风格
        }
    }

    /**
     * 向后兼容:只返回下限(不推荐使用)
     */
    @Deprecated
    public static float calculateMinQuality(int gemLevel) {
        return calculateQualityRange(gemLevel)[0];
    }

    // ==========================================
    // Range计算方法(4种模式)
    // ==========================================

    /**
     * 线性模式Range
     *
     * 特点:均匀成长
     *
     * 1级:   0.6% - 20%
     * 25级:  15% - 45%
     * 50级:  30% - 70%
     * 75级:  45% - 90%
     * 100级: 60% - 100%
     */
    private static float[] calculateLinearRange(int level) {
        float minQuality = level * 0.006f;
        float maxQuality = 0.20f + (level * 0.008f); // 20%基础 + 80%成长
        return new float[]{minQuality, Math.min(1.0f, maxQuality)};
    }

    /**
     * 平方根模式Range
     *
     * 特点:前期提升快,后期趋于平缓
     *
     * 1级:   3% - 20%
     * 25级:  15% - 50%
     * 50级:  21% - 71%
     * 75级:  26% - 87%
     * 100级: 30% - 100%
     */
    private static float[] calculateSqrtRange(int level) {
        float normalized = level / 100.0f;

        float minQuality = (float)(Math.sqrt(normalized) * 0.30);
        float maxQuality = 0.20f + (float)(Math.sqrt(normalized) * 0.80);

        return new float[]{minQuality, Math.min(1.0f, maxQuality)};
    }

    /**
     * 指数模式Range
     *
     * 特点:前期慢,后期快速增长
     *
     * 1级:   0.01% - 20%
     * 25级:  1.56% - 40%
     * 50级:  6.25% - 65%
     * 75级:  14% - 86%
     * 100级: 25% - 100%
     */
    private static float[] calculateExponentialRange(int level) {
        float normalized = level / 100.0f;

        // 下限:指数增长(二次方)
        float minQuality = normalized * normalized * 0.25f;

        // 上限:线性增长(从20%到100%)
        float maxQuality = 0.20f + (normalized * 0.80f);

        return new float[]{minQuality, maxQuality};
    }

    /**
     * ⭐ POE分段模式Range(推荐)
     *
     * 特点:明确的等级断点,清晰的Tier划分
     *
     * 1-20级:  0% - 20%   (垃圾Tier - 只能出最差的)
     * 21-40级: 10% - 45%  (普通Tier - 最多中等)
     * 41-60级: 25% - 70%  (良好Tier - 最多良好)
     * 61-80级: 45% - 90%  (优秀Tier - 最多优秀)
     * 81-100级: 70% - 100% (传说Tier - 可以出完美)
     */
    private static float[] calculateBreakpointRange(int level) {
        if (level <= 20) {
            return new float[]{0.00f, 0.20f};  // Tier 1: 垃圾
        } else if (level <= 40) {
            return new float[]{0.10f, 0.45f};  // Tier 2: 普通
        } else if (level <= 60) {
            return new float[]{0.25f, 0.70f};  // Tier 3: 良好
        } else if (level <= 80) {
            return new float[]{0.45f, 0.90f};  // Tier 4: 优秀
        } else {
            return new float[]{0.70f, 1.00f};  // Tier 5: 传说
        }
    }

    // ==========================================
    // 工具方法:应用Tier限制到roll范围
    // ==========================================

    /**
     * 计算考虑Tier限制后的实际最小值
     *
     * @param minValue 词条原始最小值
     * @param maxValue 词条原始最大值
     * @param gemLevel 宝石等级
     * @return 调整后的最小值
     */
    public static float getAdjustedMinValue(float minValue, float maxValue, int gemLevel) {
        float[] range = calculateQualityRange(gemLevel);
        float minQuality = range[0];
        float valueRange = maxValue - minValue;
        return minValue + (valueRange * minQuality);
    }

    /**
     * ✅ 新增:计算考虑Tier限制后的实际最大值
     *
     * @param minValue 词条原始最小值
     * @param maxValue 词条原始最大值
     * @param gemLevel 宝石等级
     * @return 调整后的最大值
     */
    public static float getAdjustedMaxValue(float minValue, float maxValue, int gemLevel) {
        float[] range = calculateQualityRange(gemLevel);
        float maxQuality = range[1];
        float valueRange = maxValue - minValue;
        return minValue + (valueRange * maxQuality);
    }

    /**
     * 计算品质百分比(用于调试)
     *
     * @param value 实际roll的值
     * @param minValue 词条最小值
     * @param maxValue 词条最大值
     * @return 品质百分比 (0-100)
     */
    public static int calculateQualityPercent(float value, float minValue, float maxValue) {
        float range = maxValue - minValue;
        if (range <= 0) return 100;

        float percent = (value - minValue) / range;
        return Math.round(percent * 100);
    }

    // ==========================================
    // 调试方法
    // ==========================================

    /**
     * 调试:打印当前使用的模式和配置
     */
    public static void debugPrintCurrentMode() {
        if (!DEBUG_ENABLED) return;

        System.out.println("========================================");
        System.out.println("  AffixTierMapper 当前配置");
        System.out.println("========================================");
        System.out.println("  启用状态: " + (ENABLED ? "开启" : "关闭"));
        System.out.println("  计算模式: " + MODE.name());
        System.out.println("========================================");

        if (ENABLED) {
            System.out.println("\n当前模式下的品质范围:");
            System.out.println("---------|----------------");
            System.out.println("  等级   |  品质范围");
            System.out.println("---------|----------------");

            int[] testLevels = {1, 10, 20, 25, 40, 50, 60, 75, 80, 90, 100};
            for (int level : testLevels) {
                float[] range = calculateQualityRange(level);
                System.out.println(String.format("  %-6d | %5.1f%% - %5.1f%%",
                        level, range[0] * 100, range[1] * 100));
            }
            System.out.println("========================================");
        }
    }

    /**
     * 调试:测试特定等级的品质范围
     */
    public static void debugTestLevel(int gemLevel) {
        if (!DEBUG_ENABLED) return;

        float[] range = calculateQualityRange(gemLevel);
        System.out.println(String.format(
                "[AffixTier] 等级%d → 品质范围: %.1f%% - %.1f%% (模式: %s)",
                gemLevel, range[0] * 100, range[1] * 100, MODE.name()
        ));
    }

    /**
     * 调试:打印所有模式在不同等级的品质范围对比
     */
    public static void debugPrintAllModes() {
        if (!DEBUG_ENABLED) return;

        System.out.println("========================================");
        System.out.println("  AffixTierMapper 品质范围对比(所有模式)");
        System.out.println("========================================");
        System.out.println(String.format("%-8s | %-15s | %-15s | %-15s | %-15s",
                "等级", "LINEAR", "SQRT", "EXPO", "BREAKPOINT"));
        System.out.println("---------|-----------------|-----------------|-----------------|------------------");

        int[] testLevels = {1, 10, 20, 25, 40, 50, 60, 75, 80, 90, 100};

        for (int level : testLevels) {
            float[] linear = calculateLinearRange(level);
            float[] sqrt = calculateSqrtRange(level);
            float[] expo = calculateExponentialRange(level);
            float[] breakpoint = calculateBreakpointRange(level);

            System.out.println(String.format("%-8d | %5.1f%% - %5.1f%% | %5.1f%% - %5.1f%% | %5.1f%% - %5.1f%% | %5.1f%% - %5.1f%%",
                    level,
                    linear[0] * 100, linear[1] * 100,
                    sqrt[0] * 100, sqrt[1] * 100,
                    expo[0] * 100, expo[1] * 100,
                    breakpoint[0] * 100, breakpoint[1] * 100
            ));
        }

        System.out.println("========================================");
    }

    /**
     * 模拟roll分布(带上限限制)
     */
    public static void debugSimulateRolls(int gemLevel, int rollCount) {
        if (!DEBUG_ENABLED) return;

        Random random = new Random();
        float minValue = 10.0f;
        float maxValue = 100.0f;

        float[] range = calculateQualityRange(gemLevel);
        float adjustedMin = minValue + ((maxValue - minValue) * range[0]);
        float adjustedMax = minValue + ((maxValue - minValue) * range[1]);

        System.out.println(String.format(
                "\n[模拟] 等级%d宝石, roll %d次 (模式: %s)",
                gemLevel, rollCount, MODE.name()
        ));
        System.out.println(String.format(
                "原始范围: %.1f - %.1f",
                minValue, maxValue
        ));
        System.out.println(String.format(
                "限制后范围: %.1f - %.1f (品质: %.1f%% - %.1f%%)",
                adjustedMin, adjustedMax, range[0] * 100, range[1] * 100
        ));

        int[] qualityBuckets = new int[11]; // 0-10%, 10-20%, ..., 90-100%

        for (int i = 0; i < rollCount; i++) {
            float rolled = adjustedMin + random.nextFloat() * (adjustedMax - adjustedMin);
            int quality = calculateQualityPercent(rolled, minValue, maxValue);
            int bucket = Math.min(quality / 10, 10);
            qualityBuckets[bucket]++;
        }

        System.out.println("\n品质分布:");
        for (int i = 0; i < qualityBuckets.length; i++) {
            if (qualityBuckets[i] == 0) continue; // 跳过0次的区间

            int percent = (qualityBuckets[i] * 100) / rollCount;
            StringBuilder bar = new StringBuilder();
            for (int j = 0; j < percent / 2; j++) {
                bar.append("█");
            }
            System.out.println(String.format("  %3d-%3d%%: %4d次 (%3d%%) %s",
                    i * 10, (i + 1) * 10, qualityBuckets[i], percent, bar.toString()));
        }
        System.out.println("========================================");
    }

    /**
     * 调试:验证特定宝石的品质是否在合法范围内
     */
    public static boolean validateQuality(int gemLevel, int actualQuality) {
        float[] range = calculateQualityRange(gemLevel);
        int minQuality = Math.round(range[0] * 100);
        int maxQuality = Math.round(range[1] * 100);

        boolean valid = actualQuality >= minQuality && actualQuality <= maxQuality;

        if (!valid && DEBUG_ENABLED) {
            System.err.println(String.format(
                    "[AffixTier] ❌ 品质异常！等级%d宝石的品质%d%%超出了合法范围[%d%% - %d%%]",
                    gemLevel, actualQuality, minQuality, maxQuality
            ));
        }

        return valid;
    }
}