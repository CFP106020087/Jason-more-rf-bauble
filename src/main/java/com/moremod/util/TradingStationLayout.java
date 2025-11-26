package com.moremod.util;

/** 以工業先鋒(Industrial Foregoing) 風格繪製的 256×256 GUI 佈局常量 */
public final class TradingStationLayout {

    public static final int GUI_W = 256;
    public static final int GUI_H = 256;

    /** 經典內容寬 176 像素，於 256 畫布水平置中 */
    public static final int BASE_X = (GUI_W - 176) / 2; // 40

    /** 與標準 166 高度的差距，下移玩家物品欄與熱欄 */
    public static final int Y_SHIFT = GUI_H - 166; // 90

    // ---------------- 槽位：完全參考 IF 的畫法分區 ----------------

    /** 玩家物品欄與熱欄（置中） */
    public static final int PLAYER_INV_X = BASE_X + 8;
    public static final int PLAYER_INV_Y = 84 + Y_SHIFT;   // 174
    public static final int HOTBAR_Y     = 142 + Y_SHIFT;  // 232

    /** 村民工具槽：右下 */
    public static final int CAPSULE_X = BASE_X + 146;
    public static final int CAPSULE_Y = 130;

    /** 實際交易輸入/輸出：中下區域（左為投入，右為產出） */
    public static final int INPUT_A_X = BASE_X + 26;
    public static final int INPUT_A_Y = 130;
    public static final int INPUT_B_X = BASE_X + 62;
    public static final int INPUT_B_Y = 130;
    public static final int OUTPUT_X  = BASE_X + 116;
    public static final int OUTPUT_Y  = 130;

    /** 上方交易預覽（可選：當前配方 A/B/OUT 的幽靈槽） */
    public static final int PREVIEW_A_X = BASE_X + 44;
    public static final int PREVIEW_A_Y = 48;
    public static final int PREVIEW_B_X = BASE_X + 80;
    public static final int PREVIEW_B_Y = 48;
    public static final int PREVIEW_O_X = BASE_X + 142;
    public static final int PREVIEW_O_Y = 48;

    /** 左側能量條（垂直） */
    public static final int ENERGY_X = BASE_X + 8;
    public static final int ENERGY_Y = 40;
    public static final int ENERGY_W = 12;
    public static final int ENERGY_H = 72;

    /** 右上緩衝/進度條（水平） */
    public static final int BUFFER_X = BASE_X + 176 - 8 - 72; // 寬 72
    public static final int BUFFER_Y = 20;
    public static final int BUFFER_W = 72;
    public static final int BUFFER_H = 12;

    /** 上方左右翻頁箭頭（切換交易） */
    public static final int ARROW_L_X = BASE_X + 8;
    public static final int ARROW_L_Y = 16;
    public static final int ARROW_R_X = BASE_X + 176 - 8 - 16;
    public static final int ARROW_R_Y = 16;
    public static final int ARROW_SIZE = 16;

    /** 可選展示/緩存 3x3（與 IF 無必然關係，保留擴展位） */
    public static final int GRID_X = BASE_X + 116;
    public static final int GRID_Y = 76;

    private TradingStationLayout() {}
}