package com.moremod.integration.jei.generator;

import net.minecraft.item.ItemStack;

/**
 * 发电机燃料数据类
 * 用于JEI显示
 */
public class GeneratorFuel {
    private final ItemStack fuel;
    private final int totalRF;
    private final int burnTime;
    private final int rfPerTick;

    public GeneratorFuel(ItemStack fuel, int totalRF, int burnTime, int rfPerTick) {
        this.fuel = fuel;
        this.totalRF = totalRF;
        this.burnTime = burnTime;
        this.rfPerTick = rfPerTick;
    }

    public ItemStack getFuel() {
        return fuel;
    }

    public int getTotalRF() {
        return totalRF;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public int getRFPerTick() {
        return rfPerTick;
    }

    /**
     * 获取燃烧时间（秒）
     */
    public float getBurnTimeSeconds() {
        return burnTime / 20.0f;
    }
}
