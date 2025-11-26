package com.moremod.dimension;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 私人维度事件处理器
 * 集中处理所有维度相关的事件
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class PersonalDimensionEventHandler {

    private static boolean isInitialized = false;

    /**
     * 世界加载事件
     */
    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld().isRemote) return;

        // 处理私人维度加载
        if (event.getWorld().provider.getDimension() == PersonalDimensionManager.PERSONAL_DIM_ID) {
            System.out.println("[私人维度] 维度世界加载");

            // 确保管理器初始化
            if (!isInitialized) {
                PersonalDimensionManager.init();
                isInitialized = true;
            }
        }
    }

    /**
     * 世界卸载事件 - 关键修复
     */
    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) return;

        // 处理私人维度卸载
        if (event.getWorld().provider.getDimension() == PersonalDimensionManager.PERSONAL_DIM_ID) {
            System.out.println("[私人维度] 维度世界卸载");

            // ✅ 关键修复：先保存所有数据
            System.out.println("[私人维度] 正在保存玩家空间数据...");
            PersonalDimensionManager.savePlayerSpaces();

            System.out.println("[私人维度] 正在保存UUID绑定数据...");
            PersonalDimensionManager.saveBindings();

            System.out.println("[私人维度] 数据保存完成，开始清理资源");

            // 获取WorldProvider并清理
            if (event.getWorld().provider instanceof PersonalDimensionWorldProvider) {
                PersonalDimensionWorldProvider provider = (PersonalDimensionWorldProvider) event.getWorld().provider;
                provider.cleanup();
            }

            // 清理生成处理器缓存
            PersonalDimensionSpawnHandler.onWorldUnload();

            System.out.println("[私人维度] 维度卸载完成");
        }

        // 如果是主世界卸载（服务器关闭）
        if (event.getWorld().provider.getDimension() == 0) {
            System.out.println("[私人维度] 主世界卸载，执行最终保存");

            // 最终保存所有数据
            PersonalDimensionManager.savePlayerSpaces();
            PersonalDimensionManager.saveBindings();

            // 清理所有私人维度世界
            WorldServer personalWorld = DimensionManager.getWorld(PersonalDimensionManager.PERSONAL_DIM_ID);
            if (personalWorld != null && personalWorld.provider instanceof PersonalDimensionWorldProvider) {
                PersonalDimensionWorldProvider provider = (PersonalDimensionWorldProvider) personalWorld.provider;
                provider.cleanup();
            }

            System.out.println("[私人维度] 服务器关闭前数据保存完成");
        }
    }

    /**
     * 世界保存事件
     */
    @SubscribeEvent
    public static void onWorldSave(WorldEvent.Save event) {
        if (event.getWorld().isRemote) return;

        // 定期保存私人维度数据
        if (event.getWorld().provider.getDimension() == 0) {
            PersonalDimensionManager.onWorldSave(event);
        }
    }

    /**
     * 服务器停止事件
     */
    @SubscribeEvent
    public static void onServerStopping(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // 这里可以添加额外的清理逻辑
    }
}