package com.moremod.client.render;

import com.moremod.client.model.ModelWeepingAngel;
import com.moremod.entity.EntityWeepingAngel;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

/**
 * GeckoLib渲染器
 */
public class RenderWeepingAngel extends GeoEntityRenderer<EntityWeepingAngel> {

    public RenderWeepingAngel(RenderManager renderManager) {
        super(renderManager, new ModelWeepingAngel());
        this.shadowSize = 0.5F;
    }

    @Override
    public void renderEarly(EntityWeepingAngel entity, float ticks, float red, float green, float blue, float partialTicks) {
        super.renderEarly(entity, ticks, red, green, blue, partialTicks);

        // 如果被玩家看着（石化状态），添加石化效果
        if (entity.isPlayerWatching()) {
            GlStateManager.color(0.9F, 0.9F, 0.9F, 1.0F);
        }
    }
}