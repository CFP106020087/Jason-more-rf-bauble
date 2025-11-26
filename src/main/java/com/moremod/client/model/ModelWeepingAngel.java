package com.moremod.client.model;

import com.moremod.entity.EntityWeepingAngel;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * GeckoLib模型类
 */
public class ModelWeepingAngel extends AnimatedGeoModel<EntityWeepingAngel> {

    @Override
    public ResourceLocation getModelLocation(EntityWeepingAngel entity) {
        // 使用遮脸模型作为默认
        return new ResourceLocation("moremod", "geo/weeping_angel.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(EntityWeepingAngel entity) {
        // 使用单一贴图
        return new ResourceLocation("moremod", "textures/entity/weeping_angel.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(EntityWeepingAngel entity) {
        // 动画文件
        return new ResourceLocation("moremod", "animations/weeping_angel.animation.json");
    }
}