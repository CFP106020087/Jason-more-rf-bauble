package com.moremod.accessorybox;

import baubles.api.BaubleType;
import com.moremod.accessorybox.unlock.UnlockableSlotsConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * 槽位布局计算器 - 增强版
 * 支持槽位信息查询
 */
public class SlotLayoutHelper {

    // 缓存槽位信息映射 SlotID -> SlotInfo
    private static Map<Integer, UnlockableSlotsConfig.SlotInfo> slotInfoCache = new HashMap<>();
    private static SlotAllocation cachedAllocation = null;

    /**
     * 槽位分配结果
     */
    public static class SlotAllocation {
        public final int[] amuletSlots;
        public final int[] ringSlots;
        public final int[] beltSlots;
        public final int[] headSlots;
        public final int[] bodySlots;
        public final int[] charmSlots;
        public final int[] trinketSlots;
        public final int totalSlots;

        public SlotAllocation(int[] amuletSlots, int[] ringSlots, int[] beltSlots,
                              int[] headSlots, int[] bodySlots, int[] charmSlots,
                              int[] trinketSlots, int totalSlots) {
            this.amuletSlots = amuletSlots;
            this.ringSlots = ringSlots;
            this.beltSlots = beltSlots;
            this.headSlots = headSlots;
            this.bodySlots = bodySlots;
            this.charmSlots = charmSlots;
            this.trinketSlots = trinketSlots;
            this.totalSlots = totalSlots;
        }
    }

    /**
     * 计算槽位分配
     */
    public static SlotAllocation calculateSlotAllocation() {
        if (cachedAllocation != null) {
            return cachedAllocation;
        }

        // 从配置读取
        int extraAmulets = EarlyConfigLoader.extraAmulets;
        int extraRings = EarlyConfigLoader.extraRings;
        int extraBelts = EarlyConfigLoader.extraBelts;
        int extraHeads = EarlyConfigLoader.extraHeads;
        int extraBodies = EarlyConfigLoader.extraBodies;
        int extraCharms = EarlyConfigLoader.extraCharms;
        int extraTrinkets = EarlyConfigLoader.extraTrinkets;

        // 原版槽位 ID
        int[] vanillaAmulet = {0};
        int[] vanillaRing = {1, 2};
        int[] vanillaBelt = {3};
        int[] vanillaHead = {4};
        int[] vanillaBody = {5};
        int[] vanillaCharm = {6};
        int[] vanillaTrinket = {0, 1, 2, 3, 4, 5, 6};

        // 下一个可用槽位 ID（从 7 开始）
        int nextSlotId = 7;

        // AMULET: 原版 [0] + 额外
        int[] amuletSlots = createSlotArray(vanillaAmulet, extraAmulets, nextSlotId);
        buildSlotInfo(amuletSlots, 1, "AMULET");
        nextSlotId += extraAmulets;

        // RING: 原版 [1, 2] + 额外
        int[] ringSlots = createSlotArray(vanillaRing, extraRings, nextSlotId);
        buildSlotInfo(ringSlots, 2, "RING");
        nextSlotId += extraRings;

        // BELT: 原版 [3] + 额外
        int[] beltSlots = createSlotArray(vanillaBelt, extraBelts, nextSlotId);
        buildSlotInfo(beltSlots, 1, "BELT");
        nextSlotId += extraBelts;

        // HEAD: 原版 [4] + 额外
        int[] headSlots = createSlotArray(vanillaHead, extraHeads, nextSlotId);
        buildSlotInfo(headSlots, 1, "HEAD");
        nextSlotId += extraHeads;

        // BODY: 原版 [5] + 额外
        int[] bodySlots = createSlotArray(vanillaBody, extraBodies, nextSlotId);
        buildSlotInfo(bodySlots, 1, "BODY");
        nextSlotId += extraBodies;

        // CHARM: 原版 [6] + 额外
        int[] charmSlots = createSlotArray(vanillaCharm, extraCharms, nextSlotId);
        buildSlotInfo(charmSlots, 1, "CHARM");
        nextSlotId += extraCharms;

        // TRINKET: 原版 [0-6] + 额外
        int[] trinketSlots = createSlotArray(vanillaTrinket, extraTrinkets, nextSlotId);
        buildSlotInfo(trinketSlots, 7, "TRINKET");
        nextSlotId += extraTrinkets;

        // 计算总槽位数
        int totalSlots = 7 + EarlyConfigLoader.getTotalExtraSlots();

        cachedAllocation = new SlotAllocation(
                amuletSlots, ringSlots, beltSlots,
                headSlots, bodySlots, charmSlots, trinketSlots,
                totalSlots
        );

        printAllocation(cachedAllocation);

        return cachedAllocation;
    }

