package com.moremod.accessorybox.unlock;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * 可解锁槽位系统初始化
 * 在你的主mod类中调用这些方法
 */
public class UnlockableSlotsInit {

    /**
     * PreInit 阶段初始化
     * 在主mod的 preInit 中调用
     */
    public static void preInit(FMLPreInitializationEvent event) {
        System.out.println("========================================");
        System.out.println("[UnlockableSlots] 可解锁槽位系统初始化");
        System.out.println("========================================");

        // 打印配置
        UnlockableSlotsConfig.printConfig();

        // 注册网络数据包
        ModNetworkHandler.registerPackets();

        System.out.println("[UnlockableSlots] PreInit 完成");
        System.out.println("========================================");
    }

    /**
     * Init 阶段初始化
     * 在主mod的 init 中调用
     */
    public static void init(FMLInitializationEvent event) {
        System.out.println("========================================");
        System.out.println("[UnlockableSlots] 槽位布局初始化");
        System.out.println("========================================");

        // 初始化槽位布局（触发缓存构建）
        com.moremod.accessorybox.SlotLayoutHelper.calculateSlotAllocation();

        // 打印系统状态
        printSystemStatus();

        System.out.println("[UnlockableSlots] Init 完成");
        System.out.println("========================================");
    }

    /**
     * 打印系统状态
     */
    private static void printSystemStatus() {
        System.out.println("\n系统状态:");
        System.out.println("  解锁系统: " + (UnlockableSlotsConfig.enableUnlockSystem ? "启用" : "禁用"));

        if (UnlockableSlotsConfig.enableUnlockSystem) {
            // 统计锁定槽位数量
            int totalLocked = 0;
            int totalExtra = 0;

            totalLocked += countLocked(UnlockableSlotsConfig.extraAmuletLocks);
            totalLocked += countLocked(UnlockableSlotsConfig.extraRingLocks);
            totalLocked += countLocked(UnlockableSlotsConfig.extraBeltLocks);
            totalLocked += countLocked(UnlockableSlotsConfig.extraHeadLocks);
            totalLocked += countLocked(UnlockableSlotsConfig.extraBodyLocks);
            totalLocked += countLocked(UnlockableSlotsConfig.extraCharmLocks);
            totalLocked += countLocked(UnlockableSlotsConfig.extraTrinketLocks);

            totalExtra += UnlockableSlotsConfig.extraAmuletLocks.length;
            totalExtra += UnlockableSlotsConfig.extraRingLocks.length;
            totalExtra += UnlockableSlotsConfig.extraBeltLocks.length;
            totalExtra += UnlockableSlotsConfig.extraHeadLocks.length;
            totalExtra += UnlockableSlotsConfig.extraBodyLocks.length;
            totalExtra += UnlockableSlotsConfig.extraCharmLocks.length;
            totalExtra += UnlockableSlotsConfig.extraTrinketLocks.length;

            System.out.println("  额外槽位总数: " + totalExtra);
            System.out.println("  默认锁定数量: " + totalLocked);
            System.out.println("  默认解锁数量: " + (totalExtra - totalLocked));
        }
    }

    /**
     * 统计锁定数量
     */
    private static int countLocked(boolean[] locks) {
        if (locks == null) return 0;
        int count = 0;
        for (boolean locked : locks) {
            if (locked) count++;
        }
        return count;
    }

    /**
     * 调试模式 - 打印详细信息
     */
    public static void debugMode() {
        System.out.println("\n========================================");
        System.out.println("[UnlockableSlots] 调试模式");
        System.out.println("========================================");

        // 打印配置详情
        UnlockableSlotsConfig.printConfig();

        // 打印槽位分配
        com.moremod.accessorybox.SlotLayoutHelper.calculateSlotAllocation();

        System.out.println("========================================\n");
    }
}
