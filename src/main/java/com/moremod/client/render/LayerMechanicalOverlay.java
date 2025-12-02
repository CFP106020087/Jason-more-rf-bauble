package com.moremod.client.render;

import com.moremod.system.humanity.AscensionRoute;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 机械化叠加渲染层 (回归稳定版)
 * * 核心逻辑：
 * 1. 使用 renderPlayer.getMainModel() 确保最大的动作兼容性 (Mo' Bends)。
 * 2. 渲染时暂时"关闭"模型的外套层 (Hat/Jacket/Pants)，解决"悬浮方块"和"炸裂"问题。
 * 3. 渲染后立即恢复外套层，不影响原本皮肤。
 * 4. 修复了 NPE 崩溃问题 (传参 player)。
 */
@SideOnly(Side.CLIENT)
public class LayerMechanicalOverlay implements LayerRenderer<AbstractClientPlayer> {

    private final RenderPlayer renderPlayer;

    // 资源路径
    private static final ResourceLocation TEX_LAYER_1_SKELETON =
            new ResourceLocation("moremod", "textures/models/armor/mechanical_layer_1_skeleton.png");
    private static final ResourceLocation TEX_LAYER_2_RED =
            new ResourceLocation("moremod", "textures/models/armor/mechanical_layer_2_red.png");
    private static final ResourceLocation TEX_LAYER_3_BLUE =
            new ResourceLocation("moremod", "textures/models/armor/mechanical_layer_3_blue.png");

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

        // --- 1. 计算透明度 ---
        float alphaSkeleton = 1.0f; // 骨架常驻

        float alphaRed = 0f;
        if (route == AscensionRoute.BROKEN_GOD) {
            alphaRed = 0.6f;
        } else if (humanity < 50f) {
            float danger = (50f - humanity) / 50f;
            float pulse = (MathHelper.sin(ageInTicks * 0.15f) * 0.5f + 0.5f);
            alphaRed = danger * (0.4f + 0.6f * pulse);
        }

        float alphaBlue = 0f;
        if (route == AscensionRoute.BROKEN_GOD) {
            float glitchPulse = (MathHelper.sin(ageInTicks * 0.3f) * 0.5f + 0.5f);
            alphaBlue = 0.8f + 0.2f * glitchPulse;
        } else if (humanity < 10f) {
            alphaBlue = (10f - humanity) / 10f * 0.5f;
        }

        if (route == AscensionRoute.SHAMBHALA) {
            alphaRed = 0f;
            alphaBlue = 0f;
        }

        // --- 2. 准备渲染 (关键修复) ---
        ModelPlayer model = renderPlayer.getMainModel();

        // 传递动作参数 (Mo' Bends 兼容)
        model.setModelAttributes(renderPlayer.getMainModel());
        model.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, player);

        // [重要] 备份外套层的显示状态
        boolean showHat = model.bipedHeadwear.showModel;
        boolean showJacket = model.bipedBodyWear.showModel;
        boolean showLeftArm = model.bipedLeftArmwear.showModel;
        boolean showRightArm = model.bipedRightArmwear.showModel;
        boolean showLeftLeg = model.bipedLeftLegwear.showModel;
        boolean showRightLeg = model.bipedRightLegwear.showModel;

        try {
            // [关键] 强制关闭所有"外套层"！
            // 解决"悬浮黑块"问题的核心：只渲染贴肉的身体层
            model.bipedHeadwear.showModel = false;
            model.bipedBodyWear.showModel = false;
            model.bipedLeftArmwear.showModel = false;
            model.bipedRightArmwear.showModel = false;
            model.bipedLeftLegwear.showModel = false;
            model.bipedRightLegwear.showModel = false;

            // --- 3. 渲染循环 ---

            // A. 骨架 (Base) - 极小膨胀 (0.001) 防止 Z-Fighting
            renderLayer(player, model, TEX_LAYER_1_SKELETON, alphaSkeleton, false, 0.001f);

            // B. 红光 (Warning)
            if (alphaRed > 0.05f) {
                renderLayer(player, model, TEX_LAYER_2_RED, alphaRed, true, 0.002f);
            }

            // C. 蓝光 (Energy)
            if (alphaBlue > 0.05f) {
                renderLayer(player, model, TEX_LAYER_3_BLUE, alphaBlue, true, 0.003f);
            }

        } finally {
            // [重要] 恢复外套层状态 (必须执行，否则玩家皮肤会坏掉)
            model.bipedHeadwear.showModel = showHat;
            model.bipedBodyWear.showModel = showJacket;
            model.bipedLeftArmwear.showModel = showLeftArm;
            model.bipedRightArmwear.showModel = showRightArm;
            model.bipedLeftLegwear.showModel = showLeftLeg;
            model.bipedRightLegwear.showModel = showRightLeg;
        }
    }

    /**
     * 渲染单层
     * [CRASH FIX]: 必须传入 AbstractClientPlayer 实体，否则 RenderLivingBase 会报 NPE
     */
    private void renderLayer(AbstractClientPlayer player, ModelPlayer model, ResourceLocation texture, float alpha, boolean emissive, float expand) {
        renderPlayer.bindTexture(texture);
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0f, 1.0f, 1.0f, alpha);

        // 稍微放大一点点，形成层级
        GlStateManager.scale(1.0f + expand, 1.0f + expand, 1.0f + expand);

        float lastX = OpenGlHelper.lastBrightnessX;
        float lastY = OpenGlHelper.lastBrightnessY;

        if (emissive) {
            GlStateManager.disableLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        } else {
            GlStateManager.enableLighting();
        }

        // [CRASH FIX] 传入 player 实体！
        model.render(player, 0, 0, 0, 0, 0, 0.0625f);

        if (emissive) {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastX, lastY);
            GlStateManager.enableLighting();
        }

        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}