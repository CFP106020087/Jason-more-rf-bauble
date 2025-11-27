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
 * 破碎之神：死亡拒绝序列 Overlay (Full Cycle Version)
 * 流程：
 * 1. Reboot (30%): 建立希望
 * 2. CRT Off (10%): 毁灭
 * 3. Void (50%): 判决 (Death Denied)
 * 4. CRT On (10%): 强制唤醒 (New!)
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
        if (data == null || !data.isInShutdown() || data.getShutdownTimer() <= 0) return;

        event.setCanceled(true);

        int currentTimer = data.getShutdownTimer();
        int maxTimer = BrokenGodConfig.shutdownTicks; 

        // =======================================================
        // 核心逻辑：四阶段比例分割 (4-Stage Proportional Split)
        // =======================================================
        
        int rebootDuration = (int)(maxTimer * 0.30f); // 30%
        int crtOffDuration = (int)(maxTimer * 0.10f); // 10%
        int crtOnDuration  = (int)(maxTimer * 0.10f); // 10% (新增：强制开机)
        
        // 剩下的 50% 给独白
        int textDuration   = maxTimer - rebootDuration - crtOffDuration - crtOnDuration; 

        // 临界点计算 (倒计时逻辑 max -> 0)
        // 1. Reboot 结束点
        int crtOffStartTime = textDuration + crtOnDuration + crtOffDuration; 
        // 2. CRT Off 结束点 (进入 Void)
        int textStartTime   = textDuration + crtOnDuration;
        // 3. Void 结束点 (进入 CRT On)
        int crtOnStartTime  = crtOnDuration;

        ScaledResolution res = event.getResolution();
        GlStateManager.disableDepth();

        // --- 阶段分发 ---

        if (currentTimer > crtOffStartTime) {
            // [阶段 1: 系统重启]
            int timeSpent = maxTimer - currentTimer;
            float progress = (float)timeSpent / rebootDuration;
            renderCyberpunkReboot(res, currentTimer, Math.min(1.0f, progress));
        } 
        else if (currentTimer > textStartTime) {
            // [阶段 2: CRT 关机]
            int timeSpent = crtOffStartTime - currentTimer;
            float progress = (float)timeSpent / crtOffDuration;
            renderCRTShutdown(res, Math.min(1.0f, progress));
        } 
        else if (currentTimer > crtOnStartTime) {
            // [阶段 3: 虚空独白]
            int timeSpent = textStartTime - currentTimer;
            float progress = (float)timeSpent / textDuration;
            renderVoidMessage(res, Math.min(1.0f, progress));
        }
        else {
            // [阶段 4: CRT 开机 - 强制唤醒]
            // currentTimer: crtOnStartTime -> 0
            // progress: 0.0 -> 1.0
            int timeSpent = crtOnStartTime - currentTimer;
            float progress = (float)timeSpent / crtOnDuration;
            renderCRTTurnOn(res, Math.min(1.0f, progress));
        }
        
        GlStateManager.enableDepth();
    }

    // ====================================================================================
    // 阶段 4: CRT 开机效果 (强制唤醒)
    // 原理：画一个黑色的遮罩，中间挖一个洞，洞从小变大，露出背后的游戏画面
    // ====================================================================================
    private static void renderCRTTurnOn(ScaledResolution res, float anim) {
        int w = res.getScaledWidth();
        int h = res.getScaledHeight();
        
        // 不需要 drawRect(全屏黑)，因为我们要露出游戏画面

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        
        // 动画逻辑：
        // 0.0 -> 0.3: 横向展开 (一条白线迅速变宽)
        // 0.3 -> 1.0: 纵向展开 (屏幕拉开)
        
        float widthFactor;
        float heightFactor;
        
        if (anim < 0.3f) {
            // 横向展开阶段
            float t = anim / 0.3f;
            widthFactor = t * t; // Ease In
            heightFactor = 0.005f; // 保持一条细线
        } else {
            // 纵向展开阶段
            widthFactor = 1.0f;
            float t = (anim - 0.3f) / 0.7f;
            heightFactor = 0.005f + (t * t) * 0.995f; // 从细线变全屏
        }
        
        // 计算"洞"的大小
        int centerX = w / 2;
        int centerY = h / 2;
        int halfW = (int)((w / 2) * widthFactor);
        int halfH = (int)((h / 2) * heightFactor);
        
        // 绘制四周的黑色遮罩 (Mask)
        int blackColor = 0xFF000000;
        
        // 上半部分黑块
        drawRect(0, 0, w, centerY - halfH, blackColor);
        // 下半部分黑块
        drawRect(0, centerY + halfH, w, h, blackColor);
        // 左半部分黑块
        drawRect(0, centerY - halfH, centerX - halfW, centerY + halfH, blackColor);
        // 右半部分黑块
        drawRect(centerX + halfW, centerY - halfH, w, centerY + halfH, blackColor);
        
        // 绘制边缘的白色光晕/扫描线 (Flash)
        // 只有当屏幕还没完全打开时才显示
        if (anim < 0.95f) {
            int flashColor = 0xFFFFFFFF;
            // 上边缘线
            drawRect(centerX - halfW, centerY - halfH - 1, centerX + halfW, centerY - halfH, flashColor);
            // 下边缘线
            drawRect(centerX - halfW, centerY + halfH, centerX + halfW, centerY + halfH + 1, flashColor);
            
            // 如果还在横向展开阶段，两头也发光
            if (anim < 0.3f) {
                 drawRect(centerX - halfW - 2, centerY - halfH, centerX - halfW, centerY + halfH, flashColor);
                 drawRect(centerX + halfW, centerY - halfH, centerX + halfW + 2, centerY + halfH, flashColor);
            }
        }
        
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    // ====================================================================================
    // 阶段 3: 虚空独白 (英文终端版)
    // ====================================================================================
    private static void renderVoidMessage(ScaledResolution res, float progress) {
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();

        // 纯黑背景 (必须画，否则会透出游戏)
        drawRect(0, 0, width, height, 0xFF000000);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        
        String line1 = "DEATH REQUEST    [ DENIED ]";
        String line2 = "ETERNAL REST     [ NOT FOUND ]";
        
        int centerX = width / 2;
        int centerY = height / 2;
        
        // 打字机 (前 60% 时间打完)
        float typeProgress = Math.min(1.0f, progress / 0.6f); 
        
        int totalChars = line1.length() + line2.length();
        int charsToShow = (int)(totalChars * typeProgress);
        
        int line1Show = Math.min(line1.length(), charsToShow);
        int line2Show = Math.max(0, charsToShow - line1.length());
        
        String drawLine1 = line1.substring(0, line1Show);
        String drawLine2 = line2Show > 0 ? line2.substring(0, line2Show) : "";
        
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
    // 阶段 2: CRT 关机效果
    // ====================================================================================
    private static void renderCRTShutdown(ScaledResolution res, float anim) {
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();
        
        drawRect(0, 0, width, height, 0xFF000000);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D(); 
        
        float vScale = 1.0f;
        float hScale = 1.0f;
        
        if (anim < 0.5f) {
            float t = anim * 2f; 
            vScale = 1.0f - (t * t); 
        } else {
            vScale = 0.005f; 
            float t = (anim - 0.5f) * 2f;
            hScale = 1.0f - (t * t);
        }
        
        GlStateManager.translate(width / 2f, height / 2f, 0);
        GlStateManager.scale(hScale, vScale, 1.0f);
        GlStateManager.translate(-width / 2f, -height / 2f, 0);
        
        drawRect(0, 0, width, height, 0xFFFFFFFF);
        
        if (vScale < 0.1f) {
            GlStateManager.scale(1.5f, 4.0f, 1.0f);
            drawRect(0, 0, width, height, 0x40FFFFFF);
        }
        
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    // ====================================================================================
    // 阶段 1: 赛博朋克重启
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