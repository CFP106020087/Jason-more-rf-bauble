package com.moremod.module;

import com.moremod.module.effect.IModuleEventHandler;
import com.moremod.module.effect.ModuleEffect;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                      模块定义类 (ModuleDefinition)                            ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  包含创建一个完整模块所需的所有信息，支持两种效果定义方式:                          ║
 * ║  1. 简单效果 - 使用 .effects() 添加预定义效果                                   ║
 * ║  2. 完整事件 - 使用 .handler() 添加自定义事件处理器                              ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                         方式一: 简单效果 (.effects)                            ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  ModuleDefinition.builder("SPEED_BOOST")                                     ║
 * ║      .displayName("速度提升")                                                  ║
 * ║      .effects(                                                               ║
 * ║          ModuleEffect.attribute(MOVEMENT_SPEED)                              ║
 * ║              .baseValue(0.2).perLevel(0.2).build(),                          ║
 * ║          ModuleEffect.potion(MobEffects.SPEED)                               ║
 * ║              .amplifierPerLevel(1).build()                                   ║
 * ║      )                                                                       ║
 * ║      .register();                                                            ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                     方式二: 完整事件处理器 (.handler)                           ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  // 1. 先实现 IModuleEventHandler 接口                                        ║
 * ║  public class MagicAbsorbHandler implements IModuleEventHandler {            ║
 * ║      @Override                                                               ║
 * ║      public float onPlayerHurt(EventContext ctx, DamageSource src,           ║
 * ║                                 float damage) {                              ║
 * ║          if (!src.isMagicDamage()) return damage;                            ║
 * ║          float absorbed = damage * (0.1f + ctx.level * 0.1f);                ║
 * ║          ctx.setNBT("ember", ctx.getNBTFloat("ember") + absorbed);           ║
 * ║          return damage - absorbed;                                           ║
 * ║      }                                                                       ║
 * ║                                                                              ║
 * ║      @Override                                                               ║
 * ║      public int getPassiveEnergyCost() { return 5; }                         ║
 * ║  }                                                                           ║
 * ║                                                                              ║
 * ║  // 2. 然后注册模块                                                            ║
 * ║  ModuleDefinition.builder("MAGIC_ABSORB")                                    ║
 * ║      .displayName("魔力吸收")                                                  ║
 * ║      .handler(new MagicAbsorbHandler())                                      ║
 * ║      .register();                                                            ║
 * ║                                                                              ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                     IModuleEventHandler 可用事件钩子                           ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  【Tick】 onTick / onSecondTick / getTickInterval                            ║
 * ║  【攻击】 onPlayerAttack / onPlayerHitEntity / onPlayerKillEntity            ║
 * ║  【受伤】 onPlayerHurt / onPlayerAttacked / onPlayerDeath                    ║
 * ║  【交互】 onRightClickBlock / onRightClickItem / onLeftClickBlock            ║
 * ║  【状态】 onModuleActivated / onModuleDeactivated / onLevelChanged           ║
 * ║  【能量】 getPassiveEnergyCost / onEnergyDepleted / onEnergyRestored         ║
 * ║                                                                              ║
 * ║  详见 IModuleEventHandler.java 的完整文档                                      ║
 * ║                                                                              ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
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

    // 效果定义 (自动处理)
    public final List<ModuleEffect> effects;                     // 模块效果列表 (简单效果)
    public final IModuleEventHandler handler;                    // 事件处理器 (完整事件)

    private ModuleDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.color = builder.color;
        this.category = builder.category;
        this.maxLevel = builder.maxLevel;
        this.levelDescriptions = builder.levelDescriptions;
        this.stackSizes = builder.stackSizes;
        this.effects = Collections.unmodifiableList(new ArrayList<>(builder.effects));
        this.handler = builder.handler;
    }

    /**
     * 是否有事件处理器
     */
    public boolean hasHandler() {
        return handler != null;
    }

    /**
     * 是否有简单效果
     */
    public boolean hasEffects() {
        return effects != null && !effects.isEmpty();
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
        private List<ModuleEffect> effects = new ArrayList<>();
        private IModuleEventHandler handler = null;

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

        /**
         * 添加模块效果 (自动处理)
         * 支持的效果类型:
         * - 属性修改器 (攻击力、移动速度等)
         * - 药水效果 (夜视、水下呼吸等)
         * - 周期性恢复 (生命、饥饿)
         * - 伤害加成/减免/反弹
         * - 自定义回调
         */
        public Builder effects(ModuleEffect... effects) {
            this.effects.addAll(Arrays.asList(effects));
            return this;
        }

        /**
         * 添加单个效果
         */
        public Builder addEffect(ModuleEffect effect) {
            this.effects.add(effect);
            return this;
        }

        /**
         * 设置事件处理器 (完整自定义逻辑)
         *
         * 使用方式:
         * 1. 实现 IModuleEventHandler 接口
         * 2. 覆盖你需要的事件方法 (onTick, onPlayerHurt, 等)
         * 3. 调用 .handler(new YourHandler())
         *
         * 详见 IModuleEventHandler.java 的完整文档
         */
        public Builder handler(IModuleEventHandler handler) {
            this.handler = handler;
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
