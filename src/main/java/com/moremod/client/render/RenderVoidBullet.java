package com.moremod.client.render;

import com.moremod.entity.projectile.EntityVoidBullet;
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
public class RenderVoidBullet extends Render<EntityVoidBullet> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("moremod", "textures/entity/void_bullet.png");

    public RenderVoidBullet(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.5F;
    }

    @Override
    public void doRender(EntityVoidBullet entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x, (float)y, (float)z);

        // 旋转动画
        float rotation = entity.ticksExisted + partialTicks;
        GlStateManager.rotate(rotation * 10, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rotation * 5, 1.0F, 0.0F, 0.0F);

        // 启用混合和透明
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();

        // 绘制多层球体
        renderSphere(0.5F, 1.0F, 0.5F, 1.0F, 0.8F); // 核心
        renderSphere(0.8F, 0.8F, 0.3F, 0.8F, 0.5F); // 中层
        renderSphere(1.2F, 0.6F, 0.2F, 0.6F, 0.3F); // 外层

        // 绘制能量环
        renderEnergyRing(1.5F, rotation);

        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private void renderSphere(float size, float r, float g, float b, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int segments = 16;
        int rings = 8;

        GlStateManager.color(r, g, b, alpha);

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_NORMAL);

        for (int i = 0; i < rings; i++) {
            float theta1 = (float)(i * Math.PI / rings);
            float theta2 = (float)((i + 1) * Math.PI / rings);

            for (int j = 0; j < segments; j++) {
                float phi1 = (float)(j * 2 * Math.PI / segments);
                float phi2 = (float)((j + 1) * 2 * Math.PI / segments);

                // 顶点计算
                float x1 = (float)(size * Math.sin(theta1) * Math.cos(phi1));
                float y1 = (float)(size * Math.cos(theta1));
                float z1 = (float)(size * Math.sin(theta1) * Math.sin(phi1));

                float x2 = (float)(size * Math.sin(theta1) * Math.cos(phi2));
                float y2 = (float)(size * Math.cos(theta1));
                float z2 = (float)(size * Math.sin(theta1) * Math.sin(phi2));

                float x3 = (float)(size * Math.sin(theta2) * Math.cos(phi2));
                float y3 = (float)(size * Math.cos(theta2));
                float z3 = (float)(size * Math.sin(theta2) * Math.sin(phi2));

                float x4 = (float)(size * Math.sin(theta2) * Math.cos(phi1));
                float y4 = (float)(size * Math.cos(theta2));
                float z4 = (float)(size * Math.sin(theta2) * Math.sin(phi1));

                buffer.pos(x1, y1, z1).normal(x1, y1, z1).endVertex();
                buffer.pos(x2, y2, z2).normal(x2, y2, z2).endVertex();
                buffer.pos(x3, y3, z3).normal(x3, y3, z3).endVertex();
                buffer.pos(x4, y4, z4).normal(x4, y4, z4).endVertex();
            }
        }

        tessellator.draw();
    }

    private void renderEnergyRing(float radius, float rotation) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.pushMatrix();
        GlStateManager.rotate(rotation * 20, 0, 1, 0);

        GlStateManager.color(0.8F, 0.2F, 1.0F, 0.6F);

        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION);

        int segments = 32;
        for (int i = 0; i <= segments; i++) {
            float angle = (float)(i * 2 * Math.PI / segments);
            float x = (float)(Math.cos(angle) * radius);
            float z = (float)(Math.sin(angle) * radius);

            buffer.pos(x, 0.1, z).endVertex();
            buffer.pos(x, -0.1, z).endVertex();
        }

        tessellator.draw();

        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityVoidBullet entity) {
        return TEXTURE;
    }
}

// ===== 更新Boss的shootVoidBullet方法 =====
// 在EntityRiftwarden中更新：

