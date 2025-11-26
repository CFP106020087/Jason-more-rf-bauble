package com.moremod.client.gui.component;

import com.moremod.client.gui.component.GuiRenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

/**
 * 槽位组件
 */
public class GuiSlot {

    private int x, y;
    private int size;
    private String label;

    public GuiSlot(int x, int y) {
        this(x, y, 18, null);
    }

    public GuiSlot(int x, int y, int size, String label) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.label = label;
    }

    public void draw() {
        GuiRenderUtils.drawSlot(x, y, size);

        // 绘制标签
        if (label != null && !label.isEmpty()) {
            Minecraft mc = Minecraft.getMinecraft();
            mc.fontRenderer.drawString(label, x, y - 10, GuiRenderUtils.COLOR_TEXT);
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}

// ============================================
// 4. GuiPanel.java - 面板组件
// 位置: com/moremod/client/gui/components/GuiPanel.java
// ============================================
