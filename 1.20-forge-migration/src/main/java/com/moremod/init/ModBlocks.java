package com.moremod.init;

import com.moremod.MoreMod;
import com.moremod.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块注册表 - 1.20 Forge版本
 *
 * 使用DeferredRegister替代1.12的GameRegistry
 *
 * 1.12 -> 1.20 API变更:
 * - Material.IRON -> BlockBehaviour.Properties.of()
 * - setHardness/setResistance -> .strength(hardness, resistance)
 * - setCreativeTab -> 通过CreativeModeTab.displayItems添加
 */
public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MoreMod.MOD_ID);

    // ========== 机械核心系统方块 ==========

    // 升级艙核心
    public static final RegistryObject<Block> UPGRADE_CHAMBER_CORE = BLOCKS.register("upgrade_chamber_core",
            () -> new UpgradeChamberCoreBlock(BlockBehaviour.Properties.of()
                    .strength(5.0F, 15.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // 祭坛核心
    public static final RegistryObject<Block> RITUAL_CORE = BLOCKS.register("ritual_core",
            () -> new RitualCoreBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 10.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    // 智慧之泉核心
    public static final RegistryObject<Block> WISDOM_FOUNTAIN_CORE = BLOCKS.register("wisdom_fountain_core",
            () -> new WisdomFountainCoreBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 10.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    // 量子采石场
    public static final RegistryObject<Block> QUANTUM_QUARRY = BLOCKS.register("quantum_quarry",
            () -> new QuantumQuarryBlock(BlockBehaviour.Properties.of()
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // 量子驱动器
    public static final RegistryObject<Block> QUARRY_ACTUATOR = BLOCKS.register("quarry_actuator",
            () -> new QuarryActuatorBlock(BlockBehaviour.Properties.of()
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // 充电站
    public static final RegistryObject<Block> CHARGING_STATION = BLOCKS.register("charging_station",
            () -> new ChargingStationBlock(BlockBehaviour.Properties.of()
                    .strength(3.5F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // 时间加速器
    public static final RegistryObject<Block> TEMPORAL_ACCELERATOR = BLOCKS.register("temporal_accelerator",
            () -> new TemporalAcceleratorBlock(BlockBehaviour.Properties.of()
                    .strength(4.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // 展示台
    public static final RegistryObject<Block> PEDESTAL = BLOCKS.register("pedestal",
            () -> new PedestalBlock(BlockBehaviour.Properties.of()
                    .strength(2.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    // 守护者方块
    public static final RegistryObject<Block> GUARDIAN_STONE = BLOCKS.register("guardian_stone",
            () -> new GuardianStoneBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 10.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    // 符文虚空石
    public static final RegistryObject<Block> RUNED_VOID_STONE = BLOCKS.register("runed_void_stone",
            () -> new RunedVoidStoneBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 10.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    // ========== 发电机 ==========

    // 生物质发电机
    public static final RegistryObject<Block> BIO_GENERATOR = BLOCKS.register("bio_generator",
            () -> new BioGeneratorBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 10.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // 石油发电机
    public static final RegistryObject<Block> OIL_GENERATOR = BLOCKS.register("oil_generator",
            () -> new OilGeneratorBlock(BlockBehaviour.Properties.of()
                    .strength(4.0F, 10.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // ========== 自动化方块 ==========

    // 动物喂食器
    public static final RegistryObject<Block> ANIMAL_FEEDER = BLOCKS.register("animal_feeder",
            () -> new AnimalFeederBlock(BlockBehaviour.Properties.of()
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD)));

    // 渔网
    public static final RegistryObject<Block> FISHING_NET = BLOCKS.register("fishing_net",
            () -> new FishingNetBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F, 3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    // 超大箱子
    public static final RegistryObject<Block> MEGA_CHEST = BLOCKS.register("mega_chest",
            () -> new MegaChestBlock(BlockBehaviour.Properties.of()
                    .strength(2.5F, 5.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    // 转移台
    public static final RegistryObject<Block> TRANSFER_STATION = BLOCKS.register("transfer_station",
            () -> new TransferStationBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 15.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // 血液发电机
    public static final RegistryObject<Block> BLOOD_GENERATOR = BLOCKS.register("blood_generator",
            BloodGeneratorBlock::new);

    // 堆肥桶
    public static final RegistryObject<Block> COMPOST_BIN = BLOCKS.register("compost_bin",
            CompostBinBlock::new);

    // 植物油压榨机
    public static final RegistryObject<Block> PLANT_OIL_PRESS = BLOCKS.register("plant_oil_press",
            PlantOilPressBlock::new);

    // 时间控制器
    public static final RegistryObject<Block> TIME_CONTROLLER = BLOCKS.register("time_controller",
            TimeControllerBlock::new);

    // 交易站
    public static final RegistryObject<Block> TRADING_STATION = BLOCKS.register("trading_station",
            TradingStationBlock::new);

    // 装瓶机
    public static final RegistryObject<Block> BOTTLING_MACHINE = BLOCKS.register("bottling_machine",
            BottlingMachineBlock::new);

    // 抽油机核心
    public static final RegistryObject<Block> OIL_EXTRACTOR_CORE = BLOCKS.register("oil_extractor_core",
            OilExtractorCoreBlock::new);

    // ========== 特殊方块 ==========

    // 不可破坏屏障 - 虚空水晶
    public static final RegistryObject<Block> UNBREAKABLE_BARRIER_VOID_CRYSTAL = BLOCKS.register(
            "unbreakable_barrier_void_crystal",
            () -> new UnbreakableBarrierBlock(UnbreakableBarrierBlock.BarrierType.VOID_CRYSTAL));

    // 不可破坏屏障 - 量子力场
    public static final RegistryObject<Block> UNBREAKABLE_BARRIER_QUANTUM_FIELD = BLOCKS.register(
            "unbreakable_barrier_quantum_field",
            () -> new UnbreakableBarrierBlock(UnbreakableBarrierBlock.BarrierType.QUANTUM_FIELD));

    // ========== 新增方块 (Phase 3.3) ==========

    // 古代核心
    public static final RegistryObject<Block> ANCIENT_CORE = BLOCKS.register("ancient_core",
            AncientCoreBlock::new);

    // 维度织布机
    public static final RegistryObject<Block> DIMENSION_LOOM = BLOCKS.register("dimension_loom",
            DimensionLoomBlock::new);

    // 提取站
    public static final RegistryObject<Block> EXTRACTION_STATION = BLOCKS.register("extraction_station",
            ExtractionStationBlock::new);

    // 假玩家激活器
    public static final RegistryObject<Block> FAKE_PLAYER_ACTIVATOR = BLOCKS.register("fake_player_activator",
            FakePlayerActivatorBlock::new);

    // 提纯祭坛
    public static final RegistryObject<Block> PURIFICATION_ALTAR = BLOCKS.register("purification_altar",
            PurificationAltarBlock::new);

    // 剑升级工作站
    public static final RegistryObject<Block> SWORD_UPGRADE_STATION = BLOCKS.register("sword_upgrade_station",
            SwordUpgradeStationBlock::new);

    // 附魔增强方块 - 奥术石
    public static final RegistryObject<Block> ENCHANTING_BOOSTER_ARCANE = BLOCKS.register("enchanting_booster_arcane",
            () -> new EnchantingBoosterBlock(EnchantingBoosterBlock.BoosterType.ARCANE_STONE));

    // 附魔增强方块 - 强化书架
    public static final RegistryObject<Block> ENCHANTING_BOOSTER_BOOKSHELF = BLOCKS.register("enchanting_booster_bookshelf",
            () -> new EnchantingBoosterBlock(EnchantingBoosterBlock.BoosterType.ENCHANTED_BOOKSHELF));

    // 附魔增强方块 - 知识水晶
    public static final RegistryObject<Block> ENCHANTING_BOOSTER_CRYSTAL = BLOCKS.register("enchanting_booster_crystal",
            () -> new EnchantingBoosterBlock(EnchantingBoosterBlock.BoosterType.KNOWLEDGE_CRYSTAL));

    // 附魔增强方块 - 灵魂图书馆
    public static final RegistryObject<Block> ENCHANTING_BOOSTER_SOUL = BLOCKS.register("enchanting_booster_soul",
            () -> new EnchantingBoosterBlock(EnchantingBoosterBlock.BoosterType.SOUL_LIBRARY));

    // ========== TODO: 更多方块 ==========
    // - BlockAncientCore
    // - BlockAnimalFeeder
    // - BlockBioGenerator
    // - BlockBloodGenerator
    // - BlockBottlingMachine
    // - BlockCompostBin
    // - BlockDimensionLoom
    // - BlockExtractionStation
    // - BlockFakePlayerActivator
    // - BlockFishingNet
    // - BlockMegaChest
    // - BlockOilExtractorCore
    // - BlockPlantOilPress
    // - BlockPurificationAltar
    // - BlockRespawnChamberCore
    // - BlockSpacetimeShard
    // - BlockSwordUpgradeStation
    // - BlockTimeController
    // - BlockTradingStation
    // - BlockTransferStation
    // - BlockUnbreakableBarrier
    // - ItemTransporter
    // - ProtectionFieldGenerator
    // - BlockEnchantingBooster
    // - BlockOilGenerator
}
