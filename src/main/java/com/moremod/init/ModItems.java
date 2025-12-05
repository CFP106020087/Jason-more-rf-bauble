package com.moremod.init;

import com.moremod.item.ItemMaterial;
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
import com.moremod.item.ItemCurseSpread;
// 🌹 添加荆棘王冠之碎片导入
import com.moremod.item.curse.ItemThornShard;
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

    // 🏪 自动交易机相关物品
    public static Item VILLAGER_CAPSULE;        // 村民胶囊

    // 🌌⚡ 机械核心饰品系列
    public static Item VOID_BACKPACK_LINK;      // 虚空背包链接
    public static Item RESOURCE_MAGNET_RING;    // 资源磁化戒指
    public static Item EXPLORER_COMPASS;        // 🧭 探险者罗盘
    public static Item CAUSAL_GATEBAND;         // ⭕ 因果闕帶（智能沉默）
    public static Item BEHAVIOR_ANALYSIS_CHIP;  // 🧠 行為分析晶片
    public static Item CURSE_SPREAD;            // 👻 诅咒蔓延
    public static Item THORN_SHARD;             // 🌹 荆棘王冠之碎片（七咒联动）

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

        // 🌹 注册荆棘王冠之碎片
        THORN_SHARD = reg(e, new ItemThornShard());
        System.out.println("[MoreMod] 🌹 荆棘王冠之碎片已注册");

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

        // 🌹 绑定荆棘王冠之碎片模型
        bindModel(THORN_SHARD, "thorn_shard");
        System.out.println("[MoreMod] 🌹 荆棘王冠之碎片模型已注册");

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
    }

    @SideOnly(Side.CLIENT)
    private static void bindModel(Item item, String path) {
        if (item != null) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation("moremod:" + path, "inventory"));
        }
    }
}