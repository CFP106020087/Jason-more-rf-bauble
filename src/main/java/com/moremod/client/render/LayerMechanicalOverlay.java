package com.moremod.client.render;

import com.moremod.system.humanity.AscensionRoute;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * 机械化叠加渲染层 - 纯几何风格（无需自定义纹理）
 * 
 * 视觉设计：
 * - 破碎之神：金橙熔岩裂隙 + 黑色骨架 + 故障闪烁
 * - 高度机械化：冰蓝全息线框 + 扫描波
 * - 中度机械化：暗红脉冲 + 心跳节奏
 * - 低度机械化：微弱青光轮廓
 */
@SideOnly(Side.CLIENT)
public class LayerMechanicalOverlay implements LayerRenderer<AbstractClientPlayer> {

    private final RenderPlayer renderPlayer;

    public LayerMechanicalOverlay(RenderPlayer renderPlayer) {
        this.renderPlayer = renderPlayer;
    }

    @Override
    public void doRenderLayer(
            AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
            float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale
    ) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        float humanity = data.getHumanity();
        AscensionRoute route = data.getAscensionRoute();
        boolean brokenGod = (route == AscensionRoute.BROKEN_GOD);

        if (humanity > 80f && !brokenGod) return;

        float intensity = brokenGod ? 1.0f : Math.min(1.0f, (80f - humanity) / 80f);

