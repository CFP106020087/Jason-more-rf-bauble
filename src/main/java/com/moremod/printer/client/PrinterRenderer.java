package com.moremod.printer.client;

import com.moremod.moremod;
import com.moremod.printer.TileEntityPrinter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

/**
 * 打印机渲染器 - AMD 兼容版本
 *
 * Override render 方法避免调用 OpenGlHelper.setLightmapTextureCoords
 * 该调用会导致 AMD 显卡驱动崩溃 (atio6axx.dll)
 */
public class PrinterRenderer extends GeoBlockRenderer<TileEntityPrinter> {

    private final AnimatedGeoModel<TileEntityPrinter> model;

    public PrinterRenderer() {
        super(new PrinterModel());
        this.model = new PrinterModel();
    }

    @Override
    public ResourceLocation getTextureLocation(TileEntityPrinter instance) {
        return new ResourceLocation(moremod.MODID, "textures/blocks/printer.png");
    }

    @Override
    public void render(TileEntityPrinter te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (te == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.translate(0.5, 0, 0.5);

        // 绑定纹理
        Minecraft.getMinecraft().getTextureManager().bindTexture(getTextureLocation(te));

        // 设置渲染状态 - 不调用 setLightmapTextureCoords 以避免 AMD 崩溃
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderHelper.disableStandardItemLighting();

        try {
            GeoModel geoModel = model.getModel(model.getModelLocation(te));
            model.setLivingAnimations(te, this.getUniqueID(te));

            this.render(geoModel, te, partialTicks, 1.0F, 1.0F, 1.0F, 1.0F);
        } catch (Exception e) {
            // 静默处理渲染错误
        }

        RenderHelper.enableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
