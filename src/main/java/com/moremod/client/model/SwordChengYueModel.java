package com.moremod.client.model;

import com.moremod.item.ItemSwordChengYue;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class SwordChengYueModel extends AnimatedGeoModel<ItemSwordChengYue> {

    public enum VisibilityMode {
        NORMAL,           // 全部顯示
        HIDE_SCABBARD,    // 手持：隱藏刀鞘
        SCABBARD_ONLY     // 背負：只顯示刀鞘
    }

    private VisibilityMode mode = VisibilityMode.NORMAL;
    private ItemCameraTransforms.TransformType currentTransform = ItemCameraTransforms.TransformType.NONE;
    
    public void setMode(VisibilityMode m) {
        this.mode = (m == null) ? VisibilityMode.NORMAL : m;
    }

    public void setTransformType(ItemCameraTransforms.TransformType transform) {
        this.currentTransform = transform;
    }

    @Override
    public ResourceLocation getModelLocation(ItemSwordChengYue object) {
        return new ResourceLocation("moremod", "geo/sword_chengyue.geo.json");
    }

    @Override
    public ResourceLocation getTextureLocation(ItemSwordChengYue object) {
        return new ResourceLocation("moremod", "textures/item/sword_chengyue.png");
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ItemSwordChengYue animatable) {
        return null;
    }

    // ====== 劍本體骨骼清單 ======
    private static final String[] SWORD_BONES = new String[]{
            "swordHandPart1","swordHandLeftPart1","swordHandPart1_3","swordHandPart1_4",
            "swordHandPart1_1","swordHandPart1_2","swordHandRightPart1","swordHandPart1_5",
            "swordHandPart1_6","swordHandPart1_7","swordHandPart1_8",
            "swordHandUpPart1","swordHanUptLefTPart1","swordHanUpLefttPart1",
            "swordHanUptPart1_2","swordHanUptPart1_3","swordHanUptPart1_4",
            "swordHanUptLefTPart2","swordHanUpRighttPart1","swordHanUptPart1_5",
            "swordHanUptPart1_6","swordHanUptPart1_7",
            "swordHandUpPart2","swordHanUptLefTPart3","swordHanUpLefttPart2",
            "swordHanUptPart1_8","swordHanUptPart1_9","swordHanUptPart1_10",
            "swordHanUptLefTPart4","swordHanUpRighttPart2","swordHanUptPart1_11",
            "swordHanUptPart1_12","swordHanUptPart1_13",
            "swordHandPart2","swordHandLeftPart2","swordHandPart1_9","swordHandPart1_10",
            "swordHandPart1_11","swordHandPart1_12","swordHandRightPart2","swordHandPart1_13",
            "swordHandPart1_14","swordHandPart1_15","swordHandPart1_16",
            "swordHandUpPart3","swordHanUptLefTPart5","swordHanUpLefttPart3",
            "swordHanUptPart1_14","swordHanUptPart1_15","swordHanUptPart1_16",
            "swordHanUptLefTPart6","swordHanUpRighttPart3","swordHanUptPart1_17",
            "swordHanUptPart1_18","swordHanUptPart1_19",
            "swordHandUpPart4","swordHanUptLefTPart7","swordHanUpLefttPart4",
            "swordHanUptPart1_20","swordHanUptPart1_21","swordHanUptPart1_22",
            "swordHanUptLefTPart8","swordHanUpRighttPart4","swordHanUptPart1_23",
            "swordHanUptPart1_24","swordHanUptPart1_25",
            "swordHead","swordHead1","swordHead1_2","swordHead1_3",
            "swordHead1_4","swordHead1_5","swordHead2","swordHead3","swordHead3_1","swordHead3_2"
    };

    private static void setHidden(IBone bone, boolean hidden) {
        if (bone != null) bone.setHidden(hidden);
    }

    private void setSwordVisible(boolean visible) {
        for (String name : SWORD_BONES) {
            IBone b = getBone(name);
            if (b != null) b.setHidden(!visible);
        }
    }
    
    /**
     * 調整主骨骼位置來對齊手部
     */
    private void adjustBonePosition() {
        IBone mainBone = getBone("all");
        if (mainBone == null) return;
        
        // 根據不同的顯示模式調整位置
        switch (currentTransform) {
            case FIRST_PERSON_RIGHT_HAND:
            case FIRST_PERSON_LEFT_HAND:
                // 第一人稱：調整位置讓刀柄對準手部
                mainBone.setPositionX(0);
                mainBone.setPositionY(-59);  // 將 Y=59 移到 Y=0
                mainBone.setPositionZ(-14.85f);  // 將 Z=14.85 移到 Z=0
                mainBone.setScaleX(0.7f);
                mainBone.setScaleY(0.7f);
                mainBone.setScaleZ(0.7f);
                break;
                
            case THIRD_PERSON_RIGHT_HAND:
            case THIRD_PERSON_LEFT_HAND:
                // 第三人稱：稍微不同的調整
                mainBone.setPositionX(0);
                mainBone.setPositionY(-59);
                mainBone.setPositionZ(-14.85f);
                mainBone.setScaleX(0.8f);
                mainBone.setScaleY(0.8f);
                mainBone.setScaleZ(0.8f);
                break;
                
            case GUI:
                // GUI 顯示：居中並縮小
                mainBone.setPositionX(0);
                mainBone.setPositionY(-30);
                mainBone.setPositionZ(-7);
                mainBone.setScaleX(0.5f);
                mainBone.setScaleY(0.5f);
                mainBone.setScaleZ(0.5f);
                break;
                
            default:
                // 默認：重置位置
                mainBone.setPositionX(0);
                mainBone.setPositionY(0);
                mainBone.setPositionZ(0);
                mainBone.setScaleX(1.0f);
                mainBone.setScaleY(1.0f);
                mainBone.setScaleZ(1.0f);
                break;
        }
    }

    @Override
    public void setLivingAnimations(ItemSwordChengYue animatable, Integer uniqueID, AnimationEvent customPredicate) {
        super.setLivingAnimations(animatable, uniqueID, customPredicate);

        // 調整骨骼位置
        adjustBonePosition();

        // 處理刀鞘顯示/隱藏
        IBone scabbard = getBone("scabbard");
        IBone scabbardTip = getBone("scabbard1_2");

        switch (mode) {
            case HIDE_SCABBARD:
                setSwordVisible(true);
                setHidden(scabbard, true);
                setHidden(scabbardTip, true);
                break;
            case SCABBARD_ONLY:
                setSwordVisible(false);
                setHidden(scabbard, false);
                setHidden(scabbardTip, false);
                break;
            case NORMAL:
            default:
                setSwordVisible(true);
                setHidden(scabbard, false);
                setHidden(scabbardTip, false);
                break;
        }
    }
}