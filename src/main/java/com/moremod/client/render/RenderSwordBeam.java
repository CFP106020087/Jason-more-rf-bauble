package com.moremod.client.render;

import com.moremod.client.model.ModelSwordBeam;
import com.moremod.entity.EntitySwordBeam;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoProjectilesRenderer;

/**
 * 剑气渲染器 - 完整实现
 */
public class RenderSwordBeam extends GeoProjectilesRenderer<EntitySwordBeam> {

    // 默认模型
    public RenderSwordBeam(RenderManager renderManager) {
        super(renderManager, new ModelSwordBeam());
        this.shadowSize = 0.0F;
    }

    @Override
    public void doRender(EntitySwordBeam entity, double x, double y, double z,
                         float entityYaw, float partialTicks) {

        GlStateManager.pushMatrix();

        // 必须先移动到实体位置！
        GlStateManager.translate((float)x, (float)y, (float)z);
        GlStateManager.translate(0, -0.5F, 0);  // 加这行！向下移动0.5格

        // 然后再做旋转（如果需要）
         GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);

        // 放大3倍
        float baseScale = 3.0F;
        float typeScale = getScaleForType(entity.getBeamType());
        float finalScale = baseScale * typeScale * entity.getScale();
        GlStateManager.scale(finalScale, finalScale, finalScale);

        // 启用混合和发光
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );

        // 应用颜色
        float alpha = 1.0F - entity.getLifeProgress();
        GlStateManager.color(
                entity.getRed(),
                entity.getGreen(),
                entity.getBlue(),
                alpha
        );

        // 重要：translate后，传给父类的是相对坐标(0,0,0)
        super.doRender(entity, 0, 0, 0, entityYaw, partialTicks);
        //                     ↑ ↑ ↑ 注意这里是0,0,0！

        // 恢复状态
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * 根据类型返回缩放值
     */
    private float getScaleForType(EntitySwordBeam.BeamType type) {
        switch (type) {
            case BALL:
                return 0.01F;  // ball_light.geo.json有200x200的平面，需要极度缩小
            case DRAGON:
                return 0.1F;   // dragon模型也比较大
            case NORMAL:
            case SPIRAL:
            case CRESCENT:
            case CROSS:
            case PHOENIX:
            default:
                return 0.05F;  // lxs_light模型
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(EntitySwordBeam entity) {
        return this.getGeoModelProvider().getTextureLocation(entity);
    }
}