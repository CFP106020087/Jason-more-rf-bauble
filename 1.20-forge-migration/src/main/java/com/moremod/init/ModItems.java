package com.moremod.init;

import com.moremod.MoreMod;
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

    // ========== 机械核心系统物品 ==========

    // TODO: 从1.12移植以下物品:
    // - ItemMechanicalCore -> MechanicalCoreItem (Curios兼容)
    // - ItemMechanicalCoreExtended
    // - ItemCausalGateband -> CausalGatebandItem (Curios兼容)
    // - 各种升级模块 (ItemUpgradeComponent)

    // ========== 材料和组件 ==========

    // TODO: 移植材料物品
    // - 机械零件
    // - 能量核心
    // - 升级芯片
    // 等...
}
