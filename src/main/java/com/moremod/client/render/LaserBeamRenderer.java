package com.moremod.client.render;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

/**
 * 激光束渲染辅助类
 */
public class LaserBeamRenderer {

    /**
     * 渲染激光束
     * 
     * @param startPos 起始位置（相对于渲染原点）
     * @param endPos 结束位置（相对于渲染原点）
     * @param width 激光宽度
     * @param time 用于动画的时间值
     * @param r, g, b, a 颜色和透明度
     */
    public static void renderBeam(Vec3d startPos, Vec3d endPos, float width, 
                                  float time, float r, float g, float b, float a) {
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        
        Vec3d direction = endPos.subtract(startPos).normalize();
        double length = startPos.distanceTo(endPos);
        
        // 计算垂直于激光方向的两个向量用于构建四边形
        Vec3d up = new Vec3d(0, 1, 0);
        if (Math.abs(direction.y) > 0.99) {
            up = new Vec3d(1, 0, 0);
        }
        
        Vec3d right = direction.crossProduct(up).normalize().scale(width);
        Vec3d upVec = direction.crossProduct(right).normalize().scale(width);
        
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        
        // 核心光束
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        
        // 水平面
        addQuad(buffer, startPos, endPos, right, r, g, b, a);
        // 垂直面
        addQuad(buffer, startPos, endPos, upVec, r, g, b, a);
        
        tessellator.draw();
        
        // 外层光晕（更宽更透明）
        float glowWidth = width * 2.5F;
        Vec3d rightGlow = direction.crossProduct(up).normalize().scale(glowWidth);
        Vec3d upGlow = direction.crossProduct(rightGlow).normalize().scale(glowWidth);
        
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        
        float glowAlpha = a * 0.3F;
        addQuad(buffer, startPos, endPos, rightGlow, r, g, b, glowAlpha);
        addQuad(buffer, startPos, endPos, upGlow, r, g, b, glowAlpha);
        
        tessellator.draw();
        
        // 动态脉冲效果
        float pulsePos = (time * 0.1F) % 1.0F;
        Vec3d pulseStart = startPos.add(direction.scale(length * pulsePos));
        Vec3d pulseEnd = startPos.add(direction.scale(Math.min(length, length * pulsePos + length * 0.1)));
        
        if (pulsePos < 0.9F) {
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            
            float pulseWidth = width * 1.5F;
            Vec3d rightPulse = direction.crossProduct(up).normalize().scale(pulseWidth);
            
            addQuad(buffer, pulseStart, pulseEnd, rightPulse, 1.0F, 1.0F, 1.0F, a * 0.8F);
            
            tessellator.draw();
        }
        
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void addQuad(BufferBuilder buffer, Vec3d start, Vec3d end, Vec3d offset,
                                float r, float g, float b, float a) {
        int ri = (int)(r * 255);
        int gi = (int)(g * 255);
        int bi = (int)(b * 255);
        int ai = (int)(a * 255);
        
        buffer.pos(start.x + offset.x, start.y + offset.y, start.z + offset.z)
              .color(ri, gi, bi, ai).endVertex();
        buffer.pos(start.x - offset.x, start.y - offset.y, start.z - offset.z)
              .color(ri, gi, bi, ai).endVertex();
        buffer.pos(end.x - offset.x, end.y - offset.y, end.z - offset.z)
              .color(ri, gi, bi, ai).endVertex();
        buffer.pos(end.x + offset.x, end.y + offset.y, end.z + offset.z)
              .color(ri, gi, bi, ai).endVertex();
    }

    /**
     * 渲染护盾环
     */
    public static void renderShieldRing(double x, double y, double z, float radius, 
                                        float rotation, float r, float g, float b, float a) {
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(rotation, 0, 1, 0);
        
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        
        int segments = 32;
        float thickness = 0.1F;
        
        buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);
        
        int ri = (int)(r * 255);
        int gi = (int)(g * 255);
        int bi = (int)(b * 255);
        int ai = (int)(a * 255);
        
        for (int i = 0; i <= segments; i++) {
            float angle = (float)(Math.PI * 2 * i / segments);
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            
            // 外圈
            buffer.pos(cos * (radius + thickness), 0, sin * (radius + thickness))
                  .color(ri, gi, bi, ai).endVertex();
            // 内圈
            buffer.pos(cos * (radius - thickness), 0, sin * (radius - thickness))
                  .color(ri, gi, bi, 0).endVertex();
        }
        
        tessellator.draw();
        
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
}