package com.moremod.synergy.bridge;

import com.moremod.synergy.api.IInstalledModuleView;

/**
 * IInstalledModuleView 的简单实现
 *
 * 用于从现有模块系统创建只读视图。
 */
public class InstalledModuleViewImpl implements IInstalledModuleView {

    private final String moduleId;
    private final int level;
    private final boolean active;
    private final String displayName;
    private final int maxLevel;
    private final String category;

    public InstalledModuleViewImpl(String moduleId, int level, boolean active,
                                   String displayName, int maxLevel, String category) {
        this.moduleId = moduleId;
        this.level = level;
        this.active = active;
        this.displayName = displayName != null ? displayName : moduleId;
        this.maxLevel = maxLevel;
        this.category = category != null ? category : "UNKNOWN";
    }

    /**
     * 简化构造器（用于快速创建）
     */
    public InstalledModuleViewImpl(String moduleId, int level, boolean active) {
        this(moduleId, level, active, moduleId, 5, "UNKNOWN");
    }

    @Override
    public String getModuleId() {
        return moduleId;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return "ModuleView{" +
                "id='" + moduleId + '\'' +
                ", level=" + level +
                ", active=" + active +
                '}';
    }

    // ==================== Builder ====================

    public static Builder builder(String moduleId) {
        return new Builder(moduleId);
    }

    public static class Builder {
        private final String moduleId;
        private int level = 0;
        private boolean active = false;
        private String displayName = null;
        private int maxLevel = 5;
        private String category = "UNKNOWN";

        public Builder(String moduleId) {
            this.moduleId = moduleId;
        }

        public Builder level(int level) {
            this.level = level;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder displayName(String name) {
            this.displayName = name;
            return this;
        }

        public Builder maxLevel(int max) {
            this.maxLevel = max;
            return this;
        }

        public Builder category(String cat) {
            this.category = cat;
            return this;
        }

        public InstalledModuleViewImpl build() {
            return new InstalledModuleViewImpl(moduleId, level, active, displayName, maxLevel, category);
        }
    }
}
