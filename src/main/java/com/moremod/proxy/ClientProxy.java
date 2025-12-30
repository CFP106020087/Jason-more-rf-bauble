package com.moremod.proxy;

import com.moremod.client.ClientEventHandler;
import com.moremod.client.ClientHandlerPacketSyncRejectionData;
import com.moremod.client.gui.EventHUDOverlay;
import com.moremod.client.gui.StoryOverlayRenderer;
import com.moremod.client.render.*;
import com.moremod.client.render.debug.*;
import com.moremod.client.render.fx.RenderLaserBeam;
import com.moremod.client.render.fx.RenderLightningArc;
import com.moremod.client.render.fx.RenderPlayerLaserBeam;
import com.moremod.client.render.fx.RenderRiftLightning;
import com.moremod.entity.*;
import com.moremod.entity.boss.EntityRiftwarden;
import com.moremod.entity.boss.EntityStoneSentinel;
import com.moremod.entity.fx.EntityLaserBeam;
import com.moremod.entity.fx.EntityLightningArc;
import com.moremod.entity.fx.EntityPlayerLaserBeam;
import com.moremod.entity.fx.EntityRiftLightning;
import com.moremod.entity.projectile.EntityVoidBullet;
import com.moremod.init.ModItems;
import com.moremod.item.ItemHeroSword;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemSawBladeSword;
import com.moremod.item.ItemSwordChengYue;
// ✨ 新增导入：锯刃剑渲染层
import com.moremod.item.sawblade.client.BloodEuphoriaRenderer;
import com.moremod.moremod;
import com.moremod.printer.TileEntityPrinter;
import com.moremod.accessorybox.EarlyConfigLoader;
import com.moremod.sponsor.client.SponsorKeyBindings;
import com.moremod.network.PacketHandler;
import com.moremod.network.PacketSyncRejectionData;
import com.moremod.tile.TileEntityPedestal;
import com.moremod.tile.TileEntityProtectionField;
import com.moremod.tile.TileEntityRitualCore;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;

import static com.moremod.init.ModBlocks.SWORD_UPGRADE_STATION;
import static com.moremod.init.ModItems.SWORD_CHENGYUE;
import static com.moremod.item.RegisterItem.*;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = moremod.MODID)
public class ClientProxy extends CommonProxy {

