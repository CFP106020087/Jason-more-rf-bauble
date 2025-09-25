package com.moremod.client.model;

import com.moremod.entity.EntityVoidRipper;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class ModelVoidRipper extends AnimatedGeoModel<EntityVoidRipper> {

    private static final ResourceLocation MODEL = new ResourceLocation("moremod:geo/void_ripper.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation("moremod:textures/entity/void_ripper.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation("moremod:animations/void_ripper.animation.json");

    @Override
    public ResourceLocation getModelLocation(EntityVoidRipper object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityVoidRipper object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(EntityVoidRipper animatable) {
        return ANIMATION;
    }
}

