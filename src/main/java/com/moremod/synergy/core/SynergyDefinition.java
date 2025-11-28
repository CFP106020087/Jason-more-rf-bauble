package com.moremod.synergy.core;

import com.moremod.synergy.api.IInstalledModuleView;
import com.moremod.synergy.api.ISynergyCondition;
import com.moremod.synergy.api.ISynergyEffect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Synergy 定义
 *
 * 描述一个完整的 Synergy 规则，包括：
 * - 唯一标识
 * - 所需模块组合（支持有序链路）
 * - 触发条件
 * - 执行效果
 * - 元数据（显示名称、描述、图标等）
 *
 * 设计特点：
 * - 不可变：创建后不能修改
 * - 支持链路：requiredModules 可以是有序的（A→B→C）
 * - 支持图结构：通过 moduleLinks 定义节点间的连接
 */
public class SynergyDefinition {

    // ==================== 核心字段 ====================

    private final String id;
    private final String displayName;
    private final String description;
    private final List<String> requiredModules;
    private final List<ModuleLink> moduleLinks;
    private final List<ISynergyCondition> conditions;
    private final List<ISynergyEffect> effects;
    private final Set<SynergyEventType> triggerEvents;
    private final boolean requireAllModulesActive;
    private final int priority;
    private final boolean enabled;
    private final String category;

    // ==================== 构造器 ====================

