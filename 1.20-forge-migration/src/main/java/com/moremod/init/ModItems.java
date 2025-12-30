package com.moremod.init;

import com.moremod.MoreMod;
import com.moremod.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品注册表 - 1.20 Forge版本
 *
 * 使用DeferredRegister替代1.12的GameRegistry
 *
 * 1.12 -> 1.20 API变更:
 * - GameRegistry.register -> DeferredRegister
 * - Item.Properties需要显式设置
 * - BlockItem需要单独注册
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MoreMod.MOD_ID);

    // ========== 方块物品 ==========

    public static final RegistryObject<Item> UPGRADE_CHAMBER_CORE = ITEMS.register("upgrade_chamber_core",
            () -> new BlockItem(ModBlocks.UPGRADE_CHAMBER_CORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> RITUAL_CORE = ITEMS.register("ritual_core",
            () -> new BlockItem(ModBlocks.RITUAL_CORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> WISDOM_FOUNTAIN_CORE = ITEMS.register("wisdom_fountain_core",
            () -> new BlockItem(ModBlocks.WISDOM_FOUNTAIN_CORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> QUANTUM_QUARRY = ITEMS.register("quantum_quarry",
            () -> new BlockItem(ModBlocks.QUANTUM_QUARRY.get(), new Item.Properties()));

    public static final RegistryObject<Item> QUARRY_ACTUATOR = ITEMS.register("quarry_actuator",
            () -> new BlockItem(ModBlocks.QUARRY_ACTUATOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> CHARGING_STATION = ITEMS.register("charging_station",
            () -> new BlockItem(ModBlocks.CHARGING_STATION.get(), new Item.Properties()));

    public static final RegistryObject<Item> TEMPORAL_ACCELERATOR = ITEMS.register("temporal_accelerator",
            () -> new BlockItem(ModBlocks.TEMPORAL_ACCELERATOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> PEDESTAL = ITEMS.register("pedestal",
            () -> new BlockItem(ModBlocks.PEDESTAL.get(), new Item.Properties()));

    public static final RegistryObject<Item> GUARDIAN_STONE = ITEMS.register("guardian_stone",
            () -> new BlockItem(ModBlocks.GUARDIAN_STONE.get(), new Item.Properties()));

    public static final RegistryObject<Item> RUNED_VOID_STONE = ITEMS.register("runed_void_stone",
            () -> new BlockItem(ModBlocks.RUNED_VOID_STONE.get(), new Item.Properties()));

    // ========== 更多方块物品 ==========

    public static final RegistryObject<Item> BIO_GENERATOR = ITEMS.register("bio_generator",
            () -> new BlockItem(ModBlocks.BIO_GENERATOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> OIL_GENERATOR = ITEMS.register("oil_generator",
            () -> new BlockItem(ModBlocks.OIL_GENERATOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> ANIMAL_FEEDER = ITEMS.register("animal_feeder",
            () -> new BlockItem(ModBlocks.ANIMAL_FEEDER.get(), new Item.Properties()));

    public static final RegistryObject<Item> FISHING_NET = ITEMS.register("fishing_net",
            () -> new BlockItem(ModBlocks.FISHING_NET.get(), new Item.Properties()));

    public static final RegistryObject<Item> MEGA_CHEST = ITEMS.register("mega_chest",
            () -> new BlockItem(ModBlocks.MEGA_CHEST.get(), new Item.Properties()));

    public static final RegistryObject<Item> TRANSFER_STATION = ITEMS.register("transfer_station",
            () -> new BlockItem(ModBlocks.TRANSFER_STATION.get(), new Item.Properties()));

    public static final RegistryObject<Item> ANCIENT_CORE = ITEMS.register("ancient_core",
            () -> new BlockItem(ModBlocks.ANCIENT_CORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> SPACETIME_SHARD_ORE = ITEMS.register("spacetime_shard_ore",
            () -> new BlockItem(ModBlocks.SPACETIME_SHARD_ORE.get(), new Item.Properties()));

    // ========== 机械核心系统物品 ==========

    // 机械核心 - 核心饰品 (TODO: Phase 4 Curios集成)
    public static final RegistryObject<Item> MECHANICAL_CORE = ITEMS.register("mechanical_core",
            MechanicalCoreItem::new);

    // 便携电池 (TODO: Phase 4 Curios集成)
    public static final RegistryObject<Item> BATTERY_BAUBLE = ITEMS.register("battery_bauble",
            BatteryBaubleItem::new);

    // 时空碎片
    public static final RegistryObject<Item> SPACETIME_SHARD = ITEMS.register("spacetime_shard",
            SpacetimeShardItem::new);

    // ========== 升级模块 ==========

    public static final RegistryObject<Item> UPGRADE_YELLOW_SHIELD = ITEMS.register("upgrade_yellow_shield",
            () -> new UpgradeModuleItem(UpgradeModuleItem.ModuleType.YELLOW_SHIELD));

    public static final RegistryObject<Item> UPGRADE_HEALTH_REGEN = ITEMS.register("upgrade_health_regen",
            () -> new UpgradeModuleItem(UpgradeModuleItem.ModuleType.HEALTH_REGEN));

    public static final RegistryObject<Item> UPGRADE_FIRE_EXTINGUISH = ITEMS.register("upgrade_fire_extinguish",
            () -> new UpgradeModuleItem(UpgradeModuleItem.ModuleType.FIRE_EXTINGUISH));

    public static final RegistryObject<Item> UPGRADE_MOVEMENT_SPEED = ITEMS.register("upgrade_movement_speed",
            () -> new UpgradeModuleItem(UpgradeModuleItem.ModuleType.MOVEMENT_SPEED));

    public static final RegistryObject<Item> UPGRADE_WATER_BREATHING = ITEMS.register("upgrade_water_breathing",
            () -> new UpgradeModuleItem(UpgradeModuleItem.ModuleType.WATER_BREATHING));

    public static final RegistryObject<Item> UPGRADE_DAMAGE_BOOST = ITEMS.register("upgrade_damage_boost",
            () -> new UpgradeModuleItem(UpgradeModuleItem.ModuleType.DAMAGE_BOOST));

    public static final RegistryObject<Item> UPGRADE_ATTACK_SPEED = ITEMS.register("upgrade_attack_speed",
            () -> new UpgradeModuleItem(UpgradeModuleItem.ModuleType.ATTACK_SPEED));

    public static final RegistryObject<Item> UPGRADE_CRITICAL_STRIKE = ITEMS.register("upgrade_critical_strike",
            () -> new UpgradeModuleItem(UpgradeModuleItem.ModuleType.CRITICAL_STRIKE));

    public static final RegistryObject<Item> UPGRADE_NIGHT_VISION = ITEMS.register("upgrade_night_vision",
            () -> new UpgradeModuleItem(UpgradeModuleItem.ModuleType.NIGHT_VISION));

    public static final RegistryObject<Item> UPGRADE_ORE_VISION = ITEMS.register("upgrade_ore_vision",
            () -> new UpgradeModuleItem(UpgradeModuleItem.ModuleType.ORE_VISION));

    public static final RegistryObject<Item> UPGRADE_ITEM_MAGNET = ITEMS.register("upgrade_item_magnet",
            () -> new UpgradeModuleItem(UpgradeModuleItem.ModuleType.ITEM_MAGNET));

    public static final RegistryObject<Item> UPGRADE_SOLAR_GENERATOR = ITEMS.register("upgrade_solar_generator",
            () -> new UpgradeModuleItem(UpgradeModuleItem.ModuleType.SOLAR_GENERATOR));

    public static final RegistryObject<Item> UPGRADE_KINETIC_GENERATOR = ITEMS.register("upgrade_kinetic_generator",
            () -> new UpgradeModuleItem(UpgradeModuleItem.ModuleType.KINETIC_GENERATOR));

    // ========== 材料和组件 ==========

    public static final RegistryObject<Item> MECHANICAL_GEAR = ITEMS.register("mechanical_gear",
            () -> new MaterialItem(MaterialItem.MaterialType.MECHANICAL_GEAR));

    public static final RegistryObject<Item> ENERGY_CORE = ITEMS.register("energy_core",
            () -> new MaterialItem(MaterialItem.MaterialType.ENERGY_CORE));

    public static final RegistryObject<Item> CIRCUIT_BOARD = ITEMS.register("circuit_board",
            () -> new MaterialItem(MaterialItem.MaterialType.CIRCUIT_BOARD));

    public static final RegistryObject<Item> ADVANCED_CIRCUIT = ITEMS.register("advanced_circuit",
            () -> new MaterialItem(MaterialItem.MaterialType.ADVANCED_CIRCUIT));

    public static final RegistryObject<Item> UPGRADE_BASE = ITEMS.register("upgrade_base",
            () -> new MaterialItem(MaterialItem.MaterialType.UPGRADE_BASE));

    public static final RegistryObject<Item> NEURAL_PROCESSOR = ITEMS.register("neural_processor",
            () -> new MaterialItem(MaterialItem.MaterialType.NEURAL_PROCESSOR));

    public static final RegistryObject<Item> QUANTUM_CHIP = ITEMS.register("quantum_chip",
            () -> new MaterialItem(MaterialItem.MaterialType.QUANTUM_CHIP));

    public static final RegistryObject<Item> VOID_ESSENCE = ITEMS.register("void_essence",
            () -> new MaterialItem(MaterialItem.MaterialType.VOID_ESSENCE));

    public static final RegistryObject<Item> TEMPORAL_DUST = ITEMS.register("temporal_dust",
            () -> new MaterialItem(MaterialItem.MaterialType.TEMPORAL_DUST));

    public static final RegistryObject<Item> DIMENSIONAL_FABRIC = ITEMS.register("dimensional_fabric",
            () -> new MaterialItem(MaterialItem.MaterialType.DIMENSIONAL_FABRIC));

    public static final RegistryObject<Item> SOLAR_CELL = ITEMS.register("solar_cell",
            () -> new MaterialItem(MaterialItem.MaterialType.SOLAR_CELL));

    public static final RegistryObject<Item> KINETIC_COIL = ITEMS.register("kinetic_coil",
            () -> new MaterialItem(MaterialItem.MaterialType.KINETIC_COIL));

    public static final RegistryObject<Item> FUSION_CORE = ITEMS.register("fusion_core",
            () -> new MaterialItem(MaterialItem.MaterialType.FUSION_CORE));
}
