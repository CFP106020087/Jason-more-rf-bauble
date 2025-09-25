package com.moremod.client.render.fx;

import com.moremod.entity.fx.EntityRiftLightning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class RenderRiftLightning extends Render<EntityRiftLightning> {

    public RenderRiftLightning(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityRiftLightning entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }

    @Override
    public void doRender(EntityRiftLightning entity, double x, double y, double z, float entityYaw, float partialTicks) {
        BlockRendererDispatcher blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        // 绑定方块纹理
        this.bindEntityTexture(entity);

        // 旋转动画（快速旋转）
        float rotation = (float)(entity.ticksExisted * 20);
        GlStateManager.rotate(rotation, 1.0F, 1.0F, 1.0F);

        // 脉动缩放
        float pulse = (float)Math.sin((entity.ticksExisted + partialTicks) * 0.3F) * 0.1F;
        float scale = 0.5F + pulse;
        GlStateManager.scale(scale, scale, scale);

        // 半透明和发光
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();

        // 设置颜色（紫白色调，带脉动亮度）
        float brightness = 0.9F + (float)Math.sin((entity.ticksExisted + partialTicks) * 0.5F) * 0.1F;
        GlStateManager.color(brightness, brightness * 0.9F, 1.0F, 0.9F);

        // 渲染核心方块（海晶灯）
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);
        blockRenderer.renderBlockBrightness(Blocks.SEA_LANTERN.getDefaultState(), 1.0F);

        // 渲染外层光晕（更大的半透明层）
        GlStateManager.scale(1.3F, 1.3F, 1.3F);
        GlStateManager.translate(-0.12F, -0.12F, -0.12F);
        GlStateManager.color(0.7F, 0.7F, 1.0F, 0.4F);
        blockRenderer.renderBlockBrightness(Blocks.PACKED_ICE.getDefaultState(), 1.0F);

        // 恢复状态
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GL11.glPopMatrix();
    }
}