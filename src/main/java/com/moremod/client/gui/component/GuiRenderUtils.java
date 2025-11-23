package com.moremod.client.gui.component;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * GUI 渲染工具类 - 工业先锋风格
 */
public class GuiRenderUtils extends Gui {

    // 颜色主题
    public static final int COLOR_BG = 0xFF3F3F3F;
    public static final int COLOR_PANEL = 0xFF565656;
    public static final int COLOR_DARK = 0xFF2A2A2A;
    public static final int COLOR_DARKER = 0xFF1A1A1A;
    public static final int COLOR_BORDER = 0xFF1A1A1A;
    public static final int COLOR_TEXT = 0xFFFFFFFF;
    public static final int COLOR_TEXT_GRAY = 0xFFAAAAAA;
    public static final int COLOR_GREEN = 0xFF4CAF50;
    public static final int COLOR_RED = 0xFFF44336;
    public static final int COLOR_ORANGE = 0xFFFF9800;

    /**
     * 绘制面板
     */
    public static void drawPanel(int x, int y, int width, int height) {
        drawRect(x, y, x + width, y + height, COLOR_PANEL);
        drawHollowRect(x, y, width, height, COLOR_BORDER);
    }

    /**
     * 绘制槽位
     */
    public static void drawSlot(int x, int y, int size) {
        drawRect(x - 1, y - 1, x + size - 1, y + size - 1, COLOR_DARKER);
        drawHollowRect(x - 1, y - 1, size, size, COLOR_BORDER);
    }

    /**
     * 绘制空心矩形
     */
    public static void drawHollowRect(int x, int y, int width, int height, int color) {
        drawRect(x, y, x + width, y + 1, color);
        drawRect(x, y + height - 1, x + width, y + height, color);
        drawRect(x, y, x + 1, y + height, color);
        drawRect(x + width - 1, y, x + width, y + height, color);
    }

    /**
     * 绘制边框
     */
    public static void drawBorder(int x, int y, int width, int height) {
        drawRect(x, y, x + width, y + 2, COLOR_BORDER);
        drawRect(x, y + height - 2, x + width, y + height, COLOR_BORDER);
        drawRect(x, y, x + 2, y + height, COLOR_BORDER);
        drawRect(x + width - 2, y, x + width, y + height, COLOR_BORDER);
    }

    /**
     * 绘制能量条
     */
    public static void drawEnergyBar(int x, int y, int width, int height, float percentage) {
        // 背景
        drawRect(x, y, x + width, y + height, COLOR_DARKER);

        // 能量填充
        int fillHeight = (int) ((height - 2) * percentage);
        if (fillHeight > 0) {
            int color = getEnergyColor(percentage);
            drawGradientRectVertical(
                    x + 1,
                    y + height - 1 - fillHeight,
                    x + width - 1,
                    y + height - 1,
                    color,
                    mixColor(color, 0xFF000000, 0.5f)
            );
        }

        // 边框
        drawHollowRect(x, y, width, height, COLOR_BORDER);
    }

    /**
     * 垂直渐变矩形
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
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
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
     * 获取能量颜色
     */
    public static int getEnergyColor(float percentage) {
        if (percentage < 0.25f) return COLOR_RED;
        if (percentage < 0.75f) return COLOR_ORANGE;
        return COLOR_GREEN;
    }

    /**
     * 混合颜色
     */
    public static int mixColor(int c1, int c2, float ratio) {
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int r = (int) (r1 * (1 - ratio) + r2 * ratio);
        int g = (int) (g1 * (1 - ratio) + g2 * ratio);
        int b = (int) (b1 * (1 - ratio) + b2 * ratio);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
