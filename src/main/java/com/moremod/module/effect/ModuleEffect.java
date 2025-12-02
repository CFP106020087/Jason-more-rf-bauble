package com.moremod.module.effect;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 模块效果定义类 - 声明式定义模块效果
 *
 * 支持的效果类型：
 * - 属性修改器 (攻击力、移动速度、攻击速度等)
 * - 药水效果 (夜视、水下呼吸、速度等)
 * - 周期性恢复 (生命、饥饿)
 * - 伤害修改 (伤害加成、伤害减免、反伤)
 * - 周期性触发 (自定义周期效果)
 *
 * 使用示例：
 * <pre>
 * ModuleEffect.attribute(SharedMonsterAttributes.ATTACK_DAMAGE)
 *     .baseValue(0.25)
 *     .perLevel(0.25)
 *     .operation(Operation.MULTIPLY)
 *     .build();
 *
 * ModuleEffect.potion(MobEffects.SPEED)
 *     .amplifierPerLevel(1)
 *     .duration(100)
 *     .build();
 *
 * ModuleEffect.healing()
 *     .amount(0.5f)
 *     .perLevel(0.5f)
 *     .interval(60)
 *     .build();
 * </pre>
 */
public class ModuleEffect {

    public enum EffectType {
        ATTRIBUTE_MODIFIER,     // 属性修改器
        POTION_EFFECT,          // 药水效果
        HEALING,                // 生命恢复
        FOOD_RESTORE,           // 饥饿恢复
        DAMAGE_BOOST,           // 伤害加成
        DAMAGE_REDUCTION,       // 伤害减免
        DAMAGE_REFLECTION,      // 伤害反弹
        TICK_CALLBACK,          // 每tick回调
        ON_HIT,                 // 攻击时触发
        ON_HURT,                // 受伤时触发
        CUSTOM                  // 自定义(需手动实现)
    }

    public enum Operation {
        ADD(0),              // 加法
        ADD_PERCENT(1),      // 百分比加法
        MULTIPLY(2);         // 乘法

        public final int mcValue;
        Operation(int mcValue) { this.mcValue = mcValue; }
    }

    // ===== 基本属性 =====
    public final EffectType type;
    public final String effectId;  // 效果唯一标识

    // ===== 属性修改器参数 =====
    public IAttribute attribute;
    public UUID modifierUUID;
    public double baseValue;
    public double perLevelValue;
    public Operation operation = Operation.MULTIPLY;

    // ===== 药水效果参数 =====
    public Potion potion;
    public int baseDuration = 100;
    public int baseAmplifier = 0;
    public int amplifierPerLevel = 0;
    public boolean ambient = true;
    public boolean showParticles = false;

    // ===== 恢复效果参数 =====
    public float healAmount = 0.5f;
    public float healPerLevel = 0.5f;
    public int foodAmount = 1;
    public int foodPerLevel = 0;
    public float saturation = 0.5f;

    // ===== 伤害效果参数 =====
    public float damageMultiplier = 1.0f;
    public float damagePerLevel = 0.25f;
    public float reductionPercent = 0.0f;
    public float reductionPerLevel = 0.0f;
    public float reflectionPercent = 0.0f;
    public float reflectionPerLevel = 0.0f;
    public List<String> damageTypes = new ArrayList<>();  // 空=所有类型

    // ===== 通用参数 =====
    public int tickInterval = 20;       // tick间隔 (20 = 1秒)
    public int energyCost = 0;          // 每次触发能量消耗
    public int energyPerTick = 0;       // 每tick能量消耗
    public boolean requiresEnergy = false;

    // ===== 回调 =====
    public IEffectCallback tickCallback;
    public IEffectCallback hitCallback;
    public IEffectCallback hurtCallback;

    private ModuleEffect(EffectType type, String effectId) {
        this.type = type;
        this.effectId = effectId;
    }

    // ========== 静态工厂方法 ==========

    /**
     * 创建属性修改器效果
     */
    public static AttributeBuilder attribute(IAttribute attribute) {
        return new AttributeBuilder(attribute);
    }

    /**
     * 创建属性修改器效果 (通过字符串)
     */
    public static AttributeBuilder attribute(String attributeName) {
        IAttribute attr = getAttributeByName(attributeName);
        return new AttributeBuilder(attr);
    }

    /**
     * 创建药水效果
     */
    public static PotionBuilder potion(Potion potion) {
        return new PotionBuilder(potion);
    }

    /**
     * 创建生命恢复效果
     */
    public static HealingBuilder healing() {
        return new HealingBuilder();
    }

    /**
     * 创建饥饿恢复效果
     */
    public static FoodBuilder food() {
        return new FoodBuilder();
    }

