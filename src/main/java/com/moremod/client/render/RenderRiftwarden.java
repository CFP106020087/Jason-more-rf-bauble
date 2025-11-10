package com.moremod.client.render;

import com.moremod.client.model.ModelRiftwarden;
import com.moremod.entity.boss.EntityRiftwarden;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
@SideOnly(Side.CLIENT)

public class RenderRiftwarden extends GeoEntityRenderer<EntityRiftwarden> {

    public RenderRiftwarden(RenderManager renderManager) {
        super(renderManager, new ModelRiftwarden());
        this.shadowSize = 0.7F; // 阴影大小
    }

    @Override
    public void renderEarly(EntityRiftwarden animatable, float ticks, float red, float green, float blue, float partialTicks) {
        super.renderEarly(animatable, ticks, red, green, blue, partialTicks);

        // 解决负尺寸导致的内外翻转问题
        // 禁用背面剔除，这样内外两面都会渲染
        GlStateManager.disableCull();

        // 根据Boss阶段调整大小和颜色
        float scale = 1.0F;
        int phase = animatable.getPhase();

        switch (phase) {
            case 0:
                scale = 1.0F;
                break;
            case 1:
                scale = 1.1F;
                break;
            case 2:
                scale = 1.2F;
                // 球体开始出现，添加紫色光晕
                GlStateManager.color(1.0F, 0.9F, 1.0F, 1.0F);
                break;
            case 3:
                scale = 1.3F;
                // 最后阶段添加红色光晕
                GlStateManager.color(1.0F, 0.8F, 0.8F, 1.0F);
                break;
        }

        GlStateManager.scale(scale, scale, scale);

        // 球体旋转效果（阶段2及以上）
        if (phase >= 2 && !animatable.isMoving()) {
            // 添加微妙的悬浮效果
            float hover = MathHelper.sin(animatable.ticksExisted * 0.1F) * 0.05F;
            GlStateManager.translate(0, hover, 0);

            // 球体发光效果
            float ballGlow = 0.8F + MathHelper.sin(animatable.ticksExisted * 0.05F) * 0.2F;
            GlStateManager.color(ballGlow, ballGlow * 0.8F, 1.0F, 1.0F);
        }

        // 如果正在充能激光，添加发光效果
        if (animatable.isChargingLaser()) {
            float chargeProgress = (float)animatable.getLaserChargeTime() / 40.0F;
            float glow = 0.5F + chargeProgress * 0.5F;
            GlStateManager.color(glow, glow, 1.0F, 1.0F);

            // 添加震动效果
            if (chargeProgress > 0.5F) {
                float shake = (chargeProgress - 0.5F) * 0.1F;
                GlStateManager.translate(
                        (Math.random() - 0.5) * shake,
                        (Math.random() - 0.5) * shake,
                        (Math.random() - 0.5) * shake
                );
            }
        }

        // 如果正在蓄力射击，添加紫色光晕
        if (animatable.isChargingShooting()) {
            float chargeProgress = (float)animatable.getChargeShootTimer() / 60.0F;
            float glow = 0.5F + chargeProgress * 0.5F;
            GlStateManager.color(1.0F, glow, 1.0F, 1.0F);

            // 添加旋转效果
            GlStateManager.rotate(chargeProgress * 360F, 0, 1, 0);
        }

        // 无敌状态时的特效
        if (animatable.isGateInvulnerable()) {
            // 添加透明度和颜色变化
            float alpha = 0.6F + (float)Math.sin(animatable.ticksExisted * 0.1F) * 0.4F;
            GlStateManager.color(0.5F, 0.0F, 1.0F, alpha);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

            // 添加护盾波动效果
            float shieldScale = 1.0F + (float)Math.sin(animatable.ticksExisted * 0.2F) * 0.05F;
            GlStateManager.scale(shieldScale, shieldScale, shieldScale);
        }
    }

    @Override
    public void doRender(EntityRiftwarden entity, double x, double y, double z, float entityYaw, float partialTicks) {
        // 在渲染前设置OpenGL状态
        GlStateManager.pushMatrix();

        // 解决负尺寸问题的关键设置
        GlStateManager.disableCull(); // 禁用背面剔除

        super.doRender(entity, x, y, z, entityYaw, partialTicks);

        // 恢复OpenGL状态
        GlStateManager.enableCull(); // 重新启用背面剔除
        GlStateManager.popMatrix();

        // 重置颜色状态，避免影响其他渲染
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
    }

    @Override
    public void renderLate(EntityRiftwarden animatable, float ticks, float red, float green, float blue, float partialTicks) {
        super.renderLate(animatable, ticks, red, green, blue, partialTicks);

        // 在渲染后恢复剔除状态
        GlStateManager.enableCull();

        // 确保颜色状态正确重置
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}