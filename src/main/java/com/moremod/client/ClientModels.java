// src/main/java/com/moremod/client/ClientModels.java
package com.moremod.client;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = ClientModels.MODID, value = Side.CLIENT)
public final class ClientModels {

    public static final String MODID = "moremod";

    private ClientModels() {}

    // --- 兼容工具：同时支持 getNamespace/getPath 与 getResourceDomain/getResourcePath ---
    private static String ns(ResourceLocation rl) {
        try { return (String) rl.getClass().getMethod("getNamespace").invoke(rl); }
        catch (Exception ignored) {}
        try { return (String) rl.getClass().getMethod("getResourceDomain").invoke(rl); }
        catch (Exception ignored) {}
        // 最后兜底：解析 "domain:path"
        String s = rl.toString();
        int i = s.indexOf(':');
        return i >= 0 ? s.substring(0, i) : "minecraft";
    }

    private static String path(ResourceLocation rl) {
        try { return (String) rl.getClass().getMethod("getPath").invoke(rl); }
        catch (Exception ignored) {}
        try { return (String) rl.getClass().getMethod("getResourcePath").invoke(rl); }
        catch (Exception ignored) {}
        String s = rl.toString();
        int i = s.indexOf(':');
        return i >= 0 ? s.substring(i + 1) : s;
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        // 1.12 通用：遍历 Item.REGISTRY（不依赖 ForgeRegistries）
        for (ResourceLocation key : Item.REGISTRY.getKeys()) {
            if (!MODID.equals(ns(key))) continue;     // 只处理本 mod
            final Item item = Item.REGISTRY.getObject(key);
            if (item == null) continue;

            // 绑定 meta=0 的 inventory 模型
            ModelLoader.setCustomModelResourceLocation(
                    item, 0,
                    new ModelResourceLocation(key, "inventory")
            );
        }
    }
}
