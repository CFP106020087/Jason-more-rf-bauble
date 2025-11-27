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
 * 机械化叠加渲染层 - 硬核双层线框版
 * 兼容 MoBends，状态安全
 */
@SideOnly(Side.CLIENT)
public class LayerMechanicalOverlay implements LayerRenderer<AbstractClientPlayer> {

    private final RenderPlayer renderPlayer;

    // MoBends 的 IModelPart 接口，用反射避免硬依赖
    private static Class<?> iModelPartClass;
    private static java.lang.reflect.Method applyCharacterTransformMethod;

    static {
        try {
            iModelPartClass = Class.forName("goblinbob.mobends.core.client.model.IModelPart");
            applyCharacterTransformMethod = iModelPartClass.getMethod("applyCharacterTransform", float.class);
        } catch (Exception e) {
            // MoBends 不存在，忽略
            iModelPartClass = null;
            applyCharacterTransformMethod = null;
        }
    }

    public LayerMechanicalOverlay(RenderPlayer renderPlayer) {
        this.renderPlayer = renderPlayer;
    }

    @Override
    public void doRenderLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {

        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        float humanity = data.getHumanity();
        AscensionRoute route = data.getAscensionRoute();
        boolean isBrokenGod = route == AscensionRoute.BROKEN_GOD;

        if (humanity > 75 && !isBrokenGod) return;

        float intensity = isBrokenGod ? 1.0f : Math.min(1.0f, (75 - humanity) / 75f);

        renderMechanicalEffect(player, limbSwing, limbSwingAmount, partialTicks, ageInTicks,
                netHeadYaw, headPitch, scale, intensity, isBrokenGod);
    }

    private void renderMechanicalEffect(
            AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount,
            float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch,
            float scale,
            float intensity, boolean brokenGod
    ) {
        ModelPlayer model = renderPlayer.getMainModel();
        if (model == null) return;

        boolean isMoBends = isMoBendsModel(model);

        // 保存所有需要恢复的状态
        float prevBrightnessX = OpenGlHelper.lastBrightnessX;
        float prevBrightnessY = OpenGlHelper.lastBrightnessY;
        boolean prevBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean prevTexture2D = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean prevLighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean prevDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int prevPolygonMode = GL11.glGetInteger(GL11.GL_POLYGON_MODE);

        GlStateManager.pushMatrix();
        try {
            // 动作同步（仅非 MoBends 时需要）
            if (!isMoBends) {
                model.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, player);
            }

            // 基础渲染状态
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);

            // 强制全亮度
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

