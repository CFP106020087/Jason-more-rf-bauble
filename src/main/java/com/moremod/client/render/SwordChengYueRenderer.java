package com.moremod.client.render;

import com.moremod.capability.ChengYueCapability;
import com.moremod.client.model.SwordChengYueModel;
import com.moremod.item.ItemSwordChengYue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

/**
 * 澄月剑渲染器
 * 支持普通模式和技能模式（双手拔刀动画）
 */
@SideOnly(Side.CLIENT)
public class SwordChengYueRenderer extends GeoItemRenderer<ItemSwordChengYue> {

    public SwordChengYueRenderer() {
        super(new SwordChengYueModel());
    }

    @Override
    public void renderByItem(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemSwordChengYue)) {
            super.renderByItem(stack);
            return;
        }

        // 获取当前的变换类型
        ItemCameraTransforms.TransformType transformType = getCurrentTransformType(stack);

        // 获取模型
        SwordChengYueModel model = (SwordChengYueModel) this.getGeoModelProvider();

        // 检查技能状态
        boolean skillActive = isSkillActive();
        model.setSkillMode(skillActive);
        model.setTransformType(transformType);

        // 设置刀鞘显示逻辑（非技能模式时）
        if (!skillActive) {
            if (transformType == ItemCameraTransforms.TransformType.GUI) {
                model.setMode(SwordChengYueModel.VisibilityMode.NORMAL);
            } else {
                model.setMode(SwordChengYueModel.VisibilityMode.HIDE_SCABBARD);
            }
        }

        // 开始渲染
        GlStateManager.pushMatrix();

        if (skillActive) {
            // 技能模式：使用专门的变换
            renderSkillMode(transformType);
        } else {
            // 普通模式：使用原来的变换
            renderNormalMode(transformType);
        }

        // 调用父类渲染
        super.renderByItem(stack);

        GlStateManager.popMatrix();
    }

    /**
     * 普通模式渲染变换
     */
    private void renderNormalMode(ItemCameraTransforms.TransformType transformType) {
        // 基础变换
        GlStateManager.translate(0.5, 0.5, 0.5);
        GlStateManager.rotate(-77.5f, 1.0f, 0.0f, 0.0f);
        GlStateManager.translate(0, -3.7, -0.93);

        // 根据不同情况进行变换
        if (transformType != null) {
            switch (transformType) {
                case FIRST_PERSON_RIGHT_HAND:
                    GlStateManager.scale(0.7f, 0.7f, 0.7f);
                    GeckoDisplayConfig.applyTransform("firstperson_right");
                    break;

                case FIRST_PERSON_LEFT_HAND:
                    GlStateManager.scale(0.7f, 0.7f, 0.7f);
                    GeckoDisplayConfig.applyTransform("firstperson_left");
                    break;

                case THIRD_PERSON_RIGHT_HAND:
                    GlStateManager.rotate(180f, 0.0f, 1.0f, 0.0f);
                    GlStateManager.scale(0.8f, 0.8f, 0.8f);
                    GeckoDisplayConfig.applyTransform("thirdperson_right");
                    break;

                case THIRD_PERSON_LEFT_HAND:
                    GlStateManager.rotate(180f, 0.0f, 1.0f, 0.0f);
                    GlStateManager.scale(0.8f, 0.8f, 0.8f);
                    GeckoDisplayConfig.applyTransform("thirdperson_left");
                    break;

                case GUI:
                    GlStateManager.rotate(45f, 0.0f, 1.0f, 0.0f);
                    GlStateManager.rotate(-25f, 0.0f, 0.0f, 1.0f);
                    GlStateManager.translate(0, 1.7, 0);
                    GlStateManager.scale(0.5f, 0.5f, 0.5f);
                    GeckoDisplayConfig.applyTransform("gui");
                    break;

                case GROUND:
                    GlStateManager.translate(0, 1.7, 0);
                    GlStateManager.scale(0.4f, 0.4f, 0.4f);
                    GeckoDisplayConfig.applyTransform("ground");
                    break;

                case FIXED:
                    GlStateManager.rotate(180f, 0.0f, 1.0f, 0.0f);
                    GlStateManager.translate(0, 1.7, 0);
                    GlStateManager.scale(0.6f, 0.6f, 0.6f);
                    GeckoDisplayConfig.applyTransform("fixed");
                    break;

                default:
                    GlStateManager.scale(0.8f, 0.8f, 0.8f);
                    GeckoDisplayConfig.applyTransform("gui");
                    break;
            }
        }
    }

    /**
     * 技能模式渲染变换（双手拔刀动画）
     */
    private void renderSkillMode(ItemCameraTransforms.TransformType transformType) {
        // 技能模式使用不同的基础变换，适配新的 moon_sword 模型
        // 模型枢轴点约在 (0, 20, -20)，需要相应调整

        if (transformType != null) {
            switch (transformType) {
                case FIRST_PERSON_RIGHT_HAND:
                case FIRST_PERSON_LEFT_HAND:
                    // 第一人称：显示双手持剑
                    GlStateManager.translate(0.5, 1.5, 0.5);
                    GlStateManager.scale(0.04f, 0.04f, 0.04f);
                    GlStateManager.rotate(180f, 0.0f, 1.0f, 0.0f);
                    GlStateManager.translate(0, -20, 10); // 补偿模型枢轴点偏移
                    break;

                case THIRD_PERSON_RIGHT_HAND:
                case THIRD_PERSON_LEFT_HAND:
                    // 第三人称
                    GlStateManager.translate(0.5, 1.2, 0.5);
                    GlStateManager.scale(0.035f, 0.035f, 0.035f);
                    GlStateManager.rotate(180f, 0.0f, 1.0f, 0.0f);
                    GlStateManager.translate(0, -20, 10);
                    break;

                default:
                    // GUI 等其他模式
                    GlStateManager.translate(0.5, 0.5, 0.5);
                    GlStateManager.scale(0.02f, 0.02f, 0.02f);
                    break;
            }
        }
    }

    /**
     * 检查玩家是否处于技能激活状态
     */
    private boolean isSkillActive() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayerSP player = mc.player;
            if (player == null) return false;

            ChengYueCapability cap = player.getCapability(ChengYueCapability.CAPABILITY, null);
            return cap != null && cap.isSkillActive();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取当前的变换类型（支持副手检测）
     */
    private ItemCameraTransforms.TransformType getCurrentTransformType(ItemStack stack) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayerSP player = mc.player;

            // GUI 检测
            if (mc.currentScreen != null) {
                return ItemCameraTransforms.TransformType.GUI;
            }

            // 玩家检测
            if (player == null) {
                return ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND;
            }

            // 检测主手和副手
            ItemStack mainHand = player.getHeldItemMainhand();
            ItemStack offHand = player.getHeldItemOffhand();

            boolean mainHasSword = mainHand.getItem() instanceof ItemSwordChengYue;
            boolean offHasSword = offHand.getItem() instanceof ItemSwordChengYue;

            // 判断是副手还是主手
            boolean isOffHand = offHasSword && !mainHasSword;

            // 根据视角返回正确的 TransformType
            if (mc.gameSettings.thirdPersonView == 0) {
                // 第一人称
                return isOffHand ?
                    ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND :
                    ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND;
            } else {
                // 第三人称
                return isOffHand ?
                    ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND :
                    ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND;
        }
    }
}
