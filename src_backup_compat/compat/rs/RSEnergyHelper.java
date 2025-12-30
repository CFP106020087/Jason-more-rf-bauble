package com.moremod.compat.rs;

/**
 * RS 能量计算帮助类
 */
public class RSEnergyHelper {
    
    /**
     * 获取两张卡同时使用的能耗
     */
    public static int getBothCardsEnergy() {
        return RSConfig.infinityCardEnergyUsage + RSConfig.dimensionCardEnergyUsage;
    }
    
    /**
     * 获取无限卡的能耗
     */
    public static int getInfinityCardEnergy() {
        return RSConfig.infinityCardEnergyUsage;
    }
    
    /**
     * 获取维度卡的能耗
     */
    public static int getDimensionCardEnergy() {
        return RSConfig.dimensionCardEnergyUsage;
    }
}
