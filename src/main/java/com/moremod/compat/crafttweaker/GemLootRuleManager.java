package com.moremod.compat.crafttweaker;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import atomicstryker.infernalmobs.common.InfernalMobsCore;
import atomicstryker.infernalmobs.common.MobModifier;
import c4.champions.common.capability.CapabilityChampionship;
import c4.champions.common.capability.IChampionship;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 宝石掉落规则管理器 v2.4 - Ice and Fire修复版
 *
 * 核心修复：
 * 1. ✅ 在static块中添加Ice and Fire龙的内建规则
 * 2. ✅ 提高DEFAULT_RULE掉落率（1% → 10%）
 * 3. ✅ Infernal使用NBT检测（主要）+ API检测（备用）
 * 4. ✅ 保留所有原有方法和逻辑
 */
public class GemLootRuleManager {

    private static final List<LootRule> RULES = new ArrayList<>();
    
    // ⭐ v2.4：提高默认掉落率 1% → 10%
    private static LootRule DEFAULT_RULE = new LootRule(
            "default",
            1, 10,           // Lv1-10
            1, 1,            // 1词条
            0.10f,           // 10%掉落率（原1%）
            0.0f,            // 最低品质0%
            1                // 重roll 1次
    );

    static {
        // ==========================================
        // ⭐⭐⭐ 内建规则已禁用 ⭐⭐⭐
        // 所有规则改由 CTGemLootRules.setupAllRules() 配置
        // ==========================================

        System.out.println("[GemLootRuleManager] ========================================");
        System.out.println("[GemLootRuleManager] 内建规则已禁用");
        System.out.println("[GemLootRuleManager] 所有规则由 CTGemLootRules.setupAllRules() 配置");
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

    // ==========================================
    // 规则匹配
    // ==========================================

    public static LootRule findRule(EntityLivingBase entity) {
        String entityName = entity.getName().toLowerCase();
        String entityClass = entity.getClass().getName();
        String entityClassSimple = entity.getClass().getSimpleName();
        String modId = getModId(entityClass);
        float health = entity.getMaxHealth();

        for (LootRule rule : RULES) {
            if (rule.matches(entity, entityName, entityClass, entityClassSimple, modId, health)) {
                LootRule adjusted = rule.applyDynamicAdjustments(entity);
                return adjusted;
            }
        }

        return DEFAULT_RULE;
    }

    private static String getModId(String className) {
        if (className.contains("iceandfire")) return "iceandfire";
        if (className.contains("champions")) return "champions";
        if (className.contains("infernalmobs")) return "infernalmobs";
        if (className.contains("lycanitesmobs")) return "lycanitesmobs";
        if (className.contains("srparasites")) return "srparasites";

        String[] parts = className.split("\\.");
        if (parts.length >= 3) {
            return parts[parts.length - 3].toLowerCase();
        }
        return "";
    }

    // ==========================================
    // 掉落规则类
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
        private int minAffixCount = -1;
        private int maxAffixCount = -1;
        private boolean growthFactorBonus = false;
        private int minModCount = -1;
        private int maxModCount = -1;
        private int minType = -1;
        private int maxType = -1;
        private Set<String> requiredInterfaces = new HashSet<>();
        private Set<String> excludedInterfaces = new HashSet<>();
        private boolean excludeBoss = false;
        private boolean requireHostile = false;

        // ⭐ Ice and Fire 龙阶段检查
        private int minDragonStage = -1;   // 最小龙阶段 (1-5)
        private int maxDragonStage = -1;   // 最大龙阶段 (1-5)

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
        
        // ⭐ 新增：血量动态加成
        private boolean healthScaling = false;           // 是否启用血量加成
        private float healthScalingThreshold = 200.0f;   // 血量门槛（低于此值不掉落）
        private float healthScalingFactor = 0.01f;       // 血量加成系数

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

        public LootRule setMinType(int type) {
            this.minType = type;
            return this;
        }

        public LootRule setMaxType(int type) {
            this.maxType = type;
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
        
        // ⭐ 新增：血量动态加成配置
        public LootRule setHealthScaling(boolean enable) {
            this.healthScaling = enable;
            return this;
        }
        
        public LootRule setHealthScalingThreshold(float threshold) {
            this.healthScalingThreshold = threshold;
            return this;
        }
        
        public LootRule setHealthScalingFactor(float factor) {
            this.healthScalingFactor = factor;
            return this;
        }

        // ⭐ 新增：Ice and Fire 龙阶段匹配
        public LootRule setDragonStage(int stage) {
            this.minDragonStage = stage;
            this.maxDragonStage = stage;
            this.priority += 200;  // 高优先级
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
        // 匹配逻辑（完整保留）
        // ==========================================

        public boolean matches(EntityLivingBase entity, String entityName, String className,
                               String simpleClassName, String modId, float health) {

            // === 第一步：基础匹配 ===
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

                // 类名匹配
                if (!basicMatched) {
                    for (String cls : classNames) {
                        if (simpleClassName.equals(cls) || className.contains(cls)) {
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

                // 如果有基础条件但没匹配上，直接返回false
                if (!basicMatched) {
                    return false;
                }
            }

            // === 第二步：血量检查 ===
            if (minHealth > 0 && health < minHealth) return false;
            if (maxHealth > 0 && health > maxHealth) return false;
            
            // ⭐ 新增：血量动态加成门槛检查
            if (healthScaling && health < healthScalingThreshold) {
                return false;  // 低于门槛不匹配
            }

            // === 第三步：高级条件检查 ===

            // Champions检查
            if (championTier > 0 || minChampionTier > 0 || minAffixCount > 0 || maxAffixCount > 0) {
                if (!checkChampions(entity)) return false;
            }

            // Infernal Mobs检查
            if (minModCount > 0 || maxModCount > 0) {
                if (!checkInfernalMobs(entity)) return false;
            }

            // SRP检查
            if (minType > 0 || maxType > 0) {
                if (!checkSRP(entity)) return false;
            }

            // Lycanites检查
            if (!requiredInterfaces.isEmpty() || !excludedInterfaces.isEmpty() || excludeBoss) {
                if (!checkLycanites(entity)) return false;
            }

            // ⭐ Ice and Fire 龙阶段检查
            if (minDragonStage > 0 || maxDragonStage > 0) {
                if (!checkDragonStage(entity)) return false;
            }

            // 敌对性检查
            if (requireHostile) {
                if (entity instanceof com.lycanitesmobs.core.entity.BaseCreatureEntity) {
                    com.lycanitesmobs.core.entity.BaseCreatureEntity lycanite =
                            (com.lycanitesmobs.core.entity.BaseCreatureEntity) entity;
                    if (!lycanite.isAggressive()) {
                        return false;
                    }
                } else if (entity instanceof net.minecraft.entity.monster.IMob) {
                    // 敌对怪物，通过
                } else if (entity instanceof net.minecraft.entity.passive.EntityAnimal) {
                    // 友善动物，拒绝
                    return false;
                }
            }

            return true;
        }

        // ==========================================
        // Champions检查（完整保留）
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

                // Tier检查
                if (championTier > 0 && tier != championTier) {
                    return false;
                }
                if (minChampionTier > 0 && tier < minChampionTier) {
                    return false;
                }

                // 词条数检查
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
        // Infernal检查（三重检测策略）
        // ==========================================

        private boolean checkInfernalMobs(EntityLivingBase entity) {
            try {
                // 策略1: NBT标签检测
                NBTTagCompound nbt = entity.getEntityData();
                String infernalTag = nbt.getString("InfernalMobsMod");

                if (infernalTag != null && !infernalTag.isEmpty()) {
                    int modSize = infernalTag.trim().split("\\s+").length;

                    if (minModCount > 0 && modSize < minModCount) {
                        return false;
                    }
                    if (maxModCount > 0 && modSize > maxModCount) {
                        return false;
                    }

                    return true;
                }

                // 策略2: API检测
                try {
                    if (InfernalMobsCore.getIsRareEntity(entity)) {
                        Map<EntityLivingBase, MobModifier> raresMap = InfernalMobsCore.proxy.getRareMobs();
                        MobModifier chain = raresMap.get(entity);

                        if (chain != null) {
                            int modSize = chain.getModSize();

                            if (minModCount > 0 && modSize < minModCount) {
                                return false;
                            }
                            if (maxModCount > 0 && modSize > maxModCount) {
                                return false;
                            }

                            return true;
                        }
                    }
                } catch (Exception e) {
                    // API检测失败，继续
                }

                // 策略3: 名称模式检测
                if (entity.hasCustomName()) {
                    String customName = entity.getCustomNameTag();

                    if (customName.contains("§")) {
                        String cleanName = customName.replaceAll("§.", "");
                        String[] parts = cleanName.trim().split("\\s+");
                        int estimatedModSize = Math.max(0, parts.length - 1);

                        if (minModCount > 0 && estimatedModSize < minModCount) {
                            return false;
                        }
                        if (maxModCount > 0 && estimatedModSize > maxModCount) {
                            return false;
                        }

                        return true;
                    }
                }

                return false;

            } catch (Exception e) {
                return false;
            }
        }

        // ==========================================
        // SRP检查（完整保留）
        // ==========================================

        private boolean checkSRP(EntityLivingBase entity) {
            try {
                if (entity.getClass().getName().contains("srparasites")) {
                    byte type = (byte) entity.getClass().getMethod("getParasiteType").invoke(entity);
                    if (minType > 0 && type < minType) return false;
                    if (maxType > 0 && type > maxType) return false;
                    return true;
                }
            } catch (Exception e) {
                // SRP未安装或不是寄生虫
            }
            return false;
        }

        // ==========================================
        // Lycanites检查（完整保留）
        // ==========================================

        private boolean checkLycanites(EntityLivingBase entity) {
            String className = entity.getClass().getName();
            if (!className.contains("lycanitesmobs")) return false;

            for (String interfaceName : requiredInterfaces) {
                try {
                    Class<?> interfaceClass = Class.forName("com.lycanitesmobs.api." + interfaceName);
                    if (!interfaceClass.isInstance(entity)) return false;
                } catch (Exception e) {
                    return false;
                }
            }

            for (String interfaceName : excludedInterfaces) {
                try {
                    Class<?> interfaceClass = Class.forName("com.lycanitesmobs.api." + interfaceName);
                    if (interfaceClass.isInstance(entity)) return false;
                } catch (Exception e) {
                    // 接口不存在，继续
                }
            }

            if (excludeBoss) {
                try {
                    Class<?> bossInterface = Class.forName("com.lycanitesmobs.api.IGroupBoss");
                    if (bossInterface.isInstance(entity)) return false;
                } catch (Exception e) {
                    // 接口不存在，继续
                }
            }

            return true;
        }

        // ==========================================
        // ⭐ Ice and Fire 龙阶段检查
        // ==========================================

        private boolean checkDragonStage(EntityLivingBase entity) {
            String className = entity.getClass().getName();

            // 只检查 Ice and Fire 的龙
            if (!className.contains("iceandfire")) {
                return false;
            }

            try {
                // 使用反射获取 getDragonStage() 方法
                java.lang.reflect.Method method = entity.getClass().getMethod("getDragonStage");
                int stage = (int) method.invoke(entity);

                // 检查阶段范围
                if (minDragonStage > 0 && stage < minDragonStage) {
                    return false;
                }
                if (maxDragonStage > 0 && stage > maxDragonStage) {
                    return false;
                }

                return true;

            } catch (Exception e) {
                // 如果方法不存在或调用失败，返回 false
                System.err.println("[GemLoot] 无法获取龙阶段: " + e.getMessage());
                return false;
            }
        }

        // ==========================================
        // 动态调整（完整保留）
        // ==========================================

        public LootRule applyDynamicAdjustments(EntityLivingBase entity) {
            if (!dynamicDropRate && !dynamicLevel && !growthFactorBonus && !healthScaling) {
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

            // ==========================================
            // ⭐ 血量动态加成（优先计算）
            // ==========================================
            if (healthScaling) {
                float health = entity.getMaxHealth();
                if (health >= healthScalingThreshold) {
                    // 计算超出门槛的血量
                    float excessHealth = health - healthScalingThreshold;
                    
                    // 等级加成：每100血 +5级
                    int levelBonus = (int) (excessHealth / 100.0f * 5);
                    adjusted.minLevel += levelBonus;
                    adjusted.maxLevel += levelBonus;
                    
                    // 词条加成：每200血 +1词条
                    int affixBonus = (int) (excessHealth / 200.0f);
                    adjusted.minAffixes += affixBonus;
                    adjusted.maxAffixes += affixBonus;
                    
                    // 掉落率加成：每100血 +2%
                    float dropBonus = (excessHealth / 100.0f) * 0.02f;
                    adjusted.dropChance += dropBonus;
                    
                    // 品质加成：每300血 +5%
                    float qualityBonus = (excessHealth / 300.0f) * 0.05f;
                    adjusted.minQuality += qualityBonus;
                    
                    System.out.println(String.format(
                            "[GemLoot-HealthScaling] %s (%.0f血): 等级+%d, 词条+%d, 掉落率+%.1f%%, 品质+%.1f%%",
                            entity.getName(),
                            health,
                            levelBonus,
                            affixBonus,
                            dropBonus * 100,
                            qualityBonus * 100
                    ));
                }
            }

            // Champions动态调整
            if (growthFactorBonus && entity instanceof EntityLiving) {
                try {
                    IChampionship chp = CapabilityChampionship.getChampionship((EntityLiving) entity);
                    if (chp != null && chp.getRank() != null) {
                        int growth = chp.getRank().getGrowthFactor();
                        adjusted.dropChance += growth * 0.02f;
                        adjusted.minLevel += growth * 2;
                        adjusted.maxLevel += growth * 2;
                    }
                } catch (Exception e) {
                    // 忽略
                }
            }

            // 基于词条数的动态调整
            if (dynamicDropRate || dynamicLevel) {
                // Champions词条
                if (entity instanceof EntityLiving) {
                    try {
                        IChampionship chp = CapabilityChampionship.getChampionship((EntityLiving) entity);
                        if (chp != null) {
                            int affixCount = chp.getAffixes().size();
                            if (dynamicDropRate) {
                                adjusted.dropChance += affixCount * 0.05f;
                            }
                            if (dynamicLevel) {
                                adjusted.minLevel += affixCount * 5;
                                adjusted.maxLevel += affixCount * 5;
                            }
                        }
                    } catch (Exception e) {
                        // 忽略
                    }
                }

                // Infernal Mobs词条（使用NBT检测）
                try {
                    NBTTagCompound nbt = entity.getEntityData();
                    String infernalTag = nbt.getString("InfernalMobsMod");

                    if (infernalTag != null && !infernalTag.isEmpty()) {
                        int modSize = infernalTag.trim().split("\\s+").length;

                        if (dynamicDropRate) {
                            adjusted.dropChance += modSize * 0.03f;
                        }
                        if (dynamicLevel) {
                            adjusted.minLevel += modSize * 3;
                            adjusted.maxLevel += modSize * 3;
                        }
                    } else {
                        // 备用：尝试API方式
                        if (InfernalMobsCore.getIsRareEntity(entity)) {
                            MobModifier chain = InfernalMobsCore.proxy.getRareMobs().get(entity);
                            if (chain != null) {
                                int modSize = chain.getModSize();
                                if (dynamicDropRate) {
                                    adjusted.dropChance += modSize * 0.03f;
                                }
                                if (dynamicLevel) {
                                    adjusted.minLevel += modSize * 3;
                                    adjusted.maxLevel += modSize * 3;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // 忽略
                }
            }

            // 限制最大值
            adjusted.dropChance = Math.min(adjusted.dropChance, 1.0f);
            adjusted.minLevel = Math.max(1, Math.min(adjusted.minLevel, 100));
            adjusted.maxLevel = Math.max(adjusted.minLevel, Math.min(adjusted.maxLevel, 100));
            adjusted.minAffixes = Math.max(1, Math.min(adjusted.minAffixes, 10));
            adjusted.maxAffixes = Math.max(adjusted.minAffixes, Math.min(adjusted.maxAffixes, 10));
            adjusted.minQuality = Math.max(0.0f, Math.min(adjusted.minQuality, 1.0f));

            return adjusted;
        }

        // ==========================================
        // 工具方法
        // ==========================================

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