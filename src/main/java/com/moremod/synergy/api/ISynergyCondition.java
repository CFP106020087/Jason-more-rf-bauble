package com.moremod.synergy.api;

import com.moremod.synergy.core.SynergyContext;

/**
 * Synergy 触发条件接口
 *
 * 定义何时触发一个 Synergy 效果。
 * 条件可以基于：
 * - 玩家拥有的模块组合
 * - 事件类型（攻击、受伤、tick 等）
 * - 目标属性（是否为怪物、生命值状态等）
 * - 环境因素（能量状态、维度等）
 *
 * 设计原则：
 * - 无状态：条件判断不应修改任何状态
 * - 可组合：多个条件可以通过 AND/OR 组合
 */
public interface ISynergyCondition {

    /**
     * 检查条件是否满足
     *
     * @param context 包含事件、玩家、模块等信息的上下文
     * @return true 如果条件满足
     */
    boolean test(SynergyContext context);

    /**
     * 获取条件的描述（用于调试和 GUI 显示）
     *
     * @return 人类可读的条件描述
     */
    default String getDescription() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取条件的唯一标识符（用于序列化）
     *
     * @return 条件类型 ID
     */
    default String getConditionType() {
        return this.getClass().getSimpleName().toLowerCase().replace("condition", "");
    }

    // ==================== 组合器 ====================

    /**
     * 创建 AND 组合条件
     *
     * @param other 另一个条件
     * @return 组合后的条件（两者都满足才返回 true）
     */
    default ISynergyCondition and(ISynergyCondition other) {
        ISynergyCondition self = this;
        return new ISynergyCondition() {
            @Override
            public boolean test(SynergyContext context) {
                return self.test(context) && other.test(context);
            }

            @Override
            public String getDescription() {
                return "(" + self.getDescription() + " AND " + other.getDescription() + ")";
            }
        };
    }

    /**
     * 创建 OR 组合条件
     *
     * @param other 另一个条件
     * @return 组合后的条件（任一满足即返回 true）
     */
    default ISynergyCondition or(ISynergyCondition other) {
        ISynergyCondition self = this;
        return new ISynergyCondition() {
            @Override
            public boolean test(SynergyContext context) {
                return self.test(context) || other.test(context);
            }

            @Override
            public String getDescription() {
                return "(" + self.getDescription() + " OR " + other.getDescription() + ")";
            }
        };
    }

    /**
     * 创建 NOT 条件
     *
     * @return 取反后的条件
     */
    default ISynergyCondition negate() {
        ISynergyCondition self = this;
        return new ISynergyCondition() {
            @Override
            public boolean test(SynergyContext context) {
                return !self.test(context);
            }

            @Override
            public String getDescription() {
                return "NOT(" + self.getDescription() + ")";
            }
        };
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 始终为 true 的条件
     */
    static ISynergyCondition always() {
        return context -> true;
    }

    /**
     * 始终为 false 的条件
     */
    static ISynergyCondition never() {
        return context -> false;
    }
}
