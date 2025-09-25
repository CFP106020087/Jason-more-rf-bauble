package com.moremod.client.render;

import com.moremod.entity.EntityVoidPortal;
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

@SideOnly(Side.CLIENT)
public class RenderVoidPortal2x1 extends Render<EntityVoidPortal> {

    // 两层贴图（1×2，64x128）
    private static final ResourceLocation OUTER_TEX =
            new ResourceLocation("moremod", "textures/entity/void_portal_outer_1x2.png");
    private static final ResourceLocation INNER_TEX =
            new ResourceLocation("moremod", "textures/entity/void_portal_inner_1x2.png");

    // 固定尺寸：宽 1，高 2（与实体 setSize(1,2) 对应）
    private static final float WIDTH  = 1.0f;
    private static final float HEIGHT = 2.0f;
    // 轻微厚度，避免 Z 冲突，画双面
    private static final float THICK  = 0.001f;

    public RenderVoidPortal2x1(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0f;
    }

    @Override
    public void doRender(EntityVoidPortal entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        // 面片底部贴地，中心对齐
        GlStateManager.translate(0.0, 0.5 * HEIGHT, 0.0);

        // 恢复旋转：billboard，始终面向摄像机
        GlStateManager.rotate(-this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);

        // 基本渲染状态
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        GlStateManager.disableCull(); // 双面可见
        GlStateManager.color(1f, 1f, 1f, 1f);

        // ---------- Pass 1：outer（正常 alpha，静态无滚动） ----------
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        this.bindTexture(OUTER_TEX);
        drawQuad(WIDTH, HEIGHT, THICK, 1.0f);

        // ---------- Pass 2：inner（加色发光，略小，静态） ----------
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        this.bindTexture(INNER_TEX);
        // 略缩小，让内层更“在里面”（静态缩放，不是呼吸）
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.96f, 0.96f, 1.0f);
        drawQuad(WIDTH, HEIGHT, THICK, 0.85f);
        GlStateManager.popMatrix();

        // （可选）淡淡外扩光晕（静态放大）
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.color(1f, 1f, 1f, 0.35f);
        this.bindTexture(OUTER_TEX);
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.06f, 1.08f, 1.0f);
        drawQuad(WIDTH, HEIGHT, THICK, 1.0f);
        GlStateManager.popMatrix();

        // 还原
        GlStateManager.color(1f,1f,1f,1f);
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();

        GlStateManager.popMatrix();
    }

    /**
     * 在 X–Y 平面绘制一个双面矩形（宽×高），前后各偏 THICK；支持整体 alpha。
     * 注意：不做任何 UV 滚动或旋转动画。
     */
    private void drawQuad(float width, float height, float thick, float alpha) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        float halfW = width * 0.5f;
        float halfH = height * 0.5f;

        float u0 = 0.0f, v0 = 0.0f, u1 = 1.0f, v1 = 1.0f;
        int a = (int)(alpha * 255);

        // 正面（朝 +Z）
        buf.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_TEX_COLOR);
        buf.pos(-halfW, -halfH, +thick).tex(u0, v1).color(255,255,255,a).endVertex();
        buf.pos(-halfW,  halfH, +thick).tex(u0, v0).color(255,255,255,a).endVertex();
        buf.pos( halfW, -halfH, +thick).tex(u1, v1).color(255,255,255,a).endVertex();
        buf.pos( halfW,  halfH, +thick).tex(u1, v0).color(255,255,255,a).endVertex();
        tess.draw();

        // 反面（朝 -Z），U 翻转避免镜像纹理重复感
        buf.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_TEX_COLOR);
        buf.pos( halfW, -halfH, -thick).tex(u0, v1).color(255,255,255,a).endVertex();
        buf.pos( halfW,  halfH, -thick).tex(u0, v0).color(255,255,255,a).endVertex();
        buf.pos(-halfW, -halfH, -thick).tex(u1, v1).color(255,255,255,a).endVertex();
        buf.pos(-halfW,  halfH, -thick).tex(u1, v0).color(255,255,255,a).endVertex();
        tess.draw();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityVoidPortal entity) {
        // 不使用此返回值（在 doRender 里手动 bind），但必须返回一个
        return INNER_TEX;
    }
}
