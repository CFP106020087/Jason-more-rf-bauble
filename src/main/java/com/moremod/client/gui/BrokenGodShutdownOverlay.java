package com.moremod.client.gui;

import com.moremod.config.BrokenGodConfig;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.Color;
import java.util.Random;

/**
 * 破碎之神：死亡拒绝序列 Overlay (Proportional Split Version)
 * 采用动态比例分割时间轴，完美适配任意 Config 时长。
 */
@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class BrokenGodShutdownOverlay extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();
    private static final ResourceLocation VIGNETTE_TEX = new ResourceLocation("textures/misc/vignette.png");

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        EntityPlayer player = mc.player;
        if (player == null) return;

        IHumanityData data = HumanityCapabilityHandler.getData(player);
        // 确保 timer > 0
        if (data == null || !data.isInShutdown() || data.getShutdownTimer() <= 0) return;

        event.setCanceled(true);

        int currentTimer = data.getShutdownTimer();
        int maxTimer = BrokenGodConfig.shutdownTicks;

        // =======================================================
        // 核心逻辑：按比例分割时间轴 (Proportional Split)
        // =======================================================
        // 1. 系统重启 (Reboot): 35% 时间 -> 建立虚假的希望
        // 2. 电视关机 (CRT):    10% 时间 -> 瞬间的断层
        // 3. 虚空独白 (Void):   55% 时间 -> 漫长的冷漠
        // =======================================================

        int rebootDuration = (int)(maxTimer * 0.35f);
        int crtDuration    = (int)(maxTimer * 0.10f);
        // 剩下的全部给独白，确保总和 = maxTimer
        int textDuration   = maxTimer - rebootDuration - crtDuration;

        // 临界点计算 (倒计时逻辑)
        // Timer: max -> crtStart -> textStart -> 0
        int crtStartTime = textDuration + crtDuration;
        int textStartTime = textDuration;

        ScaledResolution res = event.getResolution();

        // 全局禁用深度测试 (防止 Z-fighting)
        GlStateManager.disableDepth();

        // --- 阶段分发 ---

        if (currentTimer > crtStartTime) {
            // [阶段 1: 系统重启]
            // 计算当前阶段的 normalized progress (0.0 -> 1.0)
            int timeSpentInPhase = maxTimer - currentTimer;
            float phaseProgress = (float)timeSpentInPhase / rebootDuration;

            renderCyberpunkReboot(res, currentTimer, Math.min(1.0f, phaseProgress));
        }
        else if (currentTimer > textStartTime) {
            // [阶段 2: CRT 关机]
            int timeSpentInPhase = crtStartTime - currentTimer;
            float animProgress = (float)timeSpentInPhase / crtDuration;

            renderCRTShutdown(res, Math.min(1.0f, animProgress));
        }
        else {
            // [阶段 3: 虚空独白]
            int timeSpentInPhase = textStartTime - currentTimer;
            float textProgress = (float)timeSpentInPhase / textDuration;

            renderVoidMessage(res, Math.min(1.0f, textProgress));
        }

        GlStateManager.enableDepth();
    }

    // ====================================================================================
    // 阶段 3: 虚空独白 (英文终端版 - 无机冷漠)
    // ====================================================================================
    private static void renderVoidMessage(ScaledResolution res, float progress) {
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();

        // 纯黑背景
        drawRect(0, 0, width, height, 0xFF000000);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();

        String line1 = "DEATH REQUEST    [ DENIED ]";
        String line2 = "ETERNAL REST     [ NOT FOUND ]";

        int centerX = width / 2;
        int centerY = height / 2;

        // 动效：打字机 (前 60% 时间打完字)
        float typeProgress = Math.min(1.0f, progress / 0.6f);

        int totalChars = line1.length() + line2.length();
        int charsToShow = (int)(totalChars * typeProgress);

        int line1Show = Math.min(line1.length(), charsToShow);
        int line2Show = Math.max(0, charsToShow - line1.length());

        String drawLine1 = line1.substring(0, line1Show);
        String drawLine2 = line2Show > 0 ? line2.substring(0, line2Show) : "";

        // 终端冷白
        int color = 0xFFFFFF;

        float scale = 1.5f;
        GlStateManager.scale(scale, scale, scale);

        float scaledCX = centerX / scale;
        float scaledCY = centerY / scale;

        float l1FullWidth = mc.fontRenderer.getStringWidth(line1);
        float l2FullWidth = mc.fontRenderer.getStringWidth(line2);

        mc.fontRenderer.drawString(drawLine1, scaledCX - l1FullWidth / 2.0f, scaledCY - 10, color, false);

        if (line2Show > 0) {
            mc.fontRenderer.drawString(drawLine2, scaledCX - l2FullWidth / 2.0f, scaledCY + 5, color, false);
        }

        // 闪烁光标
        boolean showCursor = (mc.player.ticksExisted / 10) % 2 == 0;
        if (showCursor) {
            String cursor = "█";
            float cursorX, cursorY;

            if (line2Show > 0) {
                float currentW = mc.fontRenderer.getStringWidth(drawLine2);
                cursorX = scaledCX - l2FullWidth / 2.0f + currentW + 2;
                cursorY = scaledCY + 5;
            } else {
                float currentW = mc.fontRenderer.getStringWidth(drawLine1);
                cursorX = scaledCX - l1FullWidth / 2.0f + currentW + 2;
                cursorY = scaledCY - 10;
            }
            mc.fontRenderer.drawString(cursor, cursorX, cursorY, 0xFFFFFFFF, false);
        }

        GlStateManager.scale(1/scale, 1/scale, 1/scale);
        GlStateManager.popMatrix();
    }

    // ====================================================================================
    // 阶段 2: CRT 关机效果 (Z-Fighting 修复版)
    // ====================================================================================
    private static void renderCRTShutdown(ScaledResolution res, float anim) {
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();

        // 铺底色
        drawRect(0, 0, width, height, 0xFF000000);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();

        float vScale = 1.0f;
        float hScale = 1.0f;

        // 0.0 -> 0.5: 压扁
        // 0.5 -> 1.0: 变窄
        if (anim < 0.5f) {
            float t = anim * 2f;
            vScale = 1.0f - (t * t); // Ease In
        } else {
            vScale = 0.005f;
            float t = (anim - 0.5f) * 2f;
            hScale = 1.0f - (t * t);
        }

        GlStateManager.translate(width / 2f, height / 2f, 0);
        GlStateManager.scale(hScale, vScale, 1.0f);
        GlStateManager.translate(-width / 2f, -height / 2f, 0);

        drawRect(0, 0, width, height, 0xFFFFFFFF);

        // 光晕增强
        if (vScale < 0.1f) {
            GlStateManager.scale(1.5f, 4.0f, 1.0f);
            drawRect(0, 0, width, height, 0x40FFFFFF);
        }

        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    // ====================================================================================
    // 阶段 1: 赛博朋克重启 (保持不变)
    // ====================================================================================
    private static void renderCyberpunkReboot(ScaledResolution res, int timer, float progress) {
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableAlpha();

        drawRect(0, 0, width, height, 0xFF050505);

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        mc.getTextureManager().bindTexture(VIGNETTE_TEX);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.SRC_COLOR);
        GlStateManager.color(0.4f, 0.4f, 0.4f, 1.0f);
        drawModalRectWithCustomSizedTexture(0, 0, 0, 0, width, height, width, height);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        GlStateManager.disableTexture2D();
        int scanAlpha = 30;
        for (int y = 0; y < height; y += 4) {
            drawRect(0, y, width, y + 1, (scanAlpha << 24) | 0x000000);
        }

        if (random.nextFloat() < 0.05f || progress < 0.1f) {
            int h = random.nextInt(height / 4) + 10;
            int y = random.nextInt(height - h);
            int glitchColor = (random.nextInt(100) + 50) << 24 | 0xFFFFFF;
            drawRect(0, y, width, y + h, glitchColor);
        }

        GlStateManager.enableTexture2D();
        renderSystemText(width, height, timer, progress);

        GlStateManager.enableAlpha();
        GlStateManager.popMatrix();
    }

    private static void renderSystemText(int width, int height, int timer, float progress) {
        int centerX = width / 2;
        int centerY = height / 2;
        int phase = (int) (progress * 4);

        float shake = 0f;
        if (progress < 0.2f) shake = 2.0f;
        else if (progress < 0.8f) shake = 0.5f;

        String title;
        int colorBase;

        if (phase == 0) { title = "CRITICAL FAILURE // SYSTEM HALTED"; colorBase = 0xFF0000; }
        else if (phase == 1) { title = "BIOS RECOVERY INITIATED..."; colorBase = 0xFF5500; }
        else if (phase == 2) { title = "RELOADING MODULES..."; colorBase = 0xFFFF00; }
        else { title = "SYSTEM RESTORED"; colorBase = 0x00FF00; }

        drawGlitchText(title, centerX, centerY - 50, colorBase, shake);

        int barWidth = 220;
        int barHeight = 6;
        int barX = centerX - barWidth / 2;
        int barY = centerY + 10;

        drawRect(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xFF111111);

        int fillWidth = (int) (barWidth * progress);
        int barColor = Color.HSBtoRGB(progress * 0.33f, 1.0f, 1.0f);
        barColor = (0xCC << 24) | (barColor & 0x00FFFFFF);

        drawRect(barX, barY, barX + fillWidth, barY + barHeight, barColor);

        String percent = String.format("%d%%", (int)(progress * 100));
        mc.fontRenderer.drawString(percent, barX + barWidth + 5, barY, 0xFFAAAAAA);

        if (progress < 0.95f) {
            float scale = 0.5f;
            GlStateManager.scale(scale, scale, scale);

            int lineY = (int) ((centerY + 30) / scale);
            int lineX = (int) ((centerX - 100) / scale);

            long seed = (System.currentTimeMillis() / 100);
            Random lineRand = new Random(seed);

            for (int i = 0; i < 5; i++) {
                String code = "0x" + Integer.toHexString(lineRand.nextInt()).toUpperCase();
                String msg = getLogMessage(phase, lineRand);
                mc.fontRenderer.drawString(code + " " + msg, lineX, lineY + (i * 10), 0xFF666666);
            }

            GlStateManager.scale(1/scale, 1/scale, 1/scale);
        }
    }

    private static void drawGlitchText(String text, int x, int y, int colorRGB, float shakeIntensity) {
        int w = mc.fontRenderer.getStringWidth(text);
        float xBase = x - w / 2.0f;

        float offsetX = (random.nextFloat() - 0.5f) * shakeIntensity;
        float offsetY = (random.nextFloat() - 0.5f) * shakeIntensity;

        if (shakeIntensity > 0.1f) {
            mc.fontRenderer.drawString(text, xBase + offsetX - 1, y + offsetY, (0x88 << 24) | 0xFF0000, false);
            mc.fontRenderer.drawString(text, xBase + offsetX + 1, y + offsetY, (0x88 << 24) | 0x0000FF, false);
        }
        mc.fontRenderer.drawString(text, xBase + offsetX, y + offsetY, (0xFF << 24) | colorRGB, false);
    }

    private static String getLogMessage(int phase, Random r) {
        String[] pool;
        if (phase == 0) pool = new String[]{"KERNEL_PANIC", "MEMORY_DUMP", "CONNECTION_LOST", "CRITICAL_ERR"};
        else if (phase == 1) pool = new String[]{"LOADING_LIB", "CHECKING_FS", "ALLOCATING_RAM", "MOUNTING_DRIVE"};
        else pool = new String[]{"SYNC_NEURAL", "CALIBRATING", "OPTIMIZING", "STARTING_DAEMON"};
        return pool[r.nextInt(pool.length)];
    }
}