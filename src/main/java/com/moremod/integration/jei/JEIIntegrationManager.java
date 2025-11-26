package com.moremod.integration.jei;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import java.lang.annotation.Annotation;

/**
 * JEI集成管理器 - 检测JEI是否存在（修复版）
 */
public class JEIIntegrationManager {

    private static final String JEI_MOD_ID = "jei";
    private static boolean jeiLoaded = false;
    private static boolean integrationLoaded = false;

    /**
     * 在PostInit阶段调用
     */
    public static void init(FMLPostInitializationEvent event) {
        System.out.println("[MoreMod] =====================================");
        System.out.println("[MoreMod] Checking for JEI integration...");
        System.out.println("[MoreMod] =====================================");

        // 检测JEI是否加载
        if (Loader.isModLoaded(JEI_MOD_ID)) {
            jeiLoaded = true;
            System.out.println("[MoreMod] ✓ JEI detected!");

            // 验证JEI API是否可用
            if (verifyJEIAPI()) {
                System.out.println("[MoreMod] ✓ JEI API verified");

                // 验证我们的集成类
                if (verifyIntegrationClasses()) {
                    integrationLoaded = true;
                    System.out.println("[MoreMod] ✓ JEI integration classes found");
                    System.out.println("[MoreMod] ✓ JEI will automatically load @JEIPlugin classes");
                } else {
                    System.out.println("[MoreMod] ✗ JEI integration classes not found");
                }
            } else {
                System.out.println("[MoreMod] ✗ JEI API not available");
            }
        } else {
            System.out.println("[MoreMod] ℹ JEI not detected - this is normal");
            System.out.println("[MoreMod] ℹ Recipes will work without JEI");
        }

        System.out.println("[MoreMod] =====================================");
        printStatus();
        System.out.println("[MoreMod] =====================================");
    }

    /**
     * 验证JEI API是否可用
     */
    private static boolean verifyJEIAPI() {
        try {
            Class.forName("mezz.jei.api.IModPlugin");
            Class.forName("mezz.jei.api.JEIPlugin");
            Class.forName("mezz.jei.api.IModRegistry");
            return true;
        } catch (ClassNotFoundException e) {
            System.err.println("[MoreMod] JEI API classes not found: " + e.getMessage());
            return false;
        }
    }

    /**
     * 验证我们的集成类是否存在
     */
    private static boolean verifyIntegrationClasses() {
        String[] requiredClasses = {
                "com.moremod.integration.jei.MoreModJEIPlugin",
                "com.moremod.integration.jei.RitualInfusionCategory",
                "com.moremod.integration.jei.RitualInfusionWrapper",
                "com.moremod.integration.jei.DimensionLoomCategory",
                "com.moremod.integration.jei.DimensionLoomWrapper"
        };

        boolean allFound = true;
        for (String className : requiredClasses) {
            try {
                Class<?> clazz = Class.forName(className);

                // 特殊检查：主插件类必须有@JEIPlugin注解
                if (className.endsWith("MoreModJEIPlugin")) {
                    checkJEIPluginAnnotation(clazz);
                }

            } catch (ClassNotFoundException e) {
                System.err.println("[MoreMod] Missing integration class: " + className);
                allFound = false;
            }
        }

        return allFound;
    }

    /**
     * 检查@JEIPlugin注解（修复版）
     */
    @SuppressWarnings("unchecked")
    private static void checkJEIPluginAnnotation(Class<?> clazz) {
        try {
            // 方法1：使用反射获取注解
            Class<?> annotationClass = Class.forName("mezz.jei.api.JEIPlugin");
            if (annotationClass.isAnnotation()) {
                Class<? extends Annotation> jeiPluginAnnotation = (Class<? extends Annotation>) annotationClass;
                boolean hasAnnotation = clazz.isAnnotationPresent(jeiPluginAnnotation);

                if (hasAnnotation) {
                    System.out.println("[MoreMod] ✓ @JEIPlugin annotation found on " + clazz.getSimpleName());
                } else {
                    System.out.println("[MoreMod] ⚠ @JEIPlugin annotation missing on " + clazz.getSimpleName());
                }
            }
        } catch (ClassNotFoundException e) {
            // JEI注解类不存在（正常，如果JEI未安装）
            System.out.println("[MoreMod] JEI annotation class not found (this is ok if JEI is not installed)");
        } catch (Exception e) {
            // 其他错误，忽略
            System.err.println("[MoreMod] Error checking annotation: " + e.getMessage());
        }
    }

    /**
     * 备选方法：使用getAnnotations()遍历
     */
    private static void checkJEIPluginAnnotationAlternative(Class<?> clazz) {
        try {
            Annotation[] annotations = clazz.getAnnotations();
            boolean found = false;

            for (Annotation annotation : annotations) {
                if (annotation.annotationType().getName().equals("mezz.jei.api.JEIPlugin")) {
                    found = true;
                    break;
                }
            }

            if (found) {
                System.out.println("[MoreMod] ✓ @JEIPlugin annotation found");
            } else {
                System.out.println("[MoreMod] ⚠ @JEIPlugin annotation not found");
            }
        } catch (Exception e) {
            System.err.println("[MoreMod] Error checking annotation: " + e.getMessage());
        }
    }

    /**
     * 打印最终状态
     */
    private static void printStatus() {
        System.out.println("[MoreMod] JEI Integration Status:");
        System.out.println("[MoreMod] - JEI Mod: " + (jeiLoaded ? "✓ Loaded" : "✗ Not Loaded"));
        System.out.println("[MoreMod] - Integration: " + (integrationLoaded ? "✓ Ready" : "✗ Not Ready"));

        if (jeiLoaded && integrationLoaded) {
            System.out.println("[MoreMod] ✓ Players with JEI will see recipes in JEI");
            System.out.println("[MoreMod] ✓ Players without JEI can still use recipes normally");
        } else if (!jeiLoaded) {
            System.out.println("[MoreMod] ℹ Recipes work normally without JEI");
        } else {
            System.out.println("[MoreMod] ⚠ JEI found but integration failed");
        }
    }

    public static boolean isJeiLoaded() {
        return jeiLoaded;
    }

    public static boolean isIntegrationLoaded() {
        return integrationLoaded;
    }
}