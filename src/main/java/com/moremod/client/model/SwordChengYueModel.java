package com.moremod.client.model;

import com.moremod.item.ItemSwordChengYue;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * 澄月剑 GeckoLib 模型
 * 支持双模型切换：
 * - 普通状态: sword_chengyue.geo.json
 * - 技能状态: moon_sword.json (带双手拔刀动画)
 */
public class SwordChengYueModel extends AnimatedGeoModel<ItemSwordChengYue> {

    // 模型资源路径
    private static final ResourceLocation MODEL_NORMAL = new ResourceLocation("moremod", "geo/sword_chengyue.geo.json");
    private static final ResourceLocation MODEL_SKILL = new ResourceLocation("moremod", "geo/moon_sword.geo.json");

    private static final ResourceLocation TEXTURE_NORMAL = new ResourceLocation("moremod", "textures/item/sword_chengyue.png");
    private static final ResourceLocation TEXTURE_SKILL = new ResourceLocation("moremod", "geo/moon_sword.png");

    private static final ResourceLocation ANIMATION_SKILL = new ResourceLocation("moremod", "geo/moon_sword.animation.json");

    public enum VisibilityMode {
        NORMAL,           // 全部显示（GUI用）
        HIDE_SCABBARD,    // 隐藏刀鞘（手持时）
        SCABBARD_ONLY     // 只显示刀鞘
    }

    public enum ViewMode {
        FIRST_PERSON,
        THIRD_PERSON,
        GUI,
        OTHER
    }

    private VisibilityMode mode = VisibilityMode.NORMAL;
    private ViewMode viewMode = ViewMode.OTHER;
    private ItemCameraTransforms.TransformType currentTransform = ItemCameraTransforms.TransformType.NONE;

    // 技能模式
    private boolean skillMode = false;

    public void setMode(VisibilityMode m) {
        this.mode = (m == null) ? VisibilityMode.NORMAL : m;
    }

    public void setViewMode(ViewMode mode) {
        this.viewMode = mode;
    }

    public ViewMode getViewMode() {
        return viewMode;
    }

    public void setSkillMode(boolean active) {
        this.skillMode = active;
    }

    public boolean isSkillMode() {
        return skillMode;
    }

    public void setTransformType(ItemCameraTransforms.TransformType transform) {
        this.currentTransform = transform;
        // 根据 transform 自动设置 viewMode
        if (transform == ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND ||
            transform == ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND) {
            this.viewMode = ViewMode.FIRST_PERSON;
        } else if (transform == ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND ||
                   transform == ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND) {
            this.viewMode = ViewMode.THIRD_PERSON;
        } else if (transform == ItemCameraTransforms.TransformType.GUI) {
            this.viewMode = ViewMode.GUI;
        } else {
            this.viewMode = ViewMode.OTHER;
        }
    }

    @Override
    public ResourceLocation getModelLocation(ItemSwordChengYue object) {
        return skillMode ? MODEL_SKILL : MODEL_NORMAL;
    }

    @Override
    public ResourceLocation getTextureLocation(ItemSwordChengYue object) {
        return skillMode ? TEXTURE_SKILL : TEXTURE_NORMAL;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(ItemSwordChengYue animatable) {
        // 只在技能模式下返回动画文件
        return skillMode ? ANIMATION_SKILL : null;
    }

    // ====== 手臂骨骼 (技能模型) ======
    private static final String[] ARM_BONES = new String[]{
        "leftArm1", "leftArm1_2", "rightArm1", "rightArm1_2"
    };

    // ====== 剑鞘骨骼 (两个模型都有) ======
    private static final String[] SCABBARD_BONES_NORMAL = new String[]{
        "scabbard", "scabbard1_2"
    };

    private static final String[] SCABBARD_BONES_SKILL = new String[]{
        "scabbard", "scabbardMain", "scabbard1_2"
    };

    // ====== 原始模型的剑骨骼 ======
    private static final String[] SWORD_BONES_NORMAL = new String[]{
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

        if (skillMode) {
            // 技能模式：使用 moon_sword 模型
            // 根据视图模式控制手臂显示（只在第一人称显示双手）
            boolean showArms = (viewMode == ViewMode.FIRST_PERSON);
            setBonesHidden(ARM_BONES, !showArms);

            // 显示剑鞘
            setBonesHidden(SCABBARD_BONES_SKILL, false);
        } else {
            // 普通模式：使用原始 sword_chengyue 模型
            // 根据显示模式控制刀鞘
            switch (mode) {
                case HIDE_SCABBARD:
                    setBonesHidden(SCABBARD_BONES_NORMAL, true);
                    break;
                case SCABBARD_ONLY:
                    // 隐藏剑，只显示鞘
                    setBonesHidden(SWORD_BONES_NORMAL, true);
                    setBonesHidden(SCABBARD_BONES_NORMAL, false);
                    break;
                case NORMAL:
                default:
                    setBonesHidden(SCABBARD_BONES_NORMAL, false);
                    break;
            }
        }
    }
}
