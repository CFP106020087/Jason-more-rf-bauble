package com.moremod.item;

import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = "moremod")
public class RegisterMaterials {

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> e) {
        e.getRegistry().registerAll(
                ModMaterialItems.ANCIENT_COMPONENT,
                ModMaterialItems.MYSTERIOUS_DUST,
                ModMaterialItems.RARE_CRYSTAL,
                ModMaterialItems.BLANK_TEMPLATE     // ← 新增
        );
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent e) {
        registerModel(ModMaterialItems.ANCIENT_COMPONENT);
        registerModel(ModMaterialItems.MYSTERIOUS_DUST);
        registerModel(ModMaterialItems.RARE_CRYSTAL);
        registerModel(ModMaterialItems.BLANK_TEMPLATE);     // ← 新增
    }

    @SideOnly(Side.CLIENT)
    private static void registerModel(Item item) {
        ModelResourceLocation mrl =
                new ModelResourceLocation(item.getRegistryName(), "inventory");
        ModelLoader.setCustomModelResourceLocation(item, 0, mrl);
    }
}
