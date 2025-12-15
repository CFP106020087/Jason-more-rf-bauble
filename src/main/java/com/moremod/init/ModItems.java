package com.moremod.init;

import com.moremod.item.ItemMaterial;
import com.moremod.item.ItemBasicFabric;
// 🏪 添加村民胶囊导入
import com.moremod.item.ItemVillagerCapsule;
// 🌌 添加虚空背包链接导入
import com.moremod.item.ItemVoidBackpackLink;
// ⚡ 添加资源磁化戒指导入
import com.moremod.item.ItemResourceMagnetRing;
// 🧭 添加探险者罗盘导入
import com.moremod.item.ItemExplorerCompass;
// ⭕ 因果闕帶（智能沉默）
import com.moremod.item.ItemCausalGateband;
// 🧠 添加行為分析晶片導入
import com.moremod.item.ItemBehaviorAnalysisChip;
// 👻 添加诅咒蔓延导入
import com.moremod.item.ItemAlchemistStone;
import com.moremod.item.ItemCurseSpread;
// 🌹 荆棘王冠之碎片（七咒联动）
import com.moremod.item.curse.ItemThornShard;
// 👁 虚无之眸（七咒联动）
import com.moremod.item.curse.ItemVoidGaze;
// 🦴 饕餮指骨（七咒联动）
import com.moremod.item.curse.ItemGluttonousPhalanx;
// 💎 怨念结晶（七咒联动）
import com.moremod.item.curse.ItemCrystallizedResentment;
// 🗡️ 添加澄月剑导入
import com.moremod.item.ItemSwordChengYue;
// 🌟 添加剑气测试器导入
import com.moremod.item.ItemSwordBeamTester;
// ⚔️ 添加锯刃剑导入
import com.moremod.item.ItemSawBladeSword;
// 🛡️ 添加勇者之剑导入
import com.moremod.item.ItemHeroSword;
// 💎 添加宝石系统导入
import com.moremod.item.ItemGem;
import com.moremod.item.ItemIdentifyScroll;
// 🧬 添加机械核心升级系统导入
import com.moremod.item.upgrades.ItemNeuralSynchronizer;
import com.moremod.item.ItemBioStabilizer;
import com.moremod.item.ItemTowel;
// 🧵 添加织布拆解器导入
import com.moremod.item.ItemFabricRemover;
// 📦 添加结构胶囊导入
import com.moremod.item.ItemStructureCapsule;
// ✨ 七圣遗物
import com.moremod.item.curse.ItemSacredRelic;
// 🎲 仪式道具
import com.moremod.item.ritual.ItemFateApple;
import com.moremod.item.ritual.ItemVoidEssence;
import com.moremod.item.ritual.ItemCursedMirror;
import com.moremod.item.ritual.ItemSoulFruit;
import com.moremod.item.ritual.ItemFakePlayerCore;

// ⛽ 能源系統物品
import com.moremod.item.energy.ItemOilProspector;
import com.moremod.item.energy.ItemOilBucket;
import com.moremod.item.energy.ItemPlantOilBucket;
import com.moremod.item.energy.ItemSpeedUpgrade;

// 📖 綜合指南書
import com.moremod.item.ItemModGuide;

// 🖨️ 打印系統
import com.moremod.printer.ItemPrintTemplate;
import com.moremod.printer.ItemBlankPrintTemplate;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = "moremod")
public final class ModItems {

    // —— Boss & 小怪材料（全部先实现为物品） ——
    public static Item FRGUARDIAN_STONE;       // 碎裂的守护石
    public static Item ANCIENT_CORE_FRAGMENT;  // 远古核心碎片
    public static Item RUNED_VOID_STONE;       // 虚空雕纹石（后续可升格为方块）
    public static Item RIFT_CRYSTAL;           // 裂隙水晶
    public static Item OTHERWORLDLY_FIBER;     // 异界纤维
    public static Item ETHEREAL_SHARD;         // 虚境碎片
    public static Item RENDING_CORE;           // 撕裂核心
    public static Item VOIDSCALE_SHARD;        // 虚空鳞片
    public static Item EYE_OF_THE_ABYSS;       // 深渊之眼
    public static Item VOID_ICHOR;             // 虚空之血（此版先做成瓶装材料）
    public static Item CURSED_INGOT;           // 诅咒钢锭
    public static Item SHADOW_FRAGMENT;        // 暗影残片
    public static Item GAZE_FRAGMENT;          // 凝视碎片
    public static Item TEAR_OF_STILLNESS;      // 静止之泪
    public static Item SPECTRAL_DUST;          // 幽影尘
    public static Item VOID_ECHO;              // 虚空残响
    public static Item PARASITIC_MASS;         // 寄生质块
    public static Item CORRUPTED_NUCLEUS;      // 腐蚀晶核
    public static Item UNFORMED_FIBER;         // 未成形的纤维（原有）
    public static Item UNFORMED_FABRIC;        // ★ 新增：尚未成为任何事物的布料（漆黑无光）

