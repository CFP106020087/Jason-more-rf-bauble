package com.moremod.accessorybox.compat;

import baubles.api.BaubleType;
import com.moremod.accessorybox.SlotLayoutHelper;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * SetBonus 兼容性辅助类 - 适配新的动态槽位系统
 */
public class SetBonusAccessoryBoxCompat {

    public static final int BAUBLES_OFFSET = Integer.MIN_VALUE + 1;

    /**
     * 验证 BaubleType.TRINKET 是否包含额外槽位
     */
    public static void verifyBaubleTypeModification() {
        System.out.println("\n==========================================");
        System.out.println("[AccessoryBox] Verifying BaubleType Modification");
        System.out.println("==========================================");

        try {
            int[] trinketSlots = BaubleType.TRINKET.getValidSlots();

            System.out.println("[AccessoryBox] BaubleType.TRINKET.getValidSlots():");
            System.out.println("[AccessoryBox]   Array: " + Arrays.toString(trinketSlots));
            System.out.println("[AccessoryBox]   Length: " + trinketSlots.length);

            // 获取所有额外槽位
            int[] extraSlots = SlotLayoutHelper.getAllExtraSlots();

            // 分析槽位
            int originalCount = 0;
            int extraCount = 0;

            System.out.println("\n[AccessoryBox] Slot Analysis:");
            for (int slot : trinketSlots) {
                if (SlotLayoutHelper.isExtraSlot(slot)) {
                    extraCount++;
                    System.out.println("[AccessoryBox]   ✓ EXTRA slot " + slot);
                } else {
                    originalCount++;
                    System.out.println("[AccessoryBox]   - Original slot " + slot);
                }
            }

            System.out.println("\n[AccessoryBox] Summary:");
            System.out.println("[AccessoryBox]   Original Baubles TRINKET slots: " + originalCount);
            System.out.println("[AccessoryBox]   AccessoryBox extra slots: " + extraCount);
            System.out.println("[AccessoryBox]   Total extra slots configured: " + extraSlots.length);

            if (extraCount == extraSlots.length) {
                System.out.println("\n[AccessoryBox] ✓✓✓ SUCCESS!");
                System.out.println("[AccessoryBox] BaubleType modification successful!");
                System.out.println("[AccessoryBox] SetBonus will automatically detect items in extra slots!");
                System.out.println("[AccessoryBox] No configuration changes needed!");
            } else if (extraCount > 0) {
                System.out.println("\n[AccessoryBox] ⚠ WARNING!");
                System.out.println("[AccessoryBox] Only " + extraCount + "/" + extraSlots.length + " extra slots detected");
            } else {
                System.out.println("\n[AccessoryBox] ✗ FAILED!");
                System.out.println("[AccessoryBox] No extra slots detected");
                System.out.println("[AccessoryBox] ASM patch may have failed");
            }

        } catch (Exception e) {
            System.err.println("[AccessoryBox] Verification failed:");
            e.printStackTrace();
        }

        System.out.println("==========================================\n");
    }

    /**
     * 测试 SetBonus 是否能正确检测额外槽位
     */
    public static void testSetBonusIntegration() {
        if (!Loader.isModLoaded("setbonus")) {
            System.out.println("[AccessoryBox] SetBonus not loaded, skipping integration test");
            return;
        }

        System.out.println("\n==========================================");
        System.out.println("[AccessoryBox] Testing SetBonus Integration");
        System.out.println("==========================================");

        try {
            Class<?> slotDataClass = Class.forName(
                    "com.fantasticsource.setbonus.common.bonusrequirements.setrequirement.SlotData"
            );

            Method getSlotIDsMethod = slotDataClass.getDeclaredMethod("getSlotIDs", String.class);

            System.out.println("\n[AccessoryBox] Calling SlotData.getSlotIDs(\"bauble_trinket\")...");

            @SuppressWarnings("unchecked")
            ArrayList<Integer> slots = (ArrayList<Integer>) getSlotIDsMethod.invoke(null, "bauble_trinket");

            System.out.println("[AccessoryBox] Result: " + slots.size() + " slots returned");

            // 获取配置的额外槽位
            int[] extraSlots = SlotLayoutHelper.getAllExtraSlots();

            // 分析结果
            int originalSlots = 0;
            int detectedExtraSlots = 0;

            System.out.println("\n[AccessoryBox] Slot Details:");
            for (int slot : slots) {
                int actualSlot = slot - BAUBLES_OFFSET;

                if (SlotLayoutHelper.isExtraSlot(actualSlot)) {
                    detectedExtraSlots++;
                    BaubleType type = SlotLayoutHelper.getExpectedTypeForSlot(actualSlot);
                    System.out.println("[AccessoryBox]   ✓ EXTRA slot " + actualSlot +
                            " (type: " + type + ", raw: " + slot + ")");
                } else {
                    originalSlots++;
                    System.out.println("[AccessoryBox]   - Original slot " + actualSlot + " (raw: " + slot + ")");
                }
            }

            System.out.println("\n[AccessoryBox] SetBonus Query Summary:");
            System.out.println("[AccessoryBox]   Original slots detected: " + originalSlots);
            System.out.println("[AccessoryBox]   Extra slots detected: " + detectedExtraSlots);
            System.out.println("[AccessoryBox]   Total extra slots configured: " + extraSlots.length);

            if (detectedExtraSlots == extraSlots.length) {
                System.out.println("\n[AccessoryBox] ✓✓✓ PERFECT!");
                System.out.println("[AccessoryBox] SetBonus integration fully working!");
                System.out.println("[AccessoryBox] Users can now use 'bauble_trinket' in SetBonus config");
                System.out.println("[AccessoryBox] Items in ANY accessory box slot will be detected!");
            } else {
                System.out.println("\n[AccessoryBox] ⚠ Integration issue detected");
            }

        } catch (Exception e) {
            System.err.println("[AccessoryBox] SetBonus integration test failed:");
            e.printStackTrace();
        }

        System.out.println("==========================================\n");
    }

