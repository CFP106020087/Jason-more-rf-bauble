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

    // 三个等级的物品实例，使用同一个类但不同的tier参数
    public static final Item ACCESSORY_BOX_T1 = new ItemAccessoryBox(1);
    public static final Item ACCESSORY_BOX_T2 = new ItemAccessoryBox(2);
    public static final Item ACCESSORY_BOX_T3 = new ItemAccessoryBox(3);

    // 物品注册
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                ACCESSORY_BOX_T1,
                ACCESSORY_BOX_T2,
                ACCESSORY_BOX_T3
        );
    }

    // 模型注册（客户端）
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        registerModel(ACCESSORY_BOX_T1, 0);
        registerModel(ACCESSORY_BOX_T2, 0);
        registerModel(ACCESSORY_BOX_T3, 0);
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