package com.moremod.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;

/**
 * SimpleDifficulty 口渴系统辅助工具类
 *
 * 使用静态缓存的反射方法，性能几乎等同于直接调用。
 * Method lookup 只在类加载时执行一次。
 */
public class ThirstHelper {

    // SimpleDifficulty 加载状态
    public static final boolean SIMPLE_DIFFICULTY_LOADED = Loader.isModLoaded("simpledifficulty");
    private static boolean INITIALIZED = false;

    // 反射缓存
    private static Object thirstCapability;
    private static Method getCapabilityMethod;
    private static Method getThirstLevelMethod;
    private static Method setThirstLevelMethod;
    private static Method addThirstLevelMethod;
    private static Method getThirstSaturationMethod;
    private static Method setThirstSaturationMethod;
    private static Method addThirstSaturationMethod;
    private static Method getThirstExhaustionMethod;
    private static Method setThirstExhaustionMethod;
    private static Method isThirstyMethod;
    private static Method isDirtyMethod;
    private static Method setCleanMethod;

    static {
        if (SIMPLE_DIFFICULTY_LOADED) {
            initializeReflection();
        }
    }

    private static void initializeReflection() {
        try {
            // 加载SDCapabilities类
            Class<?> sdCapabilitiesClass = Class.forName("com.charles445.simpledifficulty.api.SDCapabilities");

            // 获取THIRST字段
            thirstCapability = sdCapabilitiesClass.getField("THIRST").get(null);

            // 获取getCapability方法
            getCapabilityMethod = EntityPlayer.class.getMethod("getCapability",
                    Class.forName("net.minecraftforge.common.capabilities.Capability"),
                    Class.forName("net.minecraft.util.EnumFacing"));

            // 加载IThirstCapability接口
            Class<?> thirstCapabilityClass = Class.forName("com.charles445.simpledifficulty.api.thirst.IThirstCapability");

            // 获取口渴相关方法
            getThirstLevelMethod = thirstCapabilityClass.getMethod("getThirstLevel");
            setThirstLevelMethod = thirstCapabilityClass.getMethod("setThirstLevel", int.class);
            addThirstLevelMethod = thirstCapabilityClass.getMethod("addThirstLevel", int.class);
            getThirstSaturationMethod = thirstCapabilityClass.getMethod("getThirstSaturation");
            setThirstSaturationMethod = thirstCapabilityClass.getMethod("setThirstSaturation", float.class);
            addThirstSaturationMethod = thirstCapabilityClass.getMethod("addThirstSaturation", float.class);
            getThirstExhaustionMethod = thirstCapabilityClass.getMethod("getThirstExhaustion");
            setThirstExhaustionMethod = thirstCapabilityClass.getMethod("setThirstExhaustion", float.class);
            isThirstyMethod = thirstCapabilityClass.getMethod("isThirsty");
            isDirtyMethod = thirstCapabilityClass.getMethod("isDirty");
            setCleanMethod = thirstCapabilityClass.getMethod("setClean");

            INITIALIZED = true;
            System.out.println("[ThirstHelper] SimpleDifficulty API 初始化成功");
        } catch (Exception e) {
            INITIALIZED = false;
            System.err.println("[ThirstHelper] SimpleDifficulty API 初始化失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否可用
     */
    public static boolean isAvailable() {
        return SIMPLE_DIFFICULTY_LOADED && INITIALIZED;
    }

    /**
     * 获取玩家的口渴 Capability 对象
     */
    private static Object getThirstCapability(EntityPlayer player) {
        if (!isAvailable()) return null;
        try {
            return getCapabilityMethod.invoke(player, thirstCapability, null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取口渴等级 (0-20)
     */
    public static int getThirstLevel(EntityPlayer player) {
        Object cap = getThirstCapability(player);
        if (cap == null) return 20; // 默认满口渴
        try {
            return (int) getThirstLevelMethod.invoke(cap);
        } catch (Exception e) {
            return 20;
        }
    }

    /**
     * 设置口渴等级
     */
    public static void setThirstLevel(EntityPlayer player, int level) {
        Object cap = getThirstCapability(player);
        if (cap == null) return;
        try {
            setThirstLevelMethod.invoke(cap, Math.max(0, Math.min(20, level)));
        } catch (Exception ignored) {}
    }

    /**
     * 增加口渴等级
     */
    public static void addThirstLevel(EntityPlayer player, int amount) {
        Object cap = getThirstCapability(player);
        if (cap == null) return;
        try {
            addThirstLevelMethod.invoke(cap, amount);
        } catch (Exception ignored) {}
    }

    /**
     * 获取口渴饱和度
     */
    public static float getThirstSaturation(EntityPlayer player) {
        Object cap = getThirstCapability(player);
        if (cap == null) return 5.0f;
        try {
            return (float) getThirstSaturationMethod.invoke(cap);
        } catch (Exception e) {
            return 5.0f;
        }
    }

    /**
     * 设置口渴饱和度
     */
    public static void setThirstSaturation(EntityPlayer player, float saturation) {
        Object cap = getThirstCapability(player);
        if (cap == null) return;
        try {
            setThirstSaturationMethod.invoke(cap, Math.max(0f, Math.min(20f, saturation)));
        } catch (Exception ignored) {}
    }

    /**
     * 增加口渴饱和度
     */
    public static void addThirstSaturation(EntityPlayer player, float amount) {
        Object cap = getThirstCapability(player);
        if (cap == null) return;
        try {
            addThirstSaturationMethod.invoke(cap, amount);
        } catch (Exception ignored) {}
    }

    /**
     * 获取口渴疲劳度
     */
    public static float getThirstExhaustion(EntityPlayer player) {
        Object cap = getThirstCapability(player);
        if (cap == null) return 0f;
        try {
            return (float) getThirstExhaustionMethod.invoke(cap);
        } catch (Exception e) {
            return 0f;
        }
    }

    /**
     * 设置口渴疲劳度
     */
    public static void setThirstExhaustion(EntityPlayer player, float exhaustion) {
        Object cap = getThirstCapability(player);
        if (cap == null) return;
        try {
            setThirstExhaustionMethod.invoke(cap, Math.max(0f, exhaustion));
        } catch (Exception ignored) {}
    }

    /**
     * 是否口渴
     */
    public static boolean isThirsty(EntityPlayer player) {
        Object cap = getThirstCapability(player);
        if (cap == null) return false;
        try {
            return (boolean) isThirstyMethod.invoke(cap);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 水源是否脏污
     */
    public static boolean isDirty(EntityPlayer player) {
        Object cap = getThirstCapability(player);
        if (cap == null) return false;
        try {
            return (boolean) isDirtyMethod.invoke(cap);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 净化水源
     */
    public static void setClean(EntityPlayer player) {
        Object cap = getThirstCapability(player);
        if (cap == null) return;
        try {
            setCleanMethod.invoke(cap);
        } catch (Exception ignored) {}
    }

    /**
     * 完全恢复口渴（满口渴 + 满饱和度 + 清除疲劳 + 净化）
     */
    public static void fullyRestoreThirst(EntityPlayer player) {
        if (!isAvailable()) return;
        setThirstLevel(player, 20);
        setThirstSaturation(player, 20f);
        setThirstExhaustion(player, 0f);
        setClean(player);
    }

    /**
     * 部分恢复口渴
     * @param thirstAmount 口渴恢复量
     * @param saturationAmount 饱和度恢复量
     */
    public static void restoreThirst(EntityPlayer player, int thirstAmount, float saturationAmount) {
        if (!isAvailable()) return;
        addThirstLevel(player, thirstAmount);
        addThirstSaturation(player, saturationAmount);
    }
}
