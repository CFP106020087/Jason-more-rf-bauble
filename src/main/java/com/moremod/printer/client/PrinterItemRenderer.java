package com.moremod.printer.client;

import com.moremod.moremod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * 打印机物品渲染器
 *
 * 使用 GeckoLib 在物品栏中渲染打印机的 3D 模型
 */
@SideOnly(Side.CLIENT)
public class PrinterItemRenderer extends TileEntityItemStackRenderer {

    public static final PrinterItemRenderer INSTANCE = new PrinterItemRenderer();

    private final PrinterItemModel model = new PrinterItemModel();
    private final ResourceLocation texture = new ResourceLocation(moremod.MODID, "textures/blocks/printer.png");

    @Override
    public void renderByItem(ItemStack stack) {
        GlStateManager.pushMatrix();

        // 调整位置和缩放以适应物品栏显示
        GlStateManager.translate(0.5, 0.0, 0.5);
        GlStateManager.scale(0.5, 0.5, 0.5);

        // 渲染 GeckoLib 模型
        try {
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            GeoModel geoModel = model.getModel(model.getModelLocation(null));

            if (geoModel != null) {
                // 使用简化的渲染方式
                GlStateManager.enableRescaleNormal();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

                // 渲染模型的每个骨骼
                software.bernie.geckolib3.geo.render.built.GeoBone rootBone = geoModel.topLevelBones.isEmpty() ? null : geoModel.topLevelBones.get(0);
                if (rootBone != null) {
                    renderBone(rootBone);
                }

                GlStateManager.disableBlend();
                GlStateManager.disableRescaleNormal();
            }
        } catch (Exception e) {
            // 静默处理渲染错误
        }

        GlStateManager.popMatrix();
    }

    /**
     * 递归渲染骨骼及其子骨骼
     */
    private void renderBone(software.bernie.geckolib3.geo.render.built.GeoBone bone) {
        GlStateManager.pushMatrix();

        // 应用骨骼变换
        GlStateManager.translate(bone.getPivotX() / 16.0, bone.getPivotY() / 16.0, bone.getPivotZ() / 16.0);

        if (bone.getRotationX() != 0) {
            GlStateManager.rotate((float) Math.toDegrees(bone.getRotationX()), 1, 0, 0);
        }
        if (bone.getRotationY() != 0) {
            GlStateManager.rotate((float) Math.toDegrees(bone.getRotationY()), 0, 1, 0);
        }
        if (bone.getRotationZ() != 0) {
            GlStateManager.rotate((float) Math.toDegrees(bone.getRotationZ()), 0, 0, 1);
        }

        GlStateManager.translate(-bone.getPivotX() / 16.0, -bone.getPivotY() / 16.0, -bone.getPivotZ() / 16.0);

        // 渲染立方体
        for (software.bernie.geckolib3.geo.render.built.GeoCube cube : bone.childCubes) {
            renderCube(cube);
        }

        // 递归渲染子骨骼
        for (software.bernie.geckolib3.geo.render.built.GeoBone child : bone.childBones) {
            renderBone(child);
        }

        GlStateManager.popMatrix();
    }

    /**
     * 渲染单个立方体
     */
    private void renderCube(software.bernie.geckolib3.geo.render.built.GeoCube cube) {
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.BufferBuilder buffer = tessellator.getBuffer();

        for (software.bernie.geckolib3.geo.render.built.GeoQuad quad : cube.quads) {
            if (quad == null) continue;

            buffer.begin(org.lwjgl.opengl.GL11.GL_QUADS, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX_NORMAL);

            // quad.normal is Vec3i in GeckoLib 3.0.31, convert to float
            net.minecraft.util.math.Vec3i normal = quad.normal;
            float nx = normal.getX();
            float ny = normal.getY();
            float nz = normal.getZ();

            for (software.bernie.geckolib3.geo.render.built.GeoVertex vertex : quad.vertices) {
                buffer.pos(vertex.position.x / 16.0, vertex.position.y / 16.0, vertex.position.z / 16.0)
                      .tex(vertex.textureU, vertex.textureV)
                      .normal(nx, ny, nz)
                      .endVertex();
            }

            tessellator.draw();
        }
    }

    /**
     * 虚拟 IAnimatable 实现，用于物品渲染
     */
    private static class DummyAnimatable implements IAnimatable {
        private final AnimationFactory factory = new AnimationFactory(this);

        @Override
        public void registerControllers(AnimationData data) {
            // 物品渲染不需要动画控制器
        }

        @Override
        public AnimationFactory getFactory() {
            return factory;
        }
    }

    /**
     * 专用于物品渲染的简化模型
     */
    private static class PrinterItemModel extends AnimatedGeoModel<DummyAnimatable> {
        @Override
        public ResourceLocation getModelLocation(DummyAnimatable object) {
            return new ResourceLocation(moremod.MODID, "geo/printer.geo.json");
        }

        @Override
        public ResourceLocation getTextureLocation(DummyAnimatable object) {
            return new ResourceLocation(moremod.MODID, "textures/blocks/printer.png");
        }

        @Override
        public ResourceLocation getAnimationFileLocation(DummyAnimatable animatable) {
            return new ResourceLocation(moremod.MODID, "animations/printer.animation.json");
        }
    }
}
