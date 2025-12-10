package com.moremod.compat;

import net.minecraft.entity.EntityLivingBase;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Infernal Mobs 模组反射工具类
 * 使用完全反射的方式调用 Infernal Mobs API，无需编译时依赖
 */
public class InfernalReflectionHelper {

    private static boolean initialized = false;
    private static boolean infernalAvailable = false;

    private static Class<?> infernalMobsCoreClass;
    private static Class<?> mobModifierClass;
    private static Object proxyInstance;

    private static Method getIsRareEntityMethod;
    private static Method removeEntFromElitesMethod;
    private static Method addEntityModifiersByStringMethod;
    private static Method instanceMethod;
    private static Method getRareMobsMethod;

    /**
     * 初始化反射缓存
     */
    private static void init() {
        if (initialized) return;
        initialized = true;

        try {
            infernalMobsCoreClass = Class.forName("atomicstryker.infernalmobs.common.InfernalMobsCore");
            mobModifierClass = Class.forName("atomicstryker.infernalmobs.common.MobModifier");

            // 静态方法
            getIsRareEntityMethod = infernalMobsCoreClass.getMethod("getIsRareEntity", EntityLivingBase.class);
            removeEntFromElitesMethod = infernalMobsCoreClass.getMethod("removeEntFromElites", EntityLivingBase.class);

            // 实例方法
            instanceMethod = infernalMobsCoreClass.getMethod("instance");
            addEntityModifiersByStringMethod = infernalMobsCoreClass.getMethod("addEntityModifiersByString", EntityLivingBase.class, String.class);

            // proxy.getRareMobs()
            Field proxyField = infernalMobsCoreClass.getField("proxy");
            proxyInstance = proxyField.get(null);
            if (proxyInstance != null) {
                getRareMobsMethod = proxyInstance.getClass().getMethod("getRareMobs");
            }

            infernalAvailable = true;
            System.out.println("[InfernalReflection] Infernal Mobs 模组已加载，反射初始化成功");

        } catch (Exception e) {
            infernalAvailable = false;
            System.out.println("[InfernalReflection] Infernal Mobs 模组未加载: " + e.getMessage());
        }
    }

    /**
     * 检查 Infernal Mobs 是否可用
     */
    public static boolean isInfernalAvailable() {
        init();
        return infernalAvailable;
    }

    /**
     * 检测实体是否为稀有怪物
     */
    public static boolean isRareEntity(EntityLivingBase entity) {
        if (!isInfernalAvailable() || entity == null) return false;

        try {
            return (Boolean) getIsRareEntityMethod.invoke(null, entity);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从精英列表中移除实体
     */
    public static void removeEntFromElites(EntityLivingBase entity) {
        if (!isInfernalAvailable() || entity == null) return;

        try {
            removeEntFromElitesMethod.invoke(null, entity);
        } catch (Exception e) {
            System.err.println("[InfernalReflection] removeEntFromElites 失败: " + e.getMessage());
        }
    }

    /**
     * 通过字符串添加实体修改器
     */
    public static void addEntityModifiersByString(EntityLivingBase entity, String modifiers) {
        if (!isInfernalAvailable() || entity == null) return;

        try {
            Object instance = instanceMethod.invoke(null);
            addEntityModifiersByStringMethod.invoke(instance, entity, modifiers);
        } catch (Exception e) {
            System.err.println("[InfernalReflection] addEntityModifiersByString 失败: " + e.getMessage());
        }
    }

    /**
     * 获取稀有怪物Map
     */
    @SuppressWarnings("unchecked")
    public static Map<EntityLivingBase, Object> getRareMobs() {
        if (!isInfernalAvailable() || proxyInstance == null || getRareMobsMethod == null) {
            return java.util.Collections.emptyMap();
        }

        try {
            return (Map<EntityLivingBase, Object>) getRareMobsMethod.invoke(proxyInstance);
        } catch (Exception e) {
            System.err.println("[InfernalReflection] getRareMobs 失败: " + e.getMessage());
            return java.util.Collections.emptyMap();
        }
    }

    /**
     * 获取实体的修改器名称字符串
     */
    public static String getModifierString(Object modifier) {
        if (!isInfernalAvailable() || modifier == null) return "";

        try {
            // MobModifier 有 getLinkedModifierName() 或 getModName() 方法
            Method getModNameMethod = mobModifierClass.getMethod("getModName");
            StringBuilder sb = new StringBuilder();
            Object current = modifier;

            while (current != null) {
                String name = (String) getModNameMethod.invoke(current);
                if (sb.length() > 0) sb.append(" ");
                sb.append(name);

                // 获取下一个链接的修改器
                try {
                    Field nextField = mobModifierClass.getDeclaredField("nextMod");
                    nextField.setAccessible(true);
                    current = nextField.get(current);
                } catch (NoSuchFieldException e) {
                    current = null;
                }
            }

            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取修改器链的未翻译名称
     */
    public static String getLinkedModNameUntranslated(Object modifier) {
        if (!isInfernalAvailable() || modifier == null) return "";

        try {
            Method method = mobModifierClass.getMethod("getLinkedModNameUntranslated");
            return (String) method.invoke(modifier);
        } catch (Exception e) {
            return getModifierString(modifier);
        }
    }

    /**
     * 调用修改器的 onSpawningComplete 方法
     */
    public static void onSpawningComplete(Object modifier, EntityLivingBase entity) {
        if (!isInfernalAvailable() || modifier == null) return;

        try {
            Method method = mobModifierClass.getMethod("onSpawningComplete", EntityLivingBase.class);
            method.invoke(modifier, entity);
        } catch (Exception e) {
            System.err.println("[InfernalReflection] onSpawningComplete 失败: " + e.getMessage());
        }
    }

    /**
     * 检查实体是否在稀有怪物Map中
     */
    public static boolean isInRareMobs(EntityLivingBase entity) {
        Map<EntityLivingBase, Object> rareMobs = getRareMobs();
        return rareMobs.containsKey(entity);
    }

    /**
     * 获取实体的修改器
     */
    public static Object getModifier(EntityLivingBase entity) {
        Map<EntityLivingBase, Object> rareMobs = getRareMobs();
        return rareMobs.get(entity);
    }

    /**
     * 将修改器放回稀有怪物Map
     */
    public static void putRareMob(EntityLivingBase entity, Object modifier) {
        if (!isInfernalAvailable() || entity == null || modifier == null) return;

        try {
            @SuppressWarnings("unchecked")
            Map<EntityLivingBase, Object> rareMobs = getRareMobs();
            rareMobs.put(entity, modifier);
        } catch (Exception e) {
            System.err.println("[InfernalReflection] putRareMob 失败: " + e.getMessage());
        }
    }
}
