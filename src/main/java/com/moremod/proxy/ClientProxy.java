package com.moremod.proxy;

import com.moremod.client.ClientEventHandler;
import com.moremod.client.render.*;
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
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemSwordChengYue;
import com.moremod.moremod;
import com.moremod.tile.TileEntityPedestal;
import com.moremod.tile.TileEntityProtectionField;
import com.moremod.tile.TileEntityRitualCore;
import net.minecraft.block.Block;  // 新增
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
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
// 新增：导入你的方块注册类（根据实际路径调整）
// import static com.moremod.init.ModBlocks.*;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = moremod.MODID)
public class ClientProxy extends CommonProxy {

    // 標記是否已經成功把 TEISR 綁到澄月劍
    private static boolean teisrBound = false;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        // GeckoLib 初始化（1.12.2 需要手動呼叫）
        try {
            software.bernie.geckolib3.GeckoLib.initialize();
            System.out.println("[moremod] GeckoLib.initialize() OK");
        } catch (Throwable t) {
            System.err.println("[moremod] GeckoLib.initialize() FAILED: " + t.getMessage());
            t.printStackTrace();
        }

        // 優先嘗試在 preInit 綁定澄月劍的 TEISR（若此時物品已註冊）
        tryBindSwordTEISR("preInit");

        // 客戶端事件處理
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());

        // TESR 綁定
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityRitualCore.class, new TileEntityRitualCoreRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPedestal.class, new TileEntityPedestalRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityProtectionField.class, new TESRProtectionField());

        // 實體渲染器註冊
        registerEntityRenderers();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // 在 init 再嘗試一次（避免載入順序造成 preInit 拿不到物品）
        tryBindSwordTEISR("init");

        // 原本的各類事件、Layer 註冊
        ItemMechanicalCore.registerEnergyGenerationEvents();
        MinecraftForge.EVENT_BUS.register(new JetpackBaubleRenderer());
        System.out.println("[moremod] Registered JetpackBaubleRenderer");

        registerMechanicalCoreLayer();
        System.out.println("[moremod] Registered MechanicalCore Render Layer");

        registerMechanicalExoskeletonLayer();
        System.out.println("[moremod] Registered MechanicalExoskeleton Render Layer");

        // 如果你的 ClientEventHandler 有 init(...) 內容
        ClientEventHandler.init(event);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        RenderPlayer steve = Minecraft.getMinecraft().getRenderManager().getSkinMap().get("default");
        RenderPlayer alex  = Minecraft.getMinecraft().getRenderManager().getSkinMap().get("slim");



        // 最后一次尝试绑定 TEISR
        tryBindSwordTEISR("postInit");
        // 給 Steve/Alex 兩種皮膚玩家加上「背鞘」Layer（顯示在背上的刀鞘）

        if (!teisrBound) {
            System.err.println("[moremod] WARNING: Failed to bind SwordChengYue TEISR in all init phases!");
        }
    }

    // ------------------------------------------------------------
    // 事件：模型註冊（這個時間點 Item 幾乎一定已經存在）
    // ------------------------------------------------------------
    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        System.out.println("[moremod] ModelRegistryEvent start");

        // 在這個事件中也嘗試綁定（保險）
        tryBindSwordTEISR("ModelRegistryEvent");

        registerEnergySwordModelsSafe();
        registerJetpackModelsSafe();
        registerMechanicalCoreModelsSafe();
        registerCoreMaterialsModelsSafe();
        registerSwordChengYueModelsSafe();
        registerBlockModelsSafe();  // 新增：注册方块模型

        System.out.println("[moremod] ModelRegistryEvent done");
    }

    // ------------------------------------------------------------
    // 劍渲染器（TEISR）綁定流程
    // ------------------------------------------------------------
    private static void tryBindSwordTEISR(String phase) {
        if (teisrBound) return;

        System.out.println("[moremod] Trying to bind SwordChengYue TEISR in " + phase + "...");

        Item sword = SWORD_CHENGYUE;
        if (sword == null) {
            // 以註冊名尋找（避免靜態域尚未賦值）
            sword = Item.getByNameOrId("moremod:sword_chengyue");
        }
        if (sword == null) {
            System.err.println("[moremod] SWORD_CHENGYUE is null in " + phase);
            return;
        }

        bindTEISR(sword, phase);
    }

    private static void bindTEISR(Item sword, String phase) {
        if (teisrBound || sword == null) return;

        try {
            if (sword instanceof ItemSwordChengYue) {
                // 最佳路徑：呼叫物品本身的 client-only 註冊方法
                ((ItemSwordChengYue) sword).registerISTER();
                teisrBound = true;
                System.out.println("[moremod] SwordChengYue TEISR bound via item.registerISTER() in " + phase);
                return;
            }

            // 備援路徑：直接在 client 端設置 TEISR
            sword.setTileEntityItemStackRenderer(new SwordChengYueRenderer());

            teisrBound = true;
            System.out.println("[moremod] TEISR bound via setTileEntityItemStackRenderer in " + phase);

        } catch (Throwable t) {
            System.err.println("[moremod] bindTEISR failed in " + phase + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    // ------------------------------------------------------------
    // 各種註冊輔助
    // ------------------------------------------------------------
    private static void registerEntityRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityRiftwarden.class, RenderRiftwarden::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityRiftLightning.class, RenderRiftLightning::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityWeepingAngel.class, RenderWeepingAngel::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityVoidRipper.class, RenderVoidRipper::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityPlayerLaserBeam.class, RenderPlayerLaserBeam::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityVoidPortal.class, RenderVoidPortal2x1::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityCursedKnight.class, RenderCursedKnight::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityVoidBullet.class, RenderVoidBullet::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityRiftPortal.class, manager -> new RenderRiftPortal(manager));
        RenderingRegistry.registerEntityRenderingHandler(EntityStoneSentinel.class, RenderStoneSentinel::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityLaserBeam.class, RenderLaserBeam::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityLightningArc.class, RenderLightningArc::new);
    }

    private static void registerMechanicalCoreLayer() {
        try {
            Map<String, RenderPlayer> skinMap = Minecraft.getMinecraft().getRenderManager().getSkinMap();
            if (skinMap == null || skinMap.isEmpty()) return;
            RenderLayerMechanicalCore layer = new RenderLayerMechanicalCore();
            for (Map.Entry<String, RenderPlayer> e : skinMap.entrySet()) {
                e.getValue().addLayer(layer);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void registerMechanicalExoskeletonLayer() {
        try {
            Map<String, RenderPlayer> skinMap = Minecraft.getMinecraft().getRenderManager().getSkinMap();
            if (skinMap == null || skinMap.isEmpty()) return;
            // 若未實作外骨骼 Layer，可留空或之後再加
            // for (Map.Entry<String, RenderPlayer> e : skinMap.entrySet()) { ... }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------
    // 模型（ModelResourceLocation）註冊（不影響 TEISR）
    // ------------------------------------------------------------

    // 新增：方块模型注册
    private static void registerBlockModelsSafe() {
        try {
            // 方法1: 如果你在 ModBlocks 中有静态字段
            registerBlockItemModel(SWORD_UPGRADE_STATION);


            // 在这里添加其他需要注册模型的方块

        } catch (Throwable t) {
            System.err.println("[moremod] registerBlockModelsSafe error: " + t.getMessage());
            t.printStackTrace();
        }
    }

    // 通用方块物品模型注册方法
    private static void registerBlockItemModel(Block block) {
        if (block == null) return;

        Item item = Item.getItemFromBlock(block);
        if (item == null) {
            System.err.println("[moremod] No item for block: " + block.getRegistryName());
            return;
        }

        String name = block.getRegistryName().toString();
        ModelLoader.setCustomModelResourceLocation(
                item,
                0,
                new ModelResourceLocation(name, "inventory")
        );
    }

    private static void registerMechanicalCoreModelsSafe() {
        try {
            if (MECHANICAL_CORE != null) {
                ModelLoader.setCustomModelResourceLocation(
                        MECHANICAL_CORE, 0, new ModelResourceLocation("moremod:mechanical_core", "inventory"));
                ModelLoader.registerItemVariants(
                        MECHANICAL_CORE, new ModelResourceLocation("moremod:mechanical_core", "inventory"));
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void registerEnergySwordModelsSafe() {
        try {
            Item item = ENERGY_SWORD;
            if (item == null) return;
            ModelLoader.registerItemVariants(item,
                    new ModelResourceLocation("moremod:energy_sword", "inventory"),
                    new ModelResourceLocation("moremod:energy_sword_sheathed", "inventory"),
                    new ModelResourceLocation("moremod:energy_sword_with_sheath", "inventory"),
                    new ModelResourceLocation("moremod:energy_sword_unsheathed", "inventory")
            );
            ModelLoader.setCustomModelResourceLocation(
                    item, 0, new ModelResourceLocation("moremod:energy_sword", "inventory"));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void registerJetpackModelsSafe() {
        try {
            if (JETPACK_T1 != null) {
                ModelLoader.setCustomModelResourceLocation(
                        JETPACK_T1, 0, new ModelResourceLocation("moremod:jetpack_t1", "inventory"));
            }
            if (JETPACK_T2 != null) {
                ModelLoader.setCustomModelResourceLocation(
                        JETPACK_T2, 0, new ModelResourceLocation("moremod:jetpack_t2", "inventory"));
            }
            if (JETPACK_T3 != null) {
                ModelLoader.setCustomModelResourceLocation(
                        JETPACK_T3, 0, new ModelResourceLocation("moremod:jetpack_t3", "inventory"));
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void registerCoreMaterialsModelsSafe() {
        // 如無此等物品可留白
    }

    private static void registerSwordChengYueModelsSafe() {
        try {
            Item sword = SWORD_CHENGYUE;
            if (sword == null) {
                sword = Item.getByNameOrId("moremod:sword_chengyue");
            }
            if (sword != null) {
                ModelLoader.setCustomModelResourceLocation(
                        sword, 0, new ModelResourceLocation("moremod:sword_chengyue", "inventory"));
                System.out.println("[moremod] SwordChengYue model location registered");
            } else {
                System.err.println("[moremod] SWORD_CHENGYUE still null in ModelRegistryEvent");
            }
        } catch (Throwable t) {
            System.err.println("[moremod] registerSwordChengYueModelsSafe error: " + t.getMessage());
            t.printStackTrace();
        }
    }
}