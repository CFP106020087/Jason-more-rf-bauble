package com.moremod.compat.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * POE T1-T10 宝石掉落系统 v3.0 - 无血量判定版
 *
 * 核心改动：
 * ✅ 完全移除所有血量判定
 * ✅ 仅使用精确的类名/模组/接口匹配
 * ✅ 九头蛇和哀悼者使用精确匹配
 */
@ZenRegister
@ZenClass("mods.moremod.GemLootRules")
public class CTGemLootRules {

    @ZenMethod
    public static void setupAllRules() {
        GemLootRuleManager.clearRules();

        CraftTweakerAPI.logInfo("========================================");
        CraftTweakerAPI.logInfo("[POE v3.0] 配置无血量判定规则...");
        CraftTweakerAPI.logInfo("========================================");

        setupPOETierSystem();

        GemLootGenerator.setFilterPeaceful(true);
        GemLootGenerator.setMaxGemLevel(100);
        GemLootGenerator.setDebugMode(false);
        GemLootGenerator.setEnabled(true);

        CraftTweakerAPI.logInfo("[POE v3.0] ✅ 配置完成（无血量判定）");
    }

    @ZenMethod
    public static void setupPOETierSystem() {
        // T1-T10配置，完全不使用血量

        // T1: Lv 10-20 (默认垃圾怪)
        setupT1_Default();

        // T2: Lv 20-30 (基础怪物)
        setupT2_Basic();

        // T3: Lv 30-40 (SRP Crude等)
        setupT3_SRPWeak();

        // T4: Lv 40-50 (Stage3龙, SRP Primitive)
        setupT4_Stage3Dragon();
        setupT4_SRPPrimitive();
        setupT4_ChampionsTier1();

        // T5: Lv 50-60 (Champions T2-3, SRP Adapted)
        setupT5_ChampionsLow();
        setupT5_InfernalLow();
        setupT5_SRPAdapted();

        // T6: Lv 60-70 (Stage4龙, 九头蛇, SRP Pure)
        setupT6_Stage4Dragon();
        setupT6_IceAndFireHydra();
        setupT6_ChampionsMid();
        setupT6_InfernalMid();
        setupT6_SRPPure();

        // T7: Lv 70-80 (哀悼者, SRP Preeminent)
        setupT7_ChampionsHigh();
        setupT7_InfernalHigh();
        setupT7_SRPPreeminent();
        setupT7_LycanitesElite();

        // T8: Lv 80-90 (Stage5龙, SRP Ancient)
        setupT8_Stage5Dragon();
        setupT8_ChampionsTop();
        setupT8_InfernalUltra();
        setupT8_SRPAncient();
        setupT8_LycanitesBoss();

        // T9: Lv 90-99 (凋灵, 末影龙)
        setupT9_VanillaBosses();
        setupT9_ChampionsTier10();

        // T10: Lv 100 (三王)
        setupT10_ThreeKings();

        // 默认规则
        setDefaultRule();
    }

    // ==========================================
    // T1-T2: 基础怪物
    // ==========================================

