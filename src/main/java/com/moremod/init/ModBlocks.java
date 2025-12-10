package com.moremod.init;

import com.moremod.block.BlockDimensionLoom;
import com.moremod.block.ProtectionFieldGenerator;
import com.moremod.block.BlockSpacetimeShard;
import com.moremod.block.BlockTemporalAccelerator;
import com.moremod.block.BlockTimeController;
import com.moremod.block.BlockUnbreakableBarrier;
import com.moremod.block.BlockGuardianStone;
import com.moremod.block.BlockRunedVoidStone;
import com.moremod.block.BlockAncientCore;
import com.moremod.block.BlockWisdomFountainCore;
import com.moremod.block.ItemTransporter;
import com.moremod.block.BlockTradingStation;
import com.moremod.block.BlockSimpleWisdomShrine;
import com.moremod.block.BlockExtractionStation;  // ⭐ 添加提取台导入
import com.moremod.block.BlockPurificationAltar;  // 🔮 添加提纯祭坛导入
import com.moremod.block.BlockTransferStation;    // 🎨 添加转移台导入
import com.moremod.block.BlockEnchantingBooster;  // ✨ 附魔增强方块
import com.moremod.block.ItemBlockEnchantingBooster;
import com.moremod.block.BlockFishingNet;         // 🎣 渔网
import com.moremod.block.BlockCompostBin;         // 堆肥桶
import com.moremod.block.BlockAnimalFeeder;       // 动物喂食器
import com.moremod.block.BlockBioGenerator;       // 生物质发电机
import com.moremod.block.BlockFakePlayerActivator; // 假玩家激活器
import com.moremod.block.BlockUpgradeChamberCore;  // 升級艙核心
import com.moremod.block.BlockRespawnChamberCore;  // 重生倉核心

// ⛽ 能源系統方塊
import com.moremod.block.BlockOilExtractorCore;
import com.moremod.block.BlockPlantOilPress;
import com.moremod.block.BlockOilGenerator;
import com.moremod.block.energy.BlockChargingStation;

// 🩸 血液发电机
import com.moremod.block.BlockBloodGenerator;

// 🖨️ 打印机
import com.moremod.printer.BlockPrinter;
import com.moremod.printer.TileEntityPrinter;

// 🗡️ 两个版本的剑升级台方块
import com.moremod.block.BlockSwordUpgradeStation;
import com.moremod.block.BlockSwordUpgradeStationMaterial;

import com.moremod.moremod;

// TileEntities
import com.moremod.tile.TileEntityDimensionLoom;
import com.moremod.tile.TileEntityProtectionField;
import com.moremod.tile.TileEntityTemporalAccelerator;
import com.moremod.tile.TileEntityTimeController;
import com.moremod.tile.TileEntityWisdomFountain;
import com.moremod.tile.TileEntityItemTransporter;
import com.moremod.tile.TileTradingStation;
import com.moremod.tile.TileEntitySimpleWisdomShrine;
import com.moremod.tile.TileEntityExtractionStation;  // ⭐ 添加提取台 TE 导入
import com.moremod.tile.TileEntityPurificationAltar;  // 🔮 添加提纯祭坛 TE 导入
import com.moremod.tile.TileEntityTransferStation;    // 🎨 添加转移台 TE 导入
import com.moremod.tile.TileEntityFishingNet;         // 🎣 渔网 TE
import com.moremod.tile.TileEntityCompostBin;         // 堆肥桶 TE
import com.moremod.tile.TileEntityAnimalFeeder;       // 动物喂食器 TE
import com.moremod.tile.TileEntityBioGenerator;       // 生物质发电机 TE
import com.moremod.tile.TileEntityFakePlayerActivator; // 假玩家激活器 TE
import com.moremod.tile.TileEntityUpgradeChamberCore;  // 升級艙核心 TE
import com.moremod.tile.TileEntityRespawnChamberCore;  // 重生倉核心 TE

// ⛽ 能源系統 TileEntity
import com.moremod.tile.TileEntityOilExtractorCore;
import com.moremod.tile.TileEntityPlantOilPress;
import com.moremod.tile.TileEntityOilGenerator;
import com.moremod.tile.TileEntityChargingStation;

// 🩸 血液发电机 TileEntity
import com.moremod.tile.TileEntityBloodGenerator;

// 🗡️ 两个版本的剑升级台 TE
import com.moremod.tile.TileEntitySwordUpgradeStation;
import com.moremod.tile.TileEntitySwordUpgradeStationMaterial;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = moremod.MODID)
public class ModBlocks {

