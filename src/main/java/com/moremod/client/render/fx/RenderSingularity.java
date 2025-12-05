package com.moremod.client.render.fx;

import com.moremod.entity.fx.EntitySingularity;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * 奇点渲染器 - 黑色实体球
 * Singularity Renderer - Black Entity Sphere
 */
@SideOnly(Side.CLIENT)
public class RenderSingularity extends Render<EntitySingularity> {

    public RenderSingularity(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0F;  // 无阴影
    }

    @Override
    public void doRender(EntitySingularity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        float scale = entity.getScale();
        int phase = entity.getPhase();
        float rotation = entity.ticksExisted + partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);

        // 旋转动画
        GlStateManager.rotate(rotation * 5, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rotation * 3, 1.0F, 0.0F, 0.0F);

        // 启用混合
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();

        // 计算脉动
        float pulse = 1.0f;
        if (phase == EntitySingularity.PHASE_STABLE) {
            pulse = 1.0f + (float) Math.sin(rotation * 0.3) * 0.1f;
        } else if (phase == EntitySingularity.PHASE_EXPLODING) {
            pulse = 2.0f + (entity.ticksExisted % 5) * 0.5f;
        }

        // ===== 1. 核心黑球 =====
        renderSphere(scale * 0.4f * pulse, 0.02f, 0.02f, 0.02f, 1.0f);

        // ===== 2. 事件视界层 (深紫色半透明) =====
        renderSphere(scale * 0.6f * pulse, 0.1f, 0.0f, 0.15f, 0.8f);

        // ===== 3. 吸积盘层 (暗紫色) =====
        renderSphere(scale * 0.8f * pulse, 0.15f, 0.05f, 0.2f, 0.5f);

        // ===== 4. 外层扭曲场 =====
        GlStateManager.pushMatrix();
        GlStateManager.rotate(rotation * 10, 0, 1, 0);
        renderDistortionField(scale * 1.2f * pulse, rotation);
        GlStateManager.popMatrix();

        // ===== 5. 能量环 (水平) =====
        GlStateManager.pushMatrix();
        renderEnergyRing(scale * 1.5f, rotation, 0.3f, 0.1f, 0.5f, 0.6f);
        GlStateManager.popMatrix();

        // ===== 6. 能量环 (垂直) =====
        GlStateManager.pushMatrix();
        GlStateManager.rotate(90, 1, 0, 0);
        renderEnergyRing(scale * 1.3f, rotation * 0.7f, 0.2f, 0.0f, 0.4f, 0.4f);
        GlStateManager.popMatrix();

        // ===== 7. 吸引线 (稳定阶段) =====
        if (phase == EntitySingularity.PHASE_STABLE) {
            renderAttractionLines(scale, rotation);
        }

        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    /**
     * 渲染球体
     */
    private void renderSphere(float size, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int segments = 24;
        int rings = 12;

        GlStateManager.color(r, g, b, alpha);

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_NORMAL);

        for (int i = 0; i < rings; i++) {
            float theta1 = (float) (i * Math.PI / rings);
            float theta2 = (float) ((i + 1) * Math.PI / rings);

            for (int j = 0; j < segments; j++) {
                float phi1 = (float) (j * 2 * Math.PI / segments);
                float phi2 = (float) ((j + 1) * 2 * Math.PI / segments);

                float x1 = (float) (size * Math.sin(theta1) * Math.cos(phi1));
                float y1 = (float) (size * Math.cos(theta1));
                float z1 = (float) (size * Math.sin(theta1) * Math.sin(phi1));

                float x2 = (float) (size * Math.sin(theta1) * Math.cos(phi2));
                float y2 = (float) (size * Math.cos(theta1));
                float z2 = (float) (size * Math.sin(theta1) * Math.sin(phi2));

                float x3 = (float) (size * Math.sin(theta2) * Math.cos(phi2));
                float y3 = (float) (size * Math.cos(theta2));
                float z3 = (float) (size * Math.sin(theta2) * Math.sin(phi2));

                float x4 = (float) (size * Math.sin(theta2) * Math.cos(phi1));
                float y4 = (float) (size * Math.cos(theta2));
                float z4 = (float) (size * Math.sin(theta2) * Math.sin(phi1));

                buffer.pos(x1, y1, z1).normal(x1, y1, z1).endVertex();
                buffer.pos(x2, y2, z2).normal(x2, y2, z2).endVertex();
                buffer.pos(x3, y3, z3).normal(x3, y3, z3).endVertex();
                buffer.pos(x4, y4, z4).normal(x4, y4, z4).endVertex();
            }
        }

