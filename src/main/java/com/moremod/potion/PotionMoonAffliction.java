package com.moremod.potion;

import net.minecraft.potion.Potion;

/**
 * 月殇药水效果 - 易伤效果
 */
public class PotionMoonAffliction extends Potion {

    public PotionMoonAffliction() {
        super(true, 0x4A0E4E); // 深紫色
        setRegistryName("moremod", "moon_affliction");
        setPotionName("effect.moon_affliction");
        setIconIndex(0, 0);
    }

    /**
     * 获取易伤倍率
     */
    public static float getDamageMultiplier(int amplifier) {
        return 1.0f + (amplifier + 1) * 0.03f;
    }
}