package com.moremod.compat.crafttweaker;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;

import java.util.*;
import java.util.regex.Pattern;
import java.lang.reflect.Method;

/**
 * 宝石掉落规则管理器 v4.0 - Phase 1 解耦版
 *
 * Phase 1 解耦说明:
 * - 移除了 Champions 直接依赖 (c4.champions)
 * - 移除了 Infernal Mobs 直接依赖 (atomicstryker.infernalmobs)
 * - 移除了 Lycanites Mobs 直接依赖 (com.lycanitesmobs)
 * - 移除了 SRP 直接依赖 (com.dhanantry.scapeandrunparasites)
 *
 * 所有外部MOD检测改用反射方式，确保MOD不存在时不会崩溃
 * 原版本备份在: src_backup_compat/compat/GemLootRuleManager.java.bak
 */
public class GemLootRuleManager {

    private static final List<LootRule> RULES = new ArrayList<>();
    private static boolean debugMode = false;

    // 缓存反射方法（仅用于龙）
    private static final Map<Class<?>, Method> DRAGON_STAGE_METHOD_CACHE = new HashMap<>();

    // 默认规则
    private static LootRule DEFAULT_RULE = new LootRule(
            "default",
            1, 10,
            1, 1,
            0.05f,      // POE系统：默认5%
            0.0f,
            1
    );

    // ==========================================
    // 规则数据结构
    // ==========================================

    public static class LootRule {
        public final String name;
        public final int minLevel;
        public final int maxLevel;
        public final int minGems;
        public final int maxGems;
        public final float dropChance;
        public final float bonusChance;
        public final int priority;
        private Condition condition;

        public LootRule(String name, int minLevel, int maxLevel, int minGems, int maxGems,
                        float dropChance, float bonusChance, int priority) {
            this.name = name;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.minGems = minGems;
            this.maxGems = maxGems;
            this.dropChance = dropChance;
            this.bonusChance = bonusChance;
            this.priority = priority;
        }

        public void setCondition(Condition cond) {
            this.condition = cond;
        }

        public boolean matches(EntityLivingBase entity) {
            return condition == null || condition.test(entity);
        }
    }

    @FunctionalInterface
    public interface Condition {
        boolean test(EntityLivingBase entity);
    }

    // ==========================================
    // 规则管理
    // ==========================================

    public static void clearRules() {
        RULES.clear();
    }

    public static void addRule(LootRule rule) {
        RULES.add(rule);
        RULES.sort((a, b) -> b.priority - a.priority);
    }

    public static void setDefaultRule(LootRule rule) {
        DEFAULT_RULE = rule;
    }

    public static LootRule matchRule(EntityLivingBase entity) {
        for (LootRule rule : RULES) {
            if (rule.matches(entity)) {
                return rule;
            }
        }
        return DEFAULT_RULE;
    }

    // ==========================================
    // Phase 1 解耦: 反射辅助系统
    // ==========================================

    private static Boolean infernalAvailable = null;
    private static Method getIsRareEntity = null;
    private static Method getMobModifiers = null;

    private static Boolean championsAvailable = null;
    private static Boolean lycanitesAvailable = null;
    private static Boolean srpAvailable = null;

    /**
     * 检测是否为Infernal精英怪
     */
    public static boolean isInfernalElite(EntityLivingBase entity) {
        if (infernalAvailable == null) {
            try {
                Class<?> clazz = Class.forName("atomicstryker.infernalmobs.common.InfernalMobsCore");
                getIsRareEntity = clazz.getMethod("getIsRareEntity", EntityLivingBase.class);
                infernalAvailable = true;
            } catch (Exception e) {
                infernalAvailable = false;
            }
        }
        if (!infernalAvailable) return false;
        try {
            return (Boolean) getIsRareEntity.invoke(null, entity);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检测是否为Champions精英怪
     */
    public static boolean isChampionsElite(EntityLiving entity) {
        if (championsAvailable == null) {
            try {
                Class.forName("c4.champions.common.capability.CapabilityChampionship");
                championsAvailable = true;
            } catch (Exception e) {
                championsAvailable = false;
            }
        }
        if (!championsAvailable) return false;
        // 简化实现：通过NBT检测
        NBTTagCompound data = entity.getEntityData();
        return data.hasKey("championsTier") && data.getInteger("championsTier") > 0;
    }

    /**
     * 检测是否为Lycanites Boss
     */
    public static boolean isLycanitesBoss(EntityLivingBase entity) {
        if (lycanitesAvailable == null) {
            try {
                Class.forName("com.lycanitesmobs.api.IGroupBoss");
                lycanitesAvailable = true;
            } catch (Exception e) {
                lycanitesAvailable = false;
            }
        }
        if (!lycanitesAvailable) return false;
        try {
            Class<?> bossInterface = Class.forName("com.lycanitesmobs.api.IGroupBoss");
            return bossInterface.isInstance(entity);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检测是否为SRP寄生虫
     */
    public static boolean isSRPParasite(EntityLivingBase entity) {
        if (srpAvailable == null) {
            try {
                Class.forName("com.dhanantry.scapeandrunparasites.entity.ai.misc.EntityParasiteBase");
                srpAvailable = true;
            } catch (Exception e) {
                srpAvailable = false;
            }
        }
        if (!srpAvailable) return false;
        try {
            Class<?> parasiteClass = Class.forName("com.dhanantry.scapeandrunparasites.entity.ai.misc.EntityParasiteBase");
            return parasiteClass.isInstance(entity);
        } catch (Exception e) {
            return false;
        }
    }

    // ==========================================
    // 内置条件生成器
    // ==========================================

    /**
     * 创建实体类名匹配条件
     */
    public static Condition entityClassContains(String... patterns) {
        return entity -> {
            String className = entity.getClass().getName().toLowerCase();
            for (String pattern : patterns) {
                if (className.contains(pattern.toLowerCase())) return true;
            }
            return false;
        };
    }

    /**
     * 创建Infernal精英条件
     */
    public static Condition isInfernal() {
        return GemLootRuleManager::isInfernalElite;
    }

    /**
     * 创建Champions精英条件
     */
    public static Condition isChampion() {
        return entity -> entity instanceof EntityLiving && isChampionsElite((EntityLiving) entity);
    }

    /**
     * 创建Lycanites Boss条件
     */
    public static Condition isLycaniteBoss() {
        return GemLootRuleManager::isLycanitesBoss;
    }

    /**
     * 创建怪物条件
     */
    public static Condition isMob() {
        return entity -> entity instanceof IMob;
    }

    /**
     * 创建Boss条件（检测生命值）
     */
    public static Condition isBoss(float minHealth) {
        return entity -> entity.getMaxHealth() >= minHealth;
    }

    // ==========================================
    // 调试
    // ==========================================

    public static void setDebugMode(boolean debug) {
        debugMode = debug;
    }

    public static boolean isDebugMode() {
        return debugMode;
    }
}
