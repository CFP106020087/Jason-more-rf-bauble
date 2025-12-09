package com.moremod.client.gui;

import com.moremod.container.ContainerMegaChest;
import com.moremod.tile.TileEntityMegaChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 超大容量箱子 GUI - 純代碼繪製版本
 */
@SideOnly(Side.CLIENT)
public class GuiMegaChest extends GuiContainer {

    private final TileEntityMegaChest tile;

    // 顏色常量
    private static final int COLOR_BG = 0xFFC6C6C6;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_BORDER_DARK = 0xFF373737;
    private static final int COLOR_BORDER_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_TITLE_BG = 0xFF8B4513;  // 木頭色標題

    public GuiMegaChest(InventoryPlayer playerInventory, TileEntityMegaChest tile) {
        super(new ContainerMegaChest(playerInventory, tile));
        this.tile = tile;

        // GUI 尺寸：寬度保持 176，高度根據 12 行計算
        this.xSize = 176;
        this.ySize = ContainerMegaChest.HOTBAR_START_Y + 18 + 7;  // 底部留 7 像素邊距
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

        // ===== 主背景 =====
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, COLOR_BG);

        // 3D邊框
        drawHorizontalLine(guiLeft, guiLeft + xSize - 1, guiTop, COLOR_BORDER_LIGHT);
        drawVerticalLine(guiLeft, guiTop, guiTop + ySize - 1, COLOR_BORDER_LIGHT);
        drawHorizontalLine(guiLeft, guiLeft + xSize - 1, guiTop + ySize - 1, COLOR_BORDER_DARK);
        drawVerticalLine(guiLeft + xSize - 1, guiTop, guiTop + ySize - 1, COLOR_BORDER_DARK);

        // ===== 標題欄背景 =====
        drawRect(guiLeft + 4, guiTop + 4, guiLeft + xSize - 4, guiTop + 14, COLOR_TITLE_BG);

        // ===== 箱子槽位 (12行 x 9列) =====
        for (int row = 0; row < ContainerMegaChest.CHEST_ROWS; row++) {
            for (int col = 0; col < ContainerMegaChest.CHEST_COLS; col++) {
                drawSlot(guiLeft + 8 + col * 18, guiTop + ContainerMegaChest.CHEST_START_Y + row * 18);
            }
        }

        // ===== 分隔線 =====
        int separatorY = guiTop + ContainerMegaChest.CHEST_START_Y + ContainerMegaChest.CHEST_ROWS * 18 + 3;
        drawHorizontalLine(guiLeft + 7, guiLeft + xSize - 8, separatorY, COLOR_BORDER_DARK);
        drawHorizontalLine(guiLeft + 7, guiLeft + xSize - 8, separatorY + 1, COLOR_BORDER_LIGHT);

        // ===== 玩家物品欄 (3行 x 9列) =====
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(guiLeft + 8 + col * 18, guiTop + ContainerMegaChest.PLAYER_INV_START_Y + row * 18);
            }
        }

        // ===== 快捷欄 (1行 x 9列) =====
        for (int col = 0; col < 9; col++) {
            drawSlot(guiLeft + 8 + col * 18, guiTop + ContainerMegaChest.HOTBAR_START_Y);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 標題（白色，居中）
        String title = "Mega Chest";
        int titleWidth = fontRenderer.getStringWidth(title);
        fontRenderer.drawString(title, (xSize - titleWidth) / 2, 5, 0xFFFFFF);

        // 物品欄標籤
        fontRenderer.drawString(I18n.format("container.inventory"),
                8, ContainerMegaChest.PLAYER_INV_START_Y - 11, 0x404040);

        // 容量提示
        String capacityText = "108 Slots";
        fontRenderer.drawString(capacityText, xSize - fontRenderer.getStringWidth(capacityText) - 8,
                ContainerMegaChest.PLAYER_INV_START_Y - 11, 0x404040);
    }

    private void drawSlot(int x, int y) {
        // 3D槽位效果
        drawRect(x - 1, y - 1, x + 17, y, COLOR_BORDER_DARK);
        drawRect(x - 1, y - 1, x, y + 17, COLOR_BORDER_DARK);
        drawRect(x + 16, y - 1, x + 17, y + 17, COLOR_BORDER_LIGHT);
        drawRect(x - 1, y + 16, x + 17, y + 17, COLOR_BORDER_LIGHT);
        drawRect(x, y, x + 16, y + 16, COLOR_SLOT);
    }
}
