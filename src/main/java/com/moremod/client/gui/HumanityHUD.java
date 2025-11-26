package com.moremod.client.gui;

import com.moremod.config.HumanityConfig;
import com.moremod.system.humanity.BiologicalProfile;
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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * 人性值HUD显示
 * Humanity HUD Display
 *
 * 显示当前人性值、崩解状态、分析进度等信息
 */
@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class HumanityHUD extends Gui {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();

    // 动画相关
    private static float displayedHumanity = 75f;
    private static float pulseAnimation = 0f;
    private static boolean pulseExpanding = true;
    private static int shakeTimer = 0;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT) return;

        if (!HumanityConfig.showHumanityHUD) return;

        EntityPlayer player = mc.player;
        if (player == null) return;

        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        // 更新动画
        updateAnimations(data);

        // 渲染HUD
        renderHumanityHUD(data, event.getResolution());
    }

    /**
     * 更新动画状态
     */
    private static void updateAnimations(IHumanityData data) {
        float targetHumanity = data.getHumanity();

        // 平滑过渡
        float diff = targetHumanity - displayedHumanity;
        displayedHumanity += diff * 0.1f;

        // 脉冲动画
        if (pulseExpanding) {
            pulseAnimation += 0.05f;
            if (pulseAnimation >= 1.0f) {
                pulseAnimation = 1.0f;
                pulseExpanding = false;
            }
        } else {
            pulseAnimation -= 0.05f;
            if (pulseAnimation <= 0f) {
                pulseAnimation = 0f;
                pulseExpanding = true;
            }
        }

        // 低人性抖动
        if (data.getHumanity() < 40f) {
            shakeTimer++;
        } else {
            shakeTimer = 0;
        }
    }

    /**
     * 渲染人性值HUD
     */
    private static void renderHumanityHUD(IHumanityData data, ScaledResolution resolution) {
        int x = HumanityConfig.hudXOffset;
        int y = HumanityConfig.hudYOffset;

        float humanity = data.getHumanity();

        // 低人性抖动效果
        if (humanity < 40f && random.nextFloat() < 0.1f) {
            x += random.nextInt(3) - 1;
            y += random.nextInt(3) - 1;
        }

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();

        // 获取颜色
        int color = getHumanityColor(humanity);
        int bgColor = 0x80000000; // 半透明黑色背景

        // 绘制背景
        Gui.drawRect(x - 2, y - 2, x + 102, y + 14, bgColor);

        // 绘制进度条背景
        Gui.drawRect(x, y, x + 100, y + 8, 0xFF333333);

        // 绘制进度条
        int barWidth = (int) (displayedHumanity);
        Gui.drawRect(x, y, x + barWidth, y + 8, color);

        // 崩解状态特殊效果
        if (data.isDissolutionActive()) {
            int dissolutionColor = 0xFFFF0000;
            float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 100.0);
            int alpha = (int) (pulse * 255);
            dissolutionColor = (dissolutionColor & 0x00FFFFFF) | (alpha << 24);
            Gui.drawRect(x - 2, y - 2, x + 102, y + 14, dissolutionColor);
        }

        // 绘制文本
        String text = String.format("%.0f%%", displayedHumanity);
        int textColor = humanity < 25f ? 0xFF8800AA : (humanity < 50f ? 0xFFDD88FF : 0xFFFFFFFF);
        mc.fontRenderer.drawStringWithShadow(text, x + 50 - mc.fontRenderer.getStringWidth(text) / 2f, y + 1, textColor);

        // 绘制状态标签
        String label = getStatusLabel(data);
        if (!label.isEmpty()) {
            mc.fontRenderer.drawStringWithShadow(label, x, y + 12, getStatusLabelColor(data));
        }

        // 崩解倒计时
        if (data.isDissolutionActive()) {
            int seconds = data.getDissolutionTicks() / 20;
            String warning = "\u00a74【崩解中】\u00a7c " + seconds + "s";
            mc.fontRenderer.drawStringWithShadow(warning, x, y + 22, 0xFFFF0000);
        }

        // 分析进度
        ResourceLocation analyzing = data.getAnalyzingEntity();
        if (analyzing != null) {
            int progress = data.getAnalysisProgress();
            String analysisText = "\u00a7a分析: \u00a7f" + analyzing.getPath() + " \u00a7e" + progress + "%";
            mc.fontRenderer.drawStringWithShadow(analysisText, x, y + (data.isDissolutionActive() ? 32 : 22), 0xFF88FF88);
        }

        // 存在锚定标记
        if (data.isExistenceAnchored(mc.world.getTotalWorldTime())) {
            String anchorText = "\u00a7d[锚定中]";
            mc.fontRenderer.drawStringWithShadow(anchorText, x + 70, y + 1, 0xFFDD88FF);
        }

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * 获取人性值对应的颜色
     */
    private static int getHumanityColor(float humanity) {
        if (humanity >= 75f) {
            return 0xFFAADDFF;  // 蓝白 - 高人性
        } else if (humanity >= 60f) {
            return 0xFFBBDDEE;  // 浅蓝
        } else if (humanity >= 50f) {
            return 0xFFFFFFAA;  // 黄白 - 中间
        } else if (humanity >= 40f) {
            return 0xFFEEBBFF;  // 浅紫 - 灰域
        } else if (humanity >= 25f) {
            return 0xFFDD88FF;  // 紫
        } else if (humanity >= 10f) {
            return 0xFFAA44DD;  // 深紫
        } else {
            return 0xFF8800AA;  // 暗紫 - 极低人性
        }
    }

    /**
     * 获取状态标签
     */
    private static String getStatusLabel(IHumanityData data) {
        float humanity = data.getHumanity();

        if (data.isDissolutionActive()) {
            return "";  // 崩解状态单独显示
        }

        if (humanity >= 80f) {
            return "\u00a7b猎人协议";
        } else if (humanity >= 60f) {
            return "\u00a77高人性";
        } else if (humanity >= 40f) {
            return "\u00a7d灰域";
        } else if (humanity >= 25f) {
            return "\u00a75异常协议";
        } else if (humanity >= 10f) {
            return "\u00a74深度异常";
        } else {
            return "\u00a74\u00a7l临界崩解";
        }
    }

    /**
     * 获取状态标签颜色
     */
    private static int getStatusLabelColor(IHumanityData data) {
        float humanity = data.getHumanity();

        if (humanity >= 80f) return 0xFF88DDFF;
        if (humanity >= 60f) return 0xFFAAAAAA;
        if (humanity >= 40f) return 0xFFDD88FF;
        if (humanity >= 25f) return 0xFFAA44DD;
        return 0xFFFF4444;
    }
}
