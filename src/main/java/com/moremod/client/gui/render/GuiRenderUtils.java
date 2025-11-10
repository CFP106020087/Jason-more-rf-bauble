// ============================================
// 進階 GUI 繪製工具類
// ============================================
package com.moremod.client.gui.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * GUI 渲染工具類 - 模仿工業先鋒風格
 */
public class GuiRenderUtils extends Gui {

    // === 顏色主題配置 ===
    public static class Theme {
        // 工業先鋒深色主題
        public static final int BG_MAIN = 0xFF3F3F3F;
        public static final int BG_PANEL = 0xFF565656;
        public static final int BG_DARK = 0xFF2A2A2A;
        public static final int BG_DARKER = 0xFF1A1A1A;

        public static final int BORDER_MAIN = 0xFF1A1A1A;
        public static final int BORDER_LIGHT = 0xFF707070;
        public static final int BORDER_HIGHLIGHT = 0xFF8B8B8B;

        public static final int TEXT_MAIN = 0xFFFFFFFF;
        public static final int TEXT_DARK = 0xFFAAAAAA;
        public static final int TEXT_DISABLED = 0xFF666666;
        public static final int TEXT_HIGHLIGHT = 0xFF4FC3F7;

        public static final int ACCENT_BLUE = 0xFF2196F3;
        public static final int ACCENT_GREEN = 0xFF4CAF50;
        public static final int ACCENT_ORANGE = 0xFFFF9800;
        public static final int ACCENT_RED = 0xFFF44336;
    }

    /**
     * 繪製帶陰影的面板
     */
    public static void drawPanel(int x, int y, int width, int height) {
        // 外陰影
        drawGradientRectVertical(x - 2, y - 2, x, y + height + 2,
                0x33000000, 0x00000000);
        drawGradientRectVertical(x + width, y - 2, x + width + 2, y + height + 2,
                0x33000000, 0x00000000);

        // 主體
        drawRect(x, y, x + width, y + height, Theme.BG_PANEL);

        // 高光（頂部）
        drawRect(x, y, x + width, y + 1, Theme.BORDER_LIGHT);

        // 陰影（底部）
        drawRect(x, y + height - 1, x + width, y + height, Theme.BORDER_MAIN);

        // 邊框
        drawHollowRect(x, y, width, height, Theme.BORDER_MAIN, 1);
    }

    /**
     * 繪製凹陷效果的槽位
     */
    public static void drawSlot(int x, int y, int size) {
        // 外邊框（凸起）
        drawRect(x - 1, y - 1, x + size, y, Theme.BORDER_MAIN); // 上
        drawRect(x - 1, y - 1, x, y + size, Theme.BORDER_MAIN); // 左
        drawRect(x + size - 1, y, x + size, y + size, Theme.BORDER_LIGHT); // 右
        drawRect(x, y + size - 1, x + size, y + size, Theme.BORDER_LIGHT); // 下

        // 內部（凹陷）
        drawRect(x, y, x + size - 1, y + 1, Theme.BG_DARKER); // 上內
        drawRect(x, y, x + 1, y + size - 1, Theme.BG_DARKER); // 左內

        // 槽位背景
        drawRect(x + 1, y + 1, x + size - 1, y + size - 1, Theme.BG_DARK);
    }

