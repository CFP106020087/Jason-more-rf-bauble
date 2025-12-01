package com.moremod.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.LinkedList;
import java.util.Random;

@SideOnly(Side.CLIENT)
public class BiosLogRenderer {

    private static final LinkedList<LogEntry> logs = new LinkedList<>();
    private static final int MAX_LOGS = 10; // 常駐顯示 10 行
    private static final Random rand = new Random();

    // 正常詞庫 (綠色)
    private static final String[] SYS_PREFIX = {"SYS", "MEM", "CPU", "NET", "I/O", "GPU", "LOG"};
    private static final String[] SYS_MSG = {"OPTIMIZING", "SYNC_OK", "PING_ACK", "TRACING", "BUFFER_FLUSH", "IDLE_CHECK", "REFRESH"};

    // 警告詞庫 (黃色/紅色)
    private static final String[] ERR_PREFIX = {"WARN", "CRIT", "FAIL", "ERR", "ALERT", "DMG"};
    private static final String[] ERR_MSG = {"CORRUPTION DETECTED", "PACKET LOSS", "INTEGRITY FAIL", "OVERHEAT", "IMPACT DETECTED", "SHIELD BREACH"};

    private static int updateTicker = 0;

    // 內部類：保存單條日誌的顏色和內容
    private static class LogEntry {
        String text;
        int color; // 0=Green, 1=Yellow, 2=Red
        LogEntry(String text, int color) {
            this.text = text;
            this.color = color;
        }
    }

    /**
     * 外部調用：瞬間爆發錯誤信息
     * @param intensity 爆發數量 (受傷越重，刷得越多)
     */
    public static void triggerDamageBurst(int intensity) {
        for (int i = 0; i < intensity; i++) {
            boolean isCritical = rand.nextBoolean();
            String prefix = ERR_PREFIX[rand.nextInt(ERR_PREFIX.length)];
            String msg = ERR_MSG[rand.nextInt(ERR_MSG.length)];
            String log = String.format("[%s] %s (%d ms)", prefix, msg, rand.nextInt(50));

            // 隨機插入紅色(嚴重)或黃色(警告)
            addLog(log, isCritical ? 2 : 1);
        }
    }

    public static void render(float time) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRenderer;

        // --- 1. 平時自動更新 (綠色流水帳) ---
        updateTicker++;
        // 每 10~30 tick 刷一條正常的，保持動態感但不要太煩
        if (updateTicker > rand.nextInt(20) + 10) {
            addNormalLog();
            updateTicker = 0;
        }

        // --- 2. 渲染邏輯 ---
        GlStateManager.pushMatrix();

        // 字體縮放 (0.5)
        float scale = 0.5f;
        GlStateManager.scale(scale, scale, 1.0f);

        int startX = 20; // 左邊距
        int startY = 20; // 上邊距
        int lineHeight = 10;

        for (int i = 0; i < logs.size(); i++) {
            LogEntry entry = logs.get(i);

            // 越新的日誌越亮，舊的變暗
            int alpha = 255 - (logs.size() - 1 - i) * 15;
            if (alpha < 80) alpha = 80; // 最低亮度

            int color;
            switch (entry.color) {
                case 2: // Red (Critical)
                    color = (alpha << 24) | 0xFF5555;
                    break;
                case 1: // Yellow (Warning)
                    color = (alpha << 24) | 0xFFFF55;
                    break;
                case 0: // Green (Normal) - 駭客綠
                default:
                    color = (alpha << 24) | 0x55FF55;
                    break;
            }

            fr.drawStringWithShadow(entry.text, startX, startY + i * lineHeight, color);
        }

        GlStateManager.popMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void addNormalLog() {
        String prefix = SYS_PREFIX[rand.nextInt(SYS_PREFIX.length)];
        String action = SYS_MSG[rand.nextInt(SYS_MSG.length)];
        String hex = Integer.toHexString(rand.nextInt(0xFFFFFF)).toUpperCase();
        String log = String.format("[%s] %s ADDR:0x%s", prefix, action, hex);
        addLog(log, 0);
    }

    private static void addLog(String text, int colorType) {
        logs.add(new LogEntry(text, colorType));
        if (logs.size() > MAX_LOGS) {
            logs.removeFirst();
        }
    }
}