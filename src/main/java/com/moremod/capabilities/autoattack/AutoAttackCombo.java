package com.moremod.capabilities.autoattack;

/**
 * 自动攻击连击数据实现（平衡版）
 * 
 * 考虑6槽叠加：
 * - 单个攻速：2-35倍
 * - 叠加后：12-210倍（0.5-8x Haste 255）
 * - 连击上限：20倍
 */
public class AutoAttackCombo implements IAutoAttackCombo {
    
    private boolean autoAttacking = false;
    private int comboCount = 0;
    private float comboPower = 1.0f;
    private long lastAttackTime = 0;
    private int comboTime = 0;
    private float attackSpeedMultiplier = 1.0f;
    
    @Override
    public boolean isAutoAttacking() {
        return autoAttacking;
    }
    
    @Override
    public void setAutoAttacking(boolean attacking) {
        this.autoAttacking = attacking;
    }
    
    @Override
    public int getComboCount() {
        return comboCount;
    }
    
    @Override
    public void setComboCount(int count) {
        this.comboCount = Math.max(0, count);
    }
    
    @Override
    public float getComboPower() {
        return comboPower;
    }
    
    @Override
    public void setComboPower(float power) {
        // 平衡版：最高20倍连击
        this.comboPower = Math.max(1.0f, Math.min(power, 20.0f));
    }
    
    @Override
    public long getLastAttackTime() {
        return lastAttackTime;
    }
    
    @Override
    public void setLastAttackTime(long time) {
        this.lastAttackTime = time;
    }
    
    @Override
    public int getComboTime() {
        return comboTime;
    }
    
    @Override
    public void setComboTime(int time) {
        this.comboTime = Math.max(0, time);
    }
    
    @Override
    public float getAttackSpeedMultiplier() {
        return attackSpeedMultiplier;
    }
    
    @Override
    public void setAttackSpeedMultiplier(float multiplier) {
        // 平衡版：考虑6槽叠加
        // 单个上限35倍，6个叠加 = 210倍 ≈ 8x Haste 255
        this.attackSpeedMultiplier = Math.max(0.1f, Math.min(multiplier, 250.0f));
    }
    
    @Override
    public void resetCombo() {
        this.comboCount = 0;
        this.comboPower = 1.0f;
        this.comboTime = 0;
    }
}