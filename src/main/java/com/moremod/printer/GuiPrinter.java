package com.moremod.printer;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * 打印机GUI - 纯代码绘制版本
 *
 * 布局:
 * ┌─────────────────────────────────────────────┐
 * │         远古打印机              [状态]      │
 * │  ┌──┐   ┌──┬──┬──┐   ┌──┐                  │
 * │  │能│   │材│材│材│   │  │                  │
 * │  │量│ ┌─┤料│料│料├─→ │输│                  │
 * │  │条│ │模│材│材│材│   │出│                  │
 * │  │  │ │版│料│料│料│   │  │                  │
 * │  │  │ └─┴──┴──┴──┘   └──┘                  │
 * │  └──┘     [进度条]                          │
 * │─────────────────────────────────────────────│
 * │           玩家背包                          │
 * └─────────────────────────────────────────────┘
 */
@SideOnly(Side.CLIENT)
public class GuiPrinter extends GuiContainer {

    private final TileEntityPrinter tile;

    // 颜色定义
    private static final int COLOR_BG_DARK = 0xFFC6C6C6;      // 背景深色
    private static final int COLOR_BG_LIGHT = 0xFFE0E0E0;     // 背景浅色
    private static final int COLOR_BORDER_DARK = 0xFF373737;  // 边框深色
    private static final int COLOR_BORDER_LIGHT = 0xFFFFFFFF; // 边框浅色
    private static final int COLOR_SLOT_BG = 0xFF8B8B8B;      // 槽位背景
    private static final int COLOR_ENERGY_EMPTY = 0xFF1A1A1A; // 能量条空
    private static final int COLOR_ENERGY_FULL = 0xFFFF4444;  // 能量条满 (红色)
    private static final int COLOR_ENERGY_MID = 0xFFFFAA00;   // 能量条中 (橙色)
    private static final int COLOR_PROGRESS_BG = 0xFF4A4A4A;  // 进度条背景
    private static final int COLOR_PROGRESS_FG = 0xFF00FF00;  // 进度条前景 (绿色)
    private static final int COLOR_WARNING = 0x80FF0000;      // 警告覆盖 (半透明红)

    public GuiPrinter(InventoryPlayer playerInventory, TileEntityPrinter tile) {
        super(new ContainerPrinter(playerInventory, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int x = guiLeft;
        int y = guiTop;

        // 绘制主背景
        drawMainBackground(x, y);

        // 绘制能量条
        drawEnergyBar(x + 8, y + 17, 14, 52);

        // 绘制模版槽位背景
        drawSlotBackground(x + 25, y + 34, 18, 18, true);  // 模版槽 (特殊边框)

        // 绘制材料槽位背景 (3x3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlotBackground(x + 61 + col * 18, y + 16 + row * 18, 18, 18, false);
            }
        }

        // 绘制输出槽位背景 (稍大一点)
        drawSlotBackground(x + 133, y + 34, 18, 18, true);

        // 绘制箭头
        drawArrow(x + 115, y + 35, 14, 16);

        // 绘制进度条
        drawProgressBar(x + 61, y + 72, 54, 6);

        // 绘制玩家背包槽位
        drawPlayerInventorySlots(x, y);

        // 如果多方块未形成，绘制警告覆盖
        if (!tile.isMultiblockFormed()) {
            drawWarningOverlay(x + 60, y + 15, 58, 58);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 标题
        String title = I18n.format("container.moremod.printer");
        this.fontRenderer.drawString(title, (this.xSize - this.fontRenderer.getStringWidth(title)) / 2, 6, 0x404040);

        // 玩家背包标题
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 0x404040);

        // 多方块状态图标
        String statusIcon = tile.isMultiblockFormed() ? "\u2714" : "\u2718";  // 勾或叉
        int statusColor = tile.isMultiblockFormed() ? 0x00AA00 : 0xAA0000;
        this.fontRenderer.drawString(statusIcon, this.xSize - 12, 6, statusColor);

        // 进度百分比
        if (tile.isProcessing() && tile.getMaxProgress() > 0) {
            int progress = tile.getProgress();
            int maxProgress = tile.getMaxProgress();
            String progressText = String.format("%d%%", (progress * 100 / maxProgress));
            int textWidth = this.fontRenderer.getStringWidth(progressText);
            this.fontRenderer.drawString(progressText, 88 - textWidth / 2, 62, 0x404040);
        }

        // 能量文字 (在能量条下方)
        String energyText = formatEnergy(tile.getEnergyStored());
        int energyTextWidth = this.fontRenderer.getStringWidth(energyText);
        this.fontRenderer.drawString(energyText, 15 - energyTextWidth / 2, 70, 0x404040);
    }