    /**
     * 繪製能量條（帶動畫效果）
     */
    public static void drawEnergyBar(int x, int y, int width, int height,
                                     float percentage, boolean animated) {
        // 背景槽
        drawRect(x, y, x + width, y + height, Theme.BG_DARKER);

        // 內陰影
        drawRect(x + 1, y + 1, x + 2, y + height - 1, 0x88000000);
        drawRect(x + 1, y + 1, x + width - 1, y + 2, 0x88000000);

        // 計算填充高度
        int fillHeight = (int) ((height - 4) * percentage);

        if (fillHeight > 0) {
            // 根據百分比選擇顏色
            int color = getEnergyColor(percentage);

            // 能量填充（底部到頂部）
            int yStart = y + height - 2 - fillHeight;
            int yEnd = y + height - 2;

            // 主體漸變
            drawGradientRectVertical(x + 2, yStart, x + width - 2, yEnd,
                    lightenColor(color, 0.3f), color);

            // 動畫閃光效果
            if (animated && percentage > 0.1f) {
                long time = System.currentTimeMillis();
                float pulse = (float) Math.sin(time / 200.0) * 0.3f + 0.7f;
                int highlightColor = multiplyAlpha(lightenColor(color, 0.5f), pulse);

                // 頂部高光
                drawRect(x + 2, yStart, x + width - 2, yStart + 2, highlightColor);
            }

            // 能量邊緣發光
            drawRect(x + 1, yStart, x + 2, yEnd, multiplyAlpha(color, 0.5f));
            drawRect(x + width - 2, yStart, x + width - 1, yEnd, multiplyAlpha(color, 0.5f));
        }

        // 外邊框
        drawHollowRect(x, y, width, height, Theme.BORDER_MAIN, 1);

        // 刻度線
        for (int i = 1; i < 4; i++) {
            int lineY = y + (height * i / 4);
            drawRect(x + 1, lineY, x + width - 1, lineY + 1, Theme.BORDER_MAIN);
        }
    }

    /**
     * 繪製進度條
     */
    public static void drawProgressBar(int x, int y, int width, int height,
                                       float percentage, int color) {
        // 背景
        drawRect(x, y, x + width, y + height, Theme.BG_DARKER);

        // 進度填充
        int fillWidth = (int) ((width - 4) * percentage);
        if (fillWidth > 0) {
            drawGradientRectHorizontal(x + 2, y + 2, x + 2 + fillWidth, y + height - 2,
                    color, lightenColor(color, 0.3f));
        }

        // 邊框
        drawHollowRect(x, y, width, height, Theme.BORDER_MAIN, 1);
    }

    /**
     * 繪製按鈕（3D效果）
     */
    public static void drawButton(int x, int y, int width, int height,
                                  String text, boolean hovered, boolean pressed) {
        int offset = pressed ? 1 : 0;

        // 按鈕主體
        if (pressed) {
            drawRect(x, y, x + width, y + height, Theme.BG_DARK);
        } else {
            drawGradientRectVertical(x, y, x + width, y + height,
                    lightenColor(Theme.BG_PANEL, 0.2f), Theme.BG_PANEL);
        }

        // 高光和陰影
        if (!pressed) {
            drawRect(x, y, x + width, y + 1, Theme.BORDER_LIGHT); // 上高光
            drawRect(x, y, x + 1, y + height, Theme.BORDER_LIGHT); // 左高光
            drawRect(x, y + height - 1, x + width, y + height, Theme.BORDER_MAIN); // 下陰影
            drawRect(x + width - 1, y, x + width, y + height, Theme.BORDER_MAIN); // 右陰影
        }

        // 懸停效果
        if (hovered && !pressed) {
            drawRect(x + 1, y + 1, x + width - 1, y + height - 1,
                    multiplyAlpha(Theme.ACCENT_BLUE, 0.2f));
        }

        // 文字
        Minecraft mc = Minecraft.getMinecraft();
        int textColor = hovered ? Theme.TEXT_HIGHLIGHT : Theme.TEXT_MAIN;
        int textX = x + (width - mc.fontRenderer.getStringWidth(text)) / 2 + offset;
        int textY = y + (height - 8) / 2 + offset;
        mc.fontRenderer.drawString(text, textX, textY, textColor);

        // 外邊框
        drawHollowRect(x, y, width, height, Theme.BORDER_MAIN, 1);
    }

    /**
     * 繪製箭頭圖標
     */
    public static void drawArrow(int x, int y, int size, Direction direction, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);

        switch (direction) {
            case UP:
                drawTriangle(size / 2, 0, size, size, 0, size, color);
                break;
            case DOWN:
                drawTriangle(0, 0, size, 0, size / 2, size, color);
                break;
            case LEFT:
                drawTriangle(size, 0, size, size, 0, size / 2, color);
                break;
            case RIGHT:
                drawTriangle(0, 0, 0, size, size, size / 2, color);
                break;
        }

