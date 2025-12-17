package com.moremod.gui;

import com.moremod.ritual.RitualInfusionAPI;
import com.moremod.ritual.RitualInfusionRecipe;
import com.moremod.ritual.LegacyRitualConfig;
import com.moremod.init.ModBlocks;
import com.moremod.moremod;
import com.moremod.quarry.QuarryRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            "functional",    // 功能性方塊
            "module",        // 模組系統
            "synergy",       // 協同系統
            "humanity",      // 人性系統
            "ritual"         // 仪式系统
    };

    private static final String[] CATEGORY_NAMES = {
            "總覽",
            "多方塊結構",
            "能源系統",
            "功能方塊",
            "模組系統",
            "協同系統",
            "人性系統",
            "儀式系統"
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
            case "functional":
                loadFunctionalBlocksPages();
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
            case "ritual":
                loadRitualPages();
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
                            "开始时获得",
                        "",
                        "2. 建造能源設施",
                        "   (發電機、充能站)",
                        "",
                        "3. 安裝升級模組",
                        "   (使用升級艙/右键 视配置而定)",
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
                        "可在升級艙，或右键升級"
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
        // ==================== 抽油機 (3x3x4) ====================
        StructureTemplate oilPumpStruct = new StructureTemplate(3, 3)
                .addKey('I', new ItemStack(Blocks.IRON_BLOCK))
                .addKey('C', new ItemStack(ModBlocks.OIL_EXTRACTOR_CORE))  // 抽油機核心
                .addKey('P', new ItemStack(Blocks.PISTON));

        oilPumpStruct.addLayer("I.I", ".C.", "I.I");  // 第0層 - 四角框架 + 核心
        oilPumpStruct.addLayer("I.I", ".P.", "I.I");  // 第1層 - 四角框架 + 活塞
        oilPumpStruct.addLayer("I.I", ".P.", "I.I");  // 第2層 - 四角框架 + 活塞
        oilPumpStruct.addLayer("III", "III", "III");  // 第3層 - 封頂

        currentPages.add(new GuidePageContent(
                "抽油機結構",
                new String[]{
                        "§e自動化石油開採§r",
                        "",
                        "§6預覽說明：§r",
                        "右側為結構全息圖",
                        "它會§a自動輪播§r",
                        "顯示每一層結構。",
                        "",
                        "§6搭建材料：§r",
                        "- 抽油機核心 x1",
                        "- 鐵/金/鑽石塊 x13",
                        "- 活塞 x2"
                },
                new String[]{
                        "§a使用方法：§r",
                        "1. 探測器找石油區塊",
                        "2. 建造多方塊結構",
                        "3. 對核心供電",
                        "4. 從核心提取石油",
                        "",
                        "§7(滑鼠懸停查看方塊)§r"
                },
                oilPumpStruct
        ));

        // ==================== 升級艙 (3x3x4) ====================
        StructureTemplate upgradeChamberStruct = new StructureTemplate(3, 3)
                .addKey('I', new ItemStack(Blocks.IRON_BLOCK))
                .addKey('G', new ItemStack(Blocks.GLASS))
                .addKey('C', new ItemStack(ModBlocks.UPGRADE_CHAMBER_CORE));  // 升級艙核心

        upgradeChamberStruct.addLayer("I.I", ".C.", "I.I");  // 第0層 - 四角框架 + 核心
        upgradeChamberStruct.addLayer("G.G", "...", "G.G");  // 第1層 - 玻璃柱 + 空間
        upgradeChamberStruct.addLayer("G.G", "...", "G.G");  // 第2層 - 玻璃柱 + 空間
        upgradeChamberStruct.addLayer("III", "III", "III");  // 第3層 - 封頂

        currentPages.add(new GuidePageContent(
                "升級艙結構",
                new String[]{
                        "§e模組升級設施§r",
                        "",
                        "§6框架等級：§r",
                        "鐵塊 = 基礎",
                        "金塊 = 進階",
                        "鑽石塊 = 精英",
                        "綠寶石塊 = 終極",
                        "",
                        "§a使用步驟：§r",
                        "1. 放入升級模組",
                        "2. 對核心供電",
                        "3. 裝備機械核心",
                        "4. 走進升級艙"
                },
                new String[]{
                        "§c注意事項：§r",
                        "能量不足會失敗",
                        "更高等級更快",
                        "",
                        "§7(滑鼠懸停查看方塊)§r"
                },
                upgradeChamberStruct
        ));

        // ==================== 簡易智慧祭壇 (3x3x3) ====================
        StructureTemplate wisdomShrineStruct = new StructureTemplate(3, 3)
                .addKey('E', new ItemStack(Blocks.EMERALD_BLOCK))
                .addKey('B', new ItemStack(Blocks.BOOKSHELF))
                .addKey('G', new ItemStack(Blocks.GOLD_BLOCK))
                .addKey('T', new ItemStack(Blocks.ENCHANTING_TABLE))
                .addKey('C', new ItemStack(ModBlocks.SIMPLE_WISDOM_SHRINE));  // 智慧祭壇核心

        wisdomShrineStruct.addLayer("EBE", "BCB", "EBE");  // 第0層 - 綠寶石角 + 書架邊 + 核心
        wisdomShrineStruct.addLayer("B.B", "...", "B.B");  // 第1層 - 四角書架 + 空間
        wisdomShrineStruct.addLayer("GBG", "BTB", "GBG");  // 第2層 - 金塊角 + 書架邊 + 附魔台

        currentPages.add(new GuidePageContent(
                "簡易智慧祭壇",
                new String[]{
                        "§e村民增強設施§r",
                        "",
                        "§a效果1：§r解鎖交易",
                        "每5秒檢測範圍內",
                        "村民，重置被鎖定",
                        "的交易。",
                        "",
                        "§a效果2：§r加速成長",
                        "範圍內村民幼體",
                        "成長速度大幅加快",
                        "",
                        "§6範圍：§r 15格"
                },
                new String[]{
                        "§6建造提示：§r",
                        "建在村莊中心",
                        "無需能量供應",
                        "",
                        "§7(滑鼠懸停查看方塊)§r"
                },
                wisdomShrineStruct
        ));

        // ==================== 重生艙 (3x3x3) ====================
        StructureTemplate respawnStruct = new StructureTemplate(3, 3)
                .addKey('I', new ItemStack(Blocks.IRON_BLOCK))
                .addKey('L', new ItemStack(Blocks.SEA_LANTERN))  // 光源
                .addKey('C', new ItemStack(ModBlocks.RESPAWN_CHAMBER_CORE));  // 重生艙核心

        respawnStruct.addLayer("III", "ICI", "III");  // 第0層 - 框架 + 核心
        respawnStruct.addLayer("I.I", "...", "I.I");  // 第1層 - 四角框架 + 空間
        respawnStruct.addLayer("III", "ILI", "III");  // 第2層 - 框架 + 光源

        currentPages.add(new GuidePageContent(
                "重生艙結構",
                new String[]{
                        "§e重生點設施§r",
                        "",
                        "§a功能：§r",
                        "設置玩家重生點",
                        "死亡後在此復活",
                        "",
                        "§6使用方法：§r",
                        "1. 建造結構",
                        "2. 對核心供電",
                        "3. 進入重生艙",
                        "4. 右鍵核心綁定"
                },
                new String[]{
                        "§6材料：§r",
                        "鐵/金/鑽塊 + 核心",
                        "頂部中心：光源",
                        "",
                        "§7(滑鼠懸停查看方塊)§r"
                },
                respawnStruct
        ));

        // ==================== 量子礦機 (3x3x3) ====================
        StructureTemplate quarryStruct = new StructureTemplate(3, 3)
                .addKey('A', new ItemStack(QuarryRegistry.blockQuarryActuator))  // 量子代理
                .addKey('C', new ItemStack(QuarryRegistry.blockQuantumQuarry));  // 量子礦機核心

        quarryStruct.addLayer("...", ".A.", "...");   // 底層（下方代理）
        quarryStruct.addLayer(".A.", "ACA", ".A.");   // 中間層 - 核心 + 四方代理
        quarryStruct.addLayer("...", ".A.", "...");   // 頂層（上方代理）

        currentPages.add(new GuidePageContent(
                "量子礦機結構",
                new String[]{
                        "§e自動採礦設施§r",
                        "",
                        "§6結構說明：§r",
                        "核心被6個代理",
                        "方塊包圍（上下",
                        "左右前後各1個）",
                        "",
                        "§c重要：§r",
                        "代理必須朝向核心！",
                        "",
                        "§6能量：§r",
                        "640,000 RF/次"
                },
                new String[]{
                        "§a功能：§r",
                        "消耗RF隨機產礦",
                        "",
                        "§7(滑鼠懸停查看方塊)§r"
                },
                quarryStruct
        ));

        // ==================== 儀式祭壇 (5x5x2) ====================
        StructureTemplate ritualStruct = new StructureTemplate(5, 5)
                .addKey('P', new ItemStack(moremod.RITUAL_PEDESTAL_BLOCK))  // 儀式基座
                .addKey('C', new ItemStack(moremod.RITUAL_CORE_BLOCK));     // 儀式核心

        ritualStruct.addLayer(".P.P.", "P...P", "..C..", "P...P", ".P.P.");  // 第0層 - 核心 + 8基座
        ritualStruct.addLayer(".P.P.", "P...P", ".....", "P...P", ".P.P.");  // 第1層 - 8基座

        currentPages.add(new GuidePageContent(
                "儀式祭壇結構",
                new String[]{
                        "§e儀式進行設施§r",
                        "",
                        "§6結構說明：§r",
                        "核心周圍放置",
                        "8個基座方塊",
                        "呈對角線排列",
                        "",
                        "§6材料：§r",
                        "- 儀式核心 x1",
                        "- 基座 x8",
                        "",
                        "§a功能：§r",
                        "詳見儀式系統分類"
                },
                new String[]{
                        "§7(滑鼠懸停查看方塊)§r"
                },
                ritualStruct
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

    private void loadFunctionalBlocksPages() {
        // 第1頁：功能方塊概述
        currentPages.add(new GuidePageContent(
                "功能方塊總覽",
                new String[]{
                        "§e自動化方塊§r",
                        "",
                        "§6渔网§r",
                        "自動捕魚",
                        "",
                        "§6堆肥桶§r",
                        "將植物轉化為骨粉",
                        "",
                        "§6動物餵食器§r",
                        "自動餵食周圍動物",
                        "",
                        "§6假玩家激活器§r",
                        "模擬玩家右鍵操作"
                },
                new String[]{
                        "§e實用方塊§r",
                        "",
                        "§6物品傳輸器§r",
                        "遠距離傳輸物品",
                        "",
                        "§6交易站§r",
                        "與村民自動交易",
                        "",
                        "§6時間控制器§r",
                        "加速/減速周圍方塊",
                        "",
                        "§6保護力場§r",
                        "保護區域不被破壞"
                }
        ));

        // 第2頁：渔网
        currentPages.add(new GuidePageContent(
                "渔网",
                new String[]{
                        "§e自動捕魚方塊§r",
                        "",
                        "放置在水面上",
                        "每隔一段時間",
                        "自動產生魚類",
                        "",
                        "§6產出物品：§r",
                        "- 生鱈魚",
                        "- 生鮭魚",
                        "- 河豚",
                        "- 熱帶魚",
                        "- 附魔書 (稀有)"
                },
                new String[]{
                        "§a使用方法：§r",
                        "",
                        "1. 放置在水面上",
                        "2. 需要接觸水方塊",
                        "3. 自動工作",
                        "4. 漏斗可提取產物",
                        "",
                        "§6注意：§r",
                        "需要足夠的水",
                        "約10秒產出一次"
                }
        ));

        // 第3頁：堆肥桶
        currentPages.add(new GuidePageContent(
                "堆肥桶",
                new String[]{
                        "§e植物轉骨粉§r",
                        "",
                        "將植物材料轉化",
                        "為骨粉",
                        "",
                        "§6可堆肥材料：§r",
                        "- 樹葉",
                        "- 草/蕨類",
                        "- 花朵",
                        "- 種子",
                        "- 農作物",
                        "- 腐肉"
                },
                new String[]{
                        "§a使用方法：§r",
                        "",
                        "1. 對堆肥桶右鍵",
                        "   放入可堆肥材料",
                        "2. 等待發酵",
                        "3. 空手右鍵取出",
                        "",
                        "§6轉化率：§r",
                        "約8個材料=1個骨粉",
                        "",
                        "§7自動化的骨粉來源"
                }
        ));

        // 第4頁：動物餵食器
        currentPages.add(new GuidePageContent(
                "動物餵食器",
                new String[]{
                        "§e自動繁殖動物§r",
                        "",
                        "定期餵食範圍內",
                        "的可繁殖動物",
                        "",
                        "§6工作範圍：§r",
                        "以方塊為中心",
                        "8格半徑範圍",
                        "",
                        "§6支持動物：§r",
                        "牛、羊、豬、雞"
                },
                new String[]{
                        "§a使用方法：§r",
                        "",
                        "1. 放入對應食物",
                        "   小麥/種子/胡蘿蔔",
                        "2. 確保動物在範圍內",
                        "3. 自動餵食繁殖",
                        "",
                        "§6注意：§r",
                        "需要RF能量供應",
                        "約5秒餵食一次",
                        "",
                        "§7畜牧自動化必備"
                }
        ));

        // 第5頁：假玩家激活器
        currentPages.add(new GuidePageContent(
                "假玩家激活器",
                new String[]{
                        "§e模擬玩家操作§r",
                        "",
                        "安裝靈魂核心後",
                        "可模擬玩家行為",
                        "",
                        "§6功能：§r",
                        "- 自動右鍵使用",
                        "- 觸發機關/按鈕",
                        "- 與方塊互動",
                        "",
                        "§c需要靈魂核心！§r"
                },
                new String[]{
                        "§a獲取靈魂核心：§r",
                        "",
                        "使用儀式系統",
                        "將頭顱綁定靈魂",
                        "",
                        "§6使用方法：§r",
                        "1. 放置激活器",
                        "2. 安裝靈魂核心",
                        "3. 對準目標方塊",
                        "4. 提供紅石信號",
                        "",
                        "§7強力自動化工具"
                }
        ));

        // 第6頁：時間控制器
        currentPages.add(new GuidePageContent(
                "時間控制器",
                new String[]{
                        "§e加速周圍方塊§r",
                        "",
                        "消耗RF能量",
                        "加速周圍方塊的tick",
                        "",
                        "§6影響範圍：§r",
                        "3x3x3 區域",
                        "",
                        "§6可加速：§r",
                        "- 熔爐",
                        "- 農作物",
                        "- 機器",
                        "- 大部分TileEntity"
                },
                new String[]{
                        "§a使用方法：§r",
                        "",
                        "1. 放置時間控制器",
                        "2. 提供RF能量",
                        "3. 提供紅石信號",
                        "",
                        "§6能量消耗：§r",
                        "約100 RF/tick",
                        "加速倍率：5x",
                        "",
                        "§7配合農場效果絕佳"
                }
        ));

        // 第7頁：時域加速器
        currentPages.add(new GuidePageContent(
                "時域加速器",
                new String[]{
                        "§e更強的時間控制§r",
                        "",
                        "時間控制器的",
                        "進階版本",
                        "",
                        "§6優勢：§r",
                        "- 更大的範圍",
                        "- 更高的加速倍率",
                        "- 更低的能耗比",
                        "",
                        "§6影響範圍：§r",
                        "5x5x5 區域"
                },
                new String[]{
                        "§6能量需求：§r",
                        "200 RF/tick",
                        "加速倍率：10x",
                        "",
                        "§a適用場景：§r",
                        "- 大型農場",
                        "- 機器陣列",
                        "- 生物生長",
                        "",
                        "§c注意：§r",
                        "高能耗，確保",
                        "電力供應充足"
                }
        ));

        // 第8頁：保護力場生成器
        currentPages.add(new GuidePageContent(
                "保護力場生成器",
                new String[]{
                        "§e區域保護§r",
                        "",
                        "創建不可破壞的",
                        "力場保護區域",
                        "",
                        "§6保護範圍：§r",
                        "以方塊為中心",
                        "可配置半徑",
                        "",
                        "§6效果：§r",
                        "- 阻止方塊破壞",
                        "- 阻止爆炸破壞",
                        "- 阻止液體流入"
                },
                new String[]{
                        "§6能量需求：§r",
                        "容量：5,000,000 RF",
                        "輸入：200,000 RF/t",
                        "消耗：5,000 RF/tick",
                        "",
                        "§a使用方法：§r",
                        "1. 放置生成器",
                        "2. 充滿能量",
                        "3. 提供紅石信號",
                        "",
                        "§7保護你的基地！"
                }
        ));

        // 第9頁：量子採礦機
        currentPages.add(new GuidePageContent(
                "量子採礦機",
                new String[]{
                        "§e自動化採礦§r",
                        "",
                        "消耗RF能量",
                        "自動獲取礦物",
                        "",
                        "§6產出物品：§r",
                        "- 煤炭",
                        "- 鐵礦/金礦",
                        "- 鑽石/綠寶石",
                        "- 紅石/青金石",
                        "- 模組礦物"
                },
                new String[]{
                        "§6能量需求：§r",
                        "640,000 RF/次",
                        "",
                        "§a優化建議：§r",
                        "配合2座發電廠",
                        "確保持續供電",
                        "",
                        "§6特點：§r",
                        "無需實際挖掘",
                        "概率獲取礦物",
                        "支持升級插件"
                }
        ));

        // 第10頁：物品傳輸器
        currentPages.add(new GuidePageContent(
                "物品傳輸器",
                new String[]{
                        "§e遠距離傳輸§r",
                        "",
                        "將物品瞬間傳送",
                        "到連結的位置",
                        "",
                        "§6工作原理：§r",
                        "1. 放置兩個傳輸器",
                        "2. 使用連結工具連結",
                        "3. 物品自動傳輸",
                        "",
                        "§6傳輸距離：§r",
                        "同維度內無限"
                },
                new String[]{
                        "§a使用方法：§r",
                        "",
                        "1. 用傳輸連結器",
                        "   右鍵第一個傳輸器",
                        "2. 再右鍵第二個",
                        "3. 完成連結",
                        "",
                        "§6能量需求：§r",
                        "根據距離變化",
                        "",
                        "§7跨基地物流必備"
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

    private void loadRitualPages() {
        // 第1頁：儀式系統概述
        currentPages.add(new GuidePageContent(
                "儀式系統概述",
                new String[]{
                        "§e什麼是儀式系統？§r",
                        "",
                        "儀式系統允許你通過",
                        "特殊的祭壇結構來",
                        "製作強大的物品",
                        "",
                        "§6核心組件：§r",
                        "- 儀式核心方塊",
                        "- 基座方塊 (8個)",
                        "- RF能量供應"
                },
                new String[]{
                        "§a使用方法：§r",
                        "",
                        "1. 放置儀式核心",
                        "2. 周圍放8個基座",
                        "3. 在基座上放材料",
                        "4. 在核心上放中心物品",
                        "5. 對核心供電",
                        "6. 右鍵核心開始儀式",
                        "",
                        "§c注意：§r 有失敗機率！"
                }
        ));

        // 第2頁：祭壇結構圖
        currentPages.add(new GuidePageContent(
                "祭壇結構 (3x3)",
                new String[]{
                        "§e俯視圖 (Y+1層):§r",
                        "",
                        "  P . P",
                        "  . C .",
                        "  P . P",
                        "",
                        "§e俯視圖 (Y+0層):§r",
                        "",
                        "  P . P",
                        "  . . .",
                        "  P . P"
                },
                new String[]{
                        "§6圖例：§r",
                        "C = 儀式核心",
                        "P = 基座 (Pedestal)",
                        ". = 空氣",
                        "",
                        "§6總計材料：§r",
                        "- 儀式核心 x1",
                        "- 基座 x8",
                        "",
                        "§7基座圍繞核心",
                        "呈對角線排列"
                }
        ));

        // 第3頁：祭壇階層系統
        currentPages.add(new GuidePageContent(
                "祭壇階層系統",
                new String[]{
                        "§e祭壇分為三個階層§r",
                        "",
                        "§6一階祭壇：§r",
                        "基礎儀式核心",
                        "可執行基礎注魔配方",
                        "",
                        "§6二階祭壇：§r",
                        "進階儀式核心",
                        "可執行：",
                        "- 詛咒附魔淨化",
                        "- 武器經驗加速"
                },
                new String[]{
                        "§6三階祭壇：§r",
                        "終極儀式核心",
                        "可執行：",
                        "- 附魔轉移",
                        "- 詛咒創造",
                        "- 織印強化",
                        "- 七咒嵌入",
                        "",
                        "§c注意：§r",
                        "高階儀式需要更多能量",
                        "失敗率也會降低"
                }
        ));

        // 第4頁：詛咒淨化儀式
        currentPages.add(new GuidePageContent(
                "詛咒淨化儀式 (二階)",
                new String[]{
                        "§e移除物品上的詛咒§r",
                        "",
                        "§6材料需求：§r",
                        "中心：有詛咒的物品",
                        "基座：聖水/金蘋果/",
                        "      龍息/地獄之星",
                        "",
                        "§a效果：§r",
                        "二階：移除1個詛咒",
                        "三階：移除全部詛咒"
                },
                new String[]{
                        "§6可淨化詛咒：§r",
                        "- 綁定詛咒",
                        "- 消失詛咒",
                        "",
                        "§c注意事項：§r",
                        "需要足夠RF能量",
                        "每個詛咒消耗10000RF",
                        "",
                        "§7詛咒不再困擾你！"
                }
        ));

        // 第5頁：附魔轉移儀式
        currentPages.add(new GuidePageContent(
                "附魔轉移儀式 (三階)",
                new String[]{
                        "§e轉移附魔到其他物品§r",
                        "",
                        "§6材料需求：§r",
                        "中心：附魔來源物品",
                        "基座1：青金石/龍息",
                        "基座2：目標物品",
                        "",
                        "§a效果：§r",
                        "所有附魔從來源轉移",
                        "到目標物品上"
                },
                new String[]{
                        "§6轉移規則：§r",
                        "- 附魔必須適用目標",
                        "- 同附魔取最高等級",
                        "- 來源物品失去附魔",
                        "",
                        "§6支持轉移到：§r",
                        "- 書本 → 附魔書",
                        "- 工具/武器/盔甲",
                        "",
                        "§7拯救珍貴附魔！"
                }
        ));

        // 第6頁：七咒聯動系統
        currentPages.add(new GuidePageContent(
                "七咒聯動系統",
                new String[]{
                        "§5七咒之戒§r",
                        "",
                        "來自 Enigmatic Legacy",
                        "強力但有7種詛咒限制",
                        "",
                        "§6聯動飾品：§r",
                        "- §c饕餮指骨§r：飽食度回復",
                        "- §d怨念結晶§r：受傷反彈",
                        "- §5虛空之眼§r：死亡免疫",
                        "- §4荊棘碎片§r：反傷加成"
                },
                new String[]{
                        "§6淨化系統：§r",
                        "使用七聖遺物在三階",
                        "祭壇中進行嵌入儀式",
                        "可逐一淨化詛咒",
                        "",
                        "§a七聖遺物：§r",
                        "聖光之心、和平徽章",
                        "治愈寶珠、希望之石...",
                        "",
                        "§6淨化全部後解鎖§r",
                        "§d真正的力量！§r"
                }
        ));

        // 第7頁：靈魂核心系統
        currentPages.add(new GuidePageContent(
                "靈魂核心系統",
                new String[]{
                        "§e假玩家核心§r",
                        "",
                        "通過儀式創造靈魂核心",
                        "可安裝到假玩家激活器",
                        "",
                        "§6獲取方式：§r",
                        "在祭壇中使用：",
                        "- 末影珍珠 (中心)",
                        "- 靈魂沙 x4",
                        "- 烈焰粉 x4"
                },
                new String[]{
                        "§a靈魂核心功能：§r",
                        "",
                        "安裝到假玩家後",
                        "可模擬玩家行為：",
                        "- 自動右鍵使用",
                        "- 觸發機關/按鈕",
                        "- 與某些方塊互動",
                        "",
                        "§7自動化的強力助手！"
                }
        ));

        // 第8頁：織印強化儀式
        currentPages.add(new GuidePageContent(
                "織印強化儀式",
                new String[]{
                        "§e強化織印盔甲§r",
                        "",
                        "已織入布料的盔甲",
                        "可在祭壇中進一步強化",
                        "",
                        "§6強化材料 (任一)：§r",
                        "龍息/終界之眼/",
                        "地獄之星/海晶碎片/",
                        "烈焰粉"
                },
                new String[]{
                        "§a階層加成：§r",
                        "",
                        "§6一階：§r +25%能量",
                        "       +15%能力",
                        "",
                        "§6二階：§r +50%能量",
                        "       +30%能力",
                        "",
                        "§6三階：§r +100%能量",
                        "       +50%能力"
                }
        ));

        // 第9頁：詛咒之鏡複製儀式
        currentPages.add(new GuidePageContent(
                "詛咒之鏡複製儀式",
                new String[]{
                        "§5詛咒之鏡§r",
                        "",
                        "映照虛空的神秘之鏡",
                        "可複製任意物品！",
                        "",
                        "§6使用方法：§r",
                        "1. 副手持要複製的物品",
                        "2. 主手持鏡子右鍵",
                        "3. 物品被鏡面吸收",
                        "4. 再右鍵可取出物品"
                },
                new String[]{
                        "§6複製儀式：§r",
                        "",
                        "中心：存有物品的詛咒之鏡",
                        "基座：虛空精華 x8",
                        "時間：15秒",
                        "",
                        "§c成功率僅 1%！§r",
                        "§4失敗會毀掉原物品！§r",
                        "",
                        "§7鏡子可使用10次"
                }
        ));

        // 第10頁：詛咒創造儀式
        currentPages.add(new GuidePageContent(
                "詛咒創造儀式",
                new String[]{
                        "§5創建詛咒附魔書§r",
                        "",
                        "§6材料需求：§r",
                        "中心：書本",
                        "基座：",
                        "  墨囊 x1",
                        "  腐肉/蜘蛛眼/",
                        "  發酵蜘蛛眼 x1~2",
                        "",
                        "§a產出：§r",
                        "詛咒附魔書",
                        "(綁定/消失詛咒)"
                },
                new String[]{
                        "§6特殊說明：§r",
                        "",
                        "這些詛咒是「虛假」的",
                        "看起來像真的詛咒",
                        "但實際上沒有效果！",
                        "",
                        "§a用途：§r",
                        "- 裝飾/惡作劇",
                        "- 某些特殊配方",
                        "",
                        "§7詛咒不再可怕了"
                }
        ));

        // 第11頁：注魔儀式
        currentPages.add(new GuidePageContent(
                "注魔儀式 (三階)",
                new String[]{
                        "§e將附魔書注入物品§r",
                        "",
                        "§6材料需求：§r",
                        "中心：目標物品",
                        "基座：附魔書 x8",
                        "",
                        "§c需要三階祭壇！§r",
                        "",
                        "§6成功率：§r 10%"
                },
                new String[]{
                        "§a效果：§r",
                        "",
                        "基座上的附魔書",
                        "會注入到中心物品",
                        "",
                        "§6優勢：§r",
                        "- 突破附魔台限制",
                        "- 多附魔一次完成",
                        "",
                        "§c失敗風險：§r",
                        "附魔書會被消耗！"
                }
        ));

        // 第12頁：不可破壞儀式（读取CRT配置）
        {
            String[] defaultMats = {"  地獄之星 x2", "  黑曜石 x2", "  鑽石 x4"};
            String[] materials = getRitualMaterials(LegacyRitualConfig.UNBREAKABLE, defaultMats);
            String params = getRitualParamsText(LegacyRitualConfig.UNBREAKABLE);
            int tier = LegacyRitualConfig.getRequiredTier(LegacyRitualConfig.UNBREAKABLE);

            List<String> leftLines = new ArrayList<>();
            leftLines.add("§e使物品永不損壞§r");
            leftLines.add("");
            leftLines.add("§6材料需求：§r");
            leftLines.add("中心：任意有耐久物品");
            leftLines.add("基座：");
            for (String mat : materials) {
                leftLines.add(mat);
            }
            leftLines.add("");
            leftLines.add("§c需要" + tier + "階祭壇！§r");

            currentPages.add(new GuidePageContent(
                    "不可破壞儀式 (" + tier + "階)",
                    leftLines.toArray(new String[0]),
                    new String[]{
                            "§a效果：§r",
                            "",
                            "為物品添加",
                            "Unbreakable 標籤",
                            "物品將永不損壞",
                            "",
                            "§6參數(CRT可改)：§r",
                            params,
                            "",
                            "§6特點：§r",
                            "- §a保留所有NBT數據§r",
                            "- 保留所有附魔",
                            "",
                            "§c失敗懲罰：§r",
                            "物品損失50%耐久"
                    }
            ));
        }

        // 第13頁：靈魂束縛儀式（读取CRT配置）
        {
            String[] defaultMats = {"  末影珍珠 x4", "  惡魂之淚 x2", "  金塊 x2"};
            String[] materials = getRitualMaterials(LegacyRitualConfig.SOULBOUND, defaultMats);
            String params = getRitualParamsText(LegacyRitualConfig.SOULBOUND);
            int tier = LegacyRitualConfig.getRequiredTier(LegacyRitualConfig.SOULBOUND);

            List<String> leftLines = new ArrayList<>();
            leftLines.add("§e使物品死亡不掉落§r");
            leftLines.add("");
            leftLines.add("§6材料需求：§r");
            leftLines.add("中心：任意物品");
            leftLines.add("基座：");
            for (String mat : materials) {
                leftLines.add(mat);
            }
            leftLines.add("");
            leftLines.add("§c需要" + tier + "階祭壇！§r");

            currentPages.add(new GuidePageContent(
                    "靈魂束縛儀式 (" + tier + "階)",
                    leftLines.toArray(new String[0]),
                    new String[]{
                            "§a效果：§r",
                            "",
                            "為物品添加",
                            "Soulbound 標籤",
                            "死亡時物品不會掉落",
                            "",
                            "§6參數(CRT可改)：§r",
                            params,
                            "",
                            "§6特點：§r",
                            "- §a保留所有NBT數據§r",
                            "- 保留所有附魔",
                            "",
                            "§c失敗懲罰：§r",
                            "物品被虛空吞噬！"
                    }
            ));
        }

        // 第14頁：武器經驗加速儀式
        currentPages.add(new GuidePageContent(
                "武器經驗加速 (二階)",
                new String[]{
                        "§e加速武器升級§r",
                        "",
                        "§6適用武器：§r",
                        "- 澄月 (Clarity)",
                        "- 勇者之劍",
                        "- 鉅刃劍",
                        "",
                        "§6材料需求：§r",
                        "中心：上述武器",
                        "基座：經驗瓶/附魔書",
                        "      或綠寶石 (≥1)"
                },
                new String[]{
                        "§a效果：§r",
                        "",
                        "為武器添加經驗",
                        "加速Buff",
                        "",
                        "§6持續時間：§r",
                        "10分鐘",
                        "",
                        "§7加速武器的經驗",
                        "獲取速度"
                }
        ));

        // 第14頁：村正攻擊提升儀式
        currentPages.add(new GuidePageContent(
                "村正攻擊提升 (二階)",
                new String[]{
                        "§c臨時提升村正攻擊力§r",
                        "",
                        "§6材料需求：§r",
                        "中心：村正",
                        "基座：岩漿膏/烈焰粉",
                        "      或地獄之星 (≥1)",
                        "",
                        "§c需要二階祭壇！§r"
                },
                new String[]{
                        "§a效果：§r",
                        "",
                        "村正獲得臨時的",
                        "攻擊力提升效果",
                        "",
                        "§6持續時間：§r",
                        "10分鐘",
                        "",
                        "§7村正的力量",
                        "因鮮血而覺醒"
                }
        ));

        // 第15頁：特殊儀式總覽
        currentPages.add(new GuidePageContent(
                "特殊儀式總覽",
                new String[]{
                        "§e二階祭壇儀式：§r",
                        "",
                        "§6詛咒淨化§r",
                        "移除物品詛咒附魔",
                        "",
                        "§6詛咒創造§r",
                        "創建詛咒卷軸",
                        "",
                        "§6武器經驗加速§r",
                        "加速武器經驗獲取",
                        "",
                        "§6村正攻擊提升§r",
                        "臨時提升村正攻擊力",
                        "",
                        "§6織印強化§r",
                        "強化織印盔甲效果"
                },
                new String[]{
                        "§e三階祭壇儀式：§r",
                        "",
                        "§6注魔儀式§r",
                        "附魔書注入物品",
                        "",
                        "§6附魔轉移§r",
                        "轉移附魔到其他物品",
                        "",
                        "§6靈魂綁定§r",
                        "頭顱創建假玩家核心",
                        "",
                        "§6禁忌複製§r",
                        "詛咒之鏡複製物品",
                        "",
                        "§6七聖遺物嵌入§r",
                        "遺物嵌入七咒玩家",
                        "",
                        "§6不可破壞§r",
                        "使物品永不損壞",
                        "",
                        "§6§l靈魂束縛§r",
                        "使物品死亡不掉落"
                }
        ));

        // 動態加載儀式配方
        List<RitualInfusionRecipe> recipes = RitualInfusionAPI.RITUAL_RECIPES;
        if (recipes.isEmpty()) {
            currentPages.add(new GuidePageContent(
                    "可用儀式配方",
                    new String[]{
                            "§c暫無配方§r",
                            "",
                            "目前沒有註冊的",
                            "儀式配方。",
                            "",
                            "§7配方可能在遊戲",
                            "完全加載後出現"
                    },
                    new String[]{
                            "§6提示：§r",
                            "",
                            "儀式配方可以通過",
                            "JSON文件或",
                            "CraftTweaker添加",
                            "",
                            "詳見模組配置"
                    }
            ));
        } else {
            // 每頁顯示一個配方
            int recipeNum = 0;
            for (RitualInfusionRecipe recipe : recipes) {
                recipeNum++;
                String[] leftContent = buildRecipeLeftContent(recipe, recipeNum);
                String[] rightContent = buildRecipeRightContent(recipe);

                String outputName = getItemDisplayName(recipe.getOutput());
                currentPages.add(new GuidePageContent(
                        "儀式 #" + recipeNum + ": " + outputName,
                        leftContent,
                        rightContent
                ));
            }
        }
    }

    /**
     * 構建配方左側內容（輸入材料）
     */
    private String[] buildRecipeLeftContent(RitualInfusionRecipe recipe, int num) {
        List<String> lines = new ArrayList<>();

        // 中心物品
        lines.add("§e中心物品：§r");
        ItemStack[] coreStacks = recipe.getCore().getMatchingStacks();
        if (coreStacks.length > 0) {
            lines.add("  " + getItemDisplayName(coreStacks[0]));
        }
        lines.add("");

        // 基座物品
        lines.add("§e基座物品：§r");
        Map<String, Integer> pedestalCounts = new HashMap<>();
        for (Ingredient ing : recipe.getPedestalItems()) {
            ItemStack[] stacks = ing.getMatchingStacks();
            if (stacks.length > 0) {
                String name = getItemDisplayName(stacks[0]);
                pedestalCounts.merge(name, 1, Integer::sum);
            }
        }

        for (Map.Entry<String, Integer> entry : pedestalCounts.entrySet()) {
            lines.add("  " + entry.getKey() + " x" + entry.getValue());
        }

        return lines.toArray(new String[0]);
    }

    /**
     * 構建配方右側內容（輸出和統計）
     */
    private String[] buildRecipeRightContent(RitualInfusionRecipe recipe) {
        List<String> lines = new ArrayList<>();

        // 輸出
        lines.add("§a產出：§r");
        ItemStack output = recipe.getOutput();
        String outputName = getItemDisplayName(output);
        int count = output.getCount();
        lines.add("  " + outputName + (count > 1 ? " x" + count : ""));
        lines.add("");

        // 統計資訊
        lines.add("§6儀式資訊：§r");

        int timeTicks = recipe.getTime();
        float timeSeconds = timeTicks / 20.0f;
        lines.add("  時間: " + String.format("%.1f", timeSeconds) + " 秒");

        int energy = recipe.getEnergyPerPedestal();
        int totalEnergy = energy * recipe.getPedestalCount();
        lines.add("  能量: " + formatEnergy(totalEnergy) + " RF");

        float failChance = recipe.getFailChance();
        if (failChance > 0) {
            lines.add("  §c失敗率: " + String.format("%.0f", failChance * 100) + "%§r");
        } else {
            lines.add("  §a失敗率: 0%§r");
        }

        int tier = recipe.getRequiredTier();
        if (tier > 1) {
            lines.add("  §d需要階層: " + tier + "§r");
        }

        return lines.toArray(new String[0]);
    }

    /**
     * 獲取物品的顯示名稱
     */
    private String getItemDisplayName(ItemStack stack) {
        if (stack.isEmpty()) {
            return "§7(空)§r";
        }
        return stack.getDisplayName();
    }

    /**
     * 格式化能量數值
     */
    private String formatEnergy(int energy) {
        if (energy >= 1000000) {
            return String.format("%.2fM", energy / 1000000.0);
        } else if (energy >= 1000) {
            return String.format("%.1fK", energy / 1000.0);
        }
        return String.valueOf(energy);
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
            drawPageContent(currentPages.get(currentPage), mouseX, mouseY);
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
     * ⭐ 核心方法：繪製頁面內容（支援結構渲染）
     */
    private void drawPageContent(GuidePageContent page, int mouseX, int mouseY) {
        int centerX = guiLeft + BOOK_WIDTH / 2;
        int startY = guiTop + 20;
        int pageWidth = 100;
        int leftX = guiLeft + 18;
        int rightX = centerX + 12;

        // 1. 繪製標題
        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, guiTop + 12, 0);
        GlStateManager.scale(1.2f, 1.2f, 1f);
        drawCenteredString(fontRenderer, TextFormatting.DARK_BLUE + page.title, 0, 0, 0);
        GlStateManager.popMatrix();

        // 2. 繪製左頁文字
        drawText(page.leftContent, leftX, startY + 10, pageWidth);

        // 3. 繪製右頁 (結構 + 文字)
        int rightTextY = startY + 10;

        if (page.structure != null) {
            // 渲染結構並獲取佔用高度
            int structHeight = drawStructure(page.structure, centerX, guiTop, mouseX, mouseY);
            rightTextY += structHeight + 5;
        }

        // 繪製右頁剩餘文字
        drawText(page.rightContent, rightX, rightTextY, pageWidth);
    }

    /**
     * 輔助方法：繪製文字
     */
    private void drawText(String[] lines, int x, int y, int width) {
        for (String line : lines) {
            if (y > guiTop + BOOK_HEIGHT - 35) break;
            fontRenderer.drawSplitString(line, x, y, width, 0x000000);
            y += fontRenderer.getWordWrappedHeight(line, width);
        }
    }

    /**
     * ⭐⭐⭐ 核心渲染方法：繪製多方塊結構 ⭐⭐⭐
     * 包含：自動縮放、層級輪播、物品光照、Tooltip
     * @return 渲染區域佔用的總高度
     */
    private int drawStructure(StructureTemplate struct, int centerX, int topY, int mouseX, int mouseY) {
        if (struct == null || struct.layers.isEmpty()) return 0;

        // 1. 計算當前輪播到哪一層 (基於世界時間)
        long totalTime = Minecraft.getMinecraft().world != null ?
                Minecraft.getMinecraft().world.getTotalWorldTime() : 0;
        int totalLayers = struct.layers.size();
        int currentIdx = (int) ((totalTime / StructureTemplate.TICKS_PER_LAYER) % totalLayers);

        ItemStack[] layerBlocks = struct.layers.get(currentIdx);

        // 2. 計算自動縮放比例
        int maxWidth = 100;
        int maxHeight = 80;
        int realWidth = struct.sizeX * 16;
        int realHeight = struct.sizeZ * 16;

        float scale = Math.min(1.5f, Math.min((float) maxWidth / realWidth, (float) maxHeight / realHeight));

        int renderW = (int) (realWidth * scale);
        int renderH = (int) (realHeight * scale);

        // 3. 計算居中位置
        int offsetX = 12 + (maxWidth - renderW) / 2;
        int offsetY = 25;

        // 4. 開始繪製
        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX + offsetX, topY + offsetY, 100);
        GlStateManager.scale(scale, scale, 1.0f);

        RenderHelper.enableGUIStandardItemLighting();

        ItemStack hoveredStack = ItemStack.EMPTY;

        for (int z = 0; z < struct.sizeZ; z++) {
            for (int x = 0; x < struct.sizeX; x++) {
                int idx = z * struct.sizeX + x;
                ItemStack stack = layerBlocks[idx];

                if (!stack.isEmpty()) {
                    int itemX = x * 16;
                    int itemY = z * 16;

                    this.itemRender.renderItemAndEffectIntoGUI(stack, itemX, itemY);

                    // 檢測鼠標懸停
                    float screenX = centerX + offsetX + itemX * scale;
                    float screenY = topY + offsetY + itemY * scale;
                    float size = 16 * scale;

                    if (mouseX >= screenX && mouseX < screenX + size &&
                            mouseY >= screenY && mouseY < screenY + size) {
                        hoveredStack = stack;
                    }
                }
            }
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();

        // 5. 繪製層級提示
        String label = "§7第 " + currentIdx + " 層 (共" + totalLayers + "層)§r";
        if (currentIdx == 0) label += " §8(底座)§r";
        else if (currentIdx == totalLayers - 1) label += " §8(頂部)§r";

        int labelWidth = fontRenderer.getStringWidth(label);
        fontRenderer.drawString(label, centerX + 12 + (maxWidth - labelWidth) / 2, topY + offsetY + renderH + 5, 0x555555);

        // 6. 繪製 Tooltip
        if (!hoveredStack.isEmpty()) {
            this.renderToolTip(hoveredStack, mouseX, mouseY);
        }

        return renderH + 20;
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
     * 多方塊結構模板 - 用於存儲結構數據
     */
    private static class StructureTemplate {
        final int sizeX, sizeZ;
        final List<ItemStack[]> layers = new ArrayList<>();
        final Map<Character, ItemStack> keyMap = new HashMap<>();

        // 每層顯示時間 (Ticks)，40 ticks = 2秒
        static final int TICKS_PER_LAYER = 40;

        public StructureTemplate(int sizeX, int sizeZ) {
            this.sizeX = sizeX;
            this.sizeZ = sizeZ;
            keyMap.put('.', ItemStack.EMPTY);
            keyMap.put(' ', ItemStack.EMPTY);
        }

        public StructureTemplate addKey(char key, ItemStack stack) {
            keyMap.put(key, stack);
            return this;
        }

        public StructureTemplate addLayer(String... rows) {
            ItemStack[] layerData = new ItemStack[sizeX * sizeZ];
            for (int z = 0; z < sizeZ; z++) {
                String row = (z < rows.length) ? rows[z] : "";
                for (int x = 0; x < sizeX; x++) {
                    char c = (x < row.length()) ? row.charAt(x) : ' ';
                    layerData[z * sizeX + x] = keyMap.getOrDefault(c, ItemStack.EMPTY);
                }
            }
            layers.add(layerData);
            return this;
        }
    }

    // ========== CRT配置读取辅助方法 ==========

    /**
     * 获取仪式的材料显示文本
     * 自动读取CRT配置，如无配置则返回默认材料
     */
    private String[] getRitualMaterials(String ritualId, String[] defaultMaterials) {
        if (LegacyRitualConfig.hasCustomMaterials(ritualId)) {
            List<LegacyRitualConfig.MaterialRequirement> reqs = LegacyRitualConfig.getMaterialRequirements(ritualId);
            if (reqs.isEmpty()) {
                return new String[]{"§7(已被CRT清除)"};
            }
            List<String> result = new ArrayList<>();
            for (LegacyRitualConfig.MaterialRequirement req : reqs) {
                result.add("  " + req.getDescription());
            }
            return result.toArray(new String[0]);
        }
        return defaultMaterials;
    }

    /**
     * 获取仪式参数显示文本（时间、能量、失败率）
     */
    private String getRitualParamsText(String ritualId) {
        int duration = LegacyRitualConfig.getDuration(ritualId);
        int energy = LegacyRitualConfig.getEnergyPerPedestal(ritualId);
        float failChance = LegacyRitualConfig.getFailChance(ritualId);
        float successRate = (1 - failChance) * 100;

        return String.format("時間:%.1f秒 能量:%dRF/座 成功:%.0f%%",
                duration / 20.0f, energy, successRate);
    }

    /**
     * 頁面內容容器
     */
    private static class GuidePageContent {
        final String title;
        final String[] leftContent;
        final String[] rightContent;
        final StructureTemplate structure;

        GuidePageContent(String title, String[] left, String[] right) {
            this(title, left, right, null);
        }

        GuidePageContent(String title, String[] left, String[] right, StructureTemplate structure) {
            this.title = title;
            this.leftContent = left;
            this.rightContent = right;
            this.structure = structure;
        }
    }
}