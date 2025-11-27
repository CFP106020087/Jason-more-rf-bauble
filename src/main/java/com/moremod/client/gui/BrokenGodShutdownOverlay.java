package com.moremod.client.gui;

import com.moremod.config.BrokenGodConfig;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * 破碎之神停机模式 Overlay
 * Broken God Shutdown Mode Overlay
 *
 * 在停机期间显示全屏重启动画效果
 */
@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class BrokenGodShutdownOverlay extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();

    // 动画状态
    private static float bootProgress = 0f;
    private static int glitchTimer = 0;
    private static int lastShutdownTimer = -1;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @SideOnly(Side.CLIENT)
    public static void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        EntityPlayer player = mc.player;
        if (player == null) return;

        // 获取人性值数据（从客户端同步数据）
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null) return;

        // 检查是否处于停机状态
        if (!data.isInShutdown()) {
            // 重置动画状态
            bootProgress = 0f;
            lastShutdownTimer = -1;
            return;
        }

        int shutdownTimer = data.getShutdownTimer();
        int maxTimer = BrokenGodConfig.shutdownTicks;

        // 计算重启进度 (0 = 刚开始停机, 1 = 即将完成)
        bootProgress = 1f - (float) shutdownTimer / maxTimer;

        // 渲染停机 overlay
        renderShutdownOverlay(event.getResolution(), shutdownTimer, maxTimer);

        lastShutdownTimer = shutdownTimer;
    }

    /**
     * 渲染停机 overlay
     */
    private static void renderShutdownOverlay(ScaledResolution resolution, int timer, int maxTimer) {
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();

        // 计算阶段 (0-3)
        int phase = (int) (bootProgress * 4);

        // 背景颜色 - 从深黑逐渐变亮
        float bgAlpha = 0.95f - bootProgress * 0.3f;
        int bgColor = ((int)(bgAlpha * 255) << 24) | 0x000510;

        // 绘制全屏背景
        Gui.drawRect(0, 0, width, height, bgColor);

        // 故障效果
        if (random.nextFloat() < 0.1f && bootProgress < 0.8f) {
            glitchTimer = 3;
        }

        if (glitchTimer > 0) {
            glitchTimer--;
            // 随机条纹
            for (int i = 0; i < 5; i++) {
                int y = random.nextInt(height);
                int h = random.nextInt(10) + 2;
                int glitchColor = 0x40 << 24 | (random.nextInt(100) << 16) | (random.nextInt(255) << 8) | random.nextInt(100);
                Gui.drawRect(0, y, width, y + h, glitchColor);
            }
        }

        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();

        // 渲染文本
        int centerX = width / 2;
        int centerY = height / 2;

        // 主标题
        String title;
        int titleColor;

        if (phase == 0) {
            title = "[ SYSTEM SHUTDOWN ]";
            titleColor = 0xFF880000;
        } else if (phase == 1) {
            title = "[ INITIALIZING REBOOT ]";
            titleColor = 0xFFAA4400;
        } else if (phase == 2) {
            title = "[ LOADING SYSTEMS ]";
            titleColor = 0xFFAAAA00;
        } else {
            title = "[ REBOOT COMPLETE ]";
            titleColor = 0xFF00FF00;
        }

        // 闪烁效果
        if (timer % 10 < 5 || phase >= 3) {
            mc.fontRenderer.drawStringWithShadow(title,
                    centerX - mc.fontRenderer.getStringWidth(title) / 2f,
                    centerY - 40, titleColor);
        }

        // 进度条背景
        int barWidth = 200;
        int barHeight = 8;
        int barX = centerX - barWidth / 2;
        int barY = centerY - 10;

        Gui.drawRect(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xFF333333);
        Gui.drawRect(barX, barY, barX + barWidth, barY + barHeight, 0xFF111111);

        // 进度条填充
        int fillWidth = (int) (barWidth * bootProgress);
        int progressColor = getProgressColor(bootProgress);
        Gui.drawRect(barX, barY, barX + fillWidth, barY + barHeight, progressColor);

        // 进度百分比
        String percentText = String.format("%.0f%%", bootProgress * 100);
        mc.fontRenderer.drawStringWithShadow(percentText,
                centerX - mc.fontRenderer.getStringWidth(percentText) / 2f,
                barY + 12, 0xFFCCCCCC);

        // 状态信息
        String[] statusLines = getStatusLines(phase, timer);
        int lineY = centerY + 30;
        for (String line : statusLines) {
            mc.fontRenderer.drawStringWithShadow(line,
                    centerX - mc.fontRenderer.getStringWidth(line) / 2f,
                    lineY, 0xFF888888);
            lineY += 12;
        }

        // 剩余时间
        int seconds = timer / 20;
        String timeText = "预计恢复: " + seconds + "s";
        mc.fontRenderer.drawStringWithShadow(timeText,
                centerX - mc.fontRenderer.getStringWidth(timeText) / 2f,
                centerY + 70, 0xFF666666);

        // 扫描线效果
        if (bootProgress < 0.9f) {
            int scanY = (int) ((System.currentTimeMillis() / 20) % height);
            Gui.drawRect(0, scanY, width, scanY + 2, 0x20FFFFFF);
        }

        // 边框装饰
        int borderColor = 0xFF000000 | (progressColor & 0x00FFFFFF);
        Gui.drawRect(0, 0, width, 2, borderColor);
        Gui.drawRect(0, height - 2, width, height, borderColor);
        Gui.drawRect(0, 0, 2, height, borderColor);
        Gui.drawRect(width - 2, 0, width, height, borderColor);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * 获取进度条颜色
     */
    private static int getProgressColor(float progress) {
        if (progress < 0.25f) {
            return 0xFF880000; // 深红
        } else if (progress < 0.5f) {
            return 0xFFAA4400; // 橙红
        } else if (progress < 0.75f) {
            return 0xFFAAAA00; // 黄
        } else {
            return 0xFF00AA00; // 绿
        }
    }

    /**
     * 获取状态信息行
     */
    private static String[] getStatusLines(int phase, int timer) {
        switch (phase) {
            case 0:
                return new String[] {
                        "检测到核心损伤...",
                        "启动紧急协议",
                        "暂停所有系统"
                };
            case 1:
                return new String[] {
                        "初始化内存...",
                        "加载核心驱动",
                        "校验系统完整性"
                };
            case 2:
                return new String[] {
                        "恢复神经连接...",
                        "重新校准传感器",
                        "启动辅助系统"
                };
            case 3:
                return new String[] {
                        "所有系统已就绪",
                        "核心状态: 正常",
                        "准备恢复运行..."
                };
            default:
                return new String[] { "处理中..." };
        }
    }
}