    private static boolean teisrBound = false;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPrinter.class, new TileEntityRendererPrinter());
        MinecraftForge.EVENT_BUS.register(new EventHUDOverlay());

        // 注册 OBJLoader 域名
        OBJLoader.INSTANCE.addDomain(moremod.MODID);
        System.out.println("[moremod] OBJLoader domain registered");

        // GeckoLib init (1.12.2 requires manual call)
        try {
            software.bernie.geckolib3.GeckoLib.initialize();
            System.out.println("[moremod] GeckoLib.initialize() OK");
        } catch (Throwable t) {
            System.err.println("[moremod] GeckoLib.initialize() FAILED");
            t.printStackTrace();
        }

        // 澄月剑优先绑定
        tryBindSwordTEISR("preInit");

        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());

        // TileEntity 渲染器
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityRitualCore.class, new TileEntityRitualCoreRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPedestal.class, new TileEntityPedestalRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityProtectionField.class, new TESRProtectionField());

        // 打印机渲染器 - 由用户手动实现

        // 赞助者物品快捷键（仅在诛仙剑启用时注册）
        if (EarlyConfigLoader.isZhuxianSwordEnabled()) {
            SponsorKeyBindings.registerKeyBindings();
        } else {
            System.out.println("[moremod] 诛仙剑已禁用，跳过快捷键注册");
        }

        registerEntityRenderers();
    }
    @Override
    public void registerNetworkMessages() {

        int id = PacketHandler.nextId();

        PacketHandler.INSTANCE.registerMessage(
                ClientHandlerPacketSyncRejectionData.class,
                PacketSyncRejectionData.class,
                id,
                Side.CLIENT
        );
    }
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MoBendsCompat.init();
        // =========================
        // 注册 NumPad Debug 按键
        // =========================
        //RenderDebugKeys.register();
        MinecraftForge.EVENT_BUS.register(RenderDebugKeys.class);
        MinecraftForge.EVENT_BUS.register(new RenderDebugKeyHandler());
        MinecraftForge.EVENT_BUS.register(new StoryOverlayRenderer());
        // =========================
        // 给锯刃剑绑定 Debug TEISR
        // =========================
        ItemSawBladeSword sawBlade = ModItems.SAW_BLADE_SWORD;
        if (sawBlade != null) {
            sawBlade.setTileEntityItemStackRenderer(SawBladeSwordRenderer.INSTANCE);
            System.out.println("[moremod] SawBladeSword Debug TEISR bound");
        } else {
            System.err.println("[moremod] ERROR: SAW_BLADE_SWORD is NULL!");
        }
        ItemHeroSword heroSword = ModItems.HERO_SWORD;
        if (heroSword != null) {
            heroSword.setTileEntityItemStackRenderer(HeroSwordRenderer.INSTANCE);
            System.out.println("[moremod] HeroSword Debug TEISR bound");
        } else {
            System.err.println("[moremod] ERROR: HERO_SWORD is NULL!");
        }
        RenderDebugConfig.loadConfig();

        tryBindSwordTEISR("init");

        ItemMechanicalCore.registerEnergyGenerationEvents();
        MinecraftForge.EVENT_BUS.register(new JetpackBaubleRenderer());

        registerMechanicalCoreLayer();
        registerMechanicalExoskeletonLayer();

        // ✨ 新增：注册锯刃剑鲜血欢愉渲染层
        registerBloodEuphoriaLayer();

        // ✨ 新增：注册人性值机械化叠加渲染层
        registerMechanicalOverlayLayer();

        ClientEventHandler.init(event);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);

        tryBindSwordTEISR("postInit");

        if (!teisrBound) {
            System.err.println("[moremod] WARNING: ChengYue TEISR not bound!");
        }
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        tryBindSwordTEISR("ModelRegistryEvent");

        registerEnergySwordModelsSafe();
        registerJetpackModelsSafe();
        registerMechanicalCoreModelsSafe();
        registerCoreMaterialsModelsSafe();
        registerSwordChengYueModelsSafe();
        registerBlockModelsSafe();
    }

    private static void tryBindSwordTEISR(String phase) {
        if (teisrBound) return;

        Item sword = SWORD_CHENGYUE;
        if (sword == null) sword = Item.getByNameOrId("moremod:sword_chengyue");
        if (sword == null) {
            System.err.println("[moremod] SWORD_CHENGYUE null in " + phase);
            return;
        }

        bindTEISR(sword, phase);
    }

    private static void bindTEISR(Item sword, String phase) {
        if (teisrBound) return;

        try {
            if (sword instanceof ItemSwordChengYue) {
                ((ItemSwordChengYue) sword).registerISTER();
                teisrBound = true;
                System.out.println("[moremod] ChengYue TEISR bound via registerISTER() @" + phase);
                return;
            }

            sword.setTileEntityItemStackRenderer(new SwordChengYueRenderer());
            teisrBound = true;
            System.out.println("[moremod] ChengYue TEISR bound fallback @" + phase);

        } catch (Throwable t) {
            System.err.println("[moremod] bindTEISR failed @" + phase);
            t.printStackTrace();
        }
    }

    // ===== 实体渲染 =====
    private static void registerEntityRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityRiftwarden.class, RenderRiftwarden::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityRiftLightning.class, RenderRiftLightning::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityWeepingAngel.class, RenderWeepingAngel::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityVoidRipper.class, RenderVoidRipper::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityPlayerLaserBeam.class, RenderPlayerLaserBeam::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityVoidPortal.class, RenderVoidPortal2x1::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityCursedKnight.class, RenderCursedKnight::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityVoidBullet.class, RenderVoidBullet::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityRiftPortal.class, RenderRiftPortal::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityStoneSentinel.class, RenderStoneSentinel::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityLaserBeam.class, RenderLaserBeam::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityLightningArc.class, RenderLightningArc::new);
        RenderingRegistry.registerEntityRenderingHandler(EntitySwordBeam.class, RenderSwordBeam::new);
        RenderingRegistry.registerEntityRenderingHandler(com.moremod.entity.fx.EntitySingularity.class, com.moremod.client.render.fx.RenderSingularity::new);
        // 投掷胶囊渲染器
        RenderingRegistry.registerEntityRenderingHandler(EntityThrownCapsule.class, RenderThrownCapsule::new);
    }

    // ===== Player Layer System =====
    private static void registerMechanicalCoreLayer() {
        try {
            Map<String, RenderPlayer> map = Minecraft.getMinecraft().getRenderManager().getSkinMap();
            RenderLayerMechanicalCore layer = new RenderLayerMechanicalCore();
            map.values().forEach(r -> r.addLayer(layer));
        } catch (Throwable ignored) {}
    }

    private static void registerMechanicalExoskeletonLayer() {}


    private void registerMechanicalOverlayLayer() {
        Map<String, RenderPlayer> skinMap = Minecraft.getMinecraft().getRenderManager().getSkinMap();

        // "default" = Steve 模型（4像素手臂）
        // "slim" = Alex 模型（3像素手臂）
        for (RenderPlayer renderPlayer : skinMap.values()) {
            renderPlayer.addLayer(new LayerMechanicalOverlay(renderPlayer));
        }
    }

    // ✨ 新增：注册鲜血欢愉渲染层（玩家红色光晕）
    /**
     * 注册鲜血欢愉渲染层
     * 为玩家添加红色光晕效果
     */
    private static void registerBloodEuphoriaLayer() {
        try {
            // 方法1：使用BloodEuphoriaRenderer的静态方法（推荐）
            BloodEuphoriaRenderer.registerLayer();
            System.out.println("[MoreMod] Blood Euphoria Renderer registered!");

        } catch (Exception e) {
            System.err.println("[MoreMod] Failed to register Blood Euphoria Renderer (method 1), trying fallback...");

            // 方法2：手动注册（备用方案）
            try {
                Map<String, RenderPlayer> skinMap = Minecraft.getMinecraft().getRenderManager().getSkinMap();

                // 注册到默认皮肤
                RenderPlayer renderPlayerDefault = skinMap.get("default");
                if (renderPlayerDefault != null) {
                    renderPlayerDefault.addLayer(new BloodEuphoriaRenderer(renderPlayerDefault));
                    System.out.println("[MoreMod] Blood Euphoria Renderer added to default skin");
                }

                // 注册到纤细皮肤
                RenderPlayer renderPlayerSlim = skinMap.get("slim");
                if (renderPlayerSlim != null) {
                    renderPlayerSlim.addLayer(new BloodEuphoriaRenderer(renderPlayerSlim));
                    System.out.println("[MoreMod] Blood Euphoria Renderer added to slim skin");
                }

                System.out.println("[MoreMod] Blood Euphoria Renderer registered (fallback method)!");

            } catch (Exception e2) {
                System.err.println("[MoreMod] CRITICAL: Failed to register Blood Euphoria Renderer!");
                e2.printStackTrace();
            }
        }
    }

    // ===== 模型注册 =====
    private static void registerBlockModelsSafe() {
        registerBlockItemModel(SWORD_UPGRADE_STATION);
    }

    private static void registerBlockItemModel(Block block) {
        if (block == null) return;
        Item item = Item.getItemFromBlock(block);
        if (item == null) return;

        ModelLoader.setCustomModelResourceLocation(
                item, 0,
                new ModelResourceLocation(block.getRegistryName(), "inventory")
        );
    }

    private static void registerMechanicalCoreModelsSafe() {
        if (MECHANICAL_CORE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    MECHANICAL_CORE, 0, new ModelResourceLocation("moremod:mechanical_core", "inventory"));
        }
    }

    private static void registerEnergySwordModelsSafe() {
        if (ENERGY_SWORD != null) {
            ModelLoader.setCustomModelResourceLocation(
                    ENERGY_SWORD, 0, new ModelResourceLocation("moremod:energy_sword", "inventory"));
        }
    }

    private static void registerJetpackModelsSafe() {
        if (JETPACK_T1 != null)
            ModelLoader.setCustomModelResourceLocation(JETPACK_T1, 0, new ModelResourceLocation("moremod:jetpack_t1", "inventory"));
        if (JETPACK_T2 != null)
            ModelLoader.setCustomModelResourceLocation(JETPACK_T2, 0, new ModelResourceLocation("moremod:jetpack_t2", "inventory"));
        if (JETPACK_T3 != null)
            ModelLoader.setCustomModelResourceLocation(JETPACK_T3, 0, new ModelResourceLocation("moremod:jetpack_t3", "inventory"));
    }

    private static void registerCoreMaterialsModelsSafe() {}

    private static void registerSwordChengYueModelsSafe() {
        Item sword = SWORD_CHENGYUE;
        if (sword == null) return;

        ModelLoader.setCustomModelResourceLocation(
                sword, 0, new ModelResourceLocation("moremod:sword_chengyue", "inventory"));
    }
}