    public static final Block SPACETIME_SHARD_ORE = new BlockSpacetimeShard();
    public static Block TIME_CONTROLLER;
    public static final Block UNBREAKABLE_BARRIER_VOID = new BlockUnbreakableBarrier(BlockUnbreakableBarrier.BarrierType.VOID_CRYSTAL);
    public static final Block UNBREAKABLE_BARRIER_QUANTUM = new BlockUnbreakableBarrier(BlockUnbreakableBarrier.BarrierType.QUANTUM_FIELD);
    public static final Block UNBREAKABLE_BARRIER_TEMPORAL = new BlockUnbreakableBarrier(BlockUnbreakableBarrier.BarrierType.TEMPORAL_LOCK);
    public static final Block UNBREAKABLE_BARRIER_ANCHOR = new BlockUnbreakableBarrier(BlockUnbreakableBarrier.BarrierType.DIMENSIONAL_ANCHOR);
    public static final Block UNBREAKABLE_BARRIER_ETHEREAL = new BlockUnbreakableBarrier(BlockUnbreakableBarrier.BarrierType.ETHEREAL_WALL);
    public static Block TEMPORAL_ACCELERATOR;
    public static BlockDimensionLoom dimensionLoom;
    public static Block PROTECTION_FIELD_GENERATOR;
    public static Block GUARDIAN_STONE_BLOCK;
    public static Block RUNED_VOID_STONE_BLOCK;
    public static Block ANCIENT_CORE_BLOCK;
    public static Block WISDOM_FOUNTAIN_CORE;
    public static Block ITEM_TRANSPORTER;
    public static Block TRADING_STATION;
    public static Block SIMPLE_WISDOM_SHRINE;

    // ⭐ 提取台方块
    public static Block EXTRACTION_STATION;

    // 🔮 提纯祭坛方块
    public static Block PURIFICATION_ALTAR;

    // 🎨 转移台方块
    public static Block TRANSFER_STATION;

    // 🗡️ 剑升级台（两个版本并存）
    public static Block SWORD_UPGRADE_STATION;
    public static Block SWORD_UPGRADE_STATION_MATERIAL;

    // ✨ 附魔增强方块
    public static BlockEnchantingBooster ENCHANTING_BOOSTER;

    // 🎣 渔网方块
    public static Block FISHING_NET;

    // 堆肥桶方块
    public static Block COMPOST_BIN;

    // 动物喂食器方块
    public static Block ANIMAL_FEEDER;

    // 生物质发电机方块
    public static Block BIO_GENERATOR;

    // 假玩家激活器方块
    public static Block FAKE_PLAYER_ACTIVATOR;

    // 升級艙核心方块
    public static Block UPGRADE_CHAMBER_CORE;

    // 重生倉核心方块
    public static Block RESPAWN_CHAMBER_CORE;

    // ⛽ 能源系統方塊
    public static Block OIL_EXTRACTOR_CORE;
    public static Block PLANT_OIL_PRESS;
    public static Block OIL_GENERATOR;
    public static Block CHARGING_STATION;

    // 🩸 血液发电机方块
    public static Block BLOOD_GENERATOR;

    // 🖨️ 打印机方块
    public static Block PRINTER;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(SPACETIME_SHARD_ORE);

        TIME_CONTROLLER = new BlockTimeController();
        event.getRegistry().register(TIME_CONTROLLER);

        dimensionLoom = new BlockDimensionLoom();
        event.getRegistry().register(dimensionLoom);

        event.getRegistry().registerAll(
                UNBREAKABLE_BARRIER_VOID,
                UNBREAKABLE_BARRIER_QUANTUM,
                UNBREAKABLE_BARRIER_TEMPORAL,
                UNBREAKABLE_BARRIER_ANCHOR,
                UNBREAKABLE_BARRIER_ETHEREAL
        );

        TEMPORAL_ACCELERATOR = new BlockTemporalAccelerator();
        event.getRegistry().register(TEMPORAL_ACCELERATOR);

        PROTECTION_FIELD_GENERATOR = new ProtectionFieldGenerator();
        event.getRegistry().register(PROTECTION_FIELD_GENERATOR);

        GUARDIAN_STONE_BLOCK = new BlockGuardianStone();
        event.getRegistry().register(GUARDIAN_STONE_BLOCK);

        RUNED_VOID_STONE_BLOCK = new BlockRunedVoidStone();
        event.getRegistry().register(RUNED_VOID_STONE_BLOCK);

        ANCIENT_CORE_BLOCK = new BlockAncientCore();
        event.getRegistry().register(ANCIENT_CORE_BLOCK);

        WISDOM_FOUNTAIN_CORE = new BlockWisdomFountainCore();
        event.getRegistry().register(WISDOM_FOUNTAIN_CORE);

