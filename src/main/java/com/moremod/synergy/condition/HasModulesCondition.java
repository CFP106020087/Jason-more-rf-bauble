package com.moremod.synergy.condition;

import com.moremod.synergy.api.ISynergyCondition;
import com.moremod.synergy.core.SynergyContext;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 模块存在条件
 *
 * 检查玩家是否拥有指定的模块组合。
 */
public class HasModulesCondition implements ISynergyCondition {

    private final List<String> requiredModules;
    private final boolean requireAll;
    private final boolean requireActive;

    /**
     * @param requireAll true = 需要所有模块，false = 需要任意一个
     * @param requireActive true = 模块必须处于激活状态
     * @param moduleIds 模块ID列表
     */
    public HasModulesCondition(boolean requireAll, boolean requireActive, String... moduleIds) {
        this.requireAll = requireAll;
        this.requireActive = requireActive;
        this.requiredModules = Arrays.stream(moduleIds)
                .map(id -> id.toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    @Override
    public boolean test(SynergyContext context) {
        if (requiredModules.isEmpty()) {
            return true;
        }

        if (requireAll) {
            // 检查是否拥有所有模块
            for (String moduleId : requiredModules) {
                if (requireActive) {
                    if (!context.hasActiveModule(moduleId)) {
                        return false;
                    }
                } else {
                    if (context.getModule(moduleId) == null) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            // 检查是否拥有任意一个模块
            for (String moduleId : requiredModules) {
                if (requireActive) {
                    if (context.hasActiveModule(moduleId)) {
                        return true;
                    }
                } else {
                    if (context.getModule(moduleId) != null) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @Override
    public String getDescription() {
        String mode = requireAll ? "all of" : "any of";
        String activeStr = requireActive ? " (active)" : "";
        return "Has " + mode + " " + requiredModules + activeStr;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 需要所有指定的激活模块
     */
    public static HasModulesCondition allActive(String... moduleIds) {
        return new HasModulesCondition(true, true, moduleIds);
    }

    /**
     * 需要所有指定的模块（不论是否激活）
     */
    public static HasModulesCondition all(String... moduleIds) {
        return new HasModulesCondition(true, false, moduleIds);
    }

    /**
     * 需要任意一个激活模块
     */
    public static HasModulesCondition anyActive(String... moduleIds) {
        return new HasModulesCondition(false, true, moduleIds);
    }

    /**
     * 需要任意一个模块
     */
    public static HasModulesCondition any(String... moduleIds) {
        return new HasModulesCondition(false, false, moduleIds);
    }
}