    private SynergyDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.requiredModules = Collections.unmodifiableList(new ArrayList<>(builder.requiredModules));
        this.moduleLinks = Collections.unmodifiableList(new ArrayList<>(builder.moduleLinks));
        this.conditions = Collections.unmodifiableList(new ArrayList<>(builder.conditions));
        this.effects = Collections.unmodifiableList(new ArrayList<>(builder.effects));
        this.triggerEvents = Collections.unmodifiableSet(EnumSet.copyOf(
                builder.triggerEvents.isEmpty() ? EnumSet.of(SynergyEventType.ANY) : builder.triggerEvents));
        this.requireAllModulesActive = builder.requireAllModulesActive;
        this.priority = builder.priority;
        this.enabled = builder.enabled;
        this.category = builder.category;
    }

    // ==================== 访问器 ====================

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName;
    }

    @Nonnull
    public String getDescription() {
        return description;
    }

    /**
     * 获取所需模块列表（有序）
     * 如果 Synergy 需要模块按特定顺序链接，这个列表是有序的
     */
    @Nonnull
    public List<String> getRequiredModules() {
        return requiredModules;
    }

    /**
     * 获取模块链接定义（用于 GUI 可视化）
     */
    @Nonnull
    public List<ModuleLink> getModuleLinks() {
        return moduleLinks;
    }

    @Nonnull
    public List<ISynergyCondition> getConditions() {
        return conditions;
    }

    @Nonnull
    public List<ISynergyEffect> getEffects() {
        return effects;
    }

    @Nonnull
    public Set<SynergyEventType> getTriggerEvents() {
        return triggerEvents;
    }

    public boolean isRequireAllModulesActive() {
        return requireAllModulesActive;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取 Synergy 类别（用于 GUI 分类显示）
     * @return 类别字符串，如 "combat", "energy", "mechanism" 等
     */
    @Nonnull
    public String getCategory() {
        return category != null ? category : "misc";
    }

    // ==================== 逻辑方法 ====================

    /**
     * 检查上下文是否满足此 Synergy 的所有要求
     *
     * @param context Synergy 上下文
     * @return true 如果满足所有要求
     */
    public boolean matches(SynergyContext context) {
        if (!enabled) {
            return false;
        }

        // 1. 检查事件类型
        if (!matchesEventType(context.getEventType())) {
            return false;
        }

        // 2. 检查模块要求
        if (!matchesModuleRequirements(context)) {
            return false;
        }

        // 3. 检查所有条件
        for (ISynergyCondition condition : conditions) {
            if (!condition.test(context)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查事件类型是否匹配
     */
    public boolean matchesEventType(SynergyEventType eventType) {
        for (SynergyEventType trigger : triggerEvents) {
            if (trigger.matches(eventType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查模块要求是否满足
     */
    public boolean matchesModuleRequirements(SynergyContext context) {
        if (requiredModules.isEmpty()) {
            return true;
        }

        Set<String> playerModules = requireAllModulesActive
                ? context.getActiveModuleIds()
                : getAllPlayerModuleIds(context);

        for (String required : requiredModules) {
            if (!playerModules.contains(required.toUpperCase(Locale.ROOT))) {
                return false;
            }
        }

        return true;
    }

    private Set<String> getAllPlayerModuleIds(SynergyContext context) {
        Set<String> ids = new HashSet<>();
        for (IInstalledModuleView module : context.getModules()) {
            ids.add(module.getModuleId().toUpperCase(Locale.ROOT));
        }
        return ids;
    }

    /**
     * 执行此 Synergy 的所有效果
     *
     * @param context Synergy 上下文
     */
    public void execute(SynergyContext context) {
        if (!enabled) return;

        // 按优先级排序执行效果
        List<ISynergyEffect> sortedEffects = new ArrayList<>(effects);
        sortedEffects.sort(Comparator.comparingInt(ISynergyEffect::getPriority));

        for (ISynergyEffect effect : sortedEffects) {
            try {
                if (effect.canApply(context)) {
                    effect.apply(context);
                }
            } catch (Exception e) {
                System.err.println("[Synergy] Error executing effect for " + id + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // ==================== 内部类 ====================

    /**
     * 模块链接定义（用于 GUI 拖拽连线）
     *
     * 表示两个模块之间的连接关系
     */
    public static class ModuleLink {
        private final String fromModule;
        private final String toModule;
        private final String linkType;  // "required", "optional", "exclusive" 等

        public ModuleLink(String fromModule, String toModule, String linkType) {
            this.fromModule = fromModule;
            this.toModule = toModule;
            this.linkType = linkType;
        }

        public ModuleLink(String fromModule, String toModule) {
            this(fromModule, toModule, "required");
        }

        public String getFromModule() { return fromModule; }
        public String getToModule() { return toModule; }
        public String getLinkType() { return linkType; }

        @Override
        public String toString() {
            return fromModule + " -[" + linkType + "]-> " + toModule;
        }
    }

    // ==================== Builder ====================

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private String displayName;
        private String description = "";
        private List<String> requiredModules = new ArrayList<>();
        private List<ModuleLink> moduleLinks = new ArrayList<>();
        private List<ISynergyCondition> conditions = new ArrayList<>();
        private List<ISynergyEffect> effects = new ArrayList<>();
        private Set<SynergyEventType> triggerEvents = EnumSet.noneOf(SynergyEventType.class);
        private boolean requireAllModulesActive = true;
        private int priority = 100;
        private boolean enabled = true;
        private String category = "misc";

        public Builder(String id) {
            this.id = Objects.requireNonNull(id, "Synergy ID cannot be null");
            this.displayName = id;
        }

        public Builder displayName(String name) {
            this.displayName = name;
            return this;
        }

        public Builder description(String desc) {
            this.description = desc != null ? desc : "";
            return this;
        }

        /**
         * 添加所需模块（可以多次调用来添加多个）
         */
        public Builder requireModule(String moduleId) {
            this.requiredModules.add(moduleId.toUpperCase(Locale.ROOT));
            return this;
        }

        /**
         * 设置所有所需模块
         */
        public Builder requireModules(String... moduleIds) {
            this.requiredModules.clear();
            for (String id : moduleIds) {
                this.requiredModules.add(id.toUpperCase(Locale.ROOT));
            }
            return this;
        }

        /**
         * 添加模块链接（用于 GUI）
         */
        public Builder addLink(String from, String to) {
            this.moduleLinks.add(new ModuleLink(from.toUpperCase(Locale.ROOT), to.toUpperCase(Locale.ROOT)));
            return this;
        }

        public Builder addLink(String from, String to, String linkType) {
            this.moduleLinks.add(new ModuleLink(from.toUpperCase(Locale.ROOT), to.toUpperCase(Locale.ROOT), linkType));
            return this;
        }

        public Builder addCondition(ISynergyCondition condition) {
            this.conditions.add(condition);
            return this;
        }

        public Builder addEffect(ISynergyEffect effect) {
            this.effects.add(effect);
            return this;
        }

        public Builder triggerOn(SynergyEventType... eventTypes) {
            this.triggerEvents.clear();
            Collections.addAll(this.triggerEvents, eventTypes);
            return this;
        }

        public Builder requireAllModulesActive(boolean require) {
            this.requireAllModulesActive = require;
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

        /**
         * 设置 Synergy 类别（用于 GUI 分类显示）
         * @param category 类别字符串，如 "combat", "energy", "mechanism" 等
         */
        public Builder category(String category) {
            this.category = category != null ? category : "misc";
            return this;
        }

        public SynergyDefinition build() {
            return new SynergyDefinition(this);
        }
    }

    @Override
    public String toString() {
        return "SynergyDefinition{" +
                "id='" + id + '\'' +
                ", modules=" + requiredModules +
                ", events=" + triggerEvents +
                '}';
    }
}
