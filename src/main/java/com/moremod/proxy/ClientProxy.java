// 文件: com/moremod/proxy/ClientProxy.java
package com.moremod.proxy;

import com.moremod.client.ClientEventHandler;
import com.moremod.client.render.*;
import com.moremod.client.render.fx.RenderPlayerLaserBeam;
import com.moremod.client.render.fx.RenderRiftLightning;
import com.moremod.entity.EntityCursedKnight;
import com.moremod.entity.EntityVoidPortal;
import com.moremod.entity.EntityVoidRipper;
import com.moremod.entity.EntityWeepingAngel;
import com.moremod.entity.boss.EntityRiftwarden;
import com.moremod.client.render.fx.RenderLaserBeam;
import com.moremod.client.render.fx.RenderLightningArc;
import com.moremod.entity.boss.EntityStoneSentinel;
import com.moremod.entity.fx.EntityLaserBeam;
import com.moremod.entity.fx.EntityLightningArc;
import com.moremod.entity.fx.EntityPlayerLaserBeam;
import com.moremod.entity.fx.EntityRiftLightning;
import com.moremod.entity.projectile.EntityVoidBullet;
import com.moremod.item.*;
import com.moremod.moremod; // ← 如果 MODID 不在这里，请改成你的主类路径，例如 com.moremod.moremod

import com.moremod.tile.TileEntityPedestal;
import com.moremod.tile.TileEntityRitualCore;
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
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;

