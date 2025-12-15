package com.moremod.printer.client;

import com.moremod.moremod;
import com.moremod.printer.TileEntityPrinter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.TRSRTransformation;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * 打印机 OBJ 渲染器
 *
 * 使用 Forge OBJLoader 加载 OBJ 模型进行渲染
 * AMD 显卡兼容 - 避免调用 setLightmapTextureCoords
 */
public class PrinterRenderer extends TileEntitySpecialRenderer<TileEntityPrinter> {

    private static final ResourceLocation OBJ_MODEL = new ResourceLocation(moremod.MODID, "models/block/printer.obj");
    private static final ResourceLocation TEXTURE = new ResourceLocation(moremod.MODID, "textures/blocks/printer.png");

    private IBakedModel bakedModel = null;
    private boolean modelLoadAttempted = false;

    @Override
    public void render(TileEntityPrinter te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (te == null) return;

        // 延迟加载模型
        if (!modelLoadAttempted) {
            loadModel();
            modelLoadAttempted = true;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);

        // 绑定纹理
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);

        // 设置渲染状态 - AMD 兼容
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableCull();

        RenderHelper.disableStandardItemLighting();

        if (bakedModel != null) {
            renderObjModel(te);
        } else {
            // 备用: 渲染简单方块
            renderFallbackCube();
        }

        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableCull();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void loadModel() {
        try {
            IModel model = ModelLoaderRegistry.getModel(OBJ_MODEL);
            bakedModel = model.bake(
                TRSRTransformation.identity(),
                DefaultVertexFormats.ITEM,
                location -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString())
            );
            System.out.println("[moremod] Printer OBJ model loaded successfully");
        } catch (Exception e) {
            System.err.println("[moremod] Failed to load Printer OBJ model: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void renderObjModel(TileEntityPrinter te) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 绑定方块纹理图集
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);

        // 渲染所有面
        for (EnumFacing facing : EnumFacing.values()) {
            List<BakedQuad> quads = bakedModel.getQuads(null, facing, 0);
            for (BakedQuad quad : quads) {
                buffer.addVertexData(quad.getVertexData());
            }
        }

        // 渲染无方向面
        List<BakedQuad> generalQuads = bakedModel.getQuads(null, null, 0);
        for (BakedQuad quad : generalQuads) {
            buffer.addVertexData(quad.getVertexData());
        }

        tessellator.draw();
    }

    private void renderFallbackCube() {
        // 简单的备用立方体渲染
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        float size = 0.4f;

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        // 顶面
        buffer.pos(-size, size * 2, -size).tex(0, 0).endVertex();
        buffer.pos(-size, size * 2, size).tex(0, 1).endVertex();
        buffer.pos(size, size * 2, size).tex(1, 1).endVertex();
        buffer.pos(size, size * 2, -size).tex(1, 0).endVertex();

        // 底面
        buffer.pos(-size, 0, size).tex(0, 0).endVertex();
        buffer.pos(-size, 0, -size).tex(0, 1).endVertex();
        buffer.pos(size, 0, -size).tex(1, 1).endVertex();
        buffer.pos(size, 0, size).tex(1, 0).endVertex();

        // 前面
        buffer.pos(-size, 0, size).tex(0, 0).endVertex();
        buffer.pos(size, 0, size).tex(1, 0).endVertex();
        buffer.pos(size, size * 2, size).tex(1, 1).endVertex();
        buffer.pos(-size, size * 2, size).tex(0, 1).endVertex();

        // 后面
        buffer.pos(size, 0, -size).tex(0, 0).endVertex();
        buffer.pos(-size, 0, -size).tex(1, 0).endVertex();
        buffer.pos(-size, size * 2, -size).tex(1, 1).endVertex();
        buffer.pos(size, size * 2, -size).tex(0, 1).endVertex();

        // 左面
        buffer.pos(-size, 0, -size).tex(0, 0).endVertex();
        buffer.pos(-size, 0, size).tex(1, 0).endVertex();
        buffer.pos(-size, size * 2, size).tex(1, 1).endVertex();
        buffer.pos(-size, size * 2, -size).tex(0, 1).endVertex();

        // 右面
        buffer.pos(size, 0, size).tex(0, 0).endVertex();
        buffer.pos(size, 0, -size).tex(1, 0).endVertex();
        buffer.pos(size, size * 2, -size).tex(1, 1).endVertex();
        buffer.pos(size, size * 2, size).tex(0, 1).endVertex();

        tessellator.draw();
    }
}
