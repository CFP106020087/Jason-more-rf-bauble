package com.moremod.client.render.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RenderDebugConfig {

    public static class RenderParams {
        public float translateX = 0;
        public float translateY = 0;
        public float translateZ = 0;

        public float rotateX = 0;
        public float rotateY = 0;
        public float rotateZ = 0;

        public float scale = 1.0f;
    }

    private static final File CONFIG_FILE = new File("moremod_debug.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean debugEnabled = false;
    private static String currentItem = "none";

    private static ItemCameraTransforms.TransformType currentMode = ItemCameraTransforms.TransformType.GUI;

    private static final Map<String, Map<String, RenderParams>> data = new HashMap<>();

    // 获取当前模式键
    private static String modeKey() {
        switch (currentMode) {
            case FIRST_PERSON_RIGHT_HAND:
            case FIRST_PERSON_LEFT_HAND:
                return "first";
            case THIRD_PERSON_RIGHT_HAND:
            case THIRD_PERSON_LEFT_HAND:
                return "third";
            default:
                return "gui";
        }
    }

    public static RenderParams getCurrentParams() {
        data.putIfAbsent(currentItem, new HashMap<>());
        Map<String, RenderParams> itemMap = data.get(currentItem);

        itemMap.putIfAbsent(modeKey(), new RenderParams());
        return itemMap.get(modeKey());
    }

    // 调整功能
    public static void adjustTranslateX(float amt, boolean fine) {
        getCurrentParams().translateX += fine ? amt * 0.01f : amt * 0.1f;
    }

    public static void adjustTranslateY(float amt, boolean fine) {
        getCurrentParams().translateY += fine ? amt * 0.01f : amt * 0.1f;
    }

    public static void adjustTranslateZ(float amt, boolean fine) {
        getCurrentParams().translateZ += fine ? amt * 0.01f : amt * 0.1f;
    }

    public static void adjustRotateX(float amt, boolean fine) {
        getCurrentParams().rotateX += fine ? amt * 0.2f : amt * 2.0f;
    }

    public static void adjustRotateY(float amt, boolean fine) {
        getCurrentParams().rotateY += fine ? amt * 0.2f : amt * 2.0f;
    }

    public static void adjustRotateZ(float amt, boolean fine) {
        getCurrentParams().rotateZ += fine ? amt * 0.2f : amt * 2.0f;
    }

    public static void adjustScale(float amt, boolean fine) {
        getCurrentParams().scale += fine ? amt * 0.005f : amt * 0.02f;
        if (getCurrentParams().scale < 0.01f) getCurrentParams().scale = 0.01f;
    }

    public static void toggleDebug() {
        debugEnabled = !debugEnabled;
        System.out.println("[Debug] Debug mode = " + debugEnabled);
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void setCurrentItem(String id) {
        currentItem = id;
    }

    public static void setCurrentMode(ItemCameraTransforms.TransformType type) {
        currentMode = type;
    }

    public static void cycleMode() {
        switch (currentMode) {
            case GUI:
                currentMode = ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND;
                break;
            case FIRST_PERSON_RIGHT_HAND:
                currentMode = ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND;
                break;
            default:
                currentMode = ItemCameraTransforms.TransformType.GUI;
                break;
        }
        System.out.println("[Debug] Mode set to: " + modeKey());
    }

    public static void resetCurrent() {
        data.getOrDefault(currentItem, new HashMap<>()).put(modeKey(), new RenderParams());
        System.out.println("[Debug] Reset current mode " + modeKey());
    }

    public static void resetAll() {
        data.put(currentItem, new HashMap<>());
        System.out.println("[Debug] Reset ALL modes for item " + currentItem);
    }

    public static void printCurrentValues() {
        RenderParams p = getCurrentParams();
        System.out.println("===== " + currentItem + " @ " + modeKey() + " =====");
        System.out.println("translate(" + p.translateX + "f, " + p.translateY + "f, " + p.translateZ + "f);");
        System.out.println("rotateX = " + p.rotateX + "f;");
        System.out.println("rotateY = " + p.rotateY + "f;");
        System.out.println("rotateZ = " + p.rotateZ + "f;");
        System.out.println("scale(" + p.scale + "f);");
    }

    // 保存到 JSON
    public static void saveConfig() {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
            GSON.toJson(data, w);
            System.out.println("[Debug] Saved to moremod_debug.json");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 启动时加载 - 修复版本
    public static void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            System.out.println("[Debug] Config file not found: moremod_debug.json");
            return;
        }
        
        try (Reader r = new InputStreamReader(new FileInputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
            // 先加载为原始Map
            @SuppressWarnings("unchecked")
            Map<String, Object> loaded = GSON.fromJson(r, Map.class);
            
            if (loaded != null) {
                // 手动转换每个物品的数据
                for (Map.Entry<String, Object> itemEntry : loaded.entrySet()) {
                    String itemId = itemEntry.getKey();
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> modesMap = (Map<String, Object>) itemEntry.getValue();
                    
                    Map<String, RenderParams> convertedModes = new HashMap<>();
                    
                    // 转换每个模式的参数
                    for (Map.Entry<String, Object> modeEntry : modesMap.entrySet()) {
                        String mode = modeEntry.getKey();
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Object> paramMap = (Map<String, Object>) modeEntry.getValue();
                        
                        // 手动创建 RenderParams 对象
                        RenderParams params = new RenderParams();
                        
                        if (paramMap.containsKey("translateX")) {
                            params.translateX = ((Number) paramMap.get("translateX")).floatValue();
                        }
                        if (paramMap.containsKey("translateY")) {
                            params.translateY = ((Number) paramMap.get("translateY")).floatValue();
                        }
                        if (paramMap.containsKey("translateZ")) {
                            params.translateZ = ((Number) paramMap.get("translateZ")).floatValue();
                        }
                        
                        if (paramMap.containsKey("rotateX")) {
                            params.rotateX = ((Number) paramMap.get("rotateX")).floatValue();
                        }
                        if (paramMap.containsKey("rotateY")) {
                            params.rotateY = ((Number) paramMap.get("rotateY")).floatValue();
                        }
                        if (paramMap.containsKey("rotateZ")) {
                            params.rotateZ = ((Number) paramMap.get("rotateZ")).floatValue();
                        }
                        
                        if (paramMap.containsKey("scale")) {
                            params.scale = ((Number) paramMap.get("scale")).floatValue();
                        }
                        
                        convertedModes.put(mode, params);
                    }
                    
                    data.put(itemId, convertedModes);
                }
                
                System.out.println("[Debug] Loaded moremod_debug.json successfully");
                System.out.println("[Debug] Items loaded: " + loaded.size());
            }
        } catch (Exception e) {
            System.err.println("[Debug] Failed to load config:");
            e.printStackTrace();
        }
    }
}