    /**
     * 绘制主背景
     */
    private void drawMainBackground(int x, int y) {
        // 外边框
        drawRect(x, y, x + xSize, y + ySize, COLOR_BORDER_DARK);

        // 内部背景
        drawRect(x + 3, y + 3, x + xSize - 3, y + ySize - 3, COLOR_BG_DARK);

        // 3D边框效果
        // 顶部和左侧亮边
        drawHorizontalLine(x + 1, x + xSize - 2, y + 1, COLOR_BORDER_LIGHT);
        drawVerticalLine(x + 1, y + 1, y + ySize - 2, COLOR_BORDER_LIGHT);

        // 底部和右侧暗边
        drawHorizontalLine(x + 2, x + xSize - 1, y + ySize - 2, COLOR_BORDER_DARK);
        drawVerticalLine(x + xSize - 2, y + 2, y + ySize - 1, COLOR_BORDER_DARK);

        // 标题区域
        drawRect(x + 4, y + 4, x + xSize - 4, y + 16, COLOR_BG_LIGHT);

        // 分隔线 (玩家背包上方)
        drawHorizontalLine(x + 7, x + xSize - 8, y + 82, COLOR_BORDER_DARK);
    }

    /**
     * 绘制槽位背景
     */
    private void drawSlotBackground(int x, int y, int width, int height, boolean special) {
        // 槽位背景
        drawRect(x, y, x + width, y + height, COLOR_SLOT_BG);

        // 3D凹陷效果
        drawHorizontalLine(x, x + width - 1, y, COLOR_BORDER_DARK);
        drawVerticalLine(x, y, y + height - 1, COLOR_BORDER_DARK);
        drawHorizontalLine(x + 1, x + width, y + height - 1, COLOR_BORDER_LIGHT);
        drawVerticalLine(x + width - 1, y + 1, y + height, COLOR_BORDER_LIGHT);

        // 特殊槽位 (模版和输出) 有金色边框
        if (special) {
            int gold = 0xFFFFD700;
            drawHorizontalLine(x - 1, x + width, y - 1, gold);
            drawHorizontalLine(x - 1, x + width, y + height, gold);
            drawVerticalLine(x - 1, y - 1, y + height + 1, gold);
            drawVerticalLine(x + width, y - 1, y + height + 1, gold);
        }
    }

    /**
     * 绘制能量条
     */
    private void drawEnergyBar(int x, int y, int width, int height) {
        // 背景
        drawRect(x, y, x + width, y + height, COLOR_ENERGY_EMPTY);

        // 3D凹陷效果
        drawHorizontalLine(x, x + width - 1, y, COLOR_BORDER_DARK);
        drawVerticalLine(x, y, y + height - 1, COLOR_BORDER_DARK);

        // 能量填充
        int stored = tile.getEnergyStored();
        int max = tile.getMaxEnergyStored();
        if (max > 0 && stored > 0) {
            int fillHeight = stored * (height - 2) / max;
            if (fillHeight > 0) {
                // 渐变色: 低电量红色，高电量绿色
                float ratio = (float) stored / max;
                int color;
                if (ratio < 0.3f) {
                    color = COLOR_ENERGY_FULL;  // 红色
                } else if (ratio < 0.7f) {
                    color = COLOR_ENERGY_MID;   // 橙色
                } else {
                    color = 0xFF00FF00;         // 绿色
                }
                drawRect(x + 1, y + height - 1 - fillHeight, x + width - 1, y + height - 1, color);
            }
        }

        // 刻度线 (每25%一条)
        for (int i = 1; i < 4; i++) {
            int lineY = y + height - (height * i / 4);
            drawHorizontalLine(x + 1, x + 3, lineY, 0xFF000000);
            drawHorizontalLine(x + width - 4, x + width - 2, lineY, 0xFF000000);
        }
    }

    /**
     * 绘制进度条
     */
    private void drawProgressBar(int x, int y, int width, int height) {
        // 背景
        drawRect(x, y, x + width, y + height, COLOR_PROGRESS_BG);

        // 3D凹陷效果
        drawHorizontalLine(x, x + width - 1, y, 0xFF2A2A2A);
        drawVerticalLine(x, y, y + height - 1, 0xFF2A2A2A);

        // 进度填充
        if (tile.isProcessing() && tile.getMaxProgress() > 0) {
            int progress = tile.getProgress();
            int maxProgress = tile.getMaxProgress();
            int fillWidth = progress * (width - 2) / maxProgress;
            if (fillWidth > 0) {
                // 渐变绿色效果
                for (int i = 0; i < fillWidth; i++) {
                    float ratio = (float) i / (width - 2);
                    int green = (int) (180 + 75 * ratio);
                    int color = 0xFF000000 | (green << 8);
                    drawVerticalLine(x + 1 + i, y + 1, y + height - 2, color);
                }
            }
        }
    }

