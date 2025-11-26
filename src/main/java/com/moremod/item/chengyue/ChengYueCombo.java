package com.moremod.item.chengyue;

/**
 * 连击系统 - 纯计算（平衡版）
 */
public class ChengYueCombo {

    /**
     * 获取连击伤害加成（已经削弱版）
     *
     * 目标：连击感保留，但不再指数爆炸
     *
     * 0  -> 1.0x
     * 10 -> 1.3x
     * 20 -> 1.5x
     * 30+-> 1.6x 封顶
     */
    public static float getComboMultiplier(int combo) {
        if (combo <= 0) {
            return 1.0f;
        }

        float mult;
        if (combo <= 10) {
            // 前 10 hits：每 hit +3%
            mult = 1.0f + combo * 0.03f;  // 1.0 ~ 1.3
        } else if (combo <= 20) {
            // 10~20：每 hit +2%
            mult = 1.3f + (combo - 10) * 0.02f; // 1.3 ~ 1.5
        } else {
            // 20 以上：每 hit +1%，整体 hard cap
            mult = 1.5f + (combo - 20) * 0.01f;
        }

        return Math.min(1.6f, mult);
    }
}
