package com.moremod.client.model;

import com.moremod.item.ItemSwordChengYue;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * 澄月剑 GeckoLib 模型（简化版）
 */
public class SwordChengYueModel extends AnimatedGeoModel<ItemSwordChengYue> {

    private static final ResourceLocation MODEL = new ResourceLocation("moremod", "geo/sword_chengyue.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation("moremod", "textures/item/sword_chengyue.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation("moremod", "animations/sword_chengyue.animation.json");

    public enum VisibilityMode {
        NORMAL,           // 全部显示（GUI用）
        HIDE_SCABBARD,    // 隐藏刀鞘（手持时）
        SCABBARD_ONLY     // 只显示刀鞘
    }

    private VisibilityMode mode = VisibilityMode.NORMAL;

    public void setMode(VisibilityMode m) {
        this.mode = (m == null) ? VisibilityMode.NORMAL : m;
    }

    @Override
    public ResourceLocation getModelLocation(ItemSwordChengYue object) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(ItemSwordChengYue object) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ItemSwordChengYue animatable) {
        return ANIMATION;
    }

    // 剑鞘骨骼
    private static final String[] SCABBARD_BONES = new String[]{
        "scabbard", "scabbard1_2"
    };

    // 剑骨骼
    private static final String[] SWORD_BONES = new String[]{
        "swordHandPart1", "swordHandLeftPart1", "swordHandPart1_3", "swordHandPart1_4",
        "swordHandPart1_1", "swordHandPart1_2", "swordHandRightPart1", "swordHandPart1_5",
        "swordHandPart1_6", "swordHandPart1_7", "swordHandPart1_8",
        "swordHandUpPart1", "swordHanUptLefTPart1", "swordHanUpLefttPart1",
        "swordHanUptPart1_2", "swordHanUptPart1_3", "swordHanUptPart1_4",
        "swordHanUptLefTPart2", "swordHanUpRighttPart1", "swordHanUptPart1_5",
        "swordHanUptPart1_6", "swordHanUptPart1_7",
        "swordHandUpPart2", "swordHanUptLefTPart3", "swordHanUpLefttPart2",
        "swordHanUptPart1_8", "swordHanUptPart1_9", "swordHanUptPart1_10",
        "swordHanUptLefTPart4", "swordHanUpRighttPart2", "swordHanUptPart1_11",
        "swordHanUptPart1_12", "swordHanUptPart1_13",
        "swordHandPart2", "swordHandLeftPart2", "swordHandPart1_9", "swordHandPart1_10",
        "swordHandPart1_11", "swordHandPart1_12", "swordHandRightPart2", "swordHandPart1_13",
        "swordHandPart1_14", "swordHandPart1_15", "swordHandPart1_16",
        "swordHandUpPart3", "swordHanUptLefTPart5", "swordHanUpLefttPart3",
        "swordHanUptPart1_14", "swordHanUptPart1_15", "swordHanUptPart1_16",
        "swordHanUptLefTPart6", "swordHanUpRighttPart3", "swordHanUptPart1_17",
        "swordHanUptPart1_18", "swordHanUptPart1_19",
        "swordHandUpPart4", "swordHanUptLefTPart7", "swordHanUpLefttPart4",
        "swordHanUptPart1_20", "swordHanUptPart1_21", "swordHanUptPart1_22",
        "swordHanUptLefTPart8", "swordHanUpRighttPart4", "swordHanUptPart1_23",
        "swordHanUptPart1_24", "swordHanUptPart1_25",
        "swordHead", "swordHead1", "swordHead1_2", "swordHead1_3",
        "swordHead1_4", "swordHead1_5", "swordHead2", "swordHead3", "swordHead3_1", "swordHead3_2"
    };

    private void setBonesHidden(String[] boneNames, boolean hidden) {
        for (String name : boneNames) {
            IBone b = getBone(name);
            if (b != null) b.setHidden(hidden);
        }
    }

    @Override
    public void setLivingAnimations(ItemSwordChengYue animatable, Integer uniqueID, AnimationEvent customPredicate) {
        super.setLivingAnimations(animatable, uniqueID, customPredicate);

        switch (mode) {
            case HIDE_SCABBARD:
                setBonesHidden(SCABBARD_BONES, true);
                break;
            case SCABBARD_ONLY:
                setBonesHidden(SWORD_BONES, true);
                setBonesHidden(SCABBARD_BONES, false);
                break;
            case NORMAL:
            default:
                setBonesHidden(SCABBARD_BONES, false);
                break;
        }
    }
}
