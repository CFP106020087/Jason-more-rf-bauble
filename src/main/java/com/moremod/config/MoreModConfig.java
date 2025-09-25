package com.moremod.config;

import net.minecraftforge.fml.common.Loader;

public class MoreModConfig {
    // 槽位分配：
    // 0-13: EL的槽位
    // 14-20: MoreMod的7个镜像槽位
    public static final int TOTAL_SLOTS = 21;
    public static final int MOREMOD_START_SLOT = 14;
    public static final int MOREMOD_EXTRA_SLOTS = 7;

    // 槽位类型映射
    public static final int EXTRA_AMULET_SLOT = 14;     // 额外项链
    public static final int EXTRA_RING_SLOT_1 = 15;    // 额外戒指1
    public static final int EXTRA_RING_SLOT_2 = 16;    // 额外戒指2
    public static final int EXTRA_BELT_SLOT = 17;      // 额外腰带
    public static final int EXTRA_HEAD_SLOT = 18;      // 额外头部
    public static final int EXTRA_BODY_SLOT = 19;      // 额外身体
    public static final int EXTRA_CHARM_SLOT = 20;     // 额外护符

    public static boolean isExtendingEL() {
        return Loader.isModLoaded("enigmaticlegacy");
    }

    public static boolean isMoreModSlot(int slot) {
        return slot >= MOREMOD_START_SLOT && slot < TOTAL_SLOTS;
    }

    // 获取槽位对应的原始类型
    public static String getSlotType(int slot) {
        switch(slot) {
            case 14: return "AMULET";
            case 15:
            case 16: return "RING";
            case 17: return "BELT";
            case 18: return "HEAD";
            case 19: return "BODY";
            case 20: return "CHARM";
            default: return "UNKNOWN";
        }
    }
}