    @ZenMethod
    private static void setupT1_Default() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t1_default",
                10, 20,
                1, 1,
                0.05f,
                0.0f,
                1
        );
        // 不设置任何条件，作为最低优先级的兜底
        rule.setPriority(1);
        GemLootRuleManager.setDefaultRule(rule);
    }

    @ZenMethod
    private static void setupT2_Basic() {
        // 特定的基础怪物列表
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t2_basic",
                20, 30,
                1, 2,
                0.06f,
                0.05f,
                1
        );
        rule.matchClassName("EntityZombie");
        rule.matchClassName("EntitySkeleton");
        rule.matchClassName("EntitySpider");
        rule.matchClassName("EntityCreeper");
        rule.setPriority(150);
        GemLootRuleManager.addRule(rule);
    }

    // ==========================================
    // T3: SRP最弱
    // ==========================================

    @ZenMethod
    private static void setupT3_SRPWeak() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t3_srp_weak",
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
    }

    // ==========================================
    // T4: Stage3龙等
    // ==========================================

    @ZenMethod
    private static void setupT4_Stage3Dragon() {
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
    }

    @ZenMethod
    private static void setupT4_SRPPrimitive() {
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
    }

    @ZenMethod
    private static void setupT4_ChampionsTier1() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t4_champions_t1",
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
    // T5: 中级精英
    // ==========================================

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
        rule.setPriority(550);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT5_SRPAdapted() {
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
    }

    // ==========================================
    // T6: 高级精英（包括九头蛇）
    // ==========================================

    @ZenMethod
    private static void setupT6_Stage4Dragon() {
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
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT6_IceAndFireHydra() {
        GemLootRuleManager.LootRule hydra = new GemLootRuleManager.LootRule(
                "t6_hydra",
                60, 70,
                3, 4,
                0.40f,
                0.35f,
                3
        );
        // 精确匹配九头蛇
        hydra.matchClassName("com.github.alexthe666.iceandfire.entity.EntityHydra");
        hydra.setPriority(680);
        hydra.setRandomDropCount(1, 2);
        GemLootRuleManager.addRule(hydra);
        CraftTweakerAPI.logInfo("[T6] Ice and Fire九头蛇（精确匹配）");
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
        rule.setPriority(650);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT6_SRPPure() {
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
    }

    // ==========================================
    // T7: 顶级精英（包括哀悼者）
    // ==========================================



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
        rule.setPriority(750);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT7_SRPPreeminent() {
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
    }

    @ZenMethod
    private static void setupT7_LycanitesElite() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t7_lycanites_elite",
                70, 80,
                3, 4,
                0.15f,
                0.15f,
                3
        );
        rule.matchModId("lycanitesmobs");
        rule.matchInterface("IGroupDemon");

        rule.setPriority(750);
        GemLootRuleManager.addRule(rule);
    }

    // ==========================================
    // T8: Boss级
    // ==========================================

    @ZenMethod
    private static void setupT8_Stage5Dragon() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t8_dragon_stage5",
                80, 90,
                4, 5,
                1.0f,
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
        rule.setPriority(850);
        GemLootRuleManager.addRule(rule);
    }

    @ZenMethod
    private static void setupT8_SRPAncient() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t8_srp_ancient",
                80, 90,
                4, 5,
                0.60f,
                0.50f,
                3
        );
        rule.matchModId("srparasites");
        rule.matchClassPattern(".*\\.(EntityPAncient|EntityPStationaryArchitect)$");
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
        // 排除三王
        rule.excludeInterface("EntityRahovart");
        rule.excludeInterface("EntityAsmodeus");
        rule.excludeInterface("EntityAmalgalich");
        rule.setPriority(850);
        GemLootRuleManager.addRule(rule);
    }

    // ==========================================
    // T9: 传奇Boss
    // ==========================================

    @ZenMethod
    private static void setupT9_VanillaBosses() {
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
        dragon.setPriority(950);
        dragon.setRandomDropCount(1, 2);
        GemLootRuleManager.addRule(dragon);

        // 凋灵（精确匹配）
        GemLootRuleManager.LootRule wither = new GemLootRuleManager.LootRule(
                "t9_wither",
                90, 99,
                5, 6,
                0.80f,
                0.55f,
                4
        );
        wither.matchClassName("net.minecraft.entity.boss.EntityWither");
        wither.setPriority(960);
        wither.setRandomDropCount(1, 2);
        GemLootRuleManager.addRule(wither);
    }

    @ZenMethod
    private static void setupT9_ChampionsTier10() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t9_champions_t10",
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

    // ==========================================
    // T10: 三王
    // ==========================================

    @ZenMethod
    private static void setupT10_ThreeKings() {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "t10_three_kings",
                100, 100,
                6, 6,
                1.0f,
                0.80f,
                5
        );
        rule.matchClassName("EntityRahovart");
        rule.matchClassName("EntityAsmodeus");
        rule.matchClassName("EntityAmalgalich");
        rule.setPriority(Integer.MAX_VALUE);
        rule.setRandomDropCount(1, 3);
        GemLootRuleManager.addRule(rule);
        CraftTweakerAPI.logInfo("[T10] Lycanites三王（固定Lv100）");
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
    }
}