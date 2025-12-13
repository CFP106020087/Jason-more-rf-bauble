package com.moremod.printer.client;

import com.moremod.printer.TileEntityPrinter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

/**
 * 打印机GeckoLib渲染器
 *
 * 使用GeckoLib渲染带有机械臂动画的打印机
 * 包含AMD显卡驱动崩溃修复
 */
public class PrinterRenderer extends GeoBlockRenderer<TileEntityPrinter> {

    public PrinterRenderer() {
        super(new PrinterModel());
    }

    @Override
    public void render(TileEntityPrinter te, double x, double y, double z, float partialTicks, int destroyStage) {
        if (te == null) return;

        try {
            // 保存OpenGL状态，防止AMD驱动崩溃
            GlStateManager.pushMatrix();
            GlStateManager.pushAttrib();

            // 设置正确的纹理单元状态
            // 这是修复AMD驱动在setLightmapTextureCoords时崩溃的关键
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.enableTexture2D();

            // 保存当前lightmap坐标
            float lastBrightnessX = OpenGlHelper.lastBrightnessX;
            float lastBrightnessY = OpenGlHelper.lastBrightnessY;

            // 确保lightmap纹理已绑定
            int light = te.getWorld().getCombinedLight(te.getPos(), 0);
            int lightX = light % 65536;
            int lightY = light / 65536;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);

            // 切换回主纹理单元
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D();

            // 调用父类渲染
            super.render(te, x, y, z, partialTicks, destroyStage);

            // 恢复lightmap坐标
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);

            GlStateManager.popAttrib();
            GlStateManager.popMatrix();
        } catch (Exception e) {
            // 如果GeckoLib渲染失败，恢复GL状态并使用备用渲染
            try {
                GlStateManager.popAttrib();
                GlStateManager.popMatrix();
            } catch (Exception ignored) {}

            // 备用渲染：简单方块
            renderFallback(te, x, y, z);
        }
    }

    /**
     * 备用渲染方法，当GeckoLib渲染失败时使用
     */
    private void renderFallback(TileEntityPrinter te, double x, double y, double z) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);

        // 简单的立方体渲染作为备用
        Minecraft.getMinecraft().getTextureManager().bindTexture(getTextureLocation(te));

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        // 渲染一个简单的方块轮廓表示
        GlStateManager.disableTexture2D();
        GlStateManager.color(0.5f, 0.5f, 0.5f, 0.5f);

        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION);
        // 简单立方体顶点
        double s = 0.4; // 半边长
        // 底面
        buffer.pos(-s, 0, -s).endVertex();
        buffer.pos(s, 0, -s).endVertex();
        buffer.pos(s, 0, s).endVertex();
        buffer.pos(-s, 0, s).endVertex();
        // 顶面
        buffer.pos(-s, 0.8, s).endVertex();
        buffer.pos(s, 0.8, s).endVertex();
        buffer.pos(s, 0.8, -s).endVertex();
        buffer.pos(-s, 0.8, -s).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @Override
    public ResourceLocation getTextureLocation(TileEntityPrinter instance) {
        // 根据激活状态切换纹理
        if (instance != null && instance.isProcessing()) {
            return new ResourceLocation("moremod", "textures/blocks/printer_active.png");
        }
        return new ResourceLocation("moremod", "textures/blocks/printer.png");
    }
}
