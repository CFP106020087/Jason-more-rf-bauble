package com.moremod.quarry.gui;

import com.moremod.quarry.QuarryMode;
import com.moremod.quarry.network.PacketHandler;
import com.moremod.quarry.network.PacketQuarryButton;
import com.moremod.quarry.network.PacketSelectBiome;
import com.moremod.quarry.tile.TileQuantumQuarry;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 量子採石場 GUI - 純代碼繪製版本（不使用圖片）
 *
 * 槽位布局（來自 ContainerQuantumQuarry）：
 * - 附魔書槽位：(8, 17)
 * - 過濾器槽位：(8, 53)
 * - 輸出緩衝槽位：3x6 網格，起始於 (62, 17)
 * - 玩家物品欄：標準位置 (8, 84)
 * - 玩家快捷欄：標準位置 (8, 142)
 */
@SideOnly(Side.CLIENT)
public class GuiQuantumQuarry extends GuiContainer {

    private final TileQuantumQuarry tile;
    private final ContainerQuantumQuarry container;

    // 按鈕 - 使用小型按鈕避免與槽位衝突
    private GuiButton modeButton;
    private GuiButton redstoneButton;
    private GuiButton biomeButton;

    // 生物群系選擇器
    private boolean showBiomeSelector = false;
    private List<Biome> biomeList;
    private int biomeScrollOffset = 0;
    private static final int BIOMES_PER_PAGE = 8;

    // 顏色常量
    private static final int COLOR_BG_DARK = 0xFF1A1A2E;
    private static final int COLOR_BG_MEDIUM = 0xFF16213E;
    private static final int COLOR_BG_LIGHT = 0xFF0F3460;
    private static final int COLOR_BORDER = 0xFF00D9FF;
    private static final int COLOR_BORDER_DIM = 0xFF006080;
    private static final int COLOR_ENERGY_FULL = 0xFF00FF00;
    private static final int COLOR_ENERGY_MID = 0xFFFFFF00;
    private static final int COLOR_ENERGY_LOW = 0xFFFF0000;
    private static final int COLOR_PROGRESS = 0xFF00D9FF;
    private static final int COLOR_TEXT_TITLE = 0x00D9FF;
    private static final int COLOR_TEXT_NORMAL = 0xCCCCCC;
    private static final int COLOR_TEXT_WARNING = 0xFF6666;
    private static final int COLOR_SLOT_BG = 0xFF8B8B8B;

    public GuiQuantumQuarry(InventoryPlayer playerInventory, TileQuantumQuarry tile) {
        super(new ContainerQuantumQuarry(playerInventory, tile));
        this.tile = tile;
        this.container = (ContainerQuantumQuarry) inventorySlots;

        this.xSize = 176;
        this.ySize = 166;

        // 初始化生物群系列表
        biomeList = new ArrayList<>();
        for (Biome biome : ForgeRegistries.BIOMES) {
            biomeList.add(biome);
        }
        biomeList.sort(Comparator.comparing(Biome::getBiomeName));
    }

