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
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.geo.render.built.GeoQuad;
import software.bernie.geckolib3.geo.render.built.GeoVertex;

import java.util.Collections;

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
                // 处理动画
                processAnimations(te, partialTicks);

                // 渲染所有顶级骨骼
                for (GeoBone bone : model.topLevelBones) {
                    renderBone(bone);
                }
            }
        } catch (Exception e) {
            // 静默处理渲染错误
        }

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * 处理动画
     */
    private void processAnimations(TileEntityPrinter te, float partialTicks) {
        try {
            // 创建动画事件
            AnimationEvent<TileEntityPrinter> event = new AnimationEvent<>(
                te, 0, 0, partialTicks, false, Collections.emptyList()
            );

            // 获取动画控制器并更新
            for (AnimationController<?> controller : te.getFactory().getOrCreateAnimationData(te.hashCode()).getAnimationControllers().values()) {
                controller.process(
                    te.getFactory().getOrCreateAnimationData(te.hashCode()).tick,
                    event,
                    modelProvider.getBakedAnimations(),
                    modelProvider.getBakedModel(modelProvider.getModelLocation(te)),
                    modelProvider,
                    true
                );
            }

            // 更新 tick
            te.getFactory().getOrCreateAnimationData(te.hashCode()).tick += partialTicks;
        } catch (Exception e) {
            // 动画处理失败时静默忽略
        }
    }

    /**
     * 递归渲染骨骼
     */
    private void renderBone(GeoBone bone) {
        GlStateManager.pushMatrix();

        // 应用骨骼变换
        GlStateManager.translate(
            bone.getPivotX() / 16.0,
            bone.getPivotY() / 16.0,
            bone.getPivotZ() / 16.0
        );

        // 应用旋转
        if (bone.getRotationZ() != 0) {
            GlStateManager.rotate((float) Math.toDegrees(bone.getRotationZ()), 0, 0, 1);
        }
        if (bone.getRotationY() != 0) {
            GlStateManager.rotate((float) Math.toDegrees(bone.getRotationY()), 0, 1, 0);
        }
        if (bone.getRotationX() != 0) {
            GlStateManager.rotate((float) Math.toDegrees(bone.getRotationX()), 1, 0, 0);
        }

        // 应用缩放
        GlStateManager.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());

        GlStateManager.translate(
            -bone.getPivotX() / 16.0,
            -bone.getPivotY() / 16.0,
            -bone.getPivotZ() / 16.0
        );

        // 渲染立方体
        if (!bone.isHidden()) {
            for (GeoCube cube : bone.childCubes) {
                renderCube(cube);
            }
        }

        // 递归渲染子骨骼
        for (GeoBone child : bone.childBones) {
            renderBone(child);
        }

        GlStateManager.popMatrix();
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
                    vertex.position.x / 16.0,
                    vertex.position.y / 16.0,
                    vertex.position.z / 16.0
                ).tex(vertex.textureU, vertex.textureV)
                 .normal(nx, ny, nz)
                 .endVertex();
            }

            tessellator.draw();
        }
    }
}