    /**
     * 绘制箭头
     */
    private void drawArrow(int x, int y, int width, int height) {
        int midY = y + height / 2;

        // 箭头主体
        drawRect(x, midY - 2, x + width - 4, midY + 3, 0xFF808080);

        // 箭头尖端
        for (int i = 0; i < 5; i++) {
            drawVerticalLine(x + width - 4 + i, midY - i, midY + i + 1, 0xFF808080);
        }

        // 如果正在处理，填充绿色
        if (tile.isProcessing() && tile.getMaxProgress() > 0) {
            int progress = tile.getProgress();
            int maxProgress = tile.getMaxProgress();
            int fillWidth = progress * (width - 4) / maxProgress;
            if (fillWidth > 0) {
                drawRect(x + 1, midY - 1, x + 1 + fillWidth, midY + 2, COLOR_PROGRESS_FG);
            }
        }
    }

    /**
     * 绘制警告覆盖
     */
    private void drawWarningOverlay(int x, int y, int width, int height) {
        // 半透明红色覆盖
        drawRect(x, y, x + width, y + height, COLOR_WARNING);

        // 绘制X符号
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        int size = 10;

        for (int i = -size; i <= size; i++) {
            // 对角线1
            drawRect(centerX + i - 1, centerY + i - 1, centerX + i + 2, centerY + i + 2, 0xFFFF0000);
            // 对角线2
            drawRect(centerX + i - 1, centerY - i - 1, centerX + i + 2, centerY - i + 2, 0xFFFF0000);
        }
    }

    /**
     * 绘制玩家背包槽位
     */
    private void drawPlayerInventorySlots(int x, int y) {
        // 主背包 (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBackground(x + 7 + col * 18, y + 83 + row * 18, 18, 18, false);
            }
        }

        // 快捷栏 (1x9)
        for (int col = 0; col < 9; col++) {
            drawSlotBackground(x + 7 + col * 18, y + 141, 18, 18, false);
        }
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        super.renderHoveredToolTip(mouseX, mouseY);

        int x = guiLeft;
        int y = guiTop;

        // 能量条tooltip
        if (mouseX >= x + 8 && mouseX <= x + 22 && mouseY >= y + 17 && mouseY <= y + 69) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a76" + I18n.format("tooltip.moremod.energy_cost"));
            tooltip.add(formatEnergy(tile.getEnergyStored()) + " / " + formatEnergy(tile.getMaxEnergyStored()) + " RF");

            // 如果有配方，显示消耗
            PrinterRecipe recipe = tile.getCurrentRecipe();
            if (recipe != null) {
                tooltip.add("");
                tooltip.add("\u00a7c" + I18n.format("gui.moremod.printer.recipe_cost") + ": " + formatEnergy(recipe.getEnergyCost()) + " RF");
            }

            this.drawHoveringText(tooltip, mouseX, mouseY);
        }

        // 多方块状态tooltip
        if (!tile.isMultiblockFormed() && mouseX >= x + 60 && mouseX <= x + 118 && mouseY >= y + 15 && mouseY <= y + 73) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a7c" + I18n.format("gui.moremod.printer.multiblock_required"));
            tooltip.add("");
            tooltip.add("\u00a77" + I18n.format("gui.moremod.printer.build_guide"));
            tooltip.add("\u00a7f  3x3 " + I18n.format("tile.blockIron.name"));
            tooltip.add("\u00a7f  4x " + I18n.format("tile.blockRedstone.name"));
            this.drawHoveringText(tooltip, mouseX, mouseY);
        }

        // 进度条tooltip
        if (tile.isProcessing() && mouseX >= x + 61 && mouseX <= x + 115 && mouseY >= y + 72 && mouseY <= y + 78) {
            List<String> tooltip = new ArrayList<>();
            int progress = tile.getProgress();
            int maxProgress = tile.getMaxProgress();
            float seconds = (maxProgress - progress) / 20.0f;
            tooltip.add("\u00a7a" + I18n.format("tooltip.moremod.processing_time"));
            tooltip.add(String.format("%.1fs / %.1fs", progress / 20.0f, maxProgress / 20.0f));
            tooltip.add("\u00a77" + String.format("(%.1fs remaining)", seconds));
            this.drawHoveringText(tooltip, mouseX, mouseY);
        }
    }

    private String formatEnergy(int energy) {
        if (energy >= 1000000) {
            return String.format("%.2fM", energy / 1000000.0);
        } else if (energy >= 1000) {
            return String.format("%.1fk", energy / 1000.0);
        }
        return String.valueOf(energy);
    }
}