    @Override
    public void initGui() {
        super.initGui();

        buttonList.clear();

        // 按鈕放在左側槽位下方和右側能量條旁邊
        // 模式切換按鈕 - 放在左上角標題欄
        modeButton = new GuiButton(0, guiLeft + 4, guiTop + 4, 40, 12, "");
        buttonList.add(modeButton);

        // 紅石控制按鈕 - 放在過濾器槽位下方
        redstoneButton = new GuiButton(1, guiLeft + 4, guiTop + 70, 26, 12, "");
        buttonList.add(redstoneButton);

        // 生物群系選擇按鈕 - 放在右側能量條下方
        biomeButton = new GuiButton(2, guiLeft + 32, guiTop + 70, 26, 12, "Bio");
        buttonList.add(biomeButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;

        switch (button.id) {
            case 0:  // 切換模式
                PacketHandler.INSTANCE.sendToServer(new PacketQuarryButton(tile.getPos(), 0));
                break;
            case 1:  // 切換紅石控制
                PacketHandler.INSTANCE.sendToServer(new PacketQuarryButton(tile.getPos(), 1));
                break;
            case 2:  // 顯示/隱藏生物群系選擇器
                showBiomeSelector = !showBiomeSelector;
                break;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // 處理生物群系選擇 - 必須在 super.mouseClicked 之前處理
        if (showBiomeSelector && mouseButton == 0) {
            int selectorX = guiLeft + xSize + 2;
            int selectorY = guiTop + 10;
            int selectorWidth = 120;
            int entryHeight = 14;

            if (mouseX >= selectorX && mouseX < selectorX + selectorWidth) {
                int relativeY = mouseY - selectorY;
                if (relativeY >= 0 && relativeY < BIOMES_PER_PAGE * entryHeight) {
                    int index = biomeScrollOffset + relativeY / entryHeight;
                    if (index < biomeList.size()) {
                        Biome selectedBiome = biomeList.get(index);
                        int biomeId = Biome.REGISTRY.getIDForObject(selectedBiome);
                        PacketHandler.INSTANCE.sendToServer(new PacketSelectBiome(tile.getPos(), biomeId));
                        showBiomeSelector = false;
                        return;
                    }
                }
            }

            // 點擊選擇器外部關閉
            if (mouseX < selectorX || mouseX > selectorX + selectorWidth ||
                mouseY < selectorY || mouseY > selectorY + BIOMES_PER_PAGE * entryHeight + 4) {
                showBiomeSelector = false;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        // 處理生物群系列表滾動
        if (showBiomeSelector) {
            int scroll = org.lwjgl.input.Mouse.getDWheel();
            if (scroll != 0) {
                if (scroll > 0) {
                    biomeScrollOffset = Math.max(0, biomeScrollOffset - 1);
                } else {
                    biomeScrollOffset = Math.min(Math.max(0, biomeList.size() - BIOMES_PER_PAGE), biomeScrollOffset + 1);
                }
            }
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableTexture2D();

        // ===== 主背景 =====
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, COLOR_BG_DARK);

        // 外框
        drawHollowRect(guiLeft, guiTop, xSize, ySize, COLOR_BORDER);

        // 標題欄背景
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + xSize - 1, guiTop + 16, COLOR_BG_MEDIUM);
        drawHorizontalLine(guiLeft + 1, guiLeft + xSize - 2, guiTop + 16, COLOR_BORDER_DIM);

        // ===== 左側區域（附魔書槽 + 過濾器槽）=====
        // 附魔書槽位背景 (8, 17)
        drawSlotBackground(guiLeft + 8, guiTop + 17);

        // 標籤
        GlStateManager.enableTexture2D();
        fontRenderer.drawString("E", guiLeft + 26, guiTop + 21, COLOR_TEXT_NORMAL);
        GlStateManager.disableTexture2D();

        // 過濾器槽位背景 (8, 53)
        drawSlotBackground(guiLeft + 8, guiTop + 53);

        GlStateManager.enableTexture2D();
        fontRenderer.drawString("F", guiLeft + 26, guiTop + 57, COLOR_TEXT_NORMAL);
        GlStateManager.disableTexture2D();

        // ===== 中間區域（輸出緩衝 3x6）=====
        drawRect(guiLeft + 60, guiTop + 15, guiLeft + 170, guiTop + 72, COLOR_BG_MEDIUM);
        drawHollowRect(guiLeft + 60, guiTop + 15, 110, 57, COLOR_BORDER_DIM);

        // 繪製輸出物品槽背景 (3x6 網格)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 6; col++) {
                int slotX = guiLeft + 62 + col * 18;
                int slotY = guiTop + 17 + row * 18;
                drawSlotBackground(slotX, slotY);
            }
        }

        // ===== 進度條 =====
        int progress = tile.getProgress();
        drawRect(guiLeft + 32, guiTop + 36, guiLeft + 58, guiTop + 42, 0xFF000000);
        if (progress > 0) {
            int progressWidth = progress * 24 / 100;
            drawRect(guiLeft + 33, guiTop + 37, guiLeft + 33 + progressWidth, guiTop + 41, COLOR_PROGRESS);
        }
        drawHollowRect(guiLeft + 32, guiTop + 36, 26, 6, COLOR_BORDER_DIM);

        // ===== 能量條（右下角）=====
        int energyBarX = guiLeft + 32;
        int energyBarY = guiTop + 44;
        drawRect(energyBarX, energyBarY, energyBarX + 24, energyBarY + 24, 0xFF000000);
        drawHollowRect(energyBarX, energyBarY, 24, 24, COLOR_BORDER_DIM);

        // 能量條填充
        int energyHeight = getEnergyBarHeight(22);
        if (energyHeight > 0) {
            int energyColor = getEnergyColor();
            drawRect(energyBarX + 1, energyBarY + 23 - energyHeight, energyBarX + 23, energyBarY + 23, energyColor);
        }

        // ===== 玩家物品欄區域 =====
        drawRect(guiLeft + 4, guiTop + 82, guiLeft + xSize - 4, guiTop + ySize - 4, COLOR_BG_MEDIUM);
        drawHollowRect(guiLeft + 4, guiTop + 82, xSize - 8, ySize - 86, COLOR_BORDER_DIM);

        // 玩家物品欄槽位 (來自 Container：8, 84)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = guiLeft + 8 + col * 18;
                int slotY = guiTop + 84 + row * 18;
                drawSlotBackground(slotX, slotY);
            }
        }

