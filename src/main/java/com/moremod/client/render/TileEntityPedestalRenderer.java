package com.moremod.client.render;

import com.moremod.tile.TileEntityPedestal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)

public class TileEntityPedestalRenderer extends TileEntitySpecialRenderer<TileEntityPedestal> {

    @Override
    public void render(TileEntityPedestal tile, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        if (tile == null) return;

        ItemStack stack = tile.getInv().getStackInSlot(0);
        if (stack.isEmpty()) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x + 0.5F, (float)y + 1.25F, (float)z + 0.5F);

        // 旋转动画
        double boop = Minecraft.getSystemTime() / 800D;
        GlStateManager.translate(0D, Math.sin(boop % (2 * Math.PI)) * 0.065, 0D);
        GlStateManager.rotate((float)(boop * 40D % 360), 0, 1, 0);

        // 缩放
        float scale = stack.getItem() instanceof ItemBlock ? 0.75F : 0.5F;
        GlStateManager.scale(scale, scale, scale);

        // 渲染物品
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.pushAttrib();
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popAttrib();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();

        GlStateManager.popMatrix();
    }
}