    /**
     * 创建伤害加成效果
     */
    public static DamageBoostBuilder damageBoost() {
        return new DamageBoostBuilder();
    }

    /**
     * 创建伤害减免效果
     */
    public static DamageReductionBuilder damageReduction() {
        return new DamageReductionBuilder();
    }

    /**
     * 创建伤害反弹效果
     */
    public static DamageReflectionBuilder damageReflection() {
        return new DamageReflectionBuilder();
    }

    /**
     * 创建周期触发效果
     */
    public static TickBuilder tick(IEffectCallback callback) {
        return new TickBuilder(callback);
    }

    /**
     * 创建攻击触发效果
     */
    public static OnHitBuilder onHit(IEffectCallback callback) {
        return new OnHitBuilder(callback);
    }

    /**
     * 创建受伤触发效果
     */
    public static OnHurtBuilder onHurt(IEffectCallback callback) {
        return new OnHurtBuilder(callback);
    }

    // ========== Builder Classes ==========

    public static class AttributeBuilder {
        private final ModuleEffect effect;

        AttributeBuilder(IAttribute attribute) {
            this.effect = new ModuleEffect(EffectType.ATTRIBUTE_MODIFIER, "attr_" + attribute.getName());
            this.effect.attribute = attribute;
            this.effect.modifierUUID = UUID.randomUUID();
        }

        public AttributeBuilder uuid(UUID uuid) {
            effect.modifierUUID = uuid;
            return this;
        }

        public AttributeBuilder baseValue(double value) {
            effect.baseValue = value;
            return this;
        }

        public AttributeBuilder perLevel(double value) {
            effect.perLevelValue = value;
            return this;
        }

        public AttributeBuilder operation(Operation op) {
            effect.operation = op;
            return this;
        }

        public ModuleEffect build() {
            return effect;
        }
    }

    public static class PotionBuilder {
        private final ModuleEffect effect;

        PotionBuilder(Potion potion) {
            this.effect = new ModuleEffect(EffectType.POTION_EFFECT, "potion_" + potion.getRegistryName());
            this.effect.potion = potion;
        }

        public PotionBuilder duration(int ticks) {
            effect.baseDuration = ticks;
            return this;
        }

        public PotionBuilder amplifier(int level) {
            effect.baseAmplifier = level;
            return this;
        }

        public PotionBuilder amplifierPerLevel(int perLevel) {
            effect.amplifierPerLevel = perLevel;
            return this;
        }

        public PotionBuilder showParticles(boolean show) {
            effect.showParticles = show;
            return this;
        }

        public ModuleEffect build() {
            return effect;
        }
    }

    public static class HealingBuilder {
        private final ModuleEffect effect;

        HealingBuilder() {
            this.effect = new ModuleEffect(EffectType.HEALING, "healing");
        }

        public HealingBuilder amount(float amount) {
            effect.healAmount = amount;
            return this;
        }

        public HealingBuilder perLevel(float perLevel) {
            effect.healPerLevel = perLevel;
            return this;
        }

        public HealingBuilder interval(int ticks) {
            effect.tickInterval = ticks;
            return this;
        }

        public HealingBuilder energyCost(int cost) {
            effect.energyCost = cost;
            effect.requiresEnergy = cost > 0;
            return this;
        }

        public ModuleEffect build() {
            return effect;
        }
    }

    public static class FoodBuilder {
        private final ModuleEffect effect;

        FoodBuilder() {
            this.effect = new ModuleEffect(EffectType.FOOD_RESTORE, "food");
        }

        public FoodBuilder amount(int amount) {
            effect.foodAmount = amount;
            return this;
        }

        public FoodBuilder perLevel(int perLevel) {
            effect.foodPerLevel = perLevel;
            return this;
        }

        public FoodBuilder saturation(float sat) {
            effect.saturation = sat;
            return this;
        }

        public FoodBuilder interval(int ticks) {
            effect.tickInterval = ticks;
            return this;
        }

        public ModuleEffect build() {
            return effect;
        }
    }

    public static class DamageBoostBuilder {
        private final ModuleEffect effect;

        DamageBoostBuilder() {
            this.effect = new ModuleEffect(EffectType.DAMAGE_BOOST, "damage_boost");
        }

        public DamageBoostBuilder multiplier(float mult) {
            effect.damageMultiplier = mult;
            return this;
        }

        public DamageBoostBuilder perLevel(float perLevel) {
            effect.damagePerLevel = perLevel;
            return this;
        }

        public ModuleEffect build() {
            return effect;
        }
    }

    public static class DamageReductionBuilder {
        private final ModuleEffect effect;

        DamageReductionBuilder() {
            this.effect = new ModuleEffect(EffectType.DAMAGE_REDUCTION, "damage_reduction");
        }

