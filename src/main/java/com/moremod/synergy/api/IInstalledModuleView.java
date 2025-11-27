package com.moremod.synergy.api;

/**
 * 已安装模块的只读视图接口
 *
 * 这是 Synergy 系统与现有模块系统之间的解耦层。
 * Synergy 系统只需要知道模块的基本信息，不关心模块的具体实现。
 *
 * 设计原则：
 * - 只读：不修改模块状态
 * - 最小化：只暴露 Synergy 系统需要的信息
 * - 解耦：不依赖具体的模块类
 */
public interface IInstalledModuleView {

    /**
     * 获取模块的唯一标识符
     *
     * @return 模块ID（如 "CRITICAL_STRIKE", "YELLOW_SHIELD" 等）
     */
    String getModuleId();

    /**
     * 获取模块的当前等级
     *
     * @return 等级值，0 表示暂停或未安装
     */
    int getLevel();

    /**
     * 检查模块是否处于激活状态
     *
     * 激活状态意味着：
     * - 等级 > 0
     * - 未被禁用
     * - 能量状态允许运行
     *
     * @return true 如果模块正在运行
     */
    boolean isActive();

    /**
     * 获取模块的显示名称（可选）
     *
     * @return 显示名称，如果不可用则返回 moduleId
     */
    default String getDisplayName() {
        return getModuleId();
    }

    /**
     * 获取模块的最大等级（可选）
     *
     * @return 最大等级，默认为 5
     */
    default int getMaxLevel() {
        return 5;
    }

    /**
     * 获取模块的类别标签（可选，用于 Synergy 规则匹配）
     *
     * @return 类别名称，如 "COMBAT", "SURVIVAL", "AUXILIARY", "ENERGY"
     */
    default String getCategory() {
        return "UNKNOWN";
    }
}
