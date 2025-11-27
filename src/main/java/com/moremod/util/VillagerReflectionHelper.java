package com.moremod.util;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;

/**
 * 村民反射工具类
 */
public class VillagerReflectionHelper {

    private static Field careerIdField;
    private static Field careerLevelField;

    static {
        try {
            // 获取私有字段
            // 开发环境使用反混淆名，生产环境使用混淆名
            careerIdField = ReflectionHelper.findField(
                    EntityVillager.class,
                    "careerId",           // 开发环境名称
                    "field_175563_bv"     // 混淆名称（SRG名）
            );
            careerIdField.setAccessible(true);

            careerLevelField = ReflectionHelper.findField(
                    EntityVillager.class,
                    "careerLevel",        // 开发环境名称
                    "field_175562_bw"     // 混淆名称（SRG名）
            );
            careerLevelField.setAccessible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取村民的Career ID
     */
    public static int getCareerId(EntityVillager villager) {
        try {
            return careerIdField.getInt(villager);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取村民的Career Level
     */
    public static int getCareerLevel(EntityVillager villager) {
        try {
            return careerLevelField.getInt(villager);
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * 设置村民的Career ID（如果需要）
     */
    public static void setCareerId(EntityVillager villager, int careerId) {
        try {
            careerIdField.setInt(villager, careerId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置村民的Career Level（如果需要）
     */
    public static void setCareerLevel(EntityVillager villager, int level) {
        try {
            careerLevelField.setInt(villager, level);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}