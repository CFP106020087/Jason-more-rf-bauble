package com.moremod.capabilities.autoattack;

/**
 * 自动攻击连击数据接口
 * 
 * 存储：
 * - 是否正在自动攻击
 * - 连击次数
 * - 连击倍率
 * - 最后攻击时间
 */
public interface IAutoAttackCombo {
    
    /**
     * 是否正在自动攻击
     */
    boolean isAutoAttacking();
    void setAutoAttacking(boolean attacking);
    
    /**
     * 连击次数
     */
    int getComboCount();
    void setComboCount(int count);
    
    /**
     * 连击伤害倍率
     */
    float getComboPower();
    void setComboPower(float power);
    
    /**
     * 最后攻击时间（tick）
     */
    long getLastAttackTime();
    void setLastAttackTime(long time);
    
    /**
     * 连击持续时间（tick，用于判断是否中断）
     */
    int getComboTime();
    void setComboTime(int time);
    
    /**
     * 攻击间隔倍率（由宝石词条控制）
     */
    float getAttackSpeedMultiplier();
    void setAttackSpeedMultiplier(float multiplier);
    
    /**
     * 重置连击数据
     */
    void resetCombo();
}