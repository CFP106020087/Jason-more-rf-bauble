package com.moremod.util;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.datasync.DataParameter;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;

/**
 * 直接操作血量的工具类
 * 绕过 First Aid 等模组对 setHealth 的包装
 */
public class DirectHealthHelper {

    private static final DataParameter<Float> HEALTH_PARAM;
    private static boolean initialized = false;
    private static boolean available = false;

    static {
        DataParameter<Float> temp = null;
        try {
            // EntityLivingBase.HEALTH 是一个 DataParameter<Float>
            Field healthField = ReflectionHelper.findField(
                    EntityLivingBase.class,
                    "HEALTH", "field_184632_c"
            );
            healthField.setAccessible(true);
            @SuppressWarnings("unchecked")
            DataParameter<Float> param = (DataParameter<Float>) healthField.get(null);
            temp = param;
            available = true;
        } catch (Exception e) {
            System.err.println("[moremod] DirectHealthHelper: Failed to get HEALTH DataParameter");
            e.printStackTrace();
            available = false;
        }
        HEALTH_PARAM = temp;
        initialized = true;
    }

    /**
     * 直接设置实体血量，绕过 setHealth 包装
     *
     * @param entity 目标实体
     * @param health 目标血量
     * @return 是否成功
     */
    public static boolean setHealthDirect(EntityLivingBase entity, float health) {
        if (!available || entity == null) return false;

        try {
            // 限制血量范围
            float maxHealth = entity.getMaxHealth();
            float clampedHealth = Math.max(0.0F, Math.min(health, maxHealth));

            // 直接通过 DataManager 设置血量
            entity.getDataManager().set(HEALTH_PARAM, clampedHealth);
            return true;
        } catch (Exception e) {
            // 回退到普通方式
            entity.setHealth(health);
            return false;
        }
    }

    /**
     * 直接设置血量，不做上限检查（用于破碎之神等特殊情况）
     */
    public static boolean setHealthDirectUnclamped(EntityLivingBase entity, float health) {
        if (!available || entity == null) return false;

        try {
            float clampedHealth = Math.max(0.0F, health);
            entity.getDataManager().set(HEALTH_PARAM, clampedHealth);
            return true;
        } catch (Exception e) {
            entity.setHealth(health);
            return false;
        }
    }

    /**
     * 检查直接设置是否可用
     */
    public static boolean isAvailable() {
        return initialized && available;
    }
}
