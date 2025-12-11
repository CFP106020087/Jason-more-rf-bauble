package com.moremod.printer.client;

import com.moremod.printer.TileEntityPrinter;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

/**
 * 打印机GeckoLib渲染器
 *
 * 使用GeckoLib渲染带有机械臂动画的打印机
 */
public class PrinterRenderer extends GeoBlockRenderer<TileEntityPrinter> {

    public PrinterRenderer() {
        super(new PrinterModel());
    }

    @Override
    public ResourceLocation getTextureLocation(TileEntityPrinter instance) {
        // 根据激活状态切换纹理
        if (instance != null && instance.isProcessing()) {
            return new ResourceLocation("moremod", "textures/blocks/printer_active.png");
        }
        return new ResourceLocation("moremod", "textures/blocks/printer.png");
    }
}
