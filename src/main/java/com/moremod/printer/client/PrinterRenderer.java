package com.moremod.printer.client;

import com.moremod.moremod;
import com.moremod.printer.TileEntityPrinter;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

/**
 * 打印机 GeckoLib 渲染器
 *
 * 使用 GeckoLib 渲染带有机械臂动画的打印机
 */
public class PrinterRenderer extends GeoBlockRenderer<TileEntityPrinter> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(moremod.MODID, "textures/blocks/printer.png");

    public PrinterRenderer() {
        super(new PrinterModel());
    }

    @Override
    public ResourceLocation getTextureLocation(TileEntityPrinter instance) {
        return TEXTURE;
    }
}