    // —— 织布机相关中间产物 ——
    public static Item DIMENSIONAL_WEAVER_CORE; // 维度织布机核心
    public static Item SPACETIME_FABRIC;        // 空间布料
    public static Item CHRONO_FABRIC;           // 时空布料
    public static Item ABYSSAL_FABRIC;          // 深渊布料
    public static Item VOID_SPINDLE;            // 虚空纺锤

    // —— 基础织布（可合成） ——
    public static Item RESILIENT_FIBER;         // 弹性纤维 - 减伤
    public static Item VITAL_THREAD;            // 生机丝线 - 生命回复
    public static Item LIGHT_WEAVE;             // 轻盈织物 - 速度/跳跃
    public static Item PREDATOR_CLOTH;          // 掠食者布料 - 攻击增强
    public static Item SIPHON_WRAP;             // 虹吸包裹 - 生命偷取

    // —— 织布工具 ——
    public static Item FABRIC_REMOVER;          // 织布拆解器

    // 🏪 自动交易机相关物品
    public static Item VILLAGER_CAPSULE;        // 村民胶囊

    // 🌌⚡ 机械核心饰品系列
    public static Item VOID_BACKPACK_LINK;      // 虚空背包链接
    public static Item RESOURCE_MAGNET_RING;    // 资源磁化戒指
    public static Item EXPLORER_COMPASS;        // 🧭 探险者罗盘
    public static Item CAUSAL_GATEBAND;         // ⭕ 因果闕帶（智能沉默）
    public static Item BEHAVIOR_ANALYSIS_CHIP;  // 🧠 行為分析晶片
    public static Item CURSE_SPREAD;            // 👻 诅咒蔓延
    public static Item ALCHEMIST_STONE;         // 🧪 炼药师的术石（七咒联动）
    public static Item THORN_SHARD;             // 🌹 荆棘王冠之碎片（七咒联动）
    public static Item VOID_GAZE;               // 👁 虚无之眸（七咒联动）
    public static Item GLUTTONOUS_PHALANX;      // 🦴 饕餮指骨（七咒联动）
    public static Item CRYSTALLIZED_RESENTMENT; // 💎 怨念结晶（七咒联动）
    public static Item NOOSE_OF_HANGED_KING;    // 🪢 缢王之索（七咒联动）
    public static Item SCRIPT_OF_FIFTH_ACT;     // 📜 第五幕剧本（七咒联动）

    // ✨ 七圣遗物（嵌入抵消七咒）
    public static Item SACRED_HEART;            // 圣光之心 - 抵消受伤加倍
    public static Item PEACE_EMBLEM;            // 和平徽章 - 抵消中立生物攻击
    public static Item GUARDIAN_SCALE;          // 守护鳞片 - 抵消护甲降低
    public static Item COURAGE_BLADE;           // 勇气之刃 - 抵消伤害降低
    public static Item FROST_DEW;               // 霜华之露 - 抵消永燃
    public static Item SOUL_ANCHOR;             // 灵魂锚点 - 抵消灵魂破碎
    public static Item SLUMBER_SACHET;          // 安眠香囊 - 抵消失眠症

    // 🎲 仪式道具（三阶祭坛特殊制品）
    public static Item FATE_APPLE;              // 命运苹果 - 重置附魔种子
    public static Item VOID_ESSENCE;            // 虚空精华 - 仪式催化剂
    public static Item CURSED_MIRROR;           // 诅咒之镜 - 复制仪式核心
    public static Item SOUL_FRUIT;              // 灵魂果实 - 强力临时增益
    public static Item FAKE_PLAYER_CORE;        // 假玩家核心 - 从玩家头颅仪式创建

    // 🗡️ 武器系列
    public static ItemSwordChengYue SWORD_CHENGYUE;      // 澄月 - 成长性终极武器
    public static Item SWORD_BEAM_TESTER;                // 🌟 剑气测试器 - 用于测试剑气渲染
    public static ItemSawBladeSword SAW_BLADE_SWORD;     // ⚔️ 锯刃剑 - GeckoLib动画武器
    public static ItemHeroSword HERO_SWORD;              // 🛡️ 勇者之剑 - GeckoLib动画武器

