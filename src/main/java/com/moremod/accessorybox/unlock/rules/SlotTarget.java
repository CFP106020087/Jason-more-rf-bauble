package com.moremod.accessorybox.unlock.rules;

import com.moremod.accessorybox.SlotLayoutHelper;

/**
 * 槽位目标
 * 标识要解锁的槽位
 */
public class SlotTarget {
    private final String type;  // AMULET, RING, BELT, etc.
    private final int extraIndex;  // 额外槽位索引 (0, 1, 2...)
    private int slotId = -1;  // 缓存的实际槽位ID

    public SlotTarget(String type, int extraIndex) {
        this.type = type.toUpperCase();
        this.extraIndex = extraIndex;
    }

    /**
     * 获取类型
     */
    public String getType() {
        return type;
    }

    /**
     * 获取额外槽位索引
     */
    public int getExtraIndex() {
        return extraIndex;
    }

    /**
     * 获取实际的槽位ID
     * 通过SlotLayoutHelper查询
     */
    public int getSlotId() {
        if (slotId == -1) {
            calculateSlotId();
        }
        return slotId;
    }

    /**
     * 计算实际槽位ID
     */
    private void calculateSlotId() {
        int[] slots = SlotLayoutHelper.getSlotIdsForType(type);
        
        // 获取原版槽位数量
        int vanillaCount = getVanillaCount(type);
        
        // 计算额外槽位的实际索引
        int actualIndex = vanillaCount + extraIndex;
        
        if (actualIndex >= 0 && actualIndex < slots.length) {
            slotId = slots[actualIndex];
        } else {
            System.err.println("[SlotTarget] 槽位索引越界: " + type + ":" + extraIndex);
            System.err.println("[SlotTarget] 可用范围: 0-" + (slots.length - vanillaCount - 1));
            slotId = -1;
        }
    }

    /**
     * 获取原版槽位数量
     */
    private int getVanillaCount(String type) {
        switch (type) {
            case "AMULET": return 1;
            case "RING": return 2;
            case "BELT": return 1;
            case "HEAD": return 1;
            case "BODY": return 1;
            case "CHARM": return 1;
            case "TRINKET": return 7;
            default: return 0;
        }
    }

    /**
     * 检查槽位目标是否有效
     */
    public boolean isValid() {
        return getSlotId() >= 0;
    }

    @Override
    public String toString() {
        return type + ":" + extraIndex + " (slotId=" + getSlotId() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SlotTarget)) return false;
        SlotTarget other = (SlotTarget) obj;
        return this.type.equals(other.type) && this.extraIndex == other.extraIndex;
    }

    @Override
    public int hashCode() {
        return type.hashCode() * 31 + extraIndex;
    }
}
