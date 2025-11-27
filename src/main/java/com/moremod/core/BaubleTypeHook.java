package com.moremod.core;

import baubles.api.BaubleType;
import com.moremod.accessorybox.SlotLayoutHelper;

public final class BaubleTypeHook {
    private BaubleTypeHook() {}

    public static boolean hasSlotBridged(BaubleType self, int slotId) {
        if (slotId < 0) return false;

        // 动态推导全局总槽位：取所有类型数组的最大 id + 1（最稳）
        int total = totalSlotsByLayout();
        if (slotId >= total) return false;

        // ★ 物品类型是 TRINKET -> 任意槽位都可
        if (self == BaubleType.TRINKET) return true;

        // 本类型命中 or TRINKET 类型集合命中（保留你原本“TRINKET 白名单”习惯）
        SlotLayoutHelper.SlotAllocation a = SlotLayoutHelper.calculateSlotAllocation();
        return inType(self, slotId, a) || inType(BaubleType.TRINKET, slotId, a);
    }

    private static boolean inType(BaubleType t, int id, SlotLayoutHelper.SlotAllocation a) {
        int[] arr =
                t == BaubleType.AMULET ? a.amuletSlots :
                        t == BaubleType.RING   ? a.ringSlots   :
                                t == BaubleType.BELT   ? a.beltSlots   :
                                        t == BaubleType.HEAD   ? a.headSlots   :
                                                t == BaubleType.BODY   ? a.bodySlots   :
                                                        t == BaubleType.CHARM  ? a.charmSlots  :
                                                                a.trinketSlots;
        if (arr == null) return false;
        for (int s : arr) if (s == id) return true;
        return false;
    }

    private static int totalSlotsByLayout() {
        int max = -1;
        max = Math.max(max, maxIn(SlotLayoutHelper.getSlotIdsForType("AMULET")));
        max = Math.max(max, maxIn(SlotLayoutHelper.getSlotIdsForType("RING")));
        max = Math.max(max, maxIn(SlotLayoutHelper.getSlotIdsForType("BELT")));
        max = Math.max(max, maxIn(SlotLayoutHelper.getSlotIdsForType("HEAD")));
        max = Math.max(max, maxIn(SlotLayoutHelper.getSlotIdsForType("BODY")));
        max = Math.max(max, maxIn(SlotLayoutHelper.getSlotIdsForType("CHARM")));
        max = Math.max(max, maxIn(SlotLayoutHelper.getSlotIdsForType("TRINKET")));
        return max + 1; // 没有槽位时返回 0
    }

    private static int maxIn(int[] arr) {
        if (arr == null || arr.length == 0) return -1;
        int m = -1; for (int v : arr) if (v > m) m = v; return m;
    }
}
