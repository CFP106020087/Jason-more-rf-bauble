package com.moremod.item.chengyue;

/**
 * 连击系统 - 纯计算（平衡版）
 */
public class ChengYueCombo {

    /**
     * 获取连击伤害加成（平衡版）
     *
     * 目标：连击感保留，上限降低
     *
     * 0  -> 1.0x
     * 10 -> 1.2x
     * 20 -> 1.3x
     * 30+-> 1.35x 封顶
     */
    public static float getComboMultiplier(int combo) {
        if (combo <= 0) {
            return 1.0f;
        }

        float mult;
        if (combo <= 10) {
            // 前 10 hits：每 hit +2%
            mult = 1.0f + combo * 0.02f;  // 1.0 ~ 1.2
        } else if (combo <= 20) {
            // 10~20：每 hit +1%
            mult = 1.2f + (combo - 10) * 0.01f; // 1.2 ~ 1.3
        } else {
            // 20 以上：每 hit +0.5%，整体 hard cap
            mult = 1.3f + (combo - 20) * 0.005f;
        }

        return Math.min(1.35f, mult);
    }
}
