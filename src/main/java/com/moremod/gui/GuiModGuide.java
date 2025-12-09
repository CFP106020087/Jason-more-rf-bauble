package com.moremod.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * MoreMod 綜合指南書 GUI (合併版)
 * 包含完整內容 + 自動換行功能
 */
@SideOnly(Side.CLIENT)
public class GuiModGuide extends GuiScreen {

    private static final ResourceLocation BOOK_TEXTURE = new ResourceLocation("textures/gui/book.png");

    private static final int BOOK_WIDTH = 256;
    private static final int BOOK_HEIGHT = 180;

    // 分類
    private static final String[] CATEGORIES = {
            "overview",      // 總覽
            "multiblock",    // 多方塊結構
            "energy",        // 能源系統
            "module",        // 模組系統
            "synergy",       // 協同系統
            "humanity"       // 人性系統
    };

    private static final String[] CATEGORY_NAMES = {
            "總覽",
            "多方塊結構",
            "能源系統",
            "模組系統",
            "協同系統",
            "人性系統"
    };

    private final EntityPlayer player;
    private int guiLeft;
    private int guiTop;

    private int currentCategory = 0;
    private int currentPage = 0;
    private List<GuidePageContent> currentPages = new ArrayList<>();

    // 按鈕ID
    private static final int BTN_NEXT = 1;
    private static final int BTN_PREV = 2;
    private static final int BTN_CAT_START = 10;

