package com.moremod.item;

import com.moremod.item.upgrades.ItemUpgradeComponent;
import com.moremod.item.upgrades.UpgradeItemsExtended;
import com.moremod.items.BloodyThirstMask;
import com.moremod.item.battery.*;
import com.moremod.item.broken.ItemBrokenHand;
import com.moremod.item.broken.ItemBrokenHeart;
import com.moremod.item.broken.ItemBrokenArm;
import com.moremod.item.broken.ItemBrokenShackles;
import com.moremod.item.broken.ItemBrokenProjection;
import com.moremod.item.broken.ItemBrokenTerminus;
import com.moremod.system.ascension.BrokenGodItems;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.moremod.item.UpgradeItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

@Mod.EventBusSubscriber(modid = "moremod")
public class RegisterItem {

    // —— 仅保留常量，避免在 <clinit> 期间做任何 new 操作 ——
    private static final boolean SIMPLE_DIFFICULTY_LOADED = Loader.isModLoaded("simpledifficulty");
    public static Item SAGEBOOK;
    // —— 所有物品字段改为"延迟实例化"，初始化为 null ——
    public static Item LIGHTING_BOLT;
    public static Item TIME_BLOCK;
    public static Item IMMORTAL_AMULET;
    public static Item RIFTGUN;
    public static Item ENCHANT_RING_T1;
    public static Item ENCHANT_RING_T2;
    public static Item ENCHANT_RING_T3;
    public static Item ENCHANT_RING_ULTIMATE;
    public static Item UPGRADE_SELECTOR;
    public static Item MECHANICAL_EXOSKELETON;
    public static Item LAW_SWORD;
    public static Item ENERGY_RING;
    public static Item SPEAR;
    public static Item CLEANSING_BAUBLE;
    public static Item ENERGY_BARRIER;
    public static Item ENERGY_SWORD;
    public static Item BASIC_ENERGY_BARRIER;
    public static Item CRUDE_ENERGY_BARRIER;
    public static Item ADV_ENERGY_BARRIER;
    public static Item CIRCULATION_SYSTEM;
    public static Item TEMPORAL_RIFT;
    public static Item ANCHORKEY;
    // 电池
    public static Item BATTERY_BAUBLE;
    public static Item CREATIVE_BATTERY_BAUBLE;
    public static Item BATTERY_BASIC;
    public static Item BATTERY_ADVANCED;
    public static Item BATTERY_ELITE;
    public static Item BATTERY_ULTIMATE;
    public static Item BATTERY_QUANTUM;
    public static Item SPACETIME_SHARD;
    // 其他饰品/材料
    public static Item MECHANICAL_HEART;
    public static Item TEMPORAL_HEART;
    public static Item BLOODY_THIRST_MASK;
    public static Item MERCHANT_PERSUADER;
    public static Item VILLAGER_PROFESSION_TOOL;
    public static ItemMechanicalCore MECHANICAL_CORE;
    public static Item ITEM_RENDER;
    public static Item TOGGLE_RENDER;
    public static Item ANTIKYTHERA_GEAR;
    public static Item COPPER_WISHBONE;
    public static Item DIMENSIONALRIPPER;
    // SimpleDifficulty 相关（延迟创建）
    private static Item TEMPERATURE_REGULATOR;
    private static Item THIRST_PROCESSOR;

    public static Item getTemperatureRegulator() { return TEMPERATURE_REGULATOR; }
    public static Item getThirstProcessor()      { return THIRST_PROCESSOR; }

    // 喷气背包
    public static final List<Item> JETPACKS = new ArrayList<>();
    public static Item JETPACK_T1;
    public static Item JETPACK_T2;
    public static Item JETPACK_T3;
    public static Item CREATIVE_JETPACK;

    // ===== 基础升级组件（分级版本） =====
    // 能量容量（10级）
    public static Item ENERGY_CAPACITY_LV1;
    public static Item ENERGY_CAPACITY_LV2;
    public static Item ENERGY_CAPACITY_LV3;
    public static Item ENERGY_CAPACITY_LV4;
    public static Item ENERGY_CAPACITY_LV5;
    public static Item ENERGY_CAPACITY_LV6;
    public static Item ENERGY_CAPACITY_LV7;
    public static Item ENERGY_CAPACITY_LV8;
    public static Item ENERGY_CAPACITY_LV9;
    public static Item ENERGY_CAPACITY_LV10;

