package com.moremod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.LinkedList;
import java.util.List;

/**
 * EventHUDOverlay - 右中間浮動事件提示 HUD
 *
 * 呼叫方式：
 *   EventHUDOverlay.push("⚠ 排異 +2.3%");
 */
@SideOnly(Side.CLIENT)
public class EventHUDOverlay extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();

    /** 單條訊息資料 */
    private static class Message {
        String text;
        long startTime;
        long duration = 1500; // 顯示 1.5 秒

        Message(String text) {
            this.text = text;
            this.startTime = Minecraft.getSystemTime();
        }

        float getLifeRatio() {
            long now = Minecraft.getSystemTime();
            return (float)(now - startTime) / duration;
        }

        boolean isExpired() {
            return getLifeRatio() >= 1.0f;
        }
    }

    /** 訊息佇列 */
    private static final List<Message> messages = new LinkedList<>();

    /** 對外：新增浮動訊息 */
    public static void push(String text) {
        if (text == null || text.isEmpty()) return;
        messages.add(new Message(text));
        if (messages.size() > 5) messages.remove(0);
    }

    @SubscribeEvent
    public void onRenderHUD(RenderGameOverlayEvent.Post event) {
        // 只在TEXT阶段渲染，避免重复绘制
        if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.player == null || mc.world == null) return;
        if (messages.isEmpty()) return;

        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRenderer;

        int screenW = sr.getScaledWidth();
        int screenH = sr.getScaledHeight();

        int baseX;
        int baseY;

        // ========= 位置改成「右中間」 =========
        // 基準 Y：螢幕中央稍微往上
        baseY = screenH / 2 - 5;

        // 基準 X：畫面右側偏左一點，就在你 HUD 右側邊緣附近
        // 文字會以其寬度往左延伸
        // 讓整體感覺剛好「靠右但不是右下角」
        baseX = screenW - 10; // 右邊距離 10px
        // ====================================

        int index = 0;
        List<Message> expired = new LinkedList<>();

        for (Message msg : messages) {

            float t = msg.getLifeRatio();
            if (t >= 1f) {
                expired.add(msg);
                continue;
            }

            float alpha = (t < 0.8f) ? 1.0f : 1.0f - (t - 0.8f) / 0.2f;
            if (alpha < 0f) alpha = 0f;

            // 向上飄移（垂直 12px）
            int y = baseY - index * 12 - (int)(t * 10);

            // 計算文字寬度
            int textWidth = fr.getStringWidth(msg.text);

            // 真正繪製位置：右側對齊
            int x = baseX - textWidth;

            int bgColor = (int)(alpha * 120) << 24;
            int textColor = 0xFFFFFF | ((int)(alpha * 255) << 24);

            // 背景
            drawRect(x - 3, y - 2, x + textWidth + 3, y + 10, bgColor);

            // 文字
            fr.drawStringWithShadow(msg.text, x, y, textColor);

            index++;
        }

        messages.removeAll(expired);
    }
}
