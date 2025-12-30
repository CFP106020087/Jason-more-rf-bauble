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

    // ========== TODO: 更多方块实体 ==========
    // - AnimalFeederBlockEntity
    // - BioGeneratorBlockEntity
    // - BloodGeneratorBlockEntity
    // - BottlingMachineBlockEntity
    // - CompostBinBlockEntity
    // - DimensionLoomBlockEntity
    // - ExtractionStationBlockEntity
    // - FakePlayerActivatorBlockEntity
    // - FishingNetBlockEntity
    // - MegaChestBlockEntity
    // - OilExtractorCoreBlockEntity
    // - OilGeneratorBlockEntity
    // - PlantOilPressBlockEntity
    // - ProtectionFieldBlockEntity
    // - PurificationAltarBlockEntity
    // - RespawnChamberCoreBlockEntity
    // - SwordUpgradeStationBlockEntity
    // - TimeControllerBlockEntity
    // - TradingStationBlockEntity
    // - TransferStationBlockEntity
}