    // 能量效率（5级）
    public static Item ENERGY_EFFICIENCY_LV1;
    public static Item ENERGY_EFFICIENCY_LV2;
    public static Item ENERGY_EFFICIENCY_LV3;
    public static Item ENERGY_EFFICIENCY_LV4;
    public static Item ENERGY_EFFICIENCY_LV5;

    // 护甲强化（5级）
    public static Item ARMOR_ENHANCEMENT_LV1;
    public static Item ARMOR_ENHANCEMENT_LV2;
    public static Item ARMOR_ENHANCEMENT_LV3;
    public static Item ARMOR_ENHANCEMENT_LV4;
    public static Item ARMOR_ENHANCEMENT_LV5;

    // 速度提升（3级）
    public static Item SPEED_BOOST_LV1;
    public static Item SPEED_BOOST_LV2;
    public static Item SPEED_BOOST_LV3;

    // 飞行模块（3个独立等级）
    public static Item FLIGHT_MODULE_BASIC;
    public static Item FLIGHT_MODULE_ADVANCED;
    public static Item FLIGHT_MODULE_ULTIMATE;

    // 温度调节（5级）
    public static Item TEMPERATURE_CONTROL_LV1;
    public static Item TEMPERATURE_CONTROL_LV2;
    public static Item TEMPERATURE_CONTROL_LV3;
    public static Item TEMPERATURE_CONTROL_LV4;
    public static Item TEMPERATURE_CONTROL_LV5;

    // 特殊套装
    public static Item OMNIPOTENT_PACKAGE;

    // ===== 扩展升级组件（分级版本） =====
    // 生存类升级
    public static Item YELLOW_SHIELD_LV1;
    public static Item YELLOW_SHIELD_LV2;
    public static Item YELLOW_SHIELD_LV3;

    public static Item HEALTH_REGEN_LV1;
    public static Item HEALTH_REGEN_LV2;
    public static Item HEALTH_REGEN_LV3;

    public static Item HUNGER_THIRST_LV1;
    public static Item HUNGER_THIRST_LV2;
    public static Item HUNGER_THIRST_LV3;

    public static Item THORNS_LV1;
    public static Item THORNS_LV2;
    public static Item THORNS_LV3;

    public static Item FIRE_EXTINGUISH_LV1;
    public static Item FIRE_EXTINGUISH_LV2;
    public static Item FIRE_EXTINGUISH_LV3;

    public static Item HOLY_WATER;

    // 辅助类升级
    public static Item WATERPROOF_MODULE_BASIC;
    public static Item WATERPROOF_MODULE_ADVANCED;
    public static Item WATERPROOF_MODULE_DEEP_SEA;

    public static Item ORE_VISION_LV1;
    public static Item ORE_VISION_LV2;
    public static Item ORE_VISION_LV3;

    public static Item MOVEMENT_SPEED_LV1;
    public static Item MOVEMENT_SPEED_LV2;
    public static Item MOVEMENT_SPEED_LV3;

    public static Item STEALTH_LV1;
    public static Item STEALTH_LV2;
    public static Item STEALTH_LV3;

    public static Item EXP_AMPLIFIER_LV1;
    public static Item EXP_AMPLIFIER_LV2;
    public static Item EXP_AMPLIFIER_LV3;

    public static Item DIMENSIONALANCOR;

    // 破碎之神终局饰品
    public static Item BROKEN_HAND;
    public static Item BROKEN_HEART;
    public static Item BROKEN_ARM;
    public static Item BROKEN_SHACKLES;
    public static Item BROKEN_PROJECTION;
    public static Item BROKEN_TERMINUS;

    // 战斗类升级
    public static Item DAMAGE_BOOST_LV1;
    public static Item DAMAGE_BOOST_LV2;
    public static Item DAMAGE_BOOST_LV3;
    public static Item DAMAGE_BOOST_LV4;
    public static Item DAMAGE_BOOST_LV5;

