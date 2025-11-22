package com.moremod;
import com.moremod.accessorybox.unlock.rules.RuleChecker;
import com.moremod.accessorybox.compat.SetBonusAccessoryBoxCompat;
import com.moremod.accessorybox.unlock.UnlockableSlotsInit;
import com.moremod.capabilities.autoattack.AutoAttackCapabilityHandler;
import com.moremod.client.ClientTickEvent;
import com.moremod.client.JetpackKeyHandler;
import com.moremod.client.KeyBindHandler;
import com.moremod.client.RenderHandler;
import com.moremod.client.gui.EventHUDOverlay;
import com.moremod.client.gui.SmartRejectionGuide;
import com.moremod.commands.CommandLootDebug;
import com.moremod.commands.CommandResetEquipTime;
import com.moremod.compat.PotionCoreCompatEnhanced;
import com.moremod.config.*;
import com.moremod.dimension.PersonalDimensionManager;
import com.moremod.dimension.PersonalDimensionSpawnHandler;
import com.moremod.dimension.PersonalDimensionType;
import com.moremod.entity.*;
import com.moremod.entity.boss.EntityRiftwarden;
import com.moremod.entity.boss.EntityStoneSentinel;
import com.moremod.entity.fx.EntityLaserBeam;
import com.moremod.entity.fx.EntityLightningArc;
import com.moremod.entity.fx.EntityPlayerLaserBeam;
import com.moremod.entity.fx.EntityRiftLightning;
import com.moremod.entity.projectile.EntityVoidBullet;
// ========== 新增：剑气实体导入 ==========
import com.moremod.entity.EntitySwordBeam;
import com.moremod.client.render.RenderSwordBeam;
// ========================================
import com.moremod.event.*;
import com.moremod.eventHandler.*;
import com.moremod.client.gui.GuiHandler;
import com.moremod.fabric.handler.SpatialFabricFirstAidHandler;
import com.moremod.fabric.sanity.CompleteSanitySystem;
import com.moremod.init.GemSystemInit;
import com.moremod.init.RSNodeRegistryCompat;
import com.moremod.init.SimpleReverseDeducer;
import com.moremod.integration.ModIntegration;
import com.moremod.integration.jei.JEIIntegrationManager;
import com.moremod.item.ItemDimensionalRipper;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.chengyue.ChengYueEventHandler;
import com.moremod.item.sawblade.BleedEventHandler;
import com.moremod.network.PacketCreateEnchantedBook;
import com.moremod.network.PacketHandler;
import com.moremod.network.NetworkHandler;
import com.moremod.proxy.CommonProxy;
import com.moremod.recipe.DimensionLoomRecipeLoader;
import com.moremod.ritual.RitualRecipeLoader;
import com.moremod.ritual.fabric.UniversalFabricRituals;
import com.moremod.eventHandler.SimpleCoreHandler;
import com.moremod.eventHandler.CoreDropProtection;
import com.moremod.eventHandler.SmartUpgradeHandler;
import com.moremod.handler.DimensionalRipperEventHandler;

// 配置系统导入
// 时光之心相关导入
import com.moremod.capability.*;

// 机械核心系统导入
import com.moremod.shields.integrated.EnhancedVisualsHandler;
import com.moremod.system.*;
import com.moremod.upgrades.MechanicalCoreNetworkHandler;
import com.moremod.upgrades.auxiliary.AuxiliaryUpgradeManager;
import com.moremod.upgrades.combat.CombatUpgradeManager;
import com.moremod.upgrades.energy.EnergyUpgradeManager;
import com.moremod.upgrades.survival.SurvivalUpgradeManager;

// 飾品盒系統導入

// 装瓶机系统导入
import com.moremod.block.BlockBottlingMachine;
import com.moremod.tile.TileEntityBottlingMachine;

import com.moremod.recipe.BottlingMachineRecipe;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.init.Items;

// Mixin 相关导入
import com.moremod.world.SpacetimeOreWorldGenerator;
import com.moremod.world.VoidStructureWorldGenerator;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.Loader;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.GeckoLib;

// ========== 新增：渲染注册导入 ==========
import net.minecraftforge.fml.client.registry.RenderingRegistry;
// ========================================

