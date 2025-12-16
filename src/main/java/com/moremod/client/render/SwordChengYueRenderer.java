package com.moremod.client.render;

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
 * 澄月剑渲染器（简化版）
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

        ItemCameraTransforms.TransformType transformType = getCurrentTransformType(stack);
        SwordChengYueModel model = (SwordChengYueModel) this.getGeoModelProvider();

        // GUI显示刀鞘，其他隐藏
        if (transformType == ItemCameraTransforms.TransformType.GUI) {
            model.setMode(SwordChengYueModel.VisibilityMode.NORMAL);
        } else {
            model.setMode(SwordChengYueModel.VisibilityMode.HIDE_SCABBARD);
        }

        GlStateManager.pushMatrix();

        // 基础变换
        GlStateManager.translate(0.5, 0.5, 0.5);
        GlStateManager.rotate(-77.5f, 1.0f, 0.0f, 0.0f);
        GlStateManager.translate(0, -3.7, -0.93);

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

        super.renderByItem(stack);
        GlStateManager.popMatrix();
    }

    private ItemCameraTransforms.TransformType getCurrentTransformType(ItemStack stack) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayerSP player = mc.player;

            if (mc.currentScreen != null) {
                return ItemCameraTransforms.TransformType.GUI;
            }

            if (player == null) {
                return ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND;
            }

            ItemStack mainHand = player.getHeldItemMainhand();
            ItemStack offHand = player.getHeldItemOffhand();

            boolean mainHasSword = mainHand.getItem() instanceof ItemSwordChengYue;
            boolean offHasSword = offHand.getItem() instanceof ItemSwordChengYue;
            boolean isOffHand = offHasSword && !mainHasSword;

            if (mc.gameSettings.thirdPersonView == 0) {
                return isOffHand ?
                    ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND :
                    ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND;
            } else {
                return isOffHand ?
                    ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND :
                    ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND;
            }

        } catch (Exception e) {
            return ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND;
        }
    }
}