    // 💎 宝石系统
    public static ItemGem GEM;                      // 宝石（支持品质颜色）
    public static Item IDENTIFY_SCROLL;             // 鉴定卷轴

    // 🧬 机械核心升级与维护系统
    public static Item NEURAL_SYNCHRONIZER;     // 神经同步器
    public static Item BIO_STABILIZER;          // 生物稳定剂
    public static Item TOWEL;                   // 毛巾

    // 📦 结构胶囊系列
    public static ItemStructureCapsule STRUCTURE_CAPSULE_SMALL;   // 小型结构胶囊 3×3×3
    public static ItemStructureCapsule STRUCTURE_CAPSULE_MEDIUM;  // 中型结构胶囊 7×7×7
    public static ItemStructureCapsule STRUCTURE_CAPSULE_LARGE;   // 大型结构胶囊 15×15×15
    public static ItemStructureCapsule STRUCTURE_CAPSULE_HUGE;    // 巨型结构胶囊 31×31×31
    public static ItemStructureCapsule STRUCTURE_CAPSULE_MEGA;    // 超巨型结构胶囊 63×63×63

    // ⛽ 能源系統物品
    public static Item OIL_PROSPECTOR;       // 石油探測器
    public static Item CRUDE_OIL_BUCKET;     // 原油桶
    public static Item PLANT_OIL_BUCKET;     // 植物油桶
    public static Item SPEED_UPGRADE;        // 發電機增速插件

    // 📖 綜合指南書
    public static Item MOREMOD_GUIDE;        // MoreMod 綜合指南

    // 🖨️ 打印系統物品
    public static Item PRINT_TEMPLATE;       // 打印模版 (已定义)
    public static Item BLANK_PRINT_TEMPLATE; // 空白打印模版 (未定义，需CRT配置)
    public static Item CUSTOM_PRINT_TEMPLATE; // 自定义打印模版 (NBT存储配方)

