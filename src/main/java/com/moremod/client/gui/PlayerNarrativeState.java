package com.moremod.client.gui;

public enum PlayerNarrativeState {
    HUMAN_HIGH,      // 高人性：乾淨，無特效
    HUMAN_LOW,       // 低人性：紅色警告，視野模糊，心跳聲
    BROKEN_GOD,      // 破碎之神：銀色裂痕，數位雜訊，機械運轉聲
    SHAMBHALA,       // 香巴拉化身：金色光暈，神聖幾何，空靈吟唱
    NONE             // 無 (剛出生或特殊情況)
}