package com.moremod.gui;

import com.moremod.container.ContainerChargingStation;
import com.moremod.tile.TileEntityChargingStation;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * 充能站GUI - 純代碼繪製
 */
@SideOnly(Side.CLIENT)
public class GuiChargingStation extends GuiContainer {

    private final TileEntityChargingStation tile;

    // 顏色常量
    private static final int COLOR_BG = 0xFF1A1A2E;          // 深藍黑背景
    private static final int COLOR_BORDER = 0xFF0F3460;      // 深藍邊框
    private static final int COLOR_ACCENT = 0xFF16C79A;      // 青綠強調色
    private static final int COLOR_ENERGY_BG = 0xFF0F0F1A;   // 能量條背景
    private static final int COLOR_ENERGY_FILL = 0xFF00D9FF; // 能量條填充（青色）
    private static final int COLOR_ENERGY_GLOW = 0xFF00FFFF; // 能量條發光
    private static final int COLOR_SLOT_BG = 0xFF2A2A4A;     // 槽位背景
    private static final int COLOR_SLOT_BORDER = 0xFF4A4A7A; // 槽位邊框
    private static final int COLOR_TEXT_TITLE = 0xFFFFFFFF;  // 標題文字
    private static final int COLOR_TEXT_INFO = 0xFFAAAAAA;   // 信息文字
    private static final int COLOR_TEXT_ENERGY = 0xFF00FFFF; // 能量文字

    public GuiChargingStation(InventoryPlayer playerInventory, TileEntityChargingStation tile) {
        super(new ContainerChargingStation(playerInventory, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);

        // 能量條tooltip
        int energyBarX = guiLeft + 10;
        int energyBarY = guiTop + 17;
        if (mouseX >= energyBarX && mouseX <= energyBarX + 12 &&
            mouseY >= energyBarY && mouseY <= energyBarY + 54) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("§b能量存儲");
            tooltip.add("§f" + formatEnergy(tile.getEnergyStored()) + " §7/ §f" + formatEnergy(tile.getMaxEnergyStored()) + " §bRF");
            tooltip.add("");
            tooltip.add("§7充電速度: §a無限快");
            drawHoveringText(tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();

        int x = guiLeft;
        int y = guiTop;

        // 主背景
        drawRect(x, y, x + xSize, y + ySize, COLOR_BG);

        // 外邊框
        drawHorizontalLine(x, x + xSize - 1, y, COLOR_BORDER);
        drawHorizontalLine(x, x + xSize - 1, y + ySize - 1, COLOR_BORDER);
        drawVerticalLine(x, y, y + ySize - 1, COLOR_BORDER);
        drawVerticalLine(x + xSize - 1, y, y + ySize - 1, COLOR_BORDER);

        // 強調線
        drawHorizontalLine(x + 1, x + xSize - 2, y + 1, COLOR_ACCENT);
        drawHorizontalLine(x + 1, x + xSize - 2, y + ySize - 2, COLOR_ACCENT);

        // 分隔線
        drawHorizontalLine(x + 7, x + xSize - 8, y + 75, COLOR_BORDER);

        // 能量條背景
        int energyBarX = x + 10;
        int energyBarY = y + 17;
        int energyBarWidth = 12;
        int energyBarHeight = 54;
        drawRect(energyBarX - 1, energyBarY - 1, energyBarX + energyBarWidth + 1, energyBarY + energyBarHeight + 1, COLOR_SLOT_BORDER);
        drawRect(energyBarX, energyBarY, energyBarX + energyBarWidth, energyBarY + energyBarHeight, COLOR_ENERGY_BG);

        // 能量條填充
        float energyPercent = (float) tile.getEnergyStored() / tile.getMaxEnergyStored();
        int filledHeight = (int) (energyBarHeight * energyPercent);
        if (filledHeight > 0) {
            int fillY = energyBarY + energyBarHeight - filledHeight;
            drawGradientRect(energyBarX, fillY, energyBarX + energyBarWidth, energyBarY + energyBarHeight,
                COLOR_ENERGY_GLOW, COLOR_ENERGY_FILL);
        }

        // 繪製9個充電槽位背景
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = x + 61 + col * 18;
                int slotY = y + 16 + row * 18;
                drawRect(slotX, slotY, slotX + 18, slotY + 18, COLOR_SLOT_BORDER);
                drawRect(slotX + 1, slotY + 1, slotX + 17, slotY + 17, COLOR_SLOT_BG);
            }
        }

        // 繪製裝飾元素 - 閃電圖標
        drawLightningBolt(x + 140, y + 30);

        // 繪製玩家背包槽位背景
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + 7 + col * 18;
                int slotY = y + 83 + row * 18;
                drawRect(slotX, slotY, slotX + 18, slotY + 18, 0xFF3A3A5A);
                drawRect(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF2A2A3A);
            }
        }

        // 繪製快捷欄槽位背景
        for (int col = 0; col < 9; col++) {
            int slotX = x + 7 + col * 18;
            int slotY = y + 141;
            drawRect(slotX, slotY, slotX + 18, slotY + 18, 0xFF4A4A6A);
            drawRect(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF2A2A3A);
        }

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    /**
     * 繪製閃電圖標
     */
    private void drawLightningBolt(int x, int y) {
        int color = COLOR_ENERGY_GLOW;
        // 簡易閃電形狀
        drawRect(x + 4, y, x + 10, y + 4, color);
        drawRect(x + 2, y + 4, x + 8, y + 8, color);
        drawRect(x, y + 8, x + 10, y + 12, color);
        drawRect(x + 4, y + 12, x + 10, y + 16, color);
        drawRect(x + 6, y + 16, x + 12, y + 20, color);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 標題
        String title = "§b⚡ 充能站";
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2 + 10, 5, COLOR_TEXT_TITLE);

        // 能量信息
        String energyText = formatEnergy(tile.getEnergyStored());
        fontRenderer.drawString("§3RF", 8, 73, COLOR_TEXT_INFO);
        fontRenderer.drawString(energyText, 25, 73, COLOR_TEXT_ENERGY);

        // 右側信息
        fontRenderer.drawString("§7放入物品", 120, 50, COLOR_TEXT_INFO);
        fontRenderer.drawString("§7或站上充電", 118, 60, COLOR_TEXT_INFO);
    }

    /**
     * 格式化能量顯示
     */
    private String formatEnergy(int energy) {
        if (energy >= 1000000000) {
            return String.format("%.2fG", energy / 1000000000.0);
        } else if (energy >= 1000000) {
            return String.format("%.2fM", energy / 1000000.0);
        } else if (energy >= 1000) {
            return String.format("%.1fk", energy / 1000.0);
        }
        return String.valueOf(energy);
    }

    /**
     * 繪製垂直線
     */
    public void drawVerticalLine(int x, int startY, int endY, int color) {
        drawRect(x, startY, x + 1, endY, color);
    }

    /**
     * 繪製水平線
     */
    public void drawHorizontalLine(int startX, int endX, int y, int color) {
        drawRect(startX, y, endX, y + 1, color);
    }
}
