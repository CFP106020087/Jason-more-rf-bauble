package com.moremod.client.render;

import com.moremod.tile.TileEntityProtectionField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * 保护领域发生器的特殊渲染器
 * 可选：添加能量核心动画或其他视觉效果
 */
@SideOnly(Side.CLIENT)
public class TESRProtectionField extends TileEntitySpecialRenderer<TileEntityProtectionField> {

    private static final ResourceLocation ENERGY_CORE_TEXTURE =
            new ResourceLocation("moremod", "textures/effects/energy_core.png");

    @Override
    public void render(TileEntityProtectionField te, double x, double y, double z,
                       float partialTicks, int destroyStage, float alpha) {
        if (te == null || !te.isActive()) {
            return;
        }

        // 渲染浮动的能量核心
        renderFloatingCore(te, x, y, z, partialTicks);

        // 可选：渲染半透明的保护领域边界
        if (Minecraft.getMinecraft().player.getHeldItemMainhand().getItem() ==
                net.minecraft.item.Item.getItemFromBlock(com.moremod.block.ProtectionFieldGenerator.INSTANCE)) {
            renderFieldBoundary(te, x, y, z, partialTicks);
        }
    }

    /**
     * 渲染浮动的能量核心
     */
    private void renderFloatingCore(TileEntityProtectionField te, double x, double y, double z, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 1.5, z + 0.5);

        float rotation = (te.getWorld().getTotalWorldTime() + partialTicks) * 2;
        GlStateManager.rotate(rotation, 0, 1, 0);

        float bobHeight = (float) Math.sin((te.getWorld().getTotalWorldTime() + partialTicks) * 0.1) * 0.1f;
        GlStateManager.translate(0, bobHeight, 0);

        float energyRatio = (float) te.getEnergyStored() / Math.max(1, te.getMaxEnergyStored());
        float scale = 0.3f + energyRatio * 0.2f;
        GlStateManager.scale(scale, scale, scale);

        // ★ 关键：看板到相机 + 关闭背面裁剪
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0F, 1F, 0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1F, 0F, 0F);
        GlStateManager.disableCull();

        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableLighting();

        bindTexture(ENERGY_CORE_TEXTURE);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        float r = 0.5f + energyRatio * 0.5f;
        float g = 0.8f;
        float b = 1.0f;
        float a = 0.7f + energyRatio * 0.3f;

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(-0.5, -0.5, 0).tex(0, 1).color(r, g, b, a).endVertex();
        buffer.pos( 0.5, -0.5, 0).tex(1, 1).color(r, g, b, a).endVertex();
        buffer.pos( 0.5,  0.5, 0).tex(1, 0).color(r, g, b, a).endVertex();
        buffer.pos(-0.5,  0.5, 0).tex(0, 0).color(r, g, b, a).endVertex();
        tessellator.draw();

        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
    }


    /**
     * 渲染半透明的领域边界（手持方块时显示）
     */
    private void renderFieldBoundary(TileEntityProtectionField te, double x, double y, double z, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);

        // 设置渲染状态
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);

        float range = te.getRange();
        float alpha = 0.1f;

        // 绘制线框球体
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 水平圆环
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        int segments = 64;
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2 * i) / segments;
            double x1 = Math.cos(angle) * range;
            double z1 = Math.sin(angle) * range;
            buffer.pos(x1, 0, z1).color(0.5f, 0.8f, 1.0f, alpha).endVertex();
        }
        tessellator.draw();

        // 垂直圆环（X-Y平面）
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2 * i) / segments;
            double x1 = Math.cos(angle) * range;
            double y1 = Math.sin(angle) * range;
            buffer.pos(x1, y1, 0).color(0.5f, 0.8f, 1.0f, alpha).endVertex();
        }
        tessellator.draw();

        // 垂直圆环（Z-Y平面）
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2 * i) / segments;
            double z1 = Math.cos(angle) * range;
            double y1 = Math.sin(angle) * range;
            buffer.pos(0, y1, z1).color(0.5f, 0.8f, 1.0f, alpha).endVertex();
        }
        tessellator.draw();

        // 恢复状态
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @Override
    public boolean isGlobalRenderer(TileEntityProtectionField te) {
        // 如果需要渲染超出方块边界的效果，返回true
        return te.isActive();
    }
}