        ITEM_TRANSPORTER = ItemTransporter.INSTANCE;
        event.getRegistry().register(ITEM_TRANSPORTER);

        TRADING_STATION = new BlockTradingStation();
        event.getRegistry().register(TRADING_STATION);


        SIMPLE_WISDOM_SHRINE = new BlockSimpleWisdomShrine();
        event.getRegistry().register(SIMPLE_WISDOM_SHRINE);

        // ⭐ 注册提取台方块
        EXTRACTION_STATION = new BlockExtractionStation();
        event.getRegistry().register(EXTRACTION_STATION);
        System.out.println("[MoreMod] 提取台方塊已註冊");

        // 🔮 注册提纯祭坛方块
        PURIFICATION_ALTAR = new BlockPurificationAltar();
        event.getRegistry().register(PURIFICATION_ALTAR);
        System.out.println("[MoreMod] 提純祭壇方塊已註冊");

        // 🎨 注册转移台方块
        TRANSFER_STATION = new BlockTransferStation();
        event.getRegistry().register(TRANSFER_STATION);
        System.out.println("[MoreMod] 轉移台方塊已註冊");

        // 🗡️ 注册剑升级台（legacy）
        SWORD_UPGRADE_STATION = new BlockSwordUpgradeStation();
        event.getRegistry().register(SWORD_UPGRADE_STATION);
        System.out.println("[MoreMod] 劍升級台(legacy) 方塊已註冊");

        // 🗡️ 注册剑升级台（material）
        SWORD_UPGRADE_STATION_MATERIAL = new BlockSwordUpgradeStationMaterial();
        event.getRegistry().register(SWORD_UPGRADE_STATION_MATERIAL);
        System.out.println("[MoreMod] 劍升級台(material) 方塊已註冊");

        // ✨ 注册附魔增强方块
        ENCHANTING_BOOSTER = new BlockEnchantingBooster();
        event.getRegistry().register(ENCHANTING_BOOSTER);
        System.out.println("[MoreMod] ✨ 附魔增強方塊已註冊 (4種類型)");

        // 🎣 注册渔网方块
        FISHING_NET = new BlockFishingNet();
        event.getRegistry().register(FISHING_NET);
        System.out.println("[MoreMod] 🎣 漁網方塊已註冊");

        // 堆肥桶方块
        COMPOST_BIN = new BlockCompostBin();
        event.getRegistry().register(COMPOST_BIN);
        System.out.println("[MoreMod] 堆肥桶方塊已註冊");

        // 动物喂食器方块
        ANIMAL_FEEDER = new BlockAnimalFeeder();
        event.getRegistry().register(ANIMAL_FEEDER);
        System.out.println("[MoreMod] 動物餵食器方塊已註冊");

        // 生物质发电机方块
        BIO_GENERATOR = new BlockBioGenerator();
        event.getRegistry().register(BIO_GENERATOR);
        System.out.println("[MoreMod] 生物質發電機方塊已註冊");

        // 假玩家激活器方块
        FAKE_PLAYER_ACTIVATOR = new BlockFakePlayerActivator();
        event.getRegistry().register(FAKE_PLAYER_ACTIVATOR);
        System.out.println("[MoreMod] 假玩家激活器方塊已註冊");

        // 升級艙核心方块
        UPGRADE_CHAMBER_CORE = new BlockUpgradeChamberCore();
        event.getRegistry().register(UPGRADE_CHAMBER_CORE);
        System.out.println("[MoreMod] 升級艙核心方塊已註冊");

        // 重生倉核心方块
        RESPAWN_CHAMBER_CORE = new BlockRespawnChamberCore();
        event.getRegistry().register(RESPAWN_CHAMBER_CORE);
        System.out.println("[MoreMod] 重生倉核心方塊已註冊");

        // ⛽ 能源系統方塊
        OIL_EXTRACTOR_CORE = new BlockOilExtractorCore();
        event.getRegistry().register(OIL_EXTRACTOR_CORE);
        System.out.println("[MoreMod] ⛽ 抽油機核心方塊已註冊");

        PLANT_OIL_PRESS = new BlockPlantOilPress();
        event.getRegistry().register(PLANT_OIL_PRESS);
        System.out.println("[MoreMod] ⛽ 植物油壓榨機方塊已註冊");

        OIL_GENERATOR = new BlockOilGenerator();
        event.getRegistry().register(OIL_GENERATOR);
        System.out.println("[MoreMod] ⛽ 石油發電機方塊已註冊");

        CHARGING_STATION = new BlockChargingStation();
        event.getRegistry().register(CHARGING_STATION);
        System.out.println("[MoreMod] ⚡ 充能站方塊已註冊");