    /**
     * 创建槽位数组
     */
    private static int[] createSlotArray(int[] vanillaSlots, int extraCount, int startId) {
        int[] result = new int[vanillaSlots.length + extraCount];

        // 复制原版槽位
        System.arraycopy(vanillaSlots, 0, result, 0, vanillaSlots.length);

        // 添加额外槽位
        for (int i = 0; i < extraCount; i++) {
            result[vanillaSlots.length + i] = startId + i;
        }

        return result;
    }

    /**
     * 构建槽位信息映射
     */
    private static void buildSlotInfo(int[] slots, int vanillaCount, String type) {
        for (int i = 0; i < slots.length; i++) {
            int slotId = slots[i];
            
            // 只为额外槽位创建信息
            if (i >= vanillaCount) {
                int extraIndex = i - vanillaCount;
                slotInfoCache.put(slotId, new UnlockableSlotsConfig.SlotInfo(type, extraIndex, slotId));
            }
        }
    }

    /**
     * 根据槽位ID获取槽位信息
     */
    public static UnlockableSlotsConfig.SlotInfo getSlotInfo(int slotId) {
        if (slotInfoCache.isEmpty()) {
            calculateSlotAllocation(); // 确保已初始化
        }
        return slotInfoCache.get(slotId);
    }

    /**
     * 根据类型获取所有槽位ID
     */
    public static int[] getSlotIdsForType(String type) {
        SlotAllocation alloc = calculateSlotAllocation();
        switch (type.toUpperCase()) {
            case "AMULET": return alloc.amuletSlots;
            case "RING": return alloc.ringSlots;
            case "BELT": return alloc.beltSlots;
            case "HEAD": return alloc.headSlots;
            case "BODY": return alloc.bodySlots;
            case "CHARM": return alloc.charmSlots;
            case "TRINKET": return alloc.trinketSlots;
            default: return new int[0];
        }
    }

    /**
     * 获取指定类型的 validSlots 数组
     */
    public static int[] getValidSlotsForType(BaubleType type) {
        SlotAllocation alloc = calculateSlotAllocation();
        switch (type) {
            case AMULET:  return alloc.amuletSlots;
            case RING:    return alloc.ringSlots;
            case BELT:    return alloc.beltSlots;
            case HEAD:    return alloc.headSlots;
            case BODY:    return alloc.bodySlots;
            case CHARM:   return alloc.charmSlots;
            case TRINKET: return alloc.trinketSlots;
            default:      return new int[0];
        }
    }

    /**
     * 打印槽位分配详情
     */
    private static void printAllocation(SlotAllocation alloc) {
        System.out.println("========== 槽位分配详情 ==========");
        System.out.println("AMULET:  " + formatSlotArray(alloc.amuletSlots));
        System.out.println("RING:    " + formatSlotArray(alloc.ringSlots));
        System.out.println("BELT:    " + formatSlotArray(alloc.beltSlots));
        System.out.println("HEAD:    " + formatSlotArray(alloc.headSlots));
        System.out.println("BODY:    " + formatSlotArray(alloc.bodySlots));
        System.out.println("CHARM:   " + formatSlotArray(alloc.charmSlots));
        System.out.println("TRINKET: " + formatSlotArray(alloc.trinketSlots));
        System.out.println("--------------------------------------");
        System.out.println("总槽位数: " + alloc.totalSlots);
        System.out.println("======================================");
    }

