package com.moremod.client.render;

import com.moremod.client.model.ModelVoidRipper;
import com.moremod.entity.EntityVoidRipper;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
@SideOnly(Side.CLIENT)

public class RenderVoidRipper extends GeoEntityRenderer<EntityVoidRipper> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("moremod:textures/entity/void_ripper.png");

    public RenderVoidRipper(RenderManager renderManager) {
        super(renderManager, new ModelVoidRipper());
        this.shadowSize = 0.8F;
    }

    @Override
    public void renderEarly(EntityVoidRipper animatable, float ticks, float red, float green, float blue, float partialTicks) {
        super.renderEarly(animatable, ticks, red, green, blue, partialTicks);

        // 修复负尺寸导致的面剔除问题
        GlStateManager.disableCull();

        // 缩放调整
        GlStateManager.scale(1.2F, 1.2F, 1.2F);

        // 充能时的视觉效果
        if (animatable.getIsCharging()) {
            float charge = (ticks % 20) / 20.0F;
            float scale = 1.0F + charge * 0.1F;
            GlStateManager.scale(scale, scale, scale);

            // 发光效果
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 0.5F + charge * 0.5F);
        }
    }

    @Override
    public void renderLate(EntityVoidRipper animatable, float ticks, float red, float green, float blue, float partialTicks) {
        super.renderLate(animatable, ticks, red, green, blue, partialTicks);

        // 渲染完成后恢复面剔除
        GlStateManager.enableCull();

        // 重置颜色和混合状态
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
    }

    @Override
    public ResourceLocation getEntityTexture(EntityVoidRipper entity) {
        return TEXTURE;
    }
}