        // 🩸 血液发电机方块
        BLOOD_GENERATOR = new BlockBloodGenerator();
        event.getRegistry().register(BLOOD_GENERATOR);
        System.out.println("[MoreMod] 🩸 血液發電機方塊已註冊");

        // 🖨️ 打印机方块
        PRINTER = new BlockPrinter();
        event.getRegistry().register(PRINTER);
        System.out.println("[MoreMod] 🖨️ 打印機方塊已註冊");

        // ---- TileEntity 注册 ----
        GameRegistry.registerTileEntity(TileEntityDimensionLoom.class,
                new ResourceLocation(moremod.MODID, "dimension_loom"));
        GameRegistry.registerTileEntity(TileEntityTimeController.class,
                new ResourceLocation(moremod.MODID, "time_controller"));
        GameRegistry.registerTileEntity(TileEntityTemporalAccelerator.class,
                new ResourceLocation(moremod.MODID, "temporal_accelerator"));
        GameRegistry.registerTileEntity(TileEntityProtectionField.class,
                new ResourceLocation(moremod.MODID, "protection_field_generator"));
        GameRegistry.registerTileEntity(TileEntityWisdomFountain.class,
                new ResourceLocation(moremod.MODID, "wisdom_fountain"));
        GameRegistry.registerTileEntity(TileEntityItemTransporter.class,
                new ResourceLocation(moremod.MODID, "item_transporter"));
        GameRegistry.registerTileEntity(TileTradingStation.class,
                new ResourceLocation(moremod.MODID, "trading_station"));
        GameRegistry.registerTileEntity(TileEntitySimpleWisdomShrine.class,
                new ResourceLocation(moremod.MODID, "simple_wisdom_shrine"));

        // ⭐ 提取台 TileEntity
        GameRegistry.registerTileEntity(TileEntityExtractionStation.class,
                new ResourceLocation(moremod.MODID, "extraction_station"));
        System.out.println("[MoreMod] 提取台 TileEntity 已註冊");

        // 🔮 提纯祭坛 TileEntity
        GameRegistry.registerTileEntity(TileEntityPurificationAltar.class,
                new ResourceLocation(moremod.MODID, "purification_altar"));
        System.out.println("[MoreMod] 提純祭壇 TileEntity 已註冊");

        // 🎨 转移台 TileEntity
        GameRegistry.registerTileEntity(TileEntityTransferStation.class,
                new ResourceLocation(moremod.MODID, "transfer_station"));
        System.out.println("[MoreMod] 轉移台 TileEntity 已註冊");

        // 🗡️ 剑升级台 TE（两个版本）
        GameRegistry.registerTileEntity(TileEntitySwordUpgradeStation.class,
                new ResourceLocation(moremod.MODID, "sword_upgrade_station"));
        GameRegistry.registerTileEntity(TileEntitySwordUpgradeStationMaterial.class,
                new ResourceLocation(moremod.MODID, "sword_upgrade_station_material"));
        System.out.println("[MoreMod] 劍升級台 TileEntity（legacy+material）已註冊");

        // 🎣 渔网 TileEntity
        GameRegistry.registerTileEntity(TileEntityFishingNet.class,
                new ResourceLocation(moremod.MODID, "fishing_net"));
        System.out.println("[MoreMod] 🎣 漁網 TileEntity 已註冊");

        // 堆肥桶 TileEntity
        GameRegistry.registerTileEntity(TileEntityCompostBin.class,
                new ResourceLocation(moremod.MODID, "compost_bin"));
        System.out.println("[MoreMod] 堆肥桶 TileEntity 已註冊");

        // 动物喂食器 TileEntity
        GameRegistry.registerTileEntity(TileEntityAnimalFeeder.class,
                new ResourceLocation(moremod.MODID, "animal_feeder"));
        System.out.println("[MoreMod] 動物餵食器 TileEntity 已註冊");

        // 生物质发电机 TileEntity
        GameRegistry.registerTileEntity(TileEntityBioGenerator.class,
                new ResourceLocation(moremod.MODID, "bio_generator"));
        System.out.println("[MoreMod] 生物質發電機 TileEntity 已註冊");

        // 假玩家激活器 TileEntity
        GameRegistry.registerTileEntity(TileEntityFakePlayerActivator.class,
                new ResourceLocation(moremod.MODID, "fake_player_activator"));
        System.out.println("[MoreMod] 假玩家激活器 TileEntity 已註冊");

        // 升級艙核心 TileEntity
        GameRegistry.registerTileEntity(TileEntityUpgradeChamberCore.class,
                new ResourceLocation(moremod.MODID, "upgrade_chamber_core"));
        System.out.println("[MoreMod] 升級艙核心 TileEntity 已註冊");

        // 重生倉核心 TileEntity
        GameRegistry.registerTileEntity(TileEntityRespawnChamberCore.class,
                new ResourceLocation(moremod.MODID, "respawn_chamber_core"));
        System.out.println("[MoreMod] 重生倉核心 TileEntity 已註冊");

        // ⛽ 能源系統 TileEntity
        GameRegistry.registerTileEntity(TileEntityOilExtractorCore.class,
                new ResourceLocation(moremod.MODID, "oil_extractor_core"));
        System.out.println("[MoreMod] ⛽ 抽油機核心 TileEntity 已註冊");

        GameRegistry.registerTileEntity(TileEntityPlantOilPress.class,
                new ResourceLocation(moremod.MODID, "plant_oil_press"));
        System.out.println("[MoreMod] ⛽ 植物油壓榨機 TileEntity 已註冊");

        GameRegistry.registerTileEntity(TileEntityOilGenerator.class,
                new ResourceLocation(moremod.MODID, "oil_generator"));
        System.out.println("[MoreMod] ⛽ 石油發電機 TileEntity 已註冊");

        GameRegistry.registerTileEntity(TileEntityChargingStation.class,
                new ResourceLocation(moremod.MODID, "charging_station"));
        System.out.println("[MoreMod] ⚡ 充能站 TileEntity 已註冊");

        // 🩸 血液发电机 TileEntity
        GameRegistry.registerTileEntity(TileEntityBloodGenerator.class,
                new ResourceLocation(moremod.MODID, "blood_generator"));
        System.out.println("[MoreMod] 🩸 血液發電機 TileEntity 已註冊");

