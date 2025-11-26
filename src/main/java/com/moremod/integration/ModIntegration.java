package com.moremod.integration;

import net.minecraftforge.fml.common.Loader;

public class ModIntegration {

    private static boolean craftTweakerLoaded = false;

    public static void preInit() {
        craftTweakerLoaded = Loader.isModLoaded("crafttweaker");

        if (craftTweakerLoaded) {
            System.out.println("[moremod] CraftTweaker detected, integration will be loaded");
        }
    }

    public static void postInit() {
        if (craftTweakerLoaded) {
            try {
                // 使用反射确保不会在CRT不存在时加载类
                Class.forName("com.moremod.integration.crafttweaker.RitualCraftTweaker");
                System.out.println("[moremod] ✅ CraftTweaker integration loaded successfully");
            } catch (Exception e) {
                System.err.println("[moremod] ❌ Failed to load CraftTweaker integration: " + e.getMessage());
            }
        }
    }

    public static boolean isCraftTweakerLoaded() {
        return craftTweakerLoaded;
    }
}