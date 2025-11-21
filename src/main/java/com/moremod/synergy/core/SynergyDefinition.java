package com.moremod.synergy.core;

import com.moremod.synergy.api.ISynergyCondition;
import com.moremod.synergy.api.ISynergyEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Synergy 定义 - 描述一个模块联动规则
 *
 * 说明：
 * - 包含唯一 ID、所需模块组合、触发条件、效果列表
 * - 支持链式结构（为未来 GUI 拖拽连线预留）
 * - 不可变对象，线程安全
 */
public final class SynergyDefinition {

    private final String id;
    private final String displayName;
    private final String description;

    private final List<String> requiredModules;        // 所需模块 ID 列表
    private final ModuleChain moduleChain;              // 模块链结构（为 GUI 预留）

    private final List<ISynergyCondition> conditions;   // 触发条件列表
    private final List<ISynergyEffect> effects;         // 效果列表

    private final int priority;                         // 优先级（数值越小越先执行）
    private final boolean enabled;                      // 是否启用

    private SynergyDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName != null ? builder.displayName : id;
        this.description = builder.description != null ? builder.description : "";

        this.requiredModules = Collections.unmodifiableList(new ArrayList<>(builder.requiredModules));
        this.moduleChain = builder.moduleChain;

        this.conditions = Collections.unmodifiableList(new ArrayList<>(builder.conditions));
        this.effects = Collections.unmodifiableList(new ArrayList<>(builder.effects));

        this.priority = builder.priority;
        this.enabled = builder.enabled;
    }

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public List<String> getRequiredModules() { return requiredModules; }
    public ModuleChain getModuleChain() { return moduleChain; }
    public List<ISynergyCondition> getConditions() { return conditions; }
    public List<ISynergyEffect> getEffects() { return effects; }
    public int getPriority() { return priority; }
    public boolean isEnabled() { return enabled; }

    @Override
    public String toString() {
        return String.format("Synergy[%s] modules=%s conditions=%d effects=%d",
                id, requiredModules, conditions.size(), effects.size());
    }

    /**
     * Builder 模式构建器
     */
    public static class Builder {
        private final String id;
        private String displayName;
        private String description;

        private List<String> requiredModules = new ArrayList<>();
        private ModuleChain moduleChain;

        private List<ISynergyCondition> conditions = new ArrayList<>();
        private List<ISynergyEffect> effects = new ArrayList<>();

        private int priority = 100;
        private boolean enabled = true;

        public Builder(String id) {
            this.id = id;
        }

        public Builder displayName(String name) {
            this.displayName = name;
            return this;
        }

        public Builder description(String desc) {
            this.description = desc;
            return this;
        }

        public Builder requireModules(String... moduleIds) {
            this.requiredModules.addAll(Arrays.asList(moduleIds));
            return this;
        }

        public Builder requireModules(List<String> moduleIds) {
            this.requiredModules.addAll(moduleIds);
            return this;
        }

        public Builder chain(ModuleChain chain) {
            this.moduleChain = chain;
            // 自动从链中提取所需模块
            if (chain != null) {
                this.requiredModules.addAll(chain.getAllModuleIds());
            }
            return this;
        }

        public Builder condition(ISynergyCondition condition) {
            this.conditions.add(condition);
            return this;
        }

        public Builder conditions(ISynergyCondition... conditions) {
            this.conditions.addAll(Arrays.asList(conditions));
            return this;
        }

        public Builder effect(ISynergyEffect effect) {
            this.effects.add(effect);
            return this;
        }

        public Builder effects(ISynergyEffect... effects) {
            this.effects.addAll(Arrays.asList(effects));
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public SynergyDefinition build() {
            if (id == null || id.isEmpty()) {
                throw new IllegalStateException("Synergy ID cannot be null or empty");
            }
            if (requiredModules.isEmpty() && moduleChain == null) {
                throw new IllegalStateException("Synergy must require at least one module");
            }
            if (effects.isEmpty()) {
                throw new IllegalStateException("Synergy must have at least one effect");
            }
            return new SynergyDefinition(this);
        }
    }
}
