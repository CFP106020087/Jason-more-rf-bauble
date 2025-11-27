package com.moremod.dimension;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 私人维度类型 - 优化修复版
 * 修复了重复注册和资源清理问题
 */
public class PersonalDimensionType {

    public static DimensionType PERSONAL_DIM_TYPE;
    private static boolean isRegistered = false;
    private static boolean isInitialized = false;

    // 防止并发问题
    private static final Object LOCK = new Object();

    /**
     * 注册维度类型
     * 在主Mod类的preInit中调用
     */
    public static void registerDimension() {
        synchronized (LOCK) {
            if (isRegistered) {
                System.out.println("[私人维度] 维度已注册，跳过重复注册");
                return;
            }

            try {
                // 检查维度ID是否已被占用
                if (DimensionManager.isDimensionRegistered(PersonalDimensionManager.PERSONAL_DIM_ID)) {
                    System.out.println("[私人维度] 维度ID已被占用，尝试获取现有类型");

                    // 尝试获取现有的维度类型
                    DimensionType existingType = DimensionManager.getProviderType(PersonalDimensionManager.PERSONAL_DIM_ID);
                    if (existingType != null) {
                        PERSONAL_DIM_TYPE = existingType;
                        isRegistered = true;
                        return;
                    }
                }

                // 创建维度类型
                PERSONAL_DIM_TYPE = DimensionType.register(
                        "personal_dimension",                    // 名称
                        "_personal",                             // 后缀
                        PersonalDimensionManager.PERSONAL_DIM_ID, // 维度ID
                        PersonalDimensionWorldProvider.class,     // WorldProvider类
                        false                                     // keepLoaded
                );

                // 注册到DimensionManager
                if (!DimensionManager.isDimensionRegistered(PersonalDimensionManager.PERSONAL_DIM_ID)) {
                    DimensionManager.registerDimension(
                            PersonalDimensionManager.PERSONAL_DIM_ID,
                            PERSONAL_DIM_TYPE
                    );
                }

                isRegistered = true;
                System.out.println("[私人维度] 维度类型注册完成，ID: " + PersonalDimensionManager.PERSONAL_DIM_ID);
            } catch (Exception e) {
                System.err.println("[私人维度] 维度注册失败: " + e.getMessage());
                e.printStackTrace();

                // 尝试恢复
                tryRecoverRegistration();
            }
        }
    }

    /**
     * 尝试恢复注册
     */
    private static void tryRecoverRegistration() {
        try {
            // 如果注册失败，尝试使用不同的ID
            int alternativeId = PersonalDimensionManager.PERSONAL_DIM_ID;

            // 查找可用的维度ID
            while (DimensionManager.isDimensionRegistered(alternativeId) && alternativeId < 200) {
                alternativeId++;
            }

            if (alternativeId < 200 && alternativeId != PersonalDimensionManager.PERSONAL_DIM_ID) {
                System.out.println("[私人维度] 尝试使用备用ID: " + alternativeId);

                PERSONAL_DIM_TYPE = DimensionType.register(
                        "personal_dimension_alt",
                        "_personal_alt",
                        alternativeId,
                        PersonalDimensionWorldProvider.class,
                        false
                );

                DimensionManager.registerDimension(alternativeId, PERSONAL_DIM_TYPE);

                // 更新管理器中的ID
                System.out.println("[私人维度] 注意：使用备用维度ID " + alternativeId);
                isRegistered = true;
            }
        } catch (Exception e2) {
            System.err.println("[私人维度] 恢复注册失败: " + e2.getMessage());
        }
    }

    /**
     * 确保维度被初始化（在世界加载后调用）
     */
    public static void ensureDimensionExists() {
        synchronized (LOCK) {
            if (!isRegistered) {
                registerDimension();
            }

            if (isInitialized) {
                return;
            }

            try {
                // 只在维度已注册的情况下初始化
                if (DimensionManager.isDimensionRegistered(PersonalDimensionManager.PERSONAL_DIM_ID)) {
                    // 检查维度是否已经存在
                    WorldServer existingWorld = DimensionManager.getWorld(PersonalDimensionManager.PERSONAL_DIM_ID);
                    if (existingWorld != null) {
                        isInitialized = true;
                        System.out.println("[私人维度] 维度已存在");
                        return;
                    }

                    // 初始化维度
                    DimensionManager.initDimension(PersonalDimensionManager.PERSONAL_DIM_ID);
                    isInitialized = true;
                    System.out.println("[私人维度] 维度初始化成功");
                }
            } catch (Exception e) {
                System.err.println("[私人维度] 维度初始化失败: " + e.getMessage());
                // 不抛出异常，允许继续运行
            }
        }
    }

    /**
     * 卸载维度
     */
    public static void unregisterDimension() {
        synchronized (LOCK) {
            if (!isRegistered) {
                return;
            }

            try {
                // 先卸载世界
                WorldServer world = DimensionManager.getWorld(PersonalDimensionManager.PERSONAL_DIM_ID);
                if (world != null) {
                    DimensionManager.setWorld(PersonalDimensionManager.PERSONAL_DIM_ID, null, world.getMinecraftServer());
                }

                // 取消注册维度
                if (DimensionManager.isDimensionRegistered(PersonalDimensionManager.PERSONAL_DIM_ID)) {
                    DimensionManager.unregisterDimension(PersonalDimensionManager.PERSONAL_DIM_ID);
                }

                isRegistered = false;
                isInitialized = false;
                PERSONAL_DIM_TYPE = null;

                System.out.println("[私人维度] 维度已卸载");
            } catch (Exception e) {
                System.err.println("[私人维度] 维度卸载失败: " + e.getMessage());
            }
        }
    }

    /**
     * 重置维度状态
     */
    public static void reset() {
        synchronized (LOCK) {
            isInitialized = false;
            // 不重置isRegistered，因为维度类型应该保持注册状态
        }
    }

    /**
     * 世界卸载事件处理
     */
    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            return;
        }

        // 如果是私人维度被卸载
        if (event.getWorld().provider.getDimension() == PersonalDimensionManager.PERSONAL_DIM_ID) {
            synchronized (LOCK) {
                isInitialized = false;
            }
            System.out.println("[私人维度] 维度世界已卸载");
        }
    }

    /**
     * 获取维度是否已注册
     */
    public static boolean isRegistered() {
        return isRegistered;
    }

    /**
     * 获取维度是否已初始化
     */
    public static boolean isInitialized() {
        return isInitialized;
    }
}