import static com.moremod.item.RegisterItem.*; // 你现有的物品静态引用

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = moremod.MODID)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityRitualCore.class, new TileEntityRitualCoreRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPedestal.class, new TileEntityPedestalRenderer());

        // === 注册实体渲染（这里做！） ===
        registerEntityRenderers();
    }

    private static void registerEntityRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(
                EntityRiftwarden.class, RenderRiftwarden::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
                EntityRiftLightning.class, RenderRiftLightning::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
                EntityWeepingAngel.class, RenderWeepingAngel::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
                EntityVoidRipper.class, RenderVoidRipper::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
                EntityPlayerLaserBeam.class, RenderPlayerLaserBeam::new
        );

        RenderingRegistry.registerEntityRenderingHandler(
                EntityVoidPortal.class, RenderVoidPortal2x1::new);
        RenderingRegistry.registerEntityRenderingHandler(
                EntityCursedKnight.class, RenderCursedKnight::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
                EntityVoidBullet.class, RenderVoidBullet::new
        );



                // 註冊巨石守衛的渲染器
        RenderingRegistry.registerEntityRenderingHandler(
                        EntityStoneSentinel.class,
                        RenderStoneSentinel::new
                );

        RenderingRegistry.registerEntityRenderingHandler(EntityLaserBeam.class, RenderLaserBeam::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityLightningArc.class, RenderLightningArc::new);

    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ItemMechanicalCore.registerEnergyGenerationEvents();
        MinecraftForge.EVENT_BUS.register(new JetpackBaubleRenderer());
        System.out.println("[moremod] Registered JetpackBaubleRenderer");

        registerMechanicalCoreLayer();
        System.out.println("[moremod] Registered MechanicalCore Render Layer");

        registerMechanicalExoskeletonLayer();
        System.out.println("[moremod] Registered MechanicalExoskeleton Render Layer");

        ClientEventHandler.init(event);
    }

    /** 只在 ModelRegistryEvent 里做所有 ModelLoader.* 的事情，同时挂 TEISR */
    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        System.out.println("[moremod] ModelRegistryEvent start");

        // 你的其他物品
        registerEnergySwordModelsSafe();
        registerJetpackModelsSafe();
        registerMechanicalCoreModelsSafe();

        // 新材料（世界纤维 / 量子核心）——如你已在别处注册，可移除
        registerCoreMaterialsModelsSafe();

        System.out.println("[moremod] ModelRegistryEvent done");
    }

    /** 给所有玩家渲染器挂一层机械核心的 Layer */
    private static void registerMechanicalCoreLayer() {
        try {
            Map<String, RenderPlayer> skinMap = Minecraft.getMinecraft().getRenderManager().getSkinMap();
            if (skinMap == null || skinMap.isEmpty()) {
                System.err.println("[moremod] 警告: 无法获取玩家渲染器列表");
                return;
            }
            RenderLayerMechanicalCore layer = new RenderLayerMechanicalCore();
            for (Map.Entry<String, RenderPlayer> entry : skinMap.entrySet()) {
                entry.getValue().addLayer(layer);
                System.out.println("[moremod] 已为皮肤类型 '" + entry.getKey() + "' 添加机械核心渲染层");
            }
        } catch (Throwable e) {
            System.err.println("[moremod] 注册机械核心渲染层时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** 给所有玩家渲染器添加机械外骨骼渲染层（占位日志） */
    private static void registerMechanicalExoskeletonLayer() {
        try {
            Map<String, RenderPlayer> skinMap = Minecraft.getMinecraft().getRenderManager().getSkinMap();
            if (skinMap == null || skinMap.isEmpty()) {
                System.err.println("[moremod] 警告: 无法获取玩家渲染器列表");
                return;
            }
            for (Map.Entry<String, RenderPlayer> entry : skinMap.entrySet()) {
                System.out.println("[moremod] 已为皮肤类型 '" + entry.getKey() + "' 添加机械外骨骼渲染层");
            }
        } catch (Throwable e) {
            System.err.println("[moremod] 注册机械外骨骼渲染层时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ----------------- 下方都是 "只在 ModelRegistryEvent 调用" 的注册函数 -----------------

    private static void registerMechanicalCoreModelsSafe() {
        try {
            if (MECHANICAL_CORE != null) {
                ModelLoader.setCustomModelResourceLocation(
                        MECHANICAL_CORE, 0,
                        new ModelResourceLocation("moremod:mechanical_core", "inventory")
                );

                ModelLoader.setCustomMeshDefinition(MECHANICAL_CORE, stack -> {
                    if (stack == null || !(stack.getItem() instanceof ItemMechanicalCore)) {
                        return new ModelResourceLocation("moremod:mechanical_core", "inventory");
                    }
                    boolean hasEnergy = false;
                    if (stack.hasCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, null)) {
                        net.minecraftforge.energy.IEnergyStorage es =
                                stack.getCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, null);
                        hasEnergy = (es != null && es.getEnergyStored() > 0);
                    }

                    return new ModelResourceLocation("moremod:mechanical_core", "inventory");
                });

                ModelLoader.registerItemVariants(
                        MECHANICAL_CORE,
                        new ModelResourceLocation("moremod:mechanical_core", "inventory")
                );
            }
        } catch (Throwable t) {
            System.err.println("[moremod] registerMechanicalCoreModelsSafe error: " + t);
        }
    }

    private static void registerEnergySwordModelsSafe() {
        try {
            Item item = ENERGY_SWORD;
            if (item == null) {
                System.err.println("[moremod] ENERGY_SWORD is null, skip");
                return;
            }
            ModelLoader.registerItemVariants(item,
                    new ModelResourceLocation("moremod:energy_sword", "inventory"),
                    new ModelResourceLocation("moremod:energy_sword_sheathed", "inventory"),
                    new ModelResourceLocation("moremod:energy_sword_with_sheath", "inventory"),
                    new ModelResourceLocation("moremod:energy_sword_unsheathed", "inventory")
            );

            ModelLoader.setCustomModelResourceLocation(
                    item, 0, new ModelResourceLocation("moremod:energy_sword", "inventory")
            );

            ModelLoader.setCustomMeshDefinition(item, stack -> {
                if (stack == null) {
                    return new ModelResourceLocation("moremod:energy_sword", "inventory");
                }
                float state = ItemEnergySword.getSwordState(stack);
                if (state >= 2.0f) {
                    return new ModelResourceLocation("moremod:energy_sword_unsheathed", "inventory");
                } else if (state >= 1.0f) {
                    return new ModelResourceLocation("moremod:energy_sword_with_sheath", "inventory");
                } else {
                    return new ModelResourceLocation("moremod:energy_sword_sheathed", "inventory");
                }
            });
        } catch (Throwable t) {
            System.err.println("[moremod] registerEnergySwordModelsSafe error: " + t);
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
            System.err.println("[moremod] registerJetpackModelsSafe error: " + t);
        }
    }

    // —— 新增：注册世界纤维 / 量子核心（如已在别处做，可删） ——
    private static void registerCoreMaterialsModelsSafe() {


       {
            System.err.println("[moremod] registerCoreMaterialsModelsSafe error: " );
        }
    }
}
