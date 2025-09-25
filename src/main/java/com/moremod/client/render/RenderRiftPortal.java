package com.moremod.client.render;

import com.moremod.entity.EntityRiftPortal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * 传送门的面片渲染器（billboard）：
 * - 双层贴图（base + glow）
 * - additive blending 发光
 * - 轻微旋转 + 呼吸脉动
 * - 强制亮度，不受环境光影响
 */
public class RenderRiftPortal extends Render<EntityRiftPortal> {

    private static final ResourceLocation TEX_BASE =
            new ResourceLocation("moremod", "textures/entity/rift_portal_base.png");
    private static final ResourceLocation TEX_GLOW =
            new ResourceLocation("moremod", "textures/entity/rift_portal_glow.png"); // 可选

    // 实体碰撞是 3x3，这里画面片的半径=1.5，略微放大一点更显眼
    private static final float HALF = 1.5F;
    private static final float SCALE = 1.10F;

    public RenderRiftPortal(RenderManager mgr) {
        super(mgr);
        this.shadowSize = 0.0F; // 不要阴影
    }

    @Override
    public void doRender(EntityRiftPortal e, double x, double y, double z,
                         float entityYaw, float partialTicks) {

        float t = e.ticksExisted + partialTicks;

        // 位置：略抬高到中线上方一点（看起来更居中）
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + 1.5D, z);

        // billboard：永远面向玩家
        GlStateManager.rotate(-renderManager.playerViewY, 0F, 1F, 0F);
        GlStateManager.rotate(renderManager.playerViewX, 1F, 0F, 0F);

        // 状态设置
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE); // additive
        GlStateManager.disableCull();
        GlStateManager.depthMask(false); // 不写深度，避免发光把自己挡暗

        // 自发光：把光照坐标拉满
        int light = 0xF000F0;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                (float)(light & 0xFFFF), (float)(light >> 16));

        // 呼吸/旋转
        float pulse = 1.0F + 0.05F * (float)Math.sin(t * 0.25F);     // 0.95~1.05
        float a1 = 0.90F; // base 透明度
        float a2 = 0.55F; // glow 透明度
        float base = HALF * SCALE * pulse;

        // 底层
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEX_BASE);
        drawQuad(base, base, a1);

        // 叠一层旋转，让画面更“厚”
        GlStateManager.rotate(t * 2.0F, 0F, 0F, 1F);
        drawQuad(base * 0.92F, base * 0.92F, 0.7F);

        // 辉光层（可选：若你没有放这张图，也不会崩）
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEX_GLOW);
        GlStateManager.rotate(-t * 1.2F, 0F, 0F, 1F);
        drawQuad(base * 1.06F, base * 1.06F, a2);

        // 还原
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();

        super.doRender(e, x, y, z, entityYaw, partialTicks);
    }

    private void drawQuad(float halfX, float halfY, float alpha) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        GlStateManager.color(1F, 1F, 1F, alpha);

        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        //   左上
        buf.pos(-halfX,  halfY, 0).tex(0, 0).color(255,255,255,(int)(alpha*255)).endVertex();
        //   右上
        buf.pos( halfX,  halfY, 0).tex(1, 0).color(255,255,255,(int)(alpha*255)).endVertex();
        //   右下
        buf.pos( halfX, -halfY, 0).tex(1, 1).color(255,255,255,(int)(alpha*255)).endVertex();
        //   左下
        buf.pos(-halfX, -halfY, 0).tex(0, 1).color(255,255,255,(int)(alpha*255)).endVertex();
        tess.draw();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityRiftPortal entity) {
        // 实际贴图在 doRender 里绑定，这里返回任意一张即可
        return TEX_BASE;
    }
}
