package com.moremod.client.gui.component;

import com.moremod.client.gui.component.GuiRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

/**
 * 能量条组件
 */
public class GuiEnergyBar {

    private int x, y, width, height;
    private int energy, maxEnergy;

    public GuiEnergyBar(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setEnergy(int energy, int maxEnergy) {
        this.energy = energy;
        this.maxEnergy = maxEnergy;
    }

    public void draw() {
        float percentage = maxEnergy > 0 ? (float) energy / maxEnergy : 0;
        GuiRenderUtils.drawEnergyBar(x, y, width, height, percentage);
    }

    public void drawWithText() {
        draw();

        // 绘制百分比文字
        float percentage = maxEnergy > 0 ? (float) energy / maxEnergy * 100 : 0;
        String text = String.format("%.0f%%", percentage);

        Minecraft mc = Minecraft.getMinecraft();
        int textWidth = mc.fontRenderer.getStringWidth(text);
        mc.fontRenderer.drawString(text, x + (width - textWidth) / 2, y + height + 2, 0xFFFFFFFF);
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public String getTooltip() {
        return String.format("%,d / %,d RF", energy, maxEnergy);
    }
}