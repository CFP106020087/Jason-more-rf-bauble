package com.moremod.printer.client;

import com.moremod.moremod;
import com.moremod.printer.TileEntityPrinter;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * 打印机GeckoLib模型
 */
public class PrinterModel extends AnimatedGeoModel<TileEntityPrinter> {

    @Override
    public ResourceLocation getModelLocation(TileEntityPrinter object) {
        return new ResourceLocation(moremod.MODID, "geo/printer.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(TileEntityPrinter object) {
        return new ResourceLocation(moremod.MODID, "textures/blocks/printer.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(TileEntityPrinter animatable) {
        return new ResourceLocation(moremod.MODID, "animations/printer.animation.json");
    }
}
