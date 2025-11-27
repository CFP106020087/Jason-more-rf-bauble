package com.moremod.client.render;

import com.moremod.client.model.ModelStoneSentinel;
import com.moremod.entity.boss.EntityStoneSentinel;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
@SideOnly(Side.CLIENT)

public class RenderStoneSentinel extends GeoEntityRenderer<EntityStoneSentinel> {

    public RenderStoneSentinel(RenderManager renderManager) {
        super(renderManager, new ModelStoneSentinel());
        this.shadowSize = 2.0F;
    }

    @Override
    public void doRender(EntityStoneSentinel entity, double x, double y, double z, float entityYaw, float partialTicks) {
        // 材質發光效果（憤怒時）
        if (entity.getIsAngry()) {
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();

            // 紅色光暈
            float pulse = (float)(Math.sin(entity.ticksExisted * 0.1) * 0.2 + 0.8);
            GlStateManager.color(1.0F, pulse * 0.5F, pulse * 0.5F, 1.0F);
        }

        super.doRender(entity, x, y, z, entityYaw, partialTicks);

        // 恢復渲染狀態
        if (entity.getIsAngry()) {
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }

        // 渲染眼睛發光層
        renderEyeGlow(entity, x, y, z, partialTicks);
    }

    private void renderEyeGlow(EntityStoneSentinel entity, double x, double y, double z, float partialTicks) {
        if (entity.getEyeState() == 0) return; // 閉眼時不發光

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        // 眼睛發光效果
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        // 根據狀態決定眼睛顏色
        if (entity.getIsAngry()) {
            // 憤怒：紅色眼睛
            GlStateManager.color(1.0F, 0.0F, 0.0F, 0.8F);
        } else if (entity.getEyeState() == 2) {
            // 正常睜眼：藍色眼睛
            GlStateManager.color(0.0F, 0.5F, 1.0F, 0.6F);
        } else {
            // 睜眼中：白色眼睛
            GlStateManager.color(1.0F, 1.0F, 1.0F, 0.4F);
        }

        // 這裡可以渲染額外的發光層
        // 需要一個單獨的眼睛發光材質

        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    @Override
    protected void applyRotations(EntityStoneSentinel entityLiving, float ageInTicks, float rotationYaw, float partialTicks) {
        // 巨石不旋轉身體，只有眼睛移動
        GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
    }
}