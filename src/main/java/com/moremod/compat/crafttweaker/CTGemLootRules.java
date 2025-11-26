package com.moremod.compat.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * POE风格 T1-T10 宝石掉落系统 v2.0 完整版
 *
 * 一键配置所有规则：setupAllRules()
 * SRP正确排序：Ancient > Preeminent > Pure > Adapted > Primitive > Crude
 */
@ZenRegister
@ZenClass("mods.moremod.GemLootRules")
public class CTGemLootRules {

    // ==========================================
    // 🎮 一键配置入口（最重要的方法）
    // ==========================================

    @ZenMethod
    public static void setupAllRules() {
        // 清空旧规则
        GemLootRuleManager.clearRules();

        CraftTweakerAPI.logInfo("========================================");
        CraftTweakerAPI.logInfo("[POE宝石系统] 开始配置T1-T10规则...");
        CraftTweakerAPI.logInfo("========================================");

        // 调用POE系统配置
        setupPOETierSystem();

        // 设置系统参数
        GemLootGenerator.setFilterPeaceful(true);  // 启用友善生物过滤
        GemLootGenerator.setMaxGemLevel(100);      // 最大等级100
        GemLootGenerator.setDebugMode(false);      // 关闭调试（生产环境）
        GemLootGenerator.setEnabled(true);         // 启用系统

        CraftTweakerAPI.logInfo("========================================");
        CraftTweakerAPI.logInfo("[POE宝石系统] ✅ 配置完成！");
        CraftTweakerAPI.logInfo("[POE宝石系统] 规则数量: " + GemLootRuleManager.getAllRules().size());
        CraftTweakerAPI.logInfo("========================================");
    }

    // ==========================================
    // POE Tier 系统核心配置
    // ==========================================

    @ZenMethod
    public static void setupPOETierSystem() {
        // T1: Lv 10-20 (垃圾怪)
        setupT1_Trash();

        // T2: Lv 20-30 (普通怪)
        setupT2_Common();

        // T3: Lv 30-40 (SRP最弱: Crude/Dispatcher/Rooster)
        setupT3_Advanced();
        setupT3_SRP_Weakest();

        // T4: Lv 40-50 (SRP Primitive, Stage3龙, Champions T1)
        setupT4_Basic();
        setupT4_SRP_Primitive();
        setupT4_DragonStage3();
        setupT4_ChampionsTier1();

        // T5: Lv 50-60 (SRP Adapted/Feral/Hijacked, Champions T2-3)
        setupT5_Intermediate();
        setupT5_SRP_Adapted();
        setupT5_ChampionsLow();
        setupT5_InfernalLow();

        // T6: Lv 60-70 (SRP Pure/Cosmical, Stage4龙, Champions T4-5)
        setupT6_Advanced();
        setupT6_SRP_Pure();
        setupT6_DragonStage4();
        setupT6_ChampionsMid();
        setupT6_InfernalMid();

        // T7: Lv 70-80 (SRP Preeminent/Stationary, Champions T6-7)
        setupT7_Elite();
        setupT7_SRP_Preeminent();
        setupT7_ChampionsHigh();
        setupT7_InfernalHigh();
        setupT7_LycanitesElite();

        // T8: Lv 80-90 (SRP Ancient最强, Stage5龙, Champions T8-9)
        setupT8_Boss();
        setupT8_SRP_Ancient();
        setupT8_DragonStage5();
        setupT8_ChampionsTop();
        setupT8_InfernalUltra();
        setupT8_LycanitesBoss();

        // T9: Lv 90-99 (双重精英, Champions T10, 原版Boss)
        setupT9_SubLegendary();
        setupT9_DoubleElite();
        setupT9_ChampionsTier10();
        setupT9_VanillaBoss();

        // T10: Lv 100 (三王专属)
        setupT10_ThreeKingsOnly();

        // 默认规则
        setDefaultRule();
    }

    // ==========================================
    // T1 规则 (Lv 10-20)
    // ==========================================