        // 玩家快捷欄 (來自 Container：8, 142)
        for (int col = 0; col < 9; col++) {
            int slotX = guiLeft + 8 + col * 18;
            int slotY = guiTop + 142;
            drawSlotBackground(slotX, slotY);
        }

        GlStateManager.enableTexture2D();

        // 繪製生物群系選擇器
        if (showBiomeSelector) {
            drawBiomeSelector(mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 標題
        String title = I18n.format("tile.quantum_quarry.name");
        if (title.equals("tile.quantum_quarry.name")) {
            title = "Quantum Quarry";
        }
        fontRenderer.drawString(title, 48, 5, COLOR_TEXT_TITLE);

        // 更新按鈕文本
        updateButtonText();

        // 狀態信息
        drawStatusInfo();

        // 繪製工具提示
        drawTooltips(mouseX, mouseY);
    }

    private void updateButtonText() {
        // 模式按鈕
        QuarryMode mode = tile.getMode();
        String modeText;
        switch (mode) {
            case MINING: modeText = "Mine"; break;
            case MOB_DROPS: modeText = "Mob"; break;
            case LOOT_TABLE: modeText = "Loot"; break;
            default: modeText = "???"; break;
        }
        modeButton.displayString = modeText;

        // 紅石按鈕
        redstoneButton.displayString = tile.isRedstoneControlEnabled() ? "RS" : "rs";

        // 生物群系按鈕
        biomeButton.displayString = "Bio";
    }

    private void drawStatusInfo() {
        // 結構狀態
        if (!tile.isStructureValid()) {
            String warning = "\u00a7c!";
            fontRenderer.drawString(warning, 27, 22, 0xFFFFFF);
        }

        // 能量顯示
        String energyText = formatEnergy(tile.getEnergyStored());
        fontRenderer.drawString(energyText, 32, 76, COLOR_TEXT_NORMAL);
    }

    private void drawTooltips(int mouseX, int mouseY) {
        int relX = mouseX - guiLeft;
        int relY = mouseY - guiTop;

        // 能量條工具提示
        if (relX >= 32 && relX < 56 && relY >= 44 && relY < 68) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a7bEnergy");
            tooltip.add(formatEnergy(tile.getEnergyStored()) + " / " + formatEnergy(tile.getMaxEnergyStored()) + " RF");
            int percent = tile.getMaxEnergyStored() > 0 ?
                (tile.getEnergyStored() * 100 / tile.getMaxEnergyStored()) : 0;
            tooltip.add("\u00a77" + percent + "%");
            drawHoveringText(tooltip, relX, relY);
        }

        // 附魔書槽位工具提示
        if (relX >= 8 && relX < 24 && relY >= 17 && relY < 33) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a7bEnchant Book Slot");
            tooltip.add("\u00a77Place Fortune/Looting books");
            drawHoveringText(tooltip, relX, relY);
        }

