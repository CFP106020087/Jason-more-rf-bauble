package com.moremod.quarry;

import net.minecraft.util.IStringSerializable;

/**
 * 量子采石场工作模式
 */
public enum QuarryMode implements IStringSerializable {
    MINING("mining", 0),           // 挖矿模式 - 模拟矿物生成
    MOB_DROPS("mob_drops", 1),     // 怪物掉落模式
    LOOT_TABLE("loot_table", 2);   // 自定义战利品表模式

    private final String name;
    private final int meta;

    QuarryMode(String name, int meta) {
        this.name = name;
        this.meta = meta;
    }

    @Override
    public String getName() {
        return name;
    }

    public int getMeta() {
        return meta;
    }

    public static QuarryMode fromMeta(int meta) {
        for (QuarryMode mode : values()) {
            if (mode.meta == meta) return mode;
        }
        return MINING;
    }

    public QuarryMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
