package com.moremod.core.registry;

import net.minecraft.util.text.TextFormatting;

import java.util.*;

/**
 * 升级定义 - 描述一个升级模块的所有静态属性
 */
public class UpgradeDefinition {

    private final String id;                    // 规范ID（全大写）
    private final String displayName;           // 显示名称
    private final TextFormatting color;         // 显示颜色
    private final int maxLevel;                 // 最大等级
    private final UpgradeCategory category;     // 类别
    private final Set<String> aliases;          // 别名集合

    /**
     * 升级类别（用于分组显示）
     */
    public enum UpgradeCategory {
        BASIC("基础", TextFormatting.WHITE),
        SURVIVAL("生存", TextFormatting.GREEN),
        AUXILIARY("辅助", TextFormatting.AQUA),
        COMBAT("战斗", TextFormatting.RED),
        ENERGY("能源", TextFormatting.YELLOW);

        private final String displayName;
        private final TextFormatting color;

        UpgradeCategory(String displayName, TextFormatting color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public TextFormatting getColor() {
            return color;
        }
    }

    /**
     * 构造函数
     */
    public UpgradeDefinition(String id, String displayName, TextFormatting color,
                           int maxLevel, UpgradeCategory category) {
        this(id, displayName, color, maxLevel, category, new HashSet<>());
    }

    /**
     * 完整构造函数（含别名）
     */
    public UpgradeDefinition(String id, String displayName, TextFormatting color,
                           int maxLevel, UpgradeCategory category, Set<String> aliases) {
        this.id = id.trim().toUpperCase(Locale.ROOT);
        this.displayName = displayName;
        this.color = color;
        this.maxLevel = maxLevel;
        this.category = category;
        this.aliases = new HashSet<>(aliases);
    }

    /**
     * 建造器模式
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private String displayName;
        private TextFormatting color = TextFormatting.WHITE;
        private int maxLevel = 3;
        private UpgradeCategory category = UpgradeCategory.BASIC;
        private final Set<String> aliases = new HashSet<>();

        private Builder(String id) {
            this.id = id;
            this.displayName = id; // 默认使用ID作为显示名称
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder color(TextFormatting color) {
            this.color = color;
            return this;
        }

        public Builder maxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
            return this;
        }

        public Builder category(UpgradeCategory category) {
            this.category = category;
            return this;
        }

        public Builder alias(String alias) {
            this.aliases.add(alias.trim().toUpperCase(Locale.ROOT));
            return this;
        }

        public Builder aliases(String... aliases) {
            for (String alias : aliases) {
                this.aliases.add(alias.trim().toUpperCase(Locale.ROOT));
            }
            return this;
        }

        public UpgradeDefinition build() {
            return new UpgradeDefinition(id, displayName, color, maxLevel, category, aliases);
        }
    }

    // ===== Getters =====

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TextFormatting getColor() {
        return color;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public UpgradeCategory getCategory() {
        return category;
    }

    public Set<String> getAliases() {
        return Collections.unmodifiableSet(aliases);
    }

    /**
     * 检查给定的ID是否匹配此升级（包括别名）
     */
    public boolean matches(String checkId) {
        if (checkId == null) return false;
        String normalized = checkId.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals(id)) return true;
        return aliases.contains(normalized);
    }

    @Override
    public String toString() {
        return String.format("UpgradeDefinition[id=%s, name=%s, max=%d, category=%s, aliases=%s]",
                id, displayName, maxLevel, category, aliases);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpgradeDefinition that = (UpgradeDefinition) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
