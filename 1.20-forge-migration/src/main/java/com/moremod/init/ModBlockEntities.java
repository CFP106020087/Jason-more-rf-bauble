package com.moremod.init;

import com.moremod.MoreMod;
import com.moremod.block.entity.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块实体注册表 - 1.20 Forge版本
 *
 * 1.12的TileEntity在1.20中称为BlockEntity
 * 使用BlockEntityTicker替代ITickable接口
 *
 * 1.12 -> 1.20 API变更:
 * - TileEntity -> BlockEntity
 * - GameRegistry.registerTileEntity -> DeferredRegister + BlockEntityType.Builder
 * - ITickable.update() -> BlockEntityTicker lambda
 */
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MoreMod.MOD_ID);

    // ========== 机械核心系统方块实体 ==========

    // 升级艙核心
    public static final RegistryObject<BlockEntityType<UpgradeChamberCoreBlockEntity>> UPGRADE_CHAMBER_CORE =
            BLOCK_ENTITIES.register("upgrade_chamber_core",
                    () -> BlockEntityType.Builder.of(UpgradeChamberCoreBlockEntity::new,
                            ModBlocks.UPGRADE_CHAMBER_CORE.get()).build(null));

    // 祭坛核心
    public static final RegistryObject<BlockEntityType<RitualCoreBlockEntity>> RITUAL_CORE =
            BLOCK_ENTITIES.register("ritual_core",
                    () -> BlockEntityType.Builder.of(RitualCoreBlockEntity::new,
                            ModBlocks.RITUAL_CORE.get()).build(null));

    // 智慧之泉
    public static final RegistryObject<BlockEntityType<WisdomFountainBlockEntity>> WISDOM_FOUNTAIN =
            BLOCK_ENTITIES.register("wisdom_fountain",
                    () -> BlockEntityType.Builder.of(WisdomFountainBlockEntity::new,
                            ModBlocks.WISDOM_FOUNTAIN_CORE.get()).build(null));

    // 量子采石场
    public static final RegistryObject<BlockEntityType<QuantumQuarryBlockEntity>> QUANTUM_QUARRY =
            BLOCK_ENTITIES.register("quantum_quarry",
                    () -> BlockEntityType.Builder.of(QuantumQuarryBlockEntity::new,
                            ModBlocks.QUANTUM_QUARRY.get()).build(null));

    // 充电站
    public static final RegistryObject<BlockEntityType<ChargingStationBlockEntity>> CHARGING_STATION =
            BLOCK_ENTITIES.register("charging_station",
                    () -> BlockEntityType.Builder.of(ChargingStationBlockEntity::new,
                            ModBlocks.CHARGING_STATION.get()).build(null));

    // 时间加速器
    public static final RegistryObject<BlockEntityType<TemporalAcceleratorBlockEntity>> TEMPORAL_ACCELERATOR =
            BLOCK_ENTITIES.register("temporal_accelerator",
                    () -> BlockEntityType.Builder.of(TemporalAcceleratorBlockEntity::new,
                            ModBlocks.TEMPORAL_ACCELERATOR.get()).build(null));

    // 展示台
    public static final RegistryObject<BlockEntityType<PedestalBlockEntity>> PEDESTAL =
            BLOCK_ENTITIES.register("pedestal",
                    () -> BlockEntityType.Builder.of(PedestalBlockEntity::new,
                            ModBlocks.PEDESTAL.get()).build(null));

    // ========== 发电机 ==========

    // 生物质发电机
    public static final RegistryObject<BlockEntityType<BioGeneratorBlockEntity>> BIO_GENERATOR =
            BLOCK_ENTITIES.register("bio_generator",
                    () -> BlockEntityType.Builder.of(BioGeneratorBlockEntity::new,
                            ModBlocks.BIO_GENERATOR.get()).build(null));

    // 石油发电机
    public static final RegistryObject<BlockEntityType<OilGeneratorBlockEntity>> OIL_GENERATOR =
            BLOCK_ENTITIES.register("oil_generator",
                    () -> BlockEntityType.Builder.of(OilGeneratorBlockEntity::new,
                            ModBlocks.OIL_GENERATOR.get()).build(null));

    // ========== 自动化方块 ==========

    // 动物喂食器
    public static final RegistryObject<BlockEntityType<AnimalFeederBlockEntity>> ANIMAL_FEEDER =
            BLOCK_ENTITIES.register("animal_feeder",
                    () -> BlockEntityType.Builder.of(AnimalFeederBlockEntity::new,
                            ModBlocks.ANIMAL_FEEDER.get()).build(null));

    // 渔网
    public static final RegistryObject<BlockEntityType<FishingNetBlockEntity>> FISHING_NET =
            BLOCK_ENTITIES.register("fishing_net",
                    () -> BlockEntityType.Builder.of(FishingNetBlockEntity::new,
                            ModBlocks.FISHING_NET.get()).build(null));

    // 超大箱子
    public static final RegistryObject<BlockEntityType<MegaChestBlockEntity>> MEGA_CHEST =
            BLOCK_ENTITIES.register("mega_chest",
                    () -> BlockEntityType.Builder.of(MegaChestBlockEntity::new,
                            ModBlocks.MEGA_CHEST.get()).build(null));

    // 转移台
    public static final RegistryObject<BlockEntityType<TransferStationBlockEntity>> TRANSFER_STATION =
            BLOCK_ENTITIES.register("transfer_station",
                    () -> BlockEntityType.Builder.of(TransferStationBlockEntity::new,
                            ModBlocks.TRANSFER_STATION.get()).build(null));

    // 血液发电机
    public static final RegistryObject<BlockEntityType<BloodGeneratorBlockEntity>> BLOOD_GENERATOR =
            BLOCK_ENTITIES.register("blood_generator",
                    () -> BlockEntityType.Builder.of(BloodGeneratorBlockEntity::new,
                            ModBlocks.BLOOD_GENERATOR.get()).build(null));

    // 堆肥桶
    public static final RegistryObject<BlockEntityType<CompostBinBlockEntity>> COMPOST_BIN =
            BLOCK_ENTITIES.register("compost_bin",
                    () -> BlockEntityType.Builder.of(CompostBinBlockEntity::new,
                            ModBlocks.COMPOST_BIN.get()).build(null));

    // 植物油压榨机
    public static final RegistryObject<BlockEntityType<PlantOilPressBlockEntity>> PLANT_OIL_PRESS =
            BLOCK_ENTITIES.register("plant_oil_press",
                    () -> BlockEntityType.Builder.of(PlantOilPressBlockEntity::new,
                            ModBlocks.PLANT_OIL_PRESS.get()).build(null));

    // 时间控制器
    public static final RegistryObject<BlockEntityType<TimeControllerBlockEntity>> TIME_CONTROLLER =
            BLOCK_ENTITIES.register("time_controller",
                    () -> BlockEntityType.Builder.of(TimeControllerBlockEntity::new,
                            ModBlocks.TIME_CONTROLLER.get()).build(null));

    // 交易站
    public static final RegistryObject<BlockEntityType<TradingStationBlockEntity>> TRADING_STATION =
            BLOCK_ENTITIES.register("trading_station",
                    () -> BlockEntityType.Builder.of(TradingStationBlockEntity::new,
                            ModBlocks.TRADING_STATION.get()).build(null));

    // 装瓶机
    public static final RegistryObject<BlockEntityType<BottlingMachineBlockEntity>> BOTTLING_MACHINE =
            BLOCK_ENTITIES.register("bottling_machine",
                    () -> BlockEntityType.Builder.of(BottlingMachineBlockEntity::new,
                            ModBlocks.BOTTLING_MACHINE.get()).build(null));

    // 抽油机核心
    public static final RegistryObject<BlockEntityType<OilExtractorCoreBlockEntity>> OIL_EXTRACTOR_CORE =
            BLOCK_ENTITIES.register("oil_extractor_core",
                    () -> BlockEntityType.Builder.of(OilExtractorCoreBlockEntity::new,
                            ModBlocks.OIL_EXTRACTOR_CORE.get()).build(null));

    // ========== 新增方块实体 (Phase 3.3) ==========

    // 维度织布机
    public static final RegistryObject<BlockEntityType<DimensionLoomBlockEntity>> DIMENSION_LOOM =
            BLOCK_ENTITIES.register("dimension_loom",
                    () -> BlockEntityType.Builder.of(DimensionLoomBlockEntity::new,
                            ModBlocks.DIMENSION_LOOM.get()).build(null));

    // 提取站
    public static final RegistryObject<BlockEntityType<ExtractionStationBlockEntity>> EXTRACTION_STATION =
            BLOCK_ENTITIES.register("extraction_station",
                    () -> BlockEntityType.Builder.of(ExtractionStationBlockEntity::new,
                            ModBlocks.EXTRACTION_STATION.get()).build(null));

    // 假玩家激活器
    public static final RegistryObject<BlockEntityType<FakePlayerActivatorBlockEntity>> FAKE_PLAYER_ACTIVATOR =
            BLOCK_ENTITIES.register("fake_player_activator",
                    () -> BlockEntityType.Builder.of(FakePlayerActivatorBlockEntity::new,
                            ModBlocks.FAKE_PLAYER_ACTIVATOR.get()).build(null));

    // 提纯祭坛
    public static final RegistryObject<BlockEntityType<PurificationAltarBlockEntity>> PURIFICATION_ALTAR =
            BLOCK_ENTITIES.register("purification_altar",
                    () -> BlockEntityType.Builder.of(PurificationAltarBlockEntity::new,
                            ModBlocks.PURIFICATION_ALTAR.get()).build(null));

    // 剑升级工作站
    public static final RegistryObject<BlockEntityType<SwordUpgradeStationBlockEntity>> SWORD_UPGRADE_STATION =
            BLOCK_ENTITIES.register("sword_upgrade_station",
                    () -> BlockEntityType.Builder.of(SwordUpgradeStationBlockEntity::new,
                            ModBlocks.SWORD_UPGRADE_STATION.get()).build(null));

    // 重生仓核心
    public static final RegistryObject<BlockEntityType<RespawnChamberCoreBlockEntity>> RESPAWN_CHAMBER_CORE =
            BLOCK_ENTITIES.register("respawn_chamber_core",
                    () -> BlockEntityType.Builder.of(RespawnChamberCoreBlockEntity::new,
                            ModBlocks.RESPAWN_CHAMBER_CORE.get()).build(null));

    // 简易智慧之泉
    public static final RegistryObject<BlockEntityType<SimpleWisdomShrineBlockEntity>> SIMPLE_WISDOM_SHRINE =
            BLOCK_ENTITIES.register("simple_wisdom_shrine",
                    () -> BlockEntityType.Builder.of(SimpleWisdomShrineBlockEntity::new,
                            ModBlocks.SIMPLE_WISDOM_SHRINE.get()).build(null));

    // ========== BlockEntity迁移基本完成 ==========
    // 已迁移: 28/29个主要BlockEntity类型
    // SpacetimeShardBlock 无需BlockEntity (普通矿石方块)
}
