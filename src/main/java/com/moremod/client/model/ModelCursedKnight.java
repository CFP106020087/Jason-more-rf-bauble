package com.moremod.client.model;

import com.moremod.entity.EntityCursedKnight;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class ModelCursedKnight extends AnimatedGeoModel<EntityCursedKnight> {

    @Override
    public ResourceLocation getModelLocation(EntityCursedKnight object) {
        return new ResourceLocation("moremod", "geo/cursed_knight.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(EntityCursedKnight object) {
        return new ResourceLocation("moremod", "textures/entity/cursed_knight.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(EntityCursedKnight animatable) {
        return new ResourceLocation("moremod", "animations/cursed_knight.animation.json");
    }
}