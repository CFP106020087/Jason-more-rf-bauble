package com.moremod.client;

import com.moremod.client.gui.MechanicalCoreHUD;
import com.moremod.item.ItemEnergySword;
import com.moremod.item.RegisterItem;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onModelRegister(ModelRegistryEvent event) {
        // 注册基础模型
        ModelLoader.setCustomModelResourceLocation(
                RegisterItem.ENERGY_SWORD,
                0,
                new ModelResourceLocation(RegisterItem.ENERGY_SWORD.getRegistryName(), "inventory")
        );

        // 注册属性重写 - 这是关键！
        RegisterItem.ENERGY_SWORD.addPropertyOverride(
                new ResourceLocation("moremod", "sword_state"),
                (stack, world, entity) -> {
                    return (float) ItemEnergySword.getSwordState(stack);
                }
        );
    } public static void init(FMLInitializationEvent event) {
        // 注册机械核心HUD
        MinecraftForge.EVENT_BUS.register(new MechanicalCoreHUD());
    }
}