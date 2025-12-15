package com.moremod.client.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import com.moremod.client.model.Model3DPrinter;
import com.moremod.tile.TileEntity3DPrinter;

/**
 * 3D打印机方块实体渲染器
 * 
 * 使用方法：
 * 1. 在你的ClientProxy中注册：
 *    ClientRegistry.bindTileEntitySpecialRenderer(TileEntity3DPrinter.class, new TileEntityRenderer3DPrinter());
 * 
 * 2. 确保你的TileEntity实现了ITickable以获得平滑的动画
 */
public class TileEntityRenderer3DPrinter extends TileEntitySpecialRenderer<TileEntity3DPrinter> {
    
    // 模型纹理路径
    private static final ResourceLocation TEXTURE = new ResourceLocation("moremod", "textures/entity/3d_printer.png");
    
    // 模型实例（可以共享）
    private final Model3DPrinter model = new Model3DPrinter();
    
    @Override
    public void render(TileEntity3DPrinter te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        
        // 移动到方块中心
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        
        // 可选：根据方块朝向旋转
        // if (te.hasWorld()) {
        //     IBlockState state = te.getWorld().getBlockState(te.getPos());
        //     EnumFacing facing = state.getValue(BlockHorizontal.FACING);
        //     float rotation = facing.getHorizontalAngle();
        //     GlStateManager.rotate(rotation, 0, 1, 0);
        // }
        
        // 绑定纹理
        this.bindTexture(TEXTURE);
        
        // 启用透明度混合
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        
        // 计算动画时间
        // 使用世界时间 + partialTicks 实现平滑动画
        float worldTime = te.hasWorld() ? te.getWorld().getTotalWorldTime() : 0;
        float ageInTicks = worldTime + partialTicks;
        
        // 设置动画角度
        model.setRotationAngles(0, 0, ageInTicks, 0, 0, 0.0625F, null);
        
        // 翻转模型（Minecraft的Y轴向上，但模型通常是向下构建的）
        GlStateManager.rotate(180, 1, 0, 0);
        GlStateManager.translate(0, -1.5, 0);
        
        // 渲染模型
        model.render(null, 0, 0, ageInTicks, 0, 0, 0.0625F);
        
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}