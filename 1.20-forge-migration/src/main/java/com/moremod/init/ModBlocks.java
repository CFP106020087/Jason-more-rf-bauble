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
