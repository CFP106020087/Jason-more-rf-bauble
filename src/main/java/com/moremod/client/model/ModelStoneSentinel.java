package com.moremod.client.model;

import com.moremod.entity.boss.EntityStoneSentinel;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class ModelStoneSentinel extends AnimatedGeoModel<EntityStoneSentinel> {

    // 兩種材質路徑
    private static final ResourceLocation TEXTURE_NORMAL =
            new ResourceLocation("moremod", "textures/entity/stone_sentinel_normal.png");
    private static final ResourceLocation TEXTURE_ANGRY =
            new ResourceLocation("moremod", "textures/entity/stone_sentinel_angry.png");

    @Override
    public ResourceLocation getModelLocation(EntityStoneSentinel object) {
        return new ResourceLocation("moremod", "geo/stone.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(EntityStoneSentinel object) {
        // 根據憤怒狀態切換材質
        return object.getIsAngry() ? TEXTURE_ANGRY : TEXTURE_NORMAL;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(EntityStoneSentinel animatable) {
        return new ResourceLocation("moremod", "animations/stone.animation.json");
    }
  @Override
public void setLivingAnimations(EntityStoneSentinel entity, Integer uniqueID, AnimationEvent customPredicate) {
      super.setLivingAnimations(entity, uniqueID, customPredicate);

      // 獲取 "all" 骨骼並應用旋轉
      IBone allBone = this.getAnimationProcessor().getBone("all");
      if (allBone != null) {
          // 轉換角度到弧度並應用
          float rotationRadians = entity.renderYawOffset * ((float) Math.PI / 180F);
          allBone.setRotationY(-rotationRadians); // 負值因為Minecraft的坐標系統
      }
  }}