        renderMechanicalOverlay(player, limbSwing, limbSwingAmount,
                ageInTicks, netHeadYaw, headPitch, scale, humanity, intensity, brokenGod);
    }

    private void renderMechanicalOverlay(
            AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch,
            float scale, float humanity, float intensity, boolean brokenGod
    ) {
        ModelPlayer model = renderPlayer.getMainModel();
        if (model == null) return;

        // 保存状态
        float prevLX = OpenGlHelper.lastBrightnessX;
        float prevLY = OpenGlHelper.lastBrightnessY;

        GlStateManager.pushMatrix();
        try {
            // MoBends 兼容
            boolean isMoBends = MoBendsCompat.isBendsModel(model);
            if (!isMoBends) {
                model.setRotationAngles(limbSwing, limbSwingAmount,
                        ageInTicks, netHeadYaw, headPitch, scale, player);
            }

            // 基础渲染状态
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();  // 关键：不使用纹理
            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

            // 根据状态选择渲染风格
            if (brokenGod) {
                renderBrokenGodStyle(model, player, scale, ageInTicks, isMoBends);
            } else if (humanity <= 20f) {
                renderFullCyborg(model, player, scale, ageInTicks, intensity, isMoBends);
            } else if (humanity <= 40f) {
                renderHighMechanization(model, player, scale, ageInTicks, intensity, isMoBends);
            } else if (humanity <= 60f) {
                renderMediumMechanization(model, player, scale, ageInTicks, intensity, isMoBends);
            } else {
                renderLowMechanization(model, player, scale, ageInTicks, intensity, isMoBends);
            }

        } finally {
            // 恢复所有状态
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevLX, prevLY);
            GlStateManager.popMatrix();
        }
    }

    // ========================================================================
    // 破碎之神：熔岩裂隙 + 黑色骨架 + 剧烈故障
    // ========================================================================
    private void renderBrokenGodStyle(ModelPlayer model, AbstractClientPlayer player,
                                       float scale, float ageInTicks, boolean isMoBends) {

        // 心电图脉冲
        float cycle = (ageInTicks * 0.3f) % 20.0f;
        float pulse = (float) Math.pow(Math.abs(Math.sin(cycle * 0.314f)), 4);

        // 故障抖动
        float glitchX = 0, glitchY = 0;
        boolean glitching = player.world.rand.nextFloat() < 0.12f;
        if (glitching) {
            glitchX = (player.world.rand.nextFloat() - 0.5f) * 0.04f;
            glitchY = (player.world.rand.nextFloat() - 0.5f) * 0.04f;
        }

        // === 第一层：黑色深渊骨架 ===
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.02f, 1.02f, 1.02f);
        GlStateManager.color(0.02f, 0.02f, 0.02f, 0.8f);
        renderAllParts(model, scale, isMoBends);
        GlStateManager.popMatrix();

        // === 第二层：熔岩能量核心 ===
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);

        GlStateManager.pushMatrix();
        GlStateManager.translate(glitchX, glitchY, 0);
        GlStateManager.scale(1.01f, 1.01f, 1.01f);

        // 金橙 -> 白黄（脉冲时）
        float r = 1.0f;
        float g = 0.35f + pulse * 0.55f;
        float b = 0.0f + pulse * 0.4f;
        float a = 0.5f + pulse * 0.4f;
        GlStateManager.color(r, g, b, a);

        renderAllParts(model, scale, isMoBends);
        GlStateManager.popMatrix();

        // === 第三层：外层辉光 ===
        GlStateManager.pushMatrix();
        GlStateManager.translate(glitchX * 2f, glitchY * 2f, 0);
        GlStateManager.scale(1.06f, 1.06f, 1.06f);
        GlStateManager.color(1.0f, 0.5f, 0.1f, 0.12f + pulse * 0.08f);
        renderAllParts(model, scale, isMoBends);
        GlStateManager.popMatrix();

        // === 故障闪白 ===
        if (glitching && player.world.rand.nextFloat() < 0.3f) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(1.03f, 1.03f, 1.03f);
            GlStateManager.color(1f, 1f, 1f, 0.6f);
            renderAllParts(model, scale, isMoBends);
            GlStateManager.popMatrix();
        }
    }

    // ========================================================================
    // 全面机械化 (≤20%)：冰蓝全息 + 扫描波
    // ========================================================================
    private void renderFullCyborg(ModelPlayer model, AbstractClientPlayer player,
                                   float scale, float ageInTicks, float intensity, boolean isMoBends) {

        float pulse = MathHelper.sin(ageInTicks * 0.08f) * 0.5f + 0.5f;

        // 扫描波：从下往上移动的亮带
        float scanY = (ageInTicks * 0.05f) % 2.0f - 1.0f;

        // === 第一层：深蓝底色 ===
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.015f, 1.015f, 1.015f);
        GlStateManager.color(0.0f, 0.08f, 0.15f, 0.5f);
        renderAllParts(model, scale, isMoBends);
        GlStateManager.popMatrix();

        // === 第二层：冰蓝主光 ===
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.008f, 1.008f, 1.008f);
        GlStateManager.color(0.2f, 0.7f, 1.0f, 0.4f + pulse * 0.2f);
        renderAllParts(model, scale, isMoBends);
        GlStateManager.popMatrix();

        // === 第三层：白色边缘光 ===
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.03f, 1.03f, 1.03f);
        GlStateManager.color(0.6f, 0.9f, 1.0f, 0.1f + pulse * 0.06f);
        renderAllParts(model, scale, isMoBends);
        GlStateManager.popMatrix();

        // === 扫描波效果 ===
        renderScanWave(model, scale, scanY, isMoBends);
    }

    // ========================================================================
    // 高度机械化 (21-40%)：警告红 + 心跳
    // ========================================================================
    private void renderHighMechanization(ModelPlayer model, AbstractClientPlayer player,
                                          float scale, float ageInTicks, float intensity, boolean isMoBends) {

        float heartbeat = getHeartbeatPulse(ageInTicks, 0.12f);

        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);

        // === 主层：暗红 ===
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.01f, 1.01f, 1.01f);
        GlStateManager.color(0.9f, 0.15f, 0.1f, 0.35f + heartbeat * 0.35f);

        renderPart(model.bipedHead, scale);
        renderPart(model.bipedBody, scale);
        renderPart(model.bipedRightArm, scale);
        renderPart(model.bipedLeftArm, scale);
        renderPart(model.bipedRightLeg, scale);
        renderPart(model.bipedLeftLeg, scale);

        GlStateManager.popMatrix();

        // === 外层辉光 ===
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.04f, 1.04f, 1.04f);
        GlStateManager.color(1.0f, 0.2f, 0.15f, 0.08f + heartbeat * 0.12f);

        renderPart(model.bipedBody, scale);
        renderPart(model.bipedRightArm, scale);
        renderPart(model.bipedLeftArm, scale);

        GlStateManager.popMatrix();

        if (isMoBends) {
            renderMoBendsExtensions(model, scale, 0.9f, 0.15f, 0.1f, 0.3f + heartbeat * 0.2f);
        }
    }

    // ========================================================================
    // 中度机械化 (41-60%)：赛博青
    // ========================================================================
    private void renderMediumMechanization(ModelPlayer model, AbstractClientPlayer player,
                                            float scale, float ageInTicks, float intensity, boolean isMoBends) {

        float pulse = MathHelper.sin(ageInTicks * 0.06f) * 0.5f + 0.5f;

        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);

        // === 主层 ===
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.008f, 1.008f, 1.008f);
        GlStateManager.color(0.1f, 0.6f, 0.85f, 0.3f + pulse * 0.15f);

        renderPart(model.bipedRightArm, scale);
        renderPart(model.bipedLeftArm, scale);
        renderPart(model.bipedRightLeg, scale);
        renderPart(model.bipedLeftLeg, scale);

        GlStateManager.popMatrix();

        // === 淡外光 ===
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.025f, 1.025f, 1.025f);
        GlStateManager.color(0.3f, 0.75f, 1.0f, 0.06f + pulse * 0.04f);

        renderPart(model.bipedRightArm, scale);
        renderPart(model.bipedLeftArm, scale);

        GlStateManager.popMatrix();

        if (isMoBends) {
            renderMoBendsExtensions(model, scale, 0.1f, 0.6f, 0.85f, 0.25f + pulse * 0.1f);
        }
    }

    // ========================================================================
    // 低度机械化 (61-80%)：微弱提示
    // ========================================================================
    private void renderLowMechanization(ModelPlayer model, AbstractClientPlayer player,
                                         float scale, float ageInTicks, float intensity, boolean isMoBends) {

        float breath = MathHelper.sin(ageInTicks * 0.04f) * 0.5f + 0.5f;
        float alpha = (0.12f + breath * 0.08f) * intensity;

        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.005f, 1.005f, 1.005f);
        GlStateManager.color(0.4f, 0.75f, 0.9f, alpha);

        renderPart(model.bipedRightArm, scale);

        GlStateManager.popMatrix();

        if (isMoBends) {
            ModelRenderer rightForeArm = MoBendsCompat.getExtensionPart(model.bipedRightArm);
            if (rightForeArm != null) {
                GlStateManager.pushMatrix();
                GlStateManager.scale(1.005f, 1.005f, 1.005f);
                GlStateManager.color(0.4f, 0.75f, 0.9f, alpha);
                rightForeArm.render(scale);
                GlStateManager.popMatrix();
            }
        }
    }

    // ========================================================================
    // 扫描波效果
    // ========================================================================
    private void renderScanWave(ModelPlayer model, float scale, float scanY, boolean isMoBends) {
        // 扫描波是一条水平亮带，从下往上移动
        // scanY: -1.0 (底部) -> 1.0 (顶部)

        float waveAlpha = 0.4f;
        float waveHeight = 0.15f;

        // 只在波经过的位置加亮
        GlStateManager.pushMatrix();

        // 计算波的 Y 位置（玩家身高约 1.8 格）
        float worldY = scanY * 0.9f; // -0.9 到 0.9

        GlStateManager.translate(0, worldY, 0);
        GlStateManager.scale(1.02f, waveHeight, 1.02f);
        GlStateManager.color(0.8f, 0.95f, 1.0f, waveAlpha);

        // 只渲染身体作为扫描波载体
        renderPart(model.bipedBody, scale);

        GlStateManager.popMatrix();
    }

    // ========================================================================
    // 心跳脉冲算法
    // ========================================================================
    private float getHeartbeatPulse(float ticks, float rate) {
        float cycle = (ticks * rate) % 6.28f;

        // 双峰心跳：lub-dub
        float peak1 = (float) Math.pow(Math.max(0, Math.sin(cycle * 2f)), 8);
        float peak2 = (float) Math.pow(Math.max(0, Math.sin(cycle * 2f - 0.5f)), 12) * 0.6f;

        return Math.min(1f, peak1 + peak2);
    }

    // ========================================================================
    // 部件渲染
    // ========================================================================
    private void renderPart(ModelRenderer part, float scale) {
        if (part == null || part.isHidden || !part.showModel) return;
        part.render(scale);
    }

    private void renderAllParts(ModelPlayer model, float scale, boolean isMoBends) {
        renderPart(model.bipedHead, scale);
        renderPart(model.bipedBody, scale);
        renderPart(model.bipedRightArm, scale);
        renderPart(model.bipedLeftArm, scale);
        renderPart(model.bipedRightLeg, scale);
        renderPart(model.bipedLeftLeg, scale);

        if (isMoBends) {
            renderMoBendsExtensions(model, scale, -1, -1, -1, -1);
        }
    }

    private void renderMoBendsExtensions(ModelPlayer model, float scale, float r, float g, float b, float a) {
        ModelRenderer leftForeArm = MoBendsCompat.getExtensionPart(model.bipedLeftArm);
        ModelRenderer rightForeArm = MoBendsCompat.getExtensionPart(model.bipedRightArm);
        ModelRenderer leftForeLeg = MoBendsCompat.getExtensionPart(model.bipedLeftLeg);
        ModelRenderer rightForeLeg = MoBendsCompat.getExtensionPart(model.bipedRightLeg);

        if (r >= 0 && g >= 0 && b >= 0 && a >= 0) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(1.008f, 1.008f, 1.008f);
            GlStateManager.color(r, g, b, a);
        }

        if (leftForeArm != null) leftForeArm.render(scale);
        if (rightForeArm != null) rightForeArm.render(scale);
        if (leftForeLeg != null) leftForeLeg.render(scale);
        if (rightForeLeg != null) rightForeLeg.render(scale);

        if (r >= 0) {
            GlStateManager.popMatrix();
        }
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}