        // 過濾器槽位工具提示
        if (relX >= 8 && relX < 24 && relY >= 53 && relY < 69) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a7bFilter Slot");
            tooltip.add("\u00a77Items to prioritize");
            drawHoveringText(tooltip, relX, relY);
        }

        // 進度條工具提示
        if (relX >= 32 && relX < 58 && relY >= 36 && relY < 42) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a7bProgress: \u00a7f" + tile.getProgress() + "%");
            drawHoveringText(tooltip, relX, relY);
        }

        // 模式按鈕工具提示
        if (modeButton.isMouseOver()) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a7bMode: \u00a7f" + tile.getMode().getName());
            tooltip.add("\u00a77Click to cycle modes");
            tooltip.add("\u00a78Mine=Ores, Mob=Drops, Loot=Tables");
            drawHoveringText(tooltip, relX, relY);
        }

        // 紅石按鈕工具提示
        if (redstoneButton.isMouseOver()) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a7bRedstone Control");
            tooltip.add(tile.isRedstoneControlEnabled() ?
                "\u00a7aEnabled - Requires signal" : "\u00a7cDisabled - Always run");
            drawHoveringText(tooltip, relX, relY);
        }

        // 生物群系按鈕工具提示
        if (biomeButton.isMouseOver()) {
            List<String> tooltip = new ArrayList<>();
            Biome biome = tile.getSelectedBiome();
            tooltip.add("\u00a7bBiome Filter");
            tooltip.add("\u00a7f" + (biome != null ? biome.getBiomeName() : "Not Selected"));
            tooltip.add("\u00a77Click to select biome");
            drawHoveringText(tooltip, relX, relY);
        }

        // 結構狀態工具提示
        if (!tile.isStructureValid() && relX >= 26 && relX < 32 && relY >= 18 && relY < 30) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a7cStructure Invalid!");
            tooltip.add("\u00a77Place Quarry Actuators around");
            drawHoveringText(tooltip, relX, relY);
        }
    }

    private void drawBiomeSelector(int mouseX, int mouseY) {
        GlStateManager.disableTexture2D();

        int selectorX = guiLeft + xSize + 2;
        int selectorY = guiTop + 10;
        int selectorWidth = 120;
        int selectorHeight = BIOMES_PER_PAGE * 14 + 4;

        // 背景
        drawRect(selectorX - 2, selectorY - 2, selectorX + selectorWidth + 2,
                 selectorY + selectorHeight + 2, COLOR_BG_DARK);
        drawRect(selectorX, selectorY, selectorX + selectorWidth,
                 selectorY + selectorHeight, COLOR_BG_MEDIUM);
        drawHollowRect(selectorX - 2, selectorY - 2, selectorWidth + 4, selectorHeight + 4, COLOR_BORDER);

        GlStateManager.enableTexture2D();

        // 生物群系列表
        for (int i = 0; i < BIOMES_PER_PAGE; i++) {
            int index = biomeScrollOffset + i;
            if (index >= biomeList.size()) break;

            Biome biome = biomeList.get(index);
            int entryY = selectorY + 2 + i * 14;

            // 高亮選中的或鼠標懸停的
            boolean isSelected = biome.equals(tile.getSelectedBiome());
            boolean isHovered = mouseX >= selectorX && mouseX < selectorX + selectorWidth &&
                               mouseY >= entryY && mouseY < entryY + 14;

            GlStateManager.disableTexture2D();
            if (isSelected) {
                drawRect(selectorX, entryY, selectorX + selectorWidth - 6, entryY + 14, 0xFF006600);
            } else if (isHovered) {
                drawRect(selectorX, entryY, selectorX + selectorWidth - 6, entryY + 14, COLOR_BG_LIGHT);
            }
            GlStateManager.enableTexture2D();

            String name = biome.getBiomeName();
            if (fontRenderer.getStringWidth(name) > selectorWidth - 10) {
                while (fontRenderer.getStringWidth(name + "...") > selectorWidth - 10 && name.length() > 0) {
                    name = name.substring(0, name.length() - 1);
                }
                name += "...";
            }

            int textColor = isSelected ? 0x00FF00 : (isHovered ? 0xFFFFFF : 0xCCCCCC);
            fontRenderer.drawString(name, selectorX + 2, entryY + 3, textColor);
        }

        // 滾動條
        if (biomeList.size() > BIOMES_PER_PAGE) {
            GlStateManager.disableTexture2D();
            int scrollBarHeight = selectorHeight - 4;
            int thumbHeight = Math.max(10, scrollBarHeight * BIOMES_PER_PAGE / biomeList.size());
            int maxScroll = biomeList.size() - BIOMES_PER_PAGE;
            int thumbY = selectorY + 2 + (maxScroll > 0 ?
                (scrollBarHeight - thumbHeight) * biomeScrollOffset / maxScroll : 0);

            drawRect(selectorX + selectorWidth - 5, selectorY + 2,
                     selectorX + selectorWidth - 1, selectorY + selectorHeight - 2, 0xFF202020);
            drawRect(selectorX + selectorWidth - 5, thumbY,
                     selectorX + selectorWidth - 1, thumbY + thumbHeight, COLOR_BORDER);
            GlStateManager.enableTexture2D();
        }
    }

    // ===== 繪製輔助方法 =====

    private void drawSlotBackground(int x, int y) {
        // 槽位邊框 (3D效果) - 標準 Minecraft 風格
        drawRect(x - 1, y - 1, x + 17, y, 0xFF373737);      // 上
        drawRect(x - 1, y - 1, x, y + 17, 0xFF373737);      // 左
        drawRect(x + 16, y - 1, x + 17, y + 17, 0xFFFFFFFF); // 右
        drawRect(x - 1, y + 16, x + 17, y + 17, 0xFFFFFFFF); // 下
        // 槽位背景
        drawRect(x, y, x + 16, y + 16, COLOR_SLOT_BG);
    }

    private void drawHollowRect(int x, int y, int width, int height, int color) {
        drawHorizontalLine(x, x + width - 1, y, color);
        drawHorizontalLine(x, x + width - 1, y + height - 1, color);
        drawVerticalLine(x, y, y + height - 1, color);
        drawVerticalLine(x + width - 1, y, y + height - 1, color);
    }

    private int getEnergyBarHeight(int maxHeight) {
        int stored = tile.getEnergyStored();
        int max = tile.getMaxEnergyStored();
        if (max == 0) return 0;
        return stored * maxHeight / max;
    }

    private int getEnergyColor() {
        int stored = tile.getEnergyStored();
        int max = tile.getMaxEnergyStored();
        if (max == 0) return COLOR_ENERGY_LOW;

        float percent = (float) stored / max;
        if (percent > 0.5f) {
            return COLOR_ENERGY_FULL;
        } else if (percent > 0.25f) {
            return COLOR_ENERGY_MID;
        } else {
            return COLOR_ENERGY_LOW;
        }
    }

    private String formatEnergy(int energy) {
        if (energy >= 1000000) {
            return String.format("%.1fM", energy / 1000000.0);
        } else if (energy >= 1000) {
            return String.format("%.0fK", energy / 1000.0);
        }
        return String.valueOf(energy);
    }
}