        public DamageReductionBuilder percent(float percent) {
            effect.reductionPercent = percent;
            return this;
        }

        public DamageReductionBuilder perLevel(float perLevel) {
            effect.reductionPerLevel = perLevel;
            return this;
        }

        public DamageReductionBuilder forDamageTypes(String... types) {
            for (String type : types) {
                effect.damageTypes.add(type);
            }
            return this;
        }

        public ModuleEffect build() {
            return effect;
        }
    }

    public static class DamageReflectionBuilder {
        private final ModuleEffect effect;

        DamageReflectionBuilder() {
            this.effect = new ModuleEffect(EffectType.DAMAGE_REFLECTION, "damage_reflection");
        }

        public DamageReflectionBuilder percent(float percent) {
            effect.reflectionPercent = percent;
            return this;
        }

        public DamageReflectionBuilder perLevel(float perLevel) {
            effect.reflectionPerLevel = perLevel;
            return this;
        }

        public ModuleEffect build() {
            return effect;
        }
    }

    public static class TickBuilder {
        private final ModuleEffect effect;

        TickBuilder(IEffectCallback callback) {
            this.effect = new ModuleEffect(EffectType.TICK_CALLBACK, "tick_custom");
            this.effect.tickCallback = callback;
        }

        public TickBuilder interval(int ticks) {
            effect.tickInterval = ticks;
            return this;
        }

        public TickBuilder energyPerTick(int cost) {
            effect.energyPerTick = cost;
            effect.requiresEnergy = cost > 0;
            return this;
        }

        public ModuleEffect build() {
            return effect;
        }
    }

    public static class OnHitBuilder {
        private final ModuleEffect effect;

        OnHitBuilder(IEffectCallback callback) {
            this.effect = new ModuleEffect(EffectType.ON_HIT, "on_hit_custom");
            this.effect.hitCallback = callback;
        }

        public OnHitBuilder energyCost(int cost) {
            effect.energyCost = cost;
            effect.requiresEnergy = cost > 0;
            return this;
        }

        public ModuleEffect build() {
            return effect;
        }
    }

    public static class OnHurtBuilder {
        private final ModuleEffect effect;

        OnHurtBuilder(IEffectCallback callback) {
            this.effect = new ModuleEffect(EffectType.ON_HURT, "on_hurt_custom");
            this.effect.hurtCallback = callback;
        }

        public OnHurtBuilder energyCost(int cost) {
            effect.energyCost = cost;
            effect.requiresEnergy = cost > 0;
            return this;
        }

        public ModuleEffect build() {
            return effect;
        }
    }

    // ========== 工具方法 ==========

    private static IAttribute getAttributeByName(String name) {
        switch (name.toUpperCase()) {
            case "ATTACK_DAMAGE":
                return SharedMonsterAttributes.ATTACK_DAMAGE;
            case "ATTACK_SPEED":
                return SharedMonsterAttributes.ATTACK_SPEED;
            case "MOVEMENT_SPEED":
                return SharedMonsterAttributes.MOVEMENT_SPEED;
            case "MAX_HEALTH":
                return SharedMonsterAttributes.MAX_HEALTH;
            case "ARMOR":
                return SharedMonsterAttributes.ARMOR;
            case "ARMOR_TOUGHNESS":
                return SharedMonsterAttributes.ARMOR_TOUGHNESS;
            case "KNOCKBACK_RESISTANCE":
                return SharedMonsterAttributes.KNOCKBACK_RESISTANCE;
            default:
                throw new IllegalArgumentException("Unknown attribute: " + name);
        }
    }

    /**
     * 计算指定等级的效果值
     */
    public double getValueForLevel(int level) {
        return baseValue + (perLevelValue * (level - 1));
    }

    /**
     * 计算指定等级的恢复量
     */
    public float getHealAmountForLevel(int level) {
        return healAmount + (healPerLevel * (level - 1));
    }

    /**
     * 计算指定等级的伤害倍率
     */
    public float getDamageMultiplierForLevel(int level) {
        return damageMultiplier + (damagePerLevel * (level - 1));
    }

    /**
     * 计算指定等级的减伤百分比
     */
    public float getReductionForLevel(int level) {
        return reductionPercent + (reductionPerLevel * (level - 1));
    }

    /**
     * 计算指定等级的反伤百分比
     */
    public float getReflectionForLevel(int level) {
        return reflectionPercent + (reflectionPerLevel * (level - 1));
    }

    /**
     * 计算指定等级的药水等级
     */
    public int getAmplifierForLevel(int level) {
        return baseAmplifier + (amplifierPerLevel * (level - 1));
    }
}
