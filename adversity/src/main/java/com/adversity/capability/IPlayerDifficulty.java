package com.adversity.capability;

import net.minecraft.nbt.NBTTagCompound;

/**
 * 玩家难度设置接口
 */
public interface IPlayerDifficulty {

    /**
     * 获取玩家的难度倍率 (0.0 - 2.0)
     * 1.0 = 正常, 0.5 = 简单, 1.5 = 困难, 2.0 = 噩梦
     */
    float getDifficultyMultiplier();

    /**
     * 设置难度倍率
     */
    void setDifficultyMultiplier(float multiplier);

    /**
     * 获取玩家是否禁用难度系统
     */
    boolean isDifficultyDisabled();

    /**
     * 设置是否禁用难度系统
     */
    void setDifficultyDisabled(boolean disabled);

    /**
     * 获取玩家的击杀数（用于额外难度计算）
     */
    int getKillCount();

    /**
     * 增加击杀数
     */
    void addKill();

    /**
     * 重置击杀数
     */
    void resetKillCount();

    /**
     * 序列化
     */
    NBTTagCompound serializeNBT();

    /**
     * 反序列化
     */
    void deserializeNBT(NBTTagCompound nbt);
}
