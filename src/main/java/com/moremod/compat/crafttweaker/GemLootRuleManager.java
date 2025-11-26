package com.moremod.compat.crafttweaker;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;

// Champions（已优化，无反射）
import c4.champions.common.capability.CapabilityChampionship;
import c4.champions.common.capability.IChampionship;

// Infernal（已优化，使用NBT）
import atomicstryker.infernalmobs.common.InfernalMobsCore;
import atomicstryker.infernalmobs.common.MobModifier;

// ⭐ Lycanites - 直接导入（有软依赖）
import com.lycanitesmobs.core.entity.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.TameableCreatureEntity;
import com.lycanitesmobs.api.IGroupBoss;
import com.lycanitesmobs.api.IGroupHeavy;
import com.lycanitesmobs.api.IGroupDemon;
import com.lycanitesmobs.api.IGroupShadow;

// ⭐ SRP - 直接导入（有软依赖）
import com.dhanantry.scapeandrunparasites.entity.ai.misc.EntityParasiteBase;

import java.util.*;
import java.util.regex.Pattern;
import java.lang.reflect.Method;

/**
 * 宝石掉落规则管理器 v3.0 - 性能优化版
 *
 * 优化策略：
 * 1. ✅ Ice and Fire龙：保留反射（private access）
 * 2. ✅ Champions：已优化（CapabilityChampionship）
 * 3. ✅ Infernal：已优化（NBT检测）
 * 4. ✅ Lycanites：直接导入类和接口
 * 5. ✅ SRP：直接导入EntityParasiteBase
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

    static {
        System.out.println("[GemLootRuleManager] ========================================");
        System.out.println("[GemLootRuleManager] v3.0 性能优化版");
        System.out.println("[GemLootRuleManager] 仅龙使用反射，其他均已优化");
        System.out.println("[GemLootRuleManager] ========================================");
    }

    // ==========================================
    // 规则注册API
    // ==========================================

    public static void addRule(LootRule rule) {
        RULES.add(rule);
        RULES.sort((a, b) -> Integer.compare(b.priority, a.priority));
    }

    public static boolean removeRule(String id) {
        return RULES.removeIf(rule -> rule.id.equals(id));
    }

    public static void clearRules() {
        RULES.clear();
    }

    public static void setDefaultRule(LootRule rule) {
        DEFAULT_RULE = rule;
    }

    public static Map<String, LootRule> getAllRules() {
        Map<String, LootRule> map = new LinkedHashMap<>();
        for (LootRule rule : RULES) {
            map.put(rule.id, rule);
        }
        return map;
    }

    public static void setDebugMode(boolean debug) {
        debugMode = debug;
    }

    // ==========================================
    // 规则匹配（优化版）
    // ==========================================

    public static LootRule findRule(EntityLivingBase entity) {
        String entityName = entity.getName().toLowerCase();
        String entityClass = entity.getClass().getName();
        String entityClassSimple = entity.getClass().getSimpleName();
        String modId = getModIdFast(entity);
        float health = entity.getMaxHealth();

        for (LootRule rule : RULES) {
            if (rule.matches(entity, entityName, entityClass, entityClassSimple, modId, health)) {
                LootRule adjusted = rule.applyDynamicAdjustments(entity);
                return adjusted;
            }
        }

        return DEFAULT_RULE;
    }

    // 优化的模组ID获取
    private static String getModIdFast(EntityLivingBase entity) {
        // 使用instanceof判断（性能最优）
        if (entity instanceof BaseCreatureEntity) return "lycanitesmobs";
        if (entity instanceof EntityParasiteBase) return "srparasites";
        
        // 使用类名前缀判断（次优）
        String className = entity.getClass().getName();
        if (className.startsWith("com.github.alexthe666.iceandfire")) return "iceandfire";
        if (className.startsWith("c4.champions")) return "champions";
        if (className.startsWith("atomicstryker.infernalmobs")) return "infernalmobs";
        
        // 兜底：从包名提取
        String[] parts = className.split("\\.");
        if (parts.length >= 3) {
            return parts[2].toLowerCase();  // 通常第三段是模组名
        }
        return "minecraft";
    }

    // ==========================================
    // 掉落规则类（优化版）
    // ==========================================

    public static class LootRule {
        public final String id;

        // 基础匹配条件
        private Set<String> entityNames = new HashSet<>();
        private Set<String> classNames = new HashSet<>();
        private Set<String> modIds = new HashSet<>();
        private Pattern namePattern = null;
        private Pattern classPattern = null;

        // 高级条件
        private float minHealth = -1;
        private float maxHealth = -1;
        private int championTier = -1;
        private int minChampionTier = -1;
        private int maxChampionTier = -1;
        private int minAffixCount = -1;
        private int maxAffixCount = -1;
        private boolean growthFactorBonus = false;
        private int minModCount = -1;
        private int maxModCount = -1;
        
        // Lycanites接口要求
        private Set<String> requiredInterfaces = new HashSet<>();
        private Set<String> excludedInterfaces = new HashSet<>();
        private boolean excludeBoss = false;
        private boolean requireHostile = false;
        
        // SRP进化等级
        private String requiredSRPEvolution = null;
        
        // Ice and Fire龙阶段（仍需反射）
        private int minDragonStage = -1;
        private int maxDragonStage = -1;

        // 掉落参数
        public int minLevel;
        public int maxLevel;
        public int minAffixes;
        public int maxAffixes;
        public float dropChance;
        public float minQuality;
        public int rerollCount;
        private int minDropCount = 1;
        private int maxDropCount = 1;
        private boolean dynamicDropRate = false;
        private boolean dynamicLevel = false;
        public int priority = 0;

        // ❌ 移除血量动态加成（破坏平衡）
        // private boolean healthScaling = false;
        // private float healthScalingThreshold = 200.0f;
        // private float healthScalingFactor = 0.01f;

        public LootRule(String id, int minLevel, int maxLevel, int minAffixes, int maxAffixes,
                        float dropChance, float minQuality, int rerollCount) {
            this.id = id;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.minAffixes = minAffixes;
            this.maxAffixes = maxAffixes;
            this.dropChance = dropChance;
            this.minQuality = minQuality;
            this.rerollCount = rerollCount;
        }

        // ==========================================
        // Builder方法
        // ==========================================

        public LootRule matchEntityName(String name) {
            this.entityNames.add(name.toLowerCase());
            this.priority += 100;
            return this;
        }

        public LootRule matchClassName(String className) {
            this.classNames.add(className);
            this.priority += 50;
            return this;
        }

        public LootRule matchModId(String modId) {
            this.modIds.add(modId.toLowerCase());
            this.priority += 10;
            return this;
        }

        public LootRule matchNamePattern(String regex) {
            this.namePattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            this.priority += 80;
            return this;
        }

        public LootRule matchClassPattern(String regex) {
            this.classPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            this.priority += 40;
            return this;
        }

        public LootRule setMinHealth(float health) {
            this.minHealth = health;
            return this;
        }

        public LootRule setMaxHealth(float health) {
            this.maxHealth = health;
            return this;
        }

        public LootRule setChampionTier(int tier) {
            this.championTier = tier;
            this.priority += 200;
            return this;
        }

        public LootRule setMinChampionTier(int tier) {
            this.minChampionTier = tier;
            this.priority += 150;
            return this;
        }

        public LootRule setMaxChampionTier(int tier) {
            this.maxChampionTier = tier;
            return this;
        }

        public LootRule setMinAffixCount(int count) {
            this.minAffixCount = count;
            return this;
        }

        public LootRule setMaxAffixCount(int count) {
            this.maxAffixCount = count;
            return this;
        }

        public LootRule setGrowthFactorBonus(boolean enable) {
            this.growthFactorBonus = enable;
            return this;
        }

        public LootRule setMinModCount(int count) {
            this.minModCount = count;
            this.priority += 150;
            return this;
        }

        public LootRule setMaxModCount(int count) {
            this.maxModCount = count;
            return this;
        }

        public LootRule matchInterface(String interfaceName) {
            this.requiredInterfaces.add(interfaceName);
            this.priority += 120;
            return this;
        }

        public LootRule excludeInterface(String interfaceName) {
            this.excludedInterfaces.add(interfaceName);
            return this;
        }

        public LootRule excludeBoss(boolean exclude) {
            this.excludeBoss = exclude;
            return this;
        }

        public LootRule requireHostile(boolean require) {
            this.requireHostile = require;
            return this;
        }

        public LootRule setRandomDropCount(int min, int max) {
            this.minDropCount = min;
            this.maxDropCount = max;
            return this;
        }

        public LootRule setDynamicDropRate(boolean enable) {
            this.dynamicDropRate = enable;
            return this;
        }

        public LootRule setDynamicLevel(boolean enable) {
            this.dynamicLevel = enable;
            return this;
        }

        public LootRule setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        // ❌ 血量加成方法已移除（破坏游戏平衡）
        // public LootRule setHealthScaling(boolean enable) { ... }
        // public LootRule setHealthScalingThreshold(float threshold) { ... }
        // public LootRule setHealthScalingFactor(float factor) { ... }

        // Ice and Fire龙阶段
        public LootRule setDragonStage(int stage) {
            this.minDragonStage = stage;
            this.maxDragonStage = stage;
            this.priority += 200;
            return this;
        }

        public LootRule setMinDragonStage(int stage) {
            this.minDragonStage = stage;
            return this;
        }

        public LootRule setMaxDragonStage(int stage) {
            this.maxDragonStage = stage;
            return this;
        }

        // ==========================================
        // 匹配逻辑（优化版）
        // ==========================================

        public boolean matches(EntityLivingBase entity, String entityName, String className,
                               String simpleClassName, String modId, float health) {

            // 基础匹配
            boolean hasBasicCondition = !entityNames.isEmpty() || !classNames.isEmpty() ||
                    !modIds.isEmpty() || namePattern != null || classPattern != null;
            boolean basicMatched = false;

            if (hasBasicCondition) {
                // 名称匹配
                for (String name : entityNames) {
                    if (entityName.contains(name)) {
                        basicMatched = true;
                        break;
                    }
                }

                // 类名匹配（精确匹配）
                if (!basicMatched) {
                    for (String cls : classNames) {
                        // 使用精确匹配或者以.ClassName结尾（避免EntityWither匹配EntityWitherSkeleton）
                        if (simpleClassName.equals(cls) || 
                            className.endsWith("." + cls)) {  // 完整类名匹配
                            basicMatched = true;
                            break;
                        }
                    }
                }

                // 模组匹配
                if (!basicMatched) {
                    for (String mod : modIds) {
                        if (modId.contains(mod)) {
                            basicMatched = true;
                            break;
                        }
                    }
                }

                // 正则匹配
                if (!basicMatched && namePattern != null && namePattern.matcher(entityName).find()) {
                    basicMatched = true;
                }
                if (!basicMatched && classPattern != null && classPattern.matcher(className).find()) {
                    basicMatched = true;
                }

                if (!basicMatched) {
                    return false;
                }
            }

            // 血量检查
            if (minHealth > 0 && health < minHealth) return false;
            if (maxHealth > 0 && health > maxHealth) return false;

            // Champions检查（已优化，无反射）
            if (championTier > 0 || minChampionTier > 0 || maxChampionTier > 0 || 
                minAffixCount > 0 || maxAffixCount > 0) {
                if (!checkChampions(entity)) return false;
            }

            // Infernal检查（已优化，NBT）
            if (minModCount > 0 || maxModCount > 0) {
                if (!checkInfernalMobs(entity)) return false;
            }

            // Lycanites检查（优化版，直接类判断）
            if (!requiredInterfaces.isEmpty() || !excludedInterfaces.isEmpty() || excludeBoss) {
                if (!checkLycanitesOptimized(entity)) return false;
            }

            // SRP检查（优化版，直接类判断）
            if (requiredSRPEvolution != null) {
                if (!checkSRPOptimized(entity)) return false;
            }

            // Ice and Fire龙阶段检查（仍需反射）
            if (minDragonStage > 0 || maxDragonStage > 0) {
                if (!checkDragonStage(entity)) return false;
            }

            // 敌对性检查（优化版）
            if (requireHostile) {
                if (!checkHostileOptimized(entity)) return false;
            }

            return true;
        }

        // ==========================================
        // Champions检查（已优化）
        // ==========================================

        private boolean checkChampions(EntityLivingBase entity) {
            if (!(entity instanceof EntityLiving)) {
                return false;
            }

            try {
                IChampionship chp = CapabilityChampionship.getChampionship((EntityLiving) entity);
                if (chp == null || chp.getRank() == null) {
                    return false;
                }

                int tier = chp.getRank().getTier();
                if (tier <= 0) {
                    return false;
                }

                if (championTier > 0 && tier != championTier) {
                    return false;
                }
                if (minChampionTier > 0 && tier < minChampionTier) {
                    return false;
                }
                if (maxChampionTier > 0 && tier > maxChampionTier) {
                    return false;
                }

                if (minAffixCount > 0 || maxAffixCount > 0) {
                    int affixCount = chp.getAffixes().size();
                    if (minAffixCount > 0 && affixCount < minAffixCount) {
                        return false;
                    }
                    if (maxAffixCount > 0 && affixCount > maxAffixCount) {
                        return false;
                    }
                }

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        // ==========================================
        // Infernal检查（已优化，NBT）
        // ==========================================

        private boolean checkInfernalMobs(EntityLivingBase entity) {
            try {
                NBTTagCompound nbt = entity.getEntityData();
                String infernalTag = nbt.getString("InfernalMobsMod");

                if (infernalTag != null && !infernalTag.isEmpty()) {
                    int modSize = countWords(infernalTag);
                    if (minModCount > 0 && modSize < minModCount) {
                        return false;
                    }
                    if (maxModCount > 0 && modSize > maxModCount) {
                        return false;
                    }
                    return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }

        // 快速计算词条数（避免正则）
        private int countWords(String str) {
            if (str == null || str.isEmpty()) return 0;
            int count = 1;
            for (int i = 0; i < str.length(); i++) {
                if (str.charAt(i) == ' ') count++;
            }
            return count;
        }

        // ==========================================
        // Lycanites检查（优化版）
        // ==========================================

        private boolean checkLycanitesOptimized(EntityLivingBase entity) {
            if (!(entity instanceof BaseCreatureEntity)) {
                return false;
            }

            // 使用直接类判断替代反射
            for (String interfaceName : requiredInterfaces) {
                boolean matched = false;
                switch (interfaceName) {
                    case "IGroupBoss":
                        matched = entity instanceof IGroupBoss;
                        break;
                    case "IGroupHeavy":
                        matched = entity instanceof IGroupHeavy;
                        break;
                    case "IGroupDemon":
                        matched = entity instanceof IGroupDemon;
                        break;
                    case "IGroupShadow":
                        matched = entity instanceof IGroupShadow;
                        break;
                }
                if (!matched) return false;
            }

            for (String interfaceName : excludedInterfaces) {
                boolean matched = false;
                switch (interfaceName) {
                    case "IGroupBoss":
                        matched = entity instanceof IGroupBoss;
                        break;
                    case "IGroupHeavy":
                        matched = entity instanceof IGroupHeavy;
                        break;
                    case "IGroupDemon":
                        matched = entity instanceof IGroupDemon;
                        break;
                    case "IGroupShadow":
                        matched = entity instanceof IGroupShadow;
                        break;
                }
                if (matched) return false;
            }

            if (excludeBoss && entity instanceof IGroupBoss) {
                return false;
            }

            return true;
        }

        // ==========================================
        // SRP检查（优化版）
        // ==========================================

        private boolean checkSRPOptimized(EntityLivingBase entity) {
            if (!(entity instanceof EntityParasiteBase)) {
                return false;
            }

            EntityParasiteBase parasite = (EntityParasiteBase) entity;
            
            // 直接获取包名判断进化等级
            String packageName = entity.getClass().getPackage().getName();
            String evolution = packageName.substring(packageName.lastIndexOf('.') + 1);
            
            if (requiredSRPEvolution != null && !requiredSRPEvolution.equals(evolution)) {
                return false;
            }

            return true;
        }

        // ==========================================
        // 敌对性检查（优化版）
        // ==========================================

        private boolean checkHostileOptimized(EntityLivingBase entity) {
            // Lycanites生物
            if (entity instanceof BaseCreatureEntity) {
                BaseCreatureEntity creature = (BaseCreatureEntity) entity;
                return creature.isAggressive();
            }
            
            // 原版敌对接口
            if (entity instanceof IMob) {
                return true;
            }
            
            // 原版动物（非敌对）
            if (entity instanceof EntityAnimal) {
                return false;
            }
            
            // 默认视为敌对
            return true;
        }

        // ==========================================
        // Ice and Fire龙阶段检查（保留反射，因为private）
        // ==========================================

        private boolean checkDragonStage(EntityLivingBase entity) {
            String className = entity.getClass().getName();

            if (!className.contains("iceandfire")) {
                return false;
            }

            try {
                // 使用缓存的方法
                Method method = DRAGON_STAGE_METHOD_CACHE.computeIfAbsent(
                    entity.getClass(), 
                    clazz -> {
                        // 尝试不同的方法名
                        for (String methodName : Arrays.asList("getDragonStage", "getLifeStage", "getAgeInDays")) {
                            try {
                                return clazz.getMethod(methodName);
                            } catch (NoSuchMethodException e) {
                                // 继续尝试下一个
                            }
                        }
                        return null;
                    }
                );

                if (method == null) {
                    return false;
                }

                int stage;
                String methodName = method.getName();
                Object result = method.invoke(entity);
                
                if (methodName.equals("getAgeInDays")) {
                    // 年龄转阶段
                    int ageInDays = ((Number) result).intValue();
                    stage = Math.min(5, (ageInDays / 25) + 1);
                } else {
                    stage = ((Number) result).intValue();
                }

                if (debugMode) {
                    System.out.println("[GemLoot] 龙阶段: " + stage);
                }

                if (minDragonStage > 0 && stage < minDragonStage) return false;
                if (maxDragonStage > 0 && stage > maxDragonStage) return false;

                return true;

            } catch (Exception e) {
                if (debugMode) {
                    System.err.println("[GemLoot] 龙阶段检查失败: " + e.getMessage());
                }
                return false;
            }
        }

        // ==========================================
        // 动态调整（优化版，无血量加成）
        // ==========================================

        public LootRule applyDynamicAdjustments(EntityLivingBase entity) {
            if (!dynamicDropRate && !dynamicLevel && !growthFactorBonus) {
                return this;
            }

            LootRule adjusted = new LootRule(
                    id + "_adjusted",
                    minLevel, maxLevel,
                    minAffixes, maxAffixes,
                    dropChance, minQuality,
                    rerollCount
            );
            adjusted.minDropCount = this.minDropCount;
            adjusted.maxDropCount = this.maxDropCount;

            // ❌ 血量加成已移除（避免破坏平衡）
            // 仅保留Champions和Infernal动态调整

            // Champions和Infernal加成（取最大值）
            int championsLevelBonus = 0;
            float championsDropBonus = 0.0f;
            int infernalLevelBonus = 0;
            float infernalDropBonus = 0.0f;

            // Champions动态调整（已优化）
            if ((dynamicDropRate || dynamicLevel || growthFactorBonus) && entity instanceof EntityLiving) {
                try {
                    IChampionship chp = CapabilityChampionship.getChampionship((EntityLiving) entity);
                    if (chp != null && chp.getRank() != null) {
                        if (growthFactorBonus) {
                            int growth = chp.getRank().getGrowthFactor();
                            championsDropBonus += growth * 0.02f;
                            championsLevelBonus += growth * 2;
                        }
                        if (dynamicDropRate || dynamicLevel) {
                            int affixCount = chp.getAffixes().size();
                            if (dynamicDropRate) {
                                championsDropBonus += affixCount * 0.05f;
                            }
                            if (dynamicLevel) {
                                championsLevelBonus += affixCount * 5;
                            }
                        }
                    }
                } catch (Exception e) {
                    // 忽略
                }
            }

            // Infernal动态调整（已优化，NBT）
            if (dynamicDropRate || dynamicLevel) {
                try {
                    NBTTagCompound nbt = entity.getEntityData();
                    String infernalTag = nbt.getString("InfernalMobsMod");
                    if (infernalTag != null && !infernalTag.isEmpty()) {
                        int modSize = countWords(infernalTag);
                        if (dynamicDropRate) {
                            infernalDropBonus += modSize * 0.03f;
                        }
                        if (dynamicLevel) {
                            infernalLevelBonus += modSize * 3;
                        }
                    }
                } catch (Exception e) {
                    // 忽略
                }
            }

            // 取最大值
            int finalLevelBonus = Math.max(championsLevelBonus, infernalLevelBonus);
            float finalDropBonus = Math.max(championsDropBonus, infernalDropBonus);

            adjusted.dropChance += finalDropBonus;
            adjusted.minLevel += finalLevelBonus;
            adjusted.maxLevel += finalLevelBonus;

            // 限制最大值
            adjusted.dropChance = Math.min(adjusted.dropChance, 1.0f);
            adjusted.minLevel = Math.max(1, Math.min(adjusted.minLevel, 100));
            adjusted.maxLevel = Math.max(adjusted.minLevel, Math.min(adjusted.maxLevel, 100));
            adjusted.minAffixes = Math.max(1, Math.min(adjusted.minAffixes, 10));
            adjusted.maxAffixes = Math.max(adjusted.minAffixes, Math.min(adjusted.maxAffixes, 10));
            adjusted.minQuality = Math.max(0.0f, Math.min(adjusted.minQuality, 1.0f));

            return adjusted;
        }

        // 工具方法
        public int getDropCount(Random random) {
            if (minDropCount == maxDropCount) {
                return minDropCount;
            }
            return minDropCount + random.nextInt(maxDropCount - minDropCount + 1);
        }

        @Override
        public String toString() {
            return String.format("LootRule[%s: Lv%d-%d, %d-%d词条, %.1f%%掉落, 品质≥%.0f%%, roll×%d, 优先级%d]",
                    id, minLevel, maxLevel, minAffixes, maxAffixes,
                    dropChance * 100, minQuality * 100, rerollCount, priority);
        }
    }
}