package com.moremod.dimension;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.common.DimensionManager;

/**
 * 私人维度类型
 */
public class PersonalDimensionType {

    public static DimensionType PERSONAL_DIM_TYPE;
    private static boolean isRegistered = false;

    /**
     * 注册维度类型
     * 在主Mod类的preInit中调用
     */
    public static void registerDimension() {
        if (isRegistered) {
            System.out.println("[私人维度] 维度已注册，跳过重复注册");
            return;
        }

        try {
            // 创建维度类型
            PERSONAL_DIM_TYPE = DimensionType.register(
                    "personal_dimension",                    // 名称
                    "_personal",                             // 后缀
                    PersonalDimensionManager.PERSONAL_DIM_ID, // 维度ID
                    PersonalDimensionWorldProvider.class,     // WorldProvider类
                    false                                     // keepLoaded
            );

            // 注册到DimensionManager
            DimensionManager.registerDimension(
                    PersonalDimensionManager.PERSONAL_DIM_ID,
                    PERSONAL_DIM_TYPE
            );

            isRegistered = true;
            System.out.println("[私人维度] 维度类型注册完成，ID: " + PersonalDimensionManager.PERSONAL_DIM_ID);
        } catch (Exception e) {
            System.err.println("[私人维度] 维度注册失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 确保维度被初始化（在世界加载后调用）
     */
    public static void ensureDimensionExists() {
        if (!isRegistered) {
            registerDimension();
        }

        try {
            // 只在维度已注册的情况下初始化
            if (DimensionManager.isDimensionRegistered(PersonalDimensionManager.PERSONAL_DIM_ID)) {
                DimensionManager.initDimension(PersonalDimensionManager.PERSONAL_DIM_ID);
                System.out.println("[私人维度] 维度初始化成功");
            }
        } catch (Exception e) {
            System.err.println("[私人维度] 维度初始化失败: " + e.getMessage());
        }
    }
}