    public GuiModGuide(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - BOOK_WIDTH) / 2;
        this.guiTop = (this.height - BOOK_HEIGHT) / 2;
        loadCategory(currentCategory);
        initButtons();
    }

    private void loadCategory(int category) {
        this.currentCategory = category;
        this.currentPage = 0;
        this.currentPages.clear();

        switch (CATEGORIES[category]) {
            case "overview":
                loadOverviewPages();
                break;
            case "multiblock":
                loadMultiblockPages();
                break;
            case "energy":
                loadEnergyPages();
                break;
            case "module":
                loadModulePages();
                break;
            case "synergy":
                loadSynergyPages();
                break;
            case "humanity":
                loadHumanityPages();
                break;
        }

        // 重新初始化按鈕以確保狀態正確
        initButtons();
    }

    // ==================== 頁面內容載入 ====================

    private void loadOverviewPages() {
        // 第1頁：歡迎
        currentPages.add(new GuidePageContent(
                "歡迎使用 MoreMod",
                new String[]{
                        "本指南將幫助你了解",
                        "MoreMod 的所有系統。",
                        "",
                        "使用左側標籤切換分類",
                        "使用底部箭頭翻頁",
                        "",
                        "§e主要系統：§r",
                        "- 多方塊結構",
                        "- 能源系統",
                        "- 模組升級系統",
                        "- 協同效果系統",
                        "- 人性光譜系統"
                },
                new String[]{
                        "§6快速開始§r",
                        "",
                        "1. 獲取機械核心",
                        "   (擊殺Boss或合成)",
                        "",
                        "2. 建造能源設施",
                        "   (發電機、充能站)",
                        "",
                        "3. 安裝升級模組",
                        "   (使用升級艙)",
                        "",
                        "4. 探索協同效果",
                        "   (特定模組組合)"
                }
        ));

        // 第2頁：系統介紹
        currentPages.add(new GuidePageContent(
                "核心系統總覽",
                new String[]{
                        "§e機械核心§r",
                        "MoreMod 的核心物品",
                        "可安裝各種升級模組",
                        "提供強大的被動效果",
                        "",
                        "§e升級模組§r",
                        "插入機械核心的組件",
                        "有多種類型和等級",
                        "可在升級艙中升級"
                },
                new String[]{
                        "§e能源系統§r",
                        "許多功能需要 RF 能源",
                        "可使用發電機生產",
                        "充能站為裝備充電",
                        "",
                        "§e協同效果§r",
                        "特定模組組合觸發",
                        "提供額外的強大效果",
                        "詳見協同系統分類"
                }
        ));
    }

    private void loadMultiblockPages() {
        // 抽油機
        currentPages.add(new GuidePageContent(
                "抽油機 (3x3x4)",
                new String[]{
                        "§e第0層（地基）:§r",
                        "  I I I",
                        "  I C I",
                        "  I I I",
                        "",
                        "§e第1-2層（機體）:§r",
                        "  I . I",
                        "  . P .",
                        "  I . I",
                        "",
                        "§e第3層（頂部）:§r",
                        "  全部鐵塊"
                },
                new String[]{
                        "§6圖例：§r",
                        "C = 抽油機核心",
                        "I = 鐵/金/鑽石塊",
                        "P = 活塞（管道）",
                        ". = 空氣",
                        "",
                        "§a使用方法：§r",
                        "1. 用探測器找石油區塊",
                        "2. 建造多方塊結構",
                        "3. 對核心供電",
                        "4. 從核心提取石油桶"
                }
        ));

        // 升級艙
        currentPages.add(new GuidePageContent(
                "升級艙 (3x3x3)",
                new String[]{
                        "§e第0層（地板）:§r",
                        "  I I I",
                        "  I C I",
                        "  I I I",
                        "",
                        "§e第1層（中間）:§r",
                        "  I . I",
                        "  . . .",
                        "  I . I",
                        "",
                        "§e第2層（天花板）:§r",
                        "  全部框架方塊"
                },
                new String[]{
                        "§6圖例：§r",
                        "C = 升級艙核心",
                        "I = 框架方塊",
                        ". = 空氣（玩家空間）",
                        "",
                        "§6框架等級：§r",
                        "鐵塊 = 基礎",
                        "金塊 = 進階",
                        "鑽石塊 = 精英",
                        "綠寶石塊 = 終極"
                }
        ));

        currentPages.add(new GuidePageContent(
                "升級艙使用方法",
                new String[]{
                        "§a使用步驟：§r",
                        "",
                        "1. 將升級模組放入核心",
                        "",
                        "2. 對核心供電",
                        "   (需要充滿能量)",
                        "",
                        "3. 裝備機械核心",
                        "",
                        "4. 走進升級艙",
                        "",
                        "5. 等待升級完成"
                },
                new String[]{
                        "§c注意事項：§r",
                        "",
                        "- 能量不足會導致",
                        "  升級失敗",
                        "",
                        "- 更高等級的框架",
                        "  升級速度更快",
                        "",
                        "- 模組有等級上限",
                        "  死亡時會降級",
                        "",
                        "- 可使用升級艙修復",
                        "  已降級的模組"
                }
        ));

        // 智慧之泉
        currentPages.add(new GuidePageContent(
                "智慧之泉 (7x7x4)",
                new String[]{
                        "§e複雜的祭壇結構§r",
                        "",
                        "§6核心部分 (3x3):§r",
                        "底層：守護者石塊",
                        "中層：符文虛空石",
                        "頂層：遠古核心塊",
                        "      +四角海晶燈",
                        "",
                        "§6外圍 (7x7):§r",
                        "石英塊凹槽設計"
                },
                new String[]{
                        "§6材料清單：§r",
                        "- 智慧之泉核心 x1",
                        "- 守護者石塊 x8",
                        "- 符文虛空石塊 x8",
                        "- 遠古核心塊 x1",
                        "- 海晶燈 x4",
                        "- 石英塊 若干",
                        "- 水桶 x2",
                        "",
                        "§a功能：§r",
                        "經驗轉化、附魔增強"
                }
        ));

        // 簡易智慧祭壇
        currentPages.add(new GuidePageContent(
                "簡易智慧祭壇 (3x3x3)",
                new String[]{
                        "§e第0層（祭壇層）:§r",
                        "  E B E",
                        "  B C B",
                        "  E B E",
                        "",
                        "§e第1層（中間）:§r",
                        "  B . B",
                        "  . . .",
                        "  B . B",
                        "",
                        "§e第2層（頂部）:§r",
                        "  G B G",
                        "  B T B",
                        "  G B G"
                },
                new String[]{
                        "§6圖例：§r",
                        "C = 簡易智慧祭壇核心",
                        "E = 綠寶石塊",
                        "B = 書架",
                        "G = 金塊",
                        "T = 附魔台",
                        ". = 空氣",
                        "",
                        "§6範圍：§r 15格",
                        "",
                        "§a功能：§r",
                        "解鎖村民交易",
                        "加速村民幼體成長"
                }
        ));

        currentPages.add(new GuidePageContent(
                "簡易智慧祭壇使用",
                new String[]{
                        "§e自動效果§r",
                        "",
                        "結構完成後自動運作",
                        "無需任何能量供應",
                        "",
                        "§a效果1：解鎖交易§r",
                        "每5秒檢測範圍內村民",
                        "重置被鎖定的交易",
                        "讓村民可以再次交易",
                        "",
                        "§7對經常交易的村民",
                        "非常有用§r"
                },
                new String[]{
                        "§a效果2：加速成長§r",
                        "",
                        "範圍內的村民幼體",
                        "成長速度大幅加快",
                        "",
                        "§6建造提示：§r",
                        "- 將祭壇建在村莊中",
                        "- 確保村民在範圍內",
                        "- 右鍵核心查看狀態",
                        "",
                        "§c注意：§r",
                        "結構不完整時無效果"
                }
        ));
    }

    private void loadEnergyPages() {
        // 石油系統
        currentPages.add(new GuidePageContent(
                "石油探勘系統",
                new String[]{
                        "§e石油探測器§r",
                        "",
                        "使用方法：",
                        "右鍵掃描當前區塊",
                        "",
                        "探測結果：",
                        "- 區塊是否有石油",
                        "- 石油儲量 (mB)",
                        "- 預計可提取桶數",
                        "",
                        "§6注意：§r",
                        "約15%的區塊有石油"
                },
                new String[]{
                        "§e石油儲量分布§r",
                        "",
                        "小型礦脈：",
                        "  10,000 - 50,000 mB",
                        "",
                        "中型礦脈：",
                        "  50,000 - 200,000 mB",
                        "",
                        "大型礦脈：",
                        "  200,000 - 500,000 mB",
                        "",
                        "§7每桶 = 1000 mB§r"
                }
        ));

        // 發電機
        currentPages.add(new GuidePageContent(
                "石油發電機",
                new String[]{
                        "§e燃料類型§r",
                        "",
                        "§8原油桶：§r",
                        "  產出：100,000 RF",
                        "  燃燒：100 秒",
                        "",
                        "§a植物油桶：§r",
                        "  產出：60,000 RF",
                        "  燃燒：60 秒",
                        "  (可再生資源)"
                },
                new String[]{
                        "§e發電機規格§r",
                        "",
                        "容量：1,000,000 RF",
                        "輸出：200 RF/tick",
                        "",
                        "§a使用方法：§r",
                        "1. 放置發電機",
                        "2. 手持燃料右鍵",
                        "3. 自動向周圍輸出",
                        "",
                        "§7支持管道連接§r"
                }
        ));

        // 植物油壓榨機
        currentPages.add(new GuidePageContent(
                "植物油壓榨機",
                new String[]{
                        "§e功能§r",
                        "將農作物壓榨成植物油",
                        "可再生的發電燃料",
                        "",
                        "§e規格§r",
                        "容量：100,000 RF",
                        "消耗：50 RF/tick",
                        "處理：10 秒/次",
                        "",
                        "§e使用方法§r",
                        "手持農作物右鍵放入",
                        "空手右鍵取出產物"
                },
                new String[]{
                        "§e產油量表§r",
                        "",
                        "南瓜籽：100 mB",
                        "西瓜籽：100 mB",
                        "馬鈴薯：80 mB",
                        "甜菜根：70 mB",
                        "蘋果：60 mB",
                        "胡蘿蔔：60 mB",
                        "小麥：50 mB",
                        "甜菜種子：40 mB",
                        "西瓜片：40 mB",
                        "小麥種子：30 mB"
                }
        ));

        currentPages.add(new GuidePageContent(
                "植物油使用技巧",
                new String[]{
                        "§e換算公式§r",
                        "",
                        "1桶 = 1000 mB",
                        "",
                        "§6高效作物：§r",
                        "種子類最划算",
                        "10個南瓜籽=1桶油",
                        "",
                        "§6一般作物：§r",
                        "20個小麥=1桶油",
                        "13個馬鈴薯=1桶油"
                },
                new String[]{
                        "§e自動化建議§r",
                        "",
                        "1. 用漏斗自動輸入",
                        "2. 管道提取輸出",
                        "3. 接發電機循環",
                        "",
                        "§a植物油發電：§r",
                        "每桶產出 60,000 RF",
                        "燃燒時間 60 秒",
                        "",
                        "§7完全可再生能源§r"
                }
        ));

        // 充能站
        currentPages.add(new GuidePageContent(
                "充能站",
                new String[]{
                        "§e終極充電設施§r",
                        "",
                        "§6規格：§r",
                        "容量：100,000,000 RF",
                        "接收：10,000,000 RF/t",
                        "充電速度：無限快",
                        "",
                        "§a充電方式1：§r",
                        "將可充電物品放入",
                        "9格充電槽中"
                },
                new String[]{
                        "§a充電方式2：§r",
                        "站在充能站上方",
                        "自動為以下物品充電：",
                        "",
                        "- 主手/副手物品",
                        "- 全身盔甲",
                        "- 背包中所有物品",
                        "- 飾品欄物品",
                        "",
                        "§e特點：§r",
                        "極大容量，無限快充"
                }
        ));
    }

    private void loadModulePages() {
        currentPages.add(new GuidePageContent(
                "模組系統概述",
                new String[]{
                        "§e升級模組§r",
                        "",
                        "模組可安裝在機械核心",
                        "提供各種被動效果",
                        "",
                        "§6模組等級：§r",
                        "Lv.1 - 基礎效果",
                        "Lv.2 - 增強效果",
                        "Lv.3 - 強力效果",
                        "Lv.4 - 極致效果",
                        "Lv.5 - 傳說效果"
                },
                new String[]{
                        "§e升級方法§r",
                        "",
                        "使用升級艙：",
                        "1. 放入升級材料",
                        "2. 供應足夠能量",
                        "3. 進入升級艙",
                        "",
                        "§c死亡懲罰：§r",
                        "模組等級會下降",
                        "最多降低3-5級",
                        "可在升級艙修復"
                }
        ));

        currentPages.add(new GuidePageContent(
                "常用模組類型",
                new String[]{
                        "§e戰鬥類§r",
                        "- 傷害增強",
                        "- 攻擊速度",
                        "- 暴擊率/傷害",
                        "- 生命偷取",
                        "",
                        "§e防禦類§r",
                        "- 護甲強化",
                        "- 傷害減免",
                        "- 生命回復",
                        "- 能量護盾"
                },
                new String[]{
                        "§e實用類§r",
                        "- 移動速度",
                        "- 跳躍增強",
                        "- 飛行能力",
                        "- 夜視效果",
                        "",
                        "§e特殊類§r",
                        "- 磁力吸取",
                        "- 自動修復",
                        "- 經驗加成",
                        "- 幸運增強"
                }
        ));
    }

    private void loadSynergyPages() {
        currentPages.add(new GuidePageContent(
                "協同效果系統",
                new String[]{
                        "§e什麼是協同效果？§r",
                        "",
                        "當特定模組組合時",
                        "會觸發額外的效果",
                        "",
                        "§6觸發條件：§r",
                        "- 需要特定模組",
                        "- 模組需達到等級",
                        "- 某些需要特定事件",
                        "",
                        "詳細查看：協同手冊"
                },
                new String[]{
                        "§e協同分類§r",
                        "",
                        "§c戰鬥協同§r",
                        "增強攻擊能力",
                        "觸發：攻擊、擊殺",
                        "",
                        "§6能量協同§r",
                        "增強能量效率",
                        "觸發：持續被動",
                        "",
                        "§9機制協同§r",
                        "特殊機制效果",
                        "觸發：各種事件"
                }
        ));

        currentPages.add(new GuidePageContent(
                "如何發現協同",
                new String[]{
                        "§e發現方法§r",
                        "",
                        "1. §6協同手冊§r",
                        "   查看所有已知協同",
                        "",
                        "2. §6實驗組合§r",
                        "   嘗試不同模組搭配",
                        "",
                        "3. §6觸發提示§r",
                        "   協同觸發時會提示"
                },
                new String[]{
                        "§e使用技巧§r",
                        "",
                        "- 優先升級協同需要",
                        "  的關鍵模組",
                        "",
                        "- 某些協同非常強力",
                        "  值得專門配置",
                        "",
                        "- 協同效果可疊加",
                        "  多個協同同時生效"
                }
        ));
    }

    private void loadHumanityPages() {
        // 系統概述
        currentPages.add(new GuidePageContent(
                "人性光譜系統",
                new String[]{
                        "§e系統概念§r",
                        "",
                        "人性值不是道德判斷",
                        "而是你在世界法則中",
                        "的存在分類",
                        "",
                        "§6激活條件：§r",
                        "1. 裝備機械核心",
                        "2. 排異系統已突破",
                        "3. 排異值降為0"
                },
                new String[]{
                        "§e人性值範圍§r",
                        "",
                        "§a高人性 (>60%)§r",
                        "→ 獵人協議路線",
                        "",
                        "§7灰域 (40-60%)§r",
                        "→ 不穩定量子態",
                        "",
                        "§5低人性 (<40%)§r",
                        "→ 異常協議路線",
                        "",
                        "初始值：75%"
                }
        ));

        // 獵人協議
        currentPages.add(new GuidePageContent(
                "獵人協議 (高人性)",
                new String[]{
                        "§a人性值 > 60%§r",
                        "",
                        "§e核心機制：精準打擊§r",
                        "通過研究敵人獲得",
                        "針對性的傷害加成",
                        "",
                        "§6樣本系統：§r",
                        "- 擊殺敵人掉落樣本",
                        "- 人性越高掉率越高",
                        "- Boss必掉樣本"
                },
                new String[]{
                        "§e生物檔案§r",
                        "",
                        "收集樣本升級檔案：",
                        "- 基礎：+10%傷害",
                        "- 進階：+20%傷害+暴擊",
                        "- 精英：+30%傷害+高暴",
                        "- 傳說：+50%傷害+特效",
                        "",
                        "§a治愈光環 (80%+)：§r",
                        "每3秒治療周圍友方",
                        "村民、動物、隊友"
                }
        ));

        // 異常協議
        currentPages.add(new GuidePageContent(
                "異常協議 (低人性)",
                new String[]{
                        "§5人性值 < 40%§r",
                        "",
                        "§e核心機制：純粹暴力§r",
                        "不分敵我的力量增幅",
                        "",
                        "§6傷害加成：§r",
                        "25-40%：+20%全傷害",
                        "10-25%：+40%全傷害",
                        "<10%：+60%全傷害",
                        "",
                        "§c無法獲得樣本§r"
                },
                new String[]{
                        "§e異常場效果§r",
                        "",
                        "周圍敵人受到：",
                        "- 減速效果",
                        "- 凋零傷害(25%以下)",
                        "",
                        "§e畸變脈冲 (<10%)：§r",
                        "攻擊時機率觸發",
                        "對周圍造成範圍傷害",
                        "",
                        "§c代價：戰鬥消耗人性§r"
                }
        ));

        // 灰域
        currentPages.add(new GuidePageContent(
                "灰域 (不穩定態)",
                new String[]{
                        "§7人性值 40-60%§r",
                        "",
                        "§e量子疊加狀態§r",
                        "同時存在於兩種協議",
                        "但效果都減半",
                        "",
                        "§6樣本掉率：§r",
                        "正常掉率的50%",
                        "",
                        "§6異常場：§r",
                        "間歇性激活(10s/30s)"
                },
                new String[]{
                        "§e觀測坍縮§r",
                        "",
                        "§c受到致命傷害時：§r",
                        "15%機率觸發坍縮",
                        "",
                        "效果：",
                        "- 取消本次死亡",
                        "- 人性值隨機偏移±15%",
                        "- 可能進入高/低人性",
                        "",
                        "§7薛定諤的戰士§r"
                }
        ));

        // 崩解機制
        currentPages.add(new GuidePageContent(
                "存在崩解",
                new String[]{
                        "§c人性值歸零時觸發§r",
                        "",
                        "§e崩解狀態：§r",
                        "你的存在正在從",
                        "世界法則中脫落",
                        "",
                        "§6持續時間：§r",
                        "60秒倒計時",
                        "",
                        "§c週期性傷害：§r",
                        "每5秒受到最大生命",
                        "5%的真實傷害"
                },
                new String[]{
                        "§e存活條件§r",
                        "",
                        "在60秒內不死亡",
                        "",
                        "§a存活獎勵：§r",
                        "- 人性值回復到15%",
                        "- 獲得「存在錨定」",
                        "- 1個MC日無法崩解",
                        "",
                        "§c死亡懲罰：§r",
                        "人性重置為50%"
                }
        ));

        // 環境影響
        currentPages.add(new GuidePageContent(
                "環境與行為影響",
                new String[]{
                        "§e人性恢復：§r",
                        "",
                        "§a陽光沐浴：§r",
                        "主世界白天+露天",
                        "+0.1%/秒",
                        "",
                        "§a睡眠：§r",
                        "每次睡覺+5%",
                        "(上限75%)",
                        "",
                        "§a進食熟食：§r",
                        "普通+0.5%，金蘋果+3%"
                },
                new String[]{
                        "§e人性消耗：§r",
                        "",
                        "§c異常維度：§r",
                        "下界/末地 -0.2%/秒",
                        "",
                        "§c低人性戰鬥：§r",
                        "<50%時戰鬥-0.1%/秒",
                        "",
                        "§c殺害友善生物：§r",
                        "村民-10%，動物-2%",
                        "",
                        "§c熬夜：§r",
                        "長時間不睡-0.05%/秒"
                }
        ));

        // 人性恢復行為
        currentPages.add(new GuidePageContent(
                "保持人性的方法",
                new String[]{
                        "§a日常活動：§r",
                        "",
                        "與村民交易 +1%",
                        "收穫農作物 +0.2%",
                        "餵養動物 +0.5%",
                        "",
                        "§6飲食選擇：§r",
                        "熟食 +0.5%",
                        "複雜料理 +1%",
                        "金蘋果 +3%"
                },
                new String[]{
                        "§e策略建議§r",
                        "",
                        "§a高人性路線：§r",
                        "- 保持在主世界",
                        "- 規律作息睡覺",
                        "- 多與村民互動",
                        "",
                        "§5低人性路線：§r",
                        "- 長期滯留異維度",
                        "- 頻繁戰鬥",
                        "- 注意崩解風險"
                }
        ));
    }

    // ==================== 按鈕和繪製 ====================

    private void initButtons() {
        this.buttonList.clear();

        // 翻頁按鈕
        if (currentPage > 0) {
            this.buttonList.add(new GuiButton(BTN_PREV, guiLeft + 20, guiTop + BOOK_HEIGHT - 25, 20, 20, "<"));
        }
        if (currentPage < currentPages.size() - 1) {
            this.buttonList.add(new GuiButton(BTN_NEXT, guiLeft + BOOK_WIDTH - 40, guiTop + BOOK_HEIGHT - 25, 20, 20, ">"));
        }

        // 分類標籤
        int tabY = guiTop + 10;
        for (int i = 0; i < CATEGORIES.length; i++) {
            boolean selected = (i == currentCategory);
            int tabWidth = 50;
            // 修正標籤位置，避免蓋住書本邊緣
            int xPos = guiLeft - tabWidth + (selected ? 3 : -2);

            GuiButton btn = new GuiButton(BTN_CAT_START + i, xPos, tabY, tabWidth, 16, CATEGORY_NAMES[i]);
            btn.enabled = !selected;
            this.buttonList.add(btn);
            tabY += 20;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawBookBackground();

        if (!currentPages.isEmpty() && currentPage < currentPages.size()) {
            drawPageContent(currentPages.get(currentPage));
        }

        // 繪製頁碼
        String pageNum = (currentPage + 1) + " / " + Math.max(1, currentPages.size());
        drawCenteredString(fontRenderer, pageNum, guiLeft + BOOK_WIDTH / 2, guiTop + BOOK_HEIGHT - 15, 0x555555);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawBookBackground() {
        // 書本外框 (深褐色)
        drawRect(guiLeft - 5, guiTop - 5, guiLeft + BOOK_WIDTH + 5, guiTop + BOOK_HEIGHT + 5, 0xFF3D2B1F);
        // 書本內頁 (米色)
        drawRect(guiLeft, guiTop, guiLeft + BOOK_WIDTH, guiTop + BOOK_HEIGHT, 0xFFF5E6D3);
        // 中間裝訂線 (陰影)
        drawRect(guiLeft + BOOK_WIDTH / 2 - 1, guiTop + 5, guiLeft + BOOK_WIDTH / 2 + 1, guiTop + BOOK_HEIGHT - 5, 0xFFD4C4B0);

        // 簡單的裝飾線
        drawRect(guiLeft + 10, guiTop + 10, guiLeft + BOOK_WIDTH - 10, guiTop + 11, 0xFF8B7355);
        drawRect(guiLeft + 10, guiTop + BOOK_HEIGHT - 30, guiLeft + BOOK_WIDTH - 10, guiTop + BOOK_HEIGHT - 29, 0xFF8B7355);
    }

    /**
     * ⭐ 核心方法：使用 drawSplitString 實現自動換行
     */
    private void drawPageContent(GuidePageContent page) {
        int centerX = guiLeft + BOOK_WIDTH / 2;
        int startY = guiTop + 20;

        // 定義每一頁的文字寬度 (預留邊距)
        // 書總寬 256 -> 半邊 128 -> 扣除邊距後約 100
        int pageWidth = 100;
        int leftX = guiLeft + 18;
        int rightX = centerX + 12;
        int textColor = 0x000000;

        // 1. 繪製標題 (跨頁居中)
        GlStateManager.pushMatrix();
        // 稍微往上移一點
        GlStateManager.translate(centerX, guiTop + 12, 0);
        GlStateManager.scale(1.2f, 1.2f, 1f);
        drawCenteredString(fontRenderer, TextFormatting.DARK_BLUE + page.title, 0, 0, 0);
        GlStateManager.popMatrix();

        // 2. 繪製左頁內容
        int y = startY + 10;
        for (String line : page.leftContent) {
            // 檢查是否超出頁面底部，超出則不繪製
            if (y > guiTop + BOOK_HEIGHT - 35) break;

            // ⭐ 關鍵：drawSplitString 自動換行
            fontRenderer.drawSplitString(line, leftX, y, pageWidth, textColor);

            // ⭐ 關鍵：根據換行後的高度動態增加 Y 座標
            // 如果一行字換成了兩行，高度就會增加
            y += fontRenderer.getWordWrappedHeight(line, pageWidth);
        }

        // 3. 繪製右頁內容
        y = startY + 10;
        for (String line : page.rightContent) {
            if (y > guiTop + BOOK_HEIGHT - 35) break;

            fontRenderer.drawSplitString(line, rightX, y, pageWidth, textColor);
            y += fontRenderer.getWordWrappedHeight(line, pageWidth);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_NEXT) {
            if (currentPage < currentPages.size() - 1) {
                currentPage++;
                initButtons();
            }
        } else if (button.id == BTN_PREV) {
            if (currentPage > 0) {
                currentPage--;
                initButtons();
            }
        } else if (button.id >= BTN_CAT_START && button.id < BTN_CAT_START + CATEGORIES.length) {
            int newCategory = button.id - BTN_CAT_START;
            if (newCategory != currentCategory) {
                loadCategory(newCategory);
                // 不需要再次 initButtons，loadCategory 裡已經呼叫了
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /**
     * 頁面內容容器
     */
    private static class GuidePageContent {
        final String title;
        final String[] leftContent;
        final String[] rightContent;

        GuidePageContent(String title, String[] left, String[] right) {
            this.title = title;
            this.leftContent = left;
            this.rightContent = right;
        }
    }
}