            if (isMoBends) {
                // MoBends 路径：逐部件手绘线框
                renderMoBendsWireframe(model, player, scale, intensity, brokenGod, ageInTicks);
            } else {
                // 原版路径：使用 glPolygonMode
                renderVanillaWireframe(model, player, limbSwing, limbSwingAmount, ageInTicks,
                        netHeadYaw, headPitch, scale, intensity, brokenGod);
            }

        } finally {
            // 完整状态恢复
            GlStateManager.popMatrix();

            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, prevPolygonMode);
            GlStateManager.depthMask(prevDepthMask);

            if (prevTexture2D) {
                GlStateManager.enableTexture2D();
            } else {
                GlStateManager.disableTexture2D();
            }

            if (prevLighting) {
                GlStateManager.enableLighting();
            } else {
                GlStateManager.disableLighting();
            }

            if (prevBlend) {
                GlStateManager.enableBlend();
            } else {
                GlStateManager.disableBlend();
            }

            // 恢复默认混合模式
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

            // 恢复光照
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBrightnessX, prevBrightnessY);
        }
    }

    // ========================================================
    // 原版模型：双层 glPolygonMode 线框
    // ========================================================
    private void renderVanillaWireframe(
            ModelPlayer model, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, float scale,
            float intensity, boolean brokenGod
    ) {
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);

        // 第一层：黑色裂隙
        GL11.glLineWidth(brokenGod ? 5.0F : 3.5F);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(0.05F, 0.05F, 0.05F, 0.6F);

        GlStateManager.pushMatrix();
        float baseScale = 1.002f;
        GlStateManager.scale(baseScale, baseScale, baseScale);
        model.render(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.popMatrix();

        // 第二层：能量核心
        GL11.glLineWidth(brokenGod ? 2.0F : 1.2F);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        applyHardcoreColor(intensity, brokenGod, ageInTicks);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0.001f, 0);

        if (brokenGod) {
            applyJitter(player, ageInTicks);
        }

        model.render(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.popMatrix();

        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
    }

    // ========================================================
    // MoBends 模型：手绘线框立方体
    // ========================================================
    private void renderMoBendsWireframe(
            ModelPlayer model, AbstractClientPlayer player,
            float scale, float intensity, boolean brokenGod, float ageInTicks
    ) {
        // 收集所有需要渲染的部件
        ModelRenderer[] parts = {
                model.bipedHead,
                model.bipedBody,
                model.bipedLeftArm,
                model.bipedRightArm,
                model.bipedLeftLeg,
                model.bipedRightLeg
        };

        // 尝试获取 MoBends 的前臂/小腿扩展部件
        ModelRenderer[] forearmParts = getMoBendsForearms(model);

        // 第一层：黑色裂隙
        GL11.glLineWidth(brokenGod ? 4.0F : 2.5F);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(0.05F, 0.05F, 0.05F, 0.6F);

        for (ModelRenderer part : parts) {
            renderPartWireframe(part, scale, 1.01f);
        }
        for (ModelRenderer part : forearmParts) {
            if (part != null) renderPartWireframe(part, scale, 1.01f);
        }

        // 第二层：能量核心
        GL11.glLineWidth(brokenGod ? 1.5F : 0.8F);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        applyHardcoreColor(intensity, brokenGod, ageInTicks);

        float jitterX = 0, jitterY = 0;
        if (brokenGod && player.world.rand.nextFloat() < 0.2f) {
            jitterX = 0.01f * MathHelper.sin(ageInTicks * 0.8f);
            jitterY = jitterX;
        }

        for (ModelRenderer part : parts) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(jitterX, jitterY, 0);
            renderPartWireframe(part, scale, 1.005f);
            GlStateManager.popMatrix();
        }
        for (ModelRenderer part : forearmParts) {
            if (part != null) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(jitterX, jitterY, 0);
                renderPartWireframe(part, scale, 1.005f);
                GlStateManager.popMatrix();
            }
        }
    }

    private void renderPartWireframe(ModelRenderer part, float scale, float inflation) {
        if (part == null || part.isHidden || !part.showModel) return;

        GlStateManager.pushMatrix();

        // 应用部件变换
        if (isMoBendsPart(part)) {
            applyMoBendsTransform(part, scale);
        } else {
            applyVanillaTransform(part, scale);
        }

        // 绘制线框立方体
        drawPartWireframeCube(part, scale, inflation);

        // 递归渲染子部件
        if (part.childModels != null) {
            for (ModelRenderer child : part.childModels) {
                renderPartWireframe(child, scale, inflation);
            }
        }

        GlStateManager.popMatrix();
    }

    private void applyVanillaTransform(ModelRenderer part, float scale) {
        GlStateManager.translate(part.rotationPointX * scale, part.rotationPointY * scale, part.rotationPointZ * scale);

        if (part.rotateAngleZ != 0.0F) {
            GlStateManager.rotate(part.rotateAngleZ * (180F / (float) Math.PI), 0.0F, 0.0F, 1.0F);
        }
        if (part.rotateAngleY != 0.0F) {
            GlStateManager.rotate(part.rotateAngleY * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
        }
        if (part.rotateAngleX != 0.0F) {
            GlStateManager.rotate(part.rotateAngleX * (180F / (float) Math.PI), 1.0F, 0.0F, 0.0F);
        }
    }

    private void applyMoBendsTransform(ModelRenderer part, float scale) {
        try {
            if (applyCharacterTransformMethod != null) {
                applyCharacterTransformMethod.invoke(part, scale);
            }
        } catch (Exception e) {
            // 回退到原版变换
            applyVanillaTransform(part, scale);
        }
    }

    private void drawPartWireframeCube(ModelRenderer part, float scale, float inflation) {
        if (part.cubeList == null || part.cubeList.isEmpty()) return;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        for (net.minecraft.client.model.ModelBox box : part.cubeList) {
            float x1 = box.posX1 * scale * inflation;
            float y1 = box.posY1 * scale * inflation;
            float z1 = box.posZ1 * scale * inflation;
            float x2 = box.posX2 * scale * inflation;
            float y2 = box.posY2 * scale * inflation;
            float z2 = box.posZ2 * scale * inflation;

            buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);

            // 底面
            buffer.pos(x1, y1, z1).endVertex(); buffer.pos(x2, y1, z1).endVertex();
            buffer.pos(x2, y1, z1).endVertex(); buffer.pos(x2, y1, z2).endVertex();
            buffer.pos(x2, y1, z2).endVertex(); buffer.pos(x1, y1, z2).endVertex();
            buffer.pos(x1, y1, z2).endVertex(); buffer.pos(x1, y1, z1).endVertex();

            // 顶面
            buffer.pos(x1, y2, z1).endVertex(); buffer.pos(x2, y2, z1).endVertex();
            buffer.pos(x2, y2, z1).endVertex(); buffer.pos(x2, y2, z2).endVertex();
            buffer.pos(x2, y2, z2).endVertex(); buffer.pos(x1, y2, z2).endVertex();
            buffer.pos(x1, y2, z2).endVertex(); buffer.pos(x1, y2, z1).endVertex();

            // 竖边
            buffer.pos(x1, y1, z1).endVertex(); buffer.pos(x1, y2, z1).endVertex();
            buffer.pos(x2, y1, z1).endVertex(); buffer.pos(x2, y2, z1).endVertex();
            buffer.pos(x2, y1, z2).endVertex(); buffer.pos(x2, y2, z2).endVertex();
            buffer.pos(x1, y1, z2).endVertex(); buffer.pos(x1, y2, z2).endVertex();

            tessellator.draw();
        }
    }

    // ========================================================
    // 硬核配色：心电图式脉冲
    // ========================================================
    private void applyHardcoreColor(float intensity, boolean broken, float ticks) {
        float cycle = (ticks * (broken ? 0.3f : 0.15f)) % 20.0f;
        float pulse = (float) Math.pow(Math.sin(cycle), 4);

        float baseGlow = 0.4f;
        float r, g, b, a;

        if (broken) {
            r = 1.0f;
            g = 0.2f + pulse * 0.6f;
            b = 0.0f;
            a = 0.8f + pulse * 0.2f;
        } else {
            if (intensity > 0.5f) {
                r = 0.8f + pulse * 0.2f;
                g = 0.0f;
                b = 0.0f;
            } else {
                r = 0.0f;
                g = 0.5f + pulse * 0.5f;
                b = 0.8f + pulse * 0.2f;
            }
            a = 0.6f * intensity + pulse * 0.2f;
        }

        GlStateManager.color(r, g, b, a);
    }

    private void applyJitter(AbstractClientPlayer player, float ageInTicks) {
        if (player.world.rand.nextFloat() < 0.2f) {
            float jitter = 0.01f * MathHelper.sin(ageInTicks * 0.8f);
            GlStateManager.translate(jitter, jitter, 0);
        }
    }

    // ========================================================
    // MoBends 检测与兼容
    // ========================================================
    private boolean isMoBendsModel(ModelPlayer model) {
        if (iModelPartClass == null) return false;
        return iModelPartClass.isInstance(model.bipedBody);
    }

    private boolean isMoBendsPart(ModelRenderer part) {
        if (iModelPartClass == null) return false;
        return iModelPartClass.isInstance(part);
    }

    private ModelRenderer[] getMoBendsForearms(ModelPlayer model) {
        // MoBends 将前臂作为上臂的扩展存储
        // 通过反射获取 leftForeArm, rightForeArm, leftForeLeg, rightForeLeg
        ModelRenderer[] result = new ModelRenderer[4];

        try {
            if (isMoBendsPart(model.bipedLeftArm)) {
                java.lang.reflect.Method getExtension = model.bipedLeftArm.getClass().getMethod("getExtension");
                result[0] = (ModelRenderer) getExtension.invoke(model.bipedLeftArm);
            }
            if (isMoBendsPart(model.bipedRightArm)) {
                java.lang.reflect.Method getExtension = model.bipedRightArm.getClass().getMethod("getExtension");
                result[1] = (ModelRenderer) getExtension.invoke(model.bipedRightArm);
            }
            if (isMoBendsPart(model.bipedLeftLeg)) {
                java.lang.reflect.Method getExtension = model.bipedLeftLeg.getClass().getMethod("getExtension");
                result[2] = (ModelRenderer) getExtension.invoke(model.bipedLeftLeg);
            }
            if (isMoBendsPart(model.bipedRightLeg)) {
                java.lang.reflect.Method getExtension = model.bipedRightLeg.getClass().getMethod("getExtension");
                result[3] = (ModelRenderer) getExtension.invoke(model.bipedRightLeg);
            }
        } catch (Exception e) {
            // 获取失败，返回空数组
        }

        return result;
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}