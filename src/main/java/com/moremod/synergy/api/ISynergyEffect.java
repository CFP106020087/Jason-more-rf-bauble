package com.moremod.synergy.api;

import com.moremod.synergy.core.SynergyContext;

/**
 * Synergy 效果接口
 *
 * 定义 Synergy 触发后执行的效果。
 * 效果可以包括：
 * - 额外伤害
 * - 能量返还
 * - 添加状态效果
 * - 生成护盾
 * - 修改属性
 *
 * 设计原则：
 * - 单一职责：每个效果只做一件事
 * - 可组合：多个效果可以顺序执行
 * - 安全：效果执行失败不应导致崩溃
 */
public interface ISynergyEffect {

    /**
     * 执行效果
     *
     * @param context 包含事件、玩家、模块等信息的上下文
     */
    void apply(SynergyContext context);

    /**
     * 获取效果的描述（用于调试和 GUI 显示）
     *
     * @return 人类可读的效果描述
     */
    default String getDescription() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取效果的唯一标识符（用于序列化）
     *
     * @return 效果类型 ID
     */
    default String getEffectType() {
        return this.getClass().getSimpleName().toLowerCase().replace("effect", "");
    }

    /**
     * 效果是否可以在当前上下文中执行
     *
     * 这是一个可选的前置检查，用于在 apply 之前确认效果可以安全执行。
     *
     * @param context 上下文
     * @return true 如果效果可以执行
     */
    default boolean canApply(SynergyContext context) {
        return true;
    }

    /**
     * 获取效果的优先级（数值越小越先执行）
     *
     * 用于控制多个效果的执行顺序。
     * 默认优先级为 100。
     *
     * @return 优先级值
     */
    default int getPriority() {
        return 100;
    }

    // ==================== 组合器 ====================

    /**
     * 组合两个效果，顺序执行
     *
     * @param other 另一个效果
     * @return 组合后的效果
     */
    default ISynergyEffect andThen(ISynergyEffect other) {
        ISynergyEffect self = this;
        return new ISynergyEffect() {
            @Override
            public void apply(SynergyContext context) {
                self.apply(context);
                other.apply(context);
            }

            @Override
            public String getDescription() {
                return self.getDescription() + " -> " + other.getDescription();
            }

            @Override
            public int getPriority() {
                return Math.min(self.getPriority(), other.getPriority());
            }
        };
    }

    /**
     * 创建一个条件执行的效果
     *
     * @param condition 执行条件
     * @return 带条件的效果
     */
    default ISynergyEffect onlyIf(ISynergyCondition condition) {
        ISynergyEffect self = this;
        return new ISynergyEffect() {
            @Override
            public void apply(SynergyContext context) {
                if (condition.test(context)) {
                    self.apply(context);
                }
            }

            @Override
            public String getDescription() {
                return self.getDescription() + " (if " + condition.getDescription() + ")";
            }

            @Override
            public boolean canApply(SynergyContext context) {
                return condition.test(context) && self.canApply(context);
            }
        };
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 空效果（不做任何事）
     */
    static ISynergyEffect noOp() {
        return context -> {};
    }

    /**
     * 创建一个简单的日志效果（用于调试）
     *
     * @param message 日志消息
     * @return 日志效果
     */
    static ISynergyEffect log(String message) {
        return context -> {
            System.out.println("[Synergy] " + message + " - Player: " +
                    (context.getPlayer() != null ? context.getPlayer().getName() : "null"));
        };
    }
}
