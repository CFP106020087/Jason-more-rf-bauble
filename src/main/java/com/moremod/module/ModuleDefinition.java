package com.moremod.module;

import net.minecraft.util.text.TextFormatting;
import java.util.function.Function;

/**
 * 模块定义类 - 包含创建一个完整模块所需的所有信息
 *
 * 使用示例:
 * <pre>
 * ModuleDefinition.builder("MAGIC_ABSORB")
 *     .displayName("魔力吸收")
 *     .color(TextFormatting.DARK_PURPLE)
 *     .category(Category.COMBAT)
 *     .maxLevel(3)
 *     .levelDescriptions(lv -> {
 *         switch(lv) {
 *             case 1: return new String[]{"吸收少量法伤", "叠加少量余灼"};
 *             case 2: return new String[]{"更高吸收率", "更快余灼累积"};
 *             case 3: return new String[]{"触发魔力爆心", "强力爆发伤害"};
 *             default: return new String[]{};
 *         }
 *     })
 *     .register();
 * </pre>
 */
public class ModuleDefinition {

    public enum Category {
        SURVIVAL("生存", TextFormatting.GREEN),
        AUXILIARY("辅助", TextFormatting.AQUA),
        COMBAT("战斗", TextFormatting.RED),
        ENERGY("能源", TextFormatting.YELLOW);

        public final String displayName;
        public final TextFormatting color;

        Category(String displayName, TextFormatting color) {
            this.displayName = displayName;
            this.color = color;
        }
    }

    // 基本信息
    public final String id;                    // 模块ID (大写，如 "MAGIC_ABSORB")
    public final String displayName;           // 显示名称 (如 "魔力吸收")
    public final TextFormatting color;         // 颜色
    public final Category category;            // 类别
    public final int maxLevel;                 // 最大等级

    // 物品信息
    public final Function<Integer, String[]> levelDescriptions;  // 每级描述
    public final Function<Integer, Integer> stackSizes;          // 每级堆叠数

    private ModuleDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.color = builder.color;
        this.category = builder.category;
        this.maxLevel = builder.maxLevel;
        this.levelDescriptions = builder.levelDescriptions;
        this.stackSizes = builder.stackSizes;
    }

    /**
     * 获取注册表名 (小写，如 "magic_absorb_lv1")
     */
    public String getRegistryName(int level) {
        return id.toLowerCase() + "_lv" + level;
    }

    /**
     * 获取语言文件键名
     */
    public String getLangKey(int level) {
        return "item." + getRegistryName(level) + ".name";
    }

    /**
     * 获取语言文件值
     */
    public String getLangValue(int level) {
        return displayName + " Lv." + level;
    }

    /**
     * 获取指定等级的描述
     */
    public String[] getDescriptions(int level) {
        return levelDescriptions.apply(level);
    }

    /**
     * 获取指定等级的堆叠数
     */
    public int getStackSize(int level) {
        return stackSizes.apply(level);
    }

    // ========== Builder ==========

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private String displayName;
        private TextFormatting color = TextFormatting.WHITE;
        private Category category = Category.AUXILIARY;
        private int maxLevel = 3;
        private Function<Integer, String[]> levelDescriptions = lv -> new String[]{};
        private Function<Integer, Integer> stackSizes = lv -> {
            // 默认堆叠数: lv1=16, lv2=8, lv3=4, lv4=2, lv5=1
            switch (lv) {
                case 1: return 16;
                case 2: return 8;
                case 3: return 4;
                case 4: return 2;
                default: return 1;
            }
        };

        public Builder(String id) {
            this.id = id.toUpperCase();
            this.displayName = id; // 默认使用ID作为名称
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder color(TextFormatting color) {
            this.color = color;
            return this;
        }

        public Builder category(Category category) {
            this.category = category;
            return this;
        }

        public Builder maxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
            return this;
        }

        public Builder levelDescriptions(Function<Integer, String[]> levelDescriptions) {
            this.levelDescriptions = levelDescriptions;
            return this;
        }

        /**
         * 简化版：所有等级使用相同描述模板
         */
        public Builder descriptions(String... baseDescriptions) {
            this.levelDescriptions = lv -> baseDescriptions;
            return this;
        }

        public Builder stackSizes(Function<Integer, Integer> stackSizes) {
            this.stackSizes = stackSizes;
            return this;
        }

        public ModuleDefinition build() {
            return new ModuleDefinition(this);
        }

        /**
         * 构建并注册到 ModuleAutoRegistry
         */
        public ModuleDefinition register() {
            ModuleDefinition def = build();
            ModuleAutoRegistry.register(def);
            return def;
        }
    }
}
