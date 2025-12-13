package com.moremod.printer.client;

import com.moremod.moremod;
import com.moremod.printer.TileEntityPrinter;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * 打印机 GeckoLib 模型
 */
public class PrinterModel extends AnimatedGeoModel<TileEntityPrinter> {

    private static final ResourceLocation MODEL = new ResourceLocation(moremod.MODID, "geo/printer.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(moremod.MODID, "textures/blocks/printer.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(moremod.MODID, "animations/printer.animation.json");

    @Override
    public ResourceLocation getModelLocation(TileEntityPrinter object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(TileEntityPrinter object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(TileEntityPrinter animatable) {
        return ANIMATION;
    }
}
