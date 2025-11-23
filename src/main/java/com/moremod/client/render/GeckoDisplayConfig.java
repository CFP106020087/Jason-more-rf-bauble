package com.moremod.client.render;

import net.minecraft.client.renderer.GlStateManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * GeckoLib 模型配置（只读版本）
 * 从配置文件加载，不支持运行时调试
 */
public class GeckoDisplayConfig {

    private static final File CONFIG_FILE = new File("config/gecko_model_debug.txt");
    private static final Map<String, DisplayParams> configs = new HashMap<>();

    /**
     * 显示参数类
     */
    public static class DisplayParams {
        public final float translateX;
        public final float translateY;
        public final float translateZ;

        public final float rotateX;
        public final float rotateY;
        public final float rotateZ;

        public final float scale;

        public DisplayParams(float tx, float ty, float tz,
                             float rx, float ry, float rz,
                             float s) {
            this.translateX = tx;
            this.translateY = ty;
            this.translateZ = tz;
            this.rotateX = rx;
            this.rotateY = ry;
            this.rotateZ = rz;
            this.scale = s;
        }

        /**
         * 应用此变换到当前矩阵
         */
        public void apply() {
            // 1. 平移
            if (translateX != 0 || translateY != 0 || translateZ != 0) {
                GlStateManager.translate(
                        translateX / 16.0,
                        translateY / 16.0,
                        translateZ / 16.0
                );
            }

            // 2. 旋转（顺序：Y -> X -> Z）
            if (rotateY != 0) {
                GlStateManager.rotate(rotateY, 0, 1, 0);
            }
            if (rotateX != 0) {
                GlStateManager.rotate(rotateX, 1, 0, 0);
            }
            if (rotateZ != 0) {
                GlStateManager.rotate(rotateZ, 0, 0, 1);
            }

            // 3. 缩放
            if (scale != 1.0f) {
                GlStateManager.scale(scale, scale, scale);
            }
        }
    }

    // 静态初始化：加载配置
    static {
        initDefaultConfigs();
        loadConfig();
    }

    /**
     * 初始化默认配置（如果文件不存在）
     */
    private static void initDefaultConfigs() {
        // 使用你调试好的配置值作为默认值
        configs.put("gui", new DisplayParams(
                -68.000f, 59.000f, -6.000f,
                150.000f, -105.000f, -60.000f,
                0.730f
        ));

        configs.put("firstperson_right", new DisplayParams(
                -20.000f, 73.000f, -21.000f,
                5.000f, 95.000f, 90.000f,
                0.800f
        ));

        configs.put("firstperson_left", new DisplayParams(
                -16.000f, 72.000f, -20.000f,
                170.000f, -90.000f, -110.000f,
                0.800f
        ));

        configs.put("thirdperson_right", new DisplayParams(
                -64.000f, 77.000f, -4.000f,
                10.000f, 35.000f, -80.000f,
                1.000f
        ));

        configs.put("thirdperson_left", new DisplayParams(
                -66.000f, 68.000f, -39.000f,
                10.000f, 10.000f, -75.000f,
                1.000f
        ));

        configs.put("ground", new DisplayParams(
                0.000f, 0.000f, 0.000f,
                0.000f, 0.000f, 0.000f,
                1.000f
        ));

        configs.put("fixed", new DisplayParams(
                0.000f, -34.000f, -27.000f,
                75.000f, -120.000f, 0.000f,
                1.000f
        ));

        configs.put("waist_right", new DisplayParams(
                -28.000f, -81.000f, -19.000f,
                0.000f, -95.000f, -10.000f,
                1.000f
        ));

        configs.put("waist_left", new DisplayParams(
                0.000f, 0.000f, 0.000f,
                0.000f, 0.000f, 0.000f,
                1.000f
        ));

        // 背部刀鞘（如果需要的话）
        configs.put("back_right", new DisplayParams(
                0.000f, 0.000f, 0.000f,
                0.000f, 0.000f, 0.000f,
                1.000f
        ));

        configs.put("back_left", new DisplayParams(
                0.000f, 0.000f, 0.000f,
                0.000f, 0.000f, 0.000f,
                1.000f
        ));
    }

    /**
     * 从文件加载配置（覆盖默认值）
     */
    private static void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            System.out.println("[GeckoConfig] 配置文件不存在，使用默认值");
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE));
            String line;
            int loaded = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // 跳过空行和注释
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // 解析配置行
                String[] parts = line.split(",");
                if (parts.length == 8) {
                    try {
                        String mode = parts[0].trim();
                        DisplayParams params = new DisplayParams(
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3]),
                                Float.parseFloat(parts[4]),
                                Float.parseFloat(parts[5]),
                                Float.parseFloat(parts[6]),
                                Float.parseFloat(parts[7])
                        );
                        configs.put(mode, params);
                        loaded++;
                    } catch (NumberFormatException e) {
                        System.err.println("[GeckoConfig] 解析失败: " + line);
                    }
                }
            }

            reader.close();
            System.out.println("[GeckoConfig] 已加载 " + loaded + " 个配置: " + CONFIG_FILE.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("[GeckoConfig] 加载配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取指定模式的配置
     * @param mode 模式名称（如 "gui", "firstperson_right" 等）
     * @return 配置参数，如果不存在返回 null
     */
    public static DisplayParams getParams(String mode) {
        return configs.get(mode);
    }

    /**
     * 应用指定模式的变换
     * @param mode 模式名称
     */
    public static void applyTransform(String mode) {
        DisplayParams params = configs.get(mode);
        if (params != null) {
            params.apply();
        }
    }
}