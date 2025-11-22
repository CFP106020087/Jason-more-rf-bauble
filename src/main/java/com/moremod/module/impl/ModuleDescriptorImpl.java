package com.moremod.module.impl;

import com.moremod.module.api.IModuleDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 模块描述符默认实现
 */
public class ModuleDescriptorImpl implements IModuleDescriptor {

    private final String moduleId;
    private final String name;
    private final String version;
    private final String description;
    private final String[] authors;
    private final String[] dependencies;
    private final boolean optional;
    private final int priority;
    private final Map<String, Object> properties;

    private ModuleDescriptorImpl(Builder builder) {
        this.moduleId = builder.moduleId;
        this.name = builder.name;
        this.version = builder.version;
        this.description = builder.description;
        this.authors = builder.authors;
        this.dependencies = builder.dependencies;
        this.optional = builder.optional;
        this.priority = builder.priority;
        this.properties = new HashMap<>(builder.properties);
    }

    @Nonnull
    @Override
    public String getModuleId() {
        return moduleId;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    @Nonnull
    @Override
    public String getVersion() {
        return version;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    @Nonnull
    @Override
    public String[] getAuthors() {
        return authors;
    }

    @Nonnull
    @Override
    public String[] getDependencies() {
        return dependencies;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Nullable
    @Override
    public Object getProperty(@Nonnull String key) {
        return properties.get(key);
    }

    /**
     * Builder for ModuleDescriptor
     */
    public static class Builder {
        private final String moduleId;
        private String name;
        private String version = "1.0.0";
        private String description = "";
        private String[] authors = new String[0];
        private String[] dependencies = new String[0];
        private boolean optional = true;
        private int priority = 0;
        private final Map<String, Object> properties = new HashMap<>();

        public Builder(String moduleId) {
            this.moduleId = moduleId;
            this.name = moduleId;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder authors(String... authors) {
            this.authors = authors;
            return this;
        }

        public Builder dependencies(String... dependencies) {
            this.dependencies = dependencies;
            return this;
        }

        public Builder optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public ModuleDescriptorImpl build() {
            return new ModuleDescriptorImpl(this);
        }
    }
}
