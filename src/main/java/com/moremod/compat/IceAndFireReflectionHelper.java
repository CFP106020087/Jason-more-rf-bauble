package com.moremod.compat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;

/**
 * Ice and Fire 模组反射帮助类
 *
 * 提供对 Ice and Fire API 的软依赖访问
 * 如果模组不存在，所有方法安全返回默认值
 */
public class IceAndFireReflectionHelper {

    private static boolean initialized = false;
    private static boolean iceAndFireAvailable = false;

    // ChainLightningUtils
    private static Method createChainLightningMethod = null;

    // InFCapabilities
    private static Class<?> infCapabilitiesClass = null;
    private static Method getEntityEffectCapabilityMethod = null;

    // IEntityEffectCapability
    private static Class<?> entityEffectCapabilityClass = null;
    private static Method setFrozenMethod = null;

    // Dragon classes
    private static Class<?> fireDragonClass = null;
    private static Class<?> iceDragonClass = null;

    private static void init() {
        if (initialized) return;
        initialized = true;

        if (!Loader.isModLoaded("iceandfire")) {
            System.out.println("[MoreMod] Ice and Fire not loaded, compatibility disabled");
            return;
        }

        try {
            // ChainLightningUtils
            Class<?> chainLightningUtilsClass = Class.forName("com.github.alexthe666.iceandfire.api.ChainLightningUtils");

            // 尝试不同的方法签名
            try {
                createChainLightningMethod = chainLightningUtilsClass.getMethod("createChainLightningFromTarget",
                        World.class, EntityLivingBase.class, Entity.class, float[].class, int.class, boolean.class);
            } catch (NoSuchMethodException e1) {
                try {
                    createChainLightningMethod = chainLightningUtilsClass.getMethod("createChainLightningFromTarget",
                            World.class, EntityLivingBase.class, EntityLivingBase.class, float[].class, int.class, boolean.class);
                } catch (NoSuchMethodException e2) {
                    // 方法不存在
                }
            }

            // InFCapabilities
            try {
                infCapabilitiesClass = Class.forName("com.github.alexthe666.iceandfire.api.InFCapabilities");
                getEntityEffectCapabilityMethod = infCapabilitiesClass.getMethod("getEntityEffectCapability", EntityLivingBase.class);
            } catch (Exception e) {
                // 可选功能
            }

            // IEntityEffectCapability
            try {
                entityEffectCapabilityClass = Class.forName("com.github.alexthe666.iceandfire.api.IEntityEffectCapability");
                setFrozenMethod = entityEffectCapabilityClass.getMethod("setFrozen", int.class);
            } catch (Exception e) {
                // 可选功能
            }

            // Dragon classes
            try {
                fireDragonClass = Class.forName("com.github.alexthe666.iceandfire.entity.EntityFireDragon");
                iceDragonClass = Class.forName("com.github.alexthe666.iceandfire.entity.EntityIceDragon");
            } catch (Exception e) {
                // 可选功能
            }

            iceAndFireAvailable = true;
            System.out.println("[MoreMod] Ice and Fire compatibility initialized");

        } catch (Exception e) {
            System.err.println("[MoreMod] Failed to initialize Ice and Fire compatibility: " + e.getMessage());
            iceAndFireAvailable = false;
        }
    }

    /**
     * 检查 Ice and Fire 是否可用
     */
    public static boolean isAvailable() {
        if (!initialized) init();
        return iceAndFireAvailable;
    }

    /**
     * 创建链式闪电效果
     *
     * @return true 如果成功调用，false 如果 Ice and Fire 不可用或调用失败
     */
    public static boolean createChainLightningFromTarget(World world, EntityLivingBase target,
            Entity source, float[] damagePerHop, int maxChainCount, boolean hitWater) {
        if (!isAvailable() || createChainLightningMethod == null) {
            return false;
        }

        try {
            createChainLightningMethod.invoke(null, world, target, source, damagePerHop, maxChainCount, hitWater);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 设置实体冰冻效果
     *
     * @return true 如果成功设置
     */
    public static boolean setFrozen(EntityLivingBase entity, int frozenTicks) {
        if (!isAvailable() || getEntityEffectCapabilityMethod == null || setFrozenMethod == null) {
            return false;
        }

        try {
            Object capability = getEntityEffectCapabilityMethod.invoke(null, entity);
            if (capability != null) {
                setFrozenMethod.invoke(capability, frozenTicks);
                return true;
            }
        } catch (Exception e) {
            // 静默失败
        }
        return false;
    }

    /**
     * 检查实体是否为火龙
     */
    public static boolean isFireDragon(EntityLivingBase entity) {
        if (!isAvailable() || fireDragonClass == null) {
            return false;
        }
        return fireDragonClass.isInstance(entity);
    }

    /**
     * 检查实体是否为冰龙
     */
    public static boolean isIceDragon(EntityLivingBase entity) {
        if (!isAvailable() || iceDragonClass == null) {
            return false;
        }
        return iceDragonClass.isInstance(entity);
    }

    /**
     * 检查实体是否为龙（火龙或冰龙）
     */
    public static boolean isDragon(EntityLivingBase entity) {
        return isFireDragon(entity) || isIceDragon(entity);
    }
}
