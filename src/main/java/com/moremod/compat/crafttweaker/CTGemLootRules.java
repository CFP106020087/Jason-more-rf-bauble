package com.moremod.compat.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * CraftTweaker API - 宝石掉落规则配置（Champions & Infernal完全修复版）
 *
 * 主要修复：
 * 1. ✅ Infernal规则添加最小mod数量限制
 * 2. ✅ 提升Infernal掉落率（配合NBT检测）
 * 3. ✅ 保持Champions规则不变
 * 4. 保持Lycanites增强配置
 */
@ZenRegister
@ZenClass("mods.moremod.GemLootRules")
public class CTGemLootRules {

    @ZenMethod
    public static void setupAllRules() {
        // ⭐ 清空所有内建规则，使用下面的自定义规则
        GemLootRuleManager.clearRules();
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已清空内建规则，开始配置自定义规则");

        // Champions
        championTier1();
        championTier2();
        championTier3();
        championTier4();
        championGrowthBonus();

        // Ice and Fire龙类
        dragonYoung();
        dragonStage3();
        dragonStage4();
        dragonStage5();

        // Infernal Mobs（修复版）
        infernalElite();
        infernalUltra();
        infernalInferno();

        // Lycanites Mobs（增强版）
        lycanitesNormal();
        lycanitesRare();
        lycanitesMiniBoss();
        lycanitesBoss();
        lycanitesThreeKings();
        lycanitesSuperBoss();

        // SRP寄生虫
        srpPrimitive();
        srpEvolved();
        srpAdapted();

        // 原版Boss
        vanillaBoss();

        setStrictDefault();

        CraftTweakerAPI.logInfo("[GemRules] ✅ 已配置所有预设规则（Champions & Infernal完全修复版）");
    }

    // ==========================================
    // Champions规则（保持不变）
    // ==========================================