    public static Item ATTACK_SPEED_LV1;
    public static Item ATTACK_SPEED_LV2;
    public static Item ATTACK_SPEED_LV3;

    public static Item RANGE_EXTENSION_LV1;
    public static Item RANGE_EXTENSION_LV2;
    public static Item RANGE_EXTENSION_LV3;

    public static Item PURSUIT_LV1;
    public static Item PURSUIT_LV2;
    public static Item PURSUIT_LV3;

    // 能源类升级
    public static Item KINETIC_GENERATOR_LV1;
    public static Item KINETIC_GENERATOR_LV2;
    public static Item KINETIC_GENERATOR_LV3;

    public static Item SOLAR_GENERATOR_LV1;
    public static Item SOLAR_GENERATOR_LV2;
    public static Item SOLAR_GENERATOR_LV3;

    public static Item VOID_ENERGY_LV1;
    public static Item VOID_ENERGY_LV2;
    public static Item VOID_ENERGY_LV3;

    public static Item COMBAT_CHARGER_LV1;
    public static Item COMBAT_CHARGER_LV2;
    public static Item COMBAT_CHARGER_LV3;

    // 特殊套装
    public static Item SURVIVAL_PACKAGE;
    public static Item COMBAT_PACKAGE;

    // —— 安全 new：避免因客户端类导致服务器崩溃 ——
    private static <T extends Item> T newSafe(Supplier<T> sup, String id) {
        try {
            return sup.get();
        } catch (Throwable t) {
            System.err.println("[moremod] ❌ 创建物品失败(" + id + "): " + t.getClass().getSimpleName() + ": " + t.getMessage());
            t.printStackTrace();
            return null;
        }
    }

