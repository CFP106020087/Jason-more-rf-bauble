package com.moremod.client.render;

import com.moremod.entity.EntityThrownCapsule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 投掷胶囊实体渲染器
 * 渲染飞行中的胶囊物品贴图和模型
 */
@SideOnly(Side.CLIENT)
public class RenderThrownCapsule extends Render<EntityThrownCapsule> {

    private final RenderItem itemRenderer;

    public RenderThrownCapsule(RenderManager renderManager) {
        super(renderManager);
        this.itemRenderer = Minecraft.getMinecraft().getRenderItem();
        this.shadowSize = 0.15F;
        this.shadowOpaque = 0.75F;
    }

    @Override
    public void doRender(EntityThrownCapsule entity, double x, double y, double z, float entityYaw, float partialTicks) {
        ItemStack capsuleStack = entity.getCapsuleStack();

        if (capsuleStack.isEmpty()) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + 0.1F, (float) z);
        GlStateManager.enableRescaleNormal();

        // 旋转动画 - 让胶囊在飞行时旋转
        float rotation = (entity.ticksExisted + partialTicks) * 15.0F;
        GlStateManager.rotate(rotation, 0.0F, 1.0F, 0.0F);

        // 面向摄像机
        GlStateManager.rotate(-this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((float)(this.renderManager.options.thirdPersonView == 2 ? -1 : 1) * this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);

        // 缩放
        float scale = 0.5F;
        GlStateManager.scale(scale, scale, scale);

        // 绑定物品贴图并渲染
        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        if (this.renderOutlines) {
            GlStateManager.enableColorMaterial();
            GlStateManager.enableOutlineMode(this.getTeamColor(entity));
        }

        // 渲染物品
        IBakedModel model = this.itemRenderer.getItemModelWithOverrides(capsuleStack, entity.world, null);
        this.itemRenderer.renderItem(capsuleStack, model);

        if (this.renderOutlines) {
            GlStateManager.disableOutlineMode();
            GlStateManager.disableColorMaterial();
        }

        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityThrownCapsule entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