        GlStateManager.popMatrix();
    }

    /**
     * 繪製三角形
     */
    private static void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, 0).color(r, g, b, a).endVertex();
        buffer.pos(x3, y3, 0).color(r, g, b, a).endVertex();
        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    /**
     * 繪製圓角矩形
     */
    public static void drawRoundedRect(int x, int y, int width, int height, int radius, int color) {
        // 中心矩形
        drawRect(x + radius, y, x + width - radius, y + height, color);
        drawRect(x, y + radius, x + radius, y + height - radius, color);
        drawRect(x + width - radius, y + radius, x + width, y + height - radius, color);

        // 四個角（簡化為小矩形，實際應該是圓弧）
        drawRect(x, y, x + radius, y + radius, color);
        drawRect(x + width - radius, y, x + width, y + radius, color);
        drawRect(x, y + height - radius, x + radius, y + height, color);
        drawRect(x + width - radius, y + height - radius, x + width, y + height, color);
    }

    /**
     * 繪製空心矩形
     */
    public static void drawHollowRect(int x, int y, int width, int height, int color, int thickness) {
        drawRect(x, y, x + width, y + thickness, color); // 上
        drawRect(x, y + height - thickness, x + width, y + height, color); // 下
        drawRect(x, y, x + thickness, y + height, color); // 左
        drawRect(x + width - thickness, y, x + width, y + height, color); // 右
    }

    /**
     * 繪製垂直漸變矩形
     */
    public static void drawGradientRectVertical(int left, int top, int right, int bottom,
                                                int startColor, int endColor) {
        float sa = (startColor >> 24 & 255) / 255.0F;
        float sr = (startColor >> 16 & 255) / 255.0F;
        float sg = (startColor >> 8 & 255) / 255.0F;
        float sb = (startColor & 255) / 255.0F;

        float ea = (endColor >> 24 & 255) / 255.0F;
        float er = (endColor >> 16 & 255) / 255.0F;
        float eg = (endColor >> 8 & 255) / 255.0F;
        float eb = (endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        buffer.pos(right, top, 0).color(sr, sg, sb, sa).endVertex();
        buffer.pos(left, top, 0).color(sr, sg, sb, sa).endVertex();
        buffer.pos(left, bottom, 0).color(er, eg, eb, ea).endVertex();
        buffer.pos(right, bottom, 0).color(er, eg, eb, ea).endVertex();

        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    /**
     * 繪製水平漸變矩形
     */
    public static void drawGradientRectHorizontal(int left, int top, int right, int bottom,
                                                  int startColor, int endColor) {
        float sa = (startColor >> 24 & 255) / 255.0F;
        float sr = (startColor >> 16 & 255) / 255.0F;
        float sg = (startColor >> 8 & 255) / 255.0F;
        float sb = (startColor & 255) / 255.0F;

        float ea = (endColor >> 24 & 255) / 255.0F;
        float er = (endColor >> 16 & 255) / 255.0F;
        float eg = (endColor >> 8 & 255) / 255.0F;
        float eb = (endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        buffer.pos(right, top, 0).color(er, eg, eb, ea).endVertex();
        buffer.pos(left, top, 0).color(sr, sg, sb, sa).endVertex();
        buffer.pos(left, bottom, 0).color(sr, sg, sb, sa).endVertex();
        buffer.pos(right, bottom, 0).color(er, eg, eb, ea).endVertex();

        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    // === 顏色工具方法 ===

    public static int getEnergyColor(float percentage) {
        if (percentage < 0.25f) return Theme.ACCENT_RED;
        if (percentage < 0.50f) return Theme.ACCENT_ORANGE;
        if (percentage < 0.75f) return 0xFFFFEB3B; // 黃色
        return Theme.ACCENT_GREEN;
    }

    public static int lightenColor(int color, float amount) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = Math.min(255, (int) (r + (255 - r) * amount));
        g = Math.min(255, (int) (g + (255 - g) * amount));
        b = Math.min(255, (int) (b + (255 - b) * amount));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int darkenColor(int color, float amount) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = (int) (r * (1 - amount));
        g = (int) (g * (1 - amount));
        b = (int) (b * (1 - amount));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int multiplyAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
}