    private static void initSimpleDifficultyItems() {
        if (!SIMPLE_DIFFICULTY_LOADED) return;
        if (TEMPERATURE_REGULATOR == null)
            TEMPERATURE_REGULATOR = newSafe(ItemTemperatureRegulator::new, "temperature_regulator");
        if (THIRST_PROCESSOR == null)
            THIRST_PROCESSOR = newSafe(ItemThirstProcessor::new, "thirst_processor");
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        // —— 在这里（而不是类初始化）再去实例化所有物品 ——
        HOLY_WATER        = newSafe(ItemHolyWater::new, "holy_water");
        SAGEBOOK          = newSafe(ItemSageBook::new, "sage_book");
        LIGHTING_BOLT          = newSafe(ItemArcGaze::new, "arc_gaze");
        IMMORTAL_AMULET        = newSafe(ImmortalAmulet::new, "immortal_amulet");
        RIFTGUN                = newSafe(ItemRiftLaser::new, "itemRiftLaser");
        SPACETIME_SHARD        = newSafe(ItemSpacetimeShard::new, "space_time_shard");
        UPGRADE_SELECTOR       = newSafe(ItemUpgradeSelector::new, "upgrade_selector");
        MECHANICAL_EXOSKELETON = newSafe(ItemMechanicalExoskeleton::new, "mechanical_exoskeleton");
        LAW_SWORD              = newSafe(() -> new ItemLawSword("law_sword"), "law_sword");
        ENERGY_RING            = newSafe(ItemEnergyRing::new, "energy_ring");
        SPEAR                  = newSafe(ItemSpearBauble::new, "spear_bauble");
        CLEANSING_BAUBLE       = newSafe(ItemCleansingBauble::new, "cleansing_bauble");
        ENERGY_BARRIER         = newSafe(ItemEnergyBarrier::new, "energy_barrier");
        ENERGY_SWORD           = newSafe(ItemEnergySword::new, "energy_sword");
        BASIC_ENERGY_BARRIER   = newSafe(ItemBasicEnergyBarrier::new, "basic_energy_barrier");
        CRUDE_ENERGY_BARRIER   = newSafe(ItemCrudeEnergyBarrier::new, "crude_energy_barrier");
        ADV_ENERGY_BARRIER     = newSafe(ItemadvEnergyBarrier::new, "adv_energy_barrier");
        CIRCULATION_SYSTEM     = newSafe(ItemExternalCirculationSystem::new, "external_circulation_system");
        TEMPORAL_RIFT          = newSafe(ItemTemporalRiftGlove::new, "temporal_rift_glove");
        DIMENSIONALRIPPER      = newSafe(ItemDimensionalRipper::new, "dimensionalripper");
        DIMENSIONALANCOR       = newSafe(ItemDimensionalAnchor::new, "dimensional_anchor");
        ANCHORKEY              = newSafe(ItemDimensionalKey::new, "dimensional_key");
        // 电池
        BATTERY_BAUBLE         = newSafe(ItemBatteryBauble::new, "battery_bauble");
        CREATIVE_BATTERY_BAUBLE= newSafe(ItemCreativeBatteryBauble::new, "creative_battery_bauble");
        BATTERY_BASIC          = newSafe(ItemBatteryBasic::new, "battery_basic");
        BATTERY_ADVANCED       = newSafe(ItemBatteryAdvanced::new, "battery_advanced");
        BATTERY_ELITE          = newSafe(ItemBatteryElite::new, "battery_elite");
        BATTERY_ULTIMATE       = newSafe(ItemBatteryUltimate::new, "battery_ultimate");
        BATTERY_QUANTUM        = newSafe(ItemBatteryQuantum::new, "battery_quantum");

        // 其他饰品/材料
        MECHANICAL_HEART       = newSafe(() -> new ItemMechanicalHeart("mechanical_heart"), "mechanical_heart");
        TEMPORAL_HEART         = newSafe(ItemTemporalHeart::new, "temporal_heart");
        BLOODY_THIRST_MASK     = newSafe(BloodyThirstMask::new, "bloody_thirst_mask");
        MERCHANT_PERSUADER     = newSafe(MerchantPersuader::new, "merchant_persuader");
        VILLAGER_PROFESSION_TOOL = newSafe(VillagerProfessionTool::new, "villager_profession_tool");
        MECHANICAL_CORE        = newSafe(ItemMechanicalCore::new, "mechanical_core");
        ANTIKYTHERA_GEAR       = newSafe(ItemAntikytheraGear::new, "antikythera_gear");
        COPPER_WISHBONE        = newSafe(ItemCopperWishbone::new, "copper_wishbone");
        ENCHANT_RING_T1 = newSafe(() -> new EnchantmentBoostBauble("enchant_ring_t1", 1), "enchant_ring_t1");
        ENCHANT_RING_T2 = newSafe(() -> new EnchantmentBoostBauble("enchant_ring_t2", 3), "enchant_ring_t2");
        ENCHANT_RING_T3 = newSafe(() -> new EnchantmentBoostBauble("enchant_ring_t3", 5), "enchant_ring_t3");
        ENCHANT_RING_ULTIMATE = newSafe(() -> new EnchantmentBoostBauble("enchant_ring_ultimate", 10), "enchant_ring_ultimate");

        // 破碎之神终局饰品
        BROKEN_HAND = newSafe(ItemBrokenHand::new, "broken_hand");
        BROKEN_HEART = newSafe(ItemBrokenHeart::new, "broken_heart");
        BROKEN_ARM = newSafe(ItemBrokenArm::new, "broken_arm");
        BROKEN_SHACKLES = newSafe(ItemBrokenShackles::new, "broken_shackles");
        BROKEN_PROJECTION = newSafe(ItemBrokenProjection::new, "broken_projection");
        BROKEN_TERMINUS = newSafe(ItemBrokenTerminus::new, "broken_terminus");
        // 同步到 BrokenGodItems
        BrokenGodItems.BROKEN_HAND = (ItemBrokenHand) BROKEN_HAND;
        BrokenGodItems.BROKEN_HEART = (ItemBrokenHeart) BROKEN_HEART;
        BrokenGodItems.BROKEN_ARM = (ItemBrokenArm) BROKEN_ARM;
        BrokenGodItems.BROKEN_SHACKLES = (ItemBrokenShackles) BROKEN_SHACKLES;
        BrokenGodItems.BROKEN_PROJECTION = (ItemBrokenProjection) BROKEN_PROJECTION;
        BrokenGodItems.BROKEN_TERMINUS = (ItemBrokenTerminus) BROKEN_TERMINUS;

        // SimpleDifficulty（按需）
        initSimpleDifficultyItems();

        // 喷气背包
        JETPACK_T1       = newSafe(() -> new ItemJetpackBauble("jetpack_t1", 100000, 60, 0.15, -0.08, 0.12), "jetpack_t1");
        JETPACK_T2       = newSafe(() -> new ItemJetpackBauble("jetpack_t2", 300000, 100, 0.25, -0.12, 0.20), "jetpack_t2");
        JETPACK_T3       = newSafe(() -> new ItemJetpackBauble("jetpack_t3", 600000, 160, 0.35, -0.16, 0.28), "jetpack_t3");
        CREATIVE_JETPACK = newSafe(() -> new ItemCreativeJetpackBauble("creative_jetpack"), "creative_jetpack");
        JETPACKS.clear();
        addIfNotNull(JETPACKS, JETPACK_T1, JETPACK_T2, JETPACK_T3, CREATIVE_JETPACK);

        // ===== 初始化基础升级组件（分级版本） =====
        // 能量容量（10级）
        ENERGY_CAPACITY_LV1 = UpgradeItems.ENERGY_CAPACITY_LV1;
        ENERGY_CAPACITY_LV2 = UpgradeItems.ENERGY_CAPACITY_LV2;
        ENERGY_CAPACITY_LV3 = UpgradeItems.ENERGY_CAPACITY_LV3;
        ENERGY_CAPACITY_LV4 = UpgradeItems.ENERGY_CAPACITY_LV4;
        ENERGY_CAPACITY_LV5 = UpgradeItems.ENERGY_CAPACITY_LV5;
        ENERGY_CAPACITY_LV6 = UpgradeItems.ENERGY_CAPACITY_LV6;
        ENERGY_CAPACITY_LV7 = UpgradeItems.ENERGY_CAPACITY_LV7;
        ENERGY_CAPACITY_LV8 = UpgradeItems.ENERGY_CAPACITY_LV8;
        ENERGY_CAPACITY_LV9 = UpgradeItems.ENERGY_CAPACITY_LV9;
        ENERGY_CAPACITY_LV10 = UpgradeItems.ENERGY_CAPACITY_LV10;

        // 能量效率（5级）
        ENERGY_EFFICIENCY_LV1 = UpgradeItems.ENERGY_EFFICIENCY_LV1;
        ENERGY_EFFICIENCY_LV2 = UpgradeItems.ENERGY_EFFICIENCY_LV2;
        ENERGY_EFFICIENCY_LV3 = UpgradeItems.ENERGY_EFFICIENCY_LV3;
        ENERGY_EFFICIENCY_LV4 = UpgradeItems.ENERGY_EFFICIENCY_LV4;
        ENERGY_EFFICIENCY_LV5 = UpgradeItems.ENERGY_EFFICIENCY_LV5;

        // 护甲强化（5级）
        ARMOR_ENHANCEMENT_LV1 = UpgradeItems.ARMOR_ENHANCEMENT_LV1;
        ARMOR_ENHANCEMENT_LV2 = UpgradeItems.ARMOR_ENHANCEMENT_LV2;
        ARMOR_ENHANCEMENT_LV3 = UpgradeItems.ARMOR_ENHANCEMENT_LV3;
        ARMOR_ENHANCEMENT_LV4 = UpgradeItems.ARMOR_ENHANCEMENT_LV4;
        ARMOR_ENHANCEMENT_LV5 = UpgradeItems.ARMOR_ENHANCEMENT_LV5;

        // 速度提升（3级）


        // 飞行模块（3个独立等级）
        FLIGHT_MODULE_BASIC = UpgradeItems.FLIGHT_MODULE_BASIC;
        FLIGHT_MODULE_ADVANCED = UpgradeItems.FLIGHT_MODULE_ADVANCED;
        FLIGHT_MODULE_ULTIMATE = UpgradeItems.FLIGHT_MODULE_ULTIMATE;

        // 温度调节（5级）
        TEMPERATURE_CONTROL_LV1 = UpgradeItems.TEMPERATURE_CONTROL_LV1;
        TEMPERATURE_CONTROL_LV2 = UpgradeItems.TEMPERATURE_CONTROL_LV2;
        TEMPERATURE_CONTROL_LV3 = UpgradeItems.TEMPERATURE_CONTROL_LV3;
        TEMPERATURE_CONTROL_LV4 = UpgradeItems.TEMPERATURE_CONTROL_LV4;
        TEMPERATURE_CONTROL_LV5 = UpgradeItems.TEMPERATURE_CONTROL_LV5;

        // 特殊套装
        OMNIPOTENT_PACKAGE = UpgradeItems.OMNIPOTENT_PACKAGE;

        // ===== 初始化扩展升级组件（分级版本） =====
        // 生存类（3级）
        YELLOW_SHIELD_LV1 = UpgradeItemsExtended.YELLOW_SHIELD_LV1;
        YELLOW_SHIELD_LV2 = UpgradeItemsExtended.YELLOW_SHIELD_LV2;
        YELLOW_SHIELD_LV3 = UpgradeItemsExtended.YELLOW_SHIELD_LV3;

        HEALTH_REGEN_LV1 = UpgradeItemsExtended.HEALTH_REGEN_LV1;
        HEALTH_REGEN_LV2 = UpgradeItemsExtended.HEALTH_REGEN_LV2;
        HEALTH_REGEN_LV3 = UpgradeItemsExtended.HEALTH_REGEN_LV3;

        HUNGER_THIRST_LV1 = UpgradeItemsExtended.HUNGER_THIRST_LV1;
        HUNGER_THIRST_LV2 = UpgradeItemsExtended.HUNGER_THIRST_LV2;
        HUNGER_THIRST_LV3 = UpgradeItemsExtended.HUNGER_THIRST_LV3;

        THORNS_LV1 = UpgradeItemsExtended.THORNS_LV1;
        THORNS_LV2 = UpgradeItemsExtended.THORNS_LV2;
        THORNS_LV3 = UpgradeItemsExtended.THORNS_LV3;

        FIRE_EXTINGUISH_LV1 = UpgradeItemsExtended.FIRE_EXTINGUISH_LV1;
        FIRE_EXTINGUISH_LV2 = UpgradeItemsExtended.FIRE_EXTINGUISH_LV2;
        FIRE_EXTINGUISH_LV3 = UpgradeItemsExtended.FIRE_EXTINGUISH_LV3;

        // 辅助类
        WATERPROOF_MODULE_BASIC = UpgradeItemsExtended.WATERPROOF_MODULE_BASIC;
        WATERPROOF_MODULE_ADVANCED = UpgradeItemsExtended.WATERPROOF_MODULE_ADVANCED;
        WATERPROOF_MODULE_DEEP_SEA = UpgradeItemsExtended.WATERPROOF_MODULE_DEEP_SEA;

        ORE_VISION_LV1 = UpgradeItemsExtended.ORE_VISION_LV1;
        ORE_VISION_LV2 = UpgradeItemsExtended.ORE_VISION_LV2;
        ORE_VISION_LV3 = UpgradeItemsExtended.ORE_VISION_LV3;

        MOVEMENT_SPEED_LV1 = UpgradeItemsExtended.MOVEMENT_SPEED_LV1;
        MOVEMENT_SPEED_LV2 = UpgradeItemsExtended.MOVEMENT_SPEED_LV2;
        MOVEMENT_SPEED_LV3 = UpgradeItemsExtended.MOVEMENT_SPEED_LV3;

        STEALTH_LV1 = UpgradeItemsExtended.STEALTH_LV1;
        STEALTH_LV2 = UpgradeItemsExtended.STEALTH_LV2;
        STEALTH_LV3 = UpgradeItemsExtended.STEALTH_LV3;

        EXP_AMPLIFIER_LV1 = UpgradeItemsExtended.EXP_AMPLIFIER_LV1;
        EXP_AMPLIFIER_LV2 = UpgradeItemsExtended.EXP_AMPLIFIER_LV2;
        EXP_AMPLIFIER_LV3 = UpgradeItemsExtended.EXP_AMPLIFIER_LV3;

        // 战斗类
        DAMAGE_BOOST_LV1 = UpgradeItemsExtended.DAMAGE_BOOST_LV1;
        DAMAGE_BOOST_LV2 = UpgradeItemsExtended.DAMAGE_BOOST_LV2;
        DAMAGE_BOOST_LV3 = UpgradeItemsExtended.DAMAGE_BOOST_LV3;
        DAMAGE_BOOST_LV4 = UpgradeItemsExtended.DAMAGE_BOOST_LV4;
        DAMAGE_BOOST_LV5 = UpgradeItemsExtended.DAMAGE_BOOST_LV5;

        ATTACK_SPEED_LV1 = UpgradeItemsExtended.ATTACK_SPEED_LV1;
        ATTACK_SPEED_LV2 = UpgradeItemsExtended.ATTACK_SPEED_LV2;
        ATTACK_SPEED_LV3 = UpgradeItemsExtended.ATTACK_SPEED_LV3;

        RANGE_EXTENSION_LV1 = UpgradeItemsExtended.RANGE_EXTENSION_LV1;
        RANGE_EXTENSION_LV2 = UpgradeItemsExtended.RANGE_EXTENSION_LV2;
        RANGE_EXTENSION_LV3 = UpgradeItemsExtended.RANGE_EXTENSION_LV3;

        PURSUIT_LV1 = UpgradeItemsExtended.PURSUIT_LV1;
        PURSUIT_LV2 = UpgradeItemsExtended.PURSUIT_LV2;
        PURSUIT_LV3 = UpgradeItemsExtended.PURSUIT_LV3;

        // 能源类
        KINETIC_GENERATOR_LV1 = UpgradeItemsExtended.KINETIC_GENERATOR_LV1;
        KINETIC_GENERATOR_LV2 = UpgradeItemsExtended.KINETIC_GENERATOR_LV2;
        KINETIC_GENERATOR_LV3 = UpgradeItemsExtended.KINETIC_GENERATOR_LV3;

        SOLAR_GENERATOR_LV1 = UpgradeItemsExtended.SOLAR_GENERATOR_LV1;
        SOLAR_GENERATOR_LV2 = UpgradeItemsExtended.SOLAR_GENERATOR_LV2;
        SOLAR_GENERATOR_LV3 = UpgradeItemsExtended.SOLAR_GENERATOR_LV3;

        VOID_ENERGY_LV1 = UpgradeItemsExtended.VOID_ENERGY_LV1;
        VOID_ENERGY_LV2 = UpgradeItemsExtended.VOID_ENERGY_LV2;
        VOID_ENERGY_LV3 = UpgradeItemsExtended.VOID_ENERGY_LV3;

        COMBAT_CHARGER_LV1 = UpgradeItemsExtended.COMBAT_CHARGER_LV1;
        COMBAT_CHARGER_LV2 = UpgradeItemsExtended.COMBAT_CHARGER_LV2;
        COMBAT_CHARGER_LV3 = UpgradeItemsExtended.COMBAT_CHARGER_LV3;

        // 特殊套装
        SURVIVAL_PACKAGE = UpgradeItemsExtended.SURVIVAL_PACKAGE;
        COMBAT_PACKAGE = UpgradeItemsExtended.COMBAT_PACKAGE;

        // —— 收集并注册（跳过为 null 的项） ——
        List<Item> itemsToRegister = new ArrayList<>();
        addIfNotNull(itemsToRegister,
                TEMPORAL_RIFT, CIRCULATION_SYSTEM, MECHANICAL_EXOSKELETON, UPGRADE_SELECTOR, COPPER_WISHBONE,
                ITEM_RENDER, LAW_SWORD, ENERGY_RING, SPEAR, CLEANSING_BAUBLE, ENERGY_BARRIER, ENERGY_SWORD,
                BASIC_ENERGY_BARRIER, CRUDE_ENERGY_BARRIER, ADV_ENERGY_BARRIER,
                BATTERY_BAUBLE, CREATIVE_BATTERY_BAUBLE, BATTERY_BASIC, BATTERY_ADVANCED, BATTERY_ELITE,
                BATTERY_ULTIMATE, BATTERY_QUANTUM,RIFTGUN,
                MECHANICAL_HEART, TEMPORAL_HEART, BLOODY_THIRST_MASK, MERCHANT_PERSUADER, VILLAGER_PROFESSION_TOOL,
                ANTIKYTHERA_GEAR, TOGGLE_RENDER,DIMENSIONALRIPPER,DIMENSIONALANCOR,SPACETIME_SHARD,ANCHORKEY,
                ENCHANT_RING_T1,ENCHANT_RING_T2,ENCHANT_RING_T3,ENCHANT_RING_ULTIMATE,TIME_BLOCK,LIGHTING_BOLT,
                SAGEBOOK,HOLY_WATER,
                BROKEN_HAND, BROKEN_HEART, BROKEN_ARM, BROKEN_SHACKLES, BROKEN_PROJECTION, BROKEN_TERMINUS
        );
        if (SIMPLE_DIFFICULTY_LOADED) {
            addIfNotNull(itemsToRegister, TEMPERATURE_REGULATOR, THIRST_PROCESSOR);
        }

        event.getRegistry().registerAll(itemsToRegister.toArray(new Item[0]));
        if (MECHANICAL_CORE != null) {
            event.getRegistry().register(MECHANICAL_CORE);
        }

        // ===== 注册基础升级组件（分级版本） =====
        try {
            System.out.println("[moremod] 正在注册基础升级组件（分级版本）...");
            ItemUpgradeComponent[] baseUpgrades = UpgradeItems.getAllUpgrades();
            if (baseUpgrades != null && baseUpgrades.length > 0) {
                event.getRegistry().registerAll(baseUpgrades);
                System.out.println("[moremod] ✓ 成功注册 " + baseUpgrades.length + " 个基础升级组件");

                // 打印详细信息（可选）
                UpgradeItems.printUpgradeInfo();
            } else {
                System.err.println("[moremod] ⚠ 没有找到基础升级组件");
            }
        } catch (Throwable e) {
            System.err.println("[moremod] ❌ 基础升级组件注册失败: " + e.getMessage());
            e.printStackTrace();
        }

        // ===== 注册扩展升级组件（分级版本） =====
        try {
            System.out.println("[moremod] 正在注册扩展升级组件（分级版本）...");
            ItemUpgradeComponent[] extendedUpgrades = UpgradeItemsExtended.getAllExtendedUpgrades();
            if (extendedUpgrades != null && extendedUpgrades.length > 0) {
                event.getRegistry().registerAll(extendedUpgrades);
                System.out.println("[moremod] ✓ 成功注册 " + extendedUpgrades.length + " 个扩展升级组件");
            } else {
                System.err.println("[moremod] ⚠ 没有找到扩展升级组件");
            }
        } catch (Throwable e) {
            System.err.println("[moremod] ❌ 扩展升级组件注册失败: " + e.getMessage());
            e.printStackTrace();
        }

        // 喷气背包
        if (!JETPACKS.isEmpty()) {
            event.getRegistry().registerAll(JETPACKS.toArray(new Item[0]));
        }

        // OreDictionary（判空）
        try {
            if (ANTIKYTHERA_GEAR != null) OreDictionary.registerOre("gearAntikythera", ANTIKYTHERA_GEAR);
            if (BATTERY_BASIC != null)     OreDictionary.registerOre("batteryBasic",   BATTERY_BASIC);
            if (BATTERY_ADVANCED != null)  OreDictionary.registerOre("batteryAdvanced",BATTERY_ADVANCED);
            if (BATTERY_ELITE != null)     OreDictionary.registerOre("batteryElite",   BATTERY_ELITE);
            if (BATTERY_ULTIMATE != null)  OreDictionary.registerOre("batteryUltimate",BATTERY_ULTIMATE);
            if (BATTERY_QUANTUM != null)   OreDictionary.registerOre("batteryQuantum", BATTERY_QUANTUM);
        } catch (Throwable t) {
            System.err.println("[moremod] ❌ OreDictionary 注册失败: " + t.getMessage());
        }
    }

    @SafeVarargs
    private static <T> void addIfNotNull(List<T> list, T... arr) {
        for (T t : arr) if (t != null) list.add(t);
    }
}
