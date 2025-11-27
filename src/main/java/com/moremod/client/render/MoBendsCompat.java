package com.moremod.client.render;

import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * MoBends 兼容层 - 零运行时反射开销
 */
@SideOnly(Side.CLIENT)
public class MoBendsCompat {
    
    private static boolean initialized = false;
    private static boolean mobendPresent = false;
    
    // 缓存的 Method 句柄
    private static Method applyCharacterTransform;
    private static Method getExtension;
    private static Class<?> iModelPartClass;
    
    /**
     * 在 FMLInitializationEvent 或首次渲染时调用一次
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        
        try {
            iModelPartClass = Class.forName("goblinbob.mobends.core.client.model.IModelPart");
            applyCharacterTransform = iModelPartClass.getMethod("applyCharacterTransform", float.class);
            applyCharacterTransform.setAccessible(true);
            
            // ModelPartExtended.getExtension()
            Class<?> extendedClass = Class.forName("goblinbob.mobends.core.client.model.ModelPartExtended");
            getExtension = extendedClass.getMethod("getExtension");
            getExtension.setAccessible(true);
            
            mobendPresent = true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            mobendPresent = false;
        }
    }
    
    public static boolean isPresent() {
        return mobendPresent;
    }
    
    public static boolean isBendsModel(ModelPlayer model) {
        if (!mobendPresent) return false;
        return iModelPartClass.isInstance(model.bipedBody);
    }
    
    public static boolean isBendsPart(ModelRenderer part) {
        if (!mobendPresent) return false;
        return iModelPartClass.isInstance(part);
    }
    
    /**
     * 应用 MoBends 变换 - 无反射查找开销
     */
    public static void applyTransform(ModelRenderer part, float scale) {
        if (!mobendPresent || applyCharacterTransform == null) return;
        try {
            applyCharacterTransform.invoke(part, scale);
        } catch (IllegalAccessException | InvocationTargetException e) {
            // 静默失败，回退到原版
        }
    }
    
    /**
     * 获取扩展部件（前臂/小腿）
     */
    public static ModelRenderer getExtensionPart(ModelRenderer part) {
        if (!mobendPresent || getExtension == null) return null;
        if (!iModelPartClass.isInstance(part)) return null;
        
        try {
            return (ModelRenderer) getExtension.invoke(part);
        } catch (Exception e) {
            return null;
        }
    }
}