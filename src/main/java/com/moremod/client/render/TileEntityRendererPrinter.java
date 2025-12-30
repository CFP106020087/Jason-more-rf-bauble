package com.moremod.client.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import com.moremod.client.model.Model3DPrinter;
import com.moremod.printer.BlockPrinter;
import com.moremod.printer.TileEntityPrinter;

/**
 * 3D打印机方块实体渲染器
 *
 * 使用方法：
 * 在ClientProxy中注册：
 * ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPrinter.class, new TileEntityRendererPrinter());
 */
public class TileEntityRendererPrinter extends TileEntitySpecialRenderer<TileEntityPrinter> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("moremod", "textures/entity/3d_printer.png");

    private final Model3DPrinter model = new Model3DPrinter();

    @Override
    public void render(TileEntityPrinter te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();

        // 移动到方块中心
        GlStateManager.translate(x + 0.5, y, z + 0.5);

        // 根据方块朝向旋转
        if (te.hasWorld()) {
            IBlockState state = te.getWorld().getBlockState(te.getPos());
            if (state.getBlock() instanceof BlockPrinter) {
                EnumFacing facing = state.getValue(BlockPrinter.FACING);
                float rotation = facing.getHorizontalAngle();
                GlStateManager.rotate(rotation, 0, 1, 0);
            }
        }

        // 绑定纹理
        this.bindTexture(TEXTURE);

        // 启用透明度混合
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        // 计算打印进度
        float progress = 0;
        if (te.isProcessing()) {
            progress = (float) te.getProgress() / Math.max(1, te.getMaxProgress());
        }

        // 计算动画时间
        float ageInTicks;
        float worldTime = te.hasWorld() ? (te.getWorld().getTotalWorldTime() % 10000) : 0;

        if (te.isProcessing()) {
            // 正在打印时：机械臂正常速度摆动
            ageInTicks = worldTime + partialTicks;
        } else {
            // 空闲时：机械臂正常速度摆动
            ageInTicks = worldTime + partialTicks;
        }

        // 设置打印状态（必须在setRotationAngles之前调用）
        model.setPrintingState(te.isProcessing(), te.getCurrentRecipe() != null, progress);

        // 设置动画
        model.setRotationAngles(0, 0, ageInTicks, 0, 0, 0.0625F, null);

        // 翻转模型
        GlStateManager.rotate(180, 1, 0, 0);
        GlStateManager.translate(0, -1.5, 0);

        // 渲染模型
        model.render(null, 0, 0, ageInTicks, 0, 0, 0.0625F);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}