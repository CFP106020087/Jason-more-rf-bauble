package com.moremod.system.humanity.intel;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 情报系统物品注册
 * Intel System Item Registration
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class IntelSystemItems {

    // 物品实例
    public static ItemBiologicalSample BIOLOGICAL_SAMPLE;
    public static ItemIntelBook INTEL_BOOK;
    public static ItemIntelStatisticsBook INTEL_STATISTICS_BOOK;

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        BIOLOGICAL_SAMPLE = new ItemBiologicalSample();
        INTEL_BOOK = new ItemIntelBook();
        INTEL_STATISTICS_BOOK = new ItemIntelStatisticsBook();

        event.getRegistry().register(BIOLOGICAL_SAMPLE);
        event.getRegistry().register(INTEL_BOOK);
        event.getRegistry().register(INTEL_STATISTICS_BOOK);

        System.out.println("[MoreMod] 🧬 生物样本物品已注册");
        System.out.println("[MoreMod] 📖 情报书物品已注册");
        System.out.println("[MoreMod] 📊 情报统计书物品已注册");
    }

    @SubscribeEvent
    public static void onRegisterRecipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new RecipeIntelBook());
        System.out.println("[MoreMod] 📖 情报书合成配方已注册");
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onModelRegister(ModelRegistryEvent event) {
        bindModel(BIOLOGICAL_SAMPLE, "biological_sample");
        bindModel(INTEL_BOOK, "intel_book");
        bindModel(INTEL_STATISTICS_BOOK, "intel_statistics_book");

        System.out.println("[MoreMod] 🧬 生物样本模型已注册");
        System.out.println("[MoreMod] 📖 情报书模型已注册");
        System.out.println("[MoreMod] 📊 情报统计书模型已注册");
    }

    @SideOnly(Side.CLIENT)
    private static void bindModel(Item item, String path) {
        if (item != null) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation("moremod:" + path, "inventory"));
        }
    }
}