    @ZenMethod
    private static void setupT1_Trash() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t1_trash",
                10, 20,
                1, 1,
                0.05f,
                0.0f,
                1
        );
        rule.setMaxHealth(30);
        rule.setPriority(100);
        GemLootRuleManager.addRule(rule);
    }

    // ==========================================
    // T2 规则 (Lv 20-30)
    // ==========================================

    @ZenMethod
    private static void setupT2_Common() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t2_common",
                20, 30,
                1, 2,
                0.06f,
                0.05f,
                1
        );
        rule.setMinHealth(20);
        rule.setMaxHealth(60);
        rule.setPriority(150);
        GemLootRuleManager.addRule(rule);
    }

    // ==========================================
    // T3 规则 (Lv 30-40) - SRP最弱等级
    // ==========================================

    @ZenMethod
    private static void setupT3_Advanced() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t3_advanced",
                30, 40,
                1, 2,
                0.08f,
                0.10f,
                1
        );
        rule.setMinHealth(40);
        rule.setMaxHealth(100);
        rule.setPriority(250);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT3_SRP_Weakest() {
        // SRP最弱: Crude, Dispatcher, Rooster等
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t3_srp_weakest",
                30, 40,
                1, 2,
                0.08f,
                0.10f,
                1
        );
        rule.matchModId("srparasites");
        rule.matchClassPattern(".*\\.(EntityPCrude|EntityPDispatcher|EntityPRooster)$");
        rule.setPriority(350);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[T3] SRP最弱寄生虫: Crude/Dispatcher/Rooster");
    }

    // ==========================================
    // T4 规则 (Lv 40-50) - SRP Primitive
    // ==========================================

    @ZenMethod
    private static void setupT4_Basic() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t4_basic",
                40, 50,
                2, 2,
                0.12f,
                0.15f,
                2
        );
        rule.setMinHealth(80);
        rule.setMaxHealth(150);
        rule.setPriority(400);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT4_SRP_Primitive() {
        // SRP Primitive级别
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t4_srp_primitive",
                40, 50,
                2, 2,
                0.12f,
                0.15f,
                1
        );
        rule.matchModId("srparasites");
        rule.matchClassPattern(".*\\.(EntityPPrimitive|EntityPMalleable)$");
        rule.setPriority(450);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[T4] SRP Primitive寄生虫");
    }

    @ZenMethod
    private static void setupT4_DragonStage3() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t4_dragon_stage3",
                40, 50,
                2, 3,
                0.20f,
                0.20f,
                2
        );
        rule.matchClassName("EntityFireDragon");
        rule.matchClassName("EntityIceDragon");
        rule.matchClassName("EntityLightningDragon");
        rule.setDragonStage(3);
        rule.setPriority(500);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[T4] Ice and Fire Stage 3龙");
    }

    @ZenMethod
    private static void setupT4_ChampionsTier1() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t4_champions_tier1",
                40, 50,
                2, 2,
                0.15f,
                0.15f,
                2
        );
        rule.matchModId("champions");
        rule.setChampionTier(1);
        rule.setPriority(450);
        GemLootRuleManager.addRule(rule);
    }

    // ==========================================
    // T5 规则 (Lv 50-60) - SRP Adapted
    // ==========================================

    @ZenMethod
    private static void setupT5_Intermediate() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t5_intermediate",
                50, 60,
                2, 3,
                0.15f,
                0.20f,
                2
        );
        rule.setMinHealth(100);
        rule.setPriority(500);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT5_SRP_Adapted() {
        // SRP Adapted级别: Adapted, Feral, Hijacked
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t5_srp_adapted",
                50, 60,
                2, 3,
                0.18f,
                0.20f,
                2
        );
        rule.matchModId("srparasites");
        rule.matchClassPattern(".*\\.(EntityPAdapted|EntityPFeral|EntityPHijacked)$");
        rule.setPriority(550);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[T5] SRP Adapted/Feral/Hijacked寄生虫");
    }

    @ZenMethod
    private static void setupT5_ChampionsLow() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t5_champions_low",
                50, 60,
                2, 3,
                0.18f,
                0.20f,
                2
        );
        rule.matchModId("champions");
        rule.setMinChampionTier(2);
        rule.setMaxChampionTier(3);
        rule.setDynamicDropRate(true);
        rule.setPriority(550);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT5_InfernalLow() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t5_infernal_low",
                50, 60,
                2, 3,
                0.20f,
                0.20f,
                2
        );
        rule.setMinModCount(2);
        rule.setMaxModCount(3);
        rule.setDynamicDropRate(true);
        rule.setPriority(550);
        GemLootRuleManager.addRule(rule);
    }

    // ==========================================
    // T6 规则 (Lv 60-70) - SRP Pure
    // ==========================================

    @ZenMethod
    private static void setupT6_Advanced() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t6_advanced",
                60, 70,
                3, 4,
                0.25f,
                0.30f,
                2
        );
        rule.setMinHealth(150);
        rule.setPriority(600);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT6_SRP_Pure() {
        // SRP Pure级别: Pure, Cosmical
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t6_srp_pure",
                60, 70,
                3, 4,
                0.30f,
                0.30f,
                2
        );
        rule.matchModId("srparasites");
        rule.matchClassPattern(".*\\.(EntityPPure|EntityPCosmical)$");
        rule.setPriority(650);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[T6] SRP Pure/Cosmical寄生虫");
    }

    @ZenMethod
    private static void setupT6_DragonStage4() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t6_dragon_stage4",
                60, 70,
                3, 4,
                0.45f,
                0.35f,
                3
        );
        rule.matchClassName("EntityFireDragon");
        rule.matchClassName("EntityIceDragon");
        rule.matchClassName("EntityLightningDragon");
        rule.setDragonStage(4);
        rule.setPriority(700);
        rule.setRandomDropCount(1, 1);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[T6] Ice and Fire Stage 4龙");
    }

    @ZenMethod
    private static void setupT6_ChampionsMid() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t6_champions_mid",
                60, 70,
                3, 4,
                0.30f,
                0.30f,
                2
        );
        rule.matchModId("champions");
        rule.setMinChampionTier(4);
        rule.setMaxChampionTier(5);
        rule.setDynamicLevel(true);
        rule.setPriority(650);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT6_InfernalMid() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t6_infernal_mid",
                60, 70,
                3, 4,
                0.30f,
                0.30f,
                2
        );
        rule.setMinModCount(4);
        rule.setMaxModCount(5);
        rule.setDynamicLevel(true);
        rule.setPriority(650);
        GemLootRuleManager.addRule(rule);
    }

    // ==========================================
    // T7 规则 (Lv 70-80) - SRP Preeminent
    // ==========================================

    @ZenMethod
    private static void setupT7_Elite() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t7_elite",
                70, 80,
                3, 4,
                0.35f,
                0.35f,
                3
        );
        rule.setMinHealth(200);
        rule.setPriority(700);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT7_SRP_Preeminent() {
        // SRP Preeminent级别: Preeminent, Stationary
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t7_srp_preeminent",
                70, 80,
                3, 4,
                0.40f,
                0.40f,
                3
        );
        rule.matchModId("srparasites");
        rule.matchClassPattern(".*\\.(EntityPPreeminent|EntityPStationary)$");
        rule.setPriority(750);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[T7] SRP Preeminent/Stationary寄生虫");
    }

    @ZenMethod
    private static void setupT7_ChampionsHigh() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t7_champions_high",
                70, 80,
                3, 4,
                0.40f,
                0.40f,
                3
        );
        rule.matchModId("champions");
        rule.setMinChampionTier(6);
        rule.setMaxChampionTier(7);
        rule.setDynamicLevel(true);
        rule.setDynamicDropRate(true);
        rule.setPriority(750);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT7_InfernalHigh() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t7_infernal_high",
                70, 80,
                3, 5,
                0.40f,
                0.35f,
                3
        );
        rule.setMinModCount(6);
        rule.setMaxModCount(7);
        rule.setDynamicLevel(true);
        rule.setPriority(750);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT7_LycanitesElite() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t7_lycanites_elite",
                70, 80,
                3, 4,
                0.35f,
                0.35f,
                3
        );
        rule.matchModId("lycanitesmobs");
        rule.matchInterface("IGroupHeavy");
        rule.requireHostile(true);
        rule.setPriority(750);
        GemLootRuleManager.addRule(rule);
    }

    // ==========================================
    // T8 规则 (Lv 80-90) - SRP Ancient (最强)
    // ==========================================

    @ZenMethod
    private static void setupT8_Boss() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t8_boss",
                80, 90,
                4, 5,
                0.50f,
                0.45f,
                3
        );
        rule.setMinHealth(300);
        rule.setPriority(800);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT8_SRP_Ancient() {
        // SRP Ancient级别: Ancient, StationaryArchitect (最强)
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t8_srp_ancient",
                80, 90,
                4, 5,
                0.60f,  // 60%掉落率
                0.50f,
                3
        );
        rule.matchModId("srparasites");
        rule.matchClassPattern(".*\\.(EntityPAncient|EntityPStationaryArchitect)$");
        rule.setPriority(850);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[T8] SRP Ancient/StationaryArchitect寄生虫（最强）");
    }

    @ZenMethod
    private static void setupT8_DragonStage5() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t8_dragon_stage5",
                80, 90,
                4, 5,
                1.0f,  // 100%必掉
                0.50f,
                3
        );
        rule.matchClassName("EntityFireDragon");
        rule.matchClassName("EntityIceDragon");
        rule.matchClassName("EntityLightningDragon");
        rule.setDragonStage(5);
        rule.setPriority(900);
        rule.setRandomDropCount(1, 2);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[T8] Ice and Fire Stage 5龙（必掉）");
    }

    @ZenMethod
    private static void setupT8_ChampionsTop() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t8_champions_top",
                80, 90,
                4, 5,
                0.60f,
                0.50f,
                3
        );
        rule.matchModId("champions");
        rule.setMinChampionTier(8);
        rule.setMaxChampionTier(9);
        rule.setDynamicLevel(true);
        rule.setDynamicDropRate(true);
        rule.setPriority(850);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT8_InfernalUltra() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t8_infernal_ultra",
                80, 90,
                4, 5,
                0.55f,
                0.45f,
                3
        );
        rule.setMinModCount(8);
        rule.setDynamicLevel(true);
        rule.setPriority(850);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT8_LycanitesBoss() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t8_lycanites_boss",
                80, 90,
                4, 5,
                0.70f,
                0.50f,
                3
        );
        rule.matchModId("lycanitesmobs");
        rule.matchInterface("IGroupBoss");
        // 排除三王（他们是T10）
        rule.excludeInterface("EntityRahovart");
        rule.excludeInterface("EntityAsmodeus");
        rule.excludeInterface("EntityAmalgalich");
        rule.setPriority(850);
        rule.setRandomDropCount(1, 1);
        GemLootRuleManager.addRule(rule);
    }

    // ==========================================
    // T9 规则 (Lv 90-99) - 副传奇
    // ==========================================

    @ZenMethod
    private static void setupT9_SubLegendary() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t9_sublegendary",
                90, 99,
                5, 6,
                0.75f,
                0.55f,
                4
        );
        rule.setMinHealth(400);
        rule.setPriority(900);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT9_DoubleElite() {
        // Champions + Infernal双重强化
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t9_double_elite",
                90, 99,
                5, 6,
                0.80f,
                0.60f,
                4
        );
        rule.matchModId("champions");
        rule.setMinChampionTier(5);
        rule.setMinModCount(3);  // 同时有Infernal词条
        rule.setPriority(950);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT9_ChampionsTier10() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t9_champions_tier10",
                90, 99,
                5, 6,
                0.80f,
                0.60f,
                4
        );
        rule.matchModId("champions");
        rule.setMinChampionTier(10);
        rule.setPriority(950);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT9_VanillaBoss() {
        // 末影龙
        GemLootRuleManager.LootRule dragon = new GemLootRuleManager.LootRule(
                "t9_ender_dragon",
                90, 99,
                5, 6,
                0.85f,
                0.60f,
                4
        );
        dragon.matchClassName("EntityDragon");
        dragon.setRandomDropCount(1, 2);
        dragon.setPriority(950);
        GemLootRuleManager.addRule(dragon);

        // 凋灵
        GemLootRuleManager.LootRule wither = new GemLootRuleManager.LootRule(
                "t9_wither",
                90, 99,
                5, 6,
                0.80f,
                0.55f,
                4
        );
        wither.matchClassName("EntityWither");
        wither.setRandomDropCount(1, 2);
        wither.setPriority(950);
        GemLootRuleManager.addRule(wither);
    }

    // ==========================================
    // T10 规则 (Lv 100) - 三王专属
    // ==========================================

    @ZenMethod
    private static void setupT10_ThreeKingsOnly() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t10_three_kings_only",
                100, 100,  // 固定Lv100
                6, 6,      // 固定6词条
                1.0f,      // 100%必掉
                0.80f,     // 80%品质下限
                5          // 5次reroll
        );

        // 精确匹配三王类名
        rule.matchClassName("EntityRahovart");
        rule.matchClassName("EntityAsmodeus");
        rule.matchClassName("EntityAmalgalich");

        rule.setPriority(Integer.MAX_VALUE);  // 最高优先级
        rule.setRandomDropCount(1, 3);        // 掉落1-3个

        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[T10] ⭐⭐⭐ Lycanites三王专属（固定Lv100）");
    }

    // ==========================================
    // 默认规则
    // ==========================================

    @ZenMethod
    private static void setDefaultRule() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "default",
                10, 20,
                1, 1,
                0.05f,
                0.0f,
                1
        );
        GemLootRuleManager.setDefaultRule(rule);
        CraftTweakerAPI.logInfo("[默认] 兜底规则 (Lv10-20, 5%掉落)");
    }

    // ==========================================
    // 工具方法
    // ==========================================

    @ZenMethod
    public static void clear() {
        GemLootRuleManager.clearRules();
        CraftTweakerAPI.logInfo("[POE宝石] 已清空所有规则");
    }

    @ZenMethod
    public static void printSummary() {
        CraftTweakerAPI.logInfo("========== POE T1-T10 总览 ==========");
        CraftTweakerAPI.logInfo("T1  (10-20): 垃圾怪 5%");
        CraftTweakerAPI.logInfo("T2  (20-30): 普通怪 6%");
        CraftTweakerAPI.logInfo("T3  (30-40): SRP Crude 8%");
        CraftTweakerAPI.logInfo("T4  (40-50): SRP Primitive/Stage3龙 12-20%");
        CraftTweakerAPI.logInfo("T5  (50-60): SRP Adapted 18-20%");
        CraftTweakerAPI.logInfo("T6  (60-70): SRP Pure/Stage4龙 30-45%");
        CraftTweakerAPI.logInfo("T7  (70-80): SRP Preeminent 40%");
        CraftTweakerAPI.logInfo("T8  (80-90): SRP Ancient/Stage5龙 60-100%");
        CraftTweakerAPI.logInfo("T9  (90-99): 双重精英 80%");
        CraftTweakerAPI.logInfo("T10 (100):   三王专属 100%");
        CraftTweakerAPI.logInfo("=====================================");
        CraftTweakerAPI.logInfo("SRP排序: Ancient > Preeminent > Pure > Adapted > Primitive > Crude");
        CraftTweakerAPI.logInfo("规则总数: " + GemLootRuleManager.getAllRules().size());
    }
}