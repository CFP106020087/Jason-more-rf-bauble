package com.moremod.printer.client;

import com.moremod.moremod;
import com.moremod.printer.TileEntityPrinter;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

/**
 * 打印机渲染器 - 使用 GeckoLib 标准渲染
 */
public class PrinterRenderer extends GeoBlockRenderer<TileEntityPrinter> {

    public PrinterRenderer() {
        super(new PrinterModel());
    }

    @Override
    public ResourceLocation getTextureLocation(TileEntityPrinter instance) {
        return new ResourceLocation(moremod.MODID, "textures/blocks/printer.png");
    }
}