    @ZenMethod
    public static void championTier1() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "champion_tier1", 5, 15, 1, 2, 0.08f, 0.2f, 1
        );
        rule.matchModId("champions");
        rule.setChampionTier(1);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：Champion Tier 1（平衡调整，Lv5-15）");
    }

    @ZenMethod
    public static void championTier2() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "champion_tier2", 10, 25, 2, 3, 0.15f, 0.3f, 1
        );
        rule.matchModId("champions");
        rule.setChampionTier(2);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：Champion Tier 2（平衡调整，Lv10-25）");
    }

    @ZenMethod
    public static void championTier3() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "champion_tier3", 20, 40, 3, 4, 0.25f, 0.4f, 1
        );
        rule.matchModId("champions");
        rule.setChampionTier(3);
        rule.setRandomDropCount(1, 1);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：Champion Tier 3（平衡调整，Lv20-40）");
    }

    @ZenMethod
    public static void championTier4() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "champion_tier4", 30, 55, 4, 5, 0.4f, 0.5f, 2
        );
        rule.matchModId("champions");
        rule.setChampionTier(4);
        rule.setRandomDropCount(1, 1);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：Champion Tier 4（平衡调整，Lv30-55）");
    }

    @ZenMethod
    public static void championGrowthBonus() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "champion_growth_bonus", 1, 30, 1, 3, 0.01f, 0.1f, 1
        );
        rule.matchModId("champions");
        rule.setGrowthFactorBonus(true);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：Champion成长因子奖励（平衡调整，Lv1-30）");
    }

    // ==========================================
    // ⭐⭐⭐ Ice and Fire规则（接口判定版）⭐⭐⭐
    // ==========================================

    /**
     * 龙阶段 1-2 (幼年龙)
     * 使用 getDragonStage() 接口精确判定
     */
    @ZenMethod
    public static void dragonYoung() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "dragon_young", 8, 15, 1, 1, 0.02f, 0.1f, 1
        );
        rule.matchClassName("EntityFireDragon");
        rule.matchClassName("EntityIceDragon");
        rule.matchClassName("EntityLightningDragon");
        rule.setMaxDragonStage(2);  // ⭐ 使用接口判定：Stage 1-2
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：幼龙掉落规则（阶段1-2，Lv8-15）");
    }

    /**
     * 龙阶段 3 (三级龙)
     * ⭐ 使用接口精确判定，不再依赖血量
     */
    @ZenMethod
    public static void dragonStage3() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "dragon_stage3", 25, 30, 2, 3, 0.15f, 0.3f, 1
        );
        rule.matchClassName("EntityFireDragon");
        rule.matchClassName("EntityIceDragon");
        rule.matchClassName("EntityLightningDragon");
        rule.setDragonStage(3);  // ⭐ 精确匹配阶段 3
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：阶段3龙掉落规则（接口判定，Lv25-30）");
    }

    /**
     * 龙阶段 4 (四级龙)
     */
    @ZenMethod
    public static void dragonStage4() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "dragon_stage4", 35, 55, 3, 4, 0.3f, 0.45f, 1
        );
        rule.matchClassName("EntityFireDragon");
        rule.matchClassName("EntityIceDragon");
        rule.matchClassName("EntityLightningDragon");
        rule.setDragonStage(4);  // ⭐ 精确匹配阶段 4
        rule.setRandomDropCount(1, 1);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：阶段4龙掉落规则（接口判定，Lv35-55）");
    }

    /**
     * 龙阶段 5 (古老龙)
     */
    @ZenMethod
    public static void dragonStage5() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "dragon_stage5", 50, 70, 4, 5, 0.5f, 0.6f, 2
        );
        rule.matchClassName("EntityFireDragon");
        rule.matchClassName("EntityIceDragon");
        rule.matchClassName("EntityLightningDragon");
        rule.setDragonStage(5);  // ⭐ 精确匹配阶段 5
        rule.setRandomDropCount(1, 1);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：阶段5古老龙掉落规则（接口判定，Lv50-70）");
    }

    // ==========================================
    // ⭐⭐⭐ Infernal Mobs规则（完全修复版）⭐⭐⭐
    // ==========================================

    /**
     * Infernal Elite (2-5个修饰词)
     *
     * 修复内容：
     * - ✅ 添加setMinModCount(2)，确保只匹配Elite级别
     * - ✅ 提升掉落率从12%到50%（配合NBT检测）
     * - ✅ 提升宝石等级和词条数
     *
     * 配合GemLootRuleManager_Fixed.java的NBT检测，
     * 现在能100%可靠检测到Infernal Elite
     */
    @ZenMethod
    public static void infernalElite() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "infernal_elite",
                5, 18,               // 宝石等级5-18（平衡调整）
                2, 3,                // 2-3词条
                0.50f,               // 50%掉落率
                0.2f,                // 20%最低品质
                2                    // 2次重roll
        );

        // ✅ 修复：添加最小mod数量
        rule.setMinModCount(2);      // 至少2个修饰词
        rule.setMaxModCount(5);      // 最多5个修饰词

        // 启用动态调整（根据实际mod数提升掉落率和等级）
        rule.setDynamicDropRate(true);
        rule.setDynamicLevel(true);

        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：Infernal Elite规则（修复版，2-5 mods，50%掉落）");
    }

    /**
     * Infernal Ultra (6-10个修饰词)
     *
     * 平衡调整：
     * - ✅ 75%掉落率
     * - ✅ 降低掉落数量为1个
     */
    @ZenMethod
    public static void infernalUltra() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "infernal_ultra",
                15, 35,              // 宝石等级15-35（平衡调整）
                3, 4,                // 3-4词条
                0.75f,               // 75%掉落率
                0.3f,                // 30%最低品质
                3                    // 3次重roll
        );

        rule.setMinModCount(6);      // 至少6个修饰词
        rule.setMaxModCount(10);     // 最多10个修饰词

        // 启用动态调整
        rule.setDynamicDropRate(true);
        rule.setDynamicLevel(true);

        // Ultra掉落1个宝石（平衡调整）
        rule.setRandomDropCount(1, 1);

        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：Infernal Ultra规则（平衡版，6-10 mods，75%掉落1个）");
    }

    /**
     * Infernal Inferno (11+个修饰词)
     *
     * 平衡调整：
     * - ✅ 100%掉落率（必掉）
     * - ✅ 降低掉落数量为1个
     */
    @ZenMethod
    public static void infernalInferno() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "infernal_inferno",
                30, 55,              // 宝石等级30-55（平衡调整）
                4, 6,                // 4-6词条
                1.0f,                // 100%掉落率（必掉）
                0.5f,                // 50%最低品质
                4                    // 4次重roll
        );

        rule.setMinModCount(11);     // 至少11个修饰词
        // 不设置最大值，匹配所有11+的Infernal

        // 启用动态调整
        rule.setDynamicDropRate(true);
        rule.setDynamicLevel(true);

        // Inferno掉落1个宝石（平衡调整）
        rule.setRandomDropCount(1, 1);

        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：Infernal Inferno规则（平衡版，11+ mods，必掉1个）");
    }

    // ==========================================
    // ⭐⭐⭐ Lycanites Mobs - 增强版 ⭐⭐⭐
    // ==========================================

    /**
     * Lycanites普通生物 - 平衡调整
     */
    @ZenMethod
    public static void lycanitesNormal() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "lycanites_normal", 8, 18, 1, 2, 0.05f, 0.2f, 1
        );
        rule.matchModId("lycanitesmobs");
        rule.excludeInterface("IGroupBoss");
        rule.excludeInterface("IGroupHeavy");
        rule.setMaxHealth(80);
        rule.requireHostile(true);  // ⭐ 只匹配敌对生物（排除驯服坐骑/宠物）
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：Lycanites普通生物规则（平衡调整，Lv8-18）");
    }

    /**
     * Lycanites稀有生物 - 平衡调整
     */
    @ZenMethod
    public static void lycanitesRare() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "lycanites_rare", 18, 35, 2, 3, 0.15f, 0.3f, 1
        );
        rule.matchModId("lycanitesmobs");
        rule.excludeInterface("IGroupBoss");
        rule.excludeInterface("IGroupHeavy");
        rule.setMinHealth(80);
        rule.requireHostile(true);  // ⭐ 只匹配敌对生物（排除驯服坐骑/宠物）
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：Lycanites稀有生物规则（平衡调整，Lv18-35）");
    }

    /**
     * Lycanites精英/MiniBoss - 平衡调整
     */
    @ZenMethod
    public static void lycanitesMiniBoss() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "lycanites_miniboss", 60, 75, 3, 4, 0.35f, 0.4f, 2
        );
        rule.matchModId("lycanitesmobs");
        rule.matchInterface("IGroupHeavy");
        rule.setRandomDropCount(1, 1);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：Lycanites MiniBoss规则（平衡调整，Lv60-75）");
    }

    /**
     * Lycanites普通Boss - 平衡调整
     */
    @ZenMethod
    public static void lycanitesBoss() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "lycanites_boss", 40, 65, 4, 5, 0.7f, 0.55f, 2
        );
        rule.matchModId("lycanitesmobs");
        rule.matchInterface("IGroupBoss");
        rule.setRandomDropCount(1, 1);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：Lycanites Boss规则（平衡调整，Lv40-65）");
    }

    /**
     * ⭐⭐⭐ Lycanites三王 - 必掉100级宝石 ⭐⭐⭐
     */
    @ZenMethod
    public static void lycanitesThreeKings() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "lycanites_three_kings",
                100, 100,           // 固定100级
                5, 6,               // 5-6词条
                1.0f,               // 100%掉落率
                0.8f,               // 80%最低品质
                5                   // 5次重roll
        );

        // 精确匹配三王类名
        rule.matchClassName("EntityAmalgalich");
        rule.matchClassName("EntityAsmodeus");
        rule.matchClassName("EntityRahovart");

        // 掉落1个宝石
        rule.setRandomDropCount(1, 1);

        // 最高优先级
        rule.setPriority(1000);

        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅✅✅ 已添加：Lycanites三王规则（固定Lv100必掉1个）");
    }

    /**
     * Lycanites其他超级Boss（非三王）
     */
    @ZenMethod
    public static void lycanitesSuperBoss() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "lycanites_superboss", 55, 80, 4, 6, 0.85f, 0.65f, 3
        );
        rule.matchModId("lycanitesmobs");
        rule.matchInterface("IGroupBoss");
        rule.setMinHealth(500);
        rule.setRandomDropCount(1, 1);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：Lycanites超级Boss规则（平衡调整，Lv55-80）");
    }

    // ==========================================
    // SRP规则（保持不变）
    // ==========================================

    @ZenMethod
    public static void srpPrimitive() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "srp_primitive", 10, 25, 1, 2, 0.03f, 0.15f, 1
        );
        rule.matchModId("srparasites");
        rule.setMaxType(19);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：SRP原始寄生虫规则");
    }

    @ZenMethod
    public static void srpEvolved() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "srp_evolved", 25, 45, 2, 3, 0.08f, 0.3f, 1
        );
        rule.matchModId("srparasites");
        rule.setMinType(20);
        rule.setMaxType(50);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：SRP进化寄生虫规则");
    }

    @ZenMethod
    public static void srpAdapted() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "srp_adapted", 45, 70, 3, 4, 0.15f, 0.45f, 2
        );
        rule.matchModId("srparasites");
        rule.setMinType(51);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：SRP适应寄生虫规则");
    }

    // ==========================================
    // 原版Boss规则（保持不变）
    // ==========================================

    @ZenMethod
    public static void vanillaBoss() {
        GemLootRuleManager.LootRule dragon = new GemLootRuleManager.LootRule(
                "vanilla_ender_dragon", 70, 90, 4, 5, 0.8f, 0.6f, 3
        );
        dragon.matchClassName("EntityDragon");
        GemLootRuleManager.addRule(dragon);

        GemLootRuleManager.LootRule wither = new GemLootRuleManager.LootRule(
                "vanilla_wither", 60, 85, 4, 5, 0.7f, 0.55f, 3
        );
        wither.matchClassName("EntityWither");
        GemLootRuleManager.addRule(wither);

        GemLootRuleManager.LootRule elder = new GemLootRuleManager.LootRule(
                "vanilla_elder_guardian", 40, 60, 2, 3, 0.3f, 0.4f, 1
        );
        elder.matchClassName("EntityElderGuardian");
        GemLootRuleManager.addRule(elder);

        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加：原版Boss规则");
    }

    // ==========================================
    // 简化自定义规则方法（保持不变）
    // ==========================================

    @ZenMethod
    public static void add(String entityName, int minLevel, int maxLevel,
                           int minAffixes, int maxAffixes, double dropChance) {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "custom_" + entityName,
                minLevel, maxLevel,
                minAffixes, maxAffixes,
                (float) dropChance,
                0.0f, 1
        );
        rule.matchEntityName(entityName);
        GemLootRuleManager.addRule(rule);

        CraftTweakerAPI.logInfo(String.format(
                "[GemRules] ✅ 已添加规则: %s (Lv%d-%d, %d-%d词条, %.0f%%掉落)",
                entityName, minLevel, maxLevel, minAffixes, maxAffixes, dropChance * 100
        ));
    }

    @ZenMethod
    public static void addByClass(String className, int minLevel, int maxLevel,
                                  int minAffixes, int maxAffixes, double dropChance) {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "class_" + className,
                minLevel, maxLevel,
                minAffixes, maxAffixes,
                (float) dropChance,
                0.0f, 1
        );
        rule.matchClassName(className);
        GemLootRuleManager.addRule(rule);

        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加类名规则: " + className);
    }

    @ZenMethod
    public static void addByMod(String modId, int minLevel, int maxLevel,
                                int minAffixes, int maxAffixes, double dropChance) {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "mod_" + modId,
                minLevel, maxLevel,
                minAffixes, maxAffixes,
                (float) dropChance,
                0.0f, 1
        );
        rule.matchModId(modId);
        GemLootRuleManager.addRule(rule);

        CraftTweakerAPI.logInfo("[GemRules] ✅ 已添加模组规则: " + modId);
    }

    @ZenMethod
    public static void custom(String id, String matchName,
                              int minLevel, int maxLevel,
                              int minAffixes, int maxAffixes,
                              double dropChance, double minQuality, int rerollCount) {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                id,
                minLevel, maxLevel,
                minAffixes, maxAffixes,
                (float) dropChance,
                (float) minQuality,
                rerollCount
        );
        rule.matchEntityName(matchName);
        GemLootRuleManager.addRule(rule);

        CraftTweakerAPI.logInfo(String.format(
                "[GemRules] ✅ 已添加自定义规则: %s (品质≥%.0f%%, roll×%d)",
                id, minQuality * 100, rerollCount
        ));
    }

    // ==========================================
    // 管理方法（保持不变）
    // ==========================================

    @ZenMethod
    public static void remove(String id) {
        boolean removed = GemLootRuleManager.removeRule(id);
        if (removed) {
            CraftTweakerAPI.logInfo("[GemRules] ✅ 已移除规则: " + id);
        } else {
            CraftTweakerAPI.logWarning("[GemRules] ⚠️ 规则不存在: " + id);
        }
    }

    @ZenMethod
    public static void clear() {
        GemLootRuleManager.clearRules();
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已清空所有规则");
    }

    @ZenMethod
    public static void setStrictDefault() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "default",
                5, 20,
                1, 1,
                0.01f,
                0.1f, 1
        );
        GemLootRuleManager.setDefaultRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已设置严格默认规则");
    }

    @ZenMethod
    public static void setDefault(int minLevel, int maxLevel,
                                  int minAffixes, int maxAffixes,
                                  double dropChance) {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "default",
                minLevel, maxLevel,
                minAffixes, maxAffixes,
                (float) dropChance,
                0.0f, 1
        );
        GemLootRuleManager.setDefaultRule(rule);
        CraftTweakerAPI.logInfo("[GemRules] ✅ 已设置默认规则");
    }

    // ==========================================
    // 快速添加规则（便捷方法）
    // ==========================================

    /**
     * 快速添加单个生物规则（带品质和重roll参数）
     *
     * @param entityName 实体名称
     * @param minLevel 最小等级
     * @param maxLevel 最大等级
     * @param minAffixes 最小词条数
     * @param maxAffixes 最大词条数
     * @param dropChance 掉落概率 (0.0-1.0)
     * @param minQuality 最低品质 (0.0-1.0)
     * @param rerollCount 重roll次数
     */
    @ZenMethod
    public static void addAdvanced(String entityName, int minLevel, int maxLevel,
                                   int minAffixes, int maxAffixes,
                                   double dropChance, double minQuality, int rerollCount) {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "custom_" + entityName.toLowerCase(),
                minLevel, maxLevel,
                minAffixes, maxAffixes,
                (float) dropChance,
                (float) minQuality,
                rerollCount
        );

        if (entityName.startsWith("Entity")) {
            rule.matchClassName(entityName);
        } else {
            rule.matchEntityName(entityName);
        }

        GemLootRuleManager.addRule(rule);

        CraftTweakerAPI.logInfo(String.format(
                "[GemRules] ✅ 已添加高级自定义规则: %s (Lv%d-%d, 品质≥%.0f%%, roll×%d)",
                entityName, minLevel, maxLevel, minQuality * 100, rerollCount
        ));
    }

    // ==========================================
    // 调试工具（保持不变）
    // ==========================================

    @ZenMethod
    public static void setDebug(boolean enable) {
        GemLootGenerator.setDebugMode(enable);
        CraftTweakerAPI.logInfo("[GemRules] 调试模式: " + (enable ? "开启" : "关闭"));
    }

    @ZenMethod
    public static void printRules() {
        CraftTweakerAPI.logInfo("========== 宝石掉落规则 ==========");
        GemLootRuleManager.getAllRules().forEach((id, rule) -> {
            CraftTweakerAPI.logInfo(String.format(
                    "ID: %s | Lv%d-%d | %d-%d词条 | %.1f%%掉落",
                    id, rule.minLevel, rule.maxLevel,
                    rule.minAffixes, rule.maxAffixes,
                    rule.dropChance * 100
            ));
        });
        CraftTweakerAPI.logInfo("================================");
    }
}