        tessellator.draw();
    }

    /**
     * 渲染扭曲场
     */
    private void renderDistortionField(float radius, float rotation) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int segments = 16;
        float waveAmplitude = 0.2f;

        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        for (int ring = 0; ring < 3; ring++) {
            float ringRadius = radius * (0.8f + ring * 0.15f);
            float yOffset = (ring - 1) * 0.3f;

            for (int i = 0; i <= segments; i++) {
                float angle = (float) (i * 2 * Math.PI / segments);
                float wave = (float) Math.sin(angle * 4 + rotation * 0.5f) * waveAmplitude;

                float px = (float) (Math.cos(angle) * (ringRadius + wave));
                float py = yOffset + wave * 0.5f;
                float pz = (float) (Math.sin(angle) * (ringRadius + wave));

                float alpha = 0.3f + (float) Math.sin(angle * 2 + rotation) * 0.2f;
                buffer.pos(px, py, pz).color(0.2f, 0.0f, 0.3f, alpha).endVertex();
            }
        }

        tessellator.draw();
    }

    /**
     * 渲染能量环
     */
    private void renderEnergyRing(float radius, float rotation, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.pushMatrix();
        GlStateManager.rotate(rotation * 15, 0, 1, 0);

        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        int segments = 48;
        float thickness = 0.08f;

        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2 * Math.PI / segments);
            float x = (float) (Math.cos(angle) * radius);
            float z = (float) (Math.sin(angle) * radius);

            // 环的内外边缘
            float innerX = (float) (Math.cos(angle) * (radius - thickness));
            float innerZ = (float) (Math.sin(angle) * (radius - thickness));

            // 动态透明度
            float dynamicAlpha = alpha * (0.7f + 0.3f * (float) Math.sin(angle * 3 + rotation * 0.5f));

            buffer.pos(x, thickness, z).color(r, g, b, dynamicAlpha).endVertex();
            buffer.pos(innerX, -thickness, innerZ).color(r * 0.5f, g * 0.5f, b * 0.5f, dynamicAlpha * 0.5f).endVertex();
        }

        tessellator.draw();
        GlStateManager.popMatrix();
    }

    /**
     * 渲染吸引线
     */
    private void renderAttractionLines(float scale, float rotation) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int lineCount = 12;
        float maxLength = 8.0f;

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i < lineCount; i++) {
            float angle = (float) (i * 2 * Math.PI / lineCount) + rotation * 0.1f;
            float pitch = (float) Math.sin(i + rotation * 0.2f) * 0.5f;

            // 线的起点（远端）
            float startDist = maxLength - (rotation * 0.05f % maxLength);
            float startX = (float) (Math.cos(angle) * Math.cos(pitch) * startDist);
            float startY = (float) (Math.sin(pitch) * startDist * 0.5f);
            float startZ = (float) (Math.sin(angle) * Math.cos(pitch) * startDist);

            // 线的终点（近端）
            float endDist = scale * 0.5f;
            float endX = (float) (Math.cos(angle) * Math.cos(pitch) * endDist);
            float endY = (float) (Math.sin(pitch) * endDist * 0.5f);
            float endZ = (float) (Math.sin(angle) * Math.cos(pitch) * endDist);

            buffer.pos(startX, startY, startZ).color(0.5f, 0.2f, 0.7f, 0.1f).endVertex();
            buffer.pos(endX, endY, endZ).color(0.3f, 0.0f, 0.5f, 0.6f).endVertex();
        }

        tessellator.draw();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntitySingularity entity) {
        return null;  // 纯程序渲染，不需要纹理
    }
}
