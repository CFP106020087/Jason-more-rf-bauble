package com.moremod.client.model;

import com.moremod.entity.boss.EntityRiftwarden;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class ModelRiftwarden extends AnimatedGeoModel<EntityRiftwarden> {

    private static final String MODID = "moremod";

    @Override
    public ResourceLocation getModelLocation(EntityRiftwarden entity) {
        // 模型文件路径: assets/moremod/geo/crack.geo.json
        return new ResourceLocation(MODID, "geo/crack.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(EntityRiftwarden entity) {
        // 贴图文件路径: assets/moremod/textures/entity/crack.png
        return new ResourceLocation(MODID, "textures/entity/crack.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(EntityRiftwarden entity) {
        // 动画文件路径: assets/moremod/animations/crack.animation.json
        return new ResourceLocation(MODID, "animations/crack.animation.json");
    }
}