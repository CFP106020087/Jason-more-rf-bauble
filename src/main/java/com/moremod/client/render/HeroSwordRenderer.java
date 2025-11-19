package com.moremod.client.render;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.moremod.client.model.HeroSwordModel;
import com.moremod.item.ItemHeroSword;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 勇者之剑渲染器 - 配置文件版本（增强容错）
 */
@SideOnly(Side.CLIENT)
public class HeroSwordRenderer extends GeoItemRenderer<ItemHeroSword> {

    public static final HeroSwordRenderer INSTANCE = new HeroSwordRenderer();

    private static final File CONFIG_FILE = new File("moremod_debug.json");
    private static final Gson GSON = new Gson();

    private static final Map<String, RenderParams> params = new HashMap<>();
    private static boolean configLoaded = false;

    private static class RenderParams {
        float translateX = 0;
        float translateY = 0;
        float translateZ = 0;
        float rotateX = 0;
        float rotateY = 0;
        float rotateZ = 0;
        float scale = 1.0f;
    }

    public HeroSwordRenderer() {
        super(new HeroSwordModel());
        loadConfig();
    }

    @Override
    public void renderByItem(ItemStack stack) {
        GlStateManager.pushMatrix();

        String mode = detectMode();
        RenderParams p = params.get(mode);

        if (p != null) {
            GlStateManager.translate(p.translateX, p.translateY, p.translateZ);
            GlStateManager.rotate(p.rotateX, 1, 0, 0);
            GlStateManager.rotate(p.rotateY, 0, 1, 0);
            GlStateManager.rotate(p.rotateZ, 0, 0, 1);
            GlStateManager.scale(p.scale, p.scale, p.scale);
        } else {
            applyDefaultTransform(mode);
        }

        super.renderByItem(stack);
        GlStateManager.popMatrix();
    }

    private String detectMode() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) return "gui";

            EntityPlayer player = mc.player;
            if (player == null) return "gui";

            ItemStack mainHand = player.getHeldItemMainhand();
            ItemStack offHand = player.getHeldItemOffhand();

            boolean isHolding = (mainHand != null && mainHand.getItem() instanceof ItemHeroSword) ||
                    (offHand != null && offHand.getItem() instanceof ItemHeroSword);

            if (!isHolding) return "gui";

            if (mc.gameSettings.thirdPersonView == 0) {
                return "first";
            } else {
                return "third";
            }
        } catch (Exception e) {
            return "gui";
        }
    }

    private static void loadConfig() {
        if (configLoaded) return;

        try {
            if (!CONFIG_FILE.exists()) {
                System.out.println("[moremod] Config file not found, using default parameters for HeroSword");
                configLoaded = true;
                return;
            }

            FileInputStream fis = new FileInputStream(CONFIG_FILE);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);

            JsonElement rootElement = GSON.fromJson(isr, JsonElement.class);
            isr.close();

            if (rootElement == null || !rootElement.isJsonObject()) {
                System.err.println("[moremod] Invalid config format for HeroSword");
                configLoaded = true;
                return;
            }

            JsonObject root = rootElement.getAsJsonObject();

            if (root.has("moremod:hero_sword")) {
                JsonElement itemElement = root.get("moremod:hero_sword");

                if (itemElement != null && itemElement.isJsonObject()) {
                    JsonObject itemConfig = itemElement.getAsJsonObject();

                    loadModeParams(itemConfig, "gui");
                    loadModeParams(itemConfig, "first");
                    loadModeParams(itemConfig, "third");

                    System.out.println("[moremod] Loaded HeroSword render config from moremod_debug.json");
                    System.out.println("[moremod]   GUI: " + (params.containsKey("gui") ? "✓" : "✗"));
                    System.out.println("[moremod]   First Person: " + (params.containsKey("first") ? "✓" : "✗"));
                    System.out.println("[moremod]   Third Person: " + (params.containsKey("third") ? "✓" : "✗"));
                }
            }

            configLoaded = true;
        } catch (Exception e) {
            System.err.println("[moremod] Failed to load HeroSword config: " + e.getMessage());
            e.printStackTrace();
            configLoaded = true;
        }
    }

    private static void loadModeParams(JsonObject itemConfig, String mode) {
        try {
            if (!itemConfig.has(mode)) return;

            JsonElement modeElement = itemConfig.get(mode);
            if (modeElement == null || !modeElement.isJsonObject()) return;

            JsonObject modeConfig = modeElement.getAsJsonObject();
            RenderParams p = new RenderParams();

            if (modeConfig.has("translateX") && modeConfig.get("translateX") != null) {
                p.translateX = modeConfig.get("translateX").getAsFloat();
            }
            if (modeConfig.has("translateY") && modeConfig.get("translateY") != null) {
                p.translateY = modeConfig.get("translateY").getAsFloat();
            }
            if (modeConfig.has("translateZ") && modeConfig.get("translateZ") != null) {
                p.translateZ = modeConfig.get("translateZ").getAsFloat();
            }
            if (modeConfig.has("rotateX") && modeConfig.get("rotateX") != null) {
                p.rotateX = modeConfig.get("rotateX").getAsFloat();
            }
            if (modeConfig.has("rotateY") && modeConfig.get("rotateY") != null) {
                p.rotateY = modeConfig.get("rotateY").getAsFloat();
            }
            if (modeConfig.has("rotateZ") && modeConfig.get("rotateZ") != null) {
                p.rotateZ = modeConfig.get("rotateZ").getAsFloat();
            }
            if (modeConfig.has("scale") && modeConfig.get("scale") != null) {
                p.scale = modeConfig.get("scale").getAsFloat();
            }

            params.put(mode, p);
        } catch (Exception e) {
            System.err.println("[moremod] Failed to load HeroSword params for mode: " + mode);
        }
    }

    private void applyDefaultTransform(String mode) {
        // 使用调试得到的参数作为默认值
        switch (mode) {
            case "gui":
                GlStateManager.translate(0.4f, 0.5f, 0.2f);
                GlStateManager.rotate(-4.0f, 1, 0, 0);
                GlStateManager.rotate(-12.0f, 0, 1, 0);
                GlStateManager.rotate(10.0f, 0, 0, 1);
                GlStateManager.scale(0.64f, 0.64f, 0.64f);
                break;

            case "first":
                GlStateManager.translate(0.2f, 1.2f, 0.5f);
                GlStateManager.rotate(92.0f, 1, 0, 0);
                GlStateManager.rotate(6.0f, 0, 1, 0);
                GlStateManager.rotate(-34.0f, 0, 0, 1);
                GlStateManager.scale(0.48f, 0.48f, 0.48f);
                break;

            case "third":
                GlStateManager.translate(0.3f, 0.5f, 0.2f);
                GlStateManager.rotate(0.0f, 1, 0, 0);
                GlStateManager.rotate(0.0f, 0, 1, 0);
                GlStateManager.rotate(0.0f, 0, 0, 1);
                GlStateManager.scale(0.66f, 0.66f, 0.66f);
                break;
        }
    }

    public static void reloadConfig() {
        configLoaded = false;
        params.clear();
        loadConfig();
    }
}