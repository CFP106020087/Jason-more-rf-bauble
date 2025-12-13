package com.moremod.printer.client;

import com.moremod.moremod;
import com.moremod.printer.TileEntityPrinter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.geo.render.built.GeoQuad;
import software.bernie.geckolib3.geo.render.built.GeoVertex;

/**
 * 打印机渲染器 - 手动渲染 GeckoLib 模型
 *
 * 完全绕过 GeoBlockRenderer 以避免 AMD 显卡驱动崩溃
 * AMD 驱动在 OpenGlHelper.setLightmapTextureCoords 调用时会崩溃
 */
public class PrinterRenderer extends TileEntitySpecialRenderer<TileEntityPrinter> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(moremod.MODID, "textures/blocks/printer.png");
    private final PrinterModel modelProvider = new PrinterModel();

    @Override
    public void render(TileEntityPrinter te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (te == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);

        // 绑定纹理
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);

        // 设置渲染状态
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        try {
            // 获取模型
            GeoModel model = modelProvider.getModel(modelProvider.getModelLocation(te));
            if (model != null) {
                // GeckoLib 的顶点坐标已经是最终位置，直接渲染所有骨骼
                for (GeoBone bone : model.topLevelBones) {
                    renderBoneSimple(bone);
                }
            }
        } catch (Exception e) {
            // 静默处理渲染错误
        }

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * 简单渲染骨骼 - 不做额外变换，因为顶点已经是最终坐标
     */
    private void renderBoneSimple(GeoBone bone) {
        // 渲染立方体
        if (!bone.isHidden()) {
            for (GeoCube cube : bone.childCubes) {
                renderCube(cube);
            }
        }

        // 递归渲染子骨骼
        for (GeoBone child : bone.childBones) {
            renderBoneSimple(child);
        }
    }

    /**
     * 渲染立方体
     */
    private void renderCube(GeoCube cube) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        for (GeoQuad quad : cube.quads) {
            if (quad == null) continue;

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL);

            float nx = quad.normal.getX();
            float ny = quad.normal.getY();
            float nz = quad.normal.getZ();

            for (GeoVertex vertex : quad.vertices) {
                buffer.pos(
                    vertex.position.x,
                    vertex.position.y,
                    vertex.position.z
                ).tex(vertex.textureU, vertex.textureV)
                 .normal(nx, ny, nz)
                 .endVertex();
            }

            tessellator.draw();
        }
    }
}