    /**
     * 打印使用说明
     */
    public static void printUsageInstructions() {
        System.out.println("\n========================================");
        System.out.println("[AccessoryBox] SetBonus Configuration Guide");
        System.out.println("========================================");
        System.out.println();

        // 获取实际配置的槽位信息
        SlotLayoutHelper.SlotAllocation alloc = SlotLayoutHelper.calculateSlotAllocation();
        int[] extraSlots = SlotLayoutHelper.getAllExtraSlots();

        System.out.println("在 SetBonus 配置中，使用 'bauble_trinket' 会自动包含:");
        System.out.println("  - 原始 Baubles TRINKET 槽位 (0-6)");

        if (extraSlots.length > 0) {
            System.out.println("  - 配饰盒额外槽位: " + Arrays.toString(extraSlots));
            System.out.println();
            System.out.println("额外槽位详情:");
            printSlotTypeInfo("  AMULET", alloc.amuletSlots, 1);
            printSlotTypeInfo("  RING", alloc.ringSlots, 2);
            printSlotTypeInfo("  BELT", alloc.beltSlots, 1);
            printSlotTypeInfo("  HEAD", alloc.headSlots, 1);
            printSlotTypeInfo("  BODY", alloc.bodySlots, 1);
            printSlotTypeInfo("  CHARM", alloc.charmSlots, 1);
            printSlotTypeInfo("  TRINKET", alloc.trinketSlots, 7);
        } else {
            System.out.println("  - 当前未配置额外槽位");
        }

        System.out.println();
        System.out.println("配置示例:");
        System.out.println("  Equipment:");
        System.out.println("    Cube, enigmaticlegacy:enigmatic_amulet:1");
        System.out.println();
        System.out.println("  Sets:");
        System.out.println("    CubeSet, 非欧, bauble_trinket = Cube");
        System.out.println();
        System.out.println("  Bonuses:");
        System.out.println("    CubeBonus, 非欧效果, 0, CubeSet");
        System.out.println();
        System.out.println("现在 Cube 放在配饰盒的任何槽位都会触发套装效果!");
        System.out.println("========================================\n");
    }

    private static void printSlotTypeInfo(String typeName, int[] slots, int vanillaCount) {
        if (slots.length > vanillaCount) {
            System.out.print(typeName + ": ");
            for (int i = vanillaCount; i < slots.length; i++) {
                System.out.print(slots[i]);
                if (i < slots.length - 1) System.out.print(", ");
            }
            System.out.println();
        }
    }

    /**
     * 完整测试套件
     */
    public static void runFullTest() {
        System.out.println("\n");
        System.out.println("██████████████████████████████████████████████");
        System.out.println("█  AccessoryBox SetBonus Integration Test   █");
        System.out.println("██████████████████████████████████████████████");
        System.out.println();

        verifyBaubleTypeModification();

        if (Loader.isModLoaded("setbonus")) {
            testSetBonusIntegration();
        }

        printUsageInstructions();

        System.out.println("█████████████ Test Complete ████████████████\n");
    }
}