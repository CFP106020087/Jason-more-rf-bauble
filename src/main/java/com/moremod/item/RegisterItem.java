package com.moremod.item;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "moremod")
public class RegisterItem {

    // 原有物品
    public static final Item LAW_SWORD = new ItemLawSword("law_sword");
    public static final Item ENERGY_RING = new ItemEnergyRing();
    public static final Item SPEAR = new ItemSpearBauble();
    public static final Item CLEANSING_BAUBLE = new ItemCleansingBauble();
    public static final Item ENERGY_BARRIER = new ItemEnergyBarrier();
    public static final Item ENERGY_SWORD = new com.yourmod.items.ItemEnergySword();
    public static final Item BASIC_ENERGY_BARRIER = new ItemBasicEnergyBarrier();
    public static final Item CRUDE_ENERGY_BARRIER = new ItemCrudeEnergyBarrier();

    // 新增饰品：电池饰品
    public static final Item BATTERY_BAUBLE = new ItemBatteryBauble();
    // 🌟 新增：创造模式无限电池
    public static final Item CREATIVE_BATTERY_BAUBLE = new ItemCreativeBatteryBauble();

    // 🕰️ 新增：时间之心
    public static final Item MECHANICAL_HEART = new ItemMechanicalHeart("mechanical_heart");

    // 喷气背包 - 普通版本
    public static final List<Item> JETPACKS = new ArrayList<>();
    public static final Item JETPACK_T1 = new ItemJetpackBauble("jetpack_t1", 100000, 60, 0.15, -0.08, 0.12);
    public static final Item JETPACK_T2 = new ItemJetpackBauble("jetpack_t2", 300000, 100, 0.25, -0.12, 0.20);
    public static final Item JETPACK_T3 = new ItemJetpackBauble("jetpack_t3", 600000, 160, 0.35, -0.16, 0.28);

    // 创造模式喷气背包 - 新增
    public static final Item CREATIVE_JETPACK = new ItemCreativeJetpackBauble("creative_jetpack");

    static {
        JETPACKS.add(JETPACK_T1);
        JETPACKS.add(JETPACK_T2);
        JETPACKS.add(JETPACK_T3);
        JETPACKS.add(CREATIVE_JETPACK); // 添加到喷气背包列表中
    }

    // Block Item 引用

    // 注册物品
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                LAW_SWORD,
                ENERGY_RING,
                SPEAR,
                CLEANSING_BAUBLE,
                ENERGY_BARRIER,
                ENERGY_SWORD,
                BASIC_ENERGY_BARRIER,
                CRUDE_ENERGY_BARRIER,
                BATTERY_BAUBLE, // ✅ 普通电池饰品
                CREATIVE_BATTERY_BAUBLE, // 🌟 创造模式无限电池
                MECHANICAL_HEART // 🕰️ 时间之心
        );

        // 批量注册喷气背包（包括创造模式喷气背包）
        event.getRegistry().registerAll(JETPACKS.toArray(new Item[0]));
    }

    // 注册模型
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerModel(LAW_SWORD);
        registerModel(ENERGY_RING);
        registerModel(SPEAR);
        registerModel(CLEANSING_BAUBLE);
        registerModel(ENERGY_BARRIER);
        registerModel(ENERGY_SWORD);
        registerModel(BASIC_ENERGY_BARRIER);
        registerModel(CRUDE_ENERGY_BARRIER);
        registerModel(BATTERY_BAUBLE); // ✅ 普通电池模型
        registerModel(CREATIVE_BATTERY_BAUBLE); // 🌟 创造电池模型
        registerModel(MECHANICAL_HEART); // 🕰️ 时间之心模型

        // 批量注册喷气背包模型（包括创造模式喷气背包）
        for (Item jetpack : JETPACKS) {
            registerModel(jetpack);
        }
    }

    private static void registerModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}