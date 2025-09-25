package com.moremod.init;

import com.moremod.item.ItemMaterial;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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
    }

    private static Item reg(RegistryEvent.Register<Item> e, Item item) {
        e.getRegistry().register(item);
        return item;
    }

    @SubscribeEvent
    public static void onModelRegister(ModelRegistryEvent e) {
        // 统一使用 item/generated
        bindModel(UNFORMED_FIBER, "unformed_fiber");
        bindModel(UNFORMED_FABRIC, "unformed_fabric"); // ★ 新增绑定

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
    }

    private static void bindModel(Item item, String path) {
        if (item != null) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation("moremod:" + path, "inventory"));
        }
    }
}
