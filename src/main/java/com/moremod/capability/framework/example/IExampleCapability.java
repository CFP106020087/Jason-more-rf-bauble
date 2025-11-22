package com.moremod.capability.framework.example;

import com.moremod.api.capability.ICapability;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 示例能力接口
 * 演示如何定义自定义能力
 */
public interface IExampleCapability extends ICapability<EntityPlayer> {

    /**
     * 获取能量值
     */
    int getEnergy();

    /**
     * 设置能量值
     */
    void setEnergy(int energy);

    /**
     * 添加能量
     * @return 实际添加的能量
     */
    int addEnergy(int amount);

    /**
     * 消耗能量
     * @return 是否成功消耗
     */
    boolean consumeEnergy(int amount);

    /**
     * 获取最大能量
     */
    int getMaxEnergy();
}