    //新模块(屎山包装)



    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> e) {
        // 稀有度：COMMON/UNCOMMON/RARE；glows=true 表示自发光描边
        FRGUARDIAN_STONE      = reg(e, new ItemMaterial("frguardian_stone",      EnumRarity.UNCOMMON, false, "item.moremod.frguardian_stone.desc"));
        ANCIENT_CORE_FRAGMENT = reg(e, new ItemMaterial("ancient_core_fragment", EnumRarity.UNCOMMON, false, "item.moremod.ancient_core_fragment.desc"));
        RUNED_VOID_STONE      = reg(e, new ItemMaterial("runed_void_stone",      EnumRarity.RARE,     true,  "item.moremod.runed_void_stone.desc"));
        RIFT_CRYSTAL          = reg(e, new ItemMaterial("rift_crystal",          EnumRarity.UNCOMMON, true,  "item.moremod.rift_crystal.desc"));
        OTHERWORLDLY_FIBER    = reg(e, new ItemMaterial("otherworldly_fiber",    EnumRarity.COMMON,   false, "item.moremod.otherworldly_fiber.desc"));
        ETHEREAL_SHARD        = reg(e, new ItemMaterial("ethereal_shard",        EnumRarity.RARE,     true,  "item.moremod.ethereal_shard.desc"));
        RENDING_CORE          = reg(e, new ItemMaterial("rending_core",          EnumRarity.RARE,     true,  "item.moremod.rending_core.desc"));
        VOIDSCALE_SHARD       = reg(e, new ItemMaterial("voidscale_shard",       EnumRarity.UNCOMMON, true,  "item.moremod.voidscale_shard.desc"));
        EYE_OF_THE_ABYSS      = reg(e, new ItemMaterial("eye_of_the_abyss",      EnumRarity.RARE,     true,  "item.moremod.eye_of_the_abyss.desc"));
        VOID_ICHOR            = reg(e, new ItemMaterial("void_ichor",            EnumRarity.RARE,     true,  "item.moremod.void_ichor.desc"));
        CURSED_INGOT          = reg(e, new ItemMaterial("cursed_ingot",          EnumRarity.UNCOMMON, false, "item.moremod.cursed_ingot.desc"));
        SHADOW_FRAGMENT       = reg(e, new ItemMaterial("shadow_fragment",       EnumRarity.COMMON,   false, "item.moremod.shadow_fragment.desc"));
        GAZE_FRAGMENT         = reg(e, new ItemMaterial("gaze_fragment",         EnumRarity.UNCOMMON, true,  "item.moremod.gaze_fragment.desc"));
        TEAR_OF_STILLNESS     = reg(e, new ItemMaterial("tear_of_stillness",     EnumRarity.RARE,     true,  "item.moremod.tear_of_stillness.desc"));
        SPECTRAL_DUST         = reg(e, new ItemMaterial("spectral_dust",         EnumRarity.COMMON,   false, "item.moremod.spectral_dust.desc"));
        VOID_ECHO             = reg(e, new ItemMaterial("void_echo",             EnumRarity.UNCOMMON, true,  "item.moremod.void_echo.desc"));
        PARASITIC_MASS        = reg(e, new ItemMaterial("parasitic_mass",        EnumRarity.UNCOMMON, false, "item.moremod.parasitic_mass.desc"));
        CORRUPTED_NUCLEUS     = reg(e, new ItemMaterial("corrupted_nucleus",     EnumRarity.RARE,     true,  "item.moremod.corrupted_nucleus.desc"));
        UNFORMED_FIBER        = reg(e, new ItemMaterial("unformed_fiber",        EnumRarity.UNCOMMON, false, "item.moremod.unformed_fiber.desc"));

        // ★ 新增：尚未成为任何事物的布料（漆黑无光，不发光）
        UNFORMED_FABRIC       = reg(e, new ItemMaterial("unformed_fabric",       EnumRarity.UNCOMMON, false, "item.moremod.unformed_fabric.desc"));

        DIMENSIONAL_WEAVER_CORE = reg(e, new ItemMaterial("dimensional_weaver_core", EnumRarity.RARE,   true,  "item.moremod.dimensional_weaver_core.desc"));
        SPACETIME_FABRIC         = reg(e, new ItemMaterial("spacetime_fabric",       EnumRarity.UNCOMMON, false, "item.moremod.spacetime_fabric.desc"));
        CHRONO_FABRIC            = reg(e, new ItemMaterial("chrono_fabric",          EnumRarity.RARE,     true,  "item.moremod.chrono_fabric.desc"));
        ABYSSAL_FABRIC           = reg(e, new ItemMaterial("abyssal_fabric",         EnumRarity.RARE,     true,  "item.moremod.abyssal_fabric.desc"));
        VOID_SPINDLE             = reg(e, new ItemMaterial("void_spindle",           EnumRarity.RARE,     true,  "item.moremod.void_spindle.desc"));

        // 🧵 注册基础织布
        RESILIENT_FIBER = reg(e, new ItemBasicFabric("resilient_fiber", EnumRarity.UNCOMMON));
        VITAL_THREAD    = reg(e, new ItemBasicFabric("vital_thread",    EnumRarity.UNCOMMON));
        LIGHT_WEAVE     = reg(e, new ItemBasicFabric("light_weave",     EnumRarity.UNCOMMON));
        PREDATOR_CLOTH  = reg(e, new ItemBasicFabric("predator_cloth",  EnumRarity.UNCOMMON));
        SIPHON_WRAP     = reg(e, new ItemBasicFabric("siphon_wrap",     EnumRarity.UNCOMMON));
        System.out.println("[MoreMod] 🧵 基础织布已注册 (5种)");

        // 🧵 注册织布拆解器
        FABRIC_REMOVER = reg(e, new ItemFabricRemover());
        System.out.println("[MoreMod] 🧵 织布拆解器已注册");

        // 🏪 注册村民胶囊
        VILLAGER_CAPSULE = reg(e, new ItemVillagerCapsule());
        System.out.println("[MoreMod] 村民胶囊已注册");

        // 🌌⚡ 注册机械核心饰品
        VOID_BACKPACK_LINK = reg(e, new ItemVoidBackpackLink());
        System.out.println("[MoreMod] 虚空背包链接已注册");

        RESOURCE_MAGNET_RING = reg(e, new ItemResourceMagnetRing());
        System.out.println("[MoreMod] 资源磁化戒指已注册");

        // 🧭 注册探险者罗盘
        EXPLORER_COMPASS = reg(e, new ItemExplorerCompass());
        System.out.println("[MoreMod] 探险者罗盘已注册");

        // ⭕ 注册因果闕帶（智能沉默）
        CAUSAL_GATEBAND = reg(e, new ItemCausalGateband());
        System.out.println("[MoreMod] 因果闕帶已注册");

        // 🧠 註冊行為分析晶片
        BEHAVIOR_ANALYSIS_CHIP = reg(e, new ItemBehaviorAnalysisChip());
        System.out.println("[MoreMod] 行為分析晶片已註冊");

        // 👻 注册诅咒蔓延
        CURSE_SPREAD = reg(e, new ItemCurseSpread());
        System.out.println("[MoreMod] 👻 诅咒蔓延已注册");

        // 🧪 注册炼药师的术石
        ALCHEMIST_STONE = reg(e, new ItemAlchemistStone());
        System.out.println("[MoreMod] 🧪 炼药师的术石已注册");

        // 🌹 注册荆棘王冠之碎片
        THORN_SHARD = reg(e, new ItemThornShard());
        System.out.println("[MoreMod] 🌹 荆棘王冠之碎片已注册");

        // 👁 注册虚无之眸
        VOID_GAZE = reg(e, new ItemVoidGaze());
        System.out.println("[MoreMod] 👁 虚无之眸已注册");

        // 🦴 注册饕餮指骨
        GLUTTONOUS_PHALANX = reg(e, new ItemGluttonousPhalanx());
        System.out.println("[MoreMod] 🦴 饕餮指骨已注册");

        // 💎 注册怨念结晶
        CRYSTALLIZED_RESENTMENT = reg(e, new ItemCrystallizedResentment());
        System.out.println("[MoreMod] 💎 怨念结晶已注册");

        // 🪢 注册缢王之索
        NOOSE_OF_HANGED_KING = reg(e, new com.moremod.item.curse.ItemNooseOfHangedKing());
        System.out.println("[MoreMod] 🪢 缢王之索已注册");

        // 📜 注册第五幕剧本
        SCRIPT_OF_FIFTH_ACT = reg(e, new com.moremod.item.curse.ItemScriptOfFifthAct());
        System.out.println("[MoreMod] 📜 第五幕剧本已注册");

        // ✨ 注册七圣遗物
        SACRED_HEART = reg(e, new ItemSacredRelic(ItemSacredRelic.RelicType.SACRED_HEART));
        PEACE_EMBLEM = reg(e, new ItemSacredRelic(ItemSacredRelic.RelicType.PEACE_EMBLEM));
        GUARDIAN_SCALE = reg(e, new ItemSacredRelic(ItemSacredRelic.RelicType.GUARDIAN_SCALE));
        COURAGE_BLADE = reg(e, new ItemSacredRelic(ItemSacredRelic.RelicType.COURAGE_BLADE));
        FROST_DEW = reg(e, new ItemSacredRelic(ItemSacredRelic.RelicType.FROST_DEW));
        SOUL_ANCHOR = reg(e, new ItemSacredRelic(ItemSacredRelic.RelicType.SOUL_ANCHOR));
        SLUMBER_SACHET = reg(e, new ItemSacredRelic(ItemSacredRelic.RelicType.SLUMBER_SACHET));
        System.out.println("[MoreMod] ✨ 七圣遗物已注册 (7种)");

        // 🎲 注册仪式道具
        FATE_APPLE = reg(e, new ItemFateApple());
        VOID_ESSENCE = reg(e, new ItemVoidEssence());
        CURSED_MIRROR = reg(e, new ItemCursedMirror());
        SOUL_FRUIT = reg(e, new ItemSoulFruit());
        FAKE_PLAYER_CORE = reg(e, new ItemFakePlayerCore());
        System.out.println("[MoreMod] 🎲 仪式道具已注册 (5种)");

        // 🗡️ 注册澄月剑
        SWORD_CHENGYUE = (ItemSwordChengYue) reg(e, new ItemSwordChengYue());
        System.out.println("[MoreMod] ✨ 澄月剑已注册");

        // ⚔️ 注册锯刃剑
        SAW_BLADE_SWORD = (ItemSawBladeSword) reg(e, new ItemSawBladeSword(ToolMaterial.DIAMOND));
        System.out.println("[MoreMod] ⚔️ 锯刃剑已注册");

        // 🛡️ 注册勇者之剑
        HERO_SWORD = (ItemHeroSword) reg(e, new ItemHeroSword(ToolMaterial.DIAMOND));
        System.out.println("[MoreMod] 🛡️ 勇者之剑已注册");

        // 🌟 注册剑气测试器
        SWORD_BEAM_TESTER = reg(e, new ItemSwordBeamTester());
        System.out.println("[MoreMod] 🌟 剑气测试器已注册");

        // 💎 注册宝石系统
        GEM = (ItemGem) reg(e, new ItemGem());
        System.out.println("[MoreMod] 💎 宝石已注册");

        IDENTIFY_SCROLL = reg(e, new ItemIdentifyScroll());
        System.out.println("[MoreMod] 📜 鉴定卷轴已注册");

        // 🧬 注册机械核心升级与维护系统
        NEURAL_SYNCHRONIZER = reg(e, new ItemNeuralSynchronizer());
        System.out.println("[MoreMod] 🧬 神经同步器已注册");

        BIO_STABILIZER = reg(e, new ItemBioStabilizer());
        System.out.println("[MoreMod] 💉 生物稳定剂已注册");

        TOWEL = reg(e, new ItemTowel());
        System.out.println("[MoreMod] 🧴 毛巾已注册");

        // 📦 注册结构胶囊
        STRUCTURE_CAPSULE_SMALL = (ItemStructureCapsule) reg(e, new ItemStructureCapsule("structure_capsule_small", 3));
        STRUCTURE_CAPSULE_MEDIUM = (ItemStructureCapsule) reg(e, new ItemStructureCapsule("structure_capsule_medium", 7));
        STRUCTURE_CAPSULE_LARGE = (ItemStructureCapsule) reg(e, new ItemStructureCapsule("structure_capsule_large", 15));
        STRUCTURE_CAPSULE_HUGE = (ItemStructureCapsule) reg(e, new ItemStructureCapsule("structure_capsule_huge", 31));
        STRUCTURE_CAPSULE_MEGA = (ItemStructureCapsule) reg(e, new ItemStructureCapsule("structure_capsule_mega", 63));
        System.out.println("[MoreMod] 📦 结构胶囊已注册 (5种尺寸)");

        // ⛽ 注册能源系統物品
        OIL_PROSPECTOR = reg(e, new ItemOilProspector());
        System.out.println("[MoreMod] ⛽ 石油探測器已註冊");

        CRUDE_OIL_BUCKET = reg(e, new ItemOilBucket());
        System.out.println("[MoreMod] ⛽ 原油桶已註冊");

        PLANT_OIL_BUCKET = reg(e, new ItemPlantOilBucket());
        System.out.println("[MoreMod] ⛽ 植物油桶已註冊");

        SPEED_UPGRADE = reg(e, new ItemSpeedUpgrade());
        System.out.println("[MoreMod] ⚡ 發電機增速插件已註冊");

        // 📖 註冊綜合指南書
        MOREMOD_GUIDE = reg(e, new ItemModGuide());
        System.out.println("[MoreMod] 📖 MoreMod 綜合指南已註冊");

        // 🖨️ 註冊打印模版
        PRINT_TEMPLATE = reg(e, new ItemPrintTemplate());
        System.out.println("[MoreMod] 🖨️ 打印模版已註冊");

        // 🖨️ 註冊空白打印模版
        BLANK_PRINT_TEMPLATE = reg(e, new ItemBlankPrintTemplate());
        System.out.println("[MoreMod] 🖨️ 空白打印模版已註冊 (需CRT定義配方)");

        // 🖨️ 註冊自定义打印模版
        CUSTOM_PRINT_TEMPLATE = reg(e, new com.moremod.printer.ItemCustomPrintTemplate());
        System.out.println("[MoreMod] 🖨️ 自定义打印模版已註冊 (NBT存储配方)");

        //新模块系统
    }

    private static Item reg(RegistryEvent.Register<Item> e, Item item) {
        e.getRegistry().register(item);
        return item;
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onModelRegister(ModelRegistryEvent e) {
        // 统一使用 item/generated
        bindModel(UNFORMED_FIBER, "unformed_fiber");
        bindModel(UNFORMED_FABRIC, "unformed_fabric");
        bindModel(FRGUARDIAN_STONE,      "frguardian_stone");
        bindModel(ANCIENT_CORE_FRAGMENT, "ancient_core_fragment");
        bindModel(RUNED_VOID_STONE,      "runed_void_stone");
        bindModel(RIFT_CRYSTAL,          "rift_crystal");
        bindModel(OTHERWORLDLY_FIBER,    "otherworldly_fiber");
        bindModel(ETHEREAL_SHARD,        "ethereal_shard");
        bindModel(RENDING_CORE,          "rending_core");
        bindModel(VOIDSCALE_SHARD,       "voidscale_shard");
        bindModel(EYE_OF_THE_ABYSS,      "eye_of_the_abyss");
        bindModel(VOID_ICHOR,            "void_ichor");
        bindModel(CURSED_INGOT,          "cursed_ingot");
        bindModel(SHADOW_FRAGMENT,       "shadow_fragment");
        bindModel(GAZE_FRAGMENT,         "gaze_fragment");
        bindModel(TEAR_OF_STILLNESS,     "tear_of_stillness");
        bindModel(SPECTRAL_DUST,         "spectral_dust");
        bindModel(VOID_ECHO,             "void_echo");
        bindModel(PARASITIC_MASS,        "parasitic_mass");
        bindModel(CORRUPTED_NUCLEUS,     "corrupted_nucleus");

        bindModel(DIMENSIONAL_WEAVER_CORE, "dimensional_weaver_core");
        bindModel(SPACETIME_FABRIC,        "spacetime_fabric");
        bindModel(CHRONO_FABRIC,           "chrono_fabric");
        bindModel(ABYSSAL_FABRIC,          "abyssal_fabric");
        bindModel(VOID_SPINDLE,            "void_spindle");

        // 🧵 绑定基础织布模型
        bindModel(RESILIENT_FIBER, "resilient_fiber");
        bindModel(VITAL_THREAD,    "vital_thread");
        bindModel(LIGHT_WEAVE,     "light_weave");
        bindModel(PREDATOR_CLOTH,  "predator_cloth");
        bindModel(SIPHON_WRAP,     "siphon_wrap");
        System.out.println("[MoreMod] 🧵 基础织布模型已注册");

        // 🧵 绑定织布拆解器模型
        bindModel(FABRIC_REMOVER, "fabric_remover");
        System.out.println("[MoreMod] 🧵 织布拆解器模型已注册");

        // 🏪 绑定村民胶囊模型
        bindModel(VILLAGER_CAPSULE, "villager_capsule");
        System.out.println("[MoreMod] 村民胶囊模型已注册");

        // 🌌⚡ 绑定机械核心饰品模型
        bindModel(VOID_BACKPACK_LINK, "void_backpack_link");
        System.out.println("[MoreMod] 虚空背包链接模型已注册");

        bindModel(RESOURCE_MAGNET_RING, "resource_magnet_ring");
        System.out.println("[MoreMod] 资源磁化戒指模型已注册");

        // 🧭 绑定探险者罗盘模型
        bindModel(EXPLORER_COMPASS, "explorer_compass");
        System.out.println("[MoreMod] 探险者罗盘模型已注册");

        // ⭕ 绑定因果闕帶模型
        bindModel(CAUSAL_GATEBAND, "causal_gateband");
        System.out.println("[MoreMod] 因果闕帶模型已注册");

        // 🧠 綁定行為分析晶片模型
        bindModel(BEHAVIOR_ANALYSIS_CHIP, "behavior_analysis_chip");
        System.out.println("[MoreMod] 行為分析晶片模型已註冊");

        // 👻 绑定诅咒蔓延模型
        bindModel(CURSE_SPREAD, "curse_spread");
        System.out.println("[MoreMod] 👻 诅咒蔓延模型已注册");

        // 🧪 绑定炼药师的术石模型
        bindModel(ALCHEMIST_STONE, "alchemist_stone");
        System.out.println("[MoreMod] 🧪 炼药师的术石模型已注册");

        // 🌹 绑定荆棘王冠之碎片模型
        bindModel(THORN_SHARD, "thorn_shard");
        System.out.println("[MoreMod] 🌹 荆棘王冠之碎片模型已注册");

        // 👁 绑定虚无之眸模型
        bindModel(VOID_GAZE, "void_gaze");
        System.out.println("[MoreMod] 👁 虚无之眸模型已注册");

        // 🦴 绑定饕餮指骨模型
        bindModel(GLUTTONOUS_PHALANX, "gluttonous_phalanx");
        System.out.println("[MoreMod] 🦴 饕餮指骨模型已注册");

        // 💎 绑定怨念结晶模型
        bindModel(CRYSTALLIZED_RESENTMENT, "crystallized_resentment");
        System.out.println("[MoreMod] 💎 怨念结晶模型已注册");

        // 🪢 绑定缢王之索模型
        bindModel(NOOSE_OF_HANGED_KING, "noose_of_hanged_king");
        System.out.println("[MoreMod] 🪢 缢王之索模型已注册");

        // 📜 绑定第五幕剧本模型
        bindModel(SCRIPT_OF_FIFTH_ACT, "script_of_fifth_act");
        System.out.println("[MoreMod] 📜 第五幕剧本模型已注册");

        // ✨ 绑定七圣遗物模型
        bindModel(SACRED_HEART, "sacred_heart");
        bindModel(PEACE_EMBLEM, "peace_emblem");
        bindModel(GUARDIAN_SCALE, "guardian_scale");
        bindModel(COURAGE_BLADE, "courage_blade");
        bindModel(FROST_DEW, "frost_dew");
        bindModel(SOUL_ANCHOR, "soul_anchor");
        bindModel(SLUMBER_SACHET, "slumber_sachet");
        System.out.println("[MoreMod] ✨ 七圣遗物模型已注册");

        // 🎲 绑定仪式道具模型
        bindModel(FATE_APPLE, "fate_apple");
        bindModel(VOID_ESSENCE, "void_essence");
        bindModel(CURSED_MIRROR, "cursed_mirror");
        bindModel(SOUL_FRUIT, "soul_fruit");
        bindModel(FAKE_PLAYER_CORE, "fake_player_core");
        System.out.println("[MoreMod] 🎲 仪式道具模型已注册");

        // 🗡️ 绑定澄月剑模型
        bindModel(SWORD_CHENGYUE, "sword_chengyue");
        System.out.println("[MoreMod] ✨ 澄月剑模型已注册");

        // ⚔️ 初始化锯刃剑TEISR渲染器
        System.out.println("[MoreMod] ⚔️ 锯刃剑TEISR渲染器已初始化");

        // 🛡️ 初始化勇者之剑TEISR渲染器
        System.out.println("[MoreMod] 🛡️ 勇者之剑TEISR渲染器已初始化");

        // 🌟 绑定剑气测试器模型
        bindModel(SWORD_BEAM_TESTER, "sword_beam_tester");
        System.out.println("[MoreMod] 🌟 剑气测试器模型已注册");

        // 💎 绑定宝石系统模型
        bindModel(GEM, "gem");
        System.out.println("[MoreMod] 💎 宝石模型已注册");

        bindModel(IDENTIFY_SCROLL, "identify_scroll");
        System.out.println("[MoreMod] 📜 鉴定卷轴模型已注册");

        // 🧬 绑定机械核心升级与维护系统模型
        bindModel(NEURAL_SYNCHRONIZER, "neural_synchronizer");
        System.out.println("[MoreMod] 🧬 神经同步器模型已注册");

        bindModel(BIO_STABILIZER, "bio_stabilizer");
        System.out.println("[MoreMod] 💉 生物稳定剂模型已注册");

        bindModel(TOWEL, "towel");
        System.out.println("[MoreMod] 🧴 毛巾模型已注册");

        // 📦 绑定结构胶囊模型 (需要为空/已存储两种状态都注册)
        bindCapsuleModels(STRUCTURE_CAPSULE_SMALL, "structure_capsule_small");
        bindCapsuleModels(STRUCTURE_CAPSULE_MEDIUM, "structure_capsule_medium");
        bindCapsuleModels(STRUCTURE_CAPSULE_LARGE, "structure_capsule_large");
        bindCapsuleModels(STRUCTURE_CAPSULE_HUGE, "structure_capsule_huge");
        bindCapsuleModels(STRUCTURE_CAPSULE_MEGA, "structure_capsule_mega");
        System.out.println("[MoreMod] 📦 结构胶囊模型已注册 (5种尺寸，含空/存储状态)");

        // ⛽ 綁定能源系統物品模型
        bindModel(OIL_PROSPECTOR, "oil_prospector");
        bindModel(CRUDE_OIL_BUCKET, "crude_oil_bucket");
        bindModel(PLANT_OIL_BUCKET, "plant_oil_bucket");
        bindModel(SPEED_UPGRADE, "speed_upgrade");
        System.out.println("[MoreMod] ⛽ 能源系統物品模型已註冊");

        // 📖 綁定指南書模型
        bindModel(MOREMOD_GUIDE, "moremod_guide");
        System.out.println("[MoreMod] 📖 綜合指南書模型已註冊");

        // 🖨️ 綁定打印模版模型
        bindModel(PRINT_TEMPLATE, "print_template");
        System.out.println("[MoreMod] 🖨️ 打印模版模型已註冊");

        // 🖨️ 綁定空白打印模版模型
        bindModel(BLANK_PRINT_TEMPLATE, "blank_print_template");
        System.out.println("[MoreMod] 🖨️ 空白打印模版模型已註冊");

        // 🖨️ 綁定自定义打印模版模型
        bindModel(CUSTOM_PRINT_TEMPLATE, "custom_print_template");
        System.out.println("[MoreMod] 🖨️ 自定义打印模版模型已註冊");
    }

    @SideOnly(Side.CLIENT)
    private static void bindModel(Item item, String path) {
        if (item != null) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation("moremod:" + path, "inventory"));
        }
    }

    /**
     * 为结构胶囊绑定模型 - 空状态和存储状态使用相同模型
     */
    @SideOnly(Side.CLIENT)
    private static void bindCapsuleModels(Item item, String path) {
        if (item != null) {
            ModelResourceLocation modelLoc = new ModelResourceLocation("moremod:" + path, "inventory");
            // 状态0: 空胶囊
            ModelLoader.setCustomModelResourceLocation(item, 0, modelLoc);
            // 状态1: 已存储结构
            ModelLoader.setCustomModelResourceLocation(item, 1, modelLoc);
        }
    }
}