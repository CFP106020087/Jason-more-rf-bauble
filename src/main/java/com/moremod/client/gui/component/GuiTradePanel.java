package com.moremod.client.gui.component;

import net.minecraft.client.gui.Gui;

public class GuiTradePanel extends Gui {

    private int x, y, width, height;

    public GuiTradePanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void draw() {
        // 面板背景
        drawRect(x, y, x + width, y + height, 0xFF505050);
        // 邊框
        drawRect(x, y, x + width, y + 1, 0xFF1E1E1E);
        drawRect(x, y + height - 1, x + width, y + height, 0xFF1E1E1E);
        drawRect(x, y, x + 1, y + height, 0xFF1E1E1E);
        drawRect(x + width - 1, y, x + width, y + height, 0xFF1E1E1E);
    }
}