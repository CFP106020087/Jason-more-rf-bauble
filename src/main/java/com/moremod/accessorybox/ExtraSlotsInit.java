package com.moremod.accessorybox;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * 额外槽位系统初始化 - 动态配置版
 * 在你的主mod类中调用这些方法
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ExtraSlotsInit {

    /**
     * PreInit阶段 - 读取并验证配置
     */
    public static void preInit(FMLPreInitializationEvent event) {
        System.out.println("========================================");
        System.out.println("[ExtraSlots] 额外槽位系统预初始化");
        System.out.println("========================================");

        if (!ExtraBaublesConfig.enableExtraSlots) {
            System.out.println("[ExtraSlots] 系统已禁用");
            System.out.println("========================================");
            return;
        }

        // 打印配置详情（使用配置类的方法）
        ExtraBaublesConfig.printConfig();
    }

    /**
     * Init阶段 - 显示槽位布局信息
     */
    public static void init(FMLInitializationEvent event) {
        if (!ExtraBaublesConfig.enableExtraSlots) {
            return;
        }

        System.out.println("========================================");
        System.out.println("[ExtraSlots] 初始化槽位布局");
        System.out.println("========================================");

        // 获取槽位分配
        SlotLayoutHelper.SlotAllocation alloc = SlotLayoutHelper.calculateSlotAllocation();

        System.out.println("[ExtraSlots] 槽位分配详情:");
        System.out.println("----------------------------------------");

        // 显示每种类型的槽位信息
        printTypeInfo("AMULET", alloc.amuletSlots, 1);
        printTypeInfo("RING", alloc.ringSlots, 2);
        printTypeInfo("BELT", alloc.beltSlots, 1);
        printTypeInfo("HEAD", alloc.headSlots, 1);
        printTypeInfo("BODY", alloc.bodySlots, 1);
        printTypeInfo("CHARM", alloc.charmSlots, 1);
        printTypeInfo("TRINKET", alloc.trinketSlots, 7);

        System.out.println("----------------------------------------");
        System.out.println("[ExtraSlots] 总槽位数: " + alloc.totalSlots);
        System.out.println("[ExtraSlots] 最大槽位ID: " + (alloc.totalSlots - 1));

        // 验证槽位ID范围
        if (alloc.totalSlots > 301) {
            System.err.println("========================================");
            System.err.println("[ExtraSlots] ⚠ 警告: 槽位数超出范围!");
            System.err.println("[ExtraSlots] 当前: " + alloc.totalSlots + " (限制: 301)");
            System.err.println("========================================");
        } else {
            System.out.println("[ExtraSlots] ✓ 槽位ID范围验证通过");
        }

        System.out.println("========================================");
        System.out.println("[ExtraSlots] ✓ 槽位布局初始化完成");
        System.out.println("========================================");
    }

    /**
     * 打印指定类型的槽位信息
     */
    private static void printTypeInfo(String typeName, int[] slots, int vanillaCount) {
        int extraCount = slots.length - vanillaCount;

        if (extraCount > 0) {
            // 格式化原版槽位
            String vanillaSlots = formatVanillaSlots(slots, vanillaCount);

            // 格式化额外槽位范围
            String extraSlots = "";
            if (extraCount > 0) {
                int firstExtra = slots[vanillaCount];
                int lastExtra = slots[slots.length - 1];
                if (extraCount == 1) {
                    extraSlots = String.valueOf(firstExtra);
                } else {
                    extraSlots = firstExtra + "-" + lastExtra;
                }
            }

            System.out.println(String.format("  %-8s: %2d 个额外 | 原版: %-7s | 额外: %s | 总计: %d",
                    typeName,
                    extraCount,
                    vanillaSlots,
                    extraSlots,
                    slots.length
            ));
        } else {
            // 只有原版槽位
            String vanillaSlots = formatVanillaSlots(slots, vanillaCount);
            System.out.println(String.format("  %-8s: 无额外   | 原版: %-7s | 总计: %d",
                    typeName,
                    vanillaSlots,
                    slots.length
            ));
        }
    }

    /**
     * 格式化原版槽位显示
     */
    private static String formatVanillaSlots(int[] slots, int vanillaCount) {
        if (vanillaCount == 0) return "-";
        if (vanillaCount == 1) return String.valueOf(slots[0]);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vanillaCount; i++) {
            sb.append(slots[i]);
            if (i < vanillaCount - 1) sb.append(",");
        }
        return sb.toString();
    }

    /**
     * 调试输出 - 显示所有槽位的详细信息
     */
    public static void debugSlotInfo() {
        System.out.println("========================================");
        System.out.println("[ExtraSlots] 槽位调试信息");
        System.out.println("========================================");

        SlotLayoutHelper.SlotAllocation alloc = SlotLayoutHelper.calculateSlotAllocation();

        debugTypeSlots("AMULET", alloc.amuletSlots, 1);
        debugTypeSlots("RING", alloc.ringSlots, 2);
        debugTypeSlots("BELT", alloc.beltSlots, 1);
        debugTypeSlots("HEAD", alloc.headSlots, 1);
        debugTypeSlots("BODY", alloc.bodySlots, 1);
        debugTypeSlots("CHARM", alloc.charmSlots, 1);
        debugTypeSlots("TRINKET", alloc.trinketSlots, 7);

        System.out.println("\n========================================");
    }

    /**
     * 调试单个类型的槽位信息
     */
    private static void debugTypeSlots(String typeName, int[] slots, int vanillaCount) {
        System.out.println("\n类型: " + typeName);
        System.out.println("  总槽位数: " + slots.length);
        System.out.println("  原版槽位: " + vanillaCount);
        System.out.println("  额外槽位: " + (slots.length - vanillaCount));

        // 显示 validSlots 数组
        System.out.print("  ValidSlots: [");
        int displayCount = Math.min(slots.length, 10);
        for (int i = 0; i < displayCount; i++) {
            System.out.print(slots[i]);
            if (i < displayCount - 1) System.out.print(", ");
        }
        if (slots.length > 10) {
            System.out.print("... +" + (slots.length - 10) + " more");
        }
        System.out.println("]");

        // 显示额外槽位的坐标
        if (slots.length > vanillaCount) {
            System.out.println("  额外槽位坐标:");
            int extraCount = slots.length - vanillaCount;
            int showCount = Math.min(extraCount, 5);

            for (int i = 0; i < showCount; i++) {
                int slotId = slots[vanillaCount + i];
                Point coord = DynamicGuiLayout.getSlotPosition(slotId);
                System.out.println("    槽位 " + slotId + ": " + coord);
            }

            if (extraCount > 5) {
                System.out.println("    ... +" + (extraCount - 5) + " more");
            }
        }
    }

    /**
     * 显示 GUI 布局信息
     */
    public static void debugGuiLayout() {
        System.out.println("========================================");
        System.out.println("[ExtraSlots] GUI 布局调试信息");
        System.out.println("========================================");

        SlotLayoutHelper.SlotAllocation alloc = SlotLayoutHelper.calculateSlotAllocation();

        System.out.println("\n原版槽位坐标 (固定):");
        printSlotCoord("AMULET(0)", 0);
        printSlotCoord("RING(1)", 1);
        printSlotCoord("RING(2)", 2);
        printSlotCoord("BELT(3)", 3);
        printSlotCoord("HEAD(4)", 4);
        printSlotCoord("BODY(5)", 5);
        printSlotCoord("CHARM(6)", 6);

        System.out.println("\n额外槽位坐标 (动态):");

        if (alloc.amuletSlots.length > 1) {
            System.out.println("\nAMULET 额外槽位:");
            printExtraSlots(alloc.amuletSlots, 1);
        }

        if (alloc.ringSlots.length > 2) {
            System.out.println("\nRING 额外槽位:");
            printExtraSlots(alloc.ringSlots, 2);
        }

        if (alloc.beltSlots.length > 1) {
            System.out.println("\nBELT 额外槽位:");
            printExtraSlots(alloc.beltSlots, 1);
        }

        if (alloc.headSlots.length > 1) {
            System.out.println("\nHEAD 额外槽位:");
            printExtraSlots(alloc.headSlots, 1);
        }

        if (alloc.bodySlots.length > 1) {
            System.out.println("\nBODY 额外槽位:");
            printExtraSlots(alloc.bodySlots, 1);
        }

        if (alloc.charmSlots.length > 1) {
            System.out.println("\nCHARM 额外槽位:");
            printExtraSlots(alloc.charmSlots, 1);
        }

        if (alloc.trinketSlots.length > 7) {
            System.out.println("\nTRINKET 额外槽位:");
            printExtraSlots(alloc.trinketSlots, 7);
        }

        System.out.println("\n========================================");
    }

    /**
     * 打印单个槽位的坐标
     */
    private static void printSlotCoord(String label, int slotId) {
        Point coord = DynamicGuiLayout.getSlotPosition(slotId);
        System.out.println("  " + label + ": " + coord);
    }

    /**
     * 打印额外槽位的坐标
     */
    private static void printExtraSlots(int[] slots, int vanillaCount) {
        int extraCount = slots.length - vanillaCount;
        int showCount = Math.min(extraCount, 5);

        for (int i = 0; i < showCount; i++) {
            int slotId = slots[vanillaCount + i];
            Point coord = DynamicGuiLayout.getSlotPosition(slotId);
            System.out.println("  槽位 " + slotId + ": " + coord);
        }

        if (extraCount > 5) {
            System.out.println("  ... +" + (extraCount - 5) + " more");
        }
    }
}