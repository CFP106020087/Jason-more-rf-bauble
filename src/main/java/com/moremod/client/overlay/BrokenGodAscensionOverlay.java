package com.moremod.client.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * 破碎之神升格动画 Overlay
 * Broken God Ascension Animation Overlay
 *
 * 当玩家升格为破碎之神时播放的视觉效果
 * 主题：人性消亡、机械侵蚀、自我消亡、空洞觉醒
 *
 * 阶段1: 人性消亡 (0-40 tick) - 色彩抽离、锈蚀边缘
 * 阶段2: 机械侵蚀 (40-80 tick) - 静电噪点、Glitch效果
 * 阶段3: 自我消亡 (80-120 tick) - 黑屏、齿轮符号
 * 阶段4: 空洞觉醒 (120-160 tick) - 文字显示、渐隐
 */
@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class BrokenGodAscensionOverlay extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();
    private static final ResourceLocation VIGNETTE_TEX = new ResourceLocation("textures/misc/vignette.png");

    // 动画状态
    private static boolean isAnimating = false;
    private static int animationTick = 0;
    private static final int TOTAL_DURATION = 160; // 8秒

    // 阶段时间点
    private static final int PHASE1_END = 40;   // 人性消亡
    private static final int PHASE2_END = 80;   // 机械侵蚀
    private static final int PHASE3_END = 120;  // 自我消亡
    private static final int PHASE4_END = 160;  // 空洞觉醒

    /**
     * 开始播放升格动画
     */
    public static void startAnimation() {
        isAnimating = true;
        animationTick = 0;
    }

    /**
     * 检查动画是否正在播放
     */
    public static boolean isAnimating() {
        return isAnimating;
    }

    /**
     * 客户端 tick 事件 - 用于递增动画计时器
     * 这确保动画按照游戏 tick (20/秒) 而非帧率运行
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!isAnimating) return;
        if (event.phase != TickEvent.Phase.END) return;
        // 暂停时不递增
        if (mc.isGamePaused()) return;

        animationTick++;
        if (animationTick > TOTAL_DURATION) {
            isAnimating = false;
            animationTick = 0;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        if (!isAnimating) return;
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        // 动画已结束时不渲染
        if (animationTick > TOTAL_DURATION) {
            return;
        }

        ScaledResolution res = event.getResolution();
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();

        GlStateManager.disableDepth();
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();

        // 根据当前 tick 选择阶段
        if (animationTick <= PHASE1_END) {
            // 阶段1: 人性消亡
            float progress = (float) animationTick / PHASE1_END;
            renderPhase1_HumanityFading(res, progress);
        } else if (animationTick <= PHASE2_END) {
            // 阶段2: 机械侵蚀
            float progress = (float) (animationTick - PHASE1_END) / (PHASE2_END - PHASE1_END);
            renderPhase2_MechanicalCorrosion(res, progress);
        } else if (animationTick <= PHASE3_END) {
            // 阶段3: 自我消亡
            float progress = (float) (animationTick - PHASE2_END) / (PHASE3_END - PHASE2_END);
            renderPhase3_SelfDissolution(res, progress);
        } else {
            // 阶段4: 空洞觉醒
            float progress = (float) (animationTick - PHASE3_END) / (PHASE4_END - PHASE3_END);
            renderPhase4_HollowAwakening(res, progress);
        }

        GlStateManager.popMatrix();
        GlStateManager.enableDepth();
    }

    // ==================== 阶段1: 人性消亡 ====================
    // 屏幕逐渐失去色彩，边缘出现锈蚀
    private static void renderPhase1_HumanityFading(ScaledResolution res, float progress) {
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();

        // 渐进式灰色叠加层（模拟去饱和）
        int grayAlpha = (int) (progress * 180);
        int grayColor = (grayAlpha << 24) | 0x1A1A1A;
        drawRect(0, 0, width, height, grayColor);

        // 边缘锈蚀效果（从四周向内蔓延的暗红色）
        float rustIntensity = progress * 0.6f;
        renderRustVignette(width, height, rustIntensity);

        // 轻微屏幕震动
        if (progress > 0.5f && random.nextFloat() < 0.1f) {
            float shake = (random.nextFloat() - 0.5f) * 2;
            GlStateManager.translate(shake, shake, 0);
        }

        // 提示文字（淡入）
        if (progress > 0.7f) {
            float textAlpha = (progress - 0.7f) / 0.3f;
            int alpha = (int) (textAlpha * 150);
            int textColor = (alpha << 24) | 0x8B4513; // 锈红色
            String text = "...";
            int textWidth = mc.fontRenderer.getStringWidth(text);
            mc.fontRenderer.drawString(text, width / 2 - textWidth / 2, height / 2, textColor);
        }
    }

    // ==================== 阶段2: 机械侵蚀 ====================
    // 静电噪点、Glitch效果、画面撕裂
    private static void renderPhase2_MechanicalCorrosion(ScaledResolution res, float progress) {
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();

        // 深灰背景
        drawRect(0, 0, width, height, 0xFF0A0A0A);

        // 静电雪花效果
        renderStaticNoise(width, height, progress);

        // 水平撕裂线（Glitch）
        if (random.nextFloat() < 0.3f + progress * 0.4f) {
            int tearY = random.nextInt(height);
            int tearHeight = random.nextInt(20) + 5;
            int tearOffset = random.nextInt(30) - 15;

            // 撕裂区域偏移
            GlStateManager.pushMatrix();
            GlStateManager.translate(tearOffset, 0, 0);
            int tearColor = random.nextBoolean() ? 0x50FFFFFF : 0x30FF0000;
            drawRect(0, tearY, width, tearY + tearHeight, tearColor);
            GlStateManager.popMatrix();
        }

        // 锈蚀裂纹从边缘蔓延
        renderRustCracks(width, height, progress);

        // 屏幕闪烁
        if (random.nextFloat() < 0.15f) {
            int flashAlpha = random.nextInt(80) + 20;
            drawRect(0, 0, width, height, (flashAlpha << 24) | 0xFFFFFF);
        }

        // 色调偏移到锈红
        int rustOverlay = (int) (progress * 60) << 24 | 0x4A2010;
        drawRect(0, 0, width, height, rustOverlay);
    }

    // ==================== 阶段3: 自我消亡 ====================
    // 短暂黑屏，然后显示齿轮符号
    private static void renderPhase3_SelfDissolution(ScaledResolution res, float progress) {
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();

        // 纯黑背景
        drawRect(0, 0, width, height, 0xFF000000);

        // 前半段：纯黑（自我的死亡）
        if (progress < 0.4f) {
            // 偶尔闪烁的微光
            if (random.nextFloat() < 0.05f) {
                int flickerAlpha = random.nextInt(20);
                drawRect(0, 0, width, height, (flickerAlpha << 24) | 0x333333);
            }
            return;
        }

        // 后半段：齿轮符号浮现
        float symbolProgress = (progress - 0.4f) / 0.6f;

        // 齿轮符号（用文字模拟）
        int symbolAlpha = (int) (symbolProgress * 200);
        int symbolColor = (symbolAlpha << 24) | 0x8B4513; // 锈色

        GlStateManager.pushMatrix();
        float scale = 3.0f;
        GlStateManager.scale(scale, scale, scale);

        String gearSymbol = "*"; // 简化的齿轮符号
        int symWidth = mc.fontRenderer.getStringWidth(gearSymbol);
        float symX = (width / 2f / scale) - symWidth / 2f;
        float symY = (height / 2f / scale) - 4;

        // 旋转效果
        GlStateManager.translate(width / 2f / scale, height / 2f / scale, 0);
        GlStateManager.rotate(symbolProgress * 90, 0, 0, 1);
        GlStateManager.translate(-width / 2f / scale, -height / 2f / scale, 0);

        mc.fontRenderer.drawString(gearSymbol, (int) symX, (int) symY, symbolColor);
        GlStateManager.popMatrix();
    }

    // ==================== 阶段4: 空洞觉醒 ====================
    // 显示文字，然后渐隐
    private static void renderPhase4_HollowAwakening(ScaledResolution res, float progress) {
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();

        // 背景渐淡（从黑到透明）
        int bgAlpha = (int) ((1.0f - progress) * 255);
        drawRect(0, 0, width, height, (bgAlpha << 24) | 0x000000);

        // 文字显示
        if (progress < 0.8f) {
            float textProgress = progress / 0.8f;

            // 打字机效果
            String line1 = "...";
            String line2 = "";
            String line3 = "";

            if (textProgress > 0.2f) {
                line1 = "人性已尽...";
            }
            if (textProgress > 0.5f) {
                line2 = "你已成为";
            }
            if (textProgress > 0.7f) {
                line3 = "破碎之神";
            }

            int textAlpha = (int) (Math.min(1.0f, textProgress * 2) * (1.0f - progress) * 255);
            int textColor = (textAlpha << 24) | 0x666666; // 暗淡灰色
            int titleColor = (textAlpha << 24) | 0x8B4513; // 锈色

            int centerX = width / 2;
            int centerY = height / 2;

            // 第一行
            int w1 = mc.fontRenderer.getStringWidth(line1);
            mc.fontRenderer.drawString(line1, centerX - w1 / 2, centerY - 20, textColor);

            // 第二行
            if (!line2.isEmpty()) {
                int w2 = mc.fontRenderer.getStringWidth(line2);
                mc.fontRenderer.drawString(line2, centerX - w2 / 2, centerY, textColor);
            }

            // 第三行（标题，更大）
            if (!line3.isEmpty()) {
                GlStateManager.pushMatrix();
                float scale = 1.5f;
                GlStateManager.scale(scale, scale, scale);
                int w3 = mc.fontRenderer.getStringWidth(line3);
                mc.fontRenderer.drawString(line3, (int) (centerX / scale - w3 / 2), (int) ((centerY + 20) / scale), titleColor);
                GlStateManager.popMatrix();
            }
        }

        // 最后阶段：边缘留下淡淡的机械纹路
        if (progress > 0.8f) {
            float fadeProgress = (progress - 0.8f) / 0.2f;
            int edgeAlpha = (int) ((1.0f - fadeProgress) * 40);
            renderRustVignette(width, height, edgeAlpha / 255.0f);
        }
    }

    // ==================== 辅助渲染方法 ====================

    /**
     * 渲染锈蚀暗角效果
     */
    private static void renderRustVignette(int width, int height, float intensity) {
        if (intensity <= 0) return;

        int alpha = (int) (intensity * 180);
        int rustColor = (alpha << 24) | 0x3D1C0A; // 深锈色

        // 上边缘
        for (int i = 0; i < 30; i++) {
            int a = (int) (alpha * (1.0f - i / 30.0f));
            drawRect(0, i, width, i + 1, (a << 24) | 0x3D1C0A);
        }
        // 下边缘
        for (int i = 0; i < 30; i++) {
            int a = (int) (alpha * (1.0f - i / 30.0f));
            drawRect(0, height - i - 1, width, height - i, (a << 24) | 0x3D1C0A);
        }
        // 左边缘
        for (int i = 0; i < 30; i++) {
            int a = (int) (alpha * (1.0f - i / 30.0f));
            drawRect(i, 0, i + 1, height, (a << 24) | 0x3D1C0A);
        }
        // 右边缘
        for (int i = 0; i < 30; i++) {
            int a = (int) (alpha * (1.0f - i / 30.0f));
            drawRect(width - i - 1, 0, width - i, height, (a << 24) | 0x3D1C0A);
        }
    }

    /**
     * 渲染静电噪点
     */
    private static void renderStaticNoise(int width, int height, float intensity) {
        int noiseCount = (int) (intensity * 500) + 100;

        for (int i = 0; i < noiseCount; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int size = random.nextInt(3) + 1;
            int gray = random.nextInt(100) + 50;
            int alpha = random.nextInt(150) + 50;

            drawRect(x, y, x + size, y + size, (alpha << 24) | (gray << 16) | (gray << 8) | gray);
        }
    }

    /**
     * 渲染锈蚀裂纹
     */
    private static void renderRustCracks(int width, int height, float progress) {
        int crackCount = (int) (progress * 15) + 3;
        int maxLength = (int) (progress * width / 3);

        for (int i = 0; i < crackCount; i++) {
            // 从边缘开始
            int startX, startY;
            boolean horizontal = random.nextBoolean();

            if (horizontal) {
                startX = random.nextBoolean() ? 0 : width;
                startY = random.nextInt(height);
            } else {
                startX = random.nextInt(width);
                startY = random.nextBoolean() ? 0 : height;
            }

            // 绘制裂纹
            int length = random.nextInt(maxLength) + 20;
            int crackAlpha = random.nextInt(100) + 80;
            int crackColor = (crackAlpha << 24) | 0x4A2010; // 深锈色

            for (int j = 0; j < length; j += 2) {
                int x = startX + (horizontal ? (startX == 0 ? j : -j) : random.nextInt(5) - 2);
                int y = startY + (horizontal ? random.nextInt(5) - 2 : (startY == 0 ? j : -j));

                if (x >= 0 && x < width && y >= 0 && y < height) {
                    drawRect(x, y, x + 2, y + 2, crackColor);
                }
            }
        }
    }
}
