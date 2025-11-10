package com.moremod.item.chengyue;

/**
 * 连击系统 - 纯计算
 */
public class ChengYueCombo {
    
    /**
     * 获取连击伤害加成
     */
    public static float getComboMultiplier(int combo) {
        if (combo == 0) {
            return 1.0f;
        }
        
        float multiplier;
        if (combo <= 10) {
            multiplier = 1.0f + combo * 0.05f;
        } else if (combo <= 30) {
            multiplier = 1.5f + (combo - 10) * 0.03f;
        } else {
            multiplier = 2.1f + (combo - 30) * 0.01f;
        }
        
        return Math.min(2.3f, multiplier);
    }
}