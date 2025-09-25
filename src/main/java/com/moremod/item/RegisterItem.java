package com.moremod.item;

import com.moremod.item.upgrades.ItemUpgradeComponent;
import com.moremod.item.upgrades.UpgradeItemsExtended;
import com.moremod.items.BloodyThirstMask;
import com.moremod.item.battery.*;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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

    // ===== 添加扩展升级组件的静态字段 =====
    // 生存类升级
    public static Item YELLOW_SHIELD_MODULE;
    public static Item NANO_REPAIR_SYSTEM;
    public static Item METABOLIC_REGULATOR;
    public static Item REACTIVE_ARMOR;
    public static Item FIRE_SUPPRESSION;

    // 辅助类升级
    public static Item WATERPROOF_MODULE;
    public static Item ORE_SCANNER;
    public static Item SERVO_MOTORS;
    public static Item OPTICAL_CAMOUFLAGE;
    public static Item EXP_COLLECTOR;
    public static Item DIMENSIONALANCOR;
    // 战斗类升级
    public static Item STRENGTH_AMPLIFIER;
    public static Item REFLEX_ENHANCER;
    public static Item SWEEP_MODULE;
    public static Item PURSUIT_SYSTEM;

    // 能源类升级
    public static Item KINETIC_DYNAMO;
    public static Item SOLAR_PANEL;
    public static Item VOID_RESONATOR;
    public static Item COMBAT_HARVESTER;

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
        SAGEBOOK          = newSafe(ItemSageBook::new, "sage_book");
        LIGHTING_BOLT          = newSafe(ItemArcGaze::new, "arc_gaze");
        TIME_BLOCK             = newSafe(ItemBlockTemporal::new ,"time_block");
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
        // SimpleDifficulty（按需）
        initSimpleDifficultyItems();

        // 喷气背包
        JETPACK_T1       = newSafe(() -> new ItemJetpackBauble("jetpack_t1", 100000, 60, 0.15, -0.08, 0.12), "jetpack_t1");
        JETPACK_T2       = newSafe(() -> new ItemJetpackBauble("jetpack_t2", 300000, 100, 0.25, -0.12, 0.20), "jetpack_t2");
        JETPACK_T3       = newSafe(() -> new ItemJetpackBauble("jetpack_t3", 600000, 160, 0.35, -0.16, 0.28), "jetpack_t3");
        CREATIVE_JETPACK = newSafe(() -> new ItemCreativeJetpackBauble("creative_jetpack"), "creative_jetpack");
        JETPACKS.clear();
        addIfNotNull(JETPACKS, JETPACK_T1, JETPACK_T2, JETPACK_T3, CREATIVE_JETPACK);

        // ===== 初始化扩展升级组件 =====
        // 生存类
        YELLOW_SHIELD_MODULE = UpgradeItemsExtended.YELLOW_SHIELD_MODULE;
        NANO_REPAIR_SYSTEM = UpgradeItemsExtended.NANO_REPAIR_SYSTEM;
        METABOLIC_REGULATOR = UpgradeItemsExtended.METABOLIC_REGULATOR;
        REACTIVE_ARMOR = UpgradeItemsExtended.REACTIVE_ARMOR;
        FIRE_SUPPRESSION = UpgradeItemsExtended.FIRE_SUPPRESSION;

        // 辅助类
        WATERPROOF_MODULE = UpgradeItemsExtended.WATERPROOF_MODULE;
        ORE_SCANNER = UpgradeItemsExtended.ORE_SCANNER;
        SERVO_MOTORS = UpgradeItemsExtended.SERVO_MOTORS;
        OPTICAL_CAMOUFLAGE = UpgradeItemsExtended.OPTICAL_CAMOUFLAGE;
        EXP_COLLECTOR = UpgradeItemsExtended.EXP_COLLECTOR;

        // 战斗类
        STRENGTH_AMPLIFIER = UpgradeItemsExtended.STRENGTH_AMPLIFIER;
        REFLEX_ENHANCER = UpgradeItemsExtended.REFLEX_ENHANCER;
        SWEEP_MODULE = UpgradeItemsExtended.SWEEP_MODULE;
        PURSUIT_SYSTEM = UpgradeItemsExtended.PURSUIT_SYSTEM;

        // 能源类
        KINETIC_DYNAMO = UpgradeItemsExtended.KINETIC_DYNAMO;
        SOLAR_PANEL = UpgradeItemsExtended.SOLAR_PANEL;
        VOID_RESONATOR = UpgradeItemsExtended.VOID_RESONATOR;
        COMBAT_HARVESTER = UpgradeItemsExtended.COMBAT_HARVESTER;

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
                ANTIKYTHERA_GEAR, TOGGLE_RENDER,DIMENSIONALRIPPER,DIMENSIONALANCOR,SPACETIME_SHARD,ANCHORKEY,ENCHANT_RING_T1,ENCHANT_RING_T2,ENCHANT_RING_T3,ENCHANT_RING_ULTIMATE,TIME_BLOCK,LIGHTING_BOLT,SAGEBOOK
        );
        if (SIMPLE_DIFFICULTY_LOADED) {
            addIfNotNull(itemsToRegister, TEMPERATURE_REGULATOR, THIRST_PROCESSOR);
        }

        event.getRegistry().registerAll(itemsToRegister.toArray(new Item[0]));
        if (MECHANICAL_CORE != null) {
            event.getRegistry().register(MECHANICAL_CORE);
        }

        // 升级组件
        try {
            com.moremod.item.UpgradeItems.printUpgradeInfo();
            event.getRegistry().registerAll(com.moremod.item.UpgradeItems.getAllUpgrades());
        } catch (Throwable e) {
            System.err.println("[moremod] ❌ 升级组件注册失败: " + e.getMessage());
        }

        // ===== 注册扩展升级组件 =====
        try {
            System.out.println("[moremod] 正在注册扩展升级组件...");
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