    /**
     * 格式化槽位数组为字符串
     */
    private static String formatSlotArray(int[] slots) {
        if (slots.length == 0) return "[]";
        if (slots.length == 1) return "[" + slots[0] + "]";

        StringBuilder sb = new StringBuilder("[");

        // 检查是否是连续的
        boolean continuous = true;
        for (int i = 1; i < slots.length; i++) {
            if (slots[i] != slots[i-1] + 1) {
                continuous = false;
                break;
            }
        }

        if (continuous && slots.length > 2) {
            // 连续槽位，使用简写 [1-5]
            sb.append(slots[0]).append("-").append(slots[slots.length - 1]);
        } else {
            // 非连续，列出所有
            for (int i = 0; i < Math.min(slots.length, 7); i++) {
                sb.append(slots[i]);
                if (i < slots.length - 1) sb.append(", ");
            }
            if (slots.length > 7) {
                sb.append(", ..., ").append(slots[slots.length - 1]);
            }
        }

        sb.append("]");
        return sb.toString();
    }

    /**
     * 清除缓存（配置更改时调用）
     */
    /**
     * 判断槽位是否为额外槽位
     */
    public static boolean isExtraSlot(int slotId) {
        return slotId >= 7;
    }


    /**
     * 容器槽位ID → 逻辑槽位ID
     * 因为旧系统使用 14-21 来避开 Baubles 硬编码的 0-6
     */
    public static int toLogicalSlot(int containerSlot) {

        return containerSlot;
    }

    /**
     * 逻辑槽位ID → 容器槽位ID
     */
    public static int toContainerSlot(int logicalSlot) {
        if (logicalSlot < 7) {
            return logicalSlot;  // 原版槽位
        }
        return logicalSlot + 7;  // 额外槽位：7→14, 8→15...
    }


    /**
     * 获取所有额外槽位ID
     */
    public static int[] getAllExtraSlots() {
        SlotAllocation alloc = calculateSlotAllocation();
        java.util.Set<Integer> extraSlots = new java.util.HashSet<>();

        // 收集所有额外槽位（跳过原版槽位）
        addExtraSlots(extraSlots, alloc.amuletSlots, 1);
        addExtraSlots(extraSlots, alloc.ringSlots, 2);
        addExtraSlots(extraSlots, alloc.beltSlots, 1);
        addExtraSlots(extraSlots, alloc.headSlots, 1);
        addExtraSlots(extraSlots, alloc.bodySlots, 1);
        addExtraSlots(extraSlots, alloc.charmSlots, 1);
        addExtraSlots(extraSlots, alloc.trinketSlots, 7);

        return extraSlots.stream().mapToInt(Integer::intValue).sorted().toArray();
    }

    private static void addExtraSlots(java.util.Set<Integer> set, int[] slots, int vanillaCount) {
        for (int i = vanillaCount; i < slots.length; i++) {
            set.add(slots[i]);
        }
    }

    /**
     * 根据槽位ID获取期望的BaubleType
     */
    /**
     * 根据槽位ID获取期望的BaubleType
     */
    public static BaubleType getExpectedTypeForSlot(int slotId) {
        // 原版槽位优先判断（0-6）
        if (slotId < 7) {
            switch (slotId) {
                case 0: return BaubleType.AMULET;
                case 1:
                case 2: return BaubleType.RING;
                case 3: return BaubleType.BELT;
                case 4: return BaubleType.HEAD;
                case 5: return BaubleType.BODY;
                case 6: return BaubleType.CHARM;
                default: return BaubleType.TRINKET;
            }
        }

        // 额外槽位
        SlotAllocation alloc = calculateSlotAllocation();

        if (contains(alloc.amuletSlots, slotId)) return BaubleType.AMULET;
        if (contains(alloc.ringSlots, slotId)) return BaubleType.RING;
        if (contains(alloc.beltSlots, slotId)) return BaubleType.BELT;
        if (contains(alloc.headSlots, slotId)) return BaubleType.HEAD;
        if (contains(alloc.bodySlots, slotId)) return BaubleType.BODY;
        if (contains(alloc.charmSlots, slotId)) return BaubleType.CHARM;
        if (contains(alloc.trinketSlots, slotId)) return BaubleType.TRINKET;

        return BaubleType.TRINKET; // 默认
    }

    private static boolean contains(int[] array, int value) {
        for (int v : array) {
            if (v == value) return true;
        }
        return false;
    }
    public static void clearCache() {
        cachedAllocation = null;
        slotInfoCache.clear();
    }
}