/* ===================== Ritual Integration: imports (1.12.2) ===================== */
import com.moremod.block.BlockRitualCore;
import com.moremod.block.BlockPedestal;
import com.moremod.tile.TileEntityRitualCore;
import com.moremod.tile.TileEntityPedestal;
import com.moremod.ritual.RitualInfusionAPI;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;

import net.minecraftforge.client.model.ModelLoader;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.dhanantry.scapeandrunparasites.SRPMain.network;
/* ================================================================================ */

/**
 * moremod 主类 - 完整集成版（包含装瓶机系统和剑气实体）
 */
@Mod(
        modid = moremod.MODID,
        name = moremod.NAME,
        version = moremod.VERSION,
        dependencies = "required-after:fermiumbooter;required-after:baubles;after:srparasites;after:lycanitesmobs"
)
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class moremod {

    public static final String MODID = "moremod";
    public static final String NAME = "More Mod";
    public static final String VERSION = "3.2.2";

    @Instance(MODID)
    public static moremod instance;

    @SidedProxy(
            clientSide = "com.moremod.proxy.ClientProxy",
            serverSide = "com.moremod.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.Instance(MODID)
    public static moremod INSTANCE;

    // GUI Handler
    private static final GuiHandler guiHandler = new GuiHandler();

    /* ===================== Ritual Integration: fields ===================== */
    public static Block RITUAL_CORE_BLOCK;
    public static Block RITUAL_PEDESTAL_BLOCK;
    /* ===================================================================== */

    /* ===================== 装瓶机系统字段 ===================== */
    public static Block BOTTLING_MACHINE_BLOCK;
    private static boolean enableAutoBottlingRecipes = true;  // 配置选项
    /* ========================================================== */

    /* ===================== Synergy链结器系统字段 ===================== */
    public static Block SYNERGY_LINKER_BLOCK;
    /* ========================================================== */

    /**
     * 构造阶段 - 加载可选的 Mixin 配置
     */
    @EventHandler
    public void construct(FMLConstructionEvent event) {
        System.out.println("[moremod] ========== 构造阶段 ==========");

        // 加载 SRP 相关的 Mixin（如果模组存在）
        if (Loader.isModLoaded("srparasites")) {
            try {
                System.out.println("[moremod] 检测到 SRParasites，加载 SRP Mixins...");
                System.out.println("[moremod] ✅ SRP Mixins 加载成功");
            } catch (Exception e) {
                System.err.println("[moremod] ❌ SRP Mixins 加载失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("[moremod] 未检测到 SRParasites，跳过 SRP Mixins");
        }

        // 加载 Lycanites 相关的 Mixin（如果模组存在）
        if (Loader.isModLoaded("lycanitesmobs")) {
            try {
                System.out.println("[moremod] 检测到 LycanitesMobs，加载 Lycanites Mixins...");
                System.out.println("[moremod] ✅ Lycanites Mixins 加载成功");
            } catch (Exception e) {
                System.err.println("[moremod] ❌ Lycanites Mixins 加载失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("[moremod] 未检测到 LycanitesMobs，跳过 Lycanites Mixins");
        }

        System.out.println("[moremod] ========== 构造阶段完成 ==========\n");
    }

    /**
     * 预初始化阶段
     */
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        System.out.println("[moremod] ========== 开始预初始化 ==========");
        CompleteSanitySystem.registerRecipes();
        network.registerMessage(PacketCreateEnchantedBook.Handler.class,
                PacketCreateEnchantedBook.class, 0, Side.SERVER);
        ItemConfig.init(event);  // 第一行就初始化配置
        RSNodeRegistryCompat.registerAll();
        UnlockableSlotsInit.preInit(event);
        AutoAttackCapabilityHandler.registerCapability();
        ChengYueCapabilityHandler.register();

        // 2. 注册澄月的事件处理器
        MinecraftForge.EVENT_BUS.register(new ChengYueCapabilityHandler());
        MinecraftForge.EVENT_BUS.register(new ChengYueEventHandler());
        // ========== 配置系统初始化（最先执行）==========
        System.out.println("[moremod] 📄 初始化配置系统...");
        EnergyBalanceConfig.init();
        ModConfig.updateEnergyBalanceConfig();
        EquipmentTimeConfig.initialize();

        MinecraftForge.EVENT_BUS.register(ModConfig.EventHandler.class);
        System.out.println("[moremod] ✅ 配置系统初始化完成");
        SetBonusAccessoryBoxCompat.runFullTest();

        // ========== 飾品盒系統初始化 ==========
        System.out.println("[moremod] 📦 初始化飾品盒系統...");
        System.out.println("[moremod] 📦 Extending Baubles slots to 37!");
        System.out.println("[moremod] ✅ 飾品盒系統預初始化完成");

        // ========== 实体注册 ==========
        System.out.println("[moremod] 🎭 开始注册实体...");
        registerEntities();
        System.out.println("[moremod] ✅ 实体注册完成");

        // ========== 核心系统注册 ==========
        System.out.println("[moremod] ✅ 音效系统注册完成");

        // 初始化网络通信
        initNetworkPackets();
        PacketHandler.registerMessages();
        NetworkHandler.init();
        System.out.println("[moremod] ✅ 网络通信系统注册完成");

        // 注册 Capability
        CapabilityManager.INSTANCE.register(
                IPlayerTimeData.class,
                new PlayerTimeDataStorage(),
                PlayerTimeDataImpl::new
        );
        System.out.println("[moremod] ✅ 时光之心Capability注册完成");

        // ========== Ritual 多方块：创建实例（不在这里注册）==========
        System.out.println("[moremod] 🔮 创建 Ritual 多方块实例...");
        RITUAL_CORE_BLOCK = new BlockRitualCore().setRegistryName(MODID, "ritual_core").setTranslationKey("ritual_core");
        RITUAL_PEDESTAL_BLOCK = new BlockPedestal().setRegistryName(MODID, "ritual_pedestal").setTranslationKey("ritual_pedestal");

        // 注册 TileEntity
        GameRegistry.registerTileEntity(TileEntityRitualCore.class, new ResourceLocation(MODID, "ritual_core"));
        GameRegistry.registerTileEntity(TileEntityPedestal.class, new ResourceLocation(MODID, "ritual_pedestal"));
        System.out.println("[moremod] ✅ Ritual TileEntity 注册完成");

        // ========== 装瓶机系统：创建实例和注册TileEntity ==========
        System.out.println("[moremod] 🏭 创建装瓶机实例...");
        BOTTLING_MACHINE_BLOCK = new BlockBottlingMachine()
                .setRegistryName(MODID, "bottling_machine")
                .setTranslationKey("bottling_machine");

        // 注册装瓶机 TileEntity
        GameRegistry.registerTileEntity(TileEntityBottlingMachine.class,
                new ResourceLocation(MODID, "bottling_machine"));
        System.out.println("[moremod] ✅ 装瓶机 TileEntity 注册完成");

        // ========== Synergy链结器系统：创建实例和注册TileEntity ==========
        System.out.println("[moremod] 🔗 创建 Synergy 链结器实例...");
        SYNERGY_LINKER_BLOCK = new com.moremod.synergy.block.BlockSynergyLinker()
                .setRegistryName(MODID, "synergy_linker")
                .setTranslationKey("synergy_linker");

        // 注册 Synergy 链结器 TileEntity
        GameRegistry.registerTileEntity(com.moremod.synergy.tile.TileEntitySynergyLinker.class,
                new ResourceLocation(MODID, "synergy_linker"));
        System.out.println("[moremod] ✅ Synergy 链结器 TileEntity 注册完成");

        // 初始化 Synergy 系统
        System.out.println("[moremod] 🔗 初始化 Synergy 系统...");
        com.moremod.synergy.init.SynergyBootstrap.initialize();
        System.out.println("[moremod] ✅ Synergy 系统初始化完成");

        // ========== 客户端专用注册 ==========
        if (event.getSide().isClient()) {
            registerClientSystems();
        }

        // ========== 通用事件处理器注册 ==========
        registerCommonEventHandlers();

        // 注册能量惩罚和其他系统
        MinecraftForge.EVENT_BUS.register(EnergyPunishmentSystem.class);
        MinecraftForge.EVENT_BUS.register(MechanicalExoskeletonEventHandler.class);
        MinecraftForge.EVENT_BUS.register(new DimensionalRipperEventHandler());
        MinecraftForge.EVENT_BUS.register(new OtherworldAttackEvent());
        MinecraftForge.EVENT_BUS.register(new CurseSpreadHandler());

        // 註冊飾品盒事件處理器
        System.out.println("[moremod] 📦 飾品盒事件處理器注册成功");

        proxy.preInit(event);

        System.out.println("[moremod] ========== 预初始化完成 ==========\n");
    }

    // ========================================
    // 新增：统一的实体注册方法
    // ========================================
    private void registerEntities() {
        int nextEntityId = 0;

        // 传送门实体
        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "rift_portal"),
                EntityRiftPortal.class,
                "rift_portal",
                nextEntityId++,        // 0
                INSTANCE,
                64,                    // tracking range
                1,                     // update frequency
                false                  // sendVelocityUpdates
        );

        // Boss 实体
        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "rift_warden"),
                EntityRiftwarden.class,
                "rift_warden",
                nextEntityId++,        // 1
                INSTANCE,
                64, 1, false
        );

        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "weeping_angel"),
                EntityWeepingAngel.class,
                "weeping_angel",
                nextEntityId++,        // 2
                INSTANCE,
                64, 1, false
        );

        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "curse_knight"),
                EntityCursedKnight.class,
                "curse_knight",
                nextEntityId++,        // 3
                INSTANCE,
                64, 1, false
        );

        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "void_ripper"),
                EntityVoidRipper.class,
                "void_ripper",
                nextEntityId++,        // 4
                INSTANCE,
                64, 1, false
        );

        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "stone_sentinel"),
                EntityStoneSentinel.class,
                "stone_sentinel",
                nextEntityId++,        // 5
                INSTANCE,
                64, 1, false
        );

        // 抛射物实体
        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "void_bullet"),
                EntityVoidBullet.class,
                "void_bullet",
                nextEntityId++,        // 6
                INSTANCE,
                64, 1, false
        );

        // 特效实体
        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "laser_beam"),
                EntityLaserBeam.class,
                "laser_beam",
                nextEntityId++,       // 7
                MODID,
                64, 1, true
        );

        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "lightning_arc"),
                EntityLightningArc.class,
                "lightning_arc",
                nextEntityId++,       // 8
                MODID,
                64, 1, true
        );

        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "void_portal"),
                EntityVoidPortal.class,
                "void_portal",
                nextEntityId++,        // 9
                INSTANCE,
                64, 1, true
        );

        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "lighting_orb"),
                EntityRiftLightning.class,
                "lighting_orb",
                nextEntityId++,        // 10
                INSTANCE,
                64, 1, true
        );

        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "PlayerLaser"),
                EntityPlayerLaserBeam.class,
                "PlayerLaser",
                nextEntityId++,        // 11
                INSTANCE,
                64, 1, true
        );

        // ========================================
        // 新增：剑气实体注册
        // ========================================
        EntityRegistry.registerModEntity(
                new ResourceLocation(MODID, "sword_beam"),
                EntitySwordBeam.class,
                "sword_beam",
                nextEntityId++,        // 12
                INSTANCE,
                64,                    // 追踪范围
                3,                     // 更新频率（更频繁以保证流畅）
                true                   // 发送速度更新
        );
        System.out.println("[moremod] ⚔️ 剑气实体注册成功 (ID: " + (nextEntityId - 1) + ")");
        // ========================================
    }

    /**
     * 方块注册事件（使用事件驱动的注册方式）
     */
    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        System.out.println("[moremod] 🔮 注册 Ritual 方块...");
        event.getRegistry().registerAll(
                RITUAL_CORE_BLOCK,
                RITUAL_PEDESTAL_BLOCK
        );
        System.out.println("[moremod] ✅ Ritual 方块注册完成");

        // 注册装瓶机方块
        System.out.println("[moremod] 🏭 注册装瓶机方块...");
        event.getRegistry().register(BOTTLING_MACHINE_BLOCK);
        System.out.println("[moremod] ✅ 装瓶机方块注册完成");

        // 注册 Synergy 链结器方块
        System.out.println("[moremod] 🔗 注册 Synergy 链结器方块...");
        event.getRegistry().register(SYNERGY_LINKER_BLOCK);
        System.out.println("[moremod] ✅ Synergy 链结器方块注册完成");
    }

    /**
     * 物品注册事件（使用事件驱动的注册方式）
     */
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        System.out.println("[moremod] 🔮 注册 Ritual 方块物品...");
        event.getRegistry().registerAll(
                new ItemBlock(RITUAL_CORE_BLOCK).setRegistryName(RITUAL_CORE_BLOCK.getRegistryName()),
                new ItemBlock(RITUAL_PEDESTAL_BLOCK).setRegistryName(RITUAL_PEDESTAL_BLOCK.getRegistryName())
        );
        System.out.println("[moremod] ✅ Ritual 方块物品注册完成");

        // 注册装瓶机方块物品
        System.out.println("[moremod] 🏭 注册装瓶机方块物品...");
        event.getRegistry().register(
                new ItemBlock(BOTTLING_MACHINE_BLOCK).setRegistryName(BOTTLING_MACHINE_BLOCK.getRegistryName())
        );
        System.out.println("[moremod] ✅ 装瓶机方块物品注册完成");

        // 注册 Synergy 链结器方块物品
        System.out.println("[moremod] 🔗 注册 Synergy 链结器方块物品...");
        event.getRegistry().register(
                new ItemBlock(SYNERGY_LINKER_BLOCK).setRegistryName(SYNERGY_LINKER_BLOCK.getRegistryName())
        );
        System.out.println("[moremod] ✅ Synergy 链结器方块物品注册完成");
    }

    /**
     * 初始化网络数据包
     */
    private void initNetworkPackets() {
        System.out.println("[moremod] ✅ 网络包初始化");
    }



    /**
     * 初始化阶段
     */
    @EventHandler
    public void init(FMLInitializationEvent event) {
// 在主模组类或ClientProxy中

            // 注册事件处理器
            MinecraftForge.EVENT_BUS.register(new FleshRejectionEnvironmentHandler());
            MinecraftForge.EVENT_BUS.register(new FleshRejectionEventSystem());
            MinecraftForge.EVENT_BUS.register(new RejectionSleepDecaySystem());
            MinecraftForge.EVENT_BUS.register(new RejectionPotionPenaltySystem());
            MinecraftForge.EVENT_BUS.register(new FleshRejectionFirstAidHooks());

            // 客户端注册
            if (event.getSide().isClient()) {
                MinecraftForge.EVENT_BUS.register(new EventHUDOverlay());
                MinecraftForge.EVENT_BUS.register(new SmartRejectionGuide());
            }

        System.out.println("[moremod] ========== 开始初始化 ==========");
        UnlockableSlotsInit.init(event);
        RuleChecker.initialize();
        GemSystemInit.init(event);
        // 注册维度类型
        PersonalDimensionType.registerDimension();
        System.out.println("[moremod] ✅ 维度类型注册完成");
        EquipmentTimeTracker.register();

        // 初始化维度管理器
        PersonalDimensionManager.init();
        System.out.println("[moremod] ✅ 私人空间管理器初始化完成");

        // 注册事件处理器
        ItemDimensionalRipper.initChunkLoading();
        MinecraftForge.EVENT_BUS.register(new PersonalDimensionSpawnHandler());
        MinecraftForge.EVENT_BUS.register(PersonalDimensionManager.class);

        // 注册虚空结构生成器事件
        MinecraftForge.EVENT_BUS.register(com.moremod.dimension.VoidStructureGenerator.class);
        System.out.println("[moremod] ✅ 虚空结构生成器注册完成");

        // 世界生成器
        GameRegistry.registerWorldGenerator(new SpacetimeOreWorldGenerator(), 5);
        GameRegistry.registerWorldGenerator(new VoidStructureWorldGenerator(), 1000);

        // 其他初始化
        ItemMechanicalCore.registerEnergyGenerationEvents();
        GeckoLib.initialize();
        ModConfig.updateEnergyBalanceConfig();

        // GUI - 注意：你的GuiHandler需要添加装瓶机的GUI处理
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        System.out.println("[moremod] ✅ GUI處理器注册完成（包含飾品盒和装瓶机）");

        // 客户端渲染
        if (event.getSide().isClient()) {
            RenderHandler.registerLayers();
            System.out.println("[moremod] ✅ 喷气背包渲染层注册完成");
        }

        /* ===== Ritual 多方块：配方注册 ===== */
        registerRitualRecipes();
        UniversalFabricRituals.registerRituals();

        /* ===== 装瓶机：注册基础配方 ===== */
        registerBottlingMachineRecipes();

        proxy.init(event);

        System.out.println("[moremod] ========== 初始化完成 ==========\n");
    }

    /**
     * 注册Ritual配方
     */
    private void registerRitualRecipes() {
        System.out.println("[moremod] ✅ Ritual 配方注册完成，共 " + RitualInfusionAPI.RITUAL_RECIPES.size() + " 个配方");
    }

    /**
     * 注册装瓶机基础配方
     */
    private void registerBottlingMachineRecipes() {
        System.out.println("[moremod] 🏭 注册装瓶机基础配方...");

        // 水瓶配方
        BottlingMachineRecipe.addRecipe(
                new ItemStack(Items.POTIONITEM, 1, 0), // 水瓶
                new ItemStack(Items.GLASS_BOTTLE), 1,
                new FluidStack(FluidRegistry.WATER, 250)
        );

        // 水桶配方
        BottlingMachineRecipe.addRecipe(
                new ItemStack(Items.WATER_BUCKET),
                new ItemStack(Items.BUCKET), 1,
                new FluidStack(FluidRegistry.WATER, 1000)
        );

        // 岩浆桶配方
        BottlingMachineRecipe.addRecipe(
                new ItemStack(Items.LAVA_BUCKET),
                new ItemStack(Items.BUCKET), 1,
                new FluidStack(FluidRegistry.LAVA, 1000)
        );

        System.out.println("[moremod] ✅ 装瓶机基础配方注册完成");
    }

    /**
     * 后初始化阶段
     */
    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        System.out.println("[moremod] ========== 开始后初始化 ==========");
        JEIIntegrationManager.init(event);

        if (event.getSide().isClient()) {
            AuxiliaryUpgradeManager.OreVisionSystem.initializeOreDictionary();
            System.out.println("[moremod] ✅ 矿物词典后初始化完成");
        }
        RitualRecipeLoader.loadRecipes();
        DimensionLoomRecipeLoader.loadRecipes();  // 织机配方
     //   MinecraftForge.EVENT_BUS.register(new RenderDebugKeyHandler());

        ModIntegration.postInit();

        // ========== 装瓶机自动配方注册 ==========
        if (enableAutoBottlingRecipes) {
            // 原有的自动扫描（可选）
            // AutoBottlingRecipeManager.registerAutoRecipes();

            // 新增：精简版反向推导
            SimpleReverseDeducer.deduceRecipes();
        }

        // 在这里注册模组特有物品的配方
        try {
            System.out.println("[moremod] 🔮 后初始化：注册模组配方...");

            // 检查物品是否存在再注册配方
            if (Item.getByNameOrId("moremod:spacetime_fabric") != null &&
                    Item.getByNameOrId("moremod:chrono_fabric") != null) {
                System.out.println("[moremod] ✅ 模组专属配方注册成功");
            }
        } catch (Exception e) {
            System.err.println("[moremod] ⚠️ 模组配方注册失败: " + e.getMessage());
        }

        System.out.println("[moremod] ========== 后初始化完成 ==========\n");
    }

    /**
     * 服务器即将启动事件
     */
    @EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        System.out.println("[moremod] ========== 服务器即将启动 ==========");

        VoidStructureWorldGenerator.clearAllData();
        System.out.println("[moremod] ✅ 清理上次会话的数据");

        System.out.println("[moremod] ========== 准备完成 ==========\n");
    }

    /**
     * 服务器启动事件
     */
    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        System.out.println("[moremod] ========== 服务器启动中 ==========");
        event.registerServerCommand(new CommandLootDebug());
        event.registerServerCommand(new CommandResetEquipTime());

        ModConfig.updateEnergyBalanceConfig();
        System.out.println("[moremod] ✅ 服务器配置已加载");

        PersonalDimensionType.ensureDimensionExists();
        System.out.println("[moremod] ✅ 服务器维度初始化");

        System.out.println("[moremod] ========== 服务器启动完成 ==========\n");
    }

    /**
     * 服务器停止事件
     */
    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        System.out.println("[moremod] ========== 服务器停止中 ==========");

        PersonalDimensionManager.savePlayerSpaces();
        System.out.println("[moremod] ✅ 私人维度数据已保存");
        PersonalDimensionManager.reset();
        VoidStructureWorldGenerator.clearAllData();
        System.out.println("[moremod] ✅ 虚空结构数据已清理");

        System.out.println("[moremod] ========== 服务器停止完成 ==========\n");
    }

    // ========================================
    // 修改：客户端系统注册（添加剑气渲染器）
    // ========================================
    @SideOnly(Side.CLIENT)
    private void registerClientSystems() {
        System.out.println("[moremod] --- 注册客户端系统 ---");
        MinecraftForge.EVENT_BUS.register(EnhancedVisualsHandler.instance);

        // 1. 喷气背包系统
        MinecraftForge.EVENT_BUS.register(new ClientTickEvent());
        JetpackKeyHandler.registerKeys();
        MinecraftForge.EVENT_BUS.register(new JetpackKeyHandler());
        MinecraftForge.EVENT_BUS.register(EventHandlerJetpack.class);
        System.out.println("[moremod] 🚀 喷气背包系统注册成功");

        // 2. 电池充电处理器
        MinecraftForge.EVENT_BUS.register(new BatteryChargeHandler());
        System.out.println("[moremod] 🔋 电池充电处理器注册成功");

        // 3. 渲染系统
        MinecraftForge.EVENT_BUS.register(new RenderHandler());
        System.out.println("[moremod] 🎨 渲染系统注册成功");

        // 4. 机械核心网络处理器
        MinecraftForge.EVENT_BUS.register(MechanicalCoreNetworkHandler.class);
        System.out.println("[moremod] ⚙️ 机械核心网络处理器注册成功");

        // 5. 按键绑定系统
        KeyBindHandler.init();
        MinecraftForge.EVENT_BUS.register(KeyBindHandler.class);
        System.out.println("[moremod] ⌨️ 按键系统注册成功");

        // 6. 矿物透视渲染系统
        MinecraftForge.EVENT_BUS.register(AuxiliaryUpgradeManager.OreVisionSystem.class);
        AuxiliaryUpgradeManager.OreVisionSystem.initializeOreDictionary();
        System.out.println("[moremod] ⛏️ 矿物透视系统注册成功");

        // ========================================
        // 新增：剑气渲染器注册
        // ========================================
        RenderingRegistry.registerEntityRenderingHandler(
                EntitySwordBeam.class,
                RenderSwordBeam::new
        );
        System.out.println("[moremod] ⚔️ 剑气渲染器注册成功");
        // ========================================

        /* === Ritual: 绑定方块物品模型 === */
        try {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(RITUAL_CORE_BLOCK), 0,
                    new ModelResourceLocation(RITUAL_CORE_BLOCK.getRegistryName(), "inventory"));
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(RITUAL_PEDESTAL_BLOCK), 0,
                    new ModelResourceLocation(RITUAL_PEDESTAL_BLOCK.getRegistryName(), "inventory"));
            System.out.println("[moremod] 🎭 Ritual 多方块物品模型已绑定");

            // 绑定装瓶机模型
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(BOTTLING_MACHINE_BLOCK), 0,
                    new ModelResourceLocation(BOTTLING_MACHINE_BLOCK.getRegistryName(), "inventory"));
            System.out.println("[moremod] 🏭 装瓶机物品模型已绑定");

            // 绑定 Synergy 链结器模型
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(SYNERGY_LINKER_BLOCK), 0,
                    new ModelResourceLocation(SYNERGY_LINKER_BLOCK.getRegistryName(), "inventory"));
            System.out.println("[moremod] 🔗 Synergy 链结器物品模型已绑定");
        } catch (Throwable t) {
            System.err.println("[moremod] ⚠️ 模型绑定失败： " + t.getMessage());
        }
    }

    /**
     * 注册通用事件处理器
     */
    private void registerCommonEventHandlers() {
        System.out.println("[moremod] --- 注册通用事件处理器 ---");

        // 1. 机械心脏系统
        MinecraftForge.EVENT_BUS.register(new MechanicalHeartEventHandler());
        System.out.println("[moremod] 💓 机械心脏事件处理器注册成功");
        MinecraftForge.EVENT_BUS.register(new PotionCoreCompatEnhanced());
        MinecraftForge.EVENT_BUS.register(new BleedEventHandler());



// 在主类/代理类中
        MinecraftForge.EVENT_BUS.register(new ChengYueEventHandler());
        // 2. 创造电池充电处理器
        MinecraftForge.EVENT_BUS.register(new CreativeBatteryChargeHandler());
        System.out.println("[moremod] 🌟 创造电池充电处理器注册成功");
        MinecraftForge.EVENT_BUS.register(new SpatialFabricFirstAidHandler());
        MinecraftForge.EVENT_BUS.register(new WisdomFountainEventHandler());

        // 3. 时光之心系统
        MinecraftForge.EVENT_BUS.register(PlayerTimeDataCapability.class);
        MinecraftForge.EVENT_BUS.register(ServerTickHandler.class);
        System.out.println("[moremod] 🕰️ 时光之心系统注册成功");


        // 4. 升级管理器系统
        registerUpgradeManagers();

        // 5. 机械核心系统
        registerMechanicalCoreSystem();
        MinecraftForge.EVENT_BUS.register(EnergyUpgradeManager.CombatChargerSystem.class);

        // 6. 附魔增强系统
        MinecraftForge.EVENT_BUS.register(EnchantmentHandler.class);
        System.out.println("[moremod] ✨ 附魔增强处理器注册成功");

        // 7. 经验增幅系统
        MinecraftForge.EVENT_BUS.register(AuxiliaryUpgradeManager.ExpAmplifierSystem.class);
        System.out.println("[moremod] 💎 经验增幅系统注册成功");

        // 8. 玩家开局礼包系统
        MinecraftForge.EVENT_BUS.register(PlayerStarterKitHandler.class);
        System.out.println("[moremod] 🎁 开局礼包系统注册成功");
    }

    /**
     * 注册升级管理器
     */
    private void registerUpgradeManagers() {
        try {
            MinecraftForge.EVENT_BUS.register(SurvivalUpgradeManager.class);
            System.out.println("[moremod] 🛡️ 生存升级管理器注册成功");
        } catch (Exception e) {
            System.err.println("[moremod] ❌ 生存升级管理器注册失败: " + e.getMessage());
        }

        try {
            MinecraftForge.EVENT_BUS.register(AuxiliaryUpgradeManager.class);
            System.out.println("[moremod] 🔧 辅助升级管理器注册成功");
        } catch (Exception e) {
            System.err.println("[moremod] ❌ 辅助升级管理器注册失败: " + e.getMessage());
        }

        try {
            MinecraftForge.EVENT_BUS.register(CombatUpgradeManager.class);
            System.out.println("[moremod] ⚔️ 战斗升级管理器注册成功");
        } catch (Exception e) {
            System.err.println("[moremod] ❌ 战斗升级管理器注册失败: " + e.getMessage());
        }

        try {
            MinecraftForge.EVENT_BUS.register(EnergyUpgradeManager.class);
            System.out.println("[moremod] ⚡ 能量升级管理器注册成功");
        } catch (Exception e) {
            System.err.println("[moremod] ❌ 能量升级管理器注册失败: " + e.getMessage());
        }
    }

    /**
     * 注册机械核心系统
     */
    private void registerMechanicalCoreSystem() {
        try {
            MinecraftForge.EVENT_BUS.register(com.moremod.handler.LootTableHandler.class);
            MinecraftForge.EVENT_BUS.register(new SimpleCoreHandler());
            MinecraftForge.EVENT_BUS.register(new CoreDropProtection());
            MinecraftForge.EVENT_BUS.register(new SmartUpgradeHandler());
            System.out.println("[moremod] ⚙️ 机械核心系统注册成功");
        } catch (Exception e) {
            System.err.println("[moremod] ❌ 机械核心系统注册失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}