        // 🖨️ 打印机 TileEntity
        GameRegistry.registerTileEntity(TileEntityPrinter.class,
                new ResourceLocation(moremod.MODID, "printer"));
        System.out.println("[MoreMod] 🖨️ 打印機 TileEntity 已註冊");
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemBlock(SPACETIME_SHARD_ORE).setRegistryName(SPACETIME_SHARD_ORE.getRegistryName()));
        event.getRegistry().register(new ItemBlock(TIME_CONTROLLER).setRegistryName(TIME_CONTROLLER.getRegistryName()));
        event.getRegistry().register(new ItemBlock(UNBREAKABLE_BARRIER_VOID).setRegistryName(UNBREAKABLE_BARRIER_VOID.getRegistryName()));
        event.getRegistry().register(new ItemBlock(UNBREAKABLE_BARRIER_QUANTUM).setRegistryName(UNBREAKABLE_BARRIER_QUANTUM.getRegistryName()));
        event.getRegistry().register(new ItemBlock(UNBREAKABLE_BARRIER_TEMPORAL).setRegistryName(UNBREAKABLE_BARRIER_TEMPORAL.getRegistryName()));
        event.getRegistry().register(new ItemBlock(UNBREAKABLE_BARRIER_ANCHOR).setRegistryName(UNBREAKABLE_BARRIER_ANCHOR.getRegistryName()));
        event.getRegistry().register(new ItemBlock(UNBREAKABLE_BARRIER_ETHEREAL).setRegistryName(UNBREAKABLE_BARRIER_ETHEREAL.getRegistryName()));
        event.getRegistry().register(new ItemBlock(dimensionLoom).setRegistryName(dimensionLoom.getRegistryName()));

        if (TEMPORAL_ACCELERATOR != null) {
            event.getRegistry().register(new ItemBlock(TEMPORAL_ACCELERATOR).setRegistryName(TEMPORAL_ACCELERATOR.getRegistryName()));
        }
        if (PROTECTION_FIELD_GENERATOR != null) {
            event.getRegistry().register(new ItemBlock(PROTECTION_FIELD_GENERATOR).setRegistryName(PROTECTION_FIELD_GENERATOR.getRegistryName()));
        }
        if (GUARDIAN_STONE_BLOCK != null) {
            event.getRegistry().register(new ItemBlock(GUARDIAN_STONE_BLOCK).setRegistryName(GUARDIAN_STONE_BLOCK.getRegistryName()));
        }
        if (RUNED_VOID_STONE_BLOCK != null) {
            event.getRegistry().register(new ItemBlock(RUNED_VOID_STONE_BLOCK).setRegistryName(RUNED_VOID_STONE_BLOCK.getRegistryName()));
        }
        if (ANCIENT_CORE_BLOCK != null) {
            event.getRegistry().register(new ItemBlock(ANCIENT_CORE_BLOCK).setRegistryName(ANCIENT_CORE_BLOCK.getRegistryName()));
        }
        if (WISDOM_FOUNTAIN_CORE != null) {
            event.getRegistry().register(new ItemBlock(WISDOM_FOUNTAIN_CORE).setRegistryName(WISDOM_FOUNTAIN_CORE.getRegistryName()));
        }
        if (ITEM_TRANSPORTER != null) {
            event.getRegistry().register(new ItemBlock(ITEM_TRANSPORTER).setRegistryName(ITEM_TRANSPORTER.getRegistryName()));
        }
        if (TRADING_STATION != null) {
            event.getRegistry().register(new ItemBlock(TRADING_STATION).setRegistryName(TRADING_STATION.getRegistryName()));
        }
        if (SIMPLE_WISDOM_SHRINE != null) {
            event.getRegistry().register(new ItemBlock(SIMPLE_WISDOM_SHRINE).setRegistryName(SIMPLE_WISDOM_SHRINE.getRegistryName()));
        }

        // ⭐ 提取台 ItemBlock
        if (EXTRACTION_STATION != null) {
            event.getRegistry().register(new ItemBlock(EXTRACTION_STATION)
                    .setRegistryName(EXTRACTION_STATION.getRegistryName()));
            System.out.println("[MoreMod] 提取台 ItemBlock 已註冊");
        }

        // 🔮 提纯祭坛 ItemBlock
        if (PURIFICATION_ALTAR != null) {
            event.getRegistry().register(new ItemBlock(PURIFICATION_ALTAR)
                    .setRegistryName(PURIFICATION_ALTAR.getRegistryName()));
            System.out.println("[MoreMod] 提純祭壇 ItemBlock 已註冊");
        }

        // 🎨 转移台 ItemBlock
        if (TRANSFER_STATION != null) {
            event.getRegistry().register(new ItemBlock(TRANSFER_STATION)
                    .setRegistryName(TRANSFER_STATION.getRegistryName()));
            System.out.println("[MoreMod] 轉移台 ItemBlock 已註冊");
        }

        // 🗡️ ItemBlock（两个版本）
        if (SWORD_UPGRADE_STATION != null) {
            event.getRegistry().register(new ItemBlock(SWORD_UPGRADE_STATION)
                    .setRegistryName(SWORD_UPGRADE_STATION.getRegistryName()));
            System.out.println("[MoreMod] 劍升級台(legacy) ItemBlock已註冊");
        }
        if (SWORD_UPGRADE_STATION_MATERIAL != null) {
            event.getRegistry().register(new ItemBlock(SWORD_UPGRADE_STATION_MATERIAL)
                    .setRegistryName(SWORD_UPGRADE_STATION_MATERIAL.getRegistryName()));
            System.out.println("[MoreMod] 劍升級台(material) ItemBlock已註冊");
        }

        // ✨ 附魔增强方块 ItemBlock (支持多变体)
        if (ENCHANTING_BOOSTER != null) {
            event.getRegistry().register(new ItemBlockEnchantingBooster(ENCHANTING_BOOSTER)
                    .setRegistryName(ENCHANTING_BOOSTER.getRegistryName()));
            System.out.println("[MoreMod] ✨ 附魔增強方塊 ItemBlock 已註冊");
        }

        // 🎣 渔网 ItemBlock
        if (FISHING_NET != null) {
            event.getRegistry().register(new ItemBlock(FISHING_NET)
                    .setRegistryName(FISHING_NET.getRegistryName()));
            System.out.println("[MoreMod] 🎣 漁網 ItemBlock 已註冊");
        }

        // 堆肥桶 ItemBlock
        if (COMPOST_BIN != null) {
            event.getRegistry().register(new ItemBlock(COMPOST_BIN)
                    .setRegistryName(COMPOST_BIN.getRegistryName()));
            System.out.println("[MoreMod] 堆肥桶 ItemBlock 已註冊");
        }

        // 动物喂食器 ItemBlock
        if (ANIMAL_FEEDER != null) {
            event.getRegistry().register(new ItemBlock(ANIMAL_FEEDER)
                    .setRegistryName(ANIMAL_FEEDER.getRegistryName()));
            System.out.println("[MoreMod] 動物餵食器 ItemBlock 已註冊");
        }

        // 生物质发电机 ItemBlock
        if (BIO_GENERATOR != null) {
            event.getRegistry().register(new ItemBlock(BIO_GENERATOR)
                    .setRegistryName(BIO_GENERATOR.getRegistryName()));
            System.out.println("[MoreMod] 生物質發電機 ItemBlock 已註冊");
        }

        // 假玩家激活器 ItemBlock
        if (FAKE_PLAYER_ACTIVATOR != null) {
            event.getRegistry().register(new ItemBlock(FAKE_PLAYER_ACTIVATOR)
                    .setRegistryName(FAKE_PLAYER_ACTIVATOR.getRegistryName()));
            System.out.println("[MoreMod] 假玩家激活器 ItemBlock 已註冊");
        }

        // 升級艙核心 ItemBlock
        if (UPGRADE_CHAMBER_CORE != null) {
            event.getRegistry().register(new ItemBlock(UPGRADE_CHAMBER_CORE)
                    .setRegistryName(UPGRADE_CHAMBER_CORE.getRegistryName()));
            System.out.println("[MoreMod] 升級艙核心 ItemBlock 已註冊");
        }

        // 重生倉核心 ItemBlock
        if (RESPAWN_CHAMBER_CORE != null) {
            event.getRegistry().register(new ItemBlock(RESPAWN_CHAMBER_CORE)
                    .setRegistryName(RESPAWN_CHAMBER_CORE.getRegistryName()));
            System.out.println("[MoreMod] 重生倉核心 ItemBlock 已註冊");
        }

        // ⛽ 能源系統 ItemBlock
        if (OIL_EXTRACTOR_CORE != null) {
            event.getRegistry().register(new ItemBlock(OIL_EXTRACTOR_CORE)
                    .setRegistryName(OIL_EXTRACTOR_CORE.getRegistryName()));
            System.out.println("[MoreMod] ⛽ 抽油機核心 ItemBlock 已註冊");
        }
        if (PLANT_OIL_PRESS != null) {
            event.getRegistry().register(new ItemBlock(PLANT_OIL_PRESS)
                    .setRegistryName(PLANT_OIL_PRESS.getRegistryName()));
            System.out.println("[MoreMod] ⛽ 植物油壓榨機 ItemBlock 已註冊");
        }
        if (OIL_GENERATOR != null) {
            event.getRegistry().register(new ItemBlock(OIL_GENERATOR)
                    .setRegistryName(OIL_GENERATOR.getRegistryName()));
            System.out.println("[MoreMod] ⛽ 石油發電機 ItemBlock 已註冊");
        }
        if (CHARGING_STATION != null) {
            event.getRegistry().register(new ItemBlock(CHARGING_STATION)
                    .setRegistryName(CHARGING_STATION.getRegistryName()));
            System.out.println("[MoreMod] ⚡ 充能站 ItemBlock 已註冊");
        }

        // 🩸 血液发电机 ItemBlock
        if (BLOOD_GENERATOR != null) {
            event.getRegistry().register(new ItemBlock(BLOOD_GENERATOR)
                    .setRegistryName(BLOOD_GENERATOR.getRegistryName()));
            System.out.println("[MoreMod] 🩸 血液發電機 ItemBlock 已註冊");
        }

        // 🖨️ 打印机 ItemBlock
        if (PRINTER != null) {
            event.getRegistry().register(new ItemBlock(PRINTER)
                    .setRegistryName(PRINTER.getRegistryName()));
            System.out.println("[MoreMod] 🖨️ 打印機 ItemBlock 已註冊");
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerBlockModel(SPACETIME_SHARD_ORE);
        registerBlockModel(TIME_CONTROLLER);
        registerBlockModel(UNBREAKABLE_BARRIER_VOID);
        registerBlockModel(UNBREAKABLE_BARRIER_QUANTUM);
        registerBlockModel(UNBREAKABLE_BARRIER_TEMPORAL);
        registerBlockModel(UNBREAKABLE_BARRIER_ANCHOR);
        registerBlockModel(UNBREAKABLE_BARRIER_ETHEREAL);
        registerBlockModel(dimensionLoom);
        if (TEMPORAL_ACCELERATOR != null) registerBlockModel(TEMPORAL_ACCELERATOR);
        if (PROTECTION_FIELD_GENERATOR != null) registerBlockModel(PROTECTION_FIELD_GENERATOR);
        if (GUARDIAN_STONE_BLOCK != null) registerBlockModel(GUARDIAN_STONE_BLOCK);
        if (RUNED_VOID_STONE_BLOCK != null) registerBlockModel(RUNED_VOID_STONE_BLOCK);
        if (ANCIENT_CORE_BLOCK != null) registerBlockModel(ANCIENT_CORE_BLOCK);
        if (WISDOM_FOUNTAIN_CORE != null) registerBlockModel(WISDOM_FOUNTAIN_CORE);
        if (ITEM_TRANSPORTER != null) registerBlockModel(ITEM_TRANSPORTER);
        if (TRADING_STATION != null) registerBlockModel(TRADING_STATION);
        if (SIMPLE_WISDOM_SHRINE != null) registerBlockModel(SIMPLE_WISDOM_SHRINE);

        // ⭐ 提取台模型
        if (EXTRACTION_STATION != null) {
            registerBlockModel(EXTRACTION_STATION);
            System.out.println("[MoreMod] 提取台模型已註冊");
        }

        // 🔮 提纯祭坛模型
        if (PURIFICATION_ALTAR != null) {
            registerBlockModel(PURIFICATION_ALTAR);
            System.out.println("[MoreMod] 提純祭壇模型已註冊");
        }

        // 🎨 转移台模型
        if (TRANSFER_STATION != null) {
            registerBlockModel(TRANSFER_STATION);
            System.out.println("[MoreMod] 轉移台模型已註冊");
        }

        // 🗡️ 模型（两个版本）
        if (SWORD_UPGRADE_STATION != null) {
            registerBlockModel(SWORD_UPGRADE_STATION);
            System.out.println("[MoreMod] 劍升級台(legacy) 模型已註冊");
        }
        if (SWORD_UPGRADE_STATION_MATERIAL != null) {
            registerBlockModel(SWORD_UPGRADE_STATION_MATERIAL);
            System.out.println("[MoreMod] 劍升級台(material) 模型已註冊");
        }

        // ✨ 附魔增强方块模型 (4个变体)
        if (ENCHANTING_BOOSTER != null) {
            for (BlockEnchantingBooster.BoosterType type : BlockEnchantingBooster.BoosterType.values()) {
                ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(ENCHANTING_BOOSTER),
                    type.getMeta(),
                    new ModelResourceLocation("moremod:enchanting_booster_" + type.getName(), "inventory")
                );
            }
            System.out.println("[MoreMod] ✨ 附魔增強方塊模型已註冊 (4種)");
        }

        // 🎣 渔网模型
        if (FISHING_NET != null) {
            registerBlockModel(FISHING_NET);
            System.out.println("[MoreMod] 🎣 漁網模型已註冊");
        }

        // 堆肥桶模型
        if (COMPOST_BIN != null) {
            registerBlockModel(COMPOST_BIN);
            System.out.println("[MoreMod] 堆肥桶模型已註冊");
        }

        // 动物喂食器模型
        if (ANIMAL_FEEDER != null) {
            registerBlockModel(ANIMAL_FEEDER);
            System.out.println("[MoreMod] 動物餵食器模型已註冊");
        }

        // 生物质发电机模型
        if (BIO_GENERATOR != null) {
            registerBlockModel(BIO_GENERATOR);
            System.out.println("[MoreMod] 生物質發電機模型已註冊");
        }

        // 假玩家激活器模型
        if (FAKE_PLAYER_ACTIVATOR != null) {
            registerBlockModel(FAKE_PLAYER_ACTIVATOR);
            System.out.println("[MoreMod] 假玩家激活器模型已註冊");
        }

        // 升級艙核心模型
        if (UPGRADE_CHAMBER_CORE != null) {
            registerBlockModel(UPGRADE_CHAMBER_CORE);
            System.out.println("[MoreMod] 升級艙核心模型已註冊");
        }

        // 重生倉核心模型
        if (RESPAWN_CHAMBER_CORE != null) {
            registerBlockModel(RESPAWN_CHAMBER_CORE);
            System.out.println("[MoreMod] 重生倉核心模型已註冊");
        }

        // ⛽ 能源系統模型
        if (OIL_EXTRACTOR_CORE != null) {
            registerBlockModel(OIL_EXTRACTOR_CORE);
            System.out.println("[MoreMod] ⛽ 抽油機核心模型已註冊");
        }
        if (PLANT_OIL_PRESS != null) {
            registerBlockModel(PLANT_OIL_PRESS);
            System.out.println("[MoreMod] ⛽ 植物油壓榨機模型已註冊");
        }
        if (OIL_GENERATOR != null) {
            registerBlockModel(OIL_GENERATOR);
            System.out.println("[MoreMod] ⛽ 石油發電機模型已註冊");
        }
        if (CHARGING_STATION != null) {
            registerBlockModel(CHARGING_STATION);
            System.out.println("[MoreMod] ⚡ 充能站模型已註冊");
        }

        // 🩸 血液发电机模型
        if (BLOOD_GENERATOR != null) {
            registerBlockModel(BLOOD_GENERATOR);
            System.out.println("[MoreMod] 🩸 血液發電機模型已註冊");
        }

        // 🖨️ 打印机模型
        if (PRINTER != null) {
            registerBlockModel(PRINTER);
            System.out.println("[MoreMod] 🖨️ 打印機模型已註冊");
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerBlockModel(Block block) {
        if (block != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(block), 0,
                    new ModelResourceLocation(block.getRegistryName(), "inventory")
            );
        }
    }
}