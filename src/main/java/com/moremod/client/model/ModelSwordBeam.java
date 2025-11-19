package com.moremod.client.model;

import com.moremod.entity.EntitySwordBeam;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * 剑气模型 - 根据实体类型动态返回不同的模型和贴图
 */
public class ModelSwordBeam extends AnimatedGeoModel<EntitySwordBeam> {

    @Override
    public ResourceLocation getModelLocation(EntitySwordBeam entity) {
        // 根据实体的实际类型返回对应模型
        switch (entity.getBeamType()) {
            case DRAGON:
                return new ResourceLocation("moremod", "geo/dragon.geo.json");
                
            case BALL:
                return new ResourceLocation("moremod", "geo/ball_light.geo.json");
                
            case NORMAL:
            case SPIRAL:
            case CRESCENT:
            case CROSS:
            case PHOENIX:
            default:
                // 这些都使用光刃模型
                return new ResourceLocation("moremod", "geo/lxs_light.geo.json");
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySwordBeam entity) {
        // 根据实体类型返回对应贴图
        switch (entity.getBeamType()) {
            case DRAGON:
                // 64x64 贴图
                return new ResourceLocation("moremod", "textures/entity/dragon.png");
                
            case BALL:
                // 512x512 贴图
                return new ResourceLocation("moremod", "textures/entity/ball_light.png");
                
            case SPIRAL:
                return new ResourceLocation("moremod", "textures/entity/lxs_light.png");
                
            case CRESCENT:
                return new ResourceLocation("moremod", "textures/entity/beam_crescent.png");
                
            case CROSS:
                return new ResourceLocation("moremod", "textures/entity/beam_cross.png");
                
            case PHOENIX:
                return new ResourceLocation("moremod", "textures/entity/beam_phoenix.png");
                
            case NORMAL:
            default:
                // 256x256 贴图
                return new ResourceLocation("moremod", "textures/entity/lxs_light.png");
        }
    }

    @Override
    public ResourceLocation getAnimationFileLocation(EntitySwordBeam entity) {
        // 根据实体类型返回对应动画
        switch (entity.getBeamType()) {
            case DRAGON:
                return new ResourceLocation("moremod", "animations/dragon.animation.json");
                
            case BALL:
                return new ResourceLocation("moremod", "animations/ball_light.animation.json");
                
            case NORMAL:
            case SPIRAL:
            case CRESCENT:
            case CROSS:
            case PHOENIX:
            default:
                return new ResourceLocation("moremod", "animations/lxs_light.animation.json");
        }
    }
}