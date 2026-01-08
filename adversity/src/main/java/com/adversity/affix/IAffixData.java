package com.adversity.affix;

import net.minecraft.nbt.NBTTagCompound;

/**
 * 词条数据接口 - 存储单个词条实例的运行时数据
 */
public interface IAffixData {

    /**
     * 获取关联的词条
     */
    IAffix getAffix();

    /**
     * 获取自定义数据存储
     */
    NBTTagCompound getCustomData();

    /**
     * 设置自定义数据
     */
    void setCustomData(NBTTagCompound data);

    /**
     * 获取词条激活的 tick 计数
     */
    int getTickCount();

    /**
     * 增加 tick 计数
     */
    void incrementTick();

    /**
     * 重置 tick 计数
     */
    void resetTick();

    /**
     * 检查词条是否处于激活状态
     */
    boolean isActive();

    /**
     * 设置词条激活状态
     */
    void setActive(boolean active);

    /**
     * 获取冷却时间剩余
     */
    int getCooldown();

    /**
     * 设置冷却时间
     */
    void setCooldown(int ticks);

    /**
     * 减少冷却时间
     */
    void decrementCooldown();
}
