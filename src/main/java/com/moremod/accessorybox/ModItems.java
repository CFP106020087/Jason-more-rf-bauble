package com.moremod.accessorybox;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = "moremod")
public class ModItems {

    // 物品实例
    public static final Item ACCESSORY_BOX = new ItemAccessoryBox();

    // 物品注册
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                ACCESSORY_BOX
        );
    }

    // 模型注册（客户端）
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        registerModel(ACCESSORY_BOX, 0);
    }

    @SideOnly(Side.CLIENT)
    private static void registerModel(Item item, int meta) {
        ModelLoader.setCustomModelResourceLocation(
                item,
                meta,
                new ModelResourceLocation(item.getRegistryName(